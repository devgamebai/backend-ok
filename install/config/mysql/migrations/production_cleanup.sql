-- =============================================================================
-- PRODUCTION CLEANUP: Delete all data, keep only SpecialAccount + CompanyAgent
-- =============================================================================
-- DANGER: This script DELETES ALL user and agent data.
-- Run on production ONLY after backup.
-- Order matters: child tables first (RESTRICT FK), then parents.
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ─── Phase 0b: Auth/session tables ───────────────────────────────────────
TRUNCATE TABLE vinplay.tx_user_authority;
TRUNCATE TABLE vinplay.tx_user;

-- ─── Phase 1: Financial transactions (FK RESTRICT → users) ───────────────
TRUNCATE TABLE vinplay.deposit_transactions;
TRUNCATE TABLE vinplay.bank_withdrawals;
TRUNCATE TABLE vinplay.crypto_deposits;
TRUNCATE TABLE vinplay.crypto_withdrawals;

-- ─── Phase 2: Agent commission + wallet (NO FK → orphan risk) ────────────
TRUNCATE TABLE vinplay_admin.agent_code_history;
TRUNCATE TABLE vinplay_admin.agent_code_request;
TRUNCATE TABLE vinplay_admin.deposit_commission_logs;
TRUNCATE TABLE vinplay.agency_wallet_transactions;
TRUNCATE TABLE vinplay.agency_wallet;
TRUNCATE TABLE vinplay.credit_wallet_transactions;
TRUNCATE TABLE vinplay.credit_wallet;
TRUNCATE TABLE vinplay.rebate_logs;
TRUNCATE TABLE vinplay.rebate_payout;
TRUNCATE TABLE vinplay.rebate_config;
TRUNCATE TABLE vinplay.game_commission_rate;
TRUNCATE TABLE vinplay.agency_code;

-- ─── Phase 3: User activity (NO FK → must delete manually) ──────────────

-- vinplay schema
TRUNCATE TABLE vinplay.bank_sms;
TRUNCATE TABLE vinplay.cmduser;
TRUNCATE TABLE vinplay.deposit_promotion_logs;
TRUNCATE TABLE vinplay.ebetuser;
TRUNCATE TABLE vinplay.freeze_money;
TRUNCATE TABLE vinplay.gift_codes;
TRUNCATE TABLE vinplay.history_applyfor;
TRUNCATE TABLE vinplay.history_bank;
TRUNCATE TABLE vinplay.history_bank_live;
TRUNCATE TABLE vinplay.live_user_game;
TRUNCATE TABLE vinplay.log_count_user_play;
TRUNCATE TABLE vinplay.log_hoan_tra;
TRUNCATE TABLE vinplay.log_hoan_tra_histories;
TRUNCATE TABLE vinplay.log_report_user;
TRUNCATE TABLE vinplay.log_tranfer_agent;
TRUNCATE TABLE vinplay.money_gateway_log;
TRUNCATE TABLE vinplay.rtp_auto_history;
TRUNCATE TABLE vinplay.rtp_config_audit;
TRUNCATE TABLE vinplay.sbouser;
TRUNCATE TABLE vinplay.tbl_cashback_logs;
TRUNCATE TABLE vinplay.tbl_device_install;
TRUNCATE TABLE vinplay.tbl_signing_bonus_log;
TRUNCATE TABLE vinplay.tbl_signing_bonus_user_config;
TRUNCATE TABLE vinplay.tbl_slot_win_rate;
TRUNCATE TABLE vinplay.topup;
TRUNCATE TABLE vinplay.topup_live;
TRUNCATE TABLE vinplay.total_actual_volume;
TRUNCATE TABLE vinplay.user_appotp;
TRUNCATE TABLE vinplay.user_attendance;
TRUNCATE TABLE vinplay.user_bonus;
TRUNCATE TABLE vinplay.user_fee;
TRUNCATE TABLE vinplay.user_level;
TRUNCATE TABLE vinplay.user_mission;
TRUNCATE TABLE vinplay.user_mission_vin;
TRUNCATE TABLE vinplay.user_mission_xu;
TRUNCATE TABLE vinplay.user_value;
TRUNCATE TABLE vinplay.user_wages;
TRUNCATE TABLE vinplay.users_in_game;
TRUNCATE TABLE vinplay.users_vp_event;
-- v_log_user_play is a VIEW, not a table — skip (data comes from underlying tables)
TRUNCATE TABLE vinplay.wmuser;

-- vinplay_gamebai schema
TRUNCATE TABLE vinplay_gamebai.game_tour_mark;
TRUNCATE TABLE vinplay_gamebai.game_tour_vip;
TRUNCATE TABLE vinplay_gamebai.poker_free_ticket;
TRUNCATE TABLE vinplay_gamebai.poker_tour_user;
TRUNCATE TABLE vinplay_gamebai.xoc_dia_boss;
TRUNCATE TABLE vinplay_gamebai.xoc_dia_history_award;
TRUNCATE TABLE vinplay_gamebai.xoc_dia_jackpot_history_award;

-- vinplay_minigame schema
TRUNCATE TABLE vinplay_minigame.awc_transactions;
TRUNCATE TABLE vinplay_minigame.chatbox;
TRUNCATE TABLE vinplay_minigame.lode;
TRUNCATE TABLE vinplay_minigame.lucky_rotation;
TRUNCATE TABLE vinplay_minigame.rotate_slot_free;
TRUNCATE TABLE vinplay_minigame.taixiu_record;
TRUNCATE TABLE vinplay_minigame.thanh_du;
TRUNCATE TABLE vinplay_minigame.transaction_detail_tai_xiu;
TRUNCATE TABLE vinplay_minigame.transaction_detail_tai_xiu_md5;
TRUNCATE TABLE vinplay_minigame.transaction_detail_tai_xiu_sicbo;
TRUNCATE TABLE vinplay_minigame.transaction_tai_xiu;
TRUNCATE TABLE vinplay_minigame.transaction_tai_xiu_md5;
TRUNCATE TABLE vinplay_minigame.transaction_tai_xiu_sicbo;
TRUNCATE TABLE vinplay_minigame.tx_rank;
TRUNCATE TABLE vinplay_minigame.user_rut_loc;
-- Game round results (shared, not per-user — but clear for fresh start)
TRUNCATE TABLE vinplay_minigame.result_tai_xiu_md5;
TRUNCATE TABLE vinplay_minigame.result_tai_xiu_sicbo;
-- users_bot is a VIEW, not a table — skip

-- ─── Phase 4: Users (CASCADE auto-deletes users_bank, rtp, threat, volume) ──
TRUNCATE TABLE vinplay.users_bank;
TRUNCATE TABLE vinplay.user_rtp_override;
TRUNCATE TABLE vinplay.user_threat_score;
TRUNCATE TABLE vinplay.user_volume_tracking;
TRUNCATE TABLE vinplay.users;

-- ─── Phase 5: Useragent — keep SA (151) + CompanyAgent (152) ─────────────
DELETE FROM vinplay_admin.useragent WHERE id NOT IN (151, 152);

-- ─── Phase 6: Admin logs (optional — keep for audit trail or truncate) ───
-- TRUNCATE TABLE vinplay_admin.log_loginadmin;  -- uncomment if desired

SET FOREIGN_KEY_CHECKS = 1;

-- ─── Verification ────────────────────────────────────────────────────────
SELECT 'useragent' t, COUNT(*) c FROM vinplay_admin.useragent
UNION ALL SELECT 'users', COUNT(*) FROM vinplay.users
UNION ALL SELECT 'agency_wallet', COUNT(*) FROM vinplay.agency_wallet
UNION ALL SELECT 'rebate_logs', COUNT(*) FROM vinplay.rebate_logs
UNION ALL SELECT 'deposit_transactions', COUNT(*) FROM vinplay.deposit_transactions;

-- Expected: useragent=2, users=0, agency_wallet=0, rebate_logs=0, deposit_transactions=0

-- =============================================================================
-- Phase 7: MongoDB cleanup (run separately via mongosh)
-- =============================================================================
-- Game logs are in database `win123club` (NOT vinplay):
--
-- mongosh "mongodb://$MONGO_USER:$MONGO_PW@localhost:27017/?authSource=admin" --eval '
--   ["vinplay","win123club"].forEach(function(dbname) {
--     var mydb = db.getSiblingDB(dbname);
--     mydb.getCollectionNames().forEach(function(c) {
--       if (mydb.getCollection(c).estimatedDocumentCount() > 0) {
--         mydb.getCollection(c).deleteMany({});
--         print("Cleared " + dbname + "." + c);
--       }
--     });
--   });
--   print("MongoDB cleanup done");
-- '
-- =============================================================================
