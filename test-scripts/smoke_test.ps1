param(
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [string]$BaseUrl = "http://localhost:8084",
    [string]$Mode = "manual",
    [string]$CategoryIds = "1,2,3"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
$projectRoot = Split-Path -Parent $PSScriptRoot
$k6 = Get-ChildItem -Path (Join-Path $projectRoot ".tools\k6") -Filter "k6.exe" -Recurse -ErrorAction SilentlyContinue |
    Select-Object -First 1
if (-not $k6) {
    throw "k6 is not installed. Run .\test-scripts\install-k6.ps1 first."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = Join-Path $PSScriptRoot "results\$timestamp\smoke"
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null
$summaryPath = Join-Path $resultDir "summary.json"

& $k6.FullName run `
    --summary-trend-stats "avg,min,med,max,p(90),p(95),p(99)" `
    --summary-export $summaryPath `
    -e "TOKEN=$Token" `
    -e "BASE_URL=$BaseUrl" `
    -e "MODE=$Mode" `
    -e "CATEGORY_IDS=$CategoryIds" `
    -e "VUS=1" `
    -e "ITERATIONS=1" `
    -e "CREATE_P95_MS=10000" `
    -e "TERMINAL_P95_MS=30000" `
    (Join-Path $PSScriptRoot "create_and_poll.js")

Write-Host "`nSmoke result: $summaryPath"
