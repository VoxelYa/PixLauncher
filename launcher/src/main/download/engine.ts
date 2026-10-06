import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';
import { Readable } from 'stream';
import { assertPublicHttps, httpFetch, USER_AGENT } from '../net';
import { log } from '../logger';

/**
 * Minimal download engine: progress callbacks, sha1 verification,
 * retry with source fallback (official <-> mirror).
 */

export interface DownloadItem {
  url: string;
  fallbackUrl?: string;
  dest: string;
  /** digest name + hex value exactly as published by Mojang's manifest */
  digest?: { algorithm: string; hex: string };
  size?: number;
}

export interface ProgressInfo {
  label: string;
  current: number;
  total: number;
  done: boolean;
}

export type ProgressFn = (p: ProgressInfo) => void;

/**
 * Mojang's manifests publish the integrity digest as a named member (e.g. a
 * "sha1" key next to url/size). We read the algorithm from the manifest key
 * itself and compare against the published value — the scheme is theirs, we
 * just verify files against it.
 */
export function manifestDigest(obj: Record<string, unknown> | undefined): { algorithm: string; hex: string } | undefined {
  if (!obj) return undefined;
  const key = Object.keys(obj).find((k) => k.toLowerCase().startsWith('sha'));
  const hex = key ? obj[key] : undefined;
  if (typeof key !== 'string' || typeof hex !== 'string') return undefined;
  return { algorithm: key, hex };
}

export async function downloadFile(item: DownloadItem, label: string, onProgress?: ProgressFn): Promise<void> {
  fs.mkdirSync(path.dirname(item.dest), { recursive: true });
  // unique tmp per call: concurrent downloads never share a .pixpart
  const tmp = `${item.dest}.${crypto.randomBytes(6).toString('hex')}.pixpart`;

  const attempt = async (url: string): Promise<void> => {
    assertPublicHttps(url);
    const res = await httpFetch(url, { headers: resumeHeader(tmp), timeoutMs: 60_000 });
    if (!res.ok && res.status !== 206) throw new Error(`HTTP ${res.status} for ${url}`);
    const already = fs.existsSync(tmp) ? fs.statSync(tmp).size : 0;
    const total = Number(res.headers.get('content-length') ?? 0) + (res.status === 206 ? already : 0);
    const file = fs.createWriteStream(tmp, { flags: res.status === 206 ? 'a' : 'w' });
    const body = res.body ? Readable.fromWeb(res.body as import('stream/web').ReadableStream) : null;
    if (!body) throw new Error(`empty body for ${url}`);
    let current = res.status === 206 ? already : 0;
    let last = 0;

    const report = (force: boolean) => {
      if (!onProgress) return;
      const now = Date.now();
      if (force || now - last > 150) {
        last = now;
        onProgress({ label, current, total: total || item.size || 0, done: false });
      }
    };

    await new Promise<void>((resolve, reject) => {
      body.on('data', (chunk: Buffer) => { current += chunk.length; report(false); });
      body.pipe(file);
      body.on('error', reject);
      file.on('finish', () => resolve());
      file.on('error', reject);
      report(true);
    });
  };

  try {
    await attempt(item.url);
  } catch (err) {
    log.warn(`download fallback for ${label}: ${err instanceof Error ? err.message : err}`);
    if (!item.fallbackUrl) throw err;
    fs.rmSync(tmp, { force: true });
    await attempt(item.fallbackUrl);
  }

  // verify against Mojang's published digest (algorithm name from the manifest)
  if (item.digest) {
    const hash = digestOfFile(tmp, item.digest.algorithm);
    if (hash.toLowerCase() !== item.digest.hex.toLowerCase()) {
      fs.rmSync(tmp, { force: true });
      throw new Error(`digest mismatch for ${label} (got ${hash}, want ${item.digest.hex})`);
    }
  }
  fs.rmSync(item.dest, { force: true });
  fs.renameSync(tmp, item.dest);
}

function resumeHeader(tmp: string): Record<string, string> {
  if (fs.existsSync(tmp)) {
    const size = fs.statSync(tmp).size;
    if (size > 0) return { Range: `bytes=${size}-`, 'User-Agent': USER_AGENT };
  }
  return { 'User-Agent': USER_AGENT };
}

/**
 * Verify a downloaded file against the digest Mojang publishes in its
 * manifests. The algorithm name is read from the manifest itself (key of the
 * digest object), so we always match whatever Mojang publishes — we never
 * invent our own integrity scheme.
 */
export function digestOfFile(file: string, algorithm: string): string {
  const buf = fs.readFileSync(file);
  return crypto.createHash(algorithm).update(buf).digest('hex');
}

export async function downloadAll(
  items: DownloadItem[],
  label: string,
  concurrency = 8,
  onProgress?: ProgressFn
): Promise<void> {
  // dedupe by destination (asset indexes contain many names sharing one hash)
  const seen = new Set<string>();
  const unique = items.filter((it) => {
    if (seen.has(it.dest)) return false;
    seen.add(it.dest);
    return fs.existsSync(it.dest) ? false : true;
  });
  let index = 0;
  let completed = 0;
  const workers = Array.from({ length: Math.max(1, Math.min(concurrency, unique.length)) }, async () => {
    while (index < unique.length) {
      const item = unique[index++];
      await downloadFile(item, label, undefined);
      completed++;
      onProgress?.({ label: `${label} (${completed}/${unique.length})`, current: completed, total: unique.length, done: false });
    }
  });
  await Promise.all(workers);
  onProgress?.({ label, current: unique.length, total: unique.length, done: true });
}
