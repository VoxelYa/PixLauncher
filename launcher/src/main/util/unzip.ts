import * as fs from 'fs';
import * as path from 'path';
import * as zlib from 'zlib';

/**
 * Minimal zip extractor (stored + deflate) — enough for Minecraft native jars.
 * No external tools, no child processes. Entries are resolved against destDir
 * and rejected unless they stay inside it.
 */
export function extractZip(zipPath: string, destDir: string): void {
  const buf = fs.readFileSync(zipPath);
  const root = path.resolve(destDir);
  fs.mkdirSync(root, { recursive: true });

  // locate End Of Central Directory (scan backwards for signature 0x06054b50)
  let eocd = -1;
  for (let i = buf.length - 22; i >= 0 && i > buf.length - 66000; i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('not a zip (no EOCD): ' + zipPath);

  const entryCount = buf.readUInt16LE(eocd + 10);
  let cdOffset = buf.readUInt32LE(eocd + 16);

  for (let n = 0; n < entryCount; n++) {
    if (buf.readUInt32LE(cdOffset) !== 0x02014b50) break;
    const method = buf.readUInt16LE(cdOffset + 10);
    const compressedSize = buf.readUInt32LE(cdOffset + 20);
    const nameLen = buf.readUInt16LE(cdOffset + 28);
    const extraLen = buf.readUInt16LE(cdOffset + 30);
    const commentLen = buf.readUInt16LE(cdOffset + 32);
    const localOffset = buf.readUInt32LE(cdOffset + 42);
    const name = buf.toString('utf8', cdOffset + 46, cdOffset + 46 + nameLen);

    // local file header sizes may differ via data descriptors — use central values
    const lhNameLen = buf.readUInt16LE(localOffset + 26);
    const lhExtraLen = buf.readUInt16LE(localOffset + 28);
    const dataStart = localOffset + 30 + lhNameLen + lhExtraLen;

    const outPath = path.resolve(root, name.replace(/\\/g, '/'));
    if (outPath !== root && !outPath.startsWith(root + path.sep)) {
      cdOffset += 46 + nameLen + extraLen + commentLen;
      continue; // entry escapes the destination — skip it
    }
    if (name.endsWith('/')) {
      fs.mkdirSync(outPath, { recursive: true });
    } else {
      const raw = buf.subarray(dataStart, dataStart + compressedSize);
      const data = method === 0 ? Buffer.from(raw) : zlib.inflateRawSync(raw);
      fs.mkdirSync(path.dirname(outPath), { recursive: true });
      fs.writeFileSync(outPath, data);
    }
    cdOffset += 46 + nameLen + extraLen + commentLen;
  }
}
