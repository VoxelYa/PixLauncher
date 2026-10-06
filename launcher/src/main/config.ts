import * as fs from 'fs';
import * as path from 'path';

/**
 * Portable layout: everything lives next to the exe (user decision).
 *   <exe dir>/.minecraft          game directory (isolated from system .minecraft)
 *   <exe dir>/pixlauncher/        launcher data (settings, accounts, logs, runtime)
 * Electron-free so headless test scripts can reuse everything:
 * override the root with PIXLAUNCHER_ROOT, otherwise dev = launcher/ dir.
 */

function electronApp(): { isPackaged: boolean; getPath: (k: string) => string } | null {
  try {
    const e = require('electron');
    return e && typeof e === 'object' && e.app ? e.app : null;
  } catch {
    return null;
  }
}

export function appRoot(): string {
  const env = process.env.PIXLAUNCHER_ROOT;
  if (env) return env;
  const app = electronApp();
  if (app && app.isPackaged) return path.dirname(app.getPath('exe'));
  return path.join(__dirname, '..', '..'); // launcher/ during dev (dist/main -> up two)
}

export function gameDir(): string {
  return path.join(appRoot(), '.minecraft');
}

export function dataDir(): string {
  return path.join(appRoot(), 'pixlauncher');
}

export function logsDir(): string {
  return path.join(dataDir(), 'logs');
}

export function runtimeDir(): string {
  return path.join(dataDir(), 'runtime'); // JRE8, cached installers
}

export function modsDir(): string {
  return path.join(gameDir(), 'mods');
}

export function clientPackDir(): string {
  return path.join(gameDir(), 'pixclient');
}

export interface Settings {
  memoryMB: number | null;        // null = auto-recommend from physical RAM
  mirrorPreference: 'auto' | 'official' | 'mirror';
  speedTest: Record<string, { winner: string; testedAt: number }>;
  keepLauncherOpen: boolean;
}

const DEFAULTS: Settings = {
  memoryMB: null,
  mirrorPreference: 'auto',
  speedTest: {},
  keepLauncherOpen: true
};

function settingsPath(): string {
  return path.join(dataDir(), 'settings.json');
}

export function loadSettings(): Settings {
  try {
    const raw = JSON.parse(fs.readFileSync(settingsPath(), 'utf8'));
    return { ...DEFAULTS, ...raw };
  } catch {
    return { ...DEFAULTS };
  }
}

export function saveSettings(s: Settings): void {
  fs.mkdirSync(dataDir(), { recursive: true });
  fs.writeFileSync(settingsPath(), JSON.stringify(s, null, 2), 'utf8');
}

/** Auto memory: ~1/4 of physical RAM clamped to [2, 8] GB (user decision: auto-recommend). */
export function recommendedMemoryMB(): number {
  const os = require('os') as typeof import('os');
  const totalKB = os.totalmem() / 1024;
  const mb = Math.round(totalKB / 1024 / 4 / 512) * 512;
  return Math.min(8192, Math.max(2048, mb));
}
