-- =============================================================================
-- Phase 2 — Seed PLAYER_VAULT account for every non-bot user with vault funds.
--
-- Scope: introduces a NEW account_type `PLAYER_VAULT` per player (one row per
-- user in money_account, currency='VND', is_system=0). This is distinct from
-- the legacy `PLAYER_SAFE` account_type seeded in Phase 0 (2026_05_02c) which
-- mapped 1:1 to users.safe — Phase 2 unifies the legacy users.safe BIGINT
-- column AND the parallel MongoDB shadow ledger (mongo.vinplay.safe_box) into
-- a single ledger account.
--
-- Idempotency: INSERT IGNORE respects uk_owner_type(owner_user_id,
-- account_type, currency). Re-running is safe.
--
-- Eligibility:
--   * users.is_bot = 0 (bots are house liquidity, not real funds — RFC §B1)
--   * users.safe > 0 (MySQL-side legacy vault has a balance to migrate)
--
-- NOTE: this migration only PROVISIONS the account. The actual balance is
-- credited by 20260512_phase2_safe_migration.sql (separate migration so
-- account-seeding and money-movement are visible as distinct DBA steps).
-- We seed every non-bot eligible user — including those who only have a
-- MongoDB-side balance — by union-joining the SafeBox amounts dumped to a
-- staging table by the operator BEFORE running this migration (see header
-- of 20260512_phase2_safe_migration.sql for the staging-table contract).
--
-- Expected counts:
--   ~N rows where N = COUNT(DISTINCT users.id where (is_bot=0 AND safe>0)
--                                                OR (is_bot=0 AND mongo.safe_box.amount>0))
-- =============================================================================

-- Provision a PLAYER_VAULT account for every non-bot user with a positive
-- MySQL-side `safe` balance. Balance is seeded at 0; the actual credit
-- happens in 20260512_phase2_safe_migration.sql to keep account-provisioning
-- and money-movement reviewable as separate DBA steps.
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT
    'PLAYER_VAULT',
    u.id,
    u.nick_name,
    'VND',
    0,
    0,
    0
FROM vinplay.users u
WHERE COALESCE(u.is_bot, 0) = 0
  AND u.safe > 0;

-- Also provision PLAYER_VAULT for users that have ONLY a MongoDB-side
-- balance (users.safe = 0 but mongo.safe_box.amount > 0). The operator must
-- have populated vinplay.safebox_mongo_staging (user_id BIGINT, amount BIGINT)
-- from a `mongoexport --collection=safe_box` dump joined against users.nick_name
-- before this migration runs. If the staging table does not exist, this
-- branch is silently skipped — only the MySQL-side seed above applies.
SET @staging_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'vinplay'
      AND table_name = 'safebox_mongo_staging'
);

SET @sql := IF(@staging_exists > 0,
    'INSERT IGNORE INTO vinplay.money_account
         (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
     SELECT ''PLAYER_VAULT'', u.id, u.nick_name, ''VND'', 0, 0, 0
     FROM vinplay.users u
     JOIN vinplay.safebox_mongo_staging s ON s.user_id = u.id
     WHERE COALESCE(u.is_bot, 0) = 0
       AND s.amount > 0',
    'SELECT ''skipped: safebox_mongo_staging not present'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Post-run verification (manual):
-- SELECT COUNT(*) FROM vinplay.money_account
--   WHERE account_type='PLAYER_VAULT' AND is_system=0;
-- SELECT COUNT(*) FROM vinplay.users
--   WHERE COALESCE(is_bot,0)=0 AND safe>0;
