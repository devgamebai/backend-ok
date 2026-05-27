package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.dal.audit.GscEventLogger;
import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.dal.service.seamless.SeamlessOutcome;
import com.vinplay.dal.service.seamless.SeamlessTxn;
import com.vinplay.dal.service.seamless.SeamlessWalletAggregator;
import com.vinplay.dal.service.seamless.VerifyResult;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.GscBetSideEffectMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.statics.Consts;
import com.hazelcast.core.IMap;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3d — GSC Rollback handler implemented on top of
 * {@link SeamlessWalletAggregator}. Mirrors the legacy
 * {@code game.third.hooks.gscSeamless.RollbackProcess} at the wire-response
 * level, gated behind {@code GSC_AGGREGATOR_ROLLBACK_ENABLED}.
 *
 * <h2>Does Rollback move money? Yes.</h2>
 * Verified directly in {@code RollbackProcess.java:117-126}: the legacy
 * code calls {@code userMoneyService.reward(memberAccount, amount, ...)}
 * for every transaction in the request, which routes through
 * {@code UserMoneyServiceImpl.reward} →
 * {@code userService.updateMoney(... TransType.END_TRANS)} — i.e. a
 * positive CREDIT to the player's wallet (refund of a prior bet).
 *
 * <p>This means the open question #3 in
 * {@code docs/SEAMLESS_WALLET_AGGREGATOR_DESIGN.md} §5 ("rollback of
 * non-existent bet — only an audit row") is resolved: the legacy
 * unconditionally credits the player for every rolled-back transaction;
 * there is no distinction between "rollback of existing bet" vs "rollback
 * of non-existent bet" — the provider says "give it back" and we do.
 * Therefore the aggregator path uses the standard {@link #doCredit}
 * primitive (UNIQUE {@code (tx_id, source, user_id)} on
 * {@code money_gateway_log} is the dedup gate, race-safe per audit #17 —
 * a retried rollback with the same {@code id} is a no-op at the wallet
 * layer and we return success so the provider's retry pipeline stops).
 *
 * <p><b>No {@code MoneyLedger.post(...)} zero-amount entry needed.</b>
 * {@code MoneyLedger.post} requires balanced entries with positive
 * amounts ({@code AMOUNT_MUST_BE_POSITIVE} — see
 * {@code MoneyLedger.java:54}); zero-amount audit rows are rejected by
 * the SP. The wallet-moving path through {@code MoneyGateway.creditUser}
 * already provides race-safe idempotency on {@code (tx_id, source,
 * user_id)}, which is the right mechanism here.
 *
 * <h2>External-ref namespacing (collision avoidance)</h2>
 * {@code money_gateway_log.uk_tx_source} is {@code (tx_id, source,
 * user_id)}. The original BET (or SETTLE) and a later ROLLBACK for the
 * same wager must hash to different rows or the rollback would be
 * silently dedup'd as the prior wallet-mover. We prefix the transaction
 * id by {@code rollback_}: {@code rollback_<tx.id>}. This matches the
 * design-doc-recommended pattern (§2 table — "cancel / rollback" → "cancel_" +
 * transaction.id with {@code SOURCE_GSC_CREDIT}; we use {@code rollback_}
 * to disambiguate from CancelProcess, which is a separate Phase 3c
 * aggregator that also targets the credit source).
 *
 * <h2>Behavioral parity vs legacy: rebate reversal + log_gsc_bets cleanup</h2>
 * The legacy code does two extra side-effects per successful credit
 * ({@code RollbackProcess.java:138-181}):
 * <ol>
 *   <li>{@code RebateService.reverseGscByWagerCode(...)} — undoes any
 *       agency-side commission rows the original BET earned, and
 *       invalidates the affected agents' response-cache entries.
 *   <li>{@code MongoDBConnectionFactory.getDB()
 *       .getCollection("log_gsc_bets").deleteMany(...)} — drops the
 *       Mongo bet-history row so the rolled-back bet no longer shows up
 *       in c=303 / c=9843 history endpoints.
 * </ol>
 * Both are <em>best-effort</em> in legacy (wrapped in try/Throwable, log
 * + continue) and Phase 3d preserves that exactly. They're kept here as
 * post-credit hooks, executed in {@link #afterCredit} only when the
 * credit actually posted — duplicate replays do NOT re-run the rebate
 * reversal because the wallet credit was a no-op (DUPLICATE outcome) so
 * there's nothing to reverse the second time. (RebateService.reverseBy*
 * is itself idempotent anyway, but skipping when the wallet was a no-op
 * is the cleaner posture.)
 *
 * <h2>Sign verification</h2>
 * Single verb {@code "rollback"}: {@code md5(operator_code + request_time
 * + "rollback" + secret)}. Mirrors {@code RollbackProcess.java:98}.
 *
 * <h2>Audit endpoint string</h2>
 * Matches the legacy: {@code "rollback"} ({@code RollbackProcess.java:52}).
 *
 * <h2>Production volume</h2>
 * Verified zero events in last 30 days on {@code gsc_event_log} where
 * {@code gsc_endpoint = 'rollback'} (per phase brief). The fixtures are
 * therefore synthetic — built from the documented {@code RollbackRequest}
 * shape rather than captured payloads.
 */
public class GscRollbackAggregator extends SeamlessWalletAggregator<GscRequest, String> {

    private final GscConfigProvider config;

    public GscRollbackAggregator(GscConfigProvider config) {
        if (config == null) throw new IllegalArgumentException("config is null");
        this.config = config;
    }

    /** Tighter audit-log tag than the default class-simple-name. */
    @Override
    protected String aggregatorName() { return "GSC"; }

    /** Phase 5p3 — distinct timing-metrics bucket per handler. */
    @Override
    protected String metricsName() { return "GscRollback"; }

    // ─────────────────────────────────────────────────────────────────
    // parseRequest
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse the inbound JSON into a {@link GscRequest}. The legacy
     * {@code RollbackRequest.fromJson} returns null on a malformed body
     * which is then mapped to {@code "Invalid JSON format"}; we mirror
     * that by returning an all-null {@link GscRequest} and letting
     * {@link #dispatch} flag it.
     */
    @Override
    protected GscRequest parseRequest(String body, HttpServletRequest http) {
        if (body == null || body.isEmpty()) {
            return new GscRequest(null, null, null, null, null, null, null,
                    null, Collections.<GscRequest.TransactionItem>emptyList());
        }
        JSONObject root;
        try {
            root = new JSONObject(body);
        } catch (Throwable t) {
            return new GscRequest(null, null, null, null, null, null, null,
                    null, Collections.<GscRequest.TransactionItem>emptyList());
        }

        String operatorCode  = optStr(root, "operator_code");
        String memberAccount = optStr(root, "member_account");
        Integer productCode  = root.has("product_code") && !root.isNull("product_code")
                ? optInt(root, "product_code") : null;
        String currency      = optStr(root, "currency");
        String gameType      = optStr(root, "game_type");
        String sign          = optStr(root, "sign");
        String requestTime   = optStr(root, "request_time");

        // Legacy RollbackRequest field name is plain "transactions" (no fallback).
        JSONArray txArr = root.optJSONArray("transactions");
        List<GscRequest.TransactionItem> transactions = new ArrayList<>();
        if (txArr != null) {
            for (int i = 0; i < txArr.length(); i++) {
                JSONObject t = txArr.optJSONObject(i);
                if (t == null) continue;
                String id            = optStr(t, "id");
                // Legacy code does NOT branch on action for rollback — it
                // unconditionally calls reward() per txn. We still capture
                // the action for audit/log clarity but treat every txn as
                // a CREDIT regardless.
                String action        = optStr(t, "action");
                // RollbackProcess uses the top-level memberAccount for every
                // txn (transaction.member_account is not consulted in legacy).
                String txMemberAcct  = memberAccount;
                int txProductCode    = optInt(t, "product_code");
                String gameCode      = optStr(t, "game_code");
                String txCurrency    = optStr(t, "currency");
                String wagerCode     = optStr(t, "wager_code");
                String amount        = optStr(t, "amount");
                String betAmount     = optStr(t, "bet_amount");
                transactions.add(new GscRequest.TransactionItem(
                        id, action, txMemberAcct, txProductCode, gameCode,
                        txCurrency, wagerCode, amount, betAmount));
            }
        }

        return new GscRequest(operatorCode, currency, sign, requestTime,
                memberAccount, productCode, gameType,
                /*batchMembers*/null, transactions);
    }

    private static String optStr(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return null;
        try {
            String s = o.optString(key, null);
            if (s == null || s.isEmpty()) return null;
            return s;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int optInt(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return 0;
        try { return o.getInt(key); }
        catch (Throwable ignored) {
            try { return Integer.parseInt(o.getString(key).trim()); }
            catch (Throwable ignored2) { return 0; }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // verifySignature
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected VerifyResult verifySignature(GscRequest req, HttpServletRequest http) {
        if (req == null) return VerifyResult.fail("missing request");
        if (req.getSign() == null) return VerifyResult.fail("missing sign");
        if (req.getOperatorCode() == null || req.getRequestTime() == null) {
            return VerifyResult.fail("missing operator_code or request_time");
        }
        String secret = config.getSecretKey();
        if (secret == null) return VerifyResult.fail("aggregator secret not configured");

        String expected = md5Hex(req.getOperatorCode() + req.getRequestTime() + "rollback" + secret);
        if (!req.getSign().equals(expected)) {
            return VerifyResult.fail("signature mismatch");
        }
        return VerifyResult.ok();
    }

    // ─────────────────────────────────────────────────────────────────
    // mapActionToSource / currency conversion
    // ─────────────────────────────────────────────────────────────────

    /**
     * Rollback always credits — the aggregator action verb is
     * informational at the audit layer, but the wallet direction is
     * fixed. Returns {@code SOURCE_GSC_CREDIT} unconditionally so a
     * future caller that mis-routes a debit-shaped txn through
     * {@link SeamlessWalletAggregator#doDebit} from this aggregator
     * cannot accidentally pick the wrong source.
     */
    @Override
    protected String mapActionToSource(String aggregatorAction) {
        return MoneyGateway.SOURCE_GSC_CREDIT;
    }

    @Override
    protected long currencyToInternal(double providerAmount, String currency) {
        double rateIn = exchangeRateIn(currency);
        if (rateIn == 0.0) rateIn = 1.0;
        return Math.round(providerAmount / rateIn);
    }

    @Override
    protected double currencyToExternal(long internalBalance, String currency) {
        return (double) internalBalance * exchangeRateOut(currency);
    }

    private double exchangeRateIn(String currency) {
        int per = config.getCurrencyExchangeRate(currency);
        if (per > 1) return (double) per;
        double op = config.getOperatorExchangeRate();
        if (op == 0.0 || op == 1.0) return 1.0;
        return op;
    }

    private double exchangeRateOut(String currency) {
        int per = config.getCurrencyExchangeRate(currency);
        if (per > 1) return 1.0 / (double) per;
        double op = config.getOperatorExchangeRate();
        if (op == 0.0 || op == 1.0) return 1.0;
        return 1.0 / op;
    }

    // ─────────────────────────────────────────────────────────────────
    // dispatch
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected SeamlessOutcome dispatch(GscRequest req) {
        // Invalid-JSON / null request → "Invalid JSON format" (legacy
        // RollbackProcess.java:73-75).
        if (req == null
                || (req.getOperatorCode() == null
                && req.getCurrency() == null
                && req.getTransactions().isEmpty()
                && req.getSign() == null
                && req.getRequestTime() == null
                && req.getMemberAccount() == null)) {
            return errorOutcome(SC_INTERNAL_SERVER_ERROR, "Invalid JSON format");
        }

        List<GscRequest.TransactionItem> transactions = req.getTransactions();
        // Required-parameter check (mirrors RollbackProcess.java:90-94).
        if (req.getOperatorCode() == null
                || req.getMemberAccount() == null
                || req.getProductCode() == null
                || req.getProductCode() == 0
                || req.getCurrency() == null
                || req.getGameType() == null
                || req.getSign() == null
                || req.getRequestTime() == null
                || transactions == null) {
            return errorOutcome(SC_INTERNAL_SERVER_ERROR, "Missing required parameters");
        }

        // Currency lookup. Unknown currency → empty BalanceResponse fall-back
        // (legacy RollbackProcess.java:105-109).
        int currencyFactor = config.getCurrencyExchangeRate(req.getCurrency());
        if (currencyFactor == 0) {
            return balanceFallbackOutcome();
        }

        String memberAccount = req.getMemberAccount();

        // Read before-balance for the response (legacy line 117 — Hazelcast
        // path). We use a typed IMap lookup with DB fall-back, mirroring
        // GscTransferAggregator.balanceForUser. INTERNAL units; the legacy
        // also returns before_balance in INTERNAL units (no exchange-rate
        // multiplication on line 189 — preserved verbatim).
        long beforeBalanceInternal = balanceForUser(memberAccount);

        // Dispatch each transaction in order. Per legacy: any failure
        // aborts the loop and returns the empty BalanceResponse shape
        // (RollbackProcess.java:128-133).
        for (GscRequest.TransactionItem t : transactions) {
            SeamlessOutcome perTxn = dispatchOne(req, t);
            if (!isSuccessful(perTxn)) {
                logger.warn("GscRollbackAggregator: txn failed, aborting batch."
                        + " id=" + (t == null ? "null" : t.getId())
                        + " status=" + (perTxn == null ? "null" : perTxn.status));
                return balanceFallbackOutcome();
            }
        }

        // After the loop, fetch the post-loop balance for the response.
        long afterBalanceInternal = balanceForUser(memberAccount);
        double beforeBalanceExternal = (double) beforeBalanceInternal;
        double afterBalanceExternal = currencyToExternal(afterBalanceInternal, req.getCurrency());

        // Legacy returns before_balance in INTERNAL units (line 189) but
        // after_balance multiplied by exchangeRateOut (line 184). We
        // preserve that asymmetry verbatim so the wire shape matches
        // operationally-captured Rollback responses (synthetic for now —
        // 0 events in last 30 days).
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.ROLLBACK);
        meta.put("code", SC_SUCCESS);
        meta.put("message", "Rollback processed successfully");
        meta.put("before_balance", beforeBalanceExternal);
        meta.put("balance", afterBalanceExternal);
        return SeamlessOutcome.posted(afterBalanceInternal, meta);
    }

    /**
     * Dispatch a single rollback transaction. Always a CREDIT (refund).
     * Returns POSTED or DUPLICATE on success; anything else aborts the
     * parent loop. Best-effort rebate-reversal + log_gsc_bets cleanup are
     * triggered AFTER a successful POSTED (not on DUPLICATE — those side
     * effects already ran on the original call).
     */
    private SeamlessOutcome dispatchOne(GscRequest req, GscRequest.TransactionItem t) {
        if (t == null) {
            return SeamlessOutcome.serverError(null, "null transaction");
        }
        String id = t.getId();
        if (id == null || id.isEmpty()) {
            logger.warn("GscRollbackAggregator: transaction missing id");
            return SeamlessOutcome.serverError(null, "missing transaction id");
        }

        // Resolve amount (in internal sub-units). Legacy line 120 uses
        // bet_amount specifically (getBet_amountNumber). We prefer
        // bet_amount, fall back to amount for defensive parity with
        // PushBet/Transfer.
        String amountRaw = t.getBetAmount() != null ? t.getBetAmount() : t.getAmount();
        double providerAmount;
        try {
            providerAmount = (amountRaw != null && !amountRaw.isEmpty())
                    ? Double.parseDouble(amountRaw) : 0.0;
        } catch (NumberFormatException e) {
            logger.warn("GscRollbackAggregator: bad bet_amount '" + amountRaw + "' for txn " + id);
            return SeamlessOutcome.serverError(null, "bad bet_amount");
        }

        long amountSubunit = currencyToInternal(providerAmount, req.getCurrency());
        if (amountSubunit <= 0L) {
            // Zero-amount txn: legacy reward() with amount=0 short-circuits
            // success in updateMoney. Treat as a NO-OP-success so the batch
            // continues and the response reflects an unchanged balance.
            logger.info("GscRollbackAggregator: zero-amount rollback id=" + id + " — skipped");
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("skipped", Boolean.TRUE);
            meta.put("reason", "zero-amount");
            return SeamlessOutcome.posted(0L, meta);
        }

        // External-ref namespacing: prefix by 'rollback_' so a prior BET /
        // SETTLED for the same id and the rollback hash to different rows
        // in money_gateway_log.uk_tx_source.
        String externalRef = "rollback_" + id;
        String action = t.getAction() != null ? t.getAction() : "ROLLBACK";

        SeamlessTxn txn = new SeamlessTxn(
                req.getMemberAccount(),
                externalRef,
                action,
                amountSubunit,
                req.getCurrency(),
                /*metadata*/null);

        SeamlessOutcome out = doCredit(txn);

        // Run the legacy post-credit side effects only when we actually
        // moved the wallet (POSTED). DUPLICATE means a retry — the side
        // effects already ran on the original call.
        if (out != null && out.status == SeamlessOutcome.Status.POSTED) {
            afterCredit(req, t);
        }
        return out;
    }

    /**
     * Best-effort post-credit side effects mirroring legacy
     * {@code RollbackProcess.java:134-181}:
     * <ol>
     *   <li>{@code RebateService.reverseGscByWagerCode} — reverse any
     *       agency-side commission rows from the original bet.
     *   <li>{@code log_gsc_bets} cleanup — drop the Mongo bet-history row
     *       so the rolled-back bet no longer surfaces in c=303 / c=9843.
     *   <li>Per-agent response-cache invalidation.
     * </ol>
     * All wrapped in defensive try/Throwable: a failure here must NOT
     * fail the wallet credit (which already posted successfully) — the
     * provider considers the rollback a success on a 200 response and
     * its own retry pipeline won't re-send.
     */
    private void afterCredit(GscRequest req, GscRequest.TransactionItem t) {
        String wagerCode = t.getWagerCode();
        if (wagerCode == null || wagerCode.trim().isEmpty()) {
            return;
        }

        // (1) RebateService.reverseGscByWagerCode — best effort.
        java.util.Set<Integer> agentsToInvalidate = java.util.Collections.emptySet();
        try {
            String prodCode = t.getProductCode() > 0
                    ? String.valueOf(t.getProductCode())
                    : null;
            agentsToInvalidate = com.vinplay.dal.rebate.RebateService
                    .reverseGscByWagerCode(prodCode, wagerCode, "rollback");
        } catch (Throwable revErr) {
            logger.warn("[SUN-1182] GscRollbackAggregator rebate reverse failed wager="
                    + wagerCode + " err=" + revErr.getMessage());
        }

        // (2) log_gsc_bets cleanup — async via RMQ (Phase 5 prep gate 5p2).
        // Telegram alert intentionally omitted: the legacy rollback
        // cleanup path logged + continued and never paged ops on Mongo
        // failure. Best-effort; the reconciler picks up missed
        // log_gsc_bets rows separately.
        GscBetSideEffectMessage rollSfx = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.ROLLBACK_DELETE,
                "GscRollback",
                req.getMemberAccount(),
                t.getProductCode(),
                t.getGameCode(),
                wagerCode,
                t.getId(),
                System.currentTimeMillis());
        rollSfx.telegramAlertSubject = null; // legacy parity: no alert
        publishBetSideEffect(rollSfx);

        // (3) Response-cache invalidation per agent.
        for (Integer agentId : agentsToInvalidate) {
            try {
                com.vinplay.vbee.common.cache.ResponseCacheHelper
                        .invalidateForAgent(agentId);
            } catch (Throwable cacheErr) {
                logger.warn("[SUN-1182] GscRollbackAggregator cache invalidate failed agentId="
                        + agentId + " err=" + cacheErr.getMessage());
            }
        }
    }

    /**
     * Phase 5 prep gate 5p2 — publish the bet side-effect (Mongo
     * cleanup) to {@code queue_log_gsc_bets_async} so it runs
     * asynchronously in {@code api/vbee}'s
     * {@code GscBetSideEffectProcessor}. {@code protected} so tests can
     * override and capture without touching RabbitMQ. Default delegates
     * to {@link GscBetSideEffectPublisher#publish}, which itself wraps
     * RMQ-failure with a sync-fallback so audit rows still land if the
     * broker is down.
     */
    protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
        GscBetSideEffectPublisher.publish(msg);
    }

    private static boolean isSuccessful(SeamlessOutcome out) {
        return out != null
                && (out.status == SeamlessOutcome.Status.POSTED
                || out.status == SeamlessOutcome.Status.DUPLICATE);
    }

    /**
     * Read a user's balance for the before/after numbers. Tries the
     * Hazelcast {@code users} cache first (legacy parity, sub-ms hot
     * path); falls back to the base-class DB read on cache miss /
     * unreachable cluster. Returns 0 if neither path resolves the user —
     * matches the legacy
     * {@code UserMoneyServiceImpl.getMoneyFromUsersMap} behavior.
     */
    private long balanceForUser(String memberAccount) {
        // SUN-1xxx (2026-05-11): MySQL-only, no cache. Per
        // docs/architecture/LEDGER_HARDENING_ROADMAP.md — balance reads
        // returned to external parties or used to gate money moves must
        // read the canonical store, not Hazelcast.
        if (memberAccount == null || memberAccount.isEmpty()) return 0L;
        try {
            SeamlessOutcome r = doReadBalance(memberAccount);
            if (r != null && r.status == SeamlessOutcome.Status.POSTED) {
                return r.newBalance;
            }
            return 0L;
        } catch (Throwable t) {
            logger.warn("GscRollbackAggregator.balanceForUser MySQL read failed user="
                    + memberAccount + " err=" + t.getMessage()
                    + " — returning 0 (no cache fallback per balance-read policy)");
            return 0L;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Outcome helpers
    // ─────────────────────────────────────────────────────────────────

    private static final int SC_SUCCESS               = 0;
    private static final int SC_INTERNAL_SERVER_ERROR = 999;
    private static final int SC_INVALID_SIGNATURE     = 1004;

    private static SeamlessOutcome errorOutcome(int code, String message) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.ROLLBACK);
        meta.put("code", code);
        meta.put("message", message);
        meta.put("before_balance", 0.0);
        meta.put("balance", 0.0);
        // Use validationError so postAudit marks the audit row FAILED instead
        // of COMPLETED — the wire shape comes from meta["shape"] which
        // serializeResponse reads, but the outcome status truthfully reflects
        // the validation failure (Phase 3b precedent: commit 16ae740a).
        return SeamlessOutcome.validationError(String.valueOf(code), message, meta);
    }

    /**
     * Empty {@code BalanceResponse} fall-back. Used in two cases (mirroring
     * legacy lines 105-108 and 130-132):
     * <ul>
     *   <li>Unknown currency code (currencyFactor==0).
     *   <li>Any per-transaction failure aborting the batch.
     * </ul>
     * Wire shape: {@code {"code":0,"message":"","data":[]}}.
     */
    private static SeamlessOutcome balanceFallbackOutcome() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.BALANCE_FALLBACK);
        meta.put("code", SC_SUCCESS);
        meta.put("message", "");
        return SeamlessOutcome.posted(0L, meta);
    }

    // ─────────────────────────────────────────────────────────────────
    // serializeResponse
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected String serializeResponse(SeamlessOutcome out) {
        if (out == null) return rollbackJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
        switch (out.status) {
            case SIGNATURE_ERROR:
                return rollbackJson(SC_INVALID_SIGNATURE, "Invalid sign", 0.0, 0.0);
            case SERVER_ERROR:
                return rollbackJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
            case POSTED:
            case DUPLICATE:
            case VALIDATION_ERROR: {
                // VALIDATION_ERROR shares the metadata-driven shape with
                // POSTED/DUPLICATE because errorOutcome (validation failure:
                // bad json, missing params, etc.) wraps its wire fields in
                // metadata. The status drives postAudit's FAILED/COMPLETED
                // mapping; the wire shape comes from metadata.
                Map<String, Object> meta = out.metadata;
                GscResponseShape shape = GscResponseShape.from(meta, GscResponseShape.ROLLBACK);
                if (shape == GscResponseShape.BALANCE_FALLBACK) {
                    return balanceFallbackJson();
                }
                int code = meta != null && meta.get("code") instanceof Integer
                        ? (Integer) meta.get("code") : SC_SUCCESS;
                String msg = meta != null && meta.get("message") instanceof String
                        ? (String) meta.get("message") : "";
                double before = meta != null && meta.get("before_balance") instanceof Number
                        ? ((Number) meta.get("before_balance")).doubleValue() : 0.0;
                double after = meta != null && meta.get("balance") instanceof Number
                        ? ((Number) meta.get("balance")).doubleValue() : 0.0;
                return rollbackJson(code, msg, before, after);
            }
            case USER_NOT_FOUND:
            case INSUFFICIENT_BALANCE:
            default:
                return rollbackJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
        }
    }

    /**
     * Serialize the legacy {@code BaseResponse}-shape JSON. Field order
     * (code / message / before_balance / balance) matches the legacy
     * Jackson-default declaration order so a string-compare against
     * captured production responses is stable. (Production captures don't
     * actually exist for rollback — 0 events in 30 days — but consistency
     * with TransferProcess and the documented {@code BaseResponse} field
     * declaration is the safer posture.)
     */
    private static String rollbackJson(int code, String message, double before, double balance) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("code", code);
        root.put("message", message == null ? "" : message);
        root.put("before_balance", before);
        root.put("balance", balance);
        return new JSONObject(root).toString();
    }

    private static String balanceFallbackJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("code", SC_SUCCESS);
        root.put("message", "");
        root.put("data", new JSONArray());
        return new JSONObject(root).toString();
    }

    // ─────────────────────────────────────────────────────────────────
    // audit hooks
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected long preAudit(String rawBody, HttpServletRequest http) {
        return GscEventLogger.tryLogRequest("rollback", rawBody, http);
    }

    @Override
    protected void postAudit(long auditId, SeamlessOutcome out, String responseJson) {
        if (auditId <= 0L) return;
        boolean ok = out != null
                && (out.status == SeamlessOutcome.Status.POSTED
                || out.status == SeamlessOutcome.Status.DUPLICATE);
        String lastError = (out == null || ok) ? null : out.errorMessage;
        GscEventLogger.tryLogResponse(auditId, ok ? "COMPLETED" : "FAILED",
                responseJson, lastError);
    }

    // ─────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] arr = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : arr) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
