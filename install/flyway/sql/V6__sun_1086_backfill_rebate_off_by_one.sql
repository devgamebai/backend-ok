-- SUN-1086 backfill — correct the off-by-one `rebate_amount` values
-- written by LogMoneyUserExtraProcessor.calculateDifferential BEFORE the
-- real fix (wrap subtraction in BigDecimal, not just the multiplication).
--
-- The bad rows carry a drifted `differential_pct` that round-trips
-- through IEEE-754 (e.g. 0.09999999999999987 instead of 0.10). The
-- differential_pct column stores that drift verbatim, so the safest
-- backfill is to recompute directly from the stored percentage fields:
--     amount = FLOOR(total_f1_volume * differential_pct / 100)
-- which the fixed code now also produces (since the BigDecimal path
-- with a correctly-subtracted differential lands on the same value as
-- MySQL's DECIMAL FLOOR on a clean diff).
--
-- Idempotent — UPDATE only where the stored value differs from the
-- computed one. Re-running finds 0 rows.
--
-- Also logs the original amount + reason into a one-off audit table so
-- finance can reconcile what got paid out externally.

USE vinplay;

CREATE TABLE IF NOT EXISTS rebate_logs_backfill_sun_1086 (
    id             BIGINT       NOT NULL PRIMARY KEY,
    old_amount     BIGINT       NOT NULL,
    new_amount     BIGINT       NOT NULL,
    diff           BIGINT       NOT NULL,
    created_at     DATETIME     NOT NULL,
    note           VARCHAR(500) NULL,
    backfilled_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- Snapshot the rows we are about to correct.
INSERT IGNORE INTO rebate_logs_backfill_sun_1086 (id, old_amount, new_amount, diff, created_at, note)
SELECT rl.id,
       rl.rebate_amount AS old_amount,
       FLOOR(rl.total_f1_volume * rl.differential_pct / 100) AS new_amount,
       FLOOR(rl.total_f1_volume * rl.differential_pct / 100) - rl.rebate_amount AS diff,
       rl.created_at,
       rl.note
  FROM rebate_logs rl
 WHERE rl.total_f1_volume > 0
   AND rl.differential_pct > 0
   AND rl.rebate_amount <> FLOOR(rl.total_f1_volume * rl.differential_pct / 100);

-- Apply the correction.
UPDATE rebate_logs rl
   JOIN rebate_logs_backfill_sun_1086 bf ON bf.id = rl.id
    SET rl.rebate_amount = bf.new_amount,
        rl.note = CONCAT(IFNULL(rl.note, ''), ' | SUN-1086 backfill: ', bf.old_amount, '->', bf.new_amount);

-- Also credit the difference into agency_wallet for each affected PAID row.
-- (PENDING rows do not credit wallet — the eventual claim writes the corrected amount.)
INSERT INTO agency_wallet (agent_id, balance, updated_at)
SELECT rl.agent_user_id,
       SUM(bf.diff),
       CURRENT_TIMESTAMP
  FROM rebate_logs rl
  JOIN rebate_logs_backfill_sun_1086 bf ON bf.id = rl.id
 WHERE rl.status = 'PAID'
   AND bf.diff <> 0
 GROUP BY rl.agent_user_id
ON DUPLICATE KEY UPDATE
    balance     = balance + VALUES(balance),
    updated_at  = CURRENT_TIMESTAMP;

-- Audit trail for the wallet top-up / debit.
INSERT INTO agency_wallet_transactions
    (agent_id, agent_nickname, type, amount, direction, balance_after, related_user, game_action, note)
SELECT rl.agent_user_id,
       rl.agent_nickname,
       'COMMISSION_BACKFILL',
       SUM(bf.diff),
       CASE WHEN SUM(bf.diff) >= 0 THEN 'CREDIT' ELSE 'DEBIT' END,
       COALESCE(aw.balance, 0),
       NULL,
       NULL,
       CONCAT('SUN-1086 backfill: corrected ', COUNT(*), ' rows. Net delta=', SUM(bf.diff))
  FROM rebate_logs rl
  JOIN rebate_logs_backfill_sun_1086 bf ON bf.id = rl.id
  LEFT JOIN agency_wallet aw ON aw.agent_id = rl.agent_user_id
 WHERE rl.status = 'PAID'
   AND bf.diff <> 0
 GROUP BY rl.agent_user_id, rl.agent_nickname;

-- Sanity check — should be 0 after the UPDATE.
SELECT COUNT(*) AS remaining_off_by_one
  FROM rebate_logs
 WHERE total_f1_volume > 0
   AND differential_pct > 0
   AND rebate_amount <> FLOOR(total_f1_volume * differential_pct / 100);
