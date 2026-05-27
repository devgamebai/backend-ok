-- =============================================================================
-- Phase 6: Purge zombie rows + close last 2 true FK gaps
-- =============================================================================
-- Pre-reqs: phases 0-5 applied.
-- Surfaced by install/config/mysql/diagnostics/run_inventory.sh on 2026-04-24.
--
-- P0  tx_user zombies
--     tx_user has 4759 rows whose matching vinplay.users row is gone.
--     Phase 4 added fk_txuser_user with FOREIGN_KEY_CHECKS=0, so these
--     pre-existing violations survived. Future CASCADE deletes never touch
--     them because the parent users row is already missing.
--     tx_user_authority cascades from tx_user, so deleting tx_user zombies
--     also cleans its 4759 downstream rows (verified: 0 tx_user_authority
--     rows reference a non-existent tx_user — all zombies live IN tx_user).
--     Action: DELETE the 4759 rows. Safe — their owning user is already gone.
--
-- P1a banca_bet_commission_log (SUN-1054)
--     Lookup by `nickname` only, no user_id. Table is currently empty
--     (freshly migrated). Add `user_id` + FK CASCADE before traffic lands.
--
-- P1b game_commission_rate
--     555 rows, all resolve via useragent.nickname. Add `agent_user_id`
--     + backfill + FK CASCADE so agent deletion cascades rates.
--
-- Idempotent: INFORMATION_SCHEMA guards on every ALTER.
-- =============================================================================

USE vinplay;

SET sql_mode = '';

-- P0 runs FIRST with FK_CHECKS=1 so the fk_tx_userid CASCADE fires through
-- to tx_user_authority. (Earlier draft SET FK_CHECKS=0 at top, which
-- disables cascade propagation and leaves tx_user_authority zombies behind.)
SET FOREIGN_KEY_CHECKS = 1;

-- ─── P0: tx_user zombie purge ─────────────────────────────────────────────
-- tx_user_authority cascades on tx_user delete (fk_tx_userid ON DELETE CASCADE).
-- Run a bounded DELETE + report counts so we have receipts.

SET @tx_user_zombies := (
  SELECT COUNT(*) FROM vinplay.tx_user t
    LEFT JOIN vinplay.users u ON u.id = t.id
   WHERE u.id IS NULL);

SET @tx_auth_pre := (
  SELECT COUNT(*) FROM vinplay.tx_user_authority a
    LEFT JOIN vinplay.users u ON u.id = a.user_id
   WHERE u.id IS NULL);

SELECT CONCAT('P0 before: tx_user zombies=', @tx_user_zombies,
              ', tx_user_authority w/no user=', @tx_auth_pre) AS note;

DELETE t FROM vinplay.tx_user t
  LEFT JOIN vinplay.users u ON u.id = t.id
 WHERE u.id IS NULL;

-- Verify cascade wiped the downstream side.
SET @tx_user_zombies_post := (
  SELECT COUNT(*) FROM vinplay.tx_user t
    LEFT JOIN vinplay.users u ON u.id = t.id
   WHERE u.id IS NULL);

SET @tx_auth_post := (
  SELECT COUNT(*) FROM vinplay.tx_user_authority a
    LEFT JOIN vinplay.users u ON u.id = a.user_id
   WHERE u.id IS NULL);

SELECT CONCAT('P0 after: tx_user zombies=', @tx_user_zombies_post,
              ', tx_user_authority w/no user=', @tx_auth_post) AS note;

-- ─── P1a: banca_bet_commission_log — add user_id + FK ─────────────────────

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='banca_bet_commission_log'
                       AND COLUMN_NAME='user_id');
SET @sql := IF(@col_exists=0,
  'ALTER TABLE vinplay.banca_bet_commission_log ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nickname, ADD KEY idx_bbcl_user (user_id)',
  'SELECT ''P1a user_id already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Backfill (no-op while table is empty; harmless if rows land mid-migration).
UPDATE vinplay.banca_bet_commission_log b
   JOIN vinplay.users u ON u.nick_name = CONVERT(b.nickname USING utf8mb3) COLLATE utf8mb3_general_ci
    SET b.user_id = u.id
  WHERE b.user_id IS NULL;

SET @fk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                    WHERE CONSTRAINT_SCHEMA='vinplay' AND TABLE_NAME='banca_bet_commission_log'
                      AND CONSTRAINT_NAME='fk_bbcl_user');
SET @sql := IF(@fk_exists=0,
  'ALTER TABLE vinplay.banca_bet_commission_log ADD CONSTRAINT fk_bbcl_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE',
  'SELECT ''P1a fk_bbcl_user already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ─── P1b: game_commission_rate — add agent_user_id + FK ───────────────────

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='game_commission_rate'
                       AND COLUMN_NAME='agent_user_id');
SET @sql := IF(@col_exists=0,
  'ALTER TABLE vinplay.game_commission_rate ADD COLUMN agent_user_id INT DEFAULT NULL AFTER agent_nickname, ADD KEY idx_gcr_agent (agent_user_id)',
  'SELECT ''P1b agent_user_id already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Backfill: 555/555 resolve at inventory time.
UPDATE vinplay.game_commission_rate g
   JOIN vinplay_admin.useragent a ON a.nickname = g.agent_nickname COLLATE utf8mb3_general_ci
    SET g.agent_user_id = a.id
  WHERE g.agent_user_id IS NULL;

-- Report rows that failed to resolve (nickname drift since backfill).
SELECT CONCAT('P1b unresolved rows=',
              (SELECT COUNT(*) FROM vinplay.game_commission_rate WHERE agent_user_id IS NULL)) AS note;

SET @fk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                    WHERE CONSTRAINT_SCHEMA='vinplay' AND TABLE_NAME='game_commission_rate'
                      AND CONSTRAINT_NAME='fk_gcr_agent');
SET @sql := IF(@fk_exists=0,
  'ALTER TABLE vinplay.game_commission_rate ADD CONSTRAINT fk_gcr_agent FOREIGN KEY (agent_user_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE',
  'SELECT ''P1b fk_gcr_agent already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET FOREIGN_KEY_CHECKS = 1;

-- ─── Verification ─────────────────────────────────────────────────────────
SELECT 'FK on users (vinplay)' AS metric, COUNT(*) AS value
  FROM information_schema.KEY_COLUMN_USAGE
 WHERE REFERENCED_TABLE_SCHEMA='vinplay' AND REFERENCED_TABLE_NAME='users'
UNION ALL
SELECT 'FK on useragent (vinplay_admin)', COUNT(*)
  FROM information_schema.KEY_COLUMN_USAGE
 WHERE REFERENCED_TABLE_SCHEMA='vinplay_admin' AND REFERENCED_TABLE_NAME='useragent'
UNION ALL
SELECT 'Non-CASCADE/SET NULL FK to users/useragent', COUNT(*)
  FROM information_schema.REFERENTIAL_CONSTRAINTS rc
  JOIN information_schema.KEY_COLUMN_USAGE kcu USING (CONSTRAINT_SCHEMA, CONSTRAINT_NAME)
 WHERE kcu.REFERENCED_TABLE_SCHEMA IN ('vinplay','vinplay_admin')
   AND kcu.REFERENCED_TABLE_NAME   IN ('users','useragent')
   AND rc.DELETE_RULE NOT IN ('CASCADE','SET NULL')
UNION ALL
SELECT 'tx_user zombie rows remaining', COUNT(*)
  FROM vinplay.tx_user t LEFT JOIN vinplay.users u ON u.id=t.id WHERE u.id IS NULL;
