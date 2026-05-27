-- Migration: Clear isSuper field
--
-- Context:
--   isSuper (vinplay_admin.user) is a legacy field that previously granted
--   full admin access as a bypass of the RBAC system.
--   As of this migration, admin privilege is determined purely by RBAC
--   role name ('admin' or 'super_admin') in admin_user_roles.
--   isSuper is now meaningless and must be cleared.
--
-- Pre-condition: Deploy updated backend code BEFORE running this script.
--   The new code no longer reads isSuper for permission checks.
--
-- Step 1: Safety check — find any user with isSuper=1 who does NOT
--         have an 'admin' or 'super_admin' RBAC role assigned.
--         This query MUST return 0 rows before proceeding.
--         If rows are returned, assign the correct role first.

SELECT u.ID, u.UserName, u.isSuper
FROM vinplay_admin.user u
WHERE u.isSuper = 1
  AND NOT EXISTS (
      SELECT 1
      FROM admin_user_roles aur
      JOIN admin_roles ar ON ar.id = aur.role_id
      WHERE aur.admin_id = u.ID
        AND ar.name IN ('admin', 'super_admin')
        AND ar.status = 1
  );

-- Step 2: Apply migration — reset all isSuper to 0.
--         Only proceed after Step 1 returns empty.

UPDATE vinplay_admin.user
SET isSuper = 0
WHERE isSuper = 1;

-- Verify
SELECT COUNT(*) AS remaining_super FROM vinplay_admin.user WHERE isSuper = 1;
-- Expected: 0
