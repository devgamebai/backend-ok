-- SUN-13xx Option B — align ledger PLAYER_VIN balance to users.vin
--
-- For every non-bot user whose users.vin ≠ ledger PLAYER_VIN balance, post
-- exactly one BACKFILL_RESET transaction with two entries (sum-to-zero):
--   - drift > 0:  ledger needs MORE → CREDIT PLAYER_VIN, DEBIT LEGACY_RECONCILIATION
--   - drift < 0:  ledger needs LESS → DEBIT  PLAYER_VIN, CREDIT LEGACY_RECONCILIATION
--
-- Idempotent: external_ref includes the user id + date, so re-running on the
-- same day is a no-op (post_money_transaction returns DUPLICATE on dedup).
--
-- Bots are deliberately excluded (their game flows are house liquidity, not
-- real money — see SUN-13xx bot short-circuit in MoneyGateway).
USE vinplay;

DROP PROCEDURE IF EXISTS do_baseline_reset_v1;

DELIMITER //
CREATE PROCEDURE do_baseline_reset_v1(IN p_dry_run TINYINT, IN p_run_date VARCHAR(10))
proc_end: BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_user_id BIGINT;
    DECLARE v_nickname VARCHAR(128);
    DECLARE v_users_vin BIGINT;
    DECLARE v_ledger_bal BIGINT;
    DECLARE v_drift BIGINT;
    DECLARE v_player_acc BIGINT;
    DECLARE v_legacy_acc BIGINT;
    DECLARE v_entries JSON;
    DECLARE v_external_ref VARCHAR(160);
    DECLARE v_tx_id BIGINT;
    DECLARE v_status VARCHAR(40);
    DECLARE v_processed INT DEFAULT 0;
    DECLARE v_skipped   INT DEFAULT 0;
    DECLARE v_dup       INT DEFAULT 0;
    DECLARE v_failed    INT DEFAULT 0;

    DECLARE cur CURSOR FOR
        SELECT u.id, u.nick_name, u.vin, COALESCE(ma.balance, 0)
        FROM users u
        JOIN money_account ma
          ON ma.owner_user_id = u.id
         AND ma.account_type  = 'PLAYER_VIN'
         AND ma.currency      = 'VND'
        WHERE u.is_bot = 0
          AND u.vin <> COALESCE(ma.balance, 0)
        ORDER BY u.id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SELECT account_id INTO v_legacy_acc
    FROM money_account
    WHERE account_type = 'LEGACY_RECONCILIATION'
      AND is_system    = 1
      AND currency     = 'VND'
    LIMIT 1;

    IF v_legacy_acc IS NULL THEN
        SELECT 'ERROR: LEGACY_RECONCILIATION account missing' AS msg;
        LEAVE proc_end;
    END IF;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_user_id, v_nickname, v_users_vin, v_ledger_bal;
        IF v_done = 1 THEN LEAVE read_loop; END IF;

        SET v_drift = v_users_vin - v_ledger_bal;

        SELECT account_id INTO v_player_acc
        FROM money_account
        WHERE owner_user_id = v_user_id
          AND account_type  = 'PLAYER_VIN'
          AND currency      = 'VND'
        LIMIT 1;

        IF v_player_acc IS NULL THEN
            SET v_skipped = v_skipped + 1;
            ITERATE read_loop;
        END IF;

        IF v_drift > 0 THEN
            -- Ledger short → credit player, debit legacy
            SET v_entries = JSON_ARRAY(
                JSON_OBJECT('account_id', v_legacy_acc, 'direction', 'DEBIT',  'amount', v_drift, 'note', 'baseline-reset-debit'),
                JSON_OBJECT('account_id', v_player_acc, 'direction', 'CREDIT', 'amount', v_drift, 'note', 'baseline-reset-credit')
            );
        ELSE
            -- Ledger over → debit player, credit legacy
            SET v_entries = JSON_ARRAY(
                JSON_OBJECT('account_id', v_player_acc, 'direction', 'DEBIT',  'amount', -v_drift, 'note', 'baseline-reset-debit'),
                JSON_OBJECT('account_id', v_legacy_acc, 'direction', 'CREDIT', 'amount', -v_drift, 'note', 'baseline-reset-credit')
            );
        END IF;

        SET v_external_ref = CONCAT('baseline_reset_', p_run_date, ':', v_user_id);

        IF p_dry_run = 1 THEN
            INSERT INTO wallet_baseline_reset_log
                (user_id, nickname, users_vin, ledger_balance_before, drift_amount, external_ref, dry_run, ran_at)
            VALUES
                (v_user_id, v_nickname, v_users_vin, v_ledger_bal, v_drift, v_external_ref, 1, NOW(6));
            SET v_processed = v_processed + 1;
        ELSE
            CALL post_money_transaction(
                'BACKFILL_RESET',
                v_external_ref,
                NULL,
                'ops:baseline-reset',
                CONCAT('Option B baseline reset on ', p_run_date, ' for ', v_nickname),
                NULL,
                JSON_OBJECT('reason', 'wallet-unification-baseline-reset', 'run_date', p_run_date),
                v_entries,
                v_tx_id,
                v_status
            );

            IF v_status = 'POSTED' THEN
                SET v_processed = v_processed + 1;
            ELSEIF v_status = 'DUPLICATE' THEN
                SET v_dup = v_dup + 1;
            ELSE
                SET v_failed = v_failed + 1;
            END IF;

            INSERT INTO wallet_baseline_reset_log
                (user_id, nickname, users_vin, ledger_balance_before, drift_amount, external_ref, dry_run, ledger_tx_id, status, ran_at)
            VALUES
                (v_user_id, v_nickname, v_users_vin, v_ledger_bal, v_drift, v_external_ref, 0, v_tx_id, v_status, NOW(6));
        END IF;
    END LOOP;
    CLOSE cur;

    SELECT v_processed AS processed, v_skipped AS skipped, v_dup AS duplicates, v_failed AS failed;
END//
DELIMITER ;

CREATE TABLE IF NOT EXISTS wallet_baseline_reset_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(128) NOT NULL,
    users_vin BIGINT NOT NULL,
    ledger_balance_before BIGINT NOT NULL,
    drift_amount BIGINT NOT NULL,
    external_ref VARCHAR(160) NOT NULL,
    dry_run TINYINT(1) NOT NULL,
    ledger_tx_id BIGINT,
    status VARCHAR(40),
    ran_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_ran_at (ran_at)
) ENGINE=InnoDB CHARSET=utf8mb4;
