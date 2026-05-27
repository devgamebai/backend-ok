-- Phase A — referential integrity quick wins
-- Date: 2026-05-04
-- Audit doc: docs/db-audit/AUDIT_PHASE1_FINDINGS.md
--
-- Scope (revised after first apply):
--   1. Drop dead duplicate vinplay.awc_transactions (DONE on 2026-05-04 first run)
--   2. Bigint align user_id in vin_negative_alerts (DONE first run)
--   3. Bigint align money_account.owner_user_id
--   4. Add 4 missing player FKs (CASCADE)
--   5. Latin1 → utf8mb4 for 2 admin tables
--   6. nick_name immutability trigger
--
-- Excluded from Phase A:
--   * vinplay_admin.user_recovery_codes — already correctly FKed to
--     vinplay_admin.user (admin schema), NOT to vinplay.users (players)
--   * vinplay_admin.userrole — has 37 orphan rows of 46. Needs data cleanup
--     ticket before any FK can be added. Track separately.
--   * tx_user_authority — already cascades transitively via tx_user.id
--
-- Pre-flight (verified 2026-05-04):
--   * 0 orphan rows in awc_user_residue, gsc_bets, vin_negative_alerts

-- 1. Bigint alignment (idempotent — re-running is no-op)
ALTER TABLE vinplay.money_account
    MODIFY COLUMN owner_user_id BIGINT DEFAULT NULL;

-- 2. Add missing player FKs.
--    Wrapped in dynamic SQL via stored proc would be cleaner, but the
--    existing migration corpus uses straight DDL — keep style consistent.
ALTER TABLE vinplay.awc_user_residue
    ADD CONSTRAINT fk_awc_residue_user
    FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.gsc_bets
    ADD CONSTRAINT fk_gsc_bets_user
    FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.vin_negative_alerts
    ADD CONSTRAINT fk_vin_neg_alerts_user
    FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- 3. Latin1 → utf8mb4 (admin schema)
ALTER TABLE vinplay_admin.log_loginadmin
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE vinplay_admin.price_giftcode
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. nick_name immutability trigger
DROP TRIGGER IF EXISTS vinplay.trg_users_nickname_immutable;

DELIMITER $$
CREATE TRIGGER vinplay.trg_users_nickname_immutable
BEFORE UPDATE ON vinplay.users
FOR EACH ROW
BEGIN
    IF NOT (OLD.nick_name <=> NEW.nick_name) THEN
        SIGNAL SQLSTATE '45000'
        -- 128 char SIGNAL MESSAGE_TEXT cap. Long form in audit doc.
        SET MESSAGE_TEXT = 'users.nick_name immutable: 66 tables cache it';
    END IF;
END$$
DELIMITER ;
