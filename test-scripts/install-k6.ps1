param(
    [string]$Version = "2.0.0"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$installDir = Join-Path $projectRoot ".tools\k6"
$existing = Get-ChildItem -Path $installDir -Filter "k6.exe" -Recurse -ErrorAction SilentlyContinue |
    Select-Object -First 1

if ($existing) {
    & $existing.FullName version
    exit 0
}

$archiveName = "k6-v$Version-windows-amd64.zip"
$downloadUrl = "https://github.com/grafana/k6/releases/download/v$Version/$archiveName"
$archivePath = Join-Path ([System.IO.Path]::GetTempPath()) $archiveName

Write-Host "Downloading k6 v$Version from $downloadUrl"
Invoke-WebRequest -Uri $downloadUrl -OutFile $archivePath
New-Item -ItemType Directory -Path $installDir -Force | Out-Null
Expand-Archive -LiteralPath $archivePath -DestinationPath $installDir -Force
Remove-Item -LiteralPath $archivePath

$k6 = Get-ChildItem -Path $installDir -Filter "k6.exe" -Recurse | Select-Object -First 1
if (-not $k6) {
    throw "k6.exe was not found after extracting $archiveName"
}

Write-Host "k6 installed locally: $($k6.FullName)"
& $k6.FullName version
