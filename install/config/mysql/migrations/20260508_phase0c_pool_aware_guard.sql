-- Phase 0c — pool-aware guard trigger replaces the legacy parent-rate
-- check (which blocked the operator-confirmed "master = 0, push all
-- to downline" case — see plan §6).
-- Date: 2026-05-08
-- Plan: docs/COMMISSION_SCHEMA_FIX_PLAN.md §6.2
--
-- New invariant: every individual `rate` must be ≤ master tree's
-- pool_max_pct (per-game override applies). The differential math
-- (LogMoneyUserExtraProcessor.calculateDifferential) walks the chain
-- bottom-up; for any monotone-non-increasing chain the SUM of slices
-- equals the top rate, so capping any single rate at pool_max caps
-- the total payout.
--
-- This drops the historical "rate <= parent.rate" / "descendant
-- rate <= new.rate" checks. Top-down ordering is enforced at the
-- service layer (planned P1) — the schema-level invariant is now
-- only "no rate exceeds the pool".
--
-- The audit triggers (tg_gcr_audit_*) keep their existing behavior.

USE vinplay;

DROP TRIGGER IF EXISTS tg_gcr_guard_ins;
DROP TRIGGER IF EXISTS tg_gcr_guard_upd;

DELIMITER $$

CREATE TRIGGER tg_gcr_guard_ins
BEFORE INSERT ON game_commission_rate
FOR EACH ROW
BEGIN
    DECLARE v_master_id INT;
    DECLARE v_pool      DECIMAL(5,2);
    DECLARE v_override  DECIMAL(5,2);
    DECLARE v_msg       VARCHAR(500);

    IF NEW.rate IS NULL OR NEW.rate < 0 THEN
        SET v_msg = CONCAT('gcr_guard: rate must be >= 0 (got ', IFNULL(NEW.rate, 'NULL'), ')');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NEW.rate > 0 AND NEW.agent_user_id IS NOT NULL THEN
        -- Resolve the master TĐL by climbing ancestors. If this agent
        -- itself has no parent it IS the master.
        SELECT
            CASE
                WHEN u.parentid IS NULL OR u.parentid = 0 THEN u.id
                WHEN u.ancestors IS NULL OR u.ancestors = '' THEN u.parentid
                ELSE CAST(SUBSTRING_INDEX(u.ancestors, ',', 1) AS UNSIGNED)
            END
          INTO v_master_id
          FROM vinplay_admin.useragent u
         WHERE u.id = NEW.agent_user_id;

        SELECT pool_max_pct INTO v_pool
          FROM commission_rate_policy
         WHERE master_id = v_master_id;
        IF v_pool IS NULL THEN SET v_pool = 1.25; END IF;

        -- Per-game pool override (JSON map).
        SELECT JSON_EXTRACT(per_game_pool, CONCAT('$."', NEW.game_key, '"'))
          INTO v_override
          FROM commission_rate_policy
         WHERE master_id = v_master_id;
        IF v_override IS NOT NULL THEN SET v_pool = v_override; END IF;

        IF NEW.rate > v_pool THEN
            SET v_msg = CONCAT(
                'gcr_guard: rate=', NEW.rate,
                ' game_key=', NEW.game_key,
                ' agent_id=', NEW.agent_user_id,
                ' exceeds pool_max=', v_pool,
                ' (master_id=', IFNULL(v_master_id, 'NULL'), ')');
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;
    END IF;
END$$

CREATE TRIGGER tg_gcr_guard_upd
BEFORE UPDATE ON game_commission_rate
FOR EACH ROW
BEGIN
    DECLARE v_master_id INT;
    DECLARE v_pool      DECIMAL(5,2);
    DECLARE v_override  DECIMAL(5,2);
    DECLARE v_msg       VARCHAR(500);

    IF NEW.rate IS NULL OR NEW.rate < 0 THEN
        SET v_msg = CONCAT('gcr_guard: rate must be >= 0 (got ', IFNULL(NEW.rate, 'NULL'), ')');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NEW.rate > 0 AND NEW.rate <> OLD.rate AND NEW.agent_user_id IS NOT NULL THEN
        SELECT
            CASE
                WHEN u.parentid IS NULL OR u.parentid = 0 THEN u.id
                WHEN u.ancestors IS NULL OR u.ancestors = '' THEN u.parentid
                ELSE CAST(SUBSTRING_INDEX(u.ancestors, ',', 1) AS UNSIGNED)
            END
          INTO v_master_id
          FROM vinplay_admin.useragent u
         WHERE u.id = NEW.agent_user_id;

        SELECT pool_max_pct INTO v_pool
          FROM commission_rate_policy
         WHERE master_id = v_master_id;
        IF v_pool IS NULL THEN SET v_pool = 1.25; END IF;

        SELECT JSON_EXTRACT(per_game_pool, CONCAT('$."', NEW.game_key, '"'))
          INTO v_override
          FROM commission_rate_policy
         WHERE master_id = v_master_id;
        IF v_override IS NOT NULL THEN SET v_pool = v_override; END IF;

        IF NEW.rate > v_pool THEN
            SET v_msg = CONCAT(
                'gcr_guard: rate=', NEW.rate,
                ' game_key=', NEW.game_key,
                ' agent_id=', NEW.agent_user_id,
                ' exceeds pool_max=', v_pool);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;
    END IF;
END$$

DELIMITER ;

-- ops_event_log entry
INSERT INTO ops_event_log (event_type, actor, payload)
VALUES ('trigger_replace', 'phase0c_migration',
        JSON_OBJECT('triggers', JSON_ARRAY('tg_gcr_guard_ins','tg_gcr_guard_upd'),
                    'invariant', 'rate <= commission_rate_policy.pool_max_pct (per-game override honored)'));

-- Verification
SELECT TRIGGER_NAME, ACTION_TIMING, EVENT_MANIPULATION
  FROM information_schema.TRIGGERS
 WHERE EVENT_OBJECT_SCHEMA = 'vinplay'
   AND EVENT_OBJECT_TABLE  = 'game_commission_rate'
 ORDER BY TRIGGER_NAME;
