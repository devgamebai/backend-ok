-- ============================================================================
-- WALLET PHASE 3a — Option A: Collapse `users.xu` → `users.vin` at 1:1
-- ----------------------------------------------------------------------------
-- RFC:          docs/RFC_SINGLE_WALLET_UNIFICATION.md §Phase 3
-- Addendum:     docs/RFC_SINGLE_WALLET_UNIFICATION_V2_ADDENDUM.md §M7
-- Jira:         SUN-13xx (Phase 3, Option A)
-- Audit:        docs/WALLET_PHASE3_XU_USAGE_AUDIT.md
-- Author:       Backend / Wallet Unification team
-- Created:      2026-06-01
-- ============================================================================
--
-- WHAT
--   For every NON-BOT user with `users.xu > 0`, post an `XU_TO_VIN_MIGRATION`
--   `money_transaction` with two entries:
--       DEBIT  PROMO_POOL              (system, liability) -X VND
--       CREDIT PLAYER_VIN (per user)                       +X VND
--   Then zero `users.xu` and `users.xu_total` for that user so the SQL
--   denormalized cache matches the ledger.
--
-- WHY PROMO_POOL (NOT LEGACY_RECONCILIATION)
--   Per V2 §M7: xu is promotional balance. House P&L must absorb the cost of
--   converting it to real vin — PROMO_POOL is the correct source. Using
--   LEGACY_RECONCILIATION would silently grow that catch-all account and hide
--   the promotional cost from the operator's books.
--
-- IDEMPOTENCY
--   `external_ref = CONCAT('xu_collapse:', user_id)` is UNIQUE in
--   `money_idempotency`. Re-running the migration is a no-op:
--     1. money_transaction lookup by external_ref skips users already done.
--     2. The final UPDATE only touches rows where (xu+xu_total) > 0.
--
-- BOTS
--   `WHERE is_bot = 0 AND xu > 0` — bots never get a ledger entry. We zero
--   their xu/xu_total in a separate UPDATE block at the end so the columns
--   are clean before the Phase 3 drop migration (`20260615_phase3_drop_users_xu.sql`).
--
-- ROLLBACK
--   Reverse via a `REVERSAL_XU_TO_VIN_MIGRATION` batch posting the opposite
--   entries with `correlated_transaction_id = original_id`. Re-seeding
--   `users.xu` requires snapshot restore (the column itself is dropped in the
--   companion `20260615` migration — once dropped, fix-forward only).
--
-- PREREQUISITES (must already exist in this DB):
--   - money_account row with account_type='PROMO_POOL', is_system=1, currency='VND'
--     (seeded by 2026_05_02b_money_ledger_seed_system.sql)
--   - per-user PLAYER_VIN money_account rows
--     (seeded by 2026_05_02c_money_ledger_seed_users.sql)
--   - SP `post_money_transaction` (defined in 2026_05_02a_money_ledger_schema.sql)
--
-- EXECUTION
--   DO NOT EXECUTE AS PART OF THIS COMMIT. PM signoff required for Option A
--   vs Option B (see Open Question 1 in RFC). When approved, run in a
--   maintenance window with a fresh snapshot.
-- ============================================================================

USE vinplay;

-- ---------------------------------------------------------------------------
-- STEP 1 — Snapshot pre-state for reconciliation
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS _wallet_phase3a_pre_snapshot (
    user_id         BIGINT       NOT NULL PRIMARY KEY,
    nick_name       VARCHAR(64)  NOT NULL,
    xu_before       BIGINT       NOT NULL,
    xu_total_before BIGINT       NOT NULL,
    snapshot_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO _wallet_phase3a_pre_snapshot (user_id, nick_name, xu_before, xu_total_before)
SELECT u.id, COALESCE(u.nick_name, u.user_name, CONCAT('user_', u.id)), u.xu, u.xu_total
FROM users u
WHERE u.is_bot = 0 AND u.xu > 0
ON DUPLICATE KEY UPDATE
    xu_before       = VALUES(xu_before),
    xu_total_before = VALUES(xu_total_before),
    snapshot_at     = CURRENT_TIMESTAMP;

-- ---------------------------------------------------------------------------
-- STEP 2 — Post one XU_TO_VIN_MIGRATION transaction per affected user
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS _phase3a_collapse_xu_to_vin;
DELIMITER $$

CREATE PROCEDURE _phase3a_collapse_xu_to_vin(
    IN p_operator      VARCHAR(64),
    IN p_jira_ticket   VARCHAR(32),
    IN p_batch_id      VARCHAR(64)
)
BEGIN
    DECLARE v_user_id       BIGINT;
    DECLARE v_nickname      VARCHAR(64);
    DECLARE v_xu_amount     BIGINT;
    DECLARE v_external_ref  VARCHAR(128);
    DECLARE v_done          INT DEFAULT 0;
    DECLARE v_promo_acct    BIGINT;
    DECLARE v_vin_acct      BIGINT;
    DECLARE v_tx_id         BIGINT;
    DECLARE v_already_done  INT DEFAULT 0;
    DECLARE v_metadata      JSON;

    DECLARE cur CURSOR FOR
        SELECT u.id, COALESCE(u.nick_name, u.user_name, CONCAT('user_', u.id)), u.xu
        FROM users u
        WHERE u.is_bot = 0
          AND u.xu > 0
        ORDER BY u.id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    -- Resolve PROMO_POOL system account once
    SELECT account_id INTO v_promo_acct
    FROM money_account
    WHERE account_type = 'PROMO_POOL' AND is_system = 1 AND currency = 'VND'
    LIMIT 1;

    IF v_promo_acct IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'PROMO_POOL system account not found — re-run 2026_05_02b_money_ledger_seed_system.sql';
    END IF;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_user_id, v_nickname, v_xu_amount;
        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        SET v_external_ref = CONCAT('xu_collapse:', v_user_id);

        -- Idempotency: skip if external_ref already posted
        SELECT COUNT(*) INTO v_already_done
        FROM money_transaction
        WHERE external_ref = v_external_ref
          AND transaction_type = 'XU_TO_VIN_MIGRATION';

        IF v_already_done = 0 THEN

            -- Resolve this user's PLAYER_VIN account
            SELECT account_id INTO v_vin_acct
            FROM money_account
            WHERE owner_user_id = v_user_id
              AND account_type  = 'PLAYER_VIN'
              AND currency      = 'VND'
            LIMIT 1;

            IF v_vin_acct IS NULL THEN
                -- Should not happen if 2026_05_02c seeded all users.
                -- Log + skip rather than abort the batch.
                INSERT INTO _wallet_phase3a_errors (user_id, nick_name, reason, created_at)
                VALUES (v_user_id, v_nickname,
                        'PLAYER_VIN account missing — re-seed required', NOW());
            ELSE
                SET v_metadata = JSON_OBJECT(
                    'operator',      p_operator,
                    'jira_ticket',   p_jira_ticket,
                    'batch_id',      p_batch_id,
                    'run_timestamp', CAST(NOW() AS CHAR),
                    'host',          @@hostname,
                    'phase',         '3a',
                    'option',        'A',
                    'source_field',  'users.xu',
                    'xu_before',     v_xu_amount,
                    'note',          'V2 §M7: PROMO_POOL absorbs promotional balance conversion cost'
                );

                -- Post the transaction via the canonical SP.
                -- post_money_transaction(transaction_type, external_ref,
                --     debit_account_id, credit_account_id, currency,
                --     amount, description, metadata, OUT tx_id)
                CALL post_money_transaction(
                    'XU_TO_VIN_MIGRATION',
                    v_external_ref,
                    v_promo_acct,
                    v_vin_acct,
                    'VND',
                    v_xu_amount,
                    CONCAT('Phase 3a xu→vin 1:1 collapse for user ', v_nickname),
                    v_metadata,
                    v_tx_id
                );
            END IF;
        END IF;
    END LOOP;
    CLOSE cur;
END$$

DELIMITER ;

CREATE TABLE IF NOT EXISTS _wallet_phase3a_errors (
    err_id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    nick_name   VARCHAR(64)  NOT NULL,
    reason      VARCHAR(255) NOT NULL,
    created_at  DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Run the batch (operator passes their nickname + ticket + batch UUID via app)
-- CALL _phase3a_collapse_xu_to_vin('superadmin', 'SUN-13xx', UUID());

-- ---------------------------------------------------------------------------
-- STEP 3 — Zero the legacy columns once the ledger entries are in place.
--          This makes the denormalized `users.xu` cache match ledger state.
--          Bots are zeroed too (no ledger entry needed).
-- ---------------------------------------------------------------------------
UPDATE users
SET    xu       = 0,
       xu_total = 0
WHERE  xu       > 0
   OR  xu_total > 0;

-- ---------------------------------------------------------------------------
-- STEP 4 — Verification (no-op queries; safe to keep, run as smoke)
-- ---------------------------------------------------------------------------
-- 4a. Total VND collapsed should equal pre-snapshot xu sum
-- SELECT
--   (SELECT COALESCE(SUM(xu_before),0) FROM _wallet_phase3a_pre_snapshot) AS pre_total,
--   (SELECT COALESCE(SUM(amount),0)
--    FROM money_entry me
--    JOIN money_transaction mt ON mt.transaction_id = me.transaction_id
--    WHERE mt.transaction_type = 'XU_TO_VIN_MIGRATION'
--      AND me.entry_side = 'CREDIT') AS posted_total;
--
-- 4b. No user should still have non-zero xu
-- SELECT COUNT(*) FROM users WHERE xu > 0 OR xu_total > 0;
--
-- 4c. PROMO_POOL balance should have decreased by the same total
-- SELECT balance FROM money_account WHERE account_type='PROMO_POOL' AND is_system=1;

-- ---------------------------------------------------------------------------
-- STEP 5 — Drop helper procedure
-- ---------------------------------------------------------------------------
-- DROP PROCEDURE _phase3a_collapse_xu_to_vin;
-- (Keep for 30 days in case of rerun. Drop in cleanup migration.)
