-- ================================================================
-- Cashback Per-Game % Config + Game Breakdown
-- Migration: Thêm bảng cấu hình % hoàn cược theo từng game
--            và bảng breakdown game trong lịch sử hoàn cược
-- ================================================================

-- 1. Per-game rebate % config (nhiều game per cashback program)
--    Admin có thể set % hoàn cược riêng cho từng game
CREATE TABLE IF NOT EXISTS tbl_cashback_game_config (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    config_id       INT NOT NULL COMMENT 'FK tbl_cashback_config.id',
    game_code       VARCHAR(64) NOT NULL COMMENT 'Game code/service name, e.g. TaiXiu, BauCua, Slots',
    game_name       VARCHAR(128) NOT NULL COMMENT 'Tên game hiển thị',
    rebate_percent  DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '% hoàn cược riêng của game này (0 = không áp dụng)',
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_game (config_id, game_code),
    INDEX idx_config (config_id),
    INDEX idx_active (config_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='% hoàn cược riêng theo từng game trong một chương trình cashback';

-- 2. Per-game volume + rebate breakdown trong mỗi cashback log
--    Khi click vào lịch sử hoàn cược → dropdown danh sách game, vol cược, tiền hoàn
CREATE TABLE IF NOT EXISTS tbl_cashback_log_game_detail (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cashback_log_id     BIGINT NOT NULL COMMENT 'FK tbl_cashback_logs.id',
    game_code           VARCHAR(64) NOT NULL COMMENT 'Game code/service name',
    game_name           VARCHAR(128) NOT NULL COMMENT 'Tên game',
    vol_bet             BIGINT NOT NULL DEFAULT 0 COMMENT 'Tổng vol cược trong game (abs của tiêu từ log_money_user)',
    rebate_percent      DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '% áp dụng cho game này',
    rebate_amount       BIGINT NOT NULL DEFAULT 0 COMMENT 'Tiền hoàn cược = vol_bet * rebate_percent / 100',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log (cashback_log_id),
    INDEX idx_game (game_code),
    UNIQUE KEY uk_log_game (cashback_log_id, game_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Breakdown vol cược + tiền hoàn theo từng game trong mỗi cashback log';
