package app.ui;

import app.core.JobParams;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 * Finestra principale: gestisce il passaggio tra la vista di setup
 * (form parametri) e la vista di elaborazione (progress + log).
 */
public class MainWindow {

    private final Stage stage;
    private final Scene scene;

    public MainWindow(Stage stage) {
        this.stage = stage;
        scene = new Scene(new SetupPane(this).getRoot(), 760, 640);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("rinominaPDF");
        stage.setMinWidth(680);
        stage.setMinHeight(560);
    }

    public void show() {
        stage.show();
    }

    public void showSetup() {
        setContent(new SetupPane(this).getRoot());
    }

    public void startJob(JobParams params) {
        ProcessPane pane = new ProcessPane(this, params);
        setContent(pane.getRoot());
        pane.start();
    }

    private void setContent(Parent root) {
        scene.setRoot(root);
    }
}
