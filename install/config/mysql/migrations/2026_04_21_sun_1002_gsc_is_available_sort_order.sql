-- SUN-1002 follow-up (GitLab issue #17).
--
-- The GSCGameListProcessor java shipped in commit c460b428 reads these
-- columns from vinplay.gsc_game_catalog. The migration was never
-- authored. Live staging schema acquired the columns out-of-band; prod
-- has not and silently falls back to is_available=1 / sort_order=0 for
-- every game because GSCGameListProcessor swallows the SQL error.
--
-- DEFAULT 1 for is_available means every existing game stays visible
-- the moment the columns are added — zero behaviour change until a
-- subsequent UPDATE explicitly hides something (see SUN-965).
--
-- Idempotent: guards on INFORMATION_SCHEMA so re-running on a host that
-- already has the columns (e.g. staging) is a no-op.

SET @has_is_available := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'vinplay'
      AND TABLE_NAME   = 'gsc_game_catalog'
      AND COLUMN_NAME  = 'is_available'
);

SET @sql := IF(@has_is_available = 0,
    'ALTER TABLE vinplay.gsc_game_catalog
       ADD COLUMN is_available TINYINT(1) NOT NULL DEFAULT 1 AFTER image_url,
       ADD COLUMN sort_order   INT         NOT NULL DEFAULT 0 AFTER is_available,
       ADD KEY idx_is_available (is_available)',
    'SELECT ''is_available + sort_order already exist, skipping'' AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
