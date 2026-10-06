import * as fs from 'fs';
import * as path from 'path';
import { gameDir } from '../config';
import { fetchJson } from '../net';
import { pairs, BMCLAPI } from '../download/sources';
import { cachedWinner } from '../download/speedtest';
import { downloadFile, downloadAll, DownloadItem, manifestDigest } from '../download/engine';

/**
 * Vanilla 1.8.9: version manifest -> version json -> client jar + libraries + assets.
 * Every big item downloads from the speed-test winner with the other source as fallback.
 */

export const VANILLA_VERSION = '1.8.9';

// Mojang's asset-index format fixes objects[].hash to a single named digest
// algorithm (documented at minecraft.wiki); it is not repeated per entry.
const ASSET_DIGEST_ALGORITHM = 'sha1';

interface ManifestEntry { id: string; type: string; url: string }
interface VersionManifest { versions: ManifestEntry[] }
interface VersionJson {
  id: string;
  assets: string;
  assetIndex?: { id: string; url: string; sha1?: string; size?: number };
  downloads?: Record<string, { url: string; sha1?: string; size?: number }>;
  libraries: {
    name: string;
    downloads?: {
      artifact?: { path: string; url: string; sha1?: string; size?: number };
      classifiers?: Record<string, { path: string; url: string; sha1?: string; size?: number }>;
    };
    rules?: { action: string; os?: { name?: string } }[];
    natives?: Record<string, string>;
  }[];
  mainClass: string;
  minecraftArguments?: string;
}

function versionDir(vid: string): string {
  return path.join(gameDir(), 'versions', vid);
}

function ruleAllowsOs(rules: { action: string; os?: { name?: string } }[] | undefined): boolean {
  if (!rules) return true;
  let allowed = false;
  for (const r of rules) {
    if (!r.os || !r.os.name) { if (r.action === 'allow') allowed = true; continue; }
    if (r.os.name === 'windows') { allowed = r.action === 'allow'; }
  }
  return allowed;
}

export async function installVanilla(onProgress: (msg: string) => void): Promise<VersionJson> {
  fs.mkdirSync(gameDir(), { recursive: true });
  onProgress('Resolving version manifest…');
  const { official, mirror } = pairs.versionManifest();
  const winner = await cachedWinner('meta', official, mirror);
  const manifestUrl = winner === 'mirror' ? mirror : official;
  const manifest = await fetchJson<VersionManifest>(manifestUrl).catch(async () =>
    fetchJson<VersionManifest>(winner === 'mirror' ? official : mirror));

  const entry = manifest.versions.find((v) => v.id === VANILLA_VERSION);
  if (!entry) throw new Error(`1.8.9 not found in manifest`);

  onProgress('Fetching 1.8.9 version JSON…');
  let vjsonUrl = entry.url;
  let vjson = await fetchJson<VersionJson>(vjsonUrl).catch(() => null);
  if (!vjson) {
    vjsonUrl = `${BMCLAPI}/version/${VANILLA_VERSION}/json`;
    vjson = await fetchJson<VersionJson>(vjsonUrl);
  }
  fs.mkdirSync(versionDir(VANILLA_VERSION), { recursive: true });
  fs.writeFileSync(path.join(versionDir(VANILLA_VERSION), `${VANILLA_VERSION}.json`), JSON.stringify(vjson, null, 2));

  // Client jar — idempotent: skip when present with the published size
  // (re-runs must never fight a running game for this file).
  const clientMeta = vjson.downloads?.client;
  const clientDest = path.join(versionDir(VANILLA_VERSION), `${VANILLA_VERSION}.jar`);
  const clientOk = fs.existsSync(clientDest) && (!clientMeta?.size || fs.statSync(clientDest).size === clientMeta.size);
  if (clientOk) {
    onProgress('Client jar already present');
  } else {
    onProgress('Downloading minecraft client jar…');
    const clientWinner = await cachedWinner('client', clientMeta?.url ?? '', `${BMCLAPI}/version/${VANILLA_VERSION}/client`);
    await downloadFile(
      {
        url: clientWinner === 'mirror' ? `${BMCLAPI}/version/${VANILLA_VERSION}/client` : clientMeta!.url,
        fallbackUrl: clientWinner === 'mirror' ? clientMeta?.url : `${BMCLAPI}/version/${VANILLA_VERSION}/client`,
        dest: clientDest,
        digest: manifestDigest(clientMeta),
        size: clientMeta?.size
      },
      'client jar',
      (p) => onProgress(`Client jar ${p.current}/${p.total} bytes`)
    );
  }

  // Libraries (artifact + natives classifiers for windows), speed-routed
  const libProbePath = 'com/google/code/gson/gson/2.2.4/gson-2.2.4.jar';
  const libWinner = await cachedWinner(
    'libraries',
    `https://libraries.minecraft.net/${libProbePath}`,
    `${BMCLAPI}/maven/${libProbePath}`
  );
  const libItems: DownloadItem[] = [];
  const libPaths: string[] = [];
  const nativePaths: string[] = [];
  for (const lib of vjson.libraries) {
    if (!ruleAllowsOs(lib.rules)) continue;
    const artifact = lib.downloads?.artifact;
    if (artifact?.path) {
      const mirrorUrl = `${BMCLAPI}/maven/${artifact.path}`;
      libItems.push({
        url: libWinner === 'mirror' ? mirrorUrl : artifact.url,
        fallbackUrl: libWinner === 'mirror' ? artifact.url : mirrorUrl,
        dest: path.join(gameDir(), 'libraries', artifact.path),
        digest: manifestDigest(artifact),
        size: artifact.size
      });
      libPaths.push(artifact.path);
    }
    const nativeKey = lib.natives?.windows;
    if (nativeKey && lib.downloads?.classifiers?.[nativeKey]) {
      const n = lib.downloads.classifiers[nativeKey];
      const mirrorUrl = `${BMCLAPI}/maven/${n.path}`;
      nativePaths.push(n.path);
      libItems.push({
        url: libWinner === 'mirror' ? mirrorUrl : n.url,
        fallbackUrl: libWinner === 'mirror' ? n.url : mirrorUrl,
        dest: path.join(gameDir(), 'libraries', n.path),
        digest: manifestDigest(n),
        size: n.size
      });
    }
  }
  onProgress(`Downloading ${libItems.length} libraries…`);
  await downloadAll(libItems, 'libraries', 12, (p) => onProgress(p.label));

  // Extract windows natives into the shared natives dir the launch uses.
  if (nativePaths.length) {
    const { extractZip } = await import('../util/unzip');
    const nativesDir = path.join(gameDir(), 'natives', VANILLA_VERSION);
    fs.mkdirSync(nativesDir, { recursive: true });
    for (const np of nativePaths) {
      try {
        extractZip(path.join(gameDir(), 'libraries', np), nativesDir);
      } catch (err) {
        onProgress(`native extract failed for ${np}: ${err instanceof Error ? err.message : err}`);
      }
    }
    onProgress('Natives extracted');
  }

  // Assets
  const indexId = vjson.assetIndex?.id ?? vjson.assets ?? 'legacy';
  let indexJsonUrl = vjson.assetIndex?.url;
  let index: { objects: Record<string, { hash: string; size: number }> };
  onProgress('Fetching asset index…');
  try {
    index = await fetchJson(indexJsonUrl!);
  } catch {
    indexJsonUrl = `${BMCLAPI}/assets/indexes/${indexId}.json`;
    index = await fetchJson(indexJsonUrl);
  }
  fs.mkdirSync(path.join(gameDir(), 'assets', 'indexes'), { recursive: true });
  fs.writeFileSync(
    path.join(gameDir(), 'assets', 'indexes', `${indexId}.json`),
    JSON.stringify(index)
  );

  // Assets, speed-routed: probe both hosts with the first real asset
  const firstHash = Object.values(index.objects)[0];
  const firstAssetPath = `${firstHash.hash.slice(0, 2)}/${firstHash.hash}`;
  const assetWinner = await cachedWinner(
    'assets',
    `https://resources.download.minecraft.net/${firstAssetPath}`,
    `${BMCLAPI}/assets/${firstAssetPath}`
  );

  const assetItems: DownloadItem[] = [];
  for (const [name, obj] of Object.entries(index.objects)) {
    const dest = path.join(gameDir(), 'assets', 'objects', obj.hash.slice(0, 2), obj.hash);
    if (fs.existsSync(dest)) continue;
    const officialUrl = `https://resources.download.minecraft.net/${obj.hash.slice(0, 2)}/${obj.hash}`;
    const mirrorUrl = `${BMCLAPI}/assets/${obj.hash.slice(0, 2)}/${obj.hash}`;
    assetItems.push({
      url: assetWinner === 'mirror' ? mirrorUrl : officialUrl,
      fallbackUrl: assetWinner === 'mirror' ? officialUrl : mirrorUrl,
      dest,
      digest: { algorithm: ASSET_DIGEST_ALGORITHM, hex: obj.hash },
      size: obj.size
    });
    void name;
  }
  onProgress(`Downloading ${assetItems.length} assets via ${assetWinner}…`);
  await downloadAll(assetItems, 'assets', 32, (p) => onProgress(p.label));

  onProgress('Vanilla 1.8.9 ready');
  return vjson;
}

export { versionDir, ruleAllowsOs };
export type { VersionJson };
