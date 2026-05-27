#!/bin/bash
set -eu

# =============================================================================
# reconcile-money.sh
# Runs four reconciliation invariant queries and prints a structured summary.
# Exit codes: 0=all clear, 1=invariant violated, 2=DB error
# =============================================================================

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly ENV_FILE="${PROJECT_ROOT}/.env"

# Colors for output (disabled in non-TTY to keep logs clean)
if [[ -t 1 ]]; then
  readonly RED='\033[0;31m'
  readonly GREEN='\033[0;32m'
  readonly NC='\033[0m'  # No Color
else
  readonly RED=''
  readonly GREEN=''
  readonly NC=''
fi

# =============================================================================
# Helper Functions
# =============================================================================

die() {
  echo "ERROR: $*" >&2
  exit 2
}

log_ok() {
  printf "[OK]   %-40s %s\n" "$1" "$2"
}

log_fail() {
  printf "${RED}[FAIL]${NC} %-40s %s\n" "$1" "$2"
}

# Load and validate .env file
load_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    die ".env file not found at $ENV_FILE"
  fi

  # Source .env, only export MYSQL_ROOT_PASSWORD and MYSQL_USER
  if ! grep -q "MYSQL_ROOT_PASSWORD=" "$ENV_FILE"; then
    die "MYSQL_ROOT_PASSWORD not found in $ENV_FILE"
  fi

  MYSQL_ROOT_PASSWORD=$(grep "^MYSQL_ROOT_PASSWORD=" "$ENV_FILE" | cut -d'=' -f2-)
  MYSQL_USER=$(grep "^MYSQL_USER=" "$ENV_FILE" | cut -d'=' -f2-)

  if [[ -z "$MYSQL_ROOT_PASSWORD" ]]; then
    die "MYSQL_ROOT_PASSWORD is empty in $ENV_FILE"
  fi
}

# Test database connectivity
test_db_connection() {
  if ! docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" sunwinkr-mysql \
       mysql -uroot --silent --skip-column-names -e "SELECT 1;" >/dev/null 2>&1; then
    die "Cannot connect to MySQL container 'sunwinkr-mysql'. Is it running?"
  fi
}

# Run a query and return output; validates exit code
# Exits with 2 on DB error
run_query() {
  local query="$1"
  local rc err_file

  err_file=$(mktemp)
  trap "rm -f '$err_file'" RETURN

  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" sunwinkr-mysql \
    mysql -uroot --silent --skip-column-names --batch -e "$query" 2>"$err_file"
  rc=$?

  if (( rc != 0 )); then
    local err_msg
    err_msg=$(cat "$err_file")
    die "MySQL query failed (exit code $rc): $err_msg"
  fi

  # If we reach here, the query was successful
  return 0
}

# =============================================================================
# Reconciliation Checks
# =============================================================================

check_unbalanced_transactions() {
  local count out_file err_file rc

  out_file=$(mktemp)
  err_file=$(mktemp)
  trap "rm -f '$out_file' '$err_file'" RETURN

  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" sunwinkr-mysql \
    mysql -uroot --silent --skip-column-names --batch \
    -e "SELECT COUNT(*) FROM vinplay.v_money_unbalanced_transactions;" \
    >"$out_file" 2>"$err_file"
  rc=$?

  if (( rc != 0 )); then
    local err_msg
    err_msg=$(cat "$err_file")
    die "MySQL query failed (exit code $rc): $err_msg"
  fi

  count=$(cat "$out_file")

  if [[ ! "$count" =~ ^[0-9]+$ ]]; then
    die "Unexpected count output from query (not a number): '$count'"
  fi

  if [[ "$count" -eq 0 ]]; then
    log_ok "v_money_unbalanced_transactions:" "$count rows"
    return 0
  else
    log_fail "v_money_unbalanced_transactions:" "$count rows; first 3:"
    run_query "
      SELECT CONCAT(
        'transaction_id=', transaction_id,
        ' type=', transaction_type,
        ' ref=', SUBSTRING(external_ref, 1, 16),
        ' net=', net
      ) AS details
      FROM vinplay.v_money_unbalanced_transactions
      LIMIT 3;
    " | while read line; do
      echo "  $line"
    done
    return 1
  fi
}

check_account_drift() {
  local count out_file err_file rc

  out_file=$(mktemp)
  err_file=$(mktemp)
  trap "rm -f '$out_file' '$err_file'" RETURN

  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" sunwinkr-mysql \
    mysql -uroot --silent --skip-column-names --batch \
    -e "SELECT COUNT(*) FROM vinplay.v_money_account_drift;" \
    >"$out_file" 2>"$err_file"
  rc=$?

  if (( rc != 0 )); then
    local err_msg
    err_msg=$(cat "$err_file")
    die "MySQL query failed (exit code $rc): $err_msg"
  fi

  count=$(cat "$out_file")

  if [[ ! "$count" =~ ^[0-9]+$ ]]; then
    die "Unexpected count output from query (not a number): '$count'"
  fi

  if [[ "$count" -eq 0 ]]; then
    log_ok "v_money_account_drift:" "$count rows"
    return 0
  else
    log_fail "v_money_account_drift:" "$count rows; first 3:"
    run_query "
      SELECT CONCAT(
        'account_id=', account_id,
        ' type=', SUBSTRING(account_type, 1, 20),
        ' nick=', COALESCE(SUBSTRING(owner_nickname, 1, 20), 'null'),
        ' balance=', balance,
        ' computed=', computed,
        ' drift=', drift
      ) AS details
      FROM vinplay.v_money_account_drift
      LIMIT 3;
    " | while read line; do
      echo "  $line"
    done
    return 1
  fi
}

check_negative_player() {
  local count out_file err_file rc

  out_file=$(mktemp)
  err_file=$(mktemp)
  trap "rm -f '$out_file' '$err_file'" RETURN

  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" sunwinkr-mysql \
    mysql -uroot --silent --skip-column-names --batch \
    -e "SELECT COUNT(*) FROM vinplay.v_money_negative_player;" \
    >"$out_file" 2>"$err_file"
  rc=$?

  if (( rc != 0 )); then
    local err_msg
    err_msg=$(cat "$err_file")
    die "MySQL query failed (exit code $rc): $err_msg"
  fi

  count=$(cat "$out_file")

  if [[ ! "$count" =~ ^[0-9]+$ ]]; then
    die "Unexpected count output from query (not a number): '$count'"
  fi

  if [[ "$count" -eq 0 ]]; then
    log_ok "v_money_negative_player:" "$count rows"
    return 0
  else
    log_fail "v_money_negative_player:" "$count rows; first 3:"
    run_query "
      SELECT CONCAT(
        'account_id=', account_id,
        ' type=', SUBSTRING(account_type, 1, 20),
        ' nick=', COALESCE(SUBSTRING(owner_nickname, 1, 20), 'null'),
        ' balance=', balance
      ) AS details
      FROM vinplay.v_money_negative_player
      LIMIT 3;
    " | while read line; do
      echo "  $line"
    done
    return 1
  fi
}

check_orphan_reversed() {
  local count out_file err_file rc

  out_file=$(mktemp)
  err_file=$(mktemp)
  trap "rm -f '$out_file' '$err_file'" RETURN

  docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" sunwinkr-mysql \
    mysql -uroot --silent --skip-column-names --batch \
    -e "SELECT COUNT(*) FROM vinplay.v_money_orphan_reversed;" \
    >"$out_file" 2>"$err_file"
  rc=$?

  if (( rc != 0 )); then
    local err_msg
    err_msg=$(cat "$err_file")
    die "MySQL query failed (exit code $rc): $err_msg"
  fi

  count=$(cat "$out_file")

  if [[ ! "$count" =~ ^[0-9]+$ ]]; then
    die "Unexpected count output from query (not a number): '$count'"
  fi

  if [[ "$count" -eq 0 ]]; then
    log_ok "v_money_orphan_reversed:" "$count rows"
    return 0
  else
    log_fail "v_money_orphan_reversed:" "$count rows; first 3:"
    run_query "
      SELECT CONCAT(
        'transaction_id=', transaction_id,
        ' type=', transaction_type,
        ' ref=', SUBSTRING(external_ref, 1, 16),
        ' status=', status
      ) AS details
      FROM vinplay.v_money_orphan_reversed
      LIMIT 3;
    " | while read line; do
      echo "  $line"
    done
    return 1
  fi
}

# =============================================================================
# Main
# =============================================================================

main() {
  # Print timestamp
  local timestamp
  timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  echo "reconcile-money $timestamp"

  # Load environment and test connection
  load_env
  test_db_connection || exit 2

  # Run all four checks
  # Note: if any check calls die(), it will exit with code 2 immediately (due to set -e)
  # We only increment failed_count if a check returns 1 (invariant violation)
  local failed_count=0
  local total_count=4

  check_unbalanced_transactions || ((failed_count++)) || true
  check_account_drift || ((failed_count++)) || true
  check_negative_player || ((failed_count++)) || true
  check_orphan_reversed || ((failed_count++)) || true

  # Print summary
  echo ""
  if [[ $failed_count -eq 0 ]]; then
    echo "summary: $total_count invariants, all clear"
    exit 0
  else
    local plural="s"
    [[ $failed_count -eq 1 ]] && plural=""
    echo "summary: $failed_count invariant${plural} FAILED"
    exit 1
  fi
}

main "$@"
