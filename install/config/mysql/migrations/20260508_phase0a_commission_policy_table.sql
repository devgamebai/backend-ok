-- Phase 0a — commission rate policy + ops event log
-- Date: 2026-05-08
-- Plan: docs/COMMISSION_SCHEMA_FIX_PLAN.md §3.2, §3.3, §6.1, §5
--
-- Idempotent (CREATE TABLE IF NOT EXISTS, INSERT IGNORE).
--
-- 1. commission_rate_policy: per-master agency budget. The master TĐL
--    decides how big the pool is (sum of slices across the entire
--    descendant chain) and the default per-level "step down" Δ that
--    new sub-agents inherit when they're created.
--
-- 2. ops_event_log: durable record of operational events that don't
--    fit in the existing data-audit triggers — e.g. "trigger
--    temporarily disabled for backfill", "manual schema migration".

CREATE TABLE IF NOT EXISTS vinplay.commission_rate_policy (
    master_id    INT          NOT NULL PRIMARY KEY,
    pool_max_pct DECIMAL(5,2) NOT NULL DEFAULT 1.25
        COMMENT 'maximum total commission paid out across the entire descendant chain on a single bet',
    step_pct     DECIMAL(5,2) NOT NULL DEFAULT 0.25
        COMMENT 'default Δ applied when seeding rate rows for a new sub-agent (parent_rate - step)',
    floor_pct    DECIMAL(5,2) NOT NULL DEFAULT 0.00
        COMMENT 'minimum rate any descendant tier may hold; clamps step subtraction',
    per_game_pool JSON         DEFAULT NULL
        COMMENT 'optional per-game-key pool override map, e.g. {"awc_live": 1.50, "live_cat_Slot": 0}',
    notes        VARCHAR(500) DEFAULT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_crp_master
        FOREIGN KEY (master_id) REFERENCES vinplay_admin.useragent(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Phase 0a — per-master agency commission policy (pool, step, floor, per-game overrides).';

-- Seed Policy A defaults (pool=1.25, step=0.25) for every existing
-- master TĐL — anyone whose useragent.parentid is NULL/0.
INSERT IGNORE INTO vinplay.commission_rate_policy (master_id, pool_max_pct, step_pct, floor_pct, notes)
  SELECT id, 1.25, 0.25, 0.00, 'auto-seeded 2026-05-08 phase0a'
    FROM vinplay_admin.useragent
   WHERE parentid IS NULL OR parentid = 0;

CREATE TABLE IF NOT EXISTS vinplay.ops_event_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type  VARCHAR(64)  NOT NULL,
    actor       VARCHAR(64)  NOT NULL,
    payload     JSON         DEFAULT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ops_event_type (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Phase 0a — durable log for ops-class events (trigger drops, schema migrations, manual backfills).';

-- Verification
SELECT 'commission_rate_policy seeded' AS step, COUNT(*) AS rows FROM vinplay.commission_rate_policy;
