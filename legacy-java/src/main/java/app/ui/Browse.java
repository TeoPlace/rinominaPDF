package app.ui;

import javafx.application.HostServices;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Apertura di un URL nel browser di sistema. {@link HostServices} si ottiene solo da {@code Application},
 * quindi {@code App} lo deposita qui all'avvio e le viste lo usano senza doverselo passare in giro.
 */
public final class Browse {

    private static final System.Logger log = System.getLogger(Browse.class.getName());

    private static volatile HostServices services;

    private Browse() {}

    public static void install(HostServices hostServices) {
        services = hostServices;
    }

    /** Non lancia mai: un browser assente non deve buttare giu' un dialog. */
    public static void open(String url) {
        HostServices s = services;
        if (s == null) {
            log.log(System.Logger.Level.WARNING, "HostServices non disponibile: impossibile aprire " + url);
            return;
        }
        try {
            s.showDocument(url);
        } catch (Exception e) {
            log.log(System.Logger.Level.WARNING, "Apertura di " + url + " fallita: " + e);
        }
    }

    /** Una cartella in Esplora file. Non lancia mai, come {@link #open}. */
    public static void apriCartella(Path cartella) {
        try {
            new ProcessBuilder("explorer.exe", cartella.toString()).start();
        } catch (IOException e) {
            log.log(System.Logger.Level.WARNING, "Apertura di " + cartella + " fallita: " + e);
        }
    }
}
