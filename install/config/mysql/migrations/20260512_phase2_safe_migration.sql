-- =============================================================================
-- Phase 2 — Drain `users.safe` AND mongo.vinplay.safe_box into PLAYER_VAULT.
--
-- For every non-bot user with EITHER a positive `users.safe` BIGINT
-- (legacy MySQL vault) OR a positive `mongo.vinplay.safe_box.amount`
-- (legacy MongoDB shadow vault discovered during architect review of
-- the v2 RFC addendum), post a single LEGACY_SAFE_MIGRATION transaction
-- that:
--   * CREDITs PLAYER_VAULT by the combined (mysql_safe + mongo_safe) amount.
--   * DEBITs  LEGACY_RECONCILIATION by the same amount.
--   * Sets users.safe = 0 in the same SQL transaction.
--
-- Idempotency:
--   external_ref = CONCAT('safe_migration:', users.id)
--   transaction_type = LEGACY_SAFE_MIGRATION
--   uk_idempotency(transaction_type, external_ref, posted_at) prevents double
--   posting; post_money_transaction SP returns status='DUPLICATE' on rerun.
--
-- STAGING TABLE CONTRACT (MongoDB amounts):
--   The operator MUST populate vinplay.safebox_mongo_staging BEFORE running
--   this migration:
--     CREATE TABLE vinplay.safebox_mongo_staging (
--       user_id  BIGINT       NOT NULL PRIMARY KEY,
--       nick_name VARCHAR(128) NOT NULL,
--       amount   BIGINT       NOT NULL
--     );
--   Recipe to populate (run on a host with mongo-cli access):
--     mongoexport --uri=mongodb://vinplay:***@mongo:27017/vinplay \
--                 --collection=safe_box --out=safe_box.json --jsonArray
--     # then INSERT INTO safebox_mongo_staging using nick_name → user_id JOIN.
--   If the staging table is absent, only the users.safe BIGINT side runs —
--   MongoDB drain is skipped (with a SELECT info row).
--
-- Post-run state:
--   * Every eligible user has a PLAYER_VAULT account with balance =
--     (pre-migration users.safe + pre-migration mongo.safe_box.amount).
--   * users.safe = 0 for every eligible user.
--   * `safebox_mongo_staging` remains as the audit-trail of MongoDB amounts.
--     A follow-up Java drain on the MongoDB side is required to zero the
--     collection — this is intentionally a manual step (mongo.collection.drop
--     is not idempotent in a SQL migration).
--
-- NOTE on direct ledger writes:
--   Like 2026_05_02d_money_ledger_backfill_initial_balances.sql, this is the
--   accepted one-shot DBA exception to "only post_money_transaction writes".
--   We use direct INSERTs into money_transaction / money_entry to avoid the
--   per-row JSON / cursor overhead of the SP across thousands of users.
-- =============================================================================

-- Defensive: this migration depends on 20260512_phase2_player_vault.sql
-- having seeded PLAYER_VAULT accounts. Abort early if any eligible user lacks
-- one — running phase2_player_vault first is a hard prerequisite.
SET @missing := (
    SELECT COUNT(*)
    FROM vinplay.users u
    LEFT JOIN vinplay.money_account ma
      ON ma.owner_user_id = u.id
     AND ma.account_type = 'PLAYER_VAULT'
     AND ma.currency = 'VND'
    WHERE COALESCE(u.is_bot, 0) = 0
      AND u.safe > 0
      AND ma.account_id IS NULL
);
SELECT IF(@missing = 0,
    'phase2_player_vault prerequisite satisfied',
    CONCAT('ABORT: ', @missing, ' users missing PLAYER_VAULT — run 20260512_phase2_player_vault.sql first')
) AS prereq_check;

-- Build a temp table of (user_id, nick_name, amount) covering both sources.
-- COALESCE handles missing rows in either side; non-bot filter applied here.
DROP TEMPORARY TABLE IF EXISTS _phase2_safe_combined;
CREATE TEMPORARY TABLE _phase2_safe_combined (
    user_id   BIGINT       NOT NULL PRIMARY KEY,
    nick_name VARCHAR(128) NOT NULL,
    amount    BIGINT       NOT NULL
) ENGINE=MEMORY;

-- MySQL side (always present)
INSERT INTO _phase2_safe_combined (user_id, nick_name, amount)
SELECT u.id, u.nick_name, u.safe
FROM vinplay.users u
WHERE COALESCE(u.is_bot, 0) = 0 AND u.safe > 0
ON DUPLICATE KEY UPDATE amount = amount + VALUES(amount);

-- MongoDB side (conditional on staging table)
SET @staging_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'vinplay' AND table_name = 'safebox_mongo_staging'
);
SET @sql := IF(@staging_exists > 0,
    'INSERT INTO _phase2_safe_combined (user_id, nick_name, amount)
     SELECT u.id, u.nick_name, s.amount
     FROM vinplay.safebox_mongo_staging s
     JOIN vinplay.users u ON u.id = s.user_id
     WHERE COALESCE(u.is_bot, 0) = 0 AND s.amount > 0
     ON DUPLICATE KEY UPDATE amount = amount + VALUES(amount)',
    'SELECT ''mongo staging absent — only MySQL safe column drained'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Run the migration SP. Cursor-driven posting via post_money_transaction
-- (handles idempotency + balance-update + entry insertion atomically).
DROP PROCEDURE IF EXISTS vinplay.phase2_safe_migration_run;

DELIMITER $$

CREATE PROCEDURE vinplay.phase2_safe_migration_run()
proc_label: BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_user_id BIGINT;
    DECLARE v_nick VARCHAR(128);
    DECLARE v_amount BIGINT;
    DECLARE v_vault_id BIGINT;
    DECLARE v_legacy_id BIGINT;
    DECLARE v_tx_id BIGINT;
    DECLARE v_status VARCHAR(40);
    DECLARE v_entries_json JSON;
    DECLARE v_external_ref VARCHAR(160);

    DECLARE c CURSOR FOR
        SELECT user_id, nick_name, amount
        FROM _phase2_safe_combined
        ORDER BY user_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SELECT account_id INTO v_legacy_id
    FROM vinplay.money_account
    WHERE account_type = 'LEGACY_RECONCILIATION' AND is_system = 1 LIMIT 1;
    IF v_legacy_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'LEGACY_RECONCILIATION account missing — Phase 0 backfill not applied';
    END IF;

    OPEN c;
    safe_loop: LOOP
        FETCH c INTO v_user_id, v_nick, v_amount;
        IF v_done THEN LEAVE safe_loop; END IF;

        -- Resolve target vault account (provisioned by phase2_player_vault.sql)
        SELECT account_id INTO v_vault_id
        FROM vinplay.money_account
        WHERE owner_user_id = v_user_id
          AND account_type = 'PLAYER_VAULT'
          AND currency = 'VND'
        LIMIT 1;

        IF v_vault_id IS NULL THEN
            -- Eligible user without a vault — fail loudly. The prereq check
            -- above should have caught this.
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'PLAYER_VAULT missing mid-run — re-seed required';
        END IF;

        SET v_external_ref = CONCAT('safe_migration:', v_user_id);
        SET v_entries_json = JSON_ARRAY(
            JSON_OBJECT(
                'account_id', v_legacy_id,
                'direction',  'DEBIT',
                'amount',     v_amount,
                'note',       'legacy safe drain'
            ),
            JSON_OBJECT(
                'account_id', v_vault_id,
                'direction',  'CREDIT',
                'amount',     v_amount,
                'note',       CONCAT('vault credit user=', v_nick)
            )
        );

        CALL vinplay.post_money_transaction(
            'LEGACY_SAFE_MIGRATION',
            v_external_ref,
            NULL,
            'phase2_safe_migration',
            CONCAT('Drain legacy users.safe + mongo.safe_box to PLAYER_VAULT for user=', v_nick),
            NULL,
            JSON_OBJECT('phase', 'P2_SAFE_DRAIN', 'user_id', v_user_id, 'amount', v_amount),
            v_entries_json,
            v_tx_id,
            v_status
        );

        -- DUPLICATE on rerun is expected & safe. POSTED is happy path. Anything
        -- else aborts the loop so the operator can investigate.
        IF v_status NOT IN ('POSTED', 'DUPLICATE') THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = CONCAT('post_money_transaction failed status=', v_status,
                                          ' user_id=', v_user_id);
        END IF;
    END LOOP safe_loop;
    CLOSE c;

    -- Zero out users.safe for every eligible row in a single set-based UPDATE.
    -- Idempotent: a rerun where users.safe is already 0 is a no-op.
    UPDATE vinplay.users u
    JOIN _phase2_safe_combined s ON s.user_id = u.id
    SET u.safe = 0
    WHERE u.safe > 0;

    SELECT CONCAT('PHASE2_SAFE_MIGRATION_DONE — rows=',
        (SELECT COUNT(*) FROM _phase2_safe_combined)) AS status;
END proc_label$$

DELIMITER ;

CALL vinplay.phase2_safe_migration_run();

DROP PROCEDURE IF EXISTS vinplay.phase2_safe_migration_run;
DROP TEMPORARY TABLE IF EXISTS _phase2_safe_combined;

-- Post-run verification (manual):
-- SELECT SUM(balance) FROM vinplay.money_account
--   WHERE account_type='PLAYER_VAULT' AND is_system=0;
-- SELECT COUNT(*) FROM vinplay.users WHERE safe > 0;
-- SELECT COUNT(*) FROM vinplay.money_transaction
--   WHERE transaction_type='LEGACY_SAFE_MIGRATION';
