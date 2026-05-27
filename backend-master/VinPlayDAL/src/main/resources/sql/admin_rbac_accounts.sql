-- Admin RBAC dev accounts seed.
--
-- Purpose:
--   Create 2 reusable admin accounts for dev/test and assign one RBAC role each.
--
-- Accounts:
--   admin_van_hanh -> role van_hanh
--   admin_tai_vu   -> role tai_vu
--
-- How to run:
--   1) Apply admin_rbac_schema.sql first.
--   2) Run this file against the database that contains RBAC tables
--      admin_roles/admin_user_roles.
--
-- Examples:
--   Local repo DB usually stores RBAC tables in vinplay_admin:
--     mysql ... vinplay_admin < admin_rbac_dev_accounts.sql
--
--   CasDev currently has RBAC tables in vinplay:
--     mysql ... vinplay < admin_rbac_dev_accounts.sql
--
-- Notes:
--   - Admin users are always stored in vinplay_admin.user.
--   - RBAC tables are intentionally unqualified so the selected database controls
--     where roles and role mappings are written.
--   - Script is idempotent: safe to run multiple times.

SET @admin_pw_md5 := '0192023a7bbd73250516f069df18b500';
SET @vh_user := 'admin_van_hanh';
SET @tv_user := 'admin_tai_vu';

INSERT INTO vinplay_admin.user (UserName, FullName, Password, Status, ParentID, Active, isSuper, Balance)
SELECT @vh_user, 'Admin Van Hanh', @admin_pw_md5, 'A', 0, 1, 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay_admin.user WHERE UserName = @vh_user
);

INSERT INTO vinplay_admin.user (UserName, FullName, Password, Status, ParentID, Active, isSuper, Balance)
SELECT @tv_user, 'Admin Tai Vu', @admin_pw_md5, 'A', 0, 1, 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM vinplay_admin.user WHERE UserName = @tv_user
);

UPDATE vinplay_admin.user
SET Password = @admin_pw_md5,
    Status = 'A',
    Active = 1,
    isSuper = 0
WHERE UserName IN (@vh_user, @tv_user);

SET @vh_admin_id := (
    SELECT ID FROM vinplay_admin.user WHERE UserName = @vh_user ORDER BY ID LIMIT 1
);
SET @tv_admin_id := (
    SELECT ID FROM vinplay_admin.user WHERE UserName = @tv_user ORDER BY ID LIMIT 1
);
SET @vh_role_id := (
    SELECT id FROM admin_roles WHERE name = 'van_hanh' AND status = 1 ORDER BY id LIMIT 1
);
SET @tv_role_id := (
    SELECT id FROM admin_roles WHERE name = 'tai_vu' AND status = 1 ORDER BY id LIMIT 1
);

DELETE FROM admin_user_roles
WHERE admin_id IN (@vh_admin_id, @tv_admin_id);

INSERT INTO admin_user_roles (admin_id, role_id, assigned_by)
SELECT @vh_admin_id, @vh_role_id, 'dev_seed'
WHERE @vh_admin_id IS NOT NULL AND @vh_role_id IS NOT NULL;

INSERT INTO admin_user_roles (admin_id, role_id, assigned_by)
SELECT @tv_admin_id, @tv_role_id, 'dev_seed'
WHERE @tv_admin_id IS NOT NULL AND @tv_role_id IS NOT NULL;

SELECT u.ID,
       u.UserName,
       u.FullName,
       u.Status,
       r.name AS role_name
FROM vinplay_admin.user u
JOIN admin_user_roles aur ON aur.admin_id = u.ID
JOIN admin_roles r ON r.id = aur.role_id
WHERE u.UserName IN (@vh_user, @tv_user)
ORDER BY u.UserName;
