-- Admin RBAC schema + seed data
-- Safe to run multiple times on MySQL 8+

CREATE TABLE IF NOT EXISTS admin_roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255) DEFAULT '',
    is_default TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_admin_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    permission_key VARCHAR(128) NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    module VARCHAR(128) DEFAULT '',
    module_key VARCHAR(128) DEFAULT '',
    module_name VARCHAR(128) DEFAULT '',
    feature_key VARCHAR(128) DEFAULT '',
    feature_name VARCHAR(128) DEFAULT '',
    action VARCHAR(64) DEFAULT '',
    description VARCHAR(500) DEFAULT '',
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_admin_permissions_key (permission_key),
    KEY idx_admin_permissions_module_status (module_name, status),
    KEY idx_admin_permissions_feature_status (feature_name, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    assigned_by VARCHAR(64) DEFAULT '',
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permissions_permission (permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES admin_roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES admin_permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_user_roles (
    admin_id INT NOT NULL,
    role_id INT NOT NULL,
    assigned_by VARCHAR(64) DEFAULT '',
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_id, role_id),
    KEY idx_admin_user_roles_role (role_id),
    CONSTRAINT fk_admin_user_roles_role FOREIGN KEY (role_id) REFERENCES admin_roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @aur_pk_columns := (
    SELECT GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',')
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.KEY_COLUMN_USAGE kcu
        ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
        AND tc.TABLE_NAME = kcu.TABLE_NAME
        AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    WHERE tc.TABLE_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'admin_user_roles'
      AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
);

SET @aur_drop_pk_sql := IF(@aur_pk_columns = 'admin_id', 'ALTER TABLE admin_user_roles DROP PRIMARY KEY', 'SELECT 1');
PREPARE stmt FROM @aur_drop_pk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @aur_pk_columns := (
    SELECT GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',')
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.KEY_COLUMN_USAGE kcu
        ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
        AND tc.TABLE_NAME = kcu.TABLE_NAME
        AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    WHERE tc.TABLE_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'admin_user_roles'
      AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
);

SET @aur_add_pk_sql := IF(@aur_pk_columns = 'admin_id,role_id', 'SELECT 1', 'ALTER TABLE admin_user_roles ADD PRIMARY KEY (admin_id, role_id)');
PREPARE stmt FROM @aur_add_pk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS admin_rbac_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_admin VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    before_json MEDIUMTEXT,
    after_json MEDIUMTEXT,
    ip_address VARCHAR(64) DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rbac_audit_created_at (created_at),
    KEY idx_rbac_audit_actor (actor_admin),
    KEY idx_rbac_audit_action (action_type),
    KEY idx_rbac_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO admin_roles (name, description, is_default, status)
VALUES
    ('admin', 'Admin - full RBAC control', 1, 1),
    ('van_hanh', 'Van hanh', 1, 1),
    ('tai_vu', 'Tai vu', 1, 1),
    ('co_dong', 'Co dong', 1, 1)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    status = 1;

INSERT INTO admin_permissions (permission_key, permission_name, module, module_key, module_name, feature_key, feature_name, action, description, status)
VALUES
    ('user.view', 'Xem user', 'User', 'user', 'User', 'profile', 'Profile', 'view', 'View user information', 1),
    ('user.edit', 'Sua user', 'User', 'user', 'User', 'profile', 'Profile', 'edit', 'Edit user information', 1),
    ('user.balance_adjust', 'Cong tru tien user', 'User', 'user', 'User', 'balance', 'Balance', 'adjust', 'Adjust user balance', 1),
    ('user.ip_device.view', 'Xem ip va thiet bi dang nhap', 'User', 'user', 'User', 'security', 'Security', 'view', 'View login IP/device info', 1),
    ('user.loginlog.view', 'Xem lich su dang nhap', 'User', 'user', 'User', 'login_log', 'Login Log', 'view', 'View user login history', 1),

    ('bet.history.view', 'Xem lich su cuoc', 'Bet', 'bet', 'Bet', 'history', 'History', 'view', 'View user betting history', 1),

    ('deposit.view', 'Xem nap tien', 'Deposit', 'deposit', 'Deposit', 'request', 'Request', 'view', 'View deposit requests', 1),
    ('deposit.approve', 'Duyet nap tien', 'Deposit', 'deposit', 'Deposit', 'request', 'Request', 'approve', 'Approve deposit requests', 1),
    ('deposit.reject', 'Tu choi nap tien', 'Deposit', 'deposit', 'Deposit', 'request', 'Request', 'reject', 'Reject deposit requests', 1),

    ('withdrawal.view', 'Xem rut tien', 'Withdrawal', 'withdrawal', 'Withdrawal', 'request', 'Request', 'view', 'View withdrawal requests', 1),
    ('withdrawal.approve', 'Duyet rut tien', 'Withdrawal', 'withdrawal', 'Withdrawal', 'request', 'Request', 'approve', 'Approve withdrawal requests', 1),
    ('withdrawal.reject', 'Tu choi rut tien', 'Withdrawal', 'withdrawal', 'Withdrawal', 'request', 'Request', 'reject', 'Reject withdrawal requests', 1),

    ('finance.deposit.view', 'Xem lich su nap tien', 'Finance', 'finance', 'Finance', 'deposit', 'Deposit', 'view', 'View deposit requests and logs', 1),
    ('finance.deposit.approve', 'Duyet nap tien', 'Finance', 'finance', 'Finance', 'deposit', 'Deposit', 'approve', 'Approve deposits', 1),
    ('finance.deposit.reject', 'Tu choi nap tien', 'Finance', 'finance', 'Finance', 'deposit', 'Deposit', 'reject', 'Reject deposits', 1),
    ('finance.withdraw.view', 'Xem lich su rut tien', 'Finance', 'finance', 'Finance', 'withdraw', 'Withdraw', 'view', 'View withdrawal requests and logs', 1),
    ('finance.withdraw.approve', 'Duyet rut tien', 'Finance', 'finance', 'Finance', 'withdraw', 'Withdraw', 'approve', 'Approve withdrawals', 1),
    ('finance.withdraw.reject', 'Tu choi rut tien', 'Finance', 'finance', 'Finance', 'withdraw', 'Withdraw', 'reject', 'Reject withdrawals', 1),
    ('finance.transaction.view', 'Xem giao dich nap rut', 'Finance', 'finance', 'Finance', 'transaction', 'Transaction', 'view', 'View merged deposit/withdraw transactions', 1),
    ('finance.winloss.view', 'Xem thong ke thang thua', 'Finance', 'finance', 'Finance', 'winloss', 'WinLoss', 'view', 'View user win/loss reports', 1),

    ('game.force_slot', 'Force slot', 'Game', 'game', 'Game', 'slot', 'Slot', 'force', 'Force slot result/config', 1),
    ('game.config', 'Config game', 'Game', 'game', 'Game', 'config', 'Config', 'manage', 'Manage game configurations', 1),
    ('game.taixiu.force', 'Force ket qua taixiu', 'Game', 'game', 'Game', 'taixiu', 'TaiXiu', 'force', 'Force TaiXiu result', 1),
    ('game.sicbo.force', 'Force ket qua sicbo', 'Game', 'game', 'Game', 'sicbo', 'Sicbo', 'force', 'Force Sicbo result', 1),
    ('game.fund.view', 'Xem quy game', 'Game', 'game', 'Game', 'fund', 'Fund', 'view', 'View game funds', 1),
    ('game.fund.manage', 'Cap nhat quy game', 'Game', 'game', 'Game', 'fund', 'Fund', 'manage', 'Update game funds', 1),
    ('game.rtp.manage', 'Quan ly RTP / Win-rate', 'Game', 'game', 'Game', 'rtp', 'RTP', 'manage', 'Update RTP config and user overrides', 1),
    ('game.rtp.view', 'Xem RTP / Win-rate', 'Game', 'game', 'Game', 'rtp', 'RTP', 'view', 'View RTP dashboard, P&L, audit', 1),
    ('game.catalog.view', 'Xem game catalog (AWC/GSC)', 'Game', 'game', 'Game', 'catalog', 'Catalog', 'view', 'List providers, list games, list per-user game blocks', 1),
    ('game.catalog.manage', 'Quan ly game catalog', 'Game', 'game', 'Game', 'catalog', 'Catalog', 'manage', 'Toggle active, sync from provider, add/remove user game block', 1),

    ('report.view', 'Xem bao cao', 'Report', 'report', 'Report', 'report', 'Report', 'view', 'View reports', 1),

    ('user.manage', 'Quan ly user', 'User', 'user', 'User', 'manage', 'Manage', 'manage', 'Create/update/delete user, change password', 1),

    ('cashback.view', 'Xem hoan cuoc / hoan thua', 'Cashback', 'cashback', 'Cashback', 'log', 'Log', 'view', 'View cashback logs and changelog', 1),
    ('cashback.manage', 'Quan ly hoan cuoc / hoan thua', 'Cashback', 'cashback', 'Cashback', 'config', 'Config', 'manage', 'Manage cashback config, payout, reject', 1),

    ('agent.manage', 'Quan ly dai ly', 'Agent', 'agent', 'Agent', 'manage', 'Manage', 'manage', 'Create/promote agents', 1),

    ('finance.credit_wallet', 'Quan ly vi credit', 'Finance', 'finance', 'Finance', 'credit_wallet', 'Credit Wallet', 'manage', 'Topup/revoke credit wallet', 1),

    ('system.config', 'Cau hinh he thong', 'System', 'system', 'System', 'config', 'Config', 'manage', 'Manage system configuration', 1),
    ('system.roles', 'Quan ly role', 'System', 'system', 'System', 'roles', 'Roles', 'manage', 'Create/update role and assign role', 1),
    ('system.permissions', 'Quan ly permission', 'System', 'system', 'System', 'permissions', 'Permissions', 'manage', 'Create/update/delete permission', 1),
    ('system.rbac_audit', 'Xem audit RBAC', 'System', 'system', 'System', 'rbac_audit', 'RBAC Audit', 'read', 'View RBAC audit logs', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    module = VALUES(module),
    module_key = VALUES(module_key),
    module_name = VALUES(module_name),
    feature_key = VALUES(feature_key),
    feature_name = VALUES(feature_name),
    action = VALUES(action),
    description = VALUES(description),
    status = VALUES(status);

INSERT IGNORE INTO role_permissions (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system_seed'
FROM admin_roles r
CROSS JOIN admin_permissions p
WHERE r.name = 'admin';

-- Reset default rights for van_hanh/tai_vu to avoid stale legacy grants
DELETE rp
FROM role_permissions rp
JOIN admin_roles r ON r.id = rp.role_id
WHERE r.name IN ('van_hanh', 'tai_vu');

INSERT IGNORE INTO role_permissions (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system_seed'
FROM admin_roles r
JOIN admin_permissions p ON p.permission_key IN (
    'user.view',
    'user.ip_device.view',
    'user.loginlog.view',
    'bet.history.view',
    'deposit.view',
    'withdrawal.view',
    'finance.deposit.view',
    'finance.withdraw.view',
    'finance.transaction.view',
    'game.taixiu.force',
    'game.sicbo.force',
    'game.force_slot',
    'game.fund.view',
    'game.config',
    'game.rtp.manage',
    'game.rtp.view',
    'cashback.view',
    'report.view'
)
WHERE r.name = 'van_hanh';

INSERT IGNORE INTO role_permissions (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system_seed'
FROM admin_roles r
JOIN admin_permissions p ON p.permission_key IN (
    'user.view',
    'user.ip_device.view',
    'user.loginlog.view',
    'bet.history.view',
    'deposit.view',
    'finance.deposit.view',
    'finance.deposit.approve',
    'finance.deposit.reject',
    'deposit.approve',
    'deposit.reject',
    'withdrawal.view',
    'finance.withdraw.view',
    'finance.withdraw.approve',
    'finance.withdraw.reject',
    'withdrawal.approve',
    'withdrawal.reject',
    'finance.transaction.view',
    'finance.winloss.view',
    'cashback.view',
    'report.view'
)
WHERE r.name = 'tai_vu';

INSERT IGNORE INTO role_permissions (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system_seed'
FROM admin_roles r
JOIN admin_permissions p ON p.permission_key IN (
    'report.view'
)
WHERE r.name = 'co_dong';

INSERT IGNORE INTO admin_user_roles (admin_id, role_id, assigned_by)
SELECT u.ID, r.id, 'system_seed'
FROM vinplay_admin.user u
JOIN admin_roles r ON r.name = 'admin'
WHERE u.UserName IN ('superadmin', 'admin');
