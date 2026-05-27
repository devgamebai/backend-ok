-- SUN-1112 (CQRS Phase 0) — append-only audit log for commission-rate config
-- changes.
--
-- When an admin (or a direct SQL change) modifies a row in any of the rate
-- config tables — game_commission_rate, useragent.commission_rate,
-- tbl_cashback_game_config — a row is appended here capturing
-- old_value, new_value, who/when/via.
--
-- Two population layers, defense-in-depth:
--   1. Application code on each admin endpoint inserts here in the same
--      transaction as the rate change. Captures rich context (changed_by
--      user, reason, endpoint). Wired in subsequent phases.
--   2. DB triggers on the rate config tables auto-insert here as a safety
--      net for direct SQL fixes that bypass the application. See V12.
--
-- target_pk_json holds the natural key of the row that changed:
--   game_commission_rate    → {"agent_nickname":"...","game_key":"..."}
--   useragent               → {"id":42}
--   tbl_cashback_game_config→ {"id":17,"config_id":3,"game_code":"..."}
--
-- This is the source of truth for "what was the rate at time T" queries —
-- combined with the snapshot rate already baked into Mongo
-- commission_history rows, an auditor can reconcile any past commission
-- row to the live config that produced it.
--
-- Idempotent: safe to re-run.

CREATE TABLE IF NOT EXISTS vinplay.commission_rate_audit (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,

    target_table    VARCHAR(64)  NOT NULL,
    target_pk_json  JSON         NOT NULL,
    field_name      VARCHAR(64)  NOT NULL,
    old_value       DECIMAL(8,4) NULL,
    new_value       DECIMAL(8,4) NULL,

    operation       ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    effective_from  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by      VARCHAR(128) NULL,        -- admin nickname / 'db-trigger' / 'sql-console'
    changed_via     VARCHAR(64)  NULL,        -- e.g. 'admin-api:c=9512' / 'trigger:tg_gcr_audit'
    reason          VARCHAR(255) NULL,
    changed_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_target_changed (target_table, changed_at DESC),
    INDEX idx_audit_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
