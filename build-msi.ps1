# Crea l'installer MSI di rinominaPDF (stesso metodo di queryF5):
# fat-jar con maven-shade + jpackage, che include la JVM nel pacchetto.
# L'utente finale NON deve installare ne' Java ne' JavaFX.
#
# Requisiti per chi compila: JDK 22 (con jpackage) e Maven nel PATH,
# piu' WiX Toolset 3.x per il tipo msi.

$ErrorActionPreference = "Stop"
$version = "1.0.0"
$jar = "rinominaPDF-$version-all.jar"

mvn package

if (Test-Path target\jpackage-input) {
  Remove-Item target\jpackage-input -Recurse -Force
}

New-Item -ItemType Directory target\jpackage-input | Out-Null
Copy-Item "target\$jar" target\jpackage-input\

jpackage `
  --input target\jpackage-input `
  --main-jar $jar `
  --main-class app.Launcher `
  --type msi `
  --name rinominaPDF `
  --app-version $version `
  --vendor TeoPlace `
  --icon icon.ico `
  --win-dir-chooser `
  --win-shortcut `
  --win-menu

Write-Host "Creato rinominaPDF-$version.msi nella cartella corrente."
