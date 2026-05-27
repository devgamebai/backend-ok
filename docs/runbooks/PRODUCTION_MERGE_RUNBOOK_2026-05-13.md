# Production Merge Runbook — 2026-05-13

**Source:** `staging` (commit `1ae22609`)
**Target:** `production` (currently `1d98bf90`)
**MR template title:** `release(2026-05-13): wallet unification + BanCa session ledger + AWC PTV currency`

> This is a **schema-changing** release. Several columns and tables are
> dropped permanently. Read the entire runbook before proceeding. Every
> `DROP` is one-way without the snapshot files preserved here.

---

## 1. Scope summary

20 staging commits not on production. Logical groupings:

| Group | Commits | Risk |
|---|---|---|
| Wallet unification — schema drops | 14 commits dropping `xu`, `vin_total`, `xu_total`, `safe`, `vip_point*`, `money_vp`, `recharge_money`, `gift_total`, `cgame.users.cash/cash_safe/cash_silver` + Wave-2 (vp_lv_receive, manual_quota, reversed_at, useragent.path_ancestors/wallet_balance + 14 legacy tables) | **HIGH** — irreversible |
| BanCa unified wallet | 5 commits: 18-site mutation migration, c=9997 balance read, c=9998 settle endpoint, EMERGENCY_BANCA bidirectional, replay worker, cgame_user_id nickname-join resolver, VND-unit fix, session-bracket ledger | MED — feature-flag gated |
| BanCa session ledger | session-start/session-end markers, 5s idle settle, sessionOnly mode → 1 ledger row per play episode | LOW — kill switch in `.env` |
| AWC currency change | `AWC_CURRENCY=VND → PTV`, `SEXYBCRT.LIVE.limitId=[281105] → [281401..281405]` | MED — affects new player createMember + bet routing |

---

## 2. Pre-merge gates (must all be green)

1. ✅ Staging stable ≥ 24h on `1ae22609` with **no `vin_total` / `Unknown column` errors** in any container log.
2. ✅ 28/28 AWC callback suite passes (`bash tests/test_awc_callback.sh`).
3. ✅ BanCa play test confirms session brackets produce exactly **one** `money_transaction` row per episode (`bc-idle-*` or `bc-*`).
4. ✅ `SELECT COUNT(*) FROM information_schema.COLUMNS WHERE COLUMN_NAME IN ('xu','vin_total','xu_total','safe','vip_point','vip_point_save','money_vp','recharge_money','gift_total') AND TABLE_SCHEMA='vinplay'` returns **0** on staging.
5. ✅ Wave-2 sanity probe (last query in `20260512_wave2_drop_legacy_columns_and_tables.sql`) returns **0 rows**.
6. ✅ At least one production-shape DB backup exists from within the last 24h.

If any gate is RED, STOP and remediate on staging first.

---

## 3. Production DB pre-flight

Run from a clean shell on the production host. **Do not skip any step.**

```bash
# 3.1  Full prod DB snapshot — required for any rollback
TS=$(date +%Y%m%d_%H%M)
docker exec sunwinkr-mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --no-tablespaces --single-transaction \
   --routines --triggers --events vinplay vinplay_admin vinplay_minigame vinplay_gamebai cgame' \
  > /var/backups/prod_pre_2026-05-13_${TS}.sql
ls -lh /var/backups/prod_pre_2026-05-13_${TS}.sql   # expect 2-6 GB

# 3.2  Column-level snapshot for the columns we will drop. Allows
#      surgical recovery if a forgotten reader surfaces post-cutover.
mkdir -p /var/backups/prod_pre_2026-05-13_${TS}_columns
docker exec sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -B -e "
SELECT id, vin, xu, vin_total, xu_total, safe, vip_point, vip_point_save, money_vp,
       recharge_money, gift_total
FROM vinplay.users
WHERE xu!=0 OR vin_total!=0 OR xu_total!=0 OR safe!=0 OR vip_point!=0
   OR vip_point_save!=0 OR money_vp!=0 OR recharge_money!=0 OR gift_total!=0
"' > /var/backups/prod_pre_2026-05-13_${TS}_columns/users_money_cols.tsv

# Verify it's not empty before continuing
wc -l /var/backups/prod_pre_2026-05-13_${TS}_columns/users_money_cols.tsv

# 3.3  Row counts for the legacy tables Wave-2 drops. If any row count
#      differs materially from staging (where most are 0), STOP and
#      audit — production may still have a live writer.
docker exec sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -t -e "
SELECT \"awc_round_map\", COUNT(*) FROM vinplay.awc_round_map UNION ALL
SELECT \"rebate_logs_backfill_sun_1086\", COUNT(*) FROM vinplay.rebate_logs_backfill_sun_1086 UNION ALL
SELECT \"_wallet_phase3a_errors\", COUNT(*) FROM vinplay._wallet_phase3a_errors UNION ALL
SELECT \"_wallet_phase3a_pre_snapshot\", COUNT(*) FROM vinplay._wallet_phase3a_pre_snapshot UNION ALL
SELECT \"wallet_drift_snapshot\", COUNT(*) FROM vinplay.wallet_drift_snapshot UNION ALL
SELECT \"settings_change_log\", COUNT(*) FROM vinplay.settings_change_log UNION ALL
SELECT \"commission_history_outbox\", COUNT(*) FROM vinplay.commission_history_outbox UNION ALL
SELECT \"ops_event_log\", COUNT(*) FROM vinplay.ops_event_log
"'

# 3.4  Confirm no live writers for the columns we drop. Should return 0.
docker exec sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "
SELECT COUNT(*) FROM vinplay.users WHERE vp_lv_receive != 0 OR manual_quota != 0"'
docker exec sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "
SELECT COUNT(*) FROM vinplay.money_transaction WHERE reversed_at IS NOT NULL"'
```

If 3.4 returns anything non-zero, dump those rows to a column-snapshot
file BEFORE proceeding — Wave-2 will drop the columns and you lose them.

---

## 4. Merge order — STRICT

Schema migrations must finish BEFORE Java is redeployed. Java on staging
already builds against the post-drop schema; running the old prod JAR
against the new schema is fine (it will skip `vin_total` writes via the
new `creditUserWithCumulative`). Running the new staging JAR against the
old prod schema is **NOT** safe — the JAR no longer reads `vin_total`
but `update_money_db` (the legacy SP) still does.

### 4.1 Open MR `staging → production`

```bash
glab mr create \
  --source-branch staging --target-branch production \
  --title "release(2026-05-13): wallet unification + BanCa session ledger + AWC PTV" \
  --description-file docs/runbooks/PRODUCTION_MERGE_RUNBOOK_2026-05-13.md \
  --remove-source-branch=false --no-fork
```

Do NOT merge through GitLab UI yet. We merge by hand after migrations.

### 4.2 Apply migrations on production — **in this exact order**

```bash
cd /root/sunwinkr   # production checkout path

# Pull the new migration files into prod tree but do NOT switch branches yet
git fetch origin staging
git checkout origin/staging -- install/config/mysql/migrations/

MIG=install/config/mysql/migrations

# 4.2.a  Phase 0 — views + drift analyzer (no-ops on data, safe to re-run)
for f in \
    20260511_wallet_unify_phase0_views.sql \
    20260511_wallet_unify_drift_analyzer.sql \
    20260511_gift_code_useds_idx.sql ; do
  docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < $MIG/$f
done

# 4.2.b  Phase 1 — additive SP that stops writing *_total columns
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_phase1_update_money_db_v2.sql

# 4.2.c  Phase 2 — drop users.safe column (safe-balance migrated into vin)
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_phase2_player_vault.sql
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_phase2_safe_migration.sql
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_drop_users_safe_column.sql

# 4.2.d  Phase 3a — xu collapse (xu → vin); then drop xu
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260601_phase3a_xu_collapse_to_vin.sql
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_drop_users_xu_column.sql

# 4.2.e  Phase 4 — drop legacy SP + vin_total / xu_total columns. THIS IS
#         WHERE THE CURRENT PROD STARTS FAILING IF JAVA ISN'T REDEPLOYED.
#         Run only AFTER step 4.3 has built the new image and you are
#         ready to recreate containers within the next 5 minutes.
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260601_phase4_drop_legacy_sp_and_total_columns.sql

# 4.2.f  Phase 5 — drop cgame.users.cash / cash_safe / cash_silver. Run
#         only AFTER the new banca image has been built (step 4.3).
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_phase5_drop_cgame_cash_columns.sql

# 4.2.g  Phase 6 — VIP retirement (vip_point*, money_vp drops + trigger fix)
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260601_phase6_vip_points_table.sql
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_drop_vip_columns_and_fix_trigger.sql

# 4.2.h  Phase 7 — recharge_money + gift_total drops + analytics view switch
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_drop_users_recharge_money_gift_total.sql
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260601_phase7_analytics_view_switch.sql

# 4.2.i  Wave-2 — 5 columns + 14 legacy tables
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < $MIG/20260512_wave2_drop_legacy_columns_and_tables.sql

# 4.2.j  users_bank / admin_banks rename propagation (came from prod hotfix
#         MR !416 — applied earlier on staging, idempotent on prod).
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < migrations/20260512_users_bank_fk_bank_id.sql
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < migrations/20260512_admin_banks_fk_bank_id.sql

# 4.2.k  Sanity probe — all the dropped columns and tables should be gone
docker exec sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -t -e "
SELECT COLUMN_NAME, TABLE_NAME FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=\"vinplay\"
   AND COLUMN_NAME IN (\"xu\",\"vin_total\",\"xu_total\",\"safe\",\"vip_point\",
                       \"vip_point_save\",\"money_vp\",\"recharge_money\",
                       \"gift_total\",\"vp_lv_receive\",\"manual_quota\",
                       \"reversed_at\")"'
# expect: empty
```

If 4.2.k returns rows, **STOP** and triage before continuing — the new
Java JAR will crash on every spin/bet.

### 4.3 Build new images (do NOT recreate containers yet)

```bash
cd /root/sunwinkr
git checkout production && git merge --ff-only origin/staging   # bring staging onto prod
COMPOSE_PROJECT_NAME=sunwinkr-backend docker compose \
  -p sunwinkr-backend \
  -f docker-compose.yml -f docker-compose.database.yml \
  -f docker-compose.backend.yml -f docker-compose.games.yml \
  -f docker-compose.banca.yml \
  build
```

Verify image timestamps under `docker images sunwinkr-backend-* --format '{{.Repository}} {{.CreatedSince}}'`.

### 4.4 Apply Mongo + .env changes

```bash
# 4.4.a  Mongo index for c=110 (auto-applied on fresh init; rerun on existing prod)
docker exec -i sunwinkr-mongodb mongosh -u "$MONGO_USER" -p "$MONGO_PASSWORD" \
   --authenticationDatabase admin \
   < install/config/mongo/changes/2026-05-11-user-login-info-time-log-index.js

# 4.4.b  .env — copy NEW keys from staging .env, do NOT overwrite the file
#        wholesale (prod has its own secrets). Append:
cat >> /root/sunwinkr/.env <<'EOF'

# 2026-05-13 release
AWC_CURRENCY=PTV
BANCA_USE_UNIFIED_WALLET=1
BANCA_MONEYGATEWAY_URL=http://backend-api:19082/api_backend?c=9998
BANCA_SETTLE_INTERVAL_MS=5000
BANCA_SETTLE_THRESHOLD=1
BANCA_BIG_BET_THRESHOLD=50000
BANCA_SETTLE_TIMEOUT_MS=5000
BANCA_SETTLE_MAX_RETRIES=3
BANCA_REPLAY_ENABLED=1
BANCA_IDLE_SETTLE_MS=5000
BANCA_SESSION_ONLY_SETTLE=1
BANCA_SERVICE_TOKEN=<<replace with prod token, NOT the staging one>>
EOF

# DOUBLE-CHECK: BANCA_SERVICE_TOKEN must be a NEW 32-byte hex token for prod,
# not copied from staging. The token guards c=9997/c=9998 admin endpoints.
```

### 4.5 Recreate containers (the cutover)

This is the moment Java reads the new schema. Schema 4.2 + image 4.3
must both be complete.

```bash
COMPOSE_PROJECT_NAME=sunwinkr-backend docker compose \
  -p sunwinkr-backend \
  -f docker-compose.yml -f docker-compose.database.yml \
  -f docker-compose.backend.yml -f docker-compose.games.yml \
  -f docker-compose.banca.yml \
  up -d --force-recreate --no-deps \
    backend-api portal-api vbee game-minigame game-slot banca

# Watch each container's first 90 seconds. None of these grep lines
# should match more than the boot-time INFO records.
for c in sunwinkr-backend-api sunwinkr-portal-api sunwinkr-vbee \
         sunwinkr-game-minigame sunwinkr-game-slot sunwinkr-banca ; do
  echo "=== $c ==="
  docker logs $c --since 90s 2>&1 | \
    grep -iE "Unknown column|vin_total|xu_total|SQLSyntaxException|Exception|ERROR" | \
    head -10
done

# After all 6 are green, recreate the remaining card-game tier:
COMPOSE_PROJECT_NAME=sunwinkr-backend docker compose \
  -p sunwinkr-backend \
  -f docker-compose.yml -f docker-compose.database.yml \
  -f docker-compose.backend.yml -f docker-compose.games.yml \
  up -d --force-recreate --no-deps \
    game-poker game-tlmn game-xocdia game-binh game-sam game-lieng \
    game-baicao game-bacay game-caro game-cotuong game-coup game-xizach \
    game-pokertour game-xocdiatulinh
```

### 4.6 Merge the MR

Only after every container is green and reports clean logs.

```bash
glab mr merge <new-mr-number> --yes
```

---

## 5. Verification (do not skip)

| Check | Command | Expect |
|---|---|---|
| No `Unknown column` errors | `docker logs sunwinkr-backend-api --since 5m 2>&1 \| grep -c "Unknown column"` | 0 |
| BanCa unified wallet live | `docker exec sunwinkr-banca env \| grep BANCA_USE_UNIFIED_WALLET` | `=1` |
| AWC currency PTV | `docker exec sunwinkr-portal-api env \| grep AWC_CURRENCY` | `=PTV` |
| c=110 admin endpoint registered | curl admin api, expect well-formed JSON | `errorCode 1001` for missing aat |
| End-to-end BanCa bracket | shoot fish via WS, pause 5s, query `money_transaction WHERE external_ref LIKE 'banca:settle:%' ORDER BY transaction_id DESC LIMIT 5` | exactly 1 row per play episode tagged `bc-idle-*` or `bc-*` |
| AWC suite | `bash tests/test_awc_callback.sh` | `28/28 PASS` |
| MoneyGateway no `vin_total` write | `docker exec sunwinkr-mysql sh -c 'mysql -e "SHOW CREATE PROCEDURE update_money_db"'` | `update_money_db` no longer exists (v2 only) |

---

## 6. Rollback (only if 5 fails hard)

### 6.1 Container-level rollback (Java only)

If the schema is fine but new Java has a regression, revert the image
tag and recreate:

```bash
docker tag sunwinkr-backend-backend-api:last-working sunwinkr-backend-backend-api:latest
# repeat for each affected service
COMPOSE_PROJECT_NAME=sunwinkr-backend docker compose ... up -d --force-recreate --no-deps <svc>
```

### 6.2 Schema rollback

There is **no automatic rollback** for the column/table drops. To
restore one column without restoring the whole DB:

```sql
-- 1. Re-add the dropped column (NULLable, default 0)
ALTER TABLE vinplay.users ADD COLUMN xu BIGINT NOT NULL DEFAULT 0;

-- 2. Reload from the column snapshot you took in step 3.2
LOAD DATA LOCAL INFILE '/var/backups/.../users_money_cols.tsv'
  INTO TABLE vinplay.users
  FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n' IGNORE 1 LINES
  (id, @vin, @xu, @vin_total, ...)
  SET xu = @xu;
```

If multiple columns / tables broke, restore from the full dump in 3.1
to a side schema (e.g. `vinplay_restore`) and `INSERT … SELECT` per
table.

### 6.3 Last resort — full DB restore

```bash
docker stop sunwinkr-backend-api sunwinkr-portal-api sunwinkr-game-* sunwinkr-banca
docker exec -i sunwinkr-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < /var/backups/prod_pre_2026-05-13_${TS}.sql
# Then restart with the OLD image tag (last-working)
```

You will lose every player wallet change made after the snapshot. Only
do this if a financial regression is in progress (negative balances,
double-credits).

---

## 7. Specific traps to watch for

1. **MR !416 already merged staging→prod direction** (delivered prod
   hotfixes onto staging). When we open the new staging→production MR
   it will look small because most of staging is already on prod via
   the merge commit `1ae22609`. Do not be tempted to skip the migration
   step on that basis — the **drop migrations have not yet run on prod**
   regardless of what `git log` shows.

2. **Mixed Java/schema state must be brief.** Between 4.2.e (drop
   `vin_total`) and 4.5 (recreate containers), the still-running old
   Java will throw `Unknown column 'vin_total'`. Keep the gap under 5
   minutes — pre-build the image (4.3) BEFORE running 4.2.e.

3. **BanCa session-only mode caps history at 1 row per play episode.**
   This is **intentional** (per product decision). If accounting needs
   per-shot rows, the kill switch is `BANCA_SESSION_ONLY_SETTLE=0`
   which restores the threshold-based tick settle. The hot path
   (`Player.Cash`) is unchanged either way.

4. **AWC PTV currency** is a per-environment switch. Existing AWC
   accounts created under VND keep their VND wallet on the AWC side
   regardless of our `AWC_CURRENCY=PTV` change — AWC's `createMember`
   is idempotent and currency is locked at first creation. Only NEW
   players post-release will get PTV. If product wants all players on
   PTV, AWC support has to flip them server-side (separate ticket).

5. **`BANCA_SERVICE_TOKEN` must be unique per environment.** Copying
   the staging token to prod opens a cross-environment write vector.
   Generate a fresh `openssl rand -hex 32` for prod and stash it in
   the prod secrets vault (NOT in git).

6. **Mongo index** is `background: true` so safe to apply on live
   collection, but on a multi-million-row `user_login_info` it will
   still take several minutes — kick it off during a low-traffic
   window and monitor with `db.currentOp({active: true, ns: /user_login_info/})`.

---

## 8. Roll-forward TODOs (post-merge)

- Update `docs/PRODUCTION_CLEANUP_RUNBOOK.md` with the dropped tables
  so the next operator does not try to reference them.
- After 7-day soak with zero `Unknown column` errors, drop
  `_wallet_phase3a_pre_snapshot` (96 rows) — it was kept as audit.
- Re-evaluate `vinplay.commission_rate_policy.per_game_pool` — looks
  unused but a trigger references it; verify the trigger fires at all.
- Confirm `sunkr-admin-next` ships the c=110 duplicate-IP page so the
  MR !415 backend endpoint actually has a consumer.

---

## 9. Sign-off

| Step | Operator | Time | Result |
|---|---|---|---|
| 3.1 DB snapshot | | | |
| 3.4 column-zero check | | | |
| 4.2.a–4.2.k migrations | | | |
| 4.3 image build | | | |
| 4.5 container recreate | | | |
| 5 verification | | | |
| 4.6 MR merge | | | |

Roll-out lead: __________
Reviewer: __________

