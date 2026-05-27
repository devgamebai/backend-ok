-- Rollback for V3__rename_gsc_is_available_to_active.sql
-- Renames active → is_available.

DROP PROCEDURE IF EXISTS _sun_unrename_gsc_is_available;
DELIMITER //
CREATE PROCEDURE _sun_unrename_gsc_is_available()
BEGIN
  DECLARE has_new INT DEFAULT 0;
  DECLARE has_old INT DEFAULT 0;

  SELECT COUNT(*) INTO has_new FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='gsc_game_catalog'
      AND COLUMN_NAME='active';
  SELECT COUNT(*) INTO has_old FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='gsc_game_catalog'
      AND COLUMN_NAME='is_available';

  IF has_new = 1 AND has_old = 0 THEN
    ALTER TABLE vinplay.gsc_game_catalog
      CHANGE COLUMN active is_available TINYINT(1) NOT NULL DEFAULT 1;
  END IF;
END //
DELIMITER ;

CALL _sun_unrename_gsc_is_available();
DROP PROCEDURE _sun_unrename_gsc_is_available;

-- CRITICAL: after running this rollback, revert the Java source to the
-- pre-#39 SELECT list + restart portal-api. Otherwise the Java reader
-- queries `active` against a column now named `is_available` → rs.getInt
-- throws SQLException → all c=3091 responses miss the flag.

-- docker restart sunwinkr-portal-api
