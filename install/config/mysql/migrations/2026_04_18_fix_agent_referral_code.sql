-- SUN-880: Fix agents' referral_code to show their OWN code, not their parent's
-- Previously users.referral_code stored "who referred me" for everyone.
-- For agents, it should be their own useragent.code so FE displays the
-- correct shareable referral code regardless of which field it reads.

UPDATE vinplay.users u
JOIN vinplay_admin.useragent ua ON ua.nickname = u.nick_name COLLATE utf8mb3_general_ci
SET u.referral_code = ua.code
WHERE ua.active = 1 AND ua.role IN ('agent','admin')
  AND (CAST(u.referral_code AS CHAR) != CAST(ua.code AS CHAR) OR u.referral_code IS NULL);
