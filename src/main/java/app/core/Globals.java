package app.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Stato globale dell'applicazione (stile objGlobals di sorter-fx).
 */
public final class Globals {

    private Globals() {
    }

    /** Testo fisso nel nome file di destinazione. */
    public static final String FIXED_TEXT = "sorter";

    /** Esiti selezionabili dall'utente. */
    public static final List<String> ESITI = List.of(
            "COMPIUTAGIACENZA",
            "CONSEGNATO",
            "RIFIUTATO",
            "TRASFERITO",
            "IRREPERIBILE",
            "EMESSOCAD",
            "DECEDUTO",
            "ATTIVITACESSATA");

    /** Anno valido: 4 cifre, 19xx/20xx. */
    public static final Pattern ANNO_PATTERN = Pattern.compile("^(19|20)\\d{2}$");

    /** Istat: solo cifre, max 9, poi zero-padding a sinistra fino a 9. */
    public static final int ISTAT_LENGTH = 9;

    /**
     * Barcode "giusto": codice raccomandata/atto giudiziario italiano,
     * 2 lettere + 10 cifre (es. RB0002858945, AC0002382099).
     */
    public static final Pattern BARCODE_PATTERN = Pattern.compile("^[A-Z]{2}\\d{10}$");

    // --- WSL / Scandit (stesso metodo di sorter-fx) ---
    public static final String SCANDIT_DIR = "/srv/scandit_multiple/";
    public static final String SCANDIT_PROGRAM = "it.smaart.sorter.stock";
    public static final String SCANDIT_KEY = "test.key";
    public static final String SCANDIT_SETTINGS = "codes_ALL.settings";

    /** DPI con cui le pagine PDF vengono renderizzate per Scandit via WSL. */
    public static final int RENDER_DPI = 300;

    /**
     * DPI per il fallback ZXing: sulle scansioni fax il DataMatrix si decodifica
     * in modo affidabile solo a risoluzione più alta.
     */
    public static final int RENDER_DPI_ZXING = 600;

    /**
     * Numero di thread di lavoro corrente: parte da 1 e viene regolato
     * dinamicamente in base a CPU/disco (stesso meccanismo di sorter-fx).
     */
    public static volatile int totalThreads = 1;

    /** Contatore globale dei file processati (per la progress bar). */
    public static final AtomicInteger processed = new AtomicInteger(0);
}
