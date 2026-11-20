# rinominaPDF 2.0

Applicazione desktop che apre i PDF di una cartella, legge i barcode al loro interno, seleziona il codice invio giusto e copia i file
nella cartella di output rinominati come:

```
<barcode>-sorter-<esito>-<istat>-<anno>-<pacco>.pdf
RB0002858945-sorter-COMPIUTAGIACENZA-000063064-2026-66555P310.pdf
```

**Dalla 2.0.0 (2026-11-20) è Electron + React + TypeScript, senza Java** (sopra `..\teo-kit`, come callApi, listBigStorage, queryF5 e watchCLI).
La 1.x (Java 22 + JavaFX + jpackage) sta in `legacy-java/` solo come riferimento di migrazione: non si modifica e non entra nel pacchetto.
Non serve più nessuna JVM: l'MSI porta Electron, l'interfaccia e la libreria del lettore.

- **barcode**: letto dentro al PDF (DataMatrix del codice invio, es. `AC0002404591`)
- **sorter**: testo fisso
- **esito**: scelto dall'utente tra COMPIUTAGIACENZA, CONSEGNATO, RIFIUTATO, TRASFERITO, IRREPERIBILE, EMESSOCAD, DECEDUTO, ATTIVITACESSATA
- **istat**: inserito dall'utente, completato a 9 cifre con zeri a sinistra
- **anno**: inserito dall'utente, validato (19xx / 20xx)
- **pacco**: inserito dall'utente (solo lettere e numeri, in maiuscolo)

I PDF originali non si spostano né si modificano mai: si **copiano** col nome nuovo (stessa data di modifica). Due PDF che danno lo stesso nome
prendono i suffissi `_1`, `_2`… Nella cartella di output vengono scritti anche `LOG_OK.txt`, `LOG_ERRORI.txt` e `LOG_DATAMATRIX_NON_LETTI.txt`
(stesso formato della 1.x: `<data ora>;<file>;<dettaglio>`). Si leggono solo i PDF della cartella scelta, non quelli delle sottocartelle.

## La finestra

Quella del kit: barra del titolo propria, **barra di sinistra = storico delle elaborazioni** (data, origine, esito, numeri, pallino d'esito: verde
riuscita, ambra con PDF non letti, rosso errori o interrotta, grigio nessun PDF rinominato; «Nuovo», «Controlla aggiornamenti», «Impostazioni»),
scheda in alto, centro e **pannello in basso** (Console, Network, Performance, Terminal e il **Registro** di rinominaPDF).

- **Nuova elaborazione**: Input, Output, Esito, Istat, Anno, Pacco, con gli stessi controlli e messaggi della 1.x. Un'elaborazione alla volta:
  mentre ne gira una il form si compila ma non parte.
- **Elaborazione in corso**: barra 0–100%, l'**animazione arcade anni '90** (un documento viaggia dalla cartella di input allo scanner e poi
  all'output; gira solo finché la vista è a schermo), «Interrompi» e cosa ha in mano ogni lavoratore (`worker-1 → file.pdf`).
- **Riepilogo** (un clic su una voce dello storico): rinominati, non letti, errori, i dati usati, «Apri cartella di output», «Riusa questi dati».
- **Registro** (pannello in basso): in cima rinominati / non letti / errori, **CPU, RAM** e lavoratori attivi; sotto le righe con l'ora
  (`[rename]`, `[barcode]`, `[errore]`, `[job]`, `[lettore]`). I filtri per categoria **nascondono** le righe; niente scorrimento automatico.
  I PDF non letti e gli errori sono l'unica cosa che spiega un elenco più corto del previsto.
- **Impostazioni → Programma**: il lettore in uso, la cartella dei dati, «Svuota lo storico» (solo l'elenco: i PDF e i LOG restano dove sono).

Chiudere il programma durante un'elaborazione la **interrompe e aspetta** (al massimo 90 s) i file già presi: un lavoratore ucciso a metà di una
copia lascerebbe un PDF troncato con il nome giusto.

**Dati**: `%LOCALAPPDATA%\rinominaPDF\elaborazioni.json` — la stessa cartella e lo stesso file della 1.x (record `iniziataIl`, `finitaIl`,
`input`, `output`, `esito`, `istat`, `anno`, `pacco`, `rinominati`, `nonLetti`, `errori`, `interrotta`; al massimo 200 voci, scritto in modo
atomico). Chi aggiorna ritrova la barra di sinistra; una voce rimasta «in corso» (programma morto a metà) si rilegge interrotta.
**Aggiornamenti**: li fa il kit, all'avvio e ogni sei ore, da `https://rinominapdf.teo.place/scarica/versione.json`.

## Lettura barcode

Un solo lettore: **sorter_barcode.dll**, la libreria C di sorter-barcode (la stessa di SorterNext e SorterBarcode), dalla release pubblica
[lib_sorter_barcode_windows](https://github.com/TeoPlace/lib_sorter_barcode_windows) 1.2.1, in `lib/sorter-barcode/` (versione, sha256 e come si
aggiorna nel suo `LEGGIMI.md`). Viaggia nel pacchetto (`<app>\resources\native`) e si chiama dentro il processo con **koffi** (binding N-API con i
prebuild: niente da ricompilare per Electron; la 1.x usava la Foreign Function API di Java). Legge direttamente le immagini delle pagine del PDF,
circa mezzo secondo a PDF. Se non sa aprire il PDF (codici disegnati come vettori, JBIG2, JPEG 2000) le pagine si **renderizzano a 300 DPI con
pdf.js + @napi-rs/canvas** (era PDFBox) e le si passano i pixel; su un PDF che apre ma dove non trova il codice NON si renderizza (a 300 DPI ne legge
meno che dal file). Un percorso con caratteri fuori dall'ASCII passa da una copia temporanea (la libreria apre con `fopen`, codepage ANSI). Se la
dll manca l'elaborazione si ferma con un errore, invece di dare tutti i PDF per non letti.

Fino alla 1.0.3 i lettori erano Scandit dentro WSL e ZXing: tolti, non c'è nessun lettore di riserva. Misure del 2026-11-12/13 su 300 PDF veri del
sorter (con la dll 1.2.0): 26 rinominati giusti, 3 con un codice sbagliato, 271 non letti, 0,53 s a PDF.

## Multi-thread e risorse

I PDF si leggono in **worker thread** (uno per lavoratore, ognuno con la sua istanza della libreria, che è thread-safe), il processo principale
resta libero per la finestra. Come in sorter-fx si parte con 1 lavoratore e ogni 250 ms se ne aggiunge uno se la CPU è sotto il 90%, altrimenti se ne
toglie uno (minimo 1, massimo il doppio dei processori). **Cambia rispetto alla 1.x: il disco non si misura più** (con OSHI la prima misura
poteva durare quasi un minuto; la CPU è la risorsa che i lavoratori consumano davvero).

## Sviluppo

```
npm install
npm test           # vitest: regole, storico, libreria vera, fallback vettoriale, job con worker veri, interruzione, storico
npm start          # compila il processo principale e l'interfaccia, apre la finestra
powershell -ExecutionPolicy Bypass -File .\build-msi.ps1   # test + tsc + MSI in dist\ + dist\versione.json
```

Per provare senza toccare lo storico vero: `LOCALAPPDATA` su una cartella di prova; `TEO_SCREENSHOT` e `TEO_EVAL` per uno screenshot da script.
I test usano `tests/fixtures/sample.pdf` (un PDF vero del sorter) e generano i PDF a codice vettoriale (un ITF disegnato con rettangoli) e vuoti.
Mancano rispetto alla 1.x i tool `DiagnosticaBarcode`, `RenderPagina`, `RitagliaZona` e `EseguiJob` (in `legacy-java/src/main/java/app/tools`).

## Installer MSI

`build-msi.ps1` (Node 24 e WiX Toolset 3.x; `electron-builder` fa l'MSI): UpgradeCode congelato `6E50594E-22A6-3AC3-A7FB-48CFCA119568`, solo per
macchina (per questo prodotto non esiste un UpgradeCode per utente), versione `2.0.0` maggiore della `1.0.5` distribuita, quindi si installa sopra.
Scrive `dist\rinominaPDF-<versione>.msi` e `dist\versione.json` (senza il blocco `wizard`, che sul server si eredita da quello pubblicato).
Pubblicazione su KVM2: `STANDARD.md`, «Pubblicare un prodotto».
