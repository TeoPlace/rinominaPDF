package app.ui;

import app.AppInfo;
import app.core.update.Installatore;
import app.core.update.Scaricatore;
import app.core.update.UpdateCheck;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * L'aggiornamento, dall'avviso alla riapertura del programma. Copiato da listBigStorage, che e' il
 * riferimento della linea (vedi STANDARD.md, "Il controllo aggiornamenti dentro l'app").
 *
 * <p><b>Un solo gesto.</b> Si scarica qui dentro (con l'avanzamento a vista, perche' un minuto di
 * silenzio si legge come un blocco), si verifica l'impronta, il programma si chiude, si installa e
 * si riapre. Resta <b>un</b> clic dell'utente, la conferma di amministratore che Windows chiede per
 * scrivere in {@code Program Files} e che nessun programma puo' evitare.
 *
 * <p>In sviluppo, o comunque quando non si sta girando da un'installazione ({@link
 * Installatore#possibile()}), si torna al browser: non c'e' nessun pacchetto installato da
 * sostituire.
 */
public final class UpdateDialog {

    private static final System.Logger log = System.getLogger(UpdateDialog.class.getName());

    private UpdateDialog() {}

    /**
     * Propone l'aggiornamento e, se accettato, lo porta a termine chiudendo l'applicazione.
     *
     * @param elaborazioneInCorso se {@code true} c'e' una rinomina aperta, e va detto prima di chiudere
     */
    public static void proponi(Window owner, UpdateCheck.Update u, Path controlDir, boolean elaborazioneInCorso) {
        boolean silenzioso = Installatore.possibile();

        StringBuilder testo = new StringBuilder("E' disponibile la versione " + u.versione()
                + ". In esecuzione c'e' la " + AppInfo.VERSION + ".");
        if (u.dimensione() != null) testo.append("\n\nDimensione del pacchetto: ").append(u.dimensione());
        if (silenzioso) {
            testo.append("\n\nIl programma scarica l'aggiornamento, si chiude e si riapre da solo. Windows"
                    + " chiede una conferma di amministratore: e' l'unico passaggio che resta a te.");
            // Il lavoro aperto si nomina PRIMA, non dopo: dirlo a valle del clic sarebbe una
            // domanda a cui l'utente ha gia' risposto senza sapere cosa stava rispondendo.
            if (elaborazioneInCorso) {
                testo.append("\n\nC'e' un'elaborazione in corso: verra' interrotta. I PDF gia' rinominati"
                        + " restano nella cartella di output, gli altri andranno rielaborati.");
            }
        }

        Dialog<ButtonType> d = Dialogs.decorate(new Dialog<ButtonType>());
        if (owner != null) d.initOwner(owner);
        d.setTitle("Aggiornamento disponibile");

        Label body = new Label(testo.toString());
        body.setWrapText(true);
        body.setMaxWidth(460);
        VBox content = new VBox(12, body);
        content.setPadding(new Insets(16));
        d.getDialogPane().setContent(content);

        ButtonType procedi = new ButtonType(
                silenzioso ? "Aggiorna e riavvia" : "Scarica", ButtonBar.ButtonData.OK_DONE);
        ButtonType dopo = new ButtonType("Piu' tardi", ButtonBar.ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().setAll(procedi, dopo);

        d.showAndWait()
                .filter(b -> b == procedi)
                .ifPresent(b -> {
                    if (silenzioso) aggiorna(owner, u, controlDir);
                    else Browse.open(u.url());
                });
    }

    // ---- scarica, installa, chiude ---------------------------------------------------------------

    private static void aggiorna(Window owner, UpdateCheck.Update u, Path controlDir) {
        Path destinazione = Installatore.cartella(controlDir).resolve(u.file());

        Dialog<ButtonType> d = Dialogs.decorate(new Dialog<ButtonType>());
        if (owner != null) d.initOwner(owner);
        d.setTitle("Aggiornamento");

        Label stato = new Label("Preparazione...");
        stato.setWrapText(true);
        stato.setMaxWidth(460);
        ProgressBar barra = new ProgressBar();
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        VBox content = new VBox(12, stato, barra);
        content.setPadding(new Insets(16));
        content.setPrefWidth(460);
        d.getDialogPane().setContent(content);

        ButtonType annulla = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().setAll(annulla);

        // Il download non sa niente di JavaFX e non ha un "cancel": glielo si dice attraverso
        // l'unica cosa che chiama spesso, cioe' l'avanzamento, che alza un'eccezione e fa
        // ripulire il file parziale come per qualunque altro guasto.
        AtomicBoolean annullato = new AtomicBoolean(false);
        Button bottoneAnnulla = (Button) d.getDialogPane().lookupButton(annulla);
        bottoneAnnulla.addEventFilter(ActionEvent.ACTION, ev -> annullato.set(true));

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                long[] ultimo = {0};
                Scaricatore.Esito esito = new Scaricatore().scarica(u.url(), destinazione, u.sha256(),
                        (scaricati, totali) -> {
                    if (annullato.get()) throw new AnnullatoException();
                    // Non un runLater per blocco letto: sotto rete veloce sono migliaia al secondo,
                    // la coda FX si intasa e l'interfaccia si blocca. Una decina al secondo basta.
                    long ora = System.nanoTime();
                    if (ora - ultimo[0] < 100_000_000L) return;
                    ultimo[0] = ora;
                    Platform.runLater(() -> mostraAvanzamento(stato, barra, scaricati, totali));
                });
                if (annullato.get()) throw new AnnullatoException();

                Platform.runLater(() -> {
                    stato.setText("Installazione. Il programma si chiude e si riapre da solo.");
                    barra.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                    bottoneAnnulla.setDisable(true);   // da qui in poi non si torna indietro
                });
                Installatore.avvia(esito.file(), u.versione(), controlDir);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            d.setResult(annulla);
            d.close();
            // Lo script sta aspettando che questo PID sparisca: se l'applicazione non si chiude,
            // dopo due minuti rinuncia e non installa niente. I thread di RenameJob sono daemon,
            // e App.stop() interrompe l'elaborazione.
            Platform.exit();
        });
        task.setOnFailed(e -> {
            d.setResult(annulla);
            d.close();
            Throwable causa = task.getException();
            if (causa instanceof AnnullatoException) {
                log.log(System.Logger.Level.INFO, "Aggiornamento annullato dall'utente.");
                return;
            }
            log.log(System.Logger.Level.WARNING, "Aggiornamento non riuscito: " + causa);
            fallito(owner, u, causa);
        });

        Thread th = new Thread(task, "rinominapdf-update-install");
        th.setDaemon(true);
        th.start();

        d.showAndWait();
    }

    private static void mostraAvanzamento(Label stato, ProgressBar barra, long scaricati, long totali) {
        // I byte del Content-Length, non i "90,9 MB" del manifest: quelli sono un arrotondamento,
        // e una barra costruita sopra arriverebbe al 101%.
        if (totali > 0) {
            stato.setText("Scaricamento: " + Scaricatore.dimensione(scaricati)
                    + " di " + Scaricatore.dimensione(totali));
            barra.setProgress((double) scaricati / totali);
        } else {
            // Server senza Content-Length: si dice quanto e' arrivato, che e' l'unica cosa vera.
            stato.setText("Scaricamento: " + Scaricatore.dimensione(scaricati));
            barra.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }
    }

    /**
     * Il guasto non lascia l'utente senza strada: resta la pagina del prodotto, che e' quello che
     * si faceva prima di avere l'installazione automatica.
     */
    private static void fallito(Window owner, UpdateCheck.Update u, Throwable causa) {
        Dialog<ButtonType> d = Dialogs.decorate(new Dialog<ButtonType>());
        if (owner != null) d.initOwner(owner);
        d.setTitle("Aggiornamento non riuscito");

        String motivo = causa == null || causa.getMessage() == null
                ? String.valueOf(causa) : causa.getMessage();
        Label body = new Label("Non e' stato possibile completare l'aggiornamento: " + motivo);
        body.setWrapText(true);
        body.setMaxWidth(460);
        VBox content = new VBox(12, body);
        content.setPadding(new Insets(16));
        d.getDialogPane().setContent(content);

        ButtonType apri = new ButtonType("Apri la pagina del prodotto", ButtonBar.ButtonData.OK_DONE);
        ButtonType chiudi = new ButtonType("Chiudi", ButtonBar.ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().setAll(apri, chiudi);
        d.showAndWait().filter(b -> b == apri).ifPresent(b -> Browse.open(u.url()));
    }

    /** Annullare non e' un errore: si distingue per non mostrare un guasto che non c'e'. */
    private static final class AnnullatoException extends RuntimeException {
        AnnullatoException() { super("annullato"); }
    }
}
