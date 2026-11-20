# CLAUDE.md — rinominaPDF

**Prima leggi `STANDARD.md`**: versionamento, nomenclatura degli artefatti, `build-msi.ps1` e la sezione «Prodotti Electron — `teo-kit`» valgono per
tutta la cartella e non si ripetono qui. Il `README.md` descrive il prodotto, la finestra e i comandi.

**Dalla 2.0.0 (2026-11-20) rinominaPDF e' Electron + React + TypeScript, senza Java**, sopra `..\teo-kit` (`docs\ARCHITETTURA.md`). La 1.x (Java 22 +
JavaFX + jpackage) sta in `legacy-java\` solo come riferimento di migrazione: non si modifica e non entra nel pacchetto.

## Workspace e regole di non-tocco

- `sorter-fx\2.0.4`, `queryF5` e `teo-kit` sono **riferimenti in sola lettura**.
- I vecchi test-case (`case-input`, `case-output`) non esistono piu' (verificato il 2026-11-12). I PDF veri per le misure: `Desktop\bench-250gb`
  (66.918 PDF gia' rinominati, il barcode atteso e' nel nome). I test del repo usano `tests\fixtures\sample.pdf` e PDF generati.
- Niente commit e niente push da parte di Claude (`STANDARD.md`, regola 1); si pubblica sul server solo con un «pubblica» dell'utente.

## Il progetto

Apre i PDF di una cartella, legge i barcode, sceglie quello giusto e **copia** (mai sposta) i file nella cartella di output come
`<barcode>-sorter-<esito>-<istat>-<anno>-<pacco>.pdf`. Esito scelto fra gli otto di `ESITI`, istat con zeri a sinistra fino a 9 cifre, anno 19xx/20xx,
pacco solo lettere e numeri in maiuscolo. Le regole stanno tutte in `src/shared/rules.ts` (pure, le usano main, worker e interfaccia): **sono il
contratto verso chi lavora i lotti, portate dalla 1.x senza cambiare un carattere** — i test `rules.test.ts` sono quelli di `BarcodeReaderTest`.

## Lettura barcode

- **Un solo lettore: `sorter_barcode.dll`** (libreria di sorter-barcode, release pubblica lib_sorter_barcode_windows 1.2.1, copiata in
  `lib\sorter-barcode\` con LEGGIMI e sha256), chiamata dentro il processo con **koffi** (`src/main/reader/native.ts`). Scandit/WSL e ZXing sono tolti
  (2026-11-13): niente lettore di riserva. Senza dll l'elaborazione si ferma con un errore fatale. La dll viaggia come `extraResources`
  (`<app>\resources\native`).
- La dll apre i file con `fopen` (codepage ANSI): un percorso non ASCII passa da una copia temporanea con percorso ASCII (nella 1.x si renderizzava).
  Per un PDF senza immagini che sa aprire risponde -2 (`ReadError`): allora `barcode.ts` **renderizza le pagine a 300 DPI con pdf.js + @napi-rs/canvas**
  (era PDFBox) e le passa a `sb_read_gray`. **Su un PDF che apre ma senza codice valido NON si renderizza**: a 300 DPI legge meno che dal file.
  Il fallback e' provato in `job.test.ts` con un ITF disegnato come vettori (generato nel test) — non con un PDF vero del sorter.
- Si legge nei **worker thread** (`src/main/worker/reader.ts`, un'istanza della libreria per lavoratore, thread-safe), mai nel processo principale.
  `RenameJob` (`core/job.ts`) parte da 1 lavoratore e ogni 250 ms +1 se la CPU e' sotto il 90%, -1 altrimenti (massimo il doppio dei processori).
  **Il disco non si misura piu'** (con OSHI la prima misura poteva durare quasi un minuto).
- Difetto noto, non del lettore: la ricerca come sottostringa (`INNER_CODES`) prende `OO6986475960` da `...OOOO698647596022` nel DataMatrix di Poste:
  3 nomi sbagliati su 300 con la sola libreria. Conservato (c'e' un test che lo documenta): correggerlo cambia i nomi gia' dati ai lotti.

## Dati e compatibilita' con la 1.x

`%LOCALAPPDATA%\rinominaPDF\elaborazioni.json`, STESSO file e STESSI campi della 1.x (Jackson): `src/main/core/store.ts`. **Non rinominare i campi**
(`iniziataIl`, `finitaIl`, `rinominati`, `nonLetti`, `errori`, `interrotta`…) o chi aggiorna perde la barra di sinistra. Un file rotto = storico vuoto;
una voce rimasta «in corso» (programma morto a meta') si rilegge interrotta. I `LOG_*.txt` nella cartella di output hanno lo stesso formato.

## La finestra

Quella del kit (`AppFrame`) + `src/ui/App.tsx`: form, vista in corso con `Retro.tsx` (l'animazione arcade, su canvas, ~30 fps, solo a vista aperta),
riepilogo, Registro nel pannello in basso (numeri, CPU, RAM, lavoratori; il filtro nasconde per categoria), Impostazioni → «Programma».
Un job alla volta (`ElaborazioneService`). **Chiudere durante un'elaborazione la interrompe e aspetta (max 90 s) i file gia' presi**
(`before-quit` in `app.ts`): un lavoratore ucciso a meta' di `copyFile` lascerebbe un PDF troncato col nome giusto. Niente licenza (strumento gratuito),
niente i18n, un solo tema scelto dal kit. Niente MCP (`MCP_PORT: 0`, come watchCLI).

## Aggiornamento automatico e pubblicazione

Li fa il kit (controllo all'avvio e ogni sei ore, pulsante «Controlla aggiornamenti»; vedi `STANDARD.md`). Storia della pubblicazione sul server,
fino alla 1.0.5 (2026-11-19): `/var/www/scarica/rinominapdf/` con gli MSI e `versione.json` (blocco `wizard` 1.0.13 conservato, manifest precedenti in
`versione.json.bak-<versione>`), vhost `rinominapdf.teo.place`, sito in `sito/` (copia di quello di SorterNext, cambia solo `src/prodotto.ts`, tema
`foresta`), pubblicato in `/var/www/html/rinominapdf/dist/`; nel wizard dal 2026-10-30, **gratuito come SorterNext**.
La 2.0.0 NON e' pubblicata: `build-msi.ps1` scrive `dist\versione.json` senza il blocco `wizard`, che sul server si eredita da quello pubblicato.
Il sito parla ancora di Java/JavaFX? Controllare `sito/src/prodotto.ts` prima di pubblicare.

## Cose da sapere (2.x)

- UpgradeCode `6E50594E-22A6-3AC3-A7FB-48CFCA119568` congelato in `main.js`, `electron-builder-config.js`, `build-msi.ps1` e nella tabella di `STANDARD.md`
  (`tests/identity.test.ts` li confronta). Solo per macchina: non esiste un UpgradeCode per utente.
- Prove senza toccare i dati veri: `LOCALAPPDATA` su una cartella di prova, `TEO_SCREENSHOT`/`TEO_EVAL`.
- L'icona e' disegnata da `gen-icon.ps1` (foglio con barcode su gradiente verde): quella della 1.x (`icon.ico` in radice) arrivava solo a 48 px e
  `electron-builder` ne pretende 256.
- Non provato: installazione dell'MSI sopra la 1.0.5 e aggiornamento dall'app (serve una macchina con la 1.x installata e il server).
