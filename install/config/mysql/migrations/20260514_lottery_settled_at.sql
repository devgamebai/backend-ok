-- SUN-LOTTERY PR-2/3 — add settled_at flag to lode and result_lottery.
--
-- Closes anti-cheat audit finding L-1 (pre-settle result reveal).
-- Engine-level wiring: LotterySettleService.settleAll() stamps lode.settled_at
-- per-row; DrawIngest.runOnce() stamps result_lottery.settled_at as its LAST
-- write. REST history endpoints filter WHERE settled_at IS NOT NULL so today's
-- payload is structurally invisible until the settle loop has run to completion.
--
-- Idempotent — safe to re-run. The ADD COLUMN steps fail-soft via the
-- backfill UPDATE: if the column already exists, the UPDATE turns into a
-- partial no-op (rows already populated remain unchanged). The CREATE INDEX
-- statements use IF NOT EXISTS where MariaDB supports it; on MySQL 8 the
-- migration runner should wrap each DDL in TRY/IGNORE_DUPLICATE.
--
-- Backfill strategy:
--   - lode.settled_at  := updated_date  for rows where prize IS NOT NULL
--     (these are historical settled bets; updated_date is the last touch)
--   - result_lottery.settled_at := created_date + 5min for every row
--     (pre-migration rows had no concept of pre-vs-post-settle; we treat them
--      as long-settled so the gate immediately re-opens)
--
-- Rollback: NO-OP allowed — code path falls back to legacy when settled_at
-- IS NULL. Dropping the columns is safe; the engine reads NULL-tolerant.

ALTER TABLE vinplay_minigame.lode
    ADD COLUMN settled_at TIMESTAMP NULL AFTER prize;

ALTER TABLE vinplay_minigame.result_lottery
    ADD COLUMN settled_at TIMESTAMP NULL AFTER created_date;

-- Backfill: existing settled bets get their updated_date as settled_at.
UPDATE vinplay_minigame.lode
   SET settled_at = updated_date
 WHERE prize IS NOT NULL
   AND settled_at IS NULL;

-- Backfill: existing result rows assumed settled +5min after scrape commit.
UPDATE vinplay_minigame.result_lottery
   SET settled_at = DATE_ADD(created_date, INTERVAL 5 MINUTE)
 WHERE settled_at IS NULL;

-- Indexes for the bet-gate hot path (SettledFlagStore.isSettled per-call)
-- and the REST list-settled history endpoint.
CREATE INDEX idx_lode_settled
    ON vinplay_minigame.lode (settled_at);

CREATE INDEX idx_result_lottery_settled
    ON vinplay_minigame.result_lottery (settled_at);
