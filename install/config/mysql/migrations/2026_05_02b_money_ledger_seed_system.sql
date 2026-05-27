-- =============================================================================
-- Sunwinkr Money Ledger Phase 0 — Seed Canonical System Accounts
--
-- This migration seeds the 10 system accounts that represent house pools,
-- clearing accounts, and transit wallets. Each is NOT tied to any user
-- (owner_user_id=NULL) and has is_system=1 to allow negative balance during
-- normal operation.
--
-- Idempotency: Uses INSERT ... SELECT ... WHERE NOT EXISTS pattern, so safe
-- to re-run multiple times without duplicate errors.
--
-- Reconciliation views (v_money_account_drift, etc.) will return 0 rows since
-- no transactions have been posted yet.
-- =============================================================================

-- House game pot: accumulates bets, pays out prizes and commissions
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'HOUSE_GAME_POT', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='HOUSE_GAME_POT' AND owner_user_id IS NULL AND currency='VND'
);

-- House revenue: net house keep (profit after payouts and commissions)
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'HOUSE_REVENUE', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='HOUSE_REVENUE' AND owner_user_id IS NULL AND currency='VND'
);

-- Bank inbox: incoming bank deposits before credit to player
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'BANK_INBOX', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='BANK_INBOX' AND owner_user_id IS NULL AND currency='VND'
);

-- Bank outbox: outgoing bank withdrawals being processed
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'BANK_OUTBOX', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='BANK_OUTBOX' AND owner_user_id IS NULL AND currency='VND'
);

-- Crypto inbox: incoming USDT deposits
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'CRYPTO_INBOX', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='CRYPTO_INBOX' AND owner_user_id IS NULL AND currency='VND'
);

-- Crypto outbox: outgoing USDT withdrawals
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'CRYPTO_OUTBOX', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='CRYPTO_OUTBOX' AND owner_user_id IS NULL AND currency='VND'
);

-- Promo pool: promotion bonus budget
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'PROMO_POOL', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='PROMO_POOL' AND owner_user_id IS NULL AND currency='VND'
);

-- Gift code pool: gift code redemption budget
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'GIFT_CODE_POOL', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='GIFT_CODE_POOL' AND owner_user_id IS NULL AND currency='VND'
);

-- Jackpot pool: progressive jackpot pool
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'JACKPOT_POOL', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='JACKPOT_POOL' AND owner_user_id IS NULL AND currency='VND'
);

-- Suspense: clearing account; must net to 0 daily
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'SUSPENSE', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='SUSPENSE' AND owner_user_id IS NULL AND currency='VND'
);

-- Legacy reconciliation: represents money already in the system before ledger tracking began.
-- Its balance will be deeply negative after backfill (~-195B VND) — this is CORRECT.
-- Added here as the 11th system account; also seeded idempotently by
-- 2026_05_02_money_ledger_backfill_initial_balances.sql before the backfill SP runs.
INSERT INTO vinplay.money_account
    (account_type, currency, balance, is_system, is_frozen)
SELECT 'LEGACY_RECONCILIATION', 'VND', 0, 1, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay.money_account
    WHERE account_type='LEGACY_RECONCILIATION' AND owner_user_id IS NULL AND currency='VND'
);
