-- =============================================================================
-- SUN-GAME-FK: unified game catalog + FK on commission/rebate paths.
--
-- Phase 1 (this migration): additive schema. New tables + NULLABLE FK
-- columns. NO behavioural change — readers/writers still use the existing
-- VARCHAR game_key / game_action paths. Sets up the foundation so later
-- phases can dual-write FKs and eventually cut readers over.
--
-- Phase 2 (next MR): writers stamp category_id / game_id on rebate_logs.
-- Phase 3: CommissionRateResolver prefers FK lookup when populated.
-- Phase 4: drop string columns once FK rows reach 100% coverage.
--
-- Why now: AWC/GSC each carry their own catalog tables with mismatched
-- category strings (CASINO vs Baccarat, FISH vs Fishing). The resolver
-- has hardcoded translations + per-provider lookup branches that grow
-- O(N) with every new aggregator. A central catalog + category enum
-- + FK constraint stops the drift at the schema layer.
--
-- Rollback:
--   ALTER TABLE rebate_logs DROP FOREIGN KEY fk_rebate_logs_game_id,
--                           DROP FOREIGN KEY fk_rebate_logs_category_id,
--                           DROP COLUMN game_id, DROP COLUMN category_id;
--   ALTER TABLE game_commission_rate DROP FOREIGN KEY fk_gcr_category_id,
--                                    DROP COLUMN category_id;
--   DROP TABLE games;
--   DROP TABLE game_categories;
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. game_categories — central enum lookup.
--    Names match the existing GSC convention so live_cat_<name> in
--    game_commission_rate continues to resolve via the bare string column
--    until the cutover.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vinplay.game_categories (
    id              INT NOT NULL AUTO_INCREMENT,
    name            VARCHAR(64) NOT NULL,
    display_label_vi VARCHAR(128) DEFAULT NULL,
    display_label_en VARCHAR(128) DEFAULT NULL,
    is_live         TINYINT(1) NOT NULL DEFAULT 1, -- 1 = live_cat_*, 0 = offline_cat_*
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 2. games — unified per-game catalog across providers.
--    Replaces the per-provider tables (gsc_game_catalog,
--    awc_game_catalog, awc_platform_map) for FK lookup. Existing tables
--    stay during the dual-write phase as the source of truth for backfills.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vinplay.games (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    provider        ENUM('AWC','GSC','OFFLINE') NOT NULL,
    vendor_platform VARCHAR(64) DEFAULT NULL,        -- SEXYBCRT / 1002 / NULL for offline
    game_code       VARCHAR(128) NOT NULL,
    game_name       VARCHAR(255) DEFAULT NULL,
    category_id     INT NOT NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_platform_code (provider, vendor_platform, game_code),
    KEY idx_category (category_id),
    KEY idx_provider_active (provider, is_active),
    CONSTRAINT fk_games_category_id FOREIGN KEY (category_id)
        REFERENCES vinplay.game_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 3. game_commission_rate.category_id — NULLABLE FK so existing rows
--    (string game_key) keep working until backfilled.
-- ---------------------------------------------------------------------------
ALTER TABLE vinplay.game_commission_rate
    ADD COLUMN category_id INT DEFAULT NULL,
    ADD KEY idx_gcr_agent_category (agent_nickname, category_id),
    ADD CONSTRAINT fk_gcr_category_id FOREIGN KEY (category_id)
        REFERENCES vinplay.game_categories (id);

-- ---------------------------------------------------------------------------
-- 4. rebate_logs.game_id + category_id — NULLABLE FK on every rebate row.
-- ---------------------------------------------------------------------------
ALTER TABLE vinplay.rebate_logs
    ADD COLUMN game_id BIGINT DEFAULT NULL,
    ADD COLUMN category_id INT DEFAULT NULL,
    ADD KEY idx_rebate_game (game_id),
    ADD KEY idx_rebate_category (category_id),
    ADD CONSTRAINT fk_rebate_logs_game_id FOREIGN KEY (game_id)
        REFERENCES vinplay.games (id),
    ADD CONSTRAINT fk_rebate_logs_category_id FOREIGN KEY (category_id)
        REFERENCES vinplay.game_categories (id);

-- ---------------------------------------------------------------------------
-- 5. Seed game_categories with the existing GSC-aligned names.
-- ---------------------------------------------------------------------------
INSERT INTO vinplay.game_categories (name, display_label_vi, display_label_en, is_live) VALUES
    ('Baccarat',     'Baccarat',          'Baccarat',           1),
    ('DragonTiger',  'Rồng Hổ',           'Dragon Tiger',       1),
    ('Roulette',     'Roulette',          'Roulette',           1),
    ('SicBoDice',    'Tài Xỉu',           'Sic Bo / Dice',      1),
    ('Blackjack',    'Blackjack',         'Blackjack',          1),
    ('GameShow',     'Game Show',         'Game Show',          1),
    ('Poker',        'Poker',             'Poker',              1),
    ('Slot',         'Slot',              'Slot',               1),
    ('Fishing',      'Bắn Cá',            'Fishing',            1),
    ('Sports',       'Thể Thao',          'Sports',             1),
    ('Other',        'Khác',              'Other',              1),
    ('OfflineSlot',   'Slot Nội Bộ',      'Offline Slot',       0),
    ('OfflineMinigame','Minigame',        'Offline Minigame',   0),
    ('OfflineCardGame','Game Bài',        'Offline Card Game',  0)
ON DUPLICATE KEY UPDATE display_label_vi=VALUES(display_label_vi);
