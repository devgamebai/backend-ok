-- User-level block list: prevent specific players from betting on
-- chosen providers, platforms, games, tables, or whole categories.
--
-- Match semantics: a row blocks a bet when ALL of the row's non-NULL
-- predicates match the bet's (provider, vendor_platform, game_code,
-- table_tag, category_id). NULL = "any" — so:
--
--   Row {user_id=42, provider='GSC', vendor_platform=NULL, ...}
--     → blocks all GSC bets for user 42
--
--   Row {user_id=42, provider='GSC', vendor_platform='1002', ...}
--     → blocks user 42 on Evolution (GSC product 1002) only
--
--   Row {user_id=42, category_id=1, provider=NULL, ...}
--     → blocks user 42 on EVERY baccarat (category 1) across both
--       AWC and GSC
--
-- expires_at NULL = permanent. Service layer treats expires_at <= NOW()
-- as inactive without ops needing to flip is_active.

USE vinplay;

CREATE TABLE IF NOT EXISTS user_game_block (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NULL,
    nick_name       VARCHAR(64)  NULL,
    provider        VARCHAR(8)   NULL,
    vendor_platform VARCHAR(64)  NULL,
    game_code       VARCHAR(128) NULL,
    table_tag       VARCHAR(16)  NULL,
    category_id     INT          NULL,
    reason          TEXT         NULL,
    blocked_by      VARCHAR(128) NOT NULL,
    blocked_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      DATETIME     NULL,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    KEY idx_user_active (user_id, is_active),
    KEY idx_nick_active (nick_name, is_active),
    KEY idx_active_expires (is_active, expires_at)
);
