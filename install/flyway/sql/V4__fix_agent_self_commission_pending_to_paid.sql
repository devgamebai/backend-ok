-- ============================================================================
-- Migration: Fix Agent SELF Commission PENDING → PAID
-- Date: 2026-04-24
-- Description:
--   Agent SELF commission records (rebate_type='SELF') created by
--   triggerAutoCommission (note starts with 'AUTO_COMMISSION') were
--   previously inserted as PENDING. The code has been updated to insert
--   them as PAID with instant credit to agency_wallet.
--
--   This migration:
--   1. Credits agency_wallet for each affected agent (SUM of pending SELF amounts)
--   2. Updates the rebate_logs status from PENDING to PAID
--
--   Player cashback (from RealTimeCommission, note='Cashback ...') is NOT touched.
-- ============================================================================

-- Step 1: Credit agency_wallet for all agents with PENDING SELF AUTO_COMMISSION rows.
-- Uses INSERT ... ON DUPLICATE KEY UPDATE to handle agents with/without existing wallet.
INSERT INTO vinplay.agency_wallet (agent_id, balance, updated_at)
SELECT
    agent_user_id,
    SUM(rebate_amount),
    CURRENT_TIMESTAMP
FROM vinplay.rebate_logs
WHERE rebate_type = 'SELF'
  AND status = 'PENDING'
  AND note LIKE 'AUTO_COMMISSION%'
  AND rebate_amount > 0
GROUP BY agent_user_id
ON DUPLICATE KEY UPDATE
    balance = balance + VALUES(balance),
    updated_at = CURRENT_TIMESTAMP;

-- Step 2: Log wallet transactions for audit trail.
INSERT INTO vinplay.agency_wallet_transactions
    (agent_id, agent_nickname, type, amount, direction, balance_after, related_user, game_action, note)
SELECT
    rl.agent_user_id,
    rl.agent_nickname,
    'COMMISSION_SELF',
    SUM(rl.rebate_amount),
    'CREDIT',
    COALESCE(aw.balance, 0),
    NULL,
    NULL,
    CONCAT('Migration: ', COUNT(*), ' PENDING SELF rows credited. IDs: ',
           GROUP_CONCAT(rl.id ORDER BY rl.id SEPARATOR ','))
FROM vinplay.rebate_logs rl
LEFT JOIN vinplay.agency_wallet aw ON aw.agent_id = rl.agent_user_id
WHERE rl.rebate_type = 'SELF'
  AND rl.status = 'PENDING'
  AND rl.note LIKE 'AUTO_COMMISSION%'
  AND rl.rebate_amount > 0
GROUP BY rl.agent_user_id, rl.agent_nickname;

-- Step 3: Mark all affected rows as PAID.
UPDATE vinplay.rebate_logs
SET status = 'PAID',
    approved_by = 'SYSTEM_MIGRATION',
    approved_at = NOW()
WHERE rebate_type = 'SELF'
  AND status = 'PENDING'
  AND note LIKE 'AUTO_COMMISSION%';
