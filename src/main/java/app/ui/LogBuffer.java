package app.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Le righe del registro, in sessione e non nel pannello. Da listBigStorage, senza il renderer: qui
 * le righe arrivano a blocchi dal flusher, che le passa direttamente al pannello.
 */
public final class LogBuffer {

    private static final int MAX_ENTRIES = 6000;

    /** Una riga di log: testo gia' formattato ("HH:mm:ss  ...\n") + la sua categoria. */
    public record Entry(String line, LogCategory category) {}

    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    public synchronized void append(Entry e) {
        entries.addLast(e);
        while (entries.size() > MAX_ENTRIES) entries.removeFirst();
    }

    /** Copia delle righe correnti: serve a ridisegnare il pannello quando cambia il filtro. */
    public synchronized List<Entry> snapshot() {
        return new ArrayList<>(entries);
    }
}
