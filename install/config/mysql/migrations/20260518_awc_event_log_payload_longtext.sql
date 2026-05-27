-- =============================================================================
-- 20260518_awc_event_log_payload_longtext.sql
--
-- Convert awc_event_log.raw_payload from JSON → LONGTEXT.
--
-- WHY
-- ----
-- The original 20260518_awc_event_log.sql declared raw_payload as
-- JSON NOT NULL. We control sanitizeHeaders on the request_headers side
-- (well-formed by construction), but raw_payload is captured directly
-- from AWC. Vendor occasionally ships malformed payloads (truncated by
-- a network blip, oddly encoded), which MySQL's JSON validator rejects
-- — the INSERT fails and the audit row is lost SILENTLY (the logger is
-- best-effort by contract).
--
-- LONGTEXT keeps the same content but never gates on JSON shape, so the
-- audit trail captures malformed payloads too (those are exactly the
-- ones we most need to see).
--
-- IDEMPOTENT
-- ----------
-- Reads information_schema.COLUMNS to confirm column is still JSON
-- before issuing the ALTER. Re-runs are a no-op.
--
-- INPLACE
-- -------
-- ALGORITHM=COPY is required by MySQL for JSON↔text type changes —
-- INPLACE is not supported here. Table is low-write at the migration
-- moment (observability path, not money path), so the brief copy is
-- acceptable. New deploys land on LONGTEXT via the updated base
-- migration and skip this one.
-- =============================================================================

USE vinplay;

SET @col_type := (
    SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'vinplay'
      AND TABLE_NAME   = 'awc_event_log'
      AND COLUMN_NAME  = 'raw_payload'
);
SET @ddl := IF(@col_type = 'json',
    'ALTER TABLE awc_event_log MODIFY COLUMN raw_payload LONGTEXT NOT NULL',
    'SELECT "raw_payload is already non-JSON — skip" AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
