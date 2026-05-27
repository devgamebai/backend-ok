-- SUN-13xx Phase 0 — wallet unification groundwork (additive only)
-- Adds derived views replacing direct reads of users.vin_total / users.xu_total
-- / users.recharge_money. No behavior change; all existing code paths
-- continue to work.
USE vinplay;

-- 1. Derived player balance (replaces SELECT vin FROM users for ledger-first reads)
DROP VIEW IF EXISTS v_derived_player_balance;
CREATE VIEW v_derived_player_balance AS
SELECT
    ma.owner_user_id        AS user_id,
    ma.currency             AS currency,
    ma.account_type         AS account_type,
    ma.balance              AS ledger_balance,
    ma.updated_at           AS last_change_at
FROM money_account ma
WHERE ma.is_system = 0
  AND ma.account_type IN ('PLAYER_VIN','PLAYER_XU','PLAYER_SAFE','PLAYER_VP');

-- 2. Derived player P&L (replaces users.vin_total reads)
-- Sum of WAGER_CREDIT minus WAGER_DEBIT entries against the player's account
DROP VIEW IF EXISTS v_derived_player_pnl;
CREATE VIEW v_derived_player_pnl AS
SELECT
    ma.owner_user_id                                          AS user_id,
    ma.account_type                                           AS account_type,
    ma.currency                                               AS currency,
    SUM(CASE WHEN me.direction='CREDIT' THEN me.amount ELSE 0 END)
      - SUM(CASE WHEN me.direction='DEBIT' THEN me.amount ELSE 0 END) AS pnl_net,
    SUM(CASE WHEN me.direction='DEBIT'  THEN me.amount ELSE 0 END)    AS total_bet,
    SUM(CASE WHEN me.direction='CREDIT' THEN me.amount ELSE 0 END)    AS total_win,
    MIN(me.created_at)                                        AS first_entry_at,
    MAX(me.created_at)                                        AS last_entry_at
FROM money_account ma
JOIN money_entry me        ON me.account_id = ma.account_id
JOIN money_transaction mt  ON mt.transaction_id = me.transaction_id
WHERE ma.is_system = 0
  AND ma.account_type = 'PLAYER_VIN'
  AND mt.transaction_type IN ('WAGER_DEBIT','WAGER_CREDIT','JACKPOT_PAYOUT','JACKPOT_CONTRIB')
  AND mt.status = 'POSTED'
GROUP BY ma.owner_user_id, ma.account_type, ma.currency;

-- 3. Derived deposit total (replaces users.recharge_money reads)
DROP VIEW IF EXISTS v_derived_deposit_total;
CREATE VIEW v_derived_deposit_total AS
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

-- 4. Drift monitor — compare users.vin vs ledger PLAYER_VIN balance
DROP VIEW IF EXISTS v_wallet_drift;
CREATE VIEW v_wallet_drift AS
SELECT
    u.id                  AS user_id,
    u.nick_name           AS nickname,
    u.vin                 AS users_vin,
    COALESCE(ma.balance, 0) AS ledger_balance,
    (u.vin - COALESCE(ma.balance, 0)) AS drift_vnd,
    ma.updated_at         AS ledger_last_change
FROM users u
LEFT JOIN money_account ma
       ON ma.owner_user_id = u.id
      AND ma.account_type  = 'PLAYER_VIN'
      AND ma.currency      = 'VND'
WHERE u.is_bot = 0
  AND (u.vin - COALESCE(ma.balance, 0)) <> 0;

-- 5. Phase-0 drift snapshot table — hourly cron writes here
CREATE TABLE IF NOT EXISTS wallet_drift_snapshot (
    snapshot_id    BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    total_users    BIGINT NOT NULL,
    drifting_users BIGINT NOT NULL,
    max_abs_drift  BIGINT NOT NULL,
    sum_abs_drift  BIGINT NOT NULL,
    PRIMARY KEY (snapshot_id),
    KEY idx_snapshot_at (snapshot_at)
) ENGINE=InnoDB CHARSET=utf8mb4;

-- 6. Currency CHECK constraint — only VND today, VP reserved for VIP points
-- Idempotent: skip if constraint already exists
SET @cnt = (
  SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
   WHERE CONSTRAINT_SCHEMA='vinplay' AND CONSTRAINT_NAME='chk_money_account_currency'
);
SET @sql = IF(@cnt = 0,
  'ALTER TABLE money_account ADD CONSTRAINT chk_money_account_currency CHECK (currency IN (''VND'',''VP''))',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. Reversal correlated_transaction_id invariant
-- Trigger: REVERSAL_* transaction types MUST set correlated_transaction_id
DROP TRIGGER IF EXISTS trg_money_transaction_reversal_check;
DELIMITER //
CREATE TRIGGER trg_money_transaction_reversal_check
BEFORE INSERT ON money_transaction FOR EACH ROW
BEGIN
    IF NEW.transaction_type LIKE 'REVERSAL_%' AND NEW.correlated_transaction_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'REVERSAL_* transaction_type requires correlated_transaction_id';
    END IF;
END//
DELIMITER ;
