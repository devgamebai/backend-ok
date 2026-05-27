-- =============================================================================
-- SUN-971/972 [DB-3 + DB-4]: Add FK CASCADE for users.id + useragent.id
-- =============================================================================
-- Goal: DELETE FROM users WHERE id=X auto-cascades to ALL related tables.
--       DELETE FROM useragent WHERE id=X auto-cascades to agent tables.
--
-- Pre-requisite: Run phase1_collation.sql first.
--
-- Strategy:
--   1. Fix type mismatches (int → bigint where users.id is bigint)
--   2. Change existing RESTRICT FK → CASCADE
--   3. Add new FK CASCADE for tables with user_id but no FK
--   4. Add new FK CASCADE for agent tables → useragent
-- =============================================================================

-- ─── Step 0: Disable FK checks during migration ─────────────────────────
SET FOREIGN_KEY_CHECKS = 0;

-- ─── Step 1: Fix type mismatches (int → bigint to match users.id) ───────
-- users.id is BIGINT. These tables have user_id as INT — must match for FK.

ALTER TABLE vinplay.live_user_game MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay.rtp_auto_history MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay.user_mission MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay.users_vp_event MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay_minigame.awc_transactions MODIFY user_id INT NOT NULL COMMENT 'our internal user id';
ALTER TABLE vinplay_minigame.lucky_rotation MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay_minigame.rotate_slot_free MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay_minigame.transaction_detail_tai_xiu MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay_minigame.transaction_detail_tai_xiu_md5 MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay_minigame.transaction_detail_tai_xiu_sicbo MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay_minigame.transaction_tai_xiu MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay_minigame.transaction_tai_xiu_md5 MODIFY user_id BIGINT NOT NULL;
ALTER TABLE vinplay_minigame.transaction_tai_xiu_sicbo MODIFY user_id BIGINT NOT NULL;

-- vinplay_admin tables reference useragent.id (INT) — these are fine.
-- But User_ID in access_link/userrole may reference users.id (BIGINT):
-- access_link dropped in phase0. userrole User_ID references admin users, not players.
-- ALTER TABLE vinplay_admin.userrole MODIFY User_ID BIGINT;

-- ─── Step 2: Change existing RESTRICT FK → CASCADE ──────────────────────
-- 4 financial tables currently block user deletion.

ALTER TABLE vinplay.deposit_transactions DROP FOREIGN KEY fk_deposit_user;
ALTER TABLE vinplay.deposit_transactions ADD CONSTRAINT fk_deposit_user
  FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE vinplay.bank_withdrawals DROP FOREIGN KEY fk_bankwd_user;
ALTER TABLE vinplay.bank_withdrawals ADD CONSTRAINT fk_bankwd_user
  FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE vinplay.crypto_deposits DROP FOREIGN KEY fk_cryptodep_user;
ALTER TABLE vinplay.crypto_deposits ADD CONSTRAINT fk_cryptodep_user
  FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE vinplay.crypto_withdrawals DROP FOREIGN KEY fk_cryptowd_user;
ALTER TABLE vinplay.crypto_withdrawals ADD CONSTRAINT fk_cryptowd_user
  FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- ─── Step 3: Add FK CASCADE for all user_id tables (vinplay) ────────────

ALTER TABLE vinplay.agency_code
  ADD CONSTRAINT fk_agcode_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.deposit_promotion_logs
  ADD CONSTRAINT fk_deppromo_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.freeze_money
  ADD CONSTRAINT fk_freeze_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.live_user_game
  ADD CONSTRAINT fk_livegame_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.money_gateway_log
  ADD CONSTRAINT fk_mgl_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.rtp_auto_history
  ADD CONSTRAINT fk_rtphist_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.rtp_config_audit
  ADD CONSTRAINT fk_rtpaudit_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.tbl_cashback_logs
  ADD CONSTRAINT fk_cashback_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.tbl_device_install
  ADD CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.tbl_signing_bonus_log
  ADD CONSTRAINT fk_signlog_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.tbl_signing_bonus_user_config
  ADD CONSTRAINT fk_signconf_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.total_actual_volume
  ADD CONSTRAINT fk_volume_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.user_fee
  ADD CONSTRAINT fk_userfee_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.user_mission
  ADD CONSTRAINT fk_mission_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.user_mission_vin
  ADD CONSTRAINT fk_missionvin_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay.user_mission_xu
  ADD CONSTRAINT fk_missionxu_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- users_in_game archived in phase0

ALTER TABLE vinplay.users_vp_event
  ADD CONSTRAINT fk_vpevent_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- ─── Step 3b: Add FK CASCADE for user_id tables (vinplay_gamebai) ───────

ALTER TABLE vinplay_gamebai.xoc_dia_history_award
  ADD CONSTRAINT fk_xdaward_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_gamebai.xoc_dia_jackpot_history_award
  ADD CONSTRAINT fk_xdjpaward_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- ─── Step 3c: Add FK CASCADE for user_id tables (vinplay_minigame) ──────

ALTER TABLE vinplay_minigame.awc_transactions
  ADD CONSTRAINT fk_awctx_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.lode
  ADD CONSTRAINT fk_lode_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.lucky_rotation
  ADD CONSTRAINT fk_lucky_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.rotate_slot_free
  ADD CONSTRAINT fk_rotslot_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.taixiu_record
  ADD CONSTRAINT fk_txrec_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.transaction_detail_tai_xiu
  ADD CONSTRAINT fk_tdtx_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.transaction_detail_tai_xiu_md5
  ADD CONSTRAINT fk_tdtxmd5_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.transaction_detail_tai_xiu_sicbo
  ADD CONSTRAINT fk_tdtxsicbo_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.transaction_tai_xiu
  ADD CONSTRAINT fk_ttx_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.transaction_tai_xiu_md5
  ADD CONSTRAINT fk_ttxmd5_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

ALTER TABLE vinplay_minigame.transaction_tai_xiu_sicbo
  ADD CONSTRAINT fk_ttxsicbo_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- ─── Step 4: Add FK CASCADE for agent tables → useragent.id ─────────────

ALTER TABLE vinplay.agency_wallet
  ADD CONSTRAINT fk_aw_agent FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay.agency_wallet_transactions
  ADD CONSTRAINT fk_awt_agent FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay.credit_wallet
  ADD CONSTRAINT fk_cw_agent FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay.credit_wallet_transactions
  ADD CONSTRAINT fk_cwt_agent FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay.rebate_config
  ADD CONSTRAINT fk_rc_agent FOREIGN KEY (agent_user_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay.rebate_logs
  ADD CONSTRAINT fk_rl_agent FOREIGN KEY (agent_user_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay.rebate_payout
  ADD CONSTRAINT fk_rp_agent FOREIGN KEY (agent_user_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay_admin.agent_code_request
  ADD CONSTRAINT fk_acr_agent FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay_admin.agent_code_history
  ADD CONSTRAINT fk_ach_agent FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

ALTER TABLE vinplay_admin.deposit_commission_logs
  ADD CONSTRAINT fk_dcl_agent FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id) ON DELETE CASCADE;

-- ─── Step 5: Re-enable FK checks ────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 1;

-- ─── Verification ───────────────────────────────────────────────────────
SELECT 'FK on users.id' AS category, COUNT(*) AS count
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA = 'vinplay' AND REFERENCED_TABLE_NAME = 'users'
UNION ALL
SELECT 'FK on useragent.id', COUNT(*)
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA = 'vinplay_admin' AND REFERENCED_TABLE_NAME = 'useragent';
