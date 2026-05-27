# AWC vs GSC+ — Endpoint Comparison & Operator Controls

Side-by-side comparison of the two seamless-wallet providers integrated
on Sunwinkr platform: **AWC** (Asia Win Connect) and **GSC+** (Global
Service Connect Plus).

Sources:
- AWC OpenAPI spec at <https://awc-docs.apihub888.com/spec/openapi.json>
- GSC+ Seamless Wallet API v2.0.6 (`docs/ref/GSC+ Seamless Wallet API v2.0.6EN.md`)

---

## 0. Up-front: what the seamless protocol DOES and DOES NOT let the operator do

Both AWC and GSC+ implement the **seamless single-wallet** model. This
is a strict contract:

| Side | Owns | Controls |
|---|---|---|
| **Operator (us)** | The player wallet (vin balance, transaction log) | Deposits, withdrawals, debit/credit on game events, account suspension |
| **Provider (AWC/GSC)** | Game logic, RNG, deck, dice, slot reels, dealer | Round outcomes, bet acceptance/rejection, table seat eligibility |

### What the operator CAN do (control mechanisms)

| Mechanism | AWC | GSC+ | Effect |
|---|---|---|---|
| **Block bet** by returning insufficient balance | yes (status `1003` on bet callback) | yes (code `1001` "balance insufficient" on Withdraw callback) | Bet is rejected; player sees error from provider |
| **Force-logout / kick session** | `POST /wallet/logout` | not directly exposed; invalidate token → next provider call fails | Provider closes the player's window; can re-enter only after operator issues new launch URL |
| **Suspend/lock player** before launch | `POST /wallet/updatePlayerStatus` | refuse new `Launch Game` requests | Player can't enter NEW games; in-progress rounds finish on provider side |
| **Override bet limits** (max bet on live tables) | `POST /wallet/updateBetLimit` (LIVE only, e.g. SEXYBCRT) | implicit via launch params | Caps top-bet ceiling per-player on specific provider tables |
| **Create free rounds** (give specific player free spins) | `give` callback (AWC pushes promo) | `POST /api/operators/create-free-round` (operator pushes promo) | Player gets bonus rounds without spending vin |
| **Reject signature** | by failing `key !== cert` | by failing `sign` MD5 | Drops the callback (security, not gameplay control) |

### What the operator CANNOT do

These are **explicitly NOT in either API**:

- ❌ **See game outcomes before settle** — providers don't expose pre-result state. Operator only learns the result via `settle` callback (after the dealer has dealt / reels have spun).
- ❌ **Modify game results** — there is no "force lose" or "force win" callback. Settle amounts are computed provider-side based on RNG/dealer.
- ❌ **Kick a player off a table mid-round** — once a `bet` has fired, the round runs to completion on provider side. Operator can suspend the account, but the in-progress round still settles.
- ❌ **Intercept bets in real time** to change odds, deck composition, or table state. Operator only sees post-fact bet/settle messages.
- ❌ **See other players' cards** — provider doesn't share table state with operator.

The protocols are deliberately designed this way for licensing/compliance
(provider's RNG/dealer is independently audited; operator-side wallet
manipulation would void the audit).

### What "house edge" actually means here

The casino's mathematical edge is baked into the **game design at the
provider** (RTP%, dealer rules, deck count). Operator controls the
**business margin** via:
- Selecting which provider games to enable
- Setting commission rates with the provider (off-API contract)
- Bet-limit ceilings (capping max-bet exposure on volatile games)
- Player segmentation (high-roller bet limits, suspended accounts)

If a CMS feature needs to "force lose" or "intercept result", that's a
**bot/training game** the operator owns — typically the in-house
minigame stack (Tài Xỉu, Xóc Đĩa, Bầu Cua, etc), NOT AWC/GSC. Those have
their own RTP control via `cacheGameRtp` Hazelcast map (per CLAUDE.md
patterns). AWC/GSC games run on third-party RNG; the operator gets no
result control.

---

## 1. Endpoint comparison — Operator → Provider (we call them)

| Function | AWC endpoint | GSC+ endpoint | Notes |
|---|---|---|---|
| Create player on provider | `POST /wallet/createMember` | implicit (member auto-created on first launch) | AWC requires explicit provisioning; GSC just-in-time |
| Get login lobby URL | `POST /wallet/login` | `POST /api/operators/launch-game` (with `game_code=null`) | Both return a short-lived URL, default 10 min |
| Launch a specific game | `POST /wallet/doLoginAndLaunchGame` | `POST /api/operators/launch-game` (with `game_code` set) | Direct game entry, skips lobby |
| Force logout | `POST /wallet/logout` | (not direct; rely on token expiry) | AWC has explicit endpoint |
| Check session status | `POST /wallet/checkStatus` | (not direct) | AWC only |
| Lock/unlock player | `POST /wallet/updatePlayerStatus` | (refuse Launch Game) | AWC has dedicated endpoint |
| Update bet limit (LIVE) | `POST /wallet/updateBetLimit` | (via launch params on LIVE) | AWC tracks bet-limit IDs server-side |
| Query bet limit | `POST /wallet/queryBetLimit` | (n/a) | AWC only |
| Lobby state (open/closed/maintenance) | `POST /wallet/getLobbyState` | (n/a) | AWC exposes status; GSC requires per-game probe |
| Game/provider list | `POST /fetch/getPlatformListByAgent` | `POST /api/operators/games` (Game List) | List enabled games |
| Product/provider catalog | (returned by `getPlatformListByAgent`) | `POST /api/operators/products` (Product List) | Provider-level catalog |
| Maintenance schedule | `POST /wallet/getSchedule` | (n/a — check per-game `MAINTAINED` status) | AWC has dedicated schedule API |
| Jackpot pool snapshot | `GET /wallet/getJackpotPool` | (n/a) | AWC only |
| Get one transaction status | `POST /wallet/getTransactionStatus` | (use Wager API) | Both can lookup |
| Per-player tx history | `GET /wallet/getTransactionHistoryResult` | `POST /api/operators/wagers/list` (Wager List) | Both expose history |
| Operator-wide tx history | `POST /wallet/getTransactionHistoryResultAll` | `POST /api/operators/wagers/list` (without member_account filter) | Both expose history |
| Single wager detail | (n/a — use tx status) | `POST /api/operators/wagers/get` (Wager) | GSC has dedicated wager fetch |
| Game session history | (n/a) | `POST /api/operators/game-history` (Game History) | GSC only |
| Hourly betting summary | `GET /fetch/getSummaryByBetTimeHour` | (n/a) | AWC BI/reporting only |
| Promotion summary | `GET /fetch/getPromotionSummary` | (n/a) | AWC promo accounting |
| Resubmit cancelled bet notification | `POST /wallet/resubmitCancelbetNotification` | (n/a) | AWC retry-trigger; GSC retries automatically |
| Create free round | (none — AWC pushes via `give` callback) | `POST /api/operators/create-free-round` | Direction reversed between providers |
| Cancel free round | (none) | `POST /api/operators/cancel-free-round` | GSC only |
| Get player free-round bonus | (none) | `GET /api/operators/get-player-frb` | GSC only |
| Get game bet scales | (n/a) | `POST /api/operators/games-bet-scales` | GSC only |
| Wallet balance probe | (rely on internal vin) | `GET /api/operators/wallet/balance` | GSC has dedicated balance probe |
| Auto-deposit URL (USDT→fiat) | (n/a) | `POST /api/operators/recharge/order` | GSC integrates third-party USDT payment |
| Super Lobby (multi-provider lobby) | (lobby is part of `wallet/login`) | `POST /superlobby/launch` | GSC bundles 100+ providers in one lobby |

**Counts:** AWC outbound = 18; GSC+ outbound = 13.

---

## 2. Endpoint comparison — Provider → Operator callbacks (they call us)

This is where game events flow back. Operator hosts these endpoints.

| Action | AWC | GSC+ | Notes |
|---|---|---|---|
| Get player balance | `getBalance` | `Balance` (POST `/v1/api/seamless/balance`) | AWC dispatches by `message.action`; GSC has 1 dedicated path |
| Bet (debit) | `bet` | `Withdraw` (POST `/v1/api/seamless/withdraw`) with `transaction.action="bet"` | GSC bundles bet under "Withdraw" with action subtypes |
| Settle (credit win) | `settle` | `Deposit` (POST `/v1/api/seamless/deposit`) with `transaction.action="SETTLED"` | GSC bundles settle under "Deposit" |
| Refund / void | `refund` / `voidBet` / `voidSettle` | `transaction.action="ROLLBACK"` or `"CANCEL"` on Withdraw/Deposit | GSC uses action codes; AWC has dedicated callbacks |
| Cancel bet (pre-settle) | `cancelBet` | `transaction.action="CANCEL"` | Same semantic |
| Adjust bet amount | `adjustBet` | `transaction.action="ADJUSTMENT"` | Same semantic |
| Bet+settle (single-step) | `betNSettle` | not split — Withdraw+Deposit fire separately | AWC has combined action |
| Cancel bet+settle | `cancelBetNSettle` | sequential ROLLBACK callbacks | AWC combined |
| Resettle (after unsettle) | `resettle` | `transaction.action="RESETTLED"` | GSC warns this can cause negative balance (sportsbook fractional adjustments) |
| Unsettle (reverse settle) | `unsettle` | `transaction.action="ROLLBACK"` (negative amount) | |
| Unvoid bet | `unvoidBet` | (re-issue bet) | AWC explicit |
| Unvoid settle | `unvoidSettle` | (re-issue settle) | AWC explicit |
| Free spin reward | `freeSpin` | `transaction.action="FREEBET"` on Deposit | |
| Tip (player tips dealer) | `tip` | `transaction.action="TIP"` on Withdraw | |
| Cancel tip | `cancelTip` | (rollback tip) | AWC explicit |
| Promotional bonus credit | `give` | `transaction.action="PROMO"` / `"BONUS"` / `"LEADERBOARD"` on Deposit | |
| Jackpot win | (uses `settle`) | `transaction.action="JACKPOT"` on Deposit | GSC has dedicated jackpot type |
| Bet pre-reserve (sportsbook) | (n/a) | `transaction.action="BET_PRESERVE"` on Withdraw | GSC reserves balance pre-bet |
| Pre-reserve refund | (n/a) | `transaction.action="PRESERVE_REFUND"` on Deposit | GSC only |
| Push bet history (read-only sync) | (n/a — operator pulls via tx history) | `Push Bet Data` (POST `/v1/api/seamless/pushbetdata`) | GSC pushes settled bets to operator for BI/reporting |

**Counts:** AWC inbound callbacks = 19 distinct actions; GSC+ inbound = 4 endpoints (Balance, Withdraw, Deposit, PushBetData) but Withdraw/Deposit multiplex 13+ action subtypes.

**Architecture difference:**

- **AWC** — verb-based: 19 distinct callback URLs (or one dispatcher matching `message.action`). Cleaner mapping, action = endpoint.
- **GSC+** — noun-based: 3 wallet endpoints (Balance / Withdraw / Deposit), with all bet/settle/cancel/refund variations encoded in `transaction.action` field. Fewer paths to register but harder to inspect at a glance — every Withdraw could be a real bet, a tip, a sportsbook pre-reserve, or a rollback.

---

## 3. Authentication comparison

| | AWC | GSC+ |
|---|---|---|
| Outbound (we → provider) | Per-endpoint `agentId` + `password` in body | MD5 sign: `md5(request_time + secret_key + "<funcName>" + operator_code)` |
| Inbound (provider → us) | `key === cert` (shared secret) | MD5 sign: `md5(operator_code + request_time + "<funcName>" + secret_key)` |
| Replay protection | none (operator must dedup by `platformTxId`) | none (operator must dedup by `wager_code`/`transaction.id`) |
| Currency in payload | `currency: "KRW"` etc | `currency: "CNY"` etc |
| Display vs sub-unit | Multiplier per code (KRW=×1, IDR2=×1000) | Same (operator multiplies/divides per `getExchangeRateIn`) |

GSC+ signature is more crypto-rigorous (MD5 of concatenated fields with
secret) — harder to forge than AWC's plaintext key compare. But also
more error-prone (off-by-one in concatenation order = sign mismatch).

---

## 4. Operator control levers — how to actually use them on Sunwinkr

### 4.1. Block a player from new bets

| Want | AWC | GSC+ |
|---|---|---|
| Stop player NEW logins | `wallet/updatePlayerStatus` (set inactive) | refuse new `launch-game` calls in our `LaunchGSCGameProcessor` (return error or 4xx) |
| Boot active session | `wallet/logout` | invalidate the session token in our launch-token cache |
| Block specific bet | return `1003` on AWC `bet` callback | return `1001` (balance insufficient) on GSC `Withdraw` callback |

Common SunkR pattern: set `users.is_active=0`, then return 1001/1003 on
bet callback. Player loses on next round attempt.

### 4.2. Force a refund

| Want | AWC | GSC+ |
|---|---|---|
| Refund operator-side without provider knowing | direct UPDATE vin (banned per money-gateway lint) — must use MoneyGateway | same |
| Trigger provider-side cancel/refund | `wallet/resubmitCancelbetNotification` (AWC's own pendings only) | (no direct trigger; depends on provider helpdesk) |

Operator can credit the player back via MoneyGateway, but the provider
won't know — accounting won't match the AWC/GSC ledger. Must coordinate
with provider support for actual cancel.

### 4.3. Promo / give free money

| Want | AWC | GSC+ |
|---|---|---|
| Operator pushes a promo | (n/a — AWC pushes via `give` callback) | `create-free-round` for slot bonus rounds |
| Provider pushes a promo to specific player | `give` callback (AWC promo system) | `transaction.action="PROMO"` on Deposit |
| Custom in-house promo (not provider's) | direct credit via `MoneyGateway.creditUser(source="PROMO_BONUS")` — operator-side only | same |

### 4.4. Audit / forensics (post-mortem)

| Want | AWC | GSC+ |
|---|---|---|
| Find all bets by player on date X | `getTransactionHistoryResult` | `POST /api/operators/wagers/list` |
| Re-fetch a missing settle | `wallet/getTransactionStatus` | `POST /api/operators/wagers/get` |
| Trigger missing cancel notify | `wallet/resubmitCancelbetNotification` | (manual via support) |

---

## 5. Currency support comparison

| | AWC | GSC+ |
|---|---|---|
| Number of currencies | ~25 | ~50 (full list at line 1189 of GSC doc) |
| Sub-unit suffix convention | `IDR2` = ×1000 IDR, `VND2` = ×1000 VND | Same convention |
| KRW handling | direct (no sub-unit) | direct (no sub-unit) |
| Crypto support | none | USDT (only via `recharge/order` for deposits) |

---

## 6. Game catalog comparison

| | AWC | GSC+ |
|---|---|---|
| Live casino | SEXYBCRT, EVOLUTION (SEXY family) | most live providers (Pragmatic Play Live, Evolution, etc) |
| Slots | JILI, PG Soft, Habanero (selectively) | BIG portfolio (PG Soft, JILI, Pragmatic, Spadegaming, etc) |
| Sportsbook | BTI, AWS Sport (some agents) | SABA Sports (full integration) |
| Fish/Arcade | CQ9 Fish (with `Cq9FishTransfer` wallet transfer pre-bet) | similar |
| Bingo / lottery | limited | full |
| Free-round campaigns | via `give` only (AWC-controlled) | full operator-pushed via `create-free-round` |

GSC+ is the broader catalog. AWC is more focused on premium live + selected slots.

---

## 7. Error code comparison (operator-returned)

| Meaning | AWC code | GSC+ code |
|---|---|---|
| Success | `0000` | `0` |
| Player not found | `1000` | `1000` (API member does not exist) |
| Insufficient balance | `1003` | `1001` (balance insufficient) |
| Auth fail | `1001` (Invalid key) | `1002` (proxy key error) / `1004` (signature invalid) |
| Duplicate transaction | `0000` (idempotent return) | `1003` |
| Internal error | `9999` | `999` |
| Maintenance | (n/a) | `2000` |

**Note:** AWC convention says return `0000` on duplicate dedup (idempotent
success). GSC+ returns `1003` (Duplicate transaction). Different
behavior on retry — easy mistake.

---

## 8. Implementation pointers (Sunwinkr code)

| Component | AWC file | GSC+ file |
|---|---|---|
| Single-wallet callback dispatcher | `api/VinPlayPortal/.../awc/AwcCallbackProcessor.java` (c=3097) | `game/thirdParty/.../gscSeamless/{Withdraw,Deposit,Balance,PushBet}Process.java` |
| Outbound API client | `VbeeCommon/.../awc/AwcClient.java` (per-endpoint methods) | `VinPlayPortal/.../gsc/LaunchGSCGameProcessor.java` + others |
| Config / env | `AwcConfig.java` (`AWC_*` vars) | `ThirdPartyLoad.getGscConfig()` (`GSC_*` vars) |
| Audit row source labels | `MoneyGateway.SOURCE_AWC_DEBIT` / `SOURCE_AWC_CREDIT` | (uses GSC-specific source strings) |
| Bet log Mongo collection | `vinplay.log_awc_bets` | `win123club.log_gsc_bets` |
| Indexes | `idx_user_platform_game_type_created_at_desc` (MR !260) | `user_name_1_create_time_-1` |

---

## 9. TL;DR

1. **Both providers run the games on their side.** Operator only handles the wallet. There is **no banker-side cheating mechanism** in either API — no force-lose, no result interception, no mid-round kick. RTP audit lives at the provider.

2. **AWC** is verb-rich (19 callback actions, dedicated endpoints) and verbose. Easier to reason about per-action.

3. **GSC+** is verb-poor (3 wallet endpoints + 1 push) but multiplexes 13+ action subtypes via `transaction.action`. Compact protocol, harder to grep.

4. **Operator control mechanisms** in both APIs: block player, force logout, set bet limit, refuse balance, push promos. Limited to wallet-side actions; cannot tamper with game outcomes.

5. **For internal house-edge control** (Tài Xỉu, Xóc Đĩa, Bầu Cua, etc), use the in-house engine's RTP map (`cacheGameRtp` Hazelcast). NOT applicable to AWC/GSC games.

6. **Audit/compliance** — both APIs require operator to maintain idempotent dedup on platformTxId/wager_code. Sunwinkr does this via `awc_tx_log` (MySQL) and `log_gsc_bets` (Mongo) tables.
