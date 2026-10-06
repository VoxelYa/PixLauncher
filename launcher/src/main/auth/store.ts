import * as fs from 'fs';
import * as path from 'path';
import { dataDir } from '../config';
import { log } from '../logger';
import { identityFromMsToken, refreshTokens, refreshLiveTokens, McIdentity } from './chain';
import * as vault from './vault';

/**
 * Multi-account storage shared by GUI and headless modes. Every account keeps
 * its refresh token only as layered ciphertext on disk (see vault.ts); the
 * plaintext exists just long enough to refresh, in memory. One account is
 * "active" — launches and the UI use it; switching is instant (no re-login).
 */

export type McAccount = McIdentity & { authProvider?: 'live' | 'ms' };

function file(): string {
  return path.join(dataDir(), 'accounts.bin');
}

interface StoredAccount {
  enc: string; // base64 vault ciphertext of the refresh token
  plainRefresh?: string; // legacy headless-written plaintext
  profileId: string;
  profileName: string;
  authProvider?: 'live' | 'ms';
}

interface StoredShape {
  active?: string;
  accounts?: StoredAccount[];
  // legacy single-account fields (migrated on first write)
  enc?: string;
  plainRefresh?: string;
  profileId?: string;
  profileName?: string;
  authProvider?: string;
}

function readRaw(): StoredShape {
  try {
    return JSON.parse(fs.readFileSync(file(), 'utf8')) as StoredShape;
  } catch {
    return {};
  }
}

function writeRaw(shape: StoredShape): void {
  fs.mkdirSync(dataDir(), { recursive: true });
  fs.writeFileSync(file(), JSON.stringify(shape), 'utf8');
}

function decryptRefresh(enc: string): string | null {
  return vault.decryptString(Buffer.from(enc, 'base64'));
}

function toAccount(stored: StoredAccount): McAccount | null {
  const refreshToken = decryptRefresh(stored.enc);
  if (!refreshToken) return null;
  return {
    mcAccessToken: '',
    mcTokenExpiresAt: 0, // always refresh on startup — tokens stay out of storage
    refreshToken,
    profileId: stored.profileId,
    profileName: stored.profileName,
    authProvider: stored.authProvider ?? 'ms'
  };
}

export interface AccountInfo {
  profileId: string;
  profileName: string;
  active: boolean;
}

/** All stored accounts (refresh tokens stay encrypted; only profile info exposed). */
export function listAccounts(): AccountInfo[] {
  const raw = readRaw();
  const out: AccountInfo[] = [];
  for (const a of raw.accounts ?? []) {
    out.push({ profileId: a.profileId, profileName: a.profileName, active: a.profileId === raw.active });
  }
  // legacy single-account file
  if (!raw.accounts && raw.profileId && (raw.enc || raw.plainRefresh)) {
    out.push({ profileId: raw.profileId ?? '', profileName: raw.profileName ?? '', active: true });
  }
  return out;
}

function findStored(profileId?: string): StoredAccount | null {
  const raw = readRaw();
  if (raw.accounts?.length) {
    const id = profileId ?? raw.active ?? raw.accounts[0].profileId;
    return raw.accounts.find((a) => a.profileId === id) ?? raw.accounts[0] ?? null;
  }
  if (raw.profileId && (raw.enc || raw.plainRefresh)) {
    return {
      enc: raw.enc ?? '',
      plainRefresh: raw.plainRefresh,
      profileId: raw.profileId ?? '',
      profileName: raw.profileName ?? '',
      authProvider: (raw.authProvider as 'live' | 'ms') ?? 'ms'
    };
  }
  return null;
}

export function loadAccount(profileId?: string): McAccount | null {
  const stored = findStored(profileId);
  if (!stored) return null;
  const refreshToken = stored.plainRefresh ?? decryptRefresh(stored.enc);
  if (!refreshToken) return null;
  return {
    mcAccessToken: '',
    mcTokenExpiresAt: 0, // always refresh on startup — tokens stay out of storage
    refreshToken,
    profileId: stored.profileId,
    profileName: stored.profileName,
    authProvider: stored.authProvider ?? 'ms'
  };
}

export function saveAccount(account: McAccount): void {
  const enc = vault.encryptString(account.refreshToken).toString('base64');
  const raw = readRaw();
  if (!Array.isArray(raw.accounts)) raw.accounts = [];
  const existing = raw.accounts.find((a) => a.profileId === account.profileId);
  const entry: StoredAccount = {
    enc,
    profileId: account.profileId,
    profileName: account.profileName,
    authProvider: account.authProvider ?? 'ms'
  };
  if (existing) Object.assign(existing, entry);
  else raw.accounts.push(entry);
  raw.active = account.profileId;
  writeRaw(raw);
  log.info('account stored: ' + account.profileName);
}

export function switchActive(profileId: string): boolean {
  const raw = readRaw();
  if (!raw.accounts?.some((a) => a.profileId === profileId)) return false;
  raw.active = profileId;
  writeRaw(raw);
  return true;
}

export function removeAccount(profileId: string): void {
  const raw = readRaw();
  if (Array.isArray(raw.accounts)) {
    raw.accounts = raw.accounts.filter((a) => a.profileId !== profileId);
    if (raw.active === profileId) raw.active = raw.accounts[0]?.profileId;
  } else {
    writeRaw({});
  }
  writeRaw(raw);
}

export function clearAccount(): void {
  const active = loadAccount();
  if (active) removeAccount(active.profileId);
  else fs.rmSync(file(), { force: true });
}

/** Fresh tokens for the active (or given) account, or null when nothing stored. */
export async function ensureFreshToken(profileId?: string): Promise<McAccount | null> {
  const stored = findStored(profileId);
  if (!stored) return null;
  const refreshToken = stored.plainRefresh ?? decryptRefresh(stored.enc);
  if (!refreshToken) return null;

  // refresh through the endpoint that issued the token (Live vs Entra)
  const provider = stored.authProvider ?? 'ms';
  const refresher = provider === 'live' ? refreshLiveTokens : refreshTokens;
  const refreshed = await refresher(refreshToken);
  const account = await identityFromMsToken(refreshed.accessToken, refreshed.refreshToken);
  account.authProvider = provider;
  saveAccount(account);
  return account;
}
