-- SUN-13xx Phase 4 (early-applied) — drop legacy SP + vin_total / xu_total columns
--
-- Sequencing on staging:
-- 1. Phase 0 hardening live; drift = 0
-- 2. Phase 1 flag UNIFIED_WALLET_PHASE_1=on flipped (legacy SP no longer called)
-- 3. systemRecoveryReset() reduced to freeze_money clear only
-- 4. All Java readers of rs.getInt/Long("vin_total"/"xu_total") replaced with 0/0L
-- 5. THIS migration applied
USE vinplay;

DROP PROCEDURE IF EXISTS update_money_db;

ALTER TABLE users
    DROP COLUMN vin_total,
    DROP COLUMN xu_total;
