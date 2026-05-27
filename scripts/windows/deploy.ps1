<#
.SYNOPSIS
    Windows PowerShell deploy wrapper for SunwinKR.
    Native PowerShell port of the build/setup phase, then delegates infra
    orchestration to the repo-root deploy.sh via Git Bash.

.DESCRIPTION
    Why this exists (vs. running deploy.sh directly):
      1. Git for Windows checks out *.sh with CRLF (core.autocrlf=true).
         Inside the JDK 8 / .NET 5 build containers that breaks
         `#!/bin/sh` shebangs ("/usr/bin/env: 'sh\r': No such file").
         We strip CRLF on gradlew + *.sh before bind-mounting.
      2. The repo-root deploy.sh is 700+ lines (env gen, healthcheck loops,
         docker compose orchestration, init SQL, fix permissions, ...).
         Porting it to PowerShell would create a second source of truth
         that would drift. So the build runs natively in PS, then we
         hand the rest to deploy.sh via bash.

.PARAMETER Rebuild
    Force-rebuild Java + .NET artifacts even if the output JAR/DLL exists.

.PARAMETER NoStart
    Pass --no-start through to deploy.sh: build + setup only, do not bring
    services up.

.PARAMETER RestArgs
    Extra args passed to deploy.sh. Subcommands `stop|status|logs [svc]`
    short-circuit the build phase entirely.

.EXAMPLE
    .\deploy.ps1
    .\deploy.ps1 -Rebuild
    .\deploy.ps1 -NoStart
    .\deploy.ps1 stop
    .\deploy.ps1 status
    .\deploy.ps1 logs portal-api
#>
[CmdletBinding()]
param(
    [switch]$Rebuild,
    [switch]$RebuildBanCa,
    [switch]$NoStart,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$RestArgs
)

$ErrorActionPreference = 'Stop'

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $RepoRoot

function Find-BashExe {
    $candidates = @(
        "$env:ProgramFiles\Git\bin\bash.exe",
        "$env:ProgramFiles\Git\usr\bin\bash.exe",
        "${env:ProgramFiles(x86)}\Git\bin\bash.exe"
    )
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    $cmd = Get-Command bash.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Invoke-DeploySh {
    param([string[]]$Arguments)
    $bash = Find-BashExe
    if (-not $bash) {
        throw "bash.exe not found. Install Git for Windows: https://git-scm.com/download/win"
    }
    # Build a bash command line. Single-quote args to deploy.sh to be safe.
    $argString = ($Arguments | ForEach-Object { "'$($_ -replace "'", "'\''")'" }) -join ' '
    # MSYS_NO_PATHCONV=1 + MSYS2_ARG_CONV_EXCL='*' prevent Git Bash from
    # rewriting paths like `:/app` to `C:/Program Files/Git/app` when bash
    # passes them to docker. Without this, deploy.sh's `docker run -w /app`
    # for the gradle build aborts with "the working directory ... is invalid".
    $cmdLine = "MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*' ./deploy.sh $argString".TrimEnd()
    # Pipe through Out-Host so bash's stdout streams to the screen instead of
    # being collected into this function's return value (which would later be
    # consumed by `exit` and lost). Avoid `2>&1` -- in PS 5.1 it wraps every
    # native-command stderr line in NativeCommandError noise. Stderr already
    # reaches the console because we don't capture it.
    & $bash -c $cmdLine | Out-Host
}

function Write-Step { param([string]$M) Write-Host "[deploy-win] $M" -ForegroundColor Green }
function Write-Warn { param([string]$M) Write-Host "[deploy-win] $M" -ForegroundColor Yellow }
function Write-Fail { param([string]$M) Write-Host "[deploy-win] $M" -ForegroundColor Red; exit 1 }

# Strip CR bytes (0x0D) without rename/atomic-replace -- Git for Windows NTFS
# bind mounts and even host filesystem reject the rename `sed -i`/`perl -i` use
# with "Permission denied". Open-truncate-write works (no rename, just
# in-place rewrite of the existing inode).
function Remove-CRLF {
    param([Parameter(Mandatory)] [string]$Path)
    if (-not (Test-Path $Path)) { return }
    $bytes = [IO.File]::ReadAllBytes($Path)
    $out = New-Object 'System.Collections.Generic.List[byte]'
    foreach ($b in $bytes) { if ($b -ne 0x0D) { $out.Add($b) } }
    [IO.File]::WriteAllBytes($Path, $out.ToArray())
}

# ---------------------------------------------------------------------------
# Subcommands that don't touch builds -- pass straight through to deploy.sh.
# ---------------------------------------------------------------------------
if ($RestArgs -and $RestArgs.Count -gt 0 -and $RestArgs[0] -in @('stop', 'status', 'logs')) {
    Invoke-DeploySh -Arguments $RestArgs
    exit $LASTEXITCODE
}

# Match the path deploy.sh checks (single source of truth).
$PortalJar = Join-Path $RepoRoot 'backend-master\api\VinPlayPortal\build\libs\VinPlayPortal-1.0.jar'
$BancaDll  = Join-Path $RepoRoot 'banca\BanCaLiteNet\out\BanCaLiteNet.dll'

# Docker Desktop on Windows accepts forward slashes in volume sources and
# does NOT do MSYS-style auto-conversion when called from PowerShell --
# so we don't need MSYS_NO_PATHCONV at all.
$BackendMount = ($RepoRoot.Replace('\', '/')) + '/backend-master'
$BancaMount   = ($RepoRoot.Replace('\', '/')) + '/banca'

# ---------------------------------------------------------------------------
# Step A: Java backend (Gradle inside Eclipse Temurin JDK 8)
# ---------------------------------------------------------------------------
if (-not (Test-Path $PortalJar) -or $Rebuild) {
    Write-Step "Building Java backend (Eclipse Temurin JDK 8) -- 2-5 minutes..."
    Write-Step "  Normalizing line endings on gradlew + *.sh ..."
    Remove-CRLF (Join-Path $RepoRoot 'backend-master\gradlew')
    Get-ChildItem -Path (Join-Path $RepoRoot 'backend-master') -Filter '*.sh' -Recurse -File -ErrorAction SilentlyContinue |
        ForEach-Object { Remove-CRLF $_.FullName }

    # -x :game:Minigame:build skips the Minigame subproject -- its source
    # references getUserByName(...) returning List<User>, but the upstream
    # library now returns a single User. Pre-existing tech debt unrelated
    # to this build. The previously-built Minigame JAR (in libs/) is reused.
    & docker run --rm `
        -v "${BackendMount}:/app" `
        -w /app `
        eclipse-temurin:8-jdk `
        bash -c "chmod +x gradlew && ./gradlew build copyRuntimeDeps -x test -x :game:Minigame:build -x :game:Minigame:compileJava"
    if ($LASTEXITCODE -ne 0) { Write-Fail "Java build failed (exit $LASTEXITCODE)" }
    if (-not (Test-Path $PortalJar)) { Write-Fail "Java build did not emit $PortalJar" }
    Write-Step "Java build successful."
} else {
    Write-Step "Java backend already built (use -Rebuild to force)."
}

# ---------------------------------------------------------------------------
# Step B: BanCa fish game (.NET SDK 5.0)
#
# Note: -Rebuild does NOT force a BanCa rebuild when the DLL already exists.
# BanCa's .NET SDK 5.0 image hits "Permission denied" writing to obj/ inside
# the bind-mounted volume on some Windows + Docker Desktop combos -- the
# error is non-deterministic and clears once the DLL is built. Once you have
# a working DLL, prefer reusing it. Pass -RebuildBanCa explicitly to force.
# ---------------------------------------------------------------------------
if (-not (Test-Path $BancaDll) -or $RebuildBanCa) {
    Write-Step "Building BanCa (.NET SDK 5.0)..."
    & docker run --rm `
        -v "${BancaMount}:/banca" `
        -w /banca/BanCaLiteNet `
        mcr.microsoft.com/dotnet/sdk:5.0 `
        dotnet publish -c Release -o out
    if ($LASTEXITCODE -ne 0) { Write-Fail ".NET build failed (exit $LASTEXITCODE)" }
    if (-not (Test-Path $BancaDll)) { Write-Fail "BanCa build did not emit $BancaDll" }
    Write-Step "BanCa build successful."
} else {
    Write-Step "BanCa already built (use -RebuildBanCa to force)."
}

# ---------------------------------------------------------------------------
# Step C: Defer the rest to deploy.sh. Artifacts now exist, so deploy.sh
# will skip its own build steps and proceed straight to .env, networks,
# db init, compose up. We do NOT pass --rebuild -- that flag is what
# triggers deploy.sh's own (broken on Windows) docker-run build steps.
# ---------------------------------------------------------------------------
$Passthrough = @()
if ($NoStart) { $Passthrough += '--no-start' }
# When -Rebuild is set, deploy.ps1 already rebuilt Java + .NET natively above
# (deploy.sh will then see PortalJar exists and skip its own gradle build).
# But Step 8's `compose build` only runs when REBUILD=true in deploy.sh, so
# the Docker images don't pick up changed property files / source. Pass
# --rebuild through so Step 8 rebuilds images.
if ($Rebuild) { $Passthrough += '--rebuild' }
if ($RestArgs)    { $Passthrough += $RestArgs }

Write-Step "Artifacts ready. Handing off to deploy.sh $($Passthrough -join ' ')..."
Invoke-DeploySh -Arguments $Passthrough
exit $LASTEXITCODE
