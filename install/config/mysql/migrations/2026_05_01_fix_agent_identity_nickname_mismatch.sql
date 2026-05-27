-- SUN-1192: Backfill agent identity rows whose useragent.nickname drifted
-- from the canonical game user nickname.
--
-- Canonical invariant:
--   vinplay_admin.useragent.username = vinplay.users.user_name
--   vinplay_admin.useragent.nickname = vinplay.users.nick_name
--
-- Why:
-- DetailMemberOfAgency receives nn from the agency list/detail UI. For rows
-- where nn is users.nick_name but useragent.nickname differs, the old detail
-- endpoint missed the agent row and rendered level=4 (User) + can_promote=true.
--
-- Safety:
--   1. Archive every candidate before changing it.
--   2. Do not update rows whose target nickname is already used by another
--      useragent row.
--   3. Sync active config tables that key by agent_nickname when safe.
--   4. Do not rewrite historical ledgers/logs; those preserve event-time text.

CREATE TABLE IF NOT EXISTS vinplay_admin.useragent_nickname_mismatch_20260501 (
  archived_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  agent_id             INT NOT NULL,
  username             VARCHAR(128) NOT NULL,
  old_useragent_nick   VARCHAR(128) DEFAULT NULL,
  new_users_nick       VARCHAR(128) DEFAULT NULL,
  user_id              BIGINT DEFAULT NULL,
  level                TINYINT DEFAULT NULL,
  parentid             INT DEFAULT NULL,
  code                 VARCHAR(128) DEFAULT NULL,
  action               VARCHAR(32) NOT NULL DEFAULT 'ARCHIVED',
  PRIMARY KEY (agent_id),
  KEY idx_old_nick (old_useragent_nick),
  KEY idx_new_nick (new_users_nick)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO vinplay_admin.useragent_nickname_mismatch_20260501
  (agent_id, username, old_useragent_nick, new_users_nick, user_id, level, parentid, code, action)
SELECT
  ua.id,
  ua.username,
  ua.nickname,
  u.nick_name,
  u.id,
  ua.level,
  ua.parentid,
  ua.code,
  'ARCHIVED'
FROM vinplay_admin.useragent ua
JOIN vinplay.users u
  ON BINARY ua.username = BINARY u.user_name
LEFT JOIN vinplay_admin.useragent existing
  ON BINARY existing.nickname = BINARY u.nick_name
 AND existing.id <> ua.id
WHERE u.nick_name IS NOT NULL
  AND u.nick_name <> ''
  AND (ua.nickname IS NULL OR BINARY ua.nickname <> BINARY u.nick_name)
  AND existing.id IS NULL
ON DUPLICATE KEY UPDATE
  old_useragent_nick = VALUES(old_useragent_nick),
  new_users_nick = VALUES(new_users_nick),
  user_id = VALUES(user_id),
  level = VALUES(level),
  parentid = VALUES(parentid),
  code = VALUES(code),
  archived_at = CURRENT_TIMESTAMP;

-- Keep current/active configuration tables addressable by the canonical
-- nickname. These tables are current config, not immutable event history.
UPDATE vinplay.game_commission_rate g
JOIN vinplay_admin.useragent_nickname_mismatch_20260501 a
  ON BINARY g.agent_nickname = BINARY a.old_useragent_nick
LEFT JOIN vinplay.game_commission_rate dup
  ON BINARY dup.agent_nickname = BINARY a.new_users_nick
 AND dup.game_key = g.game_key
 AND dup.id <> g.id
SET g.agent_nickname = a.new_users_nick
WHERE a.action = 'ARCHIVED'
  AND a.new_users_nick IS NOT NULL
  AND dup.id IS NULL;

UPDATE vinplay.rebate_config r
JOIN vinplay_admin.useragent_nickname_mismatch_20260501 a
  ON r.agent_user_id = a.agent_id
SET r.agent_nickname = a.new_users_nick
WHERE a.action = 'ARCHIVED'
  AND a.new_users_nick IS NOT NULL;

UPDATE vinplay_admin.agent_code_request acr
JOIN vinplay_admin.useragent_nickname_mismatch_20260501 a
  ON BINARY acr.agent_nickname = BINARY a.old_useragent_nick
SET acr.agent_nickname = a.new_users_nick
WHERE a.action = 'ARCHIVED'
  AND a.new_users_nick IS NOT NULL;

UPDATE vinplay_admin.useragent ua
JOIN vinplay_admin.useragent_nickname_mismatch_20260501 a
  ON ua.id = a.agent_id
SET ua.nickname = a.new_users_nick,
    ua.updatetime = NOW()
WHERE a.action = 'ARCHIVED'
  AND a.new_users_nick IS NOT NULL;

UPDATE vinplay_admin.useragent_nickname_mismatch_20260501
SET action = 'UPDATED'
WHERE action = 'ARCHIVED';

-- Post-check 1: rows that were skipped because the target nickname is already
-- used by another useragent row. This should be zero before considering the
-- data fully clean.
SELECT
  ua.id AS agent_id,
  ua.username,
  ua.nickname AS current_useragent_nick,
  u.nick_name AS canonical_users_nick,
  existing.id AS conflicting_agent_id
FROM vinplay_admin.useragent ua
JOIN vinplay.users u
  ON BINARY ua.username = BINARY u.user_name
JOIN vinplay_admin.useragent existing
  ON BINARY existing.nickname = BINARY u.nick_name
 AND existing.id <> ua.id
WHERE u.nick_name IS NOT NULL
  AND u.nick_name <> ''
  AND (ua.nickname IS NULL OR BINARY ua.nickname <> BINARY u.nick_name);

-- Post-check 2: remaining drift after the backfill. This should also be zero.
SELECT
  ua.id AS agent_id,
  ua.username,
  ua.nickname AS current_useragent_nick,
  u.nick_name AS canonical_users_nick,
  ua.level,
  ua.parentid,
  ua.code
FROM vinplay_admin.useragent ua
JOIN vinplay.users u
  ON BINARY ua.username = BINARY u.user_name
WHERE u.nick_name IS NOT NULL
  AND u.nick_name <> ''
  AND (ua.nickname IS NULL OR BINARY ua.nickname <> BINARY u.nick_name)
ORDER BY ua.level, ua.id;

-- Post-check 3: current config rows still keyed by the old nickname because a
-- duplicate canonical row already existed. Review and merge/delete manually if
-- this returns rows.
SELECT
  'game_commission_rate' AS source_table,
  g.id AS row_id,
  a.agent_id,
  a.username,
  a.old_useragent_nick,
  a.new_users_nick,
  g.game_key,
  g.rate
FROM vinplay.game_commission_rate g
JOIN vinplay_admin.useragent_nickname_mismatch_20260501 a
  ON BINARY g.agent_nickname = BINARY a.old_useragent_nick
WHERE a.action = 'UPDATED'
UNION ALL
SELECT
  'rebate_config' AS source_table,
  r.id AS row_id,
  a.agent_id,
  a.username,
  a.old_useragent_nick,
  a.new_users_nick,
  NULL AS game_key,
  r.rebate_percentage AS rate
FROM vinplay.rebate_config r
JOIN vinplay_admin.useragent_nickname_mismatch_20260501 a
  ON r.agent_user_id = a.agent_id
 AND BINARY r.agent_nickname = BINARY a.old_useragent_nick
WHERE a.action = 'UPDATED';
