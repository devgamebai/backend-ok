-- Rollback for 20260504_phase_a_user_fk_cleanup.sql
-- WARNING: vinplay.awc_transactions DROP cannot be reverted from this script
--          (table was empty — restore schema from backup if needed).

START TRANSACTION;

-- Drop FKs
ALTER TABLE vinplay.awc_user_residue          DROP FOREIGN KEY fk_awc_residue_user;
ALTER TABLE vinplay.gsc_bets                  DROP FOREIGN KEY fk_gsc_bets_user;
ALTER TABLE vinplay.vin_negative_alerts       DROP FOREIGN KEY fk_vin_neg_alerts_user;
ALTER TABLE vinplay_admin.user_recovery_codes DROP FOREIGN KEY fk_user_recovery_user;
ALTER TABLE vinplay_admin.userrole            DROP FOREIGN KEY fk_userrole_user;

-- Restore int (note: data values preserved; only column type reverts)
ALTER TABLE vinplay.vin_negative_alerts       MODIFY COLUMN user_id INT NOT NULL;
ALTER TABLE vinplay_admin.user_recovery_codes MODIFY COLUMN user_id INT;
ALTER TABLE vinplay_admin.userrole            MODIFY COLUMN User_ID INT NOT NULL;
ALTER TABLE vinplay.money_account             MODIFY COLUMN owner_user_id INT DEFAULT NULL;

-- Latin1 revert (only do if you really need it — utf8mb4 is strict superset)
-- ALTER TABLE vinplay_admin.log_loginadmin
--     CONVERT TO CHARACTER SET latin1 COLLATE latin1_swedish_ci;
-- ALTER TABLE vinplay_admin.price_giftcode
--     CONVERT TO CHARACTER SET latin1 COLLATE latin1_swedish_ci;

COMMIT;

-- Drop trigger
DROP TRIGGER IF EXISTS vinplay.trg_users_nickname_immutable;
