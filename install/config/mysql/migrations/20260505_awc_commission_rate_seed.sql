-- SUN-AWC-COMM: seed game_commission_rate with awc_* category keys.
-- Date: 2026-05-05
--
-- AwcCallbackProcessor.triggerCommission emits LogMoneyUserMessage with
-- game_key = "awc_live" / "awc_slot" / "awc_fish" / "awc_sport" /
-- "awc_egame" / "awc_other" (bucketed by gameType — see
-- AwcCallbackProcessor.awcCommissionKey). Without rows in
-- game_commission_rate matching those keys per agent, the vbee
-- LogMoneyUserExtraProcessor commission lookup returns 0%, and AWC bets
-- never produce rebate_logs entries — invisible in agency LS Rolling.
--
-- This migration creates one row per (agent, awc_category) with rate=0.00.
-- Operators set per-agent rates via the admin CMS rate editor afterwards.
-- INSERT IGNORE so re-running the migration is a no-op.
--
-- Rollback:
--   DELETE FROM game_commission_rate WHERE game_key LIKE 'awc\_%';

INSERT IGNORE INTO game_commission_rate (agent_nickname, agent_user_id, game_key, rate)
SELECT
    ua.nickname AS agent_nickname,
    ua.id       AS agent_user_id,
    k.game_key,
    0.00        AS rate
FROM vinplay_admin.useragent ua
CROSS JOIN (
    SELECT 'awc_live'  AS game_key UNION ALL
    SELECT 'awc_slot'           UNION ALL
    SELECT 'awc_fish'           UNION ALL
    SELECT 'awc_sport'          UNION ALL
    SELECT 'awc_egame'          UNION ALL
    SELECT 'awc_other'
) k;
