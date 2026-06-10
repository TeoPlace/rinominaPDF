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

Metodo primario (lo stesso di sorter-fx): le pagine vengono renderizzate a
PNG (PDFBox, 300 DPI) e lette da **Scandit dentro WSL**:

```
wsl /srv/scandit_multiple/it.smaart.sorter.stock /srv/scandit_multiple/test.key /srv/scandit_multiple/codes_ALL.settings /mnt/c/...png
```

Se WSL o Scandit non sono disponibili sulla macchina, l'app passa
automaticamente al lettore di riserva **ZXing** in-process (600 DPI, con
scansione a finestre per il DataMatrix degradato delle scansioni fax);
l'utente lo vede segnalato nel log-panel.

## Multi-thread e risorse

Stesso meccanismo di sorter-fx: si parte con 1 thread e ogni ciclo di
controllo aggiunge un thread se CPU < 90% e disco < 90%, altrimenti ne toglie
uno (minimo 1). CPU/disco/memoria sono monitorati con OSHI e mostrati nel
log-panel insieme allo stato dei thread.

## Sviluppo

```
mvn javafx:run        # avvio in sviluppo
mvn package           # fat-jar: target/rinominaPDF-1.0.0-all.jar
```

Tool diagnostici da riga di comando (senza UI):

```
java -cp target\rinominaPDF-1.0.0-all.jar app.tools.DiagnosticaBarcode <cartellaPdf> [--wsl]
java -cp target\rinominaPDF-1.0.0-all.jar app.tools.RenderPagina <file.pdf> <cartellaOut>
```

## Installer MSI

Stesso metodo di queryF5 (fat-jar + jpackage, JVM inclusa: l'utente finale
non deve installare ne' Java ne' JavaFX):

```
.\build-msi.ps1
```

Richiede (solo per chi compila): JDK 22, Maven, WiX Toolset 3.x.
