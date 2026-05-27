<#
.SYNOPSIS
    Windows PowerShell wrapper for the repo-root start.sh tier orchestrator.
    Delegates to start.sh via Git Bash so Windows users can run tier-level
    bring-up / stop / status / logs without leaving PowerShell.

.DESCRIPTION
    start.sh is the canonical tier-level entrypoint (does NOT build -- assumes
    images exist). Subcommands (aliases noted) map to docker compose files:

        database | db    -- mysql + mongodb + redis + rabbitmq + hazelcast cluster
        backend  | api   -- DB tier + backend-api + portal-api + vbee
        games            -- DB tier + backend + the 17 game-server containers
        web              -- DB tier + backend + nginx / admin-php / agency
        banca    | fish  -- DB tier + C# .NET fish-shooting game
        all              -- everything above (default if no subcommand)
        stop             -- bring down everything in this Compose project
        status           -- list containers + health
        logs [svc]       -- follow logs for one service (Ctrl+C to detach)

    This wrapper exists for the same reason as deploy.ps1: PowerShell users
    shouldn't have to remember `bash -c "./start.sh ..."` for routine ops.
    It's a thin pass-through -- no rewriting, no extra logic.

.PARAMETER Subcommand
    Which start.sh subcommand to run. Required. See list above.

.PARAMETER RestArgs
    Extra args (e.g. service name for `logs backend-api`).

.EXAMPLE
    .\start.ps1 database
    # Bring up the DB tier (mysql + mongodb + redis + rabbitmq + hazelcast).

.EXAMPLE
    .\start.ps1 backend
    # Backend Java APIs. Run AFTER `start.ps1 database` if DBs aren't up yet.

.EXAMPLE
    .\start.ps1 status

.EXAMPLE
    .\start.ps1 logs backend-api
    # Follow logs (Ctrl+C to detach).

.EXAMPLE
    .\start.ps1 stop

.NOTES
    Requires Git for Windows installed (provides bash.exe + Git Bash). If
    bash isn't found in standard install paths, the script aborts with a
    pointer to the installer.

    For a full clean bring-up that ALSO builds Java/.NET artifacts and
    initialises MySQL schema, use deploy.ps1 instead. start.ps1 is for the
    quick "containers were already built, just (re)start them" case.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)]
    [ValidateSet('database', 'db', 'backend', 'api', 'games', 'web',
                 'banca', 'fish', 'all', 'stop', 'status', 'logs')]
    [string]$Subcommand,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$RestArgs
)

$ErrorActionPreference = 'Stop'

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $RepoRoot

# bash.exe lookup -- same logic as deploy.ps1's Find-BashExe so behaviour
# stays consistent across the two wrappers.
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

$bash = Find-BashExe
if (-not $bash) {
    Write-Host "ERROR: bash.exe not found." -ForegroundColor Red
    Write-Host "Install Git for Windows: https://git-scm.com/download/win" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path (Join-Path $RepoRoot 'start.sh'))) {
    Write-Host "ERROR: start.sh not found at $RepoRoot" -ForegroundColor Red
    exit 1
}

# Single-quote each arg to defeat bash word-splitting / glob expansion. Replace
# any embedded ' with the standard '\'' bash escape sequence first.
$allArgs = @($Subcommand) + ($RestArgs | Where-Object { $_ })
$argString = ($allArgs | ForEach-Object { "'$($_ -replace "'", "'\''")'" }) -join ' '

# MSYS_NO_PATHCONV=1 + MSYS2_ARG_CONV_EXCL='*' stop Git Bash from rewriting
# colon-prefixed paths (like `:/app`) when start.sh hands them to docker.
$cmdLine = "MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*' ./start.sh $argString".TrimEnd()

Write-Host "[start-win] $cmdLine" -ForegroundColor Cyan
& $bash -c $cmdLine | Out-Host
exit $LASTEXITCODE
