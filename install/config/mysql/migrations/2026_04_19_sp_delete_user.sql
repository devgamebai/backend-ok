-- =============================================================================
-- delete_user: Safe single-user deletion with full cascade cleanup
-- =============================================================================
-- Handles 112+ tables with NO FK constraint + 4 RESTRICT FK tables.
-- All nick_name comparisons use COLLATE utf8mb3_unicode_ci to handle
-- mixed collation across schemas (general_ci vs unicode_ci).
--
-- Usage:
--   SET @TARGET_USER_ID = 12345;
--   SOURCE install/config/mysql/migrations/2026_04_19_sp_delete_user.sql;
-- =============================================================================

-- ██ SET TARGET USER ID BEFORE RUNNING ██
-- SET @TARGET_USER_ID = 12345;

-- Resolve user info
SET @nick = (SELECT nick_name FROM vinplay.users WHERE id = @TARGET_USER_ID);
SET @uname = (SELECT user_name FROM vinplay.users WHERE id = @TARGET_USER_ID);
SET @agid = IFNULL((SELECT id FROM vinplay_admin.useragent WHERE nickname = @nick COLLATE utf8mb3_unicode_ci LIMIT 1), 0);

SELECT @TARGET_USER_ID AS user_id, @nick AS nickname, @uname AS username, @agid AS agent_id;

-- Phase 1: RESTRICT FK tables (must delete before users)
DELETE FROM vinplay.deposit_transactions WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.bank_withdrawals WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.crypto_deposits WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.crypto_withdrawals WHERE user_id = @TARGET_USER_ID;

-- Phase 2: Agent data (skip if not agent)
DELETE FROM vinplay.agency_wallet_transactions WHERE agent_id = @agid AND @agid > 0;
DELETE FROM vinplay.agency_wallet WHERE agent_id = @agid AND @agid > 0;
DELETE FROM vinplay.credit_wallet_transactions WHERE agent_id = @agid AND @agid > 0;
DELETE FROM vinplay.credit_wallet WHERE agent_id = @agid AND @agid > 0;
DELETE FROM vinplay.rebate_logs WHERE agent_user_id = @agid AND @agid > 0;
DELETE FROM vinplay.rebate_payout WHERE agent_user_id = @agid AND @agid > 0;
DELETE FROM vinplay.rebate_config WHERE agent_user_id = @agid AND @agid > 0;
DELETE FROM vinplay.game_commission_rate WHERE agent_nickname = @nick COLLATE utf8mb3_unicode_ci AND @agid > 0;
DELETE FROM vinplay_admin.agent_code_request WHERE agent_id = @agid AND @agid > 0;
DELETE FROM vinplay_admin.agent_code_history WHERE agent_id = @agid AND @agid > 0;
DELETE FROM vinplay_admin.deposit_commission_logs WHERE agent_id = @agid AND @agid > 0;
DELETE FROM vinplay_admin.useragent WHERE id = @agid AND @agid > 0;

-- Phase 3: User activity (vinplay — no FK, COLLATE on all nick_name comparisons)
DELETE FROM vinplay.agency_code WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.bank_sms WHERE nickname = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.cmduser WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.deposit_promotion_logs WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.ebetuser WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.freeze_money WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.gift_codes WHERE user_name = @uname COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.history_applyfor WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.history_bank WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.history_bank_live WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.live_user_game WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.log_count_user_play WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.log_hoan_tra WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.log_hoan_tra_histories WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.log_report_user WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.log_tranfer_agent WHERE nick_name_send = @nick COLLATE utf8mb3_unicode_ci OR nick_name_receive = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.money_gateway_log WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.rtp_auto_history WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.rtp_config_audit WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.sbouser WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.tbl_cashback_logs WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.tbl_device_install WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.tbl_signing_bonus_log WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.tbl_signing_bonus_user_config WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.tbl_slot_win_rate WHERE nickname = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.topup WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.topup_live WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.total_actual_volume WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.user_appotp WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.user_attendance WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.user_bonus WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.user_fee WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.user_level WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.user_mission WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.user_mission_vin WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.user_mission_xu WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.user_value WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.user_wages WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay.users_in_game WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.users_vp_event WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay.wmuser WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;

-- Phase 4: Game data
DELETE FROM vinplay_gamebai.game_tour_mark WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay_gamebai.game_tour_vip WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay_gamebai.poker_free_ticket WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay_gamebai.poker_tour_user WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay_gamebai.xoc_dia_boss WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay_gamebai.xoc_dia_history_award WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_gamebai.xoc_dia_jackpot_history_award WHERE user_id = @TARGET_USER_ID;

DELETE FROM vinplay_minigame.awc_transactions WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.chatbox WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay_minigame.lode WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.lucky_rotation WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.rotate_slot_free WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.taixiu_record WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.thanh_du WHERE user_name = @uname COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay_minigame.transaction_detail_tai_xiu WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.transaction_detail_tai_xiu_md5 WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.transaction_detail_tai_xiu_sicbo WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.transaction_tai_xiu WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.transaction_tai_xiu_md5 WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.transaction_tai_xiu_sicbo WHERE user_id = @TARGET_USER_ID;
DELETE FROM vinplay_minigame.tx_rank WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
DELETE FROM vinplay_minigame.user_rut_loc WHERE user_name = @uname COLLATE utf8mb3_unicode_ci;

-- Phase 5: Delete user (CASCADE auto-deletes users_bank, rtp_override, threat_score, volume_tracking)
DELETE FROM vinplay.users WHERE id = @TARGET_USER_ID;

-- Verification
SELECT CONCAT('Deleted user ', @nick, ' (id=', @TARGET_USER_ID, ') agent_id=', @agid) AS result;
SELECT 'users' t, COUNT(*) c FROM vinplay.users WHERE id = @TARGET_USER_ID
UNION ALL SELECT 'users_bank', COUNT(*) FROM vinplay.users_bank WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci
UNION ALL SELECT 'deposit_tx', COUNT(*) FROM vinplay.deposit_transactions WHERE user_id = @TARGET_USER_ID
UNION ALL SELECT 'log_report', COUNT(*) FROM vinplay.log_report_user WHERE nick_name = @nick COLLATE utf8mb3_unicode_ci;
