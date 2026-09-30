param([switch]$Pilot, [switch]$ValidateOnly)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'stand.ps1')
if ($ValidateOnly) { Write-Output 'Verification runner and shared functions parsed'; return }
$resultDir=Join-Path $PSScriptRoot ('results\verification-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null
$status=[ordered]@{startedAt=[DateTime]::UtcNow.ToString('o'); passed=$false; phase='docker-engine'; error=$null;
    mavenExitCode=$null; mavenImage='maven:3.9.11-eclipse-temurin-25'; tests=$null; failures=$null; errors=$null; skipped=$null}
try {
    $savedPreference=$ErrorActionPreference
    try {
        $ErrorActionPreference='Continue'
        & docker info --format '{{.ServerVersion}}' 2>&1 | Tee-Object -FilePath (Join-Path $resultDir 'docker-engine.log') | Out-Host
        $engineExit=$LASTEXITCODE
    } finally { $ErrorActionPreference=$savedPreference }
    if ($engineExit -ne 0) { throw 'Docker Engine is unavailable; see docker-engine.log' }
    $status.phase='compose'
    Invoke-ResearchCompose -Arguments @('config','--quiet')
    $status.phase='maven'
    $savedPreference=$ErrorActionPreference
    try {
        $ErrorActionPreference='Continue'
        & docker run --rm --add-host host.docker.internal:host-gateway `
            -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
            -v /var/run/docker.sock:/var/run/docker.sock `
            -v "$($script:ResearchRoot):/workspace" -v river-park-research-maven-cache:/root/.m2 `
            -w /workspace $status.mavenImage mvn -B test 2>&1 |
            Tee-Object -FilePath (Join-Path $resultDir 'maven.log') | Out-Host
        $status.mavenExitCode=$LASTEXITCODE
    } finally { $ErrorActionPreference=$savedPreference }
    if ($status.mavenExitCode -ne 0) { throw 'Maven checks failed; see maven.log' }
    $image=(& docker image inspect $status.mavenImage | Out-String | ConvertFrom-Json)[0]
    $status.mavenImageId=$image.Id; $status.mavenImageDigests=$image.RepoDigests
    $status.tests=0; $status.failures=0; $status.errors=0; $status.skipped=0
    $reports=@(Get-ChildItem -LiteralPath $script:ResearchRoot -Recurse -Filter 'TEST-*.xml' |
        Where-Object {$_.Directory.Name -eq 'surefire-reports' -and $_.LastWriteTimeUtc -ge [DateTime]::Parse($status.startedAt).ToUniversalTime()})
    if ($reports.Count -eq 0) { throw 'Maven produced no current JUnit reports' }
    foreach ($file in $reports) {
        [xml]$report=Get-Content -Raw -Encoding UTF8 -LiteralPath $file.FullName
        foreach ($key in @('tests','failures','errors','skipped')) { $status[$key]+=[int]$report.testsuite.$key }
    }
    if ($Pilot) {
        $status.phase='correctness-SYNC'
        & (Join-Path $PSScriptRoot 'correctness.ps1') -Mode SYNC
        $status.phase='correctness-ASYNC'
        & (Join-Path $PSScriptRoot 'correctness.ps1') -Mode ASYNC -SkipBuild
        $status.phase='pilot-modes'
        & (Join-Path $PSScriptRoot 'run.ps1') -Comparison modes -Repeats 1 -TargetRate 1 -Duration 10s -WarmupIterations 2 -SkipBuild
        if ($LASTEXITCODE -ne 0) { throw 'SYNC/ASYNC pilot failed; preserved results must be examined' }
        $status.phase='pilot-threads'
        & (Join-Path $PSScriptRoot 'run.ps1') -Comparison threads -Repeats 1 -TargetRate 1 -Duration 10s -WarmupIterations 2 -SkipBuild
        if ($LASTEXITCODE -ne 0) { throw 'Thread pilot failed; preserved results must be examined' }
    }
    $status.passed=$true; $status.phase='finished'
} catch { $status.error=$_.Exception.Message; throw }
finally {
    $status.finishedAt=[DateTime]::UtcNow.ToString('o')
    $status | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $resultDir 'verification.json')
    @('# Проверка проекта','',"Статус: $($status.passed); последний этап: $($status.phase).",'',
        "Ошибка: $($status.error)","Maven exit code: $($status.mavenExitCode).",'',
        "Тестов: $($status.tests); failures: $($status.failures); errors: $($status.errors); skipped: $($status.skipped).",'',
        'Пустые значения означают, что этап не завершён. Успешный разбор Compose не подтверждает запуск сервисов.',
        'Логи: docker-engine.log, maven.log. Параметры и статусы: verification.json.',
        'При -Pilot дополнительно выполняются проверки корректности обоих режимов и короткие серии режимов и потоков; их RESULTS.md сохраняются рядом в каталоге results.') |
        Set-Content -Encoding UTF8 -LiteralPath (Join-Path $resultDir 'RESULTS.md')
    Write-Host "Verification artifacts: $resultDir"
}
