package app.core.update;

import app.AppInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prove sulla parte che puo' mentire in modo credibile: la decisione "c'e' un aggiornamento?".
 * La rete non c'entra — {@link UpdateCheck#decidi} e' separata apposta.
 */
class UpdateCheckTest {

    private static Manifest pubblicato(String versione, String file) {
        return new Manifest(true, versione, file, "72,8 MB", "2026-08-16", null, null);
    }

    @Test
    @DisplayName("una versione piu' recente viene offerta, con l'URL completo del pacchetto")
    void versionePiuRecenteVieneOfferta() {
        Optional<UpdateCheck.Update> u =
                UpdateCheck.decidi(pubblicato("1.0.1", "rinominaPDF-1.0.1.msi"), "1.0.0");

        assertTrue(u.isPresent());
        assertEquals("1.0.1", u.get().versione());
        assertEquals("https://rinominapdf.teo.place/scarica/rinominaPDF-1.0.1.msi", u.get().url());
        assertEquals("72,8 MB", u.get().dimensione());
        assertNull(u.get().urlAlternativo());
    }

    @Test
    @DisplayName("la stessa versione non viene offerta")
    void stessaVersioneNonVieneOfferta() {
        assertTrue(UpdateCheck.decidi(pubblicato("1.0.1", "x.msi"), "1.0.1").isEmpty());
    }

    @Test
    @DisplayName("una versione precedente non viene offerta")
    void versionePrecedenteNonVieneOfferta() {
        assertTrue(UpdateCheck.decidi(pubblicato("1.0.0", "x.msi"), "1.0.1").isEmpty());
    }

    @Test
    @DisplayName("1.0 e 1.0.0 sono lo stesso numero: non e' un aggiornamento")
    void unoPuntoZeroNonAggiornaUnoPuntoZeroPuntoZero() {
        // Il caso vero della linea: gli MSI distribuiti dichiaravano 1.0 mentre i pom dicevano
        // 1.0.0. Un confronto fra stringhe direbbe che sono diverse e offrirebbe un aggiornamento
        // che poi risponde "il programma e' gia' installato".
        assertTrue(UpdateCheck.decidi(pubblicato("1.0", "x.msi"), "1.0.0").isEmpty());
        assertTrue(UpdateCheck.decidi(pubblicato("1.0.0", "x.msi"), "1.0").isEmpty());
    }

    @Test
    @DisplayName("un quarto campo non conta")
    void ilQuartoCampoNonConta() {
        assertTrue(UpdateCheck.decidi(pubblicato("1.0.1.7", "x.msi"), "1.0.1").isEmpty());
    }

    @Test
    @DisplayName("manifest con disponibile:false non offre niente")
    void nonDisponibileNonOffreNiente() {
        Manifest m = new Manifest(false, null, null, null, null, null, null);
        assertTrue(UpdateCheck.decidi(m, "1.0.0").isEmpty());
    }

    @Test
    @DisplayName("disponibile:true ma senza file e' un manifest scritto male: non si offre")
    void disponibileSenzaFileNonSiOffre() {
        // Trattarlo come buono manderebbe l'utente su un URL che finisce con "null".
        Manifest m = new Manifest(true, "9.9.9", null, null, null, null, null);
        assertTrue(UpdateCheck.decidi(m, "1.0.0").isEmpty());
    }

    @Test
    @DisplayName("il secondo pacchetto viene offerto quando il manifest lo pubblica")
    void ilSecondoPacchettoVieneOfferto() {
        Manifest alt = new Manifest(true, "1.0.1", "rinominaPDF-portable-1.0.1.msi", "72,9 MB", "2026-08-16", null, null);
        Manifest m = new Manifest(true, "1.0.1", "rinominaPDF-1.0.1.msi", "72,8 MB", "2026-08-16", null, alt);

        Optional<UpdateCheck.Update> u = UpdateCheck.decidi(m, "1.0.0");

        assertTrue(u.isPresent());
        assertEquals("https://rinominapdf.teo.place/scarica/rinominaPDF-portable-1.0.1.msi", u.get().urlAlternativo());
    }

    @Test
    @DisplayName("un alternativo senza file non produce un secondo pulsante rotto")
    void alternativoSenzaFileNonProduceUnPulsanteRotto() {
        Manifest alt = new Manifest(true, "1.0.1", null, null, null, null, null);
        Manifest m = new Manifest(true, "1.0.1", "rinominaPDF-1.0.1.msi", null, null, null, alt);

        assertNull(UpdateCheck.decidi(m, "1.0.0").orElseThrow().urlAlternativo());
    }

    @Test
    @DisplayName("il nome del file e l'impronta arrivano fino a chi installa")
    void nomeFileEImprontaArrivanoAllInstallatore() {
        Manifest m = new Manifest(true, "1.0.8", "rinominaPDF-1.0.8.msi", "90,9 MB", "2026-08-20",
                "1a56831ccfceeec2b8869be74b95b12d965a98765b3b0c2d1d6c8e9ab6833cd4", null);

        UpdateCheck.Update u = UpdateCheck.decidi(m, "1.0.7").orElseThrow();

        // Il nome serve a salvare il pacchetto con il suo nome (due versioni non si sovrascrivono),
        // l'impronta a verificarlo prima di passarlo a msiexec: gli MSI non sono firmati.
        assertEquals("rinominaPDF-1.0.8.msi", u.file());
        assertEquals("1a56831ccfceeec2b8869be74b95b12d965a98765b3b0c2d1d6c8e9ab6833cd4", u.sha256());
    }

    @Test
    @DisplayName("un manifest senza impronta si offre lo stesso: il campo e' facoltativo")
    void senzaImprontaSiOffreLoStesso() {
        UpdateCheck.Update u = UpdateCheck.decidi(pubblicato("1.0.8", "x.msi"), "1.0.7").orElseThrow();
        assertNull(u.sha256());
    }

    @Test
    @DisplayName("l'URL del manifest si deriva da HOME_URL, non e' scritto a mano")
    void urlManifestDerivatoDaHomeUrl() {
        assertEquals("https://rinominapdf.teo.place/scarica/versione.json", UpdateCheck.urlManifest());
        assertTrue(UpdateCheck.urlManifest().startsWith(AppInfo.HOME_URL));
    }
}
