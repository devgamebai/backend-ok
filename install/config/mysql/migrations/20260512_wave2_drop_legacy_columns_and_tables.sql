-- SUN-13xx Wave-2 schema slimming — drop verified-dead columns + one-shot tables
--
-- Methodology
-- -----------
-- Wave-1 (already applied) removed the bulk money columns: xu, vin_total,
-- xu_total, safe, vip_point_total, money_vp, recharge_money, gift_total, and
-- cgame.users.cash/cash_safe/cash_silver.
--
-- Wave-2 targets columns + tables that were left behind by superseded
-- features (RBAC v2 / SUN-735 commission split / SUN-666 VinCard removal /
-- SUN-1086 wallet drift backfill / SUN-1250 AWC wager_code backfill) and
-- one-shot migration artifacts. Every drop in this file was triple-verified:
--   1. Zero refs in active Java / C# / PHP / Node code
--   2. Zero refs in live stored procedures / triggers
--   3. Not a PK / UK / referenced FK column
--   4. Table row count is either 0 or one-shot import scratch already drained
--
-- Items rejected for drop (kept to avoid future re-investigation):
--   - commission_rate_policy.per_game_pool   (live trigger — phase0c guard)
--   - useragent.percent_bonus_vincard         (9 live agent processors)
--   - lucky_rotation.rotate_daily / rotate_in_day / rotate_time
--                                              (3 live SPs: lucky_get_rotate_count,
--                                               lucky_receive_rotate_daily,
--                                               save_result_lucky)
--   - vinplay_admin.action_admin / source_giftcode / price_giftcode /
--     groupuser / rolemenu                     (live admin RBAC / giftcode processors)
--
-- Apply order matters only inside vinplay (the wallet snapshot tables and
-- artifact log are independent; cgame archives are isolated; admin
-- migrations table is isolated). The whole file is idempotent — every
-- DROP uses IF EXISTS.
--
-- Recovery: run /root/sunwinkr/sunwinkr/install/config/mysql/db/full_backup.sql
-- restore to repopulate any accidentally-dropped row. None of the items in
-- this file carry live business state.

-- =====================================================================
-- 1. vinplay.users — VP receive flag + unused quota field
-- =====================================================================
USE vinplay;

ALTER TABLE users
    DROP COLUMN vp_lv_receive,
    DROP COLUMN manual_quota;

-- =====================================================================
-- 2. vinplay.money_transaction — never-written reversal timestamp
-- =====================================================================
ALTER TABLE money_transaction
    DROP COLUMN reversed_at;

-- =====================================================================
-- 3. vinplay_admin.useragent — SUN-735 wallet_balance dead column +
--    duplicate path_ancestors (canonical is `ancestors`)
-- =====================================================================
USE vinplay_admin;

ALTER TABLE useragent
    DROP COLUMN path_ancestors,
    DROP COLUMN wallet_balance;

-- =====================================================================
-- 4. cgame schema — fully-drained legacy archives (Wave-1 dropped the
--    cash columns; these five tables are zero-row leftovers)
-- =====================================================================
DROP TABLE IF EXISTS cgame.card;
DROP TABLE IF EXISTS cgame.daily;
DROP TABLE IF EXISTS cgame.daily_cp;
DROP TABLE IF EXISTS cgame.daily_game;
DROP TABLE IF EXISTS cgame.minigame;

-- =====================================================================
-- 5. vinplay — one-shot migration artifact tables
--    SUN-1086  — rebate backfill scratch       (17 rows)
--    SUN-1250  — AWC round_id import buffer    (2817 rows, scratch)
--    SUN-13xx  — wallet Phase 3a error log     (0 rows)
--    SUN-13xx  — wallet drift snapshot         (0 rows)
--    legacy    — settings_change_log           (0 rows, never written)
--    legacy    — commission_history_outbox     (0 rows, never written)
--    legacy    — ops_event_log                 (2 rows, never read)
-- =====================================================================
DROP TABLE IF EXISTS vinplay.awc_round_map;
DROP TABLE IF EXISTS vinplay.rebate_logs_backfill_sun_1086;
DROP TABLE IF EXISTS vinplay._wallet_phase3a_errors;
DROP TABLE IF EXISTS vinplay.wallet_drift_snapshot;
DROP TABLE IF EXISTS vinplay.settings_change_log;
DROP TABLE IF EXISTS vinplay.commission_history_outbox;
DROP TABLE IF EXISTS vinplay.ops_event_log;

-- vinplay._wallet_phase3a_pre_snapshot (96 rows) is INTENTIONALLY kept
-- as a read-only audit of pre-cutover wallet state. Re-evaluate after
-- the wallet unification is past its 7-day soak window.

-- =====================================================================
-- 6. vinplay_admin — pre-Flyway CodeIgniter migration tracker + one-shot
--    SUN-1xxx nickname-mismatch report row
-- =====================================================================
DROP TABLE IF EXISTS vinplay_admin.migrations;
DROP TABLE IF EXISTS vinplay_admin.useragent_nickname_mismatch_20260501;

-- =====================================================================
-- Sanity probe — list any of the dropped objects that survived (should
-- return zero rows after a clean run)
-- =====================================================================
SELECT 'col survivor' AS kind, TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME
  FROM information_schema.COLUMNS
 WHERE (TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME) IN (
        ('vinplay',       'users',             'vp_lv_receive'),
        ('vinplay',       'users',             'manual_quota'),
        ('vinplay',       'money_transaction', 'reversed_at'),
        ('vinplay_admin', 'useragent',         'path_ancestors'),
        ('vinplay_admin', 'useragent',         'wallet_balance')
   )
UNION ALL
SELECT 'tbl survivor' AS kind, TABLE_SCHEMA, TABLE_NAME, NULL
  FROM information_schema.TABLES
 WHERE (TABLE_SCHEMA, TABLE_NAME) IN (
        ('cgame',         'card'),
        ('cgame',         'daily'),
        ('cgame',         'daily_cp'),
        ('cgame',         'daily_game'),
        ('cgame',         'minigame'),
        ('vinplay',       'awc_round_map'),
        ('vinplay',       'rebate_logs_backfill_sun_1086'),
        ('vinplay',       '_wallet_phase3a_errors'),
        ('vinplay',       'wallet_drift_snapshot'),
        ('vinplay',       'settings_change_log'),
        ('vinplay',       'commission_history_outbox'),
        ('vinplay',       'ops_event_log'),
        ('vinplay_admin', 'migrations'),
        ('vinplay_admin', 'useragent_nickname_mismatch_20260501')
   );
