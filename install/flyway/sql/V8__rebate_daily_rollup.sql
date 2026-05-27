-- SUN-1108 Tier 3 — daily pre-aggregation of rebate_logs for fast date-range
-- summary queries.
--
-- Purpose:
--   summarizeLogs(agentId, status, ft, et) currently does a fresh
--   COUNT/SUM scan of vinplay.rebate_logs for every c=9541 request.
--   At scale (rebate_logs > 5M rows), the multi-day window scan dominates
--   response time. Pre-aggregate by (agent_user_id, day, rebate_type)
--   so historical days return in O(days) instead of O(rows).
--
-- Read path semantics (RebateService.summarizeLogs after this MR):
--   day < CURDATE()  → SELECT FROM rebate_daily_rollup  (immutable)
--   day = CURDATE()  → SELECT FROM rebate_logs          (live, may grow)
--   UNION + sum on the application side.
--
-- Write path:
--   * RebateRollupScheduler runs daily 00:30 UTC and inserts rows for the
--     just-completed day via INSERT ... ON DUPLICATE KEY UPDATE so it's
--     safe to re-run / multi-instance.
--   * No code writes to this table during normal request handling.
--
-- Backfill:
--   Bottom of this migration populates the rollup for ALL existing
--   rebate_logs rows older than today. Idempotent INSERT IGNORE — safe
--   re-run.

USE vinplay;

CREATE TABLE IF NOT EXISTS rebate_daily_rollup (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    agent_user_id   INT          NOT NULL,
    rollup_date     DATE         NOT NULL,
    rebate_type     VARCHAR(16)  NOT NULL,
    sum_bet_amount  BIGINT       NOT NULL DEFAULT 0,
    sum_commission  BIGINT       NOT NULL DEFAULT 0,
    row_count       INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_agent_day_type (agent_user_id, rollup_date, rebate_type),
    KEY idx_rollup_agent_date (agent_user_id, rollup_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3
  COMMENT='SUN-1108 Tier 3: daily aggregates of rebate_logs for fast multi-day summary';

-- Backfill all completed days (< CURDATE()) from rebate_logs.
-- INSERT IGNORE keeps re-runs safe — production-deployed cron will catch up
-- any new rows that slipped in between this backfill and the first scheduled run.
INSERT IGNORE INTO rebate_daily_rollup (agent_user_id, rollup_date, rebate_type, sum_bet_amount, sum_commission, row_count)
SELECT
    agent_user_id,
    DATE(created_at)                  AS rollup_date,
    rebate_type,
    COALESCE(SUM(total_f1_volume), 0) AS sum_bet_amount,
    COALESCE(SUM(rebate_amount), 0)   AS sum_commission,
    COUNT(*)                          AS row_count
  FROM rebate_logs
 WHERE DATE(created_at) < CURDATE()
   AND status = 'PAID'
 GROUP BY agent_user_id, DATE(created_at), rebate_type;

-- Reporting: rows backfilled (sanity check).
SELECT 'backfill_rows' AS metric, COUNT(*) AS value FROM rebate_daily_rollup;
SELECT 'backfill_days' AS metric, COUNT(DISTINCT rollup_date) AS value FROM rebate_daily_rollup;
