#!/usr/bin/env bash
# =============================================================================
# Pre-soak parity check — Task S0
# =============================================================================
# Read-only sanity audit before flipping any queue from `rmq` → `dual` for
# the dual-write soak (S1). This script automates the parts of S0 that can
# be checked from a single host; the rest (running the 6 acceptance tests
# end-to-end) is operator-manual and described in the migration plan.
#
# What this checks
# ----------------
# A. Local container reachability to redis:6379 from each producer container
#    (vbee, backend-api, portal-api, game-thirdparty, game-Minigame). This
#    is the SUN-1228-class regression vector — a producer container without
#    network access to the redis service is invisible until you flip its
#    queue and start losing publishes.
#
# B. .env key parity vs a peer host's .env (default: staging vs production).
#    Compares VARIABLE NAMES only — never values, so secrets stay local.
#    Detects "key in one but not the other" drift, which is the SUN-1228
#    signature.
#
# C. message_bus_audit MySQL table existence. If S1's schema migration has
#    been applied, the table is there and audit rows are accumulating;
#    otherwise reports MISSING (expected pre-S1, but you want to flag this
#    before announcing S1 is ready).
#
# D. Redis stream-key namespace check. Iterates the queue names declared in
#    backend-master/api/vbee/config/rabbitmq_config.xml and confirms the
#    matching `{queue}.stream` key on Redis DB 1 either (a) does not exist
#    yet (clean slate, expected before first publish) or (b) is a stream
#    type. Catches stale `string` keys left over from accidental `SET`.
#
# E. Prometheus alert-rule load. Optional — only runs if --prom=URL is set.
#    Confirms all 7 alerts from infra/alerting/redis-streams-alerts.yml
#    are loaded in the running Prometheus instance.
#
# Usage
# -----
#   ./scripts/pre-soak-parity-check.sh                              # checks A,C,D
#   ./scripts/pre-soak-parity-check.sh --peer-env=/tmp/prod.env     # +B
#   ./scripts/pre-soak-parity-check.sh --prom=http://prom:9090      # +E
#   ./scripts/pre-soak-parity-check.sh --peer-env=… --prom=… --strict
#
# Args:
#   --peer-env=<path>       Path to a peer host's .env (operator scp's it
#                           to a local temp file before running). Compares
#                           key sets in both directions.
#   --prom=<url>            Prometheus base URL (no trailing /). Calls
#                           /api/v1/rules to confirm alert rules are
#                           loaded.
#   --strict                Exit non-zero on any WARN. Default exits non-
#                           zero only on FAIL.
#
# Exit codes
# ----------
#   0  all checks PASS (or only WARNs without --strict)
#   1  one or more FAIL
#   2  bad args / preflight (docker not reachable, etc.)
# =============================================================================

set -euo pipefail

PEER_ENV=""
PROM_URL=""
STRICT=0
for arg in "$@"; do
    case "${arg}" in
        --peer-env=*) PEER_ENV="${arg#--peer-env=}" ;;
        --prom=*)     PROM_URL="${arg#--prom=}" ;;
        --strict)     STRICT=1 ;;
        --help|-h)
            sed -n '2,55p' "$0"
            exit 0
            ;;
        *)
            echo "unknown flag: ${arg}" >&2
            exit 2
            ;;
    esac
done

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
RMQ_XML="${REPO_ROOT}/backend-master/api/vbee/config/rabbitmq_config.xml"

REDIS_CONTAINER="${REDIS_CONTAINER:-sunwinkr-redis}"
REDIS_DB="${REDIS_DB:-1}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-sunwinkr-mysql}"

PRODUCER_CONTAINERS=(
    sunwinkr-vbee
    sunwinkr-backend-api
    sunwinkr-portal-api
    sunwinkr-game-thirdparty
    sunwinkr-game-minigame
)

# 7 alerts owned by O2.
EXPECTED_ALERTS=(
    RedisStreamDLQNonEmpty
    RedisStreamConsumerLag
    RedisStreamPendingStuck
    RedisContainerRestart
    RedisMemoryPressure
    RedisAofRewriteSlow
    MessageBusAuditDropping
)

# .env-side keys: subset that must agree across staging/prod for the
# migration to behave the same.
RELEVANT_ENV_KEYS=(
    REDIS_PASSWORD
    REDIS_STREAMS_HOST
    REDIS_STREAMS_PORT
    REDIS_STREAMS_PASSWORD
    REDIS_STREAMS_DB
    MESSAGE_BUS_DEFAULT
    RABBITMQ_USER
    RABBITMQ_PASSWORD
)

# ---- output helpers ---------------------------------------------------------
fail_count=0
warn_count=0
pass() { printf '  [PASS] %s\n' "$*"; }
warn() { printf '  [WARN] %s\n' "$*" >&2; warn_count=$((warn_count + 1)); }
fail() { printf '  [FAIL] %s\n' "$*" >&2; fail_count=$((fail_count + 1)); }
hdr()  { printf '\n=== %s ===\n' "$*"; }

# ---- preflight --------------------------------------------------------------
if ! command -v docker >/dev/null; then
    echo "docker not found in PATH" >&2; exit 2
fi
if [[ ! -f "${ENV_FILE}" ]]; then
    echo "missing ${ENV_FILE}" >&2; exit 2
fi
REDIS_PASSWORD="$(grep -E '^REDIS_PASSWORD=' "${ENV_FILE}" | head -1 | cut -d= -f2-)"

REDIS_CLI() {
    if [[ -n "${REDIS_PASSWORD}" ]]; then
        docker exec "${REDIS_CONTAINER}" \
            redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning -n "${REDIS_DB}" "$@"
    else
        docker exec "${REDIS_CONTAINER}" redis-cli -n "${REDIS_DB}" "$@"
    fi
}

# ---- Section A: producer-container redis reachability ----------------------
hdr "A. Producer-container reachability to redis:6379"
for c in "${PRODUCER_CONTAINERS[@]}"; do
    if ! docker ps --format '{{.Names}}' | grep -qx "${c}"; then
        warn "${c}: container not running (skipping)"
        continue
    fi
    # Try a few primitives. /dev/tcp is bash-only — invoke bash explicitly
    # since `sh` (dash) on Debian-derived images does NOT honor it.
    if docker exec "${c}" sh -c '
        if command -v nc >/dev/null 2>&1; then
            nc -z -w 2 redis 6379 && exit 0
        fi
        if command -v bash >/dev/null 2>&1; then
            timeout 2 bash -c "echo > /dev/tcp/redis/6379" 2>/dev/null && exit 0
        fi
        exit 1
    ' >/dev/null 2>&1; then
        pass "${c}: redis:6379 reachable"
    else
        fail "${c}: redis:6379 NOT reachable"
    fi
done

# ---- Section B: .env key parity --------------------------------------------
hdr "B. .env key parity vs peer host"
if [[ -z "${PEER_ENV}" ]]; then
    warn "no --peer-env=<path> provided; skipping cross-host diff"
elif [[ ! -f "${PEER_ENV}" ]]; then
    fail "--peer-env=${PEER_ENV} not readable"
else
    local_keys=$(grep -vE '^\s*(#|$)' "${ENV_FILE}" | cut -d= -f1 | sort -u)
    peer_keys=$(grep -vE '^\s*(#|$)' "${PEER_ENV}" | cut -d= -f1 | sort -u)
    only_local=$(comm -23 <(echo "${local_keys}") <(echo "${peer_keys}"))
    only_peer=$(comm -13  <(echo "${local_keys}") <(echo "${peer_keys}"))
    if [[ -z "${only_local}" && -z "${only_peer}" ]]; then
        pass "key sets match"
    else
        if [[ -n "${only_local}" ]]; then
            fail "keys present locally but missing in peer:"
            echo "${only_local}" | sed 's/^/        /'
        fi
        if [[ -n "${only_peer}" ]]; then
            fail "keys present in peer but missing locally:"
            echo "${only_peer}" | sed 's/^/        /'
        fi
    fi
    # Sanity-check the migration-relevant subset is present on both sides.
    for k in "${RELEVANT_ENV_KEYS[@]}"; do
        if ! grep -qE "^${k}=" "${ENV_FILE}"; then
            fail "${k} missing from local .env"
        elif ! grep -qE "^${k}=" "${PEER_ENV}"; then
            fail "${k} missing from peer .env"
        fi
    done
fi

# ---- Section C: message_bus_audit table existence --------------------------
hdr "C. message_bus_audit table"
MYSQL_PASSWORD="$(grep -E '^MYSQL_ROOT_PASSWORD=' "${ENV_FILE}" | head -1 | cut -d= -f2-)"
if [[ -z "${MYSQL_PASSWORD}" ]]; then
    warn "MYSQL_ROOT_PASSWORD not in .env; skipping audit-table probe"
else
    # MySQL CLI prints a stderr warning about password-on-cmdline; drop it
    # so it doesn't contaminate the count. -N -B = no headers, batch mode.
    out=$(docker exec "${MYSQL_CONTAINER}" mysql -uroot -p"${MYSQL_PASSWORD}" -N -B \
        -e "SELECT COUNT(*) FROM information_schema.tables \
            WHERE table_schema='vinplay' AND table_name='message_bus_audit'" 2>/dev/null \
        || echo ERR)
    if [[ "${out}" == "1" ]]; then
        pass "vinplay.message_bus_audit exists"
    elif [[ "${out}" == "0" ]]; then
        warn "vinplay.message_bus_audit does NOT exist (expected pre-S1; flag once S1 schema migration runs)"
    else
        fail "audit-table probe failed: ${out}"
    fi
fi

# ---- Section D: redis stream-key namespace ---------------------------------
hdr "D. Redis stream-key namespace"
if [[ ! -f "${RMQ_XML}" ]]; then
    warn "${RMQ_XML} not found; skipping queue inventory"
else
    queues=$(grep -oE '<name>queue_[a-z_]+</name>' "${RMQ_XML}" \
        | sed -E 's,</?name>,,g' | sort -u)
    if [[ -z "${queues}" ]]; then
        warn "no <queue><name> entries found in ${RMQ_XML}"
    else
        bad=0
        n=0
        while IFS= read -r q; do
            [[ -z "${q}" ]] && continue
            n=$((n + 1))
            kind=$(REDIS_CLI TYPE "{${q}}.stream" 2>&1 | tr -d '\r')
            case "${kind}" in
                none|stream) ;;  # OK
                *) fail "{${q}}.stream is type=${kind} (expected stream or none)"
                   bad=$((bad + 1)) ;;
            esac
        done <<< "${queues}"
        if [[ "${bad}" == "0" ]]; then
            pass "${n} queues; all {queue}.stream keys are absent or type=stream"
        fi
    fi
fi

# ---- Section E: prometheus alert-rule load ----------------------------------
hdr "E. Prometheus alert-rule load"
if [[ -z "${PROM_URL}" ]]; then
    warn "no --prom=<url> provided; skipping alert-rule probe"
elif ! command -v curl >/dev/null; then
    warn "curl not in PATH; skipping alert-rule probe"
else
    rules_json=$(curl -fsS "${PROM_URL%/}/api/v1/rules" 2>&1 || echo '{}')
    missing=()
    for a in "${EXPECTED_ALERTS[@]}"; do
        if ! grep -q "\"${a}\"" <<< "${rules_json}"; then
            missing+=("${a}")
        fi
    done
    if [[ "${#missing[@]}" == "0" ]]; then
        pass "all 7 redis-streams alerts loaded"
    else
        fail "missing alert rules: ${missing[*]}"
    fi
fi

# ---- summary ---------------------------------------------------------------
hdr "Summary"
echo "  fails=${fail_count}   warns=${warn_count}"
if [[ "${fail_count}" -gt 0 ]]; then
    echo "  S0 NOT MET — fix FAILs before flipping any queue to dual." >&2
    exit 1
fi
if [[ "${STRICT}" == "1" && "${warn_count}" -gt 0 ]]; then
    echo "  --strict and ${warn_count} WARN(s) — exit 1." >&2
    exit 1
fi
echo "  S0 met (or warns acceptable). Proceed to S1 dual-write soak."
exit 0
