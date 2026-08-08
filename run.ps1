# Kompiliert alle .java-Dateien nach out/ und startet danach Main.
# Aufruf:  .\run.ps1
# Eigene Map laden:          .\run.ps1 -ProgramArgs maps\arena.txt
# Nur bauen, nicht starten:  .\run.ps1 -NoRun
# Andere Klasse starten:     .\run.ps1 -MainClass network.GameServer

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
