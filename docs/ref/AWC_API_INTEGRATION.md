# AWC (Asia Win Connect) Single Wallet API — Integration Reference

Reference doc for the Sunwinkr ↔ AWC seamless wallet integration. Compiled
from the AWC OpenAPI 1.0.0 spec at <https://awc-docs.apihub888.com/>
(ReDoc-rendered, served from `./spec/openapi.json`).

---

## 1. Test Environment

| Field | Value |
|---|---|
| Agent ID | `lime01` |
| Password | `PigVq2D07hNL` |
| Operator prefix | `lime` |
| Cert (callback secret) | `PigVq2D07hNL` |
| API server URL | `https://awc-api.apihub888.com/` |
| Fetch API URL | `https://awc-api.apihub888.com/` |
| Documentation | <https://awc-docs.apihub888.com/> |

**Webhook URL** must be provided to AWC support to complete onboarding.
For staging: `https://staging-play.sunkr.bet/api?c=3097` (the existing
`AwcCallbackProcessor`). For prod: `https://<prod-domain>/api?c=3097`.

The operator prefix (`lime` for test) is prepended to every AWC userId,
so `userId="abc"` on the AWC side corresponds to vinplay player `lime_abc`
in the operator's `users` table — adjust mapping in
`AwcCallbackProcessor.awcUserIdToUsername` to match prod prefix.

---

## 2. Architecture — Single-Wallet Model

AWC is a **single-wallet aggregator**: 8+ live-casino / slot / fish /
sportsbook providers expose one unified wallet API. The operator (us)
hosts the **only** copy of the player balance; AWC and its underlying
providers never hold funds.

Two-way flow:

```
                ┌─────────────────────────────────────┐
                │            AWC API Hub              │
                │ (aggregator for SEXYBCRT, EVO, BTI, │
                │  PRAGMATIC, JILI, FISH, ...)        │
                └────────────┬───────────┬────────────┘
                             │           │
                  Common API │           │ Single-Wallet API
                  (we call)  │           │ (AWC calls our webhooks)
                             │           │
              ┌──────────────▼───────────▼──────────────┐
              │      Sunwinkr Operator (vinplay)         │
              │  - Hosts users.vin balance               │
              │  - AwcCallbackProcessor (c=3097)         │
              │  - Updates vin via MoneyGateway          │
              └──────────────────────────────────────────┘
```

**Common API** — outbound (operator → AWC). We call AWC for member
provisioning, login URL generation, transaction-history queries,
schedule lookups, etc. **17 endpoints** under `/wallet/*` and
`/fetch/*`.

**Single-Wallet API** — inbound (AWC → operator). AWC posts JSON to
operator-implemented webhook URLs for every wallet event: bet, settle,
cancel, refund, void, freeSpin, tip, give. **19 callbacks** the operator
must implement under `/{callbackURL_*}` paths.

---

## 3. Authentication

### Outbound (Common API)
Each request to `https://awc-api.apihub888.com/wallet/*` includes the
agent identity in the body. Specifics (header signing, HMAC) are
documented per-endpoint in the OpenAPI spec; check `AwcConfig` /
`AwcClient` for the current operator-side implementation.

### Inbound (Webhook)
Every callback from AWC carries:

```json
{
  "message": { "action": "bet", "txns": [ ... ] },
  "key": "<cert string>"
}
```

The operator MUST verify `key === cert` (the operator-specific cert
stored in `AwcConfig.cert()`, sourced from the env var `AWC_CERT`).
On mismatch return:

```json
{ "status": "1001", "desc": "Invalid key" }
```

The current implementation: `AwcCallbackProcessor.execute` line ~88-94
calls `AwcConfig.verifyCallbackKey(key)` and returns `1001` on fail.

---

## 4. Common API — Operator → AWC (17 endpoints)

| Path | Purpose |
|---|---|
| `POST /wallet/createMember` | Provision a new player on AWC side. Operator gives userId, currency, betLimit, etc. |
| `POST /wallet/login` | Get a login URL for existing player to enter the AWC lobby. |
| `POST /wallet/doLoginAndLaunchGame` | Generate a single-game launch URL (skips lobby, jumps straight to a specific gameCode). |
| `POST /wallet/logout` | Force-logout a player session. |
| `POST /wallet/checkStatus` | Check player session/login status. |
| `POST /wallet/updatePlayerStatus` | Suspend / resume / lock a player. |
| `POST /wallet/updateBetLimit` | Change active bet-limit IDs for a LIVE player. |
| `POST /wallet/queryBetLimit` | Read current bet-limit settings. |
| `POST /wallet/getLobbyState` | Lobby-level status (open/closed/maintenance). |
| `POST /wallet/getSchedule` | Provider maintenance schedules. |
| `GET /wallet/getJackpotPool` | Current jackpot amounts. |
| `POST /wallet/getTransactionStatus` | Look up status of a single transaction by platformTxId. |
| `GET /wallet/getTransactionHistoryResult` | Pull player transaction history. |
| `POST /wallet/getTransactionHistoryResultAll` | Pull operator-wide transaction history. |
| `POST /wallet/resubmitCancelbetNotification` | Re-trigger a cancelBet webhook (only within 2 days). |
| `GET /fetch/getSummaryByBetTimeHour` | Hourly betting summary (BI/reporting). |
| `GET /fetch/getPromotionSummary` | AWC-side promotion accounting. |
| `POST /fetch/getPlatformListByAgent` | Available platforms (game providers) for this agent. |

**Currency handling:** AWC supports multiple currencies (KRW, IDR2,
VND, etc.). The operator wallet stores raw sub-units (e.g. IDR), AWC
amounts arrive in display units (e.g. IDR2 = ×1000). Existing code
multiplies/divides by `getExchangeRateIn(currencyCode)` /
`getExchangeRateOut(...)` — see `AwcCallbackProcessor.exchange*` and
the GSC counterpart for reference.

---

## 5. Single-Wallet Callback API — AWC → Operator (19 actions)

All callbacks POST to URLs the operator hosts. AWC dispatches by the
`message.action` field. Single endpoint can multiplex all 19 actions
(current implementation: c=3097 dispatches via `switch(action)`),
or 19 distinct URLs — operator's choice when registering with AWC.

### 5.1. Action catalog

| Action | Returns balance? | Purpose |
|---|---|---|
| `getBalance` | ✅ | Player asks for current balance from AWC lobby. |
| `bet` | ✅ | Place bet — debit `betAmount` from player vin. |
| `betNSettle` | ✅ | Single-step bet+settle (e.g. instant slot spin). Net = `winAmount - betAmount`. |
| `cancelBet` | ✅ | Cancel a previously-issued `bet` — refund the debit. |
| `cancelBetNSettle` | ✅ | Cancel a `betNSettle` — reverse net change. |
| `adjustBet` | ✅ | Adjust bet amount up or down. Net = `newAmount - oldAmount`. |
| `tip` | ✅ | Player tips the dealer. Debit from vin. |
| `cancelTip` | ✅ | Cancel a previous tip. Credit back to vin. |
| `settle` | ❌ | Game ended — credit `winAmount` (or zero). No balance return. |
| `refund` | ❌ | Round voided — refund original betAmount. |
| `unsettle` | ❌ | Reverse a previous `settle`. |
| `resettle` | ❌ | Re-issue a corrected `settle` after `unsettle`. |
| `voidBet` | ❌ | Mark bet as void (no money movement on its own). |
| `voidSettle` | ❌ | Mark settle as void. |
| `unvoidBet` | ❌ | Reverse a `voidBet`. |
| `unvoidSettle` | ❌ | Reverse a `voidSettle`. |
| `freeSpin` | ❌ | Apply a free-spin reward (operator may credit win without bet debit). |
| `give` | ❌ | Promotion bonus credit from AWC's promo system. |

**Note on "returns balance":** balance-returning actions expect the
operator response to include the player's NEW balance after the wallet
update. Non-balance actions return only status confirmation.

### 5.2. Common request envelope

```json
POST /{operator-webhook-url}
Content-Type: application/json

{
  "key": "<AWC cert string>",
  "message": {
    "action": "<one of 19 actions>",
    "txns": [ /* array of transaction items */ ]
  }
}
```

Some actions (`getBalance`) skip the `txns` array and put fields
(e.g. `userId`) directly on `message`.

### 5.3. Common txn item shape (bet/settle/etc)

```jsonc
{
  "platformTxId": "BAC-12748",         // game-provider's tx ID — primary dedup key
  "userId": "abc",                      // player ID (without operator prefix)
  "platform": "SEXYBCRT",               // provider name
  "gameCode": "MX-LIVE-001",            // specific game
  "betTime": "2020-12-23T15:55:24.000+07:00",  // ISO 8601
  "betType": "Banker",                  // optional — provider-specific
  "betAmount": 100.00,                  // amount in display currency
  "validBetAmount": 100.00,             // commissionable amount (sometimes < betAmount)
  "winAmount": 0,                       // present on settle / betNSettle / refund
  "currency": "KRW",
  "isPremium": false,                   // EVOLUTION-only — premium game flag
  "gameInfo": { /* provider-specific blob */ }  // do NOT validate fields here
}
```

**Idempotency:** operator MUST dedup by `platformTxId`. A retry of the
same `platformTxId` returns `0000` (success) without re-applying the
balance change. Current impl: `AwcCallbackProcessor.isDuplicateTxn`
checks an `awc_tx_log` table.

### 5.4. Standard response shape

```jsonc
// Balance-returning actions:
{ "status": "0000", "desc": "Success", "balance": 1234.56 }

// Non-balance actions:
{ "status": "0000", "desc": "Success" }

// Common errors:
{ "status": "1001", "desc": "Invalid key" }              // bad cert
{ "status": "1000", "desc": "Invalid userId" }           // unknown player
{ "status": "1003", "desc": "Insufficient balance" }     // bet > vin
{ "status": "1004", "desc": "Duplicate transaction" }    // platformTxId already seen → return 0000 anyway
{ "status": "9999", "desc": "Internal: <msg>" }          // catch-all
```

Per AWC convention, return `0000` on duplicate (success-but-noop).
Idempotent dedup is mandatory — AWC retries are common.

### 5.5. Critical flow — bet → settle

```
1. Player presses "Confirm" in AWC lobby.
2. AWC POSTs { action:"bet", txns:[{...,betAmount:100}] } → operator webhook.
3. Operator:
     a. Verify key === cert.
     b. Check platformTxId not in awc_tx_log (else return {status:"0000",balance:current}).
     c. MoneyGateway.debitUser(userId, nick, betAmount, "AWC_DEBIT", platformTxId).
        - Atomic UPDATE users SET vin = vin - ? WHERE id=? AND vin >= ?
        - On insufficient → return {status:"1003"}.
     d. Insert awc_tx_log row with platformTxId.
     e. Return { status:"0000", balance:newVin }.
4. Game round plays out provider-side.
5. AWC POSTs { action:"settle", txns:[{...,winAmount:200}] } → operator webhook.
6. Operator:
     a. Verify key === cert.
     b. Dedup on settle's platformTxId.
     c. MoneyGateway.creditUser(userId, nick, winAmount, "AWC_CREDIT", platformTxId).
     d. Mark original betTx as settled.
     e. Return { status:"0000" }   (no balance — settle is non-returning).
```

Cancel/void flows reverse the above using
`MoneyGateway.creditUser(... AWC_CREDIT_REFUND ...)` for refunding a
debit, or `MoneyGateway.debitUser(... AWC_DEBIT_REVERSE ...)` for
clawing back a credited win.

---

## 6. Webhook URL setup

Operator must provide a single public HTTPS URL to AWC support. Two
options:

**Option A — single dispatcher (current Sunwinkr impl):**
```
Webhook URL: https://staging-play.sunkr.bet/api?c=3097
```
The processor `AwcCallbackProcessor` dispatches by `message.action`.

**Option B — per-action URLs:**
```
bet:        https://.../api?c=3097&a=bet
settle:     https://.../api?c=3097&a=settle
...
```
AWC supports either. Single dispatcher is simpler and matches the spec
(see the 19 `/{callbackURL_*}` paths).

**TLS:** AWC requires HTTPS. Cloudflare proxy handles this for staging
(SSL set to "Full" — see CLAUDE.md Cloudflare section).

---

## 7. Operator-side implementation (current state)

| Component | File | Role |
|---|---|---|
| HTTP entrypoint | `api/VinPlayPortal/.../awc/AwcCallbackProcessor.java` | c=3097 dispatcher, JSON parse, key auth, action switch |
| Config | `VbeeCommon/.../config/AwcConfig.java` | env vars: `AWC_ENABLED`, `AWC_CERT`, `AWC_AGENT_ID`, prefix |
| User mapping | `AwcCallbackProcessor.awcUserIdToUsername` | AWC userId → operator users.user_name (prefix + id) |
| Balance read | `AwcCallbackProcessor.getPlayerBalance` | Hazelcast `users` map first, MySQL fallback |
| Balance write | `AwcCallbackProcessor.deductBalance` / `addBalance` | thin wrappers over `MoneyGateway.debitUser` / `creditUser` (post money-flow audit) |
| Dedup | `awc_tx_log` table on `platformTxId` (UNIQUE INDEX) | rejects retries silently |
| Audit | `money_gateway_log` rows with `source='AWC_DEBIT'` / `'AWC_CREDIT'` | queryable via c=9927 main wallet history |
| Mongo bet log | `vinplay.log_awc_bets` | per-bet audit, indexed for c=9895 history |

Per `MoneyGateway` (the canonical wallet entry point): every AWC
bet/win/cancel/refund/adjust/tip writes a money_gateway_log row
through `MoneyGateway.creditUser` / `debitUser` with source
`AWC_CREDIT` / `AWC_DEBIT`. CI guard at
`backend-master/scripts/check-no-currentmoney-trap.sh` blocks bypass.

---

## 8. Currency / amount handling

AWC currency codes follow ISO-4217 with sub-unit suffixes for some markets:

| AWC code | Operator code | Multiplier in (AWC→ops) | Multiplier out (ops→AWC) |
|---|---|---|---|
| `KRW` | KRW | ×1 | ÷1 |
| `IDR2` | IDR | ×1000 | ÷1000 |
| `VND2` | VND | ×1000 | ÷1000 |
| `THB` | THB | ×1 | ÷1 |
| `MYR` | MYR | ×1 | ÷1 |

Code: `AwcCallbackProcessor.getExchangeRateIn(currencyCode)` returns the
multiplier to convert AWC display amount → operator wallet sub-unit.
Existing GSC integration uses the same pattern (see
`game/thirdParty/.../gscSeamless/WithdrawProcess.java`).

**Always round** with `Math.round(...)` after multiplying — never
truncate, that drops fractional sub-units.

---

## 9. Error codes (operator → AWC)

| Code | Meaning | When to use |
|---|---|---|
| `0000` | Success | Normal happy path AND idempotent retry of a previously-applied tx |
| `1000` | Invalid userId | `userId` not in operator users table |
| `1001` | Invalid key | `key !== AWC_CERT` |
| `1003` | Insufficient balance | Bet exceeds player vin (no debit applied) |
| `1004` | Duplicate transaction | NOT recommended — AWC convention is to return `0000` instead. Use only if explicitly told. |
| `9999` | Internal error | Catch-all for unexpected exceptions |

---

## 10. Testing

### 10.1. Smoke a callback locally

```bash
# Bad key — expect 1001
curl -s -X POST -H "Content-Type: application/json" \
  https://staging-play.sunkr.bet/api?c=3097 \
  -d '{"key":"wrong","message":{"action":"getBalance","userId":"abc"}}'
# → {"status":"1001","desc":"Invalid key"}

# Good key, missing player — expect 1000
curl -s -X POST -H "Content-Type: application/json" \
  https://staging-play.sunkr.bet/api?c=3097 \
  -d '{"key":"PigVq2D07hNL","message":{"action":"getBalance","userId":"missing_user"}}'
# → {"status":"1000","desc":"Invalid userId"}
```

### 10.2. End-to-end via AWC test runner

The docs site exposes a Test Runner at:
`https://awc-docs.apihub888.com/test-runner.html`

Use the test agent (lime01) + cert. The runner sends real callbacks
to the configured webhook URL. Watch:
- backend-api logs (`docker logs sunwinkr-backend-api -f | grep AWC`)
- `money_gateway_log` for `AWC_DEBIT` / `AWC_CREDIT` rows
- `log_awc_bets` Mongo collection for per-bet audit rows
- Player vin moves correctly

### 10.3. Verify dedup

Replay the same `platformTxId`. Operator should return `{status:"0000"}`
WITHOUT modifying balance. The `awc_tx_log` row exists from first call;
duplicate detection short-circuits.

---

## 11. Reference: `AwcCallbackProcessor.execute` flow

Pseudocode (matches current code):

```java
1. Parse JSON or form body → JSONObject payload.
2. Verify payload.key === AwcConfig.cert().  → 1001 on mismatch.
3. JSONObject message = payload.message.
4. switch (message.action):
     case "getBalance":      handleGetBalance(message);
     case "bet":             handleBet(message, rawBody);
     case "settle":          handleSettle(message, rawBody);
     case "cancelBet":       handleCancelBet(message, rawBody);
     ... 15 more actions
   default:                  return errResp("9999", "Unknown action");
5. Each handler:
     a. Extract platformTxId, userId, betAmount, currency.
     b. Resolve userId → operator users.user_name via prefix lookup.
     c. Dedup check on awc_tx_log.
     d. Call MoneyGateway.debitUser / creditUser as needed.
     e. Return JSON envelope { status, desc, balance? }.
```

---

## 12. Production checklist

- [ ] `AWC_ENABLED=1` set in `.env`
- [ ] `AWC_CERT=<production cert>` set in `.env` (NOT the staging `PigVq2D07hNL`)
- [ ] `AWC_AGENT_ID=<production agent>` set in `.env`
- [ ] `AWC_PREFIX=<production prefix>` set in `.env`
- [ ] Webhook URL provided to AWC support (e.g. `https://prod-domain/api?c=3097`)
- [ ] HTTPS via Cloudflare A record (NOT tunnel — see CLAUDE.md)
- [ ] `awc_tx_log` table exists with UNIQUE INDEX on `platform_tx_id`
- [ ] `log_awc_bets` Mongo collection has indexes per MR !260
- [ ] `idx_user_platform_game_type_created_at_desc` for c=9895 history filter
- [ ] Smoke `getBalance` callback with prod cert returns operator-side balance
- [ ] Smoke a real bet → settle → verify vin moves + money_gateway_log + log_awc_bets

---

## 13. Source-of-truth links

- AWC OpenAPI spec: <https://awc-docs.apihub888.com/spec/openapi.json>
- Test runner: <https://awc-docs.apihub888.com/test-runner.html>
- Operator implementation: `backend-master/api/VinPlayPortal/src/main/java/com/vinplay/api/processors/awc/AwcCallbackProcessor.java`
- Money flow entry point: `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/MoneyGateway.java` (canonical)
- Mongo indexes: `install/config/mongo/changes/2026-04-28-betting-history-admin-awc-indexes.js`
