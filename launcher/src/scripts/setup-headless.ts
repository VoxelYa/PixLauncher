import { initLogger } from '../main/logger';
import { ensureJava } from '../main/game/java';
import { installVanilla } from '../main/game/vanilla';
import { installForge } from '../main/game/forge';
import { installPreinstalledMods } from '../main/game/mods';

/**
 * Headless first-run pipeline test: JRE8 -> vanilla 1.8.9 -> Forge ->
 * OptiFine + ReplayMod. Real downloads, real speed tests.
 * Usage: node dist/scripts/setup-headless.js
 */

const step = (label: string) => (msg: string) => console.log(`[${label}] ${msg}`);

async function run(): Promise<void> {
  const java = await ensureJava(step('java'));
  console.log('[java] exe: ' + java);
  await installVanilla(step('vanilla'));
  const forgeId = await installForge(java, step('forge'));
  console.log('[forge] installed: ' + forgeId);
  await installPreinstalledMods(step('mods'));
  console.log('SETUP_OK');
}

initLogger(require('../main/config').logsDir());

run().catch((err) => {
  console.error('SETUP_FAILED ' + (err instanceof Error ? (err.stack ?? err.message) : String(err)));
  process.exit(1);
});
