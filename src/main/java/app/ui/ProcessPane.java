package app.ui;

import app.core.Globals;
import app.core.JobParams;
import app.core.LogQueue;
import app.core.Pc;
import app.core.RenameJob;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Vista di elaborazione: progress bar 0-100, animazione retro, pulsante per
 * aprire/chiudere il log-panel con risorse (CPU/disco/memoria) e stato dei
 * thread. Una Timeline da 300ms svuota la coda di log e aggiorna i sensori
 * (stesso schema del flusher di sorter-fx).
 */
public class ProcessPane {

    private final MainWindow window;
    private final JobParams params;
    private final VBox root = new VBox(14);

    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("In preparazione…");
    private final Label percentLabel = new Label("0%");
    private final RetroAnimation animation = new RetroAnimation();

    private final VBox logPanel = new VBox(6);
    private final Label resourceLabel = new Label();
    private final Label threadsLabel = new Label();
    private final TextArea terminal = new TextArea();

    private final Button toggleLogButton = new Button("Mostra log ▼");
    private final Button abortButton = new Button("Interrompi");
    private final Button backButton = new Button("◀ Nuova elaborazione");

    private RenameJob job;
    private Timeline flusher;
    private int total;
    private int logged;

    public ProcessPane(MainWindow window, JobParams params) {
        this.window = window;
        this.params = params;

        Label title = new Label("Elaborazione in corso");
        title.getStyleClass().add("title");

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(26);
        percentLabel.getStyleClass().add("percent");
        HBox progressRow = new HBox(10, progressBar, percentLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        terminal.setEditable(false);
        terminal.getStyleClass().add("terminal");
        terminal.setPrefRowCount(10);
        resourceLabel.getStyleClass().add("monitor");
        threadsLabel.getStyleClass().add("monitor");
        threadsLabel.setWrapText(true);
        logPanel.getChildren().addAll(resourceLabel, threadsLabel, terminal);
        logPanel.getStyleClass().add("log-panel");
        logPanel.setVisible(false);
        logPanel.setManaged(false);
        VBox.setVgrow(logPanel, Priority.ALWAYS);
        VBox.setVgrow(terminal, Priority.ALWAYS);

        toggleLogButton.setOnAction(e -> toggleLog());
        abortButton.setOnAction(e -> {
            if (job != null) {
                job.abort();
                abortButton.setDisable(true);
                progressLabel.setText("Interruzione in corso…");
            }
        });
        backButton.setVisible(false);
        backButton.setManaged(false);
        backButton.setOnAction(e -> window.showSetup());
        HBox buttons = new HBox(10, toggleLogButton, abortButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        root.setPadding(new Insets(24));
        root.getStyleClass().add("process");
        root.getChildren().addAll(title, animation.getCanvas(), progressRow,
                progressLabel, buttons, logPanel);
        root.setAlignment(Pos.TOP_CENTER);
    }

    public void start() {
        animation.start();
        setupFlusher();
        job = new RenameJob(params, new RenameJob.Listener() {
            @Override
            public void onTotal(int totalFiles) {
                total = totalFiles;
                Platform.runLater(() -> progressLabel.setText("0 / " + totalFiles + " file"));
            }

            @Override
            public void onProgress(int done, int totalFiles) {
                Platform.runLater(() -> {
                    progressBar.setProgress((double) done / totalFiles);
                    percentLabel.setText(Math.round(100.0 * done / totalFiles) + "%");
                    progressLabel.setText(done + " / " + totalFiles + " file");
                });
            }

            @Override
            public void onFinished(int ok, int unread, int errors, boolean aborted) {
                Platform.runLater(() -> finish(ok, unread, errors, aborted));
            }
        });
        job.start();
    }

    private void finish(int ok, int unread, int errors, boolean aborted) {
        animation.stop();
        flushLogs();
        if (total > 0 && !aborted) {
            progressBar.setProgress(1);
            percentLabel.setText("100%");
        }
        progressLabel.setText((aborted ? "Interrotto. " : "Completato. ")
                + "Rinominati: " + ok + "   Non letti: " + unread + "   Errori: " + errors);
        abortButton.setVisible(false);
        abortButton.setManaged(false);
        backButton.setVisible(true);
        backButton.setManaged(true);
    }

    private void toggleLog() {
        boolean show = !logPanel.isVisible();
        logPanel.setVisible(show);
        logPanel.setManaged(show);
        toggleLogButton.setText(show ? "Nascondi log ▲" : "Mostra log ▼");
    }

    /** Flusher a 300ms: log -> terminale, sensori -> etichette (come sorter-fx). */
    private void setupFlusher() {
        flusher = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            flushLogs();
            Pc.cpu();
            Pc.disk();
            Pc.memory();
            resourceLabel.setText(String.format(
                    "CPU %.0f%%   Disco %.0f%%   RAM %.0f%% (%.0f/%.0f MB)   JVM %.0f MB   Thread attivi: %d (target %d)",
                    Pc.get("cpu"), Pc.get("disk"), Pc.get("memory"),
                    Pc.get("memoryUsedMb"), Pc.get("memoryTotalMb"), Pc.get("jvmUsedMb"),
                    RenameJob.threadStatus.size(), Globals.totalThreads));
            if (RenameJob.threadStatus.isEmpty()) {
                threadsLabel.setText("Nessun thread di lavoro attivo");
            } else {
                StringBuilder sb = new StringBuilder("Thread: ");
                RenameJob.threadStatus.forEach((name, file) ->
                        sb.append(name).append(" → ").append(file).append("   "));
                threadsLabel.setText(sb.toString());
            }
        }));
        flusher.setCycleCount(Animation.INDEFINITE);
        flusher.play();
    }

    private void flushLogs() {
        for (String line : LogQueue.drain()) {
            logged++;
            if (logged > 10_000) {
                terminal.clear();
                logged = 0;
            }
            terminal.appendText(line + "\n");
        }
    }

    public Parent getRoot() {
        return root;
    }
}
