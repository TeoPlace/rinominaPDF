import { app, BrowserWindow, dialog, ipcMain, shell } from 'electron';
import path from 'node:path';
import fs from 'node:fs';
import kitPkg from '@teoplace/kit';
import { CH } from '../shared/api';
import type { JobForm } from '../shared/rules';
import { loadNative } from './reader/native';
import { StoricoStore } from './core/store';
import { ElaborazioneService, cpuSampler } from './core/service';
import { workerReaderFactory } from './core/job';

const { createKit, controlDir } = kitPkg;

let win: BrowserWindow | null = null;
const root = path.join(__dirname, '..', '..');
/** La libreria: in sviluppo in lib\sorter-barcode (la copia pubblica con LEGGIMI e sha256), installata in <app>\resources\native. */
const dll = app.isPackaged ? path.join(process.resourcesPath, 'native', 'sorter_barcode.dll') : path.join(root, 'lib', 'sorter-barcode', 'sorter_barcode.dll');
const workerFile = path.join(__dirname, 'worker', 'reader.js');

async function createWindow() {
  win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 900,
    minHeight: 560,
    frame: false,
    backgroundColor: '#1e1e2e',
    show: false,
    icon: path.join(root, 'resources', 'icons', 'win', 'icon.ico'),
    webPreferences: { preload: require.resolve('@teoplace/kit/preload'), contextIsolation: true, nodeIntegration: false, sandbox: true }
  });
  win.once('ready-to-show', () => win?.show());
  win.on('closed', () => {
    win = null;
  });
  await win.loadFile(path.join(root, 'build', 'ui', 'index.html'));
  return win;
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (win) {
      if (win.isMinimized()) win.restore();
      win.focus();
    }
  });

  app.whenReady().then(async () => {
    const dir = controlDir();
    let kit: any = null;
    const log = (type: 'info' | 'warn' | 'error', message: string) => kit?.log(type, message);
    const send = (channel: string, payload?: unknown) => {
      if (win && !win.isDestroyed()) win.webContents.send(channel, payload);
    };

    let readerVersion = 'non disponibile';
    try {
      readerVersion = loadNative(dll).version();
    } catch (e) {
      // senza libreria l'installazione e' rovinata: l'elaborazione si ferma con un errore fatale, non tutti i PDF «non letti»
      setTimeout(() => log('error', 'sorter_barcode non disponibile: ' + (e instanceof Error ? e.message : String(e)) + ' (reinstalla rinominaPDF)'), 500);
    }

    const service = new ElaborazioneService({
      store: new StoricoStore(dir),
      controlDir: dir,
      readerVersion,
      readerFactory: workerReaderFactory(workerFile, dll),
      cpu: cpuSampler(),
      emit: (e) => {
        if (e.type === 'history') send(CH.evHistory, e.list);
        else if (e.type === 'started') {
          send(CH.evStarted, e.elaborazione);
          log('info', `Elaborazione avviata: ${e.elaborazione.input} → ${e.elaborazione.output} · ${e.elaborazione.esito} · istat ${e.elaborazione.istat} · ${e.elaborazione.anno} · ${e.elaborazione.pacco}`);
        } else if (e.type === 'progress') send(CH.evProgress, e.progress);
        else if (e.type === 'log') send(CH.evLog, e.lines);
        else {
          send(CH.evFinished, e.elaborazione);
          const x = e.elaborazione;
          log(x.errori > 0 ? 'warn' : 'info', `${x.interrotta ? 'Interrotta' : 'Finita'}: ${x.rinominati} rinominati, ${x.nonLetti} non letti, ${x.errori} errori`);
        }
      }
    });

    // ---- IPC del prodotto ------------------------------------------------------------------------------------
    ipcMain.handle(CH.state, () => service.state());
    ipcMain.handle(CH.pickFolder, async (_e, title: string, current: string) => {
      const r = await dialog.showOpenDialog(win!, {
        title,
        defaultPath: typeof current === 'string' && current && fs.existsSync(current) ? current : undefined,
        properties: ['openDirectory', 'createDirectory']
      });
      return r.canceled ? null : r.filePaths[0];
    });
    ipcMain.handle(CH.start, (_e, form: JobForm) => service.start(form));
    ipcMain.handle(CH.stop, () => {
      service.stop();
      return true;
    });
    ipcMain.handle(CH.clearHistory, () => {
      service.clearHistory();
      return true;
    });
    ipcMain.handle(CH.openFolder, (_e, folder: string) => (typeof folder === 'string' && fs.existsSync(folder) ? shell.openPath(folder) : ''));

    kit = createKit({ getWindow: () => win });

    // Chiudere durante un'elaborazione la interrompe e aspetta (al massimo 90 s) i file gia' presi: uccisi a meta' di una copia
    // lascerebbero un PDF troncato con il nome giusto.
    let closing = false;
    app.on('before-quit', (e) => {
      if (closing || !service.running) return;
      e.preventDefault();
      closing = true;
      log('warn', 'Chiusura: interrompo l’elaborazione e aspetto i file già presi…');
      service.stop();
      void service.waitFinished(90_000).then(() => app.exit(0));
    });

    await createWindow();
    kit.watchWindow();
    if (process.env.TEO_SCREENSHOT) {
      setTimeout(async () => {
        if (process.env.TEO_EVAL) {
          try {
            const r = await win!.webContents.executeJavaScript(process.env.TEO_EVAL);
            if (r !== undefined) console.log('TEO_EVAL ->', typeof r === 'string' ? r : JSON.stringify(r));
          } catch (e) {
            console.log('TEO_EVAL error', e);
          }
          await new Promise((r) => setTimeout(r, Number(process.env.TEO_EVAL_WAIT || 2500)));
        }
        const img = await win!.webContents.capturePage();
        fs.writeFileSync(process.env.TEO_SCREENSHOT!, img.toPNG());
        app.exit(0);
      }, Number(process.env.TEO_SCREENSHOT_DELAY || 3500));
    }
  });
  app.on('window-all-closed', () => app.quit());
}
