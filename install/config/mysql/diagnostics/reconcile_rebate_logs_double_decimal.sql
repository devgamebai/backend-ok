-- SUN-1248 Phase 3 — Rebate logs reconciliation
-- ==============================================
--
-- Validates that the DOUBLE → DECIMAL(7,4) migration of rate columns
-- (and the SUN-1209 amount widening to DECIMAL(20,4)) hasn't introduced
-- drift between stored `rebate_amount` and the canonical recomputation
-- `total_f1_volume * rebate_percentage / 100`.
--
-- Run AFTER applying 2026_05_03_rebate_rates_double_to_decimal.sql.
-- Expected result: zero rows in the `drift` summary, or only rows
-- whose drift is bounded by the historical scale=2 truncation that
-- the migration is correcting (those rows should have created_at
-- before the deploy).
--
-- Connect with: mysql -uroot -p"$MYSQL_ROOT_PASSWORD" vinplay
-- (read-only — emits SELECT only.)

-- 1) Column-type sanity. All five columns must be the new types.
SELECT '=== column types ===' AS section;
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'vinplay'
  AND ((TABLE_NAME = 'rebate_logs'
         AND COLUMN_NAME IN ('rebate_percentage','share_percentage',
                             'rebate_amount','share_amount','net_rebate'))
    OR (TABLE_NAME = 'rebate_config'
         AND COLUMN_NAME IN ('rebate_percentage','share_percentage'))
    OR (TABLE_NAME IN ('tbl_cashback_config','tbl_cashback_game_config')
         AND COLUMN_NAME = 'rebate_percent'))
ORDER BY TABLE_NAME, COLUMN_NAME;

-- 2) Drift summary — per row gap between stored amount and recompute.
--
--    Canonical formula (matches both LogMoneyUserExtraProcessor and the
--    refactored RealTimeCommission write paths):
--        rebate_amount = ROUND(total_f1_volume * differential_pct / 100, 4)
--
--    NOT volume * rebate_percentage — that column stores the agent's OWN
--    rate, while rebate_amount captures the DIFFERENTIAL slice the agent
--    actually earns above the prev-tier floor (downline cascade) or the
--    full rate (player SELF rows). For rows with differential_pct = 0
--    (agent rate <= floor), rebate_amount must be 0.
--
--    Pre-fix DOUBLE drift was bounded by IEEE-754 round-off for rates
--    like 1.25; post-fix the recompute is exact for all 2-decimal rates.
SELECT '=== overall drift histogram ===' AS section;
SELECT
    CASE
        WHEN drift_abs = 0                    THEN 'exact (0)'
        WHEN drift_abs <  0.0001              THEN '< 0.0001'
        WHEN drift_abs <  0.01                THEN '< 0.01'
        WHEN drift_abs <  1                   THEN '< 1'
        WHEN drift_abs <  10                  THEN '< 10'
        ELSE '>= 10 (INVESTIGATE)'
    END AS bucket,
    COUNT(*) AS rows_in_bucket,
    MIN(created_at) AS first_in_bucket,
    MAX(created_at) AS last_in_bucket
FROM (
    SELECT
        l.created_at,
        ABS(l.rebate_amount - ROUND(l.total_f1_volume * l.differential_pct / 100, 4)) AS drift_abs
    FROM vinplay.rebate_logs l
    WHERE l.total_f1_volume > 0
) d
GROUP BY bucket
ORDER BY MIN(drift_abs);

-- 3) Top 50 worst offenders for inspection.
--    Look for rows where drift >= 1 minor unit (1 vin / KRW won).
--    Pre-fix: a small tail expected from setLong-vs-setBigDecimal mismatch.
--    Post-fix: zero rows with created_at AFTER the deploy timestamp.
SELECT '=== worst-50 drift rows ===' AS section;
SELECT
    l.id,
    l.created_at,
    l.agent_user_id,
    l.agent_nickname,
    l.note,
    l.total_f1_volume,
    l.rebate_percentage,
    l.differential_pct,
    l.rebate_amount                                                          AS amt_stored,
    ROUND(l.total_f1_volume * l.differential_pct / 100, 4)                   AS amt_expected,
    l.rebate_amount - ROUND(l.total_f1_volume * l.differential_pct / 100, 4) AS drift
FROM vinplay.rebate_logs l
WHERE l.total_f1_volume > 0
  AND ABS(l.rebate_amount - ROUND(l.total_f1_volume * l.differential_pct / 100, 4)) >= 1
ORDER BY ABS(l.rebate_amount - ROUND(l.total_f1_volume * l.differential_pct / 100, 4)) DESC
LIMIT 50;

-- 4) Daily aggregate drift — does sum(rebate_amount) match sum(expected)
--    per Seoul day? Used to gauge the cumulative impact on agency LSR.
SELECT '=== daily-sum drift ===' AS section;
SELECT
    DATE(l.period_start)                                  AS rebate_day,
    COUNT(*)                                              AS row_count,
    SUM(l.rebate_amount)                                  AS sum_stored,
    SUM(ROUND(l.total_f1_volume * l.differential_pct / 100, 4)) AS sum_expected,
    SUM(l.rebate_amount)
        - SUM(ROUND(l.total_f1_volume * l.differential_pct / 100, 4)) AS daily_drift
FROM vinplay.rebate_logs l
WHERE l.total_f1_volume > 0
  AND l.period_start >= DATE_SUB(NOW(), INTERVAL 14 DAY)
GROUP BY rebate_day
ORDER BY rebate_day DESC;

-- 5) rebate_daily_rollup vs rebate_logs sum reconciliation.
--    The rollup cron aggregates rebate_logs into per-day per-agent buckets.
--    These two should match within rounding noise.
SELECT '=== rollup vs raw sum (last 14 days) ===' AS section;
SELECT
    r.rollup_date,
    r.agent_user_id,
    r.rebate_type,
    r.sum_commission                            AS rollup_sum,
    COALESCE(raw.raw_sum, 0)                    AS raw_sum,
    r.sum_commission - COALESCE(raw.raw_sum, 0) AS rollup_drift
FROM vinplay.rebate_daily_rollup r
LEFT JOIN (
    SELECT
        DATE(period_start) AS d,
        agent_user_id,
        rebate_type,
        SUM(rebate_amount) AS raw_sum
    FROM vinplay.rebate_logs
    GROUP BY d, agent_user_id, rebate_type
) raw
   ON raw.d = r.rollup_date
  AND raw.agent_user_id = r.agent_user_id
  AND raw.rebate_type = r.rebate_type
WHERE r.rollup_date >= DATE_SUB(CURDATE(), INTERVAL 14 DAY)
  AND ABS(r.sum_commission - COALESCE(raw.raw_sum, 0)) >= 0.0001
ORDER BY rollup_drift DESC
LIMIT 50;

-- 6) Agency_wallet vs rebate_logs paid-amount reconciliation.
--    For DOWNLINE rebate_logs marked PAID, the wallet ledger should hold
--    a matching credit (rounded to whole vin via amountForWallet HALF_UP
--    contract). Mismatches suggest an out-of-band wallet write or a
--    silent creditAgencyWallet failure (visible in app logs as
--    "rebate_log is PAID but wallet not updated!").
SELECT '=== wallet vs rebate_logs (today) ===' AS section;
SELECT
    l.agent_user_id,
    l.agent_nickname,
    SUM(ROUND(l.rebate_amount, 0))             AS expected_wallet_credit,
    COUNT(*)                                   AS paid_rows
FROM vinplay.rebate_logs l
WHERE l.status = 'PAID'
  AND l.rebate_type = 'DOWNLINE'
  AND DATE(l.period_start) = CURDATE()
GROUP BY l.agent_user_id, l.agent_nickname
HAVING SUM(ROUND(l.rebate_amount, 0)) > 0
ORDER BY expected_wallet_credit DESC
LIMIT 20;
