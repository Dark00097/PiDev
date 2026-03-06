Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "[NEXORA] Starting application..." -ForegroundColor Cyan

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "[NEXORA] Maven (mvn) was not found in PATH." -ForegroundColor Red
    Write-Host "Install Maven and reopen PowerShell, then run .\run.ps1 again." -ForegroundColor Yellow
    exit 1
}

# Launch JavaFX app configured in pom.xml (com.nexora.bank.MainApp)
mvn javafx:run
