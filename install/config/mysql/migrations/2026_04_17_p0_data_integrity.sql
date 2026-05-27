-- =============================================================================
-- P0: Critical Data Integrity Fixes
-- SUN-930: Database architecture optimization
-- Safe to run online. Idempotent.
-- =============================================================================

-- P0-1a: Fix broken parent refs in useragent
UPDATE vinplay_admin.useragent SET parentid = 0 WHERE id = 151 AND parentid = -1;

-- P0-1b: Fix dai_ly inconsistency (agents in useragent but dai_ly=0 in users)
UPDATE vinplay.users u
JOIN vinplay_admin.useragent a ON a.username COLLATE utf8mb3_general_ci = u.user_name
SET u.dai_ly = 1
WHERE u.dai_ly = 0 AND a.active = 1 AND a.role = 'agent';

-- P0-1c: Fix parent_agent_id referencing deleted agents
UPDATE vinplay.users SET parent_agent_id = NULL
WHERE parent_agent_id IS NOT NULL
  AND parent_agent_id NOT IN (SELECT id FROM vinplay_admin.useragent);

-- P0-1d: Delete orphan rebate_configs referencing non-existent agents
DELETE FROM vinplay.rebate_config
WHERE agent_user_id NOT IN (SELECT id FROM vinplay_admin.useragent);

-- P0-3: Fix referral_code index — drop the lying "UNIQUE" name, recreate properly
-- First check for duplicates (run SELECT manually before applying):
-- SELECT referral_code, COUNT(*) FROM vinplay.users
--   WHERE referral_code IS NOT NULL AND referral_code != ''
--   GROUP BY referral_code HAVING COUNT(*) > 1;
-- If duplicates exist, resolve them first, then:
-- ALTER TABLE vinplay.users DROP INDEX referral_code_UNIQUE;
-- ALTER TABLE vinplay.users ADD UNIQUE INDEX referral_code_UNIQUE (referral_code);
