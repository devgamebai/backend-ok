-- =============================================================================
-- SUN-970 [DB-2]: Normalize collation — utf8mb3_general_ci across all schemas
-- =============================================================================
-- 9 tables use utf8mb3_unicode_ci while 100+ use utf8mb3_general_ci.
-- This mismatch causes "Illegal mix of collations" errors on JOIN/WHERE.
-- Fix: convert the 9 outliers to match the majority.
--
-- Safe to run: ALTER TABLE CONVERT only changes metadata for VARCHAR columns.
-- Data is preserved. Indexes are rebuilt (brief lock per table).
-- =============================================================================

-- vinplay schema (5 tables)
ALTER TABLE vinplay.history_applyfor CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;
ALTER TABLE vinplay.history_bank CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;
ALTER TABLE vinplay.history_bank_live CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;
ALTER TABLE vinplay.topup CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;
ALTER TABLE vinplay.topup_live CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;

-- vinplay_admin schema (1 table — the most critical one)
ALTER TABLE vinplay_admin.useragent CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;

-- vinplay_gamebai schema (3 tables)
ALTER TABLE vinplay_gamebai.game_tour_mark CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;
ALTER TABLE vinplay_gamebai.poker_free_ticket CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;
ALTER TABLE vinplay_gamebai.xoc_dia_boss CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci;

-- Verify: should return 0 rows
SELECT CONCAT(TABLE_SCHEMA, '.', TABLE_NAME) AS still_unicode_ci
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA IN ('vinplay', 'vinplay_admin', 'vinplay_minigame', 'vinplay_gamebai')
  AND COLLATION_NAME = 'utf8mb3_unicode_ci'
  AND DATA_TYPE IN ('varchar', 'char', 'text', 'mediumtext', 'longtext')
GROUP BY TABLE_SCHEMA, TABLE_NAME;
