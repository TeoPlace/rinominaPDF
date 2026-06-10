package app.tools;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.Binarizer;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;

/**
 * Esperimenti di decodifica: DPI multipli, crop dei quadranti, due binarizer.
 * java ... app.tools.ProvaDecodifica <file.pdf>
 */
public final class ProvaDecodifica {

    private ProvaDecodifica() {
    }

    public static void main(String[] args) throws Exception {
        Path pdf = Path.of(args[0]);
        int[] dpis = {300, 450, 600};
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int dpi : dpis) {
                for (int p = 0; p < doc.getNumberOfPages(); p++) {
                    BufferedImage img = renderer.renderImageWithDPI(p, dpi, ImageType.GRAY);
                    tryRegions(img, "p" + (p + 1) + "@" + dpi);
                }
            }
        }
    }

    private static void tryRegions(BufferedImage img, String label) throws IOException {
        int w = img.getWidth();
        int h = img.getHeight();
        // pagina intera + quadranti + fasce orizzontali (i moduli CAN/CAD sono affiancati)
        int[][] regions = {
                {0, 0, w, h},
                {w / 2, 0, w / 2, h / 2},          // alto-destra
                {0, 0, w / 2, h / 2},              // alto-sinistra
                {0, h / 2, w / 2, h / 2},          // basso-sinistra
                {w / 2, h / 2, w / 2, h / 2},      // basso-destra
                {0, 0, w / 2, h},                  // metà sinistra
                {w / 2, 0, w / 2, h},              // metà destra
                {2 * w / 3, 0, w / 3, h / 4},      // angolo alto-destra stretto
        };
        for (int i = 0; i < regions.length; i++) {
            int[] r = regions[i];
            BufferedImage sub = img.getSubimage(r[0], r[1], r[2], r[3]);
            decode(sub, label + " r" + i);
        }
    }

    private static void decode(BufferedImage img, String label) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(
                BarcodeFormat.DATA_MATRIX, BarcodeFormat.CODE_128, BarcodeFormat.CODE_39,
                BarcodeFormat.ITF, BarcodeFormat.QR_CODE, BarcodeFormat.AZTEC, BarcodeFormat.PDF_417));
        LuminanceSource source = new BufferedImageLuminanceSource(img);
        Binarizer[] binarizers = {new HybridBinarizer(source), new GlobalHistogramBinarizer(source)};
        for (Binarizer b : binarizers) {
            try {
                GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(new MultiFormatReader());
                for (Result r : reader.decodeMultiple(new BinaryBitmap(b), hints)) {
                    System.out.println(label + " " + b.getClass().getSimpleName()
                            + " " + r.getBarcodeFormat() + " -> [" + r.getText() + "]");
                }
            } catch (Exception ignored) {
            }
        }
    }
}
