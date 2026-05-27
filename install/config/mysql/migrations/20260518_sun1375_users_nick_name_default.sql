-- =============================================================================
-- 20260518_sun1375_users_nick_name_default.sql
--
-- SUN-1375 — Agency CMS "Chi tiết thành viên" popup doesn't fire the
-- detail API for users whose `nick_name` is NULL. FE keys the popup
-- request by nickname; when nickname is null/empty, the request is
-- skipped entirely.
--
-- ROOT CAUSE
-- ----------
-- The SP `update_user_info` types 9/10/11 (player self-register via
-- portal QuickRegisterProcessor / Facebook / Google) INSERT `users`
-- WITHOUT setting `nick_name`. Column is `VARCHAR(128) NULL UNIQUE`,
-- so the row lands with NULL nickname. 12 non-bot rows observed on
-- staging (e.g. KwonUSA id=50056, newregtest2026 id=50108).
--
-- FIX
-- ---
--   (1) Backfill: every NULL/empty `nick_name` row → set to `user_name`.
--       Safe vs. the existing UNIQUE (nick_name) constraint because no
--       conflict pair exists (validated by pre-flight in the SUN-1375
--       investigation). `trg_users_nickname_immutable` permits NULL→value
--       transitions — only blocks value→different-value.
--   (2) BEFORE INSERT trigger sets `nick_name = user_name` whenever an
--       INSERT supplies a NULL/empty value. This defends against ALL
--       future writes (SP types 9/10/11, social-login flows, future
--       processors) without touching the SP body.
--
-- Idempotent: rerunning is a no-op (backfill matches nothing, trigger
-- DROP-then-CREATE replaces in place).
-- =============================================================================

USE vinplay;

-- (1) Backfill
UPDATE users
   SET nick_name = user_name
 WHERE nick_name IS NULL
    OR nick_name = '';

-- (2) BEFORE INSERT default
DROP TRIGGER IF EXISTS trg_users_nickname_default_on_insert;

DELIMITER //
CREATE TRIGGER trg_users_nickname_default_on_insert
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    IF NEW.nick_name IS NULL OR NEW.nick_name = '' THEN
        SET NEW.nick_name = NEW.user_name;
    END IF;
END//
DELIMITER ;

-- Sanity check: must return 0.
SELECT COUNT(*) AS users_with_null_or_blank_nick
  FROM users
 WHERE nick_name IS NULL
    OR nick_name = '';
