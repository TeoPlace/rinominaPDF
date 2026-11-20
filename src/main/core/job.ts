import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { Worker } from 'node:worker_threads';
import type { Progress } from '../../shared/api';
import { buildFileName, selectRight, type JobParams, type LogLine } from '../../shared/rules';
import type { Lettura } from '../reader/barcode';
import type { WorkerJob, WorkerReply } from '../worker/reader';
import { FileLogs } from './fileLogs';

/** Un lavoratore che legge un PDF alla volta. */
export interface PdfReader {
  read(file: string): Promise<Lettura>;
  close(): void;
}

/** I lavoratori veri: un worker thread con la sua istanza della libreria. */
export function workerReaderFactory(workerFile: string, dll: string): () => PdfReader {
  return () => {
    const w = new Worker(workerFile, { workerData: { dll } });
    let pending: { resolve: (l: Lettura) => void; reject: (e: Error) => void } | null = null;
    let seq = 0;
    w.on('message', (m: WorkerReply) => {
      const p = pending;
      pending = null;
      if (!p) return;
      if (m.ok) p.resolve(m.lettura);
      else p.reject(new Error(m.error));
    });
    w.on('error', (e) => {
      const p = pending;
      pending = null;
      p?.reject(e);
    });
    return {
      read: (file) =>
        new Promise((resolve, reject) => {
          pending = { resolve, reject };
          w.postMessage({ id: ++seq, file } satisfies WorkerJob);
        }),
      close: () => void w.terminate()
    };
  };
}

export interface JobDeps {
  readerFactory: () => PdfReader;
  /** righe del registro (tag: job, lettore, barcode, rename, errore) */
  log: (tag: string, text: string) => void;
  onTotal?: (total: number) => void;
  onProgress?: (p: Progress) => void;
  /** misure del PC per il registro e per regolare i lavoratori (CPU in percentuale) */
  cpu?: () => number;
  maxWorkers?: number;
  progressEveryMs?: number;
}

export interface JobResult {
  ok: number;
  unread: number;
  errors: number;
  aborted: boolean;
}

/**
 * Orchestratore della rinomina: coda di PDF e lavoratori a numero dinamico regolato sulla CPU (stesso meccanismo di
 * sorter-fx: +1 se la CPU e' sotto il 90%, -1 altrimenti, minimo 1). Dalla 2.0 il disco non si misura piu': nella 1.x
 * (OSHI) la prima misura poteva durare quasi un minuto, e la CPU e' la risorsa che i lavoratori consumano davvero.
 * Il PDF originale non si sposta ne' si modifica mai: si copia col nome nuovo.
 */
export class RenameJob {
  private aborted = false;
  private workers = new Map<string, string>();
  private ok = 0;
  private unread = 0;
  private errors = 0;
  private done = 0;
  private total = 0;
  private limit = 1;
  private readonly reserved = new Set<string>();

  constructor(private readonly params: JobParams, private readonly deps: JobDeps) {}

  abort() {
    this.aborted = true;
  }

  async run(): Promise<JobResult> {
    const { params, deps } = this;
    const log = deps.log;
    const maxWorkers = Math.max(1, deps.maxWorkers ?? Math.max(2, os.cpus().length * 2));
    const readers: { name: string; reader: PdfReader; busy: boolean }[] = [];
    let timer: NodeJS.Timeout | null = null;
    let logs: FileLogs | null = null;
    try {
      logs = new FileLogs(params.output);
      const files = fs
        .readdirSync(params.input, { withFileTypes: true })
        .filter((e) => e.isFile() && e.name.toLowerCase().endsWith('.pdf'))
        .map((e) => e.name)
        .sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
      this.total = files.length;
      deps.onTotal?.(this.total);
      log('job', `trovati ${this.total} PDF in ${params.input}`);
      if (this.total === 0) return this.result();

      const queue = files.slice();
      let inFlight = 0;
      const emit = () => deps.onProgress?.(this.snapshot());
      timer = setInterval(() => {
        this.adjust(deps.cpu?.() ?? 0, maxWorkers);
        emit();
      }, deps.progressEveryMs ?? 250);

      await new Promise<void>((resolve) => {
        const pump = () => {
          while (!this.aborted && inFlight < this.limit && queue.length > 0) {
            const file = queue.shift()!;
            let slot = readers.find((r) => !r.busy);
            if (!slot) {
              slot = { name: `worker-${readers.length + 1}`, reader: deps.readerFactory(), busy: false };
              readers.push(slot);
            }
            slot.busy = true;
            inFlight++;
            const s = slot;
            this.workers.set(s.name, file);
            this.processOne(s.reader, file, logs!).finally(() => {
              s.busy = false;
              inFlight--;
              this.workers.delete(s.name);
              this.done++;
              emit();
              pump();
            });
          }
          if (inFlight === 0 && (queue.length === 0 || this.aborted)) resolve();
        };
        // il limite cresce a ogni misura: parte da 1 e si ferma dove la CPU lo consente
        const grow = setInterval(() => {
          if (inFlight === 0 && (queue.length === 0 || this.aborted)) clearInterval(grow);
          else pump();
        }, 100);
        pump();
      });
    } catch (e) {
      // conta come errore: senza, una cartella di output non scrivibile finirebbe nello storico come riuscita con zero file
      this.errors++;
      const msg = e instanceof Error ? e.message : String(e);
      log('errore', `errore fatale: ${msg}`);
      logs?.error('JOB', e);
    } finally {
      if (timer) clearInterval(timer);
      for (const r of readers) r.reader.close();
      this.workers.clear();
      log('job', `fine: ok=${this.ok} nonLetti=${this.unread} errori=${this.errors}${this.aborted ? ' (interrotto)' : ''}`);
      deps.onProgress?.(this.snapshot());
    }
    return this.result();
  }

  private result(): JobResult {
    return { ok: this.ok, unread: this.unread, errors: this.errors, aborted: this.aborted };
  }

  /** +1 lavoratore se la CPU e' sotto il 90%, -1 altrimenti, mai meno di 1 ne' piu' del massimo. */
  private adjust(cpu: number, max: number) {
    if (cpu < 90) this.limit = Math.min(max, this.limit + 1);
    else if (this.limit > 1) this.limit--;
  }

  private snapshot(): Progress {
    const total = os.totalmem();
    return {
      done: this.done,
      total: this.total,
      ok: this.ok,
      unread: this.unread,
      errors: this.errors,
      workers: [...this.workers.entries()].sort(([a], [b]) => a.localeCompare(b, 'en', { numeric: true })).map(([name, file]) => ({ name, file })),
      cpu: this.deps.cpu?.() ?? 0,
      memoryUsedMb: (total - os.freemem()) / 1048576,
      memoryTotalMb: total / 1048576,
      threads: this.limit,
      aborting: this.aborted
    };
  }

  private async processOne(reader: PdfReader, fileName: string, logs: FileLogs) {
    const { params, deps } = this;
    try {
      const lettura = await reader.read(path.join(params.input, fileName));
      for (const n of lettura.note) deps.log('lettore', n);
      const barcode = selectRight(lettura.barcodes);
      if (barcode === null) {
        this.unread++;
        logs.unread(fileName, `barcode non leggibile (${lettura.lettore}), letti: [${lettura.barcodes.join(', ')}]`);
        deps.log('barcode', `${fileName} -> NON LETTO con ${lettura.lettore} (trovati: [${lettura.barcodes.join(', ')}])`);
        return;
      }
      const target = this.uniqueTarget(buildFileName(params, barcode));
      const source = path.join(params.input, fileName);
      await fs.promises.copyFile(source, target, fs.constants.COPYFILE_EXCL);
      const st = await fs.promises.stat(source);
      await fs.promises.utimes(target, st.atime, st.mtime); // come COPY_ATTRIBUTES della 1.x
      this.ok++;
      const name = path.basename(target);
      logs.ok(fileName, name);
      deps.log('rename', `${fileName} -> ${name}`);
    } catch (e) {
      this.errors++;
      logs.error(fileName, e);
      deps.log('errore', `${fileName}: ${e instanceof Error ? e.message : String(e)}`);
    }
  }

  /** Evita collisioni se due PDF producono lo stesso nome (anche fra due che finiscono insieme). */
  private uniqueTarget(name: string): string {
    const stem = name.replace(/\.pdf$/i, '');
    let target = path.join(this.params.output, name);
    let n = 1;
    while (this.reserved.has(target.toLowerCase()) || fs.existsSync(target)) {
      target = path.join(this.params.output, `${stem}_${n}.pdf`);
      n++;
    }
    this.reserved.add(target.toLowerCase());
    return target;
  }
}

export type { LogLine };
