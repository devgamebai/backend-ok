-- =============================================================================
-- Money Ledger — Atomic idempotency via money_idempotency table (#181)
--
-- PROBLEM:
--   The original SP used a pre-flight SELECT to detect duplicates, then started
--   a transaction and INSERTed. Two concurrent calls with the same
--   (transaction_type, external_ref) could both pass the pre-flight check (both
--   see NULL) and both succeed at INSERT because uk_idempotency on
--   money_transaction is a 3-column key (type, ref, posted_at). Two calls
--   arriving within the same microsecond share posted_at, but calls arriving
--   microseconds apart get different posted_at values → both inserts succeed
--   → DOUBLE-POST.
--
-- FIX:
--   Add money_idempotency with a true 2-column PRIMARY KEY (transaction_type,
--   external_ref). Inside the SP transaction, INSERT into money_idempotency
--   AFTER inserting money_transaction. MySQL enforces the PK uniqueness at
--   INSERT time with a lock (gap lock on the PK), so concurrent calls are
--   serialized: only one succeeds; the other gets errno 1062 (SQLSTATE 23000),
--   which the EXIT HANDLER catches → ROLLBACK → return DUPLICATE.
--
-- WHY SINGLE HANDLER (not nested):
--   MySQL stored procedures do not support nested DECLARE HANDLER blocks for
--   the same condition scope. Instead we use one SQLEXCEPTION handler that reads
--   GET DIAGNOSTICS to distinguish errno 1062 (dup key) from all other errors.
--   This keeps the handler structure flat and easy to audit.
--
-- Idempotency: CREATE TABLE IF NOT EXISTS + DROP/CREATE for the SP.
--              Backfill uses INSERT IGNORE (safe to re-run).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: money_idempotency — 2-column dedup guard (non-partitioned)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vinplay.money_idempotency (
    transaction_type  VARCHAR(64)  NOT NULL,
    external_ref      VARCHAR(160) NOT NULL,
    transaction_id    BIGINT       NOT NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (transaction_type, external_ref),
    KEY idx_tx (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='2-column PK prevents concurrent duplicate posts. Written inside the SP transaction before COMMIT.';

-- -----------------------------------------------------------------------------
-- Backfill: every existing money_transaction gets an idempotency row.
-- INSERT IGNORE skips conflicts → idempotent.
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO vinplay.money_idempotency (transaction_type, external_ref, transaction_id, created_at)
SELECT transaction_type, external_ref, transaction_id, posted_at
  FROM vinplay.money_transaction;

-- -----------------------------------------------------------------------------
-- Stored procedure: post_money_transaction (updated — atomic idempotency)
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS vinplay.post_money_transaction;

DELIMITER $$

CREATE PROCEDURE vinplay.post_money_transaction(
    IN  p_transaction_type    VARCHAR(64),
    IN  p_external_ref        VARCHAR(160),
    IN  p_initiator_user_id   INT,
    IN  p_initiator_label     VARCHAR(128),
    IN  p_description         VARCHAR(500),
    IN  p_correlated_tx_id    BIGINT,
    IN  p_metadata            JSON,
    IN  p_entries_json        JSON,         -- [{"account_id":1,"direction":"DEBIT","amount":N,"note":"..."}]
    OUT p_transaction_id      BIGINT,
    OUT p_status              VARCHAR(40)
)
proc_label: BEGIN
    DECLARE v_n_entries        INT;
    DECLARE v_idx              INT DEFAULT 0;
    DECLARE v_account_id       BIGINT;
    DECLARE v_direction        VARCHAR(8);
    DECLARE v_amount           BIGINT;
    DECLARE v_note             VARCHAR(255);
    DECLARE v_balance_before   BIGINT;
    DECLARE v_balance_after    BIGINT;
    DECLARE v_is_system        TINYINT;
    DECLARE v_is_frozen        TINYINT;
    DECLARE v_sum              BIGINT DEFAULT 0;
    DECLARE v_sqlstate         CHAR(5);
    DECLARE v_errno            INT;

    -- Single EXIT handler for all SQL exceptions.
    -- On errno 1062 (Duplicate entry) the money_idempotency PK was violated,
    -- meaning a concurrent call already committed this (type, ref) pair.
    -- We ROLLBACK (no-op if nothing was written yet), look up the winning
    -- transaction_id from money_idempotency, and return DUPLICATE.
    -- All other errors return EXCEPTION.
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1
            v_sqlstate = RETURNED_SQLSTATE,
            v_errno    = MYSQL_ERRNO;
        ROLLBACK;
        IF v_errno = 1062 THEN
            -- Concurrent call won the race; surface its transaction_id.
            SELECT transaction_id INTO p_transaction_id
              FROM vinplay.money_idempotency
             WHERE transaction_type = p_transaction_type
               AND external_ref     = p_external_ref;
            SET p_status = 'DUPLICATE';
        ELSE
            SET p_status         = 'EXCEPTION';
            SET p_transaction_id = NULL;
        END IF;
    END;

    -- NOTE: The old pre-flight SELECT idempotency check is intentionally
    -- removed. It was a TOCTOU race (check-then-act without a lock). The
    -- money_idempotency INSERT inside the transaction below is the atomic
    -- guard. Removing the pre-flight also shaves one round-trip per call.

    -- Validate at least 2 entries
    SET v_n_entries = JSON_LENGTH(p_entries_json);
    IF v_n_entries IS NULL OR v_n_entries < 2 THEN
        SET p_status = 'NEED_AT_LEAST_TWO_ENTRIES';
        SET p_transaction_id = NULL;
        LEAVE proc_label;
    END IF;

    -- Pre-flight: validate entries sum to zero (double-entry invariant)
    SET v_idx = 0;
    WHILE v_idx < v_n_entries DO
        SET v_direction = JSON_UNQUOTE(JSON_EXTRACT(p_entries_json, CONCAT('$[',v_idx,'].direction')));
        SET v_amount    = JSON_EXTRACT(p_entries_json, CONCAT('$[',v_idx,'].amount'));
        IF v_amount IS NULL OR v_amount <= 0 THEN
            SET p_status = 'AMOUNT_MUST_BE_POSITIVE';
            SET p_transaction_id = NULL;
            LEAVE proc_label;
        END IF;
        IF v_direction NOT IN ('DEBIT','CREDIT') THEN
            SET p_status = 'INVALID_DIRECTION';
            SET p_transaction_id = NULL;
            LEAVE proc_label;
        END IF;
        SET v_sum = v_sum + IF(v_direction='CREDIT', v_amount, -v_amount);
        SET v_idx = v_idx + 1;
    END WHILE;

    IF v_sum != 0 THEN
        SET p_status = 'UNBALANCED_ENTRIES';
        SET p_transaction_id = NULL;
        LEAVE proc_label;
    END IF;

    -- All validation passed. Begin atomic posting.
    START TRANSACTION;

    -- 1. Insert the transaction journal row.
    INSERT INTO vinplay.money_transaction
        (transaction_type, external_ref, initiator_user_id, initiator_label,
         description, correlated_transaction_id, metadata, status, posted_at)
    VALUES
        (p_transaction_type, p_external_ref, p_initiator_user_id, p_initiator_label,
         p_description, p_correlated_tx_id, p_metadata, 'POSTED', NOW(6));
    SET p_transaction_id = LAST_INSERT_ID();

    -- 2. Claim idempotency slot.  If another session already inserted this
    --    (type, ref) pair, this INSERT fails with errno 1062 and the EXIT
    --    HANDLER fires: ROLLBACK + return DUPLICATE.
    INSERT INTO vinplay.money_idempotency (transaction_type, external_ref, transaction_id)
    VALUES (p_transaction_type, p_external_ref, p_transaction_id);

    -- 3. Apply each entry: lock account, check balance, update, insert entry.
    SET v_idx = 0;
    apply_loop: WHILE v_idx < v_n_entries DO
        SET v_account_id = JSON_EXTRACT(p_entries_json, CONCAT('$[',v_idx,'].account_id'));
        SET v_direction  = JSON_UNQUOTE(JSON_EXTRACT(p_entries_json, CONCAT('$[',v_idx,'].direction')));
        SET v_amount     = JSON_EXTRACT(p_entries_json, CONCAT('$[',v_idx,'].amount'));
        SET v_note       = JSON_UNQUOTE(JSON_EXTRACT(p_entries_json, CONCAT('$[',v_idx,'].note')));
        IF v_note = 'null' THEN SET v_note = NULL; END IF;

        -- Lock the account row (InnoDB row lock; serializes concurrent writers)
        SELECT balance, is_system, is_frozen
          INTO v_balance_before, v_is_system, v_is_frozen
          FROM vinplay.money_account
         WHERE account_id = v_account_id
         FOR UPDATE;

        IF v_balance_before IS NULL THEN
            SET p_status = 'ACCOUNT_NOT_FOUND';
            SET p_transaction_id = NULL;
            ROLLBACK;
            LEAVE proc_label;
        END IF;

        IF v_is_frozen = 1 THEN
            SET p_status = 'ACCOUNT_FROZEN';
            SET p_transaction_id = NULL;
            ROLLBACK;
            LEAVE proc_label;
        END IF;

        IF v_direction = 'DEBIT' THEN
            SET v_balance_after = v_balance_before - v_amount;
            IF v_balance_after < 0 AND v_is_system = 0 THEN
                SET p_status = 'INSUFFICIENT_BALANCE';
                SET p_transaction_id = NULL;
                ROLLBACK;
                LEAVE proc_label;
            END IF;
        ELSE
            SET v_balance_after = v_balance_before + v_amount;
        END IF;

        UPDATE vinplay.money_account
           SET balance = v_balance_after
         WHERE account_id = v_account_id;

        INSERT INTO vinplay.money_entry
            (transaction_id, account_id, direction, amount,
             balance_before, balance_after, note, created_at)
        VALUES
            (p_transaction_id, v_account_id, v_direction, v_amount,
             v_balance_before, v_balance_after, v_note, NOW(6));

        UPDATE vinplay.money_account
           SET last_entry_id = LAST_INSERT_ID()
         WHERE account_id = v_account_id;

        SET v_idx = v_idx + 1;
    END WHILE apply_loop;

    SET p_status = 'POSTED';
    COMMIT;
END proc_label$$

DELIMITER ;
