-- =============================================================================
-- DB REDESIGN ORPHAN SCAN — Phase 0 discovery, read-only
-- =============================================================================
-- Iterates every user/agent ref column across the 4 sunwinkr schemas and
-- counts rows that point to a non-existent users.id / useragent.id.
--
-- Output: one row per {schema, table, column, target} with orphan_count + row_count.
--
-- Run:
--   docker exec -i sunwinkr-mysql mysql --batch -uroot -p$MYSQL_ROOT_PASSWORD \
--     < install/config/mysql/diagnostics/db_orphan_scan.sql > /tmp/orphans.tsv
--
-- Safe: read-only via SELECTs against information_schema + dynamic SELECTs.
-- No writes. No locks beyond metadata read.
--
-- Runtime: scans are COUNT(*) with LEFT JOIN — O(rows) per table. On prod-
-- sized data expect 1–5 min. Run on a replica if possible.
-- =============================================================================

-- Default schema for TEMPORARY table + proc creation (read-only — we never
-- write to vinplay in this script).
USE vinplay;

DROP PROCEDURE IF EXISTS _sun_scan_orphans;

DELIMITER //

CREATE PROCEDURE _sun_scan_orphans()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_schema   VARCHAR(64);
  DECLARE v_table    VARCHAR(64);
  DECLARE v_column   VARCHAR(64);
  DECLARE v_coltype  VARCHAR(64);
  DECLARE v_collate  VARCHAR(64);
  DECLARE v_target   VARCHAR(128);
  DECLARE v_join_sql TEXT;
  DECLARE v_sql      TEXT;

  -- Only scan columns that actually carry a user/agent reference.
  DECLARE cur CURSOR FOR
    SELECT c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME,
           c.COLUMN_TYPE, IFNULL(c.COLLATION_NAME,'')
      FROM information_schema.COLUMNS c
     WHERE c.TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai')
       AND c.COLUMN_NAME IN
           ('user_id','userid','user_name','username','nick_name','nickname',
            'agent_id','agent_user_id','useragent_id',
            'nick_name_send','nick_name_receive','agent_nickname')
       AND c.TABLE_NAME NOT LIKE '\_archive%'
       AND c.TABLE_NAME NOT IN ('users','useragent');

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  DROP TEMPORARY TABLE IF EXISTS _sun_orphan_report;
  CREATE TEMPORARY TABLE _sun_orphan_report (
    schema_name    VARCHAR(64),
    table_name     VARCHAR(64),
    column_name    VARCHAR(64),
    target         VARCHAR(128),
    row_count      BIGINT,
    non_null_count BIGINT,
    orphan_count   BIGINT,
    scan_status    VARCHAR(32),
    note           VARCHAR(255)
  ) ENGINE=MEMORY;

  OPEN cur;
  scan_loop: LOOP
    FETCH cur INTO v_schema, v_table, v_column, v_coltype, v_collate;
    IF done THEN LEAVE scan_loop; END IF;

    -- Map column name → target table + join expression.
    IF v_column IN ('user_id','userid') THEN
      SET v_target   = 'vinplay.users.id';
      SET v_join_sql = CONCAT('LEFT JOIN vinplay.users ref ON ref.id = t.`', v_column, '`');
    ELSEIF v_column IN ('agent_id','agent_user_id','useragent_id') THEN
      SET v_target   = 'vinplay_admin.useragent.id';
      SET v_join_sql = CONCAT('LEFT JOIN vinplay_admin.useragent ref ON ref.id = t.`', v_column, '`');
    -- String joins: convert source to utf8mb3 so it meets the users/useragent
    -- column collation. Source may be utf8mb4 on newer tables; CONVERT() is
    -- lossy only for 4-byte chars (nicknames are ASCII in this fleet).
    ELSEIF v_column IN ('user_name','username') THEN
      SET v_target   = 'vinplay.users.user_name';
      SET v_join_sql = CONCAT('LEFT JOIN vinplay.users ref ON ref.user_name = ',
                              'CONVERT(t.`', v_column, '` USING utf8mb3) COLLATE utf8mb3_general_ci');
    ELSEIF v_column IN ('nick_name','nickname') THEN
      SET v_target   = 'vinplay.users.nick_name';
      SET v_join_sql = CONCAT('LEFT JOIN vinplay.users ref ON ref.nick_name = ',
                              'CONVERT(t.`', v_column, '` USING utf8mb3) COLLATE utf8mb3_general_ci');
    ELSEIF v_column = 'agent_nickname' THEN
      SET v_target   = 'vinplay_admin.useragent.nickname';
      SET v_join_sql = CONCAT('LEFT JOIN vinplay_admin.useragent ref ON ref.nickname = ',
                              'CONVERT(t.`', v_column, '` USING utf8mb3) COLLATE utf8mb3_general_ci');
    ELSEIF v_column = 'nick_name_send' THEN
      SET v_target   = 'vinplay_admin.useragent.nickname (sender)';
      SET v_join_sql = 'LEFT JOIN vinplay_admin.useragent ref ON ref.nickname = CONVERT(t.`nick_name_send` USING utf8mb3) COLLATE utf8mb3_general_ci';
    ELSEIF v_column = 'nick_name_receive' THEN
      SET v_target   = 'vinplay_admin.useragent.nickname (receiver)';
      SET v_join_sql = 'LEFT JOIN vinplay_admin.useragent ref ON ref.nickname = CONVERT(t.`nick_name_receive` USING utf8mb3) COLLATE utf8mb3_general_ci';
    ELSE
      ITERATE scan_loop;
    END IF;

    -- Guarded exec: any error (col doesn't exist after all, perms, etc.) lands
    -- as a skipped row in the report instead of killing the scan.
    -- All join targets (vinplay.users, vinplay_admin.useragent) expose `id`
    -- as their PK. LEFT JOIN produces NULL on any ref column when no match,
    -- so `ref.id IS NULL` is a safe uniform orphan test regardless of which
    -- column we joined on (user_name, nick_name, etc.).
    SET v_sql = CONCAT(
      'INSERT INTO _sun_orphan_report ',
        '(schema_name, table_name, column_name, target, row_count, non_null_count, orphan_count, scan_status, note) ',
      'SELECT ''', v_schema, ''', ''', v_table, ''', ''', v_column, ''', ''', v_target, ''', ',
             'COUNT(*), ',
             'SUM(CASE WHEN t.`', v_column, '` IS NOT NULL AND t.`', v_column, '` <> '''' THEN 1 ELSE 0 END), ',
             'SUM(CASE WHEN t.`', v_column, '` IS NOT NULL AND t.`', v_column, '` <> '''' AND ref.id IS NULL THEN 1 ELSE 0 END), ',
             '''ok'', '''' ',
        'FROM `', v_schema, '`.`', v_table, '` t ',
        v_join_sql);

    -- Wrap in a dynamic exec that tolerates per-table failures.
    -- EXIT handler (not CONTINUE) so a failed PREPARE skips EXECUTE/DEALLOCATE
    -- instead of re-firing on each following statement of the same block.
    BEGIN
      DECLARE EXIT HANDLER FOR SQLEXCEPTION
      BEGIN
        GET DIAGNOSTICS CONDITION 1 @err = MESSAGE_TEXT;
        INSERT INTO _sun_orphan_report
          (schema_name, table_name, column_name, target, row_count, non_null_count, orphan_count, scan_status, note)
          VALUES (v_schema, v_table, v_column, v_target, NULL, NULL, NULL, 'error', SUBSTRING(@err,1,250));
      END;

      SET @dyn = v_sql;
      PREPARE stmt FROM @dyn;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
    END;

  END LOOP;
  CLOSE cur;

  -- Primary output: orphan detail.
  SELECT '==R6==orphan_scan==' AS section;
  SELECT *
    FROM _sun_orphan_report
   ORDER BY (orphan_count IS NULL), orphan_count DESC,
            schema_name, table_name, column_name;

  -- Aggregate rollup.
  SELECT '==R7==orphan_summary==' AS section;
  SELECT schema_name,
         COUNT(*)                                        AS cols_scanned,
         SUM(CASE WHEN orphan_count > 0 THEN 1 ELSE 0 END) AS cols_with_orphans,
         IFNULL(SUM(orphan_count), 0)                    AS total_orphan_rows,
         SUM(CASE WHEN scan_status = 'error' THEN 1 ELSE 0 END) AS scan_errors
    FROM _sun_orphan_report
   GROUP BY schema_name
   ORDER BY schema_name;

  DROP TEMPORARY TABLE _sun_orphan_report;
END //

DELIMITER ;

CALL _sun_scan_orphans();

DROP PROCEDURE _sun_scan_orphans;
