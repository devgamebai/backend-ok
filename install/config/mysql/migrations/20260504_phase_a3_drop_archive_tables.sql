-- Phase A3 — drop dead archive tables
-- Date: 2026-05-04
-- Audit: docs/db-audit/AUDIT_PHASE1_FINDINGS.md
--
-- Background:
--   2026_04_19_db_redesign_phase0_drop_legacy.sql RENAMEd live tables to
--   _archive_* prefix as a quarantine step. ~30+ days have passed with:
--     * 0 row writes
--     * 0 code references (verified across backend-master, www, sunkr-admin,
--       sunkr-admin-next, sunkr-agency, sunkr-nextagency)
--     * 0 application errors logged against these names
--   Quarantine done. Drop.
--
-- Skipped:
--   * useragent_nickname_mismatch_20260501 — recent (1 row, 2026-05-01),
--     diagnostic snapshot still being reviewed.
--
-- Rollback: restore from full_backup.sql (these are RENAMEd originals,
-- not new tables — the original DDL lives in V1__baseline.sql).

-- vinplay schema
DROP TABLE IF EXISTS vinplay._archive_sun1060_catalog_category_20260423;
DROP TABLE IF EXISTS vinplay._archive_sun1060_catalog_category_20260424;
DROP TABLE IF EXISTS vinplay._archive_tbl_slot_win_rate;
DROP TABLE IF EXISTS vinplay._archive_user_value;
DROP TABLE IF EXISTS vinplay._archive_users_in_game;

-- vinplay_admin schema
DROP TABLE IF EXISTS vinplay_admin._archive_access_link;

-- vinplay_minigame schema
DROP TABLE IF EXISTS vinplay_minigame._archive_chatbox;
DROP TABLE IF EXISTS vinplay_minigame._archive_config_history;
DROP TABLE IF EXISTS vinplay_minigame._archive_thanh_du;
DROP TABLE IF EXISTS vinplay_minigame._archive_tx_rank;
