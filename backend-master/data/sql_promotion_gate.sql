-- ================================================================
-- Promotion Gate Field Migration
-- Thêm field "gate" cho chương trình khuyến mãi
-- Gate chỉ định kênh nạp tiền được áp dụng:
--   ALL    = Tất cả kênh nạp
--   BANK   = Nạp qua ngân hàng
--   CRYPTO = Nạp qua crypto
-- ================================================================

ALTER TABLE deposit_promotions
    ADD COLUMN gate VARCHAR(20) NOT NULL DEFAULT 'ALL'
        COMMENT 'Kênh nạp tiền áp dụng: ALL, BANK, CRYPTO'
        AFTER promo_type;

-- Index để filter promotions theo gate hiệu quả
ALTER TABLE deposit_promotions
    ADD INDEX idx_gate_type_status (gate, promo_type, status);
