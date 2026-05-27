-- =============================================================================
-- 20260515_fix_remaining_legacy_procs_post_schema_drop.sql
--
-- Sweep audit of all SPs after today's schema drops surfaced 5 more legacy
-- procs still referencing dropped columns (xu / vin_total / xu_total /
-- vip_point / vip_point_save / money_vp / safe). Each is rewritten or
-- neutered.
--
-- These were NOT in the staging drop migration set because they're
-- infrequently called and weren't on the SUN-13xx hot-path audit. Production
-- callers either:
--   (a) errored silently (Java catches SQLException and returns default code), or
--   (b) never call the proc (e.g. reset_xu is deprecated admin tool).
--
-- Fixes per proc:
--
--   1. insert_bot       — bot creation. xu/xu_total/vin_total removed from
--                         INSERT column list. money_system 'Xu_Bot' update
--                         neutered (no xu column to debit against).
--   2. reset_xu         — deprecated. Neutered to no-op; keeps signature so
--                         ResetXuServiceImpl doesn't break on the CALL.
--   3. update_money_cache — UPDATE vin only; vin_total / safe / xu / xu_total
--                         branches removed. p_money_total / p_money_safe params
--                         kept for ABI compat.
--   4. update_vippoint  — VIP system retired (vip_point/save/money_vp dropped).
--                         Neutered to no-op so VippointDaoImpl callers don't
--                         throw "Unknown column". Logs the call instead.
--   5. no_hu_game_bai   — minigame jackpot. UPDATE vin only.
--
-- (Excluded: cgame.SP_Login / vinplay_admin.SP_Login — they query their own
-- schema's `users` table which retains the cash/cash_safe/cash_silver/vip_point
-- columns. Not affected by today's vinplay.users drops.)
-- =============================================================================

USE vinplay;

-- ─── 1. insert_bot ──────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS insert_bot;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `insert_bot`(
    IN p_un     VARCHAR(50),
    IN p_nn     VARCHAR(50),
    IN p_pw     VARCHAR(255),
    IN p_vin    BIGINT,
    IN p_xu     BIGINT,        -- ignored (xu column dropped)
    IN p_status INT)
BEGIN
    INSERT INTO users(user_name, nick_name, `password`, vin, avatar, `status`, is_bot)
    VALUES(p_un, p_nn, p_pw, p_vin, '0', p_status, 1);
    UPDATE money_system SET money = money - p_vin WHERE `name` = 'Vin_Bot';
END//
DELIMITER ;

-- ─── 2. reset_xu (neutered — xu column dropped) ─────────────────────────────
DROP PROCEDURE IF EXISTS reset_xu;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `reset_xu`()
BEGIN
    -- xu column dropped in SUN-13xx. Proc kept as no-op so ResetXuServiceImpl
    -- doesn't fail on CALL; the underlying functionality is retired.
    DO 0;
END//
DELIMITER ;

-- ─── 3. update_money_cache (vin only) ───────────────────────────────────────
DROP PROCEDURE IF EXISTS update_money_cache;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_money_cache`(
    IN p_user_id     INT,
    IN p_money_use   BIGINT,
    IN p_money_total BIGINT,    -- ignored (vin_total dropped)
    IN p_money_safe  BIGINT,    -- ignored (safe dropped)
    IN p_money_type  VARCHAR(5))
BEGIN
    -- xu branch dropped. Anything passed as 'xu' falls through to no-op.
    IF p_money_type = 'vin' THEN
        UPDATE users SET vin = p_money_use WHERE id = p_user_id;
    END IF;
END//
DELIMITER ;

-- ─── 4. update_vippoint (neutered — VIP system retired) ────────────────────
DROP PROCEDURE IF EXISTS update_vippoint;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_vippoint`(
    IN p_user_id  INT,
    IN p_money_vp INT,
    IN p_vp       INT)
BEGIN
    -- vip_point / vip_point_save / money_vp columns dropped in SUN-13xx.
    -- Proc kept as no-op so VippointDaoImpl / vbee callers don't fail on
    -- CALL. Functionally a no-op; cumulative VP tracking is retired.
    DO 0;
END//
DELIMITER ;

-- ─── 5. no_hu_game_bai (vin only) ───────────────────────────────────────────
DROP PROCEDURE IF EXISTS vinplay_minigame.no_hu_game_bai;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `vinplay_minigame`.`no_hu_game_bai`(
    IN p_session_id    NVARCHAR(100),
    IN p_user_id       INT(11),
    IN p_pot_name      NVARCHAR(45),
    IN p_money_use     BIGINT(20),
    IN p_money_total   BIGINT(20),    -- ignored (vin_total dropped)
    IN p_money_freeze  BIGINT(20),
    IN p_money_pot     BIGINT(20))
proc_body: BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION ROLLBACK;
    DECLARE EXIT HANDLER FOR SQLWARNING ROLLBACK;
    START TRANSACTION;
        UPDATE vinplay.users SET vin = p_money_use WHERE id = p_user_id;
        UPDATE vinplay_minigame.hu_game_bai SET `value` = p_money_pot
         WHERE `pot_name` = p_pot_name;
        IF p_money_freeze > -1 THEN
            UPDATE vinplay.freeze_money SET money = p_money_freeze
             WHERE session_id = p_session_id;
        END IF;
    COMMIT;
END proc_body//
DELIMITER ;
