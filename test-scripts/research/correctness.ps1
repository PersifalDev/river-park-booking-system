param([ValidateSet('SYNC','ASYNC')][string]$Mode = 'ASYNC', [switch]$SkipBuild, [switch]$ValidateOnly)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'stand.ps1')
if ($ValidateOnly) { Write-Output 'Correctness runner parsed'; return }
$directory = Join-Path $PSScriptRoot ('results\correctness-' + $Mode + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$values = @{}
Get-Content (Join-Path $script:ResearchRoot 'infra\.env.research.example') | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') { $values[$matches[1]] = $matches[2] }
}
$variables = @($values.Keys) + @('RESEARCH_DATASET_ID','TOKENS','BASE_DATE','RUN_ID','CATEGORY_IDS') | Select-Object -Unique
$previous = @{}
foreach ($name in $variables) { $previous[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
$status = [ordered]@{ mode = $Mode; passed = $false; error = $null }
try {
    foreach ($name in $values.Keys) { [Environment]::SetEnvironmentVariable($name,$values[$name],'Process') }
    $env:WORK_MODE = $Mode; $env:RESEARCH_DATASET_ID = [Guid]::NewGuid().ToString('N')
    $status.startedAt = [DateTime]::UtcNow.ToString('o')
    $env:RUN_ID = $env:RESEARCH_DATASET_ID; $env:CATEGORY_IDS = '1'
    $env:BASE_DATE = [DateTime]::UtcNow.AddDays(60).ToString('yyyy-MM-dd')
    if (-not $SkipBuild) { Invoke-ResearchCompose -Arguments (@('build') + @('user-service','catalog-service','booking-service','payment-service','notification-service')) }
    Invoke-ResearchCompose -Arguments @('down','--remove-orphans')
    Invoke-ResearchCompose -Arguments (@('up','-d') + $script:ResearchServices)
    Wait-ResearchHealth
    $status.passport = Get-ResearchPassport $directory
    $users = @(New-ResearchUsers 2)
    $env:TOKENS = ($users | ForEach-Object {$_.token}) -join ','
    $date = ([DateTime]::ParseExact($env:BASE_DATE,'yyyy-MM-dd',[Globalization.CultureInfo]::InvariantCulture)).AddDays(1).ToString('yyyy-MM-dd')
    Invoke-ResearchSql 'booking-postgres' "INSERT INTO booking_inventory(room_category_id,booking_date,total_units,held_units,confirmed_units,created_at,updated_at) VALUES (1,'$date',1,0,0,now(),now()) ON CONFLICT (room_category_id,booking_date) DO NOTHING;" | Out-Null
    $k6 = Get-ChildItem (Join-Path $script:ResearchRoot '.tools\k6') -Filter k6.exe -Recurse | Select-Object -First 1
    if (-not $k6) { throw 'k6 is not installed' }
    $savedPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $k6.FullName run --log-format raw --summary-export (Join-Path $directory 'summary.json') (Join-Path $PSScriptRoot 'correctness.js') 2>&1 |
            Tee-Object -FilePath (Join-Path $directory 'k6.log') | Out-Host
        $exitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $savedPreference }
    $status.k6ExitCode = $exitCode
    Wait-ResearchDrain
    $status.acceptedInDatabase = Repair-ResearchBookings $env:RUN_ID $users $directory
    Save-ResearchMetrics $directory 'after'
    $status.passed = $exitCode -eq 0
    if (-not $status.passed) { throw 'Correctness checks failed' }
} catch { $status.error = $_.Exception.Message; throw }
finally {
    foreach ($name in $variables) { [Environment]::SetEnvironmentVariable($name,$previous[$name],'Process') }
    $status.finishedAt = [DateTime]::UtcNow.ToString('o')
    if ($status.startedAt) {
        try { Save-ResearchLogs $directory $status.startedAt }
        catch { $status.logExportError = $_.Exception.Message }
    }
    $status | ConvertTo-Json -Depth 12 | Set-Content -Encoding UTF8 (Join-Path $directory 'verification.json')
    @('# Проверка корректности', '', "Режим: $Mode", "Успех: $($status.passed)", "Ошибка: $($status.error)", '', 'Подробности: k6.log, summary.json, accepted-registry.json.') |
        Set-Content -Encoding UTF8 (Join-Path $directory 'RESULTS.md')
}
