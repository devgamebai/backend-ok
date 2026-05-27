-- =============================================================================
-- SUN-930: Database Architecture Optimization
-- Applied: 2026-04-17 on staging
-- =============================================================================

-- ─── P0-1: Clean orphan data ────────────────────────────────────────────────
UPDATE vinplay_admin.useragent SET parentid = 0 WHERE id = 151 AND parentid = -1;

UPDATE vinplay.users u
JOIN vinplay_admin.useragent a ON a.username COLLATE utf8mb3_general_ci = u.user_name
SET u.dai_ly = 1
WHERE u.dai_ly = 0 AND a.active = 1 AND a.role = 'agent';

UPDATE vinplay.users SET parent_agent_id = NULL
WHERE parent_agent_id IS NOT NULL
  AND parent_agent_id NOT IN (SELECT id FROM vinplay_admin.useragent);

DELETE FROM vinplay.rebate_config
WHERE agent_user_id NOT IN (SELECT id FROM vinplay_admin.useragent);

-- ─── P0-2: Add FKs on financial tables ──────────────────────────────────────
ALTER TABLE vinplay.user_volume_tracking MODIFY user_id BIGINT NOT NULL;

ALTER TABLE vinplay.deposit_transactions
  ADD CONSTRAINT fk_deposit_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE vinplay.bank_withdrawals
  ADD CONSTRAINT fk_bankwd_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE vinplay.crypto_withdrawals
  ADD CONSTRAINT fk_cryptowd_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE vinplay.crypto_deposits
  ADD CONSTRAINT fk_cryptodep_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE vinplay.users_bank
  ADD CONSTRAINT fk_usersbank_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE vinplay.user_volume_tracking
  ADD CONSTRAINT fk_uvt_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE vinplay.user_rtp_override
  ADD CONSTRAINT fk_rtp_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE vinplay.user_threat_score
  ADD CONSTRAINT fk_threat_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- ─── P0-3: Fix misleading index name ────────────────────────────────────────
ALTER TABLE vinplay.users DROP INDEX referral_code_UNIQUE;
ALTER TABLE vinplay.users ADD INDEX idx_referral_code (referral_code);

-- ─── P1-1: Composite indexes on high-volume tables ─────────────────────────
ALTER TABLE vinplay.freeze_money ADD INDEX idx_user_status (user_id, status);
ALTER TABLE vinplay.freeze_money ADD INDEX idx_status_time (status, create_time);
ALTER TABLE vinplay.rebate_logs ADD INDEX idx_agent_period (agent_user_id, period_start);
ALTER TABLE vinplay.deposit_audit_logs ADD INDEX idx_tx_time (tx_id, created_at);

-- ─── P1-2: Add PKs to tables missing them ──────────────────────────────────
ALTER TABLE vinplay.ebetuser ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE vinplay.sbouser ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- ─── P3-1: Drop shadow/dead tables ─────────────────────────────────────────
DROP TABLE IF EXISTS vinplay.useragent;
DROP TABLE IF EXISTS vinplay.users_game568win;

-- ─── P3-2: Remove copy-paste stored procs from wrong schema ────────────────
DROP PROCEDURE IF EXISTS vinplay_admin.SP_Login;
DROP PROCEDURE IF EXISTS vinplay_admin.SP_Register;
DROP PROCEDURE IF EXISTS vinplay_admin.SP_LoginFacebook;
DROP PROCEDURE IF EXISTS vinplay_admin.SP_LoginByDevice;
DROP PROCEDURE IF EXISTS vinplay_admin.SP_GIFT_CODE;
DROP PROCEDURE IF EXISTS vinplay_admin.SP_CashoutHistories;

-- ─── P4-1: Fix cgame schema default charset ────────────────────────────────
ALTER DATABASE cgame CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ─── P4-2: Cross-schema link cgame.users → vinplay.users ───────────────────
ALTER TABLE cgame.users ADD COLUMN vinplay_user_id BIGINT DEFAULT NULL;
CREATE INDEX idx_vinplay_uid ON cgame.users (vinplay_user_id);
UPDATE cgame.users cu
JOIN vinplay.users vu ON LOWER(cu.nickname) = LOWER(vu.nick_name COLLATE utf8mb3_general_ci)
SET cu.vinplay_user_id = vu.id
WHERE cu.vinplay_user_id IS NULL;

-- ─── P4-3: Convert MyISAM to InnoDB ────────────────────────────────────────
ALTER TABLE cgame.card ENGINE=InnoDB;
ALTER TABLE cgame.daily ENGINE=InnoDB;
ALTER TABLE cgame.daily_cp ENGINE=InnoDB;
ALTER TABLE cgame.daily_game ENGINE=InnoDB;
ALTER TABLE cgame.minigame ENGINE=InnoDB;
