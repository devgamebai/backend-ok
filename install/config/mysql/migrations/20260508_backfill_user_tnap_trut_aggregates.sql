-- Backfill users.t_nap / users.t_rut / users.nap_times / users.rut_times
-- and the log_report_user daily roll-up from the canonical sources
-- (deposit_transactions, bank_withdrawals). c=9910 ListUsersProcessor
-- reads these columns directly so without this backfill the admin
-- "Quản lý user" panel shows 0 for every player despite real activity.
--
-- Idempotent: rerun-safe via:
--   - users columns RESET to 0 then SUM-set from source tables
--   - log_report_user UPSERT with INSERT ... ON DUPLICATE KEY UPDATE,
--     keyed on (nick_name, time_report) UNIQUE (nickname_time)
--
-- Sources of truth (after this migration the application also writes
-- via DepositReportAggregator on every approve, so this backfill only
-- has to fix the historical gap up to the cutover):
--   - deposit_transactions WHERE status = 'APPROVED'
--   - bank_withdrawals     WHERE status = 'APPROVED'
--
-- Reject / pending / expired rows are intentionally excluded — only
-- approved money movement counts toward t_nap / t_rut.

USE vinplay;

-- ---- 1. users.t_nap / nap_times ---------------------------------------
-- Reset then SUM-set from approved deposits. Players with no approved
-- deposit get 0 / 0.

UPDATE users u
LEFT JOIN (
    SELECT user_id,
           SUM(amount) AS sum_amount,
           COUNT(*)    AS cnt
    FROM   deposit_transactions
    WHERE  status = 'APPROVED'
    GROUP BY user_id
) d ON d.user_id = u.id
SET u.t_nap     = COALESCE(d.sum_amount, 0),
    u.nap_times = COALESCE(d.cnt, 0);

-- ---- 2. users.t_rut / rut_times ---------------------------------------

UPDATE users u
LEFT JOIN (
    SELECT user_id,
           SUM(amount_krw) AS sum_amount,
           COUNT(*)        AS cnt
    FROM   bank_withdrawals
    WHERE  status = 'APPROVED'
    GROUP BY user_id
) w ON w.user_id = u.id
SET u.t_rut     = COALESCE(w.sum_amount, 0),
    u.rut_times = COALESCE(w.cnt, 0);

-- ---- 3. log_report_user roll-up (deposit) -----------------------------
-- Per-day, per-player sum from deposit_transactions. Use the row's
-- created_at date as time_report. Composite UNIQUE (nick_name,
-- time_report) so the upsert merges into any pre-existing daily row.

INSERT INTO log_report_user (time_report, nick_name, user_id, deposit, withdraw, t_bonus)
SELECT DATE(d.created_at), d.nick_name, d.user_id, SUM(d.amount), 0, 0
FROM   deposit_transactions d
WHERE  d.status = 'APPROVED'
GROUP BY DATE(d.created_at), d.nick_name, d.user_id
ON DUPLICATE KEY UPDATE
    deposit = VALUES(deposit),
    user_id = COALESCE(user_id, VALUES(user_id));

-- ---- 4. log_report_user roll-up (withdraw) ----------------------------
-- bank_withdrawals.created_at = request time, processed_at = approval
-- time. Use processed_at when present (real settlement date), else
-- created_at (covers any APPROVED row that somehow has NULL
-- processed_at).

INSERT INTO log_report_user (time_report, nick_name, user_id, deposit, withdraw, t_bonus)
SELECT DATE(COALESCE(w.processed_at, w.created_at)), w.nick_name, w.user_id, 0, SUM(w.amount_krw), 0
FROM   bank_withdrawals w
WHERE  w.status = 'APPROVED'
GROUP BY DATE(COALESCE(w.processed_at, w.created_at)), w.nick_name, w.user_id
ON DUPLICATE KEY UPDATE
    withdraw = VALUES(withdraw),
    user_id  = COALESCE(user_id, VALUES(user_id));
