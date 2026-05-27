#!/usr/bin/env bash
#
# test-gsc-seamless.sh
#
# Run the GSC+ Seamless Wallet testcase suite against our staging endpoint and
# report pass/fail per test. Hits the GSC-provided test-runner service which in
# turn calls our /gsc/v1/api/seamless/* endpoints with a matrix of canned
# scenarios (balance, withdraw, deposit, cancel, pushbet, rollback, sign, ...).
#
# Usage:
#     scripts/test-gsc-seamless.sh                    # default: staging laviai
#     scripts/test-gsc-seamless.sh -u testag010       # override member
#     scripts/test-gsc-seamless.sh -c IDR             # override currency
#     scripts/test-gsc-seamless.sh -v                 # verbose: show full JSON per fail
#     scripts/test-gsc-seamless.sh --save out.json    # save raw response
#
# Credentials are pulled live from vinplay.game_config name='gsc_game' unless
# overridden via env vars (GSC_AGENT_URL, GSC_AGENT_CODE, GSC_SECRET_KEY).
#
# Exits 0 if every test passes, 1 otherwise.

set -euo pipefail

MEMBER="laviai"
PRODUCT_ID=1002
CURRENCY="IDR2"
VERBOSE=0
SAVE_PATH=""
RUNNER_URL="https://staging.gsimw.com/v1/system/test-case/run"

while [[ $# -gt 0 ]]; do
    case "$1" in
        -u|--user)     MEMBER="$2"; shift 2 ;;
        -p|--product)  PRODUCT_ID="$2"; shift 2 ;;
        -c|--currency) CURRENCY="$2"; shift 2 ;;
        -v|--verbose)  VERBOSE=1; shift ;;
        --save)        SAVE_PATH="$2"; shift 2 ;;
        -h|--help)     sed -n '2,22p' "$0"; exit 0 ;;
        *) echo "unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Pull agent_url / agent_code / secret_key from DB unless caller overrides.
if [[ -z "${GSC_AGENT_URL:-}" || -z "${GSC_AGENT_CODE:-}" || -z "${GSC_SECRET_KEY:-}" ]]; then
    if ! command -v docker >/dev/null; then
        echo "docker not found and GSC_* env vars unset — cannot resolve credentials" >&2
        exit 2
    fi
    ENV_FILE="$(dirname "$0")/../.env"
    if [[ ! -f "$ENV_FILE" ]]; then
        echo "cannot find .env at $ENV_FILE — set GSC_AGENT_URL/GSC_AGENT_CODE/GSC_SECRET_KEY manually" >&2
        exit 2
    fi
    MYSQL_PW="$(grep '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"
    CONFIG_JSON="$(
        docker exec sunwinkr-mysql mysql -uroot -p"$MYSQL_PW" -D vinplay -sN \
            -e "SELECT value FROM game_config WHERE name='gsc_game' LIMIT 1;" 2>/dev/null \
        || true
    )"
    if [[ -z "$CONFIG_JSON" ]]; then
        echo "failed to read game_config.gsc_game from MySQL" >&2
        exit 2
    fi
    GSC_AGENT_URL="${GSC_AGENT_URL:-$(echo "$CONFIG_JSON" | python3 -c 'import json,sys;print(json.load(sys.stdin)["operatorLobbyUrl"]+"/gsc")')}"
    GSC_AGENT_CODE="${GSC_AGENT_CODE:-$(echo "$CONFIG_JSON" | python3 -c 'import json,sys;print(json.load(sys.stdin)["operator_code"])')}"
    GSC_SECRET_KEY="${GSC_SECRET_KEY:-$(echo "$CONFIG_JSON" | python3 -c 'import json,sys;print(json.load(sys.stdin)["secret_key"])')}"
fi

echo "GSC Seamless Wallet testcase suite"
echo "  runner:    $RUNNER_URL"
echo "  agent_url: $GSC_AGENT_URL"
echo "  agent:     $GSC_AGENT_CODE"
echo "  member:    $MEMBER   (currency=$CURRENCY product_id=$PRODUCT_ID)"
echo

PAYLOAD="{\"agent_url\":\"$GSC_AGENT_URL\",\"agent_code\":\"$GSC_AGENT_CODE\",\"secret_key\":\"$GSC_SECRET_KEY\",\"currency\":\"$CURRENCY\",\"member_name\":\"$MEMBER\",\"product_id\":$PRODUCT_ID,\"member_name1\":\"$MEMBER\",\"product_id2\":$PRODUCT_ID,\"version\":\"1\"}"

RESP=$(
    curl -sS "$RUNNER_URL" \
        -H 'content-type: application/json' \
        -H 'origin: https://testcase.gscplusmd.com' \
        -H 'referer: https://testcase.gscplusmd.com/' \
        --data-raw "$PAYLOAD"
) || { echo "curl failed" >&2; exit 2; }

if [[ -n "$SAVE_PATH" ]]; then
    printf '%s' "$RESP" > "$SAVE_PATH"
    echo "(raw response saved to $SAVE_PATH)"
    echo
fi

# Parse + pretty-print. Exits non-zero if any item failed.
TMP_RESP=$(mktemp)
trap 'rm -f "$TMP_RESP"' EXIT
printf '%s' "$RESP" > "$TMP_RESP"
export VERBOSE
python3 - "$TMP_RESP" <<'PY'
import json, os, sys
verbose = os.environ.get("VERBOSE") == "1"
raw = open(sys.argv[1]).read()
try:
    doc = json.loads(raw)
except Exception as e:
    print("FAIL: unparseable response:", e)
    print(raw[:800])
    sys.exit(2)

results = doc.get("results") or {}
if not results:
    print("FAIL: no results in response")
    print(json.dumps(doc, indent=2)[:800])
    sys.exit(2)

total = 0
passed = 0
failed = []
for name, group in results.items():
    for item in group.get("items", []):
        total += 1
        msg = item.get("message", "")
        if item.get("pass"):
            passed += 1
            print(f"  PASS  {name}: {msg}")
        else:
            failed.append((name, msg, item))
            print(f"  FAIL  {name}: {msg}")

print()
print(f"RESULT: {passed}/{total} passed")
if failed:
    # Common infrastructure gotcha: the Hazelcast `users` IMap must contain the
    # member before BalanceProcess returns anything other than 1000/"Member not found".
    # The IMap is populated on login, so if you just restarted hazelcast or the member
    # has never logged in on this environment, hit the portal login once first.
    if any("Member not found" in (it.get('response') or '') for _, _, it in failed):
        print()
        print("HINT: 'Member not found' usually means the user is not yet in the")
        print("      Hazelcast 'users' IMap. Log the test user in via the portal")
        print("      (c=3 login) or restart a service that preloads users.")
    print()
    print("=== FAILED CASES ===")
    for name, msg, item in failed:
        print(f"--- {name}: {msg}")
        print(f"    url:      {item.get('apiUrl','')}")
        print(f"    request:  {item.get('request','')[:400]}")
        print(f"    response: {item.get('response','')[:400]}")
        if verbose:
            print(json.dumps(item, indent=2))
        print()
    sys.exit(1)
sys.exit(0)
PY
