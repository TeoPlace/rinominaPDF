package app.ui;

import app.AppInfo;
import app.core.BarcodeReader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * "Impostazioni" in fondo alla barra di sinistra. Il programma non ha niente da regolare: dice con
 * cosa sta leggendo e dove tiene i suoi dati, e lascia svuotare lo storico.
 */
public final class ImpostazioniPane {

    private final VBox root = new VBox(16);

    public ImpostazioniPane(AppSession session, Path controlDir, Runnable storicoCambiato) {
        Label title = new Label("Impostazioni");
        title.getStyleClass().add("rp-view-title");

        Label lettore = new Label("Verifica in corso…");
        Thread verifica = new Thread(() -> {
            // Fino a 20 secondi la prima volta, e il risultato resta: mai sul thread FX.
            boolean wsl = BarcodeReader.isWslAvailable();
            Platform.runLater(() -> lettore.setText(wsl
                    ? "WSL con Scandit"
                    : "ZXing, incluso nel programma (piu' lento: WSL con Scandit non e' disponibile)"));
        }, "rinominapdf-verifica-lettore");
        verifica.setDaemon(true);
        verifica.start();

        Label versione = new Label(AppInfo.VERSION);
        Label dati = new Label(controlDir.toString());
        lettore.setWrapText(true);
        dati.setWrapText(true);

        GridPane griglia = new GridPane();
        griglia.setHgap(14);
        griglia.setVgap(8);
        griglia.addRow(0, nome("Lettore di barcode"), lettore);
        griglia.addRow(1, nome("Cartella dei dati"), dati);
        griglia.addRow(2, nome("Versione"), versione);

        Button apri = new Button("Apri cartella dei dati");
        apri.setGraphic(Icons.of("folder-open", 14));
        apri.setDisable(!Files.isDirectory(controlDir));
        apri.setOnAction(e -> Browse.apriCartella(controlDir));

        Button svuota = new Button("Svuota lo storico");
        svuota.setGraphic(Icons.of("delete", 14));
        svuota.getStyleClass().add("rp-danger-button");
        svuota.setOnAction(e -> {
            if (conferma(root.getScene().getWindow())) {
                session.svuotaStorico();
                storicoCambiato.run();
            }
        });

        root.setPadding(new Insets(24));
        root.getChildren().addAll(title, griglia, new HBox(10, apri, svuota));
    }

    private static Label nome(String testo) {
        Label l = new Label(testo);
        l.getStyleClass().add("rp-muted");
        l.setMinWidth(Label.USE_PREF_SIZE);
        return l;
    }

    private static boolean conferma(Window owner) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Si toglie solo l'elenco qui a sinistra. I PDF rinominati e i file LOG nelle"
                        + " cartelle di output restano dove sono.",
                ButtonType.YES, ButtonType.NO);
        a.initOwner(owner);
        a.setTitle("Svuota lo storico");
        a.setHeaderText(null);
        Dialogs.decorate(a);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    public Parent getRoot() {
        return root;
    }
}
