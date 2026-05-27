-- =============================================================================
-- 20260518_awc_event_log.sql
--
-- Mirror of gsc_event_log for AWC seamless-wallet callbacks.
--
-- Captures every inbound AWC /awc/callback request BEFORE wallet logic
-- runs, then UPDATE the same row when processing completes. Lets ops
-- answer "did AWC send us this event?" in one query and feeds the
-- AWC latency Grafana dashboard with the same shape the GSC dashboard
-- already has.
--
-- Per-action breakdown:
--   awc_action ∈ ('getBalance','bet','settle','cancelBet','cancelBetNSettle',
--                 'voidSettle','resettle','refund','unsettle','betNSettle','tip',
--                 'updateBetLimit','adjust','rollback','adjustBalance')
--
-- Idempotency: AWC retries on the same platform_tx_id are common (network
-- timeouts). UNIQUE (platform_tx_id, awc_action) collapses retries to
-- one row; the caller-side detects the dup and replays the cached
-- response (same as GscEventLogger.tryLogRequest's handling of
-- (wager_code, endpoint) dups).
--
-- Idempotent migration: CREATE TABLE IF NOT EXISTS. Re-run is safe.
-- =============================================================================

CREATE TABLE IF NOT EXISTS vinplay.awc_event_log (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    awc_action          VARCHAR(64)  NOT NULL,
    received_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    -- request_headers stays JSON — we control the producer (sanitizeHeaders)
    -- and emit only well-formed JSON. raw_payload comes from AWC and MAY be
    -- malformed (vendor-side truncation / encoding bug) — keep it as
    -- LONGTEXT so MySQL never rejects an audit INSERT on JSON validation.
    -- The data still parses as JSON for app-side consumers when valid.
    request_headers     JSON         DEFAULT NULL,
    raw_payload         LONGTEXT     NOT NULL,

    -- Denormalized identifiers (best-effort extract; nullable on parse fail).
    awc_user_id         VARCHAR(64)  DEFAULT NULL,
    member_account      VARCHAR(128) DEFAULT NULL,
    platform            VARCHAR(32)  DEFAULT NULL,
    game_code           VARCHAR(128) DEFAULT NULL,
    game_type           VARCHAR(32)  DEFAULT NULL,
    round_id            VARCHAR(128) DEFAULT NULL,
    platform_tx_id      VARCHAR(128) DEFAULT NULL,
    bet_amount          DECIMAL(20,4) DEFAULT NULL,
    win_amount          DECIMAL(20,4) DEFAULT NULL,
    currency            VARCHAR(8)   DEFAULT NULL,
    transaction_count   INT          DEFAULT NULL,

    -- Processing state (mirrors GSC).
    processing_status   ENUM('RECEIVED','COMPLETED','FAILED') NOT NULL DEFAULT 'RECEIVED',
    processed_at        DATETIME(3)  DEFAULT NULL,
    response_status     INT          DEFAULT NULL,
    response_payload    JSON         DEFAULT NULL,
    response_at         DATETIME(3)  DEFAULT NULL,
    last_error          TEXT         DEFAULT NULL,

    PRIMARY KEY (id),
    -- Dedup AWC retries. (platform_tx_id, action) lets the same wager
    -- have separate bet + settle rows without colliding.
    UNIQUE KEY uk_tx_action (platform_tx_id, awc_action),
    KEY idx_member_received (member_account, received_at DESC),
    KEY idx_round           (round_id),
    KEY idx_received        (received_at),
    KEY idx_status_received (processing_status, received_at DESC),
    KEY idx_platform_action (platform, awc_action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
