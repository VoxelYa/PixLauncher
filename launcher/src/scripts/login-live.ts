import * as fs from 'fs';
import * as path from 'path';
import { buildLiveAuthorizeUrl, exchangeLiveCode, identityFromMsToken } from '../main/auth/chain';
import { saveAccount } from '../main/auth/store';

/**
 * Classic Live SDK login: prints the authorize URL, the operator pastes the
 * final oauth20_desktop.srf URL into a file, this script picks it up, extracts
 * the code and finishes the Xbox/Minecraft chain. Same final chain as the GUI.
 *
 * Usage: node dist/scripts/login-live.js
 */

function pasteFile(): string {
  return path.join(process.env.PIXLAUNCHER_ROOT ?? process.cwd(), 'pixlauncher', 'live-paste.txt');
}

async function run(): Promise<void> {
  const authorizeUrl = buildLiveAuthorizeUrl();
  console.log('LIVE_URL=' + authorizeUrl);
  console.log('Waiting for the pasted final URL in: ' + pasteFile());

  const deadline = Date.now() + 10 * 60_000;
  let pasted = '';
  while (Date.now() < deadline) {
    await new Promise((r) => setTimeout(r, 1000));
    try {
      if (fs.existsSync(pasteFile())) {
        pasted = fs.readFileSync(pasteFile(), 'utf8').trim();
        if (pasted) break;
      }
    } catch { /* retry */ }
  }
  if (!pasted) throw new Error('timeout waiting for pasted URL');

  const finalUrl = new URL(pasted);
  const code = finalUrl.searchParams.get('code');
  if (!code) throw new Error('no code in pasted URL: ' + pasted);

  console.log('LIVE_CODE_OK — exchanging…');
  const tokens = await exchangeLiveCode(code);
  console.log('MS_OAUTH_OK — Xbox/Minecraft services…');
  const account = await identityFromMsToken(tokens.accessToken, tokens.refreshToken);
  account.authProvider = 'live';
  saveAccount(account);
  console.log('LOGIN_OK profile=' + account.profileName + ' id=' + account.profileId);
  process.exit(0);
}

run().catch((err) => {
  console.error('LOGIN_FAILED ' + (err instanceof Error ? (err.stack ?? err.message) : String(err)));
  process.exit(1);
});
