$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$smokeRoot = Join-Path $env:RUNNER_TEMP "override-kiosk-windows-smoke"
$receiverData = Join-Path $smokeRoot "receiver"
$emitterData = Join-Path $smokeRoot "emitter"
$port = 18768
$emitterId = "windows-smoke-emitter"
$token = "windows-runtime-migration-token"
$imageId = "windows-smoke-image"
$receiverProcess = $null
$emitterProcess = $null

function Start-KioskProcess([string]$name, [string]$appArguments) {
    $commandPath = Join-Path $smokeRoot "$name.cmd"
    $stdoutPath = Join-Path $smokeRoot "$name.stdout.log"
    $stderrPath = Join-Path $smokeRoot "$name.stderr.log"
    $gradleWrapper = Join-Path $repoRoot "gradlew.bat"
    $command = @"
@echo off
cd /d "$repoRoot"
call "$gradleWrapper" --no-daemon --console=plain :composeApp:run "--args=$appArguments"
"@
    [System.IO.File]::WriteAllText($commandPath, $command, [System.Text.Encoding]::ASCII)
    $commandLine = "/d /c `"$commandPath`""
    return Start-Process `
        -FilePath $env:ComSpec `
        -ArgumentList $commandLine `
        -WorkingDirectory $repoRoot `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath
}

function Stop-KioskProcess($process) {
    if ($null -eq $process) { return }
    $process.Refresh()
    if (-not $process.HasExited) {
        try {
            & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
        } catch {
            Write-Warning "Could not stop process tree $($process.Id): $_"
        }
    }
}

function Wait-ForLog([string]$path, [string]$pattern, [int]$timeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Path $path) {
            $contents = [System.IO.File]::ReadAllText($path)
            if ($contents -match $pattern) { return $contents }
        }
        Start-Sleep -Seconds 2
    }
    $contents = if (Test-Path $path) { [System.IO.File]::ReadAllText($path) } else { "<log not created>" }
    throw "Timed out waiting for '$pattern' in $path.`n$contents"
}

try {
    New-Item -ItemType Directory -Force -Path (Join-Path $receiverData "media") | Out-Null
    New-Item -ItemType Directory -Force -Path $emitterData | Out-Null

    $pairing = @{
        emitterId = $emitterId
        emitterName = "Windows CI"
        token = $token
    } | ConvertTo-Json -Compress
    [System.IO.File]::WriteAllText(
        (Join-Path $receiverData "paired.json"),
        $pairing,
        [System.Text.UTF8Encoding]::new($false)
    )

    $lock = @{
        passwordHash = "windows-ci-password-hash"
        passwordLength = 4
        enabled = $true
        unlockSequence = @("SHIFT", "SHIFT", "CTRL", "CTRL", "ALT", "SHIFT")
        recoveryHash = $null
        recoveryLength = 0
    } | ConvertTo-Json -Compress
    [System.IO.File]::WriteAllText(
        (Join-Path $receiverData "lock.json"),
        $lock,
        [System.Text.UTF8Encoding]::new($false)
    )

    $png = [Convert]::FromBase64String(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nWQAAAAASUVORK5CYII="
    )
    $hash = [Convert]::ToHexString([System.Security.Cryptography.SHA256]::HashData($png)).ToLowerInvariant()
    [System.IO.File]::WriteAllBytes((Join-Path $receiverData "media/$imageId.png"), $png)
    $playlist = @{
        id = "windows-smoke-playlist"
        name = "Windows offline smoke"
        version = 1
        defaultDurationSec = 5
        items = @(@{
            mediaId = $imageId
            fileName = "$imageId.png"
            sha256 = $hash
            durationSec = 5
        })
    } | ConvertTo-Json -Depth 8 -Compress
    [System.IO.File]::WriteAllText(
        (Join-Path $receiverData "playlist.json"),
        $playlist,
        [System.Text.UTF8Encoding]::new($false)
    )

    $receiverProcess = Start-KioskProcess "receiver" "--role=receiver --data-dir=$receiverData --port=$port"
    $receiverInfo = $null
    $serverDeadline = (Get-Date).AddMinutes(3)
    while ((Get-Date) -lt $serverDeadline) {
        $receiverProcess.Refresh()
        if ($receiverProcess.HasExited) {
            throw "Receiver process exited before /info became available (exit $($receiverProcess.ExitCode))."
        }
        try {
            $receiverInfo = Invoke-RestMethod -Uri "http://127.0.0.1:$port/info" -TimeoutSec 2
            break
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    if ($null -eq $receiverInfo) { throw "Receiver did not answer GET /info on port $port." }
    if ($receiverInfo.role -ne "receiver" -or -not $receiverInfo.paired) {
        throw "Receiver returned unexpected info: $($receiverInfo | ConvertTo-Json -Compress)"
    }

    $pairingBackup = Join-Path $receiverData "legacy/paired.json.room-v1.dpapi"
    $lockBackup = Join-Path $receiverData "legacy/lock.json.room-v1.dpapi"
    foreach ($backupPath in @($pairingBackup, $lockBackup)) {
        if (-not (Test-Path $backupPath)) { throw "Expected protected migration backup at $backupPath." }
        $backup = [System.IO.File]::ReadAllText($backupPath)
        if (-not $backup.StartsWith("dpapi:v1:")) { throw "Backup $backupPath does not contain the DPAPI format prefix." }
        if ($backup.Contains($token) -or $backup.Contains("windows-ci-password-hash")) {
            throw "A migration backup contains an unprotected secret: $backupPath"
        }
    }
    if (Test-Path (Join-Path $receiverData "paired.json")) { throw "Legacy paired.json was not removed after migration." }
    if (Test-Path (Join-Path $receiverData "lock.json")) { throw "Legacy lock.json was not removed after migration." }

    $headers = @{ "X-Emitter-Id" = $emitterId; "X-Auth-Token" = $token }
    $playback = $null
    $playbackDeadline = (Get-Date).AddSeconds(45)
    while ((Get-Date) -lt $playbackDeadline) {
        try {
            $playback = Invoke-RestMethod `
                -Uri "http://127.0.0.1:$port/status" `
                -Headers $headers `
                -TimeoutSec 2
            if ($playback.currentMediaId -eq $imageId) { break }
        } catch {
            Start-Sleep -Seconds 2
        }
        Start-Sleep -Seconds 2
    }
    if ($null -eq $playback -or $playback.currentMediaId -ne $imageId) {
        throw "Receiver did not play the migrated local image. Last status: $($playback | ConvertTo-Json -Compress)"
    }

    $receiverLog = Join-Path $receiverData "logs/app.log"
    $receiverLogContents = Wait-ForLog $receiverLog "Windows low-level keyboard hook installed" 30
    if ($receiverLogContents -notmatch "Windows low-level mouse hook installed") {
        throw "Receiver started without the Windows mouse hook."
    }
    if ($receiverLogContents -notmatch "mDNS service registered") {
        throw "Receiver did not register its mDNS service."
    }

    $emitterProcess = Start-KioskProcess "emitter" "--role=emitter --data-dir=$emitterData"
    $emitterLog = Join-Path $emitterData "logs/app.log"
    [void](Wait-ForLog $emitterLog "Discovered screen" 60)

    Write-Host "Windows runtime smoke passed: Receiver /info, DPAPI migration, authenticated local playback, keyboard/mouse hooks, mDNS registration and Controller discovery."
} catch {
    Write-Host "Windows runtime smoke failed: $_"
    foreach ($path in @(
        (Join-Path $receiverData "logs/app.log"),
        (Join-Path $emitterData "logs/app.log"),
        (Join-Path $smokeRoot "receiver.stdout.log"),
        (Join-Path $smokeRoot "receiver.stderr.log"),
        (Join-Path $smokeRoot "emitter.stdout.log"),
        (Join-Path $smokeRoot "emitter.stderr.log")
    )) {
        if (Test-Path $path) {
            Write-Host "--- $path ---"
            Get-Content $path -Tail 100
        }
    }
    throw
} finally {
    Stop-KioskProcess $emitterProcess
    Stop-KioskProcess $receiverProcess
}
