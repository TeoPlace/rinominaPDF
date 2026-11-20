package app.core.update;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * Scarica il pacchetto dell'aggiornamento mostrando quanto manca, e non lascia mai in giro un file
 * a meta'.
 *
 * <p>E' la stessa classe di {@code teoSetup} (da cui e' copiata, come tutte le classi condivise
 * della linea): il wizard scarica gli stessi MSI dagli stessi indirizzi, e due implementazioni
 * diverse dello stesso download vorrebbero dire due comportamenti diversi sullo stesso file.
 *
 * <p><b>Una differenza voluta dalla copia di teoSetup</b>: qui l'avanzamento porta anche il totale
 * dichiarato dal server ({@link Avanzamento}), non i soli byte scaricati. Serve per una barra
 * <b>determinata</b>: il manifest dichiara "90,9 MB", cioe' un arrotondamento, e una percentuale
 * calcolata su quello arriverebbe al 101% — mentre il {@code Content-Length} e' esatto.
 *
 * <p><b>Mai dal thread FX</b>: novanta megabyte su una linea lenta sono minuti.
 */
public final class Scaricatore {

    private static final System.Logger log = System.getLogger(Scaricatore.class.getName());

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Esito(Path file, long byteTotali, boolean giaInCache) {}

    /**
     * Quanto e' arrivato e quanto ne e' atteso in tutto ({@code -1} se il server non lo dichiara).
     *
     * <p>Viene chiamato <b>spessissimo</b>, una volta per blocco letto: chi lo implementa non deve
     * accodare niente sul thread FX senza limitare la frequenza, o l'interfaccia si blocca proprio
     * mentre dovrebbe mostrare che sta succedendo qualcosa.
     */
    @FunctionalInterface
    public interface Avanzamento {
        void aggiorna(long scaricati, long totali);
    }

    /**
     * Scarica {@code url} in {@code destinazione} e ne verifica l'impronta.
     *
     * <p>Quattro cose che decidono se e' affidabile:
     *
     * <ul>
     *   <li><b>Si scrive su un {@code .parziale} e si rinomina alla fine.</b> Un download
     *       interrotto lascerebbe altrimenti un file della dimensione sbagliata ma con il nome
     *       giusto, e il giro dopo verrebbe passato a msiexec come se fosse buono: l'errore
     *       comparirebbe a installazione avviata, dove nessuno lo collega alla rete caduta.</li>
     *   <li><b>Si controlla la lunghezza.</b> Se il server dichiara N byte e ne arrivano meno, e'
     *       un fallimento, non un file corto.</li>
     *   <li><b>Si controlla l'impronta SHA-256</b> prima del rename, perche' gli MSI della linea
     *       non sono firmati: e' l'unica cosa che distingue il pacchetto giusto da uno troncato o
     *       sostituito, e va vista mentre e' ancora un file su disco.</li>
     *   <li><b>Un file gia' in cache con la dimensione e l'impronta giuste non si riscarica</b>:
     *       rimandare l'aggiornamento e riproporlo domani non deve costare altri novanta
     *       megabyte.</li>
     * </ul>
     *
     * <p>{@code sha256Atteso} nullo o vuoto = nessuna verifica. E' voluto: i manifest scritti prima
     * che il campo esistesse devono continuare a funzionare, e un aggiornamento che si rifiuta di
     * partire perche' il sito non pubblica ancora l'impronta sarebbe rotto per una cosa che non e'
     * colpa di nessuno.
     *
     * @param avanzamento riceve byte scaricati e totale; chiamato spesso, <b>non</b> dal thread FX
     */
    public Esito scarica(String url, Path destinazione, String sha256Atteso, Avanzamento avanzamento)
            throws Exception {
        Files.createDirectories(destinazione.getParent());

        HttpRequest richiesta = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(30))
                .header("User-Agent", app.AppInfo.NAME + "/" + app.AppInfo.VERSION)
                .GET()
                .build();

        HttpResponse<InputStream> risposta =
                http.send(richiesta, HttpResponse.BodyHandlers.ofInputStream());
        if (risposta.statusCode() / 100 != 2) {
            risposta.body().close();
            throw new java.io.IOException("il server ha risposto " + risposta.statusCode() + " per " + url);
        }

        long dichiarati = risposta.headers().firstValueAsLong("Content-Length").orElse(-1);

        if (dichiarati > 0 && Files.isRegularFile(destinazione) && Files.size(destinazione) == dichiarati) {
            // La dimensione giusta non basta a dire che il file in cache e' quello giusto: se c'e'
            // un'impronta si controlla anche qui, altrimenti una copia corrotta della volta scorsa
            // resterebbe buona per sempre. Se non torna si riscarica, che e' la risposta utile.
            if (impronteCoincidono(sha256Atteso, destinazione)) {
                risposta.body().close();
                log.log(System.Logger.Level.INFO, "Aggiornamento gia' scaricato: " + destinazione.getFileName());
                avanzamento.aggiorna(dichiarati, dichiarati);
                return new Esito(destinazione, dichiarati, true);
            }
            log.log(System.Logger.Level.WARNING, destinazione.getFileName()
                    + ": la copia in cache non corrisponde all'impronta, la riscarico");
        }

        Path parziale = destinazione.resolveSibling(destinazione.getFileName() + ".parziale");
        long scritti = 0;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = risposta.body();
             OutputStream out = Files.newOutputStream(parziale)) {
            byte[] buffer = new byte[1 << 16];
            int letti;
            while ((letti = in.read(buffer)) > 0) {
                out.write(buffer, 0, letti);
                digest.update(buffer, 0, letti);
                scritti += letti;
                avanzamento.aggiorna(scritti, dichiarati);
            }
        } catch (Exception e) {
            Files.deleteIfExists(parziale);
            throw e;
        }

        if (dichiarati > 0 && scritti != dichiarati) {
            Files.deleteIfExists(parziale);
            throw new java.io.IOException("scaricati " + scritti + " byte invece di " + dichiarati
                    + ": la connessione si e' interrotta");
        }

        // Prima del rename, non dopo: un file con il nome definitivo e' un file che il giro
        // successivo considera buono.
        if (richiedeVerifica(sha256Atteso)) {
            String ottenuto = esadecimale(digest.digest());
            if (!ottenuto.equalsIgnoreCase(sha256Atteso.trim())) {
                Files.deleteIfExists(parziale);
                throw new java.io.IOException(
                        "il file scaricato non corrisponde all'impronta pubblicata dal sito"
                        + " (attesa " + sha256Atteso.trim() + ", ottenuta " + ottenuto + "):"
                        + " non viene installato");
            }
        }

        Files.move(parziale, destinazione, StandardCopyOption.REPLACE_EXISTING);
        return new Esito(destinazione, scritti, false);
    }

    private static boolean richiedeVerifica(String sha256Atteso) {
        return sha256Atteso != null && !sha256Atteso.isBlank();
    }

    /** {@code true} anche quando non c'e' niente da verificare: nessuna impronta, nessuna obiezione. */
    static boolean impronteCoincidono(String sha256Atteso, Path file) throws Exception {
        if (!richiedeVerifica(sha256Atteso)) return true;
        return impronta(file).equalsIgnoreCase(sha256Atteso.trim());
    }

    /** SHA-256 di un file gia' su disco, letto a blocchi: i pacchetti pesano decine di MB. */
    static String impronta(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[1 << 16];
            int letti;
            while ((letti = in.read(buffer)) > 0) {
                digest.update(buffer, 0, letti);
            }
        }
        return esadecimale(digest.digest());
    }

    static String esadecimale(byte[] byteImpronta) {
        StringBuilder sb = new StringBuilder(byteImpronta.length * 2);
        for (byte b : byteImpronta) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /** "72,8 MB" come lo scrive il resto della linea: virgola decimale, unita' binarie. */
    public static String dimensione(long byteTotali) {
        if (byteTotali < 1024) return byteTotali + " B";
        String[] unita = {"KB", "MB", "GB"};
        double v = byteTotali;
        int i = -1;
        do {
            v /= 1024;
            i++;
        } while (v >= 1024 && i < unita.length - 1);
        return String.format(java.util.Locale.ITALY, "%.1f %s", v, unita[i]);
    }
}
