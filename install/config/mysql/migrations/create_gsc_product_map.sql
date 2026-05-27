-- ================================================================
-- SUN-850: Flexible GSC provider → game_key mapping
--
-- Production will have ~20 GSC providers. Each provider sends a
-- distinct product_code on the seamless-wallet callbacks (Withdraw /
-- Deposit / Cancel / Rollback). We need to translate that numeric code
-- into a game_key string (e.g. "sbo", "wm", "ag") that
-- vinplay.game_commission_rate is configured with, so the rebate
-- pipeline can pick the correct per-agent rate.
--
-- Rule: many-to-one allowed. Multiple product_codes may share the same
-- game_key (SBO / Saba / FB Sports / Panda → "sbo") so the admin only
-- configures one commission row per "pot".
--
-- Unknown product_code (no row here) → callers fall back to the
-- agent's global useragent.commission_rate (same contract as
-- LogMoneyUserExtraProcessor.effectiveRateFor when gameKey is null).
--
-- Schema is admin-editable via the CMS (c=99XX CRUD — out of scope
-- for this migration; only the table + initial seed).
-- ================================================================

USE vinplay;

CREATE TABLE IF NOT EXISTS gsc_product_map (
    product_code  INT           NOT NULL PRIMARY KEY,
    provider_name VARCHAR(64)   NOT NULL,
    game_key      VARCHAR(64)   NOT NULL,
    game_label_vi VARCHAR(128)  DEFAULT NULL,
    category      ENUM('CASINO','SLOT','SPORT','FISH','OTHER') NOT NULL DEFAULT 'OTHER',
    is_active     TINYINT(1)    NOT NULL DEFAULT 1,
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_game_key (game_key),
    KEY idx_active   (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed the currently-known providers. These map onto the existing
-- game_commission_rate pots so rebates start flowing the moment the
-- code deploys, without any extra admin work.
--
-- Many → one pot is expected: sport-book providers share "sbo".
INSERT INTO gsc_product_map
    (product_code, provider_name, game_key, game_label_vi, category, is_active)
VALUES
    (1002, 'Evolution',         'ag',  'Evolution Casino', 'CASINO', 1),
    (1007, 'PG Soft',            'wm',  'PG Soft',          'SLOT',   1),
    (1012, 'SBO Sports Book',    'sbo', 'SBO',              'SPORT',  1),
    (1046, 'Saba Sports',        'sbo', 'Saba',             'SPORT',  1),
    (1183, 'FB Sports',          'sbo', 'FB Sports',        'SPORT',  1),
    (1228, 'CMD',                'cmd', 'CMD Sports',       'SPORT',  1),
    (1229, 'Panda Sports',       'sbo', 'Panda Sports',     'SPORT',  1)
ON DUPLICATE KEY UPDATE
    provider_name = VALUES(provider_name),
    game_key      = VALUES(game_key),
    game_label_vi = VALUES(game_label_vi),
    category      = VALUES(category),
    is_active     = VALUES(is_active);

-- Verification
SELECT 'Seed result (should be 7 rows):' AS info;
SELECT product_code, provider_name, game_key, category FROM gsc_product_map ORDER BY product_code;
