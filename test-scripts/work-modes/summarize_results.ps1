param(
    [Parameter(Mandatory = $true)]
    [string]$ResultsRoot
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $ResultsRoot).Path
$measurements = @(
    @{ Metric = "booking_response_latency_ms"; Field = "p(95)"; Label = "response_p95_ms" },
    @{ Metric = "booking_hold_latency_ms"; Field = "p(95)"; Label = "hold_p95_ms" },
    @{ Metric = "notification_completion_latency_ms"; Field = "p(95)"; Label = "notification_p95_ms" },
    @{ Metric = "business_completion_latency_ms"; Field = "p(95)"; Label = "business_completion_p95_ms" },
    @{ Metric = "http_req_failed"; Field = "value"; Label = "http_error_rate" },
    @{ Metric = "successful_iterations"; Field = "value"; Label = "successful_iteration_rate" },
    @{ Metric = "iterations"; Field = "rate"; Label = "iterations_per_second" },
    @{ Metric = "dropped_iterations"; Field = "count"; Label = "dropped_iterations" }
)

function Get-Percentile([double[]]$Values, [double]$Percentile) {
    if ($Values.Count -eq 0) {
        return $null
    }
    $sorted = @($Values | Sort-Object)
    $position = ($sorted.Count - 1) * $Percentile
    $lower = [math]::Floor($position)
    $upper = [math]::Ceiling($position)
    if ($lower -eq $upper) {
        return [double]$sorted[$lower]
    }
    return [double]$sorted[$lower] +
        ($position - $lower) * ([double]$sorted[$upper] - [double]$sorted[$lower])
}

function Get-TCritical95([int]$DegreesOfFreedom) {
    $table = @{
        1 = 12.706; 2 = 4.303; 3 = 3.182; 4 = 2.776; 5 = 2.571
        6 = 2.447; 7 = 2.365; 8 = 2.306; 9 = 2.262; 10 = 2.228
        11 = 2.201; 12 = 2.179; 13 = 2.160; 14 = 2.145; 15 = 2.131
        16 = 2.120; 17 = 2.110; 18 = 2.101; 19 = 2.093; 20 = 2.086
        21 = 2.080; 22 = 2.074; 23 = 2.069; 24 = 2.064; 25 = 2.060
        26 = 2.056; 27 = 2.052; 28 = 2.048; 29 = 2.045; 30 = 2.042
    }
    if ($DegreesOfFreedom -le 0) {
        return 0.0
    }
    if ($DegreesOfFreedom -le 30) {
        return [double]$table[$DegreesOfFreedom]
    }
    return 1.96
}

$rows = [System.Collections.Generic.List[object]]::new()
foreach ($mode in @("SYNC", "ASYNC")) {
    $files = Get-ChildItem -LiteralPath $root -Recurse -Filter "summary.json" |
        Where-Object { $_.Directory.Name -eq $mode }
    foreach ($measurement in $measurements) {
        $values = [System.Collections.Generic.List[double]]::new()
        foreach ($file in $files) {
            $summary = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
            $metric = $summary.metrics.PSObject.Properties[$measurement.Metric]
            if ($null -eq $metric) {
                continue
            }
            $metricValuesProperty = $metric.Value.PSObject.Properties["values"]
            $metricValues = if ($null -eq $metricValuesProperty) {
                $metric.Value
            } else {
                $metricValuesProperty.Value
            }
            $value = $metricValues.PSObject.Properties[$measurement.Field]
            if ($null -ne $value) {
                $values.Add([double]$value.Value)
            }
        }
        if ($values.Count -eq 0) {
            continue
        }

        $mean = ($values | Measure-Object -Average).Average
        $sumSquared = 0.0
        foreach ($value in $values) {
            $sumSquared += [math]::Pow($value - $mean, 2)
        }
        $standardDeviation = if ($values.Count -gt 1) {
            [math]::Sqrt($sumSquared / ($values.Count - 1))
        } else {
            0.0
        }
        $margin = if ($values.Count -gt 1) {
            (Get-TCritical95 ($values.Count - 1)) * $standardDeviation / [math]::Sqrt($values.Count)
        } else {
            0.0
        }

        $rows.Add([pscustomobject]@{
            mode = $mode
            metric = $measurement.Label
            samples = $values.Count
            median = Get-Percentile -Values $values.ToArray() -Percentile 0.5
            mean = $mean
            ci95Low = $mean - $margin
            ci95High = $mean + $margin
            min = ($values | Measure-Object -Minimum).Minimum
            max = ($values | Measure-Object -Maximum).Maximum
        })
    }
}

$rows | ConvertTo-Json -Depth 4 |
    Set-Content -Encoding UTF8 -LiteralPath (Join-Path $root "aggregate-summary.json")
$rows | Export-Csv -NoTypeInformation -Encoding UTF8 -LiteralPath (Join-Path $root "aggregate-summary.csv")
$rows | Format-Table -AutoSize
