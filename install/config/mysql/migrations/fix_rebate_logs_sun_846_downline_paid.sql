-- ================================================================
-- SUN-846: DOWNLINE commissions stuck at PENDING instead of PAID
--
-- Bug: LogMoneyUserExtraProcessor.insertPendingLogIfAbsent() hardcoded
-- status='PENDING' for BOTH SELF and DOWNLINE rows. The legacy
-- RealTimeCommission.calculate() behaviour (now restored) writes DOWNLINE
-- as PAID with instant credit to agency_wallet; only SELF (player
-- cashback) stays PENDING because it requires a player Claim action.
--
-- Fix code: api/vbee/.../LogMoneyUserExtraProcessor.java
--   - DOWNLINE  -> status='PAID'  + creditAgencyWallet(...)
--   - SELF      -> status='PENDING' (unchanged)
--   - Also populate rebate_type column (was previously NULL)
--
-- Backfill:
--   1. Preview how many AUTO_COMMISSION DOWNLINE rows are stuck PENDING.
--   2. Credit each affected agent's agency_wallet by the sum of their
--      stuck DOWNLINE rebate_amount — idempotent via the update filter.
--   3. Flip those rebate_logs rows PENDING -> PAID.
--   4. Best-effort backfill of NULL rebate_type on legacy rows so the
--      FE rolling-history UI can distinguish SELF vs DOWNLINE.
--
-- Usage:
--   docker cp install/config/mysql/migrations/fix_rebate_logs_sun_846_downline_paid.sql \
--       sunwinkr-mysql:/tmp/
--   docker exec -i sunwinkr-mysql \
--       mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /tmp/fix_rebate_logs_sun_846_downline_paid.sql
-- ================================================================

USE vinplay;

-- 1. Preview: how many DOWNLINE rows stuck at PENDING?
SELECT
    COUNT(*)                  AS stuck_rows,
    SUM(rebate_amount)        AS stuck_amount,
    MIN(created_at)           AS earliest,
    MAX(created_at)           AS latest
FROM rebate_logs
WHERE status = 'PENDING'
  AND note LIKE 'AUTO_COMMISSION%'
  AND note LIKE '% type=DOWNLINE %';

-- 2. Per-agent totals we're about to credit:
SELECT
    agent_user_id,
    agent_nickname,
    COUNT(*)           AS rows_to_pay,
    SUM(rebate_amount) AS amount_to_credit
FROM rebate_logs
WHERE status = 'PENDING'
  AND note LIKE 'AUTO_COMMISSION%'
  AND note LIKE '% type=DOWNLINE %'
GROUP BY agent_user_id, agent_nickname
ORDER BY 4 DESC;

-- 3. Credit agency_wallet for each affected agent.
-- INSERT or UPDATE — keeps agency_wallet row in sync with the new
-- PAID rebate_logs rows.
INSERT INTO agency_wallet (agent_id, balance, updated_at)
SELECT
    agent_user_id AS agent_id,
    SUM(rebate_amount) AS balance_delta,
    NOW()
FROM rebate_logs
WHERE status = 'PENDING'
  AND note LIKE 'AUTO_COMMISSION%'
  AND note LIKE '% type=DOWNLINE %'
GROUP BY agent_user_id
ON DUPLICATE KEY UPDATE
    balance    = balance + VALUES(balance),
    updated_at = NOW();

-- 4. Flip the stuck PENDING DOWNLINE rows to PAID.
UPDATE rebate_logs
SET status = 'PAID'
WHERE status = 'PENDING'
  AND note LIKE 'AUTO_COMMISSION%'
  AND note LIKE '% type=DOWNLINE %';

-- 5. Best-effort: populate rebate_type on legacy AUTO_COMMISSION rows
-- that were written before the INSERT added rebate_type to the column list.
UPDATE rebate_logs
SET rebate_type = 'DOWNLINE'
WHERE (rebate_type IS NULL OR rebate_type = '')
  AND note LIKE 'AUTO_COMMISSION%'
  AND note LIKE '% type=DOWNLINE %';

UPDATE rebate_logs
SET rebate_type = 'SELF'
WHERE (rebate_type IS NULL OR rebate_type = '')
  AND note LIKE 'AUTO_COMMISSION%'
  AND note LIKE '% type=SELF %';

-- 6. Post-check: should be 0.
SELECT
    COUNT(*) AS remaining_stuck_downline
FROM rebate_logs
WHERE status = 'PENDING'
  AND note LIKE 'AUTO_COMMISSION%'
  AND note LIKE '% type=DOWNLINE %';

-- 7. Sanity view of top agency_wallet balances post-backfill.
SELECT a.agent_id, ua.nickname, a.balance, a.updated_at
FROM agency_wallet a
LEFT JOIN vinplay_admin.useragent ua ON ua.id = a.agent_id
WHERE a.balance > 0
ORDER BY a.updated_at DESC
LIMIT 15;
