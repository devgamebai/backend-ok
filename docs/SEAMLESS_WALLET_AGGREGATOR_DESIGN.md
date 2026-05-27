# SEAMLESS_WALLET_AGGREGATOR_DESIGN.md

## Goals

Consolidate the AWC and GSC seamless-wallet integrations under a single `SeamlessWalletAggregator` template-method base class so:

1. The two near-identical race bugs (audit #18 SELECT-then-INSERT on `awc_transactions`; audit #19 Hazelcast `containsKey` then `put` on `gsc_tx_ids`) are fixed by construction at the base class — never re-introduced when adding a new aggregator.
2. Per-aggregator code shrinks to what is actually different: callback parsing, signature scheme, currency unit, response JSON shape. Everything race-sensitive (dedup, wallet movement, audit row, balance push) lives in the base.
3. The existing GSC `provider/` adapter hierarchy (Cq9Provider, EvolutionProvider, JiliProvider, …) is **preserved** unchanged. Aggregators sit one level above them — `SeamlessWalletAggregator` is the per-vendor (AWC vs GSC vs future SBO/IBC) layer; `ProviderAdapter` is the per-game-provider (Evolution vs JILI vs CQ9) layer underneath GSC.
4. Phase-0 ledger primitives are the only dedup gate: `MoneyGateway.creditUser` / `debitUser` route through `money_gateway_log.uk_tx_source(tx_id, source, user_id)` (already race-safe per audit #17, see `MoneyGateway.java:633`). For non-wallet-movement events (rollback-of-non-existent, pure audit), `MoneyLedger.post(transactionType, externalRef, …)` from `MoneyLedger.java:179` is the dedup gate via `money_idempotency` PK.

## 1. Abstraction design

Lives in **`VinPlayDAL/src/main/java/com/vinplay/dal/service/seamless/`** alongside `MoneyGateway`. DAL is the right module because the base class statically calls `MoneyGateway.creditUser`/`debitUser`; AWC's processor at `api/VinPlayPortal/.../awc/AwcCallbackProcessor.java` and GSC's hooks at `game/thirdParty/.../gscSeamless/*Process.java` already depend on `VinPlayDAL`, so no new module dependency is created.

### Core class

```java
public abstract class SeamlessWalletAggregator<REQ, RES> {
    // ── lifecycle (final, non-overridable) ──
    public final String handle(HttpServletRequest http) {
        String rawBody = readBody(http);
        long auditId  = preAudit(rawBody, http);                 // raw-payload row, BEFORE
        SeamlessOutcome out;
        try {
            REQ req = parseRequest(rawBody, http);
            VerifyResult vr = verifySignature(req, http);
            if (!vr.ok) { out = SeamlessOutcome.signatureError(vr.message); }
            else        { out = dispatch(req); }                 // template method below
        } catch (Throwable t) { out = SeamlessOutcome.serverError(t); }
        String json = serializeResponse(out);
        postAudit(auditId, out, json);                            // outcome + response, AFTER
        return json;
    }

    // ── template method: subclass-specific request shape, common flow ──
    protected abstract SeamlessOutcome dispatch(REQ req);

    // ── shared flow primitives (final — subclasses call, never override) ──
    protected final SeamlessOutcome doDebit(SeamlessTxn txn) {
        // (1) resolve user → userId/nickname
        // (2) MoneyGateway.debitUser(uid, nick, amountSubunit,
        //                            mapActionToSource(txn.action), txn.externalRef,
        //                            "<aggregator> <action> " + txn.externalRef)
        //     — UNIQUE(tx_id, source) is THE dedup gate, race-safe per audit #17
        // (3) build SeamlessOutcome with newBalance + currencyToExternal(newBalance)
    }
    protected final SeamlessOutcome doCredit(SeamlessTxn txn) { /* MoneyGateway.creditUser, mirror */ }
    protected final SeamlessOutcome doReadBalance(String username) { /* read-only path */ }

    // ── hooks subclasses MUST implement ──
    protected abstract REQ            parseRequest(String body, HttpServletRequest http);
    protected abstract VerifyResult   verifySignature(REQ req, HttpServletRequest http);
    protected abstract long           currencyToInternal(double providerAmount, String currency);  // → wallet sub-units
    protected abstract double         currencyToExternal(long internalBalance, String currency);   // → provider display
    protected abstract String         mapActionToSource(String aggregatorAction);                  // "bet" → SOURCE_AWC_DEBIT
    protected abstract String         serializeResponse(SeamlessOutcome out);

    // ── hooks with sensible defaults (override only when needed) ──
    protected long  preAudit(String rawBody, HttpServletRequest http) { return 0L; }
    protected void  postAudit(long auditId, SeamlessOutcome out, String responseJson) {}
    protected ValidationResult validateBusinessRules(REQ req) { return ValidationResult.ok(); }
}
```

### Internal contract types

`SeamlessTxn` — normalized per-action record (`username`, `externalRef`, `action`, `amountSubunit`, `currency`, `metadata`). Built by subclass inside `dispatch` so everything below works in the wallet's native unit.

`SeamlessOutcome` — internal result (`status` enum, `newBalance` long sub-units, `errorCode`/`errorMessage`, `metadata`). Each subclass's `serializeResponse` projects this to its provider's JSON shape (AWC `{"errorCode":"0000","balance":…}`, GSC `BalanceResponse.toJson()`, etc.). The base class never touches provider JSON.

`VerifyResult { boolean ok; String message; }`. AWC implements as `key === AwcConfig.cert()` (current `AwcCallbackProcessor.java:88-95`); GSC implements as `HashUtil.md5(operatorCode + requestTime + endpoint + secretKey)` (current `WithdrawProcess.java:111`).

### Subclass shape

```
AwcAggregator           extends SeamlessWalletAggregator<AwcRequest, AwcResponse>
GscAggregator           extends SeamlessWalletAggregator<GscRequest, GscResponse>  // delegates to provider/* underneath
```

`AwcAggregator.dispatch(req)` is the small surviving switch on `action` (currently `AwcCallbackProcessor.java:105-145`); each case constructs a `SeamlessTxn` and calls `doDebit` / `doCredit` / `doReadBalance`.

`GscAggregator` subclasses one further into per-endpoint subclasses (`GscWithdrawAggregator`, `GscDepositAggregator`, …) since GSC routes by URL path, not by an action field. These subclasses keep using the existing `ProviderAdapter` + `ProviderRegistry` for game-provider quirks (fish detection, link-id resolution, hedge volume) — the aggregator layer doesn't know about Evolution vs JILI; that stays where it is in `gscSeamless/provider/`.

## 2. Dedup unification

**Drop:**
- `AwcCallbackProcessor.isDuplicateTxn` at line 796 (the SELECT-then-INSERT TOCTOU on `awc_transactions`) — and every `if (isDuplicateTxn(...))` call site (lines 173, 210, 250, 276, 311, 339, 362, 386, 422, 455, 480, 502, 523, 548, 569, 590).
- GSC's `txMap.containsKey(txId)` / `txMap.put(txId, "1")` pattern at `WithdrawProcess.java:144-150` and `:445`, plus the same pattern in `DepositProcess.java:123` and `CancelProcess.java`. Hazelcast `gsc_tx_ids` IMap stops being a dedup gate.

**Replace with one rule:** every wallet-moving call goes through `MoneyGateway.debitUser(uid, nick, amount, source, EXTERNAL_REF, desc)` or `creditUser(...)` with **a non-null `txId`** equal to the provider's stable id:

| Aggregator | Action group           | `tx_id` argument                   | `source`              |
|------------|------------------------|------------------------------------|-----------------------|
| AWC        | bet, betNSettle (debit)| `platformTxId`                     | `SOURCE_AWC_DEBIT`    |
| AWC        | settle, win (credit)   | `"settle_" + platformTxId`         | `SOURCE_AWC_CREDIT`   |
| AWC        | cancelBet, refund      | `"cancel_" + platformTxId`         | `SOURCE_AWC_CREDIT`   |
| GSC        | withdraw (BET)         | `transaction.id` (or `wager_code`) | `SOURCE_GSC_DEBIT`*   |
| GSC        | deposit (SETTLED)      | `transaction.id`                   | `SOURCE_GSC_CREDIT`*  |
| GSC        | cancel / rollback      | `"cancel_" + transaction.id`       | `SOURCE_GSC_CREDIT`*  |

*New constants `SOURCE_GSC_DEBIT` / `SOURCE_GSC_CREDIT` added to `MoneyGateway.java` next to the existing `SOURCE_AWC_DEBIT`/`SOURCE_AWC_CREDIT` (line 75-76) and `SOURCE_GSC_RECONCILE` (line 93). Note that the prefix-namespacing of cancel/refund is required because `uk_tx_source` is `(tx_id, source, user_id)` (`MoneyGateway.java:633`) — a settle and its later cancel must hash to different rows or the cancel will be silently dedup'd as the settle.

The `awc_transactions` and `gsc_event_log` tables stay — they remain the raw-payload audit (request/response bodies, headers) used for ops debugging. The base class's `preAudit` / `postAudit` hooks call `GscEventLogger.tryLogRequest/tryLogResponse` for GSC (`VinPlayDAL/.../audit/GscEventLogger.java:46`) and an analogous AWC logger we'll factor out from the current inline `saveTxn`. **Neither table is ever again read for dedup.** This is structural — `isDuplicateTxn`-style methods don't exist on the base class so a subclass cannot reintroduce the bug.

## 3. Migration plan

Each phase ships independently with a feature flag. Rollback = flip env var; no schema changes are required to roll back at any phase except 5.

### Phase 1 — Build base class (1-2 days)
- New files: `VinPlayDAL/.../seamless/SeamlessWalletAggregator.java`, `SeamlessTxn.java`, `SeamlessOutcome.java`, `VerifyResult.java`, `ValidationResult.java`.
- Add `SOURCE_GSC_DEBIT`, `SOURCE_GSC_CREDIT` constants to `MoneyGateway.java` (next to line 76).
- Tests: `SeamlessWalletAggregatorTest` covering (a) duplicate `externalRef` → second call returns `DUPLICATE` outcome, balance unchanged; (b) signature failure short-circuits before any wallet call; (c) `doDebit` returning insufficient-balance maps to subclass-defined error code via `serializeResponse`.
- No callers yet. **Rollback:** revert PR; nothing in production touches new code.

### Phase 2 — POC: migrate `BalanceProcess` (½ day)
- Pick GSC `BalanceProcess` (`game/thirdParty/.../gscSeamless/BalanceProcess.java`) — read-only, no wallet movement, no race risk. Proves the template-method shape works.
- Introduce `GscBalanceAggregator extends SeamlessWalletAggregator`. Existing `BalanceProcess.execute` delegates to it behind `gsc.aggregator.balance.enabled` env var; default off in prod, on in staging.
- Tests: contract test asserts identical JSON output for 20 captured production payloads (golden file). On output diff → FAIL.
- **Rollback:** unset `gsc.aggregator.balance.enabled`; old code path runs.

### Phase 3 — Migrate remaining GSC handlers (3-4 days)
Order: `PushBetProcess` → `TransferProcess` → `CancelProcess` → `RollbackProcess` → `DepositProcess` → `WithdrawProcess`. (Lowest TPS first; bet/settle hot path last.)

For each: introduce `GscXxxAggregator`, gate behind `gsc.aggregator.xxx.enabled`. The aggregator preserves all current per-provider quirks by delegating into the existing `ProviderAdapter` (`gscSeamless/provider/ProviderAdapter.java`) inside `dispatch`. Concretely, the inline `provider.resolveBetAmount(...)` / `provider.resolveLinkId(...)` calls at `WithdrawProcess.java:203` and `:388` move into `GscWithdrawAggregator.dispatch` unchanged.

Critical subtlety for `WithdrawProcess`: the `userMoneyService.bet(...)` call at line 322 currently does *both* the wallet deduction *and* the rebate-pipeline publish. `MoneyGateway.debitUser` does only the wallet deduction. Phase 3 keeps the rebate publish as a separate post-debit step inside the aggregator so commission behavior is byte-identical. **Rollback:** flag-flip, per handler.

### Phase 4 — Migrate AWC monolith — **PAUSED**

> **Decision 2026-05-02:** AWC integration is not live in production today.
> Migrating a handler with no traffic adds maintenance surface without
> closing a real risk. Phase 4 is paused until the AWC integration is
> enabled. Audit #18 (the AWC TOCTOU + null-txId + wrong-ordering race)
> stays open until then; once integration goes live, restart Phase 4
> first, before any AWC traffic flows.

When unpaused, the original plan stands: decompose `AwcCallbackProcessor.java` (1053 lines, all private `handleXxx` methods at lines 154-606) into `AwcAggregator` whose `dispatch(AwcRequest)` switches on `req.action()` and delegates to small per-action methods that build a `SeamlessTxn` and call `doDebit`/`doCredit`. The current `addBalance`/`deductBalance` helpers (lines 655-688) collapse into base-class `doDebit`/`doCredit`. The existing `triggerCommission` (line 916) stays as a post-credit hook on the AWC subclass since it's AWC-specific (publishes `LogMoneyUserMessage` to `queue_log_money_user_extra`).

Gate behind `awc.aggregator.enabled`. **Rollback:** flag-flip.

### Phase 5 — Cutover (1 day) — **GSC-only scope**

With Phase 4 paused, Phase 5 covers GSC handlers only. AWC cleanup items move to a future Phase-4-then-5 cycle.

- Flip the six GSC aggregator flags (`GSC_AGGREGATOR_BALANCE_ENABLED`, `_PUSHBET_`, `_TRANSFER_`, `_CANCEL_`, `_ROLLBACK_`, `_DEPOSIT_`, `_WITHDRAW_`) to `true` permanently in staging first, then prod. Soak for two weeks; if no incidents, delete old GSC legacy code paths.
- Remove the legacy `doExecuteInner` bodies inside each `*Process.java` (now unreached when flag is on).
- Remove Hazelcast `gsc_tx_ids` IMap reads/writes (lines `WithdrawProcess.java:144,445`, equivalents in `DepositProcess.java:123` and `CancelProcess.java`). Leave the Hazelcast map allocated until next deploy to avoid a cluster-config rollout.
- Hoist `Config` alias on `GscBalanceAggregator` to `GscConfigProvider` directly at the two remaining call sites (Phase 3a deferred NIT 23a).
- Replace `"shape"` metadata string with an enum (Phase 3a deferred NIT 23c).
- Update `docs/MONEY_LEDGER_OPEN_TASKS.md`: close audit #19. Leave #18 open with a note pointing to the paused Phase 4.

**Total revised:** Phase 1+2+3+5 = ~9-11 working days. Phase 4 deferred until AWC integration goes live, then re-add ~2-3 days.

## 4. Risk register

**Blast radius.** AWC and GSC together carry the bulk of casino TPS. A bug that double-debits or fails-to-credit is a P0 financial incident. Mitigations: (a) per-handler feature flags so a regression is one env var to roll back; (b) golden-file contract tests on captured production payloads guarding response-shape parity; (c) phase 2 starts with the read-only `BalanceProcess` so the template-method mechanics are proven before any wallet-moving handler migrates; (d) `MoneyGateway`'s `uk_tx_source` UNIQUE means even if the aggregator is buggy and calls `debitUser` twice for the same `tx_id`, the second call dedups at the DB layer (`MoneyGateway.java:240`, `INSERT IGNORE` + 1062 catch).

**Hazelcast → MySQL dedup load.** GSC currently dedups in Hazelcast `gsc_tx_ids` (sub-millisecond). Switching to `money_gateway_log` UNIQUE adds a row INSERT to the wallet-update transaction. But the wallet update *already* writes a `money_gateway_log` row (`MoneyGateway.java:240`) — the new dedup is in fact zero additional rows, just zero additional cost. There is one new failure mode: if MySQL is down, the aggregator can't dedup. Hazelcast was tolerant of MySQL outages; MySQL-based dedup is not. Acceptable because if MySQL is down, the wallet write fails anyway — there's no scenario where dedup matters but the wallet doesn't.

**Currency precision.** AWC sends VND in whole units (`parseMoney(...)` at `AwcCallbackProcessor.java:178` — direct long parse) whereas GSC sends a display currency that needs `getExchangeRateIn` multiplication (`WithdrawProcess.java:161`, `Math.round(Math.abs(t.getAmount()) * getExchangeRateIn(currencyCode))`). The base class hooks `currencyToInternal(double, String)` and `currencyToExternal(long, String)` keep these rules entirely inside each subclass — AWC returns `(long) amount` ignoring the currency arg, GSC returns `Math.round(amount * rate)`. **No shared rate logic** is forced; each subclass owns its rules.

**Per-game-provider quirks.** The `gscSeamless/provider/` hierarchy (Cq9Provider, EvolutionProvider, …) sits below the aggregator level and is unchanged. `GscWithdrawAggregator.dispatch` calls `ProviderRegistry.forProduct(productCode)` exactly where `WithdrawProcess.java:185-186` does today. Adding a new game provider remains a one-class-plus-registry-entry change in `gscSeamless/provider/`. The aggregator refactor is orthogonal.

## 5. Out-of-scope

Explicit non-goals for this refactor:
- **No `MoneyGateway` changes** beyond adding `SOURCE_GSC_DEBIT`/`SOURCE_GSC_CREDIT` constants. The race-safety primitives at `MoneyGateway.java:240` and `:633` are already correct (audit #17).
- **No changes to `awc_transactions` schema or `gsc_event_log` schema.** Both tables remain as raw-payload audit. The unique key on `awc_transactions.platform_tx_id` becomes redundant once dedup moves but stays in place — defense in depth, low cost.
- **No new aggregator integration** (SBO, IBC, etc.). Those are follow-ups that benefit from this refactor; designing the abstraction with their requirements is fine, but adding them is a separate PR.
- **No migration of `LaunchAwcGameProcessor` / `LaunchGameProcessor`.** Those are launch-URL endpoints, not seamless wallet — different problem domain.
- **No rebate / commission pipeline changes.** Phase 3 preserves the existing `userMoneyService.bet(...)` rebate publish as a post-debit step; phase 4 preserves AWC's `triggerCommission` as a post-credit hook. Reworking the rebate pipeline is out of scope.
- **No removal of Hazelcast `gsc_tx_ids` IMap allocation.** Phase 5 stops *reading* it; the map config stays until the next cluster-config rollout to avoid coupling.

## Open questions

1. Should `SOURCE_GSC_DEBIT`/`SOURCE_GSC_CREDIT` be a single pair, or per-product-code (e.g. `SOURCE_GSC_EVO_DEBIT`)? Single pair keeps the source enum small but means the `(tx_id, source)` UNIQUE relies entirely on `transaction.id` being unique across all GSC providers. Spot-check whether GSC guarantees that or whether tx-id collisions across products are possible.
2. The current AWC dedup uses prefixed keys (`"cancel_" + platformTxId`, `"settle_" + platformTxId` — see `AwcCallbackProcessor.java:250, 276, 311`). Do we keep that prefixing scheme as the `tx_id` value in `MoneyGateway.debitUser`, or instead vary `source` per action and use the bare `platformTxId`? Keeping prefixed keys is closer to current behavior and reduces source-enum proliferation; recommend that. Confirm with ops.
3. For GSC `RollbackProcess`, there's no wallet movement on a rollback of a non-existent bet — only an audit row. Should that go through `MoneyLedger.post` (single-source-of-truth idempotency on `money_idempotency`) or skip dedup entirely since it's a no-op? Recommend `MoneyLedger.post` with a zero-balance entry pair so the audit row is dedup-protected too; confirm.
