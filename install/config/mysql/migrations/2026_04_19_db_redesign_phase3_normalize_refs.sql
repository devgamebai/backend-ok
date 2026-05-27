-- =============================================================================
-- SUN-973 [DB-5]: Add user_id column + FK where only nick_name exists
-- + Index all unindexed nick_name/user_id columns for query performance
-- =============================================================================
-- 27 tables have nick_name but no user_id → can't FK to users.
-- Strategy: add user_id BIGINT, backfill from JOIN users, add FK CASCADE.
-- Also: add missing indexes on 34 nick_name columns for query perf.
--
-- NOTE: v_log_user_play and users_bot are VIEWS — skip.
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET sql_mode = '';

-- ─── Part A: Add user_id + FK to nick_name-only tables (vinplay) ────────

-- bank_sms
ALTER TABLE vinplay.bank_sms ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nickname;
UPDATE vinplay.bank_sms b JOIN vinplay.users u ON u.nick_name = b.nickname SET b.user_id = u.id WHERE b.user_id IS NULL;
ALTER TABLE vinplay.bank_sms ADD CONSTRAINT fk_banksms_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- cmduser
ALTER TABLE vinplay.cmduser ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.cmduser c JOIN vinplay.users u ON u.nick_name = c.nick_name SET c.user_id = u.id WHERE c.user_id IS NULL;
ALTER TABLE vinplay.cmduser ADD CONSTRAINT fk_cmduser_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- ebetuser
ALTER TABLE vinplay.ebetuser ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.ebetuser e JOIN vinplay.users u ON u.nick_name = e.nick_name SET e.user_id = u.id WHERE e.user_id IS NULL;
ALTER TABLE vinplay.ebetuser ADD CONSTRAINT fk_ebetuser_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- history_applyfor
ALTER TABLE vinplay.history_applyfor ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.history_applyfor h JOIN vinplay.users u ON u.nick_name = h.nick_name SET h.user_id = u.id WHERE h.user_id IS NULL;
ALTER TABLE vinplay.history_applyfor ADD CONSTRAINT fk_histapply_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- history_bank
ALTER TABLE vinplay.history_bank ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.history_bank h JOIN vinplay.users u ON u.nick_name = h.nick_name SET h.user_id = u.id WHERE h.user_id IS NULL;
ALTER TABLE vinplay.history_bank ADD CONSTRAINT fk_histbank_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- history_bank_live
ALTER TABLE vinplay.history_bank_live ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.history_bank_live h JOIN vinplay.users u ON u.nick_name = h.nick_name SET h.user_id = u.id WHERE h.user_id IS NULL;
ALTER TABLE vinplay.history_bank_live ADD CONSTRAINT fk_histbanklive_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- log_count_user_play
ALTER TABLE vinplay.log_count_user_play ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.log_count_user_play l JOIN vinplay.users u ON u.nick_name = l.nick_name SET l.user_id = u.id WHERE l.user_id IS NULL;
ALTER TABLE vinplay.log_count_user_play ADD CONSTRAINT fk_logcount_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- log_hoan_tra
ALTER TABLE vinplay.log_hoan_tra ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.log_hoan_tra l JOIN vinplay.users u ON u.nick_name = l.nick_name SET l.user_id = u.id WHERE l.user_id IS NULL;
ALTER TABLE vinplay.log_hoan_tra ADD CONSTRAINT fk_loghoant_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- log_hoan_tra_histories
ALTER TABLE vinplay.log_hoan_tra_histories ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.log_hoan_tra_histories l JOIN vinplay.users u ON u.nick_name = l.nick_name SET l.user_id = u.id WHERE l.user_id IS NULL;
ALTER TABLE vinplay.log_hoan_tra_histories ADD CONSTRAINT fk_loghoanthist_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- log_report_user
ALTER TABLE vinplay.log_report_user ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.log_report_user l JOIN vinplay.users u ON u.nick_name = l.nick_name SET l.user_id = u.id WHERE l.user_id IS NULL;
ALTER TABLE vinplay.log_report_user ADD CONSTRAINT fk_logreport_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- sbouser
ALTER TABLE vinplay.sbouser ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.sbouser s JOIN vinplay.users u ON u.nick_name = s.nick_name SET s.user_id = u.id WHERE s.user_id IS NULL;
ALTER TABLE vinplay.sbouser ADD CONSTRAINT fk_sbouser_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- topup
ALTER TABLE vinplay.topup ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.topup t JOIN vinplay.users u ON u.nick_name = t.nick_name SET t.user_id = u.id WHERE t.user_id IS NULL;
ALTER TABLE vinplay.topup ADD CONSTRAINT fk_topup_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- topup_live
ALTER TABLE vinplay.topup_live ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.topup_live t JOIN vinplay.users u ON u.nick_name = t.nick_name SET t.user_id = u.id WHERE t.user_id IS NULL;
ALTER TABLE vinplay.topup_live ADD CONSTRAINT fk_topuplive_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- user_appotp
ALTER TABLE vinplay.user_appotp ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.user_appotp a JOIN vinplay.users u ON u.nick_name = a.nick_name SET a.user_id = u.id WHERE a.user_id IS NULL;
ALTER TABLE vinplay.user_appotp ADD CONSTRAINT fk_appotp_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- user_attendance
ALTER TABLE vinplay.user_attendance ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.user_attendance a JOIN vinplay.users u ON u.nick_name = a.nick_name SET a.user_id = u.id WHERE a.user_id IS NULL;
ALTER TABLE vinplay.user_attendance ADD CONSTRAINT fk_attend_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- user_bonus
ALTER TABLE vinplay.user_bonus ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.user_bonus b JOIN vinplay.users u ON u.nick_name = b.nick_name SET b.user_id = u.id WHERE b.user_id IS NULL;
ALTER TABLE vinplay.user_bonus ADD CONSTRAINT fk_bonus_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- user_level
ALTER TABLE vinplay.user_level ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.user_level l JOIN vinplay.users u ON u.nick_name = l.nick_name SET l.user_id = u.id WHERE l.user_id IS NULL;
ALTER TABLE vinplay.user_level ADD CONSTRAINT fk_level_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- user_wages
ALTER TABLE vinplay.user_wages ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.user_wages w JOIN vinplay.users u ON u.nick_name = w.nick_name SET w.user_id = u.id WHERE w.user_id IS NULL;
ALTER TABLE vinplay.user_wages ADD CONSTRAINT fk_wages_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- wmuser
ALTER TABLE vinplay.wmuser ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay.wmuser w JOIN vinplay.users u ON u.nick_name = w.nick_name SET w.user_id = u.id WHERE w.user_id IS NULL;
ALTER TABLE vinplay.wmuser ADD CONSTRAINT fk_wmuser_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- ─── Part A2: vinplay_gamebai nick_name-only tables ─────────────────────

-- game_tour_mark
ALTER TABLE vinplay_gamebai.game_tour_mark ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay_gamebai.game_tour_mark g JOIN vinplay.users u ON u.nick_name = g.nick_name SET g.user_id = u.id WHERE g.user_id IS NULL;
ALTER TABLE vinplay_gamebai.game_tour_mark ADD CONSTRAINT fk_gtm_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- game_tour_vip
ALTER TABLE vinplay_gamebai.game_tour_vip ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay_gamebai.game_tour_vip g JOIN vinplay.users u ON u.nick_name = g.nick_name SET g.user_id = u.id WHERE g.user_id IS NULL;
ALTER TABLE vinplay_gamebai.game_tour_vip ADD CONSTRAINT fk_gtv_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- poker_free_ticket
ALTER TABLE vinplay_gamebai.poker_free_ticket ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay_gamebai.poker_free_ticket p JOIN vinplay.users u ON u.nick_name = p.nick_name SET p.user_id = u.id WHERE p.user_id IS NULL;
ALTER TABLE vinplay_gamebai.poker_free_ticket ADD CONSTRAINT fk_pft_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- poker_tour_user
ALTER TABLE vinplay_gamebai.poker_tour_user ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay_gamebai.poker_tour_user p JOIN vinplay.users u ON u.nick_name = p.nick_name SET p.user_id = u.id WHERE p.user_id IS NULL;
ALTER TABLE vinplay_gamebai.poker_tour_user ADD CONSTRAINT fk_ptu_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- xoc_dia_boss
ALTER TABLE vinplay_gamebai.xoc_dia_boss ADD COLUMN user_id BIGINT DEFAULT NULL AFTER nick_name;
UPDATE vinplay_gamebai.xoc_dia_boss x JOIN vinplay.users u ON u.nick_name = x.nick_name SET x.user_id = u.id WHERE x.user_id IS NULL;
ALTER TABLE vinplay_gamebai.xoc_dia_boss ADD CONSTRAINT fk_xdb_user FOREIGN KEY (user_id) REFERENCES vinplay.users(id) ON DELETE CASCADE;

-- ─── Part B: Add missing indexes on nick_name columns ───────────────────
-- These columns are used in WHERE/JOIN but have no index → full table scan.

CREATE INDEX idx_banksms_nick ON vinplay.bank_sms(nickname);
CREATE INDEX idx_bankwd_nick ON vinplay.bank_withdrawals(nick_name);
CREATE INDEX idx_cryptodep_nick ON vinplay.crypto_deposits(nick_name);
CREATE INDEX idx_cryptowd_nick ON vinplay.crypto_withdrawals(nick_name);
CREATE INDEX idx_deppromo_nick ON vinplay.deposit_promotion_logs(nick_name);
CREATE INDEX idx_deptx_nick ON vinplay.deposit_transactions(nick_name);
CREATE INDEX idx_ebet_nick ON vinplay.ebetuser(nick_name);
CREATE INDEX idx_histapply_nick ON vinplay.history_applyfor(nick_name);
CREATE INDEX idx_histbank_nick ON vinplay.history_bank(nick_name);
CREATE INDEX idx_histbanklive_nick ON vinplay.history_bank_live(nick_name);
CREATE INDEX idx_loghoant_nick ON vinplay.log_hoan_tra(nick_name);
CREATE INDEX idx_mgl_nick ON vinplay.money_gateway_log(nick_name);
CREATE INDEX idx_rtphist_nick ON vinplay.rtp_auto_history(nick_name);
CREATE INDEX idx_sbo_nick ON vinplay.sbouser(nick_name);
CREATE INDEX idx_cashback_nick ON vinplay.tbl_cashback_logs(nick_name);
CREATE INDEX idx_device_nick ON vinplay.tbl_device_install(nick_name);
CREATE INDEX idx_signlog_nick ON vinplay.tbl_signing_bonus_log(nick_name);
CREATE INDEX idx_signconf_nick ON vinplay.tbl_signing_bonus_user_config(nick_name);
CREATE INDEX idx_topup_nick ON vinplay.topup(nick_name);
CREATE INDEX idx_topuplive_nick ON vinplay.topup_live(nick_name);
CREATE INDEX idx_volume_nick ON vinplay.total_actual_volume(nick_name);
CREATE INDEX idx_attend_nick ON vinplay.user_attendance(nick_name);
CREATE INDEX idx_bonus_nick ON vinplay.user_bonus(nick_name);
CREATE INDEX idx_level_nick ON vinplay.user_level(nick_name);
CREATE INDEX idx_mission_nick ON vinplay.user_mission(nickname);
CREATE INDEX idx_threat_nick ON vinplay.user_threat_score(nick_name);
CREATE INDEX idx_voltrack_nick ON vinplay.user_volume_tracking(nick_name);
CREATE INDEX idx_wages_nick ON vinplay.user_wages(nick_name);
CREATE INDEX idx_usersbank_nick ON vinplay.users_bank(nick_name);
CREATE INDEX idx_pft_nick ON vinplay_gamebai.poker_free_ticket(nick_name);
CREATE INDEX idx_xdb_nick ON vinplay_gamebai.xoc_dia_boss(nick_name);

SET FOREIGN_KEY_CHECKS = 1;

-- ─── Verification ───────────────────────────────────────────────────────
SELECT 'Total FK on users' c, COUNT(*) n FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME='users' AND REFERENCED_TABLE_SCHEMA='vinplay'
UNION ALL
SELECT 'Total FK on useragent', COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME='useragent' AND REFERENCED_TABLE_SCHEMA='vinplay_admin'
UNION ALL
SELECT 'Tables with nick_name but no user_id', COUNT(DISTINCT CONCAT(c1.TABLE_SCHEMA,'.',c1.TABLE_NAME))
FROM information_schema.COLUMNS c1
WHERE c1.COLUMN_NAME IN ('nick_name','nickname')
  AND c1.TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai')
  AND c1.TABLE_NAME NOT LIKE '_archive%'
  AND c1.TABLE_NAME NOT IN ('users','useragent','v_log_user_play','users_bot')
  AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS c2 WHERE c2.TABLE_SCHEMA=c1.TABLE_SCHEMA AND c2.TABLE_NAME=c1.TABLE_NAME AND c2.COLUMN_NAME='user_id');
