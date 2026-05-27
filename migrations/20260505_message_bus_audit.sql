-- Migration: Create message_bus_audit table
-- Date: 2026-05-05
-- Ticket: SUN-RS / S1 dual-write soak
-- Purpose: Producer-side audit row for the RMQ → Redis Streams migration
--          dual-write soak. Each MessageBus.publish call writes one row per
--          backend it reached (RMQ-only = 1 row; DUAL = 2 rows; Redis-only
--          = 1 row). Reconciliation tooling (scripts/rmq-redis-reconcile.sh
--          from MR !341) reads this to compute per-queue drift between RMQ
--          and Redis publish counts.
--
-- Schema source-of-truth: the Javadoc on MessageBusAuditWriter.java spells
-- out the same DDL — keep them in sync if you change either.
--
-- Impact: New table on `vinplay` DB. No existing rows. No locks on existing
--         tables. Insert load during S1 soak: peak ~1000 TPS dual-mode
--         (each publish = 2 rows). At 200 TPS sustained that's ~400
--         INSERT/sec which a single audit-writer thread on the user_pool
--         handles comfortably. Add a dedicated mysqlpool_audit if cross-
--         contention with wallet writes appears in soak metrics.
--
-- Retention: writer is best-effort; rows older than 30 days have no
--           operational value once the soak window closes. Schedule a
--           DELETE cron after S1 (or partition by date if growth surprises).
--
-- Rollback:
--   DROP TABLE IF EXISTS message_bus_audit;
--   (Producers handle the missing table gracefully — MessageBusAuditWriter
--    catches SQLException, logs once/min throttled, and drops the row.
--    No producer disruption. See MessageBusAuditWriter.java header.)

-- Idempotent CREATE.
SET @db_name = DATABASE();
SET @table_exists = (
    SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'message_bus_audit'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE message_bus_audit (
       id BIGINT NOT NULL AUTO_INCREMENT,
       queue_name VARCHAR(64) NOT NULL,
       command INT NOT NULL,
       backend ENUM(\'rmq\',\'redis\') NOT NULL,
       ts DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
       success TINYINT(1) NOT NULL,
       PRIMARY KEY (id),
       KEY idx_queue_ts (queue_name, ts),
       KEY idx_backend_ts (backend, ts)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
    'SELECT "Table message_bus_audit already exists, skipping." AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
