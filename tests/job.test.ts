import { beforeAll, describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { DLL, FIX, TMP, blankPdf, buildWorker, vectorBarcodePdf } from './helpers';
import { loadNative } from '../src/main/reader/native';
import { readPdf } from '../src/main/reader/barcode';
import { RenameJob, workerReaderFactory, type PdfReader } from '../src/main/core/job';
import { ElaborazioneService, type ServiceEvent } from '../src/main/core/service';
import { StoricoStore } from '../src/main/core/store';
import type { JobParams } from '../src/shared/rules';

let workerFile = '';
const sample = path.join(FIX, 'sample.pdf');
beforeAll(() => {
  fs.mkdirSync(TMP, { recursive: true });
  workerFile = buildWorker();
}, 60000);

function fresh(name: string) {
  const d = path.join(TMP, name);
  fs.rmSync(d, { recursive: true, force: true });
  fs.mkdirSync(path.join(d, 'in', 'sotto'), { recursive: true });
  return { input: path.join(d, 'in'), output: path.join(d, 'out'), dir: d };
}
const params = (p: { input: string; output: string }): JobParams => ({ ...p, esito: 'CONSEGNATO', istat: '000063064', anno: '2026', pacco: 'MP297' });

describe('lettura di un PDF con la libreria C (koffi)', () => {
  it('un PDF con le immagini del sorter: il DataMatrix, pulito dal -BACK', async () => {
    const l = await readPdf(loadNative(DLL), sample);
    expect(l.lettore).toBe('sorter_barcode');
    expect(l.barcodes).toEqual(['RB0000593170']);
    expect(l.note).toEqual([]);
  });

  it('un PDF con il codice DISEGNATO COME VETTORI: la libreria non sa aprirlo, si renderizza e si legge', async () => {
    const f = path.join(TMP, 'vettoriale.pdf');
    vectorBarcodePdf('286524542334', f);
    const l = await readPdf(loadNative(DLL), f);
    expect(l.lettore).toBe('sorter_barcode (pagine renderizzate)');
    expect(l.barcodes).toContain('286524542334');
  }, 60000);

  it('un PDF vuoto: renderizzato, nessun codice, niente errori', async () => {
    const f = path.join(TMP, 'vuoto.pdf');
    blankPdf(f);
    const l = await readPdf(loadNative(DLL), f);
    expect(l.barcodes).toEqual([]);
    expect(l.lettore).toBe('sorter_barcode (pagine renderizzate)');
    expect(l.note).toEqual([]);
  }, 60000);

  it('un file che non e un PDF: una nota per il registro, non un crash', async () => {
    const f = path.join(TMP, 'finto.pdf');
    fs.writeFileSync(f, 'non sono un pdf');
    const l = await readPdf(loadNative(DLL), f);
    expect(l.barcodes).toEqual([]);
    expect(l.note.join(' ')).toContain('finto.pdf');
  }, 60000);

  it('un nome con caratteri fuori dall ASCII (la libreria apre con fopen ANSI) legge lo stesso', async () => {
    const f = path.join(TMP, 'è più ☃.pdf');
    fs.copyFileSync(sample, f);
    expect((await readPdf(loadNative(DLL), f)).barcodes).toEqual(['RB0000593170']);
  });
});

describe('RenameJob con i lavoratori veri (worker thread)', () => {
  it('rinomina, conta, scrive i log e non tocca gli originali', async () => {
    const t = fresh('job1');
    fs.copyFileSync(sample, path.join(t.input, 'a.pdf'));
    fs.copyFileSync(sample, path.join(t.input, 'B.PDF')); // stesso codice di a.pdf: collisione di nome
    fs.copyFileSync(sample, path.join(t.input, 'è ünï.pdf'));
    vectorBarcodePdf('286524542334', path.join(t.input, 'vettoriale.pdf'));
    blankPdf(path.join(t.input, 'vuoto.pdf'));
    fs.writeFileSync(path.join(t.input, 'rotto.pdf'), 'non sono un pdf');
    fs.writeFileSync(path.join(t.input, 'nota.txt'), 'ignorato');
    fs.copyFileSync(sample, path.join(t.input, 'sotto', 'dentro.pdf')); // le sottocartelle non si leggono

    const lines: string[] = [];
    const job = new RenameJob(params(t), { readerFactory: workerReaderFactory(workerFile, DLL), log: (tag, text) => lines.push(`[${tag}] ${text}`), cpu: () => 10, maxWorkers: 3, progressEveryMs: 50 });
    const before = fs.readFileSync(path.join(t.input, 'a.pdf'));
    const r = await job.run();

    // a, B, è ünï → RB0000593170 (3 nomi con i suffissi _1 _2), vettoriale → 286524542334; vuoto e rotto non letti
    expect(r).toEqual({ ok: 4, unread: 2, errors: 0, aborted: false });
    const out = fs.readdirSync(t.output).sort();
    const base = 'RB0000593170-sorter-CONSEGNATO-000063064-2026-MP297';
    expect(out).toEqual([
      'LOG_DATAMATRIX_NON_LETTI.txt', 'LOG_ERRORI.txt', 'LOG_OK.txt',
      '286524542334-sorter-CONSEGNATO-000063064-2026-MP297.pdf',
      `${base}.pdf`, `${base}_1.pdf`, `${base}_2.pdf`
    ].sort());
    // la copia e' identica all'originale, che e' ancora dov'era
    expect(fs.readFileSync(path.join(t.output, `${base}.pdf`)).equals(before)).toBe(true);
    expect(fs.readFileSync(path.join(t.input, 'a.pdf')).equals(before)).toBe(true);
    expect(fs.readdirSync(t.input).sort()).toEqual(['B.PDF', 'a.pdf', 'nota.txt', 'rotto.pdf', 'sotto', 'vettoriale.pdf', 'vuoto.pdf', 'è ünï.pdf'].sort());
    // i log su file: una riga `<ora>;<file>;<dettaglio>`
    const ok = fs.readFileSync(path.join(t.output, 'LOG_OK.txt'), 'utf8').trim().split(/\r?\n/);
    expect(ok).toHaveLength(4);
    expect(ok[0]).toMatch(/^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3};.+\.pdf;.+\.pdf$/);
    const unread = fs.readFileSync(path.join(t.output, 'LOG_DATAMATRIX_NON_LETTI.txt'), 'utf8');
    expect(unread).toContain('vuoto.pdf;barcode non leggibile');
    expect(unread).toContain('rotto.pdf');
    expect(fs.readFileSync(path.join(t.output, 'LOG_ERRORI.txt'), 'utf8')).toBe('');
    // il registro
    expect(lines[0]).toContain('trovati 6 PDF');
    expect(lines.filter((l) => l.startsWith('[rename]'))).toHaveLength(4);
    expect(lines.filter((l) => l.startsWith('[barcode]'))).toHaveLength(2);
    expect(lines[lines.length - 1]).toContain('fine: ok=4 nonLetti=2 errori=0');
  }, 120000);

  it('una cartella di output che non si puo creare e un errore fatale contato, non un lavoro riuscito con zero file', async () => {
    const t = fresh('job2');
    fs.copyFileSync(sample, path.join(t.input, 'a.pdf'));
    const bloccata = path.join(t.dir, 'file.txt');
    fs.writeFileSync(bloccata, 'x');
    const lines: string[] = [];
    const r = await new RenameJob({ ...params(t), output: path.join(bloccata, 'dentro') }, { readerFactory: workerReaderFactory(workerFile, DLL), log: (g, x) => lines.push(`[${g}] ${x}`) }).run();
    expect(r.errors).toBe(1);
    expect(r.ok).toBe(0);
    expect(lines.some((l) => l.startsWith('[errore] errore fatale'))).toBe(true);
  });

  it('cartella senza PDF: nessun lavoratore, finisce', async () => {
    const t = fresh('job3');
    let created = 0;
    const r = await new RenameJob(params(t), { readerFactory: () => { created++; throw new Error('non deve partire'); }, log: () => {} }).run();
    expect(r).toEqual({ ok: 0, unread: 0, errors: 0, aborted: false });
    expect(created).toBe(0);
  });
});

describe('RenameJob con lavoratori finti: regolazione, errori, interruzione', () => {
  const slow = (ms: number, onRead?: (f: string) => void): PdfReader => ({
    read: async (f) => {
      onRead?.(f);
      await new Promise((r) => setTimeout(r, ms));
      return { barcodes: ['RB' + String(path.basename(f).replace(/\D/g, '')).padStart(10, '0')], lettore: 'finto', note: ['nota ' + path.basename(f)] };
    },
    close: () => {}
  });
  const many = (n: number) => {
    const t = fresh('finto' + n + Math.random().toString(36).slice(2, 6));
    for (let i = 1; i <= n; i++) fs.writeFileSync(path.join(t.input, `p${i}.pdf`), 'x' + i);
    return t;
  };

  it('parte da un lavoratore e cresce finche la CPU e sotto il 90%', async () => {
    const t = many(30);
    let active = 0;
    let peak = 0;
    const factory = () => ({ ...slow(40), read: async (f: string) => { active++; peak = Math.max(peak, active); try { return await slow(40).read(f); } finally { active--; } } });
    const r = await new RenameJob(params(t), { readerFactory: factory, log: () => {}, cpu: () => 20, maxWorkers: 4, progressEveryMs: 30 }).run();
    expect(r.ok).toBe(30);
    expect(peak).toBeGreaterThan(1);
    expect(peak).toBeLessThanOrEqual(4);
  }, 30000);

  it('con la CPU al 100% resta a un lavoratore', async () => {
    const t = many(8);
    let active = 0;
    let peak = 0;
    const factory = () => ({ ...slow(20), read: async (f: string) => { active++; peak = Math.max(peak, active); try { return await slow(20).read(f); } finally { active--; } } });
    const r = await new RenameJob(params(t), { readerFactory: factory, log: () => {}, cpu: () => 100, maxWorkers: 8, progressEveryMs: 30 }).run();
    expect(r.ok).toBe(8);
    expect(peak).toBe(1);
  }, 30000);

  it('un lavoratore che lancia fa un errore su quel file e il lavoro prosegue', async () => {
    const t = many(4);
    const factory = (): PdfReader => ({ read: async (f) => { if (f.endsWith('p2.pdf')) throw new Error('esploso'); return slow(1).read(f); }, close: () => {} });
    const lines: string[] = [];
    const r = await new RenameJob(params(t), { readerFactory: factory, log: (g, x) => lines.push(`[${g}] ${x}`), cpu: () => 0, progressEveryMs: 20 }).run();
    expect(r).toMatchObject({ ok: 3, errors: 1, unread: 0 });
    expect(lines).toContain('[errore] p2.pdf: esploso');
    expect(fs.readFileSync(path.join(t.output, 'LOG_ERRORI.txt'), 'utf8')).toContain('p2.pdf;Error: esploso');
  }, 30000);

  it('interrotto: i file gia presi finiscono (nessuna copia a meta), gli altri non partono', async () => {
    const t = many(40);
    let job: RenameJob;
    let closed = 0;
    const factory = (): PdfReader => ({ ...slow(30, () => job.abort()), close: () => closed++ });
    job = new RenameJob(params(t), { readerFactory: factory, log: () => {}, cpu: () => 0, maxWorkers: 2, progressEveryMs: 20 });
    const r = await job.run();
    expect(r.aborted).toBe(true);
    expect(r.ok).toBeGreaterThan(0);
    expect(r.ok).toBeLessThan(40);
    // ogni PDF scritto e completo (stesso contenuto dell'originale), e tutti i lavoratori sono stati chiusi
    for (const f of fs.readdirSync(t.output).filter((x) => x.endsWith('.pdf'))) expect(fs.statSync(path.join(t.output, f)).size).toBeGreaterThan(1);
    expect(closed).toBeGreaterThan(0);
  }, 30000);

  it('l\'avanzamento arriva a 100% e dice cosa ha in mano ogni lavoratore', async () => {
    const t = many(10);
    const snaps: { done: number; total: number; workers: { name: string; file: string }[] }[] = [];
    await new RenameJob(params(t), { readerFactory: () => slow(25), log: () => {}, cpu: () => 0, maxWorkers: 3, progressEveryMs: 20, onProgress: (p) => snaps.push(p) }).run();
    const last = snaps[snaps.length - 1];
    expect(last).toMatchObject({ done: 10, total: 10, workers: [] });
    expect(snaps.some((s) => s.workers.length > 0 && /^worker-\d+$/.test(s.workers[0].name) && s.workers[0].file.endsWith('.pdf'))).toBe(true);
  }, 30000);
});

describe('ElaborazioneService: lo storico segue il lavoro', () => {
  it('valida, avvia un solo job alla volta, salva la fine e aspetta alla chiusura', async () => {
    const t = fresh('svc');
    for (let i = 1; i <= 6; i++) fs.copyFileSync(sample, path.join(t.input, `s${i}.pdf`));
    const store = new StoricoStore(path.join(t.dir, 'dati'));
    const events: ServiceEvent[] = [];
    const svc = new ElaborazioneService({ store, controlDir: path.join(t.dir, 'dati'), readerVersion: '1.2.1', readerFactory: workerReaderFactory(workerFile, DLL), emit: (e) => events.push(e), cpu: () => 10, progressEveryMs: 50 });
    const form = { input: t.input, output: t.output, esito: 'CONSEGNATO', istat: '63064', anno: '2026', pacco: 'mp297' };

    expect(svc.start({ ...form, anno: '26' })).toMatchObject({ ok: false });
    expect(svc.start({ ...form, input: path.join(t.dir, 'non-esiste') })).toMatchObject({ ok: false, error: expect.stringContaining('Input') });
    expect(svc.start(form)).toEqual({ ok: true });
    expect(svc.running).toBe(true);
    expect(svc.start(form)).toMatchObject({ ok: false, error: expect.stringContaining('già un\'elaborazione') });
    // mentre e in corso lo storico ha la voce «in corso», e lo stato la riporta
    expect(JSON.parse(fs.readFileSync(path.join(t.dir, 'dati', 'elaborazioni.json'), 'utf8'))[0].finitaIl).toBe(0);
    expect(svc.state().running?.elaborazione.pacco).toBe('MP297');

    expect(await svc.waitFinished(60000)).toBe(true);
    expect(svc.running).toBe(false);
    const saved = store.carica();
    expect(saved).toHaveLength(1);
    expect(saved[0]).toMatchObject({ rinominati: 6, nonLetti: 0, errori: 0, interrotta: false, istat: '000063064', pacco: 'MP297' });
    expect(saved[0].finitaIl).toBeGreaterThan(0);
    expect(events.map((e) => e.type)).toEqual(expect.arrayContaining(['started', 'progress', 'log', 'finished', 'history']));
    expect(fs.readdirSync(t.output).filter((f) => f.endsWith('.pdf'))).toHaveLength(6);

    svc.clearHistory();
    expect(store.carica()).toEqual([]);
  }, 120000);

  it('interrompere e chiudere: waitFinished aspetta i file gia presi e lo storico dice «interrotta»', async () => {
    const t = fresh('svc2');
    for (let i = 1; i <= 60; i++) fs.copyFileSync(sample, path.join(t.input, `s${i}.pdf`));
    const store = new StoricoStore(path.join(t.dir, 'dati'));
    const svc = new ElaborazioneService({ store, controlDir: t.dir, readerVersion: '1.2.1', readerFactory: workerReaderFactory(workerFile, DLL), emit: () => {}, cpu: () => 10, progressEveryMs: 50 });
    svc.start({ input: t.input, output: t.output, esito: 'CONSEGNATO', istat: '1', anno: '2026', pacco: 'A' });
    await new Promise((r) => setTimeout(r, 1500));
    svc.stop();
    expect(await svc.waitFinished(60000)).toBe(true);
    const e = store.carica()[0];
    expect(e.interrotta).toBe(true);
    expect(e.rinominati).toBeLessThan(60);
    // nessun PDF troncato: ogni copia pesa quanto l'originale
    const size = fs.statSync(sample).size;
    for (const f of fs.readdirSync(t.output).filter((x) => x.endsWith('.pdf'))) expect(fs.statSync(path.join(t.output, f)).size).toBe(size);
  }, 120000);
});
