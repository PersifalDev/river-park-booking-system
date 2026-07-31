param(
    [Parameter(Mandatory = $true)]
    [string]$Token,
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

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$composeFile = Join-Path $projectRoot "infra\docker-compose.yaml"
$envFile = Join-Path $projectRoot "infra\.env"
$testScript = Join-Path $PSScriptRoot "create_booking.js"
$k6 = Get-ChildItem -Path (Join-Path $projectRoot ".tools\k6") -Filter "k6.exe" -Recurse -ErrorAction SilentlyContinue |
    Select-Object -First 1
if (-not $k6) {
    throw "k6 is not installed. Run .\test-scripts\install-k6.ps1 first."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultsRoot = Join-Path $PSScriptRoot "results\$timestamp"
New-Item -ItemType Directory -Path $resultsRoot -Force | Out-Null
$gitRevision = try { (git -C $projectRoot rev-parse HEAD 2>$null).Trim() } catch { "unknown" }
$gitDirty = try { [bool](git -C $projectRoot status --porcelain 2>$null) } catch { $true }
$script:k6Failed = $false
$executedRuns = [System.Collections.Generic.List[object]]::new()

Write-Host "Building services once before the mode comparison"
docker compose --env-file $envFile -f $composeFile build `
    catalog-service payment-service notification-service booking-service
if ($LASTEXITCODE -ne 0) {
    throw "Failed to build services for WORK_MODE comparison"
}

function Wait-Service([string]$BaseUrl, [string]$Name) {
    $deadline = (Get-Date).AddSeconds(150)
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
    throw "$Name did not become healthy within 150 seconds"
}

function Wait-AsyncDrain {
    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        try {
            $bookingMetrics = (Invoke-WebRequest -UseBasicParsing -Uri "$BookingBaseUrl/actuator/prometheus" -TimeoutSec 5).Content
            $paymentMetrics = (Invoke-WebRequest -UseBasicParsing -Uri "$PaymentBaseUrl/actuator/prometheus" -TimeoutSec 5).Content
            $busyBooking = $bookingMetrics -match 'booking_outbox_backlog\{[^}]*status="(new|processing)"[^}]*\}\s+[1-9]'
            $busyTasks = $bookingMetrics -match 'async_booking_task_backlog\{[^}]*status="(new|in_progress|failed_retryable)"[^}]*\}\s+[1-9]'
            $busyPayment = $paymentMetrics -match 'payment_outbox_backlog\{[^}]*status="(new|processing)"[^}]*\}\s+[1-9]'
            if (-not $busyBooking -and -not $busyTasks -and -not $busyPayment) {
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 2
    }
    throw "Async queues did not drain within 120 seconds"
}

function Recreate-Mode([string]$Mode) {
    $env:WORK_MODE = $Mode
    $env:BOOKING_RATE_LIMIT_ENABLED = "false"
    $env:CATALOG_RATE_LIMIT_ENABLED = "false"
    docker compose --env-file $envFile -f $composeFile up -d --no-deps --force-recreate `
        catalog-service payment-service notification-service booking-service
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to recreate services in $Mode mode"
    }
    Wait-Service -BaseUrl "http://localhost:8085" -Name "catalog-service"
    Wait-Service -BaseUrl $PaymentBaseUrl -Name "payment-service"
    Wait-Service -BaseUrl $NotificationBaseUrl -Name "notification-service"
    Wait-Service -BaseUrl $BookingBaseUrl -Name "booking-service"
}

function Invoke-K6([string]$Mode, [int]$Repeat, [bool]$Warmup) {
    $runName = if ($Warmup) { "$Mode-warmup" } else { "$Mode-repeat-$Repeat" }
    $arguments = @(
        "run",
        "--summary-trend-stats", "avg,min,med,max,p(90),p(95),p(99)",
        "-e", "TOKEN=$Token",
        "-e", "BOOKING_BASE_URL=$BookingBaseUrl",
        "-e", "PAYMENT_BASE_URL=$PaymentBaseUrl",
        "-e", "NOTIFICATION_BASE_URL=$NotificationBaseUrl",
        "-e", "WORK_MODE=$Mode",
        "-e", "CATEGORY_IDS=$CategoryIds",
        "-e", "RUN_SEED=$Repeat"
    )
    if ($Warmup) {
        $arguments += @("-e", "ITERATIONS=$WarmupIterations", "-e", "VUS=1")
    } elseif ($TargetRate -gt 0) {
        $arguments += @(
            "-e", "TARGET_RATE=$TargetRate",
            "-e", "DURATION=$Duration",
            "-e", "PRE_ALLOCATED_VUS=$PreAllocatedVus",
            "-e", "MAX_VUS=$MaxVus"
        )
    } elseif ($Iterations -gt 0) {
        $arguments += @("-e", "ITERATIONS=$Iterations", "-e", "VUS=$Vus")
    } else {
        $arguments += @("-e", "DURATION=$Duration", "-e", "VUS=$Vus")
    }

    if (-not $Warmup) {
        $resultDir = Join-Path $resultsRoot "repeat-$Repeat\$Mode"
        New-Item -ItemType Directory -Path $resultDir -Force | Out-Null
        [ordered]@{
            startedAt = (Get-Date).ToUniversalTime().ToString("o")
            gitRevision = $gitRevision
            gitDirty = $gitDirty
            repeat = $Repeat
            mode = $Mode
            targetRate = $TargetRate
            duration = $Duration
            iterations = $Iterations
            vus = $Vus
            preAllocatedVus = $PreAllocatedVus
            maxVus = $MaxVus
            categoryIds = $CategoryIds
            bookingServiceCpus = ${env:BOOKING_SERVICE_CPUS}
            bookingServiceMemoryLimit = ${env:BOOKING_SERVICE_MEMORY_LIMIT}
            javaToolOptions = ${env:JAVA_TOOL_OPTIONS}
        } | ConvertTo-Json -Depth 4 |
            Set-Content -Encoding UTF8 (Join-Path $resultDir "run-manifest.json")
        $arguments += @("--summary-export", (Join-Path $resultDir "summary.json"))
        $executedRuns.Add([ordered]@{
            repeat = $Repeat
            mode = $Mode
            result = "repeat-$Repeat/$Mode/summary.json"
        })
    }
    $arguments += $testScript

    Write-Host "Running $runName"
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & $k6.FullName @arguments
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if (-not $Warmup -and $exitCode -ne 0) {
        $script:k6Failed = $true
        Write-Warning "k6 thresholds failed for $runName; results were preserved"
    }
}

try {
    for ($repeat = 1; $repeat -le $Repeats; $repeat++) {
        $modeOrder = if ($repeat % 2 -eq 0) { @("ASYNC", "SYNC") } else { @("SYNC", "ASYNC") }
        foreach ($mode in $modeOrder) {
            Recreate-Mode -Mode $mode
            if ($WarmupIterations -gt 0) {
                Invoke-K6 -Mode $mode -Repeat $repeat -Warmup $true
                Wait-AsyncDrain
            }
            Invoke-K6 -Mode $mode -Repeat $repeat -Warmup $false
            Wait-AsyncDrain
            if ($CooldownSeconds -gt 0) {
                Start-Sleep -Seconds $CooldownSeconds
            }
        }
    }
} finally {
    $manifest = [ordered]@{
        startedAt = $timestamp
        finishedAt = (Get-Date).ToUniversalTime().ToString("o")
        gitRevision = $gitRevision
        gitDirty = $gitDirty
        comparison = "sync-vs-async-interservice-communication"
        parameters = [ordered]@{
            repeats = $Repeats
            targetRate = $TargetRate
            duration = $Duration
            iterations = $Iterations
            vus = $Vus
            preAllocatedVus = $PreAllocatedVus
            maxVus = $MaxVus
            warmupIterations = $WarmupIterations
            categoryIds = $CategoryIds
        }
        runs = $executedRuns
    }
    $manifest | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 (Join-Path $resultsRoot "run-manifest.json")

    Remove-Item Env:WORK_MODE -ErrorAction SilentlyContinue
    Remove-Item Env:BOOKING_RATE_LIMIT_ENABLED -ErrorAction SilentlyContinue
    Remove-Item Env:CATALOG_RATE_LIMIT_ENABLED -ErrorAction SilentlyContinue
    docker compose --env-file $envFile -f $composeFile up -d --no-deps --force-recreate `
        catalog-service payment-service notification-service booking-service
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Failed to restore default service configuration"
    }
}

& (Join-Path $PSScriptRoot "summarize_results.ps1") -ResultsRoot $resultsRoot
Write-Host "Results: $resultsRoot"
if ($script:k6Failed) {
    exit 99
}
