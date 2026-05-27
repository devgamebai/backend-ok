-- =============================================================================
-- SUN-968 [DB-4b]: Fix remaining FK gaps found during testing
-- =============================================================================
-- tx_user, users_game568win, gift_code_useds were missed in phase 2/3.
-- tx_user_authority had NO ACTION FK → changed to CASCADE.
-- =============================================================================

SET sql_mode = '';
SET FOREIGN_KEY_CHECKS = 0;

-- tx_user.id = users.id (set by TR_INSERT_ACCOUNT trigger)
ALTER TABLE vinplay.tx_user MODIFY id BIGINT NOT NULL;
ALTER TABLE vinplay.tx_user ADD CONSTRAINT fk_txuser_user
  FOREIGN KEY (id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- tx_user_authority FK was NO ACTION → change to CASCADE (chain: users → tx_user → tx_user_authority)
ALTER TABLE vinplay.tx_user_authority DROP FOREIGN KEY fk_tx_userid;
ALTER TABLE vinplay.tx_user_authority ADD CONSTRAINT fk_tx_userid
  FOREIGN KEY (user_id) REFERENCES vinplay.tx_user(id) ON DELETE CASCADE;

-- users_game568win: add user_id + FK
ALTER TABLE vinplay.users_game568win ADD COLUMN user_id BIGINT DEFAULT NULL;
UPDATE vinplay.users_game568win g JOIN vinplay.users u ON u.user_name = g.username SET g.user_id = u.id WHERE g.user_id IS NULL;
ALTER TABLE vinplay.users_game568win ADD CONSTRAINT fk_g568_user
  FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- gift_code_useds: add user_id + FK
ALTER TABLE vinplay.gift_code_useds ADD COLUMN user_id BIGINT DEFAULT NULL;
UPDATE vinplay.gift_code_useds g JOIN vinplay.users u ON u.user_name = g.username SET g.user_id = u.id WHERE g.user_id IS NULL;
ALTER TABLE vinplay.gift_code_useds ADD CONSTRAINT fk_gcused_user
  FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

-- Verify: should be 65+ FK on users
SELECT 'FK on users' c, COUNT(*) n FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME='users' AND REFERENCED_TABLE_SCHEMA='vinplay';
