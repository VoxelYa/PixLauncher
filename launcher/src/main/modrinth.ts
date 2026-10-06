import * as fs from 'fs';
import * as path from 'path';
import { modsDir } from './config';
import { fetchJson } from './net';
import { downloadFile } from './download/engine';

/**
 * Modrinth browsing + one-click install into the game's mods folder
 * (user requirement: in-launcher mod downloads, Modrinth as the source).
 */

const API = 'https://api.modrinth.com/v2';

export interface SearchHit {
  project_id: string;
  slug: string;
  title: string;
  description: string;
  downloads: number;
  icon_url: string;
}

interface ModrinthVersion {
  id: string;
  files: { url: string; filename: string; primary: boolean; size?: number; hashes?: Record<string, string> }[];
}

export async function searchMods(query: string): Promise<SearchHit[]> {
  const facets = encodeURIComponent('[["versions:1.8.9"],["categories:forge"]]');
  const url = `${API}/search?query=${encodeURIComponent(query)}&facets=${facets}&limit=20`;
  const res = await fetchJson<{ hits: SearchHit[] }>(url);
  return res.hits ?? [];
}

export async function installMod(projectId: string, onProgress: (msg: string) => void): Promise<string> {
  const versions = await fetchJson<ModrinthVersion[]>(
    `${API}/project/${encodeURIComponent(projectId)}/version?game_versions=${encodeURIComponent('["1.8.9"]')}&loaders=${encodeURIComponent('["forge"]')}`
  );
  if (!versions.length) throw new Error('No Forge 1.8.9 build for this mod');
  const file = versions[0].files.find((f) => f.primary) ?? versions[0].files[0];

  fs.mkdirSync(modsDir(), { recursive: true });
  const dest = path.join(modsDir(), file.filename);
  onProgress(`Downloading ${file.filename}…`);
  await downloadFile(
    {
      url: file.url,
      dest,
      digest: file.hashes ? manifestDigestOf(file.hashes) : undefined,
      size: file.size
    },
    file.filename,
    (p) => onProgress(`${file.filename} ${p.current}/${p.total} bytes`)
  );
  return file.filename;
}

function manifestDigestOf(hashes: Record<string, string>): { algorithm: string; hex: string } | undefined {
  const key = Object.keys(hashes).find((k) => k.toLowerCase().startsWith('sha'));
  if (!key) return undefined;
  return { algorithm: key, hex: hashes[key] };
}

export function listInstalled(): string[] {
  if (!fs.existsSync(modsDir())) return [];
  return fs.readdirSync(modsDir()).filter((f) => f.toLowerCase().endsWith('.jar'));
}
