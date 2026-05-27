-- SUN-1205/1206 (audit-gap fix): allow one gsc_event_log row per
-- (wager_code, endpoint) so that withdraw, deposit, cancel, and
-- rollback events for the same wager can all be captured. The
-- previous schema enforced UNIQUE(gsc_wager_code) alone, which
-- meant only the first endpoint to fire for a wager (always the
-- BET via WithdrawProcess) was logged; the deposit/settle and
-- subsequent events silently collided on duplicate-key and the
-- audit trail lost everything after the BET.

-- Drop the single-column unique index. Name `uk_gsc_wager` confirmed
-- via SHOW INDEX FROM gsc_event_log on staging.
ALTER TABLE vinplay.gsc_event_log
  DROP INDEX uk_gsc_wager;

-- Replace with a composite unique index. Idempotency now keys on
-- (wager_code, endpoint) — a GSC retry of the *same* endpoint with
-- the *same* wager still collides as intended (we replay the cached
-- response), but a deposit landing after a withdraw inserts cleanly.
ALTER TABLE vinplay.gsc_event_log
  ADD UNIQUE KEY uk_wager_endpoint (gsc_wager_code, gsc_endpoint);
