# Giftcode redemption — cache-only credit, MySQL never updated

**Discovered:** 2026-05-11 (via 4 player reports: testag01, Trunny, min9494, Trungkhanh03)
**Status:** Investigation complete, backfill + code fix pending
**Severity:** Player-impacting (in-game balance ≠ admin balance) but no money lost (cache has the credit, just not persisted)
**Scope:** 70 redemptions / 69 distinct users / 1,400,000 KRW potentially un-persisted

## Symptom

Players who redeem an admin giftcode (c=3070) see the credited amount in-game (TaiXiu, Sicbo, Slot, lobby HUD) but the admin panel and any MySQL-direct readers show the pre-redeem balance. The in-game balance comes from the Hazelcast `users` cache; the admin reads from `vinplay.users.vin`. The two diverge because the giftcode credit only ever lands in the cache.

## Root cause

`backend-master/api/VinPlayPortal/src/main/java/com/vinplay/api/processors/giftcode/RedeemAdminGiftCodeProcessor.java:170-211`

The redemption path credits via the legacy cache-first + async-RMQ pattern:

```java
userMap.lock(nickname);
freshUser.setVin(vinBefore + giftMoney);          // ← cache only
freshUser.setVinTotal(vinTotalBefore + giftMoney);
MessageBusFactory.get("queue_payment").publishOrThrow(
    "queue_payment", msgMoney, 16);                // ← async, expected to write MySQL
userMap.put(nickname, freshUser);
```

The async hop lands in `UpdateMoneyProcessor.execute` at `backend-master/api/vbee/src/main/java/com/vinplay/vbee/rmq/payment/processor/UpdateMoneyProcessor.java:118-128`:

```java
if (!updateTimeOut) {       // throughput-protection skip
    return true;              // ← MySQL write skipped
}
userDao.updateMoney(message, type);
```

`updateTimeOut=false` when the user was active in vin within the last 30 seconds. The SUN-796 comment explains the rationale: "skip redundant writes for hot users — the next message will write." Valid for hot players with continuous play. **Breaks for giftcode users who don't immediately play.**

A giftcode is often the player's first action of the day. They redeem, see the balance in the lobby, plan to play later — but never trigger another vin operation within the threshold. The MySQL write that was supposed to come from the "next message" never arrives. The cache stays right; MySQL stays wrong forever.

## Audit results

Query: `gift_code_useds.user_id` × current `users.vin_total < gift_codes.money`, last 30 days, non-bot.

| Tier | Users | KRW | Definition |
|---|---|---|---|
| A | 44 | 880,000 | `vin=0 AND vin_total=0` — zero MySQL activity, very high confidence affected |
| B (no MGL match) | 24 | 480,000 | Has activity, but no `money_gateway_log` entry of +20k near redemption — high confidence affected |
| B (MGL match) | 2 | 40,000 | Has activity AND a +20k MGL row near redemption — needs inspection (Noname, dong68) |
| Total | 70 | 1,400,000 | |

Issue concentrated on 2026-05-10 (51 affected of 119 redemptions) and 2026-05-11 (19 of 44). Pre-2026-05-10: 1 redemption, worked fine. Most likely cause for the 2026-05-10 spike: first high-volume giftcode bundle (`HIGH*` codes from bundle `1778399287788`) was rolled out that morning.

## Recommended fix sequence (do later)

### Phase 1 — Tier-A bulk refund (44 users, 880,000 KRW)

Idempotent via `tx_id = SUN1XXX-GC-<gc_id>-<user_id>` (UNIQUE constraint on `(tx_id, source)` in `money_gateway_log` prevents double-credit on re-run).

```sql
START TRANSACTION;

-- Audit rows
INSERT IGNORE INTO vinplay.money_gateway_log
  (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at)
SELECT
  u.id, u.nick_name, gc.money, 'GIFTCODE_BACKFILL',
  CONCAT('SUN1XXX-GC-', gc.id, '-', u.id),
  CONCAT('Giftcode ', gc.giftcode, ' (id ', gc.id, ') redeemed ',
         DATE_FORMAT(gcu.created_at, '%Y-%m-%d %H:%i:%s'),
         ' but UpdateMoneyProcessor skipped MySQL write (cache-only). Backfilled.'),
  u.vin + gc.money,
  NOW()
FROM vinplay.gift_code_useds gcu
JOIN vinplay.gift_codes gc ON gc.id = gcu.giftcode_id
JOIN vinplay.users u ON u.id = gcu.user_id
WHERE gc.type = 0 AND u.is_bot = 0
  AND u.vin = 0 AND u.vin_total = 0
  AND gcu.created_at >= NOW() - INTERVAL 30 DAY;

-- Apply the credit
UPDATE vinplay.users u
JOIN (
  SELECT u2.id, SUM(gc.money) AS refund_total
  FROM vinplay.gift_code_useds gcu
  JOIN vinplay.gift_codes gc ON gc.id = gcu.giftcode_id
  JOIN vinplay.users u2 ON u2.id = gcu.user_id
  WHERE gc.type = 0 AND u2.is_bot = 0
    AND u2.vin = 0 AND u2.vin_total = 0
    AND gcu.created_at >= NOW() - INTERVAL 30 DAY
  GROUP BY u2.id
) r ON r.id = u.id
SET u.vin = u.vin + r.refund_total,
    u.vin_total = u.vin_total + r.refund_total;

COMMIT;
```

Follow with HZ cache eviction for the 44 affected users so their next read picks up fresh DB. List of users in audit `vinplay._sun1_giftcode_backfill_20260511_tier_a` (create the table as part of execution for audit trail).

### Phase 2 — Tier-B without MGL match (24 users, 480,000 KRW)

Same pattern, different WHERE clause (`NOT (vin=0 AND vin_total=0)` + filter out the 2 MGL-match cases). Use a slightly different tx_id prefix (`SUN1XXX-GCB-`) so the two phases are independently auditable.

### Phase 3 — Manual review of 2 MGL-match cases

- `Noname` (id 9091, gc 236): inspect `money_gateway_log` description for the +20k row near 2026-05-11 00:16
- `dong68` (id 8949, gc 240): inspect for the +20k row near 2026-05-10 20:08

If the matching row is clearly a different source (deposit, refund), refund the giftcode. If it's the giftcode, leave alone.

### Phase 4 — Code fix in `RedeemAdminGiftCodeProcessor` (hot-swap to portal-api)

Replace lines 170-211 (cache+RMQ block) with a synchronous `MoneyGateway` call:

```java
MoneyGateway.CreditResultWithCumulative cr = MoneyGateway.creditUserWithCumulative(
    userId, nickname, "vin", giftMoney,
    "GIFTCODE_ADMIN",
    "GC-" + giftCodeId + "-" + userId,
    "Giftcode redeem: " + code
);
if (!cr.success) {
    logger.error("Giftcode credit failed nick=" + nickname + " err=" + cr.error);
    return err(response, "9998", "Wallet update failed, contact support");
}
long newBalance = cr.newBalance;
```

This guarantees:
- Synchronous MySQL UPDATE in the same transaction as the giftcode usage record
- Audit row in `money_gateway_log` with idempotency key
- HZ cache eviction post-commit (via the v3 MoneyGateway path)
- Future dual-write into `money_account` ledger (once `GIFTCODE_ADMIN` is registered in the source→ledger-type map per LEDGER_HARDENING_ROADMAP fix #5)

The current implementation's RMQ publish of `LogMoneyUserMessage` for `queue_log_money` should be preserved (or routed through the outbox in a follow-up) so commission attribution / VIP point accrual continues to flow.

Touches: `RedeemAdminGiftCodeProcessor.java` only. Same hot-swap technique as today's other fixes (build VinPlayPortal.jar → `docker cp` into `sunwinkr-portal-api` → `kill -TERM 1` → ~8s downtime).

### Phase 5 — Monitoring

Daily cron query:

```sql
SELECT COUNT(*) AS new_drift
FROM vinplay.gift_code_useds gcu
JOIN vinplay.gift_codes gc ON gc.id = gcu.giftcode_id
JOIN vinplay.users u ON u.id = gcu.user_id
WHERE gc.type = 0 AND u.is_bot = 0
  AND u.vin = 0 AND u.vin_total = 0
  AND gcu.created_at BETWEEN NOW() - INTERVAL 1 DAY AND NOW();
```

Alert if > 0 — means new redemptions are still hitting the bug, or the code fix is incomplete.

## Related architectural follow-up

This is the same class of bug described in [`WALLET_LEDGER_MIGRATION_PLAN.md`](../architecture/WALLET_LEDGER_MIGRATION_PLAN.md) Phase 2 — write paths that bypass MoneyGateway and rely on async cache-to-MySQL sync. The Phase 4 code fix above eliminates this specific instance; the migration plan's Phase 2 eliminates the failure mode systemically (lint task forbids any future code path that bypasses MoneyGateway).

## Files involved

- `backend-master/api/VinPlayPortal/src/main/java/com/vinplay/api/processors/giftcode/RedeemAdminGiftCodeProcessor.java` (the bug site)
- `backend-master/api/vbee/src/main/java/com/vinplay/vbee/rmq/payment/processor/UpdateMoneyProcessor.java` (the `updateTimeOut` skip path)
- `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/service/MoneyGateway.java` (the canonical fix target)
- `vinplay.gift_code_useds` (redemption log)
- `vinplay.gift_codes` (giftcode definitions)
- `vinplay.users` (vin / vin_total — where the MySQL write should have happened)
- `vinplay.money_gateway_log` (where the audit row should have been, isn't)
