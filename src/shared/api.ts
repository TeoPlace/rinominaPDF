import type { Elaborazione, JobForm, LogLine } from './rules';

/** Canali IPC e forme dei messaggi fra il processo principale e l'interfaccia. Prefisso `rp:` (quello `teo:` e' del kit). */
export const CH = {
  state: 'rp:state',
  pickFolder: 'rp:pick-folder',
  start: 'rp:start',
  stop: 'rp:stop',
  clearHistory: 'rp:clear-history',
  openFolder: 'rp:open-folder',
  // eventi
  evHistory: 'rp:ev-history',
  evStarted: 'rp:ev-started',
  evProgress: 'rp:ev-progress',
  evLog: 'rp:ev-log',
  evFinished: 'rp:ev-finished'
} as const;

export interface Progress {
  done: number;
  total: number;
  ok: number;
  unread: number;
  errors: number;
  /** cosa ha in mano ogni lavoratore: «worker-1» → nome del file */
  workers: { name: string; file: string }[];
  /** risorse del PC, per il registro */
  cpu: number;
  memoryUsedMb: number;
  memoryTotalMb: number;
  threads: number;
  aborting: boolean;
}

export interface AppState {
  history: Elaborazione[];
  readerVersion: string;
  controlDir: string;
  /** l'elaborazione in corso, se c'e' */
  running: { elaborazione: Elaborazione; progress: Progress; log: LogLine[] } | null;
}

export type StartResult = { ok: true } | { ok: false; error: string };
export type { JobForm };
