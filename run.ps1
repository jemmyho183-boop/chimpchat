$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$apiDirectory = Join-Path $root 'chimpchatapi'
$webDirectory = Join-Path $root 'chimpchatweb'
$apiWrapper = Join-Path $apiDirectory 'mvnw.cmd'

foreach ($directory in @($apiDirectory, $webDirectory)) {
    if (-not (Test-Path $directory -PathType Container)) {
        throw "Project directory not found: $directory"
    }
}

if (-not (Test-Path $apiWrapper -PathType Leaf)) {
    throw "Maven wrapper not found: $apiWrapper"
}

foreach ($commandName in @('java', 'npm.cmd', 'ngrok')) {
    if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
        throw "'$commandName' is required and must be available on PATH."
    }
}

$rootEnvFile = Join-Path $root '.env'
if (Test-Path $rootEnvFile -PathType Leaf) {
    foreach ($line in Get-Content $rootEnvFile) {
        if ($line -match '^\s*([^#=\s]+)\s*=\s*(.*?)\s*$') {
            $key = $Matches[1]
            $value = $Matches[2].Trim()
            if ($value.Length -ge 2 -and
                (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            [Environment]::SetEnvironmentVariable($key, $value, 'Process')
        }
    }
}

if ([string]::IsNullOrWhiteSpace($env:NGROK_AUTHTOKEN)) {
    throw "NGROK_AUTHTOKEN is missing. Add it to the root .env file or configure it with ngrok."
}

foreach ($port in @(3000, 8080)) {
    $listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($listener) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        $processName = if ($process) { $process.ProcessName } else { 'unknown process' }
        throw "Port $port is already in use by $processName (PID $($listener.OwningProcess)). Stop the old ChimpChat service window, then run .\run.ps1 again."
    }
}

$npmPath = (Get-Command 'npm.cmd').Source
$ngrokPath = (Get-Command 'ngrok').Source

$apiProcess = Start-Process -FilePath $apiWrapper `
    -ArgumentList 'spring-boot:run' `
    -WorkingDirectory $apiDirectory `
    -PassThru

$frontendProcess = Start-Process -FilePath $npmPath `
    -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0') `
    -WorkingDirectory $webDirectory `
    -PassThru

$ngrokProcess = Start-Process -FilePath $ngrokPath `
    -ArgumentList @('http', '3000', '--url', 'gauntlet-sliver-headfirst.ngrok-free.dev') `
    -WorkingDirectory $root `
    -PassThru

Write-Host 'Started ChimpChat services in separate windows:'
Write-Host "  Frontend and API: http://localhost:3000 (API at /api; backend listens on 8080)"
Write-Host '  ngrok:            https://gauntlet-sliver-headfirst.ngrok-free.dev'
Write-Host 'Close each service window to stop that service.'
Write-Host "Process IDs: API $($apiProcess.Id), frontend $($frontendProcess.Id), ngrok $($ngrokProcess.Id)"
