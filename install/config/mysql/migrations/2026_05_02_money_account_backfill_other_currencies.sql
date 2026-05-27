-- =============================================================================
-- money_account — backfill missing PLAYER_VIN / PLAYER_XU / PLAYER_SAFE /
-- PLAYER_VP rows for users created after the 2026_05_02c seed migration.
--
-- WHY:
--   2026_05_02c_money_ledger_seed_users.sql seeded one row per user per
--   currency, but any user created since (registration, admin-create, etc.)
--   has no money_account rows — and the new MoneyGateway.creditCurrency code
--   refuses to write a ledger entry without a matching money_account, so
--   those credits silently skip dual-write.
--
--   Re-running the original seed-style INSERTs is naturally idempotent because
--   of the uk_owner_type(owner_user_id, account_type, currency) UNIQUE — this
--   migration just makes the operation explicit and routinely rerunnable.
--
-- COUNTS BEFORE:
--   ~6,065 users; some recent users missing one or more PLAYER_* rows.
--   (See report — exact deltas vary by run-time.)
-- COUNTS AFTER:
--   Every user has exactly one row per (PLAYER_VIN, PLAYER_XU, PLAYER_SAFE,
--   PLAYER_VP).
-- =============================================================================

-- PLAYER_VIN
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_VIN', u.id, u.nick_name, 'VND', u.vin, 0, 0
FROM vinplay.users u
LEFT JOIN vinplay.money_account ma
    ON ma.owner_user_id = u.id AND ma.account_type = 'PLAYER_VIN' AND ma.currency = 'VND'
WHERE ma.account_id IS NULL;

-- PLAYER_XU
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_XU', u.id, u.nick_name, 'VND', u.xu, 0, 0
FROM vinplay.users u
LEFT JOIN vinplay.money_account ma
    ON ma.owner_user_id = u.id AND ma.account_type = 'PLAYER_XU' AND ma.currency = 'VND'
WHERE ma.account_id IS NULL;

-- PLAYER_SAFE
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_SAFE', u.id, u.nick_name, 'VND', u.safe, 0, 0
FROM vinplay.users u
LEFT JOIN vinplay.money_account ma
    ON ma.owner_user_id = u.id AND ma.account_type = 'PLAYER_SAFE' AND ma.currency = 'VND'
WHERE ma.account_id IS NULL;

-- PLAYER_VP
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_VP', u.id, u.nick_name, 'VND', u.money_vp, 0, 0
FROM vinplay.users u
LEFT JOIN vinplay.money_account ma
    ON ma.owner_user_id = u.id AND ma.account_type = 'PLAYER_VP' AND ma.currency = 'VND'
WHERE ma.account_id IS NULL;
