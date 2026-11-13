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
     * Barcode "giusto": le famiglie di codici di una busta riconosciute da SorterNext
     * ({@code BarcodeEquivalence} in sorter-next), in ordine di preferenza.
     * <ul>
     *   <li>2 lettere + 10 cifre: RB0002858945, AC0002382099</li>
     *   <li>12 cifre che iniziano per 2, 3, 6 o 7: 286524542334</li>
     *   <li>ABW / ACW + 8 cifre: ABW12345678</li>
     *   <li>17 cifre, anche precedute da ARG: ARG12345678901234567</li>
     * </ul>
     * Fino alla 1.0.1 c'era solo la prima: le altre venivano lette e poi scartate, e il PDF
     * finiva fra i non letti. L'ordine conta quando su una pagina ci sono codici di piu'
     * famiglie: le 2 lettere + 10 cifre restano prime, cosi' i PDF che si rinominavano gia'
     * prendono ancora lo stesso nome.
     */
    public static final List<Pattern> BARCODE_FAMILIES = List.of(
            Pattern.compile("^[A-Z]{2}\\d{10}$"),
            Pattern.compile("^[2367]\\d{11}$"),
            Pattern.compile("^A[BC]W\\d{8}$"),
            Pattern.compile("^(?:ARG)?\\d{17}$"));

    /**
     * DPI con cui si renderizzano le pagine dei PDF che sorter_barcode non sa aprire da solo, e quelle
     * di RenderPagina.
     */
    public static final int RENDER_DPI = 300;

    /**
     * Numero di thread di lavoro corrente: parte da 1 e viene regolato
     * dinamicamente in base a CPU/disco (stesso meccanismo di sorter-fx).
     */
    public static volatile int totalThreads = 1;

    /** Contatore globale dei file processati (per la progress bar). */
    public static final AtomicInteger processed = new AtomicInteger(0);
}
