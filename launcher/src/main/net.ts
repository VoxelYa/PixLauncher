/**
 * Outbound HTTP guard: only https, only public hosts (SSRF constraint),
 * consistent UA. The OAuth loopback listener lives in auth/msauth.ts and is
 * a local server, not an outbound request.
 */

const PRIVATE_HOST_PATTERNS: RegExp[] = [
  /^localhost$/i,
  /^127\./,
  /^10\./,
  /^192\.168\./,
  /^172\.(1[6-9]|2\d|3[01])\./,
  /^169\.254\./,
  /^0\./,
  /^\[?::1\]?$/,
  /\.local$/i,
  /\.internal$/i
];

export function assertPublicHttps(url: string): URL {
  const u = new URL(url);
  if (u.protocol !== 'https:') {
    throw new Error(`Blocked non-https outbound URL: ${url}`);
  }
  const host = u.hostname;
  if (PRIVATE_HOST_PATTERNS.some((re) => re.test(host))) {
    throw new Error(`Blocked private/loopback host: ${host}`);
  }
  return u;
}

export const USER_AGENT =
  'PixLauncher/0.1 (+https://github.com/pixlauncher) MinecraftLauncher/1.8.9';

export interface FetchOpts {
  method?: string;
  headers?: Record<string, string>;
  timeoutMs?: number;
}

export async function httpFetch(url: string, opts: FetchOpts = {}): Promise<Response> {
  assertPublicHttps(url);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), opts.timeoutMs ?? 30_000);
  try {
    return await fetch(url, {
      method: opts.method ?? 'GET',
      headers: { 'User-Agent': USER_AGENT, ...(opts.headers ?? {}) },
      redirect: 'follow',
      signal: controller.signal
    });
  } finally {
    clearTimeout(timer);
  }
}

export async function fetchJson<T>(url: string, opts: FetchOpts = {}): Promise<T> {
  const res = await httpFetch(url, opts);
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
  return (await res.json()) as T;
}

export async function headStatus(url: string, timeoutMs = 10_000): Promise<number> {
  try {
    const res = await httpFetch(url, { method: 'HEAD', timeoutMs });
    return res.status;
  } catch {
    return -1;
  }
}
