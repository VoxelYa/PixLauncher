import * as https from 'https';
import { loadSettings, saveSettings } from '../config';
import { log } from '../logger';

/**
 * Auto speed test (user requirement): probe official vs BMCLAPI with a small
 * ranged download, score TTFB + throughput, cache the winner for 24h.
 */

const TEST_TTL_MS = 24 * 60 * 60 * 1000;
const PROBE_BYTES = 1024 * 1024;
const PROBE_TIMEOUT_MS = 10_000;

export type Source = 'official' | 'mirror';

interface ProbeResult {
  source: Source;
  ttfbMs: number;
  bytesPerSec: number;
  ok: boolean;
}

function probe(url: string): Promise<ProbeResult> {
  return new Promise((resolve) => {
    const started = Date.now();
    let ttfbMs = -1;
    let bytes = 0;
    let settled = false;
    const done = (ok: boolean) => {
      if (settled) return;
      settled = true;
      const elapsed = Math.max(1, Date.now() - started - Math.max(0, ttfbMs));
      resolve({ source: 'official', ttfbMs, bytesPerSec: (bytes / elapsed) * 1000, ok });
    };

    try {
      const req = https.request(
        url,
        {
          method: 'GET',
          headers: { Range: `bytes=0-${PROBE_BYTES - 1}`, 'User-Agent': 'PixLauncher/0.1' },
          timeout: PROBE_TIMEOUT_MS
        },
        (res) => {
          if (res.statusCode && res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
            res.resume();
            // follow one hop (BMCLAPI client route redirects)
            resolve(probeFollow(res.headers.location, started, () => Date.now()));
            return;
          }
          if (!res.statusCode || res.statusCode >= 400) {
            res.resume();
            done(false);
            return;
          }
          ttfbMs = Date.now() - started;
          res.on('data', (c: Buffer) => { bytes += c.length; if (bytes >= PROBE_BYTES) res.destroy(); });
          res.on('close', () => done(bytes > 0));
          res.on('error', () => done(bytes > 0));
        }
      );
      req.on('timeout', () => { req.destroy(); done(bytes > 0); });
      req.on('error', () => done(false));
      req.end();
    } catch {
      done(false);
    }
  });
}

function probeFollow(url: string, started: number, _t: () => number): Promise<ProbeResult> {
  // simplified follow: reuse probe() on the redirect target
  return probe(url);
}

export async function speedTest(kind: string, officialUrl: string, mirrorUrl: string): Promise<Source> {
  const [a, b] = await Promise.all([probe(officialUrl), probe(mirrorUrl)]);
  // throughput dominates; TTFB only breaks ties (sustained speed is what the
  // user feels on the 700-file asset phase)
  const score = (r: ProbeResult) => (r.ok ? r.bytesPerSec / (1 + Math.max(0, r.ttfbMs) / 1000) : 0);
  const winner: Source = score(b) > score(a) ? 'mirror' : 'official';
  log.info(`speedtest[${kind}] official=${Math.round(score(a))} mirror=${Math.round(score(b))} -> ${winner}`);
  const settings = loadSettings();
  settings.speedTest[kind] = { winner, testedAt: Date.now() };
  saveSettings(settings);
  return winner;
}

export async function cachedWinner(kind: string, officialUrl: string, mirrorUrl: string): Promise<Source> {
  const settings = loadSettings();
  if (settings.mirrorPreference !== 'auto') return settings.mirrorPreference as Source;
  const cached = settings.speedTest[kind];
  if (cached && Date.now() - cached.testedAt < TEST_TTL_MS) return cached.winner as Source;
  return speedTest(kind, officialUrl, mirrorUrl);
}
