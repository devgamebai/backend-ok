# Shared helpers for Windows PowerShell ports of docs/BACKEND_SETUP_GUIDE.md scripts.
# Dot-source from sibling scripts:   . "$PSScriptRoot\_common.ps1"

$script:DefaultStagingHost = '140.99.130.21'
$script:DefaultStagingUser = 'root'
# Password from BACKEND_SETUP_GUIDE.md Section 2 - matches the existing plain-text
# password in the doc. Override via $env:STAGING_SSH_PASSWORD whenever possible.
$script:DefaultStagingPassword = 'lB9m6E127uC7_4t4HY38x2'
# SSH host key fingerprint -- pinned so plink -batch (used in tunnel + sync
# scripts) never blocks on the "store host key?" prompt. Verify out-of-band
# if this is the first time you connect, then update if the server is rekeyed.
$script:DefaultStagingHostKey = 'SHA256:6KA2hqbIlYNS8t03bUf5jOY0PmhZ2li6e0T2O42ZiT0'

# Read a single key from a dotenv-style file. Returns $null if missing.
# Handles `KEY=value`, `KEY='single'`, `KEY="double"`, ignores comment + blank
# lines. Does NOT do shell-style variable expansion.
function Read-DotEnvKey {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$Path,
        [Parameter(Mandatory)] [string]$Key
    )
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    foreach ($line in (Get-Content -LiteralPath $Path -ErrorAction SilentlyContinue)) {
        $trim = $line.Trim()
        if ($trim -eq '' -or $trim.StartsWith('#')) { continue }
        $eq = $trim.IndexOf('=')
        if ($eq -lt 1) { continue }
        $k = $trim.Substring(0, $eq).Trim()
        if ($k -ne $Key) { continue }
        $v = $trim.Substring($eq + 1).Trim()
        if ($v.Length -ge 2 -and (
                ($v.StartsWith('"') -and $v.EndsWith('"')) -or
                ($v.StartsWith("'") -and $v.EndsWith("'")))) {
            $v = $v.Substring(1, $v.Length - 2)
        }
        return $v
    }
    return $null
}

# Lookup order:
#   1. -Override parameter (explicit caller arg)
#   2. $env:STAGING_SSH_PASSWORD (per-session env var)
#   3. STAGING_SSH_PASSWORD= line in <repo>/.env (gitignored)
#   4. $script:DefaultStagingPassword (BACKEND_SETUP_GUIDE.md baseline -- often stale)
function Get-StagingPassword {
    [CmdletBinding()]
    param([string]$Override)
    if ($Override) { return $Override }
    if ($env:STAGING_SSH_PASSWORD) { return $env:STAGING_SSH_PASSWORD }
    $envFile = Join-Path $PSScriptRoot '..\..\.env'
    $fromFile = Read-DotEnvKey -Path $envFile -Key 'STAGING_SSH_PASSWORD'
    if ($fromFile) { return $fromFile }
    return $script:DefaultStagingPassword
}

function Test-PlinkAvailable {
    $null -ne (Get-Command plink.exe -ErrorAction SilentlyContinue)
}

function Test-PscpAvailable {
    $null -ne (Get-Command pscp.exe -ErrorAction SilentlyContinue)
}

function Assert-PuttyTools {
    if (-not (Test-PlinkAvailable) -or -not (Test-PscpAvailable)) {
        Write-Host ""
        Write-Host "ERROR: plink.exe and/or pscp.exe not found in PATH." -ForegroundColor Red
        Write-Host "These are required for password-based SSH on Windows (sshpass equivalent)." -ForegroundColor Red
        Write-Host ""
        Write-Host "Install via one of:" -ForegroundColor Yellow
        Write-Host "  scoop install putty"
        Write-Host "  choco install putty"
        Write-Host "  https://www.chiark.greenend.org.uk/~sgtatham/putty/"
        Write-Host ""
        Write-Host "Alternative: set up SSH key auth on the staging server, then" -ForegroundColor Yellow
        Write-Host "this script will skip plink and use OpenSSH ssh.exe directly." -ForegroundColor Yellow
        throw "PuTTY tools missing"
    }
}

# Run a command on the staging server. Uses plink with -pw (password auth).
# Returns combined stdout+stderr; throws on non-zero exit.
function Invoke-StagingSsh {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$Command,
        [string]$StagingHost = $script:DefaultStagingHost,
        [string]$StagingUser = $script:DefaultStagingUser,
        [string]$Password
    )
    Assert-PuttyTools
    $pw = Get-StagingPassword -Override $Password
    # -batch: never prompt (fail instead). -ssh: force SSH protocol.
    # -hostkey: pin server fingerprint so batch mode doesn't bail on first connect.
    # The trailing arg is the remote command, run via the user's login shell.
    $output = & plink.exe -ssh -batch -pw $pw -hostkey $script:DefaultStagingHostKey "$StagingUser@$StagingHost" $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Remote command failed (exit $LASTEXITCODE): $Command`n$output"
    }
    return $output
}

# Copy a file FROM staging TO local. Uses pscp with -pw.
function Copy-FromStaging {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string]$RemotePath,
        [Parameter(Mandatory)] [string]$LocalPath,
        [string]$StagingHost = $script:DefaultStagingHost,
        [string]$StagingUser = $script:DefaultStagingUser,
        [string]$Password
    )
    Assert-PuttyTools
    $pw = Get-StagingPassword -Override $Password
    & pscp.exe -batch -pw $pw -hostkey $script:DefaultStagingHostKey "$StagingUser@${StagingHost}:$RemotePath" $LocalPath
    if ($LASTEXITCODE -ne 0) {
        throw "pscp download failed (exit $LASTEXITCODE): $RemotePath -> $LocalPath"
    }
}

function Test-DockerContainer {
    [CmdletBinding()]
    param([Parameter(Mandatory)] [string]$Name)
    $null = & docker inspect $Name 2>$null
    return ($LASTEXITCODE -eq 0)
}

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "    OK  $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "    !!  $Message" -ForegroundColor Yellow
}
