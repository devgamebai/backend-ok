-- =============================================================================
-- 20260518_sun1402_relax_nickname_immutable_for_default.sql
--
-- SUN-1402 follow-up to SUN-1375.
--
-- The SUN-1375 BEFORE INSERT trigger fills `users.nick_name = user_name`
-- when a register flow forgets to provide one (so the agency popup that
-- keys by nickname always has a value). But the existing
-- `trg_users_nickname_immutable` trigger then blocks the legitimate
-- first-time nickname pick from `c=5 UpdateNickname`, because OLD.nick
-- is not NULL — it's just the SUN-1375 default copy of user_name.
--
-- This migration relaxes the immutable trigger so it allows exactly the
-- "still at SUN-1375 default" → "user-chosen" transition, while keeping
-- every other change locked. Combined with the parallel processor fix
-- in `UpdateNicknameProcesscor` (which now also treats `nick==user_name`
-- as the "not yet picked" state), the player can call c=5 once after
-- register; subsequent attempts still return 1013.
--
-- Logic:
--   • OLD.nick_name IS NULL                                → allowed (legacy "first pick from null")
--   • OLD.nick_name = OLD.user_name (SUN-1375 default)     → allowed (first user pick)
--   • OLD.nick_name = NEW.nick_name (no real change)       → allowed (no-op UPDATE)
--   • anything else                                        → blocked
--
-- Idempotent: DROP-then-CREATE.
-- =============================================================================

USE vinplay;

DROP TRIGGER IF EXISTS trg_users_nickname_immutable;

DELIMITER //
CREATE TRIGGER trg_users_nickname_immutable
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    IF OLD.nick_name IS NOT NULL
       AND OLD.nick_name <> OLD.user_name
       AND NOT (OLD.nick_name <=> NEW.nick_name) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'users.nick_name immutable: 66 tables cache it';
    END IF;
END//
DELIMITER ;
