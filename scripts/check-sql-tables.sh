#!/bin/bash
# ============================================================
# CI Check: Verify SQL table references in Java source exist in DB
# Catches bugs like referencing "bank_withdrawal_transactions"
# when the actual table is "bank_withdrawals"
# ============================================================

MYSQL="docker exec sunwinkr-mysql mysql -u root -p-Lo1HgJvrWmb-gSb-cUZV9BGkrDgMa7R -sN"
ERRORS=0

echo "=== Checking SQL table references ==="

# Get all real tables from all databases
REAL_TABLES=$($MYSQL -e "
SELECT CONCAT(TABLE_SCHEMA, '.', TABLE_NAME) FROM information_schema.TABLES
WHERE TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai','cgame')
UNION
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA IN ('vinplay','vinplay_admin','vinplay_minigame','vinplay_gamebai','cgame');
" 2>/dev/null)

# Extract table names from Java SQL strings
# Pattern: FROM/INTO/UPDATE/JOIN table_name
grep -rhoP '(?:FROM|INTO|UPDATE|JOIN)\s+[`"]?(\w+)[`"]?' \
  backend-master/api/VinPlayPortal/src/ \
  backend-master/api/VinPlayBackend/src/ \
  --include="*.java" 2>/dev/null | \
  sed 's/FROM\s*//;s/INTO\s*//;s/UPDATE\s*//;s/JOIN\s*//;s/[`"]//g' | \
  grep -v '^$' | sort -u | while read TABLE; do
    # Skip common SQL keywords that look like tables
    case "$TABLE" in
      SELECT|WHERE|SET|VALUES|AND|OR|ON|AS|NOT|NULL|IN|LIKE|BETWEEN|EXISTS|DUAL|information_schema) continue ;;
    esac
    # Check if table exists
    if ! echo "$REAL_TABLES" | grep -qw "$TABLE"; then
      echo "  ❌ Table '$TABLE' referenced in Java but NOT in DB"
      ((ERRORS++))
    fi
done

if [ $ERRORS -gt 0 ]; then
  echo ""
  echo "  $ERRORS table(s) missing — check Java SQL references"
  exit 1
else
  echo "  ✅ All SQL table references match existing DB tables"
  exit 0
fi
