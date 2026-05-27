#!/usr/bin/env python3
"""
Stress test for AWC seamless wallet endpoint (/awc/callback).

Mirrors the GSC harness layout: configurable concurrency, per-action
client-side latency percentiles, server-side metrics already exposed by
AggregatorP99Scheduler under names AwcGetBalance / AwcBet / AwcSettle.

Profiles:
  balance-only  : only getBalance (hammer the read path / Hazelcast)
  bet-round     : bet -> settle (full BET-SETTLE round on one platform_tx_id)

Usage:
  python3 stress_awc_seamless.py \
    --host https://staging-play.sunkr.bet \
    --cert PigVq2D07hNL \
    --prefix lime \
    --concurrency 100 --duration 60 \
    --accounts-file accounts.txt --profile bet-round
"""
import argparse
import asyncio
import json
import os
import random
import statistics
import sys
import time
import uuid
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Dict, List, Optional

import httpx


def now_iso() -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%S+08:00",
                         time.gmtime(time.time() + 8 * 3600))


def rand_platform_tx_id() -> str:
    return "STR-" + uuid.uuid4().hex[:12].upper()


def to_awc_user_id(prefix: str, nick: str) -> str:
    clean = "".join(c for c in (prefix + nick).lower() if c.isalnum())
    return clean[:21]


def env_outer(cert: str, message: dict) -> dict:
    return {"key": cert, "message": message}


def payload_get_balance(prefix: str, nick: str) -> dict:
    return {"action": "getBalance", "userId": to_awc_user_id(prefix, nick)}


def payload_bet(prefix: str, nick: str, amount: float, platform_tx_id: str,
                round_id: str) -> dict:
    return {
        "action": "bet",
        "txns": [{
            "platformTxId": platform_tx_id,
            "userId": to_awc_user_id(prefix, nick),
            "platform": "SEXYBCRT",
            "gameCode": "MX-LIVE-001",
            "gameName": "Baccarat Classic",
            "gameType": "LIVE",
            "roundId": round_id,
            "betAmount": str(amount),
            "betTime": now_iso(),
        }],
    }


def payload_settle(prefix: str, nick: str, amount: float, win_amount: float,
                   platform_tx_id: str, round_id: str) -> dict:
    return {
        "action": "settle",
        "txns": [{
            "platformTxId": platform_tx_id,
            "userId": to_awc_user_id(prefix, nick),
            "platform": "SEXYBCRT",
            "gameCode": "MX-LIVE-001",
            "gameName": "Baccarat Classic",
            "gameType": "LIVE",
            "roundId": round_id,
            "betAmount": str(amount),
            "winAmount": str(win_amount),
            "turnover": str(amount),
            "txTime": now_iso(),
            "wagerStatus": "settled",
        }],
    }


@dataclass
class EndpointStats:
    name: str
    latencies_ms: List[float] = field(default_factory=list)
    statuses: Dict[int, int] = field(default_factory=lambda: defaultdict(int))
    biz_codes: Dict[str, int] = field(default_factory=lambda: defaultdict(int))
    errors: int = 0

    def add(self, ms: float, status: int, biz: Optional[str]):
        self.latencies_ms.append(ms)
        self.statuses[status] += 1
        if biz is not None:
            self.biz_codes[biz] += 1

    def report(self) -> dict:
        if not self.latencies_ms:
            return {"name": self.name, "count": 0}
        arr = sorted(self.latencies_ms)
        n = len(arr)
        def pct(p):
            idx = max(0, min(n - 1, int(round((n - 1) * p / 100))))
            return arr[idx]
        return {
            "name": self.name, "count": n, "errors": self.errors,
            "avg_ms": round(statistics.fmean(arr), 2),
            "p50_ms": round(pct(50), 2), "p95_ms": round(pct(95), 2),
            "p99_ms": round(pct(99), 2), "max_ms": round(max(arr), 2),
            "statuses": dict(self.statuses), "biz_codes": dict(self.biz_codes),
        }


async def post_callback(client: httpx.AsyncClient, url: str, body: dict,
                        stats: EndpointStats, timeout: float):
    t0 = time.perf_counter()
    try:
        r = await client.post(url, json=body, timeout=timeout)
        elapsed = (time.perf_counter() - t0) * 1000.0
        biz = None
        try:
            j = r.json()
            if isinstance(j, dict) and "status" in j:
                biz = str(j["status"])
        except Exception:
            pass
        stats.add(elapsed, r.status_code, biz)
    except Exception:
        elapsed = (time.perf_counter() - t0) * 1000.0
        stats.add(elapsed, 0, None)
        stats.errors += 1


async def worker(idx: int, args, deadline: float, accounts: List[str],
                 stats: Dict[str, EndpointStats], client: httpx.AsyncClient):
    url = args.host.rstrip("/") + "/awc/callback"
    while time.perf_counter() < deadline:
        nick = random.choice(accounts)

        await post_callback(client, url,
                            env_outer(args.cert, payload_get_balance(args.prefix, nick)),
                            stats["getBalance"], args.timeout)

        if args.profile == "balance-only":
            continue

        amount = float(random.choice([1, 5, 10, 25, 50]))
        ptx = rand_platform_tx_id()
        round_id = "Mexico-STR-" + uuid.uuid4().hex[:8]

        await post_callback(client, url,
                            env_outer(args.cert, payload_bet(args.prefix, nick, amount, ptx, round_id)),
                            stats["bet"], args.timeout)
        await post_callback(client, url,
                            env_outer(args.cert, payload_settle(args.prefix, nick, amount, amount, ptx, round_id)),
                            stats["settle"], args.timeout)


async def main_async(args) -> int:
    if args.accounts_file:
        with open(args.accounts_file) as f:
            accounts = [l.strip() for l in f if l.strip() and not l.startswith("#")]
    else:
        accounts = args.accounts.split(",")
    if not accounts:
        print("ERROR: no accounts", file=sys.stderr); return 2

    print(f"[awc-stress] host={args.host} prefix={args.prefix} "
          f"concurrency={args.concurrency} duration={args.duration}s "
          f"profile={args.profile} accounts={len(accounts)}")

    stats: Dict[str, EndpointStats] = {
        "getBalance": EndpointStats("getBalance"),
        "bet": EndpointStats("bet"),
        "settle": EndpointStats("settle"),
    }

    limits = httpx.Limits(max_keepalive_connections=args.concurrency * 2,
                           max_connections=args.concurrency * 3)
    timeout = httpx.Timeout(args.timeout, connect=5.0)
    verify = not args.insecure

    t_start = time.perf_counter()
    deadline = t_start + args.duration
    async with httpx.AsyncClient(limits=limits, timeout=timeout, verify=verify,
                                  http2=False) as client:
        ramp = max(0.001, args.ramp / max(1, args.concurrency))
        workers = []
        for i in range(args.concurrency):
            await asyncio.sleep(ramp)
            workers.append(asyncio.create_task(
                worker(i, args, deadline, accounts, stats, client)))
        await asyncio.gather(*workers)

    elapsed = time.perf_counter() - t_start
    total_requests = sum(len(s.latencies_ms) for s in stats.values())
    rps = total_requests / elapsed if elapsed > 0 else 0.0

    print(f"\n=== AWC STRESS RESULT ({elapsed:.1f}s, "
          f"{total_requests} requests, {rps:.1f} req/s) ===\n")
    reports = {}
    for name, s in stats.items():
        rep = s.report()
        reports[name] = rep
        if rep["count"] == 0:
            print(f"  [{name:10}] NO SAMPLES")
            continue
        print(f"  [{name:10}] n={rep['count']:>6}  "
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
            json.dump({"host": args.host, "concurrency": args.concurrency,
                       "duration_s": args.duration, "profile": args.profile,
                       "elapsed_s": elapsed, "total_requests": total_requests,
                       "rps": rps, "endpoints": reports}, f, indent=2)
        print(f"\n[awc-stress] wrote {args.json_out}")

    fail = False
    if args.fail_on_error_pct is not None and total_requests > 0:
        err_pct = (sum(s.errors for s in stats.values()) / total_requests) * 100
        if err_pct > args.fail_on_error_pct:
            print(f"[FAIL] error rate {err_pct:.2f}% > {args.fail_on_error_pct}%", file=sys.stderr)
            fail = True
    if args.fail_on_p99_ms is not None:
        for name, rep in reports.items():
            if rep.get("p99_ms", 0) > args.fail_on_p99_ms:
                print(f"[FAIL] {name} p99={rep['p99_ms']}ms > {args.fail_on_p99_ms}ms", file=sys.stderr)
                fail = True
    return 1 if fail else 0


def parse_args():
    p = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--host", default="https://staging-play.sunkr.bet")
    p.add_argument("--cert", default=os.environ.get("AWC_CERT", ""),
                   help="AWC callback cert (verifyCallbackKey)")
    p.add_argument("--prefix", default=os.environ.get("AWC_PREFIX", "lime"))
    p.add_argument("--concurrency", type=int, default=100)
    p.add_argument("--duration", type=int, default=60)
    p.add_argument("--ramp", type=float, default=2.0)
    p.add_argument("--timeout", type=float, default=10.0)
    p.add_argument("--profile", choices=["balance-only", "bet-round"], default="bet-round")
    p.add_argument("--accounts", default="")
    p.add_argument("--accounts-file", default="")
    p.add_argument("--insecure", action="store_true")
    p.add_argument("--json-out", default="")
    p.add_argument("--fail-on-error-pct", type=float, default=None)
    p.add_argument("--fail-on-p99-ms", type=float, default=None)
    return p.parse_args()


def main() -> int:
    args = parse_args()
    if not args.cert:
        print("ERROR: --cert or AWC_CERT required", file=sys.stderr); return 2
    if not args.accounts and not args.accounts_file:
        print("ERROR: --accounts or --accounts-file required", file=sys.stderr); return 2
    try:
        return asyncio.run(main_async(args))
    except KeyboardInterrupt:
        return 130


if __name__ == "__main__":
    sys.exit(main())
