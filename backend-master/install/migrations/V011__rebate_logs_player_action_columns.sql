-- SUN-1201: add `player_nickname` + `game_action` columns to rebate_logs.
--
-- Why: the agency LSR view (c=9541) renders one row per click event because
-- the underlying RealTimeCommission writes a separate INSERT per bet. To
-- collapse multi-click rounds into one display row, we need to GROUP BY the
-- player+game tuple — but those values were only available as parsed
-- substrings of the free-text `note` column. Schema-first fix: promote them
-- to real columns so the GROUP BY is index-supported and correct across all
-- note formats (AUTO_COMMISSION, Realtime, Cashback, Auto-cron).
--
-- Reversible: dropping the index + columns leaves rebate_logs identical to
-- pre-V011.

ALTER TABLE rebate_logs
    ADD COLUMN player_nickname VARCHAR(64) NULL,
    ADD COLUMN game_action     VARCHAR(64) NULL;

-- Composite index supports the GROUP BY used by RebateService.queryLogsAggregated.
-- agent_user_id + period_start filter the working set; player_nickname +
-- game_action are the grouping keys.
ALTER TABLE rebate_logs
    ADD INDEX idx_rebate_logs_agent_period_player_action
        (agent_user_id, period_start, player_nickname, game_action);

-- ── Backfill existing rows from `note` ───────────────────────────────────
-- The four note formats are exactly the ones RebateService.mapLog parses.
-- One UPDATE per format keeps the matcher tight (no false-positive grouping).

-- Format 1 — "AUTO_COMMISSION source=... type=... user=<nick> action=<game> service=..."
-- (the high-volume path from LogMoneyUserExtraProcessor)
UPDATE rebate_logs
   SET player_nickname = NULLIF(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(note, ' user=', -1), ' ', 1)), ''),
       game_action     = NULLIF(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(note, ' action=', -1), ' ', 1)), '')
 WHERE note LIKE 'AUTO_COMMISSION %' AND player_nickname IS NULL;

-- Format 2 — "Realtime from <nick> game=<action>"
-- (RealTimeCommission DOWNLINE inserts)
UPDATE rebate_logs
   SET player_nickname = NULLIF(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(note, 'Realtime from ', -1), ' game=', 1)), ''),
       game_action     = NULLIF(TRIM(SUBSTRING_INDEX(note, ' game=', -1)), '')
 WHERE note LIKE 'Realtime from %' AND player_nickname IS NULL;

-- Format 3 — "Cashback <action> bet=<amount>"
-- For SELF rows the player IS the agent (the agent_nickname column).
UPDATE rebate_logs
   SET player_nickname = agent_nickname,
       game_action     = NULLIF(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(note, 'Cashback ', -1), ' bet=', 1)), '')
 WHERE note LIKE 'Cashback %' AND player_nickname IS NULL;

-- Format 4 — "Auto-cron from user=<nick>" (CommissionCronJob)
-- No game_action carried in the note format — left NULL. The agency view
-- shows "Tổng hợp" via display-time fallback.
UPDATE rebate_logs
   SET player_nickname = NULLIF(TRIM(SUBSTRING_INDEX(note, 'Auto-cron from user=', -1)), '')
 WHERE note LIKE 'Auto-cron from user=%' AND player_nickname IS NULL;
