import * as fs from 'fs';
import * as path from 'path';
import { modsDir } from '../config';
import { fetchJson } from '../net';
import { pairs } from '../download/sources';
import { cachedWinner } from '../download/speedtest';
import { downloadFile, manifestDigest } from '../download/engine';

/**
 * Preinstalled mods (user decision): OptiFine + ReplayMod.
 *  - ReplayMod: Modrinth (verified: forge + 1.8.9 available).
 *  - OptiFine: NOT on Modrinth (verified 404) -> optifine.net official +
 *    BMCLAPI mirror (/optifine/<file>, verified), speed-tested like the game.
 */

const OPTIFINE_FILE = 'OptiFine_1.8.9_HD_U_M5.jar';

interface ModrinthVersion {
  id: string;
  name: string;
  files: { url: string; filename: string; primary: boolean; size?: number; hashes?: Record<string, string> }[];
}

async function installReplayMod(onProgress: (msg: string) => void): Promise<void> {
  fs.mkdirSync(modsDir(), { recursive: true });
  // only real jars count as installed; leftover .pixpart partials don't
  const existing = fs.existsSync(modsDir())
    ? fs.readdirSync(modsDir()).find((f) => f.toLowerCase().endsWith('.jar') && f.toLowerCase().includes('replaymod'))
    : undefined;
  if (existing) {
    onProgress(`ReplayMod already installed (${existing})`);
    return;
  }

  onProgress('Resolving ReplayMod on Modrinth…');
  const url =
    'https://api.modrinth.com/v2/project/replaymod/version' +
    `?game_versions=${encodeURIComponent('["1.8.9"]')}&loaders=${encodeURIComponent('["forge"]')}`;
  const versions = await fetchJson<ModrinthVersion[]>(url);
  if (!versions.length) throw new Error('No ReplayMod build for Forge 1.8.9 on Modrinth');
  const file = versions[0].files.find((f) => f.primary) ?? versions[0].files[0];

  onProgress(`Downloading ${file.filename}…`);
  await downloadFile(
    { url: file.url, dest: path.join(modsDir(), file.filename), digest: manifestDigest(file.hashes), size: file.size },
    'ReplayMod',
    (p) => onProgress(`ReplayMod ${p.current}/${p.total} bytes`)
  );
  onProgress('ReplayMod ready');
}

async function installOptifine(onProgress: (msg: string) => void): Promise<void> {
  fs.mkdirSync(modsDir(), { recursive: true });
  const dest = path.join(modsDir(), OPTIFINE_FILE);
  if (fs.existsSync(dest)) {
    onProgress(`OptiFine already installed (${OPTIFINE_FILE})`);
    return;
  }
  // clean up stale partials so "already installed" can't be spoofed
  for (const f of fs.existsSync(modsDir()) ? fs.readdirSync(modsDir()) : []) {
    if (f.startsWith('OptiFine') && f.endsWith('.pixpart')) fs.rmSync(path.join(modsDir(), f), { force: true });
  }

  onProgress('Downloading OptiFine (official vs mirror speed test)…');
  const pair = pairs.optifine(OPTIFINE_FILE);
  const winner = await cachedWinner('optifine', pair.official, pair.mirror);
  await downloadFile(
    { url: winner === 'mirror' ? pair.mirror : pair.official, fallbackUrl: winner === 'mirror' ? pair.official : pair.mirror, dest },
    'OptiFine',
    (p) => onProgress(`OptiFine ${p.current}/${p.total} bytes`)
  );
  onProgress('OptiFine ready');
}

export async function installPreinstalledMods(onProgress: (msg: string) => void): Promise<void> {
  await installOptifine(onProgress);
  await installReplayMod(onProgress);
}
