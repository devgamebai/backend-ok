-- SUN-1102 follow-up — restore visibility for zero-amount cascade rows.
--
-- Background: pre-SUN-1102 the rebate engine wrote a row for every bet
-- regardless of whether commission resolved to >0 (status=PAID amount=0).
-- This let admins see "this game played, this agent earned 0 because their
-- rate is 0%" — required by the agency-rolling page.
--
-- SUN-1102 tried to use status='TRACKED' for those rows but the enum didn't
-- include TRACKED, so writes failed silently with "Data truncated for column
-- 'status'" under STRICT_TRANS_TABLES. Net effect: zero-amount bets stopped
-- being recorded entirely → admin /rolling page lost visibility for any game
-- where the agent's commission rate is 0%.
--
-- This migration adds TRACKED so the writer can land those rows correctly.
-- A companion code change re-enables the write path (LogMoneyUserExtraProcessor).
--
-- Metadata-only on InnoDB. Idempotent (re-running with TRACKED already
-- present is a no-op).

ALTER TABLE vinplay.rebate_logs
    MODIFY COLUMN status enum('PENDING','APPROVED','PAID','REJECTED','REVERSED','TRACKED')
    NOT NULL DEFAULT 'PENDING';
