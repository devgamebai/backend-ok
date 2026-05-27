-- SUN-13xx Phase 7: drop analytics columns recharge_money + gift_total
-- Replaced by view v_derived_deposit_total (sums money_gateway_log DEPOSIT_*).
USE vinplay;

ALTER TABLE users
    DROP COLUMN recharge_money,
    DROP COLUMN gift_total;
