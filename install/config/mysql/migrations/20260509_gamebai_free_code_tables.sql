-- vinplay_gamebai: missing tables for the free-code (gift code) feature.
-- GameBaiUtils.init() runs at backend-api boot and reads
-- game_free_code_detail (the per-code rows). When the table is absent
-- the init logs the SQLSyntaxErrorException and the rest of GameBai
-- bootstrap continues — but the feature itself is non-functional.
--
-- Schema reverse-engineered from:
--   - GameTourDaoImpl.exportFreeCode (INSERT shape, line 831–858)
--   - GameTourDaoImpl.listFreeCodes / etc (SELECT shape, line 886+, 971+)
--   - com.vinplay.gamebai.entities.GameFreeCodeDetail (entity fields)
-- Re-runnable: CREATE TABLE IF NOT EXISTS.

USE vinplay_gamebai;

CREATE TABLE IF NOT EXISTS game_free_code_package (
    id           INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    game_name    VARCHAR(64)  NOT NULL,
    type         INT          NOT NULL DEFAULT 0,
    quantity     INT          NOT NULL DEFAULT 0,
    amount       INT          NOT NULL DEFAULT 0,
    expire       DATETIME     NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creater      VARCHAR(128) NULL,
    KEY idx_game_name  (game_name),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS game_free_code_detail (
    id           INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    package_id   INT          NOT NULL,
    code         VARCHAR(64)  NOT NULL,
    game_name    VARCHAR(64)  NOT NULL,
    type         INT          NOT NULL DEFAULT 0,
    amount       INT          NOT NULL DEFAULT 0,
    status       INT          NOT NULL DEFAULT 0,
    expire       DATETIME     NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nickname     VARCHAR(64)  NULL,
    add_info     VARCHAR(255) NULL,
    use_time     DATETIME     NULL,
    UNIQUE KEY uk_code      (code),
    KEY idx_package         (package_id),
    KEY idx_status          (status),
    KEY idx_nickname        (nickname),
    KEY idx_create_time     (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SELECT 'game_free_code_package' tbl, COUNT(*) cnt FROM game_free_code_package
UNION ALL
SELECT 'game_free_code_detail',     COUNT(*)     FROM game_free_code_detail;
