import { app, BrowserWindow, ipcMain, shell } from 'electron';
import * as path from 'path';
import { initLogger, log } from './logger';
import { loadSettings, saveSettings, recommendedMemoryMB, gameDir, dataDir } from './config';
import * as msauth from './auth/msauth';
import { ensureJava } from './game/java';
import { installVanilla } from './game/vanilla';
import { installForge } from './game/forge';
import { installPreinstalledMods } from './game/mods';
import * as modrinth from './modrinth';
import { launchGame, killGame, isRunning, setProcessStarter, gameExit, GameCommand } from './game/launch';

let win: BrowserWindow | null = null;

function createWindow(): void {
  win = new BrowserWindow({
    width: 1080,
    height: 680,
    minWidth: 900,
    minHeight: 600,
    backgroundColor: '#f7f8fa', // light/clean style (user decision)
    autoHideMenuBar: true,
    title: 'PixLauncher',
    webPreferences: {
      preload: path.join(__dirname, '..', 'preload', 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });
  win.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));
  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });
}

function send(channel: string, payload: unknown): void {
  win?.webContents.send(channel, payload);
}

function wireIpc(): void {
  ipcMain.handle('state:get', () => ({
    account: msauth.currentAccount()?.profileName ?? null,
    running: isRunning(),
    memoryAutoMB: recommendedMemoryMB(),
    settings: loadSettings(),
    gameDir: gameDir(),
    dataDir: dataDir()
  }));

  ipcMain.handle('auth:login', async () => {
    const acc = await msauth.loginInteractive();
    send('state:changed', null);
    return { profileName: acc.profileName, profileId: acc.profileId };
  });

  ipcMain.handle('auth:logout', () => {
    msauth.logout();
    send('state:changed', null);
  });

  ipcMain.handle('settings:set', (_e, patch: Record<string, unknown>) => {
    const s = { ...loadSettings(), ...patch };
    saveSettings(s);
    send('state:changed', null);
    return s;
  });

  /**
   * Full first-run pipeline (user requirement: downloads happen first):
   * JRE8 -> vanilla 1.8.9 -> Forge -> OptiFine + ReplayMod.
   */
  ipcMain.handle('setup:run', async () => {
    const step = (msg: string) => send('setup:progress', { message: msg });
    try {
      const java = await ensureJava(step);
      step('Java 8 ready');
      await installVanilla(step);
      const forgeId = await installForge(java, step);
      await installPreinstalledMods(step);
      send('setup:done', { forgeId });
      return { ok: true };
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      log.error('setup failed', err);
      send('setup:error', { message: msg });
      return { ok: false, error: msg };
    }
  });

  ipcMain.handle('game:launch', async () => {
    try {
      await launchGame();
      return { ok: true };
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      log.error('launch failed', err);
      return { ok: false, error: msg };
    }
  });

  ipcMain.handle('game:kill', () => {
    killGame();
  });

  ipcMain.handle('mods:search', (_e, query: string) => modrinth.searchMods(query));

  ipcMain.handle('mods:install', async (_e, projectId: string) => {
    try {
      const file = await modrinth.installMod(projectId, (m) => send('mods:progress', { message: m }));
      return { ok: true, file };
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      log.error('mod install failed: ' + msg);
      return { ok: false, error: msg };
    }
  });

  ipcMain.handle('mods:list', () => modrinth.listInstalled());
}

/**
 * Real process starter (argv-array spawn, piped stdio — same audited pattern
 * as forge.ts; no shell, no command-line string). Wired here so game/launch.ts
 * stays free of process creation.
 */
function wireProcessStarter(): void {
  const { spawn } = require('child_process') as typeof import('child_process');
  setProcessStarter((cmd: GameCommand) => {
    const child = spawn(cmd.javaExe, cmd.argv, { cwd: cmd.cwd, stdio: ['ignore', 'pipe', 'pipe'] });
    child.stdout?.on('data', (d: Buffer) => d.toString().split(/\r?\n/).forEach((l) => l && log.info('[game] ' + l)));
    child.stderr?.on('data', (d: Buffer) => d.toString().split(/\r?\n/).forEach((l) => l && log.warn('[game] ' + l)));
    return {
      kill: () => child.kill(),
      onExit: (cb) => child.on('exit', (code) => cb(code)),
      pipeOutput: () => { /* output already piped to the launcher log */ }
    };
  });
}

// Single instance: a second launch (double-click, racing CI) must quit
// immediately instead of racing the first one's game state.
if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (win) {
      if (win.isMinimized()) win.restore();
      win.focus();
    }
  });
}

app.whenReady().then(() => {
  initLogger(path.join(dataDir(), 'logs'));
  log.info('PixLauncher starting (dev=' + !app.isPackaged + ')');
  wireIpc();
  try {
    wireProcessStarter();
  } catch (err) {
    log.warn('process starter not wired: ' + (err instanceof Error ? err.message : err));
  }
  if (process.argv.includes('--autotest')) {
    void autotest();
    return;
  }
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

if (!process.argv.includes('--autotest')) {
  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
  });
}

/**
 * Headless self-test mode (electron . --autotest): runs the full pipeline —
 * Java 8, vanilla, Forge, OptiFine + ReplayMod, then launches the game with
 * the stored account — logging every step, then quits. No window is shown.
 * Uses the same primitives as the IPC handlers; the spawn itself is the
 * starter wired above.
 */
async function autotest(): Promise<void> {
  const step = (label: string) => (msg: string) => log.info(`[autotest:${label}] ${msg}`);
  try {
    const java = await ensureJava(step('java'));
    await installVanilla(step('vanilla'));
    await installForge(java, step('forge'));
    await installPreinstalledMods(step('mods'));
    await launchGame();
    if (process.env.PIXLAUNCHER_AGENT) {
      // performance-benchmark mode: the javaagent runs its 7-zone sweep and
      // exits the game itself; wait it out instead of killing after 90s.
      log.info('[autotest] agent mode: waiting for game exit (max 12 min)…');
      const timeout = new Promise<number>((r) => setTimeout(() => r(-2), 12 * 60_000));
      const code = await Promise.race([gameExit(), timeout]);
      log.info('[autotest] game exit code=' + code);
      log.info('[autotest] AUTOTEST_DONE');
      app.exit(0);
      return;
    }
    log.info('[autotest] game started; waiting 90s for boot milestones…');
    await new Promise((r) => setTimeout(r, 90_000));
    killGame();
    log.info('[autotest] AUTOTEST_DONE');
    app.exit(0);
  } catch (err) {
    log.error('[autotest] AUTOTEST_FAILED', err);
    app.exit(1);
  }
}
