package app;

/**
 * Launcher non-JavaFX: indirection necessaria per avviare JavaFX da fat-jar
 * non modulare (stesso pattern di queryF5).
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
