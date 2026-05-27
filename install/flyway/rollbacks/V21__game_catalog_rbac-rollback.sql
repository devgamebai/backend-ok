-- Rollback for V21__game_catalog_rbac.sql
-- Removes the 2 new permission_keys + any role_permissions rows that
-- reference them. Safe to re-run.
--
-- ======================================================================
-- !! STOP — READ BEFORE RUNNING !!
-- ======================================================================
-- Running this script WITHOUT first rolling back the matching Java
-- deploy will LOCK OUT every non-superadmin from
-- c=9980..9986 (List/Toggle/Sync games + per-user game block):
--
--   • AdminPermissionRegistry still maps each command to
--     'game.catalog.view' / 'game.catalog.manage'
--   • This script DELETES those permission_key rows
--   • RbacSupport.hasPermission() returns false for missing keys
--   • The enforcement gate in VinPlayBackendMain rejects the call
--
-- Only the 'superadmin' user keeps access (via hasSuperAdminRole bypass).
--
-- REQUIRED rollback order:
--   1) Revert / redeploy backend WITHOUT the V21 registry entries
--      (or with the entries marked permission-optional)
--   2) Verify no operator is still using c=9980..9986
--   3) Run this SQL
--
-- If you ran this script first by mistake, immediately re-apply
-- V21__game_catalog_rbac.sql (idempotent) and the gate is restored.
-- ======================================================================

USE vinplay_admin;

DELETE rp FROM role_permissions rp
  JOIN admin_permissions p ON p.id = rp.permission_id
 WHERE p.permission_key IN ('game.catalog.view', 'game.catalog.manage');

DELETE FROM admin_permissions
 WHERE permission_key IN ('game.catalog.view', 'game.catalog.manage');
