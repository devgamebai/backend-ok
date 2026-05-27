-- SUN-REG-FIX: relax users.nick_name immutability so QuickRegister →
-- UpdateNickname flow can complete the first-time set.
--
-- The original trg_users_nickname_immutable (created 2026-05-04) rejected
-- ANY change to nick_name, including the legitimate NULL → value
-- transition that c=5 (UpdateNicknameProcesscor) does immediately after
-- c=1 (QuickRegisterProcessor) inserts the row with nick_name=NULL.
--
-- Result on staging: every newly registered account got stuck with
-- nick_name=NULL, c=5 returned errorCode 1001, FE showed "cant register",
-- and 15 rows accumulated mid-flow before the bug was caught.
--
-- This migration drops the old trigger and recreates it with an
-- OLD.nick_name IS NOT NULL guard so the first set is allowed but
-- subsequent renames stay blocked (preserves the original intent —
-- ~66 tables cache nick_name).
--
-- Rollback: re-apply the prior trigger definition (drop the
-- IS NOT NULL guard).

DROP TRIGGER IF EXISTS vinplay.trg_users_nickname_immutable;

DELIMITER //
CREATE TRIGGER vinplay.trg_users_nickname_immutable
BEFORE UPDATE ON vinplay.users
FOR EACH ROW
BEGIN
    IF OLD.nick_name IS NOT NULL AND NOT (OLD.nick_name <=> NEW.nick_name) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'users.nick_name immutable: 66 tables cache it';
    END IF;
END//
DELIMITER ;
