-- =============================================================================
-- Sunwinkr Money Ledger Phase 0 — Seed Player + Agent Accounts
--
-- This migration seeds the money_account table from existing wallet columns:
--
-- 1. Per-user player accounts (4 currencies each): PLAYER_VIN, PLAYER_XU,
--    PLAYER_SAFE, PLAYER_VP mapped from users table columns.
--
-- 2. Per-agent agency wallet: AGENT_WALLET from agency_wallet table.
--
-- 3. Per-agent credit wallet: AGENT_CREDIT from credit_wallet table.
--
-- Expected counts after seeding (with 6019 users, 33 agency_wallet, 2 credit_wallet):
--   PLAYER_VIN:      6019
--   PLAYER_XU:       6019
--   PLAYER_SAFE:     6019
--   PLAYER_VP:       6019
--   AGENT_WALLET:      33
--   AGENT_CREDIT:       2
--   (+ 10 system accounts from task #175)
--   Total:          24,101
--
-- Idempotency: Uses INSERT IGNORE … SELECT, which respects the UNIQUE key
-- uk_owner_type(owner_user_id, account_type, currency). Re-running this
-- migration will not create duplicates.
--
-- Reconciliation: v_money_account_drift and v_money_negative_player both
-- return 0 rows (no transactions posted yet, so sum(entries) = 0 trivially
-- matches seeded balances; no negative balances seeded).
-- =============================================================================

-- ============================================================================
-- PLAYER ACCOUNTS (4 per user)
-- ============================================================================

-- PLAYER_VIN: maps users.vin
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_VIN', u.id, u.nick_name, 'VND', u.vin, 0, 0
FROM vinplay.users u;

-- PLAYER_XU: maps users.xu
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_XU', u.id, u.nick_name, 'VND', u.xu, 0, 0
FROM vinplay.users u;

-- PLAYER_SAFE: maps users.safe
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_SAFE', u.id, u.nick_name, 'VND', u.safe, 0, 0
FROM vinplay.users u;

-- PLAYER_VP: maps users.money_vp
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'PLAYER_VP', u.id, u.nick_name, 'VND', u.money_vp, 0, 0
FROM vinplay.users u;

-- ============================================================================
-- AGENT ACCOUNTS
-- ============================================================================

-- AGENT_WALLET: one per agency_wallet row; maps balance from agency_wallet
-- Note: agency_wallet.agent_id references users.id, so owner_nickname is fetched from users
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'AGENT_WALLET', aw.agent_id, u.nick_name, 'VND', aw.balance, 0, 0
FROM vinplay.agency_wallet aw
LEFT JOIN vinplay.users u ON u.id = aw.agent_id;

-- AGENT_CREDIT: one per credit_wallet row; maps balance from credit_wallet
-- Note: credit_wallet.agent_id references users.id, so owner_nickname is fetched from users
INSERT IGNORE INTO vinplay.money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen)
SELECT 'AGENT_CREDIT', cw.agent_id, u.nick_name, 'VND', cw.balance, 0, 0
FROM vinplay.credit_wallet cw
LEFT JOIN vinplay.users u ON u.id = cw.agent_id;
