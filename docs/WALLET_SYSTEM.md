# Wallet System — Feature Document

**Version:** 1.0 — 2026-04-25
**Audience:** ops, QA, finance, BE engineers
**Purpose:** ground-truth reference for every money movement in the platform. Use this to verify deposit / withdraw logic and reconcile balances when a discrepancy is reported.

---

## 1. Overview

The platform has **three balance pools** owned by **three identities**.
A money movement always crosses at most one pool boundary at a time, and every
boundary cross is logged in a per-pool audit table.

| # | Wallet | Owner | Storage | Mutates via |
|---|---|---|---|---|
| 1 | **Player Game Wallet** | player (`vinplay.users`) | `users.vin` (real KRW) + `users.xu` (bonus) | `MoneyGateway.creditUser` + admin/withdrawal processors |
| 2 | **Agency Wallet** | agent (`vinplay_admin.useragent`) | `vinplay.agency_wallet.balance` | `RebateService.creditAgencyWallet` / `debitAgencyWallet` |
| 3 | **Credit Wallet** | agent (`vinplay_admin.useragent`) | `vinplay.credit_wallet.balance` | `CreditWalletService.{adminTopup, adminRevoke, transferToAgent, depositToGameWallet}` |

Every wallet has a paired `*_transactions` table that captures direction
(CREDIT / DEBIT), amount, balance-after, and the reason / counter-party.

### Identity actors

| Actor | Holds | Authenticates with |
|---|---|---|
| **Player** | Game Wallet (`vin`, `xu`) | player session token (`at`) |
| **Agent** | Agency Wallet + Credit Wallet | admin session token (`aat`) when acting as agent in CMS; player session if logging in as own player account |
| **Admin** | none of their own — operates on behalf of others | admin session token (`aat`) + RBAC permission |
| **System** | none — runs RMQ consumers + cron | service-to-service via shared instance, no token |

---

## 2. Wallet 1 — Player Game Wallet

### 2.1 Schema (vinplay.users)

| Column | Type | Semantic |
|---|---|---|
| `vin` | BIGINT | Real-money balance. Withdrawable. Currency = KRW (or equivalent). |
| `xu` | BIGINT | Virtual/bonus balance. **Not withdrawable.** Used for in-game spending only. Earned via promos and special events. |
| `vin_total` | BIGINT | Lifetime cumulative `vin` ever credited (audit, monotonic). |
| `xu_total` | BIGINT | Lifetime cumulative `xu`. |
| `recharge_money` | BIGINT | Sum of every "real deposit" (bank, crypto, card, agent-credit). Drives deposit-based promo eligibility. |
| `t_nap` | BIGINT | Sum of deposit amounts for the current period (used by reports). |
| `t_rut` | BIGINT | Sum of withdrawal amounts for the current period. |
| `nap_times` | INT | Lifetime deposit count. |
| `rut_times` | INT | Lifetime withdrawal count. |
| `money_vp` | INT | VIP points (separate currency, not transferable). |

### 2.2 The single mutation gateway

**`MoneyGateway.creditUser(userId, nick, amount, source, txId, desc)`** is the
canonical entry point for crediting `vin`. It does in one transaction:

1. `UPDATE users SET vin = vin + amount, recharge_money = recharge_money + amount` (recharge_money only when `source` is in the deposit set).
2. INSERT into `money_gateway_log` with the source code, txId, and description.
3. Evaluate promo rules (`DepositPromotionService` — first-deposit bonus, daily-deposit, etc.) and credit bonus to `vin`.
4. Push the new balance into Hazelcast `users` IMap.
5. Notify the connected game server of the new balance over the BitZero socket.

Direct `UPDATE users SET vin = …` writes exist for a few legacy paths (admin
balance adjust, agency-wallet → vin conversion) and should be migrated to go
through `MoneyGateway.creditUser` over time.

### 2.3 `vin` mutation reasons (column `money_gateway_log.source`)

| Source | Direction | Trigger |
|---|---|---|
| `DEPOSIT_BANK` | + | Bank deposit confirmed by gateway / SMS parsing |
| `DEPOSIT_CRYPTO` | + | Crypto deposit confirmed by `sunkr-usdt-gateway` |
| `DEPOSIT_TELEGRAM` | + | Telegram bot top-up |
| `CARD_RECHARGE` | + | Phone-card / prepaid-card |
| `ADMIN_TOPUP` | + | Manual admin balance adjust (c=100) |
| `CREDIT_WALLET_DEPOSIT` | + | Agent spends Credit Wallet to top-up player (c=9923) |
| `PROMO_BONUS` | + | Bonus credited by `DepositPromotionService` |
| `WITHDRAW_BANK` | − | Withdrawal approved by admin (debit side) |
| `WITHDRAW_CRYPTO` | − | Crypto withdrawal approved |
| `BET_PLACED` / game-loss | − | Game server reports a losing bet via RMQ |

### 2.4 Inbound flows (Game Wallet ↑)

| Flow | Endpoint | Actor | Processor / consumer | Effect |
|---|---|---|---|---|
| Bank deposit (SMS) | RMQ `log_recharge_sms` | system | `LogRechargeSMSProcessor` | +`vin`, +`recharge_money`, `nap_times++`, +`t_nap` |
| Bank deposit (Napas) | RMQ `log_recharge_bank_napas` | system | `LogRechargeBankNapasProcessor` | same |
| Crypto deposit | RMQ `crypto_deposit_event` | system | `CryptoDepositEventProcessor` | same |
| Card recharge | RMQ `log_recharge_card` | system | `SaveLogRechargeByCardProcessor` | +`vin`, +`recharge_money` |
| Admin manual top-up | c=100 | admin | `UpdateMoneyUserProcessor` | +`vin` only (no `recharge_money` — not a real deposit) |
| Agent → user via Credit Wallet | c=9923 | agent | `AgentCreditDepositProcessor` → `CreditWalletService.depositToGameWallet` | +`vin`, +`recharge_money`, agent's `credit_wallet.balance` -= amount |
| Self-rebate (agent plays own game) | RMQ `log_money_user` | system | `LogMoneyUserExtraProcessor` → `RebateService.creditGameWallet` | +`vin` directly |
| Game win | RMQ `log_money_user` | system | `LogMoneyUserExtraProcessor` (positive moneyExchange) | +`vin` |

### 2.5 Outbound flows (Game Wallet ↓)

| Flow | Endpoint | Actor | Effect |
|---|---|---|---|
| Player bank-withdraw request | c=1020 series | player | Creates `bank_withdrawals` row in **PENDING**. `vin` is not yet debited — see "two-phase" below. |
| Admin approves withdrawal | c=8100 series | admin | Status → APPROVED, `vin -= amount`, `t_rut += amount`, `rut_times++`, `money_gateway_log` (WITHDRAW_BANK). |
| Admin rejects withdrawal | c=8100 series | admin | Status → REJECTED, **no `vin` debit**. (This is the SUN-#28 area — careful with idempotency.) |
| Player crypto-withdraw request | c=crypto series | player | Creates `crypto_withdrawals` row. Same two-phase model. |
| Game bet (loss) | RMQ `log_money_user` | system | -`vin`, mongo `log_money_user_extra` row. |

> **Two-phase withdrawal model** — the PENDING request reserves but does not debit; the admin approval is what actually debits `vin`. Verify this on the specific processor when reconciling: a stuck PENDING with money already missing from `vin` is a bug.

### 2.6 What `vin_total` / `xu_total` should equal

`SUM(money_gateway_log.amount WHERE direction=CREDIT) ≈ users.vin_total`
plus initial seed data. If they diverge, look for direct `UPDATE users SET
vin_total` writes (legacy, should not happen in new code).

---

## 3. Wallet 2 — Agency Wallet

### 3.1 Schema (vinplay.agency_wallet)

| Column | Type | Semantic |
|---|---|---|
| `agent_id` | INT | FK → `vinplay_admin.useragent.id`, ON DELETE CASCADE |
| `balance` | BIGINT | Commission earnings pool, in same currency unit as `vin` |
| `updated_at` | TIMESTAMP | Last mutation |

### 3.2 What it represents

This is **commission earned from the downline + self-play**, NOT operating credit.
The agent does not control inflow — the system credits it on every losing bet from
their downline (and from their own game if they're configured as a self-player
agent). The agent controls outflow: convert it to their `vin` (game wallet).

### 3.3 Mutation reasons (column `agency_wallet_transactions.type`)

| Type | Direction | Trigger |
|---|---|---|
| `COMMISSION_DOWNLINE` | CREDIT | Differential commission on a downline player's losing bet (`LogMoneyUserExtraProcessor.calculateDifferential`) |
| `COMMISSION_SELF` | CREDIT | Agent plays their own game, the differential at their tier credits to themselves (post-MR !197) |
| `COMMISSION_BACKFILL` | CREDIT/DEBIT | Net delta after a math-correction migration (e.g. SUN-1086 Flyway V6) |
| `CONVERT_TO_GAME` | DEBIT | Agent withdraws from agency wallet → their `vin` |

### 3.4 Inbound — how commission lands

1. Player places a bet. Game server publishes the `log_money_user` RMQ message
   with `nickname`, `actionName` (game key), and `moneyExchange` (negative for a loss).
2. The `vbee` consumer's `LogMoneyUserExtraProcessor.execute` → `triggerAutoCommission`
   builds the agent chain (player → direct upline → ... → master agent) by walking
   `useragent.ancestors`.
3. Per-game commission rate is read from `vinplay.game_commission_rate(agent_nickname, game_key)`
   with the SUN-818 "unconfigured = 0" rule. The agent's global
   `useragent.commission_rate` is only a fallback when there's no per-game row.
4. `calculateDifferential` walks the chain in BigDecimal space (post-SUN-1086 v2)
   and emits one `Distribution` per agent with `volume × differential / 100`,
   FLOOR-rounded.
5. For each distribution where `amount > 0`:
   - INSERT `rebate_logs` row with `status='PAID'`, `rebate_type='DOWNLINE'` (or `SELF`).
   - `RebateService.creditAgencyWallet(agentId, amount, type, …)` does:
     - `INSERT … ON DUPLICATE KEY UPDATE balance = balance + amount` against `agency_wallet`.
     - INSERT into `agency_wallet_transactions` with `direction=CREDIT`, full audit trail.
6. For agents in the chain who got a **0-amount distribution** (small bets that
   FLOOR to zero — fixed by SUN-1096 / MR !203), we still write a 0-amount
   `rebate_logs` row so the rolling-history view (c=9541) shows the bet existed
   for that tier. Wallet is NOT credited for 0-amount rows.

### 3.5 Outbound — agent withdraws commission

`WithdrawAgencyWalletProcessor` (admin-side, agent identity proven by `aat`):

1. Read agent's current `agency_wallet.balance`.
2. Atomically debit: `UPDATE agency_wallet SET balance = balance − ? WHERE agent_id = ? AND balance >= ?`. If `affectedRows = 0`, abort with insufficient-funds.
3. `UPDATE users SET vin = vin + amount WHERE id = (agent's user row)`.
4. INSERT into `agency_wallet_transactions` with `type='CONVERT_TO_GAME'`, `direction=DEBIT`.
5. Update Hazelcast `users` cache with the new balance.

> **Verification probe:** after an agent says "the agency wallet number on the
> CMS doesn't match what I withdrew," the chain to walk is
> `agency_wallet_transactions WHERE agent_id = ?` (CREDITs are commission earned;
> DEBITs are CONVERT_TO_GAME); `SUM(CREDITs) − SUM(DEBITs)` must equal current
> `agency_wallet.balance`.

---

## 4. Wallet 3 — Credit Wallet

### 4.1 Schema (vinplay.credit_wallet)

| Column | Type | Semantic |
|---|---|---|
| `agent_id` | INT | FK → `vinplay_admin.useragent.id`, ON DELETE CASCADE |
| `balance` | BIGINT | Credit pool the agent uses to fund downlines |
| `created_at`, `updated_at` | DATETIME | Provisioning + last-mutation |

### 4.2 What it represents

This is **operating capital provided by the platform to the agent**. The platform
(via admin) tops it up; the agent uses it to (a) pay other agents in the same
TĐL branch or (b) deposit into a downline player's game wallet. Unlike Agency
Wallet, this is an admin-controlled pool that the agent draws from rather than
something the agent earns.

### 4.3 Mutation reasons (column `credit_wallet_transactions.type`)

| Type | Direction | Trigger |
|---|---|---|
| `ADMIN_CREDIT` | CREDIT | Admin tops-up agent's credit (c=9921) |
| `ADMIN_REVOKE` | DEBIT | Admin revokes credit (c=9924) |
| `TRANSFER_OUT` | DEBIT | Agent sends credit to peer agent (c=9922, sender side) |
| `TRANSFER_IN` | CREDIT | Agent receives credit from peer agent (c=9922, receiver side) |
| `DEPOSIT_TO_USER` | DEBIT | Agent deposits credit into a downline player's `vin` (c=9923) |
| `DEPOSIT_TO_AGENT` | DEBIT | Agent deposits credit into another agent's `vin` (c=9923) |

### 4.4 Inbound flows

| Flow | Endpoint | Actor | Authorization | Effect |
|---|---|---|---|---|
| Admin top-up | c=9921 | admin | `finance.credit_wallet` permission (post-SUN-907) | +`credit_wallet.balance`, `ADMIN_CREDIT` |
| Receive transfer from peer | c=9922 | agent | OTP + same TĐL branch as sender | +`credit_wallet.balance`, `TRANSFER_IN`; sender side gets `TRANSFER_OUT` |

### 4.5 Outbound flows

| Flow | Endpoint | Actor | Effect | Audit |
|---|---|---|---|---|
| Admin revoke | c=9924 | admin | -`credit_wallet.balance`, `ADMIN_REVOKE` | atomic — refuses if insufficient |
| Transfer to peer agent | c=9922 | agent | -`credit_wallet.balance` (sender), +balance on receiver | both sides logged; OTP-protected; same-TĐL constraint enforced |
| Deposit to user `vin` | c=9923 | agent | -`credit_wallet.balance`, +`users.vin`, +`recharge_money`, `nap_times++`, KM evaluated, `bank_withdrawals.required_volume` += amount | `credit_wallet_transactions` (`DEPOSIT_TO_USER`), `money_gateway_log` (CREDIT_WALLET_DEPOSIT), `deposit_transactions` row |
| Deposit to agent `vin` | c=9923 | agent | same as above but target is an agent | `DEPOSIT_TO_AGENT` |

### 4.6 Why credit-wallet deposits count as "deposits"

Because the player receives `+vin` from a real-money source backing the credit
(the platform funded the agent up-front), promo rules and required-volume
calculations treat it identically to a bank deposit. This is intentional —
prevents agent-credit from being a back-door bonus.

---

## 5. Cross-wallet matrix

|  →  | Player vin | Player xu | Agency wallet | Credit wallet |
|---|---|---|---|---|
| **Player vin** | n/a | promo-bonus path (system) | ❌ never | ❌ never |
| **Player xu** | ❌ never | n/a | ❌ never | ❌ never |
| **Agency wallet** | `WithdrawAgencyWalletProcessor` (CONVERT_TO_GAME) | ❌ never | n/a | ❌ never |
| **Credit wallet** | `CreditWalletService.depositToGameWallet` (DEPOSIT_TO_USER / DEPOSIT_TO_AGENT) | ❌ never | ❌ never | `transferToAgent` (peer, TRANSFER_OUT/IN) |
| **Admin (no wallet)** | `UpdateMoneyUserProcessor` (c=100) | promo special events | ❌ no admin write | `adminTopup` (c=9921), `adminRevoke` (c=9924) |
| **System (RMQ)** | bank/crypto/card credit | promo events | commission credit on bet | ❌ never |

### What is **not** allowed (verify if you see it happening — that's a bug)

- Agency wallet → Credit wallet (no API, no SQL path).
- Credit wallet → Agency wallet (same).
- Player vin → Credit wallet (player can't send money to agent; only admin or system can).
- xu → vin or vin → xu (separate currencies).
- Withdraw from xu (xu is not a real-money source).
- Direct `UPDATE users SET vin` outside `MoneyGateway` for a real money flow (legacy; flag for migration).

---

## 6. Audit tables — quick reference

| Table | Owner | What it records |
|---|---|---|
| `money_gateway_log` (vinplay) | platform | every `vin` credit and the source code |
| `bank_withdrawals` / `crypto_withdrawals` (vinplay) | player | request → approval lifecycle of a `vin` debit |
| `deposit_transactions` (vinplay) | player | external-deposit lifecycle (gateway-callback bookkeeping) |
| `agency_wallet_transactions` (vinplay) | agent | every `agency_wallet.balance` mutation |
| `credit_wallet_transactions` (vinplay) | agent | every `credit_wallet.balance` mutation |
| `rebate_logs` (vinplay) | system | computed differential commission per agent per bet |
| `rebate_payout` (vinplay) | system | settlement runs (period roll-up of `rebate_logs`) |
| `log_money_user_extra` (mongo, db: `win123club`) | system | every game transaction (for reports / agency rolling) |
| `log_admin` (vinplay_admin) | admin | every admin action (balance adjusts, etc.) |
| `admin_rbac_audit_logs` (vinplay_admin) | admin | RBAC permission grants / revokes |

---

## 7. Verification checklist (use when ops/QA reports a money discrepancy)

### Player wallet ("my balance is wrong")

1. `SELECT vin, xu, recharge_money, nap_times, rut_times FROM users WHERE nick_name = ?`.
2. `SELECT direction, source, amount, created_at, related_user FROM money_gateway_log WHERE user_id = (the user id) ORDER BY created_at`. Sum should reconcile.
3. Check pending withdrawals: `SELECT * FROM bank_withdrawals WHERE user_id = ? AND status = 'PENDING'` — those amounts are **not yet debited** but UI may show them as deducted.
4. Check Hazelcast cache vs DB: a stale cache after a hot-swap can show a different `vin` than the canonical value in `users`.

### Agency wallet ("commission missing")

1. `SELECT balance FROM agency_wallet WHERE agent_id = (lookup from useragent)`.
2. `SELECT direction, type, amount, created_at, related_user FROM agency_wallet_transactions WHERE agent_id = ? ORDER BY created_at`. CREDITs − DEBITs must equal balance.
3. Cross-check `rebate_logs` for the same agent: `SUM(rebate_amount) WHERE agent_user_id = ? AND status = 'PAID'` should equal `SUM(amount)` of CREDIT transactions on the wallet, ± the SUN-1086 backfill delta if that ever ran.
4. For "agent X says they should have earned Y": query `LogMoneyUserExtraProcessor.calculateDifferential` test inputs — the bet, the chain rates, and the differential should produce that Y. Off-by-one fingerprint is the SUN-1086 root cause (closed by V6).

### Credit wallet ("agent says they have less credit than expected")

1. `SELECT balance FROM credit_wallet WHERE agent_id = ?`.
2. `SELECT direction, type, amount, created_at, related_user, related_agent_id, note FROM credit_wallet_transactions WHERE agent_id = ?`.
3. Reconcile with admin top-up history: `SELECT * FROM log_admin WHERE action LIKE 'credit_wallet%' AND username = (agent's owner admin) ORDER BY timestamp` — every `ADMIN_CREDIT` should have a paired admin action.
4. For peer transfers, the `TRANSFER_OUT` row on the sender must have a matching `TRANSFER_IN` row on the receiver with the same `created_at`. Mismatch indicates a partial-write bug.

### Cross-wallet ("agent claims they moved money but it didn't arrive")

| Symptom | Probe |
|---|---|
| Agency wallet → vin missing | `SELECT * FROM agency_wallet_transactions WHERE agent_id=? AND type='CONVERT_TO_GAME' ORDER BY created_at DESC LIMIT 5`. Then check `money_gateway_log WHERE source='AGENCY_WITHDRAW'` for the user side (or a direct `vin` UPDATE in the legacy path). |
| Credit wallet → user vin missing | `SELECT * FROM credit_wallet_transactions WHERE type='DEPOSIT_TO_USER' AND related_user=?`. Then `SELECT * FROM money_gateway_log WHERE source='CREDIT_WALLET_DEPOSIT' AND user_id=(target)`. Both must exist. |
| Peer credit transfer one-sided | Sender has `TRANSFER_OUT` but receiver has no `TRANSFER_IN`. Indicates a transactional gap — escalate. |

---

## 8. Open questions / things to verify next

These are **not** documented yet — when QA wants to certify them, run the
verification probes above and update this document.

1. **Promo bonus path for `xu`** — code paths credit `xu` only via promo events;
   no `MoneyGateway` analogue. Confirm whether `xu` writes go through a single
   gateway or are scattered across mission/promo processors.
2. **Withdrawal "reserve" semantics** — does the PENDING `bank_withdrawal` row
   block re-withdrawal of the same `vin` for a player who initiates two requests
   in parallel? If not, that's a race.
3. **Hot-swap cache freshness** — after a vbee restart, does the
   `agency_wallet.balance` Hazelcast cache need an explicit invalidation, or is
   it lazy-loaded? (Today it's pull-on-read; verify post-deploy.)
4. **xu → KRW conversion** — confirmed disallowed in code. If a future product
   feature wants `xu`-to-`vin` conversion, this doc needs a new section.
5. **AWC / GSC seamless wallets** — those providers maintain their own balance
   on the third-party side; transfer in/out of the player `vin` is brokered by
   `AwcApiClient.transferIn/transferOut` and the GSC equivalents. Document them
   in a separate "Third-party Aggregator Money Movement" section if/when QA
   reconciles those flows.

---

## 9. References

- Source files cited in this doc:
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/MoneyGateway.java`
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/CreditWalletService.java`
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/dao/impl/AgencyWalletDaoImpl.java`
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/dao/impl/CreditWalletDaoImpl.java`
  - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/rebate/RebateService.java`
  - `backend-master/api/vbee/src/main/java/com/vinplay/vbee/rmq/log/processor/LogMoneyUserExtraProcessor.java`
  - `backend-master/api/VinPlayBackend/src/main/java/com/vinplay/api/backend/processors/agent/WithdrawAgencyWalletProcessor.java`
  - `backend-master/api/VinPlayBackend/config/api_backend.xml` (command-id registry)
- Related tickets:
  - SUN-846 — agency_wallet existence + DOWNLINE PAID semantics
  - SUN-1086 — BigDecimal subtraction for differential commission (V6 backfill)
  - SUN-1094 (#46 epic) — decimal display + full-chain zero-tracking
  - SUN-907 — RBAC for `finance.credit_wallet` permission
- Migrations: `install/flyway/sql/V*` (V3 column rename, V4 self-commission backfill, V5 RBAC perms, V6 SUN-1086 backfill)

---

*Last reviewed: 2026-04-25. Review cadence: after every commission engine change or wallet-table schema change.*
