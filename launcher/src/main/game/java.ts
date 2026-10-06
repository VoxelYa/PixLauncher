import * as fs from 'fs';
import * as path from 'path';
import { execFileSync } from 'child_process';
import { runtimeDir, dataDir } from '../config';
import { fetchJson } from '../net';
import { downloadFile } from '../download/engine';
import { log } from '../logger';

/**
 * Bundled Temurin JRE 8 (user decision: auto-download; 1.8.9 requires Java 8).
 * Sources: Adoptium API (official) + Tsinghua TUNA mirror, speed choice kept simple:
 * official API gives metadata+hash; the big zip comes from whichever host responds.
 */

const ADOPTIUM_API =
  'https://api.adoptium.net/v3/assets/latest/8/hotspot?architecture=x64&image_type=jre&os=windows';
const TUNA_LIST = 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/8/jre/x64/windows/';

interface AdoptiumAsset {
  binary: {
    package: { name: string; link: string; sha256: string; size: number };
  };
  release_name: string;
}

function jreRoot(): string {
  return path.join(runtimeDir(), 'jre8');
}

export function javaExe(): string | null {
  const exe = path.join(jreRoot(), 'bin', 'java.exe');
  return fs.existsSync(exe) ? exe : null;
}

export async function ensureJava(onProgress?: (msg: string) => void): Promise<string> {
  const existing = javaExe();
  if (existing) {
    onProgress?.('Java 8 runtime: found');
    return existing;
  }
  onProgress?.('Fetching Java 8 (Temurin JRE) metadata…');
  const assets = await fetchJson<AdoptiumAsset[]>(ADOPTIUM_API);
  const pkg = assets[0]?.binary?.package;
  if (!pkg) throw new Error('Adoptium API returned no JRE 8 package');

  // Official link is api.adoptium.net/github-release redirect; TUNA mirrors the
  // same file by name under /Adoptium/8/jre/x64/windows/OpenJDK8U-jre_x64_windows_hotspot_<v>.zip
  const tunaUrl = `${TUNA_LIST}${pkg.name}`;
  const zipPath = path.join(runtimeDir(), pkg.name);

  onProgress?.(`Downloading ${pkg.name} (~${Math.round(pkg.size / 1048576)} MB)…`);
  try {
    await downloadFile(
      { url: tunaUrl, fallbackUrl: pkg.link, dest: zipPath },
      'Java 8 runtime'
    );
  } catch (err) {
    log.warn(`both Java sources failed, retrying official only: ${err}`);
    await downloadFile({ url: pkg.link, dest: zipPath }, 'Java 8 runtime');
  }

  onProgress?.('Extracting Java 8…');
  const extractTo = path.join(runtimeDir(), 'jre8.tmp');
  fs.rmSync(extractTo, { recursive: true, force: true });
  fs.mkdirSync(extractTo, { recursive: true });
  // JDK zips contain a single top-level dir; flatten it.
  execFileSync('powershell', [
    '-NoProfile', '-Command',
    `Expand-Archive -LiteralPath '${zipPath}' -DestinationPath '${extractTo}' -Force`
  ], { stdio: 'ignore' });
  const inner = fs.readdirSync(extractTo)[0];
  fs.rmSync(jreRoot(), { recursive: true, force: true });
  fs.renameSync(path.join(extractTo, inner), jreRoot());
  fs.rmSync(extractTo, { recursive: true, force: true });
  fs.rmSync(zipPath, { force: true });

  const exe = javaExe();
  if (!exe) throw new Error('JRE 8 extraction failed (bin/java.exe missing)');
  onProgress?.('Java 8 runtime ready');
  return exe;
}

export function logDir(): string {
  return dataDir();
}
