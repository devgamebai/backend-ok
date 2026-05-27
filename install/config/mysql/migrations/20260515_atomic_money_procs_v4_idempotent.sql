-- =============================================================================
-- 20260515_atomic_money_procs_v4_idempotent.sql
--
-- HOT-FIX for the V3 regression that surfaced as Vithoang's 40k gift code
-- double-credit (should have been 20k).
--
-- Root cause: dual-write producers (RedeemAdminGiftCodeProcessor,
-- ClaimCashbackProcessor, MoneyInGameServiceImpl USERSERVICE_GAME path,
-- and probably more) DO BOTH:
--   1. MoneyGateway.creditUser / debitUser  (atomic primary write +
--                                            money_gateway_log audit row)
--   2. Publish MoneyMessageInMinigame to RMQ → vbee → CALL update_money_user
--
-- Pre-V3 (absolute write): step 2 was idempotent — `UPDATE users SET vin
-- = p_money_use` set the same absolute value step 1 already produced, so
-- the second write was a no-op.
--
-- V3 (atomic delta): step 2 became `UPDATE users SET vin = vin + p_money`
-- — applies the delta a SECOND time on top of step 1's already-applied
-- delta. Net: every dual-write producer credits/debits twice.
--
-- Audit since V3 deploy (12:16 KR → 13:25 KR):
--   - 14 double-credit pairs in money_gateway_log
--   - 2 real-player gift code overcredits (Vithoang 9621, Bdndncmmc 9585 — 20k each)
--   - 12 bot/auto USERSERVICE_GAME double-applies (5609 binly3bi, 5303 VTD_2vK1)
--
-- V4 FIX:
--   Add an idempotency check at the top of each proc:
--
--     SELECT vin INTO v_curr FROM users WHERE id = p_user_id FOR UPDATE;
--     IF v_curr = p_money_use AND p_money != 0 THEN
--         -- Caller's expected post-state already in DB. Primary path
--         -- (MoneyGateway.creditUser/debitUser) already applied this
--         -- delta. Treat the message as a duplicate audit ping — skip
--         -- the wallet update, but still write a PROC_DEDUP audit row
--         -- for visibility, and commit.
--         RETURN early.
--     END IF;
--
--     -- Otherwise, normal atomic-delta path (V3 behavior).
--
-- This restores idempotency for dual-write producers while keeping
-- race-safety for solo-RMQ producers (TaiXiu, Sicbo, BauCua etc. where
-- the proc IS the only writer).
--
-- Edge case: if a producer's pre-computed p_money_use ACCIDENTALLY
-- matches the current vin (extremely rare numerical coincidence), the
-- atomic delta gets skipped. Acceptable: we lose at most one delta
-- application, which is better than double-applying.
--
-- A side-by-side `PROC_DEDUP_SKIP` audit row in money_gateway_log makes
-- the skip visible for post-deploy verification.
-- =============================================================================

USE vinplay;

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

    -- IDEMPOTENCY GUARD: if vin already equals caller's expected post-state
    -- (and the message claims a non-zero delta), a primary writer
    -- (MoneyGateway) already applied this same delta. Skip the wallet update
    -- to avoid double-credit. Write a dedup audit row instead.
    IF p_money != 0 AND p_money_use IS NOT NULL AND p_money_use != 0
       AND v_curr_vin = p_money_use THEN
        INSERT IGNORE INTO money_gateway_log
            (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
        VALUES
            (p_user_id, '', p_money, 'vin',
             CONCAT('PROC_DEDUP_SKIP_T', p_type),
             CONCAT('proc-umu-dedup-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
             CONCAT('SKIP-already-applied: ', IFNULL(p_action_name,'')),
             v_curr_vin, NOW());
        COMMIT;
        LEAVE proc_body;
    END IF;

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

-- ─── 2. update_money_in_game (same idempotency guard) ────────────────────────
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

    -- Idempotency dedup
    IF p_money != 0 AND p_money_use IS NOT NULL AND p_money_use != 0
       AND v_curr_vin = p_money_use THEN
        INSERT IGNORE INTO money_gateway_log
            (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
        VALUES
            (p_user_id, '', p_money, 'vin',
             'PROC_DEDUP_SKIP_IN_GAME',
             CONCAT('proc-umig-dedup-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
             CONCAT('SKIP-already-applied: ', IFNULL(p_game_name,'')),
             v_curr_vin, NOW());
        IF p_money_freeze > -1 THEN
            UPDATE freeze_money SET money = p_money_freeze
             WHERE session_id = p_session_id AND user_id = p_user_id AND `status` = 1;
        END IF;
        COMMIT;
        LEAVE proc_body;
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

-- ─── 3. restore_money (idempotency: skip if vin already == p_money_use) ─────
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

        IF p_money_use IS NOT NULL AND p_money_use != 0 AND v_curr_vin = p_money_use THEN
            INSERT IGNORE INTO money_gateway_log
                (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
            VALUES
                (p_user_id, '', p_money_exchange, 'vin',
                 'PROC_DEDUP_SKIP_RESTORE',
                 CONCAT('proc-rm-dedup-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
                 CONCAT('SKIP-already-applied: ', IFNULL(p_session_id,'')),
                 v_curr_vin, NOW());
            UPDATE freeze_money SET money = 0, `status` = 0
             WHERE session_id = p_session_id AND user_id = p_user_id;
            COMMIT;
            LEAVE proc_body;
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

-- ─── 4. safe_money (idempotency on vin) ─────────────────────────────────────
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

    IF p_money != 0 AND p_money_use IS NOT NULL AND p_money_use != 0
       AND v_curr_vin = p_money_use THEN
        INSERT IGNORE INTO money_gateway_log
            (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
        VALUES
            (p_user_id, '', p_money, 'vin',
             'PROC_DEDUP_SKIP_SAFE',
             CONCAT('proc-sm-dedup-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
             'SKIP-already-applied safe-box',
             v_curr_vin, NOW());
        COMMIT;
        LEAVE proc_body;
    END IF;

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

-- ─── 5. freeze_money (idempotency: if vin already debited to p_money_use) ───
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

    -- Idempotency: if primary writer already debited vin to p_money_use
    IF p_money_exchange != 0 AND p_money_use IS NOT NULL AND p_money_use != 0
       AND v_curr_vin = p_money_use THEN
        INSERT INTO freeze_money
            (session_id, user_id, game_name, room_id, money, money_type, create_time, status, nick_name)
        VALUES
            (p_session_id, p_user_id, p_game_name, p_room_id, p_money_exchange, p_money_type, NOW(), 1, p_nick_name);
        INSERT IGNORE INTO money_gateway_log
            (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
        VALUES
            (p_user_id, IFNULL(p_nick_name,''), -p_money_exchange, 'vin',
             'PROC_DEDUP_SKIP_FREEZE',
             CONCAT('proc-fm-dedup-', p_user_id, '-', p_session_id),
             CONCAT('SKIP-already-applied: ', IFNULL(p_game_name,'')),
             v_curr_vin, NOW());
        COMMIT;
        LEAVE proc_body;
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
