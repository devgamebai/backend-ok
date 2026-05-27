-- =============================================================================
-- 20260515_backfill_money_account.sql
--
-- Sunkr888 (user_id=9590) silent debit forensics showed game-thirdparty logs:
--     WARN MoneyLedger.findPlayerAccount: no account found for userId=9590 type=PLAYER_VIN
--     WARN MoneyGateway dual-write debit: no PLAYER_VIN account for userId=9590
--
-- The MoneyLedger Phase-1 dual-write is gated on the user having an
-- ACCOUNT_TYPE='PLAYER_VIN' row in money_account. Users created since the
-- ledger went live get one (via signup hook), but pre-ledger users do not.
-- This leaves the ledger blind to any wallet mutation on them — including the
-- silent stomp that caused the 1.78M loss.
--
-- SCOPE: PLAYER_VIN only. xu / safe / money_vp columns on users are slated
-- for removal in the schema cleanup, so backfilling PLAYER_XU / PLAYER_SAFE /
-- PLAYER_VP accounts here would just create rows we delete later. The five
-- thousand pre-existing PLAYER_XU/SAFE/VP rows are out of scope for this
-- migration too — they'll be dropped together with the columns.
--
-- Audit (2026-05-15, before this migration):
--   users (all):                 6,887
--   money_account PLAYER_VIN:    5,559   <- gap of 1,328 users with no row
--
-- IDEMPOTENT: only inserts where the row does not already exist.
--
-- NEGATIVE BALANCES: money_account has CHECK (is_system=1 OR balance >= 0).
-- 7 bot accounts (is_bot=1) currently have users.vin < 0 — legacy fallout from
-- the same absolute-write bug fixed in 20260515_atomic_money_procs.sql (no
-- floor check before this fix). We clamp negative seed balances to 0 via
-- GREATEST(0, …) so the ledger starts in a valid state. The users.vin column
-- itself is left untouched — those bot rows can be reset in a separate
-- one-shot cleanup if/when product decides.
-- =============================================================================

INSERT INTO money_account
    (account_type, owner_user_id, owner_nickname, currency, balance, is_system, is_frozen, created_at, updated_at)
SELECT
    'PLAYER_VIN',
    u.id,
    u.nick_name,
    'VND',
    GREATEST(0, IFNULL(u.vin, 0)),
    0,
    0,
    NOW(6),
    NOW(6)
FROM users u
LEFT JOIN money_account ma
    ON ma.owner_user_id = u.id
   AND ma.account_type  = 'PLAYER_VIN'
WHERE ma.account_id IS NULL;

-- Post-backfill verification (documentation only, run separately):
--   SELECT (SELECT COUNT(*) FROM users) AS users,
--          (SELECT COUNT(*) FROM money_account WHERE account_type='PLAYER_VIN') AS vin_accts;
--   -- vin_accts should be >= users (2 known pre-existing orphans for deleted
--   -- users 8713/8727 from earlier seeding, balance=0, harmless).
