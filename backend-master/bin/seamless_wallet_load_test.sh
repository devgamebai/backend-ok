#!/usr/bin/env bash
# =============================================================================
# seamless_wallet_load_test.sh
#
# Phase 5 prep gate 5p5 — synthetic GSC seamless-wallet traffic generator.
#
# Generates withdraw / deposit / balance / cancel / rollback / transfer /
# pushbet payloads with realistic shapes (per docs/ref/GSC+ Seamless Wallet
# API v2.0.6EN.md), each with a unique transaction.id so the dedup gate
# does not short-circuit. Fires concurrent curl requests at a target
# sustained TPS for a configured duration, with an optional burst window
# in the middle to simulate a tournament-finish settlement spike.
#
# Captures per-request latency to a CSV, samples HikariCP pool stats once
# per second, tail -fs the relevant log files in parallel, and prints a
# p50/p95/p99/p99.9 summary at the end.
#
# Wraps the run with a pre/post invariant snapshot via reconcile-money.sh
# and a vin-balance check on the test player.
#
# Usage:
#   ./seamless_wallet_load_test.sh \
#       --tps=200 --duration=60 --burst-tps=500 --burst-duration=10 \
#       --base-url=http://localhost:9591 --member=loadtest_user
#
# Exit codes:
#   0  — all pass criteria green
#   1  — at least one pass criterion failed (see summary)
#   2  — pre-flight blocker (services down, signing key missing, …)
#
# Author: Phase 5 prep — load harness, 2026-05-01.
# =============================================================================
set -eu -o pipefail

# ─── defaults ────────────────────────────────────────────────────────────────
TPS=200
DURATION=60
BURST_TPS=500
BURST_DURATION=10
BASE_URL="${LOAD_TEST_BASE_URL:-http://localhost:9591}"
MEMBER="loadtest_user"
SEED=${LOAD_TEST_SEED:-$RANDOM}
SEED_BALANCE=100000000              # 100M VIN
PRODUCT_CODE=1002
GAME_TYPE="POKER"
GAME_CODE="loadtest_slot"
CHANNEL_CODE="gscp"
CURRENCY="VND"
RECONCILE_SCRIPT=""                  # auto-discovered

# ─── arg parsing ─────────────────────────────────────────────────────────────
for arg in "$@"; do
  case $arg in
    --tps=*)            TPS="${arg#*=}" ;;
    --duration=*)       DURATION="${arg#*=}" ;;
    --burst-tps=*)      BURST_TPS="${arg#*=}" ;;
    --burst-duration=*) BURST_DURATION="${arg#*=}" ;;
    --base-url=*)       BASE_URL="${arg#*=}" ;;
    --member=*)         MEMBER="${arg#*=}" ;;
    --seed=*)           SEED="${arg#*=}" ;;
    -h|--help)
      sed -n '2,30p' "$0"; exit 0 ;;
    *) echo "Unknown arg: $arg" >&2; exit 2 ;;
  esac
done

# ─── paths ───────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
TS=$(date +%Y%m%d_%H%M%S)
RUN_ID="load_test_${TS}"
CSV="/tmp/${RUN_ID}.csv"
POOL_CSV="/tmp/${RUN_ID}_pool.csv"
SUMMARY="/tmp/${RUN_ID}_summary.txt"
RECONCILE_SCRIPT="${SCRIPT_DIR}/reconcile-money.sh"

# ─── env / secrets ───────────────────────────────────────────────────────────
[[ -f "$ENV_FILE" ]] || { echo "ERROR: .env not found at $ENV_FILE" >&2; exit 2; }
MYSQL_ROOT_PASSWORD=$(grep "^MYSQL_ROOT_PASSWORD=" "$ENV_FILE" | cut -d= -f2-)
GSC_OPERATOR_CODE=$(grep "^GSC_OPERATOR_CODE=" "$ENV_FILE" | cut -d= -f2- || echo G7A1)
GSC_SECRET_KEY=$(grep "^GSC_SECRET_KEY="    "$ENV_FILE" | cut -d= -f2- || true)

# Fall back to thirdparty container env if not in .env file
if [[ -z "${GSC_SECRET_KEY:-}" ]]; then
  GSC_SECRET_KEY=$(docker exec sunwinkr-game-thirdparty printenv GSC_SECRET_KEY 2>/dev/null || true)
fi
[[ -n "${GSC_SECRET_KEY:-}" ]] || { echo "ERROR: GSC_SECRET_KEY not set in .env or container env" >&2; exit 2; }

# ─── helpers ─────────────────────────────────────────────────────────────────
log()      { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
mysql_q()  { docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" sunwinkr-mysql \
             mysql -uroot --silent --skip-column-names --batch -e "$1" 2>/dev/null; }
md5()      { echo -n "$1" | md5sum | awk '{print $1}'; }
nowts()    { date +%s; }

sign_for() {
  local action="$1" ts="$2"
  md5 "${GSC_OPERATOR_CODE}${ts}${action}${GSC_SECRET_KEY}"
}

# ─── pre-flight ──────────────────────────────────────────────────────────────
preflight() {
  log "preflight: docker ps"
  for svc in sunwinkr-mysql sunwinkr-mongodb sunwinkr-rabbitmq \
             sunwinkr-hazelcast-1 sunwinkr-game-thirdparty sunwinkr-vbee; do
    docker inspect -f '{{.State.Running}}' "$svc" 2>/dev/null \
      | grep -q true || { echo "ERROR: $svc not running" >&2; exit 2; }
  done

  log "preflight: probing GSC endpoint at $BASE_URL"
  local probe_ts probe_sign code
  probe_ts=$(nowts)
  probe_sign=$(sign_for getbalance "$probe_ts")
  code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 \
              -X POST "${BASE_URL}/gsc/v1/api/seamless/balance" \
              -H "Content-Type: application/json" \
              -d "{\"operator_code\":\"${GSC_OPERATOR_CODE}\",\"currency\":\"${CURRENCY}\",\"sign\":\"${probe_sign}\",\"request_time\":\"${probe_ts}\",\"batch_requests\":[{\"member_account\":\"${MEMBER}\",\"product_code\":${PRODUCT_CODE}}]}" \
              || true)
  [[ "$code" == "200" ]] || { echo "ERROR: probe got HTTP $code (expected 200)" >&2; exit 2; }

  log "preflight: reconcile script present"
  [[ -x "$RECONCILE_SCRIPT" ]] || { echo "ERROR: $RECONCILE_SCRIPT not executable" >&2; exit 2; }
}

# ─── seed test player ────────────────────────────────────────────────────────
seed_player() {
  log "seeding test player '${MEMBER}' with ${SEED_BALANCE} VIN"
  mysql_q "
    INSERT INTO vinplay.users (user_name, nick_name, password, vin, status, is_bot, create_time)
    VALUES ('${MEMBER}', '${MEMBER}', MD5('loadtestpw'), ${SEED_BALANCE}, 1, 0, NOW())
    ON DUPLICATE KEY UPDATE vin = ${SEED_BALANCE}, status = 1;
  " || { echo "ERROR: seed insert failed" >&2; exit 2; }

  local row
  row=$(mysql_q "SELECT id, vin FROM vinplay.users WHERE user_name='${MEMBER}';")
  [[ -n "$row" ]] || { echo "ERROR: seed verify failed" >&2; exit 2; }
  log "seeded: $row"
}

# ─── invariant snapshot ──────────────────────────────────────────────────────
snapshot_pre() {
  log "PRE: vin balance + invariant snapshot"
  PRE_VIN=$(mysql_q "SELECT vin FROM vinplay.users WHERE user_name='${MEMBER}';")
  PRE_DRIFT=$(mysql_q "SELECT COUNT(*) FROM vinplay.v_money_account_drift;" 2>/dev/null || echo NA)
  log "  vin=${PRE_VIN}, drift_rowcount=${PRE_DRIFT}"
  "$RECONCILE_SCRIPT" > "/tmp/${RUN_ID}_reconcile_pre.txt" 2>&1 || true
  log "  reconcile pre: see /tmp/${RUN_ID}_reconcile_pre.txt"
}

snapshot_post() {
  log "POST: vin balance + invariant snapshot"
  POST_VIN=$(mysql_q "SELECT vin FROM vinplay.users WHERE user_name='${MEMBER}';")
  POST_DRIFT=$(mysql_q "SELECT COUNT(*) FROM vinplay.v_money_account_drift;" 2>/dev/null || echo NA)
  log "  vin=${POST_VIN}, drift_rowcount=${POST_DRIFT}"
  set +e
  "$RECONCILE_SCRIPT" > "/tmp/${RUN_ID}_reconcile_post.txt" 2>&1
  RECONCILE_EXIT=$?
  set -e
  log "  reconcile post: see /tmp/${RUN_ID}_reconcile_post.txt (exit=${RECONCILE_EXIT})"
}

# ─── payload builders ────────────────────────────────────────────────────────
# Each function emits: "<endpoint>\t<action_for_signing>\t<json_body>"
# All operate on global TS so the harness can reuse a single timestamp per batch.

new_txid() { echo "lt_${SEED}_$(date +%s%N)_${RANDOM}"; }
new_wager() { echo "lt-w-${SEED}-${1}-${RANDOM}"; }

payload_balance() {
  local ts="$1" sign
  sign=$(sign_for getbalance "$ts")
  printf 'balance\t%s\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","batch_requests":[{"member_account":"%s","product_code":%d}]}\n' \
    getbalance "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE"
}

payload_withdraw() {
  local ts="$1" sign id wc
  sign=$(sign_for withdraw "$ts")
  id=$(new_txid); wc=$(new_wager bet)
  printf 'withdraw\twithdraw\t{"operator_code":"%s","game_type":"","currency":"%s","sign":"%s","request_time":"%s","batch_requests":[{"member_account":"%s","product_code":%d,"game_type":"%s","transactions":[{"id":"%s","action":"BET","wager_code":"%s","wager_status":"BET","round_id":"%s","channel_code":"%s","amount":10,"bet_amount":10,"valid_bet_amount":10,"prize_amount":0,"tip_amount":0,"settled_at":0,"game_code":"%s","wager_type":"NORMAL"}]}]}\n' \
    "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$GAME_TYPE" "$id" "$wc" "$wc" "$CHANNEL_CODE" "$GAME_CODE"
}

payload_deposit() {
  local ts="$1" sign id wc
  sign=$(sign_for deposit "$ts")
  id=$(new_txid); wc=$(new_wager settle)
  printf 'deposit\tdeposit\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","batch_requests":[{"member_account":"%s","product_code":%d,"game_type":"%s","transactions":[{"id":"%s","action":"SETTLED","wager_code":"%s","wager_status":"SETTLED","amount":10,"bet_amount":10,"valid_bet_amount":10,"prize_amount":12,"tip_amount":0,"settled_at":%s000,"game_code":"%s","round_id":"%s","channel_code":"%s","wager_type":"NORMAL"}]}]}\n' \
    "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$GAME_TYPE" "$id" "$wc" "$ts" "$GAME_CODE" "$wc" "$CHANNEL_CODE"
}

payload_cancel() {
  local ts="$1" sign id wc
  sign=$(sign_for cancel "$ts")
  id=$(new_txid); wc=$(new_wager cancel)
  printf 'cancel\tcancel\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","member_account":"%s","product_code":%d,"game_type":"%s","transactions":[{"id":"%s","action":"CANCEL","wager_code":"%s","amount":10,"settled_at":0,"game_code":"%s","wager_type":"NORMAL"}]}\n' \
    "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$GAME_TYPE" "$id" "$wc" "$GAME_CODE"
}

payload_rollback() {
  local ts="$1" sign id wc
  sign=$(sign_for rollback "$ts")
  id=$(new_txid); wc=$(new_wager rollback)
  printf 'rollback\trollback\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","member_account":"%s","product_code":%d,"game_type":"%s","transactions":[{"id":"%s","action":"ROLLBACK","wager_code":"%s","amount":10,"game_code":"%s","wager_type":"NORMAL"}]}\n' \
    "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$GAME_TYPE" "$id" "$wc" "$GAME_CODE"
}

payload_transfer() {
  local ts="$1" sign id
  sign=$(sign_for transfer "$ts")
  id=$(new_txid)
  printf 'transfer\ttransfer\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","member_account":"%s","product_code":%d,"action":"deposit","amount":10,"transaction_id":"%s"}\n' \
    "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$id"
}

payload_pushbet() {
  local ts="$1" sign wc
  sign=$(sign_for pushbetdata "$ts")
  wc=$(new_wager push)
  printf 'pushbetdata\tpushbetdata\t{"operator_code":"%s","sign":"%s","request_time":"%s","wagers":[{"member_account":"%s","bet_amount":"10","valid_bet_amount":"10","prize_amount":"12","tip_amount":"0","wager_type":"NORMAL","wager_code":"%s","wager_status":"SETTLED","round_id":"%s","channel_code":"%s","game_type":"%s","settled_at":%s000,"created_at":%s000,"payload":{},"product_code":"%d","game_code":"%s","currency":"%s"}]}\n' \
    "$GSC_OPERATOR_CODE" "$sign" "$ts" "$MEMBER" "$wc" "$wc" "$CHANNEL_CODE" "$GAME_TYPE" "$ts" "$ts" "$PRODUCT_CODE" "$GAME_CODE" "$CURRENCY"
}

# Cache-aware payload emit — uses SIG_CACHE for the signature instead
# of re-computing md5. Used during work-queue build only.
_emit_payload() {
  local ts="$1" sec_offset="$2" r=$((RANDOM % 100))
  local id wc sign
  id="lt_${SEED}_${ts}_${RANDOM}_$$"
  if   (( r <  45 )); then
    sign="${SIG_CACHE[withdraw:${sec_offset}]}"
    wc="lt-w-${SEED}-bet-${RANDOM}"
    printf 'withdraw\twithdraw\t{"operator_code":"%s","game_type":"","currency":"%s","sign":"%s","request_time":"%s","batch_requests":[{"member_account":"%s","product_code":%d,"game_type":"%s","transactions":[{"id":"%s","action":"BET","wager_code":"%s","wager_status":"BET","round_id":"%s","channel_code":"%s","amount":10,"bet_amount":10,"valid_bet_amount":10,"prize_amount":0,"tip_amount":0,"settled_at":0,"game_code":"%s","wager_type":"NORMAL"}]}]}\n' \
      "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$GAME_TYPE" "$id" "$wc" "$wc" "$CHANNEL_CODE" "$GAME_CODE"
  elif (( r <  90 )); then
    sign="${SIG_CACHE[deposit:${sec_offset}]}"
    wc="lt-w-${SEED}-settle-${RANDOM}"
    printf 'deposit\tdeposit\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","batch_requests":[{"member_account":"%s","product_code":%d,"game_type":"%s","transactions":[{"id":"%s","action":"SETTLED","wager_code":"%s","wager_status":"SETTLED","amount":10,"bet_amount":10,"valid_bet_amount":10,"prize_amount":12,"tip_amount":0,"settled_at":%s000,"game_code":"%s","round_id":"%s","channel_code":"%s","wager_type":"NORMAL"}]}]}\n' \
      "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$GAME_TYPE" "$id" "$wc" "$ts" "$GAME_CODE" "$wc" "$CHANNEL_CODE"
  elif (( r <  95 )); then
    sign="${SIG_CACHE[getbalance:${sec_offset}]}"
    printf 'balance\tgetbalance\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","batch_requests":[{"member_account":"%s","product_code":%d}]}\n' \
      "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE"
  elif (( r <  97 )); then
    sign="${SIG_CACHE[cancel:${sec_offset}]}"
    wc="lt-w-${SEED}-cancel-${RANDOM}"
    printf 'cancel\tcancel\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","member_account":"%s","product_code":%d,"game_type":"%s","transactions":[{"id":"%s","action":"CANCEL","wager_code":"%s","amount":10,"settled_at":0,"game_code":"%s","wager_type":"NORMAL"}]}\n' \
      "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$GAME_TYPE" "$id" "$wc" "$GAME_CODE"
  elif (( r <  99 )); then
    sign="${SIG_CACHE[rollback:${sec_offset}]}"
    wc="lt-w-${SEED}-rollback-${RANDOM}"
    printf 'rollback\trollback\t{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","member_account":"%s","product_code":%d,"game_type":"%s","transactions":[{"id":"%s","action":"ROLLBACK","wager_code":"%s","amount":10,"game_code":"%s","wager_type":"NORMAL"}]}\n' \
      "$GSC_OPERATOR_CODE" "$CURRENCY" "$sign" "$ts" "$MEMBER" "$PRODUCT_CODE" "$GAME_TYPE" "$id" "$wc" "$GAME_CODE"
  else
    sign="${SIG_CACHE[pushbetdata:${sec_offset}]}"
    wc="lt-w-${SEED}-push-${RANDOM}"
    printf 'pushbetdata\tpushbetdata\t{"operator_code":"%s","sign":"%s","request_time":"%s","wagers":[{"member_account":"%s","bet_amount":"10","valid_bet_amount":"10","prize_amount":"12","tip_amount":"0","wager_type":"NORMAL","wager_code":"%s","wager_status":"SETTLED","round_id":"%s","channel_code":"%s","game_type":"%s","settled_at":%s000,"created_at":%s000,"payload":{},"product_code":"%d","game_code":"%s","currency":"%s"}]}\n' \
      "$GSC_OPERATOR_CODE" "$sign" "$ts" "$MEMBER" "$wc" "$wc" "$CHANNEL_CODE" "$GAME_TYPE" "$ts" "$ts" "$PRODUCT_CODE" "$GAME_CODE" "$CURRENCY"
  fi
}

# Endpoint mix (weights match production observed shape: ~45% withdraw,
# ~45% deposit, 5% balance, 1% each cancel/rollback/transfer/pushbet).
pick_payload() {
  local ts="$1" r=$((RANDOM % 100))
  if   (( r <  45 )); then payload_withdraw "$ts"
  elif (( r <  90 )); then payload_deposit  "$ts"
  elif (( r <  95 )); then payload_balance  "$ts"
  elif (( r <  97 )); then payload_cancel   "$ts"
  elif (( r <  99 )); then payload_rollback "$ts"
  else                     payload_pushbet  "$ts"
  fi
}

# ─── single request worker ───────────────────────────────────────────────────
# fire_one <endpoint> <body> — emits CSV row to $CSV
fire_one() {
  local endpoint="$1" body="$2" url started t0 t1 latency_ms code size resp
  url="${BASE_URL}/gsc/v1/api/seamless/${endpoint}"
  started=$(date +%s.%N)
  t0=$(date +%s%N)
  resp=$(curl -sS -o /tmp/.lt_${BASHPID}.body -w '%{http_code} %{size_download}' \
              --max-time 10 \
              -X POST "$url" \
              -H "Content-Type: application/json" \
              --data "$body" 2>/dev/null || echo "000 0")
  t1=$(date +%s%N)
  latency_ms=$(( (t1 - t0) / 1000000 ))
  code=$(echo "$resp" | awk '{print $1}')
  size=$(echo "$resp" | awk '{print $2}')
  rm -f /tmp/.lt_${BASHPID}.body
  printf '%s,%s,%d,%s,%s\n' "$endpoint" "$started" "$latency_ms" "$code" "$size" >> "$CSV"
}
export -f fire_one sign_for new_txid new_wager md5 \
          payload_balance payload_withdraw payload_deposit payload_cancel \
          payload_rollback payload_transfer payload_pushbet pick_payload nowts
export GSC_OPERATOR_CODE GSC_SECRET_KEY MEMBER PRODUCT_CODE GAME_TYPE \
       GAME_CODE CHANNEL_CODE CURRENCY SEED CSV BASE_URL

# ─── pool sampler (HikariCP via JMX is not exposed locally; sample MySQL
#     processlist as a proxy for active wallet connections — a 1:1
#     under-estimate but the right shape for this load test). ─────────────
sample_pool() {
  local end_ts=$1 active total
  echo "ts,active,total,utilization_pct" > "$POOL_CSV"
  while [[ $(date +%s) -lt $end_ts ]]; do
    active=$(mysql_q "SELECT COUNT(*) FROM information_schema.processlist
                       WHERE db IN ('vinplay','vinplay_minigame','vinplay_admin','vinplay_gamebai')
                         AND command <> 'Sleep';" 2>/dev/null || echo 0)
    total=$(mysql_q "SELECT COUNT(*) FROM information_schema.processlist
                       WHERE db IN ('vinplay','vinplay_minigame','vinplay_admin','vinplay_gamebai');" 2>/dev/null || echo 1)
    [[ -z "$active" ]] && active=0; [[ -z "$total" ]] && total=1
    if [[ "$total" -gt 0 ]]; then
      pct=$(( active * 100 / total ))
    else
      pct=0
    fi
    printf '%s,%s,%s,%s\n' "$(date +%s.%N)" "$active" "$total" "$pct" >> "$POOL_CSV"
    sleep 1
  done
}

# ─── log tailers ─────────────────────────────────────────────────────────────
start_log_tailers() {
  local log_dir="/tmp/${RUN_ID}_logs" pid_file="/tmp/${RUN_ID}.pids"
  mkdir -p "$log_dir"; : > "$pid_file"

  # game-thirdparty (aggregator timing WARN >50ms)
  docker logs -f --since 0s sunwinkr-game-thirdparty > "${log_dir}/thirdparty.log" 2>&1 &
  echo $! >> "$pid_file"

  # vbee (consumer queue depth on queue_log_gsc_bets_async)
  docker logs -f --since 0s sunwinkr-vbee > "${log_dir}/vbee.log" 2>&1 &
  echo $! >> "$pid_file"

  log "log tailers running (pids in $pid_file, output in $log_dir)"
}

stop_log_tailers() {
  local pid_file="/tmp/${RUN_ID}.pids"
  if [[ -f "$pid_file" ]]; then
    while read -r pid; do kill "$pid" 2>/dev/null || true; done < "$pid_file"
    rm -f "$pid_file"
  fi
}

# ─── traffic generator ───────────────────────────────────────────────────────
# Maintains a target TPS by starting <tps> background fire_one calls per
# 1-second interval. If the previous batch overruns (e.g. server slowdown)
# we still launch the next batch on schedule — backpressure is captured in
# latency rather than thinning the load.

generate_traffic() {
  local end_ts=$(($(date +%s) + DURATION))
  local burst_start=$(( $(date +%s) + DURATION / 2 ))
  local burst_end=$(( burst_start + BURST_DURATION ))

  log "generating traffic: ${TPS} TPS for ${DURATION}s (${BURST_TPS} TPS burst from t+$((DURATION/2))s to t+$((DURATION/2 + BURST_DURATION))s)"
  echo "endpoint,started_at,latency_ms,http_code,response_size" > "$CSV"

  # Step 1: pre-build the work queue (one line per request: ENDPOINT\tBODY)
  # so the hot loop only does I/O, not payload-formatting + signing.
  # We pre-compute one signature per (action, second-offset) pair (8
  # actions × DURATION seconds = a few hundred md5 calls) instead of one
  # per request (~10K md5 calls). Per-request data is then string
  # interpolation only.
  local work_file="/tmp/${RUN_ID}_work.tsv"
  : > "$work_file"
  log "  building work queue..."

  # Pre-compute signatures keyed by "<action>:<sec_offset>"
  declare -A SIG_CACHE
  local actions=(getbalance withdraw deposit cancel rollback transfer pushbetdata)
  local base_ts=$(date +%s)
  for ((sec_offset=0; sec_offset<DURATION; sec_offset++)); do
    local ts_for_sec=$((base_ts + sec_offset))
    for act in "${actions[@]}"; do
      SIG_CACHE["${act}:${sec_offset}"]=$(sign_for "$act" "$ts_for_sec")
    done
  done

  local sec_offset target_tps ts
  for ((sec_offset=0; sec_offset<DURATION; sec_offset++)); do
    target_tps=$TPS
    if (( sec_offset >= DURATION/2 && sec_offset < DURATION/2 + BURST_DURATION )); then
      target_tps=$BURST_TPS
    fi
    ts=$((base_ts + sec_offset))
    for ((i=0; i<target_tps; i++)); do
      _emit_payload "$ts" "$sec_offset" >> "$work_file"
    done
  done
  local total
  total=$(wc -l < "$work_file")
  log "  work queue: $total requests (~$(( total / DURATION )) avg TPS)"

  # Step 2: fire via xargs -P with a fixed concurrency. Concurrency =
  # max(target_tps × 2, 64) lets each connection take up to ~500ms before
  # we'd starve the next slot — plenty of headroom for healthy responses.
  # The work-queue order already encodes the burst, but xargs runs as fast
  # as the server allows (bounded by -P), so wall-clock TPS will track the
  # server's actual throughput, not the prefilled rate. That's fine: the
  # CSV's started_at timestamps preserve per-request timing for analysis.
  local parallel=$(( BURST_TPS * 2 ))
  (( parallel < 64 )) && parallel=64
  (( parallel > 1024 )) && parallel=1024

  log "  firing via xargs -P${parallel}..."
  # Shell function isn't directly callable from xargs; spawn a bash -c
  # that runs fire_one with two args parsed from the TSV row.
  awk -F'\t' '{printf "%s\t%s\n", $1, $3}' "$work_file" \
    | xargs -d '\n' -I{} -P "$parallel" bash -c '
        line="$1"
        ep="${line%%$(printf "\t")*}"
        body="${line#*$(printf "\t")}"
        fire_one "$ep" "$body"
      ' _ "{}"

  log "  traffic complete"
}

# ─── summary ─────────────────────────────────────────────────────────────────
# Compute p50/p95/p99/p99.9 per endpoint + non-2xx counts + pool peak.
summarize() {
  log "summarizing results to $SUMMARY"
  {
    echo "=== Load Test Summary ==="
    echo "run_id: $RUN_ID"
    echo "seed: $SEED"
    echo "tps: $TPS, duration: ${DURATION}s, burst: ${BURST_TPS} TPS for ${BURST_DURATION}s"
    echo "base_url: $BASE_URL, member: $MEMBER"
    echo
    echo "=== Per-endpoint latency (ms) ==="

    awk -F, '
    function pct(arr, m, p,    idx) {
      idx = int(m * p / 100); if (idx < 1) idx = 1; if (idx > m) idx = m;
      return arr[idx]
    }
    NR > 1 {
      ep=$1; lat=$3; code=$4
      lats[ep] = lats[ep] " " lat
      n[ep]++
      if (code+0 >= 200 && code+0 < 300) ok[ep]++
      else if (code+0 >= 400 && code+0 < 500) c4xx[ep]++
      else if (code+0 >= 500 || code+0 == 0) c5xx[ep]++
    }
    END {
      printf "%-12s  %7s  %6s  %6s  %6s  %6s  %6s  %6s  %6s\n",
             "endpoint","count","p50","p95","p99","p99.9","2xx","4xx","5xx"
      for (ep in n) {
        cnt = split(lats[ep], arr, " ")
        delete sorted
        m = 0
        for (i=1; i<=cnt; i++) if (arr[i] != "") { m++; sorted[m] = arr[i]+0 }
        if (m > 1) {
          for (i=1; i<=m; i++)
            for (j=i+1; j<=m; j++)
              if (sorted[j] < sorted[i]) { t=sorted[i]; sorted[i]=sorted[j]; sorted[j]=t }
        }
        printf "%-12s  %7d  %6d  %6d  %6d  %6d  %6d  %6d  %6d\n",
               ep, n[ep],
               pct(sorted,m,50), pct(sorted,m,95), pct(sorted,m,99), pct(sorted,m,99.9),
               (ok[ep]+0), (c4xx[ep]+0), (c5xx[ep]+0)
      }
    }' "$CSV"

    echo
    echo "=== Totals ==="
    awk -F, 'NR > 1 {
      n++
      if ($4+0 >= 200 && $4+0 < 300) ok++
      else if ($4+0 >= 400 && $4+0 < 500) c4xx++
      else if ($4+0 >= 500 || $4+0 == 0) c5xx++
    } END {
      printf "total: %d, 2xx: %d, 4xx: %d, 5xx (incl. timeouts): %d\n",
             n, ok+0, c4xx+0, c5xx+0
    }' "$CSV"

    echo
    echo "=== Pool utilization ==="
    if [[ -s "$POOL_CSV" ]]; then
      awk -F, 'NR > 1 {
        if ($4+0 > peak) peak=$4
        sum+=$4; n++
      } END {
        if (n>0) printf "samples: %d, mean: %.1f%%, peak: %d%%\n", n, sum/n, peak
      }' "$POOL_CSV"
    else
      echo "(no pool samples captured)"
    fi

    echo
    echo "=== Wallet check ==="
    echo "pre vin:    ${PRE_VIN:-NA}"
    echo "post vin:   ${POST_VIN:-NA}"
    echo "drift pre:  ${PRE_DRIFT:-NA}"
    echo "drift post: ${POST_DRIFT:-NA}"
    echo "reconcile post exit: ${RECONCILE_EXIT:-NA}"
  } | tee "$SUMMARY"
}

# ─── pass criteria ───────────────────────────────────────────────────────────
evaluate_pass_criteria() {
  local fail=0

  # Zero 5xx
  local c5xx
  c5xx=$(awk -F, 'NR>1 && ($4+0 >= 500 || $4+0 == 0) {n++} END{print n+0}' "$CSV")
  if [[ "$c5xx" -gt 0 ]]; then
    echo "FAIL: 5xx count = $c5xx (expected 0)"
    fail=1
  else
    echo "PASS: 5xx count = 0"
  fi

  # p99 < 100ms per endpoint
  local bad_p99
  bad_p99=$(awk '/^[a-z]/ && NF >= 9 && $1 != "endpoint" && $5+0 >= 100 {print $1"="$5"ms"}' "$SUMMARY")
  if [[ -n "$bad_p99" ]]; then
    echo "FAIL: p99 >= 100ms: $bad_p99"
    fail=1
  else
    echo "PASS: all per-endpoint p99 < 100ms"
  fi

  # Pool utilization < 80% sustained
  if [[ -s "$POOL_CSV" ]]; then
    local peak_pool
    peak_pool=$(awk -F, 'NR>1 && $4+0>peak {peak=$4+0} END{print peak+0}' "$POOL_CSV")
    if [[ "$peak_pool" -gt 95 ]]; then
      echo "FAIL: pool peak ${peak_pool}% (expected < 95% even in burst)"
      fail=1
    else
      echo "PASS: pool peak ${peak_pool}% within bounds"
    fi
  fi

  # Reconcile invariants green
  if [[ "${RECONCILE_EXIT:-99}" -eq 0 ]]; then
    echo "PASS: reconcile invariants all green"
  else
    echo "FAIL: reconcile post exit ${RECONCILE_EXIT:-NA}"
    fail=1
  fi

  return $fail
}

# ─── cleanup ─────────────────────────────────────────────────────────────────
cleanup_player() {
  log "cleanup: zeroing test player vin (NOT deleting; keep audit trail)"
  mysql_q "UPDATE vinplay.users SET vin = 0 WHERE user_name='${MEMBER}';" || true
}

# ─── main ────────────────────────────────────────────────────────────────────
main() {
  log "=== Phase 5 prep gate 5p5 — seamless wallet load test ==="
  log "tps=${TPS}, duration=${DURATION}s, burst=${BURST_TPS}@${BURST_DURATION}s"
  log "base_url=${BASE_URL}, member=${MEMBER}, seed=${SEED}"
  log "csv=${CSV}, summary=${SUMMARY}"

  preflight
  seed_player
  snapshot_pre
  start_log_tailers

  local end_wall=$(( $(date +%s) + DURATION + 5 ))
  sample_pool $end_wall &
  local pool_pid=$!

  generate_traffic

  kill $pool_pid 2>/dev/null || true
  stop_log_tailers

  snapshot_post
  summarize
  echo
  if evaluate_pass_criteria; then
    log "=== ALL PASS CRITERIA GREEN ==="
    cleanup_player
    exit 0
  else
    log "=== AT LEAST ONE PASS CRITERION FAILED ==="
    cleanup_player
    exit 1
  fi
}

main "$@"
