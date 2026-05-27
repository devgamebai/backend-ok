-- =============================================================================
-- 20260515_atomic_money_procs_v3_post_schema_drop.sql
--
-- V3 of the wallet procs, prepared to run AFTER the SUN-13xx column drops:
--
--   - 20260512_drop_users_xu_column.sql           (DROP users.xu)
--   - 20260512_drop_users_safe_column.sql         (DROP users.safe)
--   - 20260512_drop_vip_columns_and_fix_trigger.sql (DROP vip_point/vip_point_save/money_vp)
--   - 20260512_phase4_drop_legacy_sp_vin_total_xu_total.sql (DROP vin_total/xu_total)
--   - 20260512_drop_users_recharge_money_gift_total.sql (DROP recharge_money/gift_total)
--   - 20260512_wave2_drop_legacy_columns_and_tables.sql (vp_lv_receive, manual_quota,
--                                                       useragent.path_ancestors,
--                                                       useragent.wallet_balance,
--                                                       money_transaction.reversed_at)
--
-- After those drops, the only money-related column on `users` is `vin` (plus
-- analytic counters t_nap / t_rut / nap_times / rut_times that aren't slated
-- for removal in this wave). The procs therefore reduce to atomic-delta on
-- `vin` only.
--
-- Parameter signatures are kept identical to V2 so the Java callers in
-- backend-master/api/vbee don't need a second round of changes — the now-
-- unused params (p_money_total, p_money_vp, p_vp, p_money_type='xu' branch)
-- are silently ignored.
-- =============================================================================

-- ─── 1. update_money_user ────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS update_money_user;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_money_user`(
    IN p_user_id     INT(11),
    IN p_money       BIGINT(20),
    IN p_money_use   BIGINT(20),
    IN p_money_total BIGINT(20),
    IN p_money_type  VARCHAR(5),
    IN p_fee         BIGINT(20),
    IN p_action_name NVARCHAR(45),
    IN p_money_vp    INT(11),
    IN p_vp          INT(11),
    IN p_type        INT(11))
proc_body: BEGIN
    DECLARE v_curr_vin    BIGINT DEFAULT 0;
    DECLARE v_user_exists TINYINT DEFAULT 0;
    DECLARE v_new_vin     BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin INTO v_user_exists, v_curr_vin
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_user: user not found';
    END IF;

    -- p_money_type='xu' branch is dead post-drop. We ignore the differentiation
    -- and apply the delta to `vin`. If the caller still passes 'xu' (no caller
    -- in production code does), this becomes a no-op-equivalent vin write.

    IF p_type = 1 THEN
        UPDATE users
           SET vin       = vin + p_money,
               nap_times = nap_times + 1,
               t_nap     = t_nap + p_money
         WHERE id = p_user_id;
    ELSEIF p_type = 0 THEN
        IF p_action_name = 'REQUEST_CASHOUT' THEN
            UPDATE users
               SET vin       = vin + p_money,
                   t_rut     = t_rut + p_money,
                   rut_times = rut_times + 1
             WHERE id = p_user_id;
        ELSEIF p_action_name = 'REFUND_RECHARGE' THEN
            UPDATE users
               SET vin       = vin + p_money,
                   t_rut     = t_rut - p_money,
                   rut_times = rut_times - 1
             WHERE id = p_user_id;
        ELSE
            UPDATE users SET vin = vin + p_money WHERE id = p_user_id;
        END IF;
    ELSE
        -- p_type IN (2, 3): generic credit/debit. VIP-point bookkeeping retired.
        UPDATE users SET vin = vin + p_money WHERE id = p_user_id;
    END IF;

    SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, 'vin',
         CONCAT('PROC_UPDATE_MONEY_USER_T', p_type),
         CONCAT('proc-umu-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
         IFNULL(p_action_name, ''), v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_use != 0 AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('update_money_user', p_user_id, p_money_use, v_new_vin, NULL, NULL, p_money, p_action_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 2. update_money_in_game (11 params) ─────────────────────────────────────
DROP PROCEDURE IF EXISTS update_money_in_game;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_money_in_game`(
    IN p_session_id    NVARCHAR(100),
    IN p_user_id       INT(11),
    IN p_game_name     NVARCHAR(45),
    IN p_money_use     BIGINT(20),
    IN p_money_total   BIGINT(20),
    IN p_money_freeze  BIGINT(20),
    IN p_money_type    VARCHAR(5),
    IN p_fee           BIGINT(20),
    IN p_money_vp      INT(11),
    IN p_vp            INT(11),
    IN p_money         BIGINT(20))
proc_body: BEGIN
    DECLARE v_curr_vin    BIGINT DEFAULT 0;
    DECLARE v_user_exists TINYINT DEFAULT 0;
    DECLARE v_new_vin     BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin INTO v_user_exists, v_curr_vin
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_in_game: user not found';
    END IF;

    UPDATE users SET vin = vin + p_money WHERE id = p_user_id;
    SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;

    IF p_money_freeze > -1 THEN
        UPDATE freeze_money SET money = p_money_freeze
         WHERE session_id = p_session_id AND user_id = p_user_id AND `status` = 1;
    END IF;

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, 'vin',
         'PROC_UPDATE_MONEY_IN_GAME',
         CONCAT('proc-umig-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
         IFNULL(p_game_name, ''), v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_use != 0 AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('update_money_in_game', p_user_id, p_money_use, v_new_vin, NULL, NULL, p_money, p_game_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 3. restore_money ────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS restore_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `restore_money`(
    IN p_session_id     NVARCHAR(100),
    IN p_user_id        INT(11),
    IN p_money_use      BIGINT(20),
    IN p_money_total    BIGINT(20),
    IN p_money_exchange BIGINT(20),
    IN p_money_type     VARCHAR(5))
proc_body: BEGIN
    DECLARE v_curr_vin    BIGINT DEFAULT 0;
    DECLARE v_user_exists TINYINT DEFAULT 0;
    DECLARE v_new_vin     BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    IF p_money_exchange > 0 THEN
        SELECT 1, vin INTO v_user_exists, v_curr_vin
          FROM users WHERE id = p_user_id FOR UPDATE;
        IF v_user_exists = 0 THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'restore_money: user not found';
        END IF;

        UPDATE users SET vin = vin + p_money_exchange WHERE id = p_user_id;
        SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;

        INSERT IGNORE INTO money_gateway_log
            (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
        VALUES
            (p_user_id, '', p_money_exchange, 'vin',
             'PROC_RESTORE_MONEY',
             CONCAT('proc-rm-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
             IFNULL(p_session_id, ''), v_new_vin, NOW());

        IF p_money_use IS NOT NULL AND p_money_use != 0 AND v_new_vin != p_money_use THEN
            INSERT INTO money_proc_drift
                (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
            VALUES
                ('restore_money', p_user_id, p_money_use, v_new_vin, NULL, NULL, p_money_exchange, p_session_id, 0);
        END IF;
    END IF;

    UPDATE freeze_money SET money = 0, `status` = 0
     WHERE session_id = p_session_id AND user_id = p_user_id;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 4. safe_money (5 params; safe column dropped, p_money_safe ignored) ─────
DROP PROCEDURE IF EXISTS safe_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `safe_money`(
    IN p_user_id     INT(11),
    IN p_money_use   BIGINT(20),
    IN p_money_total BIGINT(20),
    IN p_money_safe  BIGINT(20),
    IN p_money       BIGINT(20))
proc_body: BEGIN
    DECLARE v_curr_vin    BIGINT DEFAULT 0;
    DECLARE v_user_exists TINYINT DEFAULT 0;
    DECLARE v_new_vin     BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin INTO v_user_exists, v_curr_vin
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'safe_money: user not found';
    END IF;

    -- Safe column dropped post-SUN-13xx. SafeBox functionality moved to
    -- the MongoDB safe_box collection. This proc only adjusts vin.
    UPDATE users SET vin = vin + p_money WHERE id = p_user_id;
    SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, 'vin',
         'PROC_SAFE_MONEY',
         CONCAT('proc-sm-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
         'safe-box transfer (legacy proc)', v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_use != 0 AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('safe_money', p_user_id, p_money_use, v_new_vin, NULL, NULL, p_money, 'safe', 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 5. freeze_money (debit vin by p_money_exchange) ─────────────────────────
DROP PROCEDURE IF EXISTS freeze_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `freeze_money`(
    IN p_session_id     NVARCHAR(100),
    IN p_user_id        INT(11),
    IN p_game_name      NVARCHAR(45),
    IN p_room_id        NVARCHAR(100),
    IN p_money_use      BIGINT(20),
    IN p_money_total    BIGINT(20),
    IN p_money_exchange BIGINT(20),
    IN p_money_type     VARCHAR(5),
    IN p_nick_name      NVARCHAR(45))
proc_body: BEGIN
    DECLARE v_curr_vin    BIGINT DEFAULT 0;
    DECLARE v_user_exists TINYINT DEFAULT 0;
    DECLARE v_new_vin     BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin INTO v_user_exists, v_curr_vin
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: user not found';
    END IF;
    IF v_curr_vin < p_money_exchange THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: insufficient balance';
    END IF;

    UPDATE users SET vin = vin - p_money_exchange WHERE id = p_user_id;
    SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;

    INSERT INTO freeze_money
        (session_id, user_id, game_name, room_id, money, money_type, create_time, status, nick_name)
    VALUES
        (p_session_id, p_user_id, p_game_name, p_room_id, p_money_exchange, p_money_type, NOW(), 1, p_nick_name);

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, IFNULL(p_nick_name, ''), -p_money_exchange, 'vin',
         'PROC_FREEZE_MONEY',
         CONCAT('proc-fm-', p_user_id, '-', p_session_id),
         IFNULL(p_game_name, ''), v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_use != 0 AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('freeze_money', p_user_id, p_money_use, v_new_vin, NULL, NULL, -p_money_exchange, p_game_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;
