-- SUN-1054 — Idempotency + audit log for BanCa-to-commission bridge.
--
-- BanCa (C# .NET, cgame DB, own currency) POSTs one event per fish
-- session end to c=9854 LogBetCommission. The processor calls
-- RealTimeCommission.calculate(...) exactly like LogMoneyUserExtraProcessor
-- does for Java games, but gets its input from this HTTP bridge instead
-- of an RMQ LogMoneyUserExtra message.
--
-- session_id is a BanCa-issued string unique per fish session. PRIMARY KEY
-- makes the POST idempotent — network retries from BanCa are safe.
--
-- Columns beyond the idempotency key are audit trail for ops: after a
-- complaint "my fish session didn't pay commission", we look here first
-- to confirm (a) BanCa sent the event, (b) what turnover it sent, (c) the
-- subsequent rebate_logs rows that RealTimeCommission wrote.

CREATE TABLE IF NOT EXISTS vinplay.banca_bet_commission_log (
    session_id        VARCHAR(64)  NOT NULL,
    nickname          VARCHAR(64)  NOT NULL,
    game_key          VARCHAR(32)  NOT NULL DEFAULT 'fish',
    bet_amount        BIGINT       NOT NULL COMMENT 'real turnover in VIN — fed to RealTimeCommission',
    deposit_in_vin    BIGINT       NOT NULL DEFAULT 0 COMMENT 'audit only',
    withdraw_out_vin  BIGINT       NOT NULL DEFAULT 0 COMMENT 'audit only',
    net_pl_vin        BIGINT       NOT NULL DEFAULT 0 COMMENT 'audit only — NOT used for commission calc',
    session_started_at DATETIME    NULL,
    session_ended_at  DATETIME     NULL,
    processed_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_ip         VARCHAR(45)  NULL,
    PRIMARY KEY (session_id),
    KEY idx_nickname_ended (nickname, session_ended_at),
    KEY idx_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='SUN-1054 BanCa fish session commission bridge idempotency + audit log';
