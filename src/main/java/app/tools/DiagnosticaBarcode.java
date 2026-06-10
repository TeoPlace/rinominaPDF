package app.tools;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import app.core.BarcodeReader;
import app.core.PdfRenderer;

/**
 * Tool da riga di comando per verificare la lettura barcode senza UI:
 * java -cp rinominaPDF-all.jar app.tools.DiagnosticaBarcode <cartellaPdf> [--wsl]
 */
public final class DiagnosticaBarcode {

    private DiagnosticaBarcode() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso: DiagnosticaBarcode <cartellaPdf> [--wsl]");
            System.exit(1);
        }
        Path dir = Path.of(args[0]);
        boolean useWsl = args.length > 1 && "--wsl".equals(args[1]);
        if (useWsl && !BarcodeReader.isWslAvailable()) {
            System.err.println("WSL/Scandit non disponibile, uso ZXing");
            useWsl = false;
        }
        Path tempDir = Files.createTempDirectory("diagbarcode");

        try (Stream<Path> s = Files.list(dir)) {
            for (Path pdf : s.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).sorted().toList()) {
                List<String> barcodes = new ArrayList<>();
                long start = System.currentTimeMillis();
                if (useWsl) {
                    for (Path png : PdfRenderer.renderToPng(pdf, tempDir)) {
                        barcodes.addAll(BarcodeReader.readWithWsl(png));
                        Files.deleteIfExists(png);
                    }
                } else {
                    for (BufferedImage page : PdfRenderer.renderPages(pdf, app.core.Globals.RENDER_DPI_ZXING)) {
                        barcodes.addAll(BarcodeReader.readWithZxing(page));
                        if (BarcodeReader.selectRight(barcodes) != null) {
                            break;
                        }
                    }
                }
                String selected = BarcodeReader.selectRight(barcodes);
                System.out.printf("%s | %d ms | trovati=%s | scelto=%s%n",
                        pdf.getFileName(), System.currentTimeMillis() - start, barcodes, selected);
            }
        }
    }
}
