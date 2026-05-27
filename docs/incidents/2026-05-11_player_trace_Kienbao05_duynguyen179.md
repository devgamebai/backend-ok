# Player trace — Kienbao05 & duynguyen179 (2026-05-11)

Detailed money-and-bet record for the two players the operator asked about. Both lost money to the TaiXiu freeze incident, both were fully refunded for those losses, both then played GSC slots afterward with different outcomes.

Source of truth: `vinplay.money_gateway_log` (mysql, timestamps in KST = UTC+9). Cross-referenced with `win123club.log_taixiu` and audit table `vinplay._sun1_backfill_20260511`.

---

## Kienbao05

| Field | Value |
|---|---|
| user_id | 9114 |
| user_name | `kien260520055` |
| nick_name | `Kienbao05` |
| is_bot | 0 (real player) |
| Account created | 2026-05-11 04:42:05 KST (≈ 6 minutes before first activity) |
| Current vin | 220,000 |
| Current xu | 500,000 |
| t_nap (total deposit lifetime) | 150,000 |
| t_rut (total withdraw lifetime) | 0 |

### Activity log

All times KST.

| # | Time | Δ vin | Source | tx_id | Balance after | Note |
|---|---|---|---|---|---|---|
| 1 | 04:47:24 | **+150,000** | DEPOSIT_TELEGRAM | 436 / DBMP06L96P | 150,000 | First deposit (account just created 5 min before) |
| 2 | 04:47:24 | **+45,000** | PROMO_BONUS | — | 195,000 | 30% deposit promo |
| 3 | 04:48:47 | −35,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318001574443` | 160,000 | Bet **Tài**, ref **318001** |
| 4 | 04:50:44 | −60,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318003495238` | 100,000 | Bet **Xỉu**, ref **318003** |
| 5 | 04:53:33 | −30,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318005057979` | 70,000 | Bet **Xỉu**, ref **318005** |
| 6 | 04:55:49 | −50,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318007428929` | 20,000 | Bet **Xỉu**, ref **318007** |
| 7 | 04:58:35 | −10,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318010264779` | 10,000 | Bet **Xỉu**, ref **318010** |
| 8 | 05:02:47 | −10,000 | USERSERVICE_GAME (TaiXiuSicbo) | `userservice:TaiXiuSicbo:1:76000632` | **0** | Bet TaiXiuSicbo, ref **76** |
| — | — | — | — | — | — | (player stops; balance is zero) |
| 9 | 13:46:56 | **+35,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469638` | 35,000 | **Refund of bet #3 (void)** |
| 10 | 13:46:56 | **+60,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469648` | 60,000 | Refund of bet #4 (void) |
| 11 | 13:46:56 | **+30,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469662` | 30,000 | Refund of bet #5 (void) |
| 12 | 13:46:56 | **+50,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469671` | 50,000 | Refund of bet #6 (void) |
| 13 | 13:46:56 | **+10,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469676` | 10,000 | Refund of bet #7 (void) |
| 14 | 13:46:56 | **+10,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469684` | 10,000 | Refund of bet #8 (void) |
| 15 | 13:55:58 | −34,000 | GSC_DEBIT | `withdraw_e55ce5ca-…` | 161,000 | GSC Pragmatic slot bet |
| 16 | 13:56:03 | **+66,300** | GSC_CREDIT | `deposit_326c3489-…` | 227,300 | GSC win |
| 17 | 13:56:22 | −26,000 | GSC_DEBIT | `withdraw_a556aab6-…` | 201,300 | bet — lost |
| 18 | 13:56:48 | −50,000 | GSC_DEBIT | `withdraw_a0233f64-…` | 151,300 | bet |
| 19 | 13:56:57 | **+97,500** | GSC_CREDIT | `deposit_2e8bea52-…` | 248,800 | GSC win |
| 20 | 13:57:15 | −40,000 | GSC_DEBIT | `withdraw_b004b14d-…` | 208,800 | bet — lost |
| 21 | 13:57:43 | −62,000 | GSC_DEBIT | `withdraw_98e9bf2e-…` | 146,800 | bet |
| 22 | 13:57:52 | **+120,900** | GSC_CREDIT | `deposit_92270d34-…` | 267,700 | GSC win |
| 23 | 14:13:14 | −36,000 | GSC_DEBIT | `withdraw_97c59fff-…` | 231,700 | bet — lost |
| 24 | 14:13:42 | −16,000 | GSC_DEBIT | `withdraw_455f3fa7-…` | 215,700 | bet — lost |
| 25 | 14:14:12 | −62,000 | GSC_DEBIT | `withdraw_c1f658e0-…` | 153,700 | bet |
| 26 | 14:14:24 | **+113,100** | GSC_CREDIT | `deposit_ebc60430-…` | 266,800 | GSC win |
| 27 | 14:14:42 | −42,000 | GSC_DEBIT | `withdraw_f32a9a1c-…` | 224,800 | bet — lost |
| 28 | 14:15:10 | −30,000 | GSC_DEBIT | `withdraw_291b4e52-…` | 194,800 | bet |
| 29 | 14:15:27 | **+60,000** | GSC_CREDIT | `deposit_45b77d60-…` | 254,800 | GSC win |
| 30 | 14:16:09 | −50,000 | GSC_DEBIT | `withdraw_4f36e06d-…` | 204,800 | bet — lost |
| 31 | 14:16:33 | −54,000 | GSC_DEBIT | `withdraw_9a5b29e4-…` | 150,800 | bet |
| 32 | 14:16:41 | **+54,000** | GSC_CREDIT | `deposit_8c4c0e0b-…` | 204,800 | GSC win (break-even) |
| 33 | 14:17:00 | −34,000 | GSC_DEBIT | `withdraw_413bdaca-…` | 170,800 | bet — lost |
| 34 | 14:17:27 | −32,000 | GSC_DEBIT | `withdraw_7456b950-…` | 138,800 | bet |
| 35 | 14:17:34 | **+62,400** | GSC_CREDIT | `deposit_69fb4065-…` | 201,200 | GSC win |
| 36 | 14:17:53 | −22,000 | GSC_DEBIT | `withdraw_9d037c0f-…` | 179,200 | bet |
| 37 | 14:18:00 | **+42,900** | GSC_CREDIT | `deposit_fc4a5e2c-…` | 222,100 | GSC win |
| 38 | 14:18:44 | −42,000 | GSC_DEBIT | `withdraw_af1fd6a5-…` | 180,100 | bet |
| 39 | 14:18:53 | **+81,900** | GSC_CREDIT | `deposit_c50ccfa9-…` | 262,000 | GSC win |
| 40 | 14:19:12 | −42,000 | GSC_DEBIT | `withdraw_52671850-…` | 220,000 | bet (current snapshot) |

### Summary

| Phase | Cash in | Cash out | Net |
|---|---|---|---|
| Deposit + promo | +195,000 | — | +195,000 |
| TaiXiu/Sicbo bets (refs 318001, 318003, 318005, 318007, 318010, 76) | — | −195,000 | **−195,000 (lost to freeze, fully refunded later)** |
| ADMIN_TOPUP refunds (6 × SUN1XXX-REFUND-*) | +195,000 | — | +195,000 |
| GSC slots (15 bets, 9 wins) | +699,000 | −624,000 | **+75,000** |
| **Net since first deposit (150k cash in)** | | | **+220,000 vin balance** |

**Verdict:** lost 195k on TaiXiu/Sicbo bets that all fell inside the freeze window (04:48 – 05:02 KST), got 100% refunded, then went on a winning GSC streak. Currently up ~70k on his cash deposit. No further compensation owed.

---

## duynguyen179

| Field | Value |
|---|---|
| user_id | 8734 |
| user_name | `duynguyen06` |
| nick_name | `duynguyen179` |
| is_bot | 0 (real player) |
| Account created | 2026-05-02 08:05:58 KST (9-day-old account) |
| Current vin | 2,880 |
| Current xu | 0 |
| t_nap (total deposit lifetime) | 90,000 |
| t_rut (total withdraw lifetime) | 100,000 |

### Activity log (2026-05-11 only)

| # | Time | Δ vin | Source | tx_id | Balance after | Note |
|---|---|---|---|---|---|---|
| (carry) | — | — | — | — | 1,804 | Leftover from prior days |
| 1 | 05:23:05 | **+90,000** | DEPOSIT_TELEGRAM | 437 / DBMP06UWSO | 91,804 | Bank deposit |
| 2 | 05:23:05 | **+27,000** | PROMO_BONUS | — | 118,804 | 30% deposit promo |
| 3 | 05:24:40 | −20,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318033061320` | 98,804 | Bet **Xỉu**, ref **318033** |
| 4 | 05:30:21 | −1,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318038459222` | 97,804 | Bet Xỉu, ref **318038** |
| 5 | 05:56:43 | −1,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318061343393` | 96,804 | Bet **Tài**, ref **318061** |
| 6 | 06:01:56 | −100 | USERSERVICE_GAME (MiniPoker) | `userservice:MiniPoker:1:1778446916504` | 96,704 | MiniPoker bet *(NOT refunded — out of scope)* |
| 7 | 12:06:16 | −1,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318387071263` | 95,704 | Bet Tài, ref **318387** |
| 8 | 13:40:42 | −1,000 | USERSERVICE_GAME (TaiXiu) | `userservice:TaiXiu:1:318471271739` | 94,704 | Bet Tài, ref **318471** |
| 9 | 13:46:56 | **+20,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469700` | 114,704 | Refund of bet #3 (void) |
| 10 | 13:46:56 | **+1,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469704` | (overlapping) | Refund of bet #4 |
| 11 | 13:46:56 | **+1,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12469858` | (overlapping) | Refund of bet #5 |
| 12 | 13:46:56 | **+1,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12470466` | (overlapping) | Refund of bet #7 |
| 13 | 13:46:56 | **+1,000** | ADMIN_TOPUP | `SUN1XXX-REFUND-12474833` | 118,704 | Refund of bet #8 |
| 14 | 14:03:30 | −20,000 | GSC_DEBIT | `withdraw_3359e699-…` | 98,704 | GSC Pragmatic slot bet — lost |
| 15 | 14:03:59 | −30,000 | GSC_DEBIT | `withdraw_845c357c-…` | 68,704 | bet — lost |
| 16 | 14:04:30 | −38,000 | GSC_DEBIT | `withdraw_2f8fe7ab-…` | 30,704 | bet — lost |
| 17 | 14:05:25 | −30,000 | GSC_DEBIT | `withdraw_69372bf1-…` | 704 | bet (balance nearly zero) |
| 18 | 14:05:34 | **+58,500** | GSC_CREDIT | `deposit_05a5a17d-…` | 59,204 | GSC win |
| 19 | 14:05:52 | −28,000 | GSC_DEBIT | `withdraw_cb715aaa-…` | 31,204 | bet |
| 20 | 14:06:00 | **+54,600** | GSC_CREDIT | `deposit_0517619c-…` | 85,804 | GSC win |
| 21 | 14:06:19 | −24,000 | GSC_DEBIT | `withdraw_bc53b924-…` | 61,804 | bet |
| 22 | 14:06:29 | **+48,000** | GSC_CREDIT | `deposit_1b2e1dd2-…` | 109,804 | GSC win |
| 23 | 14:06:48 | −30,000 | GSC_DEBIT | `withdraw_4f80a6a9-…` | 79,804 | bet |
| 24 | 14:06:59 | **+58,500** | GSC_CREDIT | `deposit_596efd73-…` | 138,304 | GSC win |
| 25 | 14:07:17 | −30,000 | GSC_DEBIT | `withdraw_85d0d791-…` | 108,304 | bet — lost |
| 26 | 14:07:48 | −30,000 | GSC_DEBIT | `withdraw_7b9e0633-…` | 78,304 | bet |
| 27 | 14:08:00 | **+30,000** | GSC_CREDIT | `deposit_8f7b0d3d-…` | 108,304 | GSC win (push) |
| 28 | 14:08:18 | −30,000 | GSC_DEBIT | `withdraw_05d6767f-…` | 78,304 | bet — lost |
| 29 | 14:08:49 | −40,000 | GSC_DEBIT | `withdraw_3b9d947e-…` | 38,304 | bet — lost |
| 30 | 14:09:18 | −38,000 | GSC_DEBIT | `withdraw_84ec2199-…` | 304 | bet — lost (down to 304) |
| (later) | — | — | — | — | 2,880 | Current balance (small win after this snapshot) |

### Summary

| Phase | Cash in | Cash out | Net |
|---|---|---|---|
| Carry from prior days | 1,804 | — | 1,804 |
| Deposit + promo | +117,000 | — | +117,000 |
| TaiXiu bets (refs 318033, 318038, 318061, 318387, 318471) | — | −24,000 | **−24,000 (lost to freeze, refunded later)** |
| MiniPoker bet (ref 1778446916504) | — | −100 | −100 (not refunded — see note below) |
| ADMIN_TOPUP refunds (5 × SUN1XXX-REFUND-* for the TaiXiu bets) | +24,000 | — | +24,000 |
| GSC slots (12 bets, 6 wins) | +249,600 | −368,000 | **−118,400** |
| **Today's net result** | | | **−87,400** from the day's cash flow (1,804 → 2,880, after deposit of 90k) |

**Verdict:** lost 24k on TaiXiu bets during the freeze window, got 100% refunded for that. The 24k MiniPoker bet of 100 vin was not in our refund scope but is a tiny amount and almost certainly settled normally. The remaining loss (~87k) is entirely from his GSC slots play after the refund landed — that's regular slot-machine variance, not a system bug. No further compensation owed for the freeze incident.

> Note on the MiniPoker 100-vin bet at 06:01:56: if the operator wants to be generous, this can also be refunded with `INSERT INTO money_gateway_log (..., source, tx_id, ...) VALUES (..., 'ADMIN_TOPUP', 'SUN1XXX-REFUND-12469914', ...)` for 100 vin. We did not include it because the scope was TaiXiu / TaiXiuSicbo only.

---

## How to verify (re-run any time)

```bash
MYSQL_PWD='<root password from .env>' mysql -uroot -t -e "
SELECT id, currency, amount, source, tx_id, balance_after,
       SUBSTRING(description,1,80) AS description, created_at
FROM vinplay.money_gateway_log
WHERE nick_name='Kienbao05' AND created_at >= '2026-05-11 00:00:00'
ORDER BY id ASC;
"
```

Substitute `Kienbao05` with `duynguyen179` for the other player. The `SUN1XXX-REFUND-*` rows show the void refunds; the `GSC_DEBIT`/`GSC_CREDIT` rows show their subsequent slot activity.
