$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$apiDirectory = Join-Path $root 'chimpchatapi'
$webDirectory = Join-Path $root 'chimpchatweb'
$apiWrapper = Join-Path $apiDirectory 'mvnw.cmd'
$publicBaseUrl = 'https://gauntlet-sliver-headfirst.ngrok-free.dev'
$googleRedirectUri = "$publicBaseUrl/login/oauth2/code/google"
$instagramRedirectUri = "$publicBaseUrl/api/v1/channels/instagram/oauth/callback"

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

# Public OAuth callbacks and email links must use the HTTPS tunnel URL.
$env:FRONTEND_URL = $publicBaseUrl
$env:GOOGLE_REDIRECT_URI = $googleRedirectUri
$env:META_REDIRECT_URI = $instagramRedirectUri
$env:SESSION_COOKIE_SECURE = 'true'
$env:API_PROXY_TARGET = 'http://localhost:8080'

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

Wait-ForService -Url 'http://localhost:8080/api/health' -Process $apiProcess -ServiceName 'ChimpChat API'

$frontendProcess = Start-Process -FilePath $npmPath `
    -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0') `
    -WorkingDirectory $webDirectory `
    -PassThru

Wait-ForService -Url 'http://localhost:3000' -Process $frontendProcess -ServiceName 'ChimpChat frontend'

$ngrokProcess = Start-Process -FilePath $ngrokPath `
    -ArgumentList @('http', '3000', '--url', ([uri]$publicBaseUrl).Host) `
    -WorkingDirectory $root `
    -PassThru

Write-Host 'Started ChimpChat services in separate windows:'
Write-Host "  Public app:       $publicBaseUrl"
Write-Host '  Internal only:    frontend :3000, API :8080'
Write-Host 'Close each service window to stop that service.'
Write-Host "Process IDs: API $($apiProcess.Id), frontend $($frontendProcess.Id), ngrok $($ngrokProcess.Id)"
