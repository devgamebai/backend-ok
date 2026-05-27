-- =============================================================================
-- DB REDESIGN INVENTORY — Phase 0 discovery, read-only
-- =============================================================================
-- Emits 5 result sets across the 4 sunwinkr schemas. Run via:
--   docker exec -i sunwinkr-mysql mysql --batch -uroot -p$MYSQL_ROOT_PASSWORD \
--     < install/config/mysql/diagnostics/db_inventory.sql > /tmp/inventory.tsv
--
-- Result sets (each starts with a sentinel header row so a grep can split):
--   R1  table_inventory           — one row per table (rows, engine, collation)
--   R2  userref_columns           — every user/agent ref column + FK status
--   R3  collation_outliers        — columns not utf8mb3_general_ci
--   R4  fk_summary                — FK to users/useragent by DELETE_RULE
--   R5  remaining_gap_tables      — user/agent ref columns WITHOUT FK
--
-- Phase 0-5 redesign already applied (see 2026_04_19…phase5). This script
-- proves the floor and flags whatever leaked in since.
-- =============================================================================

SELECT '==R1==table_inventory==' AS section;

SELECT t.TABLE_SCHEMA                  AS schema_name,
       t.TABLE_NAME                    AS table_name,
       t.ENGINE                        AS engine,
       t.TABLE_ROWS                    AS est_rows,
       t.TABLE_COLLATION               AS table_collation,
       t.CREATE_TIME                   AS created_at,
       t.UPDATE_TIME                   AS updated_at
  FROM information_schema.TABLES t
 WHERE t.TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai')
   AND t.TABLE_TYPE = 'BASE TABLE'
   AND t.TABLE_NAME NOT LIKE '\_archive%'
 ORDER BY t.TABLE_SCHEMA, t.TABLE_NAME;


SELECT '==R2==userref_columns==' AS section;

-- Every column across the 4 schemas that looks like a user/agent reference,
-- annotated with whether any FK on this column already exists.
SELECT c.TABLE_SCHEMA                                                        AS schema_name,
       c.TABLE_NAME                                                          AS table_name,
       c.COLUMN_NAME                                                         AS column_name,
       c.COLUMN_TYPE                                                         AS column_type,
       c.IS_NULLABLE                                                         AS nullable,
       c.COLLATION_NAME                                                      AS collation_name,
       IFNULL(kcu.REFERENCED_TABLE_SCHEMA, '')                               AS ref_schema,
       IFNULL(kcu.REFERENCED_TABLE_NAME,   '')                               AS ref_table,
       IFNULL(kcu.REFERENCED_COLUMN_NAME,  '')                               AS ref_column,
       IFNULL(rc.DELETE_RULE, '')                                            AS on_delete,
       IFNULL(rc.UPDATE_RULE, '')                                            AS on_update,
       CASE WHEN kcu.REFERENCED_TABLE_NAME IS NOT NULL THEN 1 ELSE 0 END     AS has_fk
  FROM information_schema.COLUMNS c
  LEFT JOIN information_schema.KEY_COLUMN_USAGE kcu
    ON  kcu.TABLE_SCHEMA = c.TABLE_SCHEMA
    AND kcu.TABLE_NAME   = c.TABLE_NAME
    AND kcu.COLUMN_NAME  = c.COLUMN_NAME
    AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
  LEFT JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON  rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
    AND rc.CONSTRAINT_NAME   = kcu.CONSTRAINT_NAME
 WHERE c.TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai')
   AND c.COLUMN_NAME IN
       ('user_id','userid','user_name','username','nick_name','nickname',
        'agent_id','agent_user_id','useragent_id','receiver_id','sender_id',
        'nick_name_send','nick_name_receive','agent_nickname')
   AND c.TABLE_NAME NOT LIKE '\_archive%'
   AND c.TABLE_NAME NOT IN ('users','useragent')
 ORDER BY c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME;


SELECT '==R3==collation_outliers==' AS section;

-- Anything not on the fleet standard (utf8mb3_general_ci).
-- Phase 5 cleaned the known outliers; anything here is drift since then.
SELECT c.TABLE_SCHEMA    AS schema_name,
       c.TABLE_NAME      AS table_name,
       c.COLUMN_NAME     AS column_name,
       c.DATA_TYPE       AS data_type,
       c.CHARACTER_SET_NAME AS charset,
       c.COLLATION_NAME  AS collation_name
  FROM information_schema.COLUMNS c
 WHERE c.TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai')
   AND c.COLLATION_NAME IS NOT NULL
   AND c.COLLATION_NAME <> 'utf8mb3_general_ci'
   AND c.TABLE_NAME NOT LIKE '\_archive%'
 ORDER BY c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME;


SELECT '==R4==fk_summary==' AS section;

-- Bucket every FK that targets users / useragent by its ON DELETE rule.
-- Goal after phase 5: everything CASCADE or SET NULL; zero RESTRICT / NO ACTION.
SELECT kcu.REFERENCED_TABLE_SCHEMA AS ref_schema,
       kcu.REFERENCED_TABLE_NAME   AS ref_table,
       rc.DELETE_RULE              AS on_delete,
       rc.UPDATE_RULE              AS on_update,
       COUNT(*)                    AS fk_count,
       GROUP_CONCAT(CONCAT(kcu.TABLE_SCHEMA, '.', kcu.TABLE_NAME)
                    ORDER BY kcu.TABLE_SCHEMA, kcu.TABLE_NAME SEPARATOR '|')
                                   AS child_tables
  FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON  rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
    AND rc.CONSTRAINT_NAME   = kcu.CONSTRAINT_NAME
 WHERE kcu.REFERENCED_TABLE_SCHEMA IN ('vinplay','vinplay_admin')
   AND kcu.REFERENCED_TABLE_NAME   IN ('users','useragent')
 GROUP BY kcu.REFERENCED_TABLE_SCHEMA, kcu.REFERENCED_TABLE_NAME,
          rc.DELETE_RULE, rc.UPDATE_RULE
 ORDER BY ref_schema, ref_table, on_delete;


SELECT '==R5==remaining_gap_tables==' AS section;

-- Tables whose user/agent ref column has NO FK — classified so reviewers can
-- tell a *real* gap (no FK anywhere on the table) from *display denorm*
-- (nick_name/user_name kept alongside a properly FK'd user_id on same row).
--
-- gap_class values:
--   true_gap  — no FK on this table to users OR useragent at all
--   denorm    — table already has a user_id/agent_id FK; this col is for display
SELECT c.TABLE_SCHEMA                             AS schema_name,
       c.TABLE_NAME                               AS table_name,
       c.COLUMN_NAME                              AS column_name,
       c.COLUMN_TYPE                              AS column_type,
       c.COLLATION_NAME                           AS collation_name,
       CASE
         WHEN EXISTS (
              SELECT 1
                FROM information_schema.KEY_COLUMN_USAGE kcu2
                JOIN information_schema.REFERENTIAL_CONSTRAINTS rc2
                  ON rc2.CONSTRAINT_SCHEMA = kcu2.CONSTRAINT_SCHEMA
                 AND rc2.CONSTRAINT_NAME   = kcu2.CONSTRAINT_NAME
               WHERE kcu2.TABLE_SCHEMA = c.TABLE_SCHEMA
                 AND kcu2.TABLE_NAME   = c.TABLE_NAME
                 AND kcu2.REFERENCED_TABLE_SCHEMA IN ('vinplay','vinplay_admin')
                 AND kcu2.REFERENCED_TABLE_NAME   IN ('users','useragent'))
         THEN 'denorm'
         ELSE 'true_gap'
       END                                        AS gap_class,
       CASE
         WHEN c.COLUMN_NAME IN ('user_id','userid')                        THEN 'vinplay.users(id)'
         WHEN c.COLUMN_NAME IN ('agent_id','agent_user_id','useragent_id') THEN 'vinplay_admin.useragent(id)'
         WHEN c.COLUMN_NAME IN ('user_name','username')                    THEN 'vinplay.users(user_name) — backfill to user_id first'
         WHEN c.COLUMN_NAME IN ('nick_name','nickname',
                                'nick_name_send','nick_name_receive',
                                'agent_nickname')                          THEN 'vinplay.users(nick_name) or useragent(nickname) — backfill first'
         ELSE ''
       END                                        AS suggested_target
  FROM information_schema.COLUMNS c
 WHERE c.TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai')
   AND c.COLUMN_NAME IN
       ('user_id','userid','user_name','username','nick_name','nickname',
        'agent_id','agent_user_id','useragent_id','receiver_id','sender_id',
        'nick_name_send','nick_name_receive','agent_nickname')
   AND c.TABLE_NAME NOT LIKE '\_archive%'
   AND c.TABLE_NAME NOT IN ('users','useragent')
   AND NOT EXISTS (
         SELECT 1
           FROM information_schema.KEY_COLUMN_USAGE kcu
          WHERE kcu.TABLE_SCHEMA = c.TABLE_SCHEMA
            AND kcu.TABLE_NAME   = c.TABLE_NAME
            AND kcu.COLUMN_NAME  = c.COLUMN_NAME
            AND kcu.REFERENCED_TABLE_NAME IS NOT NULL)
 ORDER BY
   -- true_gap first (real phase-6 work), then denorm
   CASE WHEN EXISTS (
          SELECT 1
            FROM information_schema.KEY_COLUMN_USAGE kcu3
           WHERE kcu3.TABLE_SCHEMA = c.TABLE_SCHEMA
             AND kcu3.TABLE_NAME   = c.TABLE_NAME
             AND kcu3.REFERENCED_TABLE_SCHEMA IN ('vinplay','vinplay_admin')
             AND kcu3.REFERENCED_TABLE_NAME   IN ('users','useragent')) THEN 1 ELSE 0 END,
   c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME;
