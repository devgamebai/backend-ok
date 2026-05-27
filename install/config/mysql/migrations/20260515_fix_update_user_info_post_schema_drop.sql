-- =============================================================================
-- 20260515_fix_update_user_info_post_schema_drop.sql
--
-- Player registration was returning errorCode=1001 silently after today's
-- 20260512_drop_users_xu_column / vin_total / xu_total drops. The proc
-- update_user_info type=9 (quick register), type=10 (Facebook signup), and
-- type=11 (Google signup) INSERTed into the dropped columns:
--
--     INSERT INTO users(user_name, password, vin, vin_total, xu, xu_total, avatar)
--     VALUES(..., 0, 0, 500000, 500000, '0');
--
-- After the column drops, this INSERT throws ERROR 1054 'Unknown column
-- vin_total in field list'. The Java caller (QuickRegisterProcessor) catches
-- the SQLException silently and the response keeps the default errorCode=1001.
-- Repro:
--     CALL update_user_info(0, 'testuser,passhash', 9);
--     -- ERROR 1054 (42S22): Unknown column 'vin_total' in 'field list'
--
-- This migration recreates the proc, dropping the deleted columns from each
-- INSERT statement. `vin` is initialised to 0 (unchanged). The legacy 500,000
-- xu seed for promo wallet is gone with the column itself. Avatar / nick_name /
-- create_time etc. defaults stay.
-- =============================================================================

DROP PROCEDURE IF EXISTS update_user_info;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_user_info`(
    IN p_user_id INT(11),
    IN p_new     NVARCHAR(100),
    IN p_type    INT(11))
BEGIN
    IF p_type = 1 THEN
        UPDATE users SET avatar = p_new WHERE id = p_user_id;
    ELSEIF p_type = 2 THEN
        UPDATE users SET `password` = p_new WHERE id = p_user_id;
    ELSEIF p_type = 3 THEN
        UPDATE users SET identification = p_new WHERE id = p_user_id;
    ELSEIF p_type = 4 THEN
        UPDATE users SET mobile = p_new WHERE id = p_user_id;
    ELSEIF p_type = 5 THEN
        UPDATE users SET email = p_new WHERE id = p_user_id;
    ELSEIF p_type = 6 THEN
        UPDATE users SET nick_name = p_new WHERE id = p_user_id;
    ELSEIF p_type = 7 THEN
        UPDATE users SET `status` = p_new WHERE id = p_user_id;
    ELSEIF p_type = 8 THEN
        UPDATE users SET `status` = SUBSTRING_INDEX(p_new, ',', -1),
                         `mobile` = SUBSTRING_INDEX(p_new, ',', 1)
         WHERE id = p_user_id;
    ELSEIF p_type = 9 THEN
        -- Quick register (c=1). xu/vin_total/xu_total columns dropped.
        INSERT INTO users(user_name, `password`, vin, avatar)
        VALUES(SUBSTRING_INDEX(p_new, ',', 1), SUBSTRING_INDEX(p_new, ',', -1), 0, '0');
    ELSEIF p_type = 10 THEN
        -- Facebook OAuth signup.
        INSERT INTO users(user_name, facebook_id, vin, avatar)
        VALUES(CONCAT('FB_', UNIX_TIMESTAMP()), p_new, 0, '0');
    ELSEIF p_type = 11 THEN
        -- Google OAuth signup.
        INSERT INTO users(user_name, google_id, vin, avatar)
        VALUES(CONCAT('GG_', UNIX_TIMESTAMP()), p_new, 0, '0');
    ELSEIF p_type = 12 THEN
        UPDATE users SET login_otp = SUBSTRING_INDEX(p_new, ',', 1),
                         `status`  = SUBSTRING_INDEX(p_new, ',', -1)
         WHERE id = p_user_id;
    ELSEIF p_type = 13 THEN
        UPDATE users SET `status` = p_new, security_time = CURRENT_TIMESTAMP
         WHERE id = p_user_id;
    END IF;
END//
DELIMITER ;
