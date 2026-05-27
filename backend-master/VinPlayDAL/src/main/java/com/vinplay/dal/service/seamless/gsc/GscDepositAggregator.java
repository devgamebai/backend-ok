package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.dal.audit.GscEventLogger;
import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.dal.service.seamless.SeamlessOutcome;
import com.vinplay.dal.service.seamless.SeamlessTxn;
import com.vinplay.dal.service.seamless.SeamlessWalletAggregator;
import com.vinplay.dal.service.seamless.VerifyResult;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.GscBetSideEffectMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.statics.Consts;
import com.hazelcast.core.IMap;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3e — GSC Deposit handler implemented on top of
 * {@link SeamlessWalletAggregator}. Mirrors the legacy
 * {@code game.third.hooks.gscSeamless.DepositProcess} at the wire-response
 * level, gated behind {@code GSC_AGGREGATOR_DEPOSIT_ENABLED}.
 *
 * <h2>Production volume — handle with care</h2>
 * Verified ~846 events/day (peak ~1 TPS sustained) on the {@code deposit}
 * endpoint over the last 7 days. ZERO duplicate-wager-in-5sec deposits
 * observed — GSC does NOT retry settle events under healthy conditions.
 * The {@code money_gateway_log.uk_tx_source(tx_id, source, user_id)}
 * UNIQUE remains the structural dedup gate so a retried settle is a
 * no-op at the wallet layer regardless.
 *
 * <h2>Direction — always CREDIT</h2>
 * Every deposit transaction credits the player: a normal SETTLED is the
 * winnings payout; a cancel-via-deposit (Hash Game's SUN-1182 quirk
 * where action=CANCEL or wager_status in {CANCEL,REFUND,VOID} arrives
 * on this endpoint) is a refund. Both ways the wallet primitive is
 * {@link SeamlessWalletAggregator#doCredit}; only the post-credit
 * side-effects diverge.
 *
 * <p><b>No legacy bug to preserve.</b> {@code DepositProcess} does NOT
 * route through {@code CommonProcess.TransactionAction} — it calls
 * {@code userMoneyService.reward(...)} directly (line 244), which is
 * the correct CREDIT direction. So unlike Phase 3b (which corrected
 * the {@code actionReward}-calls-{@code bet} bug), this is structurally
 * a refactor only — the wire shape and wallet effect must match legacy
 * byte-for-byte.
 *
 * <h2>External-ref namespacing</h2>
 * The {@code money_gateway_log} UNIQUE on {@code (tx_id, source, user_id)}
 * means a normal SETTLED and a cancel-via-deposit for the same wager
 * MUST hash to different rows. Strategy:
 * <ul>
 *   <li>Normal settle: {@code "deposit_" + transaction.id} — uses
 *       provider's stable per-event id.</li>
 *   <li>Cancel-via-deposit: {@code "deposit_cancel_" + wager_code} —
 *       prefix differs from normal settle so a defense-in-depth case
 *       where both events somehow arrive (which shouldn't happen) won't
 *       collapse onto the same row. Different from Phase 3c's
 *       {@code "cancel_<wager_code>"} so a Cancel-endpoint replay and a
 *       cancel-via-deposit replay also don't collide.</li>
 * </ul>
 *
 * <h2>Side-effects preserved (the SUN-tickets)</h2>
 * <ol>
 *   <li><b>SUN-888 / fish-game amount override</b> — when
 *       {@link GscDepositProviderHooks#isFishGame} matches any txn in the
 *       batch, every credit amount is overridden to {@code prize_amount}
 *       (legacy lines 178-188).</li>
 *   <li><b>SUN-865 / SUN-1201 rebate action_name</b> —
 *       {@code "gsc_<pc>_<game_code>"} preferred, with
 *       {@link com.vinplay.dal.service.BetContextResolver} session-recovery
 *       and {@link com.vinplay.dal.service.GscProductMapService} fall-back
 *       (legacy lines 190-220). NOTE: in the aggregator path the wallet
 *       call goes through {@code MoneyGateway.creditUser} which doesn't
 *       fire the rebate pipeline — but {@code RebateService} sees the
 *       eventual settle row via the Mongo write below; the resolved
 *       game-code is stamped into {@code event_key} on the settle update
 *       so c=303 / c=9843 attribution stays correct.</li>
 *   <li><b>SUN-1182 / cancel-via-deposit cleanup</b> — when
 *       {@link GscDepositProviderHooks#isCancelLikeDeposit} matches:
 *       (a) drop the {@code log_gsc_bets} row by wager_code; (b)
 *       {@code RebateService.reverseGscByWagerCode}; (c) skip the
 *       settle update entirely (legacy lines 255-285).</li>
 *   <li><b>SUN-LIVE-HIST / settle update</b> — for normal settle, do a
 *       Mongo {@code $inc prize / $set settled=true / $addToSet
 *       settle_txn_ids} on the {@code log_gsc_bets} row (legacy lines
 *       287-381).</li>
 *   <li><b>SUN-1196 / freespin-chain routing</b> — when
 *       {@link GscDepositProviderHooks#resolveLinkId} returns a non-null
 *       parent_round_id, route the prize $inc to the BUY row by
 *       {@code (user_name, product_code, vendor_game_id)} so the
 *       freespin's win lands on the parent BUY row (legacy lines
 *       298-343).</li>
 *   <li><b>SUN-1108/1110 / MongoRetry wrapper</b> — settle update
 *       wrapped in {@code MongoRetry.runWithRetry(...)} (legacy lines
 *       375-381).</li>
 *   <li><b>SUN-1184 / free-spin row cleanup</b> — after the settle
 *       update, drop any {@code bet_value=0} row matching the same
 *       filter (legacy lines 383-402).</li>
 *   <li><b>Telegram alert on Mongo failure</b> —
 *       {@link TelegramOpsNotifier#alertGscBetWriteFailure} fires only
 *       when the Mongo settle path throws (legacy lines 403-413).
 *       Signature / validation failures do NOT fire it.</li>
 * </ol>
 *
 * <p><b>{@code afterCredit} is best-effort.</b> Every Mongo / Rebate /
 * Telegram side-effect runs inside the {@code afterCredit} hook only
 * when the wallet credit actually posted (POSTED, not DUPLICATE). On
 * DUPLICATE the side-effects already ran on the original call. On
 * POSTED, the side-effects' failure is logged but NEVER fails the
 * wallet credit — the provider considers a 200 the source of truth and
 * its retry pipeline will not re-send. Mirrors Phase 3d's pattern.
 *
 * <h2>Sign verification</h2>
 * Single verb {@code "deposit"}: {@code md5(operator_code + request_time
 * + "deposit" + secret)}. Verified against {@code DepositProcess.java:101}.
 *
 * <h2>Audit endpoint string</h2>
 * Matches the legacy: {@code "deposit"} ({@code DepositProcess.java:55}).
 *
 * <h2>Response shape</h2>
 * Legacy {@code DepositResponse.toJson()} uses a hand-rolled string
 * builder (NOT Jackson) that wraps the (code,message,before_balance,
 * balance) tuple inside a {@code data:[{...}]} envelope:
 * <pre>
 *   {"code":N,"message":"...","data":[{"code":N,"message":"...",
 *     "before_balance":B,"balance":B}]}
 * </pre>
 * Numbers are formatted via {@code DecimalFormat("0.####")} — no
 * trailing {@code .0} on integer values. We reproduce both quirks
 * verbatim in {@link #depositJson} so a string-compare against captured
 * production responses is stable.
 *
 * <p>Unknown-currency / batch-failure fall-back uses the empty
 * {@link game.third.hooks.gscSeamless.response.BalanceResponse} shape:
 * {@code {"code":0,"message":"","data":[]}} — the SAME envelope key
 * ({@code data}) but the array is empty rather than holding one item.
 */
public class GscDepositAggregator extends SeamlessWalletAggregator<GscRequest, String> {

    // Bounded executor for post-credit side effects. Mirrors GscWithdrawAggregator's
    // AFTER_DEBIT_EXECUTOR — the wallet commit is already done before we
    // submit, so the HTTP response returns immediately. CallerRunsPolicy
    // is the saturation fallback so rebate/log invariants never silently
    // drop, only regress to sync.
    private static final ExecutorService AFTER_CREDIT_EXECUTOR;
    private static final AtomicInteger AFTER_CREDIT_INFLIGHT = new AtomicInteger();
    private static final AtomicLong AFTER_CREDIT_SUBMITTED = new AtomicLong();
    private static final AtomicLong AFTER_CREDIT_FELL_BACK_SYNC = new AtomicLong();
    static {
        int core = parseEnvInt("GSC_AFTER_CREDIT_CORE_THREADS", 4, 1, 64);
        int max  = parseEnvInt("GSC_AFTER_CREDIT_MAX_THREADS",  16, core, 128);
        int q    = parseEnvInt("GSC_AFTER_CREDIT_QUEUE_SIZE",   5000, 100, 100000);
        AFTER_CREDIT_EXECUTOR = new ThreadPoolExecutor(
                core, max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(q),
                new ThreadFactory() {
                    private final AtomicInteger n = new AtomicInteger();
                    @Override public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "gsc-after-credit-" + n.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
                },
                (r, ex) -> {
                    AFTER_CREDIT_FELL_BACK_SYNC.incrementAndGet();
                    r.run();
                });
    }

    private static int parseEnvInt(String name, int def, int min, int max) {
        try {
            String v = System.getenv(name);
            if (v == null || v.isEmpty()) return def;
            int n = Integer.parseInt(v.trim());
            if (n < min) return min;
            if (n > max) return max;
            return n;
        } catch (Exception e) { return def; }
    }

    private final GscConfigProvider config;
    private final GscDepositProviderHooks.Resolver providerResolver;

    public GscDepositAggregator(GscConfigProvider config) {
        this(config, GscDepositProviderHooks.Resolver.DEFAULT);
    }

    public GscDepositAggregator(GscConfigProvider config,
                                GscDepositProviderHooks.Resolver providerResolver) {
        if (config == null) throw new IllegalArgumentException("config is null");
        this.config = config;
        this.providerResolver = providerResolver != null
                ? providerResolver
                : GscDepositProviderHooks.Resolver.DEFAULT;
    }

    /** Tighter audit-log tag than the default class-simple-name. */
    @Override
    protected String aggregatorName() { return "GSC"; }

    /** Phase 5p3 — distinct timing-metrics bucket per handler. */
    @Override
    protected String metricsName() { return "GscDeposit"; }

    // ─────────────────────────────────────────────────────────────────
    // parseRequest
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse the inbound JSON into a {@link GscRequest}. Mirrors the
     * legacy {@code DepositRequest.fromJson} + {@code resolveMemberAccount}
     * / {@code resolveProductCode} / {@code resolveTransactions} chain:
     * if the top-level fields are absent, fall back to the first
     * {@code batch_requests[0]} entry. ALL production traffic uses
     * the {@code batch_requests} envelope (verified across 850+ rows
     * in {@code gsc_event_log}); the top-level path is preserved for
     * defensive parity with the legacy {@code DepositRequest} DTO.
     */
    @Override
    protected GscRequest parseRequest(String body, HttpServletRequest http) {
        if (body == null || body.isEmpty()) {
            return emptyRequest();
        }
        JSONObject root;
        try {
            root = new JSONObject(body);
        } catch (Throwable t) {
            return emptyRequest();
        }

        String operatorCode  = optStr(root, "operator_code");
        String currency      = optStr(root, "currency");
        String sign          = optStr(root, "sign");
        String requestTime   = optStr(root, "request_time");
        String gameType      = optStr(root, "game_type");
        // Legacy uses Integer 0 sentinel for "unset" (DepositRequest.product_code is int).
        Integer topProductCode = optIntBoxed(root, "product_code");
        String topMemberAccount = optStr(root, "member_account");

        // batch_requests fall-back. Legacy resolveMemberAccount /
        // resolveProductCode / resolveTransactions: pick top-level if set,
        // else batch_requests[0].
        JSONObject batch0 = null;
        JSONArray batchArr = root.optJSONArray("batch_requests");
        if (batchArr != null && batchArr.length() > 0) {
            batch0 = batchArr.optJSONObject(0);
        }

        String memberAccount = topMemberAccount;
        Integer productCode  = (topProductCode != null && topProductCode > 0) ? topProductCode : null;
        if ((memberAccount == null || memberAccount.isEmpty()) && batch0 != null) {
            memberAccount = optStr(batch0, "member_account");
        }
        if (productCode == null && batch0 != null) {
            int bp = optInt(batch0, "product_code");
            if (bp > 0) productCode = bp;
        }
        if ((gameType == null || gameType.isEmpty()) && batch0 != null) {
            String bg = optStr(batch0, "game_type");
            if (bg != null) gameType = bg;
        }

        // Transactions: top-level transactions array first, else batch_requests[0].transactions.
        JSONArray txArr = root.optJSONArray("transactions");
        if (txArr == null && batch0 != null) {
            txArr = batch0.optJSONArray("transactions");
        }

        List<GscRequest.TransactionItem> transactions = new ArrayList<>();
        if (txArr != null) {
            int effectiveProductCode = productCode != null ? productCode : 0;
            for (int i = 0; i < txArr.length(); i++) {
                JSONObject t = txArr.optJSONObject(i);
                if (t == null) continue;
                String id            = optStr(t, "id");
                String action        = optStr(t, "action");
                String gameCode      = optStr(t, "game_code");
                String txCurrency    = optStr(t, "currency");
                String wagerCode     = optStr(t, "wager_code");
                // amount/bet_amount may arrive as JSON number or string.
                String amount        = optNumberAsString(t, "amount");
                String betAmount     = optNumberAsString(t, "bet_amount");
                String wagerStatus   = optStr(t, "wager_status");
                String prizeAmount   = optNumberAsString(t, "prize_amount");
                String validBetAmount = optNumberAsString(t, "valid_bet_amount");
                String roundId       = optStr(t, "round_id");
                int txProductCode    = t.has("product_code") ? optInt(t, "product_code") : effectiveProductCode;
                Object payload       = t.has("payload") && !t.isNull("payload") ? t.opt("payload") : null;
                Object payloadAsMap  = jsonValueToJavaMap(payload);
                transactions.add(new GscRequest.TransactionItem(
                        id, action, memberAccount, txProductCode, gameCode,
                        txCurrency, wagerCode, amount, betAmount,
                        wagerStatus, prizeAmount, validBetAmount, roundId, payloadAsMap));
            }
        }

        return new GscRequest(operatorCode, currency, sign, requestTime,
                memberAccount, productCode, gameType,
                /*batchMembers*/ null, transactions);
    }

    private static GscRequest emptyRequest() {
        return new GscRequest(null, null, null, null, null, null, null,
                null, Collections.<GscRequest.TransactionItem>emptyList());
    }

    /**
     * Recursively convert a JSON-object/array value tree (as produced by
     * org.json.JSONObject) into a plain Java {@code Map}/{@code List}
     * tree. {@link com.vinplay.dal.service.seamless.gsc.GscDepositProviderHooks#resolveLinkId}
     * (and its {@code ProviderAdapter} default impls) walk the payload
     * via {@code firstNestedMap(payload)} which uses {@code instanceof Map}
     * — JSONObject does not implement Map, so we project to plain Maps.
     */
    private static Object jsonValueToJavaMap(Object v) {
        if (v == null || v == JSONObject.NULL) return null;
        if (v instanceof JSONObject) {
            JSONObject jo = (JSONObject) v;
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            for (String k : jo.keySet()) {
                out.put(k, jsonValueToJavaMap(jo.opt(k)));
            }
            return out;
        }
        if (v instanceof JSONArray) {
            JSONArray ja = (JSONArray) v;
            List<Object> out = new ArrayList<>(ja.length());
            for (int i = 0; i < ja.length(); i++) {
                out.add(jsonValueToJavaMap(ja.opt(i)));
            }
            return out;
        }
        return v; // primitive
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

    private static Integer optIntBoxed(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return null;
        try { return o.getInt(key); }
        catch (Throwable ignored) {
            try { return Integer.parseInt(o.getString(key).trim()); }
            catch (Throwable ignored2) { return null; }
        }
    }

    /**
     * Read a numeric or string-encoded value as a String. Production
     * payloads encode amounts as strings ({@code "amount": "40000"}) but
     * defensive: tolerate JSON-number form too.
     */
    private static String optNumberAsString(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return null;
        try {
            String s = o.optString(key, null);
            if (s != null && !s.isEmpty()) return s;
        } catch (Throwable ignored) {}
        try {
            double d = o.getDouble(key);
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // verifySignature
    // ─────────────────────────────────────────────────────────────────

    /**
     * Single verb {@code "deposit"}. Verified against
     * {@code DepositProcess.java:101}: {@code HashUtil.md5(operatorCode +
     * requestTime + "deposit" + secretKey)}.
     */
    @Override
    protected VerifyResult verifySignature(GscRequest req, HttpServletRequest http) {
        if (req == null) return VerifyResult.fail("missing request");
        if (req.getSign() == null) return VerifyResult.fail("missing sign");
        if (req.getOperatorCode() == null || req.getRequestTime() == null) {
            return VerifyResult.fail("missing operator_code or request_time");
        }
        String secret = config.getSecretKey();
        if (secret == null) return VerifyResult.fail("aggregator secret not configured");

        String expected = md5Hex(req.getOperatorCode() + req.getRequestTime() + "deposit" + secret);
        if (!req.getSign().equals(expected)) {
            return VerifyResult.fail("signature mismatch");
        }
        return VerifyResult.ok();
    }

    // ─────────────────────────────────────────────────────────────────
    // mapActionToSource / currency conversion
    // ─────────────────────────────────────────────────────────────────

    /**
     * Deposit is unconditionally a CREDIT — both normal SETTLED and
     * cancel-via-deposit are wallet additions. The action verb carries
     * informational value at the audit layer but the wallet direction
     * is fixed.
     */
    @Override
    protected String mapActionToSource(String aggregatorAction) {
        return MoneyGateway.SOURCE_GSC_CREDIT;
    }

    @Override
    protected long currencyToInternal(double providerAmount, String currency) {
        double rateIn = exchangeRateIn(currency);
        if (rateIn == 0.0) rateIn = 1.0;
        // Mirror legacy line 176: Math.round(Math.abs(amount) * rateIn).
        return Math.round(Math.abs(providerAmount) * rateIn);
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
    // dispatch — the per-transaction loop
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected SeamlessOutcome dispatch(GscRequest req) {
        // Invalid-JSON / null request → 999 INTERNAL_SERVER_ERROR
        // "Invalid JSON format" (legacy lines 75-78).
        if (req == null
                || (req.getOperatorCode() == null
                && req.getCurrency() == null
                && req.getTransactions().isEmpty()
                && req.getSign() == null
                && req.getRequestTime() == null
                && req.getMemberAccount() == null
                && req.getProductCode() == null)) {
            return errorOutcome(SC_INTERNAL_SERVER_ERROR, "Invalid JSON format");
        }

        List<GscRequest.TransactionItem> transactions = req.getTransactions();
        // Required-parameter check (legacy lines 92-96). NOTE: legacy
        // does NOT include game_type in this check (different from
        // Withdraw / Rollback / Transfer). Preserve verbatim.
        if (req.getOperatorCode() == null
                || req.getMemberAccount() == null
                || req.getProductCode() == null
                || req.getProductCode() == 0
                || req.getCurrency() == null
                || req.getSign() == null
                || req.getRequestTime() == null
                || transactions == null) {
            // Legacy returns 1002 INCORRECT_AGENT_KEY here (line 94).
            return errorOutcome(SC_INCORRECT_AGENT_KEY, "Missing required parameters");
        }

        // Currency lookup. Unknown → empty BalanceResponse fall-back
        // (legacy lines 112-116).
        int currencyFactor = config.getCurrencyExchangeRate(req.getCurrency());
        if (currencyFactor == 0) {
            return balanceFallbackOutcome();
        }

        String memberAccount = req.getMemberAccount();
        int productCode = req.getProductCode();

        // SUN-888 fish-game detection: if ANY transaction game_code is
        // a fish game, every credit amount in the batch flips to its
        // prize_amount (legacy lines 165-173).
        GscDepositProviderHooks provider = providerResolver.forProduct(productCode);
        if (provider == null) provider = GscDepositProviderHooks.DEFAULT;
        boolean batchHasFish = false;
        for (GscRequest.TransactionItem t : transactions) {
            if (provider.isFishGame(t.getGameCode())) {
                batchHasFish = true;
                break;
            }
        }

        // CANCEL-action wager_code existence check — only applies when
        // a transaction's action is "CANCEL". Legacy lines 130-142 use
        // the Hazelcast IMap "gsc_wager_codes". When Hazelcast is
        // unreachable we skip the check (test harness / cluster
        // outage); same conservative posture as Phase 3c.
        //
        // SUN-1245: GSC certification "Withdraw and Cancel" requires the
        // 1006 error response to carry the member's CURRENT balance in
        // both data.balance and data.before_balance — not 0. Read the
        // balance once before the loop so we can stamp it on any
        // "Bet not exist" error.
        long preCheckBalance = balanceForUser(memberAccount);
        double preCheckExternal = preCheckBalance * exchangeRateOut(req.getCurrency());
        for (GscRequest.TransactionItem t : transactions) {
            String act = t.getAction();
            if (act != null && act.equalsIgnoreCase("CANCEL")) {
                String wc = t.getWagerCode();
                if (wc == null || !wagerCodeExists(wc)) {
                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("shape", GscResponseShape.DEPOSIT);
                    meta.put("code", SC_BET_NOT_EXIST);
                    meta.put("message", "Bet not exist");
                    meta.put("before_balance", preCheckExternal);
                    meta.put("balance", preCheckExternal);
                    return SeamlessOutcome.validationError(
                            String.valueOf(SC_BET_NOT_EXIST), "Bet not exist", meta);
                }
            }
        }

        // Member existence check (legacy lines 144-150). The aggregator's
        // doCredit also checks via the SQL lookup, but the legacy probe
        // is on the Hazelcast users IMap and produces a distinct error
        // code (1000 MEMBER_NOT_EXIST) so we mirror it here. Hazelcast
        // unavailable → skip (same posture as the wager check above).
        if (!memberAccountExists(memberAccount)) {
            return errorOutcome(SC_MEMBER_NOT_EXIST, "Member not found");
        }

        // Read before-balance for the response (legacy line 155).
        long beforeBalanceInternal = balanceForUser(memberAccount);

        // Dispatch each transaction in order. Legacy aborts the loop
        // on the first failed credit and returns INTERNAL_SERVER_ERROR
        // 999 "Server Error" (lines 249-253) — NOT the empty balance
        // fall-back. Preserve the distinction.
        //
        // SUN-1245: track DUPLICATE separately. The wallet primitive
        // (MoneyGateway.creditUser) idempotently rejects retried
        // tx_ids via the (tx_id, source) UNIQUE constraint. GSC
        // certification "Withdraw and Cancel Multiple Time" expects a
        // second cancel of the same wager_code to surface code 1003
        // "Duplicate Transaction" rather than a fresh success.
        boolean anyDuplicate = false;
        for (GscRequest.TransactionItem t : transactions) {
            SeamlessOutcome perTxn = dispatchOne(req, t, provider, batchHasFish);
            if (!isSuccessful(perTxn)) {
                logger.warn("GscDepositAggregator: txn failed, aborting batch."
                        + " action=" + (t == null ? "null" : t.getAction())
                        + " id=" + (t == null ? "null" : t.getId())
                        + " status=" + (perTxn == null ? "null" : perTxn.status));
                return errorOutcome(SC_INTERNAL_SERVER_ERROR, "Server Error");
            }
            if (perTxn != null && perTxn.status == SeamlessOutcome.Status.DUPLICATE) {
                anyDuplicate = true;
            }
        }

        // After the loop: post-loop balance for response.
        long afterBalanceInternal = balanceForUser(memberAccount);
        // Legacy line 424-425: BOTH before_balance and balance multiplied
        // by getExchangeRateOut. (Different from Cancel/Rollback which
        // leave before_balance in INTERNAL units. Deposit's wire shape
        // is symmetric.)
        double beforeBalanceExternal = (double) beforeBalanceInternal * exchangeRateOut(req.getCurrency());
        double afterBalanceExternal  = currencyToExternal(afterBalanceInternal, req.getCurrency());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.DEPOSIT);
        if (anyDuplicate) {
            meta.put("code", SC_DUPLICATE_TRANSACTION);
            meta.put("message", "Duplicate Transaction");
        } else {
            meta.put("code", SC_SUCCESS);
            meta.put("message", "Deposit processed successfully");
        }
        meta.put("before_balance", beforeBalanceExternal);
        meta.put("balance", afterBalanceExternal);
        return SeamlessOutcome.posted(afterBalanceInternal, meta);
    }

    /**
     * Dispatch a single deposit transaction. Always a CREDIT direction;
     * post-credit side-effects diverge by cancel-vs-settle.
     */
    private SeamlessOutcome dispatchOne(GscRequest req,
                                        GscRequest.TransactionItem t,
                                        GscDepositProviderHooks provider,
                                        boolean batchHasFish) {
        if (t == null) {
            return SeamlessOutcome.serverError(null, "null transaction");
        }
        String id = t.getId();
        String action = t.getAction();
        String wagerStatus = t.getWagerStatus();
        boolean isCancelLike = provider.isCancelLikeDeposit(action, wagerStatus);

        // Compute amount (legacy line 176).
        double providerAmount = t.getAmountNumber();
        long amountSubunit = currencyToInternal(providerAmount, req.getCurrency());

        // SUN-888: CQ9 fish-game amount override. Substitutes the credit
        // amount with prize_amount when prize_amount is non-negative AND
        // strictly smaller than the raw amount (legacy lines 178-188).
        if (batchHasFish && t.getPrizeAmount() != null) {
            try {
                float prizeAmt = Float.parseFloat(t.getPrizeAmount());
                if (prizeAmt >= 0 && Math.abs(prizeAmt) < Math.abs(providerAmount)) {
                    long override = currencyToInternal(prizeAmt, req.getCurrency());
                    logger.info("[GSC-DEBUG] CQ9 FISH Deposit OVERRIDE amount to prize_amount: " + override);
                    amountSubunit = override;
                }
            } catch (Exception ignored) {
                // Same swallow as legacy line 187.
            }
        }

        SeamlessOutcome out;
        if (amountSubunit <= 0L) {
            // No money to credit (e.g. lose bet, free-spin trigger, or
            // refund-shaped settle). Wallet stays put — legacy reward()
            // with amount=0 short-circuits success in updateMoney, so we
            // skip the wallet primitive entirely. BUT the wager IS
            // settled — fall through to afterCredit so log_gsc_bets gets
            // marked settled=true, prize=0 within seconds. Without this,
            // lose bets only become visible in the agency-portal betting
            // history when GscWagerReconciler eventually back-fills
            // settled=true via GSC's 3.3 API (minutes of latency on the
            // wrong path). The settle update is independent of whether
            // the wallet actually moved — what matters is the wager landed.
            //
            // Synthesize a POSTED outcome so the post-credit side-effect
            // block below treats this the same as a winning settle. The
            // newBalance=0 carried here is informational; the response
            // path reads the user's current balance via balanceForUser
            // outside the per-txn loop.
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("skipped", Boolean.TRUE);
            meta.put("reason", "zero-amount");
            out = SeamlessOutcome.posted(0L, meta);
            // Pass through to the post-credit side effects below.
        } else {
            // External-ref namespacing.
            // Normal settle:    "deposit_<id>"
            // Cancel-via-dep:   "deposit_cancel_<wager_code>"  (different from Phase 3c "cancel_*")
            String externalRef;
            if (isCancelLike) {
                String wc = t.getWagerCode();
                if (wc == null || wc.isEmpty()) {
                    // No wager_code on a cancel-via-deposit means we have
                    // nothing to dedup against. Fall back to the txn id —
                    // still race-safe at the wallet layer because uk_tx_source
                    // protects, but logged as a quirk.
                    externalRef = "deposit_cancel_id_" + (id != null ? id : "");
                    logger.warn("GscDepositAggregator: cancel-via-deposit without wager_code, id=" + id);
                } else {
                    externalRef = "deposit_cancel_" + wc.trim();
                }
            } else {
                if (id == null || id.isEmpty()) {
                    logger.warn("GscDepositAggregator: settle transaction missing id, action=" + action);
                    return SeamlessOutcome.serverError(null, "missing transaction id");
                }
                externalRef = "deposit_" + id;
            }

            SeamlessTxn txn = new SeamlessTxn(
                    req.getMemberAccount(),
                    externalRef,
                    isCancelLike ? "CANCEL" : (action != null ? action : "DEPOSIT"),
                    amountSubunit,
                    req.getCurrency(),
                    /*metadata*/ null);

            out = doCredit(txn);
        }

        // Run post-credit side effects only when the wallet primitive
        // (or the synthetic zero-amount POSTED above) reported success.
        // DUPLICATE means a retry — side effects already ran on the
        // original call; running them again would re-reverse a rebate
        // that's already reversed (RebateService is idempotent so it's
        // safe, but skipping is cleaner).
        //
        // SUN-gsc-lose-latency: the zero-amount path also hits POSTED
        // here so afterCredit runs and publishes SETTLE_UPDATE for lose
        // bets — keeping agency-portal latency at seconds instead of
        // the GscWagerReconciler's minutes-grace back-fill.
        if (out != null && out.status == SeamlessOutcome.Status.POSTED) {
            // Snapshot for the executor lambda.
            final GscRequest reqRef = req;
            final GscRequest.TransactionItem tRef = t;
            final GscDepositProviderHooks providerRef = provider;
            final boolean isCancelLikeRef = isCancelLike;
            final long amt = amountSubunit;
            final long postBal = out.newBalance;
            AFTER_CREDIT_SUBMITTED.incrementAndGet();
            AFTER_CREDIT_INFLIGHT.incrementAndGet();
            AFTER_CREDIT_EXECUTOR.execute(() -> {
                try {
                    afterCredit(reqRef, tRef, providerRef, isCancelLikeRef, amt, postBal);
                } catch (Throwable afterErr) {
                    logger.warn("GscDepositAggregator: afterCredit threw (non-fatal): "
                            + afterErr.getMessage());
                } finally {
                    AFTER_CREDIT_INFLIGHT.decrementAndGet();
                }
            });
        }
        return out;
    }

    /**
     * Best-effort post-credit side effects. Two branches:
     * <ul>
     *   <li><b>cancel-via-deposit</b>: drop {@code log_gsc_bets} row +
     *       {@code RebateService.reverseGscByWagerCode} (legacy lines
     *       255-285). Skip the settle update entirely.</li>
     *   <li><b>normal settle</b>: settle update on {@code log_gsc_bets}
     *       (legacy lines 287-381) + free-spin row cleanup (legacy
     *       lines 383-402). Wrapped in {@link
     *       com.vinplay.vbee.common.mongodb.MongoRetry#runWithRetry}.
     *       On failure: WARN log + Telegram alert.</li>
     * </ul>
     */
    private void afterCredit(GscRequest req,
                             GscRequest.TransactionItem t,
                             GscDepositProviderHooks provider,
                             boolean isCancelLike,
                             long amountSubunit,
                             long postCreditBalance) {
        String memberAccount = req.getMemberAccount();
        int productCode = req.getProductCode();

        // ── cancel-via-deposit branch ────────────────────────────────
        if (isCancelLike) {
            String wagerCode = t.getWagerCode();
            if (wagerCode == null || wagerCode.trim().isEmpty()) {
                return;
            }
            // (1) drop log_gsc_bets row by wager_code — async via RMQ
            // (Phase 5 prep gate 5p2). Telegram alert intentionally
            // omitted: the legacy cancel-cleanup path logged + continued
            // and never paged ops on Mongo failure.
            GscBetSideEffectMessage cancelSfx = GscBetSideEffectMessage.of(
                    GscBetSideEffectMessage.Op.CANCEL_DELETE,
                    "GscDeposit",
                    memberAccount,
                    productCode,
                    t.getGameCode(),
                    wagerCode,
                    t.getId(),
                    System.currentTimeMillis());
            cancelSfx.telegramAlertSubject = null; // legacy parity: no alert
            publishBetSideEffect(cancelSfx);
            // (2) RebateService.reverseGscByWagerCode (legacy lines 275-281).
            // Stays synchronous — touches MySQL (rebate_logs) which is
            // the same connection pool the wallet UPDATE used; this is
            // not a Mongo backoff hazard. Future work: lift to RMQ if
            // the rebate-reverse query latency becomes a hot-path issue.
            try {
                com.vinplay.dal.rebate.RebateService
                        .reverseGscByWagerCode(String.valueOf(productCode), wagerCode, "deposit-cancel");
            } catch (Throwable rebateErr) {
                logger.warn("[SUN-1182] GscDepositAggregator rebate reverse failed wager="
                        + wagerCode + " err=" + rebateErr.getMessage());
            }
            // Skip settle update — legacy "continue" at line 284.
            return;
        }

        // ── normal settle branch ──────────────────────────────────────
        // Legacy game_code resolution chain (lines 192-220). The
        // resolved game_code is stamped into event_key on the settle
        // update so c=303 / c=9843 attribution matches the legacy.
        String txGameCode = t.getGameCode();
        try {
            com.vinplay.dal.service.BetContextResolver.Context betCtx =
                    com.vinplay.dal.service.BetContextResolver.resolve(
                            memberAccount, productCode, txGameCode);
            if (betCtx != null && betCtx.hasGameCode()) {
                txGameCode = betCtx.gameCode;
            }
        } catch (Throwable ignored) {
            // BetContextResolver is best-effort; on failure use the raw
            // txGameCode (which may be empty for lobby-style providers
            // — same posture as legacy where the resolver also returns
            // a FALLBACK context with empty gameCode).
        }

        // Rebate action_name resolution (legacy DepositProcess lines 326-350).
        // Consumed below by both:
        //   (a) the {@code event_key} stamp on the {@code log_gsc_bets} settle
        //       update — keeps c=303 / c=9843 attribution stable, and
        //   (b) the {@link LogMoneyUserMessage#actionName} field published to
        //       {@code queue_log_money_user_extra} so the agency-commission
        //       pipeline ({@code LogMoneyUserExtraProcessor}) generates the
        //       per-bet commission rows.
        //
        // (b) is the post-spec-review fix: legacy {@code userMoneyService.reward}
        // routes through {@code UserServiceImpl.updateMoney} which builds and
        // publishes the {@code LogMoneyUserMessage}. {@code MoneyGateway
        // .creditUser} (the aggregator's wallet primitive) does NOT — so
        // without the explicit publish below, the agency commission generation
        // silently breaks for SETTLED traffic when the flag is on.
        String rebateActionName;
        if (productCode > 0 && txGameCode != null && !txGameCode.isEmpty()) {
            rebateActionName = "gsc_" + productCode + "_" + txGameCode;
        } else {
            String fallback = productCode > 0
                    ? safeResolveGameKey(productCode, txGameCode)
                    : null;
            rebateActionName = (fallback != null && !fallback.isEmpty())
                    ? fallback
                    : Games.LIVE_CASINO.getName();
        }

        // SUN-LIVE-HIST settle update — async via RMQ (Phase 5 prep gate 5p2).
        // The Mongo settle update + SUN-1184 free-spin row cleanup +
        // Telegram alert all run in api/vbee's GscBetSideEffectProcessor
        // off the request hot path. SUN-1196 freespin-chain routing
        // preserved via the linkRoundId field.
        String wagerCode = t.getWagerCode();
        String eventKey = buildGscBetEventKey(productCode, txGameCode, wagerCode, t.getId());

        String linkRoundId = null;
        try {
            linkRoundId = provider.resolveLinkId(t.getPayload(), null);
        } catch (Throwable ignored) { /* defensive */ }
        if (linkRoundId != null && !linkRoundId.isEmpty()) {
            logger.info("[SUN-1196] freespin settle → BUY row vgid="
                    + linkRoundId + " spin_wager=" + wagerCode
                    + " prize_to_add=" + amountSubunit);
        }

        GscBetSideEffectMessage settleSfx = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.SETTLE_UPDATE,
                "GscDeposit",
                memberAccount,
                productCode,
                txGameCode,
                wagerCode,
                t.getId(),
                System.currentTimeMillis());
        settleSfx.prize = amountSubunit;
        settleSfx.eventKey = eventKey;
        settleSfx.linkRoundId = linkRoundId;
        settleSfx.gameKey = rebateActionName;
        settleSfx.currency = req.getCurrency();
        // SUN-1367 — preserve provider decimal precision at the input
        // boundary. Parse raw payload strings (which retain the .15 /
        // .40 etc.) with BigDecimal × currencyFactor × 1000 → lossless
        // long milli-subunit. Downstream Mongo `*_milli` sister fields
        // are read by `GameHistoryService.fetchOneMongoSource` to show
        // 2-decimal precision matching the vendor iframe. Single
        // mapping at the top — no provider-specific ×1000 branches.
        int currencyFactor = config.getCurrencyExchangeRate(req.getCurrency());
        if (currencyFactor <= 0) currencyFactor = 1;
        // Real Dream wire emits both `prize_amount` and `amount` (with the
        // same decimal value on settle); fall back to amount if prize_amount
        // is absent so the milli sister stays populated end-to-end. The
        // legacy integer `prize` field in the message is already set from
        // `amountSubunit` above, which is computed from the same source.
        String rawPrize = t.getPrizeAmount();
        if (rawPrize == null || rawPrize.isEmpty()) rawPrize = t.getAmount();
        settleSfx.prizeMilli = parseRawToMilliSubunit(rawPrize, currencyFactor);
        settleSfx.validBetAmountMilli = parseRawToMilliSubunit(t.getValidBetAmount(), currencyFactor);
        settleSfx.amountMilli = parseRawToMilliSubunit(t.getAmount(), currencyFactor);
        // SUN-1248 / Phase 2: stamp post-settle balance so the Mongo doc
        // carries current_money on the SETTLE_UPDATE side too. Powers
        // money_after in the agency view without the supplement walk-back.
        settleSfx.currentMoneyAfter = postCreditBalance;
        // Mirror legacy alert wording byte-for-byte ("deposit settleUpdate").
        settleSfx.telegramAlertSubject = "deposit settleUpdate";
        publishBetSideEffect(settleSfx);

        // SUN-1370 — providers that opt in via
        // {@link GscDepositProviderHooks#postsCommissionAtSettle()}
        // (today: Dream Gaming productCode=1052) ship the truthful
        // {@code valid_bet_amount} on the SETTLE push. Inline-post the
        // deferred rebate now instead of waiting up to 10 minutes for
        // the scheduled reconciler tick. Reconciler stays armed as a
        // safety net via the drift_audit dedup row this call writes.
        //
        // MUST run BEFORE the `amountSubunit <= 0L` early-return below
        // — loss-shaped settles (prize=0) still have a real valid_bet
        // (the player's staked-and-lost amount) and need commission
        // posted just like win-shaped settles. The earlier placement
        // missed every Dream loss, leaving motminhanhdoimai's loss
        // wager `i5ECLfFQ4yYdajnjfHYiBn` (100K stake, 0 prize) with no
        // rolling row until the 5-min reconciler caught it.
        //
        // Best-effort: failure here must NOT propagate. Wallet credit
        // is already committed; reconciler will retry on its 5-min
        // cadence if the inline path missed.
        try {
            if (provider != null && provider.postsCommissionAtSettle()) {
                String rawValidBet = t.getValidBetAmount();
                if (rawValidBet != null && !rawValidBet.isEmpty()) {
                    double parsed;
                    try {
                        parsed = Double.parseDouble(rawValidBet);
                    } catch (Throwable parseErr) {
                        parsed = 0.0;
                    }
                    if (parsed > 0.0) {
                        long validBetSubunit = currencyToInternal(parsed, req.getCurrency());
                        if (validBetSubunit > 0L) {
                            com.vinplay.dal.service.GscWagerReconciler.postFromSettlePayload(
                                    productCode,
                                    t.getWagerCode(),
                                    memberAccount,
                                    rebateActionName,
                                    validBetSubunit);
                        }
                    }
                }
            }
        } catch (Throwable inlineErr) {
            logger.warn("GscDepositAggregator: inline commission post failed wager="
                    + t.getWagerCode() + " member=" + memberAccount
                    + " err=" + inlineErr.getMessage());
        }

        // SUN-gsc-lose-latency: skip the rebate publish for amount=0
        // (lose-shaped settles). Legacy reward(0) short-circuits in
        // updateMoney and never publishes LogMoneyUserMessage either,
        // so preserving that here keeps the agency-commission pipeline
        // (LogMoneyUserExtraProcessor) byte-for-byte equivalent — no
        // moneyExchange=0 commission rows for lose bets. The settle
        // update above DID publish, which is the actual fix for the
        // agency-portal latency bug; rebate is independent.
        if (amountSubunit <= 0L) {
            return;
        }

        // Publish the rebate event so the agency-commission pipeline
        // (LogMoneyUserExtraProcessor on queue_log_money_user_extra)
        // generates the per-bet commission rows. Mirrors what
        // userMoneyService.reward() does on the legacy path via
        // UserServiceImpl.updateMoney → MessageBusFactory queue_log_money.
        // Without this, the agency-side commission generation silently
        // breaks on flag-on traffic — MoneyGateway.creditUser is a
        // pure wallet primitive and does NOT publish the rebate event.
        //
        // Best-effort: failure here must NOT propagate. The wallet
        // credit is already committed by the time we reach this point
        // (see dispatchOne — afterCredit only runs on POSTED) and the
        // Mongo settle update either succeeded above or already logged
        // its own failure path. Failing the rebate publish on top of
        // that is strictly informational — at-least-once delivery from
        // the queue, with the deterministic source-key below, makes a
        // missed publish recoverable from gsc_event_log replay anyway.
        try {
            UserCacheModel cached = lookupUserCache(memberAccount);
            int userId = resolveUserIdForRebate(memberAccount, cached);
            if (userId <= 0) {
                logger.warn("GscDepositAggregator: rebate publish skipped — no user_id for "
                        + memberAccount + " wager=" + t.getWagerCode());
                return;
            }
            boolean isBot = cached != null && cached.isBot();

            String description = "Deposit transaction - "
                    + (t.getGameCode() == null ? "" : t.getGameCode())
                    + " : " + amountSubunit;

            // Constructor signature (verified VbeeCommon LogMoneyUserMessage):
            // (int userId, String nickname, String actionName, String serviceName,
            //  long currentMoney, long moneyExchange, String moneyType,
            //  String description, long fee, boolean vp, boolean isBot)
            LogMoneyUserMessage msg = new LogMoneyUserMessage(
                    userId,
                    memberAccount,
                    rebateActionName,                  // actionName / gameName
                    String.valueOf(productCode),       // serviceName — match legacy DepositProcess line 351-353
                    postCreditBalance,                 // currentMoney — informational; consumer reads moneyExchange
                    amountSubunit,                     // moneyExchange — POSITIVE for credit
                    Consts.MONEY_VIN,                  // moneyType
                    description,
                    0L,                                // fee — legacy reward() passes 0 (UserMoneyServiceImpl.reward)
                    true,                              // vp / playgame — legacy treats reward+transId as playgame
                    isBot);
            // Stamp the user's referralCode so the agency chain walker
            // attributes correctly (legacy does this in updateMoney line 271).
            try {
                if (cached != null && cached.getReferralCode() != null) {
                    msg.setReferralCode(cached.getReferralCode());
                }
            } catch (Throwable ignored) { /* defensive */ }

            // Stable source-key so a retried publish (queue is at-least-once)
            // doesn't double-generate commission rows on the consumer side.
            // Mirrors AwcCallbackProcessor.awcSourceKey style.
            String txnId = t.getId();
            if (txnId != null && !txnId.isEmpty()) {
                msg.setId("gsc:" + txnId);
            }

            publishRebateMessage(msg);
        } catch (Throwable rebateErr) {
            // Best-effort. Failing the publish must NOT fail the wallet
            // credit (already committed) or the settle update.
            logger.warn("GscDepositAggregator: rebate publish failed wager="
                    + t.getWagerCode() + " member=" + memberAccount
                    + " amount=" + amountSubunit
                    + " err=" + rebateErr.getMessage());
        }
    }

    /**
     * Read the cached {@link UserCacheModel} for this member from the
     * Hazelcast {@code users} IMap. Returns {@code null} if Hazelcast is
     * unavailable or the user is not cached — caller falls back to a DB
     * lookup for {@code user_id} and treats {@code isBot} as false.
     */
    protected UserCacheModel lookupUserCache(String memberAccount) {
        if (memberAccount == null || memberAccount.isEmpty()) return null;
        try {
            IMap<String, UserCacheModel> userMap =
                    HazelcastClientFactory.getInstance().getMap("users");
            return userMap.get(memberAccount);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Resolve {@code users.id} for the rebate message. Hazelcast first
     * (free if the cache hit), MySQL fallback. Mirrors
     * {@link com.vinplay.api.processors.awc.AwcCallbackProcessor#resolveUserId}
     * — same pattern, same pool name. Returns 0 if nothing found.
     */
    protected int resolveUserIdForRebate(String memberAccount, UserCacheModel cached) {
        if (cached != null) {
            try {
                long id = cached.getId();
                if (id > 0) return (int) id;
            } catch (Throwable ignored) { /* defensive */ }
        }
        if (memberAccount == null || memberAccount.isEmpty()) return 0;
        // SUN-EXPLOIT-GUARD V4 (2026-05-03): GSC's member_account is nick_name,
        // not user_name. Try both.
        try (java.sql.Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM vinplay.users WHERE nick_name = ? OR user_name = ? LIMIT 1")) {
            ps.setString(1, memberAccount);
            ps.setString(2, memberAccount);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Throwable e) {
            logger.warn("GscDepositAggregator.resolveUserIdForRebate fallback failed for "
                    + memberAccount + ": " + e.getMessage());
        }
        return 0;
    }

    /**
     * Publish the rebate {@link LogMoneyUserMessage} to the
     * {@code queue_log_money} pipeline. Wrapped as a {@code protected}
     * method so tests can override and capture without touching the
     * underlying transport. The default delegates to
     * {@link MessageBusFactory}; the 3-way fanout (queue_log_money cmd
     * 601 → queue_log_money_extra cmd 1001 → queue_log_report_user_balance
     * cmd 602) is encoded in {@code QueueRouter.route("queue_log_money", 601)},
     * consulted by both the RMQ and Redis-Streams adapters. M-log_money
     * note: {@code MessageBus.publish} declares no checked exceptions and
     * log-and-swallows transport errors per its contract, so the
     * {@code throws Exception} clause is kept for ABI compatibility with
     * test overrides but the default path no longer throws.
     */
    protected void publishRebateMessage(LogMoneyUserMessage msg) throws Exception {
        MessageBusFactory.get("queue_log_money").publish("queue_log_money", msg, 601);
    }

    /**
     * Phase 5 prep gate 5p2 — publish the bet side-effect (Mongo
     * write + Telegram alert) to {@code queue_log_gsc_bets_async} so it
     * runs asynchronously in {@code api/vbee}'s
     * {@code GscBetSideEffectProcessor}. {@code protected} so tests can
     * override and capture without touching RabbitMQ. Default delegates
     * to {@link GscBetSideEffectPublisher#publish}, which itself wraps
     * RMQ-failure with a sync-fallback so audit rows still land if the
     * broker is down.
     */
    protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
        GscBetSideEffectPublisher.publish(msg);
    }

    private static String safeResolveGameKey(int productCode, String gameType) {
        try {
            return com.vinplay.dal.service.GscProductMapService.resolveGameKey(productCode, gameType);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String buildGscBetEventKey(int productCode, String gameCode, String wagerCode, String txnId) {
        String pc = productCode > 0 ? String.valueOf(productCode) : "unknown";
        String gc = cleanKeyPart(gameCode, "unknown");
        if (wagerCode != null && !wagerCode.trim().isEmpty()) {
            String w = wagerCode.trim();
            if (gc.toUpperCase().startsWith("HASH_") && w.lastIndexOf("-") > 0) {
                w = w.substring(0, w.lastIndexOf("-"));
            }
            return "gsc:" + pc + ":" + gc + ":" + w;
        }
        if (txnId != null && !txnId.trim().isEmpty()) {
            return "gsc:" + pc + ":" + gc + ":txn:" + txnId.trim();
        }
        return null;
    }

    private static String cleanKeyPart(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static boolean isSuccessful(SeamlessOutcome out) {
        return out != null
                && (out.status == SeamlessOutcome.Status.POSTED
                || out.status == SeamlessOutcome.Status.DUPLICATE);
    }

    /**
     * Existence check: Hazelcast {@code gsc_wager_codes} fast-path,
     * {@code gsc_event_log} MySQL fall-back.
     *
     * <p>SUN-1245 (2026-05-03): the legacy code (mirrored in v1) only
     * checked Hazelcast. Nothing in the current codebase ever
     * {@code put}s into {@code gsc_wager_codes} so the fast-path is
     * always a miss → {@code wagerCodeExists} always returned false →
     * every CANCEL action got rejected with "Bet not exist", breaking
     * GSC certification "Withdraw and Cancel" (the test BET first,
     * THEN cancel — we recognized the bet on /withdraw but not on the
     * subsequent cancel). Fix: fall back to {@code gsc_event_log}
     * which DOES persist the wager_code from every withdraw.
     */
    private boolean wagerCodeExists(String wagerCode) {
        if (wagerCode == null || wagerCode.isEmpty()) return false;
        try {
            IMap<String, String> wagerMap =
                    HazelcastClientFactory.getInstance().getMap("gsc_wager_codes");
            if (wagerMap.containsKey(wagerCode)) return true;
        } catch (Throwable t) {
            // Hazelcast unavailable — drop to DB fallback.
        }
        try (java.sql.Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM gsc_event_log WHERE gsc_wager_code = ? "
                             + "AND gsc_endpoint = 'withdraw' "
                             + "AND processing_status = 'COMPLETED' LIMIT 1")) {
            ps.setString(1, wagerCode);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.warn("GscDepositAggregator: gsc_event_log fallback failed for "
                    + wagerCode + ": " + e.getMessage());
            return true;
        }
    }

    /**
     * Existence check: Hazelcast {@code users} IMap fast-path, MySQL
     * fall-back. SUN-1245 (2026-05-03) — symmetric fix to
     * GscWithdrawAggregator: the cache-only check rejected logged-out
     * users with valid MySQL rows, breaking GSC certification "Withdraw
     * and Deposit" test (the deposit half ran after the withdraw
     * passed). Cache hit short-circuits; cache miss issues an indexed
     * SELECT against {@code users} (matches both nick_name and
     * user_name per SUN-1227 V4).
     */
    private boolean memberAccountExists(String memberAccount) {
        if (memberAccount == null || memberAccount.isEmpty()) return false;
        try {
            IMap<String, UserCacheModel> userMap =
                    HazelcastClientFactory.getInstance().getMap("users");
            if (userMap.containsKey(memberAccount)) return true;
        } catch (Throwable t) {
            // Hazelcast unavailable — drop to DB fallback.
        }
        try (java.sql.Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM users WHERE nick_name = ? OR user_name = ? LIMIT 1")) {
            ps.setString(1, memberAccount);
            ps.setString(2, memberAccount);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.warn("memberAccountExists DB fallback failed for "
                    + memberAccount + ": " + e.getMessage());
            return true;
        }
    }

    /**
     * Read a user's balance for the before/after numbers.
     *
     * <p>SUN-1xxx (2026-05-11): MySQL-only, no cache. Per
     * docs/architecture/LEDGER_HARDENING_ROADMAP.md — any balance
     * read returned to an external party or used to gate a money
     * movement must read the canonical store directly. Hazelcast
     * users IMap drifts (testgiftcode001 incident: vin=20k in MySQL,
     * vin=0 in HZ caused 12 ghost-rejected Ice Fishing bets).
     */
    private long balanceForUser(String memberAccount) {
        if (memberAccount == null || memberAccount.isEmpty()) return 0L;
        try {
            SeamlessOutcome r = doReadBalance(memberAccount);
            if (r != null && r.status == SeamlessOutcome.Status.POSTED) {
                return r.newBalance;
            }
            return 0L;
        } catch (Throwable t) {
            logger.warn("GscDepositAggregator.balanceForUser MySQL read failed user="
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
    private static final int SC_MEMBER_NOT_EXIST      = 1000;
    private static final int SC_INCORRECT_AGENT_KEY   = 1002;
    /** SUN-1245: GSC certification expects 1003 on replay. */
    private static final int SC_DUPLICATE_TRANSACTION = 1003;
    private static final int SC_INVALID_SIGNATURE     = 1004;
    private static final int SC_BET_NOT_EXIST         = 1006;

    private static SeamlessOutcome errorOutcome(int code, String message) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.DEPOSIT);
        meta.put("code", code);
        meta.put("message", message);
        meta.put("before_balance", 0.0);
        meta.put("balance", 0.0);
        return SeamlessOutcome.validationError(String.valueOf(code), message, meta);
    }

    /**
     * Empty {@code BalanceResponse} fall-back (legacy lines 112-116). Used
     * for unknown-currency only. Wire shape:
     * {@code {"code":0,"message":"","data":[]}}.
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
        if (out == null) return depositJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
        switch (out.status) {
            case SIGNATURE_ERROR:
                return depositJson(SC_INVALID_SIGNATURE, "Invalid sign", 0.0, 0.0);
            case SERVER_ERROR:
                return depositJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
            case POSTED:
            case DUPLICATE:
            case VALIDATION_ERROR: {
                Map<String, Object> meta = out.metadata;
                GscResponseShape shape = GscResponseShape.from(meta, GscResponseShape.DEPOSIT);
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
                return depositJson(code, msg, before, after);
            }
            case USER_NOT_FOUND:
            case INSUFFICIENT_BALANCE:
            default:
                return depositJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
        }
    }

    /**
     * Serialize the legacy {@code DepositResponse.toJson()} shape
     * verbatim. The legacy uses a hand-rolled string builder (NOT
     * Jackson), wrapping the (code,message,before_balance,balance)
     * tuple inside a {@code data:[{...}]} envelope. Numbers are
     * formatted via {@code DecimalFormat("0.####")}: integral values
     * have no trailing {@code .0} ({@code 0} not {@code 0.0}); fractional
     * values keep up to 4 decimal places. We mirror that exactly so a
     * string-compare against captured production responses is stable.
     */
    private static String depositJson(int code, String message, double before, double balance) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0.####", sym);
        String bb = df.format(before);
        String b  = df.format(balance);
        String msgEsc = escapeJsonString(message == null ? "" : message);
        return "{\"code\":" + code + ",\"message\":\"" + msgEsc + "\",\"data\":[{\"code\":"
                + code + ",\"message\":\"" + msgEsc + "\",\"before_balance\":" + bb
                + ",\"balance\":" + b + "}]}";
    }

    /**
     * Minimal JSON-string escaper — enough to keep response messages
     * with quotes / backslashes / control characters round-trippable.
     * Mirrors the small subset Jackson would do; full RFC-8259 escaping
     * is overkill given the messages are static strings in this class
     * (no untrusted input lands in the message field).
     */
    private static String escapeJsonString(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else          out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Empty-data fall-back JSON: {@code {"code":0,"message":"","data":[]}}.
     * Mirrors {@code BalanceResponse.toJson()} (Gson) on an empty
     * {@code data} list. Field order matches Gson's default declaration
     * order in {@code BalanceResponse}.
     */
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
        return GscEventLogger.tryLogRequest("deposit", rawBody, http);
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

    /**
     * SUN-1367 — currency-aware lossless conversion of a provider's
     * raw amount string (which may carry decimals like "63096.15") into
     * a {@code long} milli-subunit. Single mapping at the input
     * boundary so downstream code never has to think about decimal
     * scale or per-provider ×1000 workarounds.
     *
     * <p>Formula: {@code raw × currencyFactor × 1000}, evaluated with
     * {@link java.math.BigDecimal} so no float intermediate rounding
     * loses precision. {@code HALF_UP} rounding matches the legacy
     * {@code Math.round} semantics.
     *
     * <p>Currency interpretation:
     * <ul>
     *   <li>IDR (rate=1, decimal wire): 63096.15 × 1 × 1000 = 63,096,150 milli
     *       (recovers .15 cleanly).</li>
     *   <li>IDR2 (rate=1000, milli-wire): 63.09615 × 1000 × 1000 = 63,096,150 milli
     *       (same magnitude, same precision).</li>
     *   <li>VND (rate=1, integer wire): 1000 × 1 × 1000 = 1,000,000 milli
     *       (integer source — already lossless, just rescaled).</li>
     *   <li>CNY / KRW etc. all flow through the same path uniformly.</li>
     * </ul>
     *
     * <p>Returns 0 when the input is null, empty, or unparseable —
     * caller treats 0 as "milli unset" and falls back to the legacy
     * integer field for backward compatibility.
     */
    private static long parseRawToMilliSubunit(String rawAmount, int currencyFactor) {
        if (rawAmount == null || rawAmount.isEmpty()) return 0L;
        try {
            BigDecimal raw = new BigDecimal(rawAmount).abs();
            BigDecimal scaled = raw
                    .multiply(BigDecimal.valueOf((long) currencyFactor))
                    .multiply(BigDecimal.valueOf(1000L));
            return scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Throwable t) {
            return 0L;
        }
    }
}
