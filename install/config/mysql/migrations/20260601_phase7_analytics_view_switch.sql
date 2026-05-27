-- SUN-13xx Phase 7 — replace `users.recharge_money` / `users.gift_total`
-- with derived views over the money ledger.
--
-- `v_derived_deposit_total` already exists from Phase 0
-- (`20260511_wallet_unify_phase0_views.sql`) — this migration only adds
-- `v_derived_gift_total` and re-asserts the deposit-total view shape so
-- code can rely on the column set without re-checking Phase 0 status.
--
-- Pure additive.  Drop of the underlying columns happens in a separate
-- migration (`20260615_phase7_drop_analytics_columns.sql`, 14-day delay).
USE vinplay;

-- 1. Re-assert the deposit-total view from Phase 0 so reads have a
--    stable contract.  CREATE OR REPLACE preserves the Phase 0
--    definition if it has not changed.
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

-- 2. Derived gift total — replaces `users.gift_total`.
--    Sums every CREDIT into PLAYER_VIN whose transaction_type denotes a
--    promo / gift code redemption.  Mirrors the deposit-total shape so
--    callers can SELECT against a consistent column set.
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
  AND (mt.transaction_type = 'PROMO_CLAIM'
       OR mt.transaction_type LIKE 'GIFT\\_%' ESCAPE '\\'
       OR mt.transaction_type IN ('GIFTCODE_REDEEM','GIFT_BONUS','EVENT_GIFT'))
  AND mt.status = 'POSTED'
GROUP BY ma.owner_user_id;
