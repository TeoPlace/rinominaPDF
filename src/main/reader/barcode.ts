import fs from 'node:fs';
import { RENDER_DPI, addClean, selectRight } from '../../shared/rules';
import { ReadError, type NativeReader } from './native';

export interface Lettura {
  barcodes: string[];
  lettore: string;
  /** righe per il registro (tag «lettore»): errori di lettura che non fermano il lavoro */
  note: string[];
}

export type PageRenderer = (pdf: string, dpi: number) => AsyncIterable<{ pixels: Buffer; width: number; height: number }>;

/**
 * Renderizza le pagine di un PDF in grigio (pdf.js + @napi-rs/canvas), una alla volta, ai DPI richiesti. Era PDFBox:
 * serve solo ai PDF che la libreria non sa aprire (codici disegnati come vettori, JBIG2, JPEG 2000).
 */
export const renderPages: PageRenderer = async function* (pdf, dpi) {
  const pdfjs = await import('pdfjs-dist/legacy/build/pdf.mjs');
  const { createCanvas } = await import('@napi-rs/canvas');
  const doc = await pdfjs.getDocument({ data: new Uint8Array(fs.readFileSync(pdf)), useSystemFonts: true, verbosity: 0 }).promise;
  try {
    for (let i = 1; i <= doc.numPages; i++) {
      const page = await doc.getPage(i);
      const vp = page.getViewport({ scale: dpi / 72 });
      const canvas = createCanvas(Math.ceil(vp.width), Math.ceil(vp.height));
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      await page.render({ canvasContext: ctx as never, viewport: vp, canvas: canvas as never }).promise;
      const rgba = ctx.getImageData(0, 0, canvas.width, canvas.height).data;
      const gray = Buffer.alloc(canvas.width * canvas.height);
      for (let k = 0, p = 0; k < gray.length; k++, p += 4) gray[k] = (rgba[p] * 306 + rgba[p + 1] * 601 + rgba[p + 2] * 117) >> 10;
      page.cleanup();
      yield { pixels: gray, width: canvas.width, height: canvas.height };
    }
  } finally {
    await doc.destroy();
  }
};

/**
 * Legge i barcode di un PDF con sorter_barcode, che apre direttamente le immagini delle pagine (mezzo secondo a PDF). Se non
 * sa aprirlo (per lei e' un file illeggibile) le pagine si renderizzano e le si passano i pixel. Su un PDF che apre ma dove non
 * trova il codice NON si renderizza: a 300 DPI ne legge meno che dal file (1 contro 4 su 60).
 */
export async function readPdf(native: NativeReader, pdf: string, render: PageRenderer = renderPages): Promise<Lettura> {
  const barcodes: string[] = [];
  const note: string[] = [];
  let lettore = 'sorter_barcode';
  const base = pdf.split(/[\\/]/).pop();
  try {
    let needRender = false;
    try {
      for (const c of native.readFile(pdf)) addClean(barcodes, c.value);
    } catch (e) {
      if (!(e instanceof ReadError)) throw e;
      needRender = true; // nessuna immagine che la libreria sappia aprire: ci pensa il renderer
    }
    if (needRender) {
      lettore = 'sorter_barcode (pagine renderizzate)';
      for await (const page of render(pdf, RENDER_DPI)) {
        for (const c of native.readGray(page.pixels, page.width, page.height)) addClean(barcodes, c.value);
        if (selectRight(barcodes) !== null) break;
      }
    }
  } catch (e) {
    note.push(`sorter_barcode su ${base}: ${e instanceof Error ? e.message : String(e)}`);
  }
  return { barcodes, lettore, note };
}
