-- ================================================================
-- Module Hoa Hồng Tiền Nạp (Deposit Commission)
-- Created: 2026-04-06
-- ================================================================

-- 1. Thêm cột deposit_rate vào useragent
--    Admin set % hoa hồng theo tiền nạp cho từng agent
ALTER TABLE vinplay_admin.useragent
    ADD COLUMN IF NOT EXISTS deposit_rate DOUBLE DEFAULT 0 COMMENT '% hoa hồng tiền nạp (e.g. 1.5 = 1.5%)';

-- 2. Thêm cột wallet_balance nếu chưa có
--    creditAgentWallet() sử dụng column này
ALTER TABLE vinplay_admin.useragent
    ADD COLUMN IF NOT EXISTS wallet_balance BIGINT DEFAULT 0 COMMENT 'Số dư ví đại lý (hoa hồng tích lũy)';

-- 3. Bảng log hoa hồng nạp tiền
--    Real-time: mỗi lần user nạp thành công -> ghi bản ghi này ngay
CREATE TABLE IF NOT EXISTS vinplay_admin.deposit_commission_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Agent nhận hoa hồng
    agent_id INT NOT NULL COMMENT 'useragent.id',
    agent_nickname VARCHAR(64) NOT NULL,
    agent_level TINYINT NOT NULL DEFAULT 1 COMMENT '1=TĐL, 2=ĐL1, 3=ĐL2',

    -- Người chơi nạp tiền
    source_user_id BIGINT NOT NULL COMMENT 'users.id',
    source_nickname VARCHAR(64) NOT NULL,

    -- Giao dịch nạp
    -- UNIQUE (transaction_id, agent_id): đảm bảo idempotency thực thụ ở DB level
    transaction_id VARCHAR(128) NOT NULL COMMENT 'Mã giao dịch nạp gốc',
    deposit_amount BIGINT NOT NULL COMMENT 'Số tiền user thực nạp (VND/VIN)',

    -- Chi tiết tính toán chênh lệch
    own_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '% deposit_rate của agent này',
    child_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '% deposit_rate của tuyến dưới trực tiếp',
    diff_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '= own_rate - child_rate',

    -- Kết quả
    commission_amount BIGINT NOT NULL DEFAULT 0 COMMENT 'Hoa hồng nhận = deposit_amount * diff_rate / 100',

    -- Trạng thái (PENDING: chờ ghi ví, PAID: đã cộng vào ví, ROLLED_BACK: đã thu hồi)
    status ENUM('PENDING','PAID','ROLLED_BACK') NOT NULL DEFAULT 'PENDING',
    note VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- UNIQUE constraint đảm bảo 1 txn chỉ tạo 1 bản ghi cho 1 agent (idempotency ở DB level)
    UNIQUE KEY uk_txn_agent (transaction_id, agent_id),

    INDEX idx_agent (agent_id),
    INDEX idx_source (source_user_id),
    INDEX idx_created (created_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Log hoa hồng tiền nạp cho đại lý';

-- 4. Sync deposit_rate từ commission_rate (nếu muốn dùng lại config cũ)
--    Chạy 1 lần để populate giá trị ban đầu (optional, uncomment nếu cần)
-- UPDATE vinplay_admin.useragent SET deposit_rate = commission_rate WHERE deposit_rate = 0 AND commission_rate > 0;
