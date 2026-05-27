# Wallet drift monitor — SRE runbook

This runbook covers the hourly **wallet-drift-snapshot** cron and its
alerting / Prometheus surface. It pairs with the Phase 0 wallet-unification
migration (`install/config/mysql/migrations/20260511_wallet_unify_phase0_views.sql`)
that created the `v_wallet_drift` view and the empty `wallet_drift_snapshot`
table.

## What it measures

`v_wallet_drift` compares `users.vin` (legacy game-wallet column, still the
hot-path source of truth) against `money_account.balance` for each user's
`PLAYER_VIN / VND` row (Phase 0 ledger). A non-zero `drift_vnd` means a
writer mutated one side without the other — i.e. a dual-write gap.

The script writes one row per run to `wallet_drift_snapshot` with:

| Column           | Meaning                                                   |
| ---------------- | --------------------------------------------------------- |
| `total_users`    | non-bot users in `users`                                  |
| `drifting_users` | row count of `v_wallet_drift`                             |
| `max_abs_drift`  | `MAX(ABS(drift_vnd))` across all drifters                 |
| `sum_abs_drift`  | `SUM(ABS(drift_vnd))` — total VND out of sync platform-wide |

## Installation

```bash
# 1. Verify the script is executable and on the host.
chmod +x /root/sunwinkr/sunwinkr/scripts/wallet-drift-snapshot.sh

# 2. (Once) ensure node_exporter textfile collector directory exists.
sudo mkdir -p /var/lib/node_exporter/textfile_collector
sudo chown root:root /var/lib/node_exporter/textfile_collector
sudo chmod 755 /var/lib/node_exporter/textfile_collector

# 3. Install hourly crontab entry as root.
( crontab -l 2>/dev/null | grep -v 'wallet-drift-snapshot.sh' ; \
  echo '0 * * * * /root/sunwinkr/sunwinkr/scripts/wallet-drift-snapshot.sh >> /var/log/wallet-drift-snapshot.log 2>&1' \
) | crontab -

# 4. Manual smoke test (no cron wait).
/root/sunwinkr/sunwinkr/scripts/wallet-drift-snapshot.sh
```

Expected one-line stdout:

```
wallet-drift-snapshot: snapshot_id=42 total=12873 drifting=0 max_abs=0 sum_abs=0 alert=0
```

## Configuration

The script auto-detects `.env` in this order:

1. `$ENV_FILE` (env var override)
2. `/root/sunwinkr/sunwinkr/.env` (staging layout)
3. `/root/sunwinkr/sunwinkr-backend/.env` (production layout, per `.gitlab-ci.yml`)
4. `../.env` relative to the script

Variables read from `.env`:

| Variable                  | Used for             | Required |
| ------------------------- | -------------------- | -------- |
| `MYSQL_ROOT_PASSWORD`     | mysql exec inside container | yes |
| `TELEGRAM_BOT_TOKEN`      | alerts               | optional (alerts no-op if missing) |
| `TELEGRAM_OPS_CHAT_ID`    | alerts               | optional |

Alert thresholds (override at the cron line if needed):

| Env var                       | Default | Behaviour                |
| ----------------------------- | ------- | ------------------------ |
| `WALLET_DRIFT_ALERT_USERS`    | `0`     | alert if `drifting_users > N` |
| `WALLET_DRIFT_ALERT_SUM_VND`  | `1000`  | alert if `sum_abs_drift > N VND` (v2 RFC H6 gate) |
| `PROM_TEXTFILE_DIR`           | `/var/lib/node_exporter/textfile_collector` | textfile path |

## Prometheus / Grafana

Each run atomically replaces `wallet_drift.prom`. Existing
`node_exporter --collector.textfile.directory` scrape picks it up within
its scrape interval. Metrics exposed:

```
wallet_drift_total_users         {gauge}
wallet_drift_drifting_users      {gauge}
wallet_drift_max_abs_vnd         {gauge}
wallet_drift_sum_abs_vnd         {gauge}
wallet_drift_snapshot_id         {counter}
wallet_drift_last_run_timestamp_seconds  {gauge}
```

Suggested Grafana alerts:

- `wallet_drift_drifting_users > 0` for 2 consecutive scrapes → page on-call
- `wallet_drift_sum_abs_vnd > 1000` → page on-call (RFC H6 gate)
- `time() - wallet_drift_last_run_timestamp_seconds > 7200` → cron broken

## Investigation when an alert fires

```sql
-- 1. Sanity-check the latest snapshot
SELECT * FROM vinplay.wallet_drift_snapshot
 ORDER BY snapshot_id DESC LIMIT 10;

-- 2. Identify the biggest offenders
SELECT user_id, nickname, users_vin, ledger_balance, drift_vnd, ledger_last_change
  FROM vinplay.v_wallet_drift
 ORDER BY ABS(drift_vnd) DESC LIMIT 20;

-- 3. Per-source breakdown for a single user (Phase 0 drift analyzer)
SELECT * FROM vinplay.v_wallet_drift_source_breakdown
 WHERE user_id = <UID> ORDER BY ABS(net_amount) DESC;

-- 4. Suspected unmigrated writer? Cross-check root-cause view
SELECT * FROM vinplay.v_wallet_drift_root_cause WHERE user_id = <UID>;
```

Typical causes (during the wallet-unification rollout):

| Symptom                                   | Likely cause |
| ----------------------------------------- | ------------ |
| Single user, large negative drift         | Game server skipped the `MoneyGateway.debitUser` path (raw UPDATE remaining) |
| Many users, small positive drift          | Promotion / bonus credit wrote only to `users.vin` |
| `users.vin` zero but ledger positive      | New ledger-first writer landed before legacy column got mirrored |
| Drift stable for hours then jumps         | A deploy reintroduced a legacy write path — check the last release |

## Rollback / disable

```bash
# Comment out the crontab line, do NOT delete the table:
crontab -l | sed 's#^0 \* \* \* \* /root/.*wallet-drift-snapshot.sh.*#\# &#' | crontab -
```

The table + view are read-only side-effects; leaving them in place is safe.

## Related

- `docs/RFC_SINGLE_WALLET_UNIFICATION.md` — design intent
- `docs/RFC_SINGLE_WALLET_UNIFICATION_V2_ADDENDUM.md` — H6 alert gate spec
- `docs/WALLET_DRIFT_ANALYSIS_2026-05-11.md` — initial drift baseline
- `scripts/ledger-drift-alarm.sh` — sibling alarm for `v_money_account_drift`
- `install/config/mysql/migrations/20260511_wallet_unify_phase0_views.sql` — table + view DDL
