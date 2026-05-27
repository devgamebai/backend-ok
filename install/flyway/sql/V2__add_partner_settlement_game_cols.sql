DROP PROCEDURE IF EXISTS _sun_add_log_cols;
DELIMITER //
CREATE PROCEDURE _sun_add_log_cols()
BEGIN
  DECLARE has_col INT DEFAULT 0;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='sam';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN sam BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='sam_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN sam_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='binh';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN binh BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='binh_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN binh_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='tala';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN tala BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='tala_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN tala_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='lieng';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN lieng BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='lieng_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN lieng_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='xito';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN xito BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='xito_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN xito_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='baicao';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN baicao BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='baicao_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN baicao_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='poker';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN poker BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='poker_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN poker_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='xidzach';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN xidzach BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='xidzach_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN xidzach_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='hamcamap';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN hamcamap BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='hamcamap_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN hamcamap_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='taixiu_sicbo';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN taixiu_sicbo BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='taixiu_sicbo_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN taixiu_sicbo_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='over_under';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN over_under BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='over_under_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN over_under_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='samtruyen';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN samtruyen BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='samtruyen_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN samtruyen_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='range_rover';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN range_rover BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='range_rover_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN range_rover_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='sexygirl';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN sexygirl BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='sexygirl_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN sexygirl_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='lode';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN lode BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='lode_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_report_user ADD COLUMN lode_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='sam';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN sam BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='sam_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN sam_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='binh';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN binh BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='binh_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN binh_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='tala';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN tala BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='tala_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN tala_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='lieng';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN lieng BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='lieng_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN lieng_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='xito';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN xito BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='xito_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN xito_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='baicao';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN baicao BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='baicao_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN baicao_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='poker';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN poker BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='poker_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN poker_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='xidzach';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN xidzach BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='xidzach_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN xidzach_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='hamcamap';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN hamcamap BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='hamcamap_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN hamcamap_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='taixiu_sicbo';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN taixiu_sicbo BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='taixiu_sicbo_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN taixiu_sicbo_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='over_under';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN over_under BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='over_under_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN over_under_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='samtruyen';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN samtruyen BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='samtruyen_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN samtruyen_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='range_rover';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN range_rover BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='range_rover_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN range_rover_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='sexygirl';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN sexygirl BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='sexygirl_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN sexygirl_win BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='lode';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN lode BIGINT DEFAULT 0;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='lode_win';
  IF has_col = 0 THEN
    ALTER TABLE vinplay.log_count_user_play ADD COLUMN lode_win BIGINT DEFAULT 0;
  END IF;

END //
DELIMITER ;
CALL _sun_add_log_cols();
DROP PROCEDURE IF EXISTS _sun_add_log_cols;
