param(
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [Parameter(Mandatory = $true)]
    [string]$CategoryIds,
    [int]$Repeats = 5,
    [int]$TargetRate = 20,
    [string]$Duration = "60s",
    [int]$PreAllocatedVus = 40,
    [int]$MaxVus = 300,
    [int]$CooldownSeconds = 30
)

$ErrorActionPreference = "Stop"
$compareScript = Join-Path $PSScriptRoot "compare_modes.ps1"
$matrixRoot = Join-Path $PSScriptRoot ("results\thread-matrix-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory -Path $matrixRoot -Force | Out-Null
$failed = $false

for ($repeat = 1; $repeat -le $Repeats; $repeat++) {
    $order = if ($repeat % 2 -eq 0) { "virtual-first" } else { "platform-first" }
    Write-Host "Thread comparison repeat $repeat/$Repeats; order=$order"
    & $compareScript `
        -Token $Token `
        -CategoryIds $CategoryIds `
        -TargetRate $TargetRate `
        -Duration $Duration `
        -PreAllocatedVus $PreAllocatedVus `
        -MaxVus $MaxVus `
        -CooldownSeconds $CooldownSeconds `
        -ModeOrder $order `
        -RunSeed $repeat `
        -ResultsRoot (Join-Path $matrixRoot "repeat-$repeat")
    if ($LASTEXITCODE -ne 0) {
        $failed = $true
    }
}

& (Join-Path $PSScriptRoot "summarize_thread_results.ps1") -ResultsRoot $matrixRoot
Write-Host "Thread matrix results: $matrixRoot"
if ($failed) {
    exit 99
}
