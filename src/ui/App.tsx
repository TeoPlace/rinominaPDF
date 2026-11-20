import { useCallback, useEffect, useRef, useState } from 'react';
import { AppFrame, getTeo, type SidebarItem } from '@teoplace/kit/ui';
import { IconCopy, IconFolderOpen, IconListDetails, IconPlayerPlay, IconPlayerStop, IconSettings, IconTrash } from '@tabler/icons-react';
import { CH, type AppState, type Progress, type StartResult } from '../shared/api';
import { ESITI, LOG_LABEL, categoryOfTag, statoOf, validateForm, type Elaborazione, type JobForm, type LogCategory, type LogLine, type Stato } from '../shared/rules';
import { Retro } from './Retro';
import './rp.css';

const teo = () => getTeo();
const invoke = <T,>(channel: string, ...args: unknown[]) => teo().invoke<T>(channel, ...args);

const DOT: Record<Stato, string> = { IN_CORSO: 'var(--accent)', RIUSCITA: 'var(--green)', NON_LETTI: '#e5b567', VUOTA: 'var(--muted)', ERRORE: 'var(--danger)' };
const TITOLO: Record<Stato, (e: Elaborazione) => string> = {
  IN_CORSO: () => 'In corso',
  RIUSCITA: () => 'Completata',
  NON_LETTI: () => 'Completata, con PDF non letti',
  VUOTA: () => 'Nessun PDF rinominato',
  ERRORE: (e) => (e.interrotta ? 'Interrotta' : 'Completata, con errori')
};
const base = (p: string) => p.replace(/[\\/]+$/, '').split(/[\\/]/).pop() || p;
const quando = (t: number) => new Date(t).toLocaleString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
const hms = (t: number) => {
  const d = new Date(t);
  const p = (n: number, w = 2) => String(n).padStart(w, '0');
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${p(d.getMilliseconds(), 3)}`;
};

const EMPTY_FORM: JobForm = { input: '', output: '', esito: '', istat: '', anno: '', pacco: '' };
const noProgress: Progress = { done: 0, total: 0, ok: 0, unread: 0, errors: 0, workers: [], cpu: 0, memoryUsedMb: 0, memoryTotalMb: 0, threads: 1, aborting: false };

type View = { kind: 'setup' } | { kind: 'running' } | { kind: 'summary'; e: Elaborazione };

export function App() {
  const [state, setState] = useState<AppState | null>(null);
  const [view, setView] = useState<View>({ kind: 'setup' });
  const [form, setForm] = useState<JobForm>(EMPTY_FORM);
  const [error, setError] = useState('');
  const [running, setRunning] = useState<Elaborazione | null>(null);
  const [progress, setProgress] = useState<Progress>(noProgress);
  const [log, setLog] = useState<LogLine[]>([]);
  const [hidden, setHidden] = useState<Set<LogCategory>>(new Set());
  const [confirmClear, setConfirmClear] = useState(false);
  const viewRef = useRef(view);
  viewRef.current = view;

  useEffect(() => {
    let alive = true;
    invoke<AppState>(CH.state).then((s) => {
      if (!alive) return;
      setState(s);
      if (s.running) {
        setRunning(s.running.elaborazione);
        setProgress(s.running.progress);
        setLog(s.running.log);
        setView({ kind: 'running' });
      }
    });
    const t = teo();
    const off = [
      t.on(CH.evHistory, (list: Elaborazione[]) => setState((s) => (s ? { ...s, history: list } : s))),
      t.on(CH.evStarted, (e: Elaborazione) => {
        setRunning(e);
        setProgress(noProgress);
        setLog([]);
        setView({ kind: 'running' });
      }),
      t.on(CH.evProgress, (p: Progress) => setProgress(p)),
      t.on(CH.evLog, (lines: LogLine[]) => setLog((prev) => prev.concat(lines))),
      t.on(CH.evFinished, (e: Elaborazione) => {
        setRunning(null);
        // se l'utente guardava l'elaborazione, ora vede com'e' andata
        if (viewRef.current.kind === 'running') setView({ kind: 'summary', e });
      })
    ];
    return () => {
      alive = false;
      off.forEach((f) => f());
    };
  }, []);

  const set = (k: keyof JobForm) => (v: string) => setForm((f) => ({ ...f, [k]: v }));
  const browse = async (k: 'input' | 'output', title: string) => {
    const dir = await invoke<string | null>(CH.pickFolder, title, form[k]);
    if (dir) set(k)(dir);
  };

  const start = async () => {
    const v = validateForm(form);
    if (!v.ok) return setError(v.error);
    const r = await invoke<StartResult>(CH.start, form);
    setError(r.ok ? '' : r.error);
  };

  const reuse = useCallback((e: Elaborazione) => {
    setForm({ input: e.input, output: e.output, esito: e.esito, istat: e.istat, anno: e.anno, pacco: e.pacco });
    setError('');
    setView({ kind: 'setup' });
  }, []);

  const items: SidebarItem[] = (state?.history ?? []).map((e) => ({
    id: String(e.iniziataIl),
    title: `${base(e.input)} · ${e.esito}`,
    subtitle: e.finitaIl === 0 ? `${quando(e.iniziataIl)} · in corso` : `${quando(e.iniziataIl)} · ${e.rinominati} rinominati${e.nonLetti ? ` · ${e.nonLetti} non letti` : ''}${e.errori ? ` · ${e.errori} errori` : ''}`,
    dot: DOT[statoOf(e)]
  }));
  const activeId = view.kind === 'summary' ? String(view.e.iniziataIl) : view.kind === 'running' && running ? String(running.iniziataIl) : null;
  const select = (id: string) => {
    const e = state?.history.find((h) => String(h.iniziataIl) === id);
    if (!e) return;
    setView(e.finitaIl === 0 && running ? { kind: 'running' } : { kind: 'summary', e });
  };

  // ---- registro (pannello in basso) -------------------------------------------------------------------------
  const registerTab = {
    id: 'register',
    label: 'Registro',
    icon: <IconListDetails size={16} stroke={1.5} />,
    render: () => (
      <div className="rp-register">
        <div className="rp-reg-head">
          <span style={{ color: 'var(--green)' }}><b>{progress.ok}</b> rinominati</span>
          <span style={{ color: '#e5b567' }}><b>{progress.unread}</b> non letti</span>
          <span style={{ color: 'var(--danger)' }}><b>{progress.errors}</b> errori</span>
          <span>CPU <b>{progress.cpu.toFixed(0)}%</b></span>
          <span>RAM <b>{(progress.memoryUsedMb / 1024).toFixed(1)}</b> / {(progress.memoryTotalMb / 1024).toFixed(1)} GB</span>
          <span><b>{progress.workers.length}</b> lavoratori attivi (limite {progress.threads})</span>
          <span className="rp-spacer" />
          {(Object.keys(LOG_LABEL) as LogCategory[]).map((c) => (
            <label key={c} className="rp-chip" title="Nasconde la categoria">
              <input type="checkbox" checked={!hidden.has(c)} onChange={() => setHidden((h) => { const n = new Set(h); if (n.has(c)) n.delete(c); else n.add(c); return n; })} />
              {LOG_LABEL[c]}
            </label>
          ))}
        </div>
        <div className="rp-reg-lines">
          {log.length === 0 && <div className="rp-reg-empty">Le righe di un'elaborazione compaiono qui: i PDF non letti e gli errori sono l'unica cosa che spiega un elenco piu' corto del previsto.</div>}
          {log.filter((l) => !hidden.has(categoryOfTag(l.tag))).map((l, i) => (
            <div key={i} className={'rp-line ' + categoryOfTag(l.tag)}>
              <span className="rp-time">{hms(l.at)}</span>
              <span className="rp-tag">[{l.tag}]</span>
              <span className="rp-text">{l.text}</span>
            </div>
          ))}
        </div>
      </div>
    )
  };

  const settingsPage = {
    id: 'program',
    label: 'Programma',
    icon: <IconSettings size={16} stroke={1.5} />,
    render: () => (
      <div style={{ padding: 16, display: 'grid', gap: 12, maxWidth: 640 }}>
        <div className="rp-kv"><span>Lettore di barcode</span><b>sorter_barcode {state?.readerVersion}</b></div>
        <div className="rp-kv"><span>Cartella dei dati</span><b>{state?.controlDir}</b></div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="teo-btn" onClick={() => invoke(CH.openFolder, state?.controlDir)}><IconFolderOpen size={14} /> Apri cartella dei dati</button>
          <button className="teo-btn" style={{ borderColor: 'var(--danger)', color: 'var(--danger)' }} disabled={!state?.history.some((h) => h.finitaIl !== 0)} onClick={() => setConfirmClear(true)}><IconTrash size={14} /> Svuota lo storico</button>
        </div>
        <div className="teo-muted">Non c'e' niente da regolare: i PDF si leggono con sorter_barcode e i numeri di lavoratori si adattano da soli alla CPU.</div>
      </div>
    )
  };

  const abort = () => invoke(CH.stop);

  return (
    <AppFrame
      sidebar={{ items, activeId, onSelect: select, onNew: () => setView({ kind: 'setup' }), emptyText: 'Nessuna elaborazione ancora.' }}
      tab={{ label: view.kind === 'setup' ? 'Nuova elaborazione' : view.kind === 'running' ? 'Elaborazione in corso' : base(view.e.input), italic: view.kind === 'setup' }}
      dockTabs={[registerTab]}
      settings={{ extraPages: [settingsPage] }}
    >
      <div className="rp-main">
        {view.kind === 'setup' && (
          <div className="rp-pane">
            <h2>Nuova elaborazione</h2>
            <div className="teo-muted">Rinomina i PDF leggendo il barcode al loro interno</div>
            <div className="rp-grid">
              <label>Input</label>
              <div className="rp-row"><input className="rp-input" placeholder="Cartella con i PDF da elaborare" value={form.input} onChange={(e) => set('input')(e.target.value)} /><button className="teo-btn" onClick={() => browse('input', 'Cartella di input')}>Sfoglia…</button></div>
              <label>Output</label>
              <div className="rp-row"><input className="rp-input" placeholder="Cartella dove salvare i PDF rinominati" value={form.output} onChange={(e) => set('output')(e.target.value)} /><button className="teo-btn" onClick={() => browse('output', 'Cartella di output')}>Sfoglia…</button></div>
              <label>Esito</label>
              <select className="rp-input" value={form.esito} onChange={(e) => set('esito')(e.target.value)}>
                <option value="">Seleziona esito</option>
                {ESITI.map((x) => <option key={x}>{x}</option>)}
              </select>
              <label>Istat</label>
              <input className="rp-input" placeholder="Codice istat (solo cifre, completato a 9 con zeri)" value={form.istat} onChange={(e) => set('istat')(e.target.value)} />
              <label>Anno</label>
              <input className="rp-input" placeholder="Anno, es. 2026" value={form.anno} onChange={(e) => set('anno')(e.target.value)} />
              <label>Pacco</label>
              <input className="rp-input" placeholder="Identificativo pacco, es. MP297" value={form.pacco} onChange={(e) => set('pacco')(e.target.value)} />
            </div>
            <div className="teo-muted">Esempio risultato: RB0002858945-sorter-COMPIUTAGIACENZA-000063064-2026-66555P310.pdf</div>
            {error && <div className="rp-error">{error}</div>}
            {running && <div className="teo-muted">C'è già un'elaborazione in corso: se ne può avviare un'altra quando finisce.</div>}
            <button className="teo-btn primary rp-start" disabled={running !== null} onClick={start}><IconPlayerPlay size={16} /> Avvia</button>
          </div>
        )}

        {view.kind === 'running' && running && (
          <div className="rp-pane">
            <h2>Elaborazione in corso</h2>
            <div className="teo-muted">{running.esito} · istat {running.istat} · {running.anno} · {running.pacco}</div>
            <Retro />
            <div className="rp-progress-row">
              <div className="rp-progress"><div style={{ width: `${progress.total ? (100 * progress.done) / progress.total : 0}%` }} /></div>
              <span className="rp-percent">{progress.total ? Math.round((100 * progress.done) / progress.total) : 0}%</span>
            </div>
            <div className="teo-muted">{progress.total ? `${progress.done} / ${progress.total} file${progress.aborting ? ' · interruzione in corso…' : ''}` : 'In preparazione…'}</div>
            <div><button className="teo-btn" style={{ borderColor: 'var(--danger)', color: 'var(--danger)' }} disabled={progress.aborting} onClick={abort}><IconPlayerStop size={15} /> Interrompi</button></div>
            <pre className="rp-workers">{progress.workers.slice(0, 8).map((w) => `${w.name} → ${w.file}`).join('\n')}{progress.workers.length > 8 ? `\n… e altri ${progress.workers.length - 8}` : ''}</pre>
          </div>
        )}

        {view.kind === 'summary' && (
          <div className="rp-pane">
            <h2>{TITOLO[statoOf(view.e)](view.e)}</h2>
            <div className="teo-muted">{quando(view.e.iniziataIl)}</div>
            <div className="rp-kpis">
              <div className="rp-kpi ok"><b>{view.e.rinominati}</b><span>Rinominati</span></div>
              <div className="rp-kpi warn"><b>{view.e.nonLetti}</b><span>Non letti</span></div>
              <div className="rp-kpi crit"><b>{view.e.errori}</b><span>Errori</span></div>
            </div>
            <div className="rp-grid2">
              {([['Input', view.e.input], ['Output', view.e.output], ['Esito', view.e.esito], ['Istat', view.e.istat], ['Anno', view.e.anno], ['Pacco', view.e.pacco]] as const).map(([k, v]) => (
                <div key={k} style={{ display: 'contents' }}><span className="teo-muted">{k}</span><span>{v}</span></div>
              ))}
            </div>
            <div className="teo-muted">Nella cartella di output: LOG_OK.txt, LOG_DATAMATRIX_NON_LETTI.txt e LOG_ERRORI.txt, con il dettaglio di ogni PDF.</div>
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="teo-btn" onClick={() => invoke(CH.openFolder, view.e.output)}><IconFolderOpen size={14} /> Apri cartella di output</button>
              <button className="teo-btn" onClick={() => reuse(view.e)}><IconCopy size={14} /> Riusa questi dati</button>
            </div>
          </div>
        )}
        {confirmClear && (
          <div className="rp-veil" onClick={() => setConfirmClear(false)}>
            <div className="rp-dialog" onClick={(e) => e.stopPropagation()}>
              <h3>Svuota lo storico</h3>
              <p>Si toglie solo l'elenco qui a sinistra. I PDF rinominati e i file LOG nelle cartelle di output restano dove sono.</p>
              <div className="rp-actions">
                <button className="teo-btn" onClick={() => setConfirmClear(false)}>No</button>
                <button className="teo-btn primary" onClick={() => { setConfirmClear(false); void invoke(CH.clearHistory); setView((v) => (v.kind === 'summary' ? { kind: 'setup' } : v)); }}>Sì</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AppFrame>
  );
}
