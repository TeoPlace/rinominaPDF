package app.core.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Il {@code versione.json} che il sito prodotto pubblica sotto {@code /scarica/}.
 *
 * <p><b>Nessuna infrastruttura nuova</b>: e' lo stesso manifest che il sito legge per accendere il
 * bottone di download e che {@code teoSetup} interroga per decidere cosa proporre. L'applicazione
 * si limita a leggerlo per conto suo. Forma reale:
 *
 * <pre>{@code
 * { "disponibile": true, "versione": "1.0.1", "file": "rinominaPDF-1.0.1.msi",
 *   "dimensione": "72,8 MB", "data": "2026-08-16", "sha256": "1a56831c..." }
 * }</pre>
 *
 * <p>Quando non c'e' niente da scaricare i campi sono {@code null} e {@code disponibile} e'
 * {@code false} — cioe' <b>il manifest esiste comunque</b>. E' un caso normale, non un errore.
 *
 * @param sha256      impronta del pacchetto, {@code null} se il sito non la pubblica. Da quando
 *                    l'aggiornamento si installa da solo e' l'unica cosa che distingue il file
 *                    giusto da uno troncato o sostituito: gli MSI della linea <b>non sono
 *                    firmati</b>, quindi msiexec li esegue senza sapere da dove vengono. Resta
 *                    facoltativa di proposito — un manifest scritto prima che il campo esistesse
 *                    e' valido, e si procede come si e' sempre fatto.
 *
 * @param alternativo secondo installer, {@code null} se il sito non lo pubblica. Il campo e'
 *                    opzionale e viene letto in modo <b>tollerante</b>: un manifest che non lo ha
 *                    e' valido, non incompleto. rinominaPDF ne pubblica uno solo, ma il
 *                    campo si legge lo stesso — il formato e' quello di tutta la linea, e TraceFox
 *                    lo usa per il pacchetto per utente. Leggerlo qui costa niente e toglie una
 *                    modifica il giorno che servisse anche a questo prodotto.
 */
public record Manifest(boolean disponibile,
                       String versione,
                       String file,
                       String dimensione,
                       String data,
                       String sha256,
                       Manifest alternativo) {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Il manifest che si usa quando la rete non risponde: non disponibile, nessun errore inventato. */
    public static Manifest assente() {
        return new Manifest(false, null, null, null, null, null, null);
    }

    /**
     * Tollerante di proposito. Un campo in piu' non deve rompere niente (il sito puo' aggiungerne),
     * e un campo in meno vale {@code null}. Un'eccezione qui diventerebbe un avviso di
     * aggiornamento che non compare mai, cioe' un difetto invisibile.
     */
    public static Manifest parse(String json) {
        try {
            return from(JSON.readTree(json));
        } catch (Exception e) {
            return assente();
        }
    }

    private static Manifest from(JsonNode n) {
        if (n == null || !n.isObject()) return assente();
        JsonNode alt = n.get("alternativo");
        return new Manifest(
                n.path("disponibile").asBoolean(false),
                text(n, "versione"),
                text(n, "file"),
                text(n, "dimensione"),
                text(n, "data"),
                text(n, "sha256"),
                alt != null && alt.isObject() ? from(alt) : null);
    }

    private static String text(JsonNode n, String campo) {
        JsonNode v = n.get(campo);
        if (v == null || v.isNull()) return null;
        String s = v.asText("").trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * Un manifest e' utilizzabile solo se dice <b>tutte e tre</b> le cose che servono a scaricare.
     * {@code disponibile: true} con {@code file: null} e' un manifest scritto male, e trattarlo
     * come buono porterebbe a mandare l'utente su un URL che finisce con "null".
     */
    public boolean scaricabile() {
        return disponibile && versione != null && file != null;
    }
}
