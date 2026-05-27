-- SUN-13xx Phase 7 — derived view for users.gift_total (wallet-unification)
--
-- After Phase 7 dropped users.recharge_money and users.gift_total, analytics
-- code must read from the money ledger. `v_derived_deposit_total` already
-- exists from Phase 0 (`20260511_wallet_unify_phase0_views.sql`). This
-- migration creates the symmetric `v_derived_gift_total` so callers have a
-- stable contract.
--
-- Idempotent: CREATE OR REPLACE / DROP IF EXISTS. No data migration — the
-- view reads live from money_transaction.
USE vinplay;

-- 1. Re-assert the deposit-total view from Phase 0 so reads have a stable
--    column contract regardless of Phase-0 application status.
CREATE OR REPLACE VIEW v_derived_deposit_total AS
SELECT
    ma.owner_user_id                              AS user_id,
    SUM(me.amount)                                AS deposit_total,
    COUNT(*)                                      AS deposit_count,
    MIN(me.created_at)                            AS first_deposit_at,
    MAX(me.created_at)                            AS last_deposit_at
FROM money_account ma
JOIN money_entry me        ON me.account_id = ma.account_id AND me.direction='CREDIT'
JOIN money_transaction mt  ON mt.transaction_id = me.transaction_id
WHERE ma.is_system = 0
  AND ma.account_type = 'PLAYER_VIN'
  AND mt.transaction_type IN ('DEPOSIT_BANK','DEPOSIT_CRYPTO','DEPOSIT_TELEGRAM','CARD_RECHARGE')
  AND mt.status = 'POSTED'
GROUP BY ma.owner_user_id;

-- 2. Derived gift total — replaces users.gift_total. Mirrors the deposit
--    view shape so callers can SELECT against a consistent column set.
DROP VIEW IF EXISTS v_derived_gift_total;
CREATE VIEW v_derived_gift_total AS
SELECT
    ma.owner_user_id                              AS user_id,
    SUM(me.amount)                                AS gift_total,
    COUNT(*)                                      AS gift_count,
    MIN(me.created_at)                            AS first_gift_at,
    MAX(me.created_at)                            AS last_gift_at
FROM money_account ma
JOIN money_entry me        ON me.account_id = ma.account_id AND me.direction='CREDIT'
JOIN money_transaction mt  ON mt.transaction_id = me.transaction_id
WHERE ma.is_system = 0
  AND ma.account_type = 'PLAYER_VIN'
  AND mt.transaction_type IN ('PROMO_CLAIM','GIFTCODE_REDEEM','EVENT_GIFT')
  AND mt.status = 'POSTED'
GROUP BY ma.owner_user_id;
