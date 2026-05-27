-- SUN-1205/1206: audit table for the GscWagerReconciler drift-correction pass.
--
-- Background: Dream Gaming's seamless-wallet `withdraw` push sends
-- `valid_bet_amount = bet_amount` at BET time as a placeholder. The
-- post-hedge truth (e.g. valid_bet=0 on a Banker+Player full hedge that
-- ends in draw) is only available via the GSC wager-detail PULL API
-- after settlement. Reading the placeholder at BET time means we
-- over-pay commission on hedge bets.
--
-- The reconciler runs every N minutes, pulls wager-detail for each
-- recent rebate_logs candidate, and if it finds drift between
-- recorded `total_f1_volume` and GSC `valid_bet_amount`, reverses the
-- over-paid rebate rows (SUN-1182 helper) and re-creates them at the
-- corrected volume.
--
-- This audit table is the idempotency guard (one row per wager) and
-- gives ops a way to see drift volume + corrected volume at a glance.

CREATE TABLE IF NOT EXISTS vinplay.gsc_wager_drift_audit (
  wager_code        VARCHAR(128) NOT NULL,
  product_code      INT NOT NULL,
  player_nickname   VARCHAR(64) DEFAULT NULL,
  game_action       VARCHAR(64) DEFAULT NULL,
  recorded_volume   BIGINT NOT NULL DEFAULT 0,
  gsc_valid_bet     BIGINT NOT NULL DEFAULT 0,
  drift_amount      BIGINT GENERATED ALWAYS AS (recorded_volume - gsc_valid_bet) VIRTUAL,
  status            ENUM('NO_DRIFT','CORRECTED','RECON_FAILED') NOT NULL,
  reversed_rows     INT NOT NULL DEFAULT 0,
  note              VARCHAR(255) DEFAULT NULL,
  checked_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (wager_code),
  KEY idx_product_status (product_code, status),
  KEY idx_checked_at (checked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
