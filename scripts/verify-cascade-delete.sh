#!/usr/bin/env bash
# Phase D: cascade-delete verification.
#
# Picks a real user, opens a transaction, DELETEs the user row, counts
# remaining orphan rows across all tables that reference users.id, then
# ROLLBACKs the transaction. Reports any table that still holds orphans.
#
# Usage:
#   scripts/verify-cascade-delete.sh                # default user 50017
#   USER_ID=12345 scripts/verify-cascade-delete.sh

set -euo pipefail

USER_ID="${USER_ID:-50017}"
CONTAINER="${MYSQL_CONTAINER:-sunwinkr-mysql}"
PW="${MYSQL_ROOT_PW:--Lo1HgJvrWmb-gSb-cUZV9BGkrDgMa7R}"

MYSQL() {
    docker exec "${CONTAINER}" mysql -uroot -p"${PW}" -N "$@" 2>/dev/null
}

echo "==> Phase D — cascade-delete verification for user_id=${USER_ID}"
echo

# Step 1 — count rows ref'd to user across all user-ref tables (PRE-delete)
echo "Pre-delete row counts per table ref'ing user ${USER_ID}:"

# Build a UNION SELECT for every (schema.table.column) that references user_id
TABLES_SQL=$(cat <<EOF
SELECT CONCAT(TABLE_SCHEMA,'.',TABLE_NAME), COLUMN_NAME
FROM information_schema.columns
WHERE TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai')
  AND COLUMN_NAME IN ('user_id','userId')
  AND TABLE_NAME NOT LIKE '\\_archive%'
  AND TABLE_NAME NOT LIKE 'v\\_%'
ORDER BY 1;
EOF
)

# Tables to probe (schema.table:column)
TABLES=$(MYSQL -e "${TABLES_SQL}")

declare -a PROBES=()
while IFS=$'\t' read -r FQTABLE COL; do
    [[ -z "${FQTABLE}" ]] && continue
    PROBES+=("${FQTABLE}:${COL}")
done <<< "${TABLES}"

echo "Probing ${#PROBES[@]} tables for user_id=${USER_ID}..."

# PRE: count rows per table
PRE_FILE=$(mktemp)
for P in "${PROBES[@]}"; do
    FQ="${P%:*}"; COL="${P#*:}"
    CNT=$(MYSQL -e "SELECT COUNT(*) FROM ${FQ} WHERE ${COL}=${USER_ID}" 2>/dev/null || echo "ERR")
    if [[ "${CNT}" != "0" && "${CNT}" != "ERR" ]]; then
        echo "  ${FQ}.${COL} = ${CNT}"
        echo "${FQ}|${COL}|${CNT}" >> "${PRE_FILE}"
    fi
done
PRE_TOTAL=$(awk -F'|' '{sum+=$3} END{print sum+0}' "${PRE_FILE}")
echo "Total rows referencing user ${USER_ID} (pre): ${PRE_TOTAL}"
echo

if [[ "${PRE_TOTAL}" -eq 0 ]]; then
    echo "User has no related rows. Nothing to verify. Pick a different USER_ID."
    rm -f "${PRE_FILE}"
    exit 0
fi

# Step 2 — run DELETE in transaction, count orphans, rollback
echo "==> Running DELETE in transaction..."

# Inline POST-counts as a single SQL block within the same transaction
SQL_BLOCK=$(mktemp)
{
    echo "SET autocommit=0;"
    echo "START TRANSACTION;"
    echo "DELETE FROM vinplay.users WHERE id=${USER_ID};"
    echo "SELECT '---POST-COUNTS---' AS marker;"
    while IFS='|' read -r FQ COL CNT; do
        [[ -z "${FQ}" ]] && continue
        echo "SELECT '${FQ}' AS tbl, '${COL}' AS col, COUNT(*) AS post_cnt FROM ${FQ} WHERE ${COL}=${USER_ID};"
    done < "${PRE_FILE}"
    echo "ROLLBACK;"
} > "${SQL_BLOCK}"

docker cp "${SQL_BLOCK}" "${CONTAINER}":/tmp/cascade_test.sql
RAW=$(docker exec "${CONTAINER}" sh -c "mysql -uroot -p'${PW}' < /tmp/cascade_test.sql" 2>&1 | grep -v "Using a password")

echo
echo "==> Post-delete (in transaction) orphan rows:"
echo "${RAW}" | awk '/---POST-COUNTS---/{flag=1; next} flag' | awk -v OFS='\t' 'NF{print}' | while read -r line; do
    if [[ -n "${line}" && "${line}" != "tbl"* ]]; then
        FQ=$(echo "${line}" | awk '{print $1}')
        COL=$(echo "${line}" | awk '{print $2}')
        CNT=$(echo "${line}" | awk '{print $3}')
        if [[ "${CNT}" != "0" ]]; then
            echo "  ⚠ ORPHAN: ${FQ}.${COL} = ${CNT} rows survive cascade"
        fi
    fi
done

ORPHANS=$(echo "${RAW}" | awk '/---POST-COUNTS---/{flag=1; next} flag && NF>=3 && $3 ~ /^[0-9]+$/ && $3>0' | wc -l)

echo
if [[ "${ORPHANS}" -eq 0 ]]; then
    echo "✓ ZERO orphans. Cascade-delete is clean for user ${USER_ID}."
else
    echo "✗ ${ORPHANS} table(s) still hold orphan rows after user deletion."
    echo "  These tables ref user_id but lack ON DELETE CASCADE FK."
fi

rm -f "${PRE_FILE}" "${SQL_BLOCK}"
