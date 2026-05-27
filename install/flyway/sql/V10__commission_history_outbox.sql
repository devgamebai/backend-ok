-- SUN-1112 (CQRS Phase 0) — outbox table for commission history records.
--
-- Purpose: in the same MySQL transaction as the agency_wallet credit, vbee
-- inserts a row here with the full commission snapshot as JSON. A separate
-- drainer container reads PENDING rows and pushes them into the MongoDB
-- vinplay.commission_history collection idempotently. Once drained for
-- DRAIN_RETENTION_DAYS (default 7), DONE rows are purged.
--
-- This is a TRANSIENT table — never queried for reports, only used to
-- coordinate the write transaction with the eventual Mongo insert.
--
-- The Phase 0 deployment creates the table empty. No code path writes to
-- it yet. Phase 1 wires up the dual-write hook in vbee.
--
-- Idempotent: safe to re-run.

CREATE TABLE IF NOT EXISTS vinplay.commission_history_outbox (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bet_event_id    VARCHAR(255) NOT NULL,
    agent_id        INT          NOT NULL,
    snapshot_json   JSON         NOT NULL,
    status          ENUM('PENDING','DONE','FAILED') NOT NULL DEFAULT 'PENDING',
    attempts        INT          NOT NULL DEFAULT 0,
    last_error      VARCHAR(512) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    drained_at      DATETIME     NULL,

    INDEX idx_outbox_status_created (status, created_at),
    UNIQUE KEY uk_outbox_event_agent (bet_event_id, agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
