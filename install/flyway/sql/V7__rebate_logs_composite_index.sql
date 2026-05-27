-- SUN-1108 — composite index on rebate_logs to support agency rolling-log
-- queries at scale.
--
-- Problem (without this index):
-- /api/rolling (c=9541) → GetRebateLogs4AgencyProcessor → RebateService.queryLogs
-- runs:
--   SELECT ... FROM rebate_logs
--    WHERE agent_user_id = ? AND created_at BETWEEN ? AND ?
--    ORDER BY created_at DESC LIMIT 20 OFFSET ?
--
-- The pre-existing single-column idx_agent_user_id forced MySQL to ref-scan
-- by agent then filesort by created_at. Once rebate_logs crosses ~100k
-- rows per agent (≈months of live-casino traffic), the filesort dominates.
--
-- Solution: composite index covering both predicates in the right order.
-- agent_user_id (equality) FIRST so the index seek binds tightly; then
-- created_at DESC matching the ORDER BY direction so MySQL streams rows
-- in sorted order — no filesort, no temp file, constant-time tail latency.
--
-- Idempotent: SHOW INDEX guard via INFORMATION_SCHEMA. Safe to re-run.

DROP PROCEDURE IF EXISTS _sun_add_rebate_composite_idx;
DELIMITER //
CREATE PROCEDURE _sun_add_rebate_composite_idx()
BEGIN
  DECLARE has_idx INT DEFAULT 0;

  SELECT COUNT(*) INTO has_idx
    FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = 'vinplay'
     AND TABLE_NAME   = 'rebate_logs'
     AND INDEX_NAME   = 'idx_rebate_agent_created';

  IF has_idx = 0 THEN
    -- DESC on the second column lets the optimizer satisfy
    -- `ORDER BY created_at DESC` without a backward index scan.
    -- MySQL 8 supports descending indexes natively.
    ALTER TABLE vinplay.rebate_logs
      ADD INDEX idx_rebate_agent_created (agent_user_id, created_at DESC);
  END IF;
END //
DELIMITER ;

CALL _sun_add_rebate_composite_idx();
DROP PROCEDURE _sun_add_rebate_composite_idx;

-- Refresh optimizer cardinality stats so the planner picks the new index
-- on the next query (no-op if already analyzed recently).
ANALYZE TABLE vinplay.rebate_logs;

-- Sanity: should report idx_rebate_agent_created in the index list.
SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, COLLATION
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='rebate_logs'
   AND INDEX_NAME='idx_rebate_agent_created'
 ORDER BY SEQ_IN_INDEX;
