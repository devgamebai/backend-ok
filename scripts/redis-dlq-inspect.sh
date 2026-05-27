#!/usr/bin/env bash
# =============================================================================
# Redis Streams DLQ inventory — Task O2
# =============================================================================
# Lists every `{queue}.dlq` stream that has at least one entry, alongside its
# length and last entry id. Read-only; never modifies state.
#
# Usage:
#   ./scripts/redis-dlq-inspect.sh
#
# Companion to `redis-replay-dlq.sh`. The intended workflow when the
# `RedisStreamDLQNonEmpty` alert fires is:
#
#   1. Run this script to confirm which queue tripped.
#   2. Inspect bodies with: ./scripts/redis-replay-dlq.sh <queue>     # dry-run
#   3. Replay survivors with: ./scripts/redis-replay-dlq.sh <queue> --apply
#
# Auth & target
# -------------
# Reads REDIS_PASSWORD from .env (or the env var). Connects to
# redis://redis:6379/1 — DB 1 is where Redis Streams traffic lives per Plan
# task P3. If the host can reach the redis container directly, that's used;
# otherwise the script falls back to running redis-cli inside
# sunwinkr-redis via `docker exec`.
# =============================================================================

set -euo pipefail

# ---- locate .env ------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/.env}"

if [[ -z "${REDIS_PASSWORD:-}" ]] && [[ -f "${ENV_FILE}" ]]; then
    # shellcheck disable=SC1090
    source <(grep -E '^REDIS_PASSWORD=' "${ENV_FILE}")
fi

if [[ -z "${REDIS_PASSWORD:-}" ]]; then
    echo "ERROR: REDIS_PASSWORD not set (env or ${ENV_FILE})" >&2
    exit 2
fi

REDIS_HOST="${REDIS_HOST:-redis}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_DB="${REDIS_DB:-1}"
REDIS_CONTAINER="${REDIS_CONTAINER:-sunwinkr-redis}"

# ---- pick redis-cli transport (host vs docker exec) -------------------------
# Prefer host redis-cli if it can reach REDIS_HOST. Otherwise shell into
# sunwinkr-redis. This means the script works both from the repo root on a
# dev box AND from inside the container if ops shells in.
rcli() {
    if command -v redis-cli >/dev/null 2>&1 && \
       redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" \
                 --no-auth-warning -n "${REDIS_DB}" PING >/dev/null 2>&1; then
        redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" \
                  --no-auth-warning -n "${REDIS_DB}" "$@"
    elif docker inspect "${REDIS_CONTAINER}" >/dev/null 2>&1; then
        docker exec -e REDISCLI_AUTH="${REDIS_PASSWORD}" "${REDIS_CONTAINER}" \
            redis-cli --no-auth-warning -n "${REDIS_DB}" "$@"
    else
        echo "ERROR: cannot reach Redis (no host redis-cli + no ${REDIS_CONTAINER} docker container)" >&2
        exit 3
    fi
}

# ---- main: SCAN for *.dlq keys, report length ------------------------------
# SCAN with a glob over `{*}.dlq`. The brace pair is the cluster hashtag
# wrapping the queue name (see StreamNames.java); the literal '{' and '}' are
# part of the key, not glob metacharacters.
printf '%-50s %10s  %s\n' "DLQ" "LENGTH" "LAST_ID"
printf '%-50s %10s  %s\n' "---" "------" "-------"

# rcli SCAN streams keys one-per-line. Filter to those of TYPE stream and
# glob-match `*.dlq`. Use SCAN (not KEYS) to avoid blocking Redis on a hot
# instance — even though we only have ~50 streams in steady state.
cursor=0
found_any=0
while :; do
    # SCAN returns a multi-line response: cursor on line 1, then keys.
    # Output goes through `tr -d '\r'` because docker exec preserves CRLFs.
    out="$(rcli SCAN "${cursor}" MATCH '{*}.dlq' COUNT 200 TYPE stream | tr -d '\r')"
    cursor="$(printf '%s\n' "${out}" | head -n1)"
    keys="$(printf '%s\n' "${out}" | tail -n +2)"

    if [[ -n "${keys}" ]]; then
        while IFS= read -r key; do
            [[ -z "${key}" ]] && continue
            len="$(rcli XLEN "${key}" | tr -d '\r' || echo 0)"
            if [[ -n "${len}" ]] && [[ "${len}" != "0" ]]; then
                last="$(rcli XREVRANGE "${key}" + - COUNT 1 | tr -d '\r' | head -n1)"
                printf '%-50s %10s  %s\n' "${key}" "${len}" "${last:--}"
                found_any=1
            fi
        done <<< "${keys}"
    fi

    [[ "${cursor}" == "0" ]] && break
done

if [[ "${found_any}" == "0" ]]; then
    echo "(no non-empty DLQs found)"
fi
