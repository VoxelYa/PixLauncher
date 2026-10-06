import { assertPublicHttps, USER_AGENT } from '../net';

/**
 * Microsoft OAuth + Xbox/XSTS + Minecraft services chain — pure Node,
 * no Electron, shared by the GUI login and headless test scripts.
 * Public client: PKCE only, no secret. Client ID is public information.
 */

/**
 * Product client ID (PixLauncher's own Azure app). Interim testing override:
 * PIXLAUNCHER_CLIENT_ID env var can point at an allow-listed app — Mojang
 * gates login_with_xbox behind an AppID review (weekly); our own ID works
 * once approved. Never ship a borrowed ID.
 */
// PLACEHOLDER — replace with your own Azure app's Client ID (Mojang AppID
// Review approved). Local testing can override via PIXLAUNCHER_CLIENT_ID.
export const CLIENT_ID = process.env.PIXLAUNCHER_CLIENT_ID ?? 'YOUR-MICROSOFT-CLIENT-ID';
export const TENANT = process.env.PIXLAUNCHER_TENANT ?? 'consumers'; // personal Microsoft accounts (game accounts)
export const SCOPES = 'offline_access XboxLive.signin';
export const AUTHORIZE = `https://login.microsoftonline.com/${TENANT}/oauth2/v2.0/authorize`;
export const TOKEN = `https://login.microsoftonline.com/${TENANT}/oauth2/v2.0/token`;
export const DEVICE_CODE = `https://login.microsoftonline.com/${TENANT}/oauth2/v2.0/devicecode`;

const XBL_AUTH = 'https://user.auth.xboxlive.com/user/authenticate';
const XSTS_AUTH = 'https://xsts.auth.xboxlive.com/xsts/authorize';
const MC_LOGIN = 'https://api.minecraftservices.com/authentication/login_with_xbox';
const MC_PROFILE = 'https://api.minecraftservices.com/minecraft/profile';

/** The only hosts the auth chain may ever talk to (pinned, no user input). */
const ALLOWED_AUTH_HOSTS = new Set([
  'login.microsoftonline.com',
  'login.live.com',
  'user.auth.xboxlive.com',
  'xsts.auth.xboxlive.com',
  'api.minecraftservices.com'
]);

// ------------------------------------------------------- Live SDK flow
// The classic login.live.com flow used by the official 1.8.9-era launchers.
// `oauth20_desktop.srf` is auto-registered for every Live SDK application.
const LIVE_CLIENT_ID = '00000000402b5328';
const LIVE_AUTHORIZE = 'https://login.live.com/oauth20_authorize.srf';
const LIVE_TOKEN = 'https://login.live.com/oauth20_token.srf';
const LIVE_REDIRECT = 'https://login.live.com/oauth20_desktop.srf';

function assertAuthHost(url: string): URL {
  const u = assertPublicHttps(url);
  if (!ALLOWED_AUTH_HOSTS.has(u.hostname)) {
    throw new Error('Auth host not in allowlist: ' + u.hostname);
  }
  return u;
}

export interface McIdentity {
  mcAccessToken: string;
  mcTokenExpiresAt: number;
  refreshToken: string;
  profileId: string;
  profileName: string;
  /** which token endpoint issued the refresh token (drives refresh path) */
  authProvider?: 'live' | 'ms';
}

export function b64url(buf: Buffer): string {
  return buf.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function pkcePair(): { verifier: string; challenge: string; state: string } {
  const crypto = require('crypto') as typeof import('crypto');
  const verifier = b64url(crypto.randomBytes(48));
  return {
    verifier,
    challenge: b64url(crypto.createHash('sha256').update(verifier).digest()),
    state: b64url(crypto.randomBytes(16))
  };
}

export function buildAuthorizeUrl(redirectUri: string, verifier: string, state: string): string {
  const crypto = require('crypto') as typeof import('crypto');
  const challenge = b64url(crypto.createHash('sha256').update(verifier).digest());
  return (
    `${AUTHORIZE}?client_id=${CLIENT_ID}` +
    `&response_type=code&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&response_mode=query&scope=${encodeURIComponent(SCOPES)}` +
    `&state=${state}&code_challenge=${challenge}&code_challenge_method=S256` +
    `&prompt=select_account`
  );
}

const TOKEN_URL = `https://login.microsoftonline.com/${TENANT}/oauth2/v2.0/token`;
const DEVICECODE_URL = `https://login.microsoftonline.com/${TENANT}/oauth2/v2.0/devicecode`;

async function postTokenForm(form: Record<string, string>): Promise<Record<string, unknown>> {
  assertAuthHost(TOKEN_URL);
  const res = await fetch(TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': USER_AGENT },
    body: new URLSearchParams(form).toString()
  });
  const json = (await res.json()) as Record<string, unknown>;
  if (!res.ok) throw new Error(`token endpoint ${res.status}: ${JSON.stringify(json)}`);
  return json;
}

async function postDeviceCodeForm(form: Record<string, string>): Promise<Record<string, unknown>> {
  assertAuthHost(DEVICECODE_URL);
  const res = await fetch(DEVICECODE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': USER_AGENT },
    body: new URLSearchParams(form).toString()
  });
  const json = (await res.json()) as Record<string, unknown>;
  if (!res.ok) throw new Error(`devicecode endpoint ${res.status}: ${JSON.stringify(json)}`);
  return json;
}

async function postJson(url: string, body: unknown, token?: string): Promise<Record<string, unknown>> {
  assertAuthHost(url);
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      'User-Agent': USER_AGENT,
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(body)
  });
  const json = (await res.json()) as Record<string, unknown>;
  if (!res.ok) throw new Error(`${url} -> ${res.status}: ${JSON.stringify(json)}`);
  return json;
}

/** code -> MS tokens (call from the loopback callback handler). */
export async function exchangeCode(code: string, redirectUri: string, verifier: string): Promise<{ accessToken: string; refreshToken: string }> {
  const res = await postTokenForm({
    client_id: CLIENT_ID,
    grant_type: 'authorization_code',
    code,
    redirect_uri: redirectUri,
    code_verifier: verifier,
    scope: SCOPES
  });
  return {
    accessToken: res.access_token as string,
    refreshToken: res.refresh_token as string
  };
}

/** refresh_token -> new MS tokens. */
export async function refreshTokens(refreshToken: string): Promise<{ accessToken: string; refreshToken: string }> {
  const res = await postTokenForm({
    client_id: CLIENT_ID,
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    scope: SCOPES
  });
  return {
    accessToken: res.access_token as string,
    refreshToken: res.refresh_token as string
  };
}

/** XBL -> XSTS -> Minecraft services -> profile (standard authlib-equivalent flow). */
export async function xboxToMcChain(msAccessToken: string): Promise<Omit<McIdentity, 'refreshToken'>> {
  const xbl = await postJson(XBL_AUTH, {
    Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: `d=${msAccessToken}` },
    RelyingParty: 'http://auth.xboxlive.com',
    TokenType: 'JWT'
  });
  const xblToken = xbl.Token as string;

  const xsts = await postJson(XSTS_AUTH, {
    Properties: { SandboxId: 'RETAIL', UserTokens: [xblToken] },
    RelyingParty: 'rp://api.minecraftservices.com/',
    TokenType: 'JWT'
  });
  const xstsToken = xsts.Token as string;
  const uhs = ((xsts.DisplayClaims as { xui: { uhs: string }[] }).xui ?? [])[0]?.uhs ?? '';

  const mc = await postJson(MC_LOGIN, { identityToken: `XBL3.0 x=${uhs};${xstsToken}` });
  const mcAccess = mc.access_token as string;
  const expiresSec = Number(mc.expires_in ?? 86400);

  assertAuthHost(MC_PROFILE);
  const profileRes = await fetch(MC_PROFILE, { headers: { Authorization: `Bearer ${mcAccess}`, 'User-Agent': USER_AGENT } });
  const profile = (await profileRes.json()) as Record<string, unknown>;
  if (!profileRes.ok || !profile.id) {
    throw new Error(`No Minecraft profile on this Microsoft account (${profileRes.status}).`);
  }
  return {
    mcAccessToken: mcAccess,
    mcTokenExpiresAt: Date.now() + expiresSec * 1000,
    profileId: profile.id as string,
    profileName: profile.name as string
  };
}

// ------------------------------------------------------- Live SDK flow
/** Browser URL for the classic Live desktop flow (user pastes back the final
 *  oauth20_desktop.srf URL, which contains ?code=...). */
export function buildLiveAuthorizeUrl(): string {
  return (
    `${LIVE_AUTHORIZE}?client_id=${LIVE_CLIENT_ID}` +
    `&response_type=code&redirect_uri=${encodeURIComponent(LIVE_REDIRECT)}` +
    `&scope=${encodeURIComponent('XboxLive.signin offline_access')}` +
    `&cobrandid=8058f65d-ce06-4c30-9559-473c9275a65d&prompt=select_account`
  );
}

export async function exchangeLiveCode(code: string): Promise<{ accessToken: string; refreshToken: string }> {
  assertAuthHost(LIVE_TOKEN);
  const res = await fetch(LIVE_TOKEN, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': USER_AGENT },
    body: new URLSearchParams({
      client_id: LIVE_CLIENT_ID,
      grant_type: 'authorization_code',
      code,
      redirect_uri: LIVE_REDIRECT
    }).toString()
  });
  const json = (await res.json()) as Record<string, unknown>;
  if (!res.ok) throw new Error(`live token ${res.status}: ${JSON.stringify(json)}`);
  return { accessToken: json.access_token as string, refreshToken: json.refresh_token as string };
}

export async function refreshLiveTokens(refreshToken: string): Promise<{ accessToken: string; refreshToken: string }> {
  assertAuthHost(LIVE_TOKEN);
  const res = await fetch(LIVE_TOKEN, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': USER_AGENT },
    body: new URLSearchParams({
      client_id: LIVE_CLIENT_ID,
      grant_type: 'refresh_token',
      refresh_token: refreshToken,
      redirect_uri: LIVE_REDIRECT
    }).toString()
  });
  const json = (await res.json()) as Record<string, unknown>;
  if (!res.ok) throw new Error(`live refresh ${res.status}: ${JSON.stringify(json)}`);
  return { accessToken: json.access_token as string, refreshToken: json.refresh_token as string };
}

/** Full chain from a fresh MS access token (after authorize or refresh). */
export async function identityFromMsToken(msAccessToken: string, refreshToken: string): Promise<McIdentity> {
  const partial = await xboxToMcChain(msAccessToken);
  return { ...partial, refreshToken };
}

// ------------------------------------------------------------- device code
export interface DeviceCodeStart {
  deviceCode: string;
  userCode: string;
  verificationUri: string;
  expiresInSec: number;
  intervalSec: number;
}

/** Step 1 of the device code flow: ask for a code. */
export async function startDeviceCode(): Promise<DeviceCodeStart> {
  const res = await postDeviceCodeForm({ client_id: CLIENT_ID, scope: SCOPES });
  return {
    deviceCode: res.device_code as string,
    userCode: res.user_code as string,
    verificationUri: res.verification_uri as string,
    expiresInSec: Number(res.expires_in ?? 900),
    intervalSec: Number(res.interval ?? 5)
  };
}

/**
 * Step 2: poll until the user finishes signing in.
 * Auto-fill trick the user asked for: open `https://microsoft.com/devicelogin?otc=<userCode>`
 * — the code is pre-filled on the page.
 */
export async function pollDeviceCode(
  start: DeviceCodeStart,
  onPending?: () => void
): Promise<{ accessToken: string; refreshToken: string }> {
  const deadline = Date.now() + start.expiresInSec * 1000;
  let interval = start.intervalSec * 1000;
  while (Date.now() < deadline) {
    await new Promise((r) => setTimeout(r, interval));
    try {
      const res = await postTokenForm({
        client_id: CLIENT_ID,
        grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
        device_code: start.deviceCode
      });
      return { accessToken: res.access_token as string, refreshToken: res.refresh_token as string };
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      if (msg.includes('authorization_pending')) {
        onPending?.();
        continue;
      }
      if (msg.includes('slow_down')) {
        interval += 5000;
        continue;
      }
      throw err; // expired_token, authorization_declined, etc.
    }
  }
  throw new Error('Device code expired before sign-in completed');
}
