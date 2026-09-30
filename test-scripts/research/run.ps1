param(
    [ValidateSet('modes', 'threads')][string]$Comparison = 'modes',
    [ValidateSet('SYNC', 'ASYNC')][string]$ReferenceWorkMode = 'ASYNC',
    [int]$Repeats = 5, [int]$TargetRate = 5, [string]$Duration = '60s', [int]$Iterations = 0,
    [int]$Vus = 10, [int]$PreAllocatedVus = 30, [int]$MaxVus = 200,
    [int]$WarmupIterations = 5, [int]$CooldownSeconds = 15, [string]$CategoryIds = '1,2,3',
    [int]$DispatcherPoolSize = 16, [int]$DispatcherQueueCapacity = 30,
    [int]$ExternalConcurrency = 32, [int]$ExternalQueueCapacity = 64, [int]$HikariPoolSize = 16,
    [int]$PollerIntervalMs = 500, [int]$PollerBatchSize = 50,
    [int]$CompletionTimeoutMs = 45000, [int]$HttpTimeoutMs = 15000, [int]$PollIntervalMs = 250,
    [ValidateSet('none','notification-service','payment-service','kafka')][string]$FaultService = 'none',
    [int]$FaultAfterSeconds = 10, [int]$FaultDurationSeconds = 10,
    [string]$ResultsRoot = '', [switch]$SkipBuild, [switch]$ValidateOnly,
    [int]$SeedOffset = 0, [switch]$ReverseFirstOrder
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'stand.ps1')
if ($ValidateOnly) { Write-Output 'Research runner and shared functions parsed'; return }
if ($Repeats -lt 1 -or $TargetRate -lt 0 -or $WarmupIterations -lt 1) { throw 'Invalid series parameters; warmup is required' }
if ($Comparison -eq 'threads' -and $ReferenceWorkMode -ne 'ASYNC') { throw 'This research protocol fixes ASYNC for the separate thread comparison' }
if ($FaultService -ne 'none' -and ($TargetRate -lt 1 -or $Duration -notmatch '^\d+s$' -or
    [int]$Duration.TrimEnd('s') -le $FaultAfterSeconds + $FaultDurationSeconds + 5 -or
    $FaultAfterSeconds -lt 0 -or $FaultDurationSeconds -lt 1)) {
    throw 'Fault scenario needs arrival-rate load lasting beyond the outage and recovery; use seconds, e.g. 60s'
}
$k6 = Get-ChildItem -Path (Join-Path $script:ResearchRoot '.tools\k6') -Filter k6.exe -Recurse | Select-Object -First 1
if (-not $k6) { throw 'Install k6 using test-scripts/install-k6.ps1' }
if (-not $ResultsRoot) { $ResultsRoot = Join-Path $PSScriptRoot ('results\' + $Comparison + '-' + $FaultService + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
if (Test-Path -LiteralPath (Join-Path $ResultsRoot 'series-manifest.json')) { throw 'ResultsRoot already contains a series; choose a new directory' }
New-Item -ItemType Directory -Path $ResultsRoot -Force | Out-Null
$baseDate = [DateTime]::UtcNow.AddDays(30).ToString('yyyy-MM-dd')
$fixtureEnv = @{}
Get-Content -LiteralPath (Join-Path $script:ResearchRoot 'infra\.env.research.example') | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') { $fixtureEnv[$matches[1]] = $matches[2] }
}
$variables = @('WORK_MODE', 'BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED', 'RESEARCH_DATASET_ID',
    'BOOKING_EXTERNAL_HTTP_PLATFORM_THREAD_POOL_SIZE', 'BOOKING_EXTERNAL_HTTP_VIRTUAL_MAX_CONCURRENCY',
    'BOOKING_EXTERNAL_HTTP_PLATFORM_QUEUE_CAPACITY', 'BOOKING_TASK_DISPATCHER_THREAD_POOL_SIZE',
    'BOOKING_TASK_DISPATCHER_QUEUE_CAPACITY', 'BOOKING_DB_MAX_POOL_SIZE', 'TASK_EXEC_POOL_INTERVAL_MS', 'TASK_EXEC_POOL_BATCH_SIZE',
    'TOKENS', 'RUN_ID', 'RUN_SEED', 'BASE_DATE', 'WORK_MODE', 'THREAD_TYPE', 'CATEGORY_IDS', 'TARGET_RATE', 'DURATION',
    'ITERATIONS', 'VUS', 'PRE_ALLOCATED_VUS', 'MAX_VUS', 'SUMMARY_PATH',
    'COMPLETION_TIMEOUT_MS','HTTP_TIMEOUT_MS','POLL_INTERVAL_MS','MAX_DURATION','RESPONSE_P95_MS','COMPLETION_P95_MS','TARIFF_CODE',
    'BOOKING_BASE_URL', 'PAYMENT_BASE_URL', 'NOTIFICATION_BASE_URL') + @($fixtureEnv.Keys) | Select-Object -Unique
$previous = @{}
foreach ($name in $variables) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$runs = @(); $failed = $false
$seriesStatus=[ordered]@{startedAt=[DateTime]::UtcNow.ToString('o'); phase='preflight'; error=$null; allThresholdsPassed=$false}
try {
    foreach ($name in $fixtureEnv.Keys) { [Environment]::SetEnvironmentVariable($name, $fixtureEnv[$name], 'Process') }
    $env:BOOKING_BASE_URL = 'http://localhost:18084'; $env:PAYMENT_BASE_URL = 'http://localhost:18087'
    $env:NOTIFICATION_BASE_URL = 'http://localhost:18088'
    $savedPreference=$ErrorActionPreference
    try {
        $ErrorActionPreference='Continue'
        & docker info --format '{{.ServerVersion}}' 2>&1 | Tee-Object -FilePath (Join-Path $ResultsRoot 'docker-engine.log') | Out-Host
        $engineExit=$LASTEXITCODE
    } finally { $ErrorActionPreference=$savedPreference }
    if ($engineExit -ne 0) { throw 'Docker Engine is unavailable; see docker-engine.log' }
    Invoke-ResearchCompose -Arguments @('config', '--quiet')
    if (-not $SkipBuild) { Invoke-ResearchCompose -Arguments (@('build') + @('user-service', 'catalog-service', 'booking-service', 'payment-service', 'notification-service')) }
    foreach ($name in @('BOOKING_EXTERNAL_HTTP_PLATFORM_THREAD_POOL_SIZE', 'BOOKING_EXTERNAL_HTTP_VIRTUAL_MAX_CONCURRENCY')) {
        [Environment]::SetEnvironmentVariable($name, "$ExternalConcurrency", 'Process')
    }
    $env:BOOKING_EXTERNAL_HTTP_PLATFORM_QUEUE_CAPACITY = "$ExternalQueueCapacity"
    $env:BOOKING_TASK_DISPATCHER_THREAD_POOL_SIZE = "$DispatcherPoolSize"
    $env:BOOKING_TASK_DISPATCHER_QUEUE_CAPACITY = "$DispatcherQueueCapacity"
    $env:BOOKING_DB_MAX_POOL_SIZE = "$HikariPoolSize"
    $env:TASK_EXEC_POOL_INTERVAL_MS = "$PollerIntervalMs"; $env:TASK_EXEC_POOL_BATCH_SIZE = "$PollerBatchSize"
    $env:BASE_DATE = $baseDate; $env:CATEGORY_IDS = $CategoryIds
    $env:DURATION = $Duration; $env:PRE_ALLOCATED_VUS = "$PreAllocatedVus"; $env:MAX_VUS = "$MaxVus"
    $env:COMPLETION_TIMEOUT_MS = "$CompletionTimeoutMs"; $env:HTTP_TIMEOUT_MS = "$HttpTimeoutMs"; $env:POLL_INTERVAL_MS = "$PollIntervalMs"
    $env:MAX_DURATION = '10m'; $env:RESPONSE_P95_MS = '10000'; $env:COMPLETION_P95_MS = "$CompletionTimeoutMs"; $env:TARIFF_CODE = ''
    $seriesStatus.phase='runs'
    for ($repeat = 1; $repeat -le $Repeats; $repeat++) {
        $order = if ($Comparison -eq 'modes') { @('SYNC', 'ASYNC') } else { @('platform', 'virtual') }
        if (($repeat % 2 -eq 0) -xor [bool]$ReverseFirstOrder) { [Array]::Reverse($order) }
        foreach ($label in $order) {
            $runId = [Guid]::NewGuid().ToString('N')
            $directory = Join-Path $ResultsRoot "repeat-$repeat\$label"
            New-Item -ItemType Directory -Path $directory -Force | Out-Null
            $manifest = [ordered]@{ runId = $runId; comparison = $Comparison; repeat = $repeat; label = $label;
                mode = $(if ($Comparison -eq 'modes') { $label } else { $ReferenceWorkMode });
                threadType = $(if ($Comparison -eq 'threads') { $label } else { 'platform' });
                seed = $repeat + $SeedOffset; baseDate = $baseDate; categoryIds = $CategoryIds; targetRate = $TargetRate;
                duration = $Duration; iterations = $Iterations; vus = $Vus; preAllocatedVus = $PreAllocatedVus;
                maxVus = $MaxVus; warmupIterations = $WarmupIterations;
                dispatcherPoolSize = $DispatcherPoolSize; dispatcherQueueCapacity = $DispatcherQueueCapacity;
                externalConcurrency = $ExternalConcurrency; externalQueueCapacity = $ExternalQueueCapacity; hikariPoolSize = $HikariPoolSize;
                pollerIntervalMs = $PollerIntervalMs; pollerBatchSize = $PollerBatchSize; completionTimeoutMs = $CompletionTimeoutMs;
                httpTimeoutMs = $HttpTimeoutMs; pollIntervalMs = $PollIntervalMs; faultService = $FaultService;
                faultAfterSeconds = $FaultAfterSeconds; faultDurationSeconds = $FaultDurationSeconds;
                valid = $false; invalidReason = $null }
            $faultJob = $null
            try {
                # A fresh dataset per run; previous research volumes are retained for inspection.
                Invoke-ResearchCompose -Arguments @('down', '--remove-orphans')
                $env:RESEARCH_DATASET_ID = $runId
                $env:WORK_MODE = $manifest.mode; $env:THREAD_TYPE = $manifest.threadType
                $env:BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED = $(if ($manifest.threadType -eq 'virtual') { 'true' } else { 'false' })
                Invoke-ResearchCompose -Arguments (@('up', '-d') + $script:ResearchServices)
                Wait-ResearchHealth
                $users = @(New-ResearchUsers)
                $env:TOKENS = ($users | ForEach-Object { $_.token }) -join ','
                $manifest.userCount = $users.Count
                $manifest.passport = Get-ResearchPassport $directory
                $manifest.k6Version = (& $k6.FullName version | Out-String).Trim()
                $manifest.datasetId = $runId
                $env:RUN_SEED = "$($manifest.seed)"; $env:RUN_ID = "$runId-warmup"
                $env:ITERATIONS = "$WarmupIterations"; $env:VUS = '1'; $env:TARGET_RATE = '0'
                $env:SUMMARY_PATH = Join-Path $directory 'warmup-summary.json'
                $warmupExit = Invoke-ResearchK6 $k6.FullName (Join-Path $directory 'warmup.log')
                if ($warmupExit -ne 0) { throw 'Warmup did not complete successfully' }
                Wait-ResearchDrain
                $warmupDirectory=Join-Path $directory 'warmup'
                New-Item -ItemType Directory -Path $warmupDirectory -Force | Out-Null
                Repair-ResearchBookings "$runId-warmup" $users $warmupDirectory | Out-Null
                Assert-ResearchMonitoring
                Save-ResearchMetrics $directory 'before'
                $env:RUN_ID = $runId; $env:ITERATIONS = "$Iterations"; $env:VUS = "$Vus"; $env:TARGET_RATE = "$TargetRate"
                $env:SUMMARY_PATH = Join-Path $directory 'summary.json'
                $manifest.startedAt = [DateTime]::UtcNow.ToString('o')
                if ($FaultService -ne 'none') { $faultJob = Start-ResearchFault $FaultService $FaultAfterSeconds $FaultDurationSeconds }
                $manifest.k6ExitCode = Invoke-ResearchK6 $k6.FullName (Join-Path $directory 'k6.log')
                if ($manifest.k6ExitCode -ne 0) { $failed = $true }
                $manifest.loadFinishedAt = [DateTime]::UtcNow.ToString('o')
                if ($faultJob) { $manifest.fault = Complete-ResearchFault $faultJob $directory ($FaultAfterSeconds + $FaultDurationSeconds + 60) }
                Wait-ResearchDrain
                $manifest.drainedAt = [DateTime]::UtcNow.ToString('o')
                $manifest.acceptedInDatabase = Repair-ResearchBookings $runId $users $directory
                Save-ResearchMetrics $directory 'after'
                $manifest.valid = Test-Path -LiteralPath (Join-Path $directory 'summary.json')
                if (-not $manifest.valid) { throw 'k6 did not write its summary' }
                $summary = Get-Content -LiteralPath (Join-Path $directory 'summary.json') -Raw | ConvertFrom-Json
                $manifest.acceptedObserved = [int](Get-ResearchSummaryNumber $summary 'accepted_bookings' 'count')
                $manifest.acceptedWithoutConfirmation = $manifest.acceptedInDatabase - $manifest.acceptedObserved
                if ($manifest.acceptedWithoutConfirmation -lt 0) { throw 'Database reconciliation found fewer bookings than client confirmations' }
                Export-ResearchTimeline $directory $manifest.startedAt ([DateTime]::UtcNow.ToString('o'))
            } catch {
                $manifest.valid = $false; $manifest.invalidReason = $_.Exception.Message; $failed = $true
                Write-Warning "Invalid run ${label}: $($manifest.invalidReason)"
            } finally {
                if ($faultJob) {
                    if ($faultJob.State -in @('Running','NotStarted')) { Stop-Job $faultJob }
                    # Explicit restoration also covers interruption of a PowerShell background job.
                    try {
                        $target = Get-ResearchContainer $FaultService
                        if (-not $target.State.Running) { Invoke-ResearchCompose -Arguments @('start',$FaultService) }
                    } catch { $manifest.valid = $false; $manifest.invalidReason = 'Fault target restoration failed: ' + $_.Exception.Message; $failed = $true }
                    Remove-Job $faultJob -Force
                }
                $manifest.finishedAt = [DateTime]::UtcNow.ToString('o')
                if ($manifest.startedAt) {
                    try { Save-ResearchLogs $directory $manifest.startedAt }
                    catch { $manifest.valid = $false; $manifest.invalidReason = 'Log export failed: ' + $_.Exception.Message; $failed = $true }
                }
                $manifest | ConvertTo-Json -Depth 12 | Set-Content -Encoding UTF8 (Join-Path $directory 'run-manifest.json')
                Write-ResearchRunMarkdown $manifest $directory
                $runs += $manifest
            }
            if (-not $manifest.valid) { throw 'Series stopped because the stand could not be verified; invalid run was preserved.' }
            if ($CooldownSeconds -gt 0) { Start-Sleep -Seconds $CooldownSeconds }
        }
    }
    $seriesStatus.phase='finished'; $seriesStatus.allThresholdsPassed=-not $failed
} catch { $seriesStatus.error=$_.Exception.Message; throw }
finally {
    foreach ($name in $variables) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    ConvertTo-Json -InputObject @($runs) -Depth 12 | Set-Content -Encoding UTF8 (Join-Path $ResultsRoot 'series-manifest.json')
    $seriesStatus.finishedAt=[DateTime]::UtcNow.ToString('o')
    $seriesStatus | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $ResultsRoot 'series-status.json')
    & (Join-Path $PSScriptRoot 'summarize.ps1') -ResultsRoot $ResultsRoot
}
Write-Host "Research artifacts: $ResultsRoot"
if ($failed) { exit 99 }
