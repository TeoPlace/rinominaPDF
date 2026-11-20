package app.ui;

import app.core.Elaborazione;
import app.core.JobParams;
import app.core.LogQueue;
import app.core.Pc;
import app.core.RenameJob;
import app.core.StoricoStore;
import javafx.application.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo stato del lavoro, fuori dalle viste: il job, lo storico, il registro. Le viste si possono
 * cambiare a elaborazione aperta senza perdere niente (vedi STANDARD.md, "L'impianto della finestra").
 *
 * <p>Un job alla volta: {@code RenameJob.threadStatus} e {@code Globals} sono statici.
 */
public final class AppSession {

    private static final Pattern RIGA = Pattern.compile("^(\\d\\d:\\d\\d:\\d\\d)\\S* \\[([^]]+)] (.*)$", Pattern.DOTALL);

    private final StoricoStore store;
    private final List<Elaborazione> storico;
    private final LogBuffer log = new LogBuffer();

    // Due thread, e non per eleganza: la PRIMA lettura del disco con OSHI puo' durare quasi un minuto
    // (55 s misurati il 2026-11-06). Sul thread FX bloccava la finestra, sullo stesso thread dello
    // storico faceva perdere l'elaborazione a chi chiudeva prima. RenameJob decide quanti lavoratori
    // tenere guardando il disco misurato qui, quindi il campionamento non si ferma.
    private final ScheduledExecutorService sensori = daemon("rinominapdf-sensori");
    private final ScheduledExecutorService sfondo = daemon("rinominapdf-storico");

    private volatile RenameJob job;
    private volatile Thread jobThread;
    private volatile Elaborazione corrente;
    private volatile int totale;
    private volatile int fatti;
    private volatile Consumer<Elaborazione> suFine = e -> {};

    public AppSession(Path controlDir) {
        store = new StoricoStore(controlDir);
        storico = new ArrayList<>(store.carica());
        sensori.scheduleWithFixedDelay(AppSession::campiona, 0, 1, TimeUnit.SECONDS);
    }

    private static ScheduledExecutorService daemon(String nome) {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, nome);
            t.setDaemon(true);
            return t;
        });
    }

    /** Dentro scheduleWithFixedDelay: un'eccezione non catturata cancellerebbe i tick successivi. */
    private static void campiona() {
        try {
            Pc.cpu();
            Pc.memory();
            Pc.disk(); // per ultimo: e' quello che puo' metterci un minuto
        } catch (RuntimeException | LinkageError e) {
            System.getLogger(AppSession.class.getName()).log(System.Logger.Level.DEBUG, "Risorse: " + e);
        }
    }

    /** Chiamato sul thread FX a elaborazione finita. */
    void setSuFine(Consumer<Elaborazione> suFine) {
        this.suFine = suFine;
    }

    public Elaborazione avvia(JobParams params) {
        Elaborazione e = Elaborazione.inizio(params, System.currentTimeMillis());
        totale = 0;
        fatti = 0;
        corrente = e;
        synchronized (storico) {
            storico.add(0, e);
        }
        salva();
        job = new RenameJob(params, new RenameJob.Listener() {
            @Override
            public void onTotal(int totalFiles) {
                totale = totalFiles;
            }

            // Niente runLater per file (vedi STANDARD.md): il flusher legge i contatori ogni 300 ms.
            @Override
            public void onProgress(int done, int total) {
                fatti = done;
            }

            @Override
            public void onFinished(int ok, int unread, int errors, boolean aborted) {
                concludi(e, ok, unread, errors, aborted);
            }
        });
        jobThread = job.start();
        return e;
    }

    /**
     * Sul thread del job e non su quello FX: alla chiusura il thread FX e' fermo in {@link
     * #interrompiEAttendi}, e il risultato deve arrivare nello storico lo stesso.
     */
    private void concludi(Elaborazione inizio, int ok, int unread, int errors, boolean aborted) {
        Elaborazione fine = inizio.fine(ok, unread, errors, aborted, System.currentTimeMillis());
        synchronized (storico) {
            int i = storico.indexOf(inizio);
            if (i >= 0) storico.set(i, fine);
        }
        corrente = null;
        salva();
        try {
            Platform.runLater(() -> suFine.accept(fine));
        } catch (IllegalStateException chiuso) {
            // la finestra non c'e' piu': lo storico e' gia' scritto
        }
    }

    private void salva() {
        List<Elaborazione> copia = storico();
        try {
            sfondo.execute(() -> store.salva(copia));
        } catch (RejectedExecutionException chiuso) {
            store.salva(copia);
        }
    }

    public List<Elaborazione> storico() {
        synchronized (storico) {
            return new ArrayList<>(storico);
        }
    }

    /** Quella in corso resta: e' in mano a un thread che alla fine la cerchera' nell'elenco. */
    public void svuotaStorico() {
        synchronized (storico) {
            storico.removeIf(e -> e != corrente);
        }
        salva();
    }

    public Elaborazione corrente() {
        return corrente;
    }

    public boolean inCorso() {
        return corrente != null;
    }

    public int totale() {
        return totale;
    }

    public int fatti() {
        return fatti;
    }

    /** I contatori dell'ultima elaborazione di questa sessione. */
    public int rinominati() {
        return job == null ? 0 : job.rinominati();
    }

    public int nonLetti() {
        return job == null ? 0 : job.nonLetti();
    }

    public int errori() {
        return job == null ? 0 : job.errori();
    }

    public void interrompi() {
        if (job != null) job.abort();
    }

    /**
     * Interrompe e aspetta, al massimo {@code secondi}, che i file gia' presi finiscano. Non dal
     * thread FX se non alla chiusura: blocca.
     */
    public void interrompiEAttendi(long secondi) {
        Thread t = jobThread;
        if (t == null || !t.isAlive()) return;
        job.abort();
        try {
            t.join(secondi * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public LogBuffer log() {
        return log;
    }

    /** Le righe arrivate dall'ultimo giro, gia' classificate e tenute nel buffer. */
    public List<LogBuffer.Entry> svuotaLog() {
        List<LogBuffer.Entry> nuove = new ArrayList<>();
        for (String riga : LogQueue.drain()) {
            LogBuffer.Entry e = voce(riga);
            log.append(e);
            nuove.add(e);
        }
        return nuove;
    }

    /** Da "HH:mm:ss.SSS [tag] testo" alla riga del registro, senza millesimi e senza tag. */
    static LogBuffer.Entry voce(String riga) {
        Matcher m = RIGA.matcher(riga);
        if (!m.matches()) return new LogBuffer.Entry(riga + "\n", LogCategory.LAVORO);
        return new LogBuffer.Entry(m.group(1) + "  " + m.group(3) + "\n", LogCategory.daTag(m.group(2)));
    }

    /** Quello che resta da scrivere va scritto: {@code shutdown} fa finire i compiti in coda. */
    public void chiudi() {
        sensori.shutdownNow();
        sfondo.shutdown();
        try {
            sfondo.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
