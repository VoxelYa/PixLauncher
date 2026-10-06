import { startDeviceCode, pollDeviceCode, identityFromMsToken } from '../main/auth/chain';
import { saveAccount } from '../main/auth/store';

/**
 * Headless DEVICE CODE login (user-requested flow): prints a URL with the
 * ?otc= parameter so the code is pre-filled on the Microsoft page; polls until
 * the user finishes; then runs the XBL/XSTS/Minecraft chain and stores the
 * account. Same final chain as the GUI.
 *
 * Usage: node dist/scripts/login-device.js
 */

async function run(): Promise<void> {
  const start = await startDeviceCode();
  const autoFillUrl = `https://microsoft.com/devicelogin?otc=${encodeURIComponent(start.userCode)}`;
  console.log('DEVICE_CODE=' + start.userCode);
  console.log('DEVICE_URL=' + autoFillUrl);
  console.log('(open the URL — the code is pre-filled; sign in with the Microsoft account that owns Minecraft)');

  const tokens = await pollDeviceCode(start);
  console.log('MS_OAUTH_OK — exchanging via Xbox/Minecraft services…');
  try {
    const account = await identityFromMsToken(tokens.accessToken, tokens.refreshToken);
    saveAccount(account);
    console.log('LOGIN_OK profile=' + account.profileName + ' id=' + account.profileId);
    process.exit(0);
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    if (msg.includes('Invalid app registration')) {
      console.error('LOGIN_BLOCKED_MOJANG_APPROVAL ' + msg);
      console.error('(OAuth itself succeeded; the client ID needs Mojang AppID review approval)');
    } else {
      console.error('LOGIN_FAILED ' + msg);
    }
    process.exit(1);
  }
}

run().catch((err) => {
  console.error('LOGIN_FAILED ' + (err instanceof Error ? (err.stack ?? err.message) : String(err)));
  process.exit(1);
});
