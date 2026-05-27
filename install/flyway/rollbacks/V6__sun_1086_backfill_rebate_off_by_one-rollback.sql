-- Rollback for V6__sun_1086_backfill_rebate_off_by_one.sql
-- Restores `rebate_amount` to the pre-backfill value from the snapshot
-- table and reverses the wallet top-up transactions.
--
-- Destructive. Run only if finance determines the backfilled amounts
-- were wrong (e.g. a double pay-out already happened through another
-- channel). Safer option is almost always to leave V6 applied.

USE vinplay;

-- Revert rebate_amount to the pre-V6 value.
UPDATE rebate_logs rl
  JOIN rebate_logs_backfill_sun_1086 bf ON bf.id = rl.id
   SET rl.rebate_amount = bf.old_amount,
       rl.note = REPLACE(rl.note, CONCAT(' | SUN-1086 backfill: ', bf.old_amount, '->', bf.new_amount), '');

-- Debit the agency_wallet delta we credited in V6.
INSERT INTO agency_wallet (agent_id, balance, updated_at)
SELECT rl.agent_user_id,
       -SUM(bf.diff),
       CURRENT_TIMESTAMP
  FROM rebate_logs rl
  JOIN rebate_logs_backfill_sun_1086 bf ON bf.id = rl.id
 WHERE rl.status = 'PAID'
   AND bf.diff <> 0
 GROUP BY rl.agent_user_id
ON DUPLICATE KEY UPDATE
    balance     = balance + VALUES(balance),
    updated_at  = CURRENT_TIMESTAMP;

-- Audit the reversal.
INSERT INTO agency_wallet_transactions
    (agent_id, agent_nickname, type, amount, direction, balance_after, related_user, game_action, note)
SELECT rl.agent_user_id,
       rl.agent_nickname,
       'COMMISSION_BACKFILL_REVERSAL',
       -SUM(bf.diff),
       CASE WHEN SUM(bf.diff) > 0 THEN 'DEBIT' ELSE 'CREDIT' END,
       COALESCE(aw.balance, 0),
       NULL,
       NULL,
       CONCAT('SUN-1086 backfill ROLLBACK: reversed ', COUNT(*), ' rows')
  FROM rebate_logs rl
  JOIN rebate_logs_backfill_sun_1086 bf ON bf.id = rl.id
  LEFT JOIN agency_wallet aw ON aw.agent_id = rl.agent_user_id
 WHERE rl.status = 'PAID'
   AND bf.diff <> 0
 GROUP BY rl.agent_user_id, rl.agent_nickname;

-- Drop the snapshot table (optional — keep for audit if preferred).
-- DROP TABLE rebate_logs_backfill_sun_1086;
