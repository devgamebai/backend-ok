-- GitLab #23 — guard trigger to stop any DELETE on vinplay.users where
-- is_bot = 1. Doesn't matter whether the DELETE comes from a user-cleanup
-- script, a mistaken admin query, or a future migration — MySQL signals
-- SQLSTATE 45000 before the row is removed.
--
-- Incident 2026-04-17: user-cleanup-20260417-081046.sh ran an unscoped
-- DELETE on vinplay.users and removed 5,564 is_bot=1 rows alongside the
-- intended spam-user cleanup. Latent 5 days until Hazelcast restart on
-- 2026-04-22 tried to load an in-memory bots list from an empty table —
-- minigame/slot/sam went into a WebSocket-flap loop (SplittableRandom
-- nextInt(0) / IllegalArgumentException).
--
-- Escape hatch when an operator REALLY wants to delete a bot:
--   SET @allow_bot_delete := 1;   -- session-local
--   DELETE FROM vinplay.users WHERE user_name = 'bot_obsolete_xyz';
--   SET @allow_bot_delete := 0;
--
-- Idempotent: drops the trigger first so re-running the migration is safe.

DROP TRIGGER IF EXISTS vinplay.trg_users_block_bot_delete;

DELIMITER $$

CREATE TRIGGER vinplay.trg_users_block_bot_delete
BEFORE DELETE ON vinplay.users
FOR EACH ROW
BEGIN
    IF OLD.is_bot = 1 AND (@allow_bot_delete IS NULL OR @allow_bot_delete <> 1) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'GitLab #23: refusing to DELETE is_bot=1 user. Set @allow_bot_delete=1 in the session to override.';
    END IF;
END$$

DELIMITER ;
