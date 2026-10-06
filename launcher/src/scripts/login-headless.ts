import * as http from 'http';
import { buildAuthorizeUrl, exchangeCode, identityFromMsToken, pkcePair } from '../main/auth/chain';
import { saveAccount, loadAccount } from '../main/auth/store';

/**
 * Headless login for self-testing: starts a loopback listener on a fixed port,
 * prints the Microsoft login URL, waits for the browser callback, completes
 * the token exchange and stores the account. Same chain as the GUI.
 *
 * Usage: node dist/scripts/login-headless.js [port]
 */

const port = Number(process.argv[2] ?? 8913);
const redirectUri = `http://localhost:${port}/`;

const { verifier, challenge, state } = pkcePair();
const authorizeUrl = buildAuthorizeUrl(redirectUri, verifier, state);

console.log('AUTH_URL=' + authorizeUrl);
console.log(`waiting for callback on ${redirectUri} … (timeout 10 min)`);

const server = http.createServer(async (req, res) => {
  try {
    const callback = new URL(req.url ?? '/', 'http://localhost');
    // ignore browser noise (favicon.ico etc.) — only real callbacks carry code/error
    if (!callback.searchParams.has('code') && !callback.searchParams.has('error')) {
      res.writeHead(404).end();
      return;
    }
    const html = (ok: boolean, msg: string) =>
      `<!doctype html><title>PixLauncher</title><body style="font-family:sans-serif;background:#fafafa;` +
      `color:#222;display:grid;place-items:center;height:100vh"><div><h2>${ok ? 'Login complete' : 'Login failed'}</h2>` +
      `<p>${msg}</p><p>You can close this window.</p></div></body>`;
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(html(true, 'Processing…'));

    if (callback.searchParams.get('state') !== state) throw new Error('state mismatch');
    const code = callback.searchParams.get('code');
    if (!code) throw new Error(callback.searchParams.get('error_description') ?? 'no code');

    const tokens = await exchangeCode(code, redirectUri, verifier);
    const account = await identityFromMsToken(tokens.accessToken, tokens.refreshToken);
    saveAccount(account);
    console.log('LOGIN_OK profile=' + account.profileName + ' id=' + account.profileId);
    console.log('existing store now: ' + (loadAccount()?.profileName ?? 'null'));
    server.close(() => process.exit(0));
  } catch (err) {
    console.error('LOGIN_FAILED ' + (err instanceof Error ? err.message : String(err)));
    server.close(() => process.exit(1));
  }
});

server.on('error', (err) => {
  console.error('SERVER_ERROR ' + err.message);
  process.exit(2);
});
server.listen(port, '127.0.0.1');

setTimeout(() => {
  console.error('LOGIN_TIMEOUT no callback in 10 minutes');
  process.exit(3);
}, 10 * 60 * 1000).unref();
