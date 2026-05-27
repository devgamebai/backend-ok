<#
.SYNOPSIS
    Rewrite backend-master/api/*/config/*.properties to point at the staging
    SSH tunnel (172.17.0.1) or revert back to local Docker hostnames.
    Windows port of the find/sed block in docs/BACKEND_SETUP_GUIDE.md (Section 3.3).

.DESCRIPTION
    Target "Tunnel" rewrites:
        db_pool.properties:    127.0.0.1:3306 | mysql:3306    -> 172.17.0.1:3307
        mongo.properties:      host=127.0.0.1 | host=mongodb  -> host=172.17.0.1
                               port=27017                     -> port=27018
        rmq.properties:        rmq_server=...                 -> rmq_server=172.17.0.1
        hazelcast.properties:  address=...                    -> address=172.17.0.1

    Target "Local" reverts to Docker service names (mysql, mongodb, rabbitmq, hazelcast)
    on their default ports.

    Files are matched recursively under -Root (default: backend-master).
    A .bak copy is written next to each modified file by default; pass
    -NoBackup to skip.

.PARAMETER Target
    Tunnel | Local. Required.

.PARAMETER Root
    Root directory to scan. Default: backend-master (relative to repo root).

.PARAMETER NoBackup
    Skip writing *.bak alongside each modified file.

.PARAMETER DryRun
    Show what would change without writing.

.EXAMPLE
    ./set-config-target.ps1 -Target Tunnel
    ./set-config-target.ps1 -Target Local
    ./set-config-target.ps1 -Target Tunnel -DryRun
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('Tunnel', 'Local')]
    [string]$Target,
    [string]$Root,
    [switch]$NoBackup,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if (-not $Root) {
    # Resolve project root (two levels up from scripts/windows/)
    $repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
    $Root = Join-Path $repoRoot 'backend-master'
}

if (-not (Test-Path $Root)) {
    throw "Root directory not found: $Root"
}

# Per-filename rewrite rules. Each rule = list of @{ Pattern; Replacement }
# pairs applied in order with regex -replace.
$tunnelRules = @{
    'db_pool.properties' = @(
        @{ Pattern = '127\.0\.0\.1:3306';                 Replacement = '172.17.0.1:3307' },
        @{ Pattern = 'mysql:3306';                         Replacement = '172.17.0.1:3307' }
    )
    'mongo.properties' = @(
        @{ Pattern = '^host=127\.0\.0\.1$';                Replacement = 'host=172.17.0.1' },
        @{ Pattern = '^host=mongodb$';                     Replacement = 'host=172.17.0.1' },
        @{ Pattern = '^port=27017$';                       Replacement = 'port=27018' }
    )
    'rmq.properties' = @(
        @{ Pattern = '^rmq_server=.*$';                    Replacement = 'rmq_server=172.17.0.1' }
    )
    'hazelcast.properties' = @(
        @{ Pattern = '^address=.*$';                       Replacement = 'address=172.17.0.1' }
    )
}

$localRules = @{
    'db_pool.properties' = @(
        @{ Pattern = '172\.17\.0\.1:3307';                 Replacement = '127.0.0.1:3306' }
    )
    'mongo.properties' = @(
        @{ Pattern = '^host=172\.17\.0\.1$';               Replacement = 'host=mongodb' },
        @{ Pattern = '^port=27018$';                       Replacement = 'port=27017' }
    )
    'rmq.properties' = @(
        @{ Pattern = '^rmq_server=172\.17\.0\.1$';         Replacement = 'rmq_server=rabbitmq' }
    )
    'hazelcast.properties' = @(
        @{ Pattern = '^address=172\.17\.0\.1$';            Replacement = 'address=hazelcast' }
    )
}

$rules = if ($Target -eq 'Tunnel') { $tunnelRules } else { $localRules }

Write-Step "Rewriting *.properties under $Root  (target: $Target)"
if ($DryRun) { Write-Warn "DRY RUN -- no files will be changed." }

$totalFiles = 0
$totalChanged = 0
foreach ($filename in $rules.Keys) {
    $matches = Get-ChildItem -Path $Root -Filter $filename -Recurse -File -ErrorAction SilentlyContinue
    foreach ($file in $matches) {
        $totalFiles++
        # -Raw would lose line endings on Set-Content; read as array, rewrite per-line.
        $original = Get-Content -Path $file.FullName
        $modified = $original | ForEach-Object {
            $line = $_
            foreach ($rule in $rules[$filename]) {
                $line = $line -replace $rule.Pattern, $rule.Replacement
            }
            $line
        }
        # Compare as joined strings to detect any change (faster than per-line diff)
        $oldText = ($original -join "`n")
        $newText = ($modified -join "`n")
        if ($oldText -ne $newText) {
            $totalChanged++
            $rel = Resolve-Path -Relative $file.FullName -ErrorAction SilentlyContinue
            if (-not $rel) { $rel = $file.FullName }
            Write-Host "    [chg] $rel"
            if (-not $DryRun) {
                if (-not $NoBackup) {
                    Copy-Item -Path $file.FullName -Destination "$($file.FullName).bak" -Force
                }
                # UTF8 without BOM is the safest for Java property files. PS 5.1's
                # 'utf8' encoding writes a BOM; use [IO.File]::WriteAllText for no-BOM.
                [IO.File]::WriteAllText($file.FullName, ($modified -join "`r`n"))
            }
        }
    }
}

Write-Host ""
Write-Host ("Scanned: {0} file(s). Modified: {1}." -f $totalFiles, $totalChanged) -ForegroundColor Green
if ($Target -eq 'Tunnel' -and -not $DryRun -and $totalChanged -gt 0) {
    Write-Host ""
    Write-Host "Next: rebuild the backend so the new config is baked into JARs/configs:"
    Write-Host "  (in Git Bash)  ./start.sh backend"
}
