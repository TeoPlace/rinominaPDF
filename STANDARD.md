# STANDARD.md — standard comune dei progetti teo.place

> **Copia.** La fonte e' `CLAUDE.md` nella cartella `projects` che contiene i cinque repo, e
> chi la modifica riallinea le cinque copie. Qui e' duplicata perche' da un clone singolo
> quella cartella non esiste.

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
| **rinominaPDF** | rinomina PDF leggendo i barcode | attivo |
| **sorter-fx** | predecessore di rinominaPDF, cartelle versionate a mano (`1.0.0` … `2.0.4`) | **archivio in SOLA LETTURA** — si consulta, non si modifica |

## Regole di lavoro

1. **Niente commit e niente push**: li fa l'utente. Vale per ogni progetto.
2. **Un progetto per volta.** Quando un progetto ne cita un altro come *riferimento*, quell'altro e'
   **in sola lettura**: si legge per copiarne il metodo, non lo si modifica per far funzionare il
   primo. Le catene attuali: rinominaPDF e watchCLI guardano queryF5; TraceFox e watchCLI guardano
   listBigStorage per la UI; rinominaPDF guarda sorter-fx.
3. **Le cartelle di test dell'utente sono in sola lettura** (i `case-input` / `case-output` citati
   nei singoli `CLAUDE.md`): sono il metro di paragone, se le si modifica non misurano piu' niente.

## Toolchain

- **JDK 22** (`maven.compiler.source`/`target` = 22), Maven 3.8+, entrambi nel PATH.
- **WiX Toolset 3.x** installato: serve a `jpackage --type msi`.
- **JavaFX 22.0.2** + **AtlantaFX 2.0.1** per i temi (dove c'e' una UI).
- **JUnit 5.10.2** + surefire per i test.

Eccezione nota: queryF5 compila a 21 per ragioni storiche; nuovi progetti partono da 22.

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
| rinominaPDF | `rinominapdf` | `app.Launcher` | `6E50594E-22A6-3AC3-A7FB-48CFCA119568` |

**Vendor: `teo.place` per tutti.** Prima era `teo.place` / `TeoPlace` / `Integraa` a seconda del
progetto; e' stato unificato perche' e' la riga "Editore" in *App installate* e tre editori diversi
per lo stesso autore si leggono come tre fornitori diversi.

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

> **listBigStorage**: la versione e' passata da `1.0` a `1.0.0` per rispettare la forma, ma per
> Windows i due numeri sono **lo stesso**. Il prossimo pacchetto pubblicato dev'essere almeno
> **1.0.1**, altrimenti non aggiornera' chi ha gia' la `1.0` installata.

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
  nell'MSI**: verificato aprendo la tabella `Property` dei pacchetti di TraceFox e watchCLI, che
  quell'opzione la passavano da sempre — nessun `ARPURLINFOABOUT`, in nessuno dei due. Non e'
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

  **queryF5 oggi non la rispetta**: cifra le password dei profili DB con AES-128/ECB e una
  passphrase scritta nel sorgente, con fallback in chiaro. E' registrato come debito nel suo
  `CLAUDE.md` — e' l'eccezione da sanare, non un secondo modello valido.

### UI JavaFX

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

1. installare la versione precedente, poi questa **sopra**;
2. **non** deve comparire la richiesta di disinstallare prima;
3. in *App installate* deve restare **una sola** voce;
4. lo stato in `%LOCALAPPDATA%\<Nome>` deve sopravvivere.

Se al punto 2 l'installer chiede di disinstallare, la versione non e' cresciuta abbastanza. Se al
punto 3 le voci sono due, l'UpgradeCode e' cambiato.

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

Il `CLAUDE.md` del progetto nuovo descrive **solo** il progetto: cos'e', la mappa dei package, i
flussi, i suoi invarianti e le sue trappole. Build, versioni, nomenclatura e packaging **non si
ripetono**: stanno qui.
