# Wallet Unification — Production Deployment Runbook

**Audience:** On-call operator + DBA.
**Scope:** Every wave of the wallet unification migration (Phases 0–7) when shipped to production.
**Difference from staging:** Production has real player money. Mistakes lose actual VND. Every step here is mandatory; no shortcuts.

---

## Pre-flight checklist (every wave, no exceptions)

| Check | Owner | Evidence required |
|---|---|---|
| Staging snapshot taken | DBA | `mysqldump` filename + size |
| Staging wave deployed ≥7 days ago | Operator | Git commit timestamp |
| Staging drift monitor shows ≤0 trend | Operator | Grafana screenshot |
| Staging smoke tests passing | Operator | Last 3 CI runs green |
| Re-review by architect+security agents | Engineer | Their approval notes attached to ticket |
| PM signoff on open questions | Engineer | Jira comment |
| Production maintenance window approved | Operator | Calendar slot + comms sent |
| Two-person rule: 1 driver + 1 observer | Operator | Both on call when wave applies |

If ANY row fails: ABORT. Reschedule.

---

## Production snapshot procedure

```bash
# 1. Snapshot the involved tables BEFORE any change
TS=$(date -u +%Y%m%dT%H%M%SZ)
WAVE=N  # replace N
SNAP_DIR=/var/backups/sunwinkr/wallet-unify

mkdir -p $SNAP_DIR
docker exec sunwinkr-mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction \
    --routines \
    --triggers \
    --databases vinplay vinplay_admin cgame \
    | gzip > $SNAP_DIR/wave-${WAVE}-pre-${TS}.sql.gz

# 2. Encrypt at rest
gpg --batch --yes --passphrase-file /etc/sunwinkr/snap.key --symmetric \
    --cipher-algo AES256 $SNAP_DIR/wave-${WAVE}-pre-${TS}.sql.gz
rm $SNAP_DIR/wave-${WAVE}-pre-${TS}.sql.gz

# 3. Verify size sanity (>500MB expected for full vinplay schema)
ls -lh $SNAP_DIR/wave-${WAVE}-pre-${TS}.sql.gz.gpg

# 4. Record snapshot hash in audit log
sha256sum $SNAP_DIR/wave-${WAVE}-pre-${TS}.sql.gz.gpg \
    >> /var/log/sunwinkr/wallet-unify-audit.log

# 5. Permissions
chmod 600 $SNAP_DIR/wave-${WAVE}-pre-${TS}.sql.gz.gpg
chown root:root $SNAP_DIR/wave-${WAVE}-pre-${TS}.sql.gz.gpg
```

**Retention:** 90 days. Cron at `/etc/cron.d/wallet-snap-rotate`.

---

## Per-wave production procedure

### Step 1 — Drain (Phases 2, 5)
For phases that touch live wallet state (safe migration, BanCa migration):

```bash
# Set platform to maintenance mode via Hazelcast flag
docker exec sunwinkr-backend-api curl -s -X POST \
    'http://localhost:19082/internal/maintenance?mode=on&aat=<admin-token>'

# Wait for active game sessions to settle
# Drain checks: 0 active AWC bets, 0 active GSC bets, 0 active BanCa sessions, 0 in-flight withdrawals
docker exec sunwinkr-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
    SELECT
      (SELECT COUNT(*) FROM vinplay_minigame.lode WHERE prize IS NULL) AS pending_lode,
      (SELECT COUNT(*) FROM vinplay.log_awc_bets WHERE action='bet' AND wager_code NOT IN (SELECT wager_code FROM vinplay.log_awc_bets WHERE action IN ('settle','betNSettle'))) AS pending_awc,
      (SELECT COUNT(*) FROM vinplay.bank_withdrawals WHERE status='processing') AS pending_withdraw;
"

# Proceed only when all counts = 0
```

### Step 2 — Apply migration

```bash
# Each migration file ships in install/config/mysql/migrations/
# Filenames are date-prefixed: YYYYMMDD_phaseN_*.sql

# Dry-run on a clone first (production has a synced read replica)
docker exec sunwinkr-mysql-replica mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
    < install/config/mysql/migrations/YYYYMMDD_phaseN_xxx.sql

# Apply to primary only after replica dry-run succeeds
docker exec sunwinkr-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
    < install/config/mysql/migrations/YYYYMMDD_phaseN_xxx.sql
```

### Step 3 — Deploy code

```bash
# Build images on staging-built jars (do NOT rebuild from production source)
docker compose -p sunwinkr-prod build backend-api portal-api vbee game-minigame banca

# Recreate with feature flag in shadow mode FIRST
docker compose -p sunwinkr-prod up -d --force-recreate \
    -e UNIFIED_WALLET_PHASE_N=shadow \
    backend-api portal-api vbee game-minigame
```

### Step 4 — Validate in shadow

For 1 hour minimum, monitor:
- `wallet_drift_snapshot` table — drift must stay flat
- `money_anomaly` table — zero new rows
- Application error rate — no spike
- Player support inbox — no "wrong balance" tickets

Promote shadow → on only after the hour passes clean.

### Step 5 — Promote to active

```bash
# Update flag
docker exec sunwinkr-backend-api curl -s -X POST \
    'http://localhost:19082/internal/feature?name=UNIFIED_WALLET_PHASE_N&value=on&aat=<admin-token>'

# Recreate containers to load new env (no rebuild)
docker compose -p sunwinkr-prod up -d --force-recreate backend-api portal-api vbee game-minigame
```

### Step 6 — Lift maintenance mode

```bash
docker exec sunwinkr-backend-api curl -s -X POST \
    'http://localhost:19082/internal/maintenance?mode=off&aat=<admin-token>'
```

### Step 7 — 7-day soak

- Drift snapshot job runs hourly. Alert if drift_users > 0 or sum_abs_drift > 1000 VND.
- Manual smoke pack runs daily for 7 days (script under `tests/wallet-unification/daily-smoke.sh`).
- If ANY drift detected during soak → halt next wave + investigate.

---

## Rollback procedures (per phase)

### Reversible (Phases 0, 1, 2, 6, 7)
1. Set feature flag back: `UNIFIED_WALLET_PHASE_N=off`
2. Recreate containers
3. If migration added objects (views, triggers, columns): DROP them
4. Verify drift returns to pre-change baseline

### Non-reversible without snapshot (Phases 3, 4, 5)
1. **Stop all platform traffic immediately** (maintenance mode)
2. Confirm 2-person sign-off on rollback
3. Restore from latest pre-wave snapshot:
   ```bash
   gpg --batch --passphrase-file /etc/sunwinkr/snap.key --decrypt \
       /var/backups/sunwinkr/wallet-unify/wave-N-pre-TS.sql.gz.gpg \
       | gunzip | docker exec -i sunwinkr-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD"
   ```
4. **Replay transactions since snapshot.** This is the hard part. Read every `money_gateway_log` row with `created_at > snapshot_ts` and reapply. Manual reconciliation may take hours.
5. Post-incident review mandatory within 24h.

---

## Communication template

```
Subject: [maintenance] Wallet unification phase N — YYYY-MM-DD HH:MM ICT

Hi team,

We're shipping wallet unification phase N to production:
- Window: HH:MM – HH:MM ICT (estimated duration N minutes)
- Player impact: brief maintenance mode (~N seconds at start, ~N seconds at end). No data loss.
- Rollback plan: shadow mode → on with 1-hour shadow window; full rollback possible from snapshot.

I'll post in #ops-room when starting / finishing each step. Page me if anything looks wrong.

Driver: <name>
Observer: <name>
Pre-flight checklist: <link to ticket>
```

Send 24h before the window. Re-confirm 1h before.

---

## Anti-patterns — do NOT do these

- **Never** apply a migration on production without ≥7 days clean on staging.
- **Never** use `--rebuild` flag on production deploy.sh — rebuilds from source on production, bypassing the snapshot tied to a specific jar set.
- **Never** skip the snapshot. "It's a small change" is exactly when the snapshot saves you.
- **Never** flip a feature flag from off→on without 1h shadow mode first.
- **Never** roll back without first stopping traffic. Mid-flight transactions during a restore corrupt state worse than the original issue.
- **Never** mass-update wallet balances via raw SQL. Even for "fixes". Always via MoneyGateway with proper ledger entries.
- **Never** apply two phases in one window. One wave per maintenance slot, soak between.

---

## Audit trail requirement

Every production wave action logs to `/var/log/sunwinkr/wallet-unify-audit.log`:

```
[ISO-8601 timestamp] [driver] [observer] [phase] [action] [evidence-hash]
```

Example:
```
2026-06-15T03:00:14Z trung_le aiden_pearce phase_1 snapshot_taken sha256:abc123...
2026-06-15T03:02:31Z trung_le aiden_pearce phase_1 migration_applied install/config/mysql/migrations/20260601_phase1_views.sql
2026-06-15T03:03:00Z trung_le aiden_pearce phase_1 flag_shadow
2026-06-15T04:03:12Z trung_le aiden_pearce phase_1 shadow_clean drift_users=0 anomaly_count=0
2026-06-15T04:03:35Z trung_le aiden_pearce phase_1 flag_on
```

Retain 1 year minimum. Required for any financial audit.

---

## Specific extra care per phase

### Phase 1 (stop vin_total writes)
- Verify NO reporting query depends on `vin_total` being kept current. Run grep on `admin-php`, `agency-php`, all Java agency processors.
- `UserDaoImpl.java:1113` has `WHERE vin != vin_total and is_bot = 0` — this query will flag every user once writes stop. Disable or rewrite before flipping flag.

### Phase 2 (retire safe)
- Read MongoDB `safe_box` collection BEFORE migration. Cross-reference with `users.safe`. Resolve any discrepancy with a per-user audit row.
- Test: pick 100 users with safe>0, freeze them via maintenance, run migration on just those 100, validate, then resume.

### Phase 3 (retire xu)
- xu may still be displayed by FE. Coordinate with FE team: when xu UI element is removed, this can ship.

### Phase 5 (BanCa — HIGHEST RISK)
- Run sub-phases 5a–5e separately, each with own snapshot.
- Blue/green required: keep old BanCa containers up alongside new. Hazelcast flag controls which receives traffic. Shift 10% → 50% → 100% over 7 days.
- Per-session reconciliation must succeed for 1000 consecutive sessions before promoting.

### Phase 4 (drop legacy SP + columns)
- Add 14-day "keep but unused" buffer. SP remains in DB but is never called. Columns remain but writes are blocked by trigger. Only drop after second wave-confirmation.

---

## Escalation

| Symptom | Action |
|---|---|
| Drift > 100K VND per hour | Page on-call DBA + Backend lead |
| `money_anomaly` row appears | Page Backend lead immediately |
| Player support inbox > 5 "wrong balance" in 1h | Maintenance mode + investigate |
| Game server fails health check after deploy | Rollback container via `--force-recreate` of previous image tag |
| Snapshot restore needed | Page DBA + Operator + Backend lead; freeze platform |

Numbers above for staging. Adjust thresholds for production based on baseline traffic.
