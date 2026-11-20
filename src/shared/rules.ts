/**
 * Le regole del prodotto, pure (senza Node): le usano il processo principale, i worker e l'interfaccia.
 * Portate dalla 1.x Java (Globals, JobParams, BarcodeReader.selectRight) senza cambiare un carattere di comportamento:
 * il nome del PDF rinominato e' il contratto verso chi lavora i lotti.
 */

/** Testo fisso nel nome file di destinazione. */
export const FIXED_TEXT = 'sorter';

/** Esiti selezionabili dall'utente. */
export const ESITI = ['COMPIUTAGIACENZA', 'CONSEGNATO', 'RIFIUTATO', 'TRASFERITO', 'IRREPERIBILE', 'EMESSOCAD', 'DECEDUTO', 'ATTIVITACESSATA'] as const;

/** Anno valido: 4 cifre, 19xx/20xx. */
export const ANNO_PATTERN = /^(19|20)\d{2}$/;

/** Istat: solo cifre, max 9, poi zero-padding a sinistra fino a 9. */
export const ISTAT_LENGTH = 9;

/**
 * Barcode «giusto»: le famiglie di codici di una busta riconosciute da SorterNext, in ordine di preferenza.
 *  - 2 lettere + 10 cifre: RB0002858945, AC0002382099
 *  - 12 cifre che iniziano per 2, 3, 6 o 7: 286524542334
 *  - ABW / ACW + 8 cifre: ABW12345678
 *  - 17 cifre, anche precedute da ARG: ARG12345678901234567
 * L'ordine conta quando su una pagina ci sono codici di piu' famiglie: le 2 lettere + 10 cifre restano prime, cosi' i PDF
 * che si rinominavano gia' prendono ancora lo stesso nome.
 */
export const BARCODE_FAMILIES: RegExp[] = [/^[A-Z]{2}\d{10}$/, /^[2367]\d{11}$/, /^A[BC]W\d{8}$/, /^(?:ARG)?\d{17}$/];

/**
 * Codici cercati come sottostringa del testo di un 2D, che puo' contenere dati aggiuntivi. Solo le famiglie con lettere:
 * un 12 o 17 cifre preso a meta' di una sequenza di cifre piu' lunga sarebbe un codice inventato.
 */
const INNER_CODES: RegExp[] = [/[A-Z]{2}\d{10}/, /A[BC]W\d{8}(?!\d)/];

/** DPI con cui si renderizzano le pagine dei PDF che sorter_barcode non sa aprire da solo. */
export const RENDER_DPI = 300;

export interface JobParams {
  input: string;
  output: string;
  esito: string;
  /** con zero-padding a 9 cifre */
  istat: string;
  anno: string;
  pacco: string;
}

/** Cio' che l'utente ha scritto nel form, prima della validazione. */
export interface JobForm {
  input: string;
  output: string;
  esito: string;
  istat: string;
  anno: string;
  pacco: string;
}

export const padIstat = (raw: string): string => {
  const digits = raw.trim();
  return '0'.repeat(Math.max(0, ISTAT_LENGTH - digits.length)) + digits;
};

/** Nome di destinazione: barcode-sorter-esito-istat-anno-pacco.pdf */
export const buildFileName = (p: Pick<JobParams, 'esito' | 'istat' | 'anno' | 'pacco'>, barcode: string): string =>
  [barcode, FIXED_TEXT, p.esito, p.istat, p.anno, p.pacco].join('-') + '.pdf';

const norm = (p: string) => p.replace(/[\\/]+$/, '').toLowerCase().replace(/\//g, '\\');

/**
 * Valida il form: i parametri pronti oppure il messaggio per l'utente (gli stessi della 1.x). Non guarda il disco: che la
 * cartella di input esista lo controlla chi ha accesso al file system.
 */
export function validateForm(f: JobForm): { ok: true; params: JobParams } | { ok: false; error: string } {
  const input = f.input.trim();
  const output = f.output.trim();
  const istat = f.istat.trim();
  const anno = f.anno.trim();
  const pacco = f.pacco.trim();
  if (input === '') return { ok: false, error: 'Seleziona una cartella di Input esistente.' };
  if (output === '') return { ok: false, error: 'Seleziona la cartella di Output.' };
  if (norm(input) === norm(output)) return { ok: false, error: 'Input e Output devono essere cartelle diverse.' };
  if (!(ESITI as readonly string[]).includes(f.esito)) return { ok: false, error: 'Seleziona un Esito.' };
  if (!/^\d{1,9}$/.test(istat)) return { ok: false, error: 'Istat non valido: servono da 1 a 9 cifre (verrà completato a 9 con zeri a sinistra).' };
  if (!ANNO_PATTERN.test(anno)) return { ok: false, error: 'Anno non valido: inserisci un anno a 4 cifre (es. 2026).' };
  if (!/^[A-Za-z0-9]+$/.test(pacco)) return { ok: false, error: 'Pacco non valido: usa solo lettere e numeri (es. MP297).' };
  return { ok: true, params: { input, output, esito: f.esito, istat: padIstat(istat), anno, pacco: pacco.toUpperCase() } };
}

/** Pulizia barcode identica a barcodeClean di sorter-fx. */
export function cleanBarcode(barcode: string): string {
  let ret = barcode.replace(/-BACK/g, '').replace(/ /g, '').replace(/"/g, '');
  const m = /([0-9a-zA-Z]+)\/.+/.exec(barcode);
  if (m) ret = m[1];
  return ret.trim();
}

/** Aggiunge un codice letto alla lista dopo averlo pulito, senza doppioni e senza vuoti. */
export function addClean(list: string[], raw: string): void {
  const b = cleanBarcode(raw);
  if (b.trim() !== '' && !list.includes(b)) list.push(b);
}

function innerCode(barcode: string): string | null {
  for (const p of INNER_CODES) {
    const m = p.exec(barcode);
    if (m) return m[0];
  }
  return null;
}

/**
 * Seleziona il barcode giusto nell'ordine in cui sono stati trovati: prima un match esatto di una delle famiglie, famiglia per
 * famiglia nell'ordine di preferenza, poi la ricerca come sottostringa (i 2D possono contenere dati aggiuntivi).
 */
export function selectRight(barcodes: string[]): string | null {
  for (const family of BARCODE_FAMILIES) {
    for (const b of barcodes) if (family.test(b)) return b;
  }
  for (const b of barcodes) {
    const inner = innerCode(b);
    if (inner !== null) return inner;
  }
  return null;
}

// ---- lo storico ----------------------------------------------------------------------------------------------------
/** Una voce della barra di sinistra. `finitaIl` vale 0 finche' e' in corso. Stesso JSON della 1.x (elaborazioni.json). */
export interface Elaborazione {
  iniziataIl: number;
  finitaIl: number;
  input: string;
  output: string;
  esito: string;
  istat: string;
  anno: string;
  pacco: string;
  rinominati: number;
  nonLetti: number;
  errori: number;
  interrotta: boolean;
}

export type Stato = 'IN_CORSO' | 'RIUSCITA' | 'NON_LETTI' | 'VUOTA' | 'ERRORE';

/** Il pallino: un elenco piu' corto del previsto non deve sembrare un lavoro andato bene. */
export function statoOf(e: Elaborazione): Stato {
  if (e.finitaIl === 0) return 'IN_CORSO';
  if (e.interrotta || e.errori > 0) return 'ERRORE';
  if (e.nonLetti > 0) return 'NON_LETTI';
  return e.rinominati === 0 ? 'VUOTA' : 'RIUSCITA';
}

export const inizio = (p: JobParams, adesso: number): Elaborazione => ({
  iniziataIl: adesso, finitaIl: 0, input: p.input, output: p.output, esito: p.esito, istat: p.istat, anno: p.anno, pacco: p.pacco,
  rinominati: 0, nonLetti: 0, errori: 0, interrotta: false
});

export const fine = (e: Elaborazione, ok: number, unread: number, errors: number, aborted: boolean, adesso: number): Elaborazione => ({
  ...e, finitaIl: adesso, rinominati: ok, nonLetti: unread, errori: errors, interrotta: aborted
});

// ---- il registro ---------------------------------------------------------------------------------------------------
export type LogCategory = 'rinominati' | 'nonLetti' | 'errori' | 'lavoro';
export const LOG_LABEL: Record<LogCategory, string> = { rinominati: 'Rinominati', nonLetti: 'Non letti', errori: 'Errori', lavoro: 'Lavoro e lettore' };

/** Dal tag con cui i lavoratori scrivono nel registro. */
export const categoryOfTag = (tag: string): LogCategory =>
  tag === 'rename' ? 'rinominati' : tag === 'barcode' ? 'nonLetti' : tag === 'errore' ? 'errori' : 'lavoro';

export interface LogLine {
  at: number;
  tag: string;
  text: string;
}
