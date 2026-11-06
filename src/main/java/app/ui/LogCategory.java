package app.ui;

/** Le categorie del registro: il filtro a destra nasconde per categoria, non seleziona. */
public enum LogCategory {
    RINOMINATI("Rinominati"),
    NON_LETTI("Non letti"),
    ERRORI("Errori"),
    LAVORO("Lavoro e lettore"),
    AGGIORNAMENTI("Aggiornamenti");

    public final String etichetta;

    LogCategory(String etichetta) {
        this.etichetta = etichetta;
    }

    /** Dal tag con cui i thread di lavoro scrivono in {@code LogQueue}. */
    public static LogCategory daTag(String tag) {
        return switch (tag) {
            case "rename" -> RINOMINATI;
            case "barcode" -> NON_LETTI;
            case "errore" -> ERRORI;
            case "update" -> AGGIORNAMENTI;
            default -> LAVORO;
        };
    }
}
