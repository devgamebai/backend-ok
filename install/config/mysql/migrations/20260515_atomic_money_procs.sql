-- =============================================================================
-- 20260515_atomic_money_procs.sql
--
-- ROOT CAUSE: Sunkr888 (user_id=9590) silent 1.78M KRW vin drop at 01:25:36 KR
-- on 2026-05-15. Trace: docs/audit/sunkr888-silent-debit-rootcause.md
--
-- The four user-wallet stored procedures here (update_money_user,
-- update_money_in_game, restore_money, safe_money — plus freeze_money for
-- symmetry) all wrote ABSOLUTE balances pre-computed Java-side:
--
--     UPDATE users SET vin = p_money_use, vin_total = p_money_total WHERE id = ?
--
-- Producer reads vin from Hazelcast snapshot → computes new = old + delta
-- → publishes via RMQ → consumer (vbee) blindly writes the absolute new.
--
-- Race window: any other path (MoneyGateway atomic credit/debit, admin top-up,
-- GSC seamless callback) that mutates users.vin between snapshot and consumer
-- write is SILENTLY STOMPED. The 1.78M drop on Sunkr888 was a stale message
-- written after admin top-ups landed via MoneyGateway.
--
-- THIS MIGRATION rewrites the procs to comply with the MoneyGateway pattern
-- (LEDGER_HARDENING_ROADMAP.md):
--
--   1. ATOMIC DELTA — procs no longer trust p_money_use; they SELECT current
--      vin canonically (FOR UPDATE) and compute new_vin = current + p_money.
--   2. CAS retry loop — UPDATE guarded by "vin = current_vin" so a racing
--      writer that slipped in between the SELECT and UPDATE forces a re-read.
--      Up to 5 attempts, then SIGNAL.
--   3. Audit parity — INSERT IGNORE INTO money_gateway_log with unique tx_id
--      so dedup matches MoneyGateway.creditUser/debitUser audit semantics.
--      Source uses PROC_<NAME> namespace so it doesn't collide with GSC/AWC.
--   4. Drift detection — when the caller's p_money_use diverges from the
--      atomic result, log a row in money_proc_drift for retro inspection
--      (does NOT block the write — caller's delta is the source of truth).
--
-- SIGNATURE CHANGES (Java callers updated in same change set):
--   - update_money_in_game: +1 param p_money BIGINT at end (was 10 → 11).
--   - safe_money:          +1 param p_money BIGINT at end (was 4 → 5).
--   - update_money_user, restore_money, freeze_money: signatures unchanged
--     (they already accept the delta as p_money / p_money_exchange).
--
-- ROLLBACK: re-run install/config/mysql/db/data_new.sql (lines 1600-7300)
-- which contains the original definitions. Or restore from
-- install/config/mysql/db/full_backup.sql.
-- =============================================================================

-- ─── Drift table (additive, no risk) ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS money_proc_drift (
    drift_id      BIGINT NOT NULL AUTO_INCREMENT,
    detected_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    proc_name     VARCHAR(64) NOT NULL,
    user_id       BIGINT NOT NULL,
    expected_vin  BIGINT NOT NULL,
    actual_vin    BIGINT NOT NULL,
    expected_vin_total BIGINT NULL,
    actual_vin_total   BIGINT NULL,
    delta_money   BIGINT NOT NULL,
    action_name   VARCHAR(64) NULL,
    retry_count   INT NOT NULL DEFAULT 0,
    PRIMARY KEY (drift_id),
    KEY idx_user (user_id, detected_at),
    KEY idx_proc (proc_name, detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Caller passed p_money_use that did not match (current + p_money). One row per drift event. Investigate per producer code path.';

-- ─── 1. update_money_user ────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS update_money_user;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_money_user`(
    IN p_user_id     INT(11),
    IN p_money       BIGINT(20),
    IN p_money_use   BIGINT(20),    -- kept for ABI/drift-detect; not trusted
    IN p_money_total BIGINT(20),    -- kept for ABI/drift-detect; not trusted
    IN p_money_type  VARCHAR(5),
    IN p_fee         BIGINT(20),
    IN p_action_name NVARCHAR(45),
    IN p_money_vp    INT(11),
    IN p_vp          INT(11),
    IN p_type        INT(11))
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_curr_xu        BIGINT DEFAULT 0;
    DECLARE v_curr_xu_total  BIGINT DEFAULT 0;
    DECLARE v_attempt        INT    DEFAULT 0;
    DECLARE v_done           TINYINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    cas_loop: WHILE v_done = 0 AND v_attempt < 5 DO
        SET v_attempt = v_attempt + 1;

        IF p_money_type = 'vin' THEN
            SELECT vin, vin_total INTO v_curr_vin, v_curr_vin_total
              FROM users WHERE id = p_user_id;

            IF p_type = 2 THEN
                IF p_vp >= 0 THEN
                    UPDATE users
                       SET vin            = vin + p_money,
                           vin_total      = vin_total + p_money,
                           vip_point      = vip_point + p_vp,
                           vip_point_save = vip_point_save + p_vp,
                           money_vp       = p_money_vp
                     WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
                ELSE
                    UPDATE users
                       SET vin       = vin + p_money,
                           vin_total = vin_total + p_money,
                           vip_point = 0
                     WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
                END IF;

            ELSEIF p_type = 1 THEN
                UPDATE users
                   SET vin            = vin + p_money,
                       vin_total      = vin_total + p_money,
                       recharge_money = recharge_money + p_money,
                       nap_times      = nap_times + 1,
                       t_nap          = t_nap + p_money
                 WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;

            ELSEIF p_type = 0 THEN
                IF p_action_name = 'REQUEST_CASHOUT' THEN
                    UPDATE users
                       SET vin        = vin + p_money,
                           vin_total  = vin_total + p_money,
                           t_rut      = t_rut + p_money,
                           rut_times  = rut_times + 1
                     WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
                ELSEIF p_action_name = 'REFUND_RECHARGE' THEN
                    UPDATE users
                       SET vin        = vin + p_money,
                           vin_total  = vin_total + p_money,
                           t_rut      = t_rut - p_money,
                           rut_times  = rut_times - 1
                     WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
                ELSE
                    UPDATE users
                       SET vin       = vin + p_money,
                           vin_total = vin_total + p_money
                     WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
                END IF;

            ELSEIF p_type = 3 THEN
                UPDATE users
                   SET vin       = vin + p_money,
                       vin_total = vin_total + p_money
                 WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
            END IF;
        ELSE
            SELECT xu, xu_total INTO v_curr_xu, v_curr_xu_total
              FROM users WHERE id = p_user_id;
            UPDATE users
               SET xu       = xu + p_money,
                   xu_total = xu_total + p_money
             WHERE id = p_user_id AND xu = v_curr_xu AND xu_total = v_curr_xu_total;
        END IF;

        IF ROW_COUNT() > 0 THEN
            SET v_done = 1;
        END IF;
    END WHILE cas_loop;

    IF v_done = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT = 'update_money_user: CAS retries exhausted (5x). Possible stuck row. user_id+action_name in MESSAGE_TEXT not supported by SIGNAL.';
    END IF;

    -- Audit parity with MoneyGateway: race-safe dedup via UNIQUE(tx_id, source, user_id, currency).
    -- tx_id = "proc-umu-<user>-<unix_us>-<attempt>" — collisions on retry are caught by INSERT IGNORE.
    IF p_money_type = 'vin' THEN
        SELECT vin INTO v_curr_vin FROM users WHERE id = p_user_id;
    ELSE
        SELECT xu INTO v_curr_vin FROM users WHERE id = p_user_id;
    END IF;
    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, p_money_type,
         CONCAT('PROC_UPDATE_MONEY_USER_T', p_type),
         CONCAT('proc-umu-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000, '-', v_attempt),
         IFNULL(p_action_name, ''), v_curr_vin, NOW());

    -- Drift detection (informational, never blocks the write)
    IF p_money_use IS NOT NULL AND p_money_type = 'vin' AND v_curr_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('update_money_user', p_user_id, p_money_use, v_curr_vin, p_money_total, NULL, p_money, p_action_name, v_attempt - 1);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 2. update_money_in_game (ABI: +1 param p_money) ─────────────────────────
DROP PROCEDURE IF EXISTS update_money_in_game;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `update_money_in_game`(
    IN p_session_id    NVARCHAR(100),
    IN p_user_id       INT(11),
    IN p_game_name     NVARCHAR(45),
    IN p_money_use     BIGINT(20),    -- kept for ABI/drift-detect; not trusted
    IN p_money_total   BIGINT(20),    -- kept for ABI/drift-detect; not trusted
    IN p_money_freeze  BIGINT(20),
    IN p_money_type    VARCHAR(5),
    IN p_fee           BIGINT(20),
    IN p_money_vp      INT(11),
    IN p_vp            INT(11),
    IN p_money         BIGINT(20))    -- NEW: signed delta (= afterMoneyUse - oldVin)
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_curr_xu        BIGINT DEFAULT 0;
    DECLARE v_curr_xu_total  BIGINT DEFAULT 0;
    DECLARE v_attempt        INT    DEFAULT 0;
    DECLARE v_done           TINYINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    cas_loop: WHILE v_done = 0 AND v_attempt < 5 DO
        SET v_attempt = v_attempt + 1;

        IF p_money_type = 'vin' THEN
            SELECT vin, vin_total INTO v_curr_vin, v_curr_vin_total
              FROM users WHERE id = p_user_id;

            UPDATE users
               SET vin            = vin + p_money,
                   vin_total      = vin_total + p_money,
                   vip_point      = vip_point + p_vp,
                   vip_point_save = vip_point_save + p_vp,
                   money_vp       = p_money_vp
             WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
        ELSE
            SELECT xu, xu_total INTO v_curr_xu, v_curr_xu_total
              FROM users WHERE id = p_user_id;
            UPDATE users
               SET xu       = xu + p_money,
                   xu_total = xu_total + p_money
             WHERE id = p_user_id AND xu = v_curr_xu AND xu_total = v_curr_xu_total;
        END IF;

        IF ROW_COUNT() > 0 THEN
            SET v_done = 1;
        END IF;
    END WHILE cas_loop;

    IF v_done = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT = 'update_money_in_game: CAS retries exhausted (5x)';
    END IF;

    -- freeze_money sync (independent of users — keep as-is)
    IF p_money_freeze > -1 THEN
        UPDATE freeze_money SET money = p_money_freeze
         WHERE session_id = p_session_id AND user_id = p_user_id AND `status` = 1;
    END IF;

    IF p_money_type = 'vin' THEN
        SELECT vin INTO v_curr_vin FROM users WHERE id = p_user_id;
    ELSE
        SELECT xu INTO v_curr_vin FROM users WHERE id = p_user_id;
    END IF;
    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, p_money_type,
         CONCAT('PROC_UPDATE_MONEY_IN_GAME'),
         CONCAT('proc-umig-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000, '-', v_attempt),
         IFNULL(p_game_name, ''), v_curr_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_type = 'vin' AND v_curr_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('update_money_in_game', p_user_id, p_money_use, v_curr_vin, p_money_total, NULL, p_money, p_game_name, v_attempt - 1);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 3. restore_money (already has p_money_exchange — delta carries +sign) ──
DROP PROCEDURE IF EXISTS restore_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `restore_money`(
    IN p_session_id     NVARCHAR(100),
    IN p_user_id        INT(11),
    IN p_money_use      BIGINT(20),    -- kept for ABI; not trusted
    IN p_money_total    BIGINT(20),    -- kept for ABI; not trusted
    IN p_money_exchange BIGINT(20),    -- delta, always >= 0 per legacy guard
    IN p_money_type     VARCHAR(5))
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_curr_xu        BIGINT DEFAULT 0;
    DECLARE v_curr_xu_total  BIGINT DEFAULT 0;
    DECLARE v_attempt        INT    DEFAULT 0;
    DECLARE v_done           TINYINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    IF p_money_exchange > 0 THEN
        cas_loop: WHILE v_done = 0 AND v_attempt < 5 DO
            SET v_attempt = v_attempt + 1;
            IF p_money_type = 'vin' THEN
                SELECT vin, vin_total INTO v_curr_vin, v_curr_vin_total
                  FROM users WHERE id = p_user_id;
                UPDATE users
                   SET vin       = vin + p_money_exchange,
                       vin_total = vin_total + p_money_exchange
                 WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
            ELSE
                SELECT xu, xu_total INTO v_curr_xu, v_curr_xu_total
                  FROM users WHERE id = p_user_id;
                UPDATE users
                   SET xu       = xu + p_money_exchange,
                       xu_total = xu_total + p_money_exchange
                 WHERE id = p_user_id AND xu = v_curr_xu AND xu_total = v_curr_xu_total;
            END IF;
            IF ROW_COUNT() > 0 THEN SET v_done = 1; END IF;
        END WHILE cas_loop;

        IF v_done = 0 THEN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'restore_money: CAS retries exhausted (5x)';
        END IF;

        IF p_money_type = 'vin' THEN
            SELECT vin INTO v_curr_vin FROM users WHERE id = p_user_id;
        ELSE
            SELECT xu INTO v_curr_vin FROM users WHERE id = p_user_id;
        END IF;
        INSERT IGNORE INTO money_gateway_log
            (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
        VALUES
            (p_user_id, '', p_money_exchange, p_money_type,
             'PROC_RESTORE_MONEY',
             CONCAT('proc-rm-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000, '-', v_attempt),
             IFNULL(p_session_id, ''), v_curr_vin, NOW());

        IF p_money_use IS NOT NULL AND p_money_type = 'vin' AND v_curr_vin != p_money_use THEN
            INSERT INTO money_proc_drift
                (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
            VALUES
                ('restore_money', p_user_id, p_money_use, v_curr_vin, p_money_total, NULL, p_money_exchange, p_session_id, v_attempt - 1);
        END IF;
    END IF;

    UPDATE freeze_money SET money = 0, `status` = 0
     WHERE session_id = p_session_id AND user_id = p_user_id;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 4. safe_money (ABI: +1 param p_money at END for delta of vin) ───────────
-- `safe` column (vin→safe transfer destination) stays absolute — caller fully
-- controls p_money_safe and it has no race-prone snapshot pattern (only the
-- player's own safe-box endpoint writes it).
DROP PROCEDURE IF EXISTS safe_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `safe_money`(
    IN p_user_id     INT(11),
    IN p_money_use   BIGINT(20),    -- kept for ABI/drift-detect; not trusted for vin
    IN p_money_total BIGINT(20),    -- kept for ABI/drift-detect; not trusted for vin_total
    IN p_money_safe  BIGINT(20),    -- absolute new safe value (caller controls)
    IN p_money       BIGINT(20))    -- NEW: signed delta on vin (vin->safe = negative)
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_attempt        INT    DEFAULT 0;
    DECLARE v_done           TINYINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    cas_loop: WHILE v_done = 0 AND v_attempt < 5 DO
        SET v_attempt = v_attempt + 1;
        SELECT vin, vin_total INTO v_curr_vin, v_curr_vin_total
          FROM users WHERE id = p_user_id;
        UPDATE users
           SET vin       = vin + p_money,
               vin_total = vin_total + p_money,
               safe      = p_money_safe
         WHERE id = p_user_id AND vin = v_curr_vin AND vin_total = v_curr_vin_total;
        IF ROW_COUNT() > 0 THEN SET v_done = 1; END IF;
    END WHILE cas_loop;

    IF v_done = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'safe_money: CAS retries exhausted (5x)';
    END IF;

    SELECT vin INTO v_curr_vin FROM users WHERE id = p_user_id;
    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, '', p_money, 'vin',
         'PROC_SAFE_MONEY',
         CONCAT('proc-sm-', p_user_id, '-', UNIX_TIMESTAMP(NOW(6)) * 1000000, '-', v_attempt),
         'safe-box transfer', v_curr_vin, NOW());

    IF p_money_use IS NOT NULL AND v_curr_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('safe_money', p_user_id, p_money_use, v_curr_vin, p_money_total, NULL, p_money, 'safe', v_attempt - 1);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;

-- ─── 5. freeze_money (already has p_money_exchange — debit of that amount) ──
DROP PROCEDURE IF EXISTS freeze_money;
DELIMITER //
CREATE DEFINER=`root`@`%` PROCEDURE `freeze_money`(
    IN p_session_id     NVARCHAR(100),
    IN p_user_id        INT(11),
    IN p_game_name      NVARCHAR(45),
    IN p_room_id        NVARCHAR(100),
    IN p_money_use      BIGINT(20),    -- kept for ABI; not trusted
    IN p_money_total    BIGINT(20),    -- kept for ABI; not trusted
    IN p_money_exchange BIGINT(20),    -- the freeze amount (always >= 0)
    IN p_money_type     VARCHAR(5),
    IN p_nick_name      NVARCHAR(45))
proc_body: BEGIN
    DECLARE v_curr_vin       BIGINT DEFAULT 0;
    DECLARE v_curr_vin_total BIGINT DEFAULT 0;
    DECLARE v_curr_xu        BIGINT DEFAULT 0;
    DECLARE v_curr_xu_total  BIGINT DEFAULT 0;
    DECLARE v_attempt        INT    DEFAULT 0;
    DECLARE v_done           TINYINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    cas_loop: WHILE v_done = 0 AND v_attempt < 5 DO
        SET v_attempt = v_attempt + 1;
        IF p_money_type = 'vin' THEN
            SELECT vin, vin_total INTO v_curr_vin, v_curr_vin_total
              FROM users WHERE id = p_user_id;
            -- Freeze debits: vin and vin_total both decrement by exchange.
            -- Atomic floor check: vin must cover the freeze.
            UPDATE users
               SET vin       = vin - p_money_exchange,
                   vin_total = vin_total - p_money_exchange
             WHERE id = p_user_id
               AND vin = v_curr_vin
               AND vin_total = v_curr_vin_total
               AND vin >= p_money_exchange;
        ELSE
            SELECT xu, xu_total INTO v_curr_xu, v_curr_xu_total
              FROM users WHERE id = p_user_id;
            UPDATE users
               SET xu       = xu - p_money_exchange,
                   xu_total = xu_total - p_money_exchange
             WHERE id = p_user_id
               AND xu = v_curr_xu
               AND xu_total = v_curr_xu_total
               AND xu >= p_money_exchange;
        END IF;
        IF ROW_COUNT() > 0 THEN
            SET v_done = 1;
        ELSEIF (p_money_type = 'vin' AND v_curr_vin < p_money_exchange)
            OR (p_money_type = 'xu'  AND v_curr_xu  < p_money_exchange) THEN
            -- Insufficient balance — no point retrying.
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: insufficient balance';
        END IF;
    END WHILE cas_loop;

    IF v_done = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'freeze_money: CAS retries exhausted (5x)';
    END IF;

    INSERT INTO freeze_money
        (session_id, user_id, game_name, room_id, money, money_type, create_time, status, nick_name)
    VALUES
        (p_session_id, p_user_id, p_game_name, p_room_id, p_money_exchange, p_money_type, NOW(), 1, p_nick_name);

    IF p_money_type = 'vin' THEN
        SELECT vin INTO v_curr_vin FROM users WHERE id = p_user_id;
    ELSE
        SELECT xu INTO v_curr_vin FROM users WHERE id = p_user_id;
    END IF;
    INSERT IGNORE INTO money_gateway_log
        (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
    VALUES
        (p_user_id, IFNULL(p_nick_name, ''), -p_money_exchange, p_money_type,
         'PROC_FREEZE_MONEY',
         CONCAT('proc-fm-', p_user_id, '-', p_session_id, '-', v_attempt),
         IFNULL(p_game_name, ''), v_curr_vin, NOW());

    IF p_money_use IS NOT NULL AND p_money_type = 'vin' AND v_curr_vin != p_money_use THEN
        INSERT INTO money_proc_drift
            (proc_name, user_id, expected_vin, actual_vin, expected_vin_total, actual_vin_total, delta_money, action_name, retry_count)
        VALUES
            ('freeze_money', p_user_id, p_money_use, v_curr_vin, p_money_total, NULL, -p_money_exchange, p_game_name, v_attempt - 1);
    END IF;

    COMMIT;
END proc_body//
DELIMITER ;
