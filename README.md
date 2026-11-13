# rinominaPDF

Applicazione desktop (JavaFX) che apre i PDF di una cartella, legge i barcode
al loro interno, seleziona il codice invio giusto e copia i file nella
cartella di output rinominati come:

```
<barcode>-sorter-<esito>-<istat>-<anno>-<pacco>.pdf
RB0002858945-sorter-COMPIUTAGIACENZA-000063064-2026-66555P310.pdf
```

- **barcode**: letto dentro al PDF (DataMatrix del codice invio, es. `AC0002404591`)
- **sorter**: testo fisso
- **esito**: scelto dall'utente tra COMPIUTAGIACENZA, CONSEGNATO, RIFIUTATO,
  TRASFERITO, IRREPERIBILE, EMESSOCAD, DECEDUTO, ATTIVITACESSATA
- **istat**: inserito dall'utente, completato a 9 cifre con zeri a sinistra
- **anno**: inserito dall'utente, validato (4 cifre)
- **pacco**: inserito dall'utente

Nella cartella di output vengono scritti anche `LOG_OK.txt`, `LOG_ERRORI.txt`
e `LOG_DATAMATRIX_NON_LETTI.txt`.

## Lettura barcode

Dalla 1.0.4 il lettore e' uno solo (`app/core/BarcodeReader`):
**sorter_barcode.dll**, la libreria C di sorter-barcode (la stessa di
SorterNext), dalla release pubblica
[lib_sorter_barcode_windows](https://github.com/TeoPlace/lib_sorter_barcode_windows)
1.2.0, in `lib/sorter-barcode/` (versione, sha256 e come si aggiorna nel suo
`LEGGIMI.md`). Viaggia nel pacchetto accanto al jar e si chiama dentro il
processo con la Foreign Function API (`app/core/SorterBarcode`): legge
direttamente le immagini delle pagine del PDF, circa mezzo secondo a PDF. Se
non sa aprire il PDF (codici disegnati come vettori, JBIG2, JPEG 2000) o il
percorso ha caratteri fuori dalla codepage ANSI, le pagine si renderizzano con
PDFBox a 300 DPI e le si passano i pixel. Se la dll manca l'elaborazione si
ferma con un errore, invece di dare tutti i PDF per non letti.

Fino alla 1.0.3 i lettori erano Scandit dentro WSL (dove c'era) e ZXing: tolti
tutti e due, non c'e' piu' nessun lettore di riserva.

Misure del 2026-11-12/13 su 300 PDF veri del sorter (1 bit, CCITT G4), presi
a caso da `Desktop\bench-250gb` con il barcode atteso nel nome: la 1.0.4 ne
rinomina 26 giusti, 3 con un codice sbagliato e 271 restano non letti, in
media 0,53 s a PDF. Sugli stessi PDF Scandit ne leggeva 50 (1,1 s a PDF);
ZXing nessuno su 11, con 23-90 secondi a PDF.

## Multi-thread e risorse

Stesso meccanismo di sorter-fx: si parte con 1 thread e ogni ciclo di
controllo aggiunge un thread se CPU < 90% e disco < 90%, altrimenti ne toglie
uno (minimo 1). CPU/disco/memoria sono monitorati con OSHI e mostrati nel
log-panel insieme allo stato dei thread.

## Sviluppo

```
mvn javafx:run        # avvio in sviluppo
mvn package           # fat-jar: target/rinominaPDF-<versione>-all.jar
```

Tool diagnostici da riga di comando (senza UI), lanciati dalla radice del repo
cosi' trovano `lib\sorter-barcode\sorter_barcode.dll`:

```
java --enable-native-access=ALL-UNNAMED -cp target\rinominaPDF-1.0.4-all.jar app.tools.DiagnosticaBarcode <cartellaPdf>
java -cp target\rinominaPDF-1.0.4-all.jar app.tools.RenderPagina <file.pdf> <cartellaOut>
```

## Installer MSI

Stesso metodo di queryF5 (fat-jar + jpackage, JVM inclusa: l'utente finale
non deve installare ne' Java ne' JavaFX):

```
.\build-msi.ps1
```

Richiede (solo per chi compila): JDK 22, Maven, WiX Toolset 3.x.
