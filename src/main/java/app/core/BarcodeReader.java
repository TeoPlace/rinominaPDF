package app.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lettura dei barcode di un PDF e scelta del codice giusto.
 *
 * <p>Dalla 1.0.4 il lettore e' uno solo, <b>sorter_barcode</b> ({@link SorterBarcode}): la libreria C
 * di sorter-barcode, la stessa di SorterNext, dentro il processo. Legge direttamente le immagini delle
 * pagine del PDF (mezzo secondo a PDF). Se non sa aprire il PDF (codici disegnati come vettori, JBIG2,
 * JPEG 2000: per lei e' un file illeggibile) o il percorso ha caratteri che la dll non puo' aprire, le
 * pagine si renderizzano con PDFBox e le si passano i pixel. Su un PDF che apre ma dove non trova il
 * codice non si renderizza: a 300 DPI ne legge meno che dal file (1 contro 4 su 60).
 *
 * <p>Fino alla 1.0.3 i lettori erano Scandit dentro WSL (dove c'era) e ZXing: tolti entrambi. Misure
 * del 2026-11-12/13 su 300 PDF veri del sorter ({@code bench-250gb}): sorter_barcode 26 rinominati
 * giusti e 3 sbagliati (0,53 s a PDF), Scandit 50 letti;
 * ZXing 0 su 11, con 23-90 secondi a PDF.
 */
public final class BarcodeReader {

    private BarcodeReader() {
    }

    /** Codici letti da un PDF e come sono stati letti. */
    public record Lettura(List<String> barcodes, String lettore) {
    }

    /**
     * Codici cercati come sottostringa del testo di un 2D, che puo' contenere dati aggiuntivi.
     * Solo le famiglie con lettere: un 12 o 17 cifre preso a meta' di una sequenza di cifre
     * piu' lunga sarebbe un codice inventato.
     */
    private static final List<Pattern> INNER_CODES = List.of(
            Pattern.compile("[A-Z]{2}\\d{10}"),
            Pattern.compile("A[BC]W\\d{8}(?!\\d)"));

    /**
     * Legge un PDF con sorter_barcode.
     *
     * @throws IllegalStateException se la dll non c'e' (installazione rovinata): non e' un PDF non letto
     */
    public static Lettura read(Path pdf) {
        if (!SorterBarcode.available()) {
            throw new IllegalStateException("lettore non disponibile: " + SorterBarcode.loadError());
        }
        List<String> barcodes = new ArrayList<>();
        String lettore = "sorter_barcode";
        try {
            boolean render = !SorterBarcode.canPass(pdf);
            if (!render) {
                try {
                    addAll(barcodes, SorterBarcode.readFile(pdf, false));
                } catch (SorterBarcode.ReadException e) {
                    // nessuna immagine che la libreria sappia aprire: ci pensa PDFBox
                    render = true;
                }
            }
            if (render) {
                lettore = "sorter_barcode (pagine renderizzate)";
                for (BufferedImage page : PdfRenderer.renderPages(pdf, Globals.RENDER_DPI)) {
                    byte[] px = ((DataBufferByte) page.getRaster().getDataBuffer()).getData();
                    addAll(barcodes, SorterBarcode.readGray(px, page.getWidth(), page.getHeight(), false));
                    if (selectRight(barcodes) != null) {
                        break;
                    }
                }
            }
        } catch (SorterBarcode.ReadException | IOException | RuntimeException e) {
            LogQueue.add("lettore", "sorter_barcode su " + pdf.getFileName() + ": " + e.getMessage());
        }
        return new Lettura(barcodes, lettore);
    }

    /** Il lettore di questo PC, per Impostazioni e per il registro. */
    public static String describeReader() {
        return SorterBarcode.available()
                ? "sorter_barcode " + SorterBarcode.version()
                : "sorter_barcode non disponibile (" + SorterBarcode.loadError() + ")";
    }

    private static void addAll(List<String> into, List<String> raw) {
        for (String r : raw) {
            addClean(into, r);
        }
    }

    /** Pulizia barcode identica a barcodeClean di sorter-fx. */
    static String clean(String barcode) {
        String ret = barcode.replace("-BACK", "").replace(" ", "").replace("\"", "");
        Matcher m = Pattern.compile("([0-9a-zA-Z]+)/.+").matcher(barcode);
        if (m.find()) {
            ret = m.group(1);
        }
        return ret.trim();
    }

    private static void addClean(List<String> list, String raw) {
        String barcode = clean(raw);
        if (!barcode.isBlank() && !list.contains(barcode)) {
            list.add(barcode);
        }
    }

    /**
     * Seleziona il barcode giusto nell'ordine in cui sono stati trovati: prima un match esatto di una
     * delle {@link Globals#BARCODE_FAMILIES}, famiglia per famiglia nell'ordine di preferenza, poi la
     * ricerca come sottostringa (i 2D possono contenere dati aggiuntivi).
     */
    public static String selectRight(List<String> barcodes) {
        for (Pattern family : Globals.BARCODE_FAMILIES) {
            for (String b : barcodes) {
                if (family.matcher(b).matches()) {
                    return b;
                }
            }
        }
        for (String b : barcodes) {
            String inner = innerCode(b);
            if (inner != null) {
                return inner;
            }
        }
        return null;
    }

    private static String innerCode(String barcode) {
        for (Pattern p : INNER_CODES) {
            Matcher m = p.matcher(barcode);
            if (m.find()) {
                return m.group();
            }
        }
        return null;
    }
}
