param([string]$ResultsRoot = '')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'stand.ps1')
if (-not $ResultsRoot) { $ResultsRoot=Join-Path $PSScriptRoot ('results\self-check-'+(Get-Date -Format 'yyyyMMdd-HHmmss')) }
New-Item -ItemType Directory -Path $ResultsRoot -Force | Out-Null
$checks=@()
function Assert-Condition([bool]$Value,[string]$Message) {
    if (-not $Value) { throw "Self-check failed: $Message" }
}
function Assert-Rejected([scriptblock]$Action,[string]$Message) {
    $rejected=$false
    try { & $Action | Out-Null } catch { $rejected=$true }
    Assert-Condition $rejected $Message
}
Assert-Condition ((Get-RequiredGauge 'async_booking_task_backlog{status="new"} 0' 'async_booking_task_backlog' 'new') -eq 0) 'zero gauge must be accepted'
Assert-Rejected {Get-RequiredGauge '' 'async_booking_task_backlog' 'new'} 'missing gauge must fail'
Assert-Rejected {Get-RequiredGauge 'async_booking_task_backlog{status="new"} NaN' 'async_booking_task_backlog' 'new'} 'NaN must fail'
Assert-Rejected {Get-RequiredGauge "async_booking_task_backlog{status=`"new`"} 0`nasync_booking_task_backlog{status=`"new`",extra=`"x`"} 0" 'async_booking_task_backlog' 'new'} 'duplicate gauges must fail'
$checks+='Required metrics distinguish zero from missing, NaN and ambiguous values.'
function Write-Fixture([string]$Name,[string]$Label,[int]$Seed,[int]$Rate,[double]$Value,[bool]$Valid,[bool]$Flat=$false) {
    $directory=Join-Path $ResultsRoot $Name
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    [ordered]@{runId=$Name; label=$Label; comparison='modes'; seed=$Seed; repeat=$Seed; targetRate=$Rate;
        duration='60s'; baseDate='2026-10-01'; valid=$Valid; k6ExitCode=$(if ($Label -eq 'ASYNC') {99} else {0});
        invalidReason=$(if (-not $Valid) {'synthetic missing drain'} else {$null}); synthetic=$true} |
        ConvertTo-Json | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $directory 'run-manifest.json')
    $latency=@{'p(95)'=$Value; 'p(99)'=$Value+10}
    $samples=@{count=10}
    $zero=@{count=0}
    $metrics=if ($Flat) { @{
        booking_response_latency_ms=$latency; booking_response_samples=$samples;
        business_completion_latency_ms=@{'p(95)'=0;'p(99)'=0}; business_completion_samples=$zero
    }} else { @{
        booking_response_latency_ms=@{values=$latency}; booking_response_samples=@{values=$samples};
        business_completion_latency_ms=@{values=@{'p(95)'=0;'p(99)'=0}}; business_completion_samples=@{values=$zero}
    }}
    @{metrics=$metrics} | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $directory 'summary.json')
}
Write-Fixture 'sync-1' 'SYNC' 1 1 100 $true
Write-Fixture 'sync-2' 'SYNC' 2 1 200 $true $true
Write-Fixture 'async-1' 'ASYNC' 1 1 80 $true
Write-Fixture 'async-2' 'ASYNC' 2 1 160 $true $true
Write-Fixture 'invalid-3' 'SYNC' 3 1 9999 $false
Write-Fixture 'single' 'SYNC' 1 2 40 $true
& (Join-Path $PSScriptRoot 'summarize.ps1') -ResultsRoot $ResultsRoot
$aggregate=Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $ResultsRoot 'aggregate-summary.json') | ConvertFrom-Json
$sync=@($aggregate | Where-Object {$_.label -eq 'SYNC' -and $_.metric -eq 'booking_response_latency_ms' -and $_.field -eq 'p(95)' -and $_.availableRuns -eq 2})
Assert-Condition ($sync.Count -eq 1) 'two valid runs must aggregate together'
Assert-Condition ($sync[0].meanOfRunValues -eq 150 -and $sync[0].invalidRuns -eq 1) 'invalid run must stay out of mean'
Assert-Condition ([math]::Abs($sync[0].ci95High-785.3) -lt 0.01) 't interval for n=2 must use df=1'
$single=@($aggregate | Where-Object {$_.label -eq 'SYNC' -and $_.metric -eq 'booking_response_latency_ms' -and $_.field -eq 'p(95)' -and $_.availableRuns -eq 1})
Assert-Condition ($single.Count -eq 1 -and $single[0].meanOfRunValues -eq 40 -and $null -eq $single[0].ci95Low) 'single run, different rate and absent CI'
$missing=@($aggregate | Where-Object {$_.metric -eq 'business_completion_latency_ms'})
Assert-Condition (@($missing | Where-Object {$null -ne $_.meanOfRunValues -or $_.availableRuns -ne 0}).Count -eq 0) 'zero samples must produce missing latency'
$async=@($aggregate | Where-Object {$_.label -eq 'ASYNC' -and $_.metric -eq 'booking_response_latency_ms' -and $_.field -eq 'p(95)'})
Assert-Condition ($async[0].availableRuns -eq 2) 'k6 threshold failure must be retained'
$paired=Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $ResultsRoot 'paired-summary.json') | ConvertFrom-Json
$difference=@($paired | Where-Object {$_.metric -eq 'booking_response_latency_ms' -and $_.field -eq 'p(95)' -and $_.availablePairs -eq 2})
Assert-Condition ($difference.Count -eq 1 -and $difference[0].meanDifference -eq -30) 'paired differences must match seeds'
$checks+='n=0/n=1/n=2, missing observations, flat/nested summaries, invalid runs, threshold failures, separate configurations and paired differences.'
Assert-Condition (Test-Path -LiteralPath (Join-Path $ResultsRoot 'RESULTS.md')) 'Markdown summary must be saved'
@('# Самопроверка исследовательских скриптов','',
    'Статус: успешно. Использованы искусственные данные; это не эксперимент с приложением River Park.','') +
    @($checks | ForEach-Object {'- '+$_}) | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $ResultsRoot 'SELF-CHECK.md')
Write-Host "Self-check passed: $ResultsRoot"
