DROP PROCEDURE IF EXISTS _sun_drop_log_cols;
DELIMITER //
CREATE PROCEDURE _sun_drop_log_cols()
BEGIN
  DECLARE has_col INT DEFAULT 0;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='sam';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN sam;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='sam_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN sam_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='binh';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN binh;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='binh_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN binh_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='tala';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN tala;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='tala_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN tala_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='lieng';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN lieng;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='lieng_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN lieng_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='xito';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN xito;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='xito_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN xito_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='baicao';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN baicao;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='baicao_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN baicao_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='poker';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN poker;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='poker_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN poker_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='xidzach';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN xidzach;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='xidzach_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN xidzach_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='hamcamap';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN hamcamap;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='hamcamap_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN hamcamap_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='taixiu_sicbo';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN taixiu_sicbo;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='taixiu_sicbo_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN taixiu_sicbo_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='over_under';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN over_under;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='over_under_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN over_under_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='samtruyen';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN samtruyen;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='samtruyen_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN samtruyen_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='range_rover';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN range_rover;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='range_rover_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN range_rover_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='sexygirl';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN sexygirl;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='sexygirl_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN sexygirl_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='lode';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN lode;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_report_user' AND COLUMN_NAME='lode_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_report_user DROP COLUMN lode_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='sam';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN sam;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='sam_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN sam_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='binh';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN binh;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='binh_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN binh_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='tala';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN tala;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='tala_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN tala_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='lieng';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN lieng;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='lieng_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN lieng_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='xito';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN xito;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='xito_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN xito_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='baicao';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN baicao;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='baicao_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN baicao_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='poker';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN poker;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='poker_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN poker_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='xidzach';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN xidzach;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='xidzach_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN xidzach_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='hamcamap';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN hamcamap;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='hamcamap_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN hamcamap_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='taixiu_sicbo';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN taixiu_sicbo;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='taixiu_sicbo_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN taixiu_sicbo_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='over_under';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN over_under;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='over_under_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN over_under_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='samtruyen';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN samtruyen;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='samtruyen_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN samtruyen_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='range_rover';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN range_rover;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='range_rover_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN range_rover_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='sexygirl';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN sexygirl;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='sexygirl_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN sexygirl_win;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='lode';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN lode;
  END IF;

  SELECT COUNT(*) INTO has_col FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='vinplay' AND TABLE_NAME='log_count_user_play' AND COLUMN_NAME='lode_win';
  IF has_col > 0 THEN
    ALTER TABLE vinplay.log_count_user_play DROP COLUMN lode_win;
  END IF;

END //
DELIMITER ;
CALL _sun_drop_log_cols();
DROP PROCEDURE IF EXISTS _sun_drop_log_cols;
