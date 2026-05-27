-- SUN-13xx Phase 0 — drift analyzer
-- Categorizes the 40 drifting users by root cause: which transaction sources
-- in money_gateway_log never reached money_transaction.
USE vinplay;

-- 1. Per-user, per-source coverage analyzer
-- Compares each source's gateway_log sum vs whether equivalent money_transaction
-- entries exist for that user.
DROP VIEW IF EXISTS v_wallet_drift_source_breakdown;
CREATE VIEW v_wallet_drift_source_breakdown AS
SELECT
    gl.user_id,
    gl.nick_name                                  AS nickname,
    gl.source                                     AS source,
    COUNT(*)                                      AS log_rows,
    SUM(gl.amount)                                AS log_net_amount,
    -- Map gateway_log.source → likely money_transaction.transaction_type
    -- (best-effort; some sources have direct mapping, others don't)
    CASE gl.source
        WHEN 'USERSERVICE_GAME'  THEN 'WAGER_DEBIT/CREDIT (via SP — bypasses ledger)'
        WHEN 'AWC_DEBIT'         THEN 'WAGER_DEBIT'
        WHEN 'AWC_CREDIT'        THEN 'WAGER_CREDIT'
        WHEN 'GSC_DEBIT'         THEN 'WAGER_DEBIT'
        WHEN 'GSC_CREDIT'        THEN 'WAGER_CREDIT'
        WHEN 'DEPOSIT_BANK'      THEN 'DEPOSIT_BANK'
        WHEN 'DEPOSIT_TELEGRAM'  THEN 'DEPOSIT_TELEGRAM'
        WHEN 'DEPOSIT_CRYPTO'    THEN 'DEPOSIT_CRYPTO'
        WHEN 'WITHDRAW_BANK'     THEN 'WITHDRAW_BANK'
        WHEN 'WITHDRAW_CRYPTO'   THEN 'WITHDRAW_CRYPTO'
        WHEN 'ADMIN_TOPUP'       THEN 'ADMIN_TOPUP'
        WHEN 'PROMO_BONUS'       THEN 'DEPOSIT_PROMO'
        WHEN 'CASHBACK_PAYOUT'   THEN 'CASHBACK_PAYOUT'
        WHEN 'VIPPOINT_UPDATE'   THEN 'VIPPOINT_UPDATE'
        WHEN 'REFUND_WITHDRAW'   THEN 'REFUND_WITHDRAW'
        WHEN 'CONVERT_AGENCY_TO_VIN' THEN 'CONVERT_AGENCY_TO_VIN'
        ELSE                          gl.source
    END AS expected_tx_type,
    MIN(gl.created_at)                            AS first_log_at,
    MAX(gl.created_at)                            AS last_log_at
FROM money_gateway_log gl
JOIN v_wallet_drift wd ON wd.user_id = gl.user_id
GROUP BY gl.user_id, gl.nick_name, gl.source;

-- 2. Drift root-cause summary per user
-- For each drifting user, computes:
--   - users.vin (observed)
--   - ledger PLAYER_VIN balance (observed)
--   - gateway_log_net_since_seed (sum of all log entries since BACKFILL_INITIAL_BALANCE)
--   - missing_in_ledger (sum of log entries from sources that don't reach the ledger)
DROP VIEW IF EXISTS v_wallet_drift_root_cause;
CREATE VIEW v_wallet_drift_root_cause AS
SELECT
    wd.user_id,
    wd.nickname,
    wd.users_vin,
    wd.ledger_balance,
    wd.drift_vnd,
    -- USERSERVICE_GAME (legacy SP) never dual-writes → primary drift driver
    COALESCE((
        SELECT SUM(gl.amount)
        FROM money_gateway_log gl
        WHERE gl.user_id = wd.user_id AND gl.source = 'USERSERVICE_GAME'
    ), 0) AS userservice_game_net,
    -- All non-game sources
    COALESCE((
        SELECT SUM(gl.amount)
        FROM money_gateway_log gl
        WHERE gl.user_id = wd.user_id AND gl.source != 'USERSERVICE_GAME'
    ), 0) AS other_sources_net,
    -- Was the user ever backfilled?
    EXISTS(
        SELECT 1 FROM money_transaction mt
        JOIN money_entry me ON me.transaction_id = mt.transaction_id
        JOIN money_account ma ON ma.account_id = me.account_id
        WHERE ma.owner_user_id = wd.user_id
          AND ma.account_type = 'PLAYER_VIN'
          AND mt.transaction_type = 'BACKFILL_INITIAL_BALANCE'
    ) AS has_initial_backfill
FROM v_wallet_drift wd;

-- 3. Coverage gap analysis — which sources have ZERO ledger representation?
-- This is the canonical "fix-the-gap" list for Phase 1 prep.
DROP VIEW IF EXISTS v_ledger_coverage_gap;
CREATE VIEW v_ledger_coverage_gap AS
SELECT
    gl.source                                                       AS log_source,
    COUNT(*)                                                        AS log_rows,
    SUM(gl.amount)                                                  AS log_net_amount,
    SUM(ABS(gl.amount))                                             AS log_gross_amount,
    COUNT(DISTINCT gl.user_id)                                      AS affected_users,
    -- Ledger transactions whose transaction_type matches this source's expected mapping
    (
        SELECT COUNT(*) FROM money_transaction mt
        WHERE mt.transaction_type = CASE gl.source
            WHEN 'USERSERVICE_GAME'  THEN 'WAGER_DEBIT'
            WHEN 'AWC_DEBIT'         THEN 'WAGER_DEBIT'
            WHEN 'AWC_CREDIT'        THEN 'WAGER_CREDIT'
            WHEN 'GSC_DEBIT'         THEN 'WAGER_DEBIT'
            WHEN 'GSC_CREDIT'        THEN 'WAGER_CREDIT'
            ELSE gl.source
        END
    )                                                               AS approx_ledger_tx_for_type,
    MIN(gl.created_at)                                              AS first_log_at,
    MAX(gl.created_at)                                              AS last_log_at
FROM money_gateway_log gl
GROUP BY gl.source
ORDER BY COUNT(*) DESC;
