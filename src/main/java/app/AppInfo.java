package app;

import java.nio.file.Path;

/**
 * Identita' del prodotto. {@link #VERSION} e' l'UNICO posto nel codice in cui la versione e'
 * scritta: la UI la legge da qui e non da un letterale.
 *
 * <p>Va tenuta allineata a {@code pom.xml} e {@code build-msi.ps1}: lo verifica {@code VersionTest}.
 * Vedi {@code STANDARD.md} per il perche' della forma a tre campi.
 */
public final class AppInfo {

    public static final String NAME = "rinominaPDF";
    public static final String VERSION = "1.0.3";
    public static final String VENDOR = "teo.place";
    /**
     * Vetrina del prodotto. Da qui si ricava anche l'indirizzo del manifest degli aggiornamenti
     * ({@code <HOME_URL>scarica/versione.json}, vedi {@code core.update.UpdateCheck}): scriverlo
     * una volta sola evita che le due cose divergano il giorno che il dominio cambia.
     */
    public static final String HOME_URL = "https://rinominapdf.teo.place/";

    private AppInfo() {}

    /**
     * {@code %LOCALAPPDATA%\rinominaPDF}: oggi ci stanno solo il pacchetto scaricato e il rapporto
     * dell'aggiornamento. Non Roaming, che OneDrive sincronizza (vedi STANDARD.md).
     */
    public static Path controlDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) return Path.of(localAppData, NAME);
        return Path.of(System.getProperty("user.home"), "." + NAME);
    }
}
