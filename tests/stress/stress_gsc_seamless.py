#!/usr/bin/env python3
"""
Stress test for GSC seamless wallet endpoints.

Hits balance / withdraw / deposit (the hot path for live play) at configurable
concurrency. Each "player" loop simulates: balance check -> withdraw (BET) ->
deposit (SETTLED) -- the round a real GSC vendor pushes for one hand.

Output: client-side p50/p99/max per endpoint. For server-side percentiles,
read AggregatorP99Scheduler INFO lines from the game-thirdparty container.

Usage:
  python3 stress_gsc_seamless.py \
    --host https://staging-play.sunkr.bet \
    --operator G7A1 \
    --secret abYVbCrLT2VwpASotZGmCT \
    --currency IDR2 \
    --product 1052 \
    --concurrency 100 \
    --duration 60 \
    --accounts-file accounts.txt \
    --profile bet-round

Profiles:
  balance-only  : only /balance (hammer Hazelcast + balance aggregator)
  bet-round     : balance -> withdraw -> deposit (full BET-SETTLED cycle)
"""

import argparse
import asyncio
import hashlib
import json
import os
import random
import statistics
import string
import sys
import time
import uuid
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

import httpx


# ---------------------------------------------------------------- utilities

def md5_hex(s: str) -> str:
    return hashlib.md5(s.encode("utf-8")).hexdigest()


def sign(operator: str, request_time: str, verb: str, secret: str) -> str:
    return md5_hex(f"{operator}{request_time}{verb}{secret}")


def request_time_now() -> str:
    return time.strftime("%Y-%m-%d %H:%M:%S", time.gmtime())


def rand_wager_code() -> str:
    return "stress-" + uuid.uuid4().hex[:16]


def rand_txn_id() -> str:
    return "stress-" + uuid.uuid4().hex[:20]


# ---------------------------------------------------------------- payloads

def payload_balance(operator: str, secret: str, currency: str, member: str,
                    product: int) -> dict:
    rt = request_time_now()
    return {
        "operator_code": operator,
        "currency": currency,
        "request_time": rt,
        "sign": sign(operator, rt, "getbalance", secret),
        "batch_requests": [
            {"member_account": member, "product_code": product}
        ],
    }


def payload_withdraw(operator: str, secret: str, currency: str, member: str,
                     product: int, amount: float, wager_code: str,
                     txn_id: str) -> dict:
    rt = request_time_now()
    return {
        "operator_code": operator,
        "currency": currency,
        "request_time": rt,
        "sign": sign(operator, rt, "withdraw", secret),
        "member_account": member,
        "product_code": product,
        "transactions": [{
            "id": txn_id,
            "action": "BET",
            "amount": amount,
            "valid_bet_amount": amount,
            "bet_amount": amount,
            "game_code": "AG_BAC_A01",
            "product_code": product,
            "member_account": member,
            "wager_code": wager_code,
            "transfer_code": txn_id,
            "currency": currency,
            "round_id": wager_code,
        }],
    }


def payload_deposit(operator: str, secret: str, currency: str, member: str,
                    product: int, amount: float, wager_code: str,
                    settle_txn_id: str) -> dict:
    rt = request_time_now()
    return {
        "operator_code": operator,
        "currency": currency,
        "request_time": rt,
        "sign": sign(operator, rt, "deposit", secret),
        "member_account": member,
        "product_code": product,
        "transactions": [{
            "id": settle_txn_id,
            "action": "SETTLED",
            "amount": amount,
            "payout": amount,
            "valid_bet_amount": amount,
            "bet_amount": amount,
            "game_code": "AG_BAC_A01",
            "product_code": product,
            "member_account": member,
            "wager_code": wager_code,
            "transfer_code": settle_txn_id,
            "currency": currency,
            "round_id": wager_code,
            "wager_status": "SETTLED",
        }],
    }


# ---------------------------------------------------------------- metrics

@dataclass
class EndpointStats:
    name: str
    latencies_ms: List[float] = field(default_factory=list)
    statuses: Dict[int, int] = field(default_factory=lambda: defaultdict(int))
    biz_codes: Dict[int, int] = field(default_factory=lambda: defaultdict(int))
    errors: int = 0

    def add(self, ms: float, http_status: int, biz_code: Optional[int]):
        self.latencies_ms.append(ms)
        self.statuses[http_status] += 1
        if biz_code is not None:
            self.biz_codes[biz_code] += 1

    def report(self) -> dict:
        if not self.latencies_ms:
            return {"name": self.name, "count": 0}
        arr = sorted(self.latencies_ms)
        n = len(arr)
        def pct(p):
            idx = max(0, min(n - 1, int(round((n - 1) * p / 100))))
            return arr[idx]
        return {
            "name": self.name,
            "count": n,
            "errors": self.errors,
            "avg_ms": round(statistics.fmean(arr), 2),
            "p50_ms": round(pct(50), 2),
            "p95_ms": round(pct(95), 2),
            "p99_ms": round(pct(99), 2),
            "max_ms": round(max(arr), 2),
            "statuses": dict(self.statuses),
            "biz_codes": dict(self.biz_codes),
        }


# ---------------------------------------------------------------- worker

async def post_json(client: httpx.AsyncClient, url: str, body: dict,
                    stats: EndpointStats, timeout: float) -> None:
    t0 = time.perf_counter()
    try:
        r = await client.post(url, json=body, timeout=timeout)
        elapsed = (time.perf_counter() - t0) * 1000.0
        biz = None
        try:
            j = r.json()
            if isinstance(j, dict) and "code" in j:
                biz = int(j["code"])
        except Exception:
            pass
        stats.add(elapsed, r.status_code, biz)
    except Exception:
        elapsed = (time.perf_counter() - t0) * 1000.0
        stats.add(elapsed, 0, None)
        stats.errors += 1


async def worker(idx: int, args, deadline: float, accounts: List[str],
                 stats: Dict[str, EndpointStats],
                 client: httpx.AsyncClient) -> None:
    base = args.host.rstrip("/")
    while time.perf_counter() < deadline:
        member = random.choice(accounts)
        amount = float(random.choice([1, 5, 10, 25, 50]))

        # Always do balance
        bal_url = base + "/gsc/v1/api/seamless/balance"
        bal_body = payload_balance(args.operator, args.secret, args.currency,
                                   member, args.product)
        await post_json(client, bal_url, bal_body, stats["balance"], args.timeout)

        if args.profile == "balance-only":
            continue

        # bet-round: withdraw (BET) -> deposit (SETTLED)
        wager_code = rand_wager_code()
        bet_txn = rand_txn_id()
        settle_txn = rand_txn_id()

        wd_url = base + "/gsc/v1/api/seamless/withdraw"
        wd_body = payload_withdraw(args.operator, args.secret, args.currency,
                                   member, args.product, amount, wager_code,
                                   bet_txn)
        await post_json(client, wd_url, wd_body, stats["withdraw"], args.timeout)

        dp_url = base + "/gsc/v1/api/seamless/deposit"
        dp_body = payload_deposit(args.operator, args.secret, args.currency,
                                  member, args.product, amount, wager_code,
                                  settle_txn)
        await post_json(client, dp_url, dp_body, stats["deposit"], args.timeout)


# ---------------------------------------------------------------- main

async def main_async(args) -> int:
    # Load accounts
    if args.accounts_file:
        with open(args.accounts_file) as f:
            accounts = [l.strip() for l in f if l.strip() and not l.startswith("#")]
    else:
        accounts = args.accounts.split(",")
    if not accounts:
        print("ERROR: no accounts", file=sys.stderr)
        return 2

    print(f"[stress] host={args.host} operator={args.operator} "
          f"currency={args.currency} product={args.product}")
    print(f"[stress] concurrency={args.concurrency} duration={args.duration}s "
          f"profile={args.profile} accounts={len(accounts)}")

    stats: Dict[str, EndpointStats] = {
        "balance": EndpointStats("balance"),
        "withdraw": EndpointStats("withdraw"),
        "deposit": EndpointStats("deposit"),
    }

    limits = httpx.Limits(
        max_keepalive_connections=args.concurrency * 2,
        max_connections=args.concurrency * 3,
    )
    timeout = httpx.Timeout(args.timeout, connect=5.0)
    verify = not args.insecure

    t_start = time.perf_counter()
    deadline = t_start + args.duration

    async with httpx.AsyncClient(limits=limits, timeout=timeout, verify=verify,
                                 http2=False) as client:
        # Ramp up: stagger worker start to avoid cold-start herd
        workers = []
        ramp = max(0.001, args.ramp / max(1, args.concurrency))
        for i in range(args.concurrency):
            await asyncio.sleep(ramp)
            workers.append(asyncio.create_task(
                worker(i, args, deadline, accounts, stats, client)))
        await asyncio.gather(*workers)

    elapsed = time.perf_counter() - t_start

    # Report
    total_requests = sum(len(s.latencies_ms) for s in stats.values())
    rps = total_requests / elapsed if elapsed > 0 else 0.0
    print(f"\n=== STRESS RESULT ({elapsed:.1f}s, "
          f"{total_requests} requests, {rps:.1f} req/s) ===\n")

    reports = {}
    for name, s in stats.items():
        rep = s.report()
        reports[name] = rep
        if rep["count"] == 0:
            print(f"  [{name:8}] NO SAMPLES")
            continue
        print(f"  [{name:8}] n={rep['count']:>6}  "
              f"avg={rep['avg_ms']:>6.1f}ms  "
              f"p50={rep['p50_ms']:>6.1f}ms  "
              f"p95={rep['p95_ms']:>6.1f}ms  "
              f"p99={rep['p99_ms']:>6.1f}ms  "
              f"max={rep['max_ms']:>7.1f}ms  "
              f"errors={rep['errors']}  "
              f"http={dict(rep['statuses'])}  "
              f"biz={dict(rep['biz_codes'])}")

    if args.json_out:
        with open(args.json_out, "w") as f:
            json.dump({
                "host": args.host,
                "concurrency": args.concurrency,
                "duration_s": args.duration,
                "profile": args.profile,
                "elapsed_s": elapsed,
                "total_requests": total_requests,
                "rps": rps,
                "endpoints": reports,
            }, f, indent=2)
        print(f"\n[stress] wrote {args.json_out}")

    # Exit 1 if any errors above threshold OR p99 budget violated
    fail = False
    if args.fail_on_error_pct is not None and total_requests > 0:
        total_err = sum(s.errors for s in stats.values())
        err_pct = (total_err / total_requests) * 100
        if err_pct > args.fail_on_error_pct:
            print(f"[FAIL] error rate {err_pct:.2f}% > "
                  f"{args.fail_on_error_pct}%", file=sys.stderr)
            fail = True
    if args.fail_on_p99_ms is not None:
        for name, rep in reports.items():
            if rep.get("p99_ms", 0) > args.fail_on_p99_ms:
                print(f"[FAIL] {name} p99={rep['p99_ms']}ms > "
                      f"{args.fail_on_p99_ms}ms budget", file=sys.stderr)
                fail = True
    return 1 if fail else 0


def parse_args():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--host", default="https://staging-play.sunkr.bet")
    p.add_argument("--operator", default=os.environ.get("GSC_OPERATOR_CODE", "G7A1"))
    p.add_argument("--secret", default=os.environ.get("GSC_SECRET_KEY", ""),
                   help="GSC_SECRET_KEY")
    p.add_argument("--currency", default=os.environ.get("GSC_CURRENCY", "IDR2"))
    p.add_argument("--product", type=int, default=1052,
                   help="GSC product_code (default 1052 = Dream Gaming)")
    p.add_argument("--concurrency", type=int, default=100)
    p.add_argument("--duration", type=int, default=60, help="seconds")
    p.add_argument("--ramp", type=float, default=2.0,
                   help="total ramp-up seconds")
    p.add_argument("--timeout", type=float, default=10.0,
                   help="per-request timeout seconds")
    p.add_argument("--profile", choices=["balance-only", "bet-round"],
                   default="bet-round")
    p.add_argument("--accounts", default="",
                   help="comma-separated member_account list")
    p.add_argument("--accounts-file", default="")
    p.add_argument("--insecure", action="store_true",
                   help="disable TLS verification")
    p.add_argument("--json-out", default="",
                   help="write per-endpoint JSON report")
    p.add_argument("--fail-on-error-pct", type=float, default=None,
                   help="exit 1 if error rate exceeds this percentage")
    p.add_argument("--fail-on-p99-ms", type=float, default=None,
                   help="exit 1 if any endpoint p99 exceeds this")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    if not args.secret:
        print("ERROR: --secret or GSC_SECRET_KEY required", file=sys.stderr)
        return 2
    if not args.accounts and not args.accounts_file:
        print("ERROR: --accounts or --accounts-file required", file=sys.stderr)
        return 2
    try:
        return asyncio.run(main_async(args))
    except KeyboardInterrupt:
        print("\n[stress] interrupted", file=sys.stderr)
        return 130


if __name__ == "__main__":
    sys.exit(main())
