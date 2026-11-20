package app.ui;

import app.core.Elaborazione;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Il centro quando si riapre un'elaborazione finita: com'e' andata e con quali dati. */
public final class RiepilogoPane {

    private static final DateTimeFormatter QUANDO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final VBox root = new VBox(16);

    public RiepilogoPane(Elaborazione e, Consumer<Elaborazione> riusa) {
        Label title = new Label(titolo(e));
        title.getStyleClass().add("rp-view-title");
        Label quando = new Label(QUANDO.format(Instant.ofEpochMilli(e.iniziataIl())));
        quando.getStyleClass().add("rp-muted");

        HBox numeri = new HBox(10,
                casella(e.rinominati(), "Rinominati", "ok"),
                casella(e.nonLetti(), "Non letti", "warn"),
                casella(e.errori(), "Errori", "crit"));

        GridPane dati = new GridPane();
        dati.setHgap(14);
        dati.setVgap(6);
        String[][] righe = {
                {"Input", e.input()}, {"Output", e.output()}, {"Esito", e.esito()},
                {"Istat", e.istat()}, {"Anno", e.anno()}, {"Pacco", e.pacco()}};
        for (int i = 0; i < righe.length; i++) {
            Label nome = new Label(righe[i][0]);
            nome.getStyleClass().add("rp-muted");
            nome.setMinWidth(Label.USE_PREF_SIZE);
            Label valore = new Label(righe[i][1]);
            valore.setWrapText(true);
            dati.addRow(i, nome, valore);
        }

        Label elenchi = new Label("Nella cartella di output: LOG_OK.txt, LOG_DATAMATRIX_NON_LETTI.txt"
                + " e LOG_ERRORI.txt, con il dettaglio di ogni PDF.");
        elenchi.getStyleClass().add("rp-muted");
        elenchi.setWrapText(true);

        Path output = Path.of(e.output());
        Button apri = new Button("Apri cartella di output");
        apri.setGraphic(Icons.of("folder-open", 14));
        apri.setDisable(!Files.isDirectory(output));
        apri.setOnAction(ev -> Browse.apriCartella(output));
        Button riusaDati = new Button("Riusa questi dati");
        riusaDati.setGraphic(Icons.of("copy", 14));
        riusaDati.setOnAction(ev -> riusa.accept(e));

        root.setPadding(new Insets(24));
        root.getChildren().addAll(new VBox(2, title, quando), numeri, dati, elenchi, new HBox(10, apri, riusaDati));
    }

    private static String titolo(Elaborazione e) {
        return switch (e.stato()) {
            case IN_CORSO -> "In corso";
            case RIUSCITA -> "Completata";
            case NON_LETTI -> "Completata, con PDF non letti";
            case VUOTA -> "Nessun PDF rinominato";
            case ERRORE -> e.interrotta() ? "Interrotta" : "Completata, con errori";
        };
    }

    private static VBox casella(int valore, String etichetta, String classe) {
        Label v = new Label(String.valueOf(valore));
        v.getStyleClass().addAll("rp-kpi-value", classe);
        Label l = new Label(etichetta);
        l.getStyleClass().add("rp-gauge-label");
        VBox box = new VBox(1, v, l);
        box.getStyleClass().add("rp-kpi-card");
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    public Parent getRoot() {
        return root;
    }
}
