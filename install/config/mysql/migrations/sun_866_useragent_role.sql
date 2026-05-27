-- SUN-866: chỉ agent (ĐL) mới được login agency portal.
--
-- Hiện tại LoginAgentProcessor (c=9428) cho phép login khi
--   useragent.username = ? AND active = 1
-- → bất kỳ row nào trong useragent (kể cả SpecialAccount level=0
-- hay row test cũ) đều có thể đăng nhập agency portal nếu password
-- (game) trùng. Thêm cột `role` để admin chủ động đánh dấu role nào
-- được phép vào agency portal — chỉ `role='agent'` mới qua được.

USE vinplay_admin;

-- 1. Add role column with safe default. Mặc định 'agent' giữ nguyên
--    hành vi cho mọi row hiện có (backward-compatible).
ALTER TABLE useragent
    ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'agent' AFTER level;

-- 2. Mark SpecialAccount (level=0) là admin → không phải role login
--    agency portal. Nếu sau này có thêm row admin/system khác, cập
--    nhật bằng tay hoặc qua admin CMS.
UPDATE useragent
   SET role = 'admin'
 WHERE level = 0;

-- 3. Index để LoginAgentProcessor lookup nhanh khi thêm filter role.
CREATE INDEX idx_useragent_username_role ON useragent (username, role, active);

-- Verification: phải thấy SpecialAccount = admin, mọi row level>=1 = agent.
SELECT id, username, nickname, level, role, active
  FROM useragent
  ORDER BY level, id
  LIMIT 50;
