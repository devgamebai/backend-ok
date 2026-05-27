-- =============================================================================
-- One-shot drift reconciliation. NOT auto-applied.
--
-- Findings from 2026-05-06 forensics:
--
--   80 drifted accounts, 460,137,264 total absolute drift
--   ─ 73 from a single 2026-05-02 backfill that seeded money_account.balance
--     from users.vin/xu but never posted matching money_entry rows
--   ─ 7 small "partial_drift" rows (121,468 total) — real dual-write misses
--
-- Strategy: treat current users.vin/xu/etc. as TRUTH (since 73 Java sites + bots
-- read it). Bring money_account into agreement by:
--   1. Reset money_account.balance to current users.<col> for each drifted player.
--   2. Post a synthetic 'OPENING_BALANCE_RECONCILE' transaction:
--        CREDIT  player_account  +current_balance
--        DEBIT   LEGACY_RECONCILIATION  +current_balance
--      (sums to zero per the SP rule)
--
-- Effect: drift = 0, ledger has one synthetic OPENING entry per account that
-- explains the seed amount, audit trail intact. The "lost" historical seed
-- delta (e.g. huylee101jocker's 62.8M phantom value) was never real money —
-- it was a stale backfill snapshot that drifted as gameplay continued through
-- users.vin only.
--
-- Run order:
--   1. Verify pre-state: SELECT COUNT(*) FROM v_money_account_drift;  -- expect 80
--   2. Run the prepare block below into a session, review output rows.
--   3. Run the apply block IN A TRANSACTION on a maintenance window.
--   4. Verify post-state: should be 0 drifted rows, 0 unbalanced txs.
--
-- Recovery: if anything goes wrong, ROLLBACK while still in the session.
-- Once committed, the only undo is restoring money_account from backup.
-- =============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- PREPARE: build the worklist of (account_id, current_truth, type) to reconcile
-- ─────────────────────────────────────────────────────────────────────────────
DROP TEMPORARY TABLE IF EXISTS _drift_worklist;
CREATE TEMPORARY TABLE _drift_worklist AS
SELECT
    a.account_id,
    a.account_type,
    a.owner_user_id,
    a.owner_nickname,
    a.currency,
    a.is_system,
    a.balance AS old_cache,
    -- Map account_type → which users.<column> is the current truth.
    -- For system pools we keep the existing balance (they don't have a "truth"
    -- column — the spec says these come from net inflows/outflows, but
    -- reconstructing that is out of scope for this one-shot).
    CASE
        WHEN a.account_type = 'PLAYER_VIN'  THEN COALESCE(u.vin,        0)
        WHEN a.account_type = 'PLAYER_XU'   THEN COALESCE(u.xu,         0)
        WHEN a.account_type = 'PLAYER_VP'   THEN COALESCE(u.money_vp,   0)
        WHEN a.account_type = 'PLAYER_SAFE' THEN COALESCE(u.safe,       0)
        ELSE a.balance      -- system pools: leave as-is; reconcile differently below
    END AS truth_balance,
    d.drift AS detected_drift,
    d.computed AS current_ledger_sum
FROM v_money_account_drift d
JOIN money_account a ON a.account_id = d.account_id
LEFT JOIN users u ON u.id = a.owner_user_id;

-- Inspect: this should be ~80 rows.
SELECT account_type, COUNT(*) AS rows_count,
       CAST(SUM(ABS(detected_drift)) AS SIGNED) AS abs_drift
  FROM _drift_worklist GROUP BY account_type ORDER BY abs_drift DESC;

-- Sanity: any case where truth_balance < 0 for a player? Those need manual review,
-- not auto-reconcile (there's a chk_player_nonneg constraint and we'd violate it).
SELECT account_id, account_type, owner_nickname, old_cache, truth_balance
  FROM _drift_worklist
 WHERE is_system = 0 AND truth_balance < 0;
-- If above returns rows: STOP. Fix those manually before proceeding.


-- ─────────────────────────────────────────────────────────────────────────────
-- APPLY: only run after the SELECTs above look right.
-- ─────────────────────────────────────────────────────────────────────────────
START TRANSACTION;

-- Step 1: reset cache to ledger sum (= 0 for the seeded-no-entries cases).
-- The SP we'll call below uses balance_before = current cache and computes
-- balance_after correctly only if cache is in sync with ledger sum.
UPDATE money_account a
JOIN _drift_worklist w ON w.account_id = a.account_id
   SET a.balance = w.current_ledger_sum
 WHERE w.is_system = 0 AND w.truth_balance > 0;

-- Step 2: for each player account, call the SP to post one synthetic
-- OPENING_BALANCE_RECONCILE transaction that brings the account up to truth.
DROP PROCEDURE IF EXISTS _apply_drift_reconcile;
DELIMITER $$
CREATE PROCEDURE _apply_drift_reconcile()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_account_id BIGINT;
    DECLARE v_owner_user_id INT;
    DECLARE v_truth BIGINT;
    DECLARE v_acct_type VARCHAR(40);
    DECLARE v_legacy_acct_id BIGINT;
    DECLARE v_tx_id BIGINT;
    DECLARE v_status VARCHAR(40);
    DECLARE cur CURSOR FOR
        SELECT account_id, owner_user_id, truth_balance, account_type
          FROM _drift_worklist
         WHERE is_system = 0 AND truth_balance > 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    SELECT account_id INTO v_legacy_acct_id
      FROM money_account WHERE account_type = 'LEGACY_RECONCILIATION' LIMIT 1;
    IF v_legacy_acct_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'LEGACY_RECONCILIATION account missing';
    END IF;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_account_id, v_owner_user_id, v_truth, v_acct_type;
        IF done THEN LEAVE read_loop; END IF;

        CALL post_money_transaction(
            'OPENING_BALANCE_RECONCILE',
            CONCAT('drift-fix-2026-05-06-', v_account_id),
            v_owner_user_id,
            CONCAT('drift_fix:', v_acct_type),
            'One-shot drift fix — see migrations/20260506_reconcile_drift_one_shot.sql',
            NULL,
            JSON_OBJECT('source', 'drift-reconcile', 'detected_at', '2026-05-06'),
            JSON_ARRAY(
                JSON_OBJECT('account_id', v_account_id,        'direction', 'CREDIT', 'amount', v_truth, 'note', 'opening balance from users.<col>'),
                JSON_OBJECT('account_id', v_legacy_acct_id,    'direction', 'DEBIT',  'amount', v_truth, 'note', 'paired counterparty')
            ),
            v_tx_id,
            v_status
        );
        IF v_status NOT IN ('POSTED', 'DUPLICATE') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = CONCAT('Reconcile failed for account_id=', v_account_id, ': ', v_status);
        END IF;
    END LOOP;
    CLOSE cur;
END $$
DELIMITER ;

CALL _apply_drift_reconcile();
DROP PROCEDURE _apply_drift_reconcile;

-- Step 3: verify drift is zero before commit. Roll back if not.
SELECT COUNT(*) AS remaining_drift FROM v_money_account_drift;
SELECT COUNT(*) AS unbalanced FROM v_money_unbalanced_transactions;

-- If both above show 0, commit. Otherwise ROLLBACK and investigate.
-- COMMIT;
-- ROLLBACK;

-- ─────────────────────────────────────────────────────────────────────────────
-- POST-CHECK (after COMMIT)
-- ─────────────────────────────────────────────────────────────────────────────
-- SELECT COUNT(*) FROM v_money_account_drift;  -- expect 0 (or 7 if you skipped partials)
-- SELECT * FROM money_transaction WHERE transaction_type = 'OPENING_BALANCE_RECONCILE';
