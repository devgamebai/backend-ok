-- =============================================================
-- Signing Bonus Wagering Migration
-- Add wagering fields to signing bonus config
-- =============================================================

ALTER TABLE tbl_signing_bonus_config
    ADD COLUMN wager_enabled TINYINT NOT NULL DEFAULT 1
        COMMENT '1=apply wagering before withdrawal, 0=disable',
    ADD COLUMN wager_multiplier DECIMAL(10,2) NOT NULL DEFAULT 3.00
        COMMENT 'Required volume = bonus_amount * wager_multiplier';

UPDATE tbl_signing_bonus_config
SET wager_enabled = 1,
    wager_multiplier = 3.00
WHERE wager_enabled IS NULL OR wager_multiplier IS NULL;
