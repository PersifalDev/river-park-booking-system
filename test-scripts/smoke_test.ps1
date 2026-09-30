param([string]$Token='', [string]$BaseUrl='', [string]$Mode='manual', [string]$CategoryIds='1,2,3')
$ErrorActionPreference='Stop'
Write-Warning 'Smoke now runs one operation in each mode on the isolated research stand with synthetic users. Legacy Token/BaseUrl/Mode arguments are not used.'
& (Join-Path $PSScriptRoot 'research\run.ps1') -Comparison modes -Repeats 1 -TargetRate 0 -Iterations 1 -Vus 1 -WarmupIterations 1 -CooldownSeconds 0 -CategoryIds $CategoryIds
