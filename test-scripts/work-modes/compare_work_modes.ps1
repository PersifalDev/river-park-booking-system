param(
    [string]$Token = '',
    [Parameter(Mandatory = $true)]
    [string]$CategoryIds,
    [int]$Repeats = 5,
    [int]$TargetRate = 10,
    [string]$Duration = "60s",
    [int]$Iterations = 0,
    [int]$Vus = 10,
    [int]$PreAllocatedVus = 30,
    [int]$MaxVus = 200,
    [int]$WarmupIterations = 3,
    [int]$CooldownSeconds = 20,
    [string]$BookingBaseUrl = "http://localhost:8084",
    [string]$PaymentBaseUrl = "http://localhost:8087",
    [string]$NotificationBaseUrl = "http://localhost:8088"
)
$ErrorActionPreference = 'Stop'
Write-Warning "This entry point now uses the isolated research stand and creates synthetic users; the legacy Token parameter is not used. See test-scripts/research/README.md."
& (Join-Path $PSScriptRoot '..\research\run.ps1') -Comparison modes -CategoryIds $CategoryIds -Repeats $Repeats -TargetRate $TargetRate -Duration $Duration -Iterations $Iterations -Vus $Vus -PreAllocatedVus $PreAllocatedVus -MaxVus $MaxVus -WarmupIterations ([Math]::Max(1,$WarmupIterations)) -CooldownSeconds $CooldownSeconds
