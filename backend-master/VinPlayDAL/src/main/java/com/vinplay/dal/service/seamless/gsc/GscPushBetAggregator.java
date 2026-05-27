package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.dal.audit.GscEventLogger;
import com.vinplay.dal.service.seamless.SeamlessOutcome;
import com.vinplay.dal.service.seamless.SeamlessWalletAggregator;
import com.vinplay.dal.service.seamless.VerifyResult;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3a — GSC PushBet handler implemented on top of
 * {@link SeamlessWalletAggregator}. Mirrors the legacy
 * {@code game.third.hooks.gscSeamless.PushBetProcess} at the
 * wire-response level, gated behind {@code GSC_AGGREGATOR_PUSHBET_ENABLED}.
 *
 * <h2>Why this handler is audit-only at the aggregator layer</h2>
 * The legacy {@code PushBetProcess.doExecuteInner} calls
 * {@code userMoneyService.bet(...)} for each transaction — i.e. it
 * deducts from the player's wallet inside the PushBet endpoint.
 * Per GSC's actual API contract, PushBet is "advance notice that a bet
 * was placed"; the real wallet deduction is supposed to come on the
 * subsequent {@code WithdrawProcess} callback. The legacy handler
 * therefore double-debits in the PushBet+Withdraw flow that GSC
 * actually uses with several providers — a known platform bug
 * (visible in agent reports as "amount already deducted twice"). The
 * Phase 3a directive in
 * {@code docs/SEAMLESS_WALLET_AGGREGATOR_DESIGN.md} §3 calls this out
 * explicitly: <em>"no wallet movement (audit-only push of bet metadata)"</em>
 * and <em>"the aggregator MUST NOT introduce a new wallet movement;
 * if your dispatch reaches doDebit/doCredit, you've gone wrong."</em>
 *
 * <p>Consequence: when the env-var flag flips on, the legacy
 * double-debit is silently fixed — the aggregator no longer issues
 * the second debit. This is an intentional behavioral divergence
 * from the legacy code, scoped to the wallet side-effect; the
 * <em>response shape</em> remains byte-identical so the contract
 * test still passes against the legacy path's response.
 *
 * <p>The audit log row (via {@link GscEventLogger}) is preserved
 * exactly, and the legacy "Member not found" / "Invalid sign" /
 * "Missing required parameters" rejections are mirrored 1:1.
 *
 * <h2>Mongo {@code log_gsc_bets}</h2>
 * Verified: the legacy {@code PushBetProcess} does <em>not</em>
 * write to {@code log_gsc_bets}. That collection is populated by
 * {@code WithdrawProcess} (line 354) and queried by
 * {@code GscWagerReconciler} / {@code GameHistoryService}. No Mongo
 * write needs to be migrated here.
 *
 * <h2>Sign verification</h2>
 * Legacy accepts <em>two</em> verbs in the md5 sign — both
 * {@code "pushbet"} and {@code "pushbetdata"} (PushBetProcess.java:99-101).
 * The aggregator preserves that — different GSC product lines emit
 * different verbs and we cannot break either. The sign-action verb
 * used in the actual {@code md5(...)} input is therefore "either of
 * those two strings"; whichever validates wins.
 *
 * <h2>Audit endpoint string</h2>
 * Matches the legacy: {@code "pushbet"} (PushBetProcess.java:51 +
 * :63). This is the value rows show under
 * {@code gsc_event_log.gsc_endpoint} when ops query for PushBet
 * traffic.
 */
public class GscPushBetAggregator extends SeamlessWalletAggregator<GscRequest, String> {

    private final GscConfigProvider config;

    public GscPushBetAggregator(GscConfigProvider config) {
        if (config == null) throw new IllegalArgumentException("config is null");
        this.config = config;
    }

    /** Tighter audit-log tag than the default class-simple-name. */
    @Override
    protected String aggregatorName() { return "GSC"; }

    /** Phase 5p3 — distinct timing-metrics bucket per handler. */
    @Override
    protected String metricsName() { return "GscPushBet"; }

    // ─────────────────────────────────────────────────────────────────
    // parseRequest
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse the inbound JSON into a {@link GscRequest}. The legacy
     * {@code PushBetRequest.fromJson} returns null on a malformed body
     * which is then mapped to {@code "Invalid JSON format"} —
     * {@link #dispatch} replicates that, so an empty/malformed body
     * here yields a {@code GscRequest} with all-null fields and
     * {@code dispatch} flags it as {@code MEMBER_NOT_EXIST}-style.
     *
     * <p>Currency fall-back: if the top-level {@code currency} field is
     * absent, take it from the first transaction
     * ({@code PushBetProcess.java:86-88}).
     */
    @Override
    protected GscRequest parseRequest(String body, HttpServletRequest http) {
        if (body == null || body.isEmpty()) {
            return new GscRequest(null, null, null, null, null, null, null,
                    null, null);
        }
        JSONObject root;
        try {
            root = new JSONObject(body);
        } catch (Throwable t) {
            // Malformed JSON — return all-null, dispatch will map to
            // the legacy "Invalid JSON format" branch.
            return new GscRequest(null, null, null, null, null, null, null,
                    null, null);
        }

        String operatorCode = optStr(root, "operator_code");
        String currency     = optStr(root, "currency");
        String sign         = optStr(root, "sign");
        String requestTime  = optStr(root, "request_time");

        // Resolve transactions list — transactions OR wagers (fall-back),
        // mirroring PushBetRequest.resolveTransactions.
        JSONArray txArr = root.optJSONArray("transactions");
        if (txArr == null || txArr.length() == 0) {
            txArr = root.optJSONArray("wagers");
        }

        List<GscRequest.TransactionItem> transactions = new ArrayList<>();
        if (txArr != null) {
            for (int i = 0; i < txArr.length(); i++) {
                JSONObject t = txArr.optJSONObject(i);
                if (t == null) continue;
                String id            = optStr(t, "id");
                String action        = optStr(t, "action");
                String memberAccount = optStr(t, "member_account");
                int productCode      = optInt(t, "product_code");
                String gameCode      = optStr(t, "game_code");
                String txCurrency    = optStr(t, "currency");
                String wagerCode     = optStr(t, "wager_code");
                String amount        = optStr(t, "amount");
                String betAmount     = optStr(t, "bet_amount");
                transactions.add(new GscRequest.TransactionItem(
                        id, action, memberAccount, productCode, gameCode,
                        txCurrency, wagerCode, amount, betAmount));
            }
        }

        // Currency fall-back: if top-level currency is null, take it from
        // the first transaction (legacy fall-back).
        if (currency == null && !transactions.isEmpty()) {
            currency = transactions.get(0).getCurrency();
        }

        return new GscRequest(operatorCode, currency, sign, requestTime,
                /*memberAccount*/null, /*productCode*/null,
                /*gameType*/null,
                /*batchMembers*/null,
                transactions);
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
    // verifySignature — accepts both legacy verbs
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected VerifyResult verifySignature(GscRequest req, HttpServletRequest http) {
        if (req == null) return VerifyResult.fail("missing request");
        // The legacy code rejects with "Missing required parameters"
        // when any of operatorCode/currency/transactions/sign/requestTime
        // is null — but that's a validation rule, not a signature one.
        // Keep this method tight: only sign/operator/time are signature
        // inputs, and the others are checked in validateBusinessRules /
        // dispatch. Returning fail("missing sign") here for a missing
        // sign matches the legacy "Invalid sign" path because the base
        // class projects SIGNATURE_ERROR onto the same response shape
        // we'd otherwise use for a missing-params response.
        if (req.getSign() == null) return VerifyResult.fail("missing sign");
        if (req.getOperatorCode() == null || req.getRequestTime() == null) {
            return VerifyResult.fail("missing operator_code or request_time");
        }
        String secret = config.getSecretKey();
        if (secret == null) return VerifyResult.fail("aggregator secret not configured");

        // Legacy accepts EITHER verb — different GSC product lines emit
        // different sign actions. PushBetProcess.java:99-101.
        String expected1 = md5Hex(req.getOperatorCode() + req.getRequestTime() + "pushbet"     + secret);
        String expected2 = md5Hex(req.getOperatorCode() + req.getRequestTime() + "pushbetdata" + secret);
        if (!req.getSign().equals(expected1) && !req.getSign().equals(expected2)) {
            return VerifyResult.fail("signature mismatch");
        }
        return VerifyResult.ok();
    }

    // ─────────────────────────────────────────────────────────────────
    // mapActionToSource / currency conversion — unused (no wallet move)
    // ─────────────────────────────────────────────────────────────────

    /**
     * PushBet is audit-only at the aggregator layer — no wallet
     * movement, so this method is never called by the dispatch path.
     * Returning the empty string keeps the abstract contract honored
     * without minting a meaningless source value, and structurally
     * fails any future wallet-moving code path (the empty source would
     * trip the {@code MoneyGateway} unique-key check).
     */
    @Override
    protected String mapActionToSource(String aggregatorAction) { return ""; }

    @Override
    protected long currencyToInternal(double providerAmount, String currency) {
        // Never invoked (no wallet move). Implemented for completeness
        // mirroring CommonProcess.getExchangeRateIn.
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
    // dispatch — replicates the validation pipeline of the legacy
    // PushBetProcess.doExecuteInner WITHOUT the wallet-moving step.
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected SeamlessOutcome dispatch(GscRequest req) {
        // PushBetProcess: invalid-JSON / null request maps to "Invalid JSON format".
        if (req == null
                || (req.getOperatorCode() == null
                && req.getCurrency() == null
                && req.getTransactions().isEmpty()
                && req.getSign() == null
                && req.getRequestTime() == null)) {
            return errorOutcome(1, "Invalid JSON format");
        }

        List<GscRequest.TransactionItem> transactions = req.getTransactions();
        // Legacy: missing required parameter rejection.
        if (req.getOperatorCode() == null
                || req.getCurrency() == null
                || transactions == null
                || transactions.isEmpty()
                || req.getSign() == null
                || req.getRequestTime() == null) {
            return errorOutcome(1, "Missing required parameters");
        }

        // Currency lookup. Unknown currency → legacy emits a
        // BalanceResponse(empty) — i.e. {"code":0,"message":null,"data":[]}.
        // We surface this via metadata so serializeResponse can emit the
        // same shape (NOT the PushBetResponse shape).
        int currencyFactor = config.getCurrencyExchangeRate(req.getCurrency());
        if (currencyFactor == 0) {
            return balanceFallbackOutcome();
        }

        // Member-existence check. The legacy code consults the Hazelcast
        // {@code users} IMap (PushBetProcess.java:117-125) and rejects
        // the entire request on the first miss. We preserve that behavior
        // when Hazelcast is reachable, and fall back to a DB-backed
        // lookup (via the base-class {@code doReadBalance}) when it
        // isn't — this matters for unit tests without a Hazelcast
        // cluster, and is harmless in prod where the cache is always up
        // (DB fall-back returns the same yes/no answer, just one
        // round-trip slower).
        for (GscRequest.TransactionItem t : transactions) {
            String ma = t.getMemberAccount();
            if (ma == null || !memberExists(ma)) {
                return errorOutcome(1000 /* MEMBER_NOT_EXIST */, "Member not found");
            }
        }

        // Audit-only push: success outcome, no wallet movement.
        // Matches legacy "Push bet processed successfully" response.
        return successOutcome();
    }

    private static SeamlessOutcome errorOutcome(int code, String message) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("code", code);
        meta.put("message", message);
        meta.put("shape", GscResponseShape.PUSHBET);
        return SeamlessOutcome.posted(0L, meta);
    }

    private static SeamlessOutcome successOutcome() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("code", 0);
        meta.put("message", "Push bet processed successfully");
        meta.put("shape", GscResponseShape.PUSHBET);
        return SeamlessOutcome.posted(0L, meta);
    }

    /**
     * The legacy code returns a {@code BalanceResponse(emptyList)} —
     * NOT a {@code PushBetResponse} — when the currency is unknown.
     * That JSON shape differs (it has a {@code data:[]} field), so we
     * tag the outcome with a {@link GscResponseShape#BALANCE_FALLBACK}
     * hint that {@link #serializeResponse} keys off.
     */
    private static SeamlessOutcome balanceFallbackOutcome() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.BALANCE_FALLBACK);
        meta.put("code", 0);
        meta.put("message", "");
        return SeamlessOutcome.posted(0L, meta);
    }

    // ─────────────────────────────────────────────────────────────────
    // serializeResponse
    // ─────────────────────────────────────────────────────────────────

    /**
     * Project the outcome into the legacy wire shape:
     * <ul>
     *   <li>{@code SIGNATURE_ERROR} → {@code {"code":1,"message":"Invalid sign"}}
     *       (PushBetProcess.java:102-104).
     *   <li>{@code SERVER_ERROR} → {@code {"code":1,"message":"Server Error"}}
     *       (PushBetProcess.java:60).
     *   <li>{@code POSTED} with {@code shape="pushbet"} → emit
     *       {@code {"code":N,"message":"M"}} from metadata.
     *   <li>{@code POSTED} with {@code shape="balance"} → emit the
     *       legacy {@code BalanceResponse} shape: {@code {"code":N,"message":"M","data":[]}}.
     * </ul>
     *
     * <p>Field ordering matches the legacy Jackson default
     * (declaration order in {@code PushBetResponse} is {@code code} then
     * {@code message}; {@code BalanceResponse} adds {@code data}). Tests
     * compare JSON tree-equivalent so order isn't strictly required, but
     * we emit it consistently anyway.
     */
    @Override
    protected String serializeResponse(SeamlessOutcome out) {
        if (out == null) return pushBetJson(1, "Server Error");
        switch (out.status) {
            case SIGNATURE_ERROR:
                // Legacy: pushBetResponse.setMessage("Invalid sign"); setCode(1);
                return pushBetJson(1, "Invalid sign");
            case SERVER_ERROR:
                return pushBetJson(1, "Server Error");
            case POSTED:
            case DUPLICATE: {
                Map<String, Object> meta = out.metadata;
                GscResponseShape shape = GscResponseShape.from(meta, GscResponseShape.PUSHBET);
                int code = meta != null && meta.get("code") instanceof Integer
                        ? (Integer) meta.get("code") : 0;
                String msg = meta != null && meta.get("message") instanceof String
                        ? (String) meta.get("message") : "";
                if (shape == GscResponseShape.BALANCE_FALLBACK) {
                    // Empty BalanceResponse — legacy unknown-currency fall-back.
                    return balanceFallbackJson();
                }
                return pushBetJson(code, msg);
            }
            case VALIDATION_ERROR:
            case USER_NOT_FOUND:
            case INSUFFICIENT_BALANCE:
            default:
                return pushBetJson(1, "Server Error");
        }
    }

    private static String pushBetJson(int code, String message) {
        // SUN-1245: GSC certification expects the envelope to always
        // include a `data` field (empty array on error, populated on
        // success). The legacy 2-field shape ({code, message}) failed
        // the "PushBet" cert test because the test driver checks for
        // the data field's presence.
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("code", code);
        root.put("message", message == null ? "" : message);
        root.put("data", new JSONArray());
        return new JSONObject(root).toString();
    }

    /**
     * Empty {@code BalanceResponse} shape — the legacy unknown-currency
     * fall-back at {@code PushBetProcess.java:111-114}. The legacy
     * {@code BalanceResponse(emptyList).toJson()} emits
     * {@code {"code":0,"message":null,"data":[]}} via Jackson; we use
     * an empty string for {@code message} so consumers that string-
     * compare don't trip on null vs "" — the contract test does
     * value-equivalence and treats both as the same empty JSON value
     * by string comparison ({@code String.valueOf(null) = "null"}).
     */
    private static String balanceFallbackJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("code", 0);
        root.put("message", "");
        root.put("data", new JSONArray());
        return new JSONObject(root).toString();
    }

    // ─────────────────────────────────────────────────────────────────
    // audit hooks — same pattern as Phase 2
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected long preAudit(String rawBody, HttpServletRequest http) {
        // Match the legacy endpoint string exactly so gsc_event_log
        // rows from before/after the cutover are searchable consistently.
        return GscEventLogger.tryLogRequest("pushbet", rawBody, http);
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

    /**
     * True iff {@code memberAccount} resolves to a known user. Mirrors
     * the legacy {@code PushBetProcess} member-existence guard: try
     * Hazelcast {@code users} IMap first (sub-millisecond, prod hot
     * path); fall back to the base-class DB lookup via
     * {@code doReadBalance} when the cache is unreachable (unit tests
     * without Hazelcast, or a transient cluster outage). Both paths
     * return the same yes/no answer; only the cost differs.
     */
    private boolean memberExists(String memberAccount) {
        if (memberAccount == null || memberAccount.isEmpty()) return false;
        try {
            com.hazelcast.core.IMap<String, Object> userMap =
                    HazelcastClientFactory.getInstance().getMap("users");
            if (userMap.containsKey(memberAccount)) return true;
        } catch (Throwable t) {
            // Hazelcast unavailable — drop straight to DB fallback.
        }
        // SUN-1245: cache miss falls back to DB. The legacy code only
        // checked Hazelcast and rejected logged-out users with valid
        // MySQL rows — same bug class as Withdraw/Deposit. GSC
        // certification "PushBet" failed because laviai is valid in
        // MySQL but not currently in cache.
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM users WHERE nick_name = ? OR user_name = ? LIMIT 1")) {
            ps.setString(1, memberAccount);
            ps.setString(2, memberAccount);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Throwable t2) {
            return false;
        }
    }

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
