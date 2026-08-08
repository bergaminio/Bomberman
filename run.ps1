# Kompiliert alle .java-Dateien nach out/ und startet danach Main.
#
# Fenster, Echtzeit, 2 Spieler:  .\run.ps1
# Drei Spieler:                  .\run.ps1 -ProgramArgs gui,3
# Eigene Map:                    .\run.ps1 -ProgramArgs gui,2,maps\arena.txt
# Konsole, rundenbasiert:        .\run.ps1 -ProgramArgs konsole
# Server:                        .\run.ps1 -ProgramArgs server,5555,2
# Netzwerk im Fenster:           .\run.ps1 -ProgramArgs client,127.0.0.1,5555
# Netzwerk auf der Konsole:      .\run.ps1 -ProgramArgs client-konsole,127.0.0.1,5555
#
# Nur bauen, nicht starten:      .\run.ps1 -NoRun

param(
    [string]$MainClass = "Main",
    [string[]]$ProgramArgs = @(),
    [switch]$NoRun
)

# Immer im Ordner des Skripts arbeiten, egal von wo es aufgerufen wird.
Set-Location $PSScriptRoot

$sources = Get-ChildItem -Path . -Filter *.java -Recurse |
    Where-Object { $_.FullName -notlike "*\out\*" } |
    ForEach-Object { $_.FullName }

if ($sources.Count -eq 0) {
    Write-Host "Keine .java-Dateien gefunden." -ForegroundColor Red
    exit 1
}

Write-Host "Kompiliere $($sources.Count) Dateien ..." -ForegroundColor Cyan
javac -d out $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "Kompilieren fehlgeschlagen." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Kompilieren OK." -ForegroundColor Green

if ($NoRun) { exit 0 }

Write-Host "Starte $MainClass ...`n" -ForegroundColor Cyan
java -cp out $MainClass @ProgramArgs
