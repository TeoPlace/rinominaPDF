package app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La versione e' scritta in tre posti che non si vedono fra loro: {@code pom.xml} (da cui prende il
 * nome il fat-jar), {@link AppInfo#VERSION} (quello che l'utente legge) e {@code build-msi.ps1}
 * (nome del jar da impacchettare e {@code app-version} dell'MSI).
 *
 * <p>Che divergano non da' nessun errore al momento giusto: il build fallisce piu' tardi con
 * "fat-jar non trovato", oppure — molto peggio — riesce e produce un MSI che si dichiara di una
 * versione mentre il programma dentro ne mostra un'altra.
 *
 * <p>La forma a <b>tre campi</b> non e' estetica: Windows decide se un pacchetto e' un aggiornamento
 * confrontando l'UpgradeCode e i primi tre campi della versione. La 1.0.0 e' gia' distribuita,
 * quindi il prossimo pacchetto dev'essere almeno 1.0.1.
 */
class VersionTest {

    private static final Pattern THREE_FIELDS = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    @Test
    void theVersionHasThreeIncreasingFields() {
        assertTrue(THREE_FIELDS.matcher(AppInfo.VERSION).matches(),
                "la versione dev'essere x.y.z, altrimenti Windows non vede gli aggiornamenti: "
                        + AppInfo.VERSION);
    }

    @Test
    void pomAndBuildScriptAgreeWithAppInfo() throws IOException {
        assertEquals(AppInfo.VERSION, firstGroup(Path.of("pom.xml"),
                        "<artifactId>rinominapdf</artifactId>\\s*(?:<!--.*?-->\\s*)?<version>([^<]+)</version>"),
                "pom.xml e AppInfo.VERSION non dicono la stessa versione");

        assertEquals(AppInfo.VERSION, firstGroup(Path.of("build-msi.ps1"),
                        "\\$version\\s*=\\s*\"([^\"]+)\""),
                "build-msi.ps1 e AppInfo.VERSION non dicono la stessa versione");
    }

    /**
     * L'UpgradeCode e' l'identita' del prodotto per Windows: cambiarlo trasforma ogni aggiornamento
     * futuro in una seconda installazione affiancata. Serve congelato soprattutto qui, dove il
     * vendor e' cambiato da "TeoPlace" a "teo.place": jpackage lo deriva da
     * "UpgradeCode/&lt;vendor&gt;/&lt;nome&gt;", quindi senza il valore esplicito nello script il
     * cambio di vendor avrebbe affiancato un secondo rinominaPDF a chi ha gia' la 1.0.0.
     */
    @Test
    void theUpgradeCodeIsFrozen() throws IOException {
        String script = Files.readString(Path.of("build-msi.ps1"), StandardCharsets.UTF_8);
        assertTrue(script.contains("6E50594E-22A6-3AC3-A7FB-48CFCA119568"),
                "l'UpgradeCode e' cambiato: chi ha gia' rinominaPDF si ritroverebbe due voci");
    }

    /** Il vendor entra nella riga "Editore" di App installate ed e' unificato su tutta la cartella. */
    @Test
    void theVendorIsTheSharedOne() throws IOException {
        assertEquals("teo.place", firstGroup(Path.of("build-msi.ps1"),
                        "\\$vendor\\s*=\\s*\"([^\"]+)\""),
                "il vendor deve restare quello comune a tutti i progetti");
    }

    private static String firstGroup(Path file, String regex) throws IOException {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL)
                .matcher(Files.readString(file, StandardCharsets.UTF_8));
        assertTrue(m.find(), "versione non trovata in " + file);
        String v = m.group(1).trim();
        assertNotNull(v);
        return v;
    }
}
