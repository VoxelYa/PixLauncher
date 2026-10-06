import * as fs from 'fs';
import * as path from 'path';

let logFile: string | null = null;

export function initLogger(dir: string): void {
  fs.mkdirSync(dir, { recursive: true });
  logFile = path.join(dir, `pixlauncher-${new Date().toISOString().slice(0, 10)}.log`);
  append(`--- PixLauncher session start ${new Date().toISOString()} ---`);
}

export function append(line: string): void {
  const text = `[${new Date().toISOString()}] ${line}\n`;
  // eslint-disable-next-line no-console
  console.log(text.trimEnd());
  if (logFile) {
    try { fs.appendFileSync(logFile, text); } catch { /* best effort */ }
  }
}

export const log = {
  info: (msg: string) => append(`INFO  ${msg}`),
  warn: (msg: string) => append(`WARN  ${msg}`),
  error: (msg: string, err?: unknown) => append(`ERROR ${msg}${err instanceof Error ? ` :: ${err.stack ?? err.message}` : ''}`)
};
