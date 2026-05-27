<#
.SYNOPSIS
    Pull data from staging into local Docker DB containers.
    Windows port of the bash sync block in docs/BACKEND_SETUP_GUIDE.md (Section 2).

.DESCRIPTION
    Mode "Mongo": dumps the entire win123club database on staging, scp's the
    tarball back, restores into sunwinkr-mongodb (--drop). Use -MongoCollections
    to restrict to specific collections (e.g. just bau_cua_transaction +
    log_mini_poker for a fast sync).

    Mode "MySQL": mysqldump --databases <schemas> on staging, scp's the .sql
    back, imports into sunwinkr-mysql. Default schemas: vinplay_gamebai,
    vinplay, vinplay_admin, vinplay_minigame -- enough for admin / user /
    transaction / minigame APIs (e.g. c=109 list users). Override via
    -MysqlSchemas.

    Mode "Both": runs Mongo then MySQL.

    Requires plink.exe + pscp.exe in PATH (install via "scoop install putty"
    or "choco install putty"). SSH password resolved by Get-StagingPassword
    (-Password arg / $env:STAGING_SSH_PASSWORD / .env / hardcoded default).

    Database credentials are NOT passed in. Each `mongodump` / `mongorestore`
    / `mysqldump` / `mysql` call runs `sh -c '...'` inside the container and
    references $MONGO_INITDB_ROOT_USERNAME / $MONGO_INITDB_ROOT_PASSWORD /
    $MYSQL_ROOT_PASSWORD from the container's own env -- so the DB password
    never leaves the container and survives rotations without script edits.

.PARAMETER Mode
    Mongo | MySQL | Both. Default: Both.

.PARAMETER MysqlSchemas
    Schemas to dump in MySQL mode. Default covers the schemas the backend
    needs for typical admin / user / minigame API testing.

.PARAMETER MongoCollections
    Collections of win123club to dump in Mongo mode. Empty (default) dumps
    the entire database, which is slower but covers every API. Restrict to
    specific collections for a faster targeted sync.

.EXAMPLE
    ./sync-staging.ps1
    # Pulls full Mongo + default-schema MySQL into local containers.

.EXAMPLE
    ./sync-staging.ps1 -Mode Mongo

.EXAMPLE
    ./sync-staging.ps1 -Mode Mongo -MongoCollections log_mini_poker,bau_cua_transaction
    # Original docs/BACKEND_SETUP_GUIDE.md flow (faster, skips user_login_info etc).

.EXAMPLE
    ./sync-staging.ps1 -Mode MySQL -MysqlSchemas vinplay,vinplay_gamebai
    # Sync only the two schemas (faster, skips admin / minigame).

.EXAMPLE
    $env:STAGING_SSH_PASSWORD = 'xxx'
    ./sync-staging.ps1 -Mode MySQL
#>
[CmdletBinding()]
param(
    [ValidateSet('Mongo', 'MySQL', 'Both')]
    [string]$Mode = 'Both',
    [string]$StagingHost = '140.99.130.21',
    [string]$StagingUser = 'root',
    [string]$Password,
    [string]$MongoContainer = 'sunwinkr-mongodb',
    [string]$MysqlContainer = 'sunwinkr-mysql',
    [string[]]$MysqlSchemas = @('vinplay_gamebai', 'vinplay', 'vinplay_admin', 'vinplay_minigame'),
    [string[]]$MongoCollections = @()
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

$tmpDir = $env:TEMP
$pwArgs = @{ Password = $Password; StagingHost = $StagingHost; StagingUser = $StagingUser }

function Sync-Mongo {
    if ($MongoCollections.Count -eq 0) {
        $modeDesc = 'whole win123club DB'
    } else {
        $modeDesc = "win123club: $($MongoCollections -join ', ')"
        # Validate collection names so they can be safely interpolated.
        foreach ($c in $MongoCollections) {
            if ($c -notmatch '^[A-Za-z0-9_.-]+$') {
                throw "Invalid collection name '$c' -- only [A-Za-z0-9_.-] allowed."
            }
        }
    }
    Write-Step "MongoDB sync ($modeDesc)"

    if (-not (Test-DockerContainer -Name $MongoContainer)) {
        throw "Local container '$MongoContainer' not found. Run './deploy.sh database' (in Git Bash) first."
    }

    # Build the mongodump invocations:
    #   * empty -MongoCollections -> single full-DB dump (`-d win123club` only)
    #   * else  -> one `-d win123club -c <name>` per collection, chained with &&
    # We dump into /tmp/sunwinkr-sync/dump (NOT /tmp/dump) -- the staging
    # mongo container has other tooling (cron, ad-hoc operator dumps) that
    # uses /tmp/dump and will rm/recreate it underneath us, causing tar to
    # exit non-zero with "file changed/removed as we read it". Using a
    # script-private parent dir avoids that race.
    $remoteWorkDir = '/tmp/sunwinkr-sync'
    $remoteTarball = '/tmp/sunwinkr-sync.tar.gz'
    $authStub = '-u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin'
    if ($MongoCollections.Count -eq 0) {
        $dumpInvocations = @("mongodump $authStub -d win123club --out $remoteWorkDir/dump")
    } else {
        $dumpInvocations = $MongoCollections | ForEach-Object { "mongodump $authStub -d win123club -c $_ --out $remoteWorkDir/dump" }
    }
    # `tar -C $remoteWorkDir dump` keeps the archive paths starting with
    # `dump/...` so the local extract + `docker cp $extractDir\dump` flow
    # below stays unchanged.
    $innerSh = (@(
        "rm -rf $remoteWorkDir $remoteTarball",
        "mkdir -p $remoteWorkDir"
    ) + $dumpInvocations + @(
        "tar -C $remoteWorkDir -czf $remoteTarball dump"
    )) -join ' && '
    # Wrap inner shell command in single quotes for `sh -c '...'`. There are no
    # apostrophes inside $innerSh (validated above), so a plain wrap is safe.
    $dockerExec = "docker exec sunwinkr-mongodb sh -c '$innerSh'"

    Write-Host "    [1/5] mongodump on staging (auth via container env, tar inside container)..."
    # Note on layout:
    #   `mongodump --out $remoteWorkDir/dump` writes to that path *inside the
    #   container* (the staging container's /tmp is NOT a host bind mount).
    #   So we tar inside the container, then `docker cp` the tarball out to
    #   the staging host before pscp can pull it down. Doing tar on the host
    #   first would fail with "dump: Cannot stat".
    $remoteCmd = @(
        $dockerExec,
        "rm -f $remoteTarball",
        "docker cp sunwinkr-mongodb:$remoteTarball $remoteTarball"
    ) -join ' && '
    Invoke-StagingSsh -Command $remoteCmd @pwArgs | Out-Null

    $localTarball = Join-Path $tmpDir 'sunwinkr-mongo-dump.tar.gz'
    Write-Host "    [2/5] Downloading tarball -> $localTarball"
    Copy-FromStaging -RemotePath $remoteTarball -LocalPath $localTarball @pwArgs

    Write-Host "    [3/5] Extracting locally..."
    $extractDir = Join-Path $tmpDir 'sunwinkr-mongo-dump'
    if (Test-Path $extractDir) { Remove-Item $extractDir -Recurse -Force }
    New-Item -ItemType Directory -Path $extractDir | Out-Null
    & tar -xzf $localTarball -C $extractDir
    if ($LASTEXITCODE -ne 0) { throw "tar extract failed" }

    Write-Host "    [4/5] Copying dump into local container..."
    & docker exec $MongoContainer sh -c 'rm -rf /tmp/dump' | Out-Null
    & docker cp "$extractDir\dump" "${MongoContainer}:/tmp/dump"
    if ($LASTEXITCODE -ne 0) { throw "docker cp failed" }

    Write-Host "    [5/5] mongorestore --drop into $MongoContainer (auth via container env)..."
    & docker exec $MongoContainer sh -c 'mongorestore -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin -d win123club --drop /tmp/dump/win123club'
    if ($LASTEXITCODE -ne 0) { throw "mongorestore failed" }

    Write-Ok "MongoDB sync complete."
}

function Sync-MySQL {
    $schemaList = ($MysqlSchemas -join ' ')
    Write-Step "MySQL sync (schemas: $schemaList)"

    if (-not (Test-DockerContainer -Name $MysqlContainer)) {
        throw "Local container '$MysqlContainer' not found. Run './deploy.sh database' (in Git Bash) first."
    }

    # Validate schema names client-side (only [A-Za-z0-9_]) so the names can
    # be safely interpolated into the remote shell command without escaping.
    foreach ($s in $MysqlSchemas) {
        if ($s -notmatch '^[A-Za-z0-9_]+$') {
            throw "Invalid schema name '$s' -- only [A-Za-z0-9_] allowed."
        }
    }

    Write-Host "    [1/3] mysqldump --databases on staging (auth via container env)..."
    # The `>` redirect MUST be outside `docker exec` -- the inner `sh -c '...'`
    # only owns the mysqldump invocation; mysqldump's stdout streams back
    # through docker exec to the staging-host shell, which writes the .sql
    # file on the staging host filesystem (where pscp can read it).
    # Putting `>` inside the sh -c would land the file inside the container
    # and pscp would 404.
    #
    # `--databases` (vs single-DB form) makes mysqldump emit
    # `CREATE DATABASE IF NOT EXISTS <s>; USE <s>;` headers per schema, so
    # the import on local creates any missing schema and switches contexts
    # without us having to run separate `CREATE DATABASE` SQL.
    #
    # `--add-drop-database` emits `DROP DATABASE IF EXISTS <s>;` before each
    # CREATE. Required: when local has a stale schema with utf8mb3 charset
    # (from a previous deploy) and staging is utf8mb4, per-table DROP + CREATE
    # leaves orphan FK constraints from old tables pointing at newly-created
    # tables -- MySQL 8 rejects with ERROR 3780 ("incompatible columns") even
    # under FOREIGN_KEY_CHECKS=0 because the column-type check on FK creation
    # is unconditional. Dropping the whole DB up front avoids that.
    $remoteCmd = "docker exec sunwinkr-mysql sh -c 'mysqldump -uroot -p`"`$MYSQL_ROOT_PASSWORD`" --single-transaction --quick --add-drop-database --databases $schemaList' > /tmp/vinplay_dump.sql"
    Invoke-StagingSsh -Command $remoteCmd @pwArgs | Out-Null

    $localSql = Join-Path $tmpDir 'sunwinkr-vinplay_dump.sql'
    Write-Host "    [2/3] Downloading dump -> $localSql"
    Copy-FromStaging -RemotePath '/tmp/vinplay_dump.sql' -LocalPath $localSql @pwArgs

    Write-Host "    [3/3] Importing into $MysqlContainer (auth via container env)..."
    # `docker cp` then `sh -c '... < /tmp/...'` instead of stdin pipe so the
    # password lives only inside the container env (no MYSQL_PWD on the host
    # process tree, no `-p$pw` in the host argv).
    # No `-D database` because the dump's USE statements switch DBs per schema.
    & docker cp $localSql "${MysqlContainer}:/tmp/vinplay_dump.sql"
    if ($LASTEXITCODE -ne 0) { throw "docker cp failed" }
    & docker exec $MysqlContainer sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /tmp/vinplay_dump.sql'
    if ($LASTEXITCODE -ne 0) { throw "mysql import failed" }

    Write-Ok "MySQL sync complete (schemas: $schemaList)."
}

switch ($Mode) {
    'Mongo' { Sync-Mongo }
    'MySQL' { Sync-MySQL }
    'Both'  { Sync-Mongo; Sync-MySQL }
}

Write-Host ""
Write-Host "All sync tasks finished." -ForegroundColor Green
