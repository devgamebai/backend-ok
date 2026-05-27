# Wallet System Test Suite (SUN-1099)

Smoke + e2e tests for the 3-wallet system per `docs/WALLET_SYSTEM.md` and PM SUN-1099 clarifications.

## Quick Start

```bash
# Smoke (~20 sec) — fast invariants
bash tests/wallet/test_wallet_smoke.sh

# Full e2e (~2 min) — covers 6 suites
bash tests/wallet/test_wallet_e2e.sh
```

## Test Coverage

### `test_wallet_smoke.sh`

| Section | Verifies |
|---|---|
| **Cleanup** | c=9807 / c=9809 / c=9814 admin endpoints return `9002 Command not found` (deleted) |
| **Cleanup** | c=3080 / c=3081 portal endpoints return `9002` (deleted) |
| **Wave 2** | SpecialAccount user exists with `useragent.code='0'` |
| **Wave 2** | SpecialAccount login returns `accessToken` |
| **Wave 2** | c=3083 ClaimCashback as SpecialAccount → `errorCode 1099` |
| **Wave 2** | c=3041 WithdrawBank as SpecialAccount → `errorCode 1099` |
| **Live** | c=3082 GetPendingCashback returns valid JSON for real player |
| **Regression** | c=701 admin login → 32-char accessToken |
| **Regression** | c=124 captcha → 36-char UUID + base64 image |
| **Regression** | c=9800 / c=9801 cashback config CRUD live |

### `test_wallet_e2e.sh`

| Suite | Verifies |
|---|---|
| **E2E 1** | Live SELF rebate flow: seed `rebate_logs` SELF PENDING → c=3083 claim → `users.vin += amount` |
| **E2E 2** | SpecialAccount denied 1099 across 4 endpoints (c=3083, c=3041, c=9923, c=9922) |
| **E2E 3** | Cashback config CRUD intact (c=9800, c=9801) + game config rate table populated |
| **E2E 4** | Deleted endpoints (c=9807/c=9809/c=9814/c=3080/c=3081) all return `9002` |
| **E2E 5** | Audit trail: `agency_wallet_transactions` row writable for DOWNLINE commission |
| **E2E 6** | V12 audit trigger fires on `tbl_cashback_game_config` rate UPDATE → row in `commission_rate_audit` |

## Pre-Conditions

1. Stack running on this host (~35 sunwinkr-* containers healthy)
2. Test fixtures present in DB:
   - `SpecialAccount` user with `useragent.code='0'` (id=151)
   - `Kwon_DL1` user (id=50012, agent_id=205)
   - `KwonDe_5` player (id=50033)
3. `tbl_cashback_game_config` has at least 1 row (drives RealTimeCommission rate)

## Spec Background

Per PM SUN-1099 (2026-04-25):

| Question | Answer | Implementation |
|---|---|---|
| SELF commission destination | Game wallet (`users.vin`) PENDING until claimed | Live via `rebate_logs` SELF + `c=3083 ClaimCashbackProcessor` |
| Player has agency_wallet? | YES (Phase 2 invite-friends program) | Schema work — Wave 4 (deferred) |
| Credit transfer scope | Direct 1-level only | Already enforced in `CreditWalletService.isDirectUplineOrDownline` |
| Credit topup scope | Self + direct downline only | Already enforced in `AgentCreditDepositProcessor` |
| SpecialAccount restrictions | All money actions blocked, balance read-only | MR !231 (RoleGuard.isSpecialAccount + 4 deny points) |

## SUN-1099 Cleanup Summary

MR !233 deleted the orphan `tbl_cashback_logs` flow:

**DELETED endpoints (return 9002 now):**
- c=9807 BatchCashbackPayoutProcessor (admin batch payout)
- c=9809 CheckCashbackExpiryProcessor (expiry sweep)
- c=9814 GetCashbackLogGameDetailProcessor
- c=3080 GetCashbackHistoryProcessor (portal)
- c=3081 GetRefundHistoryProcessor (portal)

**KEPT endpoints (still live):**
- c=3082 GetPendingCashbackProcessor — reads `rebate_logs` SELF PENDING
- c=3083 ClaimCashbackProcessor — pays into `users.vin`
- c=9800 / c=9801 / c=9812 / c=9813 — cashback config CRUD

## Wallet Quick Reference

```
Player Game Wallet — users.vin (real KRW) + users.xu (bonus)
Agency Wallet      — vinplay.agency_wallet.balance (per useragent.id)
Credit Wallet      — vinplay.credit_wallet.balance (per useragent.id)
```

| Money Path | Source | Destination | Routing |
|---|---|---|---|
| SELF rebate accrue | per-bet `RealTimeCommission` | `rebate_logs` SELF PENDING | live |
| SELF rebate claim (player) | `rebate_logs` SELF PENDING | `users.vin` | `c=3083` |
| DOWNLINE commission | downline bet | `agency_wallet` | RMQ commission cascade |
| Credit topup (admin) | admin | `credit_wallet` of target agent | `AdminCreditWalletTopupProcessor` |
| Credit transfer agent → agent | `credit_wallet` | `credit_wallet` (direct only) | `AgentTransferCreditProcessor` |
| Credit deposit to game wallet | `credit_wallet` | `users.vin` (self/direct downline only) | `AgentCreditDepositProcessor` |

## Troubleshooting

### Smoke fails on Wave 2 — SpecialAccount can claim

MR !231 not deployed. Verify:
```bash
docker exec sunwinkr-backend-api unzip -l /app/libs/runtime/VinPlayDAL-1.0.jar | grep RoleGuard
# Should show RoleGuard.class. If missing, rebuild backend-api with --no-cache.
```

### Smoke fails on cleanup — c=9807 still routes

MR !233 not deployed. Verify:
```bash
docker exec sunwinkr-backend-api wc -l /app/config/api_backend.xml
# Should be smaller than pre-cleanup (10 entries removed).
```

### Suite 1 always SKIPped — Cannot login as KwonDe_5

Real player password unknown in test env. Pass it via env var or set in helpers.sh:
```bash
PLAYER_NICK=realplayer PLAYER_PASS=md5hash bash tests/wallet/test_wallet_e2e.sh
```

### SpecialAccount login fails (Suite 2)

Per `agency_enhancement_migration.sql`, default password is `account@2026` (MD5). If changed in DB, override:
```bash
SPECIAL_PASS_MD5=$(echo -n "newpass" | md5sum | awk '{print $1}') bash tests/wallet/test_wallet_e2e.sh
```

## Related MRs / Tickets

- MR !222 (cashback role-routed payout) — replaced by MR !230 → superseded by MR !233 cleanup
- MR !231 — `feat(SUN-1099)`: SpecialAccount read-only enforcement (Wave 2)
- MR !233 — `chore(SUN-1099)`: delete tbl_cashback_logs orphan flow
- SUN-1099 — PM clarification (2026-04-25)
- `docs/WALLET_SYSTEM.md` — ground-truth wallet doc
