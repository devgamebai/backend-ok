#!/usr/bin/env bash
#
# Stress-test AWC seamless wallet callback (/awc/callback) + dump server-side
# per-action p99 from AggregatorP99Scheduler (AwcGetBalance / AwcBet /
# AwcSettle).
#
# Usage: tests/stress/run_awc_stress.sh [concurrency] [duration_seconds] [profile]
# Defaults: concurrency=100, duration=60, profile=bet-round.

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
ENV_FILE="$ROOT/.env"

CONCURRENCY="${1:-100}"
DURATION="${2:-60}"
PROFILE="${3:-${PROFILE:-bet-round}}"
HOST="${HOST:-https://staging-play.sunkr.bet}"
ACCOUNT_COUNT="${ACCOUNT_COUNT:-500}"
FAIL_P99_MS="${FAIL_P99_MS:-500}"
FAIL_ERR_PCT="${FAIL_ERR_PCT:-1.0}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT_DIR="$HERE/results"
mkdir -p "$OUT_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: $ENV_FILE not found" >&2; exit 2
fi
# shellcheck disable=SC1090
. <(grep -E '^(AWC_CERT|AWC_PREFIX|MYSQL_USER|MYSQL_PASSWORD)=' "$ENV_FILE")

if [[ -z "${AWC_CERT:-}" ]]; then
  echo "ERROR: AWC_CERT missing in $ENV_FILE" >&2; exit 2
fi

ACCOUNTS_FILE="$OUT_DIR/awc_accounts_${STAMP}.txt"
echo "[awc-stress] pulling top $ACCOUNT_COUNT funded accounts from MySQL (lowercase-alnum nicknames only)"
# AWC user-id format only accepts [0-9a-z]; filter out nicks with special chars
# so awcUserIdToUsername can find the player on the way back.
docker exec sunwinkr-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" vinplay \
    -Ns -e "SELECT nick_name FROM users WHERE vin >= 1000 AND status = 0
            AND nick_name IS NOT NULL AND nick_name REGEXP '^[a-z0-9]+$'
            ORDER BY id LIMIT $ACCOUNT_COUNT;" 2>/dev/null > "$ACCOUNTS_FILE"

ACCOUNTS_N="$(wc -l < "$ACCOUNTS_FILE")"
if [[ "$ACCOUNTS_N" -lt 1 ]]; then
  echo "ERROR: no accounts pulled" >&2; exit 3
fi
echo "[awc-stress] $ACCOUNTS_N accounts -> $ACCOUNTS_FILE"

JSON_OUT="$OUT_DIR/awc_result_${STAMP}_c${CONCURRENCY}_d${DURATION}_${PROFILE}.json"
LOG_OUT="$OUT_DIR/awc_run_${STAMP}.log"

set +e
AWC_CERT="$AWC_CERT" AWC_PREFIX="${AWC_PREFIX:-lime}" \
python3 "$HERE/stress_awc_seamless.py" \
    --host "$HOST" \
    --cert "$AWC_CERT" \
    --prefix "${AWC_PREFIX:-lime}" \
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
echo "=== SERVER-SIDE AGGREGATOR P99 (portal-api scheduler ticks) ==="
# AggregatorP99Scheduler logs SLOW lines from inside portal-api JVM.
docker logs sunwinkr-portal-api --since 5m 2>&1 \
  | grep -E "AggregatorP99Scheduler: (SLOW|Awc|Gsc)" \
  | tail -50 \
  | tee "$OUT_DIR/awc_server_p99_${STAMP}.log"

echo ""
echo "[awc-stress] artifacts:"
echo "  - $JSON_OUT"
echo "  - $LOG_OUT"
echo "  - $OUT_DIR/awc_server_p99_${STAMP}.log"
exit $RC
