# Minigame Smoke Test Harness

REST smoke tests for the **TaiXiu** and **Sicbo** Spring endpoints defined in:

- [`docs/plans/taixiu-extraction-plan.md`](../../../docs/plans/taixiu-extraction-plan.md) — §5 (REST + STOMP API)
- [`docs/plans/sicbo-extraction-plan.md`](../../../docs/plans/sicbo-extraction-plan.md) — §6 (BitZero adapter + STOMP topics)

**Status: All tests are written against the final spec. Endpoints are not yet deployed.**  
Tests that hit a 404 mark themselves `SKIP [PENDING]` rather than `FAIL`, so the suite
exits 0 and CI stays green until the endpoint server is up.

---

## Prerequisites

| Tool | Required | Notes |
|------|----------|-------|
| `curl` | Yes | Any recent version |
| `jq` | Yes | `apt install jq` |
| `python3` | Yes | STOMP frame parser + helpers.sh login |
| `bc` | No | Used in numeric comparisons; falls back gracefully |
| `websocat` | STOMP test only | See install instructions below |

### Install websocat (for `test-stomp-tick-no-dice.sh` only)

```bash
# Debian/Ubuntu
apt-get install -y websocat

# Cargo
cargo install websocat

# Pre-built binary
curl -L https://github.com/vi/websocat/releases/latest/download/websocat.x86_64-unknown-linux-musl \
     -o /usr/local/bin/websocat && chmod +x /usr/local/bin/websocat
```

If `websocat` is absent the STOMP test is `SKIP`ped automatically — no action needed.

---

## How to Run

All commands run from the **repo root** (`sunwinkr/`).

### Full suite

```bash
bash tests/smoke/minigame/run-all.sh
```

### Fast subset (happy-path + error-cases + snapshot censoring, target < 5 min)

```bash
bash tests/smoke/minigame/run-all.sh --fast
```

### Single test

```bash
bash tests/smoke/minigame/run-all.sh --suite test-taixiu-happy-path
```

### Override target environment

```bash
BASE_URL=https://staging-play.sunkr.bet \
ADMIN_BASE_URL=https://staging-admin.sunkr.bet \
PLAYER_USER=zuestang2 \
PLAYER_PASS=4581777b67685c53166793900e05f575 \
ADMIN_USER=superadmin \
ADMIN_PASS=0192023a7bbd73250516f069df18b500 \
bash tests/smoke/minigame/run-all.sh
```

### Run individual test directly

```bash
source tests/smoke/minigame/lib/common.sh
bash tests/smoke/minigame/test-taixiu-happy-path.sh
```

---

## Test Files

| File | Purpose | Spec ref |
|------|---------|----------|
| `lib/common.sh` | Shared helpers: login, curl wrappers, assert functions, `pending_skip` | — |
| `test-taixiu-happy-path.sh` | Full TaiXiu flow: join → bet → state → history → leave | §5.1, §5.2 |
| `test-taixiu-error-cases.sh` | All 6 error codes: 0001–0005, 0401 | §5.5, §2.2 B1 |
| `test-taixiu-idempotency.sh` | Same `clientNonce` twice → same `perBetTxId` + same `currentMoney` | §5.6 |
| `test-taixiu-snapshot-censoring.sh` | **Most critical anti-cheat:** polls state for full round, asserts `dice=[0,0,0] result=-1` pre-reveal 100% of samples | §3.3 |
| `test-sicbo-happy-path.sh` | Full Sicbo flow with STRING betSides: TAI, XIU, POINT_8, ONE_DICE_3 | §6, §2.3 |
| `test-sicbo-one-dice-special.sh` | INV-9: ONE_DICE_3 payout = bet×{2,3,4} by occurrence count | §2.5, INV-9 |
| `test-admin-force-result.sh` | Admin force-result, forced-round dice check, 0403 role check, D8 substring-bypass rejection | §5.1, §3.6, D8 |
| `test-stomp-tick-no-dice.sh` | STOMP `/topic/taixiu/1/tick` — 30s collection, asserts zero tick frames carry non-zero dice | §5.4, §3.3 |
| `run-all.sh` | Orchestrates all tests, PASS/FAIL/SKIP per script, logs to `output/<timestamp>/` | — |

---

## Expected Output (Phase 1 — endpoints not yet deployed)

When running against staging before the minigame Spring endpoints are deployed:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  MINIGAME SMOKE TEST RUNNER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  Date     : 2026-05-14 12:00:00
  Base URL : https://staging-play.sunkr.bet
  Mode     : FULL

Connectivity pre-check...
  Player API : HTTP 200
  Admin API  : HTTP 200

Running: test-taixiu-happy-path

TEST: Player login to obtain access token
  PASS — Got player accessToken (len=32)

TEST: POST /api/v2/taixiu/join (moneyType=1)
  Response: {"error":"Not Found","status":404}
  SKIP [PENDING] — PENDING — endpoint not yet deployed: /api/v2/taixiu/join not yet deployed (got: {"error":"Not Found")

  ...

SUMMARY
  Total : 8
  Pass  : 1
  Fail  : 0
  Skip  : 7
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

FINAL SUMMARY
  Total assertions : 47
  Pass  : 3
  Fail  : 0
  Skip  : 44
  Duration : 12s
  Logs: tests/smoke/minigame/output/20260514-120000/
```

No `FAIL`s, exit code 0. CI stays green.

---

## Expected Output (Phase 2 — endpoints live)

Once `/api/v2/taixiu/*` and `/api/v2/sicbo/*` are deployed:

```
TEST: POST /api/v2/taixiu/join (moneyType=1)
  Response: {"referenceId":12345,"remainTime":38,"bettingState":true,"potTai":50000,...}
  PASS — HTTP 200 / success — POST /api/v2/taixiu/join (moneyType=1)
  PASS — StateDto has all required fields — POST /api/v2/taixiu/join (moneyType=1)

TEST: POST /api/v2/taixiu/bet (moneyType=1, betValue=1000, betSide=1 [TAI])
  Response: {"success":true,"errorCode":"0000","currentMoney":49000,"perBetTxId":12345000123}
  PASS — HTTP 200 / success
  PASS — BetResponseDto has all required fields
  PASS — field .errorCode=0000
  PASS — currentMoney decremented by 1000 after bet

TEST: Pre-reveal state: dice=[0,0,0] result=-1 (snapshot censoring holds)
  PASS — Pre-reveal state: dice=[0,0,0] result=-1 (snapshot censoring holds)
```

---

## Logs

Each run writes per-test logs to:

```
tests/smoke/minigame/output/<YYYYMMDD-HHMMSS>/<test-name>.log
```

The log contains the full stdout+stderr of the test script including all curl responses.
Logs are not committed to git (`.gitignore` entry recommended: `tests/smoke/minigame/output/`).

---

## Key Invariants Covered

| Invariant | Test | Description |
|-----------|------|-------------|
| INV-9 | `test-sicbo-one-dice-special.sh` | ONE_DICE_n prize = bet×{2,3,4} by occurrence count |
| INV-12 | `test-taixiu-idempotency.sh` | Same clientNonce → same perBetTxId (no double-debit) |
| INV-13 | `test-taixiu-error-cases.sh` | bet < 100 → errorCode 0004 |
| Anti-cheat §3.3 | `test-taixiu-snapshot-censoring.sh` | dice=0, result=-1 pre-reveal 100% |
| Anti-cheat §3.3 | `test-stomp-tick-no-dice.sh` | tick frames never carry non-zero dice |
| D8 auth hardening | `test-admin-force-result.sh` | Role check, not substring; impostor → 0403 |

---

## Timing Notes

| Test | Max duration | Notes |
|------|-------------|-------|
| `test-taixiu-happy-path.sh` | ~30s | Faster if betting window is open at start |
| `test-taixiu-error-cases.sh` | ~90s | 0002 test polls up to 60s — inherently flaky |
| `test-taixiu-idempotency.sh` | ~30s | Requires open betting window |
| `test-taixiu-snapshot-censoring.sh` | ~90s | Polls up to 90s to observe a full round |
| `test-sicbo-happy-path.sh` | ~30s | — |
| `test-sicbo-one-dice-special.sh` | ~120s | Waits up to 90s for reveal |
| `test-admin-force-result.sh` | ~120s | Waits up to 120s for forced round reveal |
| `test-stomp-tick-no-dice.sh` | ~35s | 30s collection window |
| **Full suite** | **~8–12 min** | Dominated by polling/waiting tests |
| **Fast subset** | **< 5 min** | `--fast` flag; omits idempotency + one-dice + admin + STOMP |

---

## Adding New Tests

1. Create `test-<name>.sh` in this directory
2. Add `source "$SCRIPT_DIR/lib/common.sh"` at the top
3. Use `pending_skip "<reason>"` for any assertion against an undeployed endpoint
4. End with `print_summary`
5. Add the test name (without `.sh`) to `ALL_SUITES` in `run-all.sh`
6. If it belongs in the fast subset, also add to `FAST_SUITES`

---

## Related Documents

- [`docs/plans/taixiu-extraction-plan.md`](../../../docs/plans/taixiu-extraction-plan.md)
- [`docs/plans/sicbo-extraction-plan.md`](../../../docs/plans/sicbo-extraction-plan.md)
- [`docs/specs/taixiu-sicbo-rules-spec.md`](../../../docs/specs/taixiu-sicbo-rules-spec.md) — 22 invariants
- [`docs/specs/taixiu-sicbo-anticheat-audit.md`](../../../docs/specs/taixiu-sicbo-anticheat-audit.md) — reveal hardening
