# RTP Strategy Validation Harness

Monte-Carlo simulator that validates whether our force-lose math (SlotHouseEdge.maybeForceLose + CanCuaRtpBalancer.chooseWinningSide) actually produces the expected house-edge outcomes.

## Why

Per-user RTP overrides + game-level pct config are powerful levers. But the question is: **does the strategy actually work?** Does lowering a user's configured pct produce proportional house profit? Does targeting whales net more than a flat-rate nerf?

This harness answers those questions with synthetic traffic at controlled parameters, so we can iterate on strategy before flipping live flags.

## What it models

The simulator mirrors the EXACT math from production code:

| Game | Force mechanism | Match production |
|---|---|---|
| Slot (3x3) | `extra_force_lose_pct = min(60, 92 - configured_pct)`, jackpot exempt | `SlotHouseEdge.shouldForceLoseByPctOnly` |
| TaiXiu | best-of-N picking side to hit target house edge | `CanCuaRtpBalancer.chooseWinningSide` |
| BauCua | best-of-N dice targeting house edge | `MGRoomBauCua.generateDicesPctAware` |

Slot payout table is calibrated to ~92% EV (matches `BALANCED_WIN_RATE` baseline).

## Test matrix

**4 user tiers × 3 games × 4 flag modes × N trials × M bets**

| Tier | Starting balance |
|---|---|
| small | 100k |
| medium | 1M |
| big | 10M |
| whale | 100M |

| Flag mode | Slot pct | TaiXiu pct | BauCua pct | Who's nerfed |
|---|---|---|---|---|
| `passive` | 92% | 80% | 80% | nobody (baseline) |
| `active` | 70% | 65% | 65% | all players |
| `aggressive` | 55% | 50% | 50% | all players |
| `whale_targeted` | 92% / 80% / 80% for most | 40% override for whale | | only whale |

## Usage

```bash
python3 rtp_strategy_test.py [--bets N] [--trials M] [--seed S] [--games slot,taixiu,baucua]
```

Defaults: 2000 bets × 20 trials × seed 42.

Output:
- `results_matrix.csv` — full per-cell stats (mean net, RTP, bust rate, rounds, house net)
- Console: human-readable matrix + house P/L by mode

## Latest findings (2000 bets × 30 trials, seed 42)

**House net aggregate (across all tiers + games):**

| Mode | House Net | vs passive |
|---|---|---|
| whale_targeted | 411M | **3.7×** |
| aggressive | 379M | 3.4× |
| active | 301M | 2.7× |
| passive (baseline) | 110M | 1.0× |

**Strategy validated:**
1. **Passive still profits** (baseline house edge works — 110M over 60k bets per cell)
2. **Active mode 2.7× passive** — config-driven force-lose scales linearly as expected
3. **Aggressive (55% RTP) produces only ~25% more than active (70% RTP)** — diminishing returns below ~65% because jackpot exemption + cap_prize guards limit the nerf effect
4. **Whale-targeted nets MOST** — nerfing only the whale (100M balance) via override produces highest house P/L while leaving small/medium/big users on baseline (better retention signal)

**Per-tier observations:**
- Whales on slot `whale_targeted` mode: 2.5M net loss per player (realized RTP 49%)
- BauCua saturates around 77% achieved RTP even at aggressive/active — cannot push lower because 6-face equal betting math limits force-lose surface
- TaiXiu has high variance over 2000 bets — small/medium tiers can show lucky swings (±3% RTP)

## What to tune in production

Based on simulator:
1. **Set whale override at 40%** for confirmed net-positive users (not platform-wide) → biggest house P/L per override
2. **Don't set active below ~60%** globally — diminishing returns + retention risk
3. **BauCua target floor is 77%** — pushing configured pct below that has zero effect
4. **Run at least 1000 bets before judging a user** — lower samples show false variance

## Interpreting output

```
whale   slot     whale_targeted        -2,522,000   49.56    0.0     500    25,220,000
```

Per trial: whale user (100M balance) playing 500 slot bets with override pct=40%:
- Mean net: -2.52M (whale loses 2.5% of bankroll)
- Realized RTP: 49.56% (close to configured 40% but jackpots lift it)
- Bust rate: 0% (500 rounds × ~10k per bet = 5M max stake, whale never busts)
- Rounds played: 500 (didn't hit balance floor)
- House net (sum across all 30 trials): 25.22M

## Limitations

- Pure math simulation; does not hit real WebSocket game servers
- Doesn't model session-level behavior (quitting after losses, tilt betting)
- Doesn't simulate bot population
- Slot payout table is a single tuned approximation — real slot variants (KhoBau, VQV, SAH, NDV) have different reel weights
- Doesn't model jackpot pool accumulation across rounds

For end-to-end validation after staging deploy:
1. Enable `SLOT_USE_DYNAMIC_RTP=1` in a controlled game container
2. Set one test user's override to 60% via c=9773
3. Let staging traffic run 30 min
4. Compare user's c=9783 P/L detail before/after → should show -33% expected net delta
