# Sunwinkr — Differential Commission Model & Rate Audit

**Date:** 2026-05-08
**Owner:** TBD
**Status:** Reference doc + immediate audit findings.

This is the single source of truth for how commission flows through the agent hierarchy on every bet, plus a snapshot of misconfigured rate rows that need a backfill.

---

## 1. Concepts

| Term | Meaning |
|---|---|
| **Player** | A row in `vinplay.users`. Has a `parent_agent_id` pointing at one row in `vinplay_admin.useragent`. |
| **Agency** | A row in `vinplay_admin.useragent`. Has `parentid` pointing at its own upline (recursive). The walk stops at the root TĐL whose `parentid` is the admin/root marker. |
| **Master agency / TĐL** | The top of an agency tree (no parent). Receives the global default category rate (e.g. 1.25% Baccarat) from admin. |
| **ĐL1, ĐL2, …** | Sub-agents under the master. The master "cuts down" their per-game rate (typically by some Δ such as 0.25%) so the difference stays as the master's commission slice. |
| **Differential commission** | Each tier earns `(own_rate − child_rate) × bet`. The cascade gives every level a non-overlapping slice; the sum across all tiers equals the highest tier rate. |
| **rebate_type** | `SELF` for the player's own row (player IS modelled as an agency entity at the bottom — see SUN-720); `DOWNLINE` for every parent tier. |
| **PAID vs PENDING** | DOWNLINE rebates auto-credit the agency wallet (PAID). SELF rebates wait on the player's claim flow (PENDING). |

---

## 2. The cascade — by example

Tree:

```
SpecialAccount   (master, rate 1.25%)
   ├─ Kwon_DL1     (LV1, rate 0.00%)
   │     └─ laviai      (player)
   └─ CompanyAgent (LV1, rate 1.25%)        ← misconfigured today
         └─ sunkr9111   (player)
```

When **laviai** bets 600 on `awc_sexybcrt_mx-live-001` (Baccarat):

1. Build chain bottom→top: `Kwon_DL1` (0.00) → `SpecialAccount` (1.25).
2. Walk:
   - Kwon_DL1: diff = 0.00 − 0 = 0 → amount = 0. **previousRate stays 0** because diff=0.
   - SpecialAccount: diff = 1.25 − 0 = 1.25 → amount = 600 × 1.25 ÷ 100 = **7.50**.
3. SpecialAccount banks 7.50 — visible in `/api/rolling` as `differential_pct=1.25`, `commission_earned=7.50`. ✓

When **sunkr9111** bets 200 on the same game:

1. Chain: `CompanyAgent` (1.25) → `SpecialAccount` (1.25).
2. Walk:
   - CompanyAgent: diff = 1.25 − 0 = 1.25 → amount = 200 × 1.25 ÷ 100 = 2.50. previousRate = 1.25.
   - SpecialAccount: diff = 1.25 − **1.25** = 0 → **amount = 0**.
3. SpecialAccount earns nothing because `CompanyAgent.rate == SpecialAccount.rate`. ✗ visible as `differential_pct=0.00`, `commission_earned=0.00`.

This **matches** the screenshot the operator pasted earlier today.

---

## 3. Implementation pointers (do not rewrite, just verify)

| Concern | Location |
|---|---|
| Pull per-bet game_key from log message | `LogMoneyUserExtraProcessor.handleAutoCommission` |
| Resolve effective rate (Layer chain) | `LogMoneyUserExtraProcessor.effectiveRateFor` → `CommissionRateResolver.resolve` |
| Build agent chain bottom-up | `LogMoneyUserExtraProcessor.buildAgentChain` (uses `useragent.ancestors`) |
| Differential math (BigDecimal scale 4) | `LogMoneyUserExtraProcessor.calculateDifferential` |
| Insert one row per tier (PAID / PENDING) | `LogMoneyUserExtraProcessor.insertPendingLogIfAbsent` |
| Auto-credit `agency_wallet` on DOWNLINE | `LogMoneyUserExtraProcessor.creditAgencyWallet` |
| Render rolling page (de-noised game label) | `GetRebateLogs4AgencyProcessor.process` |

The math is correct. **No code change needed for SUN-1252/1258/1259-class symptoms** — only rate rows in `vinplay.games` (display) and `vinplay.game_commission_rate` (math).

---

## 4. CommissionRateResolver fallback ladder

For a given `(agent_nickname, game_key)`:

1. **EXACT** — row with `game_key='gsc_1052_20101'` (per-table pin).
2. **CATEGORY (FK)** — `games.category_id` joined to `game_commission_rate.category_id`.
3. **CATEGORY (legacy string)** — `live_cat_<X>`, `offline_cat_<X>`.
4. **PROVIDER** — `live_provider_<pc>` (GSC only; AWC and offline skip this).
5. **NONE** — returns 0.

Operator UI today writes to layers 3 (`live_cat_*`) and 1 (exact). Layer 2 reads are auto.

---

## 5. Audit — rows that must be fixed

Source: snapshot of `vinplay.game_commission_rate` for `live_cat_Baccarat` / `live_cat_SicBoDice` taken 2026-05-08, joined against `vinplay_admin.useragent` to build hierarchies.

### 5.1 Sub-agents holding the FULL master rate (1.25%) → upline gets 0%

| agent_nickname | parent | live_cat_Baccarat | live_cat_SicBoDice | Symptom |
|---|---|---|---|---|
| `CompanyAgent` (152) | SpecialAccount (151, master 1.25) | 1.25 | 1.25 | sunkr9111 bets earn 0 for SpecialAccount |
| `congvien` | (verify) | 1.25 | 1.25 | upline 0 |
| `thuycung` | (verify) | 1.25 | 1.25 | upline 0 |
| `vuonquocgia` | (verify) | 1.25 | 1.25 | upline 0 |

### 5.2 Sub-agents at 0% → that branch's downline earns 0% from the leaf

| agent_nickname | parent | rate | Symptom |
|---|---|---|---|
| `Kwon_DL1` (205) | SpecialAccount | 0.00 (Baccarat), 0.80 (SicBoDice) | LV1 itself has nothing to credit; ALL of master's diff appears at the master row (currently the only row that actually pays). |
| `Aho6868`, `ANHNAM2`, `asloppical`, `devlord*`, `MrTony`, `TuyetLynk12`, `Kwon_User*` | various | 0.00 | Same — these LV1/LV2 never earn on their own, master skips their tier (previousRate stays 0). |

### 5.3 Half-configured rows (partial categories)

`KwonDe_3`, `Kwon_User2222`, `TDLskunk` (Baccarat=0 but SicBoDice=1.25 — likely typo).

### 5.4 Missing FK rows (rate keyed by `agent_nickname` only, `agent_user_id IS NULL`)

`camapcon`, `canock`, `congvien`, `congviennuilua`, `congvienthonigg`, `testag003`, `thuycung`, `vqgnamcattien`, `vqgnuibaden`, `vuonquocgia` — Phase 6 backfill (`game_commission_rate.agent_user_id`) didn't catch these. Won't break math (resolver still uses nickname), but they will not cascade-delete on agent removal.

---

## 6. Recommended backfill rules

The operator chooses ONE of the policies below; rates can be set via the admin UI or a single migration.

**Policy A — flat 0.25% downstep per LV (recommended baseline):**

| Tier | Baccarat | SicBoDice | Rationale |
|---|---|---|---|
| Master TĐL | 1.25 | 1.25 | unchanged (admin default) |
| ĐL1 | 1.00 | 1.00 | master earns 0.25 differential on every ĐL1+downstream bet |
| ĐL2 | 0.75 | 0.75 | ĐL1 earns 0.25, master earns 0.25 |
| ĐL3+ | 0.50 | 0.50 | continues |
| Player (leaf) | 0 | 0 | leaf is a player, no further differential |

**Policy B — operator-defined ladder per master.** Each TĐL owns the policy for their own tree. Default is Policy A on new agencies; existing agencies are migrated only after operator confirmation.

Both policies are pure data — **no code change needed**.

---

## 7. Backfill SQL (Policy A applied to current staging)

The SELECT below previews; the UPDATE is gated behind operator approval.

```sql
-- 7.1 PREVIEW — what will change for live_cat_Baccarat
SELECT g.agent_nickname,
       g.rate                    AS current_rate,
       p.commission_rate         AS parent_master_rate,
       CASE
         WHEN ua.parentid = (SELECT id FROM vinplay_admin.useragent WHERE parentid IS NULL OR parentid = 0 ORDER BY id LIMIT 1)
           THEN ROUND(p.commission_rate - 0.25, 2)  -- LV1
         ELSE ROUND(p.commission_rate - 0.50, 2)    -- LV2+
       END AS proposed_rate
  FROM vinplay.game_commission_rate g
  JOIN vinplay_admin.useragent ua  ON ua.nickname = g.agent_nickname
  LEFT JOIN vinplay_admin.useragent p  ON p.id      = ua.parentid
 WHERE g.game_key = 'live_cat_Baccarat'
   AND ua.parentid IS NOT NULL
   AND ua.parentid > 0
 ORDER BY p.commission_rate DESC, g.agent_nickname;

-- 7.2 APPLY (run only after operator review of the preview)
UPDATE vinplay.game_commission_rate g
  JOIN vinplay_admin.useragent ua ON ua.nickname = g.agent_nickname
  JOIN vinplay_admin.useragent p  ON p.id        = ua.parentid
   SET g.rate = GREATEST(0.00, ROUND(p.commission_rate - 0.25, 2))
 WHERE g.game_key IN ('live_cat_Baccarat', 'live_cat_SicBoDice', 'live_cat_DragonTiger', 'live_cat_Roulette')
   AND g.rate >= p.commission_rate;     -- only fix rows where sub-agent ≥ parent
```

Wraps the change in an audit row in `useragent_audit_log` per existing convention.

---

## 8. Things this doc does NOT change

- The differential model itself. Code is correct.
- `CommissionRateResolver` fallback order. Layer 2A FK lookup stays.
- The `agency_wallet` auto-credit path. DOWNLINE → wallet on insert.
- The way `useragent.commission_rate` is used as the "global default" before per-game keys override.

---

## 9. Open follow-ups

1. **SUN-1252/1258/1259 LS Rolling table tag:** rebate_logs has no `round_id`, so the table-tag suffix added to LS Cược (Phase B in `AwcGameNameResolver.parseTableSuffix`) does not yet appear in LS Rolling. Either store the tag on `rebate_logs` at write time (preferred — extend `LogMoneyUserExtraProcessor` to call `parseTableSuffix` once per AWC entry) or JOIN via `wager_code` to log_awc_bets at render time.
2. **Audit dashboard:** add an admin page that flags any sub-agent whose effective rate ≥ parent's effective rate (the misconfig class that produced this audit). Periodic alert beats one-off manual digging.
3. **Phase B for missing platforms:** once the operator gives canonical names for non-MX-LIVE Sexy halls (C-LIVE, R-LIVE, etc.) we add them to `vinplay.games`. Right now resolver only knows the 6 codes seen on staging.

---

## 10. Verification queries

```sql
-- A. Confirm differential is computed for both players
SELECT player_nickname, agent_nickname, game_key,
       differential_pct, rebate_amount, rebate_type
  FROM vinplay.rebate_logs
 WHERE created_at >= NOW() - INTERVAL 1 HOUR
   AND game_key IN ('awc_sexybcrt_mx-live-001','awc_sexybcrt_mx-live-016','awc_sexybcrt_mx-live-017')
 ORDER BY created_at DESC LIMIT 50;

-- B. Master shouldn't see any 0-amount rows after the backfill
SELECT player_nickname, COUNT(*) zero_rows
  FROM vinplay.rebate_logs
 WHERE rebate_type = 'DOWNLINE'
   AND agent_nickname = 'SpecialAccount'
   AND rebate_amount = 0
   AND created_at >= '2026-05-08 00:00:00'
 GROUP BY player_nickname;

-- C. Spot-check that sum(rebate_amount) for one round equals master rate × bet
-- Round 1234 with bet 1000:
SELECT SUM(rebate_amount), MAX(commission_rate) AS top_rate, MAX(bet_amount) AS bet
  FROM vinplay.rebate_logs
 WHERE wager_code = '<round-id>';
-- Expected: SUM == bet × top_rate / 100   (allow 0.0001 fp drift)
```
