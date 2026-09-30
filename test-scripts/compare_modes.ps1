param(
    [string]$Token = '',
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
$ErrorActionPreference = 'Stop'
if ($ExternalPlatformPoolSize -ne $ExternalVirtualMaxConcurrency) { throw "Thread comparison requires equal concurrency limits" }
Write-Warning "This entry point now uses the isolated research stand and creates synthetic users; the legacy Token parameter is not used. See test-scripts/research/README.md."
& (Join-Path $PSScriptRoot 'research\run.ps1') -Comparison threads -CategoryIds $CategoryIds -Repeats 1 -TargetRate $TargetRate -Duration $Duration -Iterations $Iterations -Vus $Vus -PreAllocatedVus $PreAllocatedVus -MaxVus $MaxVus -DispatcherPoolSize $DispatcherPoolSize -DispatcherQueueCapacity $DispatcherQueueCapacity -ExternalConcurrency $ExternalPlatformPoolSize -ExternalQueueCapacity $ExternalPlatformQueueCapacity -HikariPoolSize $HikariPoolSize -PollerIntervalMs $PollIntervalMs -PollerBatchSize $PollBatchSize -WarmupIterations ([Math]::Max(1,$WarmupIterations)) -CooldownSeconds $CooldownSeconds -ResultsRoot $ResultsRoot -SeedOffset ($RunSeed - 1) -ReverseFirstOrder:($ModeOrder -eq "virtual-first")
