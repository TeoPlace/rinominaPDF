# rinominaPDF — costruzione degli installer Windows.
#
# Metodo, nomenclatura e invarianti sono quelli comuni a tutta la cartella: vedi STANDARD.md.
# In breve: fat-jar con maven-shade + jpackage, che include la JVM nel pacchetto; l'utente finale
# NON deve installare ne' Java ne' JavaFX ne' altre librerie.
#
# Requisiti per chi compila: JDK 22 (con jpackage) e Maven nel PATH, piu' WiX Toolset 3.x.
# Uso: powershell -ExecutionPolicy Bypass -File .\build-msi.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# ─── Identita' del prodotto ──────────────────────────────────────────────────────────────────
# 1.0.1: la prima con il controllo aggiornamenti dentro l'app (core/update). Chi ha la 1.0.0
# non ce l'ha, quindi questa la installa a mano; dalla successiva arriva da sola.
# 1.0.2: accetta tutte le famiglie di barcode di SorterNext (12 cifre, ABW/ACW, 17 cifre/ARG),
# non solo 2 lettere + 10 cifre.
# 1.0.3: la finestra a cinque zone dello standard, con storico e registro.
$version     = "1.0.3"
$name        = "rinominaPDF"
$vendor      = "teo.place"
$mainClass   = "app.Launcher"
$description = "Rinomina PDF leggendone i barcode"

# UpgradeCode congelato (vedi STANDARD.md): e' quello con cui la 1.0.0 e' gia' stata distribuita,
# riletto dalla tabella Property dell'MSI. Va tenuto ANCHE ora che il vendor e' diventato
# "teo.place": jpackage lo deriva da "UpgradeCode/<vendor>/<nome>", quindi senza questa riga il
# cambio di vendor avrebbe affiancato un secondo rinominaPDF a chi ha gia' l'attuale.
$upgradeCode = "6E50594E-22A6-3AC3-A7FB-48CFCA119568"

$jar  = "$name-$version-all.jar"
$dest = "dist"

# ─── Comandi nativi ──────────────────────────────────────────────────────────────────────────
# mvn/jpackage non alzano eccezioni: l'unico verdetto e' l'exit code. Dentro la funzione
# ErrorActionPreference torna a "Continue" perche' con "Stop" una qualunque riga su stderr di un
# eseguibile nativo (anche un avviso di JavaFX durante i test) diventerebbe un errore terminante.
function Invoke-Native {
  param([string]$Exe, [string[]]$Arguments, [string]$What)
  $ErrorActionPreference = "Continue"
  & $Exe @Arguments
  if ($LASTEXITCODE -ne 0) { throw "$What fallito (exit $LASTEXITCODE)" }
}

if (-not (Test-Path "icon.ico")) { throw "icon.ico non trovata nella radice del progetto" }

# 1) Fat-jar (VersionTest, dentro i test, verifica che version/pom/AppInfo siano allineati)
Invoke-Native "mvn" @("clean", "package") "mvn package"
if (-not (Test-Path "target\$jar")) { throw "Fat-jar non trovato: target\$jar" }

# 2) Cartella di input con il SOLO fat-jar
if (Test-Path "target\jpackage-input") { Remove-Item "target\jpackage-input" -Recurse -Force }
New-Item -ItemType Directory "target\jpackage-input" | Out-Null
Copy-Item "target\$jar" "target\jpackage-input\"

# 3) Destinazione. jpackage rifiuta di sovrascrivere, quindi si tolgono prima gli output di
#    QUESTA versione — per nome esatto, mai "dist\*".
New-Item -ItemType Directory -Force $dest | Out-Null
foreach ($f in @("$dest\$name-$version.msi", "$dest\$name-$version.exe")) {
  if (Test-Path $f) { Remove-Item $f -Force }
}

$common = @(
  "--input", "target\jpackage-input",
  "--main-jar", $jar,
  "--main-class", $mainClass,
  "--name", $name,
  "--app-version", $version,
  "--vendor", $vendor,
  "--description", $description,
  "--icon", "icon.ico",
  "--dest", $dest,
  "--win-dir-chooser",
  "--win-shortcut",
  "--win-menu"
)

Invoke-Native "jpackage" ($common + @(
  "--type", "msi", "--win-upgrade-uuid", $upgradeCode)) "jpackage (msi)"

# L'exe e' l'MSI incapsulato in un bootstrapper: stesso UpgradeCode, altrimenti installando prima
# l'uno e poi l'altro si finisce con due voci per lo stesso programma.
Invoke-Native "jpackage" ($common + @(
  "--type", "exe", "--win-upgrade-uuid", $upgradeCode)) "jpackage (exe)"

Write-Host ""
Write-Host "Creati in $dest\:" -ForegroundColor Green
Get-ChildItem "$dest\*.msi", "$dest\*.exe" |
  Select-Object Name, @{N = "MB"; E = { [math]::Round($_.Length / 1MB, 1) } }
Write-Host ""
Write-Host "Prima di pubblicare, la prova che conta non e' che il build passi:"
Write-Host "  1. installare la versione precedente, poi questa SOPRA;"
Write-Host "  2. non deve comparire la richiesta di disinstallare prima;"
Write-Host "  3. in 'App installate' deve restare UNA sola voce;"
Write-Host "  4. le impostazioni in %LOCALAPPDATA%\$name devono sopravvivere."
