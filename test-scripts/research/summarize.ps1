param([Parameter(Mandatory = $true)][string]$ResultsRoot)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $ResultsRoot).Path
$measurements = @(
    @{ Metric='booking_response_latency_ms'; Field='p(95)'; Samples='booking_response_samples' },
    @{ Metric='booking_response_latency_ms'; Field='p(99)'; Samples='booking_response_samples' },
    @{ Metric='booking_hold_latency_ms'; Field='p(95)'; Samples='booking_hold_samples' },
    @{ Metric='payment_completion_latency_ms'; Field='p(95)'; Samples='payment_completion_samples' },
    @{ Metric='notification_completion_latency_ms'; Field='p(95)'; Samples='notification_completion_samples' },
    @{ Metric='business_completion_latency_ms'; Field='p(95)'; Samples='business_completion_samples' },
    @{ Metric='business_completion_latency_ms'; Field='p(99)'; Samples='business_completion_samples' },
    @{ Metric='booking_acceptance_rate'; Field='rate' },
    @{ Metric='accepted_completion_rate'; Field='rate' },
    @{ Metric='successful_iterations'; Field='rate' },
    @{ Metric='accepted_bookings'; Field='count' },
    @{ Metric='completed_bookings'; Field='count' },
    @{ Metric='booking_failures_total'; Field='count' },
    @{ Metric='completion_timeouts_total'; Field='count' },
    @{ Metric='cleanup_failures_total'; Field='count' },
    @{ Metric='dropped_iterations'; Field='count' },
    @{ Metric='iterations'; Field='rate' }
)
function Get-Value($Summary,[string]$Name,[string]$Field) {
    $metric = $Summary.metrics.PSObject.Properties[$Name]
    if (-not $metric) { return $null }
    $values = if ($metric.Value.PSObject.Properties['values']) { $metric.Value.values } else { $metric.Value }
    $property = $values.PSObject.Properties[$Field]
    if (-not $property -or $null -eq $property.Value) { return $null }
    $value = [double]$property.Value
    if ([double]::IsNaN($value) -or [double]::IsInfinity($value)) { return $null }
    return $value
}
function Get-Critical95([int]$Df) {
    $table = @(0,12.706,4.303,3.182,2.776,2.571,2.447,2.365,2.306,2.262,2.228,
        2.201,2.179,2.160,2.145,2.131,2.120,2.110,2.101,2.093,2.086,
        2.080,2.074,2.069,2.064,2.060,2.056,2.052,2.048,2.045,2.042)
    if ($Df -le 30) { return $table[$Df] }
    return 1.96
}
function Get-Statistics([double[]]$Values) {
    $sorted = @($Values | Sort-Object)
    $n = $sorted.Count; $mean=$null; $median=$null; $low=$null; $high=$null; $sd=$null
    if ($n -gt 0) {
        $mean = ($sorted | Measure-Object -Average).Average
        $middle = [int][math]::Floor($n / 2)
        $median = if ($n % 2 -eq 1) { $sorted[$middle] } else { ($sorted[$middle-1]+$sorted[$middle])/2 }
    }
    if ($n -gt 1) {
        $sum=0.0
        foreach ($value in $sorted) { $sum += [math]::Pow($value-$mean,2) }
        $sd = [math]::Sqrt($sum/($n-1))
        $margin = (Get-Critical95 ($n-1))*$sd/[math]::Sqrt($n)
        $low=$mean-$margin; $high=$mean+$margin
    }
    return [pscustomobject]@{n=$n; mean=$mean; median=$median; sd=$sd; low=$low; high=$high}
}
function Format-Number($Value) {
    if ($null -eq $Value) { return '—' }
    return ([double]$Value).ToString('0.###',[Globalization.CultureInfo]::InvariantCulture)
}
$rows=@(); $manifests=@()
foreach ($file in Get-ChildItem -LiteralPath $root -Recurse -Filter run-manifest.json) {
    $manifest = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
    if (-not $manifest.runId) { continue }
    $manifests += $manifest
    $configuration=[ordered]@{}
    foreach ($key in @('comparison','targetRate','duration','iterations','vus','preAllocatedVus','maxVus',
        'warmupIterations','baseDate','categoryIds','dispatcherPoolSize','dispatcherQueueCapacity',
        'externalConcurrency','externalQueueCapacity','hikariPoolSize','pollerIntervalMs','pollerBatchSize',
        'completionTimeoutMs','httpTimeoutMs','pollIntervalMs','faultService','faultAfterSeconds','faultDurationSeconds','k6Version')) {
        $configuration[$key] = $manifest.$key
    }
    $configuration.sourceDigest=$manifest.passport.sourceDigest
    $configuration.javaVersion=$manifest.passport.javaVersion
    $configText=ConvertTo-Json -InputObject $configuration -Compress
    $path=Join-Path $file.Directory.FullName 'summary.json'
    $summary=$null
    if (Test-Path -LiteralPath $path) {
        try { $summary=Get-Content -LiteralPath $path -Raw -Encoding UTF8 | ConvertFrom-Json }
        catch { $manifest.valid=$false; $manifest.invalidReason='Unreadable k6 summary: '+$_.Exception.Message }
    }
    foreach ($measurement in $measurements) {
        $value=if ($summary) { Get-Value $summary $measurement.Metric $measurement.Field } else { $null }
        $samples=if ($measurement.Samples -and $summary) { Get-Value $summary $measurement.Samples 'count' } else { $null }
        if ($measurement.Samples -and ($null -eq $samples -or $samples -eq 0)) { $value=$null }
        $rows += [pscustomobject]@{runId=$manifest.runId; repeat=$manifest.repeat; seed=$manifest.seed; label=$manifest.label;
            comparison=$manifest.comparison; configuration=$configText; valid=[bool]$manifest.valid; k6ExitCode=$manifest.k6ExitCode;
            invalidReason=$manifest.invalidReason; metric=$measurement.Metric; field=$measurement.Field; observations=$samples; value=$value}
    }
}
$rows | Export-Csv -Encoding UTF8 -NoTypeInformation -LiteralPath (Join-Path $root 'per-run.csv')
$aggregates=@()
foreach ($group in $rows | Group-Object configuration,label,metric,field) {
    $values=@($group.Group | Where-Object {$_.valid -and $null -ne $_.value} | ForEach-Object {[double]$_.value})
    $stats=Get-Statistics $values
    $first=$group.Group[0]
    $aggregates += [pscustomobject]@{configuration=$first.configuration; label=$first.label; metric=$first.metric; field=$first.field;
        availableRuns=$stats.n; allRuns=$group.Count; invalidRuns=@($group.Group | Where-Object {-not $_.valid}).Count;
        meanOfRunValues=$stats.mean; medianOfRunValues=$stats.median; sampleSd=$stats.sd; ci95Low=$stats.low; ci95High=$stats.high}
}
ConvertTo-Json -InputObject @($aggregates) -Depth 6 | Set-Content -Encoding UTF8 (Join-Path $root 'aggregate-summary.json')
$aggregates | Export-Csv -Encoding UTF8 -NoTypeInformation -LiteralPath (Join-Path $root 'aggregate-summary.csv')
$paired=@()
foreach ($group in $rows | Group-Object configuration,metric,field) {
    $first=$group.Group[0]
    $labels=if ($first.comparison -eq 'threads') {@('platform','virtual')} else {@('SYNC','ASYNC')}
    $differences=@(); $unpaired=0
    foreach ($pair in $group.Group | Group-Object seed) {
        $left=@($pair.Group | Where-Object {$_.label -eq $labels[0] -and $_.valid -and $null -ne $_.value})
        $right=@($pair.Group | Where-Object {$_.label -eq $labels[1] -and $_.valid -and $null -ne $_.value})
        if ($left.Count -eq 1 -and $right.Count -eq 1) { $differences += [double]$right[0].value-[double]$left[0].value }
        else { $unpaired++ }
    }
    $stats=Get-Statistics $differences
    $paired += [pscustomobject]@{configuration=$first.configuration; metric=$first.metric; field=$first.field;
        difference="$($labels[1]) - $($labels[0])"; availablePairs=$stats.n; unavailablePairs=$unpaired;
        meanDifference=$stats.mean; medianDifference=$stats.median; sampleSd=$stats.sd; ci95Low=$stats.low; ci95High=$stats.high}
}
ConvertTo-Json -InputObject @($paired) -Depth 6 | Set-Content -Encoding UTF8 (Join-Path $root 'paired-summary.json')
$paired | Export-Csv -Encoding UTF8 -NoTypeInformation -LiteralPath (Join-Path $root 'paired-summary.csv')
$lines=@('# Результаты исследовательской серии','',
    "Прогонов: $($manifests.Count); технически невалидных: $(@($manifests | Where-Object {-not $_.valid}).Count).",'',
    'Провал порогов k6 сохранён как результат. Невалидные прогоны остаются в per-run.csv, но не включаются в агрегаты.',
    'Наборы параметров агрегируются отдельно. Задержки — только по наблюдённым состояниям; читать вместе с долей успехов и таймаутов.',
    'Интервал относится к среднему значению показателя между повторами, включая среднее p95 отдельных прогонов. Это не объединённый p95 всех запросов.',
    'При n=1 доверительный интервал отсутствует; при n=0 значение отсутствует. Парное сравнение использует одинаковый seed. Для df>30 применяется приближение 1.96.', '')
if (@($manifests | Where-Object {$_.synthetic}).Count -gt 0) {
    $lines = @('# Самопроверка статистики на искусственных данных','', '**Это не измерения River Park и не результаты нагрузочного эксперимента.**','') + $lines
}
if ($manifests.Count -eq 0) { $lines += 'Измерений нет. Проверить ошибку запуска; отсутствие результата не означает успешность.' }
if (Test-Path -LiteralPath (Join-Path $root 'series-status.json')) {
    $seriesStatus=Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $root 'series-status.json') | ConvertFrom-Json
    $lines += @('',"Этап серии: $($seriesStatus.phase); ошибка: $($seriesStatus.error).")
}
$configIndex=0
foreach ($configGroup in $aggregates | Group-Object configuration) {
    $configIndex++
    $lines += @("## Набор параметров $configIndex",'', '```json',$configGroup.Name,'```','',
        '| Вариант | Метрика | Показатель | n | Среднее | Медиана | 95% CI |',
        '|---|---|---|---:|---:|---:|---|')
    foreach ($row in $configGroup.Group) {
        $ci=if ($null -ne $row.ci95Low) {"$(Format-Number $row.ci95Low) … $(Format-Number $row.ci95High)"} else {'—'}
        $lines += "| $($row.label) | $($row.metric) | $($row.field) | $($row.availableRuns) | $(Format-Number $row.meanOfRunValues) | $(Format-Number $row.medianOfRunValues) | $ci |"
    }
    $lines += @('', '| Разность | Метрика | Показатель | Пар | Средняя разность | 95% CI |', '|---|---|---|---:|---:|---|')
    foreach ($row in $paired | Where-Object {$_.configuration -eq $configGroup.Name}) {
        $ci=if ($null -ne $row.ci95Low) {"$(Format-Number $row.ci95Low) … $(Format-Number $row.ci95High)"} else {'—'}
        $lines += "| $($row.difference) | $($row.metric) | $($row.field) | $($row.availablePairs) | $(Format-Number $row.meanDifference) | $ci |"
    }
    $lines += ''
}
$lines += @('## Артефакты','', '- per-run.csv — каждый показатель каждого прогона, включая пропуски и причины невалидности.',
    '- aggregate-summary.json/.csv — агрегаты по одинаковой конфигурации.', '- paired-summary.json/.csv — разности сопоставимых пар.',
    '- repeat-*/<вариант>/RESULTS.md — паспорт конкретного прогона и исходные журналы.')
$lines | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $root 'RESULTS.md')
Write-Host "Summary: $(Join-Path $root 'RESULTS.md')"
