#!/usr/bin/env bash
#
# CI safety check: forbid reintroduction of the SUN-748 / SUN-753 class of bugs.
#
# HISTORY
# --------
# `UserModel.getCurrentMoney(String)` and `UserModel.setCurrentMoney(String, long)`
# were renamed to `getTotalPnl(String)` / `setTotalPnl(String, long)` on
# 2026-04-10 because the old names misled developers into treating the
# cumulative P&L counter (vin_total/xu_total) as the current wallet balance.
# For losing players (vin_total < 0) this caused the wallet UI to flip to
# negative immediately after a bet, a money transfer, or a VIN→XU conversion.
#
# We ship the actual balance via `user.getMoney(type)` / `user.getVin()` /
# `user.getXu()`. The cumulative counter lives under `getTotalPnl(type)`
# and should only be used for logging / stat messages that legitimately
# track lifetime P&L.
#
# WHAT THIS SCRIPT DOES
# ---------------------
# Fails the build if any .java file in the repo matches one of these patterns:
#
#   1. A NEW definition of the banned methods, e.g.
#        public long getCurrentMoney(String moneyType) { ... }
#        public void setCurrentMoney(String moneyType, long money) { ... }
#      We want the trap method NAME to stay dead.
#
#   2. A caller that invokes a two-arg trap method:
#        user.getCurrentMoney("vin")
#        user.getCurrentMoney(moneyType)
#        user.setCurrentMoney("vin", value)
#        user.setCurrentMoney(moneyType, value)
#      Even if someone adds a new local class with the old name, we want
#      the grep to catch it before review.
#
# WHAT THIS SCRIPT DOES NOT DO
# ----------------------------
# It does NOT complain about the single-arg wire-level setters on response
# DTOs (e.g. `MoneyResponse.setCurrentMoney(long)`, `NapXuResponse.setCurrentMoneyVin(long)`)
# because those are the legitimate wire-field setters and are runtime-guarded
# by BalanceGuard.clamp(). The patterns below are intentionally specific to
# the two-arg and "String-arg" trap variants.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

FAIL=0

# 1. Ban re-introduction of the trap method definitions.
#    Look for the public method signatures with a String parameter.
TRAP_DEF_HITS=$(grep -rn --include='*.java' \
    -E '\b(public|protected)[[:space:]]+(long|void)[[:space:]]+(get|set)CurrentMoney[[:space:]]*\([[:space:]]*String' \
    "$ROOT_DIR" 2>/dev/null || true)

if [[ -n "$TRAP_DEF_HITS" ]]; then
    echo "ERROR: reintroduction of the banned 'currentMoney' trap method:"
    echo "$TRAP_DEF_HITS"
    echo
    echo "These methods were renamed to getTotalPnl/setTotalPnl to kill the"
    echo "SUN-748/SUN-753 class of bugs. Do not bring them back."
    FAIL=1
fi

# 2. Ban new callers of the trap method.
#    We deliberately only match the two-arg and String-literal forms —
#    single-arg response-DTO setters are legitimate and guarded by BalanceGuard.
TRAP_CALL_HITS=$(grep -rn --include='*.java' \
    -E '\.(get|set)CurrentMoney\([[:space:]]*"(vin|xu)"' \
    "$ROOT_DIR" 2>/dev/null || true)

if [[ -n "$TRAP_CALL_HITS" ]]; then
    echo "ERROR: call to the banned trap method found:"
    echo "$TRAP_CALL_HITS"
    echo
    echo "Use user.getMoney(type) / user.getVin() for the REAL balance."
    echo "Use user.getTotalPnl(type) only if you need the cumulative P&L counter"
    echo "(e.g. for LogMoneyUserMessage)."
    FAIL=1
fi

# 3. Also ban the two-arg moneyType-variable form.
TRAP_VAR_HITS=$(grep -rn --include='*.java' \
    -E '\.(get|set)CurrentMoney\([[:space:]]*moneyType' \
    "$ROOT_DIR" 2>/dev/null || true)

if [[ -n "$TRAP_VAR_HITS" ]]; then
    echo "ERROR: call to the banned trap method found (moneyType variable form):"
    echo "$TRAP_VAR_HITS"
    echo
    echo "Rename to .getTotalPnl(moneyType) / .setTotalPnl(moneyType, ...)."
    FAIL=1
fi

if [[ $FAIL -ne 0 ]]; then
    echo
    echo "See backend-master/VbeeCommon/src/main/java/com/vinplay/vbee/common/response/BalanceGuard.java"
    echo "for the full explanation of the SUN-748/SUN-753 class of bugs."
    exit 1
fi

echo "OK: no getCurrentMoney/setCurrentMoney trap-method usage found."
exit 0
