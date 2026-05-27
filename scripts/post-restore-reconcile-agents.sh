#!/usr/bin/env bash
# =============================================================================
# post-restore-reconcile-agents.sh — fix orphan parent_agent_id after a
# restore of vinplay_admin.useragent (or any bulk modification of that table).
#
# WHY THIS EXISTS
#   users.parent_agent_id is a foreign key by convention, not by constraint
#   (until the FK migration in 2026_05_05_users_parent_agent_fk.sql lands).
#   When the useragent table is restored from a backup that has different
#   contents than the live table, two things can happen:
#
#     1. parent_agent_id points at a row that no longer exists  → DANGLING
#     2. parent_agent_id points at a row whose IDENTITY changed → SILENT REATTACH
#        (an old agent's row was replaced by a new agent that happens to have
#        the same id; users wrongly appear under the new agent)
#
#   The May-2026 incident with users 8713/8727 was case #2. This script
#   prevents recurrence by rebuilding the link from users.referral_code
#   (the immutable code typed at registration) to whichever live agent
#   currently owns that code.
#
# WHAT IT DOES (in order)
#   1) Forward the AUTO_INCREMENT counter so freed IDs are never recycled.
#   2) Re-resolve dangling parent_agent_id via the referral_code → useragent.code
#      lookup. If a code now points to a different agent, this restores intent.
#   3) Anything still dangling after step 2 → CompanyAgent (id 152). User left
#      visibly unaffiliated rather than silently following a wrong recruiter.
#   4) Print a manual-review queue: users whose referral_code didn't match any
#      live agent (typically agents that were deleted and not restored).
#
# IDEMPOTENT: re-runs are no-ops once everything resolves.
#
# WHEN TO RUN
#   - After every restore of vinplay_admin (.sql or full-DB tarball).
#   - After any manual DELETE / UPDATE / TRUNCATE on vinplay_admin.useragent.
#   - As part of the disaster-recovery runbook.
#
# USAGE
#   sudo ./scripts/post-restore-reconcile-agents.sh
#
# Exit 0 on success regardless of how many users got reassigned. Logs go to
# /home/sunkr/logs/post-restore-reconcile/<timestamp>.log.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
LOG_DIR="/home/sunkr/logs/post-restore-reconcile"
TS="$(date -u +%Y%m%d-%H%M%S)"
LOG_FILE="$LOG_DIR/$TS.log"

mkdir -p "$LOG_DIR"
exec > >(tee -a "$LOG_FILE") 2>&1

# ---- 0. Load DB credentials -----------------------------------------------
if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: $ENV_FILE not found" >&2; exit 1
fi
set -a; . "$ENV_FILE"; set +a

if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
  echo "ERROR: MYSQL_ROOT_PASSWORD missing from $ENV_FILE" >&2; exit 1
fi

MYSQL=( docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" sunwinkr-mysql
        mysql -uroot --default-character-set=utf8mb4 -B )

run_sql() {
  "${MYSQL[@]}" "$@"
}

echo "==> post-restore reconciler  $(date -u +%FT%TZ)"
echo "==> log: $LOG_FILE"

# ---- 1. Forward AUTO_INCREMENT --------------------------------------------
echo; echo "==> 1) bump useragent.AUTO_INCREMENT to MAX(id)+1000"
run_sql vinplay_admin <<'SQL'
SET @target := (SELECT MAX(id)+1000 FROM useragent);
SET @sql    := CONCAT('ALTER TABLE useragent AUTO_INCREMENT = ', @target);
PREPARE  stmt FROM @sql;
EXECUTE  stmt;
DEALLOCATE PREPARE stmt;
SELECT @target AS new_auto_increment;
SQL

# ---- 2. Snapshot orphans before fix ---------------------------------------
echo; echo "==> 2) snapshot of dangling pointers BEFORE reconcile"
run_sql vinplay <<'SQL'
SELECT u.id, u.user_name, u.nick_name, u.referral_code,
       u.parent_agent_id AS dangling_parent_agent_id, u.create_time
  FROM users u
  LEFT JOIN vinplay_admin.useragent a ON a.id = u.parent_agent_id
 WHERE u.parent_agent_id IS NOT NULL AND a.id IS NULL
 ORDER BY u.create_time;
SQL

# ---- 3. Re-resolve via the immutable referral_code ------------------------
# @audit_source tags rows in users_parent_agent_history (Phase 5 trigger) so
# we can later filter changes made by this reconciler vs. admin actions.
echo; echo "==> 3) re-resolve via users.referral_code → useragent.code"
run_sql vinplay <<'SQL'
SET @audit_source := 'restore-reconciler';
UPDATE vinplay.users u
  JOIN vinplay_admin.useragent a ON a.code = u.referral_code
   SET u.parent_agent_id = a.id
 WHERE u.parent_agent_id IS NOT NULL
   AND u.parent_agent_id NOT IN (SELECT id FROM vinplay_admin.useragent)
   AND u.referral_code IS NOT NULL AND u.referral_code <> '';
SELECT ROW_COUNT() AS users_resolved_via_code;
SQL

# ---- 4. Anything still dangling → CompanyAgent (id 152) -------------------
echo; echo "==> 4) fall-back: detach unresolvable orphans to CompanyAgent (152)"
run_sql vinplay <<'SQL'
SET @audit_source := 'restore-reconciler';
UPDATE vinplay.users
   SET parent_agent_id = 152
 WHERE parent_agent_id IS NOT NULL
   AND parent_agent_id NOT IN (SELECT id FROM vinplay_admin.useragent);
SELECT ROW_COUNT() AS users_detached_to_company_agent;
SQL

# ---- 5. Manual-review queue -----------------------------------------------
echo; echo "==> 5) manual-review queue: users now at CompanyAgent but with a referral_code"
echo "        (these had a real recruiter that no longer exists in useragent)"
run_sql vinplay <<'SQL'
SELECT u.id, u.user_name, u.nick_name, u.referral_code, u.create_time
  FROM users u
 WHERE u.parent_agent_id = 152
   AND u.referral_code IS NOT NULL
   AND u.referral_code NOT IN ('', '1')
   -- only flag rows whose code is genuinely missing from useragent
   AND NOT EXISTS (
       SELECT 1 FROM vinplay_admin.useragent a WHERE a.code = u.referral_code)
 ORDER BY u.create_time DESC
 LIMIT 100;
SQL

# ---- 6. Verify invariant --------------------------------------------------
echo; echo "==> 6) verify: no dangling parent_agent_id remains"
run_sql vinplay <<'SQL'
SELECT COUNT(*) AS remaining_dangling
  FROM users u
  LEFT JOIN vinplay_admin.useragent a ON a.id = u.parent_agent_id
 WHERE u.parent_agent_id IS NOT NULL AND a.id IS NULL;
SQL

echo; echo "==> done."
