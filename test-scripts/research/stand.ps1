$script:ResearchRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$script:ResearchComposeArgs = @('compose', '--env-file', (Join-Path $script:ResearchRoot 'infra\.env.research.example'),
    '-f', (Join-Path $script:ResearchRoot 'infra\docker-compose.yaml'),
    '-f', (Join-Path $script:ResearchRoot 'infra\docker-compose.research.yaml'), '-p', 'river-park-research')
$script:ResearchServices = @('user-service', 'catalog-service', 'booking-service', 'payment-service',
    'notification-service', 'prometheus', 'grafana')
$script:ResearchUrls = @{ 'user-service' = 'http://localhost:18083'; 'catalog-service' = 'http://localhost:18085';
    'booking-service' = 'http://localhost:18084'; 'payment-service' = 'http://localhost:18087';
    'notification-service' = 'http://localhost:18088' }

function Invoke-ResearchCompose([string[]]$Arguments) {
    $previousPreference=$ErrorActionPreference
    try {
        $ErrorActionPreference='Continue'
        & docker @script:ResearchComposeArgs @Arguments
        if ($LASTEXITCODE -ne 0) { throw "Research Compose failed: $($Arguments -join ' ')" }
    } finally { $ErrorActionPreference=$previousPreference }
}

function Wait-ResearchHealth {
    foreach ($service in $script:ResearchUrls.Keys) {
        $deadline = [DateTime]::UtcNow.AddSeconds(240)
        $ready = $false
        while ([DateTime]::UtcNow -lt $deadline) {
            try {
                $health = Invoke-RestMethod -Uri ($script:ResearchUrls[$service] + '/actuator/health') -TimeoutSec 5
                if ($health.status -eq 'UP') { $ready = $true; break }
            } catch { }
            Start-Sleep -Seconds 2
        }
        if (-not $ready) { throw "Service did not become healthy: $service" }
    }
}

function Invoke-ResearchK6([string]$Executable, [string]$LogFile) {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $Executable run --log-format raw (Join-Path $PSScriptRoot 'flow.js') 2>&1 |
            Tee-Object -FilePath $LogFile | Out-Host
        return $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
}

function Get-ResearchContainer([string]$Service) {
    $id = (Invoke-ResearchCompose -Arguments @('ps', '-q', $Service) | Out-String).Trim()
    if (-not $id) { throw "Research container not found: $Service" }
    $info = (& docker inspect $id | Out-String | ConvertFrom-Json)[0]
    if ($LASTEXITCODE -ne 0 -or $info.Config.Labels.'com.docker.compose.project' -ne 'river-park-research') {
        throw 'Container is outside the research project'
    }
    return $info
}

function Invoke-ResearchSql([string]$Service, [string]$Sql) {
    $info = Get-ResearchContainer $Service
    $containerEnv = @{}
    foreach ($entry in $info.Config.Env) {
        $parts = $entry.Split('=', 2); $containerEnv[$parts[0]] = $parts[1]
    }
    & docker exec $info.Id psql -X -A -t -v ON_ERROR_STOP=1 -U $containerEnv.POSTGRES_USER -d $containerEnv.POSTGRES_DB -c $Sql
    if ($LASTEXITCODE -ne 0) { throw "SQL verification failed in $Service" }
}

function Get-RequiredGauge([string]$Text, [string]$Name, [string]$Status) {
    $escaped = [regex]::Escape($Name)
    $lines = @($Text -split '\r?\n' | Where-Object { $_ -match "^$escaped\{" -and $_ -match ('status="' + $Status + '"') })
    if ($lines.Count -ne 1) { throw "Missing or ambiguous metric: $Name status=$Status" }
    $value = [double]::Parse(($lines[0] -split '\s+')[-1], [Globalization.CultureInfo]::InvariantCulture)
    if ([double]::IsNaN($value) -or [double]::IsInfinity($value) -or $value -lt 0) { throw 'Invalid gauge value' }
    return $value
}

function Get-ResearchSummaryNumber($Summary, [string]$Name, [string]$Field) {
    $metric = $Summary.metrics.PSObject.Properties[$Name]
    if (-not $metric) { throw "Missing summary metric: $Name" }
    $values = if ($metric.Value.PSObject.Properties['values']) { $metric.Value.values } else { $metric.Value }
    $property = $values.PSObject.Properties[$Field]
    if (-not $property -or $null -eq $property.Value) { throw "Missing summary value: $Name/$Field" }
    $value = [double]$property.Value
    if ([double]::IsNaN($value) -or [double]::IsInfinity($value)) { throw 'Invalid summary value' }
    return $value
}

function Wait-ResearchDrain([int]$TimeoutSeconds = 180) {
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $stable = 0; $lastReason = ''
    while ([DateTime]::UtcNow -lt $deadline) {
        try {
            $booking = (Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:18084/actuator/prometheus' -TimeoutSec 5).Content
            $payment = (Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:18087/actuator/prometheus' -TimeoutSec 5).Content
            $pending = 0.0
            foreach ($status in @('new', 'in_progress', 'failed_retryable')) {
                $pending += Get-RequiredGauge $booking 'async_booking_task_backlog' $status
            }
            foreach ($status in @('new', 'processing')) {
                $pending += Get-RequiredGauge $booking 'booking_outbox_backlog' $status
                $pending += Get-RequiredGauge $payment 'payment_outbox_backlog' $status
            }
            $query = [Uri]::EscapeDataString('sum(kafka_consumergroup_lag{topic=~"bookings-topic|payments-topic|notifications-topic"})')
            $lag = Invoke-RestMethod -Uri "http://localhost:19090/api/v1/query?query=$query" -TimeoutSec 5
            if ($lag.status -ne 'success' -or $lag.data.result.Count -ne 1) { throw 'Kafka consumer lag is unavailable' }
            $lagValue = [double]::Parse($lag.data.result[0].value[1], [Globalization.CultureInfo]::InvariantCulture)
            if ([double]::IsNaN($lagValue) -or [double]::IsInfinity($lagValue) -or $lagValue -lt 0) { throw 'Invalid consumer lag' }
            if ($pending -eq 0 -and $lagValue -eq 0) {
                $stable++
                if ($stable -ge 3) { return }
            } else { $stable = 0; $lastReason = "pending=$pending lag=$lagValue" }
        } catch { $stable = 0; $lastReason = $_.Exception.Message }
        Start-Sleep -Seconds 5
    }
    throw "Research queues did not drain: $lastReason"
}

function New-ResearchUsers([int]$Count = 8) {
    $users = @()
    for ($index = 0; $index -lt $Count; $index++) {
        $login = 'research_' + ([Guid]::NewGuid().ToString('N').Substring(0, 20))
        $password = [Guid]::NewGuid().ToString('N')
        $body = @{ login = $login; key = $password; personalDataConsentAccepted = $true; privacyPolicyAccepted = $true } | ConvertTo-Json
        $user = Invoke-RestMethod -Method Post -Uri 'http://localhost:18083/users' -ContentType 'application/json' -Body $body
        $credentials = @{ login = $login; password = $password } | ConvertTo-Json
        $jwt = Invoke-RestMethod -Method Post -Uri 'http://localhost:18083/users/auth' -ContentType 'application/json' -Body $credentials
        if (-not $jwt.accessToken) { throw 'Research authentication did not return an access token' }
        $users += [pscustomobject]@{ id = $user.id; token = $jwt.accessToken }
    }
    return $users
}

function Save-ResearchMetrics([string]$Directory, [string]$Stage) {
    foreach ($service in @('booking-service', 'payment-service', 'notification-service')) {
        (Invoke-WebRequest -UseBasicParsing -Uri ($script:ResearchUrls[$service] + '/actuator/prometheus') -TimeoutSec 10).Content |
            Set-Content -Encoding UTF8 (Join-Path $Directory "$Stage-$service.prom")
    }
}

function Assert-ResearchMonitoring {
    $text=(Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:18084/actuator/prometheus' -TimeoutSec 10).Content
    $buckets=@($text -split '\r?\n' | Where-Object {$_ -match '^http_server_requests_seconds_bucket\{' -and $_ -match 'method="POST"' -and $_ -match 'uri="/booking"'})
    if ($buckets.Count -eq 0) { throw 'POST /booking histogram buckets are missing after warmup' }
    if ($text -notmatch '(?m)^booking_created_to_hold_seconds_count') { throw 'Business HOLD timer is unavailable' }
    $targets=Invoke-RestMethod -Uri 'http://localhost:19090/api/v1/targets' -TimeoutSec 10
    if ($targets.status -ne 'success') { throw 'Prometheus target inventory is unavailable' }
    foreach ($service in $script:ResearchUrls.Keys) {
        $target=@($targets.data.activeTargets | Where-Object {$_.labels.instance -like "$service`:*"})
        if ($target.Count -ne 1 -or $target[0].health -ne 'up') { throw "Prometheus is not scraping $service" }
    }
}

function Get-ResearchPassport([string]$Directory) {
    $containers = @()
    foreach ($service in @('user-service', 'catalog-service', 'booking-service', 'payment-service', 'notification-service',
        'kafka', 'booking-postgres', 'catalog-postgres', 'payment-postgres', 'notification-postgres', 'user-postgres',
        'catalog-service-redis', 'notification-service-redis', 'prometheus', 'grafana', 'kafka-exporter', 'cadvisor')) {
        $info = Get-ResearchContainer $service
        $imageInfo = (& docker image inspect $info.Image | Out-String | ConvertFrom-Json)[0]
        $settings = @($info.Config.Env | Where-Object { $_ -match '^(WORK_MODE|JAVA_TOOL_OPTIONS|BOOKING_EXTERNAL_HTTP_|BOOKING_TASK_|BOOKING_DB_MAX_POOL_SIZE|BOOKING_DB_MIN_IDLE|TASK_EXEC_POOL_|BOOKING_OUTBOX_|PAYMENT_OUTBOX_|BOOKING_RATE_LIMIT_ENABLED|CATALOG_RATE_LIMIT_ENABLED)' })
        $containers += [ordered]@{ service = $service; image = $info.Config.Image; imageId = $info.Image;
            digests = $imageInfo.RepoDigests; nanoCpus = $info.HostConfig.NanoCpus;
            memoryBytes = $info.HostConfig.Memory; memorySwapBytes = $info.HostConfig.MemorySwap;
            pidsLimit = $info.HostConfig.PidsLimit; settings = $settings }
    }
    $dockerInfo = (& docker info --format '{{json .}}' | Out-String | ConvertFrom-Json)
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $java = (Invoke-ResearchCompose -Arguments @('exec', '-T', 'booking-service', 'java', '-version') 2>&1 | Out-String).Trim()
    } finally { $ErrorActionPreference = $previousPreference }
    $tracked = & git -c "safe.directory=$($script:ResearchRoot.Replace('\','/'))" -C $script:ResearchRoot ls-files -co --exclude-standard
    $hashes = @($tracked | Sort-Object -Unique | Where-Object { $_ -match '\.(java|xml|yaml|yml|js|mjs|ps1|sql|md)$|(^|/)Dockerfile$|\.env\.research\.example$' } | ForEach-Object {
        $_ + ':' + (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $script:ResearchRoot $_)).Hash
    })
    $hashFile = Join-Path $Directory 'source-hashes.txt'
    $hashes | Set-Content -Encoding UTF8 -LiteralPath $hashFile
    $digest = (Get-FileHash -Algorithm SHA256 -LiteralPath $hashFile).Hash
    return [ordered]@{ dockerVersion = $dockerInfo.ServerVersion; dockerCpus = $dockerInfo.NCPU;
        dockerMemoryBytes = $dockerInfo.MemTotal; dockerKernel = $dockerInfo.KernelVersion;
        dockerArchitecture = $dockerInfo.Architecture; javaVersion = $java; sourceDigest = $digest;
        workloadHash = (Get-FileHash (Join-Path $PSScriptRoot 'workload.js')).Hash;
        gitRevision = (& git -c "safe.directory=$($script:ResearchRoot.Replace('\','/'))" -C $script:ResearchRoot rev-parse HEAD | Out-String).Trim();
        gitDirty = [bool](& git -c "safe.directory=$($script:ResearchRoot.Replace('\','/'))" -C $script:ResearchRoot status --porcelain);
        containers = $containers }
}

function Repair-ResearchBookings([string]$RunId, [object[]]$Users, [string]$Directory) {
    if ($RunId -notmatch '^[a-zA-Z0-9-]+$') { throw 'Invalid run identifier' }
    $prefix = "research-$RunId-"
    $sql = "SELECT COALESCE(json_agg(json_build_object('id',b.id,'userId',b.user_id,'status',b.status,'key',k.idempotency_key)), '[]'::json) FROM booking b JOIN booking_idempotency_key k ON k.booking_id=b.id WHERE k.idempotency_key LIKE '$prefix%';"
    $bookings = @(Invoke-ResearchSql 'booking-postgres' $sql | Out-String | ConvertFrom-Json)
    ConvertTo-Json -InputObject @($bookings) -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $Directory 'accepted-registry.json')
    foreach ($booking in $bookings) {
        if ($booking.status -in @(1, 2, 3)) {
            $owner = @($Users | Where-Object { $_.id -eq $booking.userId })
            if ($owner.Count -ne 1) { throw 'Cannot reconcile booking owner' }
            Invoke-RestMethod -Method Patch -Uri "http://localhost:18084/booking/$($booking.id)/cancel" -Headers @{ Authorization = 'Bearer ' + $owner[0].token } | Out-Null
        }
    }
    Wait-ResearchDrain
    $remaining = (Invoke-ResearchSql 'booking-postgres' "SELECT count(*) FROM booking b JOIN booking_idempotency_key k ON k.booking_id=b.id WHERE k.idempotency_key LIKE '$prefix%' AND b.status IN (1,2,3);" | Out-String).Trim()
    if ($remaining -ne '0') { throw 'Active research bookings remain after cleanup' }
    $invalid = (Invoke-ResearchSql 'booking-postgres' 'SELECT count(*) FROM booking_inventory WHERE held_units < 0 OR confirmed_units < 0 OR held_units + confirmed_units > total_units;' | Out-String).Trim()
    if ($invalid -ne '0') { throw 'Inventory invariant was violated' }
    $leaked = (Invoke-ResearchSql 'booking-postgres' 'SELECT count(*) FROM booking_inventory WHERE held_units <> 0 OR confirmed_units <> 0;' | Out-String).Trim()
    if ($leaked -ne '0') { throw 'Inventory was not fully released in the isolated dataset' }
    return $bookings.Count
}

function Save-ResearchLogs([string]$Directory, [string]$Since) {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        foreach ($service in @('booking-service','payment-service','notification-service','kafka')) {
            $info = Get-ResearchContainer $service
            & docker logs --since $Since --timestamps $info.Id 2>&1 |
                Set-Content -Encoding UTF8 -LiteralPath (Join-Path $Directory "$service.log")
            if ($LASTEXITCODE -ne 0) { throw "Cannot preserve logs for $service" }
        }
    } finally { $ErrorActionPreference = $previousPreference }
}

function Export-ResearchTimeline([string]$Directory, [string]$StartedAt, [string]$FinishedAt) {
    $queries = [ordered]@{
        task_backlog = 'async_booking_task_backlog'
        outbox_backlog = 'booking_outbox_backlog or payment_outbox_backlog'
        oldest_task_age = 'booking_task_oldest_pending_age_seconds or booking_outbox_oldest_pending_age_seconds'
        kafka_lag = 'kafka_consumergroup_lag{topic=~"bookings-topic|payments-topic|notifications-topic"}'
        http_p95 = 'histogram_quantile(0.95,sum by (le,application)(rate(http_server_requests_seconds_bucket{uri="/booking",method="POST"}[30s])))'
        executor = 'booking_external_http_active_tasks or booking_external_http_waiting_tasks'
        heap = 'jvm_memory_used_bytes{area="heap"}'
        cpu = 'process_cpu_usage'
        hikari_pending = 'hikaricp_connections_pending'
    }
    foreach ($name in $queries.Keys) {
        $query = [Uri]::EscapeDataString($queries[$name])
        $start = [Uri]::EscapeDataString($StartedAt); $end = [Uri]::EscapeDataString($FinishedAt)
        $result = Invoke-RestMethod -Uri "http://localhost:19090/api/v1/query_range?query=$query&start=$start&end=$end&step=5s" -TimeoutSec 20
        if ($result.status -ne 'success') { throw "Cannot export Prometheus query: $name" }
        $result | ConvertTo-Json -Depth 20 | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $Directory "timeline-$name.json")
    }
}

function Start-ResearchFault([string]$Service, [int]$AfterSeconds, [int]$DurationSeconds) {
    $info = Get-ResearchContainer $Service
    return Start-Job -ArgumentList $info.Id,$Service,$AfterSeconds,$DurationSeconds -ScriptBlock {
        param($containerId,$service,$after,$duration)
        $result = [ordered]@{service=$service; requestedAfterSeconds=$after; requestedDurationSeconds=$duration; error=$null}
        $stopped = $false
        try {
            Start-Sleep -Seconds $after
            $info = (& docker inspect $containerId | Out-String | ConvertFrom-Json)[0]
            if ($LASTEXITCODE -ne 0 -or $info.Config.Labels.'com.docker.compose.project' -ne 'river-park-research') {
                throw 'Fault target is outside the research project'
            }
            $result.stopRequestedAt = [DateTime]::UtcNow.ToString('o')
            & docker stop -t 2 $containerId | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'Cannot stop fault target' }
            $stopped = $true; $result.stoppedAt = [DateTime]::UtcNow.ToString('o')
            Start-Sleep -Seconds $duration
        } catch { $result.error = $_.Exception.Message }
        finally {
            if ($stopped) {
                $result.startRequestedAt = [DateTime]::UtcNow.ToString('o')
                & docker start $containerId | Out-Null
                if ($LASTEXITCODE -ne 0) { $result.error = 'Cannot restore fault target' }
                else { $result.startedAt = [DateTime]::UtcNow.ToString('o') }
            }
        }
        return [pscustomobject]$result
    }
}

function Complete-ResearchFault($Job, [string]$Directory, [int]$TimeoutSeconds) {
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ($Job.State -in @('Running','NotStarted') -and [DateTime]::UtcNow -lt $deadline) { Start-Sleep -Seconds 1 }
    if ($Job.State -ne 'Completed') { throw "Fault job did not complete: $($Job.State)" }
    $result = @(Receive-Job -Job $Job -ErrorAction Stop)
    if ($result.Count -ne 1) { throw 'Fault job returned an invalid result' }
    $result[0] | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $Directory 'fault.json')
    if ($result[0].error) { throw $result[0].error }
    Wait-ResearchHealth
    return $result[0]
}

function Write-ResearchRunMarkdown($Manifest, [string]$Directory) {
    $status = if ($Manifest.valid) { 'Измерение сохранено; успешность операции оценивается по метрикам ниже.' } else { 'Невалидный прогон: ' + $Manifest.invalidReason }
    $lines = @('# Результат прогона', '', $status, '',
        "- ID: ``$($Manifest.runId)``; повтор: $($Manifest.repeat); режим: $($Manifest.mode); потоки: $($Manifest.threadType).",
        "- Нагрузка: $($Manifest.targetRate) заявок/с, длительность $($Manifest.duration); seed $($Manifest.seed); базовая дата $($Manifest.baseDate).",
        "- k6 exit code: $($Manifest.k6ExitCode). Нарушение порогов не удаляет прогон из результатов.",
        "- Принято в БД: $($Manifest.acceptedInDatabase); получено подтверждений 201: $($Manifest.acceptedObserved); принято без подтверждения клиенту: $($Manifest.acceptedWithoutConfirmation).",
        "- Начало нагрузки (UTC): $($Manifest.startedAt); завершение: $($Manifest.loadFinishedAt).", '')
    if ($Manifest.faultService -ne 'none') { $lines += "Отказ: $($Manifest.faultService). Фактические интервалы — в fault.json." }
    $lines += @('', 'Артефакты:', '', '- run-manifest.json — параметры, паспорт и статус.',
        '- summary.json, k6.log — метрики и каждая попытка с исходом.',
        '- accepted-registry.json — принятые бронирования, включая потерянные HTTP-ответы.',
        '- before-*.prom, after-*.prom, timeline-*.json — снимки и временные ряды.',
        '- *-service.log, kafka.log — логи контейнеров за прогон.', '',
        'HTTP-ответ и завершение бизнес-процесса измеряются отдельно. Отсутствующая задержка не равна нулю. Итоговая сводка находится в RESULTS.md в корне серии.')
    $lines | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $Directory 'RESULTS.md')
}
