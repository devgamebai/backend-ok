# Lottery split: extract Lô Đề out of `sunwinkr-banca` into its own service

**Status:** Proposed (2026-05-10)
**Owner:** TBD
**Effort:** 4–5 working days

## Why

The Lô Đề (Loto) game lives inside the `sunwinkr-banca` .NET container, which is named and conceptually owned by the BanCa fish-shooting game. This caused 2+ hours of wrong-system tracing today (we patched Java `LotteryModule` thoroughly before realising the actual code was C# in BanCa). The pain points:

- Subdomain `wbanca.sunkr.club` serves Loto, not just BanCa
- File path `banca/Core/Libs/Loto/` reads like "BanCa's Loto" but it's a complete unrelated game
- No documentation links Lô Đề → BanCa container
- Logs at `logs/banca/` mix both games' output
- Single restart kills BanCa to deploy a Loto change (and vice versa)
- Shared in-memory caches (`payRateCache`, `winRateCache`, `userCashCache`, `LobbyConfig`) couple the games' lifecycles

## Goal

`sunwinkr-loto` runs Lô Đề end-to-end. `sunwinkr-banca` only runs BanCa. They share Redis + MySQL + user session, nothing else. Cutover is zero-downtime for both games.

## Non-goals

- Rewriting Lô Đề in Java to match the rest of the platform — keep .NET
- Changing the WebSocket / MessagePack protocol — Cocos client keeps working as-is
- Migrating the Loto Cocos client UI
- Renaming `cgame.loto_*` tables — schema stays put
- Touching the bonus / reward formulas (already done via Redis overrides on 2026-05-10 — see `loto_pay_rate_*` / `loto_win_rate_*` keys)

## Current state — files & dependencies

**Loto-only code (safe to move):**

```
banca/Core/Libs/Loto/                              LotoGame.cs and helpers (cmd routing for LOTO1–LOTO10)
banca/Core/Libs/Database/LotoSql.cs                rate logic (GetPayRate/GetWinRate) + bet persistence (AddPlayRequest/AddResult)
banca/ScanLoto/                                    XSMB result scraper (likely separate process — verify in Phase 0)
```

**Shared with BanCa (need to copy or factor into a shared lib):**

```
banca/Core/Libs/Database/RedisManager.cs           User_Cash wallet, IncEpicCash — LotoSql calls into this
banca/Core/Libs/Database/SqlLogger.cs              LogTransaction
banca/Core/Libs/Network/CombinedServer.cs          WS+Litenet host
banca/Core/Libs/Network/WebsocketServer.cs         MessagePack frame routing
banca/Core/Libs/Logic/MsgPackEncoder.cs            shared codec
banca/Core/Libs/Logic/MsgPackDecoder.cs            shared codec
banca/Core/Libs/Logic/SimpleJSON.cs                shared JSON helper
banca/Core/Libs/{User,Lobby,Login}*.cs             user/session model — Loto needs CheckLogin
```

**Data stores (already shared, no change):**

| Store | Key / Table | Owner |
|---|---|---|
| MySQL | `cgame.loto_request`, `loto_result`, `loto_gamemode` | Loto-only |
| MySQL | `cgame.users` | shared (identity) |
| Redis | `User_Cash:<userId>` | shared wallet |
| Redis | `loto_pay_rate_*`, `loto_win_rate_*` | Loto-only (rate overrides) |

**Routing today:**

```
wbanca.sunkr.club → sunkr-fe-nginx → sunwinkr-banca:2083 (.NET WS server)
                                       └─ same host: BanCa + Loto routes muxed
```

## Architecture target

```
                 Internet
                    ↓ Cloudflare
              ┌─────┴──────────┐
              ↓                ↓
      wbanca.sunkr.club   wloto.sunkr.club    ← new subdomain
              ↓                ↓
       sunwinkr-banca   sunwinkr-loto         ← new container, .NET, port 2084
       (fish only)      (lottery only)
              │                │
              └────┬───────────┘
                   ↓
          Redis (User_Cash, rates) + MySQL (cgame.*, users)
                shared, no schema change
```

## Phased rollout

### Phase 0 — audit & freeze (½ day, read-only)

- [ ] Grep `banca/` for every `LotoSql.`, `LotoGame.`, `loto_request`, `loto_result`, `loto_pay_rate_`, `loto_win_rate_` reference; produce a complete dependency list
- [ ] Confirm BanCa's main entrypoint never calls into Loto code (and vice versa) — if it does, those couplings need refactoring before split
- [ ] Confirm `cgame.loto_*` tables are NOT also written by BanCa code paths
- [ ] Confirm `ScanLoto/` (XSMB result scraper) — does it run as part of banca's process, or as a separate scheduled job? Move it with Loto if same process
- [ ] Check `LobbyConfig` for Loto-specific settings; either duplicate the relevant subset or extract to a shared library
- [ ] **Decision point:** factor the shared code (`RedisManager`, MsgPack codec, User auth) into a `Sunkr.SharedRuntime` library project, OR just duplicate it into the new `loto-service/` repo. Recommend duplicate for speed; refactor later

### Phase 1 — create the new project (1 day)

- [ ] Create `loto-service/` directory at repo top level (peer to `banca/`, `backend-master/`)
- [ ] Copy `banca/Core/Libs/Loto/`, `banca/Core/Libs/Database/LotoSql.cs`, `banca/ScanLoto/` (if applicable) into it
- [ ] Copy the shared dependencies (RedisManager, SqlLogger, NetworkServer, MsgPack codec, User/Lobby model) into `loto-service/Shared/`
- [ ] Build a thin `Program.cs` entrypoint that:
  - Initialises Redis + MySQL connections (from same `.env`)
  - Starts `WebsocketServer` on a new port (2084)
  - Registers ONLY the LOTO* routes
  - Starts the XSMB result scheduler (if applicable)
- [ ] Write `loto-service/Dockerfile` (same .NET 3.1 base as BanCa)
- [ ] `dotnet publish -c Release -o out` builds cleanly

### Phase 2 — new compose service (½ day)

- [ ] Create `docker-compose.loto.yml` (peer to `docker-compose.lottery.yml` — note that one is just the XSMB API, this one is the lottery server itself)
- [ ] Service `loto-service`:
  - `container_name: sunwinkr-loto`
  - `networks: sunkr-games, sunkr-database, sunkr-messaging`
  - `ports: 2084` (WS), maybe 8889 (litenet/admin) — avoid colliding with banca
  - mounts: `./loto-service:/app`, `./logs/loto:/app/logs` (separate log dir)
  - healthcheck: TCP on 2084
  - `depends_on: redis, mysql`
- [ ] Add `docker-compose.loto.yml` to the master `start.sh all` set
- [ ] Spin up: `./start.sh loto` works, container becomes healthy, listens on 2084

### Phase 3 — parallel routing (1 day, zero-downtime safe)

Run BOTH the old (banca) and new (loto-service) lottery routes simultaneously. Cocos client still hits wbanca. Confirm new service works via internal test:

- [ ] Add `wloto.sunkr.club` to `sunkr-nginx/tunnel-config.yml` ingress
- [ ] Add `sunkr-nginx/nginx/wloto.conf`:

  ```nginx
  server {
      listen 80;
      server_name wloto.sunkr.club;
      resolver 127.0.0.11 valid=30s;
      location / {
          set $gs sunwinkr-loto:2084;
          proxy_pass http://$gs;
          proxy_http_version 1.1;
          proxy_set_header Upgrade $http_upgrade;
          proxy_set_header Connection "upgrade";
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_read_timeout 86400;
      }
  }
  ```

- [ ] Add to `docker-compose.yml` mount list, reload nginx
- [ ] Internal test from CDP: open the Cocos client, override its Loto WS URL via JS injection (`window.LOTO_WS_URL = 'wss://wloto.sunkr.club/'`) — verify LOTO1–LOTO10 work end-to-end against `sunwinkr-loto`
- [ ] Compare a side-by-side bet: place identical bet against `wbanca` (old) and `wloto` (new); confirm both write to `cgame.loto_request`, both debit Redis correctly

### Phase 4 — client cutover (½ day, behind a feature flag)

- [ ] Patch the Cocos `webbuild/assets/Loto/index.fe907.js` (or its pre-built source) to read the lottery WS URL from a runtime config variable instead of being hardcoded to wbanca
- [ ] Default to `wbanca` for safety, but allow override via `LOTO_USE_NEW_HOST=1` localStorage flag
- [ ] Roll out the Cocos build with the toggle defaulted OFF
- [ ] Manually flip the flag for a small set of testers (ops accounts) for 24h
- [ ] If clean: swap default to `wloto`; ship next Cocos build
- [ ] Monitor for: any unexpected disconnects, missing settle, stuck bets

### Phase 5 — remove from banca (½ day)

After 1 week of clean wloto operation:

- [ ] Delete `banca/Core/Libs/Loto/` and `banca/Core/Libs/Database/LotoSql.cs`
- [ ] Remove LOTO* route registration from BanCa's `WebsocketServer` setup
- [ ] Rebuild + redeploy `sunwinkr-banca` — verify BanCa fish game still works
- [ ] Remove `wbanca.sunkr.club` lottery routing from nginx — `wbanca` becomes BanCa-only as the name suggests
- [ ] Rotate Cocos client one last time to remove the toggle (force-use wloto)
- [ ] Update `CLAUDE.md` / docs to reflect the split

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| **Shared in-memory cache** (`payRateCache`, `winRateCache`, `userCashCache`) splits across containers — admin updates rate via UpdatePayRate write to Redis but only refresh the LOCAL container's memory | Both containers read Redis on cache miss; the rate update will eventually propagate within the cache TTL. Or: add a Redis pub/sub on rate change to invalidate all caches. |
| **Settle scheduler runs in banca currently** — moving it means a window where neither container settles | Move the scheduler atomically with the Loto code in Phase 1; activate in Phase 2 before cutover. Verify by checking `cgame.loto_request.Status` transitions resume. |
| **`User_Cash:<id>` Redis race** — both old + new containers debit the same key during parallel-running phase | Acceptable: `IncEpicCash` uses Redis atomic INCR; concurrent debits from both servers are atomic. Just don't run double-bet handlers simultaneously for the same user. |
| **MySQL connection limit** — adding a second container doubles the connection pool | Either reduce per-pool size, or bump `max_connections` on MySQL. Audit current `cgame` pool usage first. |
| **Mid-flight rounds at cutover** — players betting on `wbanca` when DNS flips | Use the client-side feature flag (Phase 4) instead of DNS flip. Each player switches at their next reconnect. No mid-round disruption. |

## Rollback plan

At every phase, rollback is "revert the last change":

- **Phase 1–2:** `docker compose -p sunwinkr stop loto-service; docker compose rm loto-service` — no production effect
- **Phase 3:** remove `wloto.conf`, reload nginx — old wbanca path unchanged
- **Phase 4:** ship Cocos build with default = wbanca; tag previous client image as `:before-loto-split`
- **Phase 5:** keep the old Loto code on a `pre-loto-split` git tag; revert + rebuild banca if needed

## Acceptance criteria

- [ ] `sunwinkr-loto` container runs only Lô Đề; `sunwinkr-banca` contains zero Loto code
- [ ] Lottery bets (any mode, any channel) place + settle identically before and after split (verify by replay of recent settle cycles)
- [ ] `cgame.loto_request` schema unchanged; bets continue writing with snapshotted `PayRate`
- [ ] `User_Cash:<id>` Redis behavior unchanged; debit + credit happen exactly once per bet
- [ ] Independent deploys: restarting `sunwinkr-loto` doesn't disturb BanCa, and vice versa
- [ ] Logs cleanly separated — `logs/loto/` for lottery, `logs/banca/` for fish game
- [ ] `CLAUDE.md` updated with the new container, naming, route table

## Open questions (resolve in Phase 0)

1. Does the BanCa fish-game code call any `LotoSql.` method? (probably not, but grep)
2. Is `ScanLoto/` a separate executable or part of the main banca process?
3. What's the actual coupling to `LobbyConfig` (e.g., `CashSaveMin`)? Can the Loto service have its own config singleton?
4. Are there scheduled jobs (cron / hangfire / .NET Timer) for settle/result-scrape that run inside banca? Where do they currently fire?
5. Does `Lobby.CheckLogin` work the same way from a separate container? (Should — Redis-backed session — but verify.)

## Background — what we learned in the 2026-05-10 trace

The team reported "lottery rate didn't change after CSV update" and we initially patched the Java `backend-master/game/Minigame/src/main/java/game/modules/minigame/LotteryModule.java` (assuming Lô Đề lived in the unified Java game-server like the other 16 games). After multiple builds and bytecode verifications confirmed the Java change was deployed but had zero effect, we used Chrome DevTools Protocol (CDP) to capture the actual WebSocket traffic from the player client. We discovered:

- The lottery's WebSocket URL is `wss://wbanca.sunkr.club/` (not `wmini.sunkr.club` or any "loto"-named subdomain)
- The protocol is **MessagePack** with `route` keys (not the BitZero binary protocol used by the Java game servers)
- Routes are `LOTO1` (place bet) through `LOTO10` (recent bets), plus `OnUpdateJackpot` and `onLOTO7` (chat) push events
- The actual handler is `banca/Core/Libs/Loto/LotoGame.cs` (.NET, in `sunwinkr-banca`)

Today's quick fix used the existing Redis override mechanism (`UpdatePayRate` writes `loto_pay_rate_<mode*100+channel>` keys; `GetPayRate` reads Redis before falling back to hard-coded defaults). Setting these keys + restarting the banca container was a 30-second deploy with no rebuild.

But the underlying confusion (lottery hidden inside the BanCa container) remains. This plan splits them so the next engineer who needs to touch lottery code can find it immediately.
