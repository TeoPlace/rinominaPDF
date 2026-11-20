package app.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoricoStoreTest {

    @TempDir
    Path dir;

    private static Elaborazione inizio(long quando) {
        return Elaborazione.inizio(new JobParams(Path.of("C:/in"), Path.of("C:/out"), "CONSEGNATO",
                "000063064", "2026", "MP297"), quando);
    }

    @Test
    void cartellaSenzaFileValeStoricoVuoto() {
        assertTrue(new StoricoStore(dir).carica().isEmpty());
    }

    @Test
    void rilegge_quello_che_ha_scritto() {
        Elaborazione e = inizio(1000).fine(3, 1, 0, false, 2000);
        new StoricoStore(dir).salva(List.of(e));
        assertEquals(List.of(e), new StoricoStore(dir).carica());
    }

    /** Il programma e' morto a meta': alla riapertura non puo' sembrare ancora in corso. */
    @Test
    void una_voce_rimasta_in_corso_si_rilegge_interrotta() {
        new StoricoStore(dir).salva(List.of(inizio(1000)));
        Elaborazione e = new StoricoStore(dir).carica().get(0);
        assertEquals(Elaborazione.Stato.ERRORE, e.stato());
        assertTrue(e.interrotta());
    }

    @Test
    void un_file_rotto_vale_storico_vuoto() throws Exception {
        Files.writeString(dir.resolve("elaborazioni.json"), "{non e' json");
        assertTrue(new StoricoStore(dir).carica().isEmpty());
    }

    @Test
    void non_tiene_piu_del_massimo() {
        List<Elaborazione> tante = new ArrayList<>();
        for (int i = 0; i < StoricoStore.MASSIMO + 5; i++) tante.add(inizio(i + 1).fine(1, 0, 0, false, i + 2));
        new StoricoStore(dir).salva(tante);
        assertEquals(StoricoStore.MASSIMO, new StoricoStore(dir).carica().size());
    }

    @Test
    void il_pallino_dice_com_e_andata() {
        Elaborazione e = inizio(1000);
        assertEquals(Elaborazione.Stato.IN_CORSO, e.stato());
        assertEquals(Elaborazione.Stato.RIUSCITA, e.fine(5, 0, 0, false, 2).stato());
        assertEquals(Elaborazione.Stato.NON_LETTI, e.fine(5, 1, 0, false, 2).stato());
        assertEquals(Elaborazione.Stato.ERRORE, e.fine(5, 1, 1, false, 2).stato());
        assertEquals(Elaborazione.Stato.ERRORE, e.fine(5, 0, 0, true, 2).stato());
        assertEquals(Elaborazione.Stato.VUOTA, e.fine(0, 0, 0, false, 2).stato());
    }
}
