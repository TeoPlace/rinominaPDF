package app.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import app.core.BarcodeReader;

/**
 * Tool da riga di comando per verificare la lettura barcode senza UI, con lo stesso lettore
 * dell'elaborazione (sorter_barcode):
 * java --enable-native-access=ALL-UNNAMED -cp rinominaPDF-all.jar app.tools.DiagnosticaBarcode <cartellaPdf>
 */
public final class DiagnosticaBarcode {

    private DiagnosticaBarcode() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso: DiagnosticaBarcode <cartellaPdf>");
            System.exit(1);
        }
        Path dir = Path.of(args[0]);
        System.out.println("lettore: " + BarcodeReader.describeReader());
        try (Stream<Path> s = Files.list(dir)) {
            for (Path pdf : s.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).sorted().toList()) {
                long start = System.currentTimeMillis();
                BarcodeReader.Lettura l = BarcodeReader.read(pdf);
                System.out.printf("%s | %d ms | %s | trovati=%s | scelto=%s%n",
                        pdf.getFileName(), System.currentTimeMillis() - start, l.lettore(), l.barcodes(),
                        BarcodeReader.selectRight(l.barcodes()));
            }
        }
    }
}
