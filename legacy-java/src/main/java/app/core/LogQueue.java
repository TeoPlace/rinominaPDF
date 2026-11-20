package app.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Coda di log in memoria: i thread di lavoro accodano, la UI svuota
 * periodicamente (ispirato a objLogTimeline di sorter-fx, ma come coda
 * ordinata invece che mappa, per non perdere messaggi ravvicinati).
 */
public final class LogQueue {

    private LogQueue() {
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final ConcurrentLinkedQueue<String> QUEUE = new ConcurrentLinkedQueue<>();

    public static void add(String tag, String message) {
        QUEUE.add(LocalDateTime.now().format(TS) + " [" + tag + "] " + message);
    }

    /** Svuota la coda restituendo i messaggi in ordine di inserimento. */
    public static List<String> drain() {
        List<String> out = new ArrayList<>();
        String line;
        while ((line = QUEUE.poll()) != null) {
            out.add(line);
        }
        return out;
    }
}
