-- Phase 0b — game_commission_rate structural hardening
-- Date: 2026-05-08
-- Plan: docs/COMMISSION_SCHEMA_FIX_PLAN.md §2.1, §2.2, §2.3
--
-- 1. agent_user_id NOT NULL  (precondition: today's audit backfill
--    populated all 367 NULL rows; verify before applying).
-- 2. UNIQUE (agent_user_id, game_key) so the resolver's LIMIT 1 is
--    deterministic.
-- 3. CHECK rate >= 0 (the math expects non-negative; trigger is
--    belt-and-braces, MySQL 8 honours CHECK).
-- 4. KEY on (agent_user_id, game_key) -- created automatically by
--    UNIQUE; old per-column KEY can stay.
--
-- Idempotent: every ALTER guarded by INFORMATION_SCHEMA exists-check.

USE vinplay;
SET @schema := 'vinplay';
SET @table  := 'game_commission_rate';

-- 1. NOT NULL on agent_user_id (only if there are zero NULLs first)
SET @null_rows := (SELECT COUNT(*) FROM game_commission_rate WHERE agent_user_id IS NULL);
SELECT @null_rows AS null_rows_pre;

SET @col_nullable := (SELECT IS_NULLABLE FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA=@schema AND TABLE_NAME=@table AND COLUMN_NAME='agent_user_id');
SET @sql := IF(@null_rows = 0 AND @col_nullable = 'YES',
  'ALTER TABLE game_commission_rate MODIFY COLUMN agent_user_id INT NOT NULL',
  CONCAT('SELECT ''skip NOT NULL: null_rows=', @null_rows, ' nullable=', @col_nullable, ''''));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. UNIQUE (agent_user_id, game_key)
SET @uk_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA=@schema AND TABLE_NAME=@table
                      AND INDEX_NAME='uk_gcr_agent_game');
SET @sql := IF(@uk_exists = 0,
  'ALTER TABLE game_commission_rate ADD CONSTRAINT uk_gcr_agent_game UNIQUE (agent_user_id, game_key)',
  'SELECT ''skip UNIQUE: already present''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3. CHECK rate >= 0 (MySQL 8.0.16+; harmless on older servers as a
--    no-op if the parser rejects).
SET @ck_exists := (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
                    WHERE CONSTRAINT_SCHEMA=@schema AND CONSTRAINT_NAME='chk_gcr_rate_nonneg');
SET @sql := IF(@ck_exists = 0,
  'ALTER TABLE game_commission_rate ADD CONSTRAINT chk_gcr_rate_nonneg CHECK (rate >= 0)',
  'SELECT ''skip CHECK: already present''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ops_event_log entry — paper trail for the migration itself.
INSERT INTO ops_event_log (event_type, actor, payload)
VALUES ('schema_migration', 'phase0b_migration',
        JSON_OBJECT('table','game_commission_rate',
                    'changes', JSON_ARRAY('agent_user_id NOT NULL',
                                          'uk_gcr_agent_game',
                                          'chk_gcr_rate_nonneg')));

-- Verification
SELECT 'agent_user_id NULL count' AS metric, COUNT(*) AS value FROM game_commission_rate WHERE agent_user_id IS NULL
UNION ALL
SELECT 'unique index present',
       (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='game_commission_rate'
           AND INDEX_NAME='uk_gcr_agent_game')
UNION ALL
SELECT 'check constraint present',
       (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
         WHERE CONSTRAINT_SCHEMA='vinplay' AND CONSTRAINT_NAME='chk_gcr_rate_nonneg');
