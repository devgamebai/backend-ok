-- =============================================================================
-- SUN-969 [DB-1]: Drop legacy/unused tables
-- =============================================================================
-- These tables have ZERO references in any Java source code.
-- Verified by searching all .java, .xml files in backend-master.
-- =============================================================================

-- Rename to _archive first (safer than DROP — can restore if needed)
RENAME TABLE vinplay.user_value TO vinplay._archive_user_value;
RENAME TABLE vinplay.users_in_game TO vinplay._archive_users_in_game;
RENAME TABLE vinplay.tbl_slot_win_rate TO vinplay._archive_tbl_slot_win_rate;
RENAME TABLE vinplay_admin.access_link TO vinplay_admin._archive_access_link;
RENAME TABLE vinplay_minigame.chatbox TO vinplay_minigame._archive_chatbox;
RENAME TABLE vinplay_minigame.config_history TO vinplay_minigame._archive_config_history;
RENAME TABLE vinplay_minigame.thanh_du TO vinplay_minigame._archive_thanh_du;
RENAME TABLE vinplay_minigame.tx_rank TO vinplay_minigame._archive_tx_rank;

-- After 30 days with no issues, run:
-- DROP TABLE vinplay._archive_user_value;
-- DROP TABLE vinplay._archive_users_in_game;
-- DROP TABLE vinplay._archive_tbl_slot_win_rate;
-- DROP TABLE vinplay_admin._archive_access_link;
-- DROP TABLE vinplay_minigame._archive_chatbox;
-- DROP TABLE vinplay_minigame._archive_config_history;
-- DROP TABLE vinplay_minigame._archive_thanh_du;
-- DROP TABLE vinplay_minigame._archive_tx_rank;
