# Deploy Hardening — fixes for recurring deploy-cycle bugs

Capture of the operational debt that caused recurring "broke after deploy" symptoms, with actionable items to harden the boot path.

## What kept breaking

| Symptom | Real cause | Status |
|---|---|---|
| Bet history froze after vbee restart | `entrypoint.sh:66` used `cp -n` → preserved stale `/app/config/rabbitmq_config.xml` → consumer for `queue_log_gsc_bets_async` never registered | **FIXED — `cp -n` → `cp -f`** in `backend-master/entrypoint.sh` |
| game-thirdparty autoheal restart loop (~3-min cycles) | Service uses dead-man-tick health probe (`GameHealthServer`) which is designed for game-loop services. game-thirdparty has no game loop → tick stale → 503 → kill | **FIXED — compose healthcheck override + watchdog auto-tick** |
| `queue_log_gsc_bets_async` had 108 backlog with 0 consumers (2026-05-06) | Same as #1 | **FIXED via #1** |
| `money_before` = 0 in agency LS Cược for GSC bets | Writer never stamped `current_money`; reader defaulted to 0 | **FIXED — Phase 2 deployed** (`current_money` stamped at BET_INSERT) |
| Voided wagers re-inserted into Mongo 30min after rollback | `GscHourlyRecon` didn't filter `wager_status=VOID` | **FIXED — `skipped_voided` guard added** |
| Deploys ship silently broken for 30+ min before user notices | No post-deploy verification step | **FIXED — `scripts/post-deploy-verify.sh`** |

## What's still pending

| Issue | Effort | Why deferred |
|---|---|---|
| Compose project name dual-use (`sunwinkr` vs `sunwinkr-backend`) | 1 day | Coordinated with team — picking one name and aliasing breaks reflexive ops habits |
| Other services using `GameHealthServer` wrong (besides game-thirdparty) | 1 day audit | No incidents reported; lower priority |
| `users.vin` → ledger-only truth migration | 1 quarter | Real architectural fix; tracked as roadmap #9 |

## Deploy procedure (post-hardening)

```bash
# 1. Make change on production branch
git checkout production
# ...edit...
git commit
git push

# 2. Build + recreate
cd /root/sunwinkr/sunwinkr-backend
docker compose -f docker-compose.yml \
               -f docker-compose.database.yml \
               -f docker-compose.backend.yml \
               -f docker-compose.games.yml \
               -f docker-compose.banca.yml \
               -f docker-compose.web.yml \
               -p sunwinkr-backend build <services>
docker compose ... -p sunwinkr-backend up -d --no-deps --force-recreate <services>

# 3. Wait 60s for boot + consumer attach
sleep 60

# 4. ALWAYS run verification
./scripts/post-deploy-verify.sh
# Exit 0 = green. Exit 1 = something is broken — DON'T leave the deploy.
```

If `post-deploy-verify.sh` fails:

| Failure | What to do |
|---|---|
| Container not running/healthy | `docker logs <container>` — usually a config or dep missing |
| Queue with backlog and 0 consumers | Check `/app/config/rabbitmq_config.xml` matches `/app/api/<svc>/config/rabbitmq_config.xml`; restart the consumer service |
| game-thirdparty restart loop | Check `:9591/` reachability + logback config + Hazelcast cluster state |
| `log_gsc_bets` stale during peak | Same as queue-no-consumer |
| `v_money_unbalanced_transactions > 0` | **CRITICAL — money is corrupt.** Halt deploys. Identify the broken transaction by `SELECT * FROM v_money_unbalanced_transactions;` and review the SP path that wrote it |

## Why these worked

The common pattern across all these bugs: **stale state surviving a restart**. Either:

- A config file that should regenerate from canonical source but doesn't (entrypoint `cp -n`)
- A consumer that should re-attach but doesn't (queue declaration missing in stale config)
- A health probe that's designed for a different operating model (dead-man tick on a non-loop service)
- An automated reconciler that doesn't know about state from a parallel system (ROLLBACK delete vs hourly recon backfill)

The fix isn't "be more careful when deploying" — it's "make the boot path deterministic and verify it." The entrypoint now force-overwrites configs, the verification script catches the failure modes 60s after restart, and the per-service healthcheck override means each service has the right liveness contract.

## Permanent reduction in deploy risk

Before today: every backend deploy carried a real risk of silent breakage that could go undetected for 30+ min.
After today: the same deploy runs `post-deploy-verify.sh` immediately, catches the same class of bugs in seconds, and exits non-zero so an operator/CI hook can act.
