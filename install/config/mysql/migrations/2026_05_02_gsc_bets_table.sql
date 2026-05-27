-- =============================================================================
-- Cross-store traceability — GSC bets MySQL table (audit follow-up #5)
--
-- Today the GSC bet ledger lives in MongoDB (`win123club.log_gsc_bets`) and
-- carries the only persistent record of LIVE-casino seamless bets:
--   - Per-bet history surfaced by c=303 / c=9843 read endpoints
--   - The reconciler's `settled=false` scan target
--   - The settle-update / cancel / rollback application target
--
-- The cross-store gap: there is no FK from MySQL `money_gateway_log.tx_id`
-- (the wallet ledger) to the Mongo `wager_code`. A money-movement audit
-- ("did we pay this player for wager X?") can only be answered by
-- application-level join. Missed Mongo writes / late writes are invisible
-- at the SQL audit layer.
--
-- This migration creates `vinplay.gsc_bets` — a MySQL mirror of the Mongo
-- collection. Phase 5 prep gate 5p2 already makes the Mongo write async;
-- the new MySQL DAO hooks in alongside it (dual-write, flag-gated). After
-- a 2-week soak + backfill, reads can flip from Mongo to MySQL (separate
-- task — NOT this migration).
--
-- Schema:
--   - `wager_code` UNIQUE (the natural key — same as Mongo's $set match)
--   - `(user_id, create_time DESC)` for c=303 / c=9843 history reads
--   - `(user_name, create_time DESC)` for legacy compatibility (Mongo path
--      keys on user_name; user_id requires a users JOIN we don't have today)
--   - `(settled, create_time)` for the reconciler's unsettled scan
--   - `event_key` index for SUN-LIVE-HIST upsert path
--
-- Idempotency: CREATE TABLE IF NOT EXISTS, safe to re-run.
--
-- See:
--   - `backend-master/VinPlayDAL/src/main/java/com/vinplay/dal/dao/GscBetsDao.java`
--   - `bin/backfill_gsc_bets_to_mysql.sh`
--   - `docs/MONEY_LEDGER_OPEN_TASKS.md` § "Cross-store gap closure"
-- =============================================================================

CREATE TABLE IF NOT EXISTS vinplay.gsc_bets (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    user_name       VARCHAR(64) NOT NULL,
    nick_name       VARCHAR(64),
    bet_value       BIGINT NOT NULL DEFAULT 0,
    prize           BIGINT NOT NULL DEFAULT 0,
    fee             BIGINT NOT NULL DEFAULT 0,
    product_code    INT NOT NULL,
    game_code       VARCHAR(128),
    game_key        VARCHAR(64),
    game_name       VARCHAR(255),
    txn_id          VARCHAR(128),
    wager_code      VARCHAR(128) NOT NULL,
    currency        VARCHAR(16),
    bet_type        VARCHAR(32),
    settled         TINYINT NOT NULL DEFAULT 0,
    settle_time     DATETIME(3) NULL,
    settle_txn_id   VARCHAR(128),
    vendor_game_id  VARCHAR(128),                                 -- SUN-1196 freespin chain
    event_key       VARCHAR(128),                                 -- SUN-LIVE-HIST de-dup key
    create_time     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    time_log        DATETIME(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_wager_code (wager_code),                        -- enforces 1 row per wager
    KEY idx_user_create (user_id, create_time DESC),
    KEY idx_user_name_create (user_name, create_time DESC),       -- legacy compat
    KEY idx_settled_create (settled, create_time),                -- reconciler scan
    KEY idx_event_key (event_key)                                 -- SUN-LIVE-HIST upsert match
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='MySQL mirror of Mongo log_gsc_bets — flag-gated dual-write target. See migration header.';
