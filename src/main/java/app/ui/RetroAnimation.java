package app.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Animazione leggera stile arcade anni '90: un "documento" pixelato viaggia
 * dalla cartella di input allo scanner (che lampeggia leggendo il barcode)
 * e poi alla cartella di output, su sfondo nero con scanline.
 * Un solo AnimationTimer, ~30 fps, disegno su Canvas: costo trascurabile.
 */
public class RetroAnimation {

    private static final double W = 640;
    private static final double H = 150;

    private final Canvas canvas = new Canvas(W, H);
    private final GraphicsContext g = canvas.getGraphicsContext2D();
    private AnimationTimer timer;
    private long lastFrame;
    private double t;

    public Canvas getCanvas() {
        return canvas;
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastFrame < 33_000_000L) {
                    return; // ~30 fps
                }
                lastFrame = now;
                t += 0.016;
                draw();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void draw() {
        // sfondo CRT
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, W, H);

        double cycle = t % 4.0;            // 4s per documento
        double progress = cycle / 4.0;     // 0..1 da sinistra a destra
        double scanX = W / 2;

        drawFolder(40, H / 2 - 18, Color.web("#39ff14"));
        drawScanner(scanX - 22, H / 2 - 34);
        drawFolder(W - 80, H / 2 - 18, Color.web("#00bfff"));

        // documento in viaggio
        double docX = 70 + progress * (W - 170);
        double docY = H / 2 - 14 + Math.sin(t * 6) * 2;
        drawDocument(docX, docY, progress);

        // raggio dello scanner quando il documento è sotto
        boolean scanning = Math.abs(docX + 10 - scanX) < 26;
        if (scanning && (int) (t * 10) % 2 == 0) {
            g.setStroke(Color.web("#ff2079"));
            g.setLineWidth(2);
            g.strokeLine(scanX, H / 2 - 30, docX + 10, docY + 8);
        }

        // scia di pixel dietro al documento
        g.setFill(Color.web("#39ff14", 0.35));
        for (int i = 1; i <= 4; i++) {
            g.fillRect(docX - i * 9, docY + 10, 4, 4);
        }

        // scanline CRT
        g.setFill(Color.web("#ffffff", 0.04));
        for (int y = 0; y < H; y += 4) {
            g.fillRect(0, y, W, 1);
        }
    }

    private void drawDocument(double x, double y, double progress) {
        g.setFill(Color.WHITE);
        g.fillRect(x, y, 20, 26);
        g.setFill(Color.web("#222"));
        for (int i = 0; i < 3; i++) {
            g.fillRect(x + 3, y + 4 + i * 5, 14, 2);
        }
        // dopo lo scanner il documento "guadagna" il barcode
        if (progress > 0.5) {
            g.setFill(Color.BLACK);
            double bx = x + 3;
            for (int i = 0; i < 7; i++) {
                g.fillRect(bx + i * 2, y + 19, (i % 3 == 0) ? 1.5 : 1, 5);
            }
        }
    }

    private void drawFolder(double x, double y, Color color) {
        g.setFill(color);
        g.fillRect(x, y + 6, 36, 26);
        g.fillRect(x, y, 16, 8);
        g.setFill(Color.BLACK);
        g.fillRect(x + 3, y + 10, 30, 18);
        g.setFill(color.deriveColor(0, 1, 0.6, 1));
        g.fillRect(x + 3, y + 10, 30, 4);
    }

    private void drawScanner(double x, double y) {
        g.setFill(Color.web("#888"));
        g.fillRect(x, y, 44, 10);
        g.setFill(Color.web("#ff2079"));
        g.fillRect(x + 18, y + 3, 8, 4);
        g.setFill(Color.web("#555"));
        g.fillRect(x + 2, y + 10, 4, 14);
        g.fillRect(x + 38, y + 10, 4, 14);
    }
}
