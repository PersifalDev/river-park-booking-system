param(
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [int]$Vus = 30,
    [int]$Iterations = 0,
    [string]$Duration = "60s",
    [string]$BaseUrl = "http://localhost:8084",
    [string]$CategoryIds = "1,2,3",
    [int]$DispatcherPoolSize = 16,
    [int]$DispatcherQueueCapacity = 30,
    [int]$ExternalPlatformPoolSize = 32,
    [int]$ExternalPlatformQueueCapacity = 64,
    [int]$ExternalVirtualMaxConcurrency = 32,
    [int]$HikariPoolSize = 16,
    [int]$PollIntervalMs = 500,
    [int]$PollBatchSize = 50,
    [string]$MaxDuration = "10m",
    [int]$WarmupIterations = 1,
    [int]$CooldownSeconds = 30,
    [ValidateSet("platform-first", "virtual-first")]
    [string]$ModeOrder = "platform-first",
    [int]$TargetRate = 0,
    [int]$PreAllocatedVus = 30,
    [int]$MaxVus = 300,
    [int]$RunSeed = 1,
    [string]$ResultsRoot = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
$projectRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $projectRoot "infra\docker-compose.yaml"
$envFile = Join-Path $projectRoot "infra\.env"
$testScript = Join-Path $PSScriptRoot "create_and_poll.js"
$k6 = Get-ChildItem -Path (Join-Path $projectRoot ".tools\k6") -Filter "k6.exe" -Recurse -ErrorAction SilentlyContinue |
    Select-Object -First 1
if (-not $k6) {
    throw "k6 is not installed. Run .\test-scripts\install-k6.ps1 first."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
if ([string]::IsNullOrWhiteSpace($ResultsRoot)) {
    $ResultsRoot = Join-Path $PSScriptRoot "results\$timestamp"
}
$resultsRoot = $ResultsRoot
New-Item -ItemType Directory -Path $resultsRoot -Force | Out-Null
$gitRevision = try { (git -C $projectRoot rev-parse HEAD 2>$null).Trim() } catch { "unknown" }
$gitDirty = try { [bool](git -C $projectRoot status --porcelain 2>$null) } catch { $true }
$manifest = [ordered]@{
    startedAt = (Get-Date).ToUniversalTime().ToString("o")
    gitRevision = $gitRevision
    gitDirty = $gitDirty
    comparison = "platform-vs-virtual-threads"
    modeOrder = $ModeOrder
    parameters = [ordered]@{
        vus = $Vus
        iterations = $Iterations
        duration = $Duration
        targetRate = $TargetRate
        preAllocatedVus = $PreAllocatedVus
        maxVus = $MaxVus
        categoryIds = $CategoryIds
        dispatcherPoolSize = $DispatcherPoolSize
        dispatcherQueueCapacity = $DispatcherQueueCapacity
        externalPlatformPoolSize = $ExternalPlatformPoolSize
        externalPlatformQueueCapacity = $ExternalPlatformQueueCapacity
        externalVirtualMaxConcurrency = $ExternalVirtualMaxConcurrency
        hikariPoolSize = $HikariPoolSize
        pollIntervalMs = $PollIntervalMs
        pollBatchSize = $PollBatchSize
        runSeed = $RunSeed
    }
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $resultsRoot "run-manifest.json")
$script:k6Failed = $false

Write-Host "Building catalog-service and booking-service once before the thread comparison"
docker compose --env-file $envFile -f $composeFile build catalog-service booking-service
if ($LASTEXITCODE -ne 0) {
    throw "Failed to build services for thread comparison"
}

function Wait-BookingService {
    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        try {
            $health = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/actuator/health" -TimeoutSec 3
            if ($health.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "booking-service did not become healthy within 120 seconds"
}

function Wait-CatalogService {
    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        try {
            $health = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8085/actuator/health" -TimeoutSec 3
            if ($health.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "catalog-service did not become healthy within 120 seconds"
}

function Wait-AsyncDrain {
    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        try {
            $metrics = (Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/actuator/prometheus" -TimeoutSec 5).Content
            $busyOutbox = $metrics -match 'booking_outbox_backlog\{[^}]*status="(new|processing)"[^}]*\}\s+[1-9]'
            $busyTasks = $metrics -match 'async_booking_task_backlog\{[^}]*status="(new|in_progress|failed_retryable)"[^}]*\}\s+[1-9]'
            if (-not $busyOutbox -and -not $busyTasks) {
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 2
    }
    throw "Booking task/outbox queues did not drain within 120 seconds"
}

function Run-Mode([string]$Name, [bool]$VirtualThreads) {
    Write-Host "`n=== $Name mode; external HTTP virtual threads: $VirtualThreads ==="
    $env:BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED = $VirtualThreads.ToString().ToLowerInvariant()
    $env:BOOKING_RATE_LIMIT_ENABLED = "false"
    $env:BOOKING_TASK_DISPATCHER_THREAD_POOL_SIZE = $DispatcherPoolSize.ToString()
    $env:BOOKING_TASK_DISPATCHER_QUEUE_CAPACITY = $DispatcherQueueCapacity.ToString()
    $env:BOOKING_EXTERNAL_HTTP_PLATFORM_THREAD_POOL_SIZE = $ExternalPlatformPoolSize.ToString()
    $env:BOOKING_EXTERNAL_HTTP_PLATFORM_QUEUE_CAPACITY = $ExternalPlatformQueueCapacity.ToString()
    $env:BOOKING_EXTERNAL_HTTP_VIRTUAL_MAX_CONCURRENCY = $ExternalVirtualMaxConcurrency.ToString()
    $env:BOOKING_DB_MAX_POOL_SIZE = $HikariPoolSize.ToString()
    $env:TASK_EXEC_POOL_INTERVAL_MS = $PollIntervalMs.ToString()
    $env:TASK_EXEC_POOL_BATCH_SIZE = $PollBatchSize.ToString()

    docker compose --env-file $envFile -f $composeFile up -d --no-deps --force-recreate booking-service
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to recreate booking-service in $Name mode"
    }
    Wait-BookingService
    Wait-AsyncDrain

    if ($WarmupIterations -gt 0) {
        Write-Host "Warming up $Name mode with $WarmupIterations iteration(s)..."
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & $k6.FullName run `
            --summary-trend-stats "avg,min,med,max,p(90),p(95),p(99)" `
            -e "TOKEN=$Token" `
            -e "BASE_URL=$BaseUrl" `
            -e "MODE=$Name-warmup" `
            -e "CATEGORY_IDS=$CategoryIds" `
            -e "VUS=1" `
            -e "ITERATIONS=$WarmupIterations" `
            -e "MAX_DURATION=$MaxDuration" `
            -e "CREATE_P95_MS=10000" `
            -e "TERMINAL_P95_MS=30000" `
            $testScript | Out-Null
        $ErrorActionPreference = $previousErrorActionPreference
        Wait-AsyncDrain
    }

    $modeResultDir = Join-Path $resultsRoot $Name
    New-Item -ItemType Directory -Path $modeResultDir -Force | Out-Null
    [ordered]@{
        startedAt = (Get-Date).ToUniversalTime().ToString("o")
        gitRevision = $gitRevision
        gitDirty = $gitDirty
        mode = $Name
        virtualThreads = $VirtualThreads
        workMode = "ASYNC"
        parameters = $manifest.parameters
    } | ConvertTo-Json -Depth 5 |
        Set-Content -Encoding UTF8 (Join-Path $modeResultDir "run-manifest.json")
    $summaryPath = Join-Path $modeResultDir "summary.json"
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $k6Arguments = @(
        "run",
        "--summary-trend-stats", "avg,min,med,max,p(90),p(95),p(99)",
        "--summary-export", $summaryPath,
        "-e", "TOKEN=$Token",
        "-e", "BASE_URL=$BaseUrl",
        "-e", "MODE=$Name",
        "-e", "CATEGORY_IDS=$CategoryIds",
        "-e", "VUS=$Vus",
        "-e", "RUN_SEED=$RunSeed"
    )
    if ($TargetRate -gt 0) {
        $k6Arguments += @(
            "-e", "TARGET_RATE=$TargetRate",
            "-e", "PRE_ALLOCATED_VUS=$PreAllocatedVus",
            "-e", "MAX_VUS=$MaxVus",
            "-e", "DURATION=$Duration"
        )
    } elseif ($Iterations -gt 0) {
        $k6Arguments += @("-e", "ITERATIONS=$Iterations", "-e", "MAX_DURATION=$MaxDuration")
    } else {
        $k6Arguments += @("-e", "DURATION=$Duration")
    }
    $k6Arguments += $testScript

    & $k6.FullName @k6Arguments
    $k6ExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference

    if ($k6ExitCode -ne 0) {
        $script:k6Failed = $true
        Write-Warning "k6 thresholds failed in $Name mode. Results were preserved."
    }
    Wait-AsyncDrain
}

try {
    $env:WORK_MODE = "ASYNC"
    Write-Host "Preparing catalog-service for load test; public rate limit: disabled"
    $env:CATALOG_RATE_LIMIT_ENABLED = "false"
    docker compose --env-file $envFile -f $composeFile up -d --no-deps --force-recreate catalog-service
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to recreate catalog-service with rate limit disabled"
    }
    Wait-CatalogService

    $modes = if ($ModeOrder -eq "virtual-first") {
        @(
            @{ Name = "virtual"; Virtual = $true },
            @{ Name = "platform"; Virtual = $false }
        )
    } else {
        @(
            @{ Name = "platform"; Virtual = $false },
            @{ Name = "virtual"; Virtual = $true }
        )
    }
    for ($index = 0; $index -lt $modes.Count; $index++) {
        Run-Mode -Name $modes[$index].Name -VirtualThreads $modes[$index].Virtual
        if ($index -lt ($modes.Count - 1) -and $CooldownSeconds -gt 0) {
            Write-Host "Cooling down for $CooldownSeconds second(s)..."
            Start-Sleep -Seconds $CooldownSeconds
        }
    }
    Write-Host "`nResults: $resultsRoot"
} finally {
    Remove-Item Env:BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED -ErrorAction SilentlyContinue
    Remove-Item Env:BOOKING_RATE_LIMIT_ENABLED -ErrorAction SilentlyContinue
    Remove-Item Env:BOOKING_TASK_DISPATCHER_THREAD_POOL_SIZE -ErrorAction SilentlyContinue
    Remove-Item Env:BOOKING_TASK_DISPATCHER_QUEUE_CAPACITY -ErrorAction SilentlyContinue
    Remove-Item Env:BOOKING_EXTERNAL_HTTP_PLATFORM_THREAD_POOL_SIZE -ErrorAction SilentlyContinue
    Remove-Item Env:BOOKING_EXTERNAL_HTTP_PLATFORM_QUEUE_CAPACITY -ErrorAction SilentlyContinue
    Remove-Item Env:BOOKING_EXTERNAL_HTTP_VIRTUAL_MAX_CONCURRENCY -ErrorAction SilentlyContinue
    Remove-Item Env:BOOKING_DB_MAX_POOL_SIZE -ErrorAction SilentlyContinue
    Remove-Item Env:TASK_EXEC_POOL_INTERVAL_MS -ErrorAction SilentlyContinue
    Remove-Item Env:TASK_EXEC_POOL_BATCH_SIZE -ErrorAction SilentlyContinue
    Remove-Item Env:CATALOG_RATE_LIMIT_ENABLED -ErrorAction SilentlyContinue
    Remove-Item Env:WORK_MODE -ErrorAction SilentlyContinue

    Write-Host "Restoring catalog-service default rate limit"
    docker compose --env-file $envFile -f $composeFile up -d --no-deps --force-recreate catalog-service
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Failed to restore catalog-service. Recreate it manually before normal use."
    }

    Write-Host "Restoring booking-service defaults from infra/.env"
    docker compose --env-file $envFile -f $composeFile up -d --no-deps --force-recreate booking-service
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Failed to restore booking-service. Recreate it manually before normal use."
    }
}

if ($script:k6Failed) {
    exit 99
}
