-- SUN-13xx Phase 6 — wallet unification: move VIP point state out of `users`.
--
-- Creates a dedicated `vip_points` table (current + lifetime counters per
-- user) and an append-only `vip_point_log` audit trail.  Both columns
-- `users.vip_point` and `users.vip_point_save` are seeded into the new
-- table so reads can switch over before the columns are dropped (see
-- `20260615_phase6_drop_vip_columns.sql`, 14-day delay).
--
-- Per RFC §Phase 6 + RFC v2 addendum §1 / §6 / open question #4:
--   - Light append-only log is sufficient — VIP points are not money,
--     no double-entry. money_transaction stays VND-only.
--   - currency='VP' on money_account is reserved for a future migration
--     when we want full ledger semantics; this phase intentionally does
--     NOT route VP through money_transaction.
--
-- Additive only. No drop. No behaviour change in existing code paths.
USE vinplay;

-- 1. vip_points — single row per user. current_points may decrement on
--    redemption; lifetime_points is monotonic non-decreasing.
CREATE TABLE IF NOT EXISTS vip_points (
    user_id          BIGINT       NOT NULL,
    current_points   BIGINT       NOT NULL DEFAULT 0,
    lifetime_points  BIGINT       NOT NULL DEFAULT 0,
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                  ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    KEY idx_vp_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. vip_point_log — append-only audit trail.
--    `source_tx_id` is the idempotency key (nullable for legacy seeding rows).
--    `reason` is a short canonical tag matching VipPointsService.SOURCE_*.
CREATE TABLE IF NOT EXISTS vip_point_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    delta            BIGINT       NOT NULL,
    reason           VARCHAR(64)  NOT NULL,
    source_tx_id     VARCHAR(128) NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_vp_log_idem (reason, source_tx_id),
    KEY idx_vp_log_user_created (user_id, created_at),
    KEY idx_vp_log_reason (reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Seed from current users.vip_point / vip_point_save.
--    `vip_point`     -> current_points (redeemable balance)
--    `vip_point_save` -> lifetime_points (cumulative earned; legacy column
--                       was only ever monotonic-incremented in production).
--    Skip rows already migrated (idempotent across re-runs).
INSERT INTO vip_points (user_id, current_points, lifetime_points, updated_at)
SELECT  u.id,
        COALESCE(u.vip_point, 0),
        COALESCE(u.vip_point_save, 0),
        NOW(6)
FROM    users u
LEFT JOIN vip_points vp ON vp.user_id = u.id
WHERE   vp.user_id IS NULL;

-- 4. Seed one audit row per user so the log shows the pre-migration
--    starting balance.  reason='PHASE6_BACKFILL' is unique per user.
INSERT IGNORE INTO vip_point_log (user_id, delta, reason, source_tx_id, created_at)
SELECT  vp.user_id,
        vp.lifetime_points,
        'PHASE6_BACKFILL',
        CONCAT('seed:', vp.user_id),
        NOW(6)
FROM    vip_points vp
WHERE   vp.lifetime_points > 0;
