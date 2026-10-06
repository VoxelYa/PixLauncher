import { initLogger } from '../main/logger';
import { buildGameCommand } from '../main/game/launch';

/**
 * Headless launch-command verification: assembles the exact game command
 * (classpath, JVM args, tweak chain, token) without starting it. The actual
 * process start runs through the Electron main (--autotest) where the
 * starter is wired.
 *
 * Usage: node dist/scripts/launch-headless.js
 */

async function run(): Promise<void> {
  initLogger(require('../main/config').logsDir());
  // runtime-generated throwaway identity — never a real credential; the token
  // only lands in argv so assembly is verifiable before Mojang approval
  const crypto = require('crypto') as typeof import('crypto');
  const cmd = await buildGameCommand({
    mcAccessToken: 'TEST.' + crypto.randomBytes(24).toString('hex'),
    mcTokenExpiresAt: Date.now() + 3600_000,
    refreshToken: 'TEST.' + crypto.randomBytes(24).toString('hex'),
    profileId: '00000000-0000-0000-0000-000000000000',
    profileName: 'PixLauncherDev'
  });
  console.log('[cmd] java: ' + cmd.javaExe);
  console.log('[cmd] cwd:  ' + cmd.cwd);
  console.log('[cmd] argv:');
  for (const a of cmd.argv) {
    console.log('  ' + (a.length > 200 ? a.slice(0, 200) + '…(' + a.length + ' chars)' : a));
  }
  console.log('CMD_OK');
}

run().catch((err) => {
  console.error('CMD_FAILED ' + (err instanceof Error ? (err.stack ?? err.message) : String(err)));
  process.exit(1);
});
