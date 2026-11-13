# sorter_barcode.dll

Il lettore dei barcode di rinominaPDF: la libreria C di sorter-barcode, la stessa di SorterNext e
SorterBarcode, presa **cosi' com'e'** dalla release pubblica
[lib_sorter_barcode_windows](https://github.com/TeoPlace/lib_sorter_barcode_windows) (licenza MIT, in
`LICENSE` qui accanto) e chiamata dentro il processo con la Foreign Function API di Java 22
(`src/main/java/app/core/SorterBarcode.java`).

| | |
|---|---|
| versione (`sb_version()`) | **1.2.0** |
| da | release `v1.2.0`, `lib_sorter_barcode_windows.zip` (sha256 `c529b1762b180615be7ca39892d8876826ee0e86d898abea9241fbeab8cc1fc1`, verificato), file `bin\x64\sorter_barcode.dll` |
| sha256 della dll | `8495c91a4b5413b3e170a54c2844effd8e11ea0d7d9b3f49858eeeb815e1a529` |

## Come viaggia

`build-msi.ps1` la copia accanto al fat-jar in `target\jpackage-input`, quindi finisce in
`<installazione>\app\sorter_barcode.dll`: si installa, si aggiorna e si disinstalla con rinominaPDF.
Non si prende dalla cartella di SorterBarcode ne' dal `sorter-barcode` nel PATH: un aggiornamento di
quelli cambierebbe il lettore di rinominaPDF a sua insaputa. In sviluppo (`mvn javafx:run`, i test) si
carica da qui; la proprieta' `-Drinominapdf.sorterbarcode=<file>` la sostituisce per una prova.

## Come si aggiorna

Con un gesto apposta: si scarica la release nuova, se ne verifica lo sha256 con il `.sha256` della
release, si copia qui `bin\x64\sorter_barcode.dll` (rinominaPDF e' a 64 bit), si aggiorna la tabella
sopra e si committano dll e LEGGIMI insieme. `SorterBarcodeTest` controlla che la dll si carichi e legga.
