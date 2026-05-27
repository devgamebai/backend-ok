-- =============================================================================
-- 20260516_atomic_money_procs_v5_floor_and_zero_dedup.sql
--
-- Two regressions discovered after V4 deploy:
--
-- A. NO atomic floor check on the V3/V4 UPDATE statement.
--    Original MoneyGateway.debitUser had:
--      UPDATE users SET vin = vin - ? WHERE id = ? AND vin >= ?
--    V3 converted to atomic-delta but dropped the floor:
--      UPDATE users SET vin = vin + p_money WHERE id = p_user_id
--    → ANY negative delta succeeds even when |delta| > current vin.
--
-- B. V4 idempotency dedup guard excludes 0-balance edge case.
--    Condition was:
--      IF p_money != 0 AND p_money_use IS NOT NULL AND p_money_use != 0
--         AND v_curr_vin = p_money_use THEN SKIP
--    The `p_money_use != 0` clause prevents SKIP when caller's expected
--    post-balance is exactly 0 — but 0 is a valid match (player betted
--    their last KRW). The RMQ-fed proc then double-applies the debit,
--    pushing vin negative.
--
-- Live victim: Quanlu99 (id=9166) 2026-05-16 11:33:27 KR. After 4 bets
-- correctly dedup-skipped (PROC_DEDUP_SKIP_T0 at balances 400/300/200/100),
-- the 5th bet (100→0 then 0→-100) leaked because dedup didn't fire at 0.
--
-- V5 FIX:
--   1. Add `AND vin + p_money >= 0` (or xu equivalent) to every UPDATE
--      so a debit that would go negative is rejected at SQL level.
--      Caller catches SQLException (or ROW_COUNT()=0) and surfaces an
--      "insufficient balance" error instead of silently corrupting state.
--   2. Remove the `p_money_use != 0` clause from the dedup guard. Only
--      require non-null (IS NOT NULL).
--   3. When the floor check rejects, ROLLBACK + SIGNAL '45000' so caller
--      sees a clean error (mirrors MoneyGateway.ERROR_INSUFFICIENT_BALANCE).
--
-- A complementary CHECK constraint on users.vin lands in a separate
-- migration (20260516_users_vin_nonneg_check.sql) so even a future
-- writer that bypasses these procs cannot push vin negative.
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
    DECLARE v_affected    INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin INTO v_user_exists, v_curr_vin
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_user: user not found';
    END IF;

    -- V5 fix B: dedup guard accepts 0 as a valid match (drop p_money_use != 0).
    IF p_money != 0 AND p_money_use IS NOT NULL AND v_curr_vin = p_money_use THEN
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

    -- V5 fix A: every UPDATE carries `AND vin + p_money >= 0` floor check.
    -- ROW_COUNT() = 0 on insufficient balance → SIGNAL 45000.
    IF p_type = 1 THEN
        UPDATE users
           SET vin       = vin + p_money,
               nap_times = nap_times + 1,
               t_nap     = t_nap + p_money
         WHERE id = p_user_id AND vin + p_money >= 0;
    ELSEIF p_type = 0 THEN
        IF p_action_name = 'REQUEST_CASHOUT' THEN
            UPDATE users
               SET vin       = vin + p_money,
                   t_rut     = t_rut + p_money,
                   rut_times = rut_times + 1
             WHERE id = p_user_id AND vin + p_money >= 0;
        ELSEIF p_action_name = 'REFUND_RECHARGE' THEN
            UPDATE users
               SET vin       = vin + p_money,
                   t_rut     = t_rut - p_money,
                   rut_times = rut_times - 1
             WHERE id = p_user_id AND vin + p_money >= 0;
        ELSE
            UPDATE users SET vin = vin + p_money
             WHERE id = p_user_id AND vin + p_money >= 0;
        END IF;
    ELSE
        UPDATE users SET vin = vin + p_money
         WHERE id = p_user_id AND vin + p_money >= 0;
    END IF;

    SET v_affected = ROW_COUNT();
    IF v_affected = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_user: insufficient balance';
    END IF;

    SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, 'vin',
         CONCAT('PROC_UPDATE_MONEY_USER_T', p_type),
         CONCAT('proc-umu-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
         IFNULL(p_action_name, ''), v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('update_money_user', p_user_id, p_money_use, v_new_vin, NULL, NULL, p_money, p_action_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 2. update_money_in_game ─────────────────────────────────────────────────
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
    DECLARE v_affected    INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin INTO v_user_exists, v_curr_vin
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_in_game: user not found';
    END IF;

    IF p_money != 0 AND p_money_use IS NOT NULL AND v_curr_vin = p_money_use THEN
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

    UPDATE users SET vin = vin + p_money
     WHERE id = p_user_id AND vin + p_money >= 0;
    SET v_affected = ROW_COUNT();
    IF v_affected = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'update_money_in_game: insufficient balance';
    END IF;

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

    IF p_money_use IS NOT NULL AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('update_money_in_game', p_user_id, p_money_use, v_new_vin, NULL, NULL, p_money, p_game_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 3. restore_money (credit only; floor not needed) ────────────────────────
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

        IF p_money_use IS NOT NULL AND v_curr_vin = p_money_use THEN
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

        IF p_money_use IS NOT NULL AND v_new_vin != p_money_use THEN
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

-- ─── 4. safe_money (signed delta on vin) ────────────────────────────────────
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
    DECLARE v_affected    INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin INTO v_user_exists, v_curr_vin
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'safe_money: user not found';
    END IF;

    IF p_money != 0 AND p_money_use IS NOT NULL AND v_curr_vin = p_money_use THEN
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

    UPDATE users SET vin = vin + p_money
     WHERE id = p_user_id AND vin + p_money >= 0;
    SET v_affected = ROW_COUNT();
    IF v_affected = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'safe_money: insufficient balance';
    END IF;

    SELECT vin INTO v_new_vin FROM users WHERE id = p_user_id;

    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, 'vin',
         'PROC_SAFE_MONEY',
         CONCAT('proc-sm-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000),
         'safe-box transfer (legacy proc)', v_new_vin, NOW());

    IF p_money_use IS NOT NULL AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('safe_money', p_user_id, p_money_use, v_new_vin, NULL, NULL, p_money, 'safe', 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 5. freeze_money (debit by p_money_exchange) ────────────────────────────
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
    DECLARE v_affected    INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SELECT 1, vin INTO v_user_exists, v_curr_vin
      FROM users WHERE id = p_user_id FOR UPDATE;
    IF v_user_exists = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: user not found';
    END IF;

    IF p_money_exchange != 0 AND p_money_use IS NOT NULL AND v_curr_vin = p_money_use THEN
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

    UPDATE users SET vin = vin - p_money_exchange
     WHERE id = p_user_id AND vin - p_money_exchange >= 0;
    SET v_affected = ROW_COUNT();
    IF v_affected = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: insufficient balance (race)';
    END IF;

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

    IF p_money_use IS NOT NULL AND v_new_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('freeze_money', p_user_id, p_money_use, v_new_vin, NULL, NULL, -p_money_exchange, p_game_name, 0);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;
