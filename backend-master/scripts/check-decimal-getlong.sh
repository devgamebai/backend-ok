#!/usr/bin/env bash
# JDBC type-symmetry lint.
# Fails the build when Java code calls rs.getLong("col") on a column whose
# Flyway migrations declare DECIMAL — that path silently truncates fractional
# values (was the SUN-1150 read-side bug: 8.40 written, 8 read back).
#
# Approach:
# 1. Parse install/flyway/sql/V*.sql for `MODIFY COLUMN <name> DECIMAL...`
#    and `<name> DECIMAL(...) ...` patterns. Build the set of decimal column
#    names per file.
# 2. Grep Java sources under backend-master/ for `rs.getLong("<name>")`
#    where <name> matches.
# 3. Fail if any match found.
#
# Allowlist via DECIMAL_GETLONG_ALLOWLIST env (space-separated col names) for
# columns whose code path doesn't actually touch fractional values.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

# Build set of column names migrated to DECIMAL via `MODIFY COLUMN <name> DECIMAL(...)`.
# Skips comment lines, DECLARE variables, and CREATE TABLE column-name lines (which can
# false-positive on local variables and stored-proc params).
decimal_cols=$(grep -hPi 'MODIFY\s+COLUMN\s+`?[a-z_]+`?\s+DECIMAL\s*\(' \
    install/flyway/sql/V*.sql 2>/dev/null \
    | grep -oPi 'MODIFY\s+COLUMN\s+`?\K[a-z_]+' \
    | sort -u)

if [ -z "$decimal_cols" ]; then
    echo "OK: no DECIMAL columns declared in install/flyway/sql/V*.sql"
    exit 0
fi

echo "DECIMAL columns scanned: $(echo "$decimal_cols" | tr '\n' ' ')"

allowlist=" ${DECIMAL_GETLONG_ALLOWLIST:-} "

violations=0
for col in $decimal_cols; do
    case "$allowlist" in *" $col "*) continue ;; esac
    # Match: rs.getLong("col")  or  ResultSetVar.getLong("col")
    hits=$(grep -RHnE "\.getLong\(\"$col\"\)" backend-master/ \
        --include='*.java' 2>/dev/null || true)
    if [ -n "$hits" ]; then
        echo "FAIL: column '$col' is DECIMAL but read via getLong:"
        echo "$hits" | sed 's/^/  /'
        violations=$((violations + 1))
    fi
done

if [ $violations -gt 0 ]; then
    echo
    echo "FAIL: $violations DECIMAL column(s) read via getLong (silent truncation)."
    echo "Use rs.getBigDecimal(\"<col>\") instead. To suppress for a specific column"
    echo "(if the code path never touches the fraction), add it to"
    echo "DECIMAL_GETLONG_ALLOWLIST."
    exit 1
fi

echo "OK: no DECIMAL columns read via getLong."
