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

$frontendPort = if ([string]::IsNullOrWhiteSpace($env:FRONTEND_PORT)) { 3000 } else { [int]$env:FRONTEND_PORT }
$apiPort = if ([string]::IsNullOrWhiteSpace($env:API_PORT)) { 8080 } else { [int]$env:API_PORT }
if ([string]::IsNullOrWhiteSpace($env:NGROK_DOMAIN)) {
    throw "NGROK_DOMAIN is missing. Set it in the root .env file."
}
if ([string]::IsNullOrWhiteSpace($env:LOCAL_FRONTEND_URL)) {
    $env:LOCAL_FRONTEND_URL = "http://localhost:$frontendPort"
}
if ([string]::IsNullOrWhiteSpace($env:LOCAL_API_URL)) {
    $env:LOCAL_API_URL = "http://localhost:$apiPort"
}
$publicBaseUrl = "https://$($env:NGROK_DOMAIN)"

# Public OAuth callbacks and email links must use the HTTPS tunnel URL.
$env:FRONTEND_URL = $publicBaseUrl
$env:SESSION_COOKIE_SECURE = 'true'
$env:FRONTEND_PORT = "$frontendPort"
$env:API_PORT = "$apiPort"

foreach ($port in @($frontendPort, $apiPort)) {
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

function Wait-ForService {
    param(
        [string]$Url,
        [System.Diagnostics.Process]$Process,
        [string]$ServiceName
    )

    $timeout = [DateTime]::UtcNow.AddMinutes(2)
    while ([DateTime]::UtcNow -lt $timeout) {
        if ($Process.HasExited) {
            throw "$ServiceName stopped before becoming ready (exit code $($Process.ExitCode))."
        }

        try {
            $null = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "$ServiceName did not become ready at $Url within 2 minutes."
}

Wait-ForService -Url "$($env:LOCAL_API_URL)/api/health" -Process $apiProcess -ServiceName 'ChimpChat API'

$frontendProcess = Start-Process -FilePath $npmPath `
    -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0') `
    -WorkingDirectory $webDirectory `
    -PassThru

Wait-ForService -Url $env:LOCAL_FRONTEND_URL -Process $frontendProcess -ServiceName 'ChimpChat frontend'

$ngrokProcess = Start-Process -FilePath $ngrokPath `
    -ArgumentList @('http', "$frontendPort", '--url', $env:NGROK_DOMAIN) `
    -WorkingDirectory $root `
    -PassThru

Write-Host 'Started ChimpChat services in separate windows:'
Write-Host "  Public app:       $publicBaseUrl"
Write-Host "  Internal only:    frontend :$frontendPort, API :$apiPort"
Write-Host 'Close each service window to stop that service.'
Write-Host "Process IDs: API $($apiProcess.Id), frontend $($frontendProcess.Id), ngrok $($ngrokProcess.Id)"
