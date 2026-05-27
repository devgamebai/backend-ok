-- SUN-1112 (CQRS Phase 0) — DB triggers that auto-populate
-- vinplay.commission_rate_audit when any of the rate config tables are
-- modified. Defense-in-depth alongside the application-level audit hooks
-- that will be added in subsequent phases.
--
-- Three triggers per table (INSERT, UPDATE, DELETE) × three tables = 9
-- triggers. Each compares old vs new value and only writes an audit row
-- when the rate field actually changed (UPDATEs that touch unrelated
-- columns are silent).
--
-- changed_by = 'db-trigger' on auto-logged rows.
-- changed_via = 'trigger:<trigger_name>' for traceability.
--
-- Idempotent: DROP IF EXISTS before each CREATE.

DROP TRIGGER IF EXISTS vinplay.tg_gcr_audit_ins;
DROP TRIGGER IF EXISTS vinplay.tg_gcr_audit_upd;
DROP TRIGGER IF EXISTS vinplay.tg_gcr_audit_del;

DROP TRIGGER IF EXISTS vinplay_admin.tg_useragent_audit_ins;
DROP TRIGGER IF EXISTS vinplay_admin.tg_useragent_audit_upd;
DROP TRIGGER IF EXISTS vinplay_admin.tg_useragent_audit_del;

DROP TRIGGER IF EXISTS vinplay.tg_cashback_gc_audit_ins;
DROP TRIGGER IF EXISTS vinplay.tg_cashback_gc_audit_upd;
DROP TRIGGER IF EXISTS vinplay.tg_cashback_gc_audit_del;

-- ─────────────────────────────────────────────────────────────────
-- vinplay.game_commission_rate (per-agent per-game rate)
-- ─────────────────────────────────────────────────────────────────
DELIMITER $$

CREATE TRIGGER vinplay.tg_gcr_audit_ins AFTER INSERT ON vinplay.game_commission_rate
FOR EACH ROW
BEGIN
    INSERT INTO vinplay.commission_rate_audit
        (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
    VALUES
        ('game_commission_rate',
         JSON_OBJECT('agent_nickname', NEW.agent_nickname, 'game_key', NEW.game_key),
         'rate',
         NULL,
         NEW.rate,
         'INSERT',
         'db-trigger',
         'trigger:tg_gcr_audit_ins');
END$$

CREATE TRIGGER vinplay.tg_gcr_audit_upd AFTER UPDATE ON vinplay.game_commission_rate
FOR EACH ROW
BEGIN
    IF NOT (OLD.rate <=> NEW.rate) THEN
        INSERT INTO vinplay.commission_rate_audit
            (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
        VALUES
            ('game_commission_rate',
             JSON_OBJECT('agent_nickname', NEW.agent_nickname, 'game_key', NEW.game_key),
             'rate',
             OLD.rate,
             NEW.rate,
             'UPDATE',
             'db-trigger',
             'trigger:tg_gcr_audit_upd');
    END IF;
END$$

CREATE TRIGGER vinplay.tg_gcr_audit_del AFTER DELETE ON vinplay.game_commission_rate
FOR EACH ROW
BEGIN
    INSERT INTO vinplay.commission_rate_audit
        (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
    VALUES
        ('game_commission_rate',
         JSON_OBJECT('agent_nickname', OLD.agent_nickname, 'game_key', OLD.game_key),
         'rate',
         OLD.rate,
         NULL,
         'DELETE',
         'db-trigger',
         'trigger:tg_gcr_audit_del');
END$$

-- ─────────────────────────────────────────────────────────────────
-- vinplay_admin.useragent.commission_rate (agent global rate)
-- ─────────────────────────────────────────────────────────────────
CREATE TRIGGER vinplay_admin.tg_useragent_audit_ins AFTER INSERT ON vinplay_admin.useragent
FOR EACH ROW
BEGIN
    IF NEW.commission_rate IS NOT NULL THEN
        INSERT INTO vinplay.commission_rate_audit
            (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
        VALUES
            ('useragent',
             JSON_OBJECT('id', NEW.id, 'nickname', NEW.nickname),
             'commission_rate',
             NULL,
             NEW.commission_rate,
             'INSERT',
             'db-trigger',
             'trigger:tg_useragent_audit_ins');
    END IF;
END$$

CREATE TRIGGER vinplay_admin.tg_useragent_audit_upd AFTER UPDATE ON vinplay_admin.useragent
FOR EACH ROW
BEGIN
    IF NOT (OLD.commission_rate <=> NEW.commission_rate) THEN
        INSERT INTO vinplay.commission_rate_audit
            (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
        VALUES
            ('useragent',
             JSON_OBJECT('id', NEW.id, 'nickname', NEW.nickname),
             'commission_rate',
             OLD.commission_rate,
             NEW.commission_rate,
             'UPDATE',
             'db-trigger',
             'trigger:tg_useragent_audit_upd');
    END IF;
END$$

CREATE TRIGGER vinplay_admin.tg_useragent_audit_del AFTER DELETE ON vinplay_admin.useragent
FOR EACH ROW
BEGIN
    IF OLD.commission_rate IS NOT NULL THEN
        INSERT INTO vinplay.commission_rate_audit
            (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
        VALUES
            ('useragent',
             JSON_OBJECT('id', OLD.id, 'nickname', OLD.nickname),
             'commission_rate',
             OLD.commission_rate,
             NULL,
             'DELETE',
             'db-trigger',
             'trigger:tg_useragent_audit_del');
    END IF;
END$$

-- ─────────────────────────────────────────────────────────────────
-- vinplay.tbl_cashback_game_config (player cashback per game)
-- ─────────────────────────────────────────────────────────────────
CREATE TRIGGER vinplay.tg_cashback_gc_audit_ins AFTER INSERT ON vinplay.tbl_cashback_game_config
FOR EACH ROW
BEGIN
    INSERT INTO vinplay.commission_rate_audit
        (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
    VALUES
        ('tbl_cashback_game_config',
         JSON_OBJECT('id', NEW.id, 'config_id', NEW.config_id, 'game_code', NEW.game_code),
         'rebate_percent',
         NULL,
         NEW.rebate_percent,
         'INSERT',
         'db-trigger',
         'trigger:tg_cashback_gc_audit_ins');
END$$

CREATE TRIGGER vinplay.tg_cashback_gc_audit_upd AFTER UPDATE ON vinplay.tbl_cashback_game_config
FOR EACH ROW
BEGIN
    IF NOT (OLD.rebate_percent <=> NEW.rebate_percent) THEN
        INSERT INTO vinplay.commission_rate_audit
            (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
        VALUES
            ('tbl_cashback_game_config',
             JSON_OBJECT('id', NEW.id, 'config_id', NEW.config_id, 'game_code', NEW.game_code),
             'rebate_percent',
             OLD.rebate_percent,
             NEW.rebate_percent,
             'UPDATE',
             'db-trigger',
             'trigger:tg_cashback_gc_audit_upd');
    END IF;
END$$

CREATE TRIGGER vinplay.tg_cashback_gc_audit_del AFTER DELETE ON vinplay.tbl_cashback_game_config
FOR EACH ROW
BEGIN
    INSERT INTO vinplay.commission_rate_audit
        (target_table, target_pk_json, field_name, old_value, new_value, operation, changed_by, changed_via)
    VALUES
        ('tbl_cashback_game_config',
         JSON_OBJECT('id', OLD.id, 'config_id', OLD.config_id, 'game_code', OLD.game_code),
         'rebate_percent',
         OLD.rebate_percent,
         NULL,
         'DELETE',
         'db-trigger',
         'trigger:tg_cashback_gc_audit_del');
END$$

DELIMITER ;
