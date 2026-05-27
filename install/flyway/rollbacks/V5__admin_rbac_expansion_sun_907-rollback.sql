-- Rollback for V5__admin_rbac_expansion_sun_907.sql — SUN-907.
-- Removes the 7 new permission_keys and any role_permissions that
-- reference them. Safe to re-run: DELETE with no existing row is a
-- no-op.
--
-- Only rolls back the INSERTs V5 made; does NOT revert any permission
-- edits an admin may have made through the CMS after V5 landed. If the
-- CMS added extra grants on these keys, they are also removed. This is
-- expected — without the keys present the CMS grants are dangling anyway.

USE vinplay_admin;

DELETE rp FROM role_permissions rp
  JOIN admin_permissions p ON p.id = rp.permission_id
 WHERE p.permission_key IN (
       'game.rtp.manage','game.rtp.view','user.manage',
       'cashback.view','cashback.manage','agent.manage','finance.credit_wallet');

DELETE FROM admin_permissions
 WHERE permission_key IN (
       'game.rtp.manage','game.rtp.view','user.manage',
       'cashback.view','cashback.manage','agent.manage','finance.credit_wallet');
