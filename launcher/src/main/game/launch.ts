import * as fs from 'fs';
import * as path from 'path';
import { gameDir, loadSettings, recommendedMemoryMB } from '../config';
import { ensureFreshToken } from '../auth/store';
import { McIdentity } from '../auth/chain';
import { ruleAllowsOs, VersionJson } from './vanilla';
import { log } from '../logger';

/**
 * Assembles the game launch command (JRE 8 -> Forge 1.8.9 -> launchwrapper).
 * The actual process creation (argv-array spawn, piped stdio, no shell) is
 * wired in main/index.ts via setProcessStarter(), keeping this module free of
 * child_process.
 *
 * Lunar-style client injection: our PixBootstrap tweak class is prepended to
 * the tweak chain (before FMLTweaker) whenever the pixclient pack exists —
 * never via the mods folder.
 */

export interface GameCommand {
  javaExe: string;
  argv: string[];
  cwd: string;
}

export type ProcessStarter = (cmd: GameCommand) => {
  kill: () => void;
  onExit: (cb: (code: number | null) => void) => void;
  pipeOutput: (line: (s: string) => void) => void;
};

const CLIENT_PACK = path.join(gameDir(), 'pixclient');

let running: { kill: () => void } | null = null;
let starterRef: ProcessStarter | null = null;

export function isRunning(): boolean {
  return running !== null;
}

export function killGame(): void {
  running?.kill();
  running = null;
}

export function setProcessStarter(starter: ProcessStarter | null): void {
  starterRef = starter;
}

/**
 * Version/coordinate strings arrive from network-provided version JSON, so
 * every path segment is whitelisted before it ever touches path.join.
 */
function safeSegment(s: string, what: string): string {
  if (!/^[\w][\w.\-]*$/.test(s)) throw new Error(`Unsafe ${what}: ${s}`);
  return s;
}

function safeMavenRel(mavenName: string): string {
  const [group, artifact, version, ...rest] = mavenName.split(':');
  if (!group || !artifact || !version || rest.length > 1) {
    throw new Error('Bad maven coordinate: ' + mavenName);
  }
  const classifier = rest[0] ? '-' + safeSegment(rest[0], 'classifier') : '';
  const fileName =
    safeSegment(artifact, 'artifact') + '-' + safeSegment(version, 'version') + classifier + '.jar';
  return path.join(...group.split('.').map((g) => safeSegment(g, 'group')), safeSegment(artifact, 'artifact'), safeSegment(version, 'version'), fileName);
}

export async function buildGameCommand(identity?: McIdentity): Promise<GameCommand> {
  // Offline test identity (env-provided, runtime-generated) lets CI/dev boot
  // the game to the title screen without a Microsoft login. Never set in prod.
  const testRaw = process.env.PIXLAUNCHER_TEST_IDENTITY;
  const account: McIdentity | null = testRaw
    ? (JSON.parse(Buffer.from(testRaw, 'base64').toString('utf8')) as McIdentity)
    : await ensureFreshToken();
  if (!account || !account.mcAccessToken) throw new Error('Not logged in');

  const versionsDir = path.join(gameDir(), 'versions');
  const found = fs.readdirSync(versionsDir).find((d) => d.includes('forge') && d.startsWith('1.8.9'));
  if (!found) throw new Error('Forge is not installed yet');
  const forgeId = safeSegment(found, 'forge version id');
  const forgeSpec = JSON.parse(fs.readFileSync(path.join(versionsDir, forgeId, forgeId + '.json'), 'utf8')) as VersionJson & {
    inheritsFrom?: string;
    libraries: { name: string; url?: string; rules?: { action: string; os?: { name?: string } }[] }[];
  };

  // ---- classpath (all entries read from disk, nothing fetched here)
  const cp: string[] = [];
  const libRoot = path.join(gameDir(), 'libraries');
  const addJarIfExists = (rel: string, what: string, fatal: boolean) => {
    // rel is built from whitelisted segments only; keep a root-boundary check
    const abs = path.resolve(libRoot, rel);
    if (!abs.startsWith(libRoot + path.sep)) throw new Error('Library escapes library dir: ' + what);
    if (fs.existsSync(abs)) {
      cp.push(abs);
      return true;
    }
    if (fatal) throw new Error('Missing library: ' + what);
    log.warn('skipping missing ' + what);
    return false;
  };
  for (const lib of forgeSpec.libraries) {
    if (!ruleAllowsOs(lib.rules)) continue;
    addJarIfExists(safeMavenRel(lib.name), lib.name, false);
  }
  const vanillaId = safeSegment(forgeSpec.inheritsFrom ?? '1.8.9', 'vanilla version id');
  const vanillaJson = JSON.parse(
    fs.readFileSync(path.join(versionsDir, vanillaId, vanillaId + '.json'), 'utf8')
  ) as VersionJson;
  for (const lib of vanillaJson.libraries) {
    if (!ruleAllowsOs(lib.rules)) continue;
    const rel = lib.downloads?.artifact?.path ?? safeMavenRel(lib.name);
    if (rel.split(/[\\/]/).some((seg) => !/^[\w.\-]+$/.test(seg))) {
      throw new Error('Unsafe artifact path: ' + rel);
    }
    addJarIfExists(rel, lib.name, false);
  }
  // vanilla jar: both path segments are whitelisted ids
  cp.push(path.resolve(versionsDir, vanillaId, vanillaId + '.jar'));

  // pixclient (our mixin pack + ASM 6) goes FIRST on the classpath so its ASM
  // is the one the parent loader sees — Lunar-style packs ship their own asm.
  if (fs.existsSync(CLIENT_PACK)) {
    for (const f of fs.readdirSync(CLIENT_PACK)) {
      if (f.endsWith('.jar') && /^[\w.\-]+\.jar$/.test(f)) cp.unshift(path.join(CLIENT_PACK, f));
    }
  }

  // Client pack jars were placed first above; here we only add the tweak that
  // wires the Lunar-style injection into the launch chain.
  const tweakArgs: string[] = [];
  if (fs.existsSync(CLIENT_PACK)) {
    tweakArgs.push('--tweakClass', 'top.pixlauncher.bootstrap.PixBootstrap');
  }

  const settings = loadSettings();
  const xmx = settings.memoryMB ?? recommendedMemoryMB();

  const nativesDir = path.join(gameDir(), 'natives', vanillaId);
  fs.mkdirSync(nativesDir, { recursive: true });

  const javaExe = path.join(gameDir(), '..', 'pixlauncher', 'runtime', 'jre8', 'bin', 'java.exe');
  if (!fs.existsSync(javaExe)) throw new Error('Java 8 runtime missing');

  const argv: string[] = [
    '-Xmx' + xmx + 'M',
    '-Dfile.encoding=UTF-8',
    '-Djava.library.path=' + nativesDir,
    '-cp',
    cp.join(path.delimiter),
    'net.minecraft.launchwrapper.Launch',
    ...tweakArgs,
    '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker',
    '--username', account.profileName,
    '--uuid', account.profileId,
    '--accessToken', account.mcAccessToken,
    '--version', forgeId,
    '--gameDir', gameDir(),
    '--assetsDir', path.join(gameDir(), 'assets'),
    '--assetIndex', '1.8'
  ];

  // performance-test agent hook (dev/benchmark only): PIXLAUNCHER_AGENT=path
  const agentPath = process.env.PIXLAUNCHER_AGENT;
  if (agentPath) argv.splice(1, 0, '-javaagent:' + agentPath);

  log.info('game command assembled: Xmx=' + xmx + 'M cp=' + cp.length + ' jars clientPack=' + (tweakArgs.length > 0));
  return { javaExe, argv, cwd: gameDir() };
}

export async function launchGame(): Promise<void> {
  log.info('launchGame entered, running=' + (running !== null) + ', starter=' + (starterRef !== null));
  if (running) throw new Error('Game is already running');
  if (!starterRef) throw new Error('Process starter not wired (see docs/PLAN.md §6)');
  const cmd = await buildGameCommand();
  const handle = starterRef(cmd);
  running = { kill: handle.kill };
  handle.onExit((code) => {
    log.info('game exited with ' + code);
    running = null;
    const w = exitWatcher;
    exitWatcher = null;
    if (w) w(code ?? -1);
  });
}

/** resolves when the current game process exits (immediate -1 if none running) */
let exitWatcher: ((code: number) => void) | null = null;

export function gameExit(): Promise<number> {
  return new Promise<number>((resolve) => {
    if (!running) { resolve(-1); return; }
    exitWatcher = resolve;
  });
}

export type { McIdentity as McAccount };
