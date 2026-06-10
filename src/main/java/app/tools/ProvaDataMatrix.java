package app.tools;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

/**
 * Tentativo mirato sul DataMatrix di pagina 1 (zona alto-destra):
 * scansione a finestre con ingrandimento e soglie diverse.
 */
public final class ProvaDataMatrix {

    private ProvaDataMatrix() {
    }

    public static void main(String[] args) throws Exception {
        Path pdf = Path.of(args[0]);
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage img = renderer.renderImageWithDPI(0, 600, ImageType.GRAY);
            int w = img.getWidth();
            int h = img.getHeight();
            // finestre scorrevoli nel quadrante alto-destra
            for (double xs = 0.70; xs <= 0.88; xs += 0.03) {
                for (double ys = 0.08; ys <= 0.30; ys += 0.03) {
                    for (double size : new double[]{0.10, 0.14, 0.18}) {
                        int x = (int) (w * xs);
                        int y = (int) (h * ys);
                        int ww = (int) (w * size);
                        int hh = (int) (w * size);
                        if (x + ww > w || y + hh > h) {
                            continue;
                        }
                        BufferedImage sub = img.getSubimage(x, y, ww, hh);
                        for (int scale : new int[]{1, 2, 3}) {
                            String r = decode(scale == 1 ? sub : upscale(sub, scale));
                            if (r != null) {
                                System.out.printf("HIT xs=%.2f ys=%.2f size=%.2f scale=%d -> [%s]%n",
                                        xs, ys, size, scale, r);
                                return;
                            }
                        }
                    }
                }
            }
            System.out.println("nessuna decodifica");
        }
    }

    private static BufferedImage upscale(BufferedImage src, int factor) {
        BufferedImage out = new BufferedImage(src.getWidth() * factor, src.getHeight() * factor,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    private static String decode(BufferedImage img) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.DATA_MATRIX));
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(img);
        try {
            Result r = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints);
            return r.getText();
        } catch (Exception e1) {
            try {
                Result r = new MultiFormatReader().decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)), hints);
                return r.getText();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
