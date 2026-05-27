# Agency Portal — Wallet Integration Spec

**Audience:** FE team (`sunkr-nextagency`), QC, BE engineers
**Last update:** 2026-04-27 (post Mr.DEAL clarification + MR !238 audit fix)
**Cross-references:** `docs/WALLET_SYSTEM.md`

---

## 1. Three wallets at a glance

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          AGENT IDENTITY                                  │
│                                                                          │
│   ┌──────────────────┐    ┌──────────────────┐   ┌──────────────────┐   │
│   │  Main Wallet     │    │  Agency Wallet   │   │  Credit Wallet   │   │
│   │  (Ví chính)      │    │  (Ví đại lý)     │   │  (Ví Credit)     │   │
│   │                  │    │                  │   │                  │   │
│   │  users.vin       │    │  agency_wallet   │   │  credit_wallet   │   │
│   │  + users.xu      │    │     .balance     │   │     .balance     │   │
│   │                  │    │                  │   │                  │   │
│   │  → for own play  │    │  → commission    │   │  → admin issued  │   │
│   │  → withdraw bank │    │     income only  │   │  → for funding   │   │
│   │                  │    │                  │   │     downline     │   │
│   └────────┬─────────┘    └────────┬─────────┘   └────────┬─────────┘   │
│            │                       │                      │             │
└────────────┼───────────────────────┼──────────────────────┼─────────────┘
             │                       │                      │
             ▼                       │                      ▼
   ┌──────────────────┐               │            ┌──────────────────┐
   │  bank withdraw   │               │            │  c=9923 Deposit  │
   │  (out of system) │               │            │  → another's vin │
   └──────────────────┘               │            └──────────────────┘
                                      │                      │
                                      ▼                      ▼
   ┌──────────────────┐    ┌──────────────────┐   ┌──────────────────┐
   │ ◄ DOWNLINE comm  │    │ c=9890 CONVERT   │   │ c=9922 Transfer  │
   │   from per-bet   │───▶│ agency → vin     │   │ → another's      │
   │   RealTimeCommis │    │ (internal)       │   │   credit_wallet  │
   └──────────────────┘    └──────────────────┘   └──────────────────┘
```

**Ownership:** all three wallets belong to the SAME agent (one identity, three pots).

**Key rules per SUN-1099 PM clarification (2026-04-25):**

| Rule | Constraint |
|---|---|
| Credit transfer scope | Direct upline/downline ONLY (1 level) |
| Credit-deposit scope | Self + direct downline ONLY (NO upline) |
| Agency → vin convert | bidirectional supported; NOT a deposit (KPI exclusion) |
| Agency → credit | NOT supported |
| SpecialAccount | All money actions blocked → errorCode 1099 |
| SELF rebate | Always lands in vin, PENDING until claimed via c=3083 |
| DOWNLINE commission | Real-time write to agency_wallet |

---

## 2. FE → BE endpoint map (sunkr-nextagency)

The FE wraps backend cmd IDs in Next.js API routes. Both layers shown below.

### 2.1 Read endpoints

| FE route | FE function | Backend cmd | Purpose | Required params |
|---|---|---|---|---|
| `GET /api/wallet/balance` | `useWalletBalance()` | `c=9460` GET_WALLET | Full 3-wallet balance for logged-in agent | `rc=user.code` (FE injects) |
| `GET /api/credit-wallet/balance?nn=...` | `useCreditBalance(nn)` | `c=9920` CREDIT_WALLET_BALANCE | Lookup credit balance by nickname (own or downline check) | `nn` |
| `GET /api/credit-wallet/history?...` | `useCreditHistory(filters)` | `c=9925` CREDIT_WALLET_HISTORY | Paginated credit_wallet_transactions | `page`, `size`, `type?`, `from?`, `to?` |
| (BE TODO #2) | (TBD) | `c=9891` AGENCY_WALLET_LOG | Agency wallet transaction history (BE ready, FE not wired yet) | `rc`, `pg`, `size`, `type?`, `ft?`, `et?` |
| (BE NEW — see Section 12) | (TBD) | `c=9927` GET_MAIN_WALLET_HISTORY | Agent's own main wallet (vin) history from  MySQL `money_gateway_log` | `rc`, `pg`, `size`, `category?`, `source?`, `ft?`, `et?` |

#### `c=9460 GET_WALLET` response shape (per `useCreditWallet.ts:WalletBalance`)

```ts
interface WalletBalance {
  agentId:        number   // useragent.id
  nick_name:      string
  main_wallet:    number   // users.vin
  main_wallet_xu: number   // users.xu
  agency_wallet:  number   // agency_wallet.balance
  credit_wallet:  number   // credit_wallet.balance
}
```

### 2.2 Mutation endpoints (3 wallet flows)

| FE route | FE function | Backend cmd | Direction |
|---|---|---|---|
| `POST /api/wallet/convert` | `useConvertAgencyToMain()` | `c=9890` CONVERT_WALLET | **agency_wallet → users.vin** (own) |
| `POST /api/credit-wallet/deposit` | `useCreditDeposit()` | `c=9923` CREDIT_WALLET_DEPOSIT | **credit_wallet → users.vin** (self / direct downline) |
| `POST /api/credit-wallet/transfer` | `useCreditTransfer()` | `c=9922` CREDIT_WALLET_TRANSFER | **credit_wallet → credit_wallet** (direct upline/downline) |
| `POST /api/credit-wallet/otp` | `useSendOTP()` | `SEND_OTP` (existing) | OTP for password-protected actions |

---

## 3. Endpoint reference (BE side, what FE talks to)

All admin endpoints accept `aat` (admin token) via `agentApi()`. Authentication done in BE; FE doesn't pass `aat` — it's resolved server-side via the Next.js session.

### 3.1 `c=9460` GET_WALLET — read full balance

**Request:**
```
GET /api_backend?c=9460&aat={token}&rc={agent_code}
```

**Response (success):**
```json
{
  "success": true,
  "errorCode": "0",
  "agentId": 205,
  "nick_name": "Kwon_DL1",
  "main_wallet": 10350000,
  "main_wallet_xu": 0,
  "agency_wallet": 193220,
  "credit_wallet": 99000
}
```

**Use case:** Sidebar display, dashboard wallet summary, polling after mutations.

### 3.2 `c=9890` CONVERT_WALLET — agency → main (1:1)

**Request:**
```
POST /api_backend?c=9890&aat={token}
Body (form-encoded):
  nn:     {agent_nickname}
  amount: {long, > 0}
  nt:     {optional note}
```

**Response (success):**
```json
{
  "success": true,
  "errorCode": "0",
  "withdrawnAmount": 50000,
  "newBalance": 10400000
}
```

**Error codes:**
| Code | Meaning |
|---|---|
| `1001` | Missing required param |
| `1002` | Invalid amount format |
| `1003` | Agent / user not found |
| `1004` | Insufficient agency_wallet balance |
| `1005` | Failed to credit main_wallet (auto-refunded) |
| `1099` | SpecialAccount blocked (when Wave 3 lands) |

**MR !238 fix:** This endpoint now writes a complete audit trail (was missing before):
- `agency_wallet_transactions` row, type=`CONVERT_TO_GAME`, direction=DEBIT
- `log_money_user` row, action_name=`CONVERT_AGENCY_TO_VIN`, money>0
- RMQ publish to `queue_action_portal` for FE real-time balance push

**KPI rule:** `CONVERT_AGENCY_TO_VIN` is **internal transfer**, NOT a deposit. Reports must EXCLUDE this action_name from total_deposit calculations.

### 3.3 `c=9923` CREDIT_WALLET_DEPOSIT — credit → main (self / direct downline)

**Request:**
```
POST /api_backend?c=9923&aat={token}
Body:
  code: {sender_agent_code}      # actor (FE auto-fills user.code)
  nn:   {target_nickname}        # recipient (agent or user)
  am:   {long, 1..10_000_000}
  tt:   "agent" | "user"         # target type
  pwd:  {sha256_withdraw_password}
  nt:   {optional note}
```

**Response (success):**
```json
{
  "success": true,
  "errorCode": "0",
  "sender_credit_balance": 89000,
  "amount": 10000,
  "target": "laviai",
  "target_type": "user",
  "promo_applied": true,
  "bonus_amount": 5000,
  "promo_type": "first_deposit"
}
```

**Validation rules:**
- `tt=agent`: target must be **direct upline OR direct downline** (1 level only — per SUN-1099)
- `tt=user`: target must be a downline player of `sender.code` (referral_code match)
- Self-deposit: target = sender → allowed (sender deposits to own vin)

**Error codes:**
| Code | Meaning |
|---|---|
| `4001` | Missing required param |
| `4002` | Invalid amount / out of range |
| `4004` | Withdraw password not set |
| `4005` | Wrong withdraw password |
| `4006` | `pwd` missing |
| `4009` | Target agent/user not found / frozen |
| `4010` | Target not direct upline/downline |
| `4011` | Cannot deposit to yourself (when `tt=agent` only) |
| `1003` | Target user not in downline (when `tt=user`) |
| `1099` | Sender or target is SpecialAccount |

### 3.4 `c=9922` CREDIT_WALLET_TRANSFER — credit → credit (direct only)

**Request:**
```
POST /api_backend?c=9922&aat={token}
Body:
  code: {sender_agent_code}
  to:   {receiver_nickname}     # must be agent
  am:   {long, 1..10_000_000}
  pwd:  {sha256_withdraw_password}
  nt:   {optional note}
```

**Response (success):**
```json
{
  "success": true,
  "errorCode": "0",
  "sender_balance": 79000,
  "amount": 10000,
  "receiver": "Kwon_DL2_A"
}
```

**Validation rules:**
- Receiver must be **direct upline OR direct downline** (1 level)
- Receiver must be active agent (not frozen, not SpecialAccount)
- Self-transfer: rejected (`4011`)

### 3.5 `c=9920` CREDIT_WALLET_BALANCE — read

**Request:**
```
GET /api_backend?c=9920&aat={token}&nn={nickname}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "agent_id": 205,
    "nick_name": "Kwon_DL1",
    "credit_wallet": 99000
  }
}
```

### 3.6 `c=9925` CREDIT_WALLET_HISTORY — paginated history

**Request:**
```
GET /api_backend?c=9925&aat={token}&page=1&size=20&type=DEPOSIT&from=2026-04-01&to=2026-04-27
```

**Response:**
```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1234,
        "type": "DEPOSIT",
        "direction": "DEBIT",
        "amount": 10000,
        "balance_after": 89000,
        "related_user": "laviai",
        "related_agent_id": 205,
        "note": "Auto promo bonus 5000",
        "created_at": "2026-04-27 13:42:00"
      }
    ],
    "total": 142,
    "page": 1,
    "size": 20
  }
}
```

---

## 4. Common flows (sequence diagrams)

### 4.1 Agent claims commission (agency → main)

```
Agent FE        Next.js         BE                  DB
   │              │              │                   │
   │── click "Convert" ────────▶ │                   │
   │              │              │                   │
   │              │── c=9890 ──▶ │                   │
   │              │              │── debit agency  ─▶│ agency_wallet -= X
   │              │              │── audit row    ──▶│ agency_wallet_transactions
   │              │              │── credit users.vin▶│ users.vin += X
   │              │              │── log_money_user ▶│ action=CONVERT_AGENCY_TO_VIN
   │              │              │── RMQ publish ───▶│ queue_action_portal
   │              │     ◀── ok ──│                   │
   │     ◀────── ok ──── │       │                   │
   │              │              │                   │
   │── poll c=9460 (or get WS push) ────────────▶    │ refresh balance
```

**Important:** `CONVERT_AGENCY_TO_VIN` is NOT a deposit. Don't surface in "lịch sử nạp tiền" UI; instead in "lịch sử ví" / wallet-internal-transfer view.

### 4.2 Agent funds user game wallet (credit → main)

```
Agent FE         Next.js          BE                          DB
   │              │                │                           │
   │── deposit modal: nn, amount, pwd ────▶                    │
   │              │                │                           │
   │              │── c=9923 ─────▶│                           │
   │              │                │── validate scope ─────────│ direct downline?
   │              │                │── verify pwd (SHA-256) ──▶│ users.withdraw_password
   │              │                │── debit credit_wallet ───▶│ credit_wallet -= X
   │              │                │── credit_wallet_transactions row (DEBIT)
   │              │                │── credit users.vin ───────▶│ users.vin += X
   │              │                │── log_money_user (target user)
   │              │                │── RMQ → user balance push
   │              │                │── apply first-deposit promo (if eligible)
   │              │      ◀── ok + promo info ─│                │
   │      ◀── ok ─│                │                           │
```

**Promo:** `c=9923` triggers first-deposit + daily-deposit promo logic (auto). FE just shows `promo_applied` + `bonus_amount`.

### 4.3 Agent transfers credit to peer agent

```
Agent FE      Next.js       BE                       DB
   │           │             │                        │
   │── transfer modal: to, amount, pwd ──▶            │
   │           │             │                        │
   │           │── c=9922 ──▶│                        │
   │           │             │── verify direct rel ──▶│ useragent parent/child?
   │           │             │── verify pwd ─────────▶│ users.withdraw_password
   │           │             │── transfer atomic ────▶│ tx: debit sender, credit receiver
   │           │             │── 2 credit_wallet_transactions rows (DEBIT + CREDIT)
   │           │   ◀── ok ───│                        │
```

---

## 5. Authentication summary

| Action | FE → Next.js auth | Next.js → BE auth |
|---|---|---|
| All endpoints | session cookie (`getSession()`) | `aat` (admin token) injected from session |
| Mutating endpoints | extra `assertCanMutate(user)` guard (RBAC permission check) | + `pwd` (SHA-256 of withdraw password) for credit ops |

**Withdraw password:** stored as SHA-256 hash in `users.withdraw_password`. FE must hash before sending. If user hasn't set one yet, server returns `4004` and FE should redirect to "Set Withdraw Password" flow.

---

## 6. Audit trail for QC verification

Every mutation writes audit. QC can verify with:

```sql
-- Agency wallet ops
SELECT * FROM vinplay.agency_wallet_transactions
WHERE agent_id = 205 ORDER BY id DESC LIMIT 10;

-- Credit wallet ops
SELECT * FROM vinplay.credit_wallet_transactions
WHERE agent_id = 205 ORDER BY id DESC LIMIT 10;

-- Main wallet (vin) movements via Money Gateway
SELECT id, action_name, money, current_money, trans_time
FROM vinplay.log_money_user
WHERE user_id = 50012 ORDER BY id DESC LIMIT 10;
```

**Internal transfer types** (excluded from deposit KPI):
- `CONVERT_AGENCY_TO_VIN` (c=9890)
- `nap_credit_dai_ly` (c=9923 to user — TBD confirm exact action_name)
- `chuyen_credit_dai_ly` (c=9922)
- All `COMMISSION_*` types

---

## 7. SpecialAccount enforcement (Wave 2 — MR !231)

Per PM SUN-1099 #5: SpecialAccount can ONLY login to agency portal (read-only balance view).

| Endpoint | SpecialAccount as actor | SpecialAccount as recipient |
|---|---|---|
| `c=3` (player login) | DENIED `1109` | n/a |
| `c=3083` (claim cashback) | DENIED `1099` | n/a |
| `c=3041` (withdraw bank) | DENIED `1099` | n/a |
| `c=9923` (credit deposit) | DENIED `1099` | DENIED `1099` |
| `c=9922` (credit transfer) | DENIED `1099` | DENIED `1099` |
| `c=9890` (convert agency) | TODO Wave 3 | n/a |

FE should match `errorCode === '1099'` and render the Vietnamese message: **"Tài khoản chỉ được phép xem số dư"**.

---

## 8. Error code quick-reference

| Code | Meaning | UI message (Vietnamese) |
|---|---|---|
| `0` | Success | — |
| `1001` | Required param missing / token invalid | "Phiên hết hạn, đăng nhập lại" |
| `1002` | Sender/target agent not found | "Không tìm thấy đại lý" |
| `1003` | Target user not in downline | "User không thuộc tuyến của bạn" |
| `1004` | Insufficient balance | "Số dư không đủ" |
| `1005` | Failed to credit destination | "Lỗi cộng tiền — đã tự hoàn về ví đại lý" |
| `1009` | Sender frozen / inactive | "Tài khoản bị tạm khóa" |
| `1099` | SpecialAccount blocked | "Tài khoản chỉ được phép xem số dư" |
| `1109` | SpecialAccount login denied | "Tài khoản này không thể đăng nhập từ portal player" |
| `4001` | Missing param | "Thiếu thông tin bắt buộc" |
| `4002` | Invalid amount | "Số tiền không hợp lệ" |
| `4004` | Withdraw password not set | "Vui lòng cài đặt mật khẩu rút tiền trước" |
| `4005` | Wrong withdraw password | "Mật khẩu rút tiền không đúng" |
| `4006` | `pwd` param missing | "Thiếu mật khẩu rút tiền" |
| `4009` | Target frozen | "Đại lý đích bị tạm khóa" |
| `4010` | Not direct relation | "Chỉ chuyển trực tiếp 1 cấp được" |
| `4011` | Can't transfer to self | "Không thể chuyển cho chính mình" |
| `9000` | Admin token required | "Phiên hết hạn" |
| `9001` | Admin token expired | "Phiên hết hạn" |
| `9002` | Cmd not found (deleted endpoint) | (should not happen if FE up-to-date) |
| `9999` | Internal error | "Lỗi hệ thống, thử lại sau" |

---

## 9. Live verification — commission cascade (proof, 2026-04-26)

Setup: `laviai` (player) → parent `Kwon_DL1` (agency) with `live_cat_Baccarat` rate = 1.50%.

After `laviai` bet on Evo Baccarat (`gsc_1002_always9baccarat1`) totaling 10,260,000:

```sql
SELECT * FROM rebate_logs WHERE agent_nickname='Kwon_DL1' ORDER BY id DESC LIMIT 1;
-- id=91, rebate_type=DOWNLINE, total_f1_volume=10260000, rate=1.50, amount=153900, status=PAID

SELECT * FROM agency_wallet_transactions WHERE agent_id=205 ORDER BY id DESC LIMIT 1;
-- id=79, type=COMMISSION_DOWNLINE, amount=153900, direction=CREDIT, balance_after=193220
```

10,260,000 × 1.5% = **153,900** ✓
agency_wallet[205].balance: 39,320 → 193,220 ✓

End-to-end commission cascade verified for Evo Gaming category.

---

## 10. Open BE TODO

| # | Task | Priority | Status |
|---|---|---|---|
| 1 | `c=9890` audit trail | 🔴 HIGH | ✅ DONE (MR !238) |
| 2 | Add agency_wallet history reader endpoint for FE (currently uses c=9891 but not wired) | 🟡 MED | TODO |
| 3 | SpecialAccount deny on `c=9890` (Wave 3) | 🟡 MED | TODO |
| 4 | WS balance push for agency_wallet (replace polling) | 🟢 LOW | TODO |
| 5 | Player invite-friends → player gets agency_wallet (Phase 2) | 🟢 LOW | wait PM B-clarification |
| 6 | Confirm `action_name` exact strings for KPI exclusion list | 🟡 MED | TODO with reporting team |

---

## 11. Quick FE/BE field-name parity check

FE TS interface (`useCreditWallet.ts`):
```ts
interface CreditTransaction {
  id, type, direction, amount, balance_after,
  related_user, related_agent_id, note, created_at
}
```

BE table `credit_wallet_transactions`: matching column names ✓

FE `WalletBalance`:
```
agentId, nick_name, main_wallet, main_wallet_xu, agency_wallet, credit_wallet
```

BE `c=9460` returns same flat shape ✓

If FE deviates from these names, doc here is canonical source of truth.

---

## 12. Unified history across 3 wallets — `c=9927` + `c=9891` + `c=9925`

For the FE "Lịch sử ví" tab where agent can see ALL wallet movements, three parallel endpoints share the same request/response shape:

| Wallet | Cmd | Storage | Filter dimension |
|---|---|---|---|
| **Main (vin)** | `c=9927` |  MySQL `money_gateway_log` | `category` (deposit/convert/promo/credit) OR exact `source` |
| **Agency** | `c=9891` | MySQL `agency_wallet_transactions` | `type` enum (COMMISSION_DOWNLINE / CONVERT_TO_GAME / etc.) |
| **Credit** | `c=9925` | MySQL `credit_wallet_transactions` | `type` enum (ADMIN_CREDIT / TRANSFER_OUT / TRANSFER_IN / DEPOSIT_TO_AGENT / DEPOSIT_TO_USER) |

### Shared param contract

All 3 endpoints accept:

| Param | Type | Default | Notes |
|---|---|---|---|
| `rc` | string | required | agent code OR nickname OR username |
| `pg` | int | 1 | page (1-indexed) |
| `size` | int | 20 | page size, max 100 |
| `ft` | string | none | from date `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss` |
| `et` | string | none | to date (inclusive) |
| `type` (9891 / 9925) **OR** `category` / `source` (9927) | string | none | wallet-specific filter — see table above |

### Shared response shape

```json
{
  "success": true,
  "errorCode": "0",
  "data": {
    "list": [ /* per-wallet row shape */ ],
    "total": 142,
    "page": 1,
    "size": 20
  }
}
```

### Per-wallet row schemas

**Main wallet (`c=9927`)** — MySQL `money_gateway_log` row:
```ts
{
  id: number,
  source: string,                 // "DEPOSIT_BANK", "ADMIN_TOPUP", "PROMO_BONUS",
                                  // "CONVERT_AGENCY_TO_VIN", "nap_credit_dai_ly", ...
  amount: number,                 // unsigned (always > 0; this table only stores credits)
  direction: 'CREDIT',            // hard-coded — money_gateway_log is credit-only
  tx_id: string | null,           // dedup key (null for admin/convert ops)
  description: string,
  balance_after: number,          // users.vin after this credit
  promo_applied: boolean,
  bonus_amount: number,           // promo bonus credited along with the main amount
  created_at: string              // "yyyy-MM-dd HH:mm:ss"
}
```

> Bet history (debits + game wins) is NOT in this stream — game-side
> writes go to Mongo `log_money_user_vin` and are surfaced via the
> existing player history endpoints (e.g. c=303). c=9927 is purely
> the MoneyGateway audit log: deposits, withdraws (when the withdraw
> path migrates to MoneyGateway), converts, promos, admin topups.

**Agency / Credit wallet (`c=9891` / `c=9925`)** — MySQL row:
```ts
{
  id: number,
  type: string,                   // enum, see filter list above
  direction: 'CREDIT' | 'DEBIT',
  amount: number,                 // unsigned, direction tells sign
  balance_after: number,
  related_user: string,           // counter-party nickname
  related_agent_id: number,       // counter-party agent id (credit only)
  game_action: string | null,     // for COMMISSION_* rows (agency only)
  note: string,
  created_at: string              // "yyyy-MM-dd HH:mm:ss"
}
```

### Filter shortcuts for `c=9927` (main wallet)

`category` is a server-side `source LIKE` shortcut — picks one if `source` is not given:

| `category` | Matches `source` |
|---|---|
| `deposit` | `DEPOSIT_%` OR `nap_%` OR `ADMIN_TOPUP` (real money in) |
| `convert` | `CONVERT_%` (e.g. `CONVERT_AGENCY_TO_VIN` — internal transfer) |
| `promo` | `PROMO_BONUS` (auto first-deposit / daily-deposit) |
| `credit` | `%credit%` (credit-wallet derived: `nap_credit_dai_ly`, etc.) |
| `all` (or omit) | no filter |

For finer control, pass `source=...` (exact match) and skip `category`.

### Sequence — agent opens "Lịch sử ví" tab

```
Agent FE                 Next.js                BE
   │                       │                     │
   │── tab: Main / Agency / Credit ─▶            │
   │                       │                     │
   │── if Main:  ──── /api/wallet/history ─────▶ │── c=9927 (MySQL money_gateway_log)
   │── if Agency:──── /api/agency-wallet/history▶ │── c=9891 (MySQL agency_wallet_transactions)
   │── if Credit:──── /api/credit-wallet/history▶ │── c=9925 (MySQL credit_wallet_transactions)
   │                       │                     │
   │                       │  ◀── { list, total, page, size }
   │  ◀── render rows + paginator + filters
```

FE renders one shared table component fed by whichever endpoint matches the active tab. Filter chips map to per-wallet enum values + the shared date range.

### FE TODO (sunkr-nextagency)

- [ ] Add `useMainWalletHistory({rc, category, source, ft, et, pg, size})` hook
- [ ] Add `useAgencyWalletHistory(...)` hook (currently missing)
- [ ] Add `/api/wallet/history` route → `agentApi(CMD.GET_MAIN_WALLET_HISTORY, params)`
- [ ] Add `/api/agency-wallet/history` route → `agentApi(CMD.AGENCY_WALLET_LOG, params)`
- [ ] Add `MAIN_WALLET_HISTORY = 9927` constant to `commands.ts`
- [ ] Build the 3-tab "Lịch sử ví" page using the shared response shape

---

End of spec.
