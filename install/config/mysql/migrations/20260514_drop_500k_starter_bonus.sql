-- 2026-05-14: two changes in one SP rewrite —
--   1. drop the legacy 500,000 VIN welcome-bonus that update_user_info
--      types 9/10/11 hard-coded; new accounts now start at 0 (operator
--      handles promotional credits via the explicit deposit-promotion flow).
--   2. remove the Facebook (type=10) and Google (type=11) signup branches
--      entirely — operator no longer supports social signup. Existing
--      vinplay.users.facebook_id / google_id rows stay readable; only
--      the create-new-social-account path is gone.
USE vinplay;

DROP PROCEDURE IF EXISTS update_user_info;
DELIMITER //
CREATE PROCEDURE update_user_info(IN p_user_id INT, IN p_new VARCHAR(2000), IN p_type INT)
BEGIN
    IF p_type = 1 THEN UPDATE users SET avatar = p_new WHERE id = p_user_id;
    ELSEIF p_type = 2 THEN UPDATE users SET `password` = p_new WHERE id = p_user_id;
    ELSEIF p_type = 3 THEN UPDATE users SET identification = p_new WHERE id = p_user_id;
    ELSEIF p_type = 4 THEN UPDATE users SET mobile = p_new WHERE id = p_user_id;
    ELSEIF p_type = 5 THEN UPDATE users SET email = p_new WHERE id = p_user_id;
    ELSEIF p_type = 6 THEN UPDATE users SET nick_name = p_new WHERE id = p_user_id;
    ELSEIF p_type = 7 THEN UPDATE users SET `status` = p_new WHERE id = p_user_id;
    ELSEIF p_type = 8 THEN UPDATE users SET `status` = SUBSTRING_INDEX(p_new, ',', -1), `mobile` = SUBSTRING_INDEX(p_new, ',', 1) WHERE id = p_user_id;
    ELSEIF p_type = 9 THEN
        INSERT INTO users(user_name, `password`, vin, avatar)
        VALUES(SUBSTRING_INDEX(p_new, ',', 1), SUBSTRING_INDEX(p_new, ',', -1), 0, '0');
    -- types 10 (Facebook signup) and 11 (Google signup) removed 2026-05-14.
    -- Callers that still hit those branches get a no-op insert and the
    -- caller surfaces a generic register failure — by design.
    ELSEIF p_type = 12 THEN UPDATE users SET login_otp = SUBSTRING_INDEX(p_new, ',', 1), `status` = SUBSTRING_INDEX(p_new, ',', -1) WHERE id = p_user_id;
    ELSEIF p_type = 13 THEN UPDATE users SET `status` = p_new, security_time = CURRENT_TIMESTAMP WHERE id = p_user_id;
    END IF;
END //
DELIMITER ;
