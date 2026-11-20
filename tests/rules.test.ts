import { describe, expect, it, beforeEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { TMP } from './helpers';
import { addClean, buildFileName, categoryOfTag, cleanBarcode, fine, inizio, padIstat, selectRight, statoOf, validateForm, type JobForm } from '../src/shared/rules';
import { StoricoStore } from '../src/main/core/store';

describe('scelta del barcode giusto (le famiglie di SorterNext)', () => {
  it('due lettere + dieci cifre', () => {
    expect(selectRight(['RB0002858945'])).toBe('RB0002858945');
    expect(selectRight(['8001234567890', 'AC0002382099'])).toBe('AC0002382099');
  });
  it('dodici cifre che iniziano per 2, 3, 6 o 7', () => {
    for (const c of ['286524542334', '393673094350', '612345678901', '712345678901']) expect(selectRight([c])).toBe(c);
  });
  it('dodici cifre con la prima fuori famiglia, o di lunghezza sbagliata, non valgono', () => {
    for (const c of ['186524542334', '486524542334', '28652454233', '2865245423345']) expect(selectRight([c])).toBeNull();
  });
  it('ABW / ACW + otto cifre', () => {
    expect(selectRight(['ABW12345678'])).toBe('ABW12345678');
    expect(selectRight(['ACW12345678'])).toBe('ACW12345678');
    expect(selectRight(['ADW12345678'])).toBeNull();
    expect(selectRight(['ABW1234567'])).toBeNull();
  });
  it('diciassette cifre, anche con ARG', () => {
    expect(selectRight(['12345678901234567'])).toBe('12345678901234567');
    expect(selectRight(['ARG12345678901234567'])).toBe('ARG12345678901234567');
    expect(selectRight(['1234567890123456'])).toBeNull();
  });
  it('con codici di piu famiglie vince quello che la 1.0.1 avrebbe gia scelto', () => {
    expect(selectRight(['286524542334', 'ABW12345678', 'AB0002434559'])).toBe('AB0002434559');
    expect(selectRight(['12345678901234567', '286524542334'])).toBe('286524542334');
  });
  it('sottostringa di un 2D', () => {
    expect(selectRight(['X|AC0002382099|2026'])).toBe('AC0002382099');
    expect(selectRight(['X|ABW12345678|2026'])).toBe('ABW12345678');
  });
  it('un 12 o 17 cifre in mezzo a cifre piu lunghe non e un codice', () => {
    expect(selectRight(['0028652454233499'])).toBeNull();
    expect(selectRight(['X|12345678901234567890'])).toBeNull();
  });
  it('niente di valido', () => {
    expect(selectRight([])).toBeNull();
    expect(selectRight(['8001234567890', 'HELLO'])).toBeNull();
  });
  it('difetto noto della 1.x, conservato: la sottostringa prende OO6986475960 da ...OOOO698647596022', () => {
    expect(selectRight(['OOOO698647596022'])).toBe('OO6986475960');
  });
});

describe('pulizia dei codici letti (barcodeClean di sorter-fx)', () => {
  it('toglie -BACK, spazi e virgolette; prende il prefisso prima di una barra', () => {
    expect(cleanBarcode('RB0000593170-BACK')).toBe('RB0000593170');
    expect(cleanBarcode('RB 0000 593170')).toBe('RB0000593170');
    expect(cleanBarcode('"RB0000593170"')).toBe('RB0000593170');
    expect(cleanBarcode('AB123/dati/altro')).toBe('AB123');
  });
  it('senza doppioni e senza vuoti', () => {
    const l: string[] = [];
    for (const c of ['RB0000593170', 'RB0000593170-BACK', ' ', '']) addClean(l, c);
    expect(l).toEqual(['RB0000593170']);
  });
});

describe('il form e il nome del file', () => {
  const ok: JobForm = { input: 'C:\\in', output: 'C:\\out', esito: 'COMPIUTAGIACENZA', istat: '63064', anno: '2026', pacco: '66555p310' };
  it('il nome come nell\'esempio dello standard', () => {
    const v = validateForm({ ...ok, istat: '000063064', pacco: '66555P310' });
    expect(v.ok).toBe(true);
    if (v.ok) expect(buildFileName(v.params, 'RB0002858945')).toBe('RB0002858945-sorter-COMPIUTAGIACENZA-000063064-2026-66555P310.pdf');
  });
  it('istat a 9 cifre con zeri a sinistra, pacco in maiuscolo', () => {
    const v = validateForm(ok);
    expect(v.ok && v.params.istat).toBe('000063064');
    expect(v.ok && v.params.pacco).toBe('66555P310');
    expect(padIstat(' 1 ')).toBe('000000001');
  });
  it('ogni errore ha il suo messaggio', () => {
    const err = (f: Partial<JobForm>) => { const v = validateForm({ ...ok, ...f }); return v.ok ? 'OK' : v.error; };
    expect(err({ input: '' })).toMatch(/Input/);
    expect(err({ output: ' ' })).toMatch(/Output/);
    expect(err({ output: 'c:\\IN\\' })).toMatch(/diverse/);
    expect(err({ esito: '' })).toMatch(/Esito/);
    expect(err({ esito: 'BOH' })).toMatch(/Esito/);
    expect(err({ istat: '' })).toMatch(/Istat/);
    expect(err({ istat: '1234567890' })).toMatch(/Istat/);
    expect(err({ istat: '12a' })).toMatch(/Istat/);
    expect(err({ anno: '1899' })).toMatch(/Anno/);
    expect(err({ anno: '20266' })).toMatch(/Anno/);
    expect(err({ pacco: '' })).toMatch(/Pacco/);
    expect(err({ pacco: 'MP 297' })).toMatch(/Pacco/);
    for (const anno of ['1900', '1999', '2000', '2099']) expect(err({ anno })).toBe('OK');
  });
});

const dir = path.join(TMP, 'storico');
beforeEach(() => fs.rmSync(dir, { recursive: true, force: true }));
const P = { input: 'C:\\in', output: 'C:\\out', esito: 'CONSEGNATO', istat: '000000001', anno: '2026', pacco: 'MP1' };

describe('lo storico (stesso elaborazioni.json della 1.x)', () => {
  it('cartella senza file = storico vuoto; un file rotto anche', () => {
    expect(new StoricoStore(dir).carica()).toEqual([]);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(path.join(dir, 'elaborazioni.json'), '{rotto');
    expect(new StoricoStore(dir).carica()).toEqual([]);
  });
  it('legge un file scritto da Jackson (record camelCase)', () => {
    fs.mkdirSync(dir, { recursive: true });
    const e = { iniziataIl: 1000, finitaIl: 2000, input: 'C:\\in', output: 'C:\\out', esito: 'CONSEGNATO', istat: '000000001', anno: '2026', pacco: 'MP1', rinominati: 5, nonLetti: 1, errori: 0, interrotta: false };
    fs.writeFileSync(path.join(dir, 'elaborazioni.json'), JSON.stringify([e], null, 2));
    expect(new StoricoStore(dir).carica()).toEqual([e]);
  });
  it('rilegge quello che ha scritto; una voce rimasta in corso si rilegge interrotta', () => {
    const s = new StoricoStore(dir);
    const finita = fine(inizio(P, 1), 5, 0, 0, false, 2);
    s.salva([inizio(P, 3), finita]);
    const l = s.carica();
    expect(l[1]).toEqual(finita);
    expect(l[0].interrotta).toBe(true);
    expect(statoOf(l[0])).toBe('ERRORE');
  });
  it('non tiene piu di 200 voci', () => {
    const s = new StoricoStore(dir);
    s.salva(Array.from({ length: 230 }, (_, i) => fine(inizio(P, i + 1), 1, 0, 0, false, i + 2)));
    expect(s.carica()).toHaveLength(200);
  });
  it('il pallino dice com\'e andata', () => {
    const e = inizio(P, 1);
    expect(statoOf(e)).toBe('IN_CORSO');
    expect(statoOf(fine(e, 5, 0, 0, false, 2))).toBe('RIUSCITA');
    expect(statoOf(fine(e, 5, 1, 0, false, 2))).toBe('NON_LETTI');
    expect(statoOf(fine(e, 5, 1, 1, false, 2))).toBe('ERRORE');
    expect(statoOf(fine(e, 5, 0, 0, true, 2))).toBe('ERRORE');
    expect(statoOf(fine(e, 0, 0, 0, false, 2))).toBe('VUOTA');
  });
  it('le categorie del registro', () => {
    expect(['rename', 'barcode', 'errore', 'job', 'lettore'].map(categoryOfTag)).toEqual(['rinominati', 'nonLetti', 'errori', 'lavoro', 'lavoro']);
  });
});
