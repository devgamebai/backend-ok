-- =============================================================================
-- Sunwinkr Money Ledger Phase 0 — Backfill Initial Balances
--
-- This migration resolves two issues found during spec review of Task #176:
--
-- ISSUE A: v_money_account_drift returns 16,762 rows (spec requires 0).
--   Root cause: money_account rows were seeded with non-zero balances, but no
--   corresponding money_entry rows were created, so balance != SUM(entries).
--   Fix: post a BACKFILL_INITIAL_BALANCE transaction for each non-system account
--   with a positive balance, debiting a new LEGACY_RECONCILIATION system account
--   and crediting the player/agent account. This brings the ledger into balance.
--
-- ISSUE B: User 5107 (alladin2) is missing their PLAYER_VIN account.
--   Root cause: users.vin = -547,424 (legacy bad data) was rejected by the
--   CHECK constraint chk_player_nonneg. INSERT IGNORE silently skipped the row.
--   Fix: seed the PLAYER_VIN account at balance=0 and log an anomaly row.
--
-- NEW SYSTEM ACCOUNT: LEGACY_RECONCILIATION
--   Represents money already in the system before ledger tracking began.
--   Its balance will be deeply negative (~-195B VND) — this is CORRECT. It is
--   a system account so negative balances are permitted. The negative balance
--   represents the aggregate pre-existing player/agent wealth that was brought
--   into the ledger without a corresponding incoming event.
--
-- NOTE ON DIRECT TABLE WRITES:
--   This migration uses direct INSERTs into money_transaction and money_entry
--   rather than calling post_money_transaction SP. This is the ONE LEGITIMATE
--   EXCEPTION to the "only SP writes" rule, justified because:
--     1. The SP cannot handle 16,762 transactions efficiently via JSON-encoded
--        entry arrays (memory, performance).
--     2. This is a one-time DBA operation, not an application write path.
--     3. balance_before/balance_after are manually tracked correctly using
--        window functions for the running totals on LEGACY_RECONCILIATION.
--   All future application writes MUST go through post_money_transaction SP.
--   Production app users do NOT have INSERT permission on these tables — DBA
--   only for this backfill.
--
-- IMPLEMENTATION APPROACH: Hybrid cursor + set-based
--   A pure cursor over 16K rows with 3 INSERTs per row is too slow in MySQL.
--   Instead, the SP uses a tight cursor loop that does ONLY one INSERT per
--   iteration (money_transaction), captures LAST_INSERT_ID(), and stores the
--   mapping in a MEMORY temp table. Then three set-based operations handle:
--   1. Bulk INSERT CREDIT entries (player/agent accounts, balance_before=0)
--   2. Bulk INSERT DEBIT entries (LEGACY_RECONCILIATION, window function running total)
--   3. Bulk UPDATE money_account balances and last_entry_id
--
-- Idempotency:
--   - LEGACY_RECONCILIATION account: INSERT ... WHERE NOT EXISTS
--   - User 5107 PLAYER_VIN: INSERT ... WHERE NOT EXISTS
--   - money_anomaly row: INSERT ... SELECT ... WHERE NOT EXISTS
--   - Backfill SP: exits early if ANY BACKFILL_INITIAL_BALANCE tx exists
--   - Re-running after completion is safe: all checks pass, nothing is inserted.
--
-- Expected post-run state:
--   SELECT COUNT(*) FROM v_money_account_drift     → 0
--   SELECT COUNT(*) FROM v_money_negative_player   → 0
--   SELECT COUNT(*) FROM v_money_unbalanced_transactions → 0
--   LEGACY_RECONCILIATION.balance = -195,578,331,344
-- =============================================================================

-- =============================================================================
-- STEP 1: Add LEGACY_RECONCILIATION system account (11th system account).
-- Also added to 2026_05_02_money_ledger_seed_system.sql for documentation.
-- =============================================================================

INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'LEGACY_RECONCILIATION', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type = 'LEGACY_RECONCILIATION'
      AND owner_user_id IS NULL
      AND currency = 'VND'
);

-- =============================================================================
-- STEP 2: Insert PLAYER_VIN for user 5107 with balance=0 (Issue B fix).
-- users.vin = -547,424 was rejected by chk_player_nonneg; we seed at 0 and log
-- the anomaly instead. We do NOT change users.vin — that is a separate decision.
-- =============================================================================

INSERT INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_VIN', 5107, 'alladin2', 'VND', 0, 0, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE owner_user_id = 5107
      AND account_type = 'PLAYER_VIN'
      AND currency = 'VND'
);

-- =============================================================================
-- STEP 3: Log anomaly for user 5107 (PLAYER_VIN seeded at 0, legacy negative).
-- =============================================================================

INSERT INTO vinplay.money_anomaly
    (severity, invariant, account_id, drift, details, resolution_note)
SELECT
    'WARN',
    'PLAYER_VIN_NEGATIVE_LEGACY',
    (SELECT account_id FROM vinplay.money_account
     WHERE owner_user_id = 5107 AND account_type = 'PLAYER_VIN' AND currency = 'VND' LIMIT 1),
    -547424,
    JSON_OBJECT(
        'user_id',    5107,
        'nick_name',  'alladin2',
        'legacy_vin', -547424,
        'note',       'legacy users.vin had negative balance pre-ledger; account seeded at 0 with anomaly logged'
    ),
    'pre-ledger negative balance documented; balance reconciled to 0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_anomaly
    WHERE invariant = 'PLAYER_VIN_NEGATIVE_LEGACY'
      AND account_id = (
          SELECT account_id FROM vinplay.money_account
          WHERE owner_user_id = 5107 AND account_type = 'PLAYER_VIN' AND currency = 'VND' LIMIT 1
      )
);

-- =============================================================================
-- STEP 4: Backfill initial-balance entries.
--
-- The SP uses a tight cursor loop for money_transaction inserts (to capture
-- per-row LAST_INSERT_ID()), then bulk set-based INSERTs for all entries.
-- Window functions compute the running totals for LEGACY_RECONCILIATION debits.
-- =============================================================================

DROP PROCEDURE IF EXISTS vinplay.backfill_initial_balances;

DELIMITER $$

CREATE PROCEDURE vinplay.backfill_initial_balances()
backfill_proc: BEGIN
    DECLARE v_legacy_id     BIGINT;
    DECLARE v_legacy_bal    BIGINT DEFAULT 0;
    DECLARE v_done          INT DEFAULT 0;
    DECLARE v_account_id    BIGINT;
    DECLARE v_balance       BIGINT;
    DECLARE v_tx_id         BIGINT;
    DECLARE v_ts            DATETIME(6);

    -- Cursor iterates only non-system accounts with positive balance
    DECLARE c CURSOR FOR
        SELECT account_id, balance
        FROM vinplay.money_account
        WHERE is_system = 0 AND balance > 0
        ORDER BY account_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    -- Resolve LEGACY_RECONCILIATION account id
    SELECT account_id INTO v_legacy_id
    FROM vinplay.money_account
    WHERE account_type = 'LEGACY_RECONCILIATION' AND is_system = 1
    LIMIT 1;

    IF v_legacy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'LEGACY_RECONCILIATION account not found — run STEP 1 first';
    END IF;

    -- -------------------------------------------------------------------------
    -- Idempotency guard: skip if backfill was already done.
    -- -------------------------------------------------------------------------
    IF (SELECT COUNT(*) FROM vinplay.money_transaction
        WHERE transaction_type = 'BACKFILL_INITIAL_BALANCE') > 0 THEN
        SELECT 'BACKFILL_ALREADY_DONE — skipping, no changes made' AS status;
        LEAVE backfill_proc;
    END IF;

    -- -------------------------------------------------------------------------
    -- Temp table: maps account_id → transaction_id.
    -- Populated in a tight cursor loop (one tx INSERT per row, capture LAST_INSERT_ID).
    -- Entry INSERTs done later in bulk — avoids N×3-INSERT per iteration slowness.
    -- MEMORY engine for speed; 256MB session limit set before calling this SP.
    -- -------------------------------------------------------------------------
    DROP TEMPORARY TABLE IF EXISTS _backfill_map;
    CREATE TEMPORARY TABLE _backfill_map (
        rn             BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        account_id     BIGINT NOT NULL,
        bal            BIGINT NOT NULL,
        transaction_id BIGINT NOT NULL,
        KEY idx_acct (account_id)
    ) ENGINE=MEMORY;

    -- Fixed timestamp: all rows land in same partition; uk_idempotency stable.
    SET v_ts = NOW(6);

    -- -------------------------------------------------------------------------
    -- Tight loop: one money_transaction INSERT per iteration, nothing else.
    -- -------------------------------------------------------------------------
    OPEN c;
    tx_loop: LOOP
        FETCH c INTO v_account_id, v_balance;
        IF v_done THEN LEAVE tx_loop; END IF;

        INSERT INTO vinplay.money_transaction
            (transaction_type, external_ref, description, status, metadata, posted_at)
        VALUES (
            'BACKFILL_INITIAL_BALANCE',
            CONCAT('init:', v_account_id),
            'Phase 0 seed — historical balance brought into ledger',
            'POSTED',
            JSON_OBJECT('account_id', v_account_id, 'phase', 'P0_SEED'),
            v_ts
        );
        SET v_tx_id = LAST_INSERT_ID();

        INSERT INTO _backfill_map (account_id, bal, transaction_id)
        VALUES (v_account_id, v_balance, v_tx_id);

    END LOOP tx_loop;
    CLOSE c;

    -- -------------------------------------------------------------------------
    -- Bulk INSERT CREDIT entries for player/agent accounts.
    -- balance_before = 0 (no prior ledger history).
    -- balance_after  = account balance.
    -- Satisfies chk_balance_calc: CREDIT → balance_after = 0 + bal = bal.
    -- -------------------------------------------------------------------------
    INSERT INTO vinplay.money_entry
        (transaction_id, account_id, direction, amount, balance_before, balance_after, created_at)
    SELECT
        m.transaction_id,
        m.account_id,
        'CREDIT',
        m.bal,
        0,
        m.bal,
        v_ts
    FROM _backfill_map m
    ORDER BY m.rn;

    -- -------------------------------------------------------------------------
    -- Bulk INSERT DEBIT entries for LEGACY_RECONCILIATION.
    -- Running balance computed with window function SUM OVER:
    --   balance_before(row N) = -SUM(bal for rows 1..N-1)   → starts at 0
    --   balance_after(row N)  = -SUM(bal for rows 1..N)     → increasingly negative
    -- Satisfies chk_balance_calc: DEBIT → balance_after = balance_before - amount.
    -- -------------------------------------------------------------------------
    INSERT INTO vinplay.money_entry
        (transaction_id, account_id, direction, amount, balance_before, balance_after, created_at)
    SELECT
        m.transaction_id,
        v_legacy_id,
        'DEBIT',
        m.bal,
        -COALESCE(SUM(m.bal) OVER (ORDER BY m.rn ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0),
        -SUM(m.bal) OVER (ORDER BY m.rn ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW),
        v_ts
    FROM _backfill_map m
    ORDER BY m.rn;

    -- -------------------------------------------------------------------------
    -- Compute final LEGACY_RECONCILIATION running balance.
    -- -------------------------------------------------------------------------
    SELECT -SUM(bal) INTO v_legacy_bal FROM _backfill_map;

    -- -------------------------------------------------------------------------
    -- Update LEGACY_RECONCILIATION: balance and last_entry_id.
    -- -------------------------------------------------------------------------
    UPDATE vinplay.money_account
    SET
        balance       = v_legacy_bal,
        last_entry_id = (
            SELECT MAX(e.entry_id)
            FROM vinplay.money_entry e
            WHERE e.account_id = v_legacy_id
        )
    WHERE account_id = v_legacy_id;

    -- -------------------------------------------------------------------------
    -- Update last_entry_id for each credited player/agent account.
    -- Join through _backfill_map to scope only backfill CREDIT entries.
    -- -------------------------------------------------------------------------
    UPDATE vinplay.money_account a
    JOIN vinplay.money_entry e
        ON e.account_id = a.account_id
       AND e.direction  = 'CREDIT'
    JOIN _backfill_map m
        ON m.account_id     = a.account_id
       AND m.transaction_id = e.transaction_id
    SET a.last_entry_id = e.entry_id
    WHERE a.is_system = 0;

    DROP TEMPORARY TABLE IF EXISTS _backfill_map;

    SELECT CONCAT('BACKFILL_DONE — LEGACY balance = ', v_legacy_bal) AS status;

END backfill_proc$$

DELIMITER ;

CALL vinplay.backfill_initial_balances();

DROP PROCEDURE IF EXISTS vinplay.backfill_initial_balances;

-- =============================================================================
-- Post-run verification queries (uncomment to run manually after apply):
-- =============================================================================
-- SELECT COUNT(*) AS drift_rows            FROM vinplay.v_money_account_drift;
-- SELECT COUNT(*) AS negative_player_rows  FROM vinplay.v_money_negative_player;
-- SELECT COUNT(*) AS unbalanced_tx_rows    FROM vinplay.v_money_unbalanced_transactions;
-- SELECT balance  AS legacy_balance        FROM vinplay.money_account WHERE account_type='LEGACY_RECONCILIATION';
-- SELECT COUNT(*) AS user_5107_accounts    FROM vinplay.money_account WHERE owner_user_id=5107;
-- SELECT COUNT(*) AS user_5107_anomalies   FROM vinplay.money_anomaly WHERE invariant='PLAYER_VIN_NEGATIVE_LEGACY';
