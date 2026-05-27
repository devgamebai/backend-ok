-- Game Catalog RBAC — gate the unified AWC/GSC catalog admin commands
-- (c=9980..9986) behind two new permission keys. Pattern mirrors V5.
--
-- Before this migration: 7 game-catalog commands were unprotected. Any
-- admin with a valid aat could list / toggle / sync the games table and
-- add/remove per-user game blocks. The CMS surface
-- (/game-catalog, /game-catalog/user-block) was likewise hardcoded
-- "open to everyone" in the FE sidebar.
--
-- After: AdminPermissionRegistry maps each command to one of:
--   game.catalog.view    — list providers, list games, list user blocks
--   game.catalog.manage  — toggle, sync, add/remove user blocks
--
-- Default grant: ONLY the `admin` role. Game Catalog is reserved for
-- Admin / Super Admin per operator decision — game-ops (`van_hanh`),
-- finance (`tai_vu`), and viewer (`co_dong`) roles do NOT get it by
-- default. To grant another role later, use the RBAC CMS
-- (/admin-mgmt → role → assign permissions).
--
-- Note: the `superadmin` user (UserName='superadmin') auto-bypasses
-- every permission via RbacSupport.hasSuperAdminRole, regardless of
-- explicit grants. The grant below covers the broader `admin` role so
-- any user holding it (not just the superadmin account) can use the
-- section.
--
-- Safe to re-run: ON DUPLICATE KEY UPDATE + INSERT IGNORE.

USE vinplay_admin;

INSERT INTO admin_permissions
    (permission_key, permission_name, module, module_key, module_name, feature_key, feature_name, action, description, status)
VALUES
    ('game.catalog.view',   'Xem game catalog (AWC/GSC)', 'Game', 'game', 'Game', 'catalog', 'Catalog', 'view',   'List providers, list games, list per-user game blocks', 1),
    ('game.catalog.manage', 'Quan ly game catalog',       'Game', 'game', 'Game', 'catalog', 'Catalog', 'manage', 'Toggle active, sync from provider, add/remove user game block', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    description     = VALUES(description),
    status          = VALUES(status);

-- Default grant for the `admin` role only.
-- The schema snapshot grants every permission to `admin` via CROSS JOIN
-- on fresh provisioning, but on an already-deployed DB that seed won't
-- re-run, so this migration must grant the new keys explicitly.
INSERT IGNORE INTO role_permissions (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'SYSTEM_MIGRATION_V21'
  FROM admin_roles r
  JOIN admin_permissions p ON p.permission_key IN (
      'game.catalog.view',
      'game.catalog.manage'
  )
 WHERE r.name = 'admin';

-- Sanity check: expected 2 rows.
SELECT permission_key FROM admin_permissions
 WHERE permission_key IN ('game.catalog.view', 'game.catalog.manage')
 ORDER BY permission_key;
