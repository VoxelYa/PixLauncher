import * as fs from 'fs';
import * as path from 'path';
import { spawn } from 'child_process';
import { gameDir, runtimeDir } from '../config';
import { pairs } from '../download/sources';
import { downloadFile } from '../download/engine';
import { log } from '../logger';

/**
 * Forge 1.8.9 — latest build 11.15.1.2318-1.8.9.
 * The installer runs silently with the bundled JRE 8 and patches/merges into
 * versions/<id>/ (Forge itself patches the vanilla jar via binpatches — the
 * established local-patch flow).
 */

export const FORGE_VERSION = '1.8.9-11.15.1.2318-1.8.9';
const FORGE_MAVEN_PATH = `${FORGE_VERSION}/forge-${FORGE_VERSION}-installer.jar`;

/** group:artifact:version[:classifier] -> maven-relative path */
function mavenToPath(mavenName: string): string {
  const [group, artifact, version, ...rest] = mavenName.split(':');
  const classifier = rest[0] ? `-${rest[0]}` : '';
  return `${group.split('.').join('/')}/${artifact}/${version}/${artifact}-${version}${classifier}.jar`;
}

/**
 * The legacy installer skips several client-required libraries (launchwrapper,
 * asm, jline, lzma, scala-*) depending on run mode; the game cannot start
 * without them. Parse the installed version JSON and fetch every missing jar,
 * mirror-first (BMCLAPI maven route), falling back to the JSON's own URL.
 */
export async function ensureForgeLibraries(forgeId: string, onProgress: (msg: string) => void): Promise<void> {
  const jsonPath = path.join(gameDir(), 'versions', forgeId, `${forgeId}.json`);
  const spec = JSON.parse(fs.readFileSync(jsonPath, 'utf8')) as {
    libraries: { name: string; url?: string; clientreq?: boolean }[];
  };
  const libRoot = path.join(gameDir(), 'libraries');
  const items: { dest: string; mirror: string; official: string; label: string }[] = [];
  for (const lib of spec.libraries) {
    if (lib.clientreq === false) continue;
    const rel = mavenToPath(lib.name);
    const dest = path.join(libRoot, rel);
    if (fs.existsSync(dest)) continue;
    const host = (lib.url ?? 'https://libraries.minecraft.net/').replace(/\/?$/, '/');
    items.push({
      dest,
      mirror: `${'https://bmclapi2.bangbang93.com'}/maven/${rel}`,
      official: `${host}${rel}`,
      label: lib.name
    });
  }
  if (!items.length) return;
  onProgress(`Fetching ${items.length} missing Forge libraries…`);
  const { downloadAll } = await import('../download/engine');
  await downloadAll(
    items.map((it) => ({ url: it.mirror, fallbackUrl: it.official, dest: it.dest })),
    'forge libraries',
    10,
    (p) => onProgress(p.label)
  );
}

export async function installForge(javaExePath: string, onProgress: (msg: string) => void): Promise<string> {
  const versionsDir = path.join(gameDir(), 'versions');
  const existing = fs.existsSync(versionsDir)
    ? fs.readdirSync(versionsDir).find((d) => d.includes('forge') && d.startsWith('1.8.9'))
    : undefined;
  if (existing) {
    onProgress(`Forge already installed (${existing})`);
    await ensureForgeLibraries(existing, onProgress);
    return existing;
  }

  // The legacy installer refuses without a launcher profile file
  // ("you need to run the launcher first") — provide a minimal one.
  const profilesPath = path.join(gameDir(), 'launcher_profiles.json');
  if (!fs.existsSync(profilesPath)) {
    fs.mkdirSync(gameDir(), { recursive: true });
    fs.writeFileSync(profilesPath, JSON.stringify({ profiles: {}, settings: {} }));
  }

  onProgress('Downloading Forge installer…');
  const pair = pairs.forgeInstaller(FORGE_MAVEN_PATH);
  const installerPath = path.join(runtimeDir(), `forge-${FORGE_VERSION}-installer.jar`);
  await downloadFile(
    { url: pair.mirror, fallbackUrl: pair.official, dest: installerPath },
    'forge installer',
    (p) => onProgress(`Forge installer ${p.current}/${p.total} bytes`)
  );

  // The 1.8.9-era installer has no headless client CLI (only --installServer
  // / --extract; verified via javap), so invoke the same ClientInstall entry
  // its GUI button uses through a small helper compiled into
  // pixlauncher/runtime/tools-classes (source: launcher/tools/).
  onProgress('Installing Forge client (headless helper)…');
  const helperClasses = path.join(runtimeDir(), 'tools-classes');
  const helperArgs = [
    '-Djava.net.preferIPv4Stack=true',
    '-cp', `${installerPath}${path.delimiter}${helperClasses}`,
    'top.pixlauncher.tools.ForgeInstallHelper',
    gameDir()
  ];
  const exitCode = await new Promise<number>((resolve, reject) => {
    const proc = spawn(javaExePath, helperArgs, { cwd: runtimeDir(), stdio: ['ignore', 'pipe', 'pipe'] });
    let out = '';
    proc.stdout.on('data', (d: Buffer) => { out += d.toString(); });
    proc.stderr.on('data', (d: Buffer) => { out += d.toString(); });
    proc.on('exit', (code) => {
      log.info(`forge helper exit ${code}: ${out.slice(-2000)}`);
      resolve(code ?? -1);
    });
    proc.on('error', reject);
  });
  if (exitCode !== 0) throw new Error(`Forge client install failed (exit ${exitCode}), see log`);

  const installed = fs.readdirSync(versionsDir).find((d) => d.includes('forge') && d.startsWith('1.8.9'));
  if (!installed) throw new Error('Forge installer finished but no version folder found');
  await ensureForgeLibraries(installed, onProgress);
  onProgress(`Forge ready (${installed})`);
  return installed;
}
