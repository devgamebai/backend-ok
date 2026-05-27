-- =============================================================================
-- IMMEDIATE GUARDRAIL: alert + log any UPDATE that takes users.vin / xu / safe
-- below zero. Until the full money_ledger migration ships, this catches the
-- TOCTOU race-condition class of bugs (hoangha, sunkr88) the moment they occur,
-- so we never need hours of forensic archaeology to find them again.
--
-- Behavior:
--   - The UPDATE is NOT blocked (would risk breaking unknown legitimate paths).
--   - A row is inserted into vin_negative_alerts capturing who/when/where.
--   - Telegram alert (configured externally) fires on any new row.
--
-- Once the money_ledger schema is live and the post_money_transaction SP is
-- the only writer, the player CHECK constraint inside money_account makes
-- this trigger redundant. Keep it as defense-in-depth for the transition.
-- =============================================================================

CREATE TABLE IF NOT EXISTS vinplay.vin_negative_alerts (
    alert_id        BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    detected_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    user_id         INT NOT NULL,
    user_name       VARCHAR(128) NULL,
    nick_name       VARCHAR(128) NULL,
    field_name      VARCHAR(32) NOT NULL,           -- 'vin' | 'xu' | 'safe' | 'money_vp'
    old_value       BIGINT NOT NULL,
    new_value       BIGINT NOT NULL,
    delta           BIGINT NOT NULL,                -- new - old
    sql_user        VARCHAR(128) NULL,              -- which DB user did the UPDATE
    sql_thread      BIGINT NULL,                    -- connection id (lets you trace the app session)
    investigated_at DATETIME(6) NULL,
    investigation   VARCHAR(500) NULL,
    KEY idx_detected (detected_at),
    KEY idx_user     (user_id, detected_at),
    KEY idx_open     (investigated_at, detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Catch-all log for any UPDATE that takes a player wallet column negative. Telegram-alerts on each new row.';

DROP TRIGGER IF EXISTS vinplay.trg_users_alert_negative_wallet;

DELIMITER $$

CREATE TRIGGER vinplay.trg_users_alert_negative_wallet
BEFORE UPDATE ON vinplay.users
FOR EACH ROW
BEGIN
    -- vin
    IF NEW.vin < 0 AND OLD.vin >= 0 THEN
        INSERT INTO vinplay.vin_negative_alerts
            (user_id, user_name, nick_name, field_name, old_value, new_value, delta, sql_user, sql_thread)
        VALUES
            (NEW.id, NEW.user_name, NEW.nick_name, 'vin', OLD.vin, NEW.vin, NEW.vin - OLD.vin,
             CURRENT_USER(), CONNECTION_ID());
    END IF;

    -- xu (BanCa fish currency)
    IF NEW.xu < 0 AND OLD.xu >= 0 THEN
        INSERT INTO vinplay.vin_negative_alerts
            (user_id, user_name, nick_name, field_name, old_value, new_value, delta, sql_user, sql_thread)
        VALUES
            (NEW.id, NEW.user_name, NEW.nick_name, 'xu', OLD.xu, NEW.xu, NEW.xu - OLD.xu,
             CURRENT_USER(), CONNECTION_ID());
    END IF;

    -- safe (vault)
    IF NEW.safe < 0 AND OLD.safe >= 0 THEN
        INSERT INTO vinplay.vin_negative_alerts
            (user_id, user_name, nick_name, field_name, old_value, new_value, delta, sql_user, sql_thread)
        VALUES
            (NEW.id, NEW.user_name, NEW.nick_name, 'safe', OLD.safe, NEW.safe, NEW.safe - OLD.safe,
             CURRENT_USER(), CONNECTION_ID());
    END IF;

    -- money_vp (VP money / promo balance)
    IF NEW.money_vp < 0 AND OLD.money_vp >= 0 THEN
        INSERT INTO vinplay.vin_negative_alerts
            (user_id, user_name, nick_name, field_name, old_value, new_value, delta, sql_user, sql_thread)
        VALUES
            (NEW.id, NEW.user_name, NEW.nick_name, 'money_vp', OLD.money_vp, NEW.money_vp, NEW.money_vp - OLD.money_vp,
             CURRENT_USER(), CONNECTION_ID());
    END IF;
END$$

DELIMITER ;

-- Sanity-check view: open (uninvestigated) alerts
CREATE OR REPLACE VIEW vinplay.v_open_negative_balance_alerts AS
SELECT alert_id, detected_at, user_id, nick_name, field_name,
       old_value, new_value, delta, sql_user, sql_thread
FROM vinplay.vin_negative_alerts
WHERE investigated_at IS NULL
ORDER BY detected_at DESC;
