-- Phase F1 — user deletion audit table.
-- Date: 2026-05-04
--
-- Persists who/when/why for every cascade-delete of a user, plus the
-- per-table row counts. Decoupled from vinplay.users via snapshot
-- columns (we capture user_name + nick_name at delete time) so the row
-- survives even after the cascade removes the user record.

CREATE TABLE IF NOT EXISTS vinplay_admin.user_deletion_audit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_user_id  BIGINT      NOT NULL,
    target_user_name VARCHAR(128) NOT NULL,
    target_nick_name VARCHAR(128) DEFAULT NULL,
    target_balance_vin BIGINT NOT NULL DEFAULT 0,
    target_balance_xu  BIGINT NOT NULL DEFAULT 0,
    admin_actor     VARCHAR(64) NOT NULL,
    admin_ip        VARCHAR(45) DEFAULT NULL,
    reason          VARCHAR(500) NOT NULL,
    rows_deleted_total INT      NOT NULL DEFAULT 0,
    rows_per_table  JSON         DEFAULT NULL,
    deleted_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_target  (target_user_id),
    KEY idx_admin   (admin_actor, deleted_at),
    KEY idx_time    (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Phase F1 — GDPR / fraud-driven user delete audit. Snapshot columns survive after cascade removes the user.';
