# Windows PowerShell scripts for SunwinKR

Native PowerShell ports of the bash / Python scripts in `README.md` and
`docs/BACKEND_SETUP_GUIDE.md`, written for **Windows PowerShell 5.1+** (the
version that ships with Windows 10/11).

| Script | Replaces |
|---|---|
| `deploy.ps1` | `deploy.sh` (build + delegate to repo-root deploy.sh via Git Bash) |
| `start.ps1` | `start.sh` tier orchestrator (database / backend / games / web / banca / all / stop / status / logs) |
| `sync-staging.ps1` | Guide Section 2: `mongodump` + `mysqldump` sync block |
| `staging-tunnel.ps1` | Guide Section 3.2: `run_tunnel.py` (pexpect) |
| `set-config-target.ps1` | Guide Section 3.3: `find ... sed -i` (forward + revert) |
| `get-aat.ps1` | New — fetches admin `aat` token via SSH + SmokeTestBypass for Swagger UI |
| `_common.ps1` | Internal helpers (dot-sourced by the others) |

`deploy.ps1` does the Java + .NET build natively in PowerShell, then hands
off to the repo-root `deploy.sh` via Git Bash for the remaining 700+ lines
of orchestration (env gen, network/volume create, MySQL init, healthcheck
loops, compose up). Porting that bash to PS would create a second source
of truth that drifts; instead we shell out for the parts that are pure
infra wiring.

---

## Prerequisites

1. **Docker Desktop** running, with the project's local containers already
   created (`./deploy.sh database` from Git Bash at least once).
2. **PuTTY tools** (`plink.exe` + `pscp.exe`) on `PATH` — sshpass equivalent
   for password-based SSH:
   ```powershell
   scoop install putty
   # or
   choco install putty
   ```
3. **PowerShell execution policy** that allows local scripts:
   ```powershell
   Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
   ```
4. *(Optional)* Set the staging SSH password. Lookup order in
   `Get-StagingPassword`:
   1. `-Password` parameter on the script call
   2. `$env:STAGING_SSH_PASSWORD` (per-session env var)
   3. `STAGING_SSH_PASSWORD=` line in the repo-root `.env` (gitignored)
   4. Hardcoded default in `_common.ps1` (often stale after rotations)

   ```powershell
   # Per-session:
   $env:STAGING_SSH_PASSWORD = 'paste-the-password-here'

   # Or set-once: add this line to .env at repo root (.env is gitignored):
   #   STAGING_SSH_PASSWORD=paste-the-password-here
   ```

---

## Usage

All commands assume your shell is at `scripts\windows\`. Use absolute or
relative paths if running from elsewhere.

### Deploy / start / stop the full stack

```powershell
.\deploy.ps1                      # Full deploy (build if missing, then start)
.\deploy.ps1 -Rebuild             # Force-rebuild Java + .NET, then start
.\deploy.ps1 -NoStart             # Build + setup only, do not bring services up
.\deploy.ps1 stop
.\deploy.ps1 status
.\deploy.ps1 logs portal-api      # Follow logs for one service
```

Equivalent to running `./deploy-windows.sh` from Git Bash; the build steps
run natively in PowerShell (no MSYS path conversion needed) and the rest
is delegated to `..\..\deploy.sh` via Git Bash.

### Tier-level orchestration (start / stop / status / logs without rebuild)

```powershell
.\start.ps1 database              # mysql + mongo + redis + rmq + hazelcast
.\start.ps1 backend               # DB tier + backend-api + portal-api + vbee
.\start.ps1 games                 # DB + backend + 17 game-server containers
.\start.ps1 web                   # DB + backend + nginx + admin-php + agency
.\start.ps1 banca                 # DB + .NET fish-shooting game
.\start.ps1 all                   # everything above (default)
.\start.ps1 stop
.\start.ps1 status
.\start.ps1 logs backend-api      # Ctrl+C to detach
```

Thin wrapper around `start.sh` via Git Bash (same pattern as `deploy.ps1`).
Aliases match `start.sh`: `db` ↔ `database`, `api` ↔ `backend`, `fish` ↔ `banca`.

Use `start.ps1` when images are already built and you just need to (re)start
a tier. Use `deploy.ps1` for first-time bring-up or after a code change that
needs Java / .NET rebuild + MySQL schema init.

### Sync data from staging to local

```powershell
.\sync-staging.ps1                 # Mongo + MySQL
.\sync-staging.ps1 -Mode Mongo
.\sync-staging.ps1 -Mode MySQL
```

Pulls `win123club.log_mini_poker` + `win123club.bau_cua_transaction` (Mongo)
and the entire `vinplay_gamebai` schema (MySQL) into the local
`sunwinkr-mongodb` / `sunwinkr-mysql` containers, replacing existing data.

### Open / close staging tunnel

```powershell
.\staging-tunnel.ps1 -Action Open -StopLocalContainers
.\staging-tunnel.ps1 -Action Status
.\staging-tunnel.ps1 -Action Close
```

`-StopLocalContainers` stops `sunwinkr-mysql/-mongodb/-redis/-rabbitmq/-hazelcast`
first so ports `3307 / 27018 / 6379 / 5672 / 5701` are free for forwarding.

The PID is saved to `%TEMP%\sunwinkr-tunnel.pid` so `Close` can find it
across PowerShell sessions.

### Fetch admin `aat` token for Swagger UI

```powershell
.\get-aat.ps1                                      # token to stdout
.\get-aat.ps1 -Copy                                # to clipboard + confirmation
.\get-aat.ps1 | Set-Clipboard                      # equivalent without the line
.\get-aat.ps1 -Username adminA -Password 'pw' -Copy
.\get-aat.ps1 -BackendPort 19082                   # bypass swagger proxy, hit backend-api directly
.\get-aat.ps1 -BackendHost 192.168.1.50
```

Reads `SMOKE_TEST_BYPASS_KEY` from the repo-root `.env`, then POSTs
`http://localhost:18080/api_backend?c=701` with the `X-Smoke-Test-Key`
header. Default port `18080` is the swagger-nginx proxy
(`docker-compose.swagger.yml`); use `-BackendPort 19082` to hit
`backend-api` directly. Either way the request originates from a
loopback / RFC 1918 address so `SmokeTestBypass.isInternalIp()` accepts
it and the captcha + 2FA paths are skipped. Prints the resulting `adminToken` (32-char hex) — paste into
the **Authorize** button in Swagger UI; `persistAuthorization` keeps it
for the 8h TTL across reloads.

Prerequisites:
- Local backend running (`./start.sh backend` or `./deploy.ps1`) on the
  same machine
- `SMOKE_TEST_BYPASS_KEY=...` set in `.env` BEFORE the backend was
  brought up (env is baked at container start; restart backend after
  changing `.env`)

Cannot be used against a public hostname (e.g. `staging-admin.sunkr.bet`)
because `SmokeTestBypass` rejects public IPv4 even with the correct key.
For staging, login through the admin UI normally and copy the `aat`
query param out of any subsequent request via DevTools.

### Switch backend configs to the tunnel (or back to local)

```powershell
.\set-config-target.ps1 -Target Tunnel        # point at 172.17.0.1:tunnelPorts
.\set-config-target.ps1 -Target Local         # back to docker service names
.\set-config-target.ps1 -Target Tunnel -DryRun
```

Rewrites every `db_pool.properties / mongo.properties / rmq.properties /
hazelcast.properties` under `backend-master/`. Each modified file gets a
`*.bak` backup unless `-NoBackup` is passed.

After switching, rebuild the backend so configs are baked into the new JARs:

```bash
# Git Bash:
./start.sh backend
```

---

## Typical flow (Section 3 "tunnel into staging" mode)

```powershell
# 1. Stop local DBs and open the SSH tunnel
.\staging-tunnel.ps1 -Action Open -StopLocalContainers

# 2. Repoint the Java backend at the tunnel
.\set-config-target.ps1 -Target Tunnel

# 3. Build + start backend  (Git Bash, not PowerShell)
#    cd ../..; ./start.sh backend

# 4. ...debug against live staging data...

# 5. Done — close tunnel and revert configs
.\staging-tunnel.ps1 -Action Close
.\set-config-target.ps1 -Target Local
```

---

## Notes & caveats

- **Password handling:** the original `sshpass` / `pexpect` scripts pass the
  staging password on the command line. PowerShell does the same via
  `plink -pw` and `pscp -pw`, which is functionally identical (and equally
  insecure for prod use). Prefer `$env:STAGING_SSH_PASSWORD` over the
  hardcoded default in `_common.ps1`.
- **`-L 0.0.0.0:...` forwarding** binds the staging DB on every interface of
  your machine, so anyone on your LAN can connect to the staging databases
  via your laptop. Close the tunnel when you are done.
- **PowerShell 5.1 only:** scripts avoid PS 7-only syntax (`??`, `&&`, `||`,
  ternary). Should run on any Windows 10/11 box without extra installs
  beyond PuTTY.
- **Property file encoding:** rewrites use `[IO.File]::WriteAllText` to write
  UTF-8 *without* BOM, matching how Java loads `.properties` files.
- **Line endings:** the rewritten property files use CRLF. The Java loader
  handles both, but if you mix this with the bash `sed -i` revert flow,
  expect mixed endings. Run `set-config-target.ps1 -Target Local` once to
  normalize.
