package app.ui;

import app.core.Elaborazione;
import app.core.RenameJob;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Il centro durante l'elaborazione: animazione retro, avanzamento e cosa ha in mano ogni lavoratore.
 * Le righe e le risorse stanno nel registro. La aggiorna il flusher di {@link Shell}.
 */
public final class ProcessPane {

    private static final int LAVORATORI_A_VISTA = 8;

    private final AppSession session;
    private final VBox root = new VBox(14);
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("In preparazione…");
    private final Label percentLabel = new Label("0%");
    private final Label threadsLabel = new Label();
    private final RetroAnimation animation = new RetroAnimation();
    private boolean interruzione;

    public ProcessPane(AppSession session, Elaborazione e) {
        this.session = session;

        Label title = new Label("Elaborazione in corso");
        title.getStyleClass().add("rp-view-title");
        Label dettaglio = new Label(e.esito() + "  ·  istat " + e.istat() + "  ·  " + e.anno() + "  ·  " + e.pacco());
        dettaglio.getStyleClass().add("rp-muted");

        progressBar.setMaxWidth(Double.MAX_VALUE);
        percentLabel.getStyleClass().add("rp-percent");
        HBox progressRow = new HBox(10, progressBar, percentLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        threadsLabel.getStyleClass().add("rp-mono");
        threadsLabel.setWrapText(true);

        Button abortButton = new Button("Interrompi");
        abortButton.getStyleClass().add("rp-danger-button");
        abortButton.setOnAction(ev -> {
            session.interrompi();
            abortButton.setDisable(true);
            interruzione = true;
        });

        root.setPadding(new Insets(24));
        root.getChildren().addAll(title, dettaglio, spazioAnimazione(), progressRow, progressLabel,
                abortButton, threadsLabel);
    }

    /** La tela e' larga 640: dove il centro e' piu' stretto si rimpicciolisce invece di sbordare. */
    private Pane spazioAnimazione() {
        Canvas canvas = animation.getCanvas();
        Pane box = new Pane(canvas);
        box.setMinWidth(0);
        box.setPrefHeight(canvas.getHeight());
        box.setMinHeight(canvas.getHeight());
        box.widthProperty().addListener((o, was, w) -> {
            double s = Math.min(1, w.doubleValue() / canvas.getWidth());
            canvas.setScaleX(s);
            canvas.setScaleY(s);
            canvas.setLayoutX((w.doubleValue() - canvas.getWidth()) / 2);
        });
        return box;
    }

    /** Anche per riprendere: l'animazione gira solo finche' la vista e' a schermo. */
    public void avviaAnimazione() {
        animation.stop();
        animation.start();
    }

    public void fermaAnimazione() {
        animation.stop();
    }

    public void aggiorna() {
        int t = session.totale();
        int d = session.fatti();
        if (t > 0) {
            progressBar.setProgress((double) d / t);
            percentLabel.setText(Math.round(100.0 * d / t) + "%");
            progressLabel.setText(d + " / " + t + " file" + (interruzione ? "  ·  interruzione in corso…" : ""));
        }
        threadsLabel.setText(lavoratoriAVista());
    }

    private static String lavoratoriAVista() {
        List<String> righe = new ArrayList<>();
        new TreeMap<>(RenameJob.threadStatus).forEach((nome, file) -> righe.add(nome + " → " + file));
        if (righe.size() <= LAVORATORI_A_VISTA) return String.join("\n", righe);
        return String.join("\n", righe.subList(0, LAVORATORI_A_VISTA))
                + "\n… e altri " + (righe.size() - LAVORATORI_A_VISTA);
    }

    public Parent getRoot() {
        return root;
    }
}
