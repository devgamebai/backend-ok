-- SUN-13xx Phase 6 — drop the legacy VIP point columns.
--
-- Apply ONLY after `20260601_phase6_vip_points_table.sql` has been live
-- for >= 14 days with zero readers of the old columns.  Confirm via
-- audit grep:
--   grep -RIn '\b(vip_point|vip_point_save|money_vp)\b' backend-master/
--   grep -RIn '\b(vip_point|vip_point_save|money_vp)\b' www/admin-php/
-- All matches must be either:
--   (a) routed through VipPointsService, or
--   (b) inside one of the legacy_allow files (response DTO field names),
--       OR
--   (c) a doc/comment string.
--
-- 14-day delay rationale: matches the RFC v2 addendum gate window for
-- fix-forward-only phases (§H5).  Rollback after drop is DR only —
-- restore the columns from the pre-drop snapshot.
USE vinplay;

ALTER TABLE users
    DROP COLUMN vip_point,
    DROP COLUMN vip_point_save,
    DROP COLUMN money_vp;
