#!/usr/bin/env bash
# =============================================================================
# SUN-1340 D2 — Long-run multi-round smoke harness
# Simulates player + bot playing TaiXiu, Sicbo, and Lottery (Lô Đề) and
# records every bet/result to JSONL + CSV logs.
#
# Usage:
#   bash tests/long_run/run_long_smoke.sh
#
# Config (env vars, all optional):
#   ROUNDS=10                            # number of rounds per game (default 10)
#   GAMES="taixiu,sicbo,lottery"         # comma-separated list of games
#   IDENTITIES="zuestang,zuestang2"      # comma-separated usernames
#   INTERVAL_SEC=60                      # seconds between rounds per game
#   BET_AMOUNT_MIN=1000                  # minimum bet (VND, multiple of 1000)
#   BET_AMOUNT_MAX=5000                  # maximum bet (VND, multiple of 1000)
#   MAX_EXPOSURE=50000                   # max total bet per identity per run
#   PLAYER_API=https://staging-play.sunkr.bet/api
#   SETTLE_POLL_SEC=15                   # how often the settle-poller checks history
#
# Output:
#   tests/long_run/results_<timestamp>.jsonl  — one JSON line per event
#   tests/long_run/summary_<timestamp>.csv   — one row per bet
#
# Verification:
#   bash -n tests/long_run/run_long_smoke.sh   # syntax check (no-op)
#   ROUNDS=2 INTERVAL_SEC=10 GAMES=sicbo bash tests/long_run/run_long_smoke.sh
# =============================================================================

set -uo pipefail

# ─── Configuration ────────────────────────────────────────────────────────────
PLAYER_API="${PLAYER_API:-https://staging-play.sunkr.bet/api}"
ROUNDS="${ROUNDS:-10}"
GAMES="${GAMES:-taixiu,sicbo,lottery}"
IDENTITIES="${IDENTITIES:-zuestang,zuestang2}"
INTERVAL_SEC="${INTERVAL_SEC:-60}"
BET_AMOUNT_MIN="${BET_AMOUNT_MIN:-1000}"
BET_AMOUNT_MAX="${BET_AMOUNT_MAX:-5000}"
MAX_EXPOSURE="${MAX_EXPOSURE:-50000}"      # per identity per run cap (VND)
SETTLE_POLL_SEC="${SETTLE_POLL_SEC:-15}"

# MD5 passwords (pre-hashed per CLAUDE.md test accounts)
declare -A IDENTITY_MD5
IDENTITY_MD5["zuestang"]="e3486545c690ee99b976888431dda037"
IDENTITY_MD5["zuestang2"]="4581777b67685c53166793900e05f575"
# SUN-1340 — operator's account (login=nguoinaodo, nickname=laviai, pw=123456)
IDENTITY_MD5["nguoinaodo"]="e10adc3949ba59abbe56e057f20f883e"

# ─── Output paths ─────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_TS="$(date +%Y%m%dT%H%M%S)"
JSONL_FILE="${SCRIPT_DIR}/results_${RUN_TS}.jsonl"
CSV_FILE="${SCRIPT_DIR}/summary_${RUN_TS}.csv"
LOCKFILE="${SCRIPT_DIR}/.run_long_smoke.lock"

# ─── Token cache (populated per identity at runtime) ─────────────────────────
declare -A TOKEN_CACHE    # TOKEN_CACHE["username"] = accessToken
declare -A BALANCE_CACHE  # BALANCE_CACHE["username"] = last known VIN balance

# ─── Exposure tracking (per identity) ────────────────────────────────────────
declare -A EXPOSURE      # EXPOSURE["username"] = total_bet_so_far

# ─── Per-identity per-game stats (for final summary) ─────────────────────────
# Keys: "<identity>|<game>" → values tracked in finish_summary()
declare -A STAT_BETS STAT_WIN STAT_LOSS STAT_PENDING

# ─── Pending tickets waiting for settle ──────────────────────────────────────
# Each entry: "<identity>|<game>|<ticket_id>|<bet_amount>"
PENDING_TICKETS=()

# ─── Lottery: track which date has been bet today per identity ────────────────
declare -A LOTTERY_BET_DATE   # LOTTERY_BET_DATE["username"] = "YYYY-MM-DD"

# =============================================================================
# Logging
# =============================================================================

ts() { date '+%H:%M:%S'; }

log_stdout() {
    echo "[$(ts)] $*"
}

log_jsonl() {
    # Append a JSON line to JSONL_FILE under flock to prevent races
    local json="$1"
    (
        flock -x 200
        echo "$json" >> "${JSONL_FILE}"
    ) 200>"${LOCKFILE}"
}

log_csv_row() {
    # Append one row to CSV_FILE under flock
    local row="$1"
    (
        flock -x 200
        echo "$row" >> "${CSV_FILE}"
    ) 200>"${LOCKFILE}"
}

# =============================================================================
# Utility: random bet amount in [MIN, MAX] in steps of 1000
# =============================================================================

random_bet() {
    local min="${BET_AMOUNT_MIN}"
    local max="${BET_AMOUNT_MAX}"
    local step=1000
    local steps=$(( (max - min) / step + 1 ))
    echo $(( (RANDOM % steps) * step + min ))
}

# =============================================================================
# Utility: generate a UUID-like client nonce
# =============================================================================

nonce() {
    # Use /proc/sys/kernel/random/uuid if available, else build from RANDOM
    if [[ -r /proc/sys/kernel/random/uuid ]]; then
        cat /proc/sys/kernel/random/uuid
    else
        printf '%04x%04x-%04x-%04x-%04x-%04x%04x%04x' \
            $RANDOM $RANDOM $RANDOM $RANDOM $RANDOM $RANDOM $RANDOM $RANDOM
    fi
}

# =============================================================================
# Auth: login and cache token per identity
# =============================================================================

ensure_token() {
    local username="$1"
    if [[ -n "${TOKEN_CACHE[$username]+_}" ]] && [[ -n "${TOKEN_CACHE[$username]}" ]]; then
        return 0
    fi
    local md5="${IDENTITY_MD5[$username]:-}"
    if [[ -z "$md5" ]]; then
        log_stdout "ERROR: no MD5 password configured for identity '$username'"
        return 1
    fi
    local resp
    # Note: platform=2 causes 1005 on some staging accounts — omit it
    resp=$(curl -sf --max-time 15 \
        "${PLAYER_API}?c=3&un=${username}&pw=${md5}" 2>/dev/null) || {
        log_stdout "ERROR: login request failed for $username"
        return 1
    }
    local token balance_from_login
    token=$(echo "$resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    # accessToken may be top-level or nested in data
    t = d.get('accessToken') or (d.get('data') or {}).get('accessToken') or ''
    print(t)
except Exception as e:
    print('')
" 2>/dev/null)
    # Extract initial balance from login sessionKey (single base64 blob, not a JWT)
    # Payload: {"userId":N,"nickname":"...","vinTotal":N,"xuTotal":N,...}
    # NOTE: use 'except Exception' not bare 'except' — bare except catches SystemExit
    balance_from_login=$(echo "$resp" | python3 -c "
import sys, json, base64
result = 0
try:
    d = json.load(sys.stdin)
    sk = d.get('sessionKey','')
    if sk:
        pad = sk + '=' * (4 - len(sk) % 4)
        payload = json.loads(base64.b64decode(pad))
        result = int(payload.get('vinTotal', payload.get('vin', 0)))
except Exception:
    result = 0
print(result)
" 2>/dev/null || echo "0")
    if [[ -z "$token" ]]; then
        log_stdout "ERROR: login failed for $username — resp: $(echo "$resp" | head -c 200)"
        return 1
    fi
    TOKEN_CACHE["$username"]="$token"
    BALANCE_CACHE["$username"]="${balance_from_login}"
    log_stdout "LOGIN OK $username token=${token:0:8}... balance=${balance_from_login}"
}

# Refresh token (called on 1001 errorCode)
refresh_token() {
    local username="$1"
    TOKEN_CACHE["$username"]=""
    ensure_token "$username"
}

# Get current balance for an identity.
# Reads from BALANCE_CACHE (populated at login and after each bet response).
# BALANCE_CACHE is NOT accessible in subshells — callers must use this function
# directly (not via $(...) command substitution) and read the result from
# a global variable LAST_BALANCE instead.
#
# Usage:
#   get_balance_into "username"   # sets LAST_BALANCE
#   echo "$LAST_BALANCE"
LAST_BALANCE=0
get_balance_into() {
    local username="$1"
    local cached="${BALANCE_CACHE[$username]:-0}"
    LAST_BALANCE="$cached"
}

# Update balance cache from a bet response's currentMoney field
update_balance_cache() {
    local username="$1"
    local money_str="$2"
    if [[ -n "$money_str" ]] && [[ "$money_str" =~ ^[0-9]+$ ]]; then
        BALANCE_CACHE["$username"]="$money_str"
    fi
}

# =============================================================================
# Exposure guard
# =============================================================================

check_exposure() {
    local username="$1"
    local bet_amount="$2"
    local current="${EXPOSURE[$username]:-0}"
    local new=$(( current + bet_amount ))
    if (( new > MAX_EXPOSURE )); then
        log_stdout "EXPOSURE CAP reached for $username (current=${current}, bet=${bet_amount}, max=${MAX_EXPOSURE}) — skipping bet"
        return 1
    fi
    EXPOSURE["$username"]="$new"
    return 0
}

# =============================================================================
# JSONL helpers
# =============================================================================

# Write a new bet event (outcome fields null/pending)
write_bet_event() {
    local timestamp="$1" game="$2" identity="$3" round_id="$4"
    local bet_side="$5" bet_amount="$6" money_before="$7" ticket_id="$8"
    local ts_str
    ts_str="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    local json
    json=$(python3 -c "
import json, sys
d = {
    'timestamp':            '${ts_str}',
    'game':                 '${game}',
    'identity':             '${identity}',
    'round_id':             '${round_id}',
    'bet_side':             '${bet_side}',
    'bet_amount':           ${bet_amount},
    'current_money_before': ${money_before},
    'ticket_id':            '${ticket_id}',
    'outcome_pending_until': None,
    'final_outcome':        None,
    'win_amount':           None,
    'current_money_after':  None,
}
print(json.dumps(d))
")
    log_jsonl "$json"
    # Also write CSV row
    local csv_row
    csv_row="${ts_str},${game},${identity},${round_id},${bet_side},${bet_amount},${money_before},${ticket_id},pending,,,"
    log_csv_row "$csv_row"
}

# Write a settle event (separate line, references ticket_id)
write_settle_event() {
    local game="$1" identity="$2" ticket_id="$3"
    local final_outcome="$4" win_amount="$5" money_after="$6"
    local ts_str
    ts_str="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    local json
    json=$(python3 -c "
import json
d = {
    'timestamp':            '${ts_str}',
    'event_type':           'settled',
    'game':                 '${game}',
    'identity':             '${identity}',
    'ticket_id':            '${ticket_id}',
    'final_outcome':        '${final_outcome}',
    'win_amount':           ${win_amount},
    'current_money_after':  ${money_after},
}
print(json.dumps(d))
")
    log_jsonl "$json"
    local csv_row
    csv_row="${ts_str},${game},${identity},,,,,,${ticket_id},${final_outcome},${win_amount},${money_after}"
    log_csv_row "$csv_row"
}

# =============================================================================
# Stat tracking
# =============================================================================

record_stat_bet() {
    local key="${1}|${2}"   # identity|game
    STAT_BETS["$key"]=$(( ${STAT_BETS["$key"]:-0} + 1 ))
}

record_stat_settle() {
    local key="${1}|${2}"   # identity|game
    local outcome="$3"
    local win_amount="$4"
    case "$outcome" in
        win)  STAT_WIN["$key"]=$(( ${STAT_WIN["$key"]:-0} + win_amount )) ;;
        loss) STAT_LOSS["$key"]=$(( ${STAT_LOSS["$key"]:-0} + win_amount )) ;;
        *)    STAT_PENDING["$key"]=$(( ${STAT_PENDING["$key"]:-0} + 1 )) ;;
    esac
}

# =============================================================================
# Game: TaiXiu
# POST /api/v2/taixiu/bet body: {moneyType:1, betValue:N, betSide:0|1, clientNonce:"..."}
# betSide: 0=XIU, 1=TAI
# =============================================================================

taixiu_play_round() {
    local identity="$1"
    local round_num="$2"

    local token="${TOKEN_CACHE[$identity]:-}"
    [[ -z "$token" ]] && { log_stdout "taixiu: no token for $identity"; return 1; }

    # Alternate bet side by round number: even=TAI(1), odd=XIU(0)
    local bet_side=$(( round_num % 2 ))
    local bet_side_name="XIU"
    (( bet_side == 1 )) && bet_side_name="TAI"

    local bet_amount
    bet_amount=$(random_bet)

    check_exposure "$identity" "$bet_amount" || return 0

    get_balance_into "$identity"
    local money_before="$LAST_BALANCE"

    # GET state first to capture round_id
    local state_resp
    state_resp=$(curl -sf --max-time 15 \
        "${PLAYER_API%/api}/api/v2/taixiu/state?at=${token}" 2>/dev/null) || state_resp="{}"
    local round_id
    round_id=$(echo "$state_resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('roundId', d.get('phienId', d.get('referenceId', 'unknown'))))
except:
    print('unknown')
" 2>/dev/null || echo "unknown")

    local client_nonce
    client_nonce=$(nonce)

    local body
    body=$(python3 -c "
import json
print(json.dumps({'moneyType':1,'betValue':${bet_amount},'betSide':${bet_side},'clientNonce':'${client_nonce}'}))
")

    log_stdout "TaiXiu round=${round_num} ${identity} bet=${bet_side_name} ${bet_amount} round_id=${round_id}..."

    local resp
    # Use -s (not -sf) — 4xx responses from the engine contain valid JSON error bodies
    resp=$(curl -s --max-time 15 \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$body" \
        "${PLAYER_API%/api}/api/v2/taixiu/bet?at=${token}" 2>/dev/null)
    if [[ -z "$resp" ]]; then
        log_stdout "TaiXiu bet CURL_ERROR (empty response) for $identity"
        EXPOSURE["$identity"]=$(( ${EXPOSURE[$identity]:-0} - bet_amount ))
        return 1
    fi

    local success errorCode ticket_id money_after
    success=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('success',False)).lower())" 2>/dev/null || echo "false")
    errorCode=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('errorCode','?'))" 2>/dev/null || echo "?")
    ticket_id=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('perBetTxId',''))" 2>/dev/null || echo "")
    money_after=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('currentMoney','0'))" 2>/dev/null || echo "0")

    # Handle token expiry
    if [[ "$errorCode" == "1001" ]]; then
        log_stdout "TaiXiu: token expired for $identity — refreshing..."
        refresh_token "$identity" && taixiu_play_round "$identity" "$round_num"
        return
    fi

    if [[ "$success" == "true" ]]; then
        update_balance_cache "$identity" "$money_after"
        log_stdout "TaiXiu round=${round_num} ${identity} bet=${bet_side_name} ${bet_amount} → ticket=${ticket_id} pending… balance_after=${money_after}"
        write_bet_event "$(ts)" "taixiu" "$identity" "$round_id" "$bet_side_name" "$bet_amount" "$money_before" "$ticket_id"
        PENDING_TICKETS+=("${identity}|taixiu|${ticket_id}|${bet_amount}")
        record_stat_bet "$identity" "taixiu"
    else
        log_stdout "TaiXiu bet FAILED for $identity: errorCode=${errorCode} resp=$(echo "$resp" | head -c 200)"
        # Record attempt even on failure (outcome=rejected) so JSONL is never empty
        update_balance_cache "$identity" "$money_after"
        write_bet_event "$(ts)" "taixiu" "$identity" "$round_id" "REJECTED:${bet_side_name}:${errorCode}" "$bet_amount" "$money_before" "NONE"
        EXPOSURE["$identity"]=$(( ${EXPOSURE[$identity]:-0} - bet_amount ))  # refund exposure on failure
    fi
}

# =============================================================================
# Game: Sicbo
# POST /api/v2/sicbo/bet body: {moneyType:1, betValue:N, betSide:"TAI"|"XIU"|"POINT_N", clientNonce:"..."}
# betSide is STRING per SicboBetRequestDto
# =============================================================================

# Sicbo bet side rotation — 4 simple sides to keep bets diverse but readable
SICBO_SIDES=("TAI" "XIU" "POINT_9" "POINT_12")

sicbo_play_round() {
    local identity="$1"
    local round_num="$2"

    local token="${TOKEN_CACHE[$identity]:-}"
    [[ -z "$token" ]] && { log_stdout "sicbo: no token for $identity"; return 1; }

    # Rotate through sides
    local side_idx=$(( round_num % ${#SICBO_SIDES[@]} ))
    local bet_side="${SICBO_SIDES[$side_idx]}"

    local bet_amount
    bet_amount=$(random_bet)

    check_exposure "$identity" "$bet_amount" || return 0

    get_balance_into "$identity"
    local money_before="$LAST_BALANCE"

    # GET state first to capture round_id
    local state_resp
    state_resp=$(curl -sf --max-time 15 \
        "${PLAYER_API%/api}/api/v2/sicbo/state?at=${token}" 2>/dev/null) || state_resp="{}"
    local round_id
    round_id=$(echo "$state_resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('roundId', d.get('referenceId', 'unknown')))
except:
    print('unknown')
" 2>/dev/null || echo "unknown")

    local client_nonce
    client_nonce=$(nonce)

    local body
    body=$(python3 -c "
import json
print(json.dumps({'moneyType':1,'betValue':${bet_amount},'betSide':'${bet_side}','clientNonce':'${client_nonce}'}))
")

    log_stdout "Sicbo round=${round_num} ${identity} bet=${bet_side} ${bet_amount} round_id=${round_id}..."

    local resp
    # Use -s (not -sf) — 4xx responses from the engine contain valid JSON error bodies
    resp=$(curl -s --max-time 15 \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$body" \
        "${PLAYER_API%/api}/api/v2/sicbo/bet?at=${token}" 2>/dev/null)
    if [[ -z "$resp" ]]; then
        log_stdout "Sicbo bet CURL_ERROR (empty response) for $identity"
        EXPOSURE["$identity"]=$(( ${EXPOSURE[$identity]:-0} - bet_amount ))
        return 1
    fi

    local success errorCode ticket_id money_after
    success=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('success',False)).lower())" 2>/dev/null || echo "false")
    errorCode=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('errorCode','?'))" 2>/dev/null || echo "?")
    ticket_id=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('perBetTxId',''))" 2>/dev/null || echo "")
    money_after=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('currentMoney','0'))" 2>/dev/null || echo "0")

    if [[ "$errorCode" == "1001" ]]; then
        log_stdout "Sicbo: token expired for $identity — refreshing..."
        refresh_token "$identity" && sicbo_play_round "$identity" "$round_num"
        return
    fi

    if [[ "$success" == "true" ]]; then
        update_balance_cache "$identity" "$money_after"
        log_stdout "Sicbo round=${round_num} ${identity} bet=${bet_side} ${bet_amount} → ticket=${ticket_id} pending… balance_after=${money_after}"
        write_bet_event "$(ts)" "sicbo" "$identity" "$round_id" "$bet_side" "$bet_amount" "$money_before" "$ticket_id"
        PENDING_TICKETS+=("${identity}|sicbo|${ticket_id}|${bet_amount}")
        record_stat_bet "$identity" "sicbo"
    else
        log_stdout "Sicbo bet FAILED for $identity: errorCode=${errorCode} resp=$(echo "$resp" | head -c 200)"
        # Record attempt even on failure so JSONL is never empty (outcome=rejected)
        update_balance_cache "$identity" "$money_after"
        write_bet_event "$(ts)" "sicbo" "$identity" "$round_id" "REJECTED:${bet_side}:${errorCode}" "$bet_amount" "$money_before" "NONE"
        EXPOSURE["$identity"]=$(( ${EXPOSURE[$identity]:-0} - bet_amount ))
    fi
}

# =============================================================================
# Game: Lottery (Lô Đề / XSMB)
# POST /api/v2/lottery/xsmb/bet body: {modeId:1..4, ticket:"NN", betValue:N, clientNonce:"..."}
# Only fires ONCE per calendar day per identity (skip if already bet today).
# Settle happens at ~18:36 VN — poller will fill in outcome later.
# =============================================================================

# Lottery — modes ↔ ticket format (per docs/LOTTERY_LODE.md + docs/ref/LodeRatio):
#   1 Lô 2 số    → 2-digit (NN), rate 22 (1 betValue ⇒ 22 vin cost)
#   2 Lô 3 số    → 3-digit (NNN), rate 23
#   6 Đề         → 2-digit (NN), rate 1
#   7 3 Càng     → 3-digit (NNN), rate 1
# Other modes (Xiên 2/3/4 = 3..5, Đuôi-* = 8..11) need multi-ticket arrays
# the legacy BetRequestDto doesn't accept yet — restrict to mode 1 here.
LOTTERY_MODES=(1)
# Per-mode rate (CSV col "Tiền cược mới") — used to log actual_cost = betValue × rate.
declare -A LOTTERY_RATE
LOTTERY_RATE[1]=22
LOTTERY_RATE[2]=23
LOTTERY_RATE[6]=1
LOTTERY_RATE[7]=1

lottery_play_round() {
    local identity="$1"
    local round_num="$2"

    local token="${TOKEN_CACHE[$identity]:-}"
    [[ -z "$token" ]] && { log_stdout "lottery: no token for $identity"; return 1; }

    # Check VN date (server date via state, or local date)
    local vn_today
    vn_today=$(TZ=Asia/Ho_Chi_Minh date +%Y-%m-%d)

    # Skip if already bet today for this identity
    if [[ "${LOTTERY_BET_DATE[$identity]:-}" == "$vn_today" ]]; then
        log_stdout "Lottery: $identity already bet today (${vn_today}) — skipping"
        return 0
    fi

    # Check lottery state to see if betting is still open
    local state_resp
    state_resp=$(curl -sf --max-time 15 \
        "${PLAYER_API%/api}/api/v2/lottery/xsmb/state?at=${token}" 2>/dev/null) || state_resp="{}"
    local betting_open round_id
    betting_open=$(echo "$state_resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print('true' if d.get('bettingOpen', False) else 'false')
except:
    print('false')
" 2>/dev/null || echo "false")
    round_id=$(echo "$state_resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('roundId', d.get('vnDate', 'unknown')))
except:
    print('unknown')
" 2>/dev/null || echo "unknown")

    if [[ "$betting_open" != "true" ]]; then
        log_stdout "Lottery: betting window CLOSED for today (${vn_today}) — settle pending until 18:36 VN"
        write_bet_event "$(ts)" "lottery" "$identity" "$round_id" "NO_BET_WINDOW_CLOSED" "0" "0" "NO_TICKET"
        return 0
    fi

    # Rotate mode by identity+round
    local identity_hash=$(( $(echo -n "$identity" | cksum | cut -d' ' -f1) % 4 ))
    local mode_idx=$(( (round_num + identity_hash) % ${#LOTTERY_MODES[@]} ))
    local mode_id="${LOTTERY_MODES[$mode_idx]}"

    # Random 2-digit ticket number (01-99)
    local ticket
    ticket=$(printf "%02d" $(( RANDOM % 99 + 1 )))

    local bet_amount
    bet_amount=$(random_bet)

    check_exposure "$identity" "$bet_amount" || return 0

    get_balance_into "$identity"
    local money_before="$LAST_BALANCE"

    local client_nonce
    client_nonce=$(nonce)

    local body
    body=$(python3 -c "
import json
print(json.dumps({'modeId':${mode_id},'ticket':'${ticket}','betValue':${bet_amount},'clientNonce':'${client_nonce}'}))
")

    log_stdout "Lottery round=${round_num} ${identity} mode=${mode_id} ticket=${ticket} bet=${bet_amount} round_id=${round_id}..."

    local resp
    # Use -s (not -sf) — 4xx responses from the engine contain valid JSON error bodies
    resp=$(curl -s --max-time 15 \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$body" \
        "${PLAYER_API%/api}/api/v2/lottery/xsmb/bet?at=${token}" 2>/dev/null)
    if [[ -z "$resp" ]]; then
        log_stdout "Lottery bet CURL_ERROR (empty response) for $identity"
        EXPOSURE["$identity"]=$(( ${EXPOSURE[$identity]:-0} - bet_amount ))
        return 1
    fi

    local success errorCode ticket_id money_after
    success=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('success',False)).lower())" 2>/dev/null || echo "false")
    errorCode=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('errorCode','?'))" 2>/dev/null || echo "?")
    ticket_id=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('ticketId') or '')" 2>/dev/null || echo "")
    money_after=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('currentMoney','0'))" 2>/dev/null || echo "0")

    if [[ "$errorCode" == "1001" ]]; then
        log_stdout "Lottery: token expired for $identity — refreshing..."
        refresh_token "$identity" && lottery_play_round "$identity" "$round_num"
        return
    fi

    # 0002 = BET_WINDOW_CLOSED (race condition: window closed between state+bet)
    if [[ "$errorCode" == "0002" ]]; then
        log_stdout "Lottery: bet window closed (0002) for $identity — settle pending"
        write_bet_event "$(ts)" "lottery" "$identity" "$round_id" "NO_BET_WINDOW_CLOSED" "0" "0" "NO_TICKET"
        EXPOSURE["$identity"]=$(( ${EXPOSURE[$identity]:-0} - bet_amount ))
        return 0
    fi

    if [[ "$success" == "true" ]]; then
        update_balance_cache "$identity" "$money_after"
        local rate="${LOTTERY_RATE[$mode_id]:-1}"
        local actual_cost=$(( money_before - money_after ))
        local expected_cost=$(( bet_amount * rate ))
        log_stdout "Lottery round=${round_num} ${identity} mode=${mode_id} ticket=${ticket} bet_unit=${bet_amount} rate=${rate}× actual_cost=${actual_cost} (expected=${expected_cost}) → ticketId=${ticket_id} settle_pending_until=18:36_VN"
        write_bet_event "$(ts)" "lottery" "$identity" "$round_id" "mode${mode_id}_${ticket}_x${rate}" "$actual_cost" "$money_before" "${ticket_id}"
        PENDING_TICKETS+=("${identity}|lottery|${ticket_id}|${actual_cost}")
        LOTTERY_BET_DATE["$identity"]="$vn_today"
        record_stat_bet "$identity" "lottery"
        # Refund the EXPOSURE delta — we counted bet_amount above but real cost is actual_cost.
        EXPOSURE["$identity"]=$(( ${EXPOSURE[$identity]:-0} - bet_amount + actual_cost ))
    else
        log_stdout "Lottery bet FAILED for $identity: errorCode=${errorCode} resp=$(echo "$resp" | head -c 200)"
        EXPOSURE["$identity"]=$(( ${EXPOSURE[$identity]:-0} - bet_amount ))
    fi
}

# =============================================================================
# Settle poller — reads c=303 history and resolves pending tickets
# c=303 params: at, nn (nickname), game (taixiu|sicbo|lode|all), p (page), l (limit)
# =============================================================================

poll_pending_settles() {
    local identity="$1"
    local game_filter="${2:-all}"

    local token="${TOKEN_CACHE[$identity]:-}"
    [[ -z "$token" ]] && return 0

    # Fetch history for this identity
    local resp
    resp=$(curl -sf --max-time 20 \
        "${PLAYER_API}?c=303&nn=${identity}&at=${token}&game=${game_filter}&p=1&l=100" \
        2>/dev/null) || return 0

    # Extract settled plays: list of {game, ticket_id (or perBetTxId), prize, bet}
    local settled_json
    settled_json=$(echo "$resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    plays = d.get('plays', [])
    # Flatten nested structures
    out = []
    for p in plays:
        out.append({
            'game':      p.get('game', ''),
            'ticket_id': str(p.get('id', p.get('ticket_id', p.get('perBetTxId', '')))),
            'prize':     int(p.get('prize', 0) or 0),
            'bet':       int(p.get('bet', 0) or 0),
        })
    print(json.dumps(out))
except Exception as e:
    print('[]')
" 2>/dev/null || echo "[]")

    # Walk pending tickets and check if any match
    local new_pending=()
    local count="${#PENDING_TICKETS[@]}"
    local i
    for (( i=0; i<count; i++ )); do
        local entry="${PENDING_TICKETS[$i]}"
        [[ -z "$entry" ]] && continue
        IFS='|' read -r p_identity p_game p_ticket p_bet_amount <<< "$entry"
        [[ "$p_identity" != "$identity" ]] && { new_pending+=("$entry"); continue; }

        # Check if this ticket appears in settled history
        local found
        found=$(echo "$settled_json" | python3 -c "
import sys, json
rows = json.load(sys.stdin)
for r in rows:
    if str(r.get('ticket_id','')) == '${p_ticket}':
        print(r.get('prize', 0))
        break
else:
    print('__not_found__')
" 2>/dev/null || echo "__not_found__")

        if [[ "$found" == "__not_found__" ]]; then
            new_pending+=("$entry")
        else
            local prize="${found:-0}"
            local outcome="loss"
            local win_amount=0
            if (( prize > 0 )); then
                outcome="win"
                win_amount="$prize"
            fi
            get_balance_into "$identity"
            local money_after="$LAST_BALANCE"
            log_stdout "${p_game^^} ticket=${p_ticket} SETTLED ${identity} ${outcome} prize=${prize} balance=${money_after}"
            write_settle_event "$p_game" "$identity" "$p_ticket" "$outcome" "$win_amount" "$money_after"
            record_stat_settle "$identity" "$p_game" "$outcome" "$win_amount"
        fi
    done
    PENDING_TICKETS=("${new_pending[@]+"${new_pending[@]}"}")
}

# Lottery-specific settle: use the XSMB history endpoint not c=303
poll_lottery_settles() {
    local identity="$1"
    local token="${TOKEN_CACHE[$identity]:-}"
    [[ -z "$token" ]] && return 0

    local resp
    resp=$(curl -sf --max-time 20 \
        "${PLAYER_API%/api}/api/v2/lottery/xsmb/history?at=${token}&limit=50&offset=0" \
        2>/dev/null) || return 0

    local settled_json
    settled_json=$(echo "$resp" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    entries = d.get('entries', d.get('data', []))
    out = []
    for e in entries:
        out.append({
            'ticket_id': str(e.get('id', e.get('ticketId', ''))),
            'prize':     int(e.get('prize', e.get('winAmount', 0)) or 0),
            'settled':   bool(e.get('settled', e.get('settledAt'))),
        })
    print(json.dumps(out))
except:
    print('[]')
" 2>/dev/null || echo "[]")

    local new_pending=()
    local count="${#PENDING_TICKETS[@]}"
    local i
    for (( i=0; i<count; i++ )); do
        local entry="${PENDING_TICKETS[$i]}"
        [[ -z "$entry" ]] && continue
        IFS='|' read -r p_identity p_game p_ticket p_bet_amount <<< "$entry"
        if [[ "$p_identity" != "$identity" ]] || [[ "$p_game" != "lottery" ]]; then
            new_pending+=("$entry")
            continue
        fi

        local found
        found=$(echo "$settled_json" | python3 -c "
import sys, json
rows = json.load(sys.stdin)
for r in rows:
    if str(r.get('ticket_id','')) == '${p_ticket}' and r.get('settled'):
        print(r.get('prize', 0))
        break
else:
    print('__not_found__')
" 2>/dev/null || echo "__not_found__")

        if [[ "$found" == "__not_found__" ]]; then
            new_pending+=("$entry")
            log_stdout "Lottery ticket=${p_ticket} ${identity}: settle_pending (XSMB draw not yet settled)"
        else
            local prize="${found:-0}"
            local outcome="loss"
            local win_amount=0
            if (( prize > 0 )); then outcome="win"; win_amount="$prize"; fi
            get_balance_into "$identity"
            local money_after="$LAST_BALANCE"
            log_stdout "Lottery ticket=${p_ticket} SETTLED ${identity} ${outcome} prize=${prize} balance=${money_after}"
            write_settle_event "lottery" "$identity" "$p_ticket" "$outcome" "$win_amount" "$money_after"
            record_stat_settle "$identity" "lottery" "$outcome" "$win_amount"
        fi
    done
    PENDING_TICKETS=("${new_pending[@]+"${new_pending[@]}"}")
}

# =============================================================================
# Final summary
# =============================================================================

print_final_summary() {
    echo ""
    echo "============================================================"
    echo " LONG-RUN SMOKE SUMMARY  —  $(date)"
    echo "============================================================"
    printf "%-20s %-10s %-8s %-12s %-12s %-12s\n" "Identity" "Game" "Bets" "TotalWin" "TotalLoss" "NetPnL"
    echo "------------------------------------------------------------"
    for identity in $(echo "$IDENTITIES" | tr ',' ' '); do
        for game in $(echo "$GAMES" | tr ',' ' '); do
            local key="${identity}|${game}"
            local bets="${STAT_BETS[$key]:-0}"
            local win="${STAT_WIN[$key]:-0}"
            local loss="${STAT_LOSS[$key]:-0}"
            local pending="${STAT_PENDING[$key]:-0}"
            local net=$(( win - loss ))
            printf "%-20s %-10s %-8s %-12s %-12s %-12s\n" \
                "$identity" "$game" "$bets" "$win" "$loss" "$net"
        done
    done
    echo "------------------------------------------------------------"
    echo " Exposure:"
    for identity in $(echo "$IDENTITIES" | tr ',' ' '); do
        echo "   ${identity}: ${EXPOSURE[$identity]:-0} VND bet total (cap: ${MAX_EXPOSURE})"
    done
    local pending_count="${#PENDING_TICKETS[@]}"
    if (( pending_count > 0 )); then
        echo ""
        echo " Settle-pending tickets (lottery or mid-round): ${pending_count}"
        local pi
        for (( pi=0; pi<pending_count; pi++ )); do
            local pt="${PENDING_TICKETS[$pi]}"
            [[ -n "$pt" ]] && echo "   $pt"
        done
    fi
    echo "============================================================"
    echo " JSONL: ${JSONL_FILE}"
    echo " CSV:   ${CSV_FILE}"
    echo "============================================================"
}

# =============================================================================
# Main loop
# =============================================================================

main() {
    # Write CSV header
    echo "timestamp,game,identity,round_id,bet_side,bet_amount,money_before,ticket_id,settle_ticket_id,final_outcome,win_amount,money_after" \
        > "${CSV_FILE}"

    log_stdout "=== SUN-1340 D2 Long-run smoke harness starting ==="
    log_stdout "Games: ${GAMES} | Identities: ${IDENTITIES} | Rounds: ${ROUNDS}"
    log_stdout "BetRange: ${BET_AMOUNT_MIN}-${BET_AMOUNT_MAX} VND | Interval: ${INTERVAL_SEC}s | MaxExposure: ${MAX_EXPOSURE} VND/identity"
    log_stdout "JSONL → ${JSONL_FILE}"
    log_stdout "CSV  → ${CSV_FILE}"

    # Initialize exposure counters
    for identity in $(echo "$IDENTITIES" | tr ',' ' '); do
        EXPOSURE["$identity"]=0
        # Pre-login all identities
        ensure_token "$identity" || {
            log_stdout "WARN: Could not login $identity — will skip their bets"
        }
    done

    # Parse games list
    IFS=',' read -ra GAME_LIST <<< "$GAMES"
    IFS=',' read -ra IDENTITY_LIST <<< "$IDENTITIES"

    local round=1
    while (( round <= ROUNDS )); do
        log_stdout "--- Round ${round}/${ROUNDS} ---"

        for game in "${GAME_LIST[@]}"; do
            game="$(echo "$game" | tr -d '[:space:]')"
            for identity in "${IDENTITY_LIST[@]}"; do
                identity="$(echo "$identity" | tr -d '[:space:]')"
                [[ -z "${TOKEN_CACHE[$identity]+_}" ]] && continue
                [[ -z "${TOKEN_CACHE[$identity]}" ]]   && continue

                case "$game" in
                    taixiu)  taixiu_play_round  "$identity" "$round" ;;
                    sicbo)   sicbo_play_round   "$identity" "$round" ;;
                    lottery) lottery_play_round "$identity" "$round" ;;
                    *)
                        log_stdout "Unknown game '${game}' — skipping"
                        ;;
                esac
            done
        done

        # After each round: poll for settled bets
        log_stdout "Polling settle status for ${#PENDING_TICKETS[@]} pending tickets..."
        for identity in "${IDENTITY_LIST[@]}"; do
            identity="$(echo "$identity" | tr -d '[:space:]')"
            [[ -z "${TOKEN_CACHE[$identity]:-}" ]] && continue
            poll_pending_settles "$identity" "all"
            # Lottery settle uses its own endpoint
            poll_lottery_settles "$identity"
        done

        round=$(( round + 1 ))

        # Sleep between rounds (skip after last round)
        if (( round <= ROUNDS )); then
            log_stdout "Sleeping ${INTERVAL_SEC}s before next round..."
            sleep "${INTERVAL_SEC}"
        fi
    done

    # Final settle poll after all rounds complete
    log_stdout "Final settle poll..."
    for identity in "${IDENTITY_LIST[@]}"; do
        identity="$(echo "$identity" | tr -d '[:space:]')"
        [[ -z "${TOKEN_CACHE[$identity]:-}" ]] && continue
        poll_pending_settles "$identity" "all"
        poll_lottery_settles "$identity"
    done

    # Emit "settle pending" notice for any tickets still outstanding
    local final_count="${#PENDING_TICKETS[@]}"
    local fi
    for (( fi=0; fi<final_count; fi++ )); do
        local entry="${PENDING_TICKETS[$fi]}"
        [[ -z "$entry" ]] && continue
        IFS='|' read -r p_identity p_game p_ticket p_bet <<< "$entry"
        log_stdout "${p_game^^} ticket=${p_ticket} ${p_identity}: SETTLE PENDING (will resolve after draw/round closes)"
        record_stat_settle "$p_identity" "$p_game" "pending" "0"
    done

    print_final_summary
}

# Cleanup on exit
cleanup() {
    rm -f "${LOCKFILE}"
}
trap cleanup EXIT

main "$@"
