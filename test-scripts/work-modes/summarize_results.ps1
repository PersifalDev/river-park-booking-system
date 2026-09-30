param([Parameter(Mandatory = $true)][string]$ResultsRoot)
& (Join-Path $PSScriptRoot '..\research\summarize.ps1') -ResultsRoot $ResultsRoot
