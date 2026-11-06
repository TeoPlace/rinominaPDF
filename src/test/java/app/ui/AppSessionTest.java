package app.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Niente toolkit JavaFX: solo la classificazione delle righe, che decide cosa nasconde il filtro. */
class AppSessionTest {

    @Test
    void togliMillesimiETagEClassifica() {
        LogBuffer.Entry e = AppSession.voce("14:32:05.123 [rename] a.pdf -> b.pdf");
        assertEquals("14:32:05  a.pdf -> b.pdf\n", e.line());
        assertEquals(LogCategory.RINOMINATI, e.category());
    }

    @Test
    void ogniTagHaLaSuaCategoria() {
        assertEquals(LogCategory.NON_LETTI, AppSession.voce("14:32:05.123 [barcode] x").category());
        assertEquals(LogCategory.ERRORI, AppSession.voce("14:32:05.123 [errore] x").category());
        assertEquals(LogCategory.AGGIORNAMENTI, AppSession.voce("14:32:05.123 [update] x").category());
        assertEquals(LogCategory.LAVORO, AppSession.voce("14:32:05.123 [job] x").category());
        assertEquals(LogCategory.LAVORO, AppSession.voce("14:32:05.123 [wsl] x").category());
    }

    @Test
    void unaRigaFuoriFormatoNonSiPerde() {
        LogBuffer.Entry e = AppSession.voce("riga strana");
        assertEquals("riga strana\n", e.line());
        assertEquals(LogCategory.LAVORO, e.category());
    }
}
