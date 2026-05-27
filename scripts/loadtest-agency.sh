#!/bin/bash
# =============================================================================
# Load test driver — agency-portal end-to-end validation
#
# Goal: create N test players under SpecialAccount, top them up, simulate GSC
# bet rounds, then verify counts in agency portal (LS Cược + LS Rolling).
# Cleanup wipes everything tagged with the lt-prefix nickname pattern.
#
# Phases (each runs independently or chain via `all`):
#   register   — POST /api?c=1 with smoke-test bypass header, N times
#   topup      — credit each user TOPUP_AMOUNT VIN, write money_gateway_log row
#   bet        — for each user, BETS_PER_USER rounds of GSC withdraw + deposit
#   verify     — count rows across users / log_gsc_bets / rebate_logs
#   cleanup    — DELETE everything WHERE nick_name LIKE 'lt%'
#   all        — register → topup → bet → verify (no cleanup)
#
# Usage:
#   N_USERS=300 BETS_PER_USER=7 ./loadtest-agency.sh all
#   ./loadtest-agency.sh cleanup
# =============================================================================

set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# Config
# ─────────────────────────────────────────────────────────────────────────────
N_USERS="${N_USERS:-300}"
BETS_PER_USER="${BETS_PER_USER:-7}"
TOPUP_AMOUNT="${TOPUP_AMOUNT:-1000000}"
BET_AMOUNT="${BET_AMOUNT:-10000}"
WIN_RATE="${WIN_RATE:-45}"           # % of rounds the player wins (rest lose)
PARENT_AGENT_RC="${PARENT_AGENT_RC:-1}"  # rc=1 = CompanyAgent, level-1 child of SpecialAccount
NICK_PREFIX="${NICK_PREFIX:-lt}"

ENV_FILE="/root/sunwinkr/sunwinkr-backend/.env"
SMOKE_KEY="$(grep '^SMOKE_TEST_BYPASS_KEY=' "$ENV_FILE" | cut -d= -f2-)"
GSC_OP_CODE="$(grep '^GSC_OPERATOR_CODE=' "$ENV_FILE" | cut -d= -f2-)"
GSC_SECRET="$(grep '^GSC_SECRET_KEY=' "$ENV_FILE" | cut -d= -f2-)"
GSC_CURRENCY="$(grep '^GSC_CURRENCY=' "$ENV_FILE" | cut -d= -f2-)"
GSC_CURRENCY="${GSC_CURRENCY:-KRW}"
MYSQL_PWD="$(grep '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"

# Active GSC tables we'll spread bets across
GAME_TABLE_1="oytmvb9m1zysmc44"   # Evo Baccarat A
GAME_TABLE_2="60i0lcfx5wkkv3sy"   # Evo Baccarat B
GAME_TABLE_3="LightningBac0001"   # Evo Lightning Baccarat
GAME_TABLE_4="leqhceumaq6qfoug"   # Evo Speed Baccarat A (active=1 after our SUN-1240 fix)
GAMES=("$GAME_TABLE_1" "$GAME_TABLE_2" "$GAME_TABLE_3" "$GAME_TABLE_4")

# State files
STATE_DIR="/tmp/loadtest-agency"
mkdir -p "$STATE_DIR"
NICK_LIST="$STATE_DIR/nicknames.txt"
WAGER_LOG="$STATE_DIR/wagers.log"

# Test password (md5: e10adc3949ba59abbe56e057f20f883e for "123456")
TEST_PWD_MD5="e10adc3949ba59abbe56e057f20f883e"

# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────
log()    { printf '\e[36m[loadtest %s]\e[0m %s\n' "$(date +%H:%M:%S)" "$*"; }
ok()     { printf '\e[32m[ok]\e[0m %s\n' "$*"; }
warn()   { printf '\e[33m[warn]\e[0m %s\n' "$*"; }
die()    { printf '\e[31m[err]\e[0m %s\n' "$*" >&2; exit 1; }

mysql_q() {
    # Suppress only the password warning — keep real errors visible.
    docker exec sunwinkr-mysql mysql -N -uroot -p"$MYSQL_PWD" vinplay -e "$1" 2> >(grep -v '\[Warning\] Using a password' >&2)
}

# Run curl from inside the docker network so internal DNS + RFC 1918 IPs work.
internal_curl() {
    docker exec sunwinkr-game-thirdparty curl -sS --max-time 10 "$@"
}

md5_hex() {
    printf '%s' "$1" | md5sum | cut -d' ' -f1
}

gen_nicks() {
    local count="$1"
    : > "$NICK_LIST"
    local ts=$(date +%s)
    for ((i=0; i<count; i++)); do
        # 16-char limit, alphanumeric only — pattern lt + 6 ts digits + 5 idx digits
        printf '%s%06d%05d\n' "$NICK_PREFIX" "$((ts % 1000000))" "$i" >> "$NICK_LIST"
    done
}

# ─────────────────────────────────────────────────────────────────────────────
# Phase 1: register
# ─────────────────────────────────────────────────────────────────────────────
phase_register() {
    log "phase 1: register $N_USERS users with rc=$PARENT_AGENT_RC"
    [[ -z "$SMOKE_KEY" ]] && die "SMOKE_TEST_BYPASS_KEY not set in .env"
    gen_nicks "$N_USERS"

    local ok_count=0 fail_count=0
    while read -r nick; do
        local resp
        resp=$(internal_curl -X POST "http://portal-api:8081/api?c=1" \
            -H "X-Smoke-Test-Key: $SMOKE_KEY" \
            --data-urlencode "un=$nick" \
            --data-urlencode "pw=$TEST_PWD_MD5" \
            --data-urlencode "rc=$PARENT_AGENT_RC" 2>&1) || { fail_count=$((fail_count+1)); continue; }
        if echo "$resp" | grep -q '"success":true'; then
            ok_count=$((ok_count+1))
        else
            fail_count=$((fail_count+1))
            warn "register $nick -> $resp"
        fi
    done < "$NICK_LIST"

    ok "registered $ok_count / $N_USERS (failed $fail_count)"
}

# ─────────────────────────────────────────────────────────────────────────────
# Phase 2: topup — proper money-ledger entry per user
# ─────────────────────────────────────────────────────────────────────────────
phase_topup() {
    log "phase 2: top up $TOPUP_AMOUNT VIN to each ${NICK_PREFIX}% user"

    # Build a single multi-row transaction: UPDATE balance + INSERT audit row.
    # Mirrors what MoneyGateway.creditUser does atomically. tx_id includes
    # the user_name so re-runs of this script are idempotent (UNIQUE on
    # money_gateway_log.tx_id+source guards double-credit).
    #
    # Note: users.nick_name is NULL on register-time (only user_name is
    # populated). We match by user_name and ALSO populate nick_name to
    # equal user_name so downstream code that joins on nick_name works.
    local sql="START TRANSACTION;
UPDATE users SET nick_name = user_name
 WHERE user_name LIKE '${NICK_PREFIX}%' AND nick_name IS NULL;
UPDATE users
   SET vin = vin + $TOPUP_AMOUNT
 WHERE user_name LIKE '${NICK_PREFIX}%';
INSERT INTO money_gateway_log (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at)
SELECT id, user_name, $TOPUP_AMOUNT, 'vin', 'ADMIN_TOPUP', CONCAT('loadtest_topup_', user_name), 'load test top-up', vin, NOW()
  FROM users
 WHERE user_name LIKE '${NICK_PREFIX}%';
COMMIT;
SELECT COUNT(*) FROM users WHERE user_name LIKE '${NICK_PREFIX}%' AND vin >= $TOPUP_AMOUNT;"

    local count
    count=$(mysql_q "$sql" | tail -1)
    ok "topped up $count users (each +$TOPUP_AMOUNT VIN)"
}

# ─────────────────────────────────────────────────────────────────────────────
# Phase 3: bets — simulate GSC seamless callbacks (BET + SETTLE)
# ─────────────────────────────────────────────────────────────────────────────
gsc_call() {
    local verb="$1" ; shift
    local body="$1"
    local url="http://game-thirdparty:9591/gsc/v1/api/seamless/$verb"
    internal_curl -X POST "$url" -H "Content-Type: application/json" --data-binary "$body" || true
}

bet_round() {
    local nick="$1" idx="$2"
    local game="${GAMES[$((idx % ${#GAMES[@]}))]}"
    local rt=$(date +%s)
    local sign_w sign_d
    sign_w=$(md5_hex "${GSC_OP_CODE}${rt}withdraw${GSC_SECRET}")
    # Wager code must be unique across (user × bet); use full nick + idx.
    # Nick is at most 13 chars (lt + 6 ts + 5 idx), idx at most 6 chars → ≤19.
    local wager="${nick}b${idx}"
    local txn_bet="ltbet_${nick}_${idx}"
    local txn_set="ltset_${nick}_${idx}"

    # BET (withdraw)
    local body_w
    body_w=$(printf '{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","game_type":"LIVE","batch_requests":[{"member_account":"%s","product_code":1002,"game_type":"LIVE","transactions":[{"id":"%s","action":"BET","wager_code":"%s","wager_status":"BET","amount":%s,"bet_amount":%s,"valid_bet_amount":%s,"prize_amount":0,"game_code":"%s","round_id":"%s"}]}]}' \
        "$GSC_OP_CODE" "$GSC_CURRENCY" "$sign_w" "$rt" "$nick" "$txn_bet" "$wager" \
        "$BET_AMOUNT" "$BET_AMOUNT" "$BET_AMOUNT" "$game" "${wager}r")
    gsc_call "withdraw" "$body_w" >/dev/null

    # Decide win/lose
    local prize=0
    if [[ $((RANDOM % 100)) -lt $WIN_RATE ]]; then
        prize=$((BET_AMOUNT * 2))
    fi

    # SETTLE (deposit) — same wager_code, distinct txn_id
    sleep 0.05  # tiny realism gap
    rt=$(date +%s)
    sign_d=$(md5_hex "${GSC_OP_CODE}${rt}deposit${GSC_SECRET}")
    local body_d
    body_d=$(printf '{"operator_code":"%s","currency":"%s","sign":"%s","request_time":"%s","game_type":"LIVE","batch_requests":[{"member_account":"%s","product_code":1002,"game_type":"LIVE","transactions":[{"id":"%s","action":"SETTLED","wager_code":"%s","wager_status":"SETTLED","amount":%s,"bet_amount":%s,"valid_bet_amount":%s,"prize_amount":%s,"settled_at":%s000,"game_code":"%s","round_id":"%s"}]}]}' \
        "$GSC_OP_CODE" "$GSC_CURRENCY" "$sign_d" "$rt" "$nick" "$txn_set" "$wager" \
        "$prize" "$BET_AMOUNT" "$BET_AMOUNT" "$prize" "$rt" "$game" "${wager}r")
    gsc_call "deposit" "$body_d" >/dev/null

    echo "$nick,$wager,$BET_AMOUNT,$prize,$game" >> "$WAGER_LOG"
}

phase_bet() {
    [[ -s "$NICK_LIST" ]] || die "no nicknames — run register first"
    log "phase 3: $BETS_PER_USER bets per user × $(wc -l <"$NICK_LIST") users"
    : > "$WAGER_LOG"
    local count=0
    while read -r nick; do
        for ((i=0; i<BETS_PER_USER; i++)); do
            bet_round "$nick" "$i"
            count=$((count+1))
        done
    done < "$NICK_LIST"
    ok "fired $count rounds (bet + settle each), wagers logged at $WAGER_LOG"
}

# ─────────────────────────────────────────────────────────────────────────────
# Phase 4: verify
# ─────────────────────────────────────────────────────────────────────────────
phase_verify() {
    log "phase 4: verify counts"
    echo
    echo "── users ($NICK_PREFIX% prefix) ──"
    mysql_q "SELECT COUNT(*) AS users, SUM(vin) AS total_vin
             FROM users WHERE user_name LIKE '${NICK_PREFIX}%';"
    echo
    echo "── log_gsc_bets (Mongo) ──"
    docker exec sunwinkr-mongodb mongosh --quiet \
        -u sunwinkr_admin -p "$(grep ^MONGO_PASSWORD "$ENV_FILE" | cut -d= -f2)" \
        --authenticationDatabase admin win123club --eval "
        const m = db.log_gsc_bets.aggregate([
          {\$match:{nick_name:{\$regex:'^${NICK_PREFIX}'}}},
          {\$group:{_id:null,count:{\$sum:1},settled:{\$sum:{\$cond:['\$settled',1,0]}},
                    total_bet:{\$sum:{\$toLong:'\$bet_value'}},
                    total_prize:{\$sum:{\$toLong:'\$prize'}}}}
        ]).toArray();
        print(JSON.stringify(m[0]||{}));" 2>/dev/null | tail -2
    echo
    echo "── rebate_logs (per agent) ──"
    mysql_q "SELECT agent_user_id, agent_nickname, COUNT(*) AS rebate_rows,
                    COUNT(DISTINCT source_key) AS distinct_wagers,
                    ROUND(SUM(rebate_amount),2) AS total_commission
             FROM rebate_logs
             WHERE player_nickname LIKE '${NICK_PREFIX}%'
             GROUP BY agent_user_id, agent_nickname
             ORDER BY agent_user_id;"
    echo
    echo "── money_gateway_log (audit trail of test moves) ──"
    mysql_q "SELECT source, COUNT(*) AS events, ROUND(SUM(amount),2) AS net
             FROM money_gateway_log
             WHERE nick_name LIKE '${NICK_PREFIX}%' OR tx_id LIKE 'loadtest_%' OR tx_id LIKE 'lt%'
             GROUP BY source;"
}

# ─────────────────────────────────────────────────────────────────────────────
# Phase 5: cleanup — wipe everything tagged with the test prefix
# ─────────────────────────────────────────────────────────────────────────────
phase_cleanup() {
    log "phase 5: cleanup ALL ${NICK_PREFIX}% data"
    read -r -p "DELETE all ${NICK_PREFIX}% test data on PRODUCTION? type 'yes': " ans
    [[ "$ans" == "yes" ]] || { warn "aborted"; return 0; }

    # Mongo first (no FK guard there)
    docker exec sunwinkr-mongodb mongosh --quiet \
        -u sunwinkr_admin -p "$(grep ^MONGO_PASSWORD "$ENV_FILE" | cut -d= -f2)" \
        --authenticationDatabase admin win123club --eval "
        const r1 = db.log_gsc_bets.deleteMany({nick_name:{\$regex:'^${NICK_PREFIX}'}});
        const r2 = db.log_money_user_vin.deleteMany({nick_name:{\$regex:'^${NICK_PREFIX}'}});
        print('mongo deleted log_gsc_bets='+r1.deletedCount+' log_money_user_vin='+r2.deletedCount);" 2>/dev/null | tail -2

    # MySQL — order matters: child rows that reference the user, then user.
    # Reverse agency_wallet credits that came from test bets (matched by description).
    mysql_q "
        START TRANSACTION;
        DELETE FROM rebate_logs       WHERE player_nickname LIKE '${NICK_PREFIX}%';
        DELETE FROM money_gateway_log WHERE nick_name LIKE '${NICK_PREFIX}%' OR tx_id LIKE 'loadtest_%' OR tx_id LIKE 'lt%';
        DELETE FROM gsc_event_log     WHERE member_account LIKE '${NICK_PREFIX}%';
        DELETE FROM gsc_bets          WHERE user_name LIKE '${NICK_PREFIX}%' OR nick_name LIKE '${NICK_PREFIX}%';
        UPDATE agency_wallet aw
          JOIN (
            SELECT agent_id,
                   SUM(CASE WHEN direction='CREDIT' THEN amount ELSE -amount END) AS net
              FROM agency_wallet_transactions
             WHERE related_user LIKE '${NICK_PREFIX}%'
             GROUP BY agent_id
          ) t ON t.agent_id = aw.agent_id
           SET aw.balance = aw.balance - t.net;
        DELETE FROM agency_wallet_transactions
          WHERE related_user LIKE '${NICK_PREFIX}%';
        DELETE FROM users             WHERE user_name LIKE '${NICK_PREFIX}%';
        COMMIT;
        SELECT 'users left'           AS what, COUNT(*) FROM users           WHERE user_name LIKE '${NICK_PREFIX}%'
        UNION SELECT 'rebate_logs',           COUNT(*) FROM rebate_logs       WHERE player_nickname LIKE '${NICK_PREFIX}%'
        UNION SELECT 'money_gateway_log',     COUNT(*) FROM money_gateway_log WHERE nick_name LIKE '${NICK_PREFIX}%' OR tx_id LIKE 'lt%'
        UNION SELECT 'gsc_event_log',         COUNT(*) FROM gsc_event_log     WHERE member_account LIKE '${NICK_PREFIX}%'
        UNION SELECT 'gsc_bets',              COUNT(*) FROM gsc_bets          WHERE nick_name LIKE '${NICK_PREFIX}%';"
    rm -f "$NICK_LIST" "$WAGER_LOG"
    ok "cleanup done"
}

# ─────────────────────────────────────────────────────────────────────────────
# Dispatch
# ─────────────────────────────────────────────────────────────────────────────
case "${1:-help}" in
    register) phase_register ;;
    topup)    phase_topup ;;
    bet)      phase_bet ;;
    verify)   phase_verify ;;
    cleanup)  phase_cleanup ;;
    all)      phase_register; phase_topup; phase_bet; phase_verify ;;
    *)        cat <<EOF
Usage: $0 {register|topup|bet|verify|cleanup|all}

Env knobs:
  N_USERS=$N_USERS
  BETS_PER_USER=$BETS_PER_USER
  TOPUP_AMOUNT=$TOPUP_AMOUNT
  BET_AMOUNT=$BET_AMOUNT
  WIN_RATE=$WIN_RATE          # % rounds player wins
  PARENT_AGENT_RC=$PARENT_AGENT_RC   # rc=1 = CompanyAgent (under SpecialAccount)
  NICK_PREFIX=$NICK_PREFIX
EOF
        ;;
esac
