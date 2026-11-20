import path from 'node:path';
import fs from 'node:fs';
import { buildSync } from 'esbuild';

export const root = path.join(__dirname, '..');
export const DLL = path.join(root, 'lib', 'sorter-barcode', 'sorter_barcode.dll');
export const FIX = path.join(__dirname, 'fixtures');
export const TMP = path.join(__dirname, '.tmp');

/** Compila (una volta) il worker di lettura in tests/.tmp, come fa lo scheletro in build/main/worker. */
export function buildWorker(): string {
  const out = path.join(TMP, 'worker');
  buildSync({
    entryPoints: [path.join(root, 'src/main/worker/reader.ts')],
    outdir: out,
    bundle: true,
    platform: 'node',
    target: 'node24',
    format: 'cjs',
    external: ['koffi', 'pdfjs-dist', 'pdfjs-dist/*', '@napi-rs/canvas', 'node:*']
  });
  return path.join(out, 'reader.js');
}

// Interleaved 2 of 5: 0 nnwwn, 1 wnnnw, 2 nwnnw, 3 wwnnn, 4 nnwnw, 5 wnwnn, 6 nwwnn, 7 nnnww, 8 wnnwn, 9 nwnwn
const ITF = ['nnwwn', 'wnnnw', 'nwnnw', 'wwnnn', 'nnwnw', 'wnwnn', 'nwwnn', 'nnnww', 'wnnwn', 'nwnwn'];

/** Le barre di un ITF come rettangoli [x, larghezza] in unita' di modulo (la prima e' una barra). */
export function itfBars(digits: string): [number, number][] {
  const widths: number[] = [1, 1, 1, 1]; // start: barra, spazio, barra, spazio (stretti)
  for (let i = 0; i < digits.length; i += 2) {
    const a = ITF[Number(digits[i])];
    const b = ITF[Number(digits[i + 1])];
    for (let k = 0; k < 5; k++) widths.push(a[k] === 'w' ? 3 : 1, b[k] === 'w' ? 3 : 1);
  }
  widths.push(3, 1, 1); // stop: barra larga, spazio, barra
  const bars: [number, number][] = [];
  let x = 0;
  widths.forEach((w, i) => {
    if (i % 2 === 0) bars.push([x, w]);
    x += w;
  });
  return bars;
}

/**
 * Un PDF a una pagina con il codice ITF DISEGNATO COME VETTORI (rettangoli): nessuna immagine dentro, quindi la libreria
 * risponde «nessuna immagine» (-2) e il lettore deve passare dal rendering delle pagine.
 */
export function vectorBarcodePdf(digits: string, file: string): void {
  const module = 2.2; // punti per modulo (300 DPI: ~9 pixel)
  const bars = itfBars(digits);
  const x0 = 80;
  const content = '0 g\n' + bars.map(([x, w]) => `${(x0 + x * module).toFixed(2)} 600 ${(w * module).toFixed(2)} 90 re f`).join('\n') + '\n';
  const objs = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << >> >>',
    `<< /Length ${Buffer.byteLength(content)} >>\nstream\n${content}endstream`
  ];
  let out = '%PDF-1.4\n';
  const offsets: number[] = [];
  objs.forEach((o, i) => {
    offsets.push(Buffer.byteLength(out));
    out += `${i + 1} 0 obj\n${o}\nendobj\n`;
  });
  const xref = Buffer.byteLength(out);
  out += `xref\n0 ${objs.length + 1}\n0000000000 65535 f \n` + offsets.map((o) => String(o).padStart(10, '0') + ' 00000 n \n').join('');
  out += `trailer\n<< /Size ${objs.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  fs.writeFileSync(file, out, 'latin1');
}

/** Un PDF a una pagina senza niente di disegnato. */
export function blankPdf(file: string): void {
  const objs = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Resources << >> >>'
  ];
  let out = '%PDF-1.4\n';
  const offsets: number[] = [];
  objs.forEach((o, i) => {
    offsets.push(Buffer.byteLength(out));
    out += `${i + 1} 0 obj\n${o}\nendobj\n`;
  });
  const xref = Buffer.byteLength(out);
  out += `xref\n0 ${objs.length + 1}\n0000000000 65535 f \n` + offsets.map((o) => String(o).padStart(10, '0') + ' 00000 n \n').join('');
  out += `trailer\n<< /Size ${objs.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  fs.writeFileSync(file, out, 'latin1');
}
