package app.tools;

import java.nio.file.Path;

import app.core.PdfRenderer;

/**
 * Renderizza le pagine di un singolo PDF in PNG per ispezione manuale:
 * java -cp rinominaPDF-all.jar app.tools.RenderPagina <file.pdf> <cartellaOut>
 */
public final class RenderPagina {

    private RenderPagina() {
    }

    public static void main(String[] args) throws Exception {
        for (Path png : PdfRenderer.renderToPng(Path.of(args[0]), Path.of(args[1]))) {
            System.out.println(png);
        }
    }
}
