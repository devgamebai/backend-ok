-- SUN-13xx Phase 7 — drop the legacy analytics columns once readers
-- have moved to the derived views.
--
-- Apply ONLY after `20260601_phase7_analytics_view_switch.sql` has been
-- live for >= 14 days AND a callsite audit confirms no SELECT, INSERT,
-- UPDATE, ORDER BY, WHERE, or PHP-side ResultSet read references the
-- old column names anywhere:
--   grep -RIn '\brecharge_money\b' backend-master/ www/
--   grep -RIn '\bgift_total\b'     backend-master/ www/
--
-- Heads-up — risky WHERE clauses must be migrated first:
--   * VinPlayDAL/.../GetUserIndexDAOImpl.java has
--     `WHERE create_time>=? AND create_time<=? AND recharge_money > 0`
--     used for "has deposited" counters — replace with a JOIN against
--     v_derived_deposit_total before this drop runs.
--   * Several ORDER BY recharge_money in ListAllAgentsUnderAgentProcessor
--     and ListAgentProcessor — switch to ORDER BY
--     v_derived_deposit_total.deposit_total via a LEFT JOIN.
--
-- Drop is fix-forward only.  Rollback = DR snapshot restore.
USE vinplay;

ALTER TABLE users
    DROP COLUMN recharge_money,
    DROP COLUMN gift_total;
