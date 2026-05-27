-- Phase 1 — RTP config backbone (win-rate control feature).
-- See docs/game_win_rate_implementation_plan.md §Phase 1.
-- Creates: game_rtp_config, user_rtp_override, rtp_config_audit, rtp_aggregator_watermark.
-- Safe to re-run (IF NOT EXISTS).

CREATE TABLE IF NOT EXISTS vinplay.game_rtp_config (
  game_code      VARCHAR(32)  NOT NULL PRIMARY KEY,
  win_rate_pct   DECIMAL(5,2) NOT NULL,
  status         TINYINT      NOT NULL DEFAULT 1,
  updated_by     VARCHAR(64)  NOT NULL DEFAULT 'system',
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  note           VARCHAR(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vinplay.user_rtp_override (
  user_id        BIGINT       NOT NULL,
  game_code      VARCHAR(32)  NOT NULL,
  win_rate_pct   DECIMAL(5,2) NOT NULL,
  reason         VARCHAR(128) NOT NULL DEFAULT 'manual',
  expires_at     DATETIME     DEFAULT NULL,
  created_by     VARCHAR(64)  NOT NULL DEFAULT 'system',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, game_code),
  KEY idx_user_expires (user_id, expires_at),
  KEY idx_game_code (game_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vinplay.rtp_config_audit (
  id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  scope      ENUM('GAME','USER') NOT NULL,
  game_code  VARCHAR(32)  DEFAULT NULL,
  user_id    BIGINT       DEFAULT NULL,
  action     ENUM('CREATE','UPDATE','DELETE') NOT NULL,
  old_value  DECIMAL(5,2) DEFAULT NULL,
  new_value  DECIMAL(5,2) DEFAULT NULL,
  actor      VARCHAR(64)  NOT NULL,
  reason     VARCHAR(255) DEFAULT NULL,
  at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_at (at),
  KEY idx_scope_at (scope, at),
  KEY idx_user_at (user_id, at),
  KEY idx_game_at (game_code, at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vinplay.rtp_aggregator_watermark (
  source_collection  VARCHAR(64)  NOT NULL PRIMARY KEY,
  last_processed_ts  DATETIME     NOT NULL,
  last_run_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  rows_ingested      BIGINT       NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
