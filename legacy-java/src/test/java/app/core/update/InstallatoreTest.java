package app.core.update;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prove sulle due parti che possono sbagliare in silenzio: <b>lo script</b> che viene generato e
 * <b>la riga di rapporto</b> che viene riletta. Nessuna delle due esegue niente — se per provarle
 * servisse installare un MSI non le proverebbe mai nessuno, ed e' proprio il codice che gira una
 * volta sola, sul PC di qualcun altro, mentre l'applicazione e' chiusa.
 */
class InstallatoreTest {

    private static String script() {
        return Installatore.script(4242,
                Path.of("C:\\Users\\tizio\\AppData\\Local\\rinominaPDF\\aggiornamento\\rinominaPDF-1.0.8.msi"),
                Path.of("C:\\Program Files\\rinominaPDF\\rinominaPDF.exe"),
                Path.of("C:\\Users\\tizio\\AppData\\Local\\rinominaPDF\\aggiornamento\\esito.txt"),
                "1.0.8");
    }

    @Test
    @DisplayName("aspetta il PID dell'applicazione, e se non si chiude non installa niente")
    void aspettaLaChiusura() {
        String s = script();
        assertTrue(s.contains("Wait-Process -Id 4242 -Timeout " + Installatore.ATTESA_CHIUSURA_S));
        assertTrue(s.contains("Riporta " + Installatore.NON_CHIUSA));
        // La rinuncia deve venire PRIMA di msiexec: un ordine invertito installerebbe sopra
        // un'applicazione aperta, che e' il caso che tutta questa attesa serve a evitare.
        assertTrue(s.indexOf("Riporta " + Installatore.NON_CHIUSA) < s.indexOf("msiexec.exe"));
    }

    @Test
    @DisplayName("msiexec: silenzioso, senza riavvio, e senza REINSTALL (qui e' sempre un aggiornamento)")
    void argomentiDiMsiexec() {
        String s = script();
        assertTrue(s.contains("'/i',$msi,'/qn','/norestart'"));
        assertFalse(s.contains("REINSTALL"));
    }

    @Test
    @DisplayName("una sola elevazione, e il riavvio dell'app NON e' dentro il processo elevato")
    void unaSolaElevazioneEIlRiavvioNonElevato() {
        String s = script();
        assertEquals(1, s.split("-Verb RunAs", -1).length - 1);
        // Se Start-Process dell'eseguibile stesse dopo un -Verb RunAs sulla stessa riga, l'app
        // ripartirebbe come amministratore e leggerebbe il %LOCALAPPDATA% sbagliato: run,
        // impostazioni e credenziali dell'utente sparirebbero alla vista.
        assertFalse(s.contains("-Verb RunAs -FilePath $exe"));
        assertTrue(s.contains("Start-Process -FilePath $exe"));
        assertTrue(s.indexOf("Start-Process -FilePath $exe") > s.indexOf("msiexec.exe"));
    }

    @Test
    @DisplayName("il pacchetto si cancella solo se e' stato davvero installato")
    void ilPacchettoSiCancellaSoloSeInstallato() {
        String s = script();
        assertTrue(s.contains("if ($codice -eq 0 -or $codice -eq 3010) {"));
        assertTrue(s.contains("Remove-Item -LiteralPath $msi"));
    }

    @Test
    @DisplayName("i percorsi sono letterali PowerShell: niente espansione di $ o backtick")
    void percorsiLetterali() {
        assertEquals("'C:\\Program Files\\app.exe'", Installatore.letterale("C:\\Program Files\\app.exe"));
        assertEquals("'C:\\Users\\l''utente\\x.msi'", Installatore.letterale("C:\\Users\\l'utente\\x.msi"));
        // Un percorso con un $ dentro e' legittimo su Windows (le condivisioni amministrative si
        // chiamano cosi'): fra apici singoli non viene espanso, quindi resta com'e'.
        assertEquals("'\\\\pc\\c$\\x.msi'", Installatore.letterale("\\\\pc\\c$\\x.msi"));
    }

    @Test
    @DisplayName("la riga di rapporto: versione e codice")
    void rapportoLetto() {
        Optional<Installatore.Esito> e = Installatore.interpreta("ESITO\t1.0.8\t0");
        assertTrue(e.isPresent());
        assertEquals("1.0.8", e.get().versione());
        assertEquals(0, e.get().codice());
        assertTrue(e.get().ok());
    }

    @Test
    @DisplayName("il BOM di PowerShell 5.1 davanti alla riga non fa sparire il rapporto")
    void rapportoConIlBom() {
        Optional<Installatore.Esito> e = Installatore.interpreta("\uFEFFESITO\t1.0.8\t1603");
        assertTrue(e.isPresent());
        assertEquals(1603, e.get().codice());
        assertFalse(e.get().ok());
    }

    @Test
    @DisplayName("3010 e' completato: serve solo un riavvio del PC")
    void riavvioRichiestoNonEUnFallimento() {
        assertTrue(Installatore.interpreta("ESITO\t1.0.8\t3010").orElseThrow().ok());
    }

    @Test
    @DisplayName("righe che non sono un rapporto vengono ignorate, non fanno fallire la lettura")
    void righeNonRapporto() {
        assertTrue(Installatore.interpreta(null).isEmpty());
        assertTrue(Installatore.interpreta("").isEmpty());
        assertTrue(Installatore.interpreta("qualcosa d'altro").isEmpty());
        assertTrue(Installatore.interpreta("ESITO\t1.0.8").isEmpty());
        assertTrue(Installatore.interpreta("ESITO\t1.0.8\tboh").isEmpty());
    }

    @Test
    @DisplayName("un codice sconosciuto si mostra come numero, non come frase inventata")
    void codiceSconosciuto() {
        assertTrue(Installatore.spiega(1234).contains("1234"));
        assertEquals("annullato", Installatore.spiega(1602));
    }

    @Test
    @DisplayName("senza jpackage.app-path non si sta girando da un'installazione: niente da aggiornare")
    void senzaInstallazioneNonSiInstalla() {
        String prima = System.getProperty("jpackage.app-path");
        System.clearProperty("jpackage.app-path");
        try {
            assertFalse(Installatore.possibile());
            assertTrue(Installatore.eseguibileInstallato().isEmpty());
        } finally {
            if (prima != null) System.setProperty("jpackage.app-path", prima);
        }
    }
}
