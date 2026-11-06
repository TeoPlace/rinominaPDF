package app.core.update;

/**
 * Confronto di versioni <b>con la regola di Windows</b>, che non e' quella che verrebbe naturale.
 *
 * <p>Windows Installer confronta i <b>primi tre campi</b> e basta. Due conseguenze che decidono se
 * l'avviso di aggiornamento dice la verita':
 *
 * <ul>
 *   <li>{@code "1.0"} e {@code "1.0.0"} sono lo <b>stesso</b> numero. Un confronto fra stringhe
 *       direbbe di no, e l'applicazione offrirebbe un aggiornamento che una volta scaricato
 *       risponde "il programma e' gia' installato" — cioe' darebbe la colpa al programma di una
 *       cosa decisa dal numero. Non e' un caso teorico: gli MSI distribuiti di TraceFox e
 *       listBigStorage dichiarano {@code 1.0} mentre i pom dicevano {@code 1.0.0}.</li>
 *   <li>Un eventuale <b>quarto</b> campo non conta: {@code 1.0.0.7} non aggiorna {@code 1.0.0}.</li>
 * </ul>
 *
 * <p>Stessa regola di {@code teoSetup}, dove il wizard la usa per decidere cosa proporre. Le due
 * copie devono restare d'accordo: se divergono, l'applicazione e il wizard direbbero due cose
 * diverse sullo stesso pacchetto.
 */
public final class Version {

    private Version() {}

    /** Negativo se {@code a} e' precedente, zero se per Windows sono la stessa, positivo se successiva. */
    public static int compare(String a, String b) {
        int[] x = fields(a);
        int[] y = fields(b);
        for (int i = 0; i < 3; i++) {
            int d = Integer.compare(x[i], y[i]);
            if (d != 0) return d;
        }
        return 0;
    }

    /** {@code true} se {@code candidate} verrebbe vista da Windows come un aggiornamento di {@code installed}. */
    public static boolean isNewer(String candidate, String installed) {
        return compare(candidate, installed) > 0;
    }

    /**
     * I tre campi, con gli assenti a zero. Tollerante di proposito: quello che arriva e' scritto a
     * mano in un JSON sul server, e un numero malformato non deve far esplodere l'avvio — vale
     * zero, che nel dubbio significa "non aggiorna niente", cioe' l'errore innocuo.
     */
    static int[] fields(String v) {
        int[] out = new int[3];
        if (v == null) return out;
        String[] parts = v.trim().split("\\.");
        for (int i = 0; i < 3 && i < parts.length; i++) {
            out[i] = number(parts[i]);
        }
        return out;
    }

    private static int number(String s) {
        StringBuilder digits = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
            else break;
        }
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
