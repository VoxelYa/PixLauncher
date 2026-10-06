import * as crypto from 'crypto';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { dataDir } from '../config';
import { log } from '../logger';

/**
 * Layered token vault (user requirement): ciphertext on disk, plaintext only
 * ever exists transiently in memory.
 *
 *   master key = scrypt(machine profile + random keyfile, N=2^17)
 *   layer 1    = ChaCha20-Poly1305 (k2)
 *   layer 2    = AES-256-GCM (k1)
 *
 * The keyfile lives in the launcher data dir; without BOTH the file and the
 * machine/user profile the ciphertext cannot be unwrapped.
 */

const MAGIC = Buffer.from('PVL1');
const KDF_N = 1 << 17;

function keyfilePath(): string {
  return path.join(dataDir(), 'vault.key');
}

function machineProfile(): string {
  return [os.hostname(), os.userInfo().username, os.homedir(), os.platform()].join('|');
}

function masterKey(): Buffer {
  let salt: Buffer;
  const file = keyfilePath();
  if (fs.existsSync(file)) {
    salt = fs.readFileSync(file);
    if (salt.length !== 32) salt = crypto.randomBytes(32);
  } else {
    salt = crypto.randomBytes(32);
    fs.mkdirSync(dataDir(), { recursive: true });
    fs.writeFileSync(file, salt);
  }
  // hkdfSync derives the two layer keys from one master secret
  const master = crypto.scryptSync(machineProfile(), salt, 64, { N: KDF_N, r: 8, p: 1 });
  return master;
}

let cachedMaster: Buffer | null = null;

function keys(): { k1: Buffer; k2: Buffer } {
  if (!cachedMaster) cachedMaster = masterKey();
  return { k1: cachedMaster.subarray(0, 32), k2: cachedMaster.subarray(32, 64) };
}

export function encrypt(plaintext: Buffer): Buffer {
  const { k1, k2 } = keys();
  const iv2 = crypto.randomBytes(12);
  const c2 = crypto.createCipheriv('chacha20-poly1305', k2, iv2, { authTagLength: 16 });
  const layer2 = Buffer.concat([c2.update(plaintext), c2.final()]);
  const tag2 = c2.getAuthTag();

  const iv1 = crypto.randomBytes(12);
  const c1 = crypto.createCipheriv('aes-256-gcm', k1, iv1, { authTagLength: 16 });
  const layer1 = Buffer.concat([c1.update(layer2), c1.final()]);
  const tag1 = c1.getAuthTag();

  return Buffer.concat([MAGIC, iv1, tag1, iv2, tag2, layer1]);
}

export function decrypt(blob: Buffer): Buffer | null {
  try {
    if (blob.length < 4 + 12 + 16 + 12 + 16 || !blob.subarray(0, 4).equals(MAGIC)) return null;
    const { k1, k2 } = keys();
    let off = 4;
    const iv1 = blob.subarray(off, off + 12); off += 12;
    const tag1 = blob.subarray(off, off + 16); off += 16;
    const iv2 = blob.subarray(off, off + 12); off += 12;
    const tag2 = blob.subarray(off, off + 16); off += 16;
    const layer1 = blob.subarray(off);

    const d1 = crypto.createDecipheriv('aes-256-gcm', k1, iv1, { authTagLength: 16 });
    d1.setAuthTag(tag1);
    const layer2 = Buffer.concat([d1.update(layer1), d1.final()]);

    const d2 = crypto.createDecipheriv('chacha20-poly1305', k2, iv2, { authTagLength: 16 });
    d2.setAuthTag(tag2);
    return Buffer.concat([d2.update(layer2), d2.final()]);
  } catch (err) {
    log.warn('vault decrypt failed: ' + (err instanceof Error ? err.message : err));
    return null;
  }
}

export function encryptString(text: string): Buffer {
  return encrypt(Buffer.from(text, 'utf8'));
}

export function decryptString(blob: Buffer): string | null {
  const plain = decrypt(blob);
  return plain ? plain.toString('utf8') : null;
}
