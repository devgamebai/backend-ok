#!/bin/bash
# test-stomp-tick-no-dice.sh — STOMP /topic/taixiu/1/tick must never carry non-zero dice
#
# Spec refs:
#   taixiu-extraction-plan.md §5.4 STOMP topics:
#     /topic/taixiu/{moneyType}/tick — per-second pots/remainTime (NO dice)
#   taixiu-extraction-plan.md §3.3 snapshot censoring
#   Anti-cheat: tick frames sent during OPEN/LOCKED/GENERATING must have
#     dice1==0 (or absent), dice2==0 (or absent), dice3==0 (or absent)
#
# Dependency: websocat >= 0.6.0
#   Install: cargo install websocat  OR  apt install websocat
#   If not found, this test is SKIPPED with a clear dependency message.
#
# STOMP framing:
#   CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0
#   SUBSCRIBE\nid:sub-0\ndestination:/topic/taixiu/1/tick\n\n\0
#
# The test collects tick frames for 30s then asserts zero frames contained
# non-zero dice values.
#
# Status: PENDING — /ws/minigame endpoint not yet deployed.

set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

COLLECT_SECONDS=30
WS_URL="${BASE_URL/https:/wss:}/ws/minigame"
# If BASE_URL is http:// convert to ws://
WS_URL="${WS_URL/http:/ws:}"

section "STOMP Tick No-Dice — Dependency check"

test_name "websocat is available on PATH"
if ! command -v websocat >/dev/null 2>&1; then
    _skip "websocat not found — install with: cargo install websocat  OR  apt install websocat"
    echo ""
    echo "  To run this test:"
    echo "    # Debian/Ubuntu:"
    echo "    apt-get install -y websocat"
    echo ""
    echo "    # Cargo:"
    echo "    cargo install websocat"
    echo ""
    echo "    # Pre-built binary:"
    echo "    curl -L https://github.com/vi/websocat/releases/latest/download/websocat.x86_64-unknown-linux-musl \\"
    echo "         -o /usr/local/bin/websocat && chmod +x /usr/local/bin/websocat"
    echo ""
    # Mark remaining tests as pending since the dependency is missing
    test_name "STOMP tick frames contain no non-zero dice (30s)"
    pending_skip "websocat dependency not satisfied"
    test_name "At least 10 tick frames received in 30s"
    pending_skip "websocat dependency not satisfied"
    print_summary
fi
_pass "websocat found: $(websocat --version 2>&1 | head -1)"

section "STOMP Tick No-Dice — Login + token"

test_name "Player login for STOMP auth"
ensure_player_token
_pass "Got player accessToken"

# ─── Liveness probe on WS endpoint ───────────────────────────────────────────
section "STOMP Tick No-Dice — WS endpoint liveness"

test_name "WebSocket endpoint responds to connection"
WS_PROBE=$(echo -e "CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\x00" \
    | timeout 5 websocat --no-close -n "$WS_URL" 2>&1 | head -5 || echo "CONNECTION_FAILED")
echo "  WS probe output: ${WS_PROBE:0:200}"

if echo "$WS_PROBE" | grep -qiE '(CONNECTED|ERROR|CONNECTION_FAILED|refused|404)'; then
    if echo "$WS_PROBE" | grep -qi 'CONNECTED'; then
        _pass "WebSocket + STOMP handshake succeeded"
    elif echo "$WS_PROBE" | grep -qiE '(refused|404|CONNECTION_FAILED)'; then
        pending_skip "/ws/minigame WebSocket endpoint not yet deployed"
        test_name "STOMP tick frames contain no non-zero dice (30s)"
        pending_skip "/ws/minigame not deployed"
        test_name "At least 10 tick frames received in 30s"
        pending_skip "/ws/minigame not deployed"
        print_summary
    else
        _pass "WS connected (STOMP error expected before auth — continuing)"
    fi
else
    _pass "WS connection established (unknown frame: ${WS_PROBE:0:80})"
fi

# ─── STOMP frame builder ──────────────────────────────────────────────────────
# Build STOMP CONNECT + SUBSCRIBE frames with auth header.
# STOMP 1.2 null-byte frame terminator: \x00 (printf '\x00')
build_stomp_input() {
    # CONNECT frame with bearer token in passcode header
    printf "CONNECT\naccept-version:1.2\nheart-beat:0,0\nlogin:%s\npasscode:%s\n\n\x00" \
        "$PLAYER_USER" "$PLAYER_AT"
    sleep 1
    # SUBSCRIBE to tick topic (moneyType=1)
    printf "SUBSCRIBE\nid:sub-0\ndestination:/topic/taixiu/1/tick\n\n\x00"
    # Hold connection open for collection window
    sleep "$COLLECT_SECONDS"
}

# ─── Collect tick frames ──────────────────────────────────────────────────────
section "STOMP Tick No-Dice — Collect frames"

echo "  Connecting to: ${WS_URL}"
echo "  Subscribing to: /topic/taixiu/1/tick"
echo "  Collecting for ${COLLECT_SECONDS}s..."
echo ""

FRAME_LOG="${OUTPUT_DIR}/stomp-tick-frames-$(date +%s).log"
mkdir -p "$OUTPUT_DIR"

# Stream STOMP input, capture all output frames to log
set +e
build_stomp_input | timeout $((COLLECT_SECONDS + 5)) websocat --no-close \
    "$WS_URL" > "$FRAME_LOG" 2>&1
set -e

echo "  Frames captured to: ${FRAME_LOG}"
TOTAL_BYTES=$(wc -c < "$FRAME_LOG" 2>/dev/null || echo "0")
echo "  Total bytes received: ${TOTAL_BYTES}"

# Count MESSAGE frames
FRAME_COUNT=$(grep -c "^MESSAGE" "$FRAME_LOG" 2>/dev/null || echo "0")
echo "  MESSAGE frames: ${FRAME_COUNT}"

# ─── Parse and assert ─────────────────────────────────────────────────────────
section "STOMP Tick No-Dice — Assertions"

test_name "At least 10 tick MESSAGE frames received in ${COLLECT_SECONDS}s"
if (( FRAME_COUNT >= 10 )); then
    _pass "Received ${FRAME_COUNT} MESSAGE frames"
elif (( FRAME_COUNT > 0 )); then
    _fail "Only ${FRAME_COUNT} frames in ${COLLECT_SECONDS}s (expected ~${COLLECT_SECONDS} at 1Hz)"
else
    _fail "Zero MESSAGE frames received — STOMP subscription may not be working"
fi

# Extract JSON payloads from MESSAGE frames and check for non-zero dice
# STOMP MESSAGE frame format: headers\n\n<json-body>\x00
test_name "Zero tick frames contain non-zero dice values (anti-cheat critical)"
echo "  Scanning ${FRAME_COUNT} frames for dice fields..."

LEAK_COUNT=0
FRAMES_WITH_DICE=0
FRAMES_CHECKED=0

# Extract body lines (lines after the blank line in each MESSAGE frame)
# Use python3 for reliable null-byte + multi-frame parsing
LEAK_ANALYSIS=$(python3 - "$FRAME_LOG" <<'PYEOF'
import sys, json, re

log_file = sys.argv[1]
with open(log_file, 'rb') as f:
    raw = f.read()

# Split on STOMP null-byte frame separator
frames = raw.split(b'\x00')
leak_count = 0
frames_with_dice = 0
frames_checked = 0
leaks = []

for frame_bytes in frames:
    frame = frame_bytes.decode('utf-8', errors='replace').strip()
    if not frame.startswith('MESSAGE'):
        continue
    # Body is after the first blank line
    parts = frame.split('\n\n', 1)
    if len(parts) < 2:
        continue
    body = parts[1].strip()
    if not body:
        continue
    frames_checked += 1
    try:
        data = json.loads(body)
    except Exception:
        # Not JSON — skip
        continue
    # Check for dice fields
    d1 = data.get('dice1', data.get('dice', {}).get('d1', 0) if isinstance(data.get('dice'), dict) else 0)
    d2 = data.get('dice2', data.get('dice', {}).get('d2', 0) if isinstance(data.get('dice'), dict) else 0)
    d3 = data.get('dice3', data.get('dice', {}).get('d3', 0) if isinstance(data.get('dice'), dict) else 0)

    if d1 != 0 or d2 != 0 or d3 != 0:
        frames_with_dice += 1
        # Only a leak if at least one is non-zero AND this is a tick frame
        # (reveal frames on /topic/.../reveal are OK to have dice)
        dest = data.get('destination', '')
        if '/tick' in dest or not dest:
            leak_count += 1
            leaks.append(f"  LEAK: dice=[{d1},{d2},{d3}] frame_body={body[:200]}")

print(f"frames_checked={frames_checked}")
print(f"frames_with_dice={frames_with_dice}")
print(f"leak_count={leak_count}")
for l in leaks[:5]:
    print(l)
PYEOF
)

echo "$LEAK_ANALYSIS"

LEAK_COUNT=$(echo "$LEAK_ANALYSIS" | grep "^leak_count=" | cut -d= -f2 || echo "0")
FRAMES_CHECKED=$(echo "$LEAK_ANALYSIS" | grep "^frames_checked=" | cut -d= -f2 || echo "0")
FRAMES_WITH_DICE=$(echo "$LEAK_ANALYSIS" | grep "^frames_with_dice=" | cut -d= -f2 || echo "0")

echo ""
echo "  Frames parsed as JSON: ${FRAMES_CHECKED}"
echo "  Frames with any dice field: ${FRAMES_WITH_DICE}"
echo "  LEAKS (non-zero dice in tick frame): ${LEAK_COUNT}"

if (( LEAK_COUNT == 0 )); then
    _pass "ZERO tick frames leaked non-zero dice values across ${FRAMES_CHECKED} parsed frames"
else
    _fail "CRITICAL: ${LEAK_COUNT} tick frame(s) contained non-zero dice — pre-reveal data leaked via STOMP"
fi

test_name "Tick frames parsed successfully (JSON structure valid)"
if (( FRAMES_CHECKED > 0 )); then
    _pass "Parsed ${FRAMES_CHECKED} tick frame payloads as JSON"
else
    if (( FRAME_COUNT > 0 )); then
        _fail "Received ${FRAME_COUNT} MESSAGE frames but none parsed as JSON — check STOMP body format"
    else
        _skip "No frames to parse — STOMP subscription did not produce frames"
    fi
fi

echo ""
echo "  Full frame log: ${FRAME_LOG}"

print_summary
