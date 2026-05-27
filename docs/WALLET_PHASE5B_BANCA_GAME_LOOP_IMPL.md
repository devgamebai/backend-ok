# Wallet Phase 5b — BanCa Main Game Loop Implementation

**Status:** implemented behind kill switch `BANCA_USE_UNIFIED_WALLET` (default OFF).
**Predecessor:** `WALLET_PHASE5_BANCA_DESIGN.md`
**Follow-on:** Phase 5c (sub-games), 5d (shadow + reconcile), 5e (blue/green soak).

## Scope

Migrate BanCa's **main game-loop** session settle from Redis `User_Cash:{id}` + MySQL `cgame.users.cash` onto the unified Java MoneyGateway (PLAYER_VIN ledger), without touching the hot-path arithmetic on `Player.Cash` / `Player.Profit`.

Out of scope (deferred):
- LobbyService deposit/withdraw paths
- Loto / OneTwoThree sub-games
- BanCaService CMS / payment paths
- `cash_safe`, `cash_silver`, `cgame.users.cash*` column removal

## Architecture

Session-batch:

1. Hot path (`GameBanCa.cs` lines 790, 918, 2127, 2365, 2704, 2865) **UNCHANGED**. `Player.Cash` and `Player.Profit` mutate in-memory at native speed.
2. **Quit settle** (`GameBanCa.RemovePlayer` ~line 1848): when flag is on, post `(userId, profit, "bc-{id}-{worldId}")` to MoneyGateway as `WAGER_CREDIT_BANCA` / `WAGER_DEBIT_BANCA`. Legacy `IncEpicCash` runs only when flag is off.
3. **Periodic 5s flush** (`BanCaServer._updateEngine`): every `BANCA_SETTLE_INTERVAL_MS` walk non-bot players, settle when `|profit| >= BANCA_SETTLE_THRESHOLD` or `|profit| >= BANCA_BIG_BET_THRESHOLD`. We reserve `Player.Profit = 0` before HTTP dispatch and refund on failure so the next tick re-flushes.
4. **Cross-game tx** (`BanCaServer.openTx` ~line 1231): settle to MoneyGateway before allowing hand-off to TaiXiu.
5. **Revive crash recovery** (`BanCaServer.Revive` ~line 260): collect per-player tuples, then `MoneyGatewayClient.BatchSettleAsync` after the Redis batch commits.

## Files

| File | Status | Lines |
|---|---|---|
| `banca/Core/Libs/UnifiedWallet/MoneyGatewayClient.cs` | NEW | ~260 |
| `banca/Core/Libs/Logic/GameBanCa.cs` | patched (RemovePlayer settle) | +35 / -5 |
| `banca/Core/Libs/Logic/BanCaServer.cs` | patched (Revive + openTx + _updateEngine timer) | +120 / -3 |
| `banca/Core/Core.csproj` | patched (exclude UnifiedWalletTests/ from main build) | +6 |
| `banca/Core/UnifiedWalletTests/UnifiedWalletTests.csproj` | NEW | ~25 |
| `banca/Core/UnifiedWalletTests/MoneyGatewayClientTests.cs` | NEW | ~190 |
| `docs/WALLET_PHASE5B_BANCA_GAME_LOOP_IMPL.md` | NEW | this file |

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `BANCA_USE_UNIFIED_WALLET` | `0` | Master kill switch. `1` → MoneyGateway. `0` → legacy Redis path. |
| `BANCA_MONEYGATEWAY_URL` | (derived) | Full URL of MoneyGateway endpoint. Falls back to `${xxeng-backend}/money_gateway` from `config.json`. |
| `BANCA_SERVICE_TOKEN` | _required_ | `X-Service-Token` header. Reused from SUN-1054 `LogBetCommission`. |
| `BANCA_SETTLE_INTERVAL_MS` | `5000` | Periodic flush cadence. |
| `BANCA_SETTLE_THRESHOLD` | `10000` | Minimum `|profit|` to flush on tick. |
| `BANCA_BIG_BET_THRESHOLD` | `50000` | Immediate-settle threshold (a single shot or kill at or above this). |
| `BANCA_SETTLE_TIMEOUT_MS` | `5000` | HTTP timeout per attempt. |
| `BANCA_SETTLE_MAX_RETRIES` | `3` | Retry attempts before drop to `banca:failed_settle`. |

## Idempotency

Every settle POSTs `external_ref = banca:settle:{userId}:{sessionId}:{checkpointMs}`. Re-posting the same `external_ref` MUST be a no-op on the Java side (server-side dedupe required — verify before enabling). `sessionId` shapes:

| Site | `sessionId` |
|---|---|
| Quit | `bc-{userId}-{worldId}` |
| Periodic tick | `bc-tick-{userId}-{worldId}` |
| openTx cross-game | `bc-openTx-{userId}-{worldId}` |
| Revive batch | `bc-revive-{userId}-{worldId}` |

`checkpointMs = TimeUtil.TimeStamp` at the moment of dispatch.

## Failure mode

On 3 consecutive 5xx/timeout failures the payload is pushed to Redis list `banca:failed_settle` with `LPUSH` and is dropped from the caller's view. A separate async worker (out of scope of Phase 5b) replays entries against MoneyGateway. Because every payload carries its own `external_ref`, replay is idempotent.

4xx responses are **terminal** — they indicate validation failure or idempotency collision on a different payload. We log + drop, no retry.

## Latency expectations

Per the design doc: target p99 ≤ 100ms for the settle path inside the Docker network.

Methodology (recommended for Phase 5a benchmark):

```bash
# Inside the banca container, with the new build deployed and flag OFF still:
docker exec sunwinkr-game-banca bash -c '
  for i in $(seq 1 1000); do
    curl -s -o /dev/null -w "%{time_total}\n" \
      -X POST "$BANCA_MONEYGATEWAY_URL" \
      -H "X-Service-Token: $BANCA_SERVICE_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"user_id\":42,\"amount\":0,\"tx_type\":\"WAGER_DEBIT_BANCA\",\"session_id\":\"benchmark-$i\",\"external_ref\":\"benchmark-$i\",\"checkpoint\":$(date +%s%3N),\"game_key\":\"banca\"}"
  done | sort -n | awk '\''
    BEGIN {n=0}
    {a[n++]=$1}
    END {
      p50=a[int(n*0.50)]; p99=a[int(n*0.99)]; p999=a[int(n*0.999)];
      printf "n=%d p50=%.4fs p99=%.4fs p99.9=%.4fs\n", n, p50, p99, p999
    }'\''
'
```

The hot path itself is **unchanged** — there should be **zero added latency** on per-shot processing.

## Build verification

```
# Baseline (before changes)
Core.dll        = 1259520 bytes
BanCaLiteNet.dll = 29696 bytes

# After Phase 5b
Core.dll        = 1270272 bytes  (+10752, ~10.5KB)
BanCaLiteNet.dll = 29696 bytes  (unchanged)
```

Build command (matches `deploy.sh`):

```
docker run --rm -v "$(pwd)/banca:/banca" -w /banca/BanCaLiteNet \
  mcr.microsoft.com/dotnet/sdk:5.0 dotnet publish -c Release -o out
```

Test command:

```
docker run --rm -v "$(pwd)/banca:/banca" -w /banca/Core/UnifiedWalletTests \
  mcr.microsoft.com/dotnet/sdk:5.0 dotnet test
```

Test results: **4 passed / 0 failed** (Settle_2xx_returns_ok, Settle_5xx_retries_then_drops, ExternalRef_is_deterministic_per_checkpoint, Settle_4xx_does_not_retry).

## Rollback procedure

The change is gated entirely behind `BANCA_USE_UNIFIED_WALLET`. The legacy `IncEpicCash` / `IncEpicCashCache` code paths are **not removed**.

To roll back:

```bash
# 1. Unset the env var on the BanCa container
docker exec sunwinkr-game-banca bash -lc 'unset BANCA_USE_UNIFIED_WALLET'

# Or edit .env and restart:
sed -i 's/^BANCA_USE_UNIFIED_WALLET=.*/BANCA_USE_UNIFIED_WALLET=0/' .env
docker compose -f docker-compose.yml -f docker-compose.banca.yml \
  up -d --force-recreate game-banca

# 2. Verify legacy path is active
docker logs sunwinkr-game-banca --tail 200 | grep -E 'Phase5b|IncEpicCash'
```

If `banca:failed_settle` accumulated entries during the rollout, they remain in Redis — drain via the replay worker once `BANCA_USE_UNIFIED_WALLET=1` is restored, or DEL the list if you abandon the migration.

There is **no code rollback** needed; the legacy code path is still on disk.

## Open follow-ups for Phase 5c

1. **Sub-game wallet writes** — `OneTwoThreeBoard.cs:251,413,466`, `LotoGame.cs:148`, `BanCaService.cs:296,336,1244+`. These still call `IncEpicCash` / `IncEpicCashCache` directly. Plan to route them through `MoneyGatewayClient` with new `tx_type` enumerations (`WAGER_DEBIT_LOTO`, `WAGER_CREDIT_123`, etc.).
2. **`LobbyService.cs` payment paths** (line 259, 269, 318, 757, 1019, 1158, 1614, 1733, 2837) — daily bonus, xxeng cashin/out, withdrawal. Must map to `DEPOSIT_BANK_BANCA`, `WITHDRAW_BANK_BANCA`, `BONUS_DAILY_BANCA`.
3. **Failed-settle replay worker** — a small Java or C# daemon that LPOPs `banca:failed_settle` and re-posts. Currently entries pile up with no automatic drain.
4. **Backend MoneyGateway endpoint** — verify the Java side at `${xxeng-backend}/money_gateway` recognises `WAGER_DEBIT_BANCA` / `WAGER_CREDIT_BANCA` / `EMERGENCY_BANCA` tx types and that `external_ref` dedupe is in place. This implementation assumes it; without it, retries WILL double-debit.
5. **Periodic settle race vs Cash mutation** — the in-memory reservation pattern (`player.Profit = 0` before dispatch, refund on failure) is safe under the single-threaded engine loop, but the kill credit happens on the same thread so this is fine. Document so Phase 5c authors do not introduce parallel cash mutation.
6. **Phase 5a benchmark** — run the latency methodology above before flipping the flag on in staging.

## Constraints honoured

- `cgame.users.cash*` columns: **not touched**. Reserved for Phase 5e.
- Legacy Redis `IncEpicCash` / `IncEpicCashCache`: **preserved verbatim** for shadow mode (Phase 5d).
- Sub-games (Loto, OneTwoThree, BanCaService): **not touched**. Phase 5c.
- Hot-path arithmetic (`Player.Cash` / `Player.Profit` per-shot/per-kill at lines 790, 918, 2127, 2365, 2704, 2865 of `GameBanCa.cs`): **not touched**.
