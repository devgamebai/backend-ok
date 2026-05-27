#!/usr/bin/env bash
# =============================================================================
# cache-migration-audit.sh — Phase 1 deliverable for HAZELCAST_TO_REDIS_PLAN.md
#
# Catalogs every Hazelcast IMap, every distributed-lock call site, every
# EntryListener, and every Hazelcast-only feature usage in backend-master/.
# Outputs three CSVs that drive the rest of the migration:
#
#   docs/architecture/cache-migration/imap-inventory-<DATE>.csv
#   docs/architecture/cache-migration/lock-sites-<DATE>.csv
#   docs/architecture/cache-migration/listener-sites-<DATE>.csv
#
# Each row in lock-sites has a TRIAGE column with one of:
#   DELETE   — the SQL transaction already provides atomicity
#   SETNX    — single-process coordination, replaceable with SET NX EX
#   REDLOCK  — cross-shard critical section, needs Redlock
#   REVIEW   — automatic classification couldn't decide; needs human read
#
# The classification is deliberately conservative: anything ambiguous lands in
# REVIEW. The audit's job is to give an honest worklist, not to over-promise.
#
# Usage:   bash scripts/cache-migration-audit.sh
# Re-run:  safe — overwrites today's CSVs
# =============================================================================

set -eu
# Intentionally NOT using `pipefail` — many ... | head -N pipes cause SIGPIPE
# on the upstream grep, which would abort the script under pipefail. We
# handle real errors with `|| true` where it matters.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/backend-master"
DATE="$(date +%Y-%m-%d)"
OUT="$ROOT/docs/architecture/cache-migration"
mkdir -p "$OUT"

IMAP_CSV="$OUT/imap-inventory-$DATE.csv"
LOCK_CSV="$OUT/lock-sites-$DATE.csv"
LISTENER_CSV="$OUT/listener-sites-$DATE.csv"
SUMMARY="$OUT/summary-$DATE.md"

if [[ ! -d "$SRC" ]]; then
  echo "ERROR: $SRC not found. Run from a checkout that contains backend-master/." >&2
  exit 1
fi

echo "Auditing Hazelcast usage under $SRC ..."
echo

# -----------------------------------------------------------------------------
# 1. IMap inventory: name, call-site count, distinct files, criticality
# -----------------------------------------------------------------------------
echo "imap_name,call_sites,files,sample_file,criticality,migration_wave" > "$IMAP_CSV"

# Find every distinct IMap name from getMap("...") calls.
mapfile -t IMAP_NAMES < <(
  # Match getMap("...") regardless of trailing args. Some call sites pass a
  # config object: getMap("users", cfg) — the closing paren isn't adjacent.
  grep -rEoh 'getMap\("[^"]+"' "$SRC" --include="*.java" 2>/dev/null \
    | sed 's/getMap("//; s/"$//' \
    | sort -u
)

classify_criticality() {
  local name="$1"
  local sites="$2"
  case "$name" in
    users|auths|cacheToken|cache_user_money|cache_user_active)
      echo "CRITICAL"
      ;;
    gsc_wager_codes|userMissionCache|missionMap|usersSetWin|cacheSetUserJackpot)
      echo "HIGH"
      ;;
    *)
      if [[ "$sites" -gt 20 ]]; then
        echo "HIGH"
      elif [[ "$sites" -gt 5 ]]; then
        echo "MEDIUM"
      else
        echo "LOW"
      fi
      ;;
  esac
}

# Wave assignment: critical = 4 (last), high = 3, medium = 2, low = 1.
classify_wave() {
  case "$1" in
    CRITICAL) echo "4" ;;
    HIGH)     echo "3" ;;
    MEDIUM)   echo "2" ;;
    LOW)      echo "1" ;;
  esac
}

for name in "${IMAP_NAMES[@]}"; do
  # Drop the closing-paren constraint — see the IMAP_NAMES discovery query.
  sites=$(grep -rEn "getMap\(\"$name\"" "$SRC" --include="*.java" 2>/dev/null | wc -l)
  files=$(grep -rlE "getMap\(\"$name\"" "$SRC" --include="*.java" 2>/dev/null | wc -l)
  sample=$(grep -rlE "getMap\(\"$name\"" "$SRC" --include="*.java" 2>/dev/null | sort | head -n1 | sed "s|^$ROOT/||")
  crit=$(classify_criticality "$name" "$sites")
  wave=$(classify_wave "$crit")
  printf '%s,%d,%d,"%s",%s,%s\n' "$name" "$sites" "$files" "$sample" "$crit" "$wave" >> "$IMAP_CSV"
done

IMAP_COUNT=${#IMAP_NAMES[@]}
echo "  IMaps found: $IMAP_COUNT → $IMAP_CSV"

# -----------------------------------------------------------------------------
# 1b. Hidden-reader sweep — for each IMap, find any source file that
#     references its name as a string literal but doesn't appear in the
#     primary inventory (catches `static final String CACHE_X = "..."` +
#     `getMap(CACHE_X)` indirection that the v1 audit missed).
#     Cross-container readers that the v1 audit lost cost us a rollback —
#     see HAZELCAST_TO_REDIS_PLAN.md "cacheGameRtp rollback 2026-05-07".
# -----------------------------------------------------------------------------
HIDDEN_CSV="$OUT/hidden-readers-$DATE.csv"
echo "imap_name,file,line,snippet" > "$HIDDEN_CSV"
HIDDEN_TOTAL=0
for name in "${IMAP_NAMES[@]}"; do
  # Files that mention the name as a string literal anywhere
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    # Skip files that already use getMap("$name") directly — those are
    # the primary call sites in IMAP_CSV.
    if grep -qE "getMap\(\"$name\"" "$f" 2>/dev/null; then continue; fi
    # Look for "$name" as a string literal — usually `= "name"` for a
    # constant or `"name"` passed to some helper.
    while IFS=: read -r ln snippet; do
      [[ -z "$ln" ]] && continue
      HIDDEN_TOTAL=$((HIDDEN_TOTAL + 1))
      rel_file=${f#"$ROOT/"}
      clean=$(echo "$snippet" | head -c 100 | tr -d '"' | tr ',' ';')
      printf '%s,"%s",%s,"%s"\n' "$name" "$rel_file" "$ln" "$clean" >> "$HIDDEN_CSV"
    done < <(grep -nE "[\"' ]$name[\"' ]" "$f" 2>/dev/null | head -3)
  done < <(grep -rl "\"$name\"" "$SRC" --include="*.java" 2>/dev/null)
done
echo "  Hidden-reader hits: $HIDDEN_TOTAL → $HIDDEN_CSV"
echo "    (each row = a file that mentions an IMap name but doesn't call getMap on it directly — review for indirection via constants)"

# -----------------------------------------------------------------------------
# 2. Lock-call inventory + automatic triage
# -----------------------------------------------------------------------------
echo "file,line,snippet,nearby_sql,triage,reason" > "$LOCK_CSV"

# Match .lock( and .tryLock( on lines that look like Hazelcast usage.
# Capture file:line:snippet, then look at ±10 lines for context to triage.
LOCK_RAW=$(grep -rEn "\.tryLock\(|\.lock\([^)]" "$SRC" --include="*.java" 2>/dev/null \
  | grep -v "ReentrantLock\|ReadWriteLock\|synchronized\|//\|^\s*\*" \
  || true)

LOCK_TOTAL=0
DELETE_COUNT=0
SETNX_COUNT=0
REDLOCK_COUNT=0
REVIEW_COUNT=0

# Process line-by-line; pull ±10 lines of context to detect the "near a SQL
# UPDATE that has its own atomicity" pattern that drives the DELETE bucket.
while IFS= read -r raw; do
  [[ -z "$raw" ]] && continue
  LOCK_TOTAL=$((LOCK_TOTAL + 1))
  file=$(echo "$raw" | cut -d: -f1)
  line=$(echo "$raw" | cut -d: -f2)
  snippet=$(echo "$raw" | cut -d: -f3- | head -c 120 | tr -d '"' | tr ',' ';')
  ctx_start=$(( line > 10 ? line - 10 : 1 ))
  ctx_end=$(( line + 10 ))
  ctx=$(sed -n "${ctx_start},${ctx_end}p" "$file" 2>/dev/null || echo "")

  triage="REVIEW"
  reason="needs human classification"
  near_sql=""

  # DELETE: the lock is wrapped around a cache update that follows an SQL
  # write whose WHERE clause already enforces atomicity — common for the
  # MoneyGateway and audit-log paths.
  if echo "$ctx" | grep -qE "UPDATE [a-zA-Z_]+ SET [a-zA-Z_]+ ?= ?[a-zA-Z_]+ ?- ?\?"; then
    triage="DELETE"
    reason="adjacent SQL UPDATE has its own atomic guard (subtractive WHERE)"
    near_sql=$(echo "$ctx" | grep -oE "UPDATE [a-zA-Z_]+ SET [^\"]+\?\"" | head -1 | tr ',' ';' | head -c 80)
  elif echo "$ctx" | grep -qE "INSERT IGNORE|ON DUPLICATE KEY"; then
    triage="DELETE"
    reason="adjacent INSERT IGNORE / ON DUPLICATE KEY is the dedup primitive"
  # SETNX: the lock guards a single-process critical section (poller, scheduled
  # task, one-of-N coordination). Heuristic: variable name suggests a job
  # name, scheduler, or "leader".
  elif echo "$snippet" | grep -qiE "scheduler|poller|leader|cron|sweep|reconcile"; then
    triage="SETNX"
    reason="single-process coordination (scheduler/poller pattern)"
  # REDLOCK is rare; only flag if the variable name screams cross-shard.
  elif echo "$snippet" | grep -qiE "redlock|globalLock|crossShard"; then
    triage="REDLOCK"
    reason="explicit cross-shard naming"
  fi

  case "$triage" in
    DELETE)  DELETE_COUNT=$((DELETE_COUNT + 1)) ;;
    SETNX)   SETNX_COUNT=$((SETNX_COUNT + 1)) ;;
    REDLOCK) REDLOCK_COUNT=$((REDLOCK_COUNT + 1)) ;;
    REVIEW)  REVIEW_COUNT=$((REVIEW_COUNT + 1)) ;;
  esac

  rel_file=${file#"$ROOT/"}
  printf '"%s",%s,"%s","%s",%s,"%s"\n' \
    "$rel_file" "$line" "$snippet" "$near_sql" "$triage" "$reason" >> "$LOCK_CSV"
done <<< "$LOCK_RAW"

echo "  Lock sites: $LOCK_TOTAL → $LOCK_CSV"
echo "    DELETE:  $DELETE_COUNT"
echo "    SETNX:   $SETNX_COUNT"
echo "    REDLOCK: $REDLOCK_COUNT"
echo "    REVIEW:  $REVIEW_COUNT (need human read)"

# -----------------------------------------------------------------------------
# 3. EntryListener inventory
# -----------------------------------------------------------------------------
echo "file,line,snippet" > "$LISTENER_CSV"

LISTENER_TOTAL=0
while IFS= read -r raw; do
  [[ -z "$raw" ]] && continue
  LISTENER_TOTAL=$((LISTENER_TOTAL + 1))
  file=$(echo "$raw" | cut -d: -f1)
  line=$(echo "$raw" | cut -d: -f2)
  snippet=$(echo "$raw" | cut -d: -f3- | head -c 120 | tr -d '"' | tr ',' ';')
  rel_file=${file#"$ROOT/"}
  printf '"%s",%s,"%s"\n' "$rel_file" "$line" "$snippet" >> "$LISTENER_CSV"
done < <(
  grep -rEn "\.addEntryListener\(" "$SRC" --include="*.java" 2>/dev/null \
    | grep -v "//\|^\s*\*"
)
echo "  EntryListener call sites: $LISTENER_TOTAL → $LISTENER_CSV"

# -----------------------------------------------------------------------------
# 4. Hazelcast-only feature scan (should be 0 per HAZELCAST_TO_REDIS_PLAN.md)
# -----------------------------------------------------------------------------
ITOPIC=$(grep -rln "getTopic\|ITopic" "$SRC" --include="*.java" 2>/dev/null | wc -l)
PROCESSOR=$(grep -rln "executeOnKey\|EntryProcessor" "$SRC" --include="*.java" 2>/dev/null | wc -l)
PREDICATES=$(grep -rln "Predicates\." "$SRC" --include="*.java" 2>/dev/null | wc -l)
PRIMITIVES=$(grep -rln "getCountDownLatch\|getSemaphore\|getAtomicLong\|getIdGenerator" "$SRC" --include="*.java" 2>/dev/null | wc -l)
REPLICATED=$(grep -rln "getReplicatedMap" "$SRC" --include="*.java" 2>/dev/null | wc -l)

echo "  Hazelcast-only features in use:"
echo "    ITopic:           $ITOPIC files"
echo "    EntryProcessor:   $PROCESSOR files"
echo "    Predicates:       $PREDICATES files"
echo "    Distributed prims: $PRIMITIVES files"
echo "    ReplicatedMap:    $REPLICATED files"

# -----------------------------------------------------------------------------
# 5. Summary
# -----------------------------------------------------------------------------
{
  echo "# Cache Migration Audit — $DATE"
  echo
  echo "Generated by \`scripts/cache-migration-audit.sh\`. Re-run anytime."
  echo
  echo "## Inventory"
  echo
  echo "| Metric | Count |"
  echo "|---|---|"
  echo "| Distinct IMaps | $IMAP_COUNT |"
  echo "| Lock call sites | $LOCK_TOTAL |"
  echo "| EntryListener call sites | $LISTENER_TOTAL |"
  echo "| ITopic usage (must be 0) | $ITOPIC |"
  echo "| EntryProcessor usage (must be 0) | $PROCESSOR |"
  echo "| Predicates usage (must be 0) | $PREDICATES |"
  echo "| Distributed primitives usage (must be 0) | $PRIMITIVES |"
  echo "| ReplicatedMap usage (must be 0) | $REPLICATED |"
  echo
  echo "## Lock-site triage"
  echo
  echo "| Bucket | Count | What to do |"
  echo "|---|---|---|"
  echo "| DELETE | $DELETE_COUNT | Remove the lock; SQL atomicity already covers it. |"
  echo "| SETNX | $SETNX_COUNT | Replace with \`cache.acquireLock(key, ttl)\`. |"
  echo "| REDLOCK | $REDLOCK_COUNT | Confirm cross-shard need; if real, use Redisson Redlock. |"
  echo "| REVIEW | $REVIEW_COUNT | **Human read required** — see \`lock-sites-$DATE.csv\`. |"
  echo
  echo "## Migration waves (from \`imap-inventory-$DATE.csv\`)"
  echo
  echo "| Wave | Criticality | Maps | Order |"
  echo "|---|---|---|---|"
  for w_label in 1:LOW 2:MEDIUM 3:HIGH 4:CRITICAL; do
    w=$(echo "$w_label" | cut -d: -f1)
    c=$(echo "$w_label" | cut -d: -f2)
    n=$(awk -F, -v c="$c" 'NR > 1 && $5 == c {n++} END{print n+0}' "$IMAP_CSV")
    when=$(case "$w" in 1) echo "Week 4–5 (Wave 1)";; 2) echo "Week 5 (Wave 2)";; 3) echo "Week 6 (Wave 3)";; 4) echo "Week 7–9 (Wave 4)";; esac)
    echo "| $w | $c | $n | $when |"
  done
  echo
  echo "## Files"
  echo
  echo "- \`$(basename "$IMAP_CSV")\` — every IMap with call-site count + criticality + wave"
  echo "- \`$(basename "$LOCK_CSV")\` — every lock site with auto-triage + reason"
  echo "- \`$(basename "$LISTENER_CSV")\` — every EntryListener site"
  echo
  echo "## Next step"
  echo
  echo "1. Walk the **REVIEW** rows in \`$(basename "$LOCK_CSV")\` and reclassify."
  echo "2. Confirm Hazelcast-only-feature counts above are all zero."
  echo "3. Begin Phase 3 (adapter layer) — see \`HAZELCAST_TO_REDIS_PLAN.md\`."
} > "$SUMMARY"

echo
echo "Summary: $SUMMARY"
echo "Done."
