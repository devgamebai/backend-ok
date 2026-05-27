#!/bin/bash
# Test: Cashback Game Configs (c=9800 variant via portal)
# Tests the GetCashbackGameConfigs endpoint

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

section "CASHBACK GAME CONFIGS — Player Portal"

test_name "Get cashback game configs as player (c=9800)"
RESP=$(player_get "c=9800")
assert_not_empty "$RESP" "cashback game configs player"
OK=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success',False))" 2>/dev/null)
EC=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('errorCode',''))" 2>/dev/null)
echo "  success=$OK errorCode=$EC"

section "CASHBACK GAME CONFIGS — Admin"

test_name "Get cashback game configs as admin (c=9800)"
RESP=$(admin_get "c=9800")
assert_success "$RESP"
assert_has_field "$RESP" "data"

print_summary
