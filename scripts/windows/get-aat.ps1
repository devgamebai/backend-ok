<#
.SYNOPSIS
    Fetch an admin auth token (aat) from the LOCAL backend by combining
    c=701 admin login with the SmokeTestBypass mechanism, so captcha + 2FA
    are skipped for dev / Swagger Try-it-out use.

.DESCRIPTION
    /api_backend?c=701 (admin login) normally requires:
        un + pw (md5)        -- always
        cp + cid (captcha)   -- when captchaRequired() returns true
        otp                  -- when the admin row has Is2FAEnabled = 1

    SmokeTestBypass.java skips captcha + 2FA when ALL three hold:
        1. SMOKE_TEST_BYPASS_KEY env var is set in the backend container
        2. caller submits matching X-Smoke-Test-Key header
        3. caller IP is in RFC 1918 / loopback range

    For LOCAL backend (this script's default), condition #3 is satisfied
    automatically because the request originates from 127.0.0.1 (or, when
    routed through swagger-nginx, from the proxy container's 172.x address
    -- both are in the internal allowlist). The script reads
    SMOKE_TEST_BYPASS_KEY from <repo>/.env (the same file your local
    docker-compose backend reads at startup), POSTs to
    http://localhost:18080/api_backend?c=701, and prints the resulting
    adminToken.

    Paste the token into the Swagger UI Authorize button (apiKey scheme
    "AatAuth"). persistAuthorization keeps it for the 8h TTL across
    reloads.

.PARAMETER Username
    Admin username. Default: superadmin (matches deploy.sh's reset).

.PARAMETER Password
    Admin password (plaintext). Will be MD5-hashed locally before sending.
    Default: admin123 (matches deploy.sh's RESET_ADMIN_PASSWORD value).

.PARAMETER Copy
    Copy the token to the Windows clipboard instead of stdout, and print
    a confirmation. Without -Copy, the token goes to stdout so it can be
    piped: e.g. './get-aat.ps1 | Set-Clipboard'.

.PARAMETER BackendHost
    Host the backend-api is reachable at. Default: localhost.

.PARAMETER BackendPort
    Host port the backend is reachable on. Default: 18080 (the
    swagger-nginx proxy from docker-compose.swagger.yml). Use 19082 to
    hit backend-api directly when only docker-compose.backend.yml is up.

.PARAMETER EnvPath
    Path to .env containing SMOKE_TEST_BYPASS_KEY. Default: repo-root .env.

.EXAMPLE
    .\get-aat.ps1
    # Token to stdout, default superadmin/admin123 against localhost:18080

.EXAMPLE
    .\get-aat.ps1 -Copy
    # Token to clipboard, prints "OK ... copied"

.EXAMPLE
    .\get-aat.ps1 | Set-Clipboard
    # Equivalent to -Copy without the confirmation line

.EXAMPLE
    .\get-aat.ps1 -Username adminA -Password 'real-pw' -Copy

.EXAMPLE
    .\get-aat.ps1 -BackendPort 19082
    # Bypass the swagger-nginx proxy, hit backend-api directly

.EXAMPLE
    .\get-aat.ps1 -BackendHost 192.168.1.50
    # Hit a backend on a different host (still must be in RFC 1918 range
    # so SmokeTestBypass accepts it)

.NOTES
    Requires the local backend to be running with SMOKE_TEST_BYPASS_KEY
    set in its env. Bring up via './start.sh backend' (Git Bash) or
    './deploy.ps1' (PowerShell), plus optionally the swagger proxy
    (`docker compose -f docker-compose.swagger.yml up -d`). Verify it's up:
        curl http://localhost:18080/swagger/openapi.json     # swagger proxy
        curl http://localhost:19082/api_backend?c=3          # backend direct

    The token is logged at WARN on the backend audit trail with the
    granting client IP -- expected and visible in backend-api logs.
#>
[CmdletBinding()]
param(
    [string]$Username = 'superadmin',
    [string]$Password = 'admin123',
    [switch]$Copy,
    [string]$BackendHost = 'localhost',
    [int]$BackendPort = 18080,
    [string]$EnvPath
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

# --- Resolve .env path
if (-not $EnvPath) {
    $EnvPath = (Resolve-Path (Join-Path $PSScriptRoot '..\..\.env')).Path
}
if (-not (Test-Path -LiteralPath $EnvPath)) {
    Write-Host "ERROR: .env not found at $EnvPath" -ForegroundColor Red
    Write-Host "Pass -EnvPath if your .env is elsewhere, or copy .env.example to .env first." -ForegroundColor Yellow
    exit 1
}

# --- Read smoke bypass key
$smoke = Read-DotEnvKey -Path $EnvPath -Key 'SMOKE_TEST_BYPASS_KEY'
if (-not $smoke) {
    Write-Host "ERROR: SMOKE_TEST_BYPASS_KEY not set (or empty) in $EnvPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Add a line like this to .env, then restart the backend so it picks up the value:" -ForegroundColor Yellow
    Write-Host "  SMOKE_TEST_BYPASS_KEY=`$(openssl rand -hex 32)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "(Or generate via PowerShell: -join ((1..64) | ForEach-Object { '{0:x}' -f (Get-Random -Max 16) }))" -ForegroundColor Yellow
    exit 1
}

# --- MD5 the admin password locally
$md5Bytes = [System.Security.Cryptography.MD5]::Create().ComputeHash(
    [System.Text.Encoding]::UTF8.GetBytes($Password))
$pwMd5 = -join ($md5Bytes | ForEach-Object { '{0:x2}' -f $_ })

# --- Call c=701
$uri = "http://${BackendHost}:${BackendPort}/api_backend?c=701&un=${Username}&pw=${pwMd5}"
try {
    $resp = Invoke-RestMethod -Method Post -Uri $uri `
        -Headers @{ 'X-Smoke-Test-Key' = $smoke } `
        -TimeoutSec 10 `
        -ErrorAction Stop
} catch {
    Write-Host "ERROR: HTTP call to ${uri} failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Common causes:" -ForegroundColor Yellow
    Write-Host "  - backend (or swagger-nginx proxy) not running"
    Write-Host "      docker ps --format '{{.Names}}\t{{.Status}}' | findstr -i 'backend swagger'"
    Write-Host "  - port $BackendPort not bound on host"
    Write-Host "      try '-BackendPort 19082' (direct) or '-BackendPort 18080' (swagger proxy)"
    Write-Host "  - SMOKE_TEST_BYPASS_KEY in .env was added AFTER backend started"
    Write-Host "      env vars are read once at container boot -- restart backend after .env changes"
    exit 1
}

# --- Validate envelope
if (-not $resp.success) {
    Write-Host "ERROR: c=701 returned errorCode=$($resp.errorCode) message=$($resp.message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common errorCodes:" -ForegroundColor Yellow
    Write-Host "  1001  -> bypass denied. Check that:"
    Write-Host "          a) SMOKE_TEST_BYPASS_KEY in .env matches what the running backend was started with"
    Write-Host "          b) backend-api was restarted after .env changed (env vars are read at boot)"
    Write-Host "          c) your IP looks internal to backend (loopback should always work)"
    Write-Host "  1005  -> admin username not found in DB"
    Write-Host "  1007  -> admin password wrong"
    Write-Host "  1008  -> 2FA required for this admin (smoke bypass not granted -> see 1001)"
    Write-Host "  115   -> captcha not satisfied (bypass not granted -> see 1001)"
    Write-Host "  1109  -> admin status not 'A' (account disabled)"
    exit 1
}

# --- data is a JSON-encoded string per the API contract
$data = $null
if ($resp.data -is [string]) {
    try { $data = $resp.data | ConvertFrom-Json -ErrorAction Stop }
    catch { Write-Host "ERROR: response.data not valid JSON: $($resp.data)" -ForegroundColor Red; exit 1 }
} else {
    $data = $resp.data
}
$token = $data.adminToken

if (-not $token) {
    Write-Host "ERROR: response did not contain adminToken. data=$($resp.data)" -ForegroundColor Red
    exit 1
}

# Sanity check: 32-char hex
if ($token -notmatch '^[0-9a-f]{32}$') {
    Write-Host "WARNING: token doesn't look like 32-char hex -- got '$token'" -ForegroundColor Yellow
}

if ($Copy) {
    $token | Set-Clipboard
    Write-Ok "aat token copied to clipboard ($($token.Length) chars)"
} else {
    Write-Output $token
}
