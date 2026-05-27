package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.dal.audit.GscEventLogger;
import com.vinplay.dal.service.seamless.SeamlessOutcome;
import com.vinplay.dal.service.seamless.SeamlessWalletAggregator;
import com.vinplay.dal.service.seamless.VerifyResult;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 2 POC — read-only GSC balance handler implemented on top of
 * {@link SeamlessWalletAggregator}. Mirrors the legacy
 * {@code game.third.hooks.gscSeamless.BalanceProcess} byte-for-byte at the
 * wire-response level, gated behind {@code GSC_AGGREGATOR_BALANCE_ENABLED}.
 *
 * <p><b>Why read-only first?</b> The base-class template proves out without
 * any wallet-movement risk. A regression here can only return a wrong
 * balance number; it cannot move money. See
 * {@code docs/SEAMLESS_WALLET_AGGREGATOR_DESIGN.md} §3 phase 2.
 *
 * <h2>Configuration</h2>
 * The aggregator does NOT reach into {@code game.third.usecase.*} — that
 * would invert the module dependency direction (thirdParty depends on
 * VinPlayDAL today). Instead the caller hands in a {@link GscConfigProvider}
 * holder at construction time, which the legacy {@code BalanceProcess}
 * builds from {@code ThirdPartyLoad.getGscConfig()} per request. This
 * keeps the module graph clean while still letting the aggregator pick
 * up ops-time changes to {@code GSCConfig}.
 *
 * <h2>Currency conversion</h2>
 * Mirrors {@code CommonProcess.getExchangeRateOut} exactly:
 * <ul>
 *   <li>currencies whose enum {@code exchangeRate} {@code > 1} (IDR2, KRW2,
 *       VND2, …) divide by that integer factor — e.g. KRW2 = 1000;
 *   <li>otherwise the operator-level config rate is used (often 1.0).
 * </ul>
 * The legacy code rounds to 4 decimal places via
 * {@code Math.round(d * 10000.0) / 10000.0}; we reproduce that exactly so
 * the contract test asserts byte equality on the response body.
 *
 * <h2>Wallet primitive used</h2>
 * Only {@link #doReadBalance(String)} — never {@code doDebit}/{@code doCredit}.
 * That is the structural guarantee that this Phase 2 POC cannot move
 * money even if a future bug rewrites {@code dispatch}.
 */
public class GscBalanceAggregator extends SeamlessWalletAggregator<GscRequest, String> {

    /**
     * MoneyService abstraction so the aggregator can stay in
     * {@code VinPlayDAL} and not pull in the
     * {@code game.third.usecase.service.UserMoneyService} concrete that
     * lives in {@code thirdParty}. The base-class
     * {@link #doReadBalance(String)} reads from {@code users.vin}
     * directly which is what the legacy code's
     * {@code userMoneyService.getBalance} resolves to anyway, so this
     * interface is currently unused — kept here for parity with future
     * deposit/withdraw aggregators that DO need it.
     */
    public interface MoneyService {
        long getBalance(String username);
    }

    private final GscConfigProvider config;

    public GscBalanceAggregator(GscConfigProvider config) {
        if (config == null) throw new IllegalArgumentException("config is null");
        this.config = config;
    }

    /** Tighter audit-log tag than the default class-simple-name. */
    @Override
    protected String aggregatorName() { return "GSC"; }

    /** Phase 5p3 — distinct timing-metrics bucket per handler. */
    @Override
    protected String metricsName() { return "GscBalance"; }

    // ─────────────────────────────────────────────────────────────────
    // parseRequest / verifySignature
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected GscRequest parseRequest(String body, HttpServletRequest http) {
        if (body == null || body.isEmpty()) {
            // Empty body parses to an all-null request — verifySignature
            // will reject it. Avoid throwing here; the legacy path returns
            // an empty response, not a 500.
            return new GscRequest(null, null, null, null, null, null, null, null);
        }
        JSONObject root;
        try {
            root = new JSONObject(body);
        } catch (Throwable t) {
            // Malformed JSON; let signature verification fail naturally
            // (sign will be null) — don't throw, the legacy path returns
            // an empty BalanceResponse for this.
            return new GscRequest(null, null, null, null, null, null, null, null);
        }
        String operatorCode = root.optString("operator_code", null);
        String currency     = root.optString("currency", null);
        String sign         = root.optString("sign", null);
        String requestTime  = root.optString("request_time", null);
        String memberAccount = root.optString("member_account", null);
        String gameType      = root.optString("game_type", null);
        Integer productCode  = null;
        if (root.has("product_code") && !root.isNull("product_code")) {
            try {
                productCode = root.getInt("product_code");
            } catch (Throwable ignored) {
                try { productCode = Integer.parseInt(root.getString("product_code").trim()); }
                catch (Throwable ignored2) { /* leave null */ }
            }
        }

        List<GscRequest.BatchMember> batch = new ArrayList<>();
        JSONArray batchArr = root.optJSONArray("batch_requests");
        if (batchArr != null) {
            for (int i = 0; i < batchArr.length(); i++) {
                JSONObject b = batchArr.optJSONObject(i);
                if (b == null) continue;
                String mAcc = b.optString("member_account", null);
                int pCode = 0;
                if (b.has("product_code") && !b.isNull("product_code")) {
                    try { pCode = b.getInt("product_code"); }
                    catch (Throwable ignored) {
                        try { pCode = Integer.parseInt(b.getString("product_code").trim()); }
                        catch (Throwable ignored2) { /* leave 0 */ }
                    }
                }
                batch.add(new GscRequest.BatchMember(mAcc, pCode));
            }
        }
        return new GscRequest(operatorCode, currency, sign, requestTime,
                memberAccount, productCode, gameType, batch);
    }

    @Override
    protected VerifyResult verifySignature(GscRequest req, HttpServletRequest http) {
        if (req == null || req.getSign() == null) return VerifyResult.fail("missing sign");
        if (req.getOperatorCode() == null || req.getRequestTime() == null) {
            return VerifyResult.fail("missing operator_code or request_time");
        }
        String secret = config.getSecretKey();
        if (secret == null) return VerifyResult.fail("aggregator secret not configured");

        // Legacy uses HashUtil.md5(operator + requestTime + "getbalance" + secret).
        // The endpoint label "getbalance" matches the current BalanceProcess
        // line 87 — distinct from the deposit / withdraw / cancel endpoint
        // labels in the design table and on each per-endpoint Process class.
        String expected = md5Hex(req.getOperatorCode() + req.getRequestTime() + "getbalance" + secret);
        if (!req.getSign().equals(expected)) {
            return VerifyResult.fail("signature mismatch");
        }
        return VerifyResult.ok();
    }

    // ─────────────────────────────────────────────────────────────────
    // mapActionToSource
    // ─────────────────────────────────────────────────────────────────

    /**
     * Balance is read-only — no wallet movement, so this method is never
     * actually called by the read-only path ({@code doReadBalance} does
     * not consult it). Returning the empty string keeps the abstract
     * contract honored without minting a meaningless source value.
     *
     * <p>If a future change ever causes {@code dispatch} to reach
     * {@code doDebit}/{@code doCredit}, the empty source will fail loudly
     * at the {@code MoneyGateway} unique-key check — desired behavior, since
     * a wallet-moving balance handler is by definition a bug.
     */
    @Override
    protected String mapActionToSource(String aggregatorAction) {
        return "";
    }

    // ─────────────────────────────────────────────────────────────────
    // currency conversion — mirrors CommonProcess exactly
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected long currencyToInternal(double providerAmount, String currency) {
        // Legacy actionBet: long amount = (long)((double)bet / getExchangeRateIn(currency))
        // Used during BET path. Balance is read-only so this is never invoked,
        // but we implement it for completeness so non-balance endpoints could
        // reuse the conversion later.
        double rateIn = exchangeRateIn(currency);
        if (rateIn == 0.0) rateIn = 1.0;
        return Math.round(providerAmount / rateIn);
    }

    @Override
    protected double currencyToExternal(long internalBalance, String currency) {
        // Legacy BalanceProcess: balance * getExchangeRateOut(currency).
        // Returned as a double so subclass response serialization can echo
        // the raw double; the 4-decimal rounding is applied in
        // serializeResponse where it's actually emitted.
        return (double) internalBalance * exchangeRateOut(currency);
    }

    /** Mirrors {@code CommonProcess.getExchangeRateIn(GSC.CurrencyCode)}. */
    private double exchangeRateIn(String currency) {
        int per = config.getCurrencyExchangeRate(currency);
        if (per > 1) return (double) per;
        double op = config.getOperatorExchangeRate();
        if (op == 0.0 || op == 1.0) return 1.0;
        return op;
    }

    /** Mirrors {@code CommonProcess.getExchangeRateOut(GSC.CurrencyCode)}. */
    private double exchangeRateOut(String currency) {
        int per = config.getCurrencyExchangeRate(currency);
        if (per > 1) return 1.0 / (double) per;
        double op = config.getOperatorExchangeRate();
        if (op == 0.0 || op == 1.0) return 1.0;
        return 1.0 / op;
    }

    // ─────────────────────────────────────────────────────────────────
    // dispatch — the per-endpoint logic
    // ─────────────────────────────────────────────────────────────────

    /**
     * Loop the batch_requests list, build a per-member result, and stash
     * everything into {@link SeamlessOutcome#metadata} under the keys
     * {@code "items"} (List of Map per legacy {@code BalanceResponseItem}),
     * {@code "topCode"}, and {@code "topMessage"} so
     * {@link #serializeResponse} can project to the wire JSON shape.
     *
     * <p>Returns POSTED outcome with newBalance=0 on success — balance is
     * not represented at the outcome level (it's per-item in metadata).
     */
    @Override
    protected SeamlessOutcome dispatch(GscRequest req) {
        // 1. Currency code lookup — legacy: GSC.CurrencyCode.findByName.
        //    Treat unknown currency the same way (empty data, code 0, message "").
        int currencyFactor = config.getCurrencyExchangeRate(req.getCurrency());
        // 0 = unknown currency in the Config contract.
        if (currencyFactor == 0) {
            return outcomeWith(new ArrayList<Map<String, Object>>(), 0, "");
        }

        List<GscRequest.BatchMember> batch = req.getBatchMembers();
        if (batch == null || batch.isEmpty()) {
            return outcomeWith(new ArrayList<Map<String, Object>>(), 0, "");
        }

        double exchange = exchangeRateOut(req.getCurrency());
        List<Map<String, Object>> items = new ArrayList<>();

        for (GscRequest.BatchMember m : batch) {
            if (m == null) continue;
            String memberAccount = m.getMemberAccount();
            int productCode = m.getProductCode();
            if (memberAccount == null || memberAccount.isEmpty() || productCode == 0) {
                // Legacy: silently drops these from the response (continue).
                continue;
            }

            SeamlessOutcome read = doReadBalance(memberAccount);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("member_account", memberAccount);
            item.put("product_code", productCode);
            if (read.status == SeamlessOutcome.Status.USER_NOT_FOUND) {
                item.put("code", 1000);
                item.put("balance", 0.0);
                item.put("message", "Member not found");
            } else if (read.status == SeamlessOutcome.Status.POSTED) {
                long rawBalance = read.newBalance;
                double balanceD = Math.round((double) rawBalance * exchange * 10000.0) / 10000.0;
                item.put("code", 0);
                item.put("balance", balanceD);
                item.put("message", "");
            } else {
                // SERVER_ERROR or anything unexpected: surface as code 1000 +
                // empty balance — same as the legacy "member not in cache"
                // branch — to avoid leaking internal state to GSC.
                item.put("code", 1000);
                item.put("balance", 0.0);
                item.put("message", "Member not found");
            }
            items.add(item);
        }

        // Legacy: if every item is non-zero code, hoist the first item's
        // (code, message) to the top level — otherwise top-level stays
        // (0, "").
        boolean allInvalid = !items.isEmpty();
        for (Map<String, Object> it : items) {
            Object c = it.get("code");
            if (c instanceof Integer && ((Integer) c).intValue() == 0) {
                allInvalid = false;
                break;
            }
        }
        int topCode = 0;
        String topMessage = "";
        if (allInvalid) {
            Object c = items.get(0).get("code");
            Object msg = items.get(0).get("message");
            if (c instanceof Integer) topCode = ((Integer) c).intValue();
            if (msg instanceof String) topMessage = (String) msg;
        }
        return outcomeWith(items, topCode, topMessage);
    }

    private static SeamlessOutcome outcomeWith(List<Map<String, Object>> items,
                                               int topCode,
                                               String topMessage) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("items", items);
        meta.put("topCode", topCode);
        meta.put("topMessage", topMessage);
        return SeamlessOutcome.posted(0L, meta);
    }

    // ─────────────────────────────────────────────────────────────────
    // serializeResponse
    // ─────────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    protected String serializeResponse(SeamlessOutcome out) {
        // Map non-POSTED outcomes (signature error, server error) onto the
        // legacy fall-back shape: empty data array, top-level code 0,
        // message "" — except SIGNATURE_ERROR which legacy maps to code
        // 1004 / "Invalid sign" (BalanceProcess.java:90-92).
        if (out == null) {
            return wireJson(0, "", new ArrayList<Map<String, Object>>());
        }
        switch (out.status) {
            case SIGNATURE_ERROR:
                return wireJson(1004, "Invalid sign", new ArrayList<Map<String, Object>>());
            case POSTED:
            case DUPLICATE: {
                Map<String, Object> meta = out.metadata;
                List<Map<String, Object>> items = meta != null && meta.get("items") instanceof List
                        ? (List<Map<String, Object>>) meta.get("items")
                        : new ArrayList<Map<String, Object>>();
                int topCode = meta != null && meta.get("topCode") instanceof Integer
                        ? (Integer) meta.get("topCode") : 0;
                String topMessage = meta != null && meta.get("topMessage") instanceof String
                        ? (String) meta.get("topMessage") : "";
                return wireJson(topCode, topMessage, items);
            }
            case VALIDATION_ERROR:
            case USER_NOT_FOUND:
            case INSUFFICIENT_BALANCE:
            case SERVER_ERROR:
            default:
                return wireJson(0, "", new ArrayList<Map<String, Object>>());
        }
    }

    /**
     * Build the GSC balance wire shape:
     * {@code {"code":N,"message":"…","data":[{member_account,product_code,balance,code,message}, …]}}.
     *
     * <p>Field ordering is preserved across the JSON object via
     * {@link LinkedHashMap}, matching the gson default the legacy code
     * uses (alphabetical = field declaration order in
     * {@code BalanceResponseItem}). The contract test compares
     * normalized JSON trees, so field-order divergence between the two
     * paths is tolerated even if a server's gson differs from
     * org.json's.
     */
    private static String wireJson(int code, String message, List<Map<String, Object>> items) {
        // org.json preserves insertion order for JSONObject when a
        // LinkedHashMap is provided.
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("code", code);
        root.put("message", message == null ? "" : message);
        JSONArray arr = new JSONArray();
        for (Map<String, Object> it : items) {
            // Field order matching legacy BalanceResponseItem declaration.
            Map<String, Object> ordered = new LinkedHashMap<>();
            ordered.put("member_account", it.get("member_account"));
            ordered.put("product_code", it.get("product_code"));
            ordered.put("balance", it.get("balance"));
            ordered.put("code", it.get("code"));
            ordered.put("message", it.get("message") == null ? "" : it.get("message"));
            arr.put(new JSONObject(ordered));
        }
        root.put("data", arr);
        return new JSONObject(root).toString();
    }

    // ─────────────────────────────────────────────────────────────────
    // audit hooks
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected long preAudit(String rawBody, HttpServletRequest http) {
        return GscEventLogger.tryLogRequest("balance", rawBody, http);
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
