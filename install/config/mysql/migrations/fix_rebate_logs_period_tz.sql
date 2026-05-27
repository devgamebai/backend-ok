-- ================================================================
-- SUN-799: Backfill rebate_logs period_start/period_end where the
-- row was written by the legacy RealTimeCommission.calculate() path
-- using the JVM default TimeZone (was UTC pre-migration).
--
-- After the Asia/Seoul migration (yyyy-MM-dd now reflects Seoul),
-- the LogMoneyUserExtraProcessor AUTO_COMMISSION path is correct
-- because it derives the date from the RMQ message createTime.
--
-- Broken rows (notes "Realtime from …" / "Cashback …") have
-- period_start one day behind created_at because the old JVM
-- computed "today" in UTC. Re-index them to DATE(created_at) in
-- MySQL server TZ (Asia/Seoul) so the rolling-history date filter
-- matches what FE sends.
--
-- Usage:
--   docker cp install/config/mysql/migrations/fix_rebate_logs_period_tz.sql sunwinkr-mysql:/tmp/
--   docker exec -i sunwinkr-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /tmp/fix_rebate_logs_period_tz.sql
-- ================================================================

USE vinplay;

-- Dry-run preview: how many rows will change?
SELECT
    COUNT(*)                           AS rows_to_fix,
    MIN(created_at)                    AS earliest_created_at,
    MAX(created_at)                    AS latest_created_at
FROM rebate_logs
WHERE DATE(period_start) <> DATE(created_at);

-- Backfill: move period_start / period_end onto the Seoul calendar
-- day of created_at. Only touches rows with a TZ mismatch.
UPDATE rebate_logs
SET
    period_start = CONCAT(DATE(created_at), ' 00:00:00'),
    period_end   = CONCAT(DATE(created_at), ' 23:59:59')
WHERE DATE(period_start) <> DATE(created_at);

-- Post-check: should report 0.
SELECT COUNT(*) AS remaining_mismatch
FROM rebate_logs
WHERE DATE(period_start) <> DATE(created_at);
