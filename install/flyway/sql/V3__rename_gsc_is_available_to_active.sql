-- GitLab #39 — complete the `is_available` → `active` rename.
--
-- JSON contract already flipped in commit `abdd341e` (c=3091 emits
-- `active`, not `is_available`). This migration renames the underlying
-- column so readers + migrations align with the public contract name.
--
-- Tables affected:
--   vinplay.gsc_game_catalog.is_available  →  active
--
-- After this migration runs:
--   - GSCGameListProcessor.java SELECT list uses `active`
--   - Historical SQL migrations in install/config/mysql/changes/ keep
--     their own references to `is_available` — they already ran at a
--     time when the column had that name; re-running them against a
--     DB with the renamed column WILL fail. That's acceptable: those
--     scripts are one-shot historical artifacts, not repeatable.
--
-- Rollback: install/flyway/rollbacks/V3__rename_gsc_is_available_to_active-rollback.sql

-- Guard: do NOT rename if the column has already been renamed on this
-- DB (i.e. Flyway version already past this migration but someone
-- manually re-applied, or the migration file was copied to another
-- project). Skip rename if `active` already exists.

DROP PROCEDURE IF EXISTS _sun_rename_gsc_is_available;
DELIMITER //
CREATE PROCEDURE _sun_rename_gsc_is_available()
BEGIN
  DECLARE has_old INT DEFAULT 0;
  DECLARE has_new INT DEFAULT 0;

  SELECT COUNT(*) INTO has_old FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='gsc_game_catalog'
      AND COLUMN_NAME='is_available';

  SELECT COUNT(*) INTO has_new FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='gsc_game_catalog'
      AND COLUMN_NAME='active';

  IF has_old = 1 AND has_new = 0 THEN
    ALTER TABLE vinplay.gsc_game_catalog
      CHANGE COLUMN is_available active TINYINT(1) NOT NULL DEFAULT 1;
  END IF;
  -- Both present → someone added the new column before this migration
  -- ran. Skip rename; operator must reconcile manually.
  -- Neither present → migration is wrong or table doesn't exist. Skip.
END //
DELIMITER ;

CALL _sun_rename_gsc_is_available();
DROP PROCEDURE _sun_rename_gsc_is_available;

-- Sanity check
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='gsc_game_catalog'
  AND COLUMN_NAME IN ('active','is_available');
-- Expected: one row for `active`, zero rows for `is_available`.

-- Restart portal-api after this migration applies to pick up the
-- updated GSCGameListProcessor SELECT list and bust the 5-min lobby
-- cache:
--   docker restart sunwinkr-portal-api
