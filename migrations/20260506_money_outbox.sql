-- =============================================================================
-- SUN-1248: Money outbox table — transactional event publication.
--
-- Spec ref: ledger-system-tech-stack-plan.pdf §4.4 ("Outbox Pattern").
--
-- The outbox row is INSERTed inside the same DB transaction as the ledger
-- write. A separate poller picks unpublished rows and ships them to the
-- message bus (Redis Streams / RabbitMQ). This eliminates the
-- "publish-then-write" race: if RMQ accepts but DB rolls back, the event
-- never existed; if DB commits but RMQ is down, the poller retries from
-- the persisted outbox row.
--
-- Phase 1 (this migration): table + index only. Producer SP and poller
-- come in follow-up commits — see docs/architecture/LEDGER_HARDENING_ROADMAP.md.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `money_outbox` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT,
    `event_type`      VARCHAR(64) NOT NULL,
    `aggregate_id`    BIGINT NOT NULL,                  -- usually transaction_id
    `payload`         JSON NOT NULL,
    `published_at`    DATETIME(6) DEFAULT NULL,         -- NULL = pending
    `publish_attempts` INT NOT NULL DEFAULT 0,
    `last_error`      VARCHAR(500) DEFAULT NULL,
    `created_at`      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    -- Pending rows are the only ones the poller scans; a partial-style index
    -- keeps the working set small even after the table grows to millions
    -- of historical rows.
    KEY `idx_pending` (`created_at`) /*!80000 INVISIBLE*/,
    KEY `idx_pending_active` (`published_at`, `created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Outbox for money-flow events. Polled and shipped to the message bus.';
