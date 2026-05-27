# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

The `sunwinkr` backend platform — a polyglot, container-orchestrated stack with **17+ game servers, 4 Java HTTP APIs, a .NET fish game, a Node lottery scraper, a PHP agency portal, and a WebSocket bridge**. All services are wired together by Docker Compose and brought up with `deploy.sh` / `start.sh`. See [README.md](README.md) for the full service inventory, ports, and architecture diagram — do not duplicate that here.

The repo is invoked from at least two layouts in the wild — `/root/sunwinkr/` on dev/staging and `/root/sunwinkr/sunwinkr-backend/` on production (`SUNWINKR_DIR` in [.gitlab-ci.yml](.gitlab-ci.yml)). Don't hard-code paths.

## Common Commands

```bash
# Full bring-up (build everything, set up DB, start all 32 services)
./deploy.sh
./deploy.sh --rebuild       # force rebuild Java + .NET (snapshots :latest → :last-working first)
./deploy.sh --no-start      # build/setup only
./deploy.sh stop|status|logs [svc]

# Tier-level orchestration (does not build — assumes images exist)
./start.sh database|backend|games|web|banca|all|stop|status|logs

# Java build (from backend-master/, JDK 8, Gradle wrapper)
cd backend-master && ./gradlew build copyRuntimeDeps -x test

# API integration test suite — calls staging APIs by default, not local
bash tests/run_all.sh                 # full suite
bash tests/run_all.sh --suite=test_bank
bash tests/run_all.sh --fast

# Smoke test
bash scripts/smoke-test.sh
```

There are **no JUnit test suites in `backend-master/`** — `gradle test` runs nothing meaningful. The only test coverage is the bash/python scripts under `tests/` that hit live APIs (default target: `staging-play.sunkr.bet` / `staging-admin.sunkr.bet`).

## Backend Java Architecture (the non-obvious parts)

Read [README.md](README.md#backend-jar-architecture) first for the 3-tier classpath. Beyond that:

### `backend-master/entrypoint.sh` rewrites config at container start
**Source `*.properties` files in git contain placeholder hosts/passwords.** The Dockerfile copies them into the image as-is; [entrypoint.sh](backend-master/entrypoint.sh) does an in-place `sed` over every `db_pool.properties` / `mongo.properties` / `rmq.properties` / `hazelcast.properties` / `rabbitmq_config.xml` to substitute the docker-network service names (`mysql`, `mongodb`, `rabbitmq`, `hazelcast`) and the `${MYSQL_USER}`/`${MYSQL_PASSWORD}`/etc. from `.env`. **Never commit real credentials to these files** — they are templates. The `M` git status on the config files in this repo is expected and should not be committed unless you're actually changing the template.

The same script also:
- Per-game: links `/app/config` → `/app/game/<name>/config` and `/app/conf` → `/app/game/<name>/conf` (the BitZero framework derives config paths from JAR location).
- Adds missing keys (`isCheat=0`, `isHuVang=0`, `isBot=0`, `isLog=0`, `dev_mod=0`) to `cluster.properties` — `GameUtils` static initializer throws `NumberFormatException` and permanently breaks the class if any are missing.
- Resolves a per-game **classpath conflict**: both `Minigame.jar` and `SlotMachine-1.0.jar` ship `game.GameConfig.GameConfig` with **incompatible fields**. Entrypoint `zip -d`s the wrong copy from each container at startup. Don't try to "fix" by deleting one upstream — both games legitimately need their own version. The TODO in entrypoint.sh:130 to deduplicate is real but not done.
- Patches each game's `server.xml` with the correct TCP port (the ports are hard-coded in the entrypoint case statement around line 147).

### Banned API: `UserModel.getCurrentMoney(String)` / `setCurrentMoney(String, long)`
These methods existed but were named like "current balance" while actually returning **cumulative P&L** (`vin_total` / `xu_total`). They were renamed to `getTotalPnl` / `setTotalPnl` on 2026-04-10 (SUN-748 / SUN-753) after multiple production incidents where a losing player's wallet flipped negative. The build now blocks reintroduction:

- [backend-master/scripts/check-no-currentmoney-trap.sh](backend-master/scripts/check-no-currentmoney-trap.sh) is wired as a `:checkNoCurrentMoneyTrap` Gradle task that **every** subproject's `compileJava` depends on. Adding either trap method or a two-arg `.getCurrentMoney("vin"|moneyType)` call **fails the build**.
- Use `user.getMoney(type)` / `user.getVin()` / `user.getXu()` for real balance.
- Use `user.getTotalPnl(type)` only for cumulative P&L (e.g. `LogMoneyUserMessage`).
- All response DTOs that ship a "current balance" wire field run through `BalanceGuard.clamp(value, fieldName)` ([VbeeCommon](backend-master/VbeeCommon/src/main/java/com/vinplay/vbee/common/response/BalanceGuard.java)) — it clamps negatives to 0 and logs a stack trace identifying the leaking caller.

### Logging stack: Logback only
The root [build.gradle](backend-master/build.gradle) **explicitly excludes** `log4j:log4j`, `slf4j-log4j12`, and `slf4j-reload4j` from every transitive dep. The replacement bridge is `org.slf4j:log4j-over-slf4j` → `logback-classic`. Existing code using the Log4j 1.x API still works. **Do not add any of the excluded modules back** (they cause `StackOverflowError` / "No SLF4J providers" loops at runtime).

### Pinned versions that must not be bumped without coordinating
- **Hazelcast 3.12.13** — pinned for client compatibility with the player-side Cocos Creator client. 3.13+ breaks the wire protocol.
- **JDK 8** — `sourceCompatibility = 1.8` everywhere. Several modules use JDK 8-only APIs.
- **HikariCP-java7 2.4.13** — chosen for JDK 8; the modern HikariCP drops JDK 8 support.
- **MongoDB Java driver 3.12.x** — known not to recover from a server restart ([INFRA_ISSUES_stale-mongo-pool-and-cicd-overrebuild.md](docs/INFRA_ISSUES_stale-mongo-pool-and-cicd-overrebuild.md)). Open issue, do not "fix" by silently bumping.

### Gradle module layout
[backend-master/settings.gradle](backend-master/settings.gradle) defines what's actually built. Modules listed there **build from source**; modules referenced only as fat JARs in `precompiled/` (`VinGameBase`, `VinOTP`, `VinPayment`) are commented out — there is **no source for them**. The Dockerfile strips overlapping packages from these fat JARs so they only contribute classes that don't exist in any source module.

## Repo layout (non-obvious bits)

- `backend-master/` — primary Java monorepo (Gradle, JDK 8). All APIs + game servers + shared libs.
- `Backend2Dx/` — **alternate decompiled-source tree** for some games (`api/wspay`, `game/poker`, etc.). Not currently wired into `settings.gradle`. Treat as reference / archive unless you know why you're touching it.
- `backend-master;C/` and `backend-master/_quarantine_*` — temporary backups from `--rebuild` runs and quarantine snapshots. **Don't read or edit these as if they were live source.** They exist for rollback.
- `banca/` — C# .NET 5 fish-shooting game (separate Docker build, separate compose file).
- `api-xsmb-today-main/` — Node 20 lottery scraper (Express + cheerio).
- `ws-bridge/` — Node JSON↔Binary WebSocket protocol bridge (only runs when `CLOUDFLARE_TUNNEL_TOKEN` is set).
- `www/agency-php/` — Laravel agency portal. (`www/admin-php/` is referenced in `start.sh` preflight but lives in a sibling repo, `../sunkr-admin/`.)
- `www/webhook/` — Telegram webhook endpoint.
- `www/webbuild/` — single-page `index.html` for the player landing page.
- `cdn/`, `upload/` — static asset roots served by nginx.
- `install/` — DB backup scripts, MySQL/Hazelcast configs, Flyway directory (not currently used as the migration runner).
- `migrations/` — raw SQL migrations applied manually (e.g. `20260419_add_idx_parent_agent_id.sql`). Numbered by date.
- `tests/` — bash + python integration test runner (see Common Commands).
- `scripts/` — operator scripts (log cleanup, secret rotation, smoke tests, GSC seamless tests, etc.).
- `docs/` — non-trivial design docs and ops runbooks. Many are in Vietnamese. **Read these before changing anything related to:** GSC integration, AWC provider ops, agent hierarchy, RTP/cashback, payment/withdrawal cleanup, production cleanup, backend setup. The `docs/superpowers/specs/` and `docs/superpowers/plans/` paths are referenced from `.gitlab-ci.yml` for design rationale.

## Compose Project Name

`COMPOSE_PROJECT_NAME=sunwinkr` is set in `.env.example` and re-exported by `start.sh` (line 28). This is **load-bearing** — it pins all containers to the `sunwinkr` Compose project regardless of the parent directory's name. The 2026-04-21 outage was caused by a bare `docker compose up` from a renamed directory creating duplicate containers under the wrong project label, which then blocked `./start.sh` recovery. `start.sh`'s `preflight_project_label` function catches this case and aborts. **Do not change this value.**

Note: [deploy.sh:45](deploy.sh#L45) defaults `PROJECT_NAME` to `sunwinkr-backend` (not `sunwinkr`). The two scripts use different project names by design — `deploy.sh` is for the production layout. If you call them in mixed combinations on the same host, you will create two parallel container sets.

## Git workflow — commit directly to `production`

Per operator preference (2026-05-06): **do not create feature branches or open MRs for backend changes**. Work on `production` directly:

```bash
git checkout production && git pull --ff-only
# edit files
git add <files>
git commit -m "<msg>"
git push
```

Then build + recreate the affected service container(s) on the host (see `deploy.sh` flags or the targeted `docker compose build <svc> && up -d --no-deps --force-recreate <svc>` pattern used in recent SUN-* hotfixes).

- `production` is the live deployment branch and the source of truth.
- Skip MR creation — no `glab mr create`, no review gate.
- Cherry-picking between `production` ↔ `staging` only when explicitly asked.
- The CI pipeline on `production` is path-aware (see "CI" below); validate-only stages still run, no gating deploy job.

This trades review hygiene for deploy speed. Operator owns rollback (revert + push + recreate). Do not change this without an explicit instruction otherwise.

## CI

GitLab CI ([.gitlab-ci.yml](.gitlab-ci.yml)) runs **path-aware** deploys on the `production` branch:
- `backend-master/**` or `game/**` → full Java rebuild.
- `www/**` → web-only.
- `nginx/**` → nginx reload.
- `banca/**` → fish game only.
- `install/config/mysql/migrations/**` → SQL migration job.

Validate stage runs on every branch: `validate-api-commands` (parses `api_backend.xml`/`api_portal.xml` for duplicate command IDs — known to be failing on `dev`/`master` with 21 real duplicates that need per-ID domain knowledge to resolve), `yaml-lint`, `java-compile-check`, `php-syntax-check`. Don't widen `validate-api-commands` to production without first cleaning the 21 duplicates.

## Game control feature flags

House-edge / rigging controls live in `.env` (see [README.md](README.md#game-control-feature-flags)) and are read at game-server boot. They include `XOCDIA_FORCE_ENABLED`, `BOT_FUND_MANIPULATION_ENABLED`, `SLOT_RTP_PERCENTAGE`, `BANCA_HACK_ENFORCE`, etc. **Treat these as production-only operator switches**, not as something to flip during normal feature work.

## Backend API contract

Consistent with the parent monorepo CLAUDE.md (`d:/project/mr/CLAUDE.md`):
- Backend API: `POST /api_backend?c=<command_id>&...&aat=<admin_token>` → `{ success, errorCode, data, message }`.
- Portal API: `POST /api?c=<command_id>&...`.
- `data` is a **JSON-encoded string** that callers must double-decode.
- The `aat` token is 32-char hex with an 8-hour TTL, required on all backend calls except `c=701` (login).
- Command-ID definitions live in `backend-master/api/VinPlayBackend/config/api_backend.xml` and `backend-master/api/VinPlayPortal/config/api_portal.xml`. Add new commands there *and* register the processor on the matching `<path>`.

## Documentation index

Key reading before non-trivial changes:
- [docs/BACKEND_SETUP_GUIDE.md](docs/BACKEND_SETUP_GUIDE.md) — local + staging tunnel workflow (Vietnamese).
- [docs/PRODUCTION_CLEANUP_RUNBOOK.md](docs/PRODUCTION_CLEANUP_RUNBOOK.md) — order-sensitive prod data wipe procedure.
- [docs/INFRA_ISSUES_stale-mongo-pool-and-cicd-overrebuild.md](docs/INFRA_ISSUES_stale-mongo-pool-and-cicd-overrebuild.md) — open infra issues (Mongo driver pool recovery, over-rebuild on small CI changes).
- [docs/AWC_PROVIDER_OPS.md](docs/AWC_PROVIDER_OPS.md), [docs/GSC_CALLBACK_SETUP.md](docs/GSC_CALLBACK_SETUP.md), [docs/ref/GSC_Integration.md](docs/ref/GSC_Integration.md) — third-party provider integrations.
- [docs/agent-hierarchy-model.md](docs/agent-hierarchy-model.md) — agent commission tree.
- [docs/api/DOCS_API_DEPOSIT_PROMOTION.md](docs/api/DOCS_API_DEPOSIT_PROMOTION.md), [docs/api/DOCS_API_SIGNING_BONUS.md](docs/api/DOCS_API_SIGNING_BONUS.md) — API contract handovers.
