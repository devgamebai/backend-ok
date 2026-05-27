-- SUN-980 — per-game commission-eligibility flag on gsc_game_catalog.
--
-- Replaces the hardcoded product_code list (1009 CQ9, 1091 JILI) in
-- game/thirdParty/.../WithdrawProcess.java with a DB-driven flag.
-- Seamless fishing games emit a single /seamless/withdraw = wallet
-- transfer instead of per-shot bet webhooks, so commission on them
-- would have to ride on balance (wrong) or be zero (current fix).
-- Flipping this flag on any future seamless-transfer game disables
-- commission at the source — no code change, no redeploy.
--
-- Default 1 (eligible) — every existing game keeps its current behaviour
-- the moment this ALTER lands. The seed UPDATE below flips 0 only on the
-- known-fishing providers.
--
-- Idempotent: INFORMATION_SCHEMA guard lets this migration re-run anywhere.

SET @has_col := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'vinplay'
      AND TABLE_NAME   = 'gsc_game_catalog'
      AND COLUMN_NAME  = 'commission_eligible'
);

SET @sql := IF(@has_col = 0,
    'ALTER TABLE vinplay.gsc_game_catalog
       ADD COLUMN commission_eligible TINYINT(1) NOT NULL DEFAULT 1 AFTER sort_order,
       ADD KEY idx_commission_eligible (commission_eligible)',
    'SELECT ''commission_eligible already exists, skipping'' AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
