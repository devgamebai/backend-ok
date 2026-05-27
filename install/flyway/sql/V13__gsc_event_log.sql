-- SUN-1121 (CQRS / GSC audit Phase 1) — durable inbound event log.
--
-- Captures every GSC seamless-wallet API call (currently /withdraw —
-- /deposit and /cancel can be hooked in later) with the raw POST body
-- and headers, BEFORE any wallet/commission/Mongo logic runs. UPDATE
-- once processing completes with the response payload and status.
--
-- This is finance audit data. The contract:
--   * Persist before processing — INSERT happens before any wallet or
--     Mongo write touches the request. If the row exists, GSC sent us
--     the event. If not, GSC didn't reach us.
--   * Append-only on financial fields — once written, raw_payload /
--     extracted amount fields / response_payload never change.
--     processing_status + response columns are the only mutable fields,
--     and they only flip RECEIVED → COMPLETED / FAILED once.
--   * Idempotent — UNIQUE on gsc_wager_code rejects GSC retries of the
--     same wager. The handler looks up the existing row and replays the
--     cached response_payload instead of re-running the cascade.
--
-- raw_payload is JSON. response_payload is the JSON we returned to GSC.
-- request_headers excludes Authorization / cookies — sanitized at write
-- time by GscEventLogger to keep secrets out of audit storage.
--
-- Idempotent: CREATE TABLE IF NOT EXISTS. Safe to re-run.

CREATE TABLE IF NOT EXISTS vinplay.gsc_event_log (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,

    -- Receipt
    gsc_endpoint        VARCHAR(64)  NOT NULL,
    received_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    request_headers     JSON         NULL,
    raw_payload         JSON         NOT NULL,

    -- Extracted identifiers (denormalized for queries; from first
    -- transaction in multi-tx requests)
    gsc_wager_code      VARCHAR(128) NULL,
    gsc_round_id        VARCHAR(128) NULL,
    member_account      VARCHAR(128) NULL,
    product_code        INT          NULL,
    game_code           VARCHAR(128) NULL,
    bet_amount          DECIMAL(20,4) NULL,
    valid_bet_amount    DECIMAL(20,4) NULL,
    prize_amount        DECIMAL(20,4) NULL,
    currency            VARCHAR(8)   NULL,
    transaction_count   INT          NULL,        -- # of tx objects in this request

    -- Processing
    processing_status   ENUM('RECEIVED','COMPLETED','FAILED') NOT NULL DEFAULT 'RECEIVED',
    processed_at        DATETIME(3)  NULL,
    response_status     INT          NULL,
    response_payload    JSON         NULL,
    response_at         DATETIME(3)  NULL,
    last_error          TEXT         NULL,

    UNIQUE KEY uk_gsc_wager (gsc_wager_code),
    INDEX idx_member_received (member_account, received_at DESC),
    INDEX idx_round (gsc_round_id),
    INDEX idx_received (received_at),
    INDEX idx_status_received (processing_status, received_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
