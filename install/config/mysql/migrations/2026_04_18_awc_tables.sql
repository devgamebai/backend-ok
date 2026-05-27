-- AWC (Asia Win Connect) integration tables
-- SUN-945: config, env vars, DB schema

-- =============================================================================
-- awc_transactions: every wallet callback from AWC (bet, settle, cancel, etc.)
-- Indexed by platformTxId (AWC's unique txn ID) for idempotency checks.
-- =============================================================================
CREATE TABLE IF NOT EXISTS vinplay_minigame.awc_transactions (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    platform_tx_id  VARCHAR(64)     NOT NULL,
    round_id        VARCHAR(128)    DEFAULT NULL,
    user_id         INT             NOT NULL COMMENT 'our internal user id',
    user_name       VARCHAR(64)     NOT NULL,
    platform        VARCHAR(32)     NOT NULL COMMENT 'AWC platform code (SEXYBCRT, PP, JILI...)',
    game_code       VARCHAR(64)     DEFAULT NULL,
    game_name       VARCHAR(128)    DEFAULT NULL,
    game_type       VARCHAR(16)     DEFAULT NULL COMMENT 'LIVE, SLOT, FH, EGAME, ESPORTS',
    action          VARCHAR(32)     NOT NULL COMMENT 'bet, betNSettle, settle, cancelBet, etc.',
    bet_amount      BIGINT          NOT NULL DEFAULT 0,
    win_amount      BIGINT          NOT NULL DEFAULT 0,
    turnover        BIGINT          NOT NULL DEFAULT 0,
    adjust_amount   BIGINT          NOT NULL DEFAULT 0,
    tip_amount      BIGINT          NOT NULL DEFAULT 0,
    currency        VARCHAR(8)      DEFAULT 'VND',
    money_type      TINYINT         NOT NULL DEFAULT 1 COMMENT '1=vin, 2=xu',
    bet_time        DATETIME        DEFAULT NULL,
    tx_time         DATETIME        DEFAULT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'success' COMMENT 'success, failed, duplicate',
    balance_after   BIGINT          NOT NULL DEFAULT 0,
    raw_json        TEXT            DEFAULT NULL COMMENT 'full callback JSON for audit',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_tx_id (platform_tx_id),
    KEY idx_user_name (user_name),
    KEY idx_user_id (user_id),
    KEY idx_round_id (round_id),
    KEY idx_platform_game (platform, game_code),
    KEY idx_action (action),
    KEY idx_bet_time (bet_time),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- awc_game_catalog: cached game list from AWC getPlatformListByAgent.
-- Mirrors gsc_game_catalog structure for consistency.
-- =============================================================================
CREATE TABLE IF NOT EXISTS vinplay_minigame.awc_game_catalog (
    platform        VARCHAR(32)     NOT NULL COMMENT 'AWC platform code (SEXYBCRT, PP, JILI...)',
    game_code       VARCHAR(64)     NOT NULL,
    game_name       VARCHAR(255)    NOT NULL,
    game_type       VARCHAR(16)     DEFAULT NULL COMMENT 'LIVE, SLOT, FH, EGAME, ESPORTS',
    category        VARCHAR(64)     DEFAULT NULL COMMENT 'inferred: live_cat_baccarat, offline_cat_slot, etc.',
    image_url       VARCHAR(512)    DEFAULT NULL,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (platform, game_code),
    KEY idx_game_type (game_type),
    KEY idx_category (category),
    KEY idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- awc_platform_map: maps AWC platform codes to display names + categories.
-- Similar to gsc_product_map but keyed by platform string instead of int.
-- =============================================================================
CREATE TABLE IF NOT EXISTS vinplay_minigame.awc_platform_map (
    platform        VARCHAR(32)     NOT NULL PRIMARY KEY,
    provider_name   VARCHAR(64)     NOT NULL,
    game_type       VARCHAR(16)     NOT NULL DEFAULT '*' COMMENT 'LIVE, SLOT, FH, EGAME, * for all',
    category        VARCHAR(64)     NOT NULL DEFAULT 'OTHER' COMMENT 'CASINO, SLOT, SPORT, FISH, EGAME, OTHER',
    label_vi        VARCHAR(128)    DEFAULT NULL,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed initial platforms (from AWC getPlatformListByAgent)
INSERT INTO vinplay_minigame.awc_platform_map
    (platform, provider_name, game_type, category, label_vi, is_active)
VALUES
    ('SEXYBCRT',    'Sexy Baccarat',    'LIVE',     'CASINO',   'Sexy Baccarat',    1),
    ('EVOLUTION',   'Evolution',        'LIVE',     'CASINO',   'Evolution Casino',  1),
    ('HOTROAD',     'Hot Road',         'LIVE',     'CASINO',   'Hot Road Casino',   1),
    ('PP',          'Pragmatic Play',   'SLOT',     'SLOT',     'Pragmatic Play',    1),
    ('JILI',        'JILI',             'SLOT',     'SLOT',     'JILI',              1),
    ('PG',          'PG Soft',          'SLOT',     'SLOT',     'PG Soft',           1),
    ('JDB',         'JDB',              'SLOT',     'SLOT',     'JDB',               1),
    ('FC',          'Fachai',           'SLOT',     'SLOT',     'Fachai',             1),
    ('NETENT',      'NetEnt',           'SLOT',     'SLOT',     'NetEnt',            1),
    ('NLC',         'Nolimit City',     'SLOT',     'SLOT',     'Nolimit City',      1),
    ('BTG',         'Big Time Gaming',  'SLOT',     'SLOT',     'Big Time Gaming',   1),
    ('FASTSPIN',    'FastSpin',         'SLOT',     'SLOT',     'FastSpin',          1),
    ('RT',          'Red Tiger',        'SLOT',     'SLOT',     'Red Tiger',         1),
    ('DRAGOONSOFT', 'Dragoon Soft',     'SLOT',     'SLOT',     'Dragoon Soft',      1),
    ('PLAY8',       'Play8',            'SLOT',     'SLOT',     'Play8',             1),
    ('KINGMAKER',   'KingMaker',        'EGAME',    'EGAME',    'KingMaker',         1),
    ('KINGMAKERMINI','KingMaker Mini',  'EGAME',    'EGAME',    'KingMaker Mini',    1),
    ('YESBINGO',    'Yes Bingo',        'EGAME',    'EGAME',    'Yes Bingo',         1),
    ('LUDO',        'Ludo',             'EGAME',    'EGAME',    'Ludo',              1),
    ('JDBFISH',     'JDB Fish',         'FH',       'FISH',     'JDB Fish',          1),
    ('SABA',        'Saba Sports',      'ESPORTS',  'SPORT',    'Saba Sports',       1),
    ('E1SPORT',     'E1 Sport',         'ESPORTS',  'SPORT',    'E1 Sport',          1),
    ('YL',          'YL',               '*',        'OTHER',    'YL',                1),
    ('SPADE',       'Spade Gaming',     'SLOT',     'SLOT',     'Spade Gaming',      1),
    ('SV388',       'SV388',            'LIVE',     'OTHER',    'SV388 Cockfight',   1),
    ('HORSEBOOK',   'Horse Book',       'LIVE',     'SPORT',    'Horse Book',        1),
    ('PT',          'Playtech',         'SLOT',     'SLOT',     'Playtech',          1)
ON DUPLICATE KEY UPDATE
    provider_name = VALUES(provider_name),
    label_vi = VALUES(label_vi),
    updated_at = CURRENT_TIMESTAMP;
