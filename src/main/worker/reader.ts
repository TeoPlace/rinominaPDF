import { parentPort, workerData } from 'node:worker_threads';
import { loadNative } from '../reader/native';
import { readPdf, type Lettura } from '../reader/barcode';

/** Un lavoratore: carica la sua istanza della libreria e legge i PDF che il padre gli manda, uno alla volta. */
export interface WorkerJob {
  id: number;
  file: string;
}
export type WorkerReply = { id: number; ok: true; lettura: Lettura } | { id: number; ok: false; error: string };

const native = loadNative((workerData as { dll: string }).dll);

parentPort!.on('message', async (job: WorkerJob) => {
  let reply: WorkerReply;
  try {
    reply = { id: job.id, ok: true, lettura: await readPdf(native, job.file) };
  } catch (e) {
    reply = { id: job.id, ok: false, error: e instanceof Error ? e.message : String(e) };
  }
  parentPort!.postMessage(reply);
});
