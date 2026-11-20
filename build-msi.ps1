# rinominaPDF 2.x (Electron + React + TypeScript) - costruzione dell'installer Windows, da questo repo soltanto:
#   dist\rinominaPDF-<v>.msi   la finestra (Electron) + sorter_barcode.dll (lib\sorter-barcode, copia della release pubblica)
#   dist\versione.json         il manifest che teoSetup, il sito e l'app leggono (STANDARD.md, "Il wizard di installazione")
#
# Metodo, nomenclatura e invarianti sono quelli comuni a tutta la cartella: vedi STANDARD.md. Cambia solo il motore:
# niente Maven, niente jpackage e niente JRE dentro, `electron-builder` fa l'MSI (WiX 3.x) con lo STESSO UpgradeCode della
# 1.x (Java), cosi' l'MSI si installa SOPRA la 1.x. La libreria C e' un binario pubblico preso cosi' com'e': non si ricompila.
#
# Requisiti: Node 24, WiX Toolset 3.x, e `npm install` fatto (il kit e' un link a ..\teo-kit).
# Uso: powershell -ExecutionPolicy Bypass -File .\build-msi.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$name = "rinominaPDF"
$pkg = Get-Content package.json -Raw | ConvertFrom-Json
$version = $pkg.version

# UpgradeCode CONGELATO (tabella "Identita' dei prodotti" di STANDARD.md): e' quello della 1.x. Non si cambia.
$upgradeCode = "6E50594E-22A6-3AC3-A7FB-48CFCA119568"
if ((Get-Content electron-builder-config.js -Raw) -notmatch [regex]::Escape($upgradeCode)) {
  throw "electron-builder-config.js non porta l'UpgradeCode congelato $upgradeCode"
}
# La 1.x gia' distribuita dichiara 1.0.5: la 2.0.0 sale di campo maggiore, quindi Windows la tratta come aggiornamento.
if ($version -notmatch '^\d+\.\d+\.\d+$') { throw "La versione deve avere tre campi: $version" }
if (-not (Test-Path "lib\sorter-barcode\sorter_barcode.dll")) { throw "manca lib\sorter-barcode\sorter_barcode.dll (vedi lib\sorter-barcode\LEGGIMI.md)" }

function Invoke-Native {
  param([string]$Exe, [string[]]$Arguments, [string]$What)
  $ErrorActionPreference = "Continue"
  & $Exe @Arguments
  if ($LASTEXITCODE -ne 0) { throw "$What fallito (exit $LASTEXITCODE)" }
}

# 1) I test prima di tutto: un MSI che non passa i test non si costruisce.
Invoke-Native "npx.cmd" @("vitest", "run") "test"
# 2) Tipi: tsc senza emissione.
Invoke-Native "npx.cmd" @("tsc", "--noEmit", "-p", ".") "tsc"
# 3) Processo principale (esbuild) + interfaccia (vite) + MSI.
Invoke-Native "npm.cmd" @("run", "dist") "electron-builder"

# 4) Come gli altri prodotti: l'installer finisce in dist\ con il nome della convenzione, e il manifest accanto.
New-Item -ItemType Directory -Force dist | Out-Null
$msi = "out\$name-$version.msi"
if (-not (Test-Path $msi)) { throw "MSI non trovato: $msi" }
Copy-Item $msi "dist\$name-$version.msi" -Force

# versione.json, SENZA il blocco "wizard": sul server il manifest nuovo lo eredita da quello pubblicato
$i = Get-Item "dist\$name-$version.msi"
$m = [ordered]@{
  disponibile = $true; versione = $version; file = $i.Name
  dimensione  = ('{0:N1} MB' -f ($i.Length / 1MB)); data = (Get-Date -Format 'yyyy-MM-dd')
  sha256      = (Get-FileHash $i.FullName -Algorithm SHA256).Hash.ToLower()
}
[IO.File]::WriteAllText("dist\versione.json", ($m | ConvertTo-Json), (New-Object Text.UTF8Encoding($false)))

Write-Host ""
Write-Host "Creato in dist\:" -ForegroundColor Green
Get-ChildItem "dist\$name-$version.msi", "dist\versione.json" | Select-Object Name, @{N = "MB"; E = { [math]::Round($_.Length / 1MB, 1) } }
Write-Host ""
Write-Host "Prima di pubblicare, la prova che conta non e' che il build passi:"
Write-Host "  1. la versione $version e' maggiore di quella in https://rinominapdf.teo.place/scarica/versione.json (1.0.5);"
Write-Host "  2. installare la 1.x, poi questa SOPRA: non deve chiedere di disinstallare, in 'App installate' UNA sola voce;"
Write-Host "  3. l'elenco in %LOCALAPPDATA%\$name\elaborazioni.json deve sopravvivere e riaprirsi nella barra di sinistra."
Write-Host ""
Write-Host "Non si pubblica niente da qui: manifest e caricamento su KVM2 sono un passo a parte (STANDARD.md)." -ForegroundColor Cyan
