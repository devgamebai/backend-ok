#!/usr/bin/env bash
#
# Stress-test GSC seamless wallet endpoints + dump server-side p99 snapshots.
#
# Pulls a set of test accounts from MySQL, runs the harness against staging,
# then greps the AggregatorP99Scheduler INFO lines from game-thirdparty
# container logs to compare client-observed vs server-observed percentiles.
#
# Usage:
#   tests/stress/run_stress.sh [concurrency] [duration_seconds] [profile]
# Defaults: concurrency=100, duration=60, profile=bet-round
#
# Env overrides:
#   HOST              base URL                (default staging-play.sunkr.bet)
#   PRODUCT           GSC product_code        (default 1052 Dream Gaming)
#   PROFILE           balance-only|bet-round  (default bet-round)
#   ACCOUNT_COUNT     # of accounts to pull   (default 50)
#   FAIL_P99_MS       exit 1 if p99 over     (default 500)
#   FAIL_ERR_PCT      exit 1 if errors over  (default 1.0)

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
ENV_FILE="$ROOT/.env"

CONCURRENCY="${1:-100}"
DURATION="${2:-60}"
PROFILE="${3:-${PROFILE:-bet-round}}"
HOST="${HOST:-https://staging-play.sunkr.bet}"
PRODUCT="${PRODUCT:-1052}"
ACCOUNT_COUNT="${ACCOUNT_COUNT:-500}"
FAIL_P99_MS="${FAIL_P99_MS:-500}"
FAIL_ERR_PCT="${FAIL_ERR_PCT:-1.0}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT_DIR="$HERE/results"
mkdir -p "$OUT_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: $ENV_FILE not found" >&2
  exit 2
fi

# shellcheck disable=SC1090
. <(grep -E '^(GSC_OPERATOR_CODE|GSC_SECRET_KEY|GSC_CURRENCY|MYSQL_USER|MYSQL_PASSWORD)=' "$ENV_FILE")

if [[ -z "${GSC_SECRET_KEY:-}" ]]; then
  echo "ERROR: GSC_SECRET_KEY missing in $ENV_FILE" >&2
  exit 2
fi

ACCOUNTS_FILE="$OUT_DIR/accounts_${STAMP}.txt"
echo "[stress] pulling top $ACCOUNT_COUNT funded accounts from MySQL"
docker exec sunwinkr-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" vinplay \
    -Ns -e "SELECT nick_name FROM users WHERE vin >= 1000 AND status = 0 AND nick_name IS NOT NULL ORDER BY id LIMIT $ACCOUNT_COUNT;" \
    2>/dev/null > "$ACCOUNTS_FILE"

ACCOUNTS_N="$(wc -l < "$ACCOUNTS_FILE")"
if [[ "$ACCOUNTS_N" -lt 1 ]]; then
  echo "ERROR: no accounts pulled" >&2
  exit 3
fi
echo "[stress] $ACCOUNTS_N accounts -> $ACCOUNTS_FILE"

JSON_OUT="$OUT_DIR/result_${STAMP}_c${CONCURRENCY}_d${DURATION}_${PROFILE}.json"
LOG_OUT="$OUT_DIR/run_${STAMP}.log"

echo "[stress] reset server-side metrics window: waiting 60s (one scheduler tick)..."
SERVER_BASELINE_TS="$(date '+%Y-%m-%d %H:%M:%S')"

set +e
GSC_OPERATOR_CODE="$GSC_OPERATOR_CODE" \
GSC_SECRET_KEY="$GSC_SECRET_KEY" \
GSC_CURRENCY="$GSC_CURRENCY" \
python3 "$HERE/stress_gsc_seamless.py" \
    --host "$HOST" \
    --operator "$GSC_OPERATOR_CODE" \
    --secret  "$GSC_SECRET_KEY" \
    --currency "$GSC_CURRENCY" \
    --product "$PRODUCT" \
    --concurrency "$CONCURRENCY" \
    --duration "$DURATION" \
    --profile "$PROFILE" \
    --accounts-file "$ACCOUNTS_FILE" \
    --insecure \
    --json-out "$JSON_OUT" \
    --fail-on-error-pct "$FAIL_ERR_PCT" \
    --fail-on-p99-ms "$FAIL_P99_MS" \
    | tee "$LOG_OUT"
RC=${PIPESTATUS[0]}
set -e

echo ""
echo "=== SERVER-SIDE AGGREGATOR P99 (last scheduler tick) ==="
# Each scheduler tick logs one INFO line per aggregator. Pull the latest
# line per name from after we started.
docker logs sunwinkr-game-thirdparty --since 5m 2>&1 \
  | grep -E "AggregatorP99Scheduler: (Gsc|SLOW)" \
  | tail -50 \
  | tee "$OUT_DIR/server_p99_${STAMP}.log"

echo ""
echo "[stress] artifacts in $OUT_DIR :"
echo "   - $JSON_OUT"
echo "   - $LOG_OUT"
echo "   - $OUT_DIR/server_p99_${STAMP}.log"
exit $RC
