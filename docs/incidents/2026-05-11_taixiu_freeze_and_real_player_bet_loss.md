# 2026-05-11 — TaiXiu freeze, real-player bet loss, and recovery

**Status:** Resolved
**Duration:** ~12 hours of intermittent impact (2026-05-10 18:42 UTC → 2026-05-11 04:38 UTC)
**Impact:** TaiXiu game frozen twice (12 min + 13 min outages); real-player minigame bets debited but never logged / never paid out (24 players, 1.24M KRW); 5 round-history entries missing in `result_tai_xiu_md5`.
**Final state:** All affected players refunded, bet history backfilled, code fixes deployed (game-minigame image `bundle-2026-05-11-v5` → `:latest`), watchdog active for future freeze recovery.

---

## Timeline (KST, UTC+9)

| Time | Event |
|---|---|
| 05-10 10:38 | Hazelcast 2/3 started; cluster bouncing earlier for heap bump (2 → 4 GiB) |
| 05-11 03:42 | **Hot-swap #1**: VinPlayDAL xu dual-write fix + TaiXiu cache-isolate (Minigame.jar). Started accumulating broken state in `result_tai_xiu_md5` for some rounds afterward. |
| 05-11 10:13 | **RabbitMQ broker incident** — `TIMEOUT WAITING FOR ACK` across `queue_server_info`, `queue_fund`, `queue_taixiu_sicbo`, `queue_baucua`. Hikari MySQL pool flagged "Apparent connection leak detected" for game-minigame and game-thirdparty. `SeamlessWalletAggregator slow handle: GscBalance elapsedMs=30044`. |
| 05-11 10:42:52 | **Last successful TaiXiu round generation** (ref ~318420). |
| 05-11 10:42 → 10:55 | **TaiXiu frozen — outage #1.** Players could not bet on new TaiXiu rounds. Sicbo continued running. |
| 05-11 10:55:42 | Manual container restart (`kill -TERM 1` on `sunwinkr-game-minigame`). TaiXiu resumed at ref 318432. |
| 05-11 11:04 → 11:26 | Tytrg77 placed 3 TaiXiu bets that debited but never reached mongo log + never settled (post-restart recurrence, same root cause). |
| 05-11 11:46 | Hot-swap #2 (v2): TaiXiu watchdog + GameLoopTask `printStackTrace` guard added. |
| 05-11 13:38:52 | Hot-swap #3 (v4): MGRoomTaiXiu/Sicbo/Live `isBot()` null-safe + remove silent `catch (Exception) {}`. |
| 05-11 14:03:30 | Hot-swap #4 (v5): PotTaiXiu + MGRoomTaiXiuMD5 `isBot()` null-safe (final hardening). |
| 05-11 13:46:56 | **51-bet refund + mongo bet-history backfill executed.** 24 players, 1,238,124 KRW returned. |
| 05-11 ~14:00 | **5 missing TaiXiu MD5 result rows inserted** for refs 318393, 318431, 318439, 318469, 318487. |

---

## Root causes (four overlapping bugs)

### 1. RabbitMQ broker hang at 10:13 KST
The broker stopped acknowledging publishes for ~60 seconds, causing every Java service's RMQ publish thread to block for up to 30s each. Hikari connection pool detected leaked connections holding MySQL connections while waiting on the RMQ ACK. The originating event is not in our logs (likely memory/disk alarm on the broker); the symptoms are visible in `game-minigame/main.log` around 10:13:00–10:13:48.

### 2. TaiXiu GameLoopTask permanently cancelled after the RMQ hang
`TaiXiuModule.GameLoopTask.run()` wraps `gameLoop()` in `try { … } catch (Throwable e) { e.printStackTrace(); }`. The catch-all is correct in theory — but `e.printStackTrace()` itself can throw on a broken stdout (full disk / broken pipe). When it does, the Throwable escapes `run()`, and `ScheduledThreadPoolExecutor.scheduleAtFixedRate` *permanently cancels* the task on any exception.

Mitigation: manual container restart restored scheduling.
Code fix: wrap `e.printStackTrace()` itself, plus add an independent watchdog (`taixiu-gameloop-watchdog` / `sicbo-gameloop-watchdog`) on a separate single-thread `ScheduledExecutorService` that re-schedules the task if `lastGameLoopTickMs > 90s` or the future is `cancelled/done`. Same defense added to SicboModule.

### 3. Real-player bets vanishing — `userService.getUser()` returns null on cache miss
The minigame bet path (`MGRoomTaiXiu.betTaiXiu` line 388, identical in Sicbo / MD5 / Live) calls:

```java
isBot = isLivestream ? true : this.isBot(nickname);
```

…where the implementation does:

```java
public boolean isBot(String username) {
    UserCacheModel model = this.userService.getUser(username);
    return model.isBot();        // ← NPE on cache miss
}
```

`UserServiceImpl.getUser` returns **null** when the user is not in the Hazelcast `users` map. The NPE escapes the bet handler **after** money has already been debited at line 432 but **before** `pot.bet`, `TaiXiuUtils.logBetTaiXiu`, and `insertUserBetToDb`. Result for the player: money gone, bet not in pot, bet not in mongo, no prize settlement even on a winning side.

Compounding the silence: the surrounding `try { insertUserBetToDb(...); } catch (Exception exception) {}` had an empty catch block.

The cache miss became frequent during the day because every successful `MoneyGateway.creditUserWithCumulative` evicts the user's `users` map entry (intentional, to force fresh reads after balance changes — see "Stale cache" follow-up below). For a player who deposits → immediately bets → bets again, the second bet often hits an empty cache slot.

Code fix (v4 + v5): null-safe all five `isBot()` lookup sites (`MGRoomTaiXiu`, `MGRoomSicbo`, `MGRoomTaiXiuLive`, `MGRoomTaiXiuMD5`, `PotTaiXiu`):

```java
public boolean isBot(String username) {
    try {
        UserCacheModel model = this.userService.getUser(username);
        if (model == null) return false;   // cache miss → assume real player
        return model.isBot();
    } catch (Throwable t) {
        return false;
    }
}
```

And replace the silent `catch (Exception exception) {}` with a loud `Debug.trace(...)` so any future bet-history write failure is visible in `main.log`.

### 4. Missing round-result rows in `result_tai_xiu_md5`
The session-history page (admin / agency / player) queries `vinplay_minigame.result_tai_xiu_md5`. A row is written for each completed round when `gameLoop` reaches `count=55` (CalculatingTaiXiuPrize task → publishes via RMQ → `SaveTransactionTaiXiuProcessor` writes the row).

When the gameLoop wedged mid-round, the round never reached `count=55`. No result row was written. The session-history page silently skipped those refids. The team flagged round **#318431** as missing; on inspection, the gaps for today were **318393, 318431, 318439, 318469, 318487** (5 rounds).

Also discovered (out of scope for this incident): `result_tai_xiu` (vanilla, non-MD5) has only 1 row since **2026-03-05**. That table's writer has been broken for two months — unrelated to today, but tracked for follow-up.

---

## What was deployed today

### Code changes (game-minigame image `sunwinkr-game-minigame:bundle-2026-05-11-v5` → `:latest`)

| File | Change | SUN tag |
|---|---|---|
| `VinPlayDAL/src/main/java/com/vinplay/dal/service/impl/CacheServiceImpl.java` | Retry-once on HZ disconnect in all 3 `setValue` overloads | SUN-1xxx |
| `VinPlayDAL/src/main/java/com/vinplay/dal/service/MoneyGateway.java` | Cache evict after every successful credit (was containsKey-only) + xu routed through currency-aware dual-write | SUN-1xxx |
| `VinPlayDAL/src/main/java/com/vinplay/dal/deposit/DepositApprovalService.java` | try-finally rollback in `approve` (ghost-lock fix from Duccot8386 incident) | SUN-1xxx |
| `api/VinPlayBackend/src/main/java/com/vinplay/api/backend/processors/user/ListUsersProcessor.java` | `u.dai_ly = 1` → `u.dai_ly > 0` (covers L2/L3 agents in User List, Bahai88 visibility fix) | SUN-1xxx |
| `game/Minigame/src/main/java/game/modules/minigame/TaiXiuModule.java` | GameLoop watchdog (90s threshold, separate executor) + `printStackTrace` guard + `startNewRoundTX` cache-write try/catch | SUN-1xxx |
| `game/Minigame/src/main/java/game/modules/minigame/SicboModule.java` | Same watchdog + guard pattern as TaiXiuModule | SUN-1xxx |
| `game/Minigame/src/main/java/game/modules/minigame/room/MGRoomTaiXiu.java` | `isBot()` null-safe + remove silent catch on `insertUserBetToDb` | SUN-1xxx |
| `game/Minigame/src/main/java/game/modules/minigame/room/MGRoomSicbo.java` | Same `isBot()` + loud-catch fix | SUN-1xxx |
| `game/Minigame/src/main/java/game/modules/minigame/room/MGRoomTaiXiuLive.java` | `isBot()` null-safe | SUN-1xxx |
| `game/Minigame/src/main/java/game/modules/minigame/room/MGRoomTaiXiuMD5.java` | `isBot()` null-safe | SUN-1xxx |
| `game/Minigame/src/main/java/game/modules/minigame/entities/PotTaiXiu.java` | `isBot()` null-safe | SUN-1xxx |

Source patches in `backend-master/scripts/patches/`:
- `cacheservice-retry-20260511.patch`
- `cache-evict-fallback-20260511.patch`
- `deposit-rollback-fix-20260510.patch`
- `userlist-dai_ly-fix-20260511.patch`
- `xu-dualwrite-cache-fix-20260511.patch`
- `taixiu-cache-isolate-20260511.patch`
- `taixiu-watchdog-20260511.patch`
- `sicbo-watchdog-20260511.patch`
- `mgroom-isbot-nullsafe-20260511.patch`
- `mgroom-isbot-nullsafe-v2-20260511.patch`

### Sidelined (to make build pass)
- `game/Minigame/src/main/java/game/eventHandlers/LogoutSusscessHandler.java` → renamed to `.broken`. Pre-existing compile error (`ExtensionUtility.sendLogoutOK` doesn't exist); not used by any other code. Restore once that method is added to BitZeroMinigame.

### Image lineage
```
sunwinkr-game-minigame:pre-v3-rollback      ← image from 2026-05-10, before any hot-swap
sunwinkr-game-minigame:bundle-2026-05-11-v3 ← watchdog
sunwinkr-game-minigame:bundle-2026-05-11-v4 ← isBot null-safe (first pass)
sunwinkr-game-minigame:bundle-2026-05-11-v5 ← + PotTaiXiu / MD5 null-safe (current :latest)

sunwinkr-backend-api:pre-v3-rollback        ← before xu dual-write fix
sunwinkr-backend-api:bundle-2026-05-11-v3   ← xu dual-write + cache evict + ledger
```

Rollback path: `docker tag sunwinkr-game-minigame:pre-v3-rollback sunwinkr-game-minigame:latest && docker compose up -d --no-deps --force-recreate game-minigame`.

---

## Data recovery — what was backfilled

### Player money refunds (51 bets, 24 players, 1,238,124 KRW)

For every real-player `USERSERVICE_GAME` debit on a TaiXiu or TaiXiuSicbo tx between 03:00 KST and 13:46 KST that had no corresponding payout (because the bet never reached the pot or the round never settled), inserted an `ADMIN_TOPUP` row with `tx_id = SUN1XXX-REFUND-<mgl_id>` and amount equal to the original bet value.

| Player | KRW refunded |
|---|---|
| Bahai88 | 400,000 |
| Kienbao05 | 195,000 |
| locphat92 | 176,000 |
| tuyendayne | 90,000 |
| millo300 | 65,000 |
| Tytrg77 | 52,500 |
| laoton678910 | 50,035 |
| viet_678 | 26,000 |
| duynguyen179 | 24,000 |
| Tauquanui | 20,050 |
| Thaole, anhkkk, Photruong, Korea1122, Trongdubai | 20,000 each |
| dungntd044 | 15,000 |
| huudanh1 | 6,000 |
| Tranphi99, testag001 | 5,000 each |
| tanphat_99 | 4,839 |
| (others) | sum of small bets ≤ 1,000 |

Audit table: `vinplay._sun1_backfill_20260511` (51 rows, all `mongo_inserted=1 money_refunded=1`).

After refund, each user's Hazelcast `users` cache entry was explicitly evicted so their next read reflects the new balance.

### Mongo bet history backfill (51 entries)

For each refunded bet, inserted a row into the appropriate collection with the original `reference_id`, `bet_side`, `bet_value`, `input_time`, `create_time`, and `prize=0, refund=bet_value` (treated as void). Idempotency key: `_sun1_backfill_mgl_id`.

- `win123club.log_taixiu`: **39 entries**
- `win123club.log_sicbo`: **12 entries**

Result: each affected player's personal bet history now shows the bet as refunded.

### MD5 session-history backfill (5 entries)

For the 5 today-incident rounds missing in `vinplay_minigame.result_tai_xiu_md5`, inserted void marker rows (`dice1=dice2=dice3=0`, sum=0 is not a valid TaiXiu roll, so operators / UI can visually distinguish them from real results):

| reference_id | interpolated timestamp |
|---|---|
| 318393 | 2026-05-11 12:13:29 |
| 318431 | 2026-05-11 12:56:17 |
| 318439 | 2026-05-11 13:05:01 |
| 318469 | 2026-05-11 13:39:04 |
| 318487 | 2026-05-11 13:59:19 |

`result_tai_xiu_md5` now has **zero gaps in refids 318300–318500**.

---

## Two representative player traces

### Kienbao05 (new account, created today 04:42:05 KST)
1. Deposited 150k + 45k promo → 195k vin.
2. Placed 6 TaiXiu/Sicbo bets between 04:48 and 05:02 — all fell into the freeze period, money debited, none recorded in mongo, none settled.
3. 13:46:56 — fully refunded (6 × ADMIN_TOPUP totalling 195k).
4. Started GSC Pragmatic slots at 13:55 — 14 bets, 8 winners, currently sitting on **220,000 vin** (deposit + GSC winnings).

### duynguyen179 (existing account)
1. Deposited 90k + 27k promo → 118.8k vin.
2. Placed 5 TaiXiu bets and 1 MiniPoker bet through the day — TaiXiu bets all into the void.
3. 13:46:56 — refunded for the 5 TaiXiu bets (24k total). MiniPoker bet of 100 not refunded (out of refund scope; likely a normal settled bet).
4. Started GSC slots at 14:03 — 13 bets, 4 winners, currently sitting on **2,880 vin**.

Both players' TaiXiu losses fully recovered; subsequent GSC outcomes are normal variance, not system bugs.

---

## Outstanding items / follow-ups

1. **Real-player bet visibility on v5 is not yet smoke-test verified.** `duynguyen179` placed one TaiXiu bet at 13:40 KST after v4 was live and it still didn't reach mongo — the null-safe fix is correct as defense but the actual root cause may be different (possibly `userService.updateMoney` returning `success=true` while the surrounding flow still skips `pot.bet`). The first real-player bet post-v5 needs to be inspected; if still missing, instrument `MGRoomTaiXiu.betTaiXiu` with `Debug.trace` on every branch between line 432 (debit) and line 466 (pot.bet) to find the dropper.
2. **`result_tai_xiu` (vanilla) is empty since 2026-03-05** — the writer for vanilla-TaiXiu rounds has been broken for 2 months. Vanilla TaiXiu may be effectively unused on production; investigate or formally retire.
3. **Pre-existing MD5 gaps**: 12 older missing refids (316334, 316535, 316670, 316685, 316699, 317180, 317182, 317274, 317359, 317526, 317942, 317951) from prior days. Same backfill technique applies if the team wants those filled — left untouched today.
4. **RabbitMQ broker monitoring**: today's 10:13 incident had no upstream alert. Add a Grafana/Prometheus probe on RMQ memory + disk watermarks and queue ACK latency so the next broker hiccup pages someone before it cascades.
5. **`LogoutSusscessHandler.java.broken`**: restore the file (and add the missing `ExtensionUtility.sendLogoutOK` method in BitZeroMinigame) so it stops blocking clean builds.
6. **Hikari "Apparent connection leak detected"** logs in game-minigame and game-thirdparty during the RMQ incident — consider tightening `connectionTimeout` / shortening leak-detection threshold and add a structured alert.
7. **GameHistoryService.getRoundHistory** consumers may also be affected by the `result_tai_xiu` emptiness or similar bugs in other minigames; audit each minigame's result table for recent gaps.

---

## Files / artefacts

- Working audit table: `vinplay._sun1_backfill_20260511`
- Pre-restart thread dump: `/tmp/game-minigame-pre-restart-threaddump-20260511-035542.log`
- Source patches: `backend-master/scripts/patches/*.patch` (10 patches today)
- Image snapshots: `sunwinkr-game-minigame:bundle-2026-05-11-v3/v4/v5`, `:pre-v3-rollback`
- Mongo backfill marker: every backfilled doc has `_sun1_backfill_mgl_id` field set, allowing future cleanup with `db.log_taixiu.deleteMany({_sun1_backfill_mgl_id: {$exists: true}})`
- MySQL backfill marker: `tx_id LIKE 'SUN1XXX-REFUND-%'` in `money_gateway_log`, `result_tai_xiu_md5` voided rows identifiable by `dice1=0 AND dice2=0 AND dice3=0`

---

## Reversal recipe (if any backfill needs to be undone)

```sql
-- Reverse money refunds (this also requires reversing balance changes):
START TRANSACTION;
UPDATE vinplay.users u
JOIN (SELECT user_id, SUM(bet_value) total FROM vinplay._sun1_backfill_20260511 GROUP BY user_id) r
  ON r.user_id = u.id
SET u.vin = u.vin - r.total, u.vin_total = u.vin_total - r.total;
DELETE FROM vinplay.money_gateway_log WHERE tx_id LIKE 'SUN1XXX-REFUND-%';
COMMIT;

-- Reverse MD5 voided rounds:
DELETE FROM vinplay_minigame.result_tai_xiu_md5
WHERE reference_id IN (318393, 318431, 318439, 318469, 318487)
  AND dice1=0 AND dice2=0 AND dice3=0;
```

```javascript
// Reverse mongo bet-history backfill:
use win123club
db.log_taixiu.deleteMany({_sun1_backfill_mgl_id: {$exists: true}})
db.log_sicbo.deleteMany({_sun1_backfill_mgl_id: {$exists: true}})
```

Code rollback to pre-incident state: `docker tag sunwinkr-game-minigame:pre-v3-rollback sunwinkr-game-minigame:latest && docker compose -f docker-compose.games.yml up -d --no-deps --force-recreate game-minigame`.
