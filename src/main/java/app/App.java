package app;

import app.core.LogQueue;
import app.core.update.Installatore;
import app.core.update.UpdateCheck;
import app.ui.AppSession;
import app.ui.Browse;
import app.ui.Shell;
import app.ui.UpdateDialog;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class App extends Application {

    private static final System.Logger log = System.getLogger(App.class.getName());

    /** Oltre al controllo d'avvio, uno ogni sei ore finche' l'app resta aperta (vedi STANDARD.md). */
    private static final long ORE_FRA_I_CONTROLLI = 6;
    private static final long ATTESA_FINE_ELABORAZIONE_S = 90;

    private final ScheduledExecutorService pianificatoreAggiornamenti =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "rinominapdf-update-check");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean dialogoAggiornamentoAperto = new AtomicBoolean(false);
    private final AtomicBoolean controlloManualeInCorso = new AtomicBoolean(false);
    private volatile String versioneGiaProposta;

    private final Path controlDir = AppInfo.controlDir();
    private AppSession session;
    private Shell window;

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        session = new AppSession(controlDir);
        window = new Shell(primaryStage, session, controlDir, this::controllaAggiornamentiManuale);
        window.show();
        // HostServices esiste solo qui: depositato per UpdateDialog, che apre la pagina della
        // versione nuova nel browser quando non puo' installarla da solo.
        Browse.install(getHostServices());
        riferisciAggiornamentoPrecedente();
        // Subito (il tick iniziale a 5s) e poi ogni sei ore, piu' il pulsante nella barra di sinistra.
        pianificatoreAggiornamenti.scheduleWithFixedDelay(
                () -> controllaInBackground(false), 5, ORE_FRA_I_CONTROLLI * 3600, TimeUnit.SECONDS);
    }

    /**
     * Com'e' andato l'aggiornamento avviato la volta scorsa.
     *
     * <p>Va nel registro, non in una finestra: un avviso modale a ogni riavvio e' un pedaggio. Il
     * rapporto si consuma leggendolo, cosi' non ricompare domani.
     */
    private void riferisciAggiornamentoPrecedente() {
        Installatore.ritiraEsito(controlDir).ifPresent(e -> {
            LogQueue.add("update", e.ok()
                    ? "Aggiornamento alla " + e.versione() + " completato."
                    : "Aggiornamento alla " + e.versione() + " non riuscito: " + e.spiegazione() + ".");
        });
    }

    /**
     * L'utente ha chiesto di controllare ADESSO, dal pulsante. Sul thread FX: si passa al
     * pianificatore, che fa la rete fuori di qui. Un secondo clic mentre il primo e' in corso non
     * fa niente.
     */
    private void controllaAggiornamentiManuale() {
        if (!controlloManualeInCorso.compareAndSet(false, true)) return;
        LogQueue.add("update", "Controllo aggiornamenti…");
        pianificatoreAggiornamenti.execute(() -> controllaInBackground(true));
    }

    /**
     * Chiede al sito, su un thread di background, e porta l'esito sul thread FX.
     *
     * <p><b>Non deve mai lasciar sfuggire un'eccezione</b>: gira dentro
     * {@code scheduleWithFixedDelay}, che al primo errore non catturato cancella in silenzio tutti
     * i tick successivi. {@code UpdateCheck.cerca()} gia' inghiotte i guasti di rete, il try qui e'
     * la rete di sicurezza.
     */
    private void controllaInBackground(boolean manuale) {
        UpdateCheck.Update trovato = null;
        try {
            trovato = new UpdateCheck().cerca().orElse(null);
        } catch (Exception e) {
            log.log(System.Logger.Level.INFO, "Controllo aggiornamenti fallito: " + e);
        }
        final UpdateCheck.Update esito = trovato;
        Platform.runLater(() -> mostraEsitoControllo(esito, manuale));
    }

    /**
     * Sul thread FX: niente di nuovo e automatico, silenzio; niente di nuovo e manuale, una riga nel
     * registro (o il pulsante sembrerebbe rotto); c'e' un aggiornamento, si propone — ma
     * l'automatico non ripropone una versione gia' vista e chiusa, il manuale si'.
     */
    private void mostraEsitoControllo(UpdateCheck.Update u, boolean manuale) {
        try {
            if (u == null) {
                if (manuale) LogQueue.add("update", "Hai gia' l'ultima versione (" + AppInfo.VERSION + ").");
                return;
            }
            if (!manuale && u.versione().equals(versioneGiaProposta)) return;
            if (dialogoAggiornamentoAperto.get()) return;
            versioneGiaProposta = u.versione();
            dialogoAggiornamentoAperto.set(true);
            try {
                UpdateDialog.proponi(window.stage(), u, controlDir, session.inCorso());
            } finally {
                dialogoAggiornamentoAperto.set(false);
            }
        } finally {
            if (manuale) controlloManualeInCorso.set(false);
        }
    }

    @Override
    public void stop() {
        pianificatoreAggiornamenti.shutdownNow();
        if (window != null) window.ferma();
        // Anche quando a chiudere e' l'aggiornamento: i thread di lavoro sono daemon, e uccisi a
        // meta' di un Files.copy lascerebbero nell'output un PDF troncato con il nome giusto.
        // Si interrompe e si aspetta che i file in mano finiscano — meno dei 120 s che lo script
        // dell'aggiornamento concede alla chiusura, oltre i quali rinuncerebbe a installare.
        if (session != null) {
            session.interrompiEAttendi(ATTESA_FINE_ELABORAZIONE_S);
            session.chiudi();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
