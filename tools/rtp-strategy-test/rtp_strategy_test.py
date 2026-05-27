#!/usr/bin/env python3
"""
RTP Strategy Validation Harness — Monte Carlo simulator

Replicates the EXACT math used in our production force-lose logic:
  - Slot:    extra_force_lose_pct = min(60, max(0, baseline_pct - configured_pct))
             jackpot (result=3/4) exempt from force-lose
  - Cân-cửa: best-of-N selection targeting (100 - pct)% house edge

Runs a matrix of (user tier × game × flag mode) × N bets and measures:
  - final_balance, net, rounds_played, realized_rtp, bust_probability,
    longest_streak, variance

User tiers: small=100k, medium=1M, big=10M, whale=100M (starting balance in VIN)
Games: slot (3x3-like payout table), taixiu (1:1 two-sided), baucua (6-face)
Flags:
  passive           — baseline pct from current game_rtp_config
  active            — aggressive house edge (slot 70%, cancua 65%)
  whale_targeted    — baseline for all except whale who gets 40% override

Usage:
    python3 rtp_strategy_test.py [--bets N] [--trials M] [--seed SEED]
    # defaults: 2000 bets per user, 20 trials per cell (averaged), seed=42

Output:
    results_matrix.csv     — per-cell aggregated stats
    bust_trajectories.csv  — sample trajectories for charting
    summary.txt            — human-readable house P/L report
"""

import argparse
import csv
import random
import statistics
from dataclasses import dataclass, field
from typing import Callable, Dict, List, Tuple

# -------------------------------------------------------------------
# User tiers (starting balance in VIN / KRW)
# -------------------------------------------------------------------
USER_TIERS = [
    ("small",  100_000),      # 100k
    ("medium", 1_000_000),    # 1M
    ("big",    10_000_000),   # 10M
    ("whale",  100_000_000),  # 100M
]

# -------------------------------------------------------------------
# Game engines — each returns (payout, is_jackpot) given a bet
# -------------------------------------------------------------------

# Slot 3x3 payout table — tuned to ~92% baseline EV (matches BALANCED_WIN_RATE)
# Weights out of 1000; lose weight = 680
SLOT_WEIGHTS = [
    ("lose",  680,  0, False),
    ("small", 270,  2, False),
    ("med",    40,  4, False),
    ("jp",     10, 22, True),  # jackpot result=3 — exempt from force-lose
]

def roll_slot(bet: int, rng: random.Random) -> Tuple[int, bool]:
    """Returns (payout, is_jackpot) for one slot spin."""
    roll = rng.randint(0, 999)
    cum = 0
    for _, w, mult, jp in SLOT_WEIGHTS:
        cum += w
        if roll < cum:
            return bet * mult, jp
    return 0, False

# TaiXiu: 1:1 payout, 2 sides. Payout = 2*bet on win, 0 on loss.
# Outcome chosen based on force_side (0=Xiu, 1=Tai) or random.
def roll_taixiu(bet: int, chosen_side: int, rng: random.Random) -> Tuple[int, bool]:
    """
    Player picks a side (simulate 50/50 for test users).
    If player's side == outcome: payout 2*bet, else 0.
    """
    player_side = rng.randint(0, 1)
    outcome = chosen_side if chosen_side >= 0 else rng.randint(0, 1)
    return (bet * 2 if player_side == outcome else 0), False

# BauCua: 6 faces, 1 die. Payout per face = bet * (1 + face_count).
# We simulate player betting equal on ALL 6 faces (worst case for house),
# house profit = bet*6 - sum of matches*(1+count).
def roll_baucua(bet_per_face: int, forced_face: int, rng: random.Random) -> Tuple[int, bool]:
    """Player bets `bet_per_face` on EACH of 6 faces. 3 dice rolled."""
    dice = [forced_face if forced_face >= 0 and i == 0 else rng.randint(0, 5)
            for i in range(3)]
    # If no force, all random
    if forced_face < 0:
        dice = [rng.randint(0, 5) for _ in range(3)]
    total_bet = bet_per_face * 6
    total_payout = 0
    for face in range(6):
        matches = dice.count(face)
        if matches > 0:
            total_payout += bet_per_face * (1 + matches)
    return total_payout, False

# -------------------------------------------------------------------
# Force-lose logic — mirrors SlotHouseEdge.maybeForceLose exactly
# -------------------------------------------------------------------
BASELINE_SLOT = 92.0
BASELINE_CANCUA = 80.0
MAX_EXTRA_FORCE_LOSE = 60

def slot_force_lose_fires(configured_pct: float, rng: random.Random) -> bool:
    """Mirrors SlotHouseEdge.shouldForceLoseByPctOnly math."""
    delta = BASELINE_SLOT - configured_pct
    if delta <= 0:
        return False
    extra = min(MAX_EXTRA_FORCE_LOSE, round(delta))
    return rng.randint(0, 99) < extra

def cancua_choose_side(bet_per_side: List[int], configured_pct: float,
                        tries: int, rng: random.Random, roller) -> int:
    """Best-of-N selection targeting (100-pct)% house edge. For 2-sided games."""
    total_bet = sum(bet_per_side)
    if total_bet <= 0:
        return -1
    target_profit = round(total_bet * (100.0 - configured_pct) / 100.0)
    best_side = -1
    best_dist = float('inf')
    for _ in range(tries):
        candidate = rng.randint(0, 1)
        # house profit if this side wins = bet_side_{other} - bet_side_{this}
        profit = bet_per_side[1 - candidate] - bet_per_side[candidate]
        dist = abs(profit - target_profit)
        if dist < best_dist:
            best_dist = dist
            best_side = candidate
    return best_side

def baucua_choose_dice(bet_per_face: List[int], configured_pct: float,
                        tries: int, rng: random.Random) -> List[int]:
    """Best-of-N dice roll targeting house edge for BauCua."""
    total_bet = sum(bet_per_face)
    if total_bet <= 0:
        return [rng.randint(0, 5) for _ in range(3)]
    target_profit = round(total_bet * (100.0 - configured_pct) / 100.0)
    best_dice = None
    best_dist = float('inf')
    for _ in range(tries):
        dice = [rng.randint(0, 5) for _ in range(3)]
        payout = 0
        for face in range(6):
            m = dice.count(face)
            if m > 0:
                payout += bet_per_face[face] * (1 + m)
        profit = total_bet - payout
        dist = abs(profit - target_profit)
        if dist < best_dist:
            best_dist = dist
            best_dice = dice
    return best_dice

# -------------------------------------------------------------------
# Simulator
# -------------------------------------------------------------------
@dataclass
class SimResult:
    tier: str
    game: str
    mode: str
    start_balance: int
    final_balance: int
    net: int
    rounds_played: int
    rounds_max: int
    total_bet: int
    total_payout: int
    realized_rtp: float
    busted: bool
    longest_win_streak: int
    longest_lose_streak: int

def simulate_slot(tier: str, start_balance: int, bet: int, n_bets: int,
                   mode: str, configured_pct: float, rng: random.Random) -> SimResult:
    balance = start_balance
    total_bet = 0
    total_payout = 0
    rounds = 0
    win_streak = lose_streak = max_win = max_lose = 0
    busted = False
    for _ in range(n_bets):
        if balance < bet:
            busted = True
            break
        balance -= bet
        total_bet += bet
        rounds += 1
        payout, is_jackpot = roll_slot(bet, rng)
        # apply force-lose (jackpot exempt)
        if not is_jackpot and payout > 0 and slot_force_lose_fires(configured_pct, rng):
            payout = 0
        balance += payout
        total_payout += payout
        if payout > 0:
            win_streak += 1; max_win = max(max_win, win_streak); lose_streak = 0
        else:
            lose_streak += 1; max_lose = max(max_lose, lose_streak); win_streak = 0
    return SimResult(
        tier=tier, game="slot", mode=mode,
        start_balance=start_balance, final_balance=balance, net=balance-start_balance,
        rounds_played=rounds, rounds_max=n_bets,
        total_bet=total_bet, total_payout=total_payout,
        realized_rtp=(100.0*total_payout/total_bet if total_bet else 0.0),
        busted=busted, longest_win_streak=max_win, longest_lose_streak=max_lose,
    )

def simulate_taixiu(tier: str, start_balance: int, bet: int, n_bets: int,
                     mode: str, configured_pct: float, flag_on: bool,
                     rng: random.Random) -> SimResult:
    balance = start_balance
    total_bet = total_payout = rounds = 0
    win_streak = lose_streak = max_win = max_lose = 0
    busted = False
    for _ in range(n_bets):
        if balance < bet:
            busted = True; break
        balance -= bet
        total_bet += bet
        rounds += 1
        # Simulate bot betting on both sides (fake "room-level" context)
        bet_per_side = [bet, bet]  # equal bets both sides — worst case for house
        forced = cancua_choose_side(bet_per_side, configured_pct, 16, rng,
                                      None) if flag_on else -1
        payout, _ = roll_taixiu(bet, forced, rng)
        balance += payout
        total_payout += payout
        if payout > 0:
            win_streak += 1; max_win = max(max_win, win_streak); lose_streak = 0
        else:
            lose_streak += 1; max_lose = max(max_lose, lose_streak); win_streak = 0
    return SimResult(
        tier=tier, game="taixiu", mode=mode,
        start_balance=start_balance, final_balance=balance, net=balance-start_balance,
        rounds_played=rounds, rounds_max=n_bets,
        total_bet=total_bet, total_payout=total_payout,
        realized_rtp=(100.0*total_payout/total_bet if total_bet else 0.0),
        busted=busted, longest_win_streak=max_win, longest_lose_streak=max_lose,
    )

def simulate_baucua(tier: str, start_balance: int, bet_per_face: int, n_bets: int,
                     mode: str, configured_pct: float, flag_on: bool,
                     rng: random.Random) -> SimResult:
    balance = start_balance
    total_bet = total_payout = rounds = 0
    win_streak = lose_streak = max_win = max_lose = 0
    busted = False
    round_stake = bet_per_face * 6  # bet on all 6 faces
    for _ in range(n_bets):
        if balance < round_stake:
            busted = True; break
        balance -= round_stake
        total_bet += round_stake
        rounds += 1
        if flag_on:
            dice = baucua_choose_dice([bet_per_face]*6, configured_pct, 16, rng)
        else:
            dice = [rng.randint(0,5) for _ in range(3)]
        payout = 0
        for face in range(6):
            m = dice.count(face)
            if m > 0:
                payout += bet_per_face * (1 + m)
        balance += payout
        total_payout += payout
        if payout > round_stake:
            win_streak += 1; max_win = max(max_win, win_streak); lose_streak = 0
        else:
            lose_streak += 1; max_lose = max(max_lose, lose_streak); win_streak = 0
    return SimResult(
        tier=tier, game="baucua", mode=mode,
        start_balance=start_balance, final_balance=balance, net=balance-start_balance,
        rounds_played=rounds, rounds_max=n_bets,
        total_bet=total_bet, total_payout=total_payout,
        realized_rtp=(100.0*total_payout/total_bet if total_bet else 0.0),
        busted=busted, longest_win_streak=max_win, longest_lose_streak=max_lose,
    )

# -------------------------------------------------------------------
# Test matrix
# -------------------------------------------------------------------
FLAG_MODES = [
    # (mode_name, slot_pct, taixiu_pct, baucua_pct, active?)
    ("passive",        92.0, 80.0, 80.0, False),
    ("active",         70.0, 65.0, 65.0, True),
    ("aggressive",     55.0, 50.0, 50.0, True),
    ("whale_targeted", 92.0, 80.0, 80.0, True),  # only whale gets nerfed
]

WHALE_OVERRIDE = (40.0, 40.0, 40.0)  # slot, taixiu, baucua for whale in whale_targeted mode

def run_trial(tier: str, start_balance: int, game: str, mode: str,
               mode_cfg: tuple, n_bets: int, seed: int) -> SimResult:
    rng = random.Random(seed)
    slot_pct, taixiu_pct, baucua_pct, flag_on = mode_cfg
    # whale_targeted override
    if mode == "whale_targeted" and tier == "whale":
        slot_pct, taixiu_pct, baucua_pct = WHALE_OVERRIDE
        flag_on = True
    bet = max(1, start_balance // 10000)  # bet = 0.01% of balance
    if game == "slot":
        return simulate_slot(tier, start_balance, bet, n_bets, mode, slot_pct, rng)
    elif game == "taixiu":
        return simulate_taixiu(tier, start_balance, bet, n_bets, mode, taixiu_pct, flag_on, rng)
    elif game == "baucua":
        # bet_per_face = bet // 6 so round stake ≈ bet
        return simulate_baucua(tier, start_balance, max(1, bet // 6), n_bets, mode, baucua_pct, flag_on, rng)
    raise ValueError(f"unknown game {game}")

def aggregate(results: List[SimResult]) -> Dict:
    """Average stats across trials."""
    return {
        "trials": len(results),
        "mean_net": int(statistics.mean(r.net for r in results)),
        "median_net": int(statistics.median(r.net for r in results)),
        "stdev_net": int(statistics.stdev(r.net for r in results)) if len(results) > 1 else 0,
        "mean_final_balance": int(statistics.mean(r.final_balance for r in results)),
        "bust_rate_pct": round(100.0 * sum(1 for r in results if r.busted) / len(results), 1),
        "mean_rounds_played": round(statistics.mean(r.rounds_played for r in results), 1),
        "mean_realized_rtp": round(statistics.mean(r.realized_rtp for r in results), 2),
        "mean_lose_streak": round(statistics.mean(r.longest_lose_streak for r in results), 1),
        "mean_win_streak": round(statistics.mean(r.longest_win_streak for r in results), 1),
        "total_bet": sum(r.total_bet for r in results),
        "total_payout": sum(r.total_payout for r in results),
        "house_net": sum(r.total_bet - r.total_payout for r in results),
    }

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--bets", type=int, default=2000, help="bets per trial")
    ap.add_argument("--trials", type=int, default=20, help="trials per cell (averaged)")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--games", default="slot,taixiu,baucua")
    args = ap.parse_args()

    games = args.games.split(",")
    matrix_rows = []
    seed_counter = args.seed

    print(f"Running {len(USER_TIERS)} tiers × {len(games)} games × {len(FLAG_MODES)} modes "
          f"× {args.trials} trials × {args.bets} bets each...")
    print()

    for tier_name, balance in USER_TIERS:
        for game in games:
            for mode_name, *mode_cfg in FLAG_MODES:
                trials = []
                for t in range(args.trials):
                    seed_counter += 1
                    trials.append(run_trial(tier_name, balance, game, mode_name,
                                             tuple(mode_cfg), args.bets, seed_counter))
                agg = aggregate(trials)
                matrix_rows.append({
                    "tier": tier_name, "start_balance": balance,
                    "game": game, "mode": mode_name, **agg,
                })

    # Output CSV
    out_csv = "/root/sunwinkr/sunwinkr/tools/rtp-strategy-test/results_matrix.csv"
    with open(out_csv, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(matrix_rows[0].keys()))
        w.writeheader()
        for row in matrix_rows:
            w.writerow(row)
    print(f"Wrote {len(matrix_rows)} rows → {out_csv}")
    print()

    # Human summary
    print("=" * 110)
    print(f"{'TIER':<7} {'GAME':<8} {'MODE':<16} {'MEAN NET':>15} {'RTP%':>7} "
          f"{'BUST%':>6} {'ROUNDS':>7} {'HOUSE NET':>15}")
    print("-" * 110)
    for r in matrix_rows:
        print(f"{r['tier']:<7} {r['game']:<8} {r['mode']:<16} "
              f"{r['mean_net']:>15,} {r['mean_realized_rtp']:>7.2f} "
              f"{r['bust_rate_pct']:>6.1f} {r['mean_rounds_played']:>7.0f} "
              f"{r['house_net']:>15,}")
    print("=" * 110)

    # Per-mode house totals
    print()
    print("HOUSE NET BY MODE (sum across all tiers + games):")
    mode_totals = {}
    for r in matrix_rows:
        mode_totals.setdefault(r['mode'], 0)
        mode_totals[r['mode']] += r['house_net']
    for mode, total in sorted(mode_totals.items(), key=lambda x: -x[1]):
        print(f"  {mode:<16} {total:>18,}")

if __name__ == "__main__":
    main()
