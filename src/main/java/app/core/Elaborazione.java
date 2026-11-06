package app.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Una voce della barra di sinistra: cosa e' stato chiesto e com'e' andata. {@code finitaIl} vale 0
 * finche' e' in corso. Immutabile: alla fine la voce si sostituisce.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Elaborazione(long iniziataIl, long finitaIl, String input, String output, String esito,
                           String istat, String anno, String pacco,
                           int rinominati, int nonLetti, int errori, boolean interrotta) {

    public enum Stato { IN_CORSO, RIUSCITA, NON_LETTI, VUOTA, ERRORE }

    public static Elaborazione inizio(JobParams p, long adesso) {
        return new Elaborazione(adesso, 0, p.input().toString(), p.output().toString(), p.esito(),
                p.istat(), p.anno(), p.pacco(), 0, 0, 0, false);
    }

    public Elaborazione fine(int ok, int unread, int errors, boolean aborted, long adesso) {
        return new Elaborazione(iniziataIl, adesso, input, output, esito, istat, anno, pacco,
                ok, unread, errors, aborted);
    }

    /** Il pallino: un elenco piu' corto del previsto non deve sembrare un lavoro andato bene. */
    public Stato stato() {
        if (finitaIl == 0) return Stato.IN_CORSO;
        if (interrotta || errori > 0) return Stato.ERRORE;
        if (nonLetti > 0) return Stato.NON_LETTI;
        return rinominati == 0 ? Stato.VUOTA : Stato.RIUSCITA;
    }
}
