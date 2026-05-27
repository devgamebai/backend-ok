# Sunwinkr Casino Platform

A full-stack online casino/gaming platform with 17+ games, real-time multiplayer, payment processing, and admin management.

## Architecture

```
                        Internet
                           |
                    [ Cloudflare Tunnel ]
                           |
                    [ Nginx :8088 ]
                           |
            +--------------+--------------+
            |              |              |
      [ Admin PHP ]  [ Portal API ]  [ Static Assets ]
      CodeIgniter     Java Jetty        CSS/JS/Images
            |         port 8081
            |              |
     +------+------+------+------+
     |      |      |      |      |
  [MySQL] [Mongo] [Redis] [RMQ] [Hazelcast]
     |
     +------ 16 Game Servers (TCP/WebSocket) ------+
     |                                              |
  [ BanCa Fish Game .NET ]              [ Lottery API Node.js ]
```

## Quick Start

### Prerequisites

- Docker Engine 24+
- Docker Compose v2+
- 8GB+ RAM recommended

### One-Command Deploy

```bash
# Deploy with Cloudflare tunnel (staging)
export CLOUDFLARE_TUNNEL_TOKEN=eyJhIj...
export PLAY_DOMAIN=staging-play.sunkr.bet    # optional, this is the default
export ADMIN_DOMAIN=staging-admin.sunkr.bet  # optional, this is the default
./deploy.sh

# Deploy with custom domains
export CLOUDFLARE_TUNNEL_TOKEN=eyJhIj...
export PLAY_DOMAIN=play.example.com
export ADMIN_DOMAIN=admin.example.com
./deploy.sh

# Deploy without Cloudflare (local access on port 8088)
./deploy.sh
```

The deploy script handles everything automatically:
1. Generates `.env` with strong random passwords
2. Builds Java backend (JDK 8 via Docker)
3. Builds BanCa fish game (.NET 5 via Docker)
4. Creates `libs/` symlink for classpath
5. Fixes file permissions
6. Configures nginx with your domains
7. Creates Docker networks and volumes
8. Starts databases and loads `full_backup.sql`
9. Creates MySQL application user
10. Patches `game_config` for missing fields
11. Starts all 33 services (including Cloudflare tunnel if token is set)

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PLAY_DOMAIN` | `staging-play.sunkr.bet` | Game client domain |
| `ADMIN_DOMAIN` | `staging-admin.sunkr.bet` | Admin panel domain |
| `CLOUDFLARE_TUNNEL_TOKEN` | *(none)* | Enables Cloudflare tunnel + WS-Bridge when set |

### deploy.sh Commands

```bash
./deploy.sh              # Full deployment (build + setup + start)
./deploy.sh --rebuild    # Force rebuild Java and .NET projects
./deploy.sh --no-start   # Build and setup only, don't start services
./deploy.sh stop         # Stop all services
./deploy.sh status       # Show service status
./deploy.sh logs [svc]   # Follow logs (optionally for specific service)
```

### start.sh Commands

For starting individual service tiers without the full build/setup:

```bash
./start.sh database     # Start databases only
./start.sh backend      # Start databases + backend APIs
./start.sh games        # Start databases + backend + game servers
./start.sh web          # Start databases + backend + web tier
./start.sh banca        # Start databases + fish game
./start.sh all          # Start everything
./start.sh stop         # Stop all services
./start.sh status       # Show service status
./start.sh logs [svc]   # Follow logs
```

### Verify

```bash
./deploy.sh status

# Test login API
curl -s "http://localhost:8088/api?c=3&un=superadmin&pw=$(echo -n admin123 | md5sum | cut -d' ' -f1)"
```

## Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| **Game Client** | `https://<PLAY_DOMAIN>` | — |
| **Admin Dashboard** | `https://<ADMIN_DOMAIN>/admin/login` | `superadmin` / `admin123` |
| **Portal API** | `https://<PLAY_DOMAIN>/api?c=9` | — |
| **Health Check** | `https://<PLAY_DOMAIN>/health` | — |

Without Cloudflare tunnel, access via `http://<server-ip>:8088/`.

## Services Overview

| Tier | Services | Count |
|------|----------|-------|
| **Database** | MySQL 8.0, MongoDB 7.0, Redis 7, RabbitMQ 3.13, Hazelcast 3.12 | 5 |
| **Backend API** | Portal API, Backend API, Payment API, VBee | 4 |
| **Game Servers** | Poker, TLMN, Xoc Dia, Slots, Minigame, BanCa, etc. | 17 |
| **Web** | Nginx, Admin PHP, Agency PHP, Webhook | 4 |
| **Staging** | Cloudflare Tunnel, WS-Bridge | 2 |
| **Total** | | **32** |

## Game Servers

All 16 game server containers start and connect to Hazelcast/MySQL. Games require the Cocos Creator client to be playable — game servers are headless.

| Game | Type | Port | WebSocket Path |
|------|------|------|---------------|
| Poker (Texas Hold'em) | Card | 1743 | `/ws/poker` |
| Poker Tournament | Card | 1738 | `/ws/pokertour` |
| Tien Len Mien Nam | Card | 2143 | `/ws/tlmn` |
| Lieng (3-Card Brag) | Card | 1543 | `/ws/lieng` |
| Binh (13-Card) | Card | 1243 | `/ws/binh` |
| Bai Cao | Card | 1143 | `/ws/baicao` |
| Ba Cay (Baccarat) | Card | 1043 | `/ws/bacay` |
| Sam | Card | 1943 | `/ws/sam` |
| Coup | Card | 2443 | `/ws/coup` |
| Xi Zach (Blackjack) | Card | 2243 | `/ws/xizach` |
| Xoc Dia (Dice) | Dice | 2343 | `/ws/xocdia` |
| Slot Machine | Slot | 1843 | `/ws/slot` |
| Minigame (Bau Cua, Tai Xiu) | Arcade | 1643 | `/ws/minigame` |
| Caro (Gomoku) | Board | 1343 | `/ws/caro` |
| Co Tuong (Chinese Chess) | Board | 1443 | `/ws/cotuong` |
| Ban Ca (Fish Shooting) | Arcade | 2083 | `/ws/banca` |

## Known Issues

| Issue | Severity | Details |
|-------|----------|---------|
| **No player client in repo** | Blocker | Game client (Cocos Creator 2.4.4) built separately. Backend is ready but games can't be played without it. |
| **Missing game_config entries** | Low | Some DB config entries expected by `GameCommon.init()` are not in `full_backup.sql`. The init method catches errors per section and logs warnings — non-critical features (VTC Pay, ePay, NganLuong, etc.) won't work. |
| **Payment API incomplete** | Low | `CreateWithdrawProcessor` source not available. Payment via third-party gateways disabled. |


### Infrastructure Security

- [x] All credentials moved to `.env` (~50 environment variables)
- [x] 5 isolated Docker networks (frontend, backend, games, database, messaging)
- [x] Container names prefixed with `sunwinkr-`
- [x] Redis requires authentication
- [x] MySQL uses `mysql_native_password` auth plugin
- [x] Hazelcast pinned to 3.12 for client compatibility

## Game Control Feature Flags

House-edge and game rigging controls are configurable via `.env`:

```env
GAME_FORCE_ENABLED=true          # Master switch
XOCDIA_FORCE_ENABLED=true        # Xoc Dia result forcing
XOCDIA_DEFAULT_FORCE_TYPE=-1     # -1=random, 0=even, 1=odd
BOT_FUND_MANIPULATION_ENABLED=true
BOT_AUTO_JOIN_ENABLED=true
BOT_DEFAULT_BALANCE=1000000
SLOT_RTP_PERCENTAGE=48           # Return-to-player percentage
TAIXIU_HOUSE_EDGE_ENABLED=true
BANCA_JACKPOT_CONTROL_ENABLED=true
BANCA_HACK_ENFORCE=false         # Kick cheaters vs log-only
```

## Project Structure

```
sunwinkr/
├── backend-master/            # Java game servers + APIs (Gradle, JDK 8)
│   ├── api/                   # Portal, Backend, VBee, Payment
│   ├── game/                  # 16 game server projects
│   ├── VbeeCommon/            # Shared: BaseController, enums, models
│   ├── VinPlayDAL/            # Shared: MySQL data access layer
│   ├── VinPlayUserCore/       # Shared: User service, GameCommon
│   ├── BitzeroAll/            # Game server framework (TCP/WebSocket)
│   ├── precompiled/           # Original JARs (no source, safe from gradle clean)
│   ├── libs/
│   │   ├── runtime/           # Third-party deps + rebuilt shared libs
│   │   ├── app/               # Rebuilt service JARs (APIs)
│   │   └── api-precompiled/   # Old API-only JARs (loaded last as fallback)
│   ├── config/                # Symlinks to API config dirs
│   └── entrypoint.sh          # Docker config templating (env var substitution)
├── banca/                     # C# .NET fish shooting game
├── www/
│   ├── admin-php/             # CodeIgniter admin dashboard
│   ├── agency-php/            # Laravel agency portal
│   └── webhook/               # Telegram webhook
├── ws-bridge/                 # WebSocket JSON↔Binary protocol bridge
├── api-xsmb-today-main/       # Node.js lottery scraper
├── nginx/                     # Reverse proxy config
├── install/                   # DB backups, Hazelcast/MySQL configs
├── docker-compose*.yml        # 8 compose files
├── deploy.sh                  # One-command deployment script
├── start.sh                   # Service tier orchestration
├── .env.example               # Environment template (~50 vars)
└── docs/
    └── API_DOCUMENTATION_MOBILE.md
```

### Backend JAR Architecture

The Java backend uses a **3-tier classpath** to handle the mix of rebuilt (from decompiled source) and original pre-compiled JARs:

```
Classpath priority (first wins):
  1. libs/app/*              ← Rebuilt service JARs (our bug fixes)
  2. libs/runtime/*          ← Third-party deps + rebuilt shared libs
  3. libs/api-precompiled/*  ← Original JARs for classes without source (fallback)
```

Game servers use `game/xxx/build/libs/*` instead of `libs/app/*` because the BitZero framework derives config paths from the JAR location.

Pre-compiled original JARs are stored in `precompiled/` (safe from `gradle clean`) and copied to `libs/` during deployment.

## Engineering Principles

**Two non-negotiables: consistency and latency.** Every change to money-touching code is judged against these — and against the money-ledger pattern that enforces them.

### Money-ledger pattern (load-bearing)

Any code that touches a player balance, agency wallet, credit wallet, deposit/withdraw counter, commission, or any cumulative ledger column **must** go through `MoneyGateway` (or its sibling reporter classes like `DepositReportAggregator`). The pattern, in short:

1. **Single source of truth — MySQL primary.** Hazelcast / Redis / Mongo are caches. Mutate MySQL first, then refresh the cache best-effort. Never the other way.
2. **Idempotency by `(tx_id, source[, user_id])`** — every write carries a transaction key that makes a retry safe. Duplicate writes return the prior result, never inflate the ledger.
3. **Atomic commit per logical operation.** Use a single `Connection` with `setAutoCommit(false)` for any update that spans more than one statement (balance + audit row, balance + aggregate column, etc.). Roll back on any error.
4. **Audit row before reply.** Each money mutation lands in `money_gateway_log` (or the per-feature audit table — `log_admin`, `deposit_audit_logs`, `tg_*_audit`). The audit row is part of the same operation, not a fire-and-forget afterthought.
5. **Aggregate / report columns ride the same idempotency gate.** When a flow has a "pending → approved" guard (e.g. `WHERE status='PENDING'`), use that guard as the once-per-row trigger. Don't invent a parallel ledger.
6. **Failures log, don't propagate.** A wallet credit must never fail because the audit row INSERT hiccuped. Log the audit failure with the credit's identifying tuple so an operator can reconcile manually — but never roll back a successful credit because of an audit-side blip.

If you find a path that mutates money/aggregates without going through the gateway, treat it as a bug. The fix is to route it through the gateway, not to add a parallel write.

### When refactoring to align with the pattern

Refactors are **encouraged** as long as they bring code under the ledger pattern without breaking it. Use this rubric:

| Scope | Action |
|-------|--------|
| **Small** — single file / single endpoint / new helper class wrapping legacy code | Just do it. PR with the ledger-pattern alignment, ship behind the usual review. |
| **Medium** — single feature area (deposit, withdraw, commission) touched in 5–10 files, no schema change | PR with a clear before/after comparison, link the ledger principle this enforces. Reviewer checks idempotency + audit. |
| **Huge** — schema migration, cross-service rename, multi-day diff, behaviour changes for >1 feature, anything that touches `MoneyGateway` itself | **Stop and discuss first.** Open a doc/issue describing scope, blast radius, rollback plan. Get sign-off from someone who owns the affected area. Only then start the work. |

The "huge" gate exists because the ledger is the one part of this codebase that **cannot** silently regress — a bad change here mints or burns money under operators' noses. The discussion isn't a tax on velocity; it's the cheapest way to keep the cost of a bad refactor bounded.

### Latency posture

- **Hot path (per-bet, per-tick, per-spin)** must read from in-process / Hazelcast caches, never DB. If a new check needs DB, route it through a periodic refresh into Hazelcast first.
- **Mutations on the hot path** are still routed through the ledger, but additional book-keeping (audit, daily roll-up, push notification) goes off the request thread or via RabbitMQ.
- **A latency regression on the hot path is a P0 for a wallet-touching change**, even if logic is correct.

### Quick checklist before merging a money-touching change

- [ ] Goes through `MoneyGateway` (or a sibling like `DepositReportAggregator`).
- [ ] Idempotency key includes user_id where the source can collide across users.
- [ ] Audit row written in the same operation, failures logged not raised.
- [ ] No DB query added to a per-tick game loop.
- [ ] Backfill SQL provided when the change introduces a new aggregate column or status, with a one-liner stating "rerun-safe".
- [ ] Smoke test that demonstrates the column / response field actually populates after the flow runs end-to-end.

## Documentation

| Document | Description |
|----------|-------------|
| [API Documentation](docs/API_DOCUMENTATION_MOBILE.md) | Complete API reference — endpoints, auth flow, game server protocol, code samples |
| [Security Audit](docs/SECURITY_AUDIT_AND_PLATFORM_ANALYSIS.md) | Full security audit with vulnerability details and remediation status |

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Game Servers | Java (Gradle) | JDK 8 |
| Fish Game | C# .NET Core | 5.0 |
| Admin Panel | PHP CodeIgniter | 3.x |
| Agency Portal | PHP Laravel | 8.x |
| Lottery API | Node.js | 20 |
| Database | MySQL | 8.0 |
| Document Store | MongoDB | 7.0 |
| Cache | Redis | 7 |
| Message Queue | RabbitMQ | 3.13 |
| Clustering | Hazelcast | 3.12 |
| Reverse Proxy | Nginx | 1.25 |
| Tunnel | Cloudflare | latest |
| Container Runtime | Docker Compose | v2 |
