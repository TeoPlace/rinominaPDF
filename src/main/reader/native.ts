import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';

/**
 * sorter_barcode.dll (la libreria di sorter-barcode, release pubblica lib_sorter_barcode_windows) chiamata dentro il
 * processo con koffi: era la Foreign Function API di Java, qui cambia solo il ponte. Thread-safe: ogni worker carica la
 * sua istanza e chiama in modo sincrono.
 */
export interface Code {
  type: string;
  value: string;
}

/** La libreria ha risposto con un errore (-2: nessuna immagine che sappia aprire). */
export class ReadError extends Error {
  constructor(message: string, readonly code: number) {
    super(message);
  }
}

export interface NativeReader {
  version(): string;
  /** Un PDF (o BMP): i codici di tutte le pagine. Lancia ReadError. */
  readFile(file: string, fast?: boolean): Code[];
  /** Pixel in grigio a 8 bit gia' in memoria (le pagine renderizzate). */
  readGray(pixels: Buffer, width: number, height: number, fast?: boolean): Code[];
}

const LINE = /^\{"type":"([^"]*)","value":"(.*)"\},?$/;

export function parseCodes(text: string): Code[] {
  const list: Code[] = [];
  for (const raw of text.split('\n')) {
    const m = LINE.exec(raw.replace(/\r$/, ''));
    if (m) list.push({ type: m[1], value: m[2] });
  }
  return list;
}

const ERRORS: Record<number, string> = {
  [-1]: 'argomenti non validi',
  [-2]: 'nessuna immagine che il lettore sappia aprire',
  [-3]: 'risposta troppo lunga'
};

/** La libreria apre i file con fopen (codepage ANSI): un percorso con caratteri fuori dall'ASCII passa da una copia. */
const isAscii = (s: string) => /^[\x20-\x7e]*$/.test(s);

function asciiTempDir(): string {
  const candidates = [os.tmpdir(), process.env.ProgramData ? path.join(process.env.ProgramData, 'rinominaPDF', 'tmp') : '', process.env.PUBLIC ?? ''];
  for (const c of candidates) {
    if (c && isAscii(c)) {
      try {
        fs.mkdirSync(c, { recursive: true });
        return c;
      } catch {
        // prossimo candidato
      }
    }
  }
  throw new Error('nessuna cartella temporanea con un percorso ASCII per leggere un file con caratteri speciali nel nome');
}

function withAsciiCopy<T>(file: string, use: (copy: string) => T): T {
  const copy = path.join(asciiTempDir(), `rinominapdf-${crypto.randomBytes(6).toString('hex')}${path.extname(file)}`);
  fs.copyFileSync(file, copy);
  try {
    return use(copy);
  } finally {
    try {
      fs.unlinkSync(copy);
    } catch {
      // resta nella cartella temporanea: non e' un errore di lettura
    }
  }
}

export function loadNative(dll: string): NativeReader {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const koffi = require('koffi');
  const lib = koffi.load(dll);
  const sbVersion = lib.func('const char *sb_version()');
  const sbReadFile = lib.func('int sb_read_file(const char *path, const char *settings, int fast, _Out_ uint8_t *out, int out_cap, _Out_ int *needed)');
  const sbReadGray = lib.func('int sb_read_gray(const uint8_t *px, int width, int height, int stride, const char *settings, int fast, _Out_ uint8_t *out, int out_cap, _Out_ int *needed)');

  const call = (invoke: (buf: Buffer, need: number[]) => number): Code[] => {
    let buf = Buffer.alloc(8192);
    const need = [0];
    let n = invoke(buf, need);
    if (n === -3) {
      buf = Buffer.alloc(need[0]);
      n = invoke(buf, need);
    }
    if (n < 0) throw new ReadError(ERRORS[n] ?? 'memoria esaurita', n);
    const end = buf.indexOf(0);
    return parseCodes(buf.toString('utf8', 0, end < 0 ? buf.length : end));
  };

  const self: NativeReader = {
    version: () => sbVersion() as string,
    readFile(file, fast = false) {
      if (!isAscii(file)) return withAsciiCopy(file, (copy) => self.readFile(copy, fast));
      return call((buf, need) => sbReadFile(file, 'ALL', fast ? 1 : 0, buf, buf.length, need));
    },
    readGray(pixels, width, height, fast = false) {
      return call((buf, need) => sbReadGray(pixels, width, height, width, 'ALL', fast ? 1 : 0, buf, buf.length, need));
    }
  };
  return self;
}
