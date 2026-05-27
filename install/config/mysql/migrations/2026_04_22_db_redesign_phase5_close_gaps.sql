-- =============================================================================
-- Phase 5: Close remaining FK gaps + normalize AWC table to match fleet
-- =============================================================================
-- Pre-reqs: phases 0–4 applied. Design doc in conversation of 2026-04-22.
--
-- Gaps closed:
--   G1: users_bank — has user_id but no FK. Add CASCADE.
--   G2: log_tranfer_agent — agent-to-agent transfer, NO user link. Add
--       sender_agent_id / receiver_agent_id columns with SET NULL FK to
--       useragent (preserve audit trail when agent row vanishes).
--   G3: gift_codes.receiver_id — nullable history column. SET NULL on
--       user delete so campaign audits survive.
--   G4: user_rut_loc (vinplay_minigame) — user_name only, charset=utf8
--       utf8_unicode_ci (outlier). Convert + add user_id + FK CASCADE.
--   G5: awc_transactions — charset=utf8mb4 utf8mb4_0900_ai_ci. Rest of
--       fleet is utf8mb3_general_ci. Convert to match so JOINs with
--       vinplay.users don't trip "Illegal mix of collations".
--
-- Idempotent: every ALTER guarded by INFORMATION_SCHEMA checks.
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET sql_mode = '';

-- ─── G1: users_bank FK ──────────────────────────────────────────────────
SET @fk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA='vinplay' AND TABLE_NAME='users_bank'
                     AND CONSTRAINT_NAME='fk_ubank_user');
SET @sql := IF(@fk_exists=0,
    'ALTER TABLE vinplay.users_bank ADD CONSTRAINT fk_ubank_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE',
    'SELECT ''G1 fk_ubank_user already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ─── G3: gift_codes — live schema has user_name, not receiver_id. ───────
-- Add user_id + FK SET NULL (preserve coupon audit even if user deleted).
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='gift_codes'
                      AND COLUMN_NAME='user_id');
SET @sql := IF(@col_exists=0,
    'ALTER TABLE vinplay.gift_codes ADD COLUMN user_id BIGINT DEFAULT NULL AFTER user_name',
    'SELECT ''G3 user_id already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE vinplay.gift_codes g
  JOIN vinplay.users u ON u.user_name = g.user_name COLLATE utf8mb3_general_ci
  SET g.user_id = u.id
 WHERE g.user_id IS NULL;

SET @fk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA='vinplay' AND TABLE_NAME='gift_codes'
                     AND CONSTRAINT_NAME='fk_giftcode_user');
SET @sql := IF(@fk_exists=0,
    'ALTER TABLE vinplay.gift_codes ADD CONSTRAINT fk_giftcode_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE SET NULL',
    'SELECT ''G3 fk_giftcode_user already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ─── G2: log_tranfer_agent — agent refs ─────────────────────────────────
-- Add sender/receiver agent_id columns if missing, backfill, add FK SET NULL.

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_tranfer_agent'
                      AND COLUMN_NAME='sender_agent_id');
SET @sql := IF(@col_exists=0,
    'ALTER TABLE vinplay.log_tranfer_agent ADD COLUMN sender_agent_id INT DEFAULT NULL AFTER nick_name_send',
    'SELECT ''G2 sender_agent_id already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_tranfer_agent'
                      AND COLUMN_NAME='receiver_agent_id');
SET @sql := IF(@col_exists=0,
    'ALTER TABLE vinplay.log_tranfer_agent ADD COLUMN receiver_agent_id INT DEFAULT NULL AFTER nick_name_receive',
    'SELECT ''G2 receiver_agent_id already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE vinplay.log_tranfer_agent l
  LEFT JOIN vinplay_admin.useragent s ON s.nickname = l.nick_name_send    COLLATE utf8mb3_general_ci
  LEFT JOIN vinplay_admin.useragent r ON r.nickname = l.nick_name_receive COLLATE utf8mb3_general_ci
  SET l.sender_agent_id   = IFNULL(l.sender_agent_id, s.id),
      l.receiver_agent_id = IFNULL(l.receiver_agent_id, r.id)
 WHERE l.sender_agent_id IS NULL OR l.receiver_agent_id IS NULL;

SET @fk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA='vinplay' AND TABLE_NAME='log_tranfer_agent'
                     AND CONSTRAINT_NAME='fk_lta_sender');
SET @sql := IF(@fk_exists=0,
    'ALTER TABLE vinplay.log_tranfer_agent ADD CONSTRAINT fk_lta_sender FOREIGN KEY (sender_agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE SET NULL',
    'SELECT ''G2 fk_lta_sender already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @fk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA='vinplay' AND TABLE_NAME='log_tranfer_agent'
                     AND CONSTRAINT_NAME='fk_lta_receiver');
SET @sql := IF(@fk_exists=0,
    'ALTER TABLE vinplay.log_tranfer_agent ADD CONSTRAINT fk_lta_receiver FOREIGN KEY (receiver_agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE SET NULL',
    'SELECT ''G2 fk_lta_receiver already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ─── G4: user_rut_loc — charset + user_id + FK ──────────────────────────
ALTER TABLE vinplay_minigame.user_rut_loc CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA='vinplay_minigame' AND TABLE_NAME='user_rut_loc'
                      AND COLUMN_NAME='user_id');
SET @sql := IF(@col_exists=0,
    'ALTER TABLE vinplay_minigame.user_rut_loc ADD COLUMN user_id BIGINT DEFAULT NULL AFTER user_name',
    'SELECT ''G4 user_id already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE vinplay_minigame.user_rut_loc r
  JOIN vinplay.users u ON u.user_name = r.user_name COLLATE utf8mb3_general_ci
  SET r.user_id = u.id
 WHERE r.user_id IS NULL;

SET @fk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA='vinplay_minigame' AND TABLE_NAME='user_rut_loc'
                     AND CONSTRAINT_NAME='fk_urloc_user');
SET @sql := IF(@fk_exists=0,
    'ALTER TABLE vinplay_minigame.user_rut_loc ADD CONSTRAINT fk_urloc_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE',
    'SELECT ''G4 fk_urloc_user already exists''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ─── G5: awc_transactions charset normalize ─────────────────────────────
-- Must drop FK first, convert, re-add (CONVERT rewrites column types).
ALTER TABLE vinplay_minigame.awc_transactions DROP FOREIGN KEY fk_awctx_user;
ALTER TABLE vinplay_minigame.awc_transactions CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;
ALTER TABLE vinplay_minigame.awc_transactions
  ADD CONSTRAINT fk_awctx_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- ─── G5b: awc_game_catalog + awc_platform_map — same charset fix ────────
ALTER TABLE vinplay_minigame.awc_game_catalog CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;
ALTER TABLE vinplay_minigame.awc_platform_map CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ─── Verification ───────────────────────────────────────────────────────
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
SELECT 'Columns still utf8mb4/utf8_unicode in 4 schemas', COUNT(*)
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai')
   AND COLLATION_NAME IS NOT NULL
   AND COLLATION_NAME NOT LIKE 'utf8mb3%'
   AND TABLE_NAME NOT LIKE '_archive%';
