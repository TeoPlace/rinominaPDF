package app.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La vera {@code sorter_barcode.dll} di {@code lib/sorter-barcode}, chiamata come la chiama il programma.
 * I PDF si generano qui con un ITF disegnato dal test (12 cifre, una famiglia di
 * {@link Globals#BARCODE_FAMILIES}): nessun PDF vero, con indirizzi di persone, nel repo.
 */
class SorterBarcodeTest {

    private static final String CODE = "286524542334";

    @Test
    void laDllSiCaricaEDaLaSuaVersione() {
        assertTrue(SorterBarcode.available(), () -> "dll non caricata: " + SorterBarcode.loadError());
        assertEquals("1.2.0", SorterBarcode.version());
    }

    @Test
    void leggeUnPdfFattoDiImmagini(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("immagine.pdf");
        pdfWithImage(pdf);
        assertTrue(SorterBarcode.readFile(pdf, false).contains(CODE));

        BarcodeReader.Lettura l = BarcodeReader.read(pdf);
        assertEquals(CODE, BarcodeReader.selectRight(l.barcodes()));
        assertEquals("sorter_barcode", l.lettore());
    }

    /** Un PDF senza immagini per la libreria e' illeggibile (-2): si renderizza la pagina. */
    @Test
    void unPdfSenzaImmaginiSiLeggeDallaPaginaRenderizzata(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("vettori.pdf");
        pdfWithVectors(pdf);
        assertThrows(SorterBarcode.ReadException.class, () -> SorterBarcode.readFile(pdf, false));

        BarcodeReader.Lettura l = BarcodeReader.read(pdf);
        assertEquals(CODE, BarcodeReader.selectRight(l.barcodes()));
        assertEquals("sorter_barcode (pagine renderizzate)", l.lettore());
    }

    @Test
    void leggeIPixelInMemoria() throws Exception {
        BufferedImage img = itfImage();
        byte[] px = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        assertEquals(List.of(CODE), SorterBarcode.readGray(px, img.getWidth(), img.getHeight(), false));
    }

    /** La dll apre i file con la codepage ANSI: un nome fuori codepage passa dalla pagina renderizzata. */
    @Test
    void unPercorsoFuoriCodepageSiLeggeLoStesso(@TempDir Path dir) throws Exception {
        Path pdf = dir.resolve("日本-ж.pdf");
        pdfWithImage(pdf);
        assertFalse(SorterBarcode.canPass(pdf));
        assertTrue(SorterBarcode.canPass(dir.resolve("normale.pdf")));

        BarcodeReader.Lettura l = BarcodeReader.read(pdf);
        assertEquals(CODE, BarcodeReader.selectRight(l.barcodes()));
        assertEquals("sorter_barcode (pagine renderizzate)", l.lettore());
    }

    @Test
    void unFileCheNonEsisteEUnErrore(@TempDir Path dir) {
        SorterBarcode.ReadException e = assertThrows(SorterBarcode.ReadException.class,
                () -> SorterBarcode.readFile(dir.resolve("manca.pdf"), false));
        assertTrue(e.getMessage().contains("-2"), e.getMessage());
    }

    @Test
    void valoriDalTestoDellaLibreria() {
        String out = "[\n{\"type\":\"code128\",\"value\":\"RB0002627784\"},\n"
                + "{\"type\":\"data-matrix\",\"value\":\"A\\\"B\\\\C\\u0041\"},\n"
                + "{\"type\":\"code128\",\"value\":\"RB0002627784\"}\n]";
        assertEquals(List.of("RB0002627784", "A\"B\\CA"), SorterBarcode.parseValues(out));
        assertEquals(List.of(), SorterBarcode.parseValues("no 1d or 2d barcodes found\n[\n]"));
    }

    // --- PDF di prova --------------------------------------------------------------------------

    private static final String[] ITF = {
            "nnwwn", "wnnnw", "nwnnw", "wwnnn", "nnwnw", "wnwnn", "nwwnn", "nnnww", "wnnwn", "nwnwn"};
    private static final int NARROW = 4;
    private static final int WIDE = 12;
    private static final int QUIET = 60;
    private static final int HEIGHT = 220;

    /** Larghezze alternate barra/spazio dell'ITF di {@link #CODE}, partendo da una barra. */
    private static List<Integer> itfWidths() {
        List<Integer> w = new ArrayList<>(List.of(NARROW, NARROW, NARROW, NARROW));
        for (int i = 0; i < CODE.length(); i += 2) {
            String bars = ITF[CODE.charAt(i) - '0'];
            String spaces = ITF[CODE.charAt(i + 1) - '0'];
            for (int k = 0; k < 5; k++) {
                w.add(bars.charAt(k) == 'w' ? WIDE : NARROW);
                w.add(spaces.charAt(k) == 'w' ? WIDE : NARROW);
            }
        }
        w.addAll(List.of(WIDE, NARROW, NARROW));
        return w;
    }

    private static BufferedImage itfImage() {
        List<Integer> widths = itfWidths();
        int total = widths.stream().mapToInt(Integer::intValue).sum();
        BufferedImage img = new BufferedImage(total + 2 * QUIET, HEIGHT + 2 * QUIET, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(Color.BLACK);
        int x = QUIET;
        for (int i = 0; i < widths.size(); i++) {
            if (i % 2 == 0) {
                g.fillRect(x, QUIET, widths.get(i), HEIGHT);
            }
            x += widths.get(i);
        }
        g.dispose();
        return img;
    }

    private static void pdfWithImage(Path pdf) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            BufferedImage img = itfImage();
            PDImageXObject x = LosslessFactory.createFromImage(doc, img);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // 300 dpi: un pixel = 72/300 punti
                float s = 72f / 300f;
                cs.drawImage(x, 60, 500, img.getWidth() * s, img.getHeight() * s);
            }
            doc.save(pdf.toFile());
        }
    }

    private static void pdfWithVectors(Path pdf) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float s = 72f / 300f;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(0f);
                List<Integer> widths = itfWidths();
                float x = 60;
                for (int i = 0; i < widths.size(); i++) {
                    if (i % 2 == 0) {
                        cs.addRect(x, 500, widths.get(i) * s, HEIGHT * s);
                    }
                    x += widths.get(i) * s;
                }
                cs.fill();
            }
            doc.save(pdf.toFile());
        }
    }
}
