import fs from 'node:fs';
import os from 'node:os';
import type { AppState, Progress, StartResult } from '../../shared/api';
import { fine, inizio, validateForm, type Elaborazione, type JobForm, type LogLine } from '../../shared/rules';
import { RenameJob, type JobDeps } from './job';
import { StoricoStore } from './store';

export type ServiceEvent =
  | { type: 'history'; list: Elaborazione[] }
  | { type: 'started'; elaborazione: Elaborazione }
  | { type: 'progress'; progress: Progress }
  | { type: 'log'; lines: LogLine[] }
  | { type: 'finished'; elaborazione: Elaborazione };

/** CPU del PC in percentuale, dalla differenza fra due letture dei contatori (come getCpuLoad della 1.x). */
export function cpuSampler(): () => number {
  const read = () => os.cpus().reduce((a, c) => ({ idle: a.idle + c.times.idle, total: a.total + c.times.user + c.times.nice + c.times.sys + c.times.idle + c.times.irq }), { idle: 0, total: 0 });
  let last = read();
  let value = 0;
  return () => {
    const now = read();
    const dt = now.total - last.total;
    if (dt > 0) {
      value = 100 * (1 - (now.idle - last.idle) / dt);
      last = now;
    }
    return value;
  };
}

export interface ServiceDeps {
  store: StoricoStore;
  controlDir: string;
  readerVersion: string;
  readerFactory: JobDeps['readerFactory'];
  emit: (e: ServiceEvent) => void;
  cpu?: () => number;
  now?: () => number;
  progressEveryMs?: number;
}

/** Un'elaborazione alla volta: valida, avvia il job, tiene lo storico e il registro, e dice com'e' andata. */
export class ElaborazioneService {
  private history: Elaborazione[];
  private job: RenameJob | null = null;
  private current: Elaborazione | null = null;
  private lastProgress: Progress | null = null;
  private log: LogLine[] = [];
  private pending: LogLine[] = [];
  private finished: Promise<void> = Promise.resolve();
  private flusher: NodeJS.Timeout | null = null;

  constructor(private readonly d: ServiceDeps) {
    this.history = d.store.carica();
  }

  get running() {
    return this.job !== null;
  }

  state(): AppState {
    return {
      history: this.history,
      readerVersion: this.d.readerVersion,
      controlDir: this.d.controlDir,
      running: this.current && this.job ? { elaborazione: this.current, progress: this.lastProgress ?? this.emptyProgress(), log: this.log } : null
    };
  }

  private emptyProgress(): Progress {
    return { done: 0, total: 0, ok: 0, unread: 0, errors: 0, workers: [], cpu: 0, memoryUsedMb: 0, memoryTotalMb: 0, threads: 1, aborting: false };
  }

  /** Righe del registro dall'ora in poi: i lavoratori accodano, l'interfaccia le riceve a blocchi. */
  private addLog(tag: string, text: string) {
    const line: LogLine = { at: (this.d.now ?? Date.now)(), tag, text };
    this.log.push(line);
    this.pending.push(line);
  }

  private flush() {
    if (this.pending.length === 0) return;
    const lines = this.pending;
    this.pending = [];
    this.d.emit({ type: 'log', lines });
  }

  start(form: JobForm): StartResult {
    if (this.job) return { ok: false, error: "C'è già un'elaborazione in corso: se ne può avviare un'altra quando finisce." };
    const v = validateForm(form);
    if (!v.ok) return v;
    const { params } = v;
    if (!fs.existsSync(params.input) || !fs.statSync(params.input).isDirectory()) return { ok: false, error: 'Seleziona una cartella di Input esistente.' };

    const now = this.d.now ?? Date.now;
    const e = inizio(params, now());
    this.current = e;
    this.history = [e, ...this.history];
    this.d.store.salva(this.history);
    this.log = [];
    this.pending = [];
    this.lastProgress = null;
    const job = new RenameJob(params, {
      readerFactory: this.d.readerFactory,
      log: (tag, text) => this.addLog(tag, text),
      cpu: this.d.cpu ?? cpuSampler(),
      progressEveryMs: this.d.progressEveryMs,
      onProgress: (p) => {
        this.lastProgress = p;
        this.d.emit({ type: 'progress', progress: p });
      }
    });
    this.job = job;
    this.d.emit({ type: 'started', elaborazione: e });
    this.d.emit({ type: 'history', list: this.history });
    this.addLog('lavoro', `lettore: sorter_barcode ${this.d.readerVersion}`);
    this.flusher = setInterval(() => this.flush(), 300);
    this.finished = job.run().then((r) => {
      if (this.flusher) clearInterval(this.flusher);
      this.flusher = null;
      // la fine si registra qui, subito: chiudere il programma subito dopo non deve lasciare la voce «in corso»
      const done = fine(e, r.ok, r.unread, r.errors, r.aborted, now());
      this.history = this.history.map((h) => (h === e ? done : h));
      this.d.store.salva(this.history);
      this.flush();
      this.job = null;
      this.current = null;
      this.d.emit({ type: 'finished', elaborazione: done });
      this.d.emit({ type: 'history', list: this.history });
    });
    return { ok: true };
  }

  /** Interrompe: i file gia' presi finiscono (uccidere un lavoratore a meta' di una copia lascerebbe un PDF troncato). */
  stop() {
    this.job?.abort();
  }

  /** Aspetta la fine dell'elaborazione, al massimo `ms`: per la chiusura del programma. */
  async waitFinished(ms: number): Promise<boolean> {
    if (!this.job) return true;
    return Promise.race([this.finished.then(() => true), new Promise<boolean>((r) => setTimeout(() => r(false), ms))]);
  }

  clearHistory() {
    this.history = this.history.filter((h) => h.finitaIl === 0);
    this.d.store.salva(this.history);
    this.d.emit({ type: 'history', list: this.history });
  }
}
