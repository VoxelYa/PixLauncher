import * as http from 'http';
import { shell } from 'electron';
import { buildAuthorizeUrl, exchangeCode, identityFromMsToken } from './chain';
import { loadAccount, saveAccount, clearAccount, ensureFreshToken, McAccount } from './store';

/**
 * GUI Microsoft login (browser callback, user decision). Loopback server on a
 * random port; Azure public clients accept any port for the registered
 * `http://localhost` redirect URI. All heavy lifting lives in chain.ts/store.ts
 * so headless tests reuse the exact same code path.
 */

export async function loginInteractive(): Promise<McAccount> {
  const { verifier, challenge, state } = await import('./chain').then((m) => m.pkcePair());

  // The token request must repeat the exact redirect_uri used at authorize,
  // so capture the real port when the loopback server starts (never re-parse
  // it from the callback URL — a root-path callback loses the port).
  let redirectUri = '';
  const callbackUrl = await new Promise<string>((resolve, reject) => {
    const server = http.createServer((req, res) => {
      try {
        const u = new URL(req.url ?? '/', 'http://localhost');
        // ignore browser noise (favicon.ico etc.) — real callbacks carry code/error
        if (!u.searchParams.has('code') && !u.searchParams.has('error')) {
          res.writeHead(404).end();
          return;
        }
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(
          '<!doctype html><title>PixLauncher</title><body style="font-family:sans-serif;' +
          'background:#fafafa;color:#222;display:grid;place-items:center;height:100vh">' +
          '<div><h2>Login complete</h2><p>You can close this window and return to PixLauncher.</p></div></body>'
        );
        resolve(`http://localhost${req.url}`);
      } catch (err) {
        reject(err);
      } finally {
        server.close();
      }
    });
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const port = (server.address() as { port: number }).port;
      redirectUri = `http://localhost:${port}/`;
      const url = buildAuthorizeUrl(redirectUri, verifier, state);
      shell.openExternal(url);
      // keep the URL reachable for troubleshooting
      (server as unknown as { authUrl?: string }).authUrl = url;
    });
  });

  const params = new URL(callbackUrl);
  if (params.searchParams.get('state') !== state) throw new Error('OAuth state mismatch');
  const code = params.searchParams.get('code');
  if (!code) throw new Error(`OAuth denied: ${params.searchParams.get('error_description') ?? 'no code'}`);

  const tokens = await exchangeCode(code, redirectUri, verifier);
  const account = await identityFromMsToken(tokens.accessToken, tokens.refreshToken);
  saveAccount(account);
  return account;
}

export { ensureFreshToken, loadAccount as currentAccount, clearAccount as logout };
export type { McAccount };
