-- ============================================================================
-- WALLET PHASE 3b — Option B: Migrate `users.xu` → `PLAYER_PROMO_WALLET`
-- ----------------------------------------------------------------------------
-- RFC:          docs/RFC_SINGLE_WALLET_UNIFICATION.md §Phase 3 (Option B)
-- Addendum:     docs/RFC_SINGLE_WALLET_UNIFICATION_V2_ADDENDUM.md
-- Jira:         SUN-13xx (Phase 3, Option B)
-- Audit:        docs/WALLET_PHASE3_XU_USAGE_AUDIT.md
-- Author:       Backend / Wallet Unification team
-- Created:      2026-06-01
-- ============================================================================
--
-- WHAT
--   Treats `users.xu` as a separate "promotional balance" that retains its
--   own identity. For every NON-BOT user with `users.xu > 0`:
--     1. Seed a `PLAYER_PROMO_WALLET` money_account (currency='VND') for them
--        if missing.
--     2. Post an `XU_TO_PROMO_MIGRATION` transaction:
--          DEBIT  LEGACY_RECONCILIATION -X
--          CREDIT PLAYER_PROMO_WALLET   +X
--   Does NOT touch `users.xu` — under Option B the column stays alive but
--   becomes a denormalized read-cache like `users.vin`. A new trigger
--   `trg_post_money_transaction_sync_xu` keeps it aligned with the ledger.
--
-- WHY LEGACY_RECONCILIATION (NOT PROMO_POOL)
--   Under Option B, the promotional balance is *still owed* to the player —
--   it has just moved bookkeeping homes. LEGACY_RECONCILIATION is the
--   "phantom money" catch-all from Phase 0 backfill; sourcing from it preserves
--   the global SUM(all accounts) = 0 invariant without distorting PROMO_POOL
--   (which represents future promotional liability, not legacy float).
--
-- IDEMPOTENCY
--   `external_ref = CONCAT('xu_promo_migrate:', user_id)`. UNIQUE per user.
--
-- BOTS
--   Excluded (`WHERE is_bot = 0`).
--
-- ROLLBACK
--   Post `REVERSAL_XU_TO_PROMO_MIGRATION` transactions. Drop the trigger.
--   PLAYER_PROMO_WALLET accounts remain (zero-balance OK).
--
-- TRIGGER MAINTENANCE
--   The post-write trigger fires whenever a PLAYER_PROMO_WALLET balance
--   changes via the ledger SP. It writes `users.xu` to match. Required for
--   60+ existing call sites that still `SELECT u.xu` directly.
--
-- EXECUTION
--   DO NOT EXECUTE AS PART OF THIS COMMIT. PM signoff required.
-- ============================================================================

USE vinplay;

-- ---------------------------------------------------------------------------
-- STEP 1 — Pre-migration snapshot
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS _wallet_phase3b_pre_snapshot (
    user_id         BIGINT       NOT NULL PRIMARY KEY,
    nick_name       VARCHAR(64)  NOT NULL,
    xu_before       BIGINT       NOT NULL,
    xu_total_before BIGINT       NOT NULL,
    snapshot_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO _wallet_phase3b_pre_snapshot (user_id, nick_name, xu_before, xu_total_before)
SELECT u.id, u.nick_name, u.xu, u.xu_total
FROM users u
WHERE u.is_bot = 0 AND u.xu > 0
ON DUPLICATE KEY UPDATE
    xu_before       = VALUES(xu_before),
    xu_total_before = VALUES(xu_total_before),
    snapshot_at     = CURRENT_TIMESTAMP;

-- ---------------------------------------------------------------------------
-- STEP 2 — Seed PLAYER_PROMO_WALLET account_type per affected user.
--   The uk_owner_type(owner_user_id, account_type, currency) UNIQUE makes
--   re-runs safe.
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO money_account
    (account_type, owner_user_id, owner_nickname, currency, balance,
     is_system, is_frozen, created_at, updated_at)
SELECT 'PLAYER_PROMO_WALLET',
       u.id,
       u.nick_name,
       'VND',
       0,
       0,
       0,
       NOW(),
       NOW()
FROM   users u
WHERE  u.is_bot = 0
  AND  u.xu     > 0;

-- ---------------------------------------------------------------------------
-- STEP 3 — Post one XU_TO_PROMO_MIGRATION per user.
--          DEBIT LEGACY_RECONCILIATION, CREDIT PLAYER_PROMO_WALLET.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS _phase3b_migrate_xu_to_promo;
DELIMITER $$

CREATE PROCEDURE _phase3b_migrate_xu_to_promo(
    IN p_operator      VARCHAR(64),
    IN p_jira_ticket   VARCHAR(32),
    IN p_batch_id      VARCHAR(64)
)
BEGIN
    DECLARE v_user_id        BIGINT;
    DECLARE v_nickname       VARCHAR(64);
    DECLARE v_xu_amount      BIGINT;
    DECLARE v_external_ref   VARCHAR(128);
    DECLARE v_legacy_acct    BIGINT;
    DECLARE v_promo_acct     BIGINT;
    DECLARE v_already_done   INT DEFAULT 0;
    DECLARE v_done           INT DEFAULT 0;
    DECLARE v_tx_id          BIGINT;
    DECLARE v_metadata       JSON;

    DECLARE cur CURSOR FOR
        SELECT u.id, u.nick_name, u.xu
        FROM   users u
        WHERE  u.is_bot = 0
          AND  u.xu     > 0
        ORDER BY u.id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SELECT account_id INTO v_legacy_acct
    FROM   money_account
    WHERE  account_type = 'LEGACY_RECONCILIATION' AND is_system = 1 AND currency = 'VND'
    LIMIT  1;

    IF v_legacy_acct IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'LEGACY_RECONCILIATION system account missing';
    END IF;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_user_id, v_nickname, v_xu_amount;
        IF v_done = 1 THEN LEAVE read_loop; END IF;

        SET v_external_ref = CONCAT('xu_promo_migrate:', v_user_id);

        SELECT COUNT(*) INTO v_already_done
        FROM   money_transaction
        WHERE  external_ref     = v_external_ref
          AND  transaction_type = 'XU_TO_PROMO_MIGRATION';

        IF v_already_done = 0 THEN
            SELECT account_id INTO v_promo_acct
            FROM   money_account
            WHERE  owner_user_id = v_user_id
              AND  account_type  = 'PLAYER_PROMO_WALLET'
              AND  currency      = 'VND'
            LIMIT  1;

            IF v_promo_acct IS NULL THEN
                INSERT INTO _wallet_phase3b_errors (user_id, nick_name, reason, created_at)
                VALUES (v_user_id, v_nickname,
                        'PLAYER_PROMO_WALLET account missing after seed', NOW());
            ELSE
                SET v_metadata = JSON_OBJECT(
                    'operator',      p_operator,
                    'jira_ticket',   p_jira_ticket,
                    'batch_id',      p_batch_id,
                    'run_timestamp', CAST(NOW() AS CHAR),
                    'host',          @@hostname,
                    'phase',         '3b',
                    'option',        'B',
                    'source_field',  'users.xu',
                    'xu_before',     v_xu_amount
                );

                CALL post_money_transaction(
                    'XU_TO_PROMO_MIGRATION',
                    v_external_ref,
                    v_legacy_acct,
                    v_promo_acct,
                    'VND',
                    v_xu_amount,
                    CONCAT('Phase 3b xu → PLAYER_PROMO_WALLET for ', v_nickname),
                    v_metadata,
                    v_tx_id
                );
            END IF;
        END IF;
    END LOOP;
    CLOSE cur;
END$$

DELIMITER ;

CREATE TABLE IF NOT EXISTS _wallet_phase3b_errors (
    err_id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    nick_name  VARCHAR(64)  NOT NULL,
    reason     VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- CALL _phase3b_migrate_xu_to_promo('superadmin', 'SUN-13xx', UUID());

-- ---------------------------------------------------------------------------
-- STEP 4 — Install AFTER INSERT trigger on `money_entry` to keep `users.xu`
--          synced with the PLAYER_PROMO_WALLET ledger balance.
--          NOTE: this is intentionally a denormalized cache, not authoritative.
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_sync_xu_after_money_entry;
DELIMITER $$

CREATE TRIGGER trg_sync_xu_after_money_entry
AFTER INSERT ON money_entry
FOR EACH ROW
BEGIN
    DECLARE v_owner_id    BIGINT;
    DECLARE v_acct_type   VARCHAR(40);
    DECLARE v_new_balance BIGINT;

    SELECT owner_user_id, account_type, balance
    INTO   v_owner_id, v_acct_type, v_new_balance
    FROM   money_account
    WHERE  account_id = NEW.account_id;

    IF v_acct_type = 'PLAYER_PROMO_WALLET' AND v_owner_id IS NOT NULL THEN
        UPDATE users
        SET    xu = v_new_balance
        WHERE  id = v_owner_id;
        -- xu_total intentionally NOT updated here — it is cumulative P&L,
        -- not balance. Phase 4 retires it; until then it stays frozen.
    END IF;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------------
-- STEP 5 — Verification queries
-- ---------------------------------------------------------------------------
-- 5a. Every snapshotted user must now have a matching PROMO_WALLET balance
-- SELECT s.user_id, s.xu_before,
--        (SELECT balance FROM money_account
--         WHERE owner_user_id = s.user_id
--           AND account_type  = 'PLAYER_PROMO_WALLET') AS promo_balance
-- FROM _wallet_phase3b_pre_snapshot s
-- HAVING s.xu_before <> promo_balance;
--
-- 5b. users.xu unchanged (trigger keeps cache aligned)
-- SELECT s.user_id FROM _wallet_phase3b_pre_snapshot s
-- JOIN users u ON u.id = s.user_id WHERE u.xu <> s.xu_before;

-- ---------------------------------------------------------------------------
-- STEP 6 — Cleanup
-- ---------------------------------------------------------------------------
-- DROP PROCEDURE _phase3b_migrate_xu_to_promo;
-- (Keep 30 days, drop in cleanup migration.)
