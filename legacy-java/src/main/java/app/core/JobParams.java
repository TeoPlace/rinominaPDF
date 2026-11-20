package app.core;

import java.nio.file.Path;

/**
 * Parametri inseriti dall'utente nel form iniziale, già validati.
 *
 * @param input  cartella con i PDF da elaborare
 * @param output cartella di destinazione dei PDF rinominati
 * @param esito  uno dei valori di {@link Globals#ESITI}
 * @param istat  codice istat con zero-padding a 9 cifre
 * @param anno   anno a 4 cifre
 * @param pacco  identificativo pacco
 */
public record JobParams(Path input, Path output, String esito, String istat, String anno, String pacco) {

    /** Nome di destinazione: barcode-sorter-esito-istat-anno-pacco.pdf */
    public String buildFileName(String barcode) {
        return String.join("-", barcode, Globals.FIXED_TEXT, esito, istat, anno, pacco) + ".pdf";
    }

    public static String padIstat(String raw) {
        String digits = raw.trim();
        return "0".repeat(Math.max(0, Globals.ISTAT_LENGTH - digits.length())) + digits;
    }
}
