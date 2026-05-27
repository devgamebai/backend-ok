# Wallet Phase 5c — BanCa Sub-Games + Admin Migration

**Status:** implemented behind kill switch `BANCA_USE_UNIFIED_WALLET` (default OFF).
**Predecessor:** `WALLET_PHASE5B_BANCA_GAME_LOOP_IMPL.md`
**Follow-on:** Phase 5d (shadow + reconcile), 5e (blue/green soak).

## Scope

Replace every legacy `IncEpicCash` / `IncEpicCashCache` / `SaveCashToDb`
call across BanCa's sub-games, payment callbacks, and CMS-admin paths
with the unified `MoneyGatewayClient.SettleAsync`. After this PR BanCa
producer code **must not** touch `cgame.users.cash` or `User_Cash:{id}`
on the write path. The columns are already gone from the staging DB.

The legacy code paths are preserved as the `else` branch of every site
so `BANCA_USE_UNIFIED_WALLET=0` still works for emergency rollback;
they themselves are downgraded to "Redis cache only — no MySQL write"
because `MySqlUser.SaveCashToDb` is now a no-op (column dropped).

## Per-site before/after

| File:Line (orig) | Site | Before | After (flag=1) |
|---|---|---|---|
| `GameBanCa.cs:1410` | Solo fee debit, slot p0 | `IncEpicCashCache(epicId,-cashToJoin,SOLO_FEE)` | `MoneyGatewayClient.SettleAsync(epicId,-cashToJoin,"bc-solo-fee-{epicId}-{worldId}-p0","WAGER_DEBIT_BANCA")` |
| `GameBanCa.cs:1491` | Solo fee debit, slot p2 | same | same with `-p2` suffix |
| `GameBanCa.cs:1920` | Solo refund (game not started) | `IncEpicCashCache(p.Id,+cashToJoin,SOLO_FEE)` + `SaveCashToDb` | `MoneyGatewayClient.SettleAsync(p.Id,+cashToJoin,"bc-solo-refund-{pid}-{worldId}","WAGER_CREDIT_BANCA")` |
| `GameBanCa.cs:399/404` | Solo END draw refund (both players) | `IncEpicCash` ×2 + `SaveCashToDb` ×2 | `MoneyGatewayClient.SettleAsync` ×2 (`"bc-solo-end-draw-{id1|id2}-{worldId}"`, `WAGER_CREDIT_BANCA`) |
| `GameBanCa.cs:427` | Solo END winner credit | `IncEpicCash(winId,winCash,…)` + `SaveCashToDb` | `MoneyGatewayClient.SettleAsync(winId,winCash,"bc-solo-end-win-{winId}-{worldId}","WAGER_CREDIT_BANCA")` |
| `LobbyService.cs:259/269/318` | `xxengCashin` / `xxengCashout` / rollback | `IncEpicCash` mirror + `EpicApi.xxengCashIn` | Skip BanCa-side mirror entirely; portal moves PLAYER_VIN directly. `newCash` returned as `0` (FE re-fetches balance). |
| `LobbyService.cs:757` | DAILY_CASH_REGISTER bonus | `IncEpicCash(uid,+DailyCash,NEW_ACCOUNT)` | `SettleAsync(uid,+DailyCash,"bc-daily-newacc-{uid}-{deviceId}","EMERGENCY_BANCA")` |
| `LobbyService.cs:1019` | DAILY_CASH_QUICKLOGIN | same | `"bc-daily-ql-{uid}-{deviceId}"` |
| `LobbyService.cs:1158` | DAILY_CASH_LOGINFB | same | `"bc-daily-fb-{uid}-{deviceId}"` |
| `LobbyService.cs:1614` | Cancel cashout (CANCEL_CASH_OUT) | `IncEpicCash(uid,+price)` | `SettleAsync(uid,+price,"bc-cancel-cashout-{uid}-{transId}","EMERGENCY_BANCA")` |
| `LobbyService.cs:1733,1745` | Telco cashout debit + remain-check refund | `IncEpicCash(uid,-cashpay,CASH_OUT)` + remain refund | `SettleAsync(uid,-cashpay,"bc-cashout-telco-{uid}-{ts}","WAGER_DEBIT_BANCA")`. Remain-check refund is now skipped under unified (server-side handles low-balance via MoneyGateway response). |
| `LobbyService.cs:2837,2849` | Bank cashout debit + remain | same as telco | `"bc-cashout-bank-{uid}-{ts}"` |
| `LobbyService.cs:3245` | Player card-charge | `IncEpicCash(uid,+add,CARD_IN)` + `SaveCashToDb` | `SettleAsync(uid,+add,"bc-pay-cardchg-{uid}-{add}-{ts}","EMERGENCY_BANCA")` |
| `OneTwoThreeBoard.cs:251` | OTT disconnect refund | `IncEpicCash(uid,+blind,ONE_TWO_THREE_PAY)` | `SettleAsync(uid,+blind,"bc-123-refund-disc-{uid}-{ts}","WAGER_CREDIT_BANCA")` |
| `OneTwoThreeBoard.cs:413` | OTT bet debit | `IncEpicCash(uid,-blind,ONE_TWO_THREE_PAY)` | `SettleAsync(uid,-blind,"bc-123-bet-{uid}-{ts}","WAGER_DEBIT_BANCA")` |
| `OneTwoThreeBoard.cs:466` | OTT cancel refund | same as :251 | `"bc-123-refund-cancel-{uid}-{ts}"` |
| `LotoGame.cs:148` | Loto bet debit | `IncEpicCash(uid,-cost,LOTO_PAY)` | `SettleAsync(uid,-cost,"bc-loto-bet-{uid}-{mode}-{ts}","WAGER_DEBIT_BANCA")` |
| `BanCaService.cs:296,336` | `/bancaapi/addCash` + `/updateCash` CMS admin | `IncEpicCash + SaveCashToDb` | `SettleAsync(uid,+changeCash,"bc-cms-addcash/updcash-{uid}-{ts}","EMERGENCY_BANCA")` |
| `BanCaService.cs:1244` | smscallback (SMS_IN) | `IncEpicCash + SaveCashToDb` | `SettleAsync(uid,+smsItem.Cash,"bc-pay-sms-{uid}-{totalAmount}-{ts}","EMERGENCY_BANCA")` |
| `BanCaService.cs:1313` | momocallback (MOMO_IN) | `IncEpicCash + SaveCashToDb` | `"bc-pay-momo-{momo_transId}-{uid}"` |
| `BanCaService.cs:1425` | muacardcallback (CARD_IN) | same | `"bc-pay-muacard-{tranid}-{uid}"` |
| `BanCaService.cs:1510` | push247callback | same | `"bc-pay-push247-{tranid}-{uid}"` |
| `BanCaService.cs:1587` | card-charge-callback | same | `"bc-pay-cardcharge-{uid}-{add}-{ts}"` |
| `BanCaService.cs:1664` | vin-pay-callback | same | `"bc-pay-vinpay-{tranid}-{uid}"` |
| `BanCaService.cs:2140` | coinpaymentipn | same | `"bc-pay-coin-{uid}-{txn_id}"` |
| `MySqlUser.cs:21` | `SaveCashToDb` | raw `UPDATE users SET cash=?` | **no-op** (column dropped) |
| `MySqlUser.cs:516` | `GetUserCash` | `SELECT cash FROM users` | **returns 0** (column dropped) |
| `RedisManager.cs:1316` | `IncEpicCash` | full body | doc-only banner — body unchanged; never calls `SaveCashToDb` now since callee is a no-op |
| `RedisManager.cs:1401` | `IncEpicCashCache` | full body | doc-only banner — body unchanged |

## Source-type mapping table

| Logical action | `tx_type` |
|---|---|
| Game bet (solo fee, OTT bet, Loto bet) | `WAGER_DEBIT_BANCA` |
| Game refund / win / kill payout / OTT refund | `WAGER_CREDIT_BANCA` |
| Player withdrawal (telco / bank cash-out) | `WAGER_DEBIT_BANCA` |
| Cancel withdrawal | `EMERGENCY_BANCA` |
| Daily login / register bonus | `EMERGENCY_BANCA` |
| CMS admin add/update cash | `EMERGENCY_BANCA` |
| Payment gateway deposit (SMS, MoMo, card, vinpay, coinpayment, card-charge) | `EMERGENCY_BANCA` |
| Crash-recovery flush (Revive) | `EMERGENCY_BANCA` (Phase 5b, unchanged) |
| Session settle on quit (Phase 5b, unchanged) | `WAGER_CREDIT_BANCA` if profit ≥ 0 else `WAGER_DEBIT_BANCA` |

The Java side (`c=9998` BanCaSettleProcessor) accepts all three
`WAGER_DEBIT_BANCA` / `WAGER_CREDIT_BANCA` / `EMERGENCY_BANCA` and
honours `external_ref` dedupe.

## Files changed

| File | Status | Net |
|---|---|---|
| `banca/Core/Libs/Database/EpicSql/Genneral/MySqlUser.cs` | `SaveCashToDb`+`GetUserCash` → no-op | -25 / +12 |
| `banca/Core/Libs/Database/RedisManager.cs` | doc banner | +0 / +17 |
| `banca/Core/Libs/Logic/GameBanCa.cs` | 5 sites migrated (solo fee p0, solo fee p2, solo refund, solo end draw, solo end win) | +85 |
| `banca/Core/Libs/Logic/LobbyService.cs` | 8 sites migrated (xxeng×3, daily×3, cashout-cancel, telco-cashout, bank-cashout, player card-charge) | +130 |
| `banca/Core/Libs/OneTwoThree/OneTwoThreeBoard.cs` | 3 sites | +40 |
| `banca/Core/Libs/Loto/LotoGame.cs` | 1 site | +15 |
| `banca/Core/Libs/WebService/BanCaService.cs` | 8 sites (CMS×2, sms, momo, muacard, push247, cardcharge, vinpay, coin) | +110 |
| `banca/Core/UnifiedWalletTests/Phase5cSiteMigrationTests.cs` | NEW — 5 site-contract tests | +220 |
| `docs/WALLET_PHASE5C_BANCA_SUBGAMES_IMPL.md` | NEW | this file |

## Build verification

```
# Baseline (after Phase 5b)
Core.dll        = 1270272 bytes
BanCaLiteNet.dll = 29696 bytes

# After Phase 5c
Core.dll        = 1281024 bytes  (+10752, ~10.5KB)
BanCaLiteNet.dll = 29696 bytes  (unchanged)
```

Build command:

```
docker run --rm -v "$(pwd)/banca:/banca" -w /banca/BanCaLiteNet \
  mcr.microsoft.com/dotnet/sdk:5.0 dotnet publish -c Release -o out
```

Test command:

```
docker run --rm -v "$(pwd)/banca:/banca" -w /banca/Core/UnifiedWalletTests \
  mcr.microsoft.com/dotnet/sdk:5.0 dotnet test
```

Phase 5c test additions (`Phase5cSiteMigrationTests`):
- `SoloFeeDebit_uses_WAGER_DEBIT_BANCA_with_correct_ref`
- `DailyBonus_uses_EMERGENCY_BANCA_with_correct_ref`
- `OneTwoThreeBet_uses_WAGER_DEBIT_BANCA_with_correct_ref`
- `OneTwoThreeRefund_correlates_with_bet_by_timestamp`
- `CashoutTelco_uses_WAGER_DEBIT_BANCA`

Phase 5b test suite (still green): 4 tests.

## Smoke test plan (staging, post-deploy)

1. **Solo fee debit + refund**: Player A enters a solo room, leaves
   before another player joins. Verify Java `bc_settle_log` shows one
   `WAGER_DEBIT_BANCA` (solo fee) and one `WAGER_CREDIT_BANCA`
   (refund) with `external_ref` shapes `bc-solo-fee-*` and
   `bc-solo-refund-*`.
2. **Solo end**: Two players complete one solo round; winner's
   `bc-solo-end-win-*` credit lands once.
3. **OneTwoThree bet + cancel**: Player bets blind 5000, cancels
   queue. Verify net wallet = 0; two ledger entries with matching
   timestamp suffix.
4. **Daily bonus**: New device login → `bc-daily-newacc-*` credit
   exactly once even on duplicate login.
5. **Cash-out cancel**: Cash-out request, then cancel. Verify single
   `bc-cancel-cashout-*` credit.
6. **CMS admin cash adjust**: admin-php → `/bancaapi/addCash/...` →
   verify `bc-cms-addcash-*` entry; calling the same URL twice with
   identical params re-uses `external_ref` so the second call dedupes
   (no double-credit).
7. **Idempotency drill**: stop BanCa mid-settle, restart, ensure
   `banca:failed_settle` replay still dedupes on the Java side.

## Configuration

Inherits all from Phase 5b. No new env vars introduced.

## Rollback

```bash
sed -i 's/^BANCA_USE_UNIFIED_WALLET=.*/BANCA_USE_UNIFIED_WALLET=0/' .env
docker compose -f docker-compose.yml -f docker-compose.banca.yml \
  up -d --force-recreate game-banca
```

WARNING: rolling back to flag=0 after staging has run for any length
of time will write to Redis `User_Cash:{id}` again but never to MySQL
(column dropped). Display numbers in Redis will drift from PLAYER_VIN
because game-loop settles have been hitting MoneyGateway. **Treat the
rollback as a write-off path; don't rely on Redis balances afterward.**

## Open risks (staging soak)

1. **Cashout remain-check skipped under unified.** The legacy
   `CASH_OUT remain not enough` refund branch is suppressed when
   `BANCA_USE_UNIFIED_WALLET=1`. The Java MoneyGateway side must
   enforce min-balance-after-debit on its own. **Pre-prod
   verification required.**
2. **xxeng cashin/out now returns `newCash=0`.** FE relies on this
   value for the post-transfer balance display. FE must re-fetch
   balance via `getUserInfo` after the response. **FE side change
   coordinated separately.**
3. **`MySqlUser.GetUserCash` now returns 0.** Any legacy reader
   (admin reports, cron jobs) that calls this method sees zero. Move
   them to the portal-side PLAYER_VIN lookup.
4. **Bot side may still call `Player.Cash` arithmetic** (hot path
   untouched per Phase 5b). Bots that pay solo fees via the same
   path will hit MoneyGateway — verify Hazelcast service token quota
   handles bot traffic.
5. **`MySqlUser.SaveCashToDb`** is now a no-op even when flag=0, so
   any one-off admin script that called it via REST will silently
   succeed without persisting. **Document for ops.**

## Production prerequisites

1. **Java `c=9998` `BanCaSettleProcessor`** must accept
   `WAGER_DEBIT_BANCA` / `WAGER_CREDIT_BANCA` / `EMERGENCY_BANCA`
   with `external_ref` dedupe live. (Already shipped per task
   context.)
2. **`BANCA_SERVICE_TOKEN`** and `BANCA_MONEYGATEWAY_URL` env vars
   present on the production `game-banca` container.
3. **`banca:failed_settle` replay worker** running (Phase 5b
   follow-up #3) — without it any 3-retry failure builds up in
   Redis indefinitely.
4. **Min-balance enforcement** on the Java side for
   `WAGER_DEBIT_BANCA` requests (Phase 5c removed the C#-side
   `remain` check on cash-out flows).
5. **Stage soak ≥ 7 days** at flag=1 across all sub-games with zero
   `banca:failed_settle` accumulation and zero drift between
   PLAYER_VIN ledger and Redis `User_Cash:{id}` mirror (the latter
   only updated by flag-off code, so drift is expected and
   acceptable — we just want it bounded).

## Phase 5c follow-up — Redis sync (SUN-1062)

**Symptom (staging, 2026-05-11):** with flag=1, the BanCa HUD balance
did not refresh after a kill / quit-settle. The settle itself succeeded
(PLAYER_VIN moved correctly via c=9998) but the next `GetUserCash` call
read a stale `User_Cash:{cgame_uid}` from Redis, because Phase 5c
demoted the legacy Redis write path to a no-op for flag-on traffic.

**Fix (commit summary):**

1. **Java — new `c=9997` `BanCaBalanceQueryProcessor`**
   (`backend-master/api/VinPlayBackend/src/main/java/com/vinplay/api/backend/processors/banca/BanCaBalanceQueryProcessor.java`).
   Returns the canonical PLAYER_VIN balance in milli-VND (for unit
   parity with the legacy `User_Cash:{id}`). Accepts `user_id`
   (vinplay) directly, or `cgame_user_id` with a direct-id fallback
   then a nickname-join fallback against `cgame.users`. Same
   `X-Service-Token` (`BANCA_SERVICE_TOKEN`) guard + same
   `NO_AUTH_COMMANDS` whitelist as c=9998. Registered in
   `api_backend.xml`.

2. **C# — `MoneyGatewayClient.QueryBalanceMilliAsync(userId)`**
   (`banca/Core/Libs/UnifiedWallet/MoneyGatewayClient.cs`).
   Single-attempt HTTP GET, no retry — caller treats `-1` as "fall
   back to legacy Redis".

3. **C# — `RedisManager.GetUserCash` flag-gated unified read.**
   When `BANCA_USE_UNIFIED_WALLET=1` and the per-user 5s in-memory
   cache (`userCashCacheExpiry`) is cold, it hits c=9997, caches
   the answer, and returns it. Failure / disabled flag falls
   through to the legacy Redis path so a backend-api outage
   degrades to stale mirror rather than failed reads.

4. **C# — `MoneyGatewayClient.SettleAsync` also refreshes
   Redis mirror.** On a 2xx response from c=9998 we parse
   `balance_after_vnd`, multiply by 1000 to get milli-VND, and
   write back to `User_Cash:{userId}` (fire-and-forget via
   `RedisManager.SetUserCash`). This keeps cron/admin tools that
   still read the legacy mirror approximately fresh — bounded
   drift only between settle and next-read.

**Cache semantics:**
- HUD hot path (per-frame `Cash` arithmetic in `Player`) is **never**
  touched, exactly as Phase 5b promised.
- Explicit `GetUserCash` calls on the seek paths (`LobbyService.cs`
  lines 64, 111, 158, 2493, 2581; `BanCaServer.cs:1143`;
  `SqlLogger.IncUserCash`) hit c=9997 at most once per 5s per user.
- Cache eviction is purely time-based; an explicit settle from the
  same process does NOT invalidate the cache. The 5s ceiling is
  the worst-case staleness for a freshly-credited win.

**Files changed for the follow-up:**

| File | Status | Net |
|---|---|---|
| `backend-master/api/VinPlayBackend/src/main/java/com/vinplay/api/backend/processors/banca/BanCaBalanceQueryProcessor.java` | new | +180 |
| `backend-master/api/VinPlayBackend/config/api_backend.xml` | +1 `<command>` for c=9997 | +5 |
| `backend-master/api/VinPlayBackend/src/main/java/com/vinplay/api/backend/auth/AdminAuthHelper.java` | + 9997 in NO_AUTH_COMMANDS | +1 |
| `banca/Core/Libs/UnifiedWallet/MoneyGatewayClient.cs` | `QueryBalanceMilliAsync`, `UpdateRedisMirror`, settle now passes `userId` through to `PostWithRetry` | +120 |
| `banca/Core/Libs/Database/RedisManager.cs` | `GetUserCash` flag-gated unified read with 5s cache | +35 |
| `banca/Core/UnifiedWalletTests/Phase5cRedisSyncTests.cs` | new xunit tests (QueryBalance happy/missing + Settle mirror refresh) | +180 |

**Open follow-ups (not done in this commit):**
- The 5s cache is per-process. Multi-process flush via Hazelcast
  pub-sub is deferred — staging has a single `game-banca` container.
- `cgame.users.user_id` ↔ `vinplay.users.id` is assumed 1:1 for
  migrated users; non-migrated edge cases (if any) fall through to
  the nickname-join branch.
- Phase 5d shadow-mode will compare c=9997 result against the legacy
  Redis mirror per request and emit a drift counter to Prometheus.
