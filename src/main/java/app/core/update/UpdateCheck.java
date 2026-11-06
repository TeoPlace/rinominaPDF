package app.core.update;

import app.AppInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Chiede al sito prodotto se esiste una versione piu' recente di quella in esecuzione.
 *
 * <p><b>Mai dal thread FX</b>, e in nessun caso questo puo' impedire all'applicazione di partire o
 * di funzionare: qui si leggono barcode da PDF locali, cosa che va benissimo senza
 * Internet, e fermare il programma perche' non e' riuscito a chiedere qualcosa a un sito sarebbe
 * un guasto inventato da noi. Rete assente, sito fermo, JSON malformato: tutti finiscono in
 * {@link Optional#empty()}, cioe' "nessun aggiornamento", che e' l'errore innocuo.
 *
 * <p>Qui non si scarica e non si installa: questa classe produce solo <b>cosa</b> c'e' da prendere
 * (URL, impronta, dimensione). Chi lo prende e lo installa e' {@link Scaricatore} +
 * {@link Installatore}, e la differenza non e' formale — la decisione si prova senza rete e senza
 * toccare la macchina, mentre il resto no.
 */
public final class UpdateCheck {

    private static final System.Logger log = System.getLogger(UpdateCheck.class.getName());

    /** Il sito puo' essere fermo: si aspetta poco e si rinuncia senza dire niente all'utente. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(12);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Un aggiornamento disponibile, gia' pronto da mostrare e da installare.
     *
     * @param file           il nome del pacchetto, che e' anche il nome con cui viene salvato: due
     *                       versioni scaricate non si sovrascrivono a vicenda, e dal nome del file
     *                       in cache si capisce sempre cosa c'e' dentro
     * @param sha256         impronta pubblicata dal sito, {@code null} se non c'e'. Si verifica
     *                       <b>prima</b> di passare il file a msiexec, vedi {@link Scaricatore}
     * @param urlAlternativo pacchetto per utente (installabile senza privilegi), {@code null} se il
     *                       prodotto non ne pubblica uno
     */
    public record Update(String versione,
                         String url,
                         String file,
                         String dimensione,
                         String data,
                         String sha256,
                         String urlAlternativo) {}

    /** {@code https://rinominapdf.teo.place/scarica/versione.json}, derivato e non riscritto a mano. */
    public static String urlManifest() {
        return base() + "versione.json";
    }

    private static String base() {
        String home = AppInfo.HOME_URL;
        if (!home.endsWith("/")) home = home + "/";
        return home + "scarica/";
    }

    /**
     * La versione pubblicata, se e' piu' recente di quella in esecuzione.
     *
     * <p>Bloccante: va chiamato da un thread di background.
     */
    public Optional<Update> cerca() {
        return decidi(leggiManifest(), AppInfo.VERSION);
    }

    /**
     * La decisione, separata dalla rete perche' e' l'unica parte che puo' sbagliare in modo
     * credibile — e cosi' si prova senza un server.
     */
    static Optional<Update> decidi(Manifest m, String versioneInEsecuzione) {
        if (!m.scaricabile()) return Optional.empty();
        if (!Version.isNewer(m.versione(), versioneInEsecuzione)) return Optional.empty();

        String alternativo = null;
        // L'alternativo si offre solo se e' completo: un secondo pulsante che punta a un URL
        // finito con "null" e' peggio di un pulsante che non c'e'.
        if (m.alternativo() != null && m.alternativo().file() != null) {
            alternativo = base() + m.alternativo().file();
        }

        return Optional.of(new Update(
                m.versione(),
                base() + m.file(),
                m.file(),
                m.dimensione(),
                m.data(),
                m.sha256(),
                alternativo));
    }

    Manifest leggiManifest() {
        HttpRequest richiesta = HttpRequest.newBuilder(URI.create(urlManifest()))
                .timeout(READ_TIMEOUT)
                .header("User-Agent", AppInfo.NAME + "/" + AppInfo.VERSION)
                .GET()
                .build();
        try {
            HttpResponse<String> r = http.send(richiesta, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() / 100 != 2) {
                log.log(System.Logger.Level.INFO, "Controllo aggiornamenti: manifest non disponibile (HTTP " + r.statusCode() + ")");
                return Manifest.assente();
            }
            return Manifest.parse(r.body());
        } catch (InterruptedException e) {
            // Il flag si ripristina SOLO qui. Alzarlo per una rete assente — che e' il caso
            // normale, non un'interruzione — lo lascerebbe alzato su un thread condiviso, e la
            // prima attivita' successiva morirebbe con un'eccezione che non la riguarda.
            Thread.currentThread().interrupt();
            return Manifest.assente();
        } catch (Exception e) {
            log.log(System.Logger.Level.INFO, "Controllo aggiornamenti: sito irraggiungibile (" + e.getMessage() + ")");
            return Manifest.assente();
        }
    }
}
