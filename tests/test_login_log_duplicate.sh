#!/bin/bash
# Test: Login Log Duplicate Detection (c=110)
# Tests: c=110 USER_LOGIN_LOG_DUPLICATE — bulk scan, mu threshold, 90-day date cap

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

# ────────────────────────────────────────────────────────────────
# T1 — Bulk scan with default 7-day window
# ────────────────────────────────────────────────────────────────
section "LOGIN LOG DUPLICATE — Default 7-day scan (c=110)"

test_name "T1: bulk scan returns success with groups array (c=110)"
RESP=$(admin_get "c=110")
assert_success "$RESP"

# Check top-level fields: groups, total, totalRecord
SCHEMA_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    missing = []
    if not isinstance(d.get('groups'), list):
        missing.append('groups (array)')
    if 'total' not in d:
        missing.append('total')
    if 'totalRecord' not in d:
        missing.append('totalRecord')
    print('missing:' + ','.join(missing) if missing else 'ok')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
if [[ "$SCHEMA_CHECK" == "ok" ]]; then
    _pass "top-level schema has groups[], total, totalRecord — c=110"
else
    _fail "top-level schema check failed (${SCHEMA_CHECK}) — c=110"
fi

# Check each group has required fields and user_count >= 2
GROUP_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    groups = d.get('groups', [])
    if not isinstance(groups, list):
        print('not_array')
    elif len(groups) == 0:
        # Empty result is acceptable (no duplicates in the window); skip field checks
        print('empty_ok')
    else:
        required_fields = {'ip', 'user_count', 'first_seen', 'last_seen', 'users'}
        errors = []
        for i, g in enumerate(groups):
            missing_f = required_fields - set(g.keys())
            if missing_f:
                errors.append(f'groups[{i}] missing fields: {missing_f}')
            uc = g.get('user_count', 0)
            if isinstance(uc, (int, float)) and uc < 2:
                errors.append(f'groups[{i}].user_count={uc} < 2')
            if not isinstance(g.get('users'), list):
                errors.append(f'groups[{i}].users is not an array')
        print('errors:' + '; '.join(errors) if errors else 'ok')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
if [[ "$GROUP_CHECK" == "ok" ]]; then
    _pass "all groups have required fields and user_count >= 2 — c=110"
elif [[ "$GROUP_CHECK" == "empty_ok" ]]; then
    _pass "groups array is empty (no duplicates in window) — c=110"
    _skip "cannot verify group schema (no data in default window)"
else
    _fail "group schema/threshold check failed (${GROUP_CHECK}) — c=110"
fi

# Print counts for visibility
echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    groups = d.get('groups', [])
    print(f'  groups count: {len(groups)}, total: {d.get(\"total\",\"?\")}, totalRecord: {d.get(\"totalRecord\",\"?\")}')
except Exception:
    pass
" 2>/dev/null || true

# ────────────────────────────────────────────────────────────────
# T2 — Raise threshold to mu=3
# ────────────────────────────────────────────────────────────────
section "LOGIN LOG DUPLICATE — mu=3 threshold (c=110)"

test_name "T2: mu=3 returns success with user_count >= 3 (c=110)"
RESP=$(admin_get "c=110&mu=3")
assert_success "$RESP"

MU3_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    groups = d.get('groups', [])
    if not isinstance(groups, list):
        print('not_array')
    elif len(groups) == 0:
        print('empty_ok')
    else:
        bad = [i for i, g in enumerate(groups)
               if isinstance(g.get('user_count'), (int, float)) and g['user_count'] < 3]
        if bad:
            samples = [f'groups[{i}].user_count={groups[i].get(\"user_count\")}' for i in bad[:3]]
            print('threshold_violated:' + '; '.join(samples))
        else:
            print('ok')
except Exception as e:
    print('error:' + str(e))
" 2>/dev/null)
if [[ "$MU3_CHECK" == "ok" ]]; then
    _pass "all groups have user_count >= 3 when mu=3 — c=110"
elif [[ "$MU3_CHECK" == "empty_ok" ]]; then
    _pass "groups array is empty (no IPs with 3+ users in window) — c=110"
else
    _fail "mu=3 threshold check failed (${MU3_CHECK}) — c=110"
fi

# ────────────────────────────────────────────────────────────────
# T3 — Date range > 90 days must be rejected
# ────────────────────────────────────────────────────────────────
section "LOGIN LOG DUPLICATE — 90-day date cap rejection (c=110)"

test_name "T3: date range > 90 days is rejected with ERR_DATE_RANGE_TOO_LARGE (c=110)"
RESP=$(admin_get "c=110&ts=2020-01-01&te=2026-05-11")
assert_error "$RESP" "ERR_DATE_RANGE_TOO_LARGE"

# Also confirm success is false
SUCCESS_CHECK=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    s = d.get('success')
    print('false' if s is False else str(s))
except Exception:
    print('non-json')
" 2>/dev/null)
if [[ "$SUCCESS_CHECK" == "false" ]]; then
    _pass "success=false for over-range date window — c=110"
else
    _fail "expected success=false for over-range date window, got: ${SUCCESS_CHECK} — c=110"
fi

print_summary
