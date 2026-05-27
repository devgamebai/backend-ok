-- SUN-1205/1206 follow-up: extend gsc_wager_drift_audit.status to
-- cover the deferred-post model (rebate created from GSC truth at
-- settle, instead of corrected after BET). New values:
--   - POSTED         : rebate created with GSC valid_bet_amount > 0
--   - SKIP_ZERO_BET  : full hedge, valid_bet=0, no rebate posted
-- Keep the prior values so any rows from the earlier reverse+recreate
-- attempt remain readable for audit purposes.

ALTER TABLE vinplay.gsc_wager_drift_audit
  MODIFY COLUMN status
    ENUM('NO_DRIFT','CORRECTED','RECON_FAILED','POSTED','SKIP_ZERO_BET') NOT NULL;
