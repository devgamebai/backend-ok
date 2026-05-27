-- Migration: Add index on users.parent_agent_id
-- Date: 2026-04-19
-- Ticket: feat/dashboard-agenc-api-optimal
-- Purpose: Tối ưu performance cho Dashboard Agency APIs.
--          Tất cả các query báo cáo đều filter theo parent_agent_id
--          nhưng cột này chưa có index, gây full table scan trên bảng users.
--
-- Impact: Bảng users (~50K+ rows production). Thời gian chạy ước tính < 5 giây.
-- Rollback: DROP INDEX idx_parent_agent_id ON users;

-- Kiểm tra index đã tồn tại chưa trước khi tạo
SET @db_name = DATABASE();
SET @index_exists = (
    SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'users'
      AND INDEX_NAME = 'idx_parent_agent_id'
);

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_parent_agent_id ON users(parent_agent_id)',
    'SELECT "Index idx_parent_agent_id already exists, skipping." AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
