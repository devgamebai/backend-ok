-- SUN-907 admin RBAC expansion — Flyway counterpart of the idempotent
-- changes landed in backend-master/VinPlayDAL/src/main/resources/sql/
-- admin_rbac_schema.sql on the same MR.
--
-- The resource file is the source-of-truth snapshot applied on fresh
-- DB provisioning. On an already-deployed database, nothing auto-runs
-- that file — the new INSERTs would be missed and the Java
-- AdminPermissionRegistry would point at permission_keys that don't
-- exist, failing any RBAC check for the affected commands.
--
-- This Flyway migration mirrors the exact rows from the schema file's
-- diff against the prior version, using the same ON DUPLICATE KEY UPDATE
-- / INSERT IGNORE semantics so it is safe to re-run.
--
-- Affects: vinplay_admin schema (admin_permissions + role_permissions).

USE vinplay_admin;

-- 6 new permission keys.
INSERT INTO admin_permissions
    (permission_key, permission_name, module, module_key, module_name, feature_key, feature_name, action, description, status)
VALUES
    ('game.rtp.manage',       'Quan ly RTP / Win-rate',           'Game',     'game',     'Game',     'rtp',           'RTP',           'manage', 'Update RTP config and user overrides', 1),
    ('game.rtp.view',         'Xem RTP / Win-rate',               'Game',     'game',     'Game',     'rtp',           'RTP',           'view',   'View RTP dashboard, P&L, audit', 1),
    ('user.manage',           'Quan ly user',                     'User',     'user',     'User',     'manage',        'Manage',        'manage', 'Create/update/delete user, change password', 1),
    ('cashback.view',         'Xem hoan cuoc / hoan thua',        'Cashback', 'cashback', 'Cashback', 'log',           'Log',           'view',   'View cashback logs and changelog', 1),
    ('cashback.manage',       'Quan ly hoan cuoc / hoan thua',    'Cashback', 'cashback', 'Cashback', 'config',        'Config',        'manage', 'Manage cashback config, payout, reject', 1),
    ('agent.manage',          'Quan ly dai ly',                   'Agent',    'agent',    'Agent',    'manage',        'Manage',        'manage', 'Create/promote agents', 1),
    ('finance.credit_wallet', 'Quan ly vi credit',                'Finance',  'finance',  'Finance',  'credit_wallet', 'Credit Wallet', 'manage', 'Topup/revoke credit wallet', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    description     = VALUES(description),
    status          = VALUES(status);

-- Grant newly-added perms to `van_hanh` role (game ops team).
INSERT IGNORE INTO role_permissions (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'SYSTEM_MIGRATION_V5'
  FROM admin_roles r
  JOIN admin_permissions p ON p.permission_key IN (
      'game.config',
      'game.rtp.manage',
      'game.rtp.view',
      'cashback.view'
  )
 WHERE r.name = 'van_hanh';

-- Grant newly-added perms to `tai_vu` role (finance team).
INSERT IGNORE INTO role_permissions (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'SYSTEM_MIGRATION_V5'
  FROM admin_roles r
  JOIN admin_permissions p ON p.permission_key IN (
      'cashback.view'
  )
 WHERE r.name = 'tai_vu';

-- Sanity check: expected to return 7 rows (the new permissions above).
SELECT permission_key FROM admin_permissions
 WHERE permission_key IN
       ('game.rtp.manage','game.rtp.view','user.manage',
        'cashback.view','cashback.manage','agent.manage','finance.credit_wallet')
 ORDER BY permission_key;
