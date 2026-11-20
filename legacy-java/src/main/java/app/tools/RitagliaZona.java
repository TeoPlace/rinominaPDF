package app.tools;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Salva un ritaglio di una pagina PDF come PNG per ispezione.
 * java ... app.tools.RitagliaZona <pdf> <pagina1based> <dpi> <x%> <y%> <w%> <h%> <out.png>
 */
public final class RitagliaZona {

    private RitagliaZona() {
    }

    public static void main(String[] args) throws Exception {
        Path pdf = Path.of(args[0]);
        int page = Integer.parseInt(args[1]) - 1;
        int dpi = Integer.parseInt(args[2]);
        double xp = Double.parseDouble(args[3]) / 100;
        double yp = Double.parseDouble(args[4]) / 100;
        double wp = Double.parseDouble(args[5]) / 100;
        double hp = Double.parseDouble(args[6]) / 100;
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            BufferedImage img = new PDFRenderer(doc).renderImageWithDPI(page, dpi, ImageType.GRAY);
            int x = (int) (img.getWidth() * xp);
            int y = (int) (img.getHeight() * yp);
            int w = (int) (img.getWidth() * wp);
            int h = (int) (img.getHeight() * hp);
            ImageIO.write(img.getSubimage(x, y, w, h), "png", Path.of(args[7]).toFile());
            System.out.println(args[7] + " " + w + "x" + h);
        }
    }
}
