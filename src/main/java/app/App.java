package app;

import app.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        new MainWindow(primaryStage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
