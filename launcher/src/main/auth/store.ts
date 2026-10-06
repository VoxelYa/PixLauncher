import * as fs from 'fs';
import * as path from 'path';
import { dataDir } from '../config';
import { log } from '../logger';
import { identityFromMsToken, refreshTokens, McIdentity } from './chain';
import * as vault from './vault';

/**
 * Account storage shared by GUI and headless modes. The refresh token is kept
 * only as layered ciphertext on disk (see vault.ts); the plaintext exists just
 * long enough to refresh, in memory.
 */

export type McAccount = McIdentity;

function file(): string {
  return path.join(dataDir(), 'accounts.bin');
}

interface StoredShape {
  enc: string; // base64 vault ciphertext of the refresh token
  profileId: string;
  profileName: string;
}

export function loadAccount(): McAccount | null {
  if (!fs.existsSync(file())) return null;
  try {
    const raw = fs.readFileSync(file());
    const stored = JSON.parse(raw.toString('utf8')) as StoredShape;
    const refreshToken = vault.decryptString(Buffer.from(stored.enc, 'base64'));
    if (!refreshToken) return null;
    return {
      mcAccessToken: '',
      mcTokenExpiresAt: 0, // always refresh on startup — tokens stay out of storage
      refreshToken,
      profileId: stored.profileId,
      profileName: stored.profileName
    };
  } catch (err) {
    log.warn('account load failed: ' + (err instanceof Error ? err.message : err));
    return null;
  }
}

export function saveAccount(account: McAccount): void {
  const enc = vault.encryptString(account.refreshToken).toString('base64');
  const stored: StoredShape = {
    enc,
    profileId: account.profileId,
    profileName: account.profileName
  };
  fs.mkdirSync(dataDir(), { recursive: true });
  fs.writeFileSync(file(), JSON.stringify(stored), 'utf8');
}

export function clearAccount(): void {
  try { fs.rmSync(file(), { force: true }); } catch { /* ignore */ }
}

/** Returns a usable account (fresh MC token) or null when not logged in. */
export async function ensureFreshToken(): Promise<McAccount | null> {
  const stored = loadAccount();
  if (!stored) return null;
  if (Date.now() < stored.mcTokenExpiresAt - 5 * 60_000 && stored.mcAccessToken) return stored;
  const refreshed = await refreshTokens(stored.refreshToken);
  const account = await identityFromMsToken(refreshed.accessToken, refreshed.refreshToken);
  saveAccount(account);
  return account;
}
