/**
 * Download source pairs (official ↔ BMCLAPI mirror).
 * Routes verified live on 2026-10-05 (HEAD probes):
 *   /mc/game/version_manifest_v2.json          -> 200
 *   /version/{v}/client                        -> 302 (redirects to file)
 *   /maven/net/minecraftforge/...universal.jar -> 200
 *   /optifine/OptiFine_1.8.9_HD_U_M5.jar       -> 200
 *   /version/{v}/json                          -> assumed (fallback: official)
 */

export const BMCLAPI = 'https://bmclapi2.bangbang93.com';

export type Kind = 'meta' | 'client' | 'libraries' | 'assets' | 'forge' | 'optifine' | 'jre';

export interface SourcePair {
  /** stable key for caching speed-test results */
  key: Kind;
  official: (ctx: MirrorCtx) => string;
  mirror: (ctx: MirrorCtx) => string;
}

export interface MirrorCtx {
  /** full original (official) URL, used for generic host rewrites */
  url?: string;
  versionId?: string;
  /** library path e.g. net/minecraftforge/forge/.../x.jar (after libraries.minecraft.net/) */
  libPath?: string;
  /** asset subpath hh/hash */
  assetPath?: string;
  /** optifine file name e.g. OptiFine_1.8.9_HD_U_M5.jar */
  optifineFile?: string;
  /** forge maven path under net/minecraftforge/forge/ */
  forgeMavenPath?: string;
}

/** Rewrite a generic Mojang URL to BMCLAPI, or return null when no rule applies. */
export function bmclapiRewrite(url: string): string | null {
  const rules: [RegExp, string][] = [
    [/^https:\/\/launchermeta\.mojang\.com\/mc\/game\//, `${BMCLAPI}/mc/game/`],
    [/^https:\/\/piston-meta\.mojang\.com\/mc\/game\//, `${BMCLAPI}/mc/game/`],
    [/^https:\/\/libraries\.minecraft\.net\//, `${BMCLAPI}/maven/`],
    [/^https:\/\/resources\.download\.minecraft\.net\//, `${BMCLAPI}/assets/`],
    [/^https:\/\/maven\.minecraftforge\.net\/net\/minecraftforge\//, `${BMCLAPI}/maven/net/minecraftforge/`]
  ];
  for (const [re, base] of rules) {
    if (re.test(url)) return url.replace(re, base);
  }
  return null;
}

/** Explicit pair builders for the big items (verified routes). */
export const pairs = {
  versionManifest: () => ({
    official: 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json',
    mirror: `${BMCLAPI}/mc/game/version_manifest_v2.json`
  }),
  versionJson: (vid: string) => ({
    official: `https://piston-meta.mojang.com/v1/packages/${vid}` as string, // resolved from manifest
    mirror: `${BMCLAPI}/version/${vid}/json`
  }),
  clientJar: (vid: string) => ({
    official: '' as string, // from version json downloads.client.url
    mirror: `${BMCLAPI}/version/${vid}/client`
  }),
  library: (libPath: string) => ({
    official: `https://libraries.minecraft.net/${libPath}`,
    mirror: `${BMCLAPI}/maven/${libPath}`
  }),
  asset: (assetPath: string) => ({
    official: `https://resources.download.minecraft.net/${assetPath}`,
    mirror: `${BMCLAPI}/assets/${assetPath}`
  }),
  forgeInstaller: (forgeMavenPath: string) => ({
    official: `https://maven.minecraftforge.net/net/minecraftforge/forge/${forgeMavenPath}`,
    mirror: `${BMCLAPI}/maven/net/minecraftforge/forge/${forgeMavenPath}`
  }),
  optifine: (file: string) => ({
    official: `https://optifine.net/download?f=${file}`,
    mirror: `${BMCLAPI}/optifine/${file}`
  })
};

/** Pick a URL given preference (auto uses the speed-test winner stored in settings). */
export function pickUrl(
  pair: { official: string; mirror: string },
  preference: 'auto' | 'official' | 'mirror',
  winner: 'official' | 'mirror' | null
): string {
  if (preference === 'official') return pair.official;
  if (preference === 'mirror') return pair.mirror;
  return winner === 'mirror' ? pair.mirror : pair.official;
}
