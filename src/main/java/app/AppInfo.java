package app;

/**
 * Identita' del prodotto. {@link #VERSION} e' l'UNICO posto nel codice in cui la versione e'
 * scritta: se la UI un giorno la mostrera', la legge da qui e non da un letterale.
 *
 * <p>Va tenuta allineata a {@code pom.xml} e {@code build-msi.ps1}: lo verifica {@code VersionTest}.
 * Vedi {@code STANDARD.md} per il perche' della forma a tre campi.
 */
public final class AppInfo {

    public static final String NAME = "rinominaPDF";
    public static final String VERSION = "1.0.0";
    public static final String VENDOR = "teo.place";

    private AppInfo() {}
}
