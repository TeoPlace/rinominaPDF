package app.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Scelta del barcode giusto fra quelli letti su un PDF: le famiglie di {@link Globals#BARCODE_FAMILIES}. */
class BarcodeReaderTest {

    @Test
    void dueLettereDieciCifre() {
        assertEquals("RB0002858945", BarcodeReader.selectRight(List.of("RB0002858945")));
        assertEquals("AC0002382099", BarcodeReader.selectRight(List.of("8001234567890", "AC0002382099")));
    }

    /** Il caso di Jenny: ZXing leggeva questi codici e la 1.0.1 li scartava. */
    @Test
    void dodiciCifre() {
        assertEquals("286524542334", BarcodeReader.selectRight(List.of("286524542334")));
        assertEquals("393673094350", BarcodeReader.selectRight(List.of("393673094350")));
        assertEquals("612345678901", BarcodeReader.selectRight(List.of("612345678901")));
        assertEquals("712345678901", BarcodeReader.selectRight(List.of("712345678901")));
    }

    @Test
    void dodiciCifreConPrimaCifraFuoriFamigliaNonValgono() {
        assertNull(BarcodeReader.selectRight(List.of("186524542334")));
        assertNull(BarcodeReader.selectRight(List.of("486524542334")));
        assertNull(BarcodeReader.selectRight(List.of("28652454233")));   // 11 cifre
        assertNull(BarcodeReader.selectRight(List.of("2865245423345"))); // 13 cifre (un EAN)
    }

    @Test
    void abwAcw() {
        assertEquals("ABW12345678", BarcodeReader.selectRight(List.of("ABW12345678")));
        assertEquals("ACW12345678", BarcodeReader.selectRight(List.of("ACW12345678")));
        assertNull(BarcodeReader.selectRight(List.of("ADW12345678")));
        assertNull(BarcodeReader.selectRight(List.of("ABW1234567")));
    }

    @Test
    void diciassetteCifreAncheConArg() {
        assertEquals("12345678901234567", BarcodeReader.selectRight(List.of("12345678901234567")));
        assertEquals("ARG12345678901234567", BarcodeReader.selectRight(List.of("ARG12345678901234567")));
        assertNull(BarcodeReader.selectRight(List.of("1234567890123456")));
    }

    /** Con codici di piu' famiglie sulla stessa busta vince quello che la 1.0.1 avrebbe gia' scelto. */
    @Test
    void dueLettereHannoLaPrecedenza() {
        assertEquals("AB0002434559",
                BarcodeReader.selectRight(List.of("286524542334", "ABW12345678", "AB0002434559")));
        assertEquals("286524542334",
                BarcodeReader.selectRight(List.of("12345678901234567", "286524542334")));
    }

    @Test
    void sottostringaDiUn2D() {
        assertEquals("AC0002382099", BarcodeReader.selectRight(List.of("X|AC0002382099|2026")));
        assertEquals("ABW12345678", BarcodeReader.selectRight(List.of("X|ABW12345678|2026")));
    }

    /** Un 12 cifre in mezzo a cifre piu' lunghe non e' un codice: verrebbe inventato. */
    @Test
    void niente12o17CifreComeSottostringa() {
        assertNull(BarcodeReader.selectRight(List.of("0028652454233499")));
        assertNull(BarcodeReader.selectRight(List.of("X|12345678901234567890")));
    }

    @Test
    void nienteDiValido() {
        assertNull(BarcodeReader.selectRight(List.of()));
        assertNull(BarcodeReader.selectRight(List.of("8001234567890", "HELLO")));
    }
}
