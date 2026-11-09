# STANDARD.md — standard comune dei progetti teo.place

> **Copia.** La fonte e' `CLAUDE.md` nella cartella `teo.place` che contiene i repo, e chi la
> modifica riallinea tutte le copie. Qui e' duplicata perche' da un clone singolo quella
> cartella non esiste.

Standard comune a **tutti** i progetti di questa cartella, presenti e futuri. Qui sta tutto cio' che
e' condiviso: toolchain, versionamento, nomenclatura degli artefatti, metodo di packaging,
convenzioni di codice. Il `CLAUDE.md` di ogni progetto contiene **solo cio' che e' suo** e da' per
letto questo file.

Ordine di lettura in una sessione nuova: **questo file, poi il `CLAUDE.md` del progetto** su cui si
lavora.

## I progetti

| progetto | cos'e' | stato |
|---|---|---|
| **TraceFox** | proxy di debug HTTP/HTTPS (stile Charles): registra, ispeziona e manipola il traffico | attivo — e' il riferimento del metodo di packaging |
| **listBigStorage** | inventario di filesystem remoti via SSH e del PC locale, in SQLite | attivo — riferimento di UI/stack JavaFX |
| **watchCLI** | monitor di comandi CLI remoti via SSH, tab staccabili e timer | attivo |
| **queryF5** | client multi-query MySQL | attivo |
| **SorterNext** | lavorazione dei file del sorter (versione nativa di sorter-fx 2.0.4): PDF, elenchi, errori guidati; **.NET/WPF, non Java** | attivo — repo `sorter-next`, pacchetto da `sorter-installer` |
| **SorterBarcode** | lettore di barcode nativo (riga di comando e DLL usate da SorterNext) con una finestra WPF a cinque zone e storico delle letture | attivo — repo `sorter-barcode`, pacchetto da `sorter-installer` |
| **PicPurg** | trova le foto duplicate e quasi identiche in una cartella; si sceglie quali tenere, le altre vanno in una cartella, nel Cestino o via | attivo — nato e pubblicato il 2026-08-21 |
| **watchHost** | monitor stile Zabbix di RAM/CPU/dischi di server Linux via SSH, dashboard **web** e alert email/WhatsApp; **senza JavaFX** | attivo — nato e pubblicato il 2026-10-30 |
| **rinominaPDF** | rinomina i PDF di una cartella leggendone il barcode (esito, istat, anno, pacco scelti dall'utente) | attivo — rientrato nel wizard il 2026-10-30, **gratuito come SorterNext** |
| **teoSetup** | il wizard di installazione di tutta la linea: un solo file per l'utente finale | attivo — vedi "Il wizard di installazione" |
| **worktime** | **web app**, non desktop: ore di lavoro per progetto/cliente e fatture su un periodo | attivo — vive su KVM2, vedi "worktime" qui sotto |

### worktime, e perche' non si misura con questo file

**worktime e' una web app** (React + Node + MySQL) e sta **solo su KVM2**, in
`/var/www/html/worktime`. Non ha un repo in questa cartella, non ha un MSI, non ha un
UpgradeCode, non e' nel wizard e non e' nella tabella "Identita' dei prodotti". Tutto cio' e'
**corretto**, non una dimenticanza: non c'e' niente da installare su un PC.

E' pero' **venduta nello stesso negozio Lemon Squeezy** dei desktop (prodotto `1294443`), e' gia'
agganciata nel database `authenticator` (`prodotti.codice = 'worktime'`, e i tre `piani` hanno il
loro `ls_variant_id`), e le sue regole stanno in `INTEGRAZIONE-WEB.md`, non in
`INTEGRAZIONE-DESKTOP.md`. Chi guarda il negozio e conosce solo questo file la scambia per
un'anomalia — **e' successo il 2026-08-28** — quindi le due differenze che sembrano difetti e non
lo sono:

- **Il free trial nativo di Lemon Squeezy e' SPENTO sulle sue varianti** (dal 2026-08-17), mentre
  sui quattro desktop e' acceso. La prova di worktime la da' `POST /license/web/prova`
  (`avviaProva`), 15 giorni, senza carta. Accendere anche quello di LS **somma le due prove**: un
  mese gratis a chi si abbona l'ultimo giorno, e la seconda meta' non l'ha decisa nessuno.
- **Le chiavi di licenza sono ACCESE anche se nessuno le incolla da nessuna parte.** La riga in
  `licenze` nasce solo dal webhook `license_key_created`: spegnendole non nascerebbe nessuna
  licenza e `/license/web/stato` risponderebbe `autorizzato: false` per sempre. La chiave e' un
  aggancio interno, non una cosa che l'utente vede.

**sorter-fx** (predecessore di rinominaPDF e di SorterNext, cartelle versionate a mano `1.0.0` …
`2.0.4`) e' in questa cartella ma **non e' un prodotto della linea**: e' un archivio in **SOLA
LETTURA**, si consulta e non si modifica.

### Gli strumenti di lavoro gratuiti: SorterNext, SorterBarcode, rinominaPDF

Tre prodotti sono nel wizard, hanno sito e scheda sulla home, ma **non si vendono**: niente
licenza nell'app, niente riga in `prodotti` su `authenticator`, niente Lemon Squeezy. Sono
programmi di lavoro del sorter, usati da chi lavora i lotti, e una licenza li renderebbe solo piu'
difficili da aprire per le persone per cui esistono. Per loro la sezione "Il licensing nelle app
desktop" e le voci "Licenza" della lista di controllo **non valgono**; tutto il resto si'.

> **rinominaPDF era stato tolto dal wizard il 2026-08-28**: teoSetup lo elencava e provava a
> installarlo, ma non c'era ne' il repo ne' un manifest pubblicato, e chi lo sceglieva non otteneva
> niente. E' **rientrato il 2026-10-30** come strumento gratuito, quando aveva tutte le cose che
> allora mancavano: repo, manifest e MSI su `rinominapdf.teo.place/scarica/`, controllo
> aggiornamenti dentro l'app. Il suo UpgradeCode e' lo stesso di sempre — quello degli MSI gia'
> distribuiti — ed e' tornato nella tabella "Identita' dei prodotti".

## Regole di lavoro

1. **Niente commit e niente push da parte di Claude: li fa l'utente.** Vale per ogni progetto. A
   fine lavoro si dice cosa e' cambiato e in quale repo, cosi' che non resti solo su disco senza
   che nessuno lo sappia: un lavoro finito e non pubblicato non e' distinguibile da uno non fatto.

   Tre cose da sapere per chi pubblica, perche' il comando "ovvio" qui e' sbagliato:

   - **`git push` e basta.** Il remote **non si chiama `origin` dappertutto**: e' `main` in queryF5
     e in TraceFox, `origin` in listBigStorage e watchCLI. E il branch non e' sempre `main`:
     queryF5 sta su **`1.1`**.

     > **queryF5: corretto il 2026-11-09.** Il remote di queryF5 si chiama `origin`
     > (`TeoPlace/queryF5`), non `main`: `main` e' un *branch* fermo a "fixed jpackage". E un
     > clone che non fa `git fetch` da settembre crede che `1.1` sia la 1.2.0 con AES ed
     > Java 21, mentre su GitHub `origin/1.1` e' la 1.2.12 allineata: **prima di lavorare si
     > guarda `git ls-remote`**, non il riferimento remoto locale. `git push origin main` fallisce in tre casi su quattro, mentre
     `git push` segue l'upstream gia' configurato, che e' quello giusto in tutti e quattro.
   - **Si pubblica solo cio' che compila e passa i test.** `mvn package` fa girare anche
     `VersionTest`, quindi una versione disallineata ferma il build prima: se il build non e'
     passato, non c'e' niente da spingere.
   - **`teo.place\` non e' un repository**, quindi *questo* file non si pubblica. Una modifica qui
     arriva su GitHub **solo** attraverso le copie `STANDARD.md`: riallinearle non e' un seguito
     facoltativo, e' la parte della modifica che la rende visibile.
2. **Un progetto per volta.** Quando un progetto ne cita un altro come *riferimento*, quell'altro e'
   **in sola lettura**: si legge per copiarne il metodo, non lo si modifica per far funzionare il
   primo. Le catene fra i repo presenti: watchCLI e rinominaPDF guardano queryF5; TraceFox e
   watchCLI guardano listBigStorage per la UI; watchHost guarda listBigStorage per il controllo
   aggiornamenti e i segreti DPAPI; rinominaPDF guarda sorter-fx.
3. **Le cartelle di test dell'utente sono in sola lettura** (i `case-input` / `case-output` citati
   nei singoli `CLAUDE.md`): sono il metro di paragone, se le si modifica non misurano piu' niente.
4. **Si pubblica sul server solo con un "pubblica" dell'utente.** Preparare tutto in locale
   (build, sito, manifest, script) si fa da soli; caricare su KVM2 e' un gesto verso l'esterno e
   aspetta la parola. Vedi "Pubblicare un prodotto".

## Toolchain

- **JDK 22** (`maven.compiler.source`/`target` = 22), Maven 3.8+, entrambi nel PATH.
- **WiX Toolset 3.x** installato: serve a `jpackage --type msi`.
- **JavaFX 22.0.2** + **AtlantaFX 2.0.1** per i temi (dove c'e' una UI).
- **JUnit 5.10.2** + surefire per i test.

Nessuna eccezione: queryF5 compilava a 21 per ragioni storiche ed e' stato allineato il
2026-08-19.

## Identita' dei prodotti

Il **nome** e' l'identita' del prodotto per Windows, maiuscole comprese, e non si tocca: cambiarlo
significa pubblicare un prodotto diverso. L'`artifactId` Maven e' il nome tutto minuscolo.

| prodotto | artifactId | main class (Launcher) | UpgradeCode (per macchina) |
|---|---|---|---|
| TraceFox | `tracefox` | `it.integraa.tracefox.Launcher` | `8F94291A-270E-3C72-AB8D-28FB57793711` |
| TraceFox-peruser | — | idem | `C079027E-DE15-3237-B36C-5508CAD4A08A` |
| listBigStorage | `listbigstorage` | `it.integraa.lbs.Launcher` | `8E0CFEE1-71D3-38E8-B6BC-3DC476CBABEC` |
| watchCLI | `watchcli` | `com.integraa.watchcli.WatchCliLauncher` | `E4A0B572-96A6-3EF0-BBCB-3C184C7E8B94` |
| queryF5 | `queryf5` | `com.teoplace.queryf5.QueryF5Launcher` | `52F97CB4-0EE2-3968-BD2F-36C835D459ED` |
| SorterNext | `sorternext` | `SorterNext.exe` (.NET 8, WPF, self-contained) | `B0E0574E-20F6-3205-B7D1-E5935C39557A` |
| SorterBarcode | `sorterbarcode` | `sorter-barcode.exe` (C, riga di comando) | `DBA640D5-9F3C-3248-A369-90FAE7DEA094` |
| PicPurg | `picpurg` | `com.teoplace.picpurg.PicPurgLauncher` | `2CBA33AD-6DFB-33A0-B2E7-B86FAD3D378D` |
| watchHost | `watchhost` | `place.teo.watchhost.Launcher` | `B08054CE-00BE-3A92-9F0B-AF5B0943C674` |
| rinominaPDF | `rinominapdf` | `app.Launcher` | `6E50594E-22A6-3AC3-A7FB-48CFCA119568` |
| teoSetup | `teosetup` | `com.teoplace.setup.TeoSetupLauncher` | `FE9E46CE-2B4C-3406-953D-02F9C71D5D7B` |

### Codici ritirati — congelati, mai riusati

Prodotti che **non** sono nel wizard e non si vendono, ma il cui UpgradeCode resta occupato: e'
stato distribuito, quindi su qualche PC c'e' un'installazione che lo porta. Riusarlo per un
prodotto nuovo farebbe credere a Windows che i due siano lo stesso programma, e il primo
aggiornamento ne disinstallerebbe un altro.

| prodotto | artifactId | UpgradeCode |
|---|---|---|
| sorter-fx | *(mai impacchettato)* | — |

> **Questa tabella ha tre colonne, e non e' un caso.** `CatalogoTest` riconosce le righe della
> tabella qui sopra dal **GUID nella quarta colonna**: con tre colonne queste righe non le vede,
> e quindi non pretende che i codici ritirati abbiano una riga in `Catalogo.PRODOTTI`. E' il modo
> di conservare il codice senza rimettere il prodotto nel wizard. rinominaPDF e' stato qui dal
> 2026-08-28 al 2026-10-30: rientrando nel wizard e' tornato nella tabella a quattro colonne,
> **con lo stesso codice**, ed e' cosi' che si fa — un codice ritirato non se ne genera uno nuovo.

**Vendor: `teo.place` per tutti.** Prima era `teo.place` / `TeoPlace` / `Integraa` a seconda del
progetto; e' stato unificato perche' e' la riga "Editore" in *App installate* e tre editori diversi
per lo stesso autore si leggono come tre fornitori diversi.

> L'unificazione vale **da qui in avanti**: gli MSI gia' distribuiti portano dentro il vendor
> vecchio, e il `Manufacturer` e' scritto nel pacchetto, non deciso al momento dell'installazione.
> Verificato il 2026-08-16: `queryF5-1.1.0.msi` dice ancora `TeoPlace` e `watchCLI-1.0.0.msi` dice
> ancora `Integraa` (TraceFox e listBigStorage erano gia' `teo.place`). Su un PC che li ha
> installati la riga "Editore" cambia **solo dopo il prossimo aggiornamento**, quindi non e' un
> difetto da inseguire: e' il pacchetto vecchio che parla.

### L'UpgradeCode, e perche' e' scritto a mano

E' il valore con cui Windows riconosce «questo e' lo stesso programma»: un MSI aggiorna il
precedente **solo se** l'UpgradeCode coincide **e** i primi tre campi della versione sono maggiori.
Cambiarlo trasforma ogni aggiornamento futuro in una **seconda installazione affiancata** — due voci
in *App installate*, due cartelle, due collegamenti — e non lo si scopre al primo aggiornamento ma
al secondo, quando il danno e' gia' dai clienti.

Senza `--win-upgrade-uuid`, **jpackage se lo calcola da solo**, e la formula (verificata sui sei MSI
gia' distribuiti) e':

```
UUID v3  di  "UpgradeCode/<vendor>/<nome>"
```

Cioe' **dipende dal vendor**. Unificare il vendor su `teo.place` avrebbe quindi cambiato in silenzio
l'UpgradeCode di queryF5, rinominaPDF e watchCLI, e il pacchetto successivo si sarebbe affiancato a
quello installato invece di aggiornarlo. Non e' successo solo perche' **ogni script passa
`--win-upgrade-uuid` esplicito**, con il valore riletto dalla tabella `Property` dell'MSI gia'
distribuito. Da qui in avanti quei valori sono **congelati**: non vanno cambiati e non vanno riusati
fra prodotti diversi.

Per rileggere l'UpgradeCode da un MSI esistente (utile per un prodotto ereditato):

```powershell
$i = New-Object -ComObject WindowsInstaller.Installer
$db = $i.GetType().InvokeMember("OpenDatabase","InvokeMethod",$null,$i,@("percorso.msi",0))
$v  = $db.GetType().InvokeMember("OpenView","InvokeMethod",$null,$db,
        @("SELECT Value FROM Property WHERE Property='UpgradeCode'"))
$v.GetType().InvokeMember("Execute","InvokeMethod",$null,$v,$null)
$r = $v.GetType().InvokeMember("Fetch","InvokeMethod",$null,$v,$null)
$r.GetType().InvokeMember("StringData","GetProperty",$null,$r,@(1))
```

**Per-macchina e per-utente hanno UpgradeCode DIVERSI di proposito**: si installano in posti diversi
e con privilegi diversi, e Windows non sa passare dall'uno all'altro con un aggiornamento. Con lo
stesso codice l'installazione per-utente proverebbe ad "aggiornare" quella per-macchina e fallirebbe
a meta'.

## Versione

**Tre campi crescenti** (`1.0.0`, poi `1.0.1`…), mai `-SNAPSHOT`, mai due campi. La forma a tre campi
non e' estetica: Windows confronta i **primi tre** campi, quindi con un numero fermo — o con un `1.0`
che vale esattamente quanto `1.0.0` — ogni pacchetto nuovo si affianca al vecchio invece di
aggiornarlo, e l'installer si ferma dicendo che il programma c'e' gia'.

> **Dare la forma giusta a un numero non lo fa crescere.** Passare da `1.0` a `1.0.0` rispetta la
> convenzione ma per Windows i due valori sono **identici**, quindi il pacchetto "nuovo" non
> aggiorna niente: l'installer risponde che il programma c'e' gia'. Il 2026-08-16 la trappola era
> aperta su tre prodotti su quattro — verificato leggendo il `ProductVersion` dalla tabella
> `Property` degli MSI **davvero distribuiti**, che e' l'unica fonte che conta:
>
> | prodotto | `ProductVersion` distribuito | si sarebbe pubblicato | esito |
> |---|---|---|---|
> | TraceFox / TraceFox-peruser | `1.0` | `1.0.0` | ❌ stesso numero |
> | listBigStorage | `1.0` | `1.0.0` | ❌ stesso numero |
> | watchCLI | `1.0.0` | `1.0.0` | ❌ numero identico |
> | queryF5 | `1.1.0` | `1.2.0` | ✅ cresce |
>
> Tutti e tre sono stati portati a **`1.0.1`**, saltando la `1.0.0`. Regola generale: **il numero da
> confrontare non e' quello dell'ultimo build, e' quello scritto nell'MSI gia' in mano ai clienti**
> — si rilegge con lo stesso metodo dell'UpgradeCode, cambiando `Property='ProductVersion'`.

La versione vive in **tre posti che non si vedono fra loro**:

| dove | cosa ne dipende |
|---|---|
| `pom.xml` → `<version>` | il nome del fat-jar |
| `AppInfo.VERSION` | quello che l'utente legge nella UI |
| `build-msi.ps1` → `$version` | il jar da impacchettare e l'`app-version` dell'MSI |

Che divergano **non da' errore al momento giusto**: il build fallisce piu' tardi con "fat-jar non
trovato", oppure — molto peggio — riesce e produce un MSI che si dichiara di una versione mentre il
programma dentro ne mostra un'altra, cosi' chi segnala un difetto cita il numero sbagliato. Per
questo ogni progetto ha un **`VersionTest`** che verifica l'allineamento dei tre posti, la forma a
tre campi e che gli UpgradeCode siano ancora quelli. Il test gira dentro `mvn package`, quindi un
disallineamento ferma il build **prima** di produrre l'installer.

Corollario: **nessun'altra stringa di versione nel codice.** La UI legge `AppInfo.VERSION`, non un
letterale. Era gia' andata storta: queryF5 mostrava `1.2` con il pom a `1.2.0`, watchCLI mostrava
`v1.0` con il pom a `1.0.0`.

## Nomenclatura degli artefatti

`<Nome>` e' il nome del prodotto con le maiuscole originali, `<versione>` i tre campi.

| artefatto | percorso | note |
|---|---|---|
| jar sottile | `target/<artifactId>-<versione>.jar` | lo produce Maven, **non si distribuisce** |
| **fat-jar** | `target/<Nome>-<versione>-all.jar` | tutto dentro, e' l'unico input di jpackage |
| input di jpackage | `target/jpackage-input/` | ricreata a ogni build, contiene **solo** il fat-jar |
| **MSI per macchina** | `dist/<Nome>-<versione>.msi` | l'installer normale |
| **EXE per macchina** | `dist/<Nome>-<versione>.exe` | l'MSI dentro un bootstrapper, stesso UpgradeCode |
| **MSI per utente** | `dist/<Nome>-peruser-<versione>.msi` | solo dove serve (oggi: TraceFox) |
| EXE portabile | `dist/<Nome>-<versione>-portable.exe` | opzionale, Launch4j, **richiede Java installato** |

Il fat-jar si chiama `-all` e non `-shaded` o `-jar-with-dependencies`: e' il nome che gli script
cercano, cambiarlo rompe il packaging senza dirlo.

`dist/` e' l'**unica** destinazione degli installer: prima nascevano accanto al `pom.xml` e la radice
si riempiva di msi/exe di versioni diverse, dove non si capiva piu' quale fosse l'ultimo. La cartella
sta nel `.gitignore` insieme a `target/` e `dependency-reduced-pom.xml`.

**La cartella di input di jpackage contiene solo il fat-jar.** Puntare jpackage direttamente su
`target/` gli farebbe imballare anche il jar sottile, le classi e i file temporanei.

## `build-msi.ps1` — lo script di packaging

**Un solo file, con un solo nome, in radice di ogni progetto: `build-msi.ps1`.** Prima erano
`build-msi.ps1`, `jpackage.ps1` e `jpackage_powershell` (quest'ultimo senza nemmeno l'estensione,
quindi non eseguibile con un doppio clic ne' trovabile cercando `*.ps1`).

Uso: `powershell -ExecutionPolicy Bypass -File .\build-msi.ps1`

Struttura fissa, nell'ordine:

1. **Intestazione** con il metodo e i requisiti di chi compila.
2. `$ErrorActionPreference = "Stop"` e `Set-Location $PSScriptRoot`, cosi' lo script funziona da
   qualunque cartella corrente.
3. **Blocco identita'**: `$version`, `$name`, `$vendor`, `$mainClass`, `$description`, `$aboutUrl`,
   `$upgradeCode`. Tutto il resto e' derivato.
4. **`Invoke-Native`** (vedi sotto).
5. `mvn clean package` → verifica che il fat-jar esista.
6. Ricreazione di `target/jpackage-input` con il solo fat-jar.
7. Preparazione di `dist/` e **rimozione dei file della stessa versione** gia' presenti.
8. Array `$common` con le opzioni condivise + una `Invoke-Native jpackage` per ogni tipo.
9. Riepilogo di cosa e' stato creato + **checklist di verifica**.

### `Invoke-Native`, e i due modi di fallire in silenzio

```powershell
function Invoke-Native {
  param([string]$Exe, [string[]]$Arguments, [string]$What)
  $ErrorActionPreference = "Continue"
  & $Exe @Arguments
  if ($LASTEXITCODE -ne 0) { throw "$What fallito (exit $LASTEXITCODE)" }
}
```

Due cose, e servono entrambe:

- **I comandi nativi non alzano eccezioni.** `mvn` e `jpackage` riportano l'esito solo nell'exit
  code: senza controllarlo, un `mvn package` fallito lascia in `target/` il fat-jar del build
  *precedente* e jpackage lo impacchetta senza un lamento. Si scopre a installazione fatta.
- **`$ErrorActionPreference = "Stop"` e' troppo zelante con gli eseguibili nativi.** In Windows
  PowerShell rende terminante qualunque riga che un exe manda su **stderr**, anche un semplice
  avviso: bastava un messaggio di JavaFX durante i test per far fallire il confezionamento di un
  build perfettamente riuscito. Dentro la funzione la preferenza torna a `Continue` (l'assegnazione
  e' locale e si ripristina all'uscita); il `throw` resta terminante comunque.

### Le opzioni di jpackage

`--input target\jpackage-input`, `--main-jar`, `--main-class`, `--name`, `--app-version`,
`--vendor`, `--description`, `--icon icon.ico`, `--dest dist`, `--win-upgrade-uuid`,
`--win-dir-chooser`, `--win-shortcut`, `--win-menu`. In piu', dove serve, `--about-url` e
`--java-options` (il tetto di heap e' una scelta di prodotto: TraceFox `-Xmx2g`, listBigStorage
`-Xmx4g`).

- **`icon.ico` in radice e' obbligatoria** e lo script si ferma se manca: un pacchetto senza icona si
  riconosce a colpo d'occhio come qualcosa arrivato per sbaglio.
- **`--about-url` non fa quello che sembra.** Lo si passa dove la pagina esiste (convenzione:
  `https://<nome-minuscolo>.teo.place/`), ma **con il template WiX del JDK 22 non finisce
  nell'MSI**: verificato aprendo la tabella `Property` di **tutti e sei** i pacchetti distribuiti
  (2026-08-16) — `ARPURLINFOABOUT` e' assente in ognuno, compresi quelli che l'opzione la passavano
  da sempre. Non e'
  dannoso, ma **non e'** cio' che mette il collegamento in *App installate*, e non va usato come
  se lo fosse. La riga "Editore" li' dentro viene da **`--vendor`**, e quella arriva davvero
  (`Manufacturer` nell'MSI).
- **jpackage rifiuta di sovrascrivere** un file gia' presente in `--dest`: ricostruire la stessa
  versione fallirebbe. Per questo lo script cancella prima i propri output di quella versione — e
  cancella **solo quelli**, per nome esatto, mai `dist\*`.

## `pom.xml` — la parte condivisa

Stessi plugin, stesse versioni, ovunque:

| plugin | versione | a cosa serve |
|---|---|---|
| `maven-compiler-plugin` | 3.13.0 | — |
| `maven-surefire-plugin` | 3.2.5 | fa girare i test (e quindi `VersionTest`) dentro `package` |
| `javafx-maven-plugin` | 0.0.8 | `mvn javafx:run` in sviluppo |
| `maven-shade-plugin` | 3.5.1 | il fat-jar |

Convenzioni:

- La main class sta nella property **`app.main.class`** e viene riferita da li' sia dal plugin JavaFX
  sia dal manifest dello shade: scritta due volte a mano, prima o poi diverge.
- Lo shade produce `outputFile` = `${project.build.directory}/<Nome>-${project.version}-all.jar`.
- **`ManifestResourceTransformer`** con la main class + **`ServicesResourceTransformer`**: il secondo
  fonde i `META-INF/services` di tutte le dipendenze. Senza, in un fat-jar sopravvive solo l'ultimo
  file copiato e i driver JDBC, i provider SLF4J/BouncyCastle e i backend SSHJ **spariscono senza
  errore di compilazione** — si scopre a runtime, con un "no suitable driver".
- Filtri `META-INF/*.SF`, `*.DSA`, `*.RSA`: le firme dei jar (BouncyCastle su tutti) non sopravvivono
  allo shade e la JVM rifiuta il jar con "Invalid signature file digest".
- Filtri `module-info.class` e `META-INF/versions/*/module-info.class`: **un fat-jar non e' un
  modulo**, e un `module-info` di una dipendenza finito nella radice del jar produce un descrittore
  fasullo.
- Lo shade genera `dependency-reduced-pom.xml` in radice: e' un file di scarto, sta nel `.gitignore`.

## Convenzioni di codice condivise

### Il `Launcher` non e' decorativo

Ogni app JavaFX ha una classe `Launcher` (o `<Nome>Launcher`) con un `main()` **che non estende
`Application`**, ed e' quella la main class del manifest e di jpackage. Con JavaFX sul **classpath**
— che e' come lo mette un fat-jar, ed e' come lo lancia jpackage — avviare direttamente una
sottoclasse di `Application` fallisce con *"JavaFX runtime components are missing"*. Il `Launcher` e'
anche il posto giusto per le system property che devono esistere **prima** che nasca il primo oggetto
che le legge (es. la cartella di log per logback: farlo in `App.init()` e' gia' tardi).

### Stato su disco

- La cartella di controllo e' **fissa**: `%LOCALAPPDATA%\<Nome>`. Non `%APPDATA%`/Roaming, che
  OneDrive sincronizza — con SQLite/WAL dentro produce lock, applicazioni lente e log da 0 byte.
- **Quello che c'e' dentro sopravvive alla disinstallazione.** Vale per settings, certificati,
  licenze. Conseguenza pratica: un flag "gia' fatto" scritto durante le prove in sviluppo si ritrova
  intatto nella prima installazione vera, e la cosa che si voleva mostrare al nuovo utente non parte.
  Se una preferenza decide se qualcosa **compare**, il default dev'essere "compare", e a spegnerla
  dev'essere solo un gesto esplicito dell'utente.
- **I segreti non vanno mai in chiaro su disco**: password e passphrase passano dal
  `CredentialStore` (DPAPI di Windows, solo il blob cifrato tocca il disco, passato via stdin), e
  ogni `char[]` di segreto va azzerato in un `finally`. L'implementazione di riferimento e' quella
  di listBigStorage.

  **Dal 2026-08-19 non ci sono piu' eccezioni**: queryF5 era l'ultima, cifrava le password dei
  profili DB con AES-128/ECB e una passphrase scritta nel sorgente, con fallback in chiaro. Ora
  passa dal `CredentialStore` come gli altri, e le password gia' salvate vengono migrate al primo
  avvio — la classe che legge il formato vecchio **non ha un `encrypt`**, cosi' nessuna modifica
  futura puo' riscriverne una nel file di configurazione.

### L'impianto della finestra — le cinque zone

**Tutte le app desktop della linea hanno la stessa finestra**, e non e' un vezzo: chi ne usa due
deve ritrovare le stesse cose nello stesso posto. Il riferimento e' `listBigStorage`; TraceFox ha
lo stesso schema.

```
BorderPane
  top     barra del titolo NOSTRA        (vedi il punto qui sotto)
  left    barra delle attivita' fatte    collassabile, icone sole quando e' chiusa
  center  quello su cui si sta lavorando
  right   il registro                    numeri in cima, righe sotto, collassabile
  bottom  il piede                       riepilogo a sinistra, nome e versione a destra
```

Le tre che si dimenticano, perche' non sono "la schermata principale":

1. **La barra di sinistra e' l'elenco di cio' che l'utente ha fatto** — i run in listBigStorage, le
   scansioni in PicPurg, le connessioni in watchCLI. In cima un pulsante hamburger che apre e
   chiude, poi "nuovo"; in fondo, dopo un separatore, "Impostazioni". Chiusa mostra **solo le
   icone**, e il testo diventa un tooltip: un'etichetta tagliata a meta' si legge come un difetto,
   non come una scelta. Ogni voce ha un **pallino colorato** che dice com'e' andata senza doverla
   riaprire.

   Una app senza storico non e' esentata: quella barra e' anche il posto di "nuovo" e
   "Impostazioni", che altrimenti finiscono sparsi in giro.

2. **Il registro a destra non e' un lusso.** Il risultato di queste applicazioni e' quasi sempre un
   elenco, e **un elenco piu' corto del previsto non si distingue da un lavoro andato bene**. Le
   righe scartate — un file rotto, un permesso negato, un formato non letto — sono l'unica cosa che
   spiega la differenza, e senza un posto dove leggerle all'utente resta solo il sospetto. In cima
   i numeri che contano per quel prodotto, sotto le righe con l'ora, e un filtro per categoria che
   **nasconde** invece di selezionare (la domanda vera e' «toglimi il rumore»).

   Il registro **non insegue l'ultima riga**: niente auto-scroll, o durante un'attivita' lunga
   diventa illeggibile proprio quando serve.

3. **Il controllo della licenza sta nella barra del titolo, e non si nasconde mai.** Dice lo stato
   quando c'e' una licenza e "Nessuna licenza · Accedi" quando non c'e', e in tutti e due i casi
   apre la stessa finestra. Un controllo che compare solo a licenza attiva manca proprio a chi ne
   ha bisogno: in listBigStorage ci si arrivava solo tentando una funzione Pro, cioe' bisognava
   prima fallire per scoprire che si poteva accedere.

**Le classi condivise si copiano, non si reinventano**: `Shell`, `AppSession`, `ThreadLog`,
`LogBuffer`, `LogCategory`, `Dialogs`, `Icons`, `WindowResizer`, `theme/Themes`, `i18n/I18n`. Lo
stato che deve sopravvivere alla ricostruzione dello Shell (cambio lingua a caldo) vive in
`AppSession` e **non** nello Shell: altrimenti una tendina cancella il lavoro appena fatto.

> **Come si sbaglia, e come si e' sbagliato davvero.** Il 2026-08-21 PicPurg e' nato con la sola
> barra del titolo copiata e una finestra a vista unica: niente barra di sinistra, niente registro,
> niente controllo licenza. Compilava, i test erano verdi, e sembrava finito. Non lo era, e la
> differenza si vede solo aprendolo accanto a un altro programma della linea.
>
> La causa e' che questo file descriveva **la barra del titolo** e dava per implicito tutto il
> resto, e "copia da TraceFox" era stato letto come "copia il pom e il Launcher". Da qui in avanti
> le cinque zone sono scritte, e un progetto nuovo **non e' finito** finche' non ce le ha tutte.

### UI JavaFX

- **La barra del titolo e' sempre NOSTRA, mai quella di Windows.** Lo stage si apre
  `StageStyle.UNDECORATED` e l'intestazione la disegna l'applicazione. Non e' una preferenza
  estetica: la cornice di sistema e' l'unico pezzo di finestra che non segue il tema, quindi su un
  tema scuro resta una fascia chiara sopra un programma scuro, e il programma sembra montato male.

  **Con la cornice se ne vanno anche le sue funzioni, e vanno tutte rimesse** — mancarne una fa
  passare la finestra da "su misura" a "rotta", ed e' il tipo di cosa che nessuno prova finche' non
  serve:

  1. **riduci a icona, ingrandisci/ripristina, chiudi**, in quest'ordine e in alto a destra;
  2. **trascinare la barra sposta la finestra** (memorizzando lo scarto fra punto premuto e angolo,
     o al primo movimento la finestra salta con l'angolo sotto il puntatore);
  3. **doppio clic sulla barra ingrandisce e ripristina**;
  4. **ridimensionamento dai bordi**, che senza cornice non esiste piu': serve un `WindowResizer`
     installato con filtri di evento sulla `Scene`, disattivato quando la finestra e' massimizzata;
  5. il **titolo dello stage si imposta lo stesso** anche se non si vede: e' quello che Windows
     mostra nella barra delle applicazioni e in Alt+Tab.

  L'icona di ingrandisci **cambia con lo stato** (quadrato / due quadrati sovrapposti), e il rosso
  su Chiudi compare **solo al passaggio del mouse**: e' l'unico dei tre che fa perdere quello che
  si sta facendo, e va distinto prima del clic.

- **`Dialogs.decorate` va chiamato su OGNI `Dialog`/`Alert`**: i dialoghi hanno una `Scene` propria e
  non ereditano il foglio di stile della finestra principale. Stessa cosa per i `Popup`.
- **`Platform.runLater` non va usato per evento**: sotto carico la coda FX si intasa e l'interfaccia
  si blocca. Gli eventi si accodano e si applicano a blocchi (una decina di volte al secondo).
- **Niente lavoro bloccante sul thread FX**: connessioni, I/O, comandi esterni, letture di credenziali
  stanno su thread di background, e il risultato torna con `Platform.runLater`.
- **i18n**: i valori con segnaposto `{0}` passano da `MessageFormat`, dove l'apostrofo va
  **raddoppiato** (`''`) o il segnaposto sparisce. Nei testi **senza** argomenti l'apostrofo resta
  singolo, altrimenti si vede davvero a schermo.
- **Icona**: 0% liberta' sul disegno, 100% sui colori.

### L'app in esecuzione tiene in lock il fat-jar

`mvn clean` fallisce mentre l'applicazione e' aperta. Non e' un errore di codice: si chiude l'app e
si ripete.

## Prima di pubblicare

Il build che passa non e' la prova che conta. Su una macchina con la versione precedente installata:

0. **rileggere il `ProductVersion` dell'MSI gia' distribuito** e verificare che i primi tre campi
   del nuovo siano **maggiori**. Va fatto prima di tutto il resto: se il numero non cresce, i punti
   da 1 a 4 misurano un aggiornamento che non poteva avvenire, e si finisce a cercare la causa
   nell'UpgradeCode. `1.0` e `1.0.0` sono lo stesso numero;
1. installare la versione precedente, poi questa **sopra**;
2. **non** deve comparire la richiesta di disinstallare prima;
3. in *App installate* deve restare **una sola** voce;
4. lo stato in `%LOCALAPPDATA%\<Nome>` deve sopravvivere.

Se al punto 2 l'installer chiede di disinstallare, la versione non e' cresciuta abbastanza. Se al
punto 3 le voci sono due, l'UpgradeCode e' cambiato.

## Il wizard di installazione — `teoSetup`

**E' il file che si consegna all'utente finale.** Un solo eseguibile per tutta la linea: mostra i
prodotti con una casella ciascuno, vede cosa e' gia' installato sul PC, e per ognuno propone
**installa / aggiorna / disinstalla**. Gli MSI dei singoli prodotti continuano a esistere e a
essere l'unita' di installazione — il wizard non li sostituisce, **sostituisce il consegnarli a
mano**, che e' la parte che non scalava: sei prodotti per due varianti sono dodici file, e chi
scarica deve gia' sapere quale gli serve e se e' piu' recente di quello che ha.

Non introduce infrastruttura nuova: **legge i manifest che i siti prodotto pubblicano gia'**,
`https://<codice>.teo.place/scarica/versione.json`, e scarica l'MSI da
`https://<codice>.teo.place/scarica/<file>`.

```json
{ "disponibile": true, "versione": "1.0.1", "file": "TraceFox-1.0.1.msi",
  "dimensione": "72,9 MB", "data": "2026-08-16",
  "alternativo": { "disponibile": true, "versione": "1.0.1",
                   "file": "TraceFox-peruser-1.0.1.msi",
                   "dimensione": "72,9 MB", "data": "2026-08-16" } }
```

**`alternativo` e' il secondo installer**, opzionale, e serve dove il prodotto ne pubblica due.
Il *tipo* di secondo pacchetto cambia con il prodotto, ed e' il motivo per cui il campo si chiama
cosi' e non "peruser": in **TraceFox** e' la variante per utente, in **teoSetup** e' l'`.msi`
accanto all'`.exe`. Con il solo campo `file` quel pacchetto sarebbe sul
server e **irraggiungibile dal sito**: chi non ha i privilegi di amministratore resterebbe senza
quello che gli serve, ed e' proprio chi ne ha piu' bisogno. Chi lo legge (wizard e app) lo tratta
come facoltativo: un manifest che non ce l'ha e' valido, non incompleto.

### Come si scarica il wizard stesso — due canali, e nessun sottodominio suo

**teoSetup non ha un `teosetup.teo.place`, e non deve averlo.** Non e' una dimenticanza: un
sottodominio accanto a quelli dei prodotti lo farebbe sembrare **un prodotto fra i prodotti**, che
e' esattamente cio' che non e' — non si vende, non ha una riga in `prodotti`, e non sta nel proprio
catalogo. La sua casa e' la radice, `teo.place`, che e' il posto giusto per il file unico di tutta
la linea.

Si scarica da **due canali**, e sono distinti di proposito:

| canale | dove | file |
|---|---|---|
| **preselezionato** | `https://<codice>.teo.place/scarica/` | `teoSetup-<codice>-<versione>.exe` |
| **neutro** | `https://teo.place/scarica/` | `teoSetup-<versione>.exe` (+ `.msi` in `alternativo`) |

Il preselezionato e' una build del wizard **gia' puntata su quel prodotto**: chi arriva dalla
pagina di PicPurg vuole PicPurg, e fargli aprire un elenco di sei caselle da cui ripescare quella
che stava gia' guardando e' un passaggio che non serve a niente. Ogni cartella prodotto ne tiene
anche il symlink **`teoSetup-<codice>-ultimo.exe`**, cosi' un link fisso sul sito non va aggiornato
a ogni versione.

Il wizard preselezionato e' **dichiarato nel manifest del prodotto**, nel blocco `wizard`, accanto
all'MSI:

```json
{ "disponibile": true, "versione": "1.0.7", "file": "PicPurg-1.0.7.msi",
  "dimensione": "65,8 MB", "data": "2026-08-29", "sha256": "8100f510...",
  "wizard": { "file": "teoSetup-picpurg-1.0.9.exe", "versione": "1.0.9",
              "dimensione": "62,7 MB", "data": "2026-08-29", "sha256": "30661d37..." } }
```

**La versione del blocco `wizard` non e' quella del prodotto**, ed e' giusto che diverga: nello
stesso manifest qui sopra il prodotto e' `1.0.7` e il wizard `1.0.9`, e su queryF5 convivono
`1.2.9` e `1.0.9`. Sono due programmi diversi che si aggiornano quando gli pare. Chi confronta
quei due numeri aspettandoseli uguali sta leggendo un difetto che non c'e'.

> **Il `/scarica/` di nginx e' un alias scritto a mano, vhost per vhost — non deriva niente dal nome
> host.** Ogni sito prodotto ha
> `location ~ ^/scarica/(...)$ { alias /var/www/scarica/<codice>/$1; }`, e `teo.place` ha lo stesso
> blocco che punta a `/var/www/scarica/teosetup/`.
>
> Va scritto perche' la riga "il `codice` e' anche il sottodominio", qui sotto, si legge facilmente
> come se **gli URL si costruissero dal nome host**. Non e' cosi', e la differenza si vede proprio
> su teoSetup: il 2026-08-30 l'assenza di `teosetup.teo.place` e' stata segnalata come un rischio
> ("se qualcosa ne derivasse l'URL non troverebbe niente"), ma nessun percorso di codice puo'
> derivarlo — il wizard e' escluso dal proprio catalogo (`CODICE_DEL_WIZARD`), ed e' il catalogo
> a generare quegli URL. Verificato: `teo.place/scarica/versione.json`,
> `teo.place/scarica/teoSetup-1.0.9.exe` e
> `picpurg.teo.place/scarica/teoSetup-picpurg-1.0.9.exe` rispondono tutti.

### Come si vincola un prodotto al wizard — presenti e futuri

Un prodotto entra nel wizard con **una riga** in `Catalogo.PRODOTTI` (repo `teoSetup`):

```java
new Prodotto("tracefox", "TraceFox",
             "8F94291A-270E-3C72-AB8D-28FB57793711",   // per macchina, dalla tabella qui sopra
             "C079027E-DE15-3237-B36C-5508CAD4A08A",   // per utente, null se non esiste
             "Proxy di debug HTTP/HTTPS: registra, ispeziona e manipola il traffico."),
```

Il `codice` e' l'`artifactId`, **ed e' anche il sottodominio**: da li' il wizard ricava manifest e
download da solo. Quindi la riga si scrive **una volta sola**: pubblicare una versione nuova non
richiede di toccare il wizard, basta aggiornare il `versione.json` del sito come si fa gia'.

**L'aggancio non e' affidato alla memoria.** `CatalogoTest`, nel wizard, legge la tabella
"Identita' dei prodotti" di questo file (dalla copia `STANDARD.md` presente nel repo) e pretende
che **ogni prodotto elencato li' abbia una riga nel catalogo, con lo stesso UpgradeCode**. Un
progetto nuovo aggiunto alla tabella fa **fallire il build del wizard** finche' non lo si aggiunge
anche al catalogo — che e' il punto: un elenco che si aggiorna a mano si dimentica, e il sintomo
sarebbe un prodotto che semplicemente non compare, senza nessun errore da nessuna parte.

Il confronto e' sull'**UpgradeCode**, non sul nome: e' cosi' che Windows riconosce un prodotto, ed
e' l'unico dato che non cambia quando il prodotto viene rinominato o cambia versione. Per lo stesso
motivo un UpgradeCode sbagliato nel catalogo non da' errore — mostrerebbe "non installato" su una
macchina che ce l'ha, e proporrebbe un'installazione affiancata.

### Cosa deve avere un prodotto per essere installabile dal wizard

1. **UpgradeCode congelato** nella tabella "Identita' dei prodotti" (vale gia' come regola sua).
2. **MSI con il nome della convenzione** (`<Nome>-<versione>.msi`): il manifest lo cita per nome.
3. **Un `versione.json` pubblicato** sul suo sottodominio. Finche' `disponibile` e' `false` il
   prodotto compare nel wizard ma **disabilitato, con il motivo scritto** — meglio di un elenco che
   nasconde meta' linea senza dire perche'.
4. **Versione a tre campi crescenti**, come per tutto il resto: il wizard confronta i primi tre
   campi esattamente come Windows, quindi un `1.0` pubblicato dopo un `1.0.0` non risulta un
   aggiornamento nemmeno qui.
5. **La build preselezionata del wizard nella sua cartella di download**
   (`teoSetup-<codice>-<versione>.exe`, il symlink `-ultimo.exe` e il blocco `wizard` nel
   manifest, vedi sopra). Senza, il sito del prodotto nuovo puo' offrire solo l'MSI nudo, oppure
   il wizard neutro che chiede all'utente di ritrovare da solo il prodotto da cui e' appena
   arrivato. Non rompe niente e nessun test se ne accorge: e' esattamente il tipo di voce che si
   dimentica.

## Pubblicare un prodotto — sito, download, home

Il wizard e l'app leggono il manifest; **chi lo pubblica, e dove, non era scritto**, e un prodotto
e' stato consegnato con l'MSI ma senza wizard, senza sito e senza scheda sulla home. Il server e'
**KVM2** (`ssh KVM2`, utente `teo`, `45.93.137.219`), nginx con un vhost per sottodominio, DNS e
certificato su Cloudflare (SSL "Full", certificato condiviso `snippets/ssl-teo-place.conf`).

| cosa | dove | chi scrive |
|---|---|---|
| installer + `versione.json` | `/var/www/scarica/<codice>/` | `teo`, senza sudo (cartelle `teo:www-data` 2755) |
| sito prodotto | `/var/www/html/<codice>/dist/` | `teo`, senza sudo |
| vhost | `/etc/nginx/sites-available/<codice>.teo.place` + link in `sites-enabled` | **sudo**: lo lancia l'utente (`applica.sh` del repo) |
| DNS | Cloudflare, record A proxato verso KVM2 | l'utente |
| wizard neutro | `/var/www/scarica/teosetup/` (`teo.place/scarica/`) | `teo` |
| scheda sulla home | `/var/www/html/index.html`, array dei prodotti (una voce `{ sigla, nome, … }`) | `teo` |

Le regole, tutte imparate pagando:

- **Il sito prodotto e' una copia di quello di un prodotto esistente** (Vite/React, sorgenti nel repo
  in `sito\`): cambia solo `src/prodotto.ts`, piu' icona, tema e `index.html`. Tema **fisso**
  scuro, l'icona **vera** del prodotto, nessun selettore di tema. Ogni frase di `prodotto.ts` e'
  vera nel codice; cio' che non e' provato va nei `limiti`.
- **Il bottone scarica il wizard preselezionato**, non l'MSI: blocco `wizard` nel manifest e
  symlink `teoSetup-<codice>-ultimo.exe`. La scheda della home punta a quel symlink, mai a un file
  con la versione nel nome.
- **Ogni file che si sostituisce lascia un backup accanto**: `versione.json.bak-<versione
  precedente>`, `index.html.bak-<motivo>`. Le versioni vecchie degli installer **non si
  cancellano**.
- **Si verifica dall'esterno, non dal server**: `https://<codice>.teo.place/`, il manifest, il
  download (dimensione e SHA-256 uguali a quelli locali), la scheda sulla home nel browser.
- **Cloudflare tiene in cache anche i 404** (4 ore): una risorsa chiesta prima che il vhost
  esistesse resta rotta anche dopo. Si verifica con `?v=...` e si fa il purge di quell'URL.
- **Il manifest nuovo eredita il blocco `wizard` dal precedente** finche' teoSetup non cambia
  versione: riscriverlo da `build-msi.ps1` senza quel blocco rimette l'MSI nudo sul bottone.

## Il controllo aggiornamenti dentro l'app

**Ogni app desktop chiede da sola al proprio sito se e' uscita una versione piu' recente, e la
installa.** Non e' infrastruttura nuova: legge lo **stesso** `versione.json` che accende il
bottone di download sul sito e che il wizard interroga per decidere cosa proporre. Tre consumatori,
un file solo.

Il pacchetto e' `<...>/update/` (in TraceFox e listBigStorage sotto `core/`, dove tutto il resto sta
li'), con le stesse classi:

| classe | cosa fa |
|---|---|
| `Version` | confronto **con la regola di Windows**, sui primi tre campi |
| `Manifest` | il `versione.json`, parser **tollerante** (campi `sha256` e `alternativo` compresi) |
| `UpdateCheck` | la richiesta HTTP **e**, separata, la decisione |
| `Scaricatore` | il pacchetto, con l'avanzamento e la verifica dell'impronta — copia di quello di `teoSetup` |
| `Installatore` | lo script che aspetta la chiusura, installa e riapre il programma |

Le cose che decidono se funziona, e nessuna e' ovvia:

- **Il confronto e' quello di Windows, non fra stringhe.** `1.0` e `1.0.0` sono lo **stesso**
  numero: un confronto testuale li vedrebbe diversi e offrirebbe un aggiornamento che, una volta
  scaricato, risponde «il programma e' gia' installato» — dando la colpa al programma di una cosa
  decisa dal numero. Non e' teorico: gli MSI distribuiti dicevano `1.0` mentre i pom dicevano
  `1.0.0`. La stessa regola vive in `teoSetup.core.windows.Versione`, e le copie devono restare
  d'accordo o app e wizard direbbero due cose diverse sullo stesso pacchetto.
- **La rete non puo' impedire niente.** Il controllo parte in fondo all'avvio, su un thread suo, e
  qualunque guasto — rete assente, sito fermo, JSON malformato — vale «nessun aggiornamento». Un
  avviso di errore per una cosa che l'utente non ha chiesto e' solo un fastidio, e un programma che
  si blocca perche' non ha potuto chiedere qualcosa a un sito e' un guasto inventato da noi.
- **Si controlla anche ad APP APERTA, e non con un webhook.** Dal 2026-09-03, su tutti e cinque i
  desktop: oltre al controllo d'avvio, l'app **richiede da sola ogni sei ore** finche' resta
  aperta, e c'e' un pulsante «Controlla aggiornamenti» che lo forza a mano. Prima si chiedeva
  **solo** all'avvio, e chi teneva il programma aperto per giorni — con un inventario o una
  scansione lunga, succede — restava con la versione vecchia senza saperlo. Non e' un webhook di
  proposito: un push vero vorrebbe dire che l'app espone un endpoint raggiungibile da fuori (dietro
  NAT/firewall non si puo') o tiene una connessione persistente verso un server nostro,
  infrastruttura nuova, mentre tutta la linea vive su un `versione.json` statico. Quindi si
  **richiede lo stesso file**, non si aspetta di essere chiamati. Le regole, uguali ovunque:
  l'automatico **non ripropone** una versione gia' vista e chiusa e **tace** se non c'e' niente
  (come all'avvio); il manuale la rimostra sempre e, dove non c'e' un registro (watchCLI, queryF5),
  risponde «hai gia' l'ultima versione» o il pulsante sembrerebbe rotto; **un dialogo alla volta**;
  e il pianificatore gira in `scheduleWithFixedDelay`, che al primo errore non catturato cancella
  in silenzio i tick successivi — quindi il controllo e' avvolto in un `try`, e si spegne in
  `App.stop()`. Il riferimento da cui si copia e' `listBigStorage`; il *cosa si fa con la risposta*
  (proposta, download con impronta, chiusura-installazione-riapertura) non cambia. Il pulsante sta
  nella barra di sinistra (dove c'e', sopra Impostazioni) o nella barra del titolo (watchCLI).
- **Il parser e' tollerante di proposito.** Un campo in piu' non rompe niente (il sito puo'
  aggiungerne), uno in meno vale `null`. Un'eccezione qui diventerebbe un avviso che **non compare
  mai**, cioe' un difetto invisibile.
- **La decisione e' separata dalla rete**, ed e' l'unica parte che puo' mentire in modo credibile:
  cosi' si prova senza un server. Sedici prove per progetto.
- **L'URL si deriva da `AppInfo.HOME_URL`**, mai riscritto a mano: il giorno che il dominio cambia
  non ci sono due posti da ricordare.
- **Si installa da solo, e resta UN clic.** Prima si apriva il browser sul pacchetto, e da li'
  l'utente aveva davanti sei passaggi: aspettare il download, trovare il file, riconoscerlo,
  avviarlo, rispondere all'installer, riaprire il programma. Ognuno e' un punto in cui si
  rinuncia — e un aggiornamento abbandonato a meta' e' un aggiornamento non fatto, cioe' un
  cliente che non riceve nemmeno le correzioni. Adesso il programma scarica, verifica, si chiude,
  installa e si riapre. **Ce l'hanno tutti e cinque i desktop** dal 2026-08-21; il riferimento da
  cui si copia e' `listBigStorage`.

  **Un modello solo, uguale ovunque.** Le due classi si copiano senza adattamenti (a parte il
  logger, dove manca slf4j) e il record `UpdateCheck.Update` ha gli stessi campi in tutti: chi
  legge il codice di un prodotto sta leggendo anche quello degli altri quattro. Dove il prodotto
  ha un **registro a schermo** l'esito del tentativo precedente finisce li'; dove non c'e'
  (watchCLI, queryF5) si parla **solo se e' andato male** — riuscito, la versione nuova si vede
  gia' nella barra del titolo, e un avviso in piu' all'avvio e' un pedaggio.

  **TraceFox ha l'unica eccezione, e non e' rinunciabile**: pubblica due pacchetti, per macchina e
  per utente, con UpgradeCode diversi. Installare l'uno sopra l'altro non da' errore, **affianca**
  una seconda installazione — quindi li' l'automatismo vale solo per il pacchetto per macchina, e
  chi gira dal profilo utente torna al browser.

  Le cose che rendono possibile che *sembri* una cosa sola, e nessuna e' facoltativa:

  - **Il consenso di amministratore resta, ed e' uno solo.** I pacchetti della linea sono per
    macchina: scrivono in `Program Files`, e nessun processo non elevato puo' farlo. "Silenzioso"
    vuol dire che dopo quel clic non c'e' piu' niente da leggere o da scegliere — `msiexec /qn
    /norestart`, nessuna finestra di installazione.
  - **Serve un processo che sopravviva all'applicazione.** Un MSI non sostituisce i file di un
    programma aperto, e il programma non puo' assistere alla propria sostituzione: si lascia in
    piedi un PowerShell nascosto (`-EncodedCommand`, **mai** uno script su disco, che sarebbe
    scrivibile da un processo non elevato e poi eseguito elevato) che aspetta il PID, installa e
    riapre.
  - **Il riavvio avviene nel processo NON elevato.** Riaprire il programma da dentro quello
    elevato lo farebbe girare come amministratore, cioe' su un altro `%LOCALAPPDATA%`: run,
    impostazioni, credenziali DPAPI e **licenza** — che e' cifrata per l'utente che l'ha attivata
    — sparirebbero alla vista, e sembrerebbe che l'aggiornamento abbia cancellato tutto.
  - **Si verifica l'impronta prima di installare.** Gli MSI della linea **non sono firmati**,
    quindi msiexec li esegue senza sapere da dove vengono: il `sha256` del manifest, servito in
    HTTPS dallo stesso sito del pacchetto, e' l'unica cosa che distingue il file giusto da uno
    troncato o sostituito. Facoltativo: un manifest che non ce l'ha si scarica come prima.
  - **L'esito si riferisce al riavvio successivo.** Il programma si e' chiuso e riaperto da solo,
    quindi senza una riga sarebbe successo tutto mentre l'utente non guardava. Va nel **registro**,
    non in una finestra: un avviso modale a ogni riavvio e' un pedaggio. Il rapporto si consuma
    leggendolo.
  - **Se il programma non si chiude, non si installa niente.** Installare sopra un'applicazione
    aperta non fallisce in modo pulito: si prenota per il riavvio del PC e lascia un aggiornamento
    "riuscito" che non e' successo.
- **`teoSetup` non diventa inutile**: resta il modo di installare la **prima** volta e di gestire
  piu' prodotti insieme. Quello che sparisce e' il giro dal browser per chi il programma ce l'ha
  gia' aperto davanti.

Aggiungerlo a un progetto nuovo vuol dire: `HOME_URL` in `AppInfo`, le classi copiate, la
chiamata in fondo all'avvio, e `jackson-databind` nel pom se non c'e' gia' (dove manca anche slf4j
si usa `System.Logger` del JDK, senza aggiungere logback per due righe di diagnostica).

## L'accesso per Claude (MCP)

**Dal 2026-11-09 watchHost, listBigStorage, queryF5 e SorterBarcode si lasciano pilotare da
Claude** (Claude Code, Claude Desktop) attraverso un server MCP dentro il programma installato.
Non e' un programma a parte: e' il programma aperto, con la sua sessione, che risponde. Le
regole sono le stesse ovunque, e un prodotto nuovo che lo aggiunge le segue tutte.

| prodotto | trasporto | indirizzo |
|---|---|---|
| watchHost | HTTP, nello stesso server della dashboard | `http://127.0.0.1:8470/mcp` |
| listBigStorage | HTTP | `http://127.0.0.1:8471/mcp` |
| queryF5 | HTTP | `http://127.0.0.1:8472/mcp` |
| SorterBarcode | **stdio**: `SorterBarcode.exe --mcp` | — |

- **HTTP per chi ha uno stato, stdio per chi non ce l'ha.** Un programma con una finestra aperta
  va raggiunto *dov'e'*: un server stdio lo farebbe lanciare una seconda volta da Claude, con una
  sessione vuota. SorterBarcode invece e' un lettore senza stato, e lanciarlo per ogni
  conversazione non costa niente: niente porta e niente token. `--mcp` esce prima che parta WPF,
  e su stdout esce **solo** JSON-RPC (il resto va su stderr, o la conversazione si rompe).
- **Porta fissa per prodotto**, la prossima libera e' **8473**. Porta occupata: lo si scrive nel
  log e in Impostazioni, il programma non si ferma.
- **Spento per default**, acceso solo da un gesto dell'utente in Impostazioni. Acceso a caldo
  apre la porta, spento la chiude (watchHost: risponde 404).
- **Tre controlli, tutti e tre**: indirizzo e header `Host` di loopback (DNS rebinding), `Origin`
  assente o di loopback, `Authorization: Bearer <token>` confrontato a tempo costante. Il token
  sono 32 byte casuali **nel `CredentialStore` sotto `mcp-token`**, mai in chiaro su disco; la
  pagina Impostazioni mostra il comando `claude mcp add` gia' completo, con Copia e "Rigenera
  token" (il vecchio smette subito di valere).
- **Nessun segreto passa dall'MCP**, in nessuna direzione: si sceglie un server o un profilo per
  alias, e la password resta dov'e'. Nessun tool crea connessioni, ne' accetta una chiave host
  nuova: quella la guarda una persona.
- **Niente di distruttivo.** Nessun tool cancella file, run, server, soglie o archivio, ne'
  installa aggiornamenti (chiuderebbe il programma). In listBigStorage resta valida la house rule
  1: l'unica scrittura sul server e' la cancellazione su gesto dell'utente, e l'MCP non la espone.
- **SQL scritto da un modello e' sola lettura TECNICA, non promessa** (queryF5): profilo
  abilitato a mano uno per uno (nessuno di default), connessione dedicata, filtro lessicale
  (una sola istruzione, solo SELECT/SHOW/DESCRIBE/EXPLAIN, niente `INTO OUTFILE`, `LOAD_FILE`,
  `SLEEP`, `/*! */`…), `readOnly` propagato al server, transazione `READ ONLY` chiusa con
  `ROLLBACK`, limiti di righe e di tempo. Servono tutti: una transazione READ ONLY da sola **non**
  ferma `INTO OUTFILE`, e un utente `root` ha il privilegio FILE.
- **Si vede cosa ha fatto Claude**: ogni chiamata finisce nel registro a schermo (categoria
  "Claude") o, dove il registro non c'e', in un file nella cartella di controllo e in una lista
  nelle Impostazioni. Un programma che cambia stato senza che si capisca chi e' stato si legge
  come un guasto.
- **Il protocollo e' scritto a mano** (Jackson nei Java, System.Text.Json in .NET): `initialize`,
  `ping`, `tools/list`, `tools/call`, notifiche con 202, nessun SSE. Il dispatcher e' una classe
  pura, testata senza rete; il trasporto e' a parte.
- **Trappola Java, `Unable to establish loopback connection`**: `HttpServer` apre un socket
  AF_UNIX in `%TEMP%`, e con un TEMP dal percorso anomalo (succede nelle sandbox di sviluppo) non
  parte. Il ripiego e' **solo su fallimento** e usa `jdk.net.unixdomain.tmpdir` =
  `%LOCALAPPDATA%\<Nome>\sock`, mai la home dell'utente. **Impostare la proprieta' dopo il
  fallimento non basta**: il JDK la legge una volta per processo, al primo Selector (in queryF5
  e' quello della licenza, prima dell'MCP). Per questo il fallimento lascia un segnaposto
  `sock\ripiego-af-unix.txt`, e il `Launcher` — come prima istruzione — imposta la proprieta' se
  lo trova (un `-D` esplicito vince): vale dall'avvio successivo. Cancellare il segnaposto torna
  al comportamento normale. Agli utenti normali non succede: e' stato visto solo con
  `%TEMP%`/`%LOCALAPPDATA%` reindirizzati nella sandbox di sviluppo.

## Il licensing nelle app desktop

**Le app desktop sono a pagamento, e dal 2026-08-16 lo verificano.** Prima non era vero: TraceFox
e listBigStorage controllavano qualcosa, watchCLI e queryF5 **niente** — erano in vendita su Lemon
Squeezy e chi pagava non riceveva nulla di diverso da chi non pagava.

Il modello e' **app intera a pagamento con 15 giorni di prova**, quindi lo sbarramento e'
**all'avvio** e non su singole funzioni. Il contratto con il server (`activate` / `refresh` /
`deactivate`, i claim del JWT, i codici d'errore) sta in `INTEGRAZIONE-DESKTOP.md`, sul server in
`/var/www/html/authenticator/`. Qui solo cio' che vale per tutti:

- **Il pacchetto e' lo stesso ovunque**: `DeviceId`, `Entitlement`, `LicenseClient`,
  `LicenseException`, `Licensing`, piu' `license/jwt_pubblica.pem` fra le risorse. Il riferimento
  da cui si copia e' TraceFox. Dove manca slf4j si usa `System.Logger` del JDK invece di tirare
  dentro logback per due righe di diagnostica.
- **Si verifica A OGNI AVVIO, e non e' piu' un controllo solo locale.** Dal 2026-08-23 ogni app
  chiede al server se la licenza esiste ancora, prima dello sbarramento
  (`Licensing.verificaAllAvvio()`). Prima il controllo era locale — due file e una firma RS256 — e
  il rinnovo partiva solo quando mancavano tre giorni alla scadenza dell'entitlement: **cancellare
  una licenza dal database non aveva nessun effetto per due settimane**. Una disdetta, un rimborso
  o una revoca devono farsi sentire al primo avvio dopo, non quando scade un token.

  I tre esiti, e sono davvero tre:

  | risposta | cosa succede |
  |---|---|
  | il server **conferma** | entitlement nuovo salvato, orologio della grazia riazzerato |
  | il server **nega** | l'entitlement locale viene **buttato subito**, e lo sbarramento scatta |
  | **non si riesce a chiedere** | non succede niente: si continua finche' la grazia regge |

- **La grazia offline vale 15 giorni, contati dall'ultima verifica RIUSCITA.** Queste applicazioni
  si usano spesso dove la rete e' limitata o chiusa, e bloccarle il giorno in cui non si riesce a
  chiedere il permesso sarebbe inutile esattamente quando servono. Quindici giorni di aereo, di
  cantiere o di rete chiusa si attraversano lavorando; il sedicesimo no, perche' a quel punto il
  programma non ha piu' nessun motivo di credere che la licenza esista ancora. Il momento
  dell'ultima verifica sta in `%LOCALAPPDATA%\<Nome>\ultima-verifica.txt`, e lo stato si chiama
  `NON_VERIFICATA`: non e' "scaduto" e non e' "non ce l'hai", ed e' l'unico dei tre che si risolve
  **aprendo il programma con la rete**, non pagando. Va detto con parole sue, o si manda al
  checkout chi ha gia' pagato.

- **Un server rotto non e' un cliente moroso.** `offline`, `http_5xx` e `http_429` **non** tolgono
  la licenza: sono guasti nostri, e farli pagare a chi ha pagato trasformerebbe un disservizio di
  dieci minuti in una giornata di assistenza. Valgono come "non si e' potuto chiedere". Solo un
  rifiuto del contratto (`invalid_license`, `inactive`, `cancelled`, `not_activated`...) revoca.

- **L'avviso arriva prima del blocco.** Dove c'e' un registro a schermo (TraceFox, PicPurg) ogni
  avvio senza verifica ci scrive quanti giorni restano; dove non c'e' (watchCLI, queryF5) compare
  un avviso **solo negli ultimi tre giorni**, che e' il compromesso fra una sorpresa e un pedaggio.
  In listBigStorage la licenza non sbarra il programma ma apre due funzioni, quindi finita la
  grazia `isPro()` torna falso e quelle si richiudono: stessa regola, effetto proporzionato.
- **`plan` vale `pro` oppure `trial`, e valgono uguale.** Rifiutare `trial` non chiude fuori
  qualche curioso: chiude fuori dei clienti veri, e un secondo controllo sul piano sarebbe solo un
  secondo posto in cui sbagliare — la scadenza e' gia' dentro `exp`, tagliato dal server. I due
  valori restano distinti per il **messaggio**: "la prova e' finita" e "l'abbonamento e' scaduto"
  mandano la stessa persona in due posti diversi.

- **Il free trial di Lemon Squeezy NON e' `trial`.** Dal 2026-08-20 `plan` vale `trial` solo per
  le licenze **omaggio emesse a mano** (`piani.tipo = 'trial'`), che dietro non hanno nessun
  abbonamento e quando finiscono non diventano niente.

  Un free trial e' un'altra cosa: e' un **abbonamento gia' sottoscritto**, con un piano scelto e
  una carta registrata, in cui il primo addebito arriva dopo quindici giorni. Chiamarlo "prova"
  contraddiceva l'area clienti, che sulla **stessa licenza** scrive «Attiva · Si rinnova il 4
  settembre», e diceva a chi si era appena abbonato che stava per restare senza. Due nostre
  superfici che descrivono la stessa cosa in due modi sono un difetto, non una sfumatura.

  Corollario: **`prova_finisce_il` non decide piu' `plan`.** Resta come dato — dice quando parte
  il primo addebito — ma non cambia che tipo di licenza sia.

- **Su una variante in abbonamento la "License length" di Lemon Squeezy non vuol dire niente.**
  LS la mostra solo per i pagamenti singoli: sugli abbonamenti la chiave segue l'abbonamento e
  scade con lui, e il campo e' `display:none` nel pannello. Il valore che resta memorizzato e'
  un default inerte, e infatti **diverge fra prodotti** senza alcun effetto — listBigStorage dice
  `1 months` sul mensile e sull'annuale, tutti gli altri dicono `1 years`.

  Va scritto perche' letto dall'API sembra un difetto grosso e credibile — "l'abbonamento annuale
  emette una chiave che scade in un mese" — e il 2026-08-28 e' stato segnalato come tale. Non lo
  e': l'unico posto dove quel campo conta davvero e' il **lifetime**, che infatti su tutti e sei i
  prodotti ha `is_license_length_unlimited: true`. Prima di "correggerlo" si guarda
  `is_subscription`.

- **`ren` dice se `lex` e' un RINNOVO o una FINE, e serve perche' la data e' la stessa.** Per un
  abbonamento attivo `scade_il` e' il giorno del prossimo addebito; per uno **disdetto** e' il
  giorno in cui l'accesso finisce davvero. La disdetta non blocca subito — chi ha pagato fino al
  31 usa il programma fino al 31 — quindi l'entitlement esiste in entrambi i casi e dalla data non
  si capisce quale sia.

  Sbagliarlo non e' una questione di stile: «si rinnova» a chi ha disdetto gli annuncia un
  addebito che non ci sara', «fino al» a chi non ha disdetto gli dice che sta per perdere il
  programma. Sono due bugie sul denaro, in direzioni opposte. Il claim e' **facoltativo** come
  `ptp` e `lex`: assente vale `false`, e allora si scrive la data secca, che non promette niente.
- **Uno sbarramento senza via d'uscita si legge come un guasto.** La finestra dice cosa e'
  successo (tre frasi per tre situazioni: prova finita, abbonamento scaduto, mai avuto niente) e
  ha il rimedio a portata di mano: una sezione che apre il browser sui piani, una per **accedere
  dal browser**, e una per incollare la chiave ricevuta per email.

- **Dal 2026-08-20 la strada normale e' l'AUTORIZZAZIONE DAL BROWSER.** L'applicazione mostra un
  codice corto e apre `account.teo.place/collega`; li' l'utente e' gia' autenticato, vede quale
  programma e su quale computer sta autorizzando, sceglie la licenza e conferma. Il programma nel
  frattempo domanda al server, e si sblocca da solo. E' il flusso di TV e console. Il campo per la
  chiave **resta**, sotto — chi ne ha una e non ha un account non va lasciato fuori.

  **Nessuna applicazione desktop chiede la password dell'account, e non e' una preferenza.** Una
  finestra che la chiede insegna a digitarla in qualunque finestra le somigli, ed e' esattamente
  la forma del phishing — la stessa finestra dice gia' che «questa finestra non chiede mai i dati
  di una carta, e nessun programma dovrebbe farlo». Per una password vale identico. Per questo nei
  client **non esiste piu' il codice che la spediva**: `accesso()`, `licenzeDiQuestoProdotto()` e
  `attivaDaAccount()` sono stati tolti da `LicenseClient`, non solo scollegati dalla UI. Un
  percorso funzionante lasciato li' e' a disposizione della prima modifica che ha fretta. Gli
  endpoint sul server restano, perche' li usa l'area clienti — nel browser, che e' il posto giusto.

  **La chiave non viene "passata dal backend", e non e' un ripiego: il backend non ce l'ha.** In
  `licenze` sta solo `chiave_hash` (sha256) piu' gli ultimi quattro caratteri per la UI — «MAI la
  chiave in chiaro» e' scritto nello schema — e nemmeno il pannello di Lemon Squeezy sa mostrarne
  una gia' emessa: la puo' solo rigenerare. Chi ha autorizzato dal browser ha pero' gia' dimostrato
  di essere il titolare, quindi il server **firma direttamente l'entitlement**, partendo dall'hash
  gia' salvato. Il claim `lkh` e' per definizione i primi 16 caratteri di quello stesso sha256,
  quindi il token e' **identico** a quello che darebbe la chiave.

- **I due codici hanno due mestieri diversi, e confonderli rompe tutto.**
  `POST /api/license/device/inizia` ne restituisce due. Il **`deviceCode`** e' lungo, resta
  nell'applicazione, non si mostra a nessuno ed e' **l'unica cosa che ritira l'entitlement**; in
  tabella ne sta solo l'hash. Il **`userCode`** e' corto (`AB7K-9TQF`) perche' lo legge una
  persona da uno schermo e lo ribatte altrove, e proprio perche' e' indovinabile **non autorizza
  niente da solo**: identifica la richiesta, non la concede. L'alfabeto non ha `0`/`O` ne'
  `1`/`I`/`L`, che a ribattere si confondono.

  L'attivazione avviene quando l'app **ritira** (`POST /api/license/device/stato`), non quando il
  browser conferma: cosi' l'entitlement nasce gia' legato al dispositivo che lo ritira, e non resta
  scritto da qualche parte in attesa che qualcuno passi a prenderlo. Si ritira **una volta sola** —
  lo stato `consumata` esiste per questo. La richiesta vive **dieci minuti**: una richiesta in
  attesa e' una finestra aperta, e se nessuno conferma l'unica cosa che la richiude e' il tempo.

  Nell'app, l'attesa sta su un thread **demone** e smette quando la finestra si chiude; un errore
  di rete **non la interrompe**, si riprova al giro dopo. Chi ha appena confermato dal browser non
  deve trovare un errore perche' una singola richiesta e' andata storta.

- **Il token di dispositivo** (`POST /api/license/refresh-device`) serve al rinnovo: `refresh` si
  autentica con la chiave, che in questo flusso non e' mai stata vista, e senza alternativa ogni
  quattordici giorni il programma tornerebbe a chiedere la password — il fastidio che il login
  doveva togliere. Sul server ne resta solo l'**hash**, su `attivazioni`, dove licenza+dispositivo
  e' gia' chiave unica: da questo discendono gratis due cose che altrimenti erano da scrivere a
  mano — revocare un dispositivo dall'area clienti ne cancella il token, e disdire l'abbonamento li
  invalida tutti insieme.

- **Il limite dispositivi, su quella strada, lo fa valere il NOSTRO database.** E' una decisione
  esplicita: Lemon Squeezy lo applica dentro la propria `activate`, che vuole la chiave, e senza
  chiave quella porta e' chiusa. Conseguenza da conoscere: per una licenza di origine
  `lemonsqueezy` attivata da account, il contatore di LS non vede quel dispositivo.

  Cio' che **non** cambia, ed e' il punto: si passa dagli stessi `verificaStatoLicenza` e
  `firmaEntitlement` del percorso con la chiave. Disdetta, rimborso e scadenza fermano anche
  questa strada, e `exp` resta tagliato a `scade_il`.
- **Prova e acquisto sono la stessa pagina.** Da quando la prova e' il free trial nativo,
  *avviare la prova e' un checkout*: due sezioni distinte descriverebbero due strade che portano
  allo stesso posto. `POST /license/trial` non esiste piu'.
- **L'etichetta dice quanto manca, non quando finisce.** In prova si contano i **giorni**
  ("Prova · 12 giorni"): una data costringe a guardare il calendario per capire la stessa cosa. Con
  un abbonamento si dice il **tipo** e cosa succede a quella data — "Pro mensile · si rinnova il 4
  settembre 2026" oppure, se e' stato disdetto, "Pro mensile · fino al 4 settembre 2026". A vita
  solo "Pro · a vita".

  **Quando non c'e' niente, l'etichetta lo dice lo stesso**: "Nessuna licenza · Accedi". Un'etichetta
  vuota non si distingue da un'etichetta rotta, e chi guarda non ha modo di sapere quale delle due
  sta vedendo. L'invito resta dopo il separatore, o si toglierebbe la via d'uscita a chi la licenza
  ce l'ha e deve solo collegarla. **La barra del titolo non riusa le stringhe del dialogo**: li' si
  dice dove si e', nel dialogo cosa si puo' fare, e una chiave sola per due mestieri li lega per
  sbaglio — e' successo, e il titolo si e' ritrovato a dire "Accedi nel browser".

  I giorni si contano da **`lex`**, non da `exp`. La prova dura quindici giorni e la finestra
  offline quattordici: nei primi giorni `exp` cade **prima** della fine vera, e contare da li'
  anticiperebbe la scadenza — cioe' direbbe a un cliente che sta per perdere qualcosa che non sta
  perdendo. E' la stessa confusione che per un abbonamento annuale gli farebbe credere di avere
  due settimane invece di un anno.

- **I file di licenza vanno in `%LOCALAPPDATA%\<Nome>`**, anche dove la configurazione sta altrove
  per ragioni storiche (watchCLI e queryF5 la tengono in `%APPDATA%`, e spostarla farebbe sparire
  connessioni salvate a chi aggiorna). Per l'entitlement `%LOCALAPPDATA%` e' comunque la scelta
  giusta: `%APPDATA%` lo sincronizza OneDrive, e un entitlement e' legato al `deviceId` di *quella*
  macchina — copiato altrove non vale, quindi sincronizzarlo non serve a nessuno.

Il threat model non cambia: un client Java decompilato e patchato resta possibile ed e' **atteso**.
L'obiettivo e' attrito ed equita', non inviolabilita'. **Un solo checkpoint**, nessun segreto nel
client: moltiplicare i controlli non rende piu' difficile patchare, rende solo il codice piu'
difficile da leggere.

## Un progetto nuovo

Si parte copiando da **TraceFox** (`pom.xml`, `build-msi.ps1`, `VersionTest`, `Launcher`) e si
cambiano: `groupId`/`artifactId`/`name`, `app.main.class`, il blocco identita' dello script e la
riga della tabella qui sopra. L'UpgradeCode del prodotto nuovo si **genera una volta sola** e si
congela subito:

```powershell
# UUID v3 di "UpgradeCode/<vendor>/<nome>" — la stessa formula di jpackage, cosi' il primo
# pacchetto ha lo stesso codice che jpackage avrebbe derivato, e chi installa la 1.0.0 non si
# ritrova affiancato al primo aggiornamento.
$s = "UpgradeCode/teo.place/<Nome>"
$b = [System.Security.Cryptography.MD5]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes($s))
$b[6] = [byte](($b[6] -band 0x0f) -bor 0x30); $b[8] = [byte](($b[8] -band 0x3f) -bor 0x80)
$h = ($b | % { $_.ToString("x2") }) -join ""
"$($h.Substring(0,8))-$($h.Substring(8,4))-$($h.Substring(12,4))-$($h.Substring(16,4))-$($h.Substring(20,12))".ToUpper()
```

Poi si **aggancia al wizard**, ed e' l'ultimo passo perche' richiede l'UpgradeCode gia' congelato:
riga nella tabella "Identita' dei prodotti" qui sopra, riga in `Catalogo.PRODOTTI` di teoSetup con
lo **stesso** UpgradeCode, e `versione.json` pubblicato sul sottodominio. Non e' facoltativo e non
si dimentica: senza la riga nel catalogo il build del wizard fallisce (`CatalogoTest`), e senza il
manifest il prodotto compare disabilitato con scritto il motivo.

### La lista di controllo, perche' "copia da TraceFox" non basta

Il 2026-08-21 un progetto nuovo e' stato consegnato compilante, con i test verdi e la barra del
titolo giusta, ma **senza la barra di sinistra, senza il registro e senza il controllo della
licenza**: cose che questo file dava per implicite e che nessun test puo' vedere. Da qui in avanti
la lista e' scritta. Un progetto nuovo e' finito quando ha **tutte** queste voci.

**Impianto** (vedi "L'impianto della finestra"):

- [ ] `BorderPane` con le **cinque zone**: barra del titolo nostra, barra di sinistra, centro,
      registro a destra, piede.
- [ ] `AppSession` che tiene lo stato, cosi' il cambio lingua ricostruisce la finestra senza
      buttare il lavoro fatto.
- [ ] `ThreadLog` + `LogBuffer` + `LogCategory` copiati e adattati alle categorie del prodotto.
- [ ] Le funzioni della cornice rimesse a mano, tutte e cinque.
- [ ] `Dialogs.decorate` su ogni dialogo.

**Identita' e packaging**:

- [ ] `pom.xml`, `build-msi.ps1`, `VersionTest`, `Launcher` non-JavaFX.
- [ ] UpgradeCode generato **una volta sola** con lo script qui sopra e congelato in tre posti che
      devono coincidere: `build-msi.ps1`, la tabella "Identita' dei prodotti", `Catalogo.PRODOTTI`.
- [ ] `icon.ico` e `app-icon.png` (`gen-icon.ps1`): 0% liberta' sul disegno, 100% sui colori.
- [ ] Versione a tre campi allineata nei tre posti, verificata da `VersionTest`.

**Aggancio alla linea**:

- [ ] Riga nella tabella "Identita' dei prodotti" (e in "I progetti") di questo file, e
      **`STANDARD.md` riallineato in tutti i repo** (vedi "Riallineare le copie").
- [ ] Riga in `Catalogo.PRODOTTI` di teoSetup con lo **stesso** UpgradeCode: senza, il build del
      wizard fallisce (`CatalogoTest`), ed e' voluto.
- [ ] `versione.json` pubblicato su `/var/www/scarica/<codice>/`, anche solo con
      `disponibile: false`: il manifest deve **esistere** o il wizard non mostra il prodotto.
- [ ] Vhost nginx e record DNS del sottodominio.
- [ ] Sito prodotto in `sito\` (copia di un sito esistente, `prodotto.ts` vero) pubblicato in
      `/var/www/html/<codice>/dist/`.
- [ ] Wizard preselezionato `teoSetup-<codice>-<v>.exe` in `/var/www/scarica/<codice>/`, symlink
      `-ultimo.exe`, blocco `wizard` nel manifest, e teoSetup **ripubblicato** (versione nuova:
      il catalogo e' compilato dentro, un wizard vecchio non conosce il prodotto nuovo). **Vanno
      ripubblicati i preselezionati di TUTTI i prodotti**, con symlink e blocco `wizard`: chi arriva
      dalla pagina di un altro prodotto scarica il suo, e il 2026-11-06 SorterNext apriva ancora un
      wizard 1.0.11 senza rinominaPDF nell'elenco, con il neutro e rinominaPDF gia' alla 1.0.13.
- [ ] Scheda sulla home `teo.place` (`/var/www/html/index.html`), con backup.

**Aggiornamenti**:

- [ ] Pacchetto `update` copiato da listBigStorage, controllo all'avvio e ogni sei ore, pulsante
      «Controlla aggiornamenti» (vedi "Il controllo aggiornamenti dentro l'app").

**Accesso per Claude** (facoltativo, ma se c'e' segue "L'accesso per Claude (MCP)"):

- [ ] Porta propria (la prossima libera) o stdio, interruttore spento di default, token nel
      `CredentialStore`, nessun tool distruttivo, ogni chiamata visibile nel registro.

**Licenza** — e' la voce che si rimanda e non va rimandata, perche' senza si finisce a vendere un
programma identico alla sua copia gratuita:

- [ ] Pacchetto `core/license` copiato (`DeviceId`, `Entitlement`, `LicenseClient`,
      `LicenseException`, `Licensing`) piu' `license/jwt_pubblica.pem` fra le risorse.
- [ ] `LicenseGate` con lo sbarramento all'**avvio** e il bottone nella barra del titolo.
- [ ] Riga in `prodotti` sul database `authenticator` (una migrazione in
      `authenticator-backend/database/`), **con il prezzo deciso**: il codice deve combaciare con
      `AppInfo.PRODUCT_CODE` e con il claim `prd`, o ogni chiave valida viene rifiutata come "di un
      altro programma".
- [ ] Prodotto su Lemon Squeezy con il suo free trial: e' quello che emette le chiavi.

> **Attenzione all'ordine**: accendere lo sbarramento prima che le ultime due voci esistano rende
> il programma **impossibile da aprire**, anche a chi lo ha scritto — non c'e' nessuna licenza che
> possa passare. O si fanno insieme, o lo sbarramento resta spento con un TODO visibile.

Il `CLAUDE.md` del progetto nuovo descrive **solo** il progetto: cos'e', la mappa dei package, i
flussi, i suoi invarianti e le sue trappole. Build, versioni, nomenclatura e packaging **non si
ripetono**: stanno qui.

## Riallineare le copie

Dopo ogni modifica a questo file, in ogni repo della cartella che ha uno `STANDARD.md` (oggi
TraceFox, listBigStorage, watchCLI, queryF5, rinominaPDF, teoSetup, watchHost; fuori cartella
`..\sorter-next` e `..\sorter-barcode`, se ce l'hanno):

```powershell
$testa = "# STANDARD.md — standard comune dei progetti teo.place`r`n`r`n> **Copia.** La fonte e' ``CLAUDE.md`` nella cartella ``teo.place`` che contiene i repo, e chi la`r`n> modifica riallinea tutte le copie. Qui e' duplicata perche' da un clone singolo quella`r`n> cartella non esiste.`r`n"
$corpo = (Get-Content CLAUDE.md -Raw -Encoding UTF8) -replace '(?s)^# CLAUDE\.md.*?\r?\n\r?\n(?=Standard comune)', ''
foreach ($r in Get-ChildItem -Directory | Where-Object { Test-Path "$($_.FullName)\STANDARD.md" }) {
  [IO.File]::WriteAllText("$($r.FullName)\STANDARD.md", $testa + "`r`n" + $corpo, (New-Object Text.UTF8Encoding $false))
}
```

Poi, in teoSetup, `mvn test`: `CatalogoTest` rilegge la tabella "Identita' dei prodotti" dalla
copia e si ferma se un prodotto non e' nel catalogo.
