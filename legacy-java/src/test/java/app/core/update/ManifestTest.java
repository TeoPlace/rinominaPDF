package app.core.update;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Il parser del {@code versione.json}, provato sui payload <b>veri</b> pubblicati dai siti. */
class ManifestTest {

    @Test
    @DisplayName("un payload nella forma pubblicata dai siti della linea")
    void payloadReale() {
        Manifest m = Manifest.parse("""
                { "disponibile": true, "versione": "1.0", "file": "rinominaPDF-1.0.msi",
                  "dimensione": "72,8 MB", "data": "2026-08-07" }
                """);

        assertTrue(m.disponibile());
        assertEquals("1.0", m.versione());
        assertEquals("rinominaPDF-1.0.msi", m.file());
        assertEquals("72,8 MB", m.dimensione());
        assertEquals("2026-08-07", m.data());
        assertNull(m.alternativo());
        assertTrue(m.scaricabile());
    }

    @Test
    @DisplayName("il payload reale di un prodotto senza pacchetto: campi null, nessun errore")
    void payloadNonDisponibile() {
        Manifest m = Manifest.parse("""
                { "disponibile": false, "versione": null, "file": null,
                  "dimensione": null, "data": null }
                """);

        assertFalse(m.disponibile());
        assertNull(m.versione());
        assertFalse(m.scaricabile());
    }

    @Test
    @DisplayName("il secondo installer si legge dal campo alternativo")
    void alternativo() {
        Manifest m = Manifest.parse("""
                { "disponibile": true, "versione": "1.0.1", "file": "rinominaPDF-1.0.1.msi",
                  "dimensione": "72,8 MB", "data": "2026-08-16",
                  "alternativo": { "disponibile": true, "versione": "1.0.1",
                                   "file": "rinominaPDF-portable-1.0.1.msi",
                                   "dimensione": "72,9 MB", "data": "2026-08-16" } }
                """);

        assertNotNull(m.alternativo());
        assertEquals("rinominaPDF-portable-1.0.1.msi", m.alternativo().file());
        assertTrue(m.alternativo().scaricabile());
    }

    @Test
    @DisplayName("l'impronta si legge, e la sua assenza non e' un errore")
    void impronta() {
        Manifest con = Manifest.parse("""
                { "disponibile": true, "versione": "1.0.7", "file": "rinominaPDF-1.0.7.msi",
                  "dimensione": "90,9 MB", "data": "2026-08-20",
                  "sha256": "1a56831ccfceeec2b8869be74b95b12d965a98765b3b0c2d1d6c8e9ab6833cd4" }
                """);
        assertEquals("1a56831ccfceeec2b8869be74b95b12d965a98765b3b0c2d1d6c8e9ab6833cd4", con.sha256());

        // I manifest scritti prima che il campo esistesse restano validi: senza impronta si
        // scarica come si e' sempre fatto, non ci si rifiuta di aggiornare.
        Manifest senza = Manifest.parse("""
                { "disponibile": true, "versione": "1.0.1", "file": "x.msi" }
                """);
        assertNull(senza.sha256());
        assertTrue(senza.scaricabile());
    }

    @Test
    @DisplayName("un campo in piu' non rompe niente: il sito puo' aggiungerne")
    void campoInPiuNonRompeNiente() {
        Manifest m = Manifest.parse("""
                { "disponibile": true, "versione": "1.0.1", "file": "x.msi",
                  "note": "qualcosa di nuovo", "sha256": "abc" }
                """);

        assertTrue(m.scaricabile());
        assertEquals("1.0.1", m.versione());
    }

    @Test
    @DisplayName("JSON malformato vale 'non disponibile', non un'eccezione")
    void jsonMalformato() {
        // Un'eccezione qui diventerebbe un avviso di aggiornamento che non compare mai.
        assertFalse(Manifest.parse("{ non e' json").disponibile());
        assertFalse(Manifest.parse("").disponibile());
        assertFalse(Manifest.parse("[1,2,3]").disponibile());
        assertFalse(Manifest.parse("null").disponibile());
    }

    @Test
    @DisplayName("una stringa vuota vale null: non e' un nome di file")
    void stringaVuotaValeNull() {
        Manifest m = Manifest.parse("""
                { "disponibile": true, "versione": "1.0.1", "file": "   " }
                """);

        assertNull(m.file());
        assertFalse(m.scaricabile());
    }
}
