package app.core.update;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Installa l'aggiornamento e riapre il programma, senza far scegliere niente all'utente.
 *
 * <p><b>Perche' serve uno script esterno.</b> Un MSI non puo' sostituire i file di un programma
 * aperto: finche' l'eseguibile e' in memoria, msiexec o fallisce o si prenota per il prossimo
 * riavvio del PC. E il programma non puo' aspettare la fine della propria sostituzione — deve
 * essere gia' chiuso. L'unico modo perche' l'aggiornamento sembri "una cosa sola" e' quindi
 * lasciare in piedi un processo che <b>sopravvive</b> all'applicazione: aspetta che si chiuda,
 * installa, e la riapre. Qui quel processo e' un PowerShell nascosto, e la sequenza e' questa:
 *
 * <ol>
 *   <li>aspetta che il PID dell'applicazione sparisca;</li>
 *   <li>{@code msiexec /i ... /qn /norestart}, elevato <b>una volta sola</b>;</li>
 *   <li>scrive l'esito in un file che l'applicazione legge al prossimo avvio;</li>
 *   <li>riapre l'applicazione — <b>non</b> elevata (vedi sotto);</li>
 *   <li>cancella il pacchetto, che sono novanta megabyte che nessuno riguardera'.</li>
 * </ol>
 *
 * <p><b>Il consenso di amministratore resta, ed e' uno solo.</b> Il pacchetto di rinominaPDF e'
 * per macchina: installa sotto {@code Program Files}, e nessun programma non elevato puo' scriverci
 * — ne' questo, ne' il wizard, ne' Windows Update per un pacchetto non firmato. "Silenzioso" qui
 * vuol dire che dopo quel clic non c'e' piu' niente da leggere, da scegliere o da chiudere: nessuna
 * finestra di installazione, nessun browser, nessun file da cercare in Download.
 *
 * <p><b>Il riavvio avviene nel processo NON elevato</b> (il PowerShell padre), mai dentro quello
 * elevato. Un programma riaperto come amministratore leggerebbe e scriverebbe
 * {@code %LOCALAPPDATA%} dell'<i>amministratore</i>: e li' non c'e' il rapporto
 * dell'aggiornamento, che resterebbe da leggere al primo avvio normale come se fosse nuovo — e quella
 * sessione girerebbe come amministratore senza che nessuno l'abbia deciso.
 *
 * <p><b>Lo script non e' un file su disco.</b> Viaggia come {@code -EncodedCommand}: uno script in
 * {@code %LOCALAPPDATA%} sarebbe scrivibile da qualunque processo non elevato dell'utente, e farlo
 * poi eseguire con l'elevazione regalerebbe un'elevazione a chi non ce l'ha. Su disco va solo il
 * <b>rapporto</b>, che sono dati.
 */
public final class Installatore {

    private static final System.Logger log = System.getLogger(Installatore.class.getName());

    private Installatore() {}

    /** Nome del file di rapporto, dentro {@link #cartella(Path)}. */
    static final String NOME_ESITO = "esito.txt";

    /**
     * Quanti secondi si aspetta che l'applicazione si chiuda prima di rinunciare.
     *
     * <p>Rinunciare e' la risposta giusta: installare sopra un programma ancora aperto non
     * fallisce in modo pulito, si prenota per il riavvio del PC e lascia l'utente con un
     * aggiornamento "riuscito" che non e' successo.
     */
    static final int ATTESA_CHIUSURA_S = 120;

    /** Codice nostro: l'applicazione non si e' chiusa, quindi non e' stato installato niente. */
    public static final int NON_CHIUSA = -2;

    /** Dove finiscono il pacchetto scaricato e il rapporto. */
    public static Path cartella(Path controlDir) {
        return controlDir.resolve("aggiornamento");
    }

    /**
     * L'eseguibile installato di questo programma, se stiamo girando da un'installazione vera.
     *
     * <p>{@code jpackage.app-path} lo dichiara il lanciatore generato da jpackage e vale il
     * percorso completo di {@code rinominaPDF.exe}. In sviluppo ({@code mvn javafx:run}) non
     * esiste, ed e' esattamente la distinzione che serve: <b>senza un'installazione non c'e' niente
     * da aggiornare</b>, e provarci sostituirebbe i file di un pacchetto che questa copia non ha
     * mai installato.
     */
    public static Optional<Path> eseguibileInstallato() {
        String dichiarato = System.getProperty("jpackage.app-path");
        if (dichiarato == null || dichiarato.isBlank()) return Optional.empty();
        try {
            Path p = Path.of(dichiarato);
            return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** {@code true} se l'aggiornamento puo' installarsi da solo; altrimenti resta il browser. */
    public static boolean possibile() {
        return eseguibileInstallato().isPresent();
    }

    /**
     * Avvia lo script e torna subito: da questo momento <b>l'applicazione deve chiudersi</b>, o lo
     * script rinuncera' dopo {@link #ATTESA_CHIUSURA_S} secondi.
     *
     * @param msi      il pacchetto gia' scaricato e gia' verificato
     * @param versione la versione che si sta installando, per il messaggio del prossimo avvio
     */
    public static void avvia(Path msi, String versione, Path controlDir) throws IOException {
        Path eseguibile = eseguibileInstallato()
                .orElseThrow(() -> new IllegalStateException("nessuna installazione da aggiornare"));
        Path esito = cartella(controlDir).resolve(NOME_ESITO);
        Files.createDirectories(esito.getParent());
        Files.deleteIfExists(esito);

        String script = script(ProcessHandle.current().pid(), msi, eseguibile, esito, versione);
        String b64 = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));

        // -WindowStyle Hidden: l'applicazione sta per sparire, e una finestra nera che resta
        // aperta al suo posto si legge come un guasto. Il processo sopravvive di proposito alla
        // JVM che lo ha creato: su Windows un figlio non muore con il padre.
        new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-EncodedCommand", b64)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        log.log(System.Logger.Level.INFO, "Aggiornamento alla " + versione + " avviato: si chiude, si installa, si riapre.");
    }

    // ---- lo script -----------------------------------------------------------------------------

    /** Puro: si costruisce e si prova senza installare niente. */
    static String script(long pid, Path msi, Path eseguibile, Path esito, String versione) {
        StringBuilder s = new StringBuilder();
        s.append("$ErrorActionPreference = 'Continue'\n");
        // Senza questo PowerShell serializza record di avanzamento sullo stream di errore; qui non
        // li legge nessuno, ma restano righe scritte da un processo che deve essere invisibile.
        s.append("$ProgressPreference = 'SilentlyContinue'\n");
        s.append("$esito = ").append(letterale(esito.toString())).append("\n");
        s.append("$msi = ").append(letterale(msi.toString())).append("\n");
        s.append("$exe = ").append(letterale(eseguibile.toString())).append("\n");
        // La cartella dell'exe la calcola JAVA (getParent) e arriva come letterale: NON si usa
        // `Split-Path -LiteralPath $exe -Parent`, che in Windows PowerShell 5.1 e' un set di
        // parametri AMBIGUO e lancia un'eccezione. Vedi la riapertura piu' sotto.
        Path cartellaExe = eseguibile.getParent();
        s.append("$dir = ").append(letterale(cartellaExe == null ? "" : cartellaExe.toString())).append("\n");
        s.append("$versione = ").append(letterale(versione)).append("\n");
        s.append("function Riporta($codice) {\n");
        s.append("  Set-Content -LiteralPath $esito -Value (\"ESITO`t$versione`t$codice\") -Encoding UTF8\n");
        s.append("}\n");

        // 1) Nessuna installazione sopra un programma aperto.
        s.append("$chiusa = $true\n");
        s.append("try {\n");
        s.append("  Wait-Process -Id ").append(pid).append(" -Timeout ").append(ATTESA_CHIUSURA_S)
                .append(" -ErrorAction Stop\n");
        // Wait-Process solleva un'eccezione in DUE casi opposti: il processo non esiste piu' (che e'
        // quello che stiamo aspettando) e il tempo e' scaduto (che e' il fallimento). Distinguerli
        // guardando il messaggio sarebbe fragile e dipenderebbe dalla lingua di Windows: si
        // ricontrolla il PID, che risponde sempre la stessa cosa.
        s.append("} catch {\n");
        s.append("  $chiusa = -not (Get-Process -Id ").append(pid).append(" -ErrorAction SilentlyContinue)\n");
        s.append("}\n");
        s.append("if (-not $chiusa) {\n");
        s.append("  Riporta ").append(NON_CHIUSA).append("\n");
        s.append("  exit\n");
        s.append("}\n");

        // 2) Un solo consenso, e nessuna domanda dopo: /qn.
        //    /norestart: il riavvio del PC, se mai servisse, lo decide l'utente.
        //    Niente REINSTALL=ALL: qui e' sempre un aggiornamento, e quei parametri riparerebbero
        //    la versione vecchia invece di sostituirla.
        s.append("$codice = 1602\n");
        s.append("$p = $null\n");
        s.append("try {\n");
        s.append("  $p = Start-Process -FilePath 'msiexec.exe' -Verb RunAs -PassThru")
                .append(" -ArgumentList @('/i',$msi,'/qn','/norestart')\n");
        s.append("} catch { $p = $null }\n");
        // Il consenso negato e' l'unica cosa che fa fallire Start-Process: 1602 e' lo stesso codice
        // con cui msiexec dice "annullato", cosi' a valle non c'e' un caso in piu' da gestire.
        s.append("if ($null -ne $p) {\n");
        // WaitForExit sull'handle di msiexec, non `-Wait`: e' il processo che fa il lavoro, e la
        // sua uscita e' la fine dell'installazione. (`-Verb RunAs -Wait` su un PowerShell
        // intermedio invece torna prima — verificato in teoSetup il 2026-08-19.)
        s.append("  try { $p.WaitForExit(); $codice = $p.ExitCode } catch { $codice = -1 }\n");
        s.append("}\n");
        s.append("Riporta $codice\n");

        // 3) Riaperto solo quando l'eseguibile c'e' di nuovo: durante un aggiornamento maggiore il
        //    file sparisce e ricompare, e un riavvio dato un istante prima non troverebbe niente.
        s.append("for ($i = 0; $i -lt 120; $i++) {\n");
        s.append("  if (Test-Path -LiteralPath $exe) { break }\n");
        s.append("  Start-Sleep -Milliseconds 500\n");
        s.append("}\n");
        // La riapertura. PRIMA usava `Split-Path -LiteralPath $exe -Parent`, che in Windows
        // PowerShell 5.1 lancia «Impossibile risolvere il set di parametri» (AmbiguousParameterSet):
        // l'eccezione faceva fallire l'INTERO Start-Process, che il catch inghiottiva -> installava
        // e non riapriva. Ora la cartella arriva da Java in $dir, senza Split-Path.
        s.append("try {\n");
        s.append("  if ($dir -and (Test-Path -LiteralPath $dir)) { Start-Process -FilePath $exe -WorkingDirectory $dir }\n");
        s.append("  else { Start-Process -FilePath $exe }\n");
        s.append("} catch { }\n");

        // 4) Il pacchetto ha finito il suo mestiere. Si tiene solo se non e' stato installato: il
        //    giro dopo lo si ritrova in cache e non si riscarica.
        s.append("if ($codice -eq 0 -or $codice -eq 3010) {\n");
        s.append("  Remove-Item -LiteralPath $msi -Force -ErrorAction SilentlyContinue\n");
        s.append("}\n");
        return s.toString();
    }

    /**
     * Una stringa PowerShell fra apici singoli: dentro, l'unico carattere speciale e' l'apice
     * stesso, che si raddoppia. Niente espansione di {@code $}, niente backtick — che e' esattamente
     * quello che serve per un percorso di Windows.
     */
    static String letterale(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    // ---- il rapporto ---------------------------------------------------------------------------

    /** Com'e' andato l'aggiornamento tentato la volta scorsa. */
    public record Esito(String versione, int codice) {
        public boolean ok() { return codice == 0 || codice == 3010; }
        public String spiegazione() { return spiega(codice); }
    }

    /**
     * L'esito dell'ultimo tentativo, se ce n'e' uno da riferire. <b>Lo consuma</b>: un rapporto
     * letto due volte diventerebbe un avviso che ricompare a ogni avvio.
     */
    public static Optional<Esito> ritiraEsito(Path controlDir) {
        Path f = cartella(controlDir).resolve(NOME_ESITO);
        try {
            if (!Files.isRegularFile(f)) return Optional.empty();
            List<String> righe = Files.readAllLines(f, StandardCharsets.UTF_8);
            Files.deleteIfExists(f);
            for (String riga : righe) {
                Optional<Esito> e = interpreta(riga);
                if (e.isPresent()) return e;
            }
        } catch (Exception ignore) {
            /* un rapporto illeggibile non e' un problema dell'utente: non se ne parla */
        }
        return Optional.empty();
    }

    /** Analisi pura della riga {@code ESITO<TAB>versione<TAB>codice}. */
    static Optional<Esito> interpreta(String riga) {
        if (riga == null) return Optional.empty();

        // IL BOM. `Set-Content -Encoding UTF8` di PowerShell 5.1 scrive UTF-8 **con** BOM, e quei
        // tre byte finiscono davanti alla prima riga — che qui e' anche l'unica. Java non li toglie
        // leggendo: la riga arriva come "﻿ESITO\t..." e non comincia piu' per "ESITO". Senza
        // questa riga il rapporto non verrebbe letto mai, cioe' un aggiornamento fallito
        // resterebbe senza spiegazione. E' la stessa trappola gia' pagata in teoSetup.
        String pulita = riga.startsWith("﻿") ? riga.substring(1) : riga;

        if (!pulita.startsWith("ESITO\t")) return Optional.empty();
        String[] c = pulita.split("\t", -1);
        if (c.length < 3) return Optional.empty();
        try {
            return Optional.of(new Esito(c[1].trim(), Integer.parseInt(c[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * I codici che vale la pena distinguere. Il resto e' un numero, e va mostrato come numero:
     * inventare una frase per un codice che non si conosce fa perdere l'unico dato utile.
     *
     * <p>Le frasi sono quelle di {@code teoSetup}: lo stesso msiexec, sullo stesso pacchetto, deve
     * spiegarsi allo stesso modo comunque lo si sia avviato.
     */
    public static String spiega(int codice) {
        return switch (codice) {
            case 0 -> "completato";
            case 3010 -> "completato — serve un riavvio del PC per finire";
            case 1602 -> "annullato";
            case 1603 -> "errore irreversibile durante l'installazione";
            case 1618 -> "c'era gia' un'altra installazione in corso su questo PC";
            case 1619 -> "il pacchetto non si e' aperto: file danneggiato o incompleto";
            case 1638 -> "e' gia' installata una versione che questo pacchetto non aggiorna";
            case 1625 -> "una regola di sistema vieta questa installazione";
            case NON_CHIUSA -> "il programma non si e' chiuso in tempo: non e' stato installato niente";
            case -1 -> "nessuna risposta: controlla la versione in App installate";
            default -> "msiexec ha risposto " + codice;
        };
    }
}
