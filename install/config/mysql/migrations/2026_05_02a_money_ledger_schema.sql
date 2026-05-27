-- =============================================================================
-- Sunwinkr Money Ledger — transaction-centered, double-entry schema
-- See docs/MONEY_LEDGER_SCHEMA.md for full design rationale.
--
-- This migration creates the three core tables plus the post_money_transaction
-- stored procedure. It is ADDITIVE — it does not touch the existing users.vin,
-- money_gateway_log, log_money_user_vin, rebate_logs, etc. Those continue to
-- function until Phase 1 of the migration plan switches writes over.
--
-- Run order:
--   1. This file (creates schema + SP).
--   2. 2026_05_02_money_ledger_seed_accounts.sql (seeds system accounts +
--      player/agent accounts from existing users / agency_wallet / credit_wallet).
--   3. 2026_05_02_money_ledger_backfill.sql (one-time: replays history into
--      the ledger from existing money sources).
--
-- Idempotency: this file uses CREATE TABLE IF NOT EXISTS, so safe to re-run
-- on a partially-applied database. The SP uses CREATE OR REPLACE syntax via
-- DROP IF EXISTS guard.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: money_account — every wallet/pool in the system
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vinplay.money_account (
    account_id        BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_type      VARCHAR(40) NOT NULL,
    owner_user_id     INT NULL,
    owner_nickname    VARCHAR(128) NULL,
    currency          VARCHAR(8) NOT NULL DEFAULT 'VND',
    balance           BIGINT NOT NULL DEFAULT 0,
    last_entry_id     BIGINT NULL,
    is_system         TINYINT(1) NOT NULL DEFAULT 0,
    is_frozen         TINYINT(1) NOT NULL DEFAULT 0,
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_owner_type (owner_user_id, account_type, currency),
    KEY idx_type            (account_type, is_system),
    KEY idx_nickname        (owner_nickname),
    CONSTRAINT chk_player_nonneg CHECK (is_system = 1 OR balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Every wallet or pool in the financial system. Balance updated atomically via post_money_transaction SP.';

-- -----------------------------------------------------------------------------
-- Table: money_transaction — the journal entry (the "why")
-- Partitioned by month for fast archival.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vinplay.money_transaction (
    transaction_id            BIGINT NOT NULL AUTO_INCREMENT,
    transaction_type          VARCHAR(64) NOT NULL,
    external_ref              VARCHAR(160) NOT NULL,
    initiator_user_id         INT NULL,
    initiator_label           VARCHAR(128) NULL,
    description               VARCHAR(500) NULL,
    correlated_transaction_id BIGINT NULL,
    metadata                  JSON NULL,
    status                    ENUM('PENDING','POSTED','REVERSED') NOT NULL DEFAULT 'POSTED',
    posted_at                 DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reversed_at               DATETIME(6) NULL,
    reversal_transaction_id   BIGINT NULL,
    PRIMARY KEY (transaction_id, posted_at),
    UNIQUE KEY uk_idempotency (transaction_type, external_ref, posted_at),
    KEY idx_type_time (transaction_type, posted_at),
    KEY idx_correlated (correlated_transaction_id),
    KEY idx_initiator (initiator_user_id, posted_at),
    KEY idx_status (status, posted_at),
    KEY idx_reversal (reversal_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Journal of every business event that moves money. Entries (debits/credits) live in money_entry.'
  PARTITION BY RANGE COLUMNS(posted_at) (
    PARTITION p2026_05 VALUES LESS THAN ('2026-06-01'),
    PARTITION p2026_06 VALUES LESS THAN ('2026-07-01'),
    PARTITION p2026_07 VALUES LESS THAN ('2026-08-01'),
    PARTITION p2026_08 VALUES LESS THAN ('2026-09-01'),
    PARTITION p2026_09 VALUES LESS THAN ('2026-10-01'),
    PARTITION p2026_10 VALUES LESS THAN ('2026-11-01'),
    PARTITION p2026_11 VALUES LESS THAN ('2026-12-01'),
    PARTITION p2026_12 VALUES LESS THAN ('2027-01-01'),
    PARTITION pmax     VALUES LESS THAN MAXVALUE
  );

-- -----------------------------------------------------------------------------
-- Table: money_entry — the actual debit/credit lines (the "what")
-- Each transaction has 2+ entries; their signed sum is always 0.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vinplay.money_entry (
    entry_id        BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id  BIGINT NOT NULL,
    account_id      BIGINT NOT NULL,
    direction       ENUM('DEBIT','CREDIT') NOT NULL,
    amount          BIGINT NOT NULL,
    balance_before  BIGINT NOT NULL,
    balance_after   BIGINT NOT NULL,
    note            VARCHAR(255) NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (entry_id, created_at),
    KEY idx_tx                (transaction_id),
    KEY idx_account_time      (account_id, created_at),
    KEY idx_account_entry     (account_id, entry_id),
    -- Note: FK to partitioned tables is NOT supported in MySQL 8 InnoDB; enforced in SP.
    CONSTRAINT chk_amount_pos   CHECK (amount > 0),
    CONSTRAINT chk_balance_calc CHECK (
        (direction='DEBIT'  AND balance_after = balance_before - amount) OR
        (direction='CREDIT' AND balance_after = balance_before + amount)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Debit/credit lines. Foreign-key relation to money_account / money_transaction enforced by post_money_transaction SP.'
  PARTITION BY RANGE COLUMNS(created_at) (
    PARTITION p2026_05 VALUES LESS THAN ('2026-06-01'),
    PARTITION p2026_06 VALUES LESS THAN ('2026-07-01'),
    PARTITION p2026_07 VALUES LESS THAN ('2026-08-01'),
    PARTITION p2026_08 VALUES LESS THAN ('2026-09-01'),
    PARTITION p2026_09 VALUES LESS THAN ('2026-10-01'),
    PARTITION p2026_10 VALUES LESS THAN ('2026-11-01'),
    PARTITION p2026_11 VALUES LESS THAN ('2026-12-01'),
    PARTITION p2026_12 VALUES LESS THAN ('2027-01-01'),
    PARTITION pmax     VALUES LESS THAN MAXVALUE
  );

-- -----------------------------------------------------------------------------
-- Table: money_anomaly — reconciliation findings + invariant violations
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vinplay.money_anomaly (
    anomaly_id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    detected_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    severity        ENUM('INFO','WARN','ERROR','CRITICAL') NOT NULL,
    invariant       VARCHAR(64) NOT NULL,
        -- 'TX_BALANCE'        — sum of entries per tx != 0
        -- 'ACCOUNT_BALANCE'   — account.balance != sum of entries
        -- 'NEGATIVE_PLAYER'   — a player account went negative
        -- 'ORPHAN_ENTRY'      — entry with no matching tx
        -- 'STALE_PENDING'     — tx stuck in PENDING > N hours
        -- 'SUSPENSE_NONZERO'  — SUSPENSE account has non-zero balance at end of day
    account_id      BIGINT NULL,
    transaction_id  BIGINT NULL,
    drift           BIGINT NULL,
    details         JSON NULL,
    resolved_at     DATETIME(6) NULL,
    resolved_by     VARCHAR(128) NULL,
    resolution_note VARCHAR(500) NULL,
    KEY idx_severity_time (severity, detected_at),
    KEY idx_unresolved    (resolved_at, severity),
    KEY idx_account       (account_id),
    KEY idx_tx            (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Auto-detected integrity violations. Telegram alert on any new CRITICAL row.';

-- -----------------------------------------------------------------------------
-- Stored procedure: post_money_transaction — the ONLY way money moves
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
    DECLARE v_existing_tx_id   BIGINT DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status = 'EXCEPTION';
        SET p_transaction_id = NULL;
    END;

    -- Idempotency: if this (type, ref) was already posted, return its id and a DUPLICATE marker.
    SELECT transaction_id INTO v_existing_tx_id
      FROM vinplay.money_transaction
     WHERE transaction_type = p_transaction_type
       AND external_ref     = p_external_ref
     LIMIT 1;
    IF v_existing_tx_id IS NOT NULL THEN
        SET p_transaction_id = v_existing_tx_id;
        SET p_status = 'DUPLICATE';
        LEAVE proc_label;
    END IF;

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

    INSERT INTO vinplay.money_transaction
        (transaction_type, external_ref, initiator_user_id, initiator_label,
         description, correlated_transaction_id, metadata, status, posted_at)
    VALUES
        (p_transaction_type, p_external_ref, p_initiator_user_id, p_initiator_label,
         p_description, p_correlated_tx_id, p_metadata, 'POSTED', NOW(6));
    SET p_transaction_id = LAST_INSERT_ID();

    -- Apply each entry: lock account, check balance, update, insert entry
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

-- -----------------------------------------------------------------------------
-- Reconciliation views — invariants 1, 2, 3 (the critical ones)
-- -----------------------------------------------------------------------------

-- Invariant 1: every transaction's entries balance to zero
CREATE OR REPLACE VIEW vinplay.v_money_unbalanced_transactions AS
SELECT t.transaction_id, t.transaction_type, t.external_ref, t.posted_at,
       SUM(IF(e.direction='CREDIT', e.amount, -e.amount)) AS net
FROM vinplay.money_transaction t
JOIN vinplay.money_entry e ON e.transaction_id = t.transaction_id
WHERE t.status IN ('POSTED','REVERSED')
GROUP BY t.transaction_id, t.transaction_type, t.external_ref, t.posted_at
HAVING net != 0;

-- Invariant 2: each account's balance == sum of its entries
CREATE OR REPLACE VIEW vinplay.v_money_account_drift AS
SELECT a.account_id, a.account_type, a.owner_nickname, a.balance,
       COALESCE(SUM(IF(e.direction='CREDIT', e.amount, -e.amount)), 0) AS computed,
       a.balance - COALESCE(SUM(IF(e.direction='CREDIT', e.amount, -e.amount)), 0) AS drift
FROM vinplay.money_account a
LEFT JOIN vinplay.money_entry e ON e.account_id = a.account_id
GROUP BY a.account_id, a.account_type, a.owner_nickname, a.balance
HAVING drift != 0;

-- Invariant 3: any non-system account with negative balance
CREATE OR REPLACE VIEW vinplay.v_money_negative_player AS
SELECT account_id, account_type, owner_user_id, owner_nickname, balance, last_entry_id
FROM vinplay.money_account
WHERE is_system = 0 AND balance < 0;

-- Invariant 4: REVERSED transactions must have a reversal_transaction_id pointer
CREATE OR REPLACE VIEW vinplay.v_money_orphan_reversed AS
SELECT transaction_id, transaction_type, external_ref, status, reversed_at
FROM vinplay.money_transaction
WHERE status = 'REVERSED' AND reversal_transaction_id IS NULL;

-- Convenience: per-player running balance feed
CREATE OR REPLACE VIEW vinplay.v_player_money_history AS
SELECT a.owner_user_id, a.owner_nickname, a.account_type, a.currency,
       e.entry_id, e.direction, e.amount, e.balance_before, e.balance_after,
       t.transaction_id, t.transaction_type, t.external_ref, t.description,
       t.metadata, e.created_at
FROM vinplay.money_account a
JOIN vinplay.money_entry e       ON e.account_id = a.account_id
JOIN vinplay.money_transaction t ON t.transaction_id = e.transaction_id
WHERE a.is_system = 0;

-- -----------------------------------------------------------------------------
-- Privilege rule (apply manually, NOT via this migration script):
--
--   GRANT SELECT, EXECUTE ON vinplay.* TO 'sunwinkr_app'@'%';
--   GRANT EXECUTE ON PROCEDURE vinplay.post_money_transaction TO 'sunwinkr_app'@'%';
--   REVOKE INSERT, UPDATE, DELETE ON vinplay.money_account     FROM 'sunwinkr_app'@'%';
--   REVOKE INSERT, UPDATE, DELETE ON vinplay.money_transaction FROM 'sunwinkr_app'@'%';
--   REVOKE INSERT, UPDATE, DELETE ON vinplay.money_entry       FROM 'sunwinkr_app'@'%';
--
-- This makes the SP the only path for app-level money writes. DBA role retains
-- full access for forensic corrections (which themselves must be logged via SP
-- with type='RECONCILIATION_FIX').
-- -----------------------------------------------------------------------------
