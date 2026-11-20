package app.ui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import app.core.Elaborazione;
import app.core.Globals;
import app.core.JobParams;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * "Nuova elaborazione": Input, Output, Esito, Istat, Anno, Pacco. Validazione alla pressione di
 * "Avvia". Resta la stessa istanza per tutta la sessione, cosi' cambiare vista non cancella cio'
 * che e' stato scritto.
 */
public class SetupPane {

    private final VBox root = new VBox(14);
    private final TextField inputField = new TextField();
    private final TextField outputField = new TextField();
    private final ComboBox<String> esitoCombo = new ComboBox<>();
    private final TextField istatField = new TextField();
    private final TextField annoField = new TextField();
    private final TextField paccoField = new TextField();
    private final Label errorLabel = new Label();
    private final Label occupatoLabel = new Label("C'è già un'elaborazione in corso: se ne può avviare un'altra quando finisce.");
    private final Button startButton = new Button("Avvia");

    public SetupPane(Consumer<JobParams> avvia) {
        Label title = new Label("Nuova elaborazione");
        title.getStyleClass().add("rp-view-title");
        Label subtitle = new Label("Rinomina i PDF leggendo il barcode al loro interno");
        subtitle.getStyleClass().add("rp-muted");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        ColumnConstraints c0 = new ColumnConstraints(90);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        inputField.setPromptText("Cartella con i PDF da elaborare");
        outputField.setPromptText("Cartella dove salvare i PDF rinominati");
        esitoCombo.getItems().addAll(Globals.ESITI);
        esitoCombo.setPromptText("Seleziona esito");
        esitoCombo.setMaxWidth(Double.MAX_VALUE);
        istatField.setPromptText("Codice istat (solo cifre, completato a 9 con zeri)");
        annoField.setPromptText("Anno, es. 2026");
        paccoField.setPromptText("Identificativo pacco, es. MP297");

        grid.addRow(0, new Label("Input"), withBrowse(inputField, "Cartella di input"));
        grid.addRow(1, new Label("Output"), withBrowse(outputField, "Cartella di output"));
        grid.addRow(2, new Label("Esito"), esitoCombo);
        grid.addRow(3, new Label("Istat"), istatField);
        grid.addRow(4, new Label("Anno"), annoField);
        grid.addRow(5, new Label("Pacco"), paccoField);

        errorLabel.getStyleClass().add("rp-error-label");
        errorLabel.setWrapText(true);
        occupatoLabel.getStyleClass().add("rp-muted");
        occupatoLabel.setWrapText(true);
        setOccupato(false);

        startButton.setGraphic(Icons.of("play", 16));
        startButton.getStyleClass().add("rp-accent-button");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(e -> {
            JobParams params = validate();
            if (params != null) {
                avvia.accept(params);
            }
        });

        Label example = new Label("Esempio risultato: RB0002858945-sorter-COMPIUTAGIACENZA-000063064-2026-66555P310.pdf");
        example.getStyleClass().add("rp-hint");
        example.setWrapText(true);

        root.setPadding(new Insets(24));
        root.getChildren().addAll(new VBox(2, title, subtitle), grid, example, errorLabel, occupatoLabel, startButton);
    }

    /** Un job alla volta: mentre ce n'e' uno il form si compila, ma non parte. */
    public void setOccupato(boolean occupato) {
        startButton.setDisable(occupato);
        occupatoLabel.setVisible(occupato);
        occupatoLabel.setManaged(occupato);
    }

    /** "Riusa questi dati" dal riepilogo di un'elaborazione passata. */
    public void riempi(Elaborazione e) {
        inputField.setText(e.input());
        outputField.setText(e.output());
        esitoCombo.setValue(e.esito());
        istatField.setText(e.istat());
        annoField.setText(e.anno());
        paccoField.setText(e.pacco());
        errorLabel.setText("");
    }

    private HBox withBrowse(TextField field, String chooserTitle) {
        Button browse = new Button("Sfoglia…");
        browse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle(chooserTitle);
            if (!field.getText().isBlank()) {
                File current = new File(field.getText());
                if (current.isDirectory()) {
                    dc.setInitialDirectory(current);
                }
            }
            File dir = dc.showDialog(field.getScene().getWindow());
            if (dir != null) {
                field.setText(dir.getAbsolutePath());
            }
        });
        HBox box = new HBox(8, field, browse);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }

    /** Restituisce i parametri validati oppure null mostrando l'errore. */
    private JobParams validate() {
        String input = inputField.getText().trim();
        String output = outputField.getText().trim();
        String esito = esitoCombo.getValue();
        String istat = istatField.getText().trim();
        String anno = annoField.getText().trim();
        String pacco = paccoField.getText().trim();

        if (input.isEmpty() || !Files.isDirectory(Path.of(input))) {
            return fail("Seleziona una cartella di Input esistente.");
        }
        if (output.isEmpty()) {
            return fail("Seleziona la cartella di Output.");
        }
        if (Path.of(input).normalize().equals(Path.of(output).normalize())) {
            return fail("Input e Output devono essere cartelle diverse.");
        }
        if (esito == null) {
            return fail("Seleziona un Esito.");
        }
        if (!istat.matches("\\d{1,9}")) {
            return fail("Istat non valido: servono da 1 a 9 cifre (verrà completato a 9 con zeri a sinistra).");
        }
        if (!Globals.ANNO_PATTERN.matcher(anno).matches()) {
            return fail("Anno non valido: inserisci un anno a 4 cifre (es. 2026).");
        }
        if (pacco.isEmpty() || !pacco.matches("[A-Za-z0-9]+")) {
            return fail("Pacco non valido: usa solo lettere e numeri (es. MP297).");
        }

        errorLabel.setText("");
        return new JobParams(Path.of(input), Path.of(output), esito,
                JobParams.padIstat(istat), anno, pacco.toUpperCase());
    }

    private JobParams fail(String message) {
        errorLabel.setText(message);
        return null;
    }

    public Parent getRoot() {
        return root;
    }
}
