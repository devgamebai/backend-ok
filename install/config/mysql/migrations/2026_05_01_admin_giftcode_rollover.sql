-- ============================================================
-- Migration: Admin GiftCode Rollover Requirement
-- Date: 2026-05-01
-- Ticket: SUN-1212
-- ============================================================

-- 1. Thêm cột rollover_rounds và source vào gift_codes
-- Gộp thành 1 lệnh ALTER TABLE duy nhất với ALGORITHM=INPLACE, LOCK=NONE để tránh lock table trên DB lớn.
-- Đổi DEFAULT thành 'LEGACY' để không leak sang event codes cũ.
ALTER TABLE gift_codes
  ADD COLUMN rollover_rounds INT NOT NULL DEFAULT 0 COMMENT 'So vong cuoc yeu cau de mo khoa rut tien (>= 1 cho ADMIN, 0 cho LEGACY)',
  ADD COLUMN source VARCHAR(50) NOT NULL DEFAULT 'LEGACY' COMMENT 'Nguon tao giftcode: ADMIN/LEGACY',
  ADD INDEX idx_source_created (source, created_at),
  ADD INDEX idx_created_by (created_by),
  ADD INDEX idx_time_used_exprired (time_used, exprired),
  ALGORITHM=INPLACE, LOCK=NONE;

-- 2. Backfill (Optional, since DEFAULT already applied 'LEGACY' and '0' to existing rows)
UPDATE gift_codes SET source='LEGACY' WHERE source != 'LEGACY';
UPDATE gift_codes SET rollover_rounds=0 WHERE source='LEGACY';
