# Evo void-bet incident — operator G7A1 — 2026-05-06

**Operator:** `G7A1`
**Member:** `sunkr_test`
**Provider / table:** Evolution Speed Baccarat A — `game_code = ndgvwvgthfuaad3q` — `product_code = 1002 (LIVE_CASINO)`
**Window:** 17:59–18:06 KST, 2026-05-06
**Behaviour:** GSC sent `action=ROLLBACK, wager_status=VOID` 3 seconds after `BET`, twice, for the same player on the same table. Operator side processed both flows correctly; player balance was preserved. Requesting Evolution-side investigation.

---

## Round 1 — wager_code `eAsVGrGgf8XtKvAhjF8xZB`

### T+0 — `POST /gsc/v1/api/seamless/withdraw` — 17:59:45.381 KST

```json
{
  "operator_code": "G7A1",
  "currency": "KRW",
  "request_time": "1778057985",
  "batch_requests": [{
    "member_account": "sunkr_test",
    "product_code": 1002,
    "game_type": "LIVE_CASINO",
    "transactions": [{
      "id": "e228f301-929f-4bca-83da-4d094ec89577",
      "action": "BET",
      "wager_status": "BET",
      "wager_code": "eAsVGrGgf8XtKvAhjF8xZB",
      "round_id": "3b96d7bc-b42f-475e-aee7-4088d4280ef6",
      "game_code": "ndgvwvgthfuaad3q",
      "amount": "-10000",
      "bet_amount": "10000",
      "valid_bet_amount": "10000",
      "prize_amount": "0"
    }]
  }]
}
```

**Operator response:** `code=0`, balance debited 10,000 KRW. Internal state: 21,150 → 11,150 KRW.

### T+2.94s — `POST /gsc/v1/api/seamless/balance` — 17:59:48.321 KST

**Operator response:**
```json
{ "code": 0, "data": [{ "code": 0, "balance": 11150,
  "product_code": 1002, "member_account": "sunkr_test" }] }
```

### T+3.11s — `POST /gsc/v1/api/seamless/deposit` — 17:59:48.492 KST

```json
{
  "operator_code": "G7A1",
  "currency": "KRW",
  "batch_requests": [{
    "member_account": "sunkr_test",
    "transactions": [{
      "id": "ba8209bd-cf4d-48ae-b0c8-d65217ad5967",
      "action": "ROLLBACK",
      "wager_status": "VOID",
      "wager_code": "eAsVGrGgf8XtKvAhjF8xZB",
      "round_id": "3b96d7bc-b42f-475e-aee7-4088d4280ef6",
      "game_code": "ndgvwvgthfuaad3q",
      "amount": "10000",
      "bet_amount": "10000",
      "prize_amount": "0",
      "settled_at": 1778057988407
    }]
  }]
}
```

**Operator response:** `code=0`, balance refunded 10,000 KRW. Internal state: 11,150 → 21,150 KRW.

---

## Round 2 — wager_code `WWMipQ6nLn56gb8bQKPx9F`

Identical pattern at 18:06:41.455 → 18:06:45.011 KST. Same `member_account`, same `game_code`, same `product_code=1002`, same `action=ROLLBACK / wager_status=VOID`. Same 3-second BET-to-rollback delta.

---

## What we need from GSC / Evolution

1. **Reason for void on these two rounds.** Round IDs:
   - `3b96d7bc-b42f-475e-aee7-4088d4280ef6` (round 1)
   - Round ID of wager `WWMipQ6nLn56gb8bQKPx9F` (round 2 — please retrieve)

   Possible causes we want confirmation on: bet arrived after the betting window closed, player session expired, table maintenance / paused, or an Evolution risk-engine rejection.

2. **Evolution-side error code or reason string** for each void. Our `/deposit` payload only carries `wager_status=VOID`, no reason field.

3. **Confirm scope.** Were these two rounds the only voids on table `ndgvwvgthfuaad3q` in this window, or were others affected? No other operator-side player got a void in the 17:55–18:10 KST window — suggests this is specific to `sunkr_test`'s session, not a table-wide event.

4. **3-second BET→ROLLBACK is unusually fast.** Indicates Evolution rejected at receipt time, not after the round resolved. Is this consistent with a network-latency or session-validation failure path on Evo's edge?

---

## Operator-side state — confirmed correct

| Step | Operator action | Status |
|---|---|---|
| BET received | Debited member_account, returned `code=0` | OK |
| Balance query | Returned correct post-debit value | OK |
| ROLLBACK received | Refunded, returned `code=0` | OK |
| Net balance impact for window | -150,000 / +160,000 / **net +10,000 KRW** | OK |
| `log_gsc_bets` cleanup after VOID | Deleted by `wager_code` per spec | OK |

No remediation requested from GSC. Investigation requested only.

---

**Contact:** [name / email]
**Operator tracking ID:** `SUN-1248 / 2026-05-06_sunkr_test_evo_void`
