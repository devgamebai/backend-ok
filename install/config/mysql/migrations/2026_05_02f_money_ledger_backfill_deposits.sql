-- =============================================================================
-- Sunwinkr Money Ledger Phase 0 — Backfill deposit_transactions (PoC)
--
-- WHAT THIS MIGRATION DOES
-- ------------------------
-- Replays every APPROVED + CREDITED row in vinplay.deposit_transactions into
-- the money_transaction / money_entry ledger as DEPOSIT_BANK (or DEPOSIT_TELEGRAM)
-- transactions. This is the proof-of-concept that any historical money source
-- can be migrated into the unified ledger.
--
-- APPROACH: Two transactions per deposit (immutable-entry design)
-- ---------------------------------------------------------------
-- For each deposit of amount D for player P:
--
--   Tx A — DEPOSIT_BANK (or DEPOSIT_TELEGRAM):
--     DEBIT  BANK_INBOX       -D   (money left the bank inbox at deposit time)
--     CREDIT PLAYER_VIN P     +D   (money arrived in player's virtual wallet)
--
--   Tx B — LEGACY_OFFSET_ADJUST (correlated to Tx A):
--     DEBIT  PLAYER_VIN P     -D   (cancel the CREDIT on the player side...)
--     CREDIT LEGACY_RECONCILIATION +D  (...and route it back to the legacy pool)
--
-- NET EFFECT on the ledger:
--   • BANK_INBOX.balance          −= total deposits (goes more negative = correct)
--   • LEGACY_RECONCILIATION.balance += total deposits (less negative = correct)
--   • player entry-sum             unchanged (CREDIT+D then DEBIT-D cancel out)
--   • money_account.balance (player) NOT updated — stays equal to seed value
--   • v_money_account_drift          still returns 0 rows (drift preserved)
--   • All 4 reconciliation invariants continue to return 0 rows
--
-- WHY NOT UPDATE PLAYER BALANCE?
-- --------------------------------
-- The seed in Task #176 set each player's money_account.balance = users.vin,
-- which already included all historical deposits. Adding another CREDIT without
-- subtracting a matching DEBIT would create drift. The LEGACY_OFFSET_ADJUST Tx B
-- is the immutable-entry mechanism that neutralises the player-side credit while
-- correctly accounting for the net flows (BANK_INBOX ↓, LEGACY_RECONCILIATION ↑).
--
-- IDEMPOTENCY
-- -----------
-- The stored procedure returns 'BACKFILL_DEPOSITS_ALREADY_DONE' and exits
-- without inserting anything if any DEPOSIT_BANK or DEPOSIT_TELEGRAM transaction
-- already exists in money_transaction.
--
-- IMPLEMENTATION NOTES (direct INSERT, not via SP)
-- ------------------------------------------------
-- Same rationale as 2026_05_02_money_ledger_backfill_initial_balances.sql:
--   1. Volume (17 deposits × 4 entries = 68 rows) is small but using the SP
--      would require JSON-encoded entry arrays per call.
--   2. This is a one-time DBA operation on historical data.
--   3. balance_before / balance_after are carefully computed using window
--      functions to keep the running totals self-consistent.
-- All future application writes MUST go through post_money_transaction SP.
--
-- EXPECTED POST-RUN STATE
-- -----------------------
--   Source count:  17 APPROVED+CREDITED rows in deposit_transactions
--   DEPOSIT_BANK tx:             17
--   LEGACY_OFFSET_ADJUST tx:     17
--   Total new money_transaction: 34
--   Total new money_entry:       68
--   BANK_INBOX.balance:          -3,020,000
--   LEGACY_RECONCILIATION.balance: -195,578,331,344 + 3,020,000 = -195,575,311,344
--   v_money_account_drift:       0 rows
--   v_money_unbalanced_transactions: 0 rows
-- =============================================================================

DROP PROCEDURE IF EXISTS vinplay.backfill_deposit_transactions;

DELIMITER $$

CREATE PROCEDURE vinplay.backfill_deposit_transactions()
backfill_dep: BEGIN

    -- -------------------------------------------------------------------------
    -- Working variables
    -- -------------------------------------------------------------------------
    DECLARE v_done              INT DEFAULT 0;
    DECLARE v_dep_id            BIGINT;
    DECLARE v_user_id           INT;
    DECLARE v_nick_name         VARCHAR(128);
    DECLARE v_amount            BIGINT;
    DECLARE v_tx_code           VARCHAR(64);
    DECLARE v_deposit_type      VARCHAR(32);
    DECLARE v_posted_at         DATETIME(6);
    DECLARE v_processed_by      VARCHAR(128);

    DECLARE v_tx_type           VARCHAR(64);
    DECLARE v_deposit_tx_id     BIGINT;
    DECLARE v_offset_tx_id      BIGINT;

    DECLARE v_legacy_id         BIGINT;
    DECLARE v_bank_inbox_id     BIGINT;
    DECLARE v_player_acc_id     BIGINT;

    -- Running balances (updated per iteration to keep balance_before/after correct)
    DECLARE v_bank_bal          BIGINT;
    DECLARE v_legacy_bal        BIGINT;
    DECLARE v_player_bal_before BIGINT;   -- player seeded balance (unchanged)
    DECLARE v_player_credit_bb  BIGINT;   -- balance_before for DEPOSIT CREDIT
    DECLARE v_player_credit_ba  BIGINT;   -- balance_after  for DEPOSIT CREDIT
    DECLARE v_player_debit_bb   BIGINT;   -- balance_before for LOA DEBIT
    DECLARE v_player_debit_ba   BIGINT;   -- balance_after  for LOA DEBIT

    DECLARE v_ts                DATETIME(6);

    -- -------------------------------------------------------------------------
    -- Cursor: one row per eligible deposit
    -- -------------------------------------------------------------------------
    DECLARE c CURSOR FOR
        SELECT
            id,
            user_id,
            nick_name,
            amount,
            tx_code,
            deposit_type,
            CAST(COALESCE(processed_at, created_at) AS DATETIME(6)),
            COALESCE(processed_by, 'SYSTEM')
        FROM vinplay.deposit_transactions
        WHERE status = 'APPROVED'
          AND credit_status = 'CREDITED'
        ORDER BY id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    -- -------------------------------------------------------------------------
    -- Idempotency guard
    -- -------------------------------------------------------------------------
    IF (SELECT COUNT(*) FROM vinplay.money_transaction
        WHERE transaction_type IN ('DEPOSIT_BANK', 'DEPOSIT_TELEGRAM')) > 0 THEN
        SELECT 'BACKFILL_DEPOSITS_ALREADY_DONE' AS status;
        LEAVE backfill_dep;
    END IF;

    -- -------------------------------------------------------------------------
    -- Resolve system account IDs
    -- -------------------------------------------------------------------------
    SELECT account_id, balance INTO v_bank_inbox_id, v_bank_bal
    FROM vinplay.money_account
    WHERE account_type = 'BANK_INBOX' AND is_system = 1
    LIMIT 1;

    IF v_bank_inbox_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'BANK_INBOX account not found';
    END IF;

    SELECT account_id, balance INTO v_legacy_id, v_legacy_bal
    FROM vinplay.money_account
    WHERE account_type = 'LEGACY_RECONCILIATION' AND is_system = 1
    LIMIT 1;

    IF v_legacy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'LEGACY_RECONCILIATION account not found';
    END IF;

    -- Fixed microsecond timestamp: all deposit entries land in same second,
    -- which is fine for historical backfill (each entry has its own processed_at).
    SET v_ts = NOW(6);

    -- -------------------------------------------------------------------------
    -- Main cursor loop
    -- -------------------------------------------------------------------------
    OPEN c;

    dep_loop: LOOP
        FETCH c INTO v_dep_id, v_user_id, v_nick_name, v_amount,
                     v_tx_code, v_deposit_type, v_posted_at, v_processed_by;
        IF v_done THEN LEAVE dep_loop; END IF;

        -- Determine transaction type from deposit_type
        SET v_tx_type = CASE v_deposit_type
            WHEN 'TELEGRAM' THEN 'DEPOSIT_TELEGRAM'
            ELSE 'DEPOSIT_BANK'
        END;

        -- Find player PLAYER_VIN account; skip if missing
        SET v_player_acc_id = NULL;
        SELECT account_id, balance INTO v_player_acc_id, v_player_bal_before
        FROM vinplay.money_account
        WHERE owner_user_id = v_user_id
          AND account_type  = 'PLAYER_VIN'
        LIMIT 1;

        IF v_player_acc_id IS NULL THEN
            -- Log anomaly and skip this deposit
            INSERT INTO vinplay.money_anomaly
                (severity, invariant, account_id, transaction_id, drift, details)
            VALUES (
                'WARN',
                'DEPOSIT_BACKFILL_NO_PLAYER_VIN',
                NULL,
                NULL,
                v_amount,
                JSON_OBJECT(
                    'deposit_id',    v_dep_id,
                    'user_id',       v_user_id,
                    'nick_name',     v_nick_name,
                    'tx_code',       v_tx_code,
                    'reason',        'No PLAYER_VIN account found; deposit skipped in backfill'
                )
            );
            ITERATE dep_loop;
        END IF;

        -- -----------------------------------------------------------------
        -- Compute balance_before / balance_after for each entry.
        --
        -- BANK_INBOX: running real balance — updated after each deposit.
        --   DEBIT entry:  bb = v_bank_bal, ba = v_bank_bal - v_amount
        --
        -- PLAYER_VIN: we do NOT update money_account.balance for the player.
        --   The net of CREDIT (Tx A) + DEBIT (Tx B) = 0.
        --   For Tx A CREDIT:  bb = v_player_bal_before, ba = v_player_bal_before + v_amount
        --   For Tx B DEBIT:   bb = v_player_bal_before + v_amount, ba = v_player_bal_before
        --   These satisfy chk_balance_calc and the entry sums net to 0 for the player.
        --
        -- LEGACY_RECONCILIATION: running real balance — updated after each deposit.
        --   CREDIT entry: bb = v_legacy_bal, ba = v_legacy_bal + v_amount
        -- -----------------------------------------------------------------

        SET v_player_credit_bb = v_player_bal_before;
        SET v_player_credit_ba = v_player_bal_before + v_amount;
        SET v_player_debit_bb  = v_player_bal_before + v_amount;
        SET v_player_debit_ba  = v_player_bal_before;

        -- =================================================================
        -- Tx A: DEPOSIT_BANK / DEPOSIT_TELEGRAM
        -- =================================================================
        INSERT INTO vinplay.money_transaction
            (transaction_type, external_ref,
             initiator_user_id, initiator_label,
             description, metadata, status, posted_at)
        VALUES (
            v_tx_type,
            v_tx_code,
            v_user_id,
            v_processed_by,
            CONCAT('Backfill: deposit ', v_tx_code),
            JSON_OBJECT(
                'deposit_id',    v_dep_id,
                'deposit_type',  v_deposit_type,
                'phase',         'P0_DEPOSIT_BACKFILL',
                'user_id',       v_user_id,
                'nick_name',     v_nick_name,
                'processed_by',  v_processed_by,
                'processed_at',  CAST(v_posted_at AS CHAR)
            ),
            'POSTED',
            v_posted_at
        );
        SET v_deposit_tx_id = LAST_INSERT_ID();

        -- Entry 1: DEBIT BANK_INBOX
        INSERT INTO vinplay.money_entry
            (transaction_id, account_id, direction, amount,
             balance_before, balance_after, note, created_at)
        VALUES (
            v_deposit_tx_id, v_bank_inbox_id, 'DEBIT', v_amount,
            v_bank_bal, v_bank_bal - v_amount,
            'Bank inbox debit — deposit backfill',
            v_posted_at
        );

        -- Entry 2: CREDIT player PLAYER_VIN
        INSERT INTO vinplay.money_entry
            (transaction_id, account_id, direction, amount,
             balance_before, balance_after, note, created_at)
        VALUES (
            v_deposit_tx_id, v_player_acc_id, 'CREDIT', v_amount,
            v_player_credit_bb, v_player_credit_ba,
            'Player credit — deposit backfill',
            v_posted_at
        );

        -- Update BANK_INBOX running balance (real balance update)
        SET v_bank_bal = v_bank_bal - v_amount;
        UPDATE vinplay.money_account
        SET balance       = v_bank_bal,
            last_entry_id = LAST_INSERT_ID()
        WHERE account_id = v_bank_inbox_id;

        -- =================================================================
        -- Tx B: LEGACY_OFFSET_ADJUST (correlated to Tx A)
        -- Cancels the player-side credit so player.entry_sum stays = seed balance.
        -- Routes the net flow to LEGACY_RECONCILIATION (less negative).
        -- =================================================================
        INSERT INTO vinplay.money_transaction
            (transaction_type, external_ref,
             initiator_user_id, initiator_label,
             description,
             correlated_transaction_id,
             metadata, status, posted_at)
        VALUES (
            'LEGACY_OFFSET_ADJUST',
            CONCAT('loa:', v_tx_code),
            v_user_id,
            'SYSTEM',
            CONCAT('Legacy offset for deposit backfill ', v_tx_code),
            v_deposit_tx_id,
            JSON_OBJECT(
                'deposit_id',       v_dep_id,
                'deposit_tx_id',    v_deposit_tx_id,
                'phase',            'P0_DEPOSIT_BACKFILL',
                'note', 'Cancels player CREDIT from deposit Tx; routes net to LEGACY_RECONCILIATION'
            ),
            'POSTED',
            v_posted_at
        );
        SET v_offset_tx_id = LAST_INSERT_ID();

        -- Entry 3: DEBIT player PLAYER_VIN (cancels the Tx A credit)
        INSERT INTO vinplay.money_entry
            (transaction_id, account_id, direction, amount,
             balance_before, balance_after, note, created_at)
        VALUES (
            v_offset_tx_id, v_player_acc_id, 'DEBIT', v_amount,
            v_player_debit_bb, v_player_debit_ba,
            'Legacy offset: undo player credit (seed already includes deposit)',
            v_posted_at
        );

        -- Entry 4: CREDIT LEGACY_RECONCILIATION
        INSERT INTO vinplay.money_entry
            (transaction_id, account_id, direction, amount,
             balance_before, balance_after, note, created_at)
        VALUES (
            v_offset_tx_id, v_legacy_id, 'CREDIT', v_amount,
            v_legacy_bal, v_legacy_bal + v_amount,
            'Legacy offset: route deposit net to LEGACY_RECONCILIATION',
            v_posted_at
        );

        -- Update LEGACY_RECONCILIATION running balance (real balance update)
        SET v_legacy_bal = v_legacy_bal + v_amount;
        UPDATE vinplay.money_account
        SET balance       = v_legacy_bal,
            last_entry_id = LAST_INSERT_ID()
        WHERE account_id = v_legacy_id;

        -- Note: player money_account.balance is NOT updated.
        -- The net entry contribution to player account is +D-D = 0.
        -- The seeded balance already captures all historical deposits.
        -- Updating last_entry_id for player (points to most recent DEBIT entry, Tx B)
        UPDATE vinplay.money_account
        SET last_entry_id = (
            SELECT entry_id FROM vinplay.money_entry
            WHERE transaction_id = v_offset_tx_id
              AND account_id     = v_player_acc_id
              AND direction      = 'DEBIT'
            LIMIT 1
        )
        WHERE account_id = v_player_acc_id;

    END LOOP dep_loop;
    CLOSE c;

    -- -------------------------------------------------------------------------
    -- Final status report
    -- -------------------------------------------------------------------------
    SELECT
        CONCAT(
            'BACKFILL_DEPOSITS_DONE — ',
            (SELECT COUNT(*) FROM vinplay.money_transaction
             WHERE transaction_type IN ('DEPOSIT_BANK','DEPOSIT_TELEGRAM')),
            ' deposit tx, ',
            (SELECT COUNT(*) FROM vinplay.money_transaction
             WHERE transaction_type = 'LEGACY_OFFSET_ADJUST'),
            ' offset tx. BANK_INBOX.balance=',
            (SELECT balance FROM vinplay.money_account WHERE account_type='BANK_INBOX' AND is_system=1 LIMIT 1),
            ' LEGACY_RECONCILIATION.balance=',
            (SELECT balance FROM vinplay.money_account WHERE account_type='LEGACY_RECONCILIATION' AND is_system=1 LIMIT 1)
        ) AS status;

END backfill_dep$$

DELIMITER ;

CALL vinplay.backfill_deposit_transactions();

DROP PROCEDURE IF EXISTS vinplay.backfill_deposit_transactions;

-- =============================================================================
-- Post-run verification queries (uncomment to run manually after apply):
-- =============================================================================
-- SELECT COUNT(*) AS drift_rows            FROM vinplay.v_money_account_drift;
-- SELECT COUNT(*) AS unbalanced_tx_rows    FROM vinplay.v_money_unbalanced_transactions;
-- SELECT COUNT(*) AS negative_player_rows  FROM vinplay.v_money_negative_player;
-- SELECT COUNT(*) AS orphan_reversed_rows  FROM vinplay.v_money_orphan_reversed;
-- SELECT transaction_type, COUNT(*) AS cnt FROM vinplay.money_transaction GROUP BY transaction_type;
-- SELECT account_type, balance FROM vinplay.money_account WHERE account_type IN ('BANK_INBOX','LEGACY_RECONCILIATION');
