package app.core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Renderizza le pagine di un PDF (PDFBox): in memoria per sorter_barcode quando il PDF non ha immagini
 * che la libreria sa aprire, e in PNG per lo strumento RenderPagina.
 */
public final class PdfRenderer {

    private PdfRenderer() {
    }

    /** Renderizza tutte le pagine in memoria ai DPI richiesti. */
    public static List<BufferedImage> renderPages(Path pdf, int dpi) throws IOException {
        List<BufferedImage> pages = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                pages.add(renderer.renderImageWithDPI(i, dpi, ImageType.GRAY));
            }
        }
        return pages;
    }

    /**
     * Salva le pagine come PNG nella cartella indicata e restituisce i percorsi creati
     * (per guardare a occhio cosa vede il lettore: app.tools.RenderPagina).
     */
    public static List<Path> renderToPng(Path pdf, Path tempDir) throws IOException {
        Files.createDirectories(tempDir);
        List<Path> out = new ArrayList<>();
        List<BufferedImage> pages = renderPages(pdf, Globals.RENDER_DPI);
        String base = pdf.getFileName().toString().replaceAll("(?i)\\.pdf$", "");
        for (int i = 0; i < pages.size(); i++) {
            Path png = tempDir.resolve(base + "_p" + (i + 1) + ".png");
            ImageIO.write(pages.get(i), "png", png.toFile());
            out.add(png);
        }
        return out;
    }
}
