import fs from 'node:fs';
import path from 'node:path';
import { fine, type Elaborazione } from '../../shared/rules';

const MASSIMO = 200;

const num = (v: unknown) => (typeof v === 'number' && Number.isFinite(v) ? v : 0);
const str = (v: unknown) => (typeof v === 'string' ? v : '');

/**
 * Lo storico delle elaborazioni, in `elaborazioni.json` nella cartella di controllo, dalla piu' recente. STESSO file e stessi
 * campi della 1.x (Jackson, camelCase): chi aggiorna ritrova la barra di sinistra. Un file rotto vale storico vuoto.
 */
export class StoricoStore {
  private readonly file: string;
  constructor(controlDir: string) {
    this.file = path.join(controlDir, 'elaborazioni.json');
  }

  /** Una voce rimasta «in corso» viene da un programma morto a meta': si legge come interrotta. */
  carica(): Elaborazione[] {
    try {
      const raw = JSON.parse(fs.readFileSync(this.file, 'utf8').replace(/^﻿/, ''));
      if (!Array.isArray(raw)) return [];
      return raw.map((r): Elaborazione => {
        const e: Elaborazione = {
          iniziataIl: num(r.iniziataIl), finitaIl: num(r.finitaIl), input: str(r.input), output: str(r.output), esito: str(r.esito), istat: str(r.istat),
          anno: str(r.anno), pacco: str(r.pacco), rinominati: num(r.rinominati), nonLetti: num(r.nonLetti), errori: num(r.errori), interrotta: r.interrotta === true
        };
        return e.finitaIl !== 0 ? e : fine(e, e.rinominati, e.nonLetti, e.errori, true, e.iniziataIl);
      });
    } catch {
      return [];
    }
  }

  /** Scrive su un temporaneo e poi rinomina: un'interruzione non lascia un file a meta'. */
  salva(voci: Elaborazione[]): boolean {
    try {
      fs.mkdirSync(path.dirname(this.file), { recursive: true });
      const tmp = this.file + '.tmp';
      fs.writeFileSync(tmp, JSON.stringify(voci.slice(0, MASSIMO), null, 2));
      fs.renameSync(tmp, this.file);
      return true;
    } catch {
      return false;
    }
  }
}
