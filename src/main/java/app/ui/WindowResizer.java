package app.ui;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/** Disattivato quando lo stage e' massimizzato via property "rp-maximized" impostata da Shell. */
public final class WindowResizer {

    private static final double MARGIN = 6;

    private final Stage stage;
    private Cursor zone = Cursor.DEFAULT;
    private boolean resizing;
    private double pressScreenX, pressScreenY;
    private double startX, startY, startW, startH;

    private WindowResizer(Stage stage) {
        this.stage = stage;
    }

    public static void install(Stage stage, Scene scene) {
        WindowResizer r = new WindowResizer(stage);
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, r::onMoved);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, r::onPressed);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, r::onDragged);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, r::onReleased);
    }

    private boolean maximized() {
        return Boolean.TRUE.equals(stage.getProperties().get("rp-maximized"));
    }

    private void onMoved(MouseEvent e) {
        if (resizing || maximized()) return;
        Scene scene = stage.getScene();
        double x = e.getSceneX(), y = e.getSceneY();
        double w = scene.getWidth(), h = scene.getHeight();
        boolean left = x < MARGIN, right = x > w - MARGIN;
        boolean top = y < MARGIN, bottom = y > h - MARGIN;

        Cursor c = Cursor.DEFAULT;
        if (top && left) c = Cursor.NW_RESIZE;
        else if (top && right) c = Cursor.NE_RESIZE;
        else if (bottom && left) c = Cursor.SW_RESIZE;
        else if (bottom && right) c = Cursor.SE_RESIZE;
        else if (left) c = Cursor.W_RESIZE;
        else if (right) c = Cursor.E_RESIZE;
        else if (top) c = Cursor.N_RESIZE;
        else if (bottom) c = Cursor.S_RESIZE;

        if (c != zone) {
            zone = c;
            scene.setCursor(c);
        }
    }

    private void onPressed(MouseEvent e) {
        if (maximized() || zone == Cursor.DEFAULT) return;
        resizing = true;
        pressScreenX = e.getScreenX();
        pressScreenY = e.getScreenY();
        startX = stage.getX();
        startY = stage.getY();
        startW = stage.getWidth();
        startH = stage.getHeight();
        e.consume();
    }

    private void onDragged(MouseEvent e) {
        if (!resizing) return;
        double dx = e.getScreenX() - pressScreenX;
        double dy = e.getScreenY() - pressScreenY;
        double minW = Math.max(1, stage.getMinWidth());
        double minH = Math.max(1, stage.getMinHeight());

        if (zone == Cursor.E_RESIZE || zone == Cursor.NE_RESIZE || zone == Cursor.SE_RESIZE) {
            stage.setWidth(Math.max(minW, startW + dx));
        }
        if (zone == Cursor.S_RESIZE || zone == Cursor.SW_RESIZE || zone == Cursor.SE_RESIZE) {
            stage.setHeight(Math.max(minH, startH + dy));
        }
        if (zone == Cursor.W_RESIZE || zone == Cursor.NW_RESIZE || zone == Cursor.SW_RESIZE) {
            double w = Math.max(minW, startW - dx);
            stage.setX(startX + (startW - w));
            stage.setWidth(w);
        }
        if (zone == Cursor.N_RESIZE || zone == Cursor.NW_RESIZE || zone == Cursor.NE_RESIZE) {
            double h = Math.max(minH, startH - dy);
            stage.setY(startY + (startH - h));
            stage.setHeight(h);
        }
        e.consume();
    }

    private void onReleased(MouseEvent e) {
        if (resizing) {
            resizing = false;
            zone = Cursor.DEFAULT;
            stage.getScene().setCursor(Cursor.DEFAULT);
        }
    }
}
