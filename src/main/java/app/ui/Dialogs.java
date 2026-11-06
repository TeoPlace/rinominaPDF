package app.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Va chiamato su OGNI {@code Dialog}/{@code Alert}, prima di mostrarlo (vedi STANDARD.md): un
 * dialogo ha una {@code Scene} propria e non eredita il foglio di stile della finestra principale.
 * Da listBigStorage: stessa barra del titolo nostra, con la chiusura e il trascinamento.
 */
public final class Dialogs {

    private static final String CSS = Dialogs.class.getResource("/css/app.css").toExternalForm();

    private Dialogs() {}

    public static <T, D extends Dialog<T>> D decorate(D d) {
        d.initStyle(StageStyle.UNDECORATED);

        DialogPane pane = d.getDialogPane();
        if (!pane.getStylesheets().contains(CSS)) pane.getStylesheets().add(CSS);
        if (!pane.getStyleClass().contains("rp-window")) pane.getStyleClass().add("rp-window");
        if (pane.getScene() != null && pane.getScene().getWindow() instanceof Stage s) {
            s.getIcons().setAll(icone());
        }

        Label title = new Label();
        title.textProperty().bind(d.titleProperty());
        title.getStyleClass().add("rp-title-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button();
        close.setGraphic(Icons.of("close", 12));
        close.getStyleClass().addAll("rp-win-button", "close");
        close.setFocusTraversable(false);
        close.setOnAction(e -> requestClose(d));

        ImageView ic = new ImageView(icone()[2]);
        ic.setFitHeight(14);
        ic.setPreserveRatio(true);

        HBox bar = new HBox(8, ic, title, spacer, close);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 0, 12));
        bar.setMinHeight(30);
        bar.getStyleClass().add("rp-titlebar");

        final double[] drag = new double[2];
        bar.setOnMousePressed(e -> {
            Window w = pane.getScene().getWindow();
            drag[0] = e.getScreenX() - w.getX();
            drag[1] = e.getScreenY() - w.getY();
        });
        bar.setOnMouseDragged(e -> {
            Window w = pane.getScene().getWindow();
            w.setX(e.getScreenX() - drag[0]);
            w.setY(e.getScreenY() - drag[1]);
        });

        // setHeader() sostituisce il disegno nativo di headerText: va rifatto qui.
        Label header = new Label();
        header.textProperty().bind(pane.headerTextProperty());
        header.setWrapText(true);
        header.getStyleClass().add("rp-dialog-header");
        header.visibleProperty().bind(header.textProperty().isNotEmpty());
        header.managedProperty().bind(header.visibleProperty());
        header.setMaxWidth(Double.MAX_VALUE);

        pane.setHeader(new VBox(bar, header));
        return d;
    }

    /** Dialog.close() non fa niente senza un bottone di annullo (i Si'/No): si preme quello negativo. */
    private static void requestClose(Dialog<?> d) {
        d.close();
        if (!d.isShowing()) return;
        for (ButtonType bt : d.getDialogPane().getButtonTypes()) {
            ButtonBar.ButtonData data = bt.getButtonData();
            if (data == ButtonBar.ButtonData.NO || data.isCancelButton()) {
                if (d.getDialogPane().lookupButton(bt) instanceof Button b) {
                    b.fire();
                    return;
                }
            }
        }
    }

    /** Le stesse della finestra principale, estratte da icon.ico (JavaFX non legge gli .ico). */
    static Image[] icone() {
        int[] lati = {16, 32, 48};
        Image[] out = new Image[lati.length];
        for (int i = 0; i < lati.length; i++) {
            out[i] = new Image(Dialogs.class.getResource("/img/app-icon-" + lati[i] + ".png").toExternalForm());
        }
        return out;
    }
}
