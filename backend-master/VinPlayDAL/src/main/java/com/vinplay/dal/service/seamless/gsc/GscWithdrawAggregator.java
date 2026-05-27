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
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 3f — GSC Withdraw handler implemented on top of
 * {@link SeamlessWalletAggregator}. Mirrors the legacy
 * {@code game.third.hooks.gscSeamless.WithdrawProcess} at the
 * wire-response level, gated behind {@code GSC_AGGREGATOR_WITHDRAW_ENABLED}.
 *
 * <h2>Production volume — handle with HIGHEST care</h2>
 * Verified ~870 events/day average, observed peak 60/min (1 TPS sustained
 * for 60 sec) on the {@code withdraw} endpoint over the last 7 days.
 * User warning notes thousands per minute possible during promotions /
 * tournaments (16-30 TPS sustained burst). Per-user row lock auto-
 * serializes and the wallet pool of 10 has headroom for 30 TPS at
 * ~25ms/handler; risk is Mongo backoff up to 2s chain-stalling
 * connections under burst. The {@code money_gateway_log.uk_tx_source}
 * UNIQUE remains the structural dedup gate so a retried debit is a
 * no-op at the wallet layer regardless.
 *
 * <h2>Direction — always DEBIT</h2>
 * Every withdraw transaction debits the player. Legacy treats action
 * verbs {@code BET / SETTLE / CANCEL / BONUS} as valid (line 130) but
 * the wallet direction is fixed — withdraw is a money-out endpoint.
 * Cancel-via-withdraw is not a documented case; the legacy code path
 * still calls {@code userMoneyService.bet(...)} (a debit) even when
 * action=CANCEL. We preserve that: this aggregator only debits.
 *
 * <p><b>No legacy bug to preserve.</b> {@code WithdrawProcess} does NOT
 * route through {@code CommonProcess.TransactionAction} — it calls
 * {@code userMoneyService.bet(...)} directly (line 322), which is the
 * correct DEBIT direction. So unlike Phase 3b (which corrected the
 * {@code actionReward}-calls-{@code bet} bug), this is structurally a
 * refactor only — the wire shape and wallet effect must match legacy
 * byte-for-byte.
 *
 * <h2>External-ref namespacing</h2>
 * {@code "withdraw_" + transaction.id}. The transaction id is GSC's
 * stable per-event id and is the same value the legacy uses to mark
 * the Hazelcast {@code gsc_tx_ids} dedup map. Distinct from
 * {@code "deposit_*"} (Phase 3e), {@code "transfer_*"} (Phase 3b),
 * {@code "cancel_*"} (Phase 3c), {@code "rollback_*"} (Phase 3d), and
 * {@code "pushbet_*"} (Phase 3a) so cross-endpoint retries cannot
 * collapse onto the same {@code (tx_id, source, user_id)} row.
 *
 * <h2>Side-effects preserved (the SUN-tickets)</h2>
 * <ol>
 *   <li><b>SUN-888 / fish-game amount override</b> — when
 *       {@link GscWithdrawProviderHooks#isFishGame} matches any txn in the
 *       batch, every debit amount is resolved via
 *       {@link GscWithdrawProviderHooks#resolveBetAmount} which substitutes
 *       {@code valid_bet_amount} / {@code bet_amount} for the raw
 *       {@code amount} (legacy lines 188-208).</li>
 *   <li><b>SUN-980 / commission-eligibility short-circuit</b> — when the
 *       {@code (product_code, game_code)} pair is marked
 *       {@code commission_eligible=0} on {@code gsc_game_catalog} AND
 *       both {@code valid_bet_amount} and {@code bet_amount} are zero,
 *       treat the webhook as a wallet transfer-in: skip the
 *       {@code log_gsc_bets} insert AND the rebate publish (set
 *       {@code rebateActionName="Cq9FishTransfer"}, the
 *       {@code RealTimeCommission.EXCLUDED_ACTIONS} sentinel). Wallet
 *       still debits (legacy lines 211-227).</li>
 *   <li><b>SUN-1175 / fish 0/0 launch transfer</b> — when batch is fish
 *       and {@code valid_bet_amount=0} AND {@code bet_amount=0} AND no
 *       {@code wager_code}, treat as commission-ineligible too (legacy
 *       lines 234-241).</li>
 *   <li><b>SUN-1184 / free-spin trigger amount=0 row skip</b> — PG Soft
 *       / Pragmatic free-spin wager registration arrives with
 *       {@code amount=0}; suppress the {@code log_gsc_bets} insert so
 *       agent "Lịch sử cược" matches the LSC counts. Wallet still debits
 *       (a no-op debit since amount=0; we treat as success-skip).
 *       Legacy lines 343-350.</li>
 *   <li><b>SUN-865 / SUN-1201 rebate action_name</b> —
 *       {@code "gsc_<pc>_<game_code>"} preferred, with
 *       {@link com.vinplay.dal.service.BetContextResolver} session-recovery
 *       and {@link com.vinplay.dal.service.GscProductMapService} fall-back
 *       (legacy lines 261-308).</li>
 *   <li><b>SUN-1205/1206 / commission-volume override</b> —
 *       {@link GscWithdrawProviderHooks#resolveCommissionVolume} reads
 *       vendor's {@code valid_bet_amount} for hedge bets (Dream Gaming
 *       Baccarat banker+player). Money flow ({@code amount}) unchanged;
 *       only the rebate pipeline sees the smaller volume (legacy lines
 *       310-326).</li>
 *   <li><b>SUN-LIVE-HIST / log_gsc_bets insert</b> — per-bet row to
 *       Mongo {@code log_gsc_bets} so c=9843 betting history can surface
 *       LIVE game bets alongside offline games (legacy lines 332-426).
 *       Wrapped in {@code MongoRetry.runWithRetry(...)} for SUN-1108/1110
 *       transient retries.</li>
 *   <li><b>SUN-1196 / freespin chain link</b> —
 *       {@link GscWithdrawProviderHooks#resolveLinkId} returns the
 *       parent_round_id / Blackjack-shared game_id; appended to the
 *       Mongo doc as {@code vendor_game_id} so settle-side merge works
 *       (legacy lines 388-392).</li>
 *   <li><b>SUN-1108/1110 / MongoRetry wrapper</b> — Mongo insert/upsert
 *       wrapped in {@code MongoRetry.runWithRetry(...)}; on exhaustion
 *       a WARN is logged AND
 *       {@link TelegramOpsNotifier#alertGscBetWriteFailure} fires
 *       (legacy lines 396-437).</li>
 *   <li><b>REBATE PUBLISH (Phase 3e gap-fix carried forward)</b> —
 *       legacy {@code userMoneyService.bet(...)} →
 *       {@code UserServiceImpl.updateMoney(...)} →
 *       {@code MessageBusFactory.get("queue_log_money").publish(...)} to
 *       {@code queue_log_money_user_extra}.
 *       {@link MoneyGateway#debitUser} does NOT publish — so without an
 *       explicit publish below, the agency-commission generation
 *       silently breaks on flag-on traffic. We publish a
 *       {@link LogMoneyUserMessage} mirroring the legacy fields. When
 *       {@code commissionVolume != amount} (SUN-1205/1206 hedge bets)
 *       we set {@link LogMoneyUserMessage#setValidBetAmount} so the
 *       rebate pipeline sees the smaller volume.</li>
 * </ol>
 *
 * <p><b>{@code afterDebit} is best-effort.</b> Every Mongo / Rebate /
 * Telegram side-effect runs inside the {@code afterDebit} hook only
 * when the wallet debit actually posted (POSTED, not DUPLICATE). On
 * DUPLICATE the side-effects already ran on the original call. On
 * POSTED, the side-effects' failure is logged but NEVER fails the
 * wallet debit — the provider considers a 200 the source of truth and
 * its retry pipeline will not re-send. Mirrors Phase 3e's pattern
 * (afterCredit) inverted to debit.
 *
 * <h2>Sign verification</h2>
 * Single verb {@code "withdraw"}: {@code md5(operator_code + request_time
 * + "withdraw" + secret)}. Verified against {@code WithdrawProcess.java:111}.
 *
 * <h2>Audit endpoint string</h2>
 * Matches the legacy: {@code "withdraw"} ({@code WithdrawProcess.java:60}).
 *
 * <h2>Response shape</h2>
 * Legacy {@code WithdrawResponse.toJson()} uses a hand-rolled string
 * builder (NOT Jackson). Two distinct shapes:
 * <pre>
 *   success (code=0):
 *     {"code":0,"message":"...","data":[{"before_balance":B,"balance":B}]}
 *   failure (code!=0):
 *     {"code":N,"message":"...","data":null}
 * </pre>
 * Numbers are formatted via {@code DecimalFormat("0.####")} — no
 * trailing {@code .0} on integer values. Note this is DIFFERENT from
 * Phase 3e's Deposit shape (which always wraps the inner code/message
 * in {@code data[0]}). We reproduce the Withdraw quirks verbatim in
 * {@link #withdrawJson} so a string-compare against captured
 * production responses is stable.
 *
 * <p>Unknown-currency fall-back uses the empty
 * {@code BalanceResponse} shape: {@code {"code":0,"message":"","data":[]}}.
 */
public class GscWithdrawAggregator extends SeamlessWalletAggregator<GscRequest, String> {

    // Bounded executor for post-debit side effects (HZ lookups, RMQ publishes
    // for rebate + log_gsc_bets). The wallet commit is already done before
    // we submit here, so the HTTP response returns immediately while the
    // pipeline catches up off-thread. CallerRunsPolicy keeps current
    // synchronous behavior as a fallback when the pool is saturated, so
    // we never silently drop a rebate publish.
    private static final ExecutorService AFTER_DEBIT_EXECUTOR;
    private static final AtomicInteger AFTER_DEBIT_INFLIGHT = new AtomicInteger();
    private static final AtomicLong AFTER_DEBIT_SUBMITTED = new AtomicLong();
    private static final AtomicLong AFTER_DEBIT_FELL_BACK_SYNC = new AtomicLong();
    static {
        int core = parseEnvInt("GSC_AFTER_DEBIT_CORE_THREADS", 4, 1, 64);
        int max  = parseEnvInt("GSC_AFTER_DEBIT_MAX_THREADS",  16, core, 128);
        int q    = parseEnvInt("GSC_AFTER_DEBIT_QUEUE_SIZE",   5000, 100, 100000);
        AFTER_DEBIT_EXECUTOR = new ThreadPoolExecutor(
                core, max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(q),
                new ThreadFactory() {
                    private final AtomicInteger n = new AtomicInteger();
                    @Override public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "gsc-after-debit-" + n.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
                },
                // Saturation: do the work on the caller (HTTP) thread. Keeps
                // the rebate/log invariants intact; alerts via counter.
                (r, ex) -> {
                    AFTER_DEBIT_FELL_BACK_SYNC.incrementAndGet();
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
    private final GscWithdrawProviderHooks.Resolver providerResolver;

    public GscWithdrawAggregator(GscConfigProvider config) {
        this(config, GscWithdrawProviderHooks.Resolver.DEFAULT);
    }

    public GscWithdrawAggregator(GscConfigProvider config,
                                 GscWithdrawProviderHooks.Resolver providerResolver) {
        if (config == null) throw new IllegalArgumentException("config is null");
        this.config = config;
        this.providerResolver = providerResolver != null
                ? providerResolver
                : GscWithdrawProviderHooks.Resolver.DEFAULT;
    }

    /** Tighter audit-log tag than the default class-simple-name. */
    @Override
    protected String aggregatorName() { return "GSC"; }

    /** Phase 5p3 — distinct timing-metrics bucket per handler. */
    @Override
    protected String metricsName() { return "GscWithdraw"; }

    // ─────────────────────────────────────────────────────────────────
    // parseRequest
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse the inbound JSON into a {@link GscRequest}. Mirrors the
     * legacy {@code WithdrawRequest.fromJson} +
     * {@code resolveMemberAccount} / {@code resolveProductCode} /
     * {@code resolveTransactions} chain: if the top-level fields are
     * absent, fall back to the first {@code batch_requests[0]} entry.
     * ALL production traffic uses the {@code batch_requests} envelope
     * (verified across 2800+ rows in {@code gsc_event_log}); the
     * top-level path is preserved for defensive parity.
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
        // product_code on the legacy WithdrawRequest is a String (different
        // from DepositRequest int). Tolerate both shapes.
        Integer topProductCode = optIntBoxed(root, "product_code");
        String topMemberAccount = optStr(root, "member_account");

        // batch_requests fall-back. Legacy resolveMemberAccount /
        // resolveProductCode / resolveTransactions: pick top-level if
        // set, else batch_requests[0].
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
     * tree. {@link GscWithdrawProviderHooks#resolveLinkId} (and its
     * {@code ProviderAdapter} default impls) walk the payload via
     * {@code firstNestedMap(payload)} which uses {@code instanceof Map}
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
     * payloads encode amounts as strings (mostly) but defensive: tolerate
     * JSON-number form too. Note negative values (action=BET amount=-2000
     * Evolution case) ARE preserved here; callers abs() at the FX step.
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
     * Single verb {@code "withdraw"}. Verified against
     * {@code WithdrawProcess.java:111}: {@code HashUtil.md5(operatorCode +
     * requestTime + "withdraw" + secretKey)}.
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

        String expected = md5Hex(req.getOperatorCode() + req.getRequestTime() + "withdraw" + secret);
        if (!req.getSign().equals(expected)) {
            return VerifyResult.fail("signature mismatch");
        }
        return VerifyResult.ok();
    }

    // ─────────────────────────────────────────────────────────────────
    // mapActionToSource / currency conversion
    // ─────────────────────────────────────────────────────────────────

    /**
     * Withdraw is unconditionally a DEBIT — every action verb on this
     * endpoint takes money out of the player's wallet (verified against
     * legacy line 322: {@code userMoneyService.bet(...)} regardless of
     * action verb). The action verb carries informational value at the
     * audit layer but the wallet direction is fixed.
     */
    @Override
    protected String mapActionToSource(String aggregatorAction) {
        return MoneyGateway.SOURCE_GSC_DEBIT;
    }

    @Override
    protected long currencyToInternal(double providerAmount, String currency) {
        double rateIn = exchangeRateIn(currency);
        if (rateIn == 0.0) rateIn = 1.0;
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
        // Invalid-JSON / null request → 1002 INCORRECT_AGENT_KEY
        // "Invalid JSON format" (legacy lines 88-90).
        if (req == null
                || (req.getOperatorCode() == null
                && req.getCurrency() == null
                && req.getTransactions().isEmpty()
                && req.getSign() == null
                && req.getRequestTime() == null
                && req.getMemberAccount() == null
                && req.getProductCode() == null)) {
            return errorOutcome(SC_INCORRECT_AGENT_KEY, "Invalid JSON format");
        }

        List<GscRequest.TransactionItem> transactions = req.getTransactions();
        // Required-parameter check (legacy lines 104-107).
        if (req.getOperatorCode() == null
                || req.getMemberAccount() == null
                || req.getProductCode() == null
                || req.getProductCode() == 0
                || req.getCurrency() == null
                || req.getSign() == null
                || req.getRequestTime() == null
                || transactions == null) {
            return errorOutcome(SC_INCORRECT_AGENT_KEY, "Missing required parameters");
        }

        // Currency lookup. Unknown → empty BalanceResponse fall-back
        // (legacy lines 117-121).
        int currencyFactor = config.getCurrencyExchangeRate(req.getCurrency());
        if (currencyFactor == 0) {
            return balanceFallbackOutcome();
        }

        // Validate transaction actions (legacy lines 128-134).
        for (GscRequest.TransactionItem t : transactions) {
            String act = t == null ? null : t.getAction();
            if (act == null
                    || !(act.equalsIgnoreCase("BET")
                    || act.equalsIgnoreCase("SETTLE")
                    || act.equalsIgnoreCase("CANCEL")
                    || act.equalsIgnoreCase("BONUS"))) {
                return errorOutcome(SC_INCORRECT_AGENT_KEY, "Invalid action");
            }
        }

        // Bet-time table gate. Refuses NEW bets on non-whitelisted products /
        // game_codes by returning code 2000 (PRODUCT_UNDER_MAINTENANCE) so
        // Evo and other live-casino aggregators surface "table under
        // maintenance" to the player without ever debiting their wallet.
        // Off by default — see {@link GscBetWhitelist#enforceEnabled()}.
        // Only `BET` is gated; `SETTLE`, `CANCEL`, `BONUS` flow through
        // unconditionally so an in-flight or already-accepted wager can
        // always be closed/refunded even if the table was added to the
        // block list mid-session.
        if (GscBetWhitelist.enforceEnabled()) {
            for (GscRequest.TransactionItem t : transactions) {
                if (t == null) continue;
                if (!"BET".equalsIgnoreCase(t.getAction())) continue;
                int txProductCode = t.getProductCode() > 0
                        ? t.getProductCode()
                        : (req.getProductCode() != null ? req.getProductCode() : 0);
                // SUN-1281: providers like Dream Gaming (pc=1052) ship empty
                // game_code in BET pushes; recover via BetContextResolver
                // (SESSION → DEFAULT chain) BEFORE consulting the whitelist,
                // otherwise the empty-string short-circuit in
                // GscBetWhitelist.isAllowed step 3 lets the bet through
                // regardless of catalog state. Evo (pc=1002) already includes
                // game_code → resolver returns VENDOR fast-path, no behavior
                // change. The same resolver runs again later in afterDebit
                // for rebate attribution; the duplicate call is cheap (one
                // Hazelcast lookup at most when game_code is empty).
                String resolvedGameCode = t.getGameCode();
                if (resolvedGameCode == null || resolvedGameCode.isEmpty()) {
                    com.vinplay.dal.service.BetContextResolver.Context ctx =
                            com.vinplay.dal.service.BetContextResolver.resolve(
                                    req.getMemberAccount(), txProductCode, resolvedGameCode);
                    if (ctx != null && ctx.hasGameCode()) {
                        resolvedGameCode = ctx.gameCode;
                    }
                }
                if (!GscBetWhitelist.isAllowed(txProductCode, resolvedGameCode)) {
                    logger.warn("GscWithdrawAggregator: bet refused by whitelist."
                            + " member=" + req.getMemberAccount()
                            + " product=" + txProductCode
                            + " game_code=" + resolvedGameCode
                            + " (vendor=" + t.getGameCode() + ")"
                            + " wager=" + t.getWagerCode());
                    return errorOutcome(SC_PRODUCT_UNDER_MAINTENANCE,
                            "Table under maintenance");
                }
            }
        }

        // Per-user block list (c=9985 admin add). Refuses BET when ANY
        // active row matches (provider=GSC, vendor_platform=<product>,
        // game_code=<code>, table_tag NULL for GSC, category_id from
        // games table). Same SETTLE/CANCEL/BONUS pass-through semantics
        // as the whitelist above.
        for (GscRequest.TransactionItem t : transactions) {
            if (t == null) continue;
            if (!"BET".equalsIgnoreCase(t.getAction())) continue;
            int txProductCode = t.getProductCode() > 0
                    ? t.getProductCode()
                    : (req.getProductCode() != null ? req.getProductCode() : 0);
            String vp = txProductCode > 0 ? String.valueOf(txProductCode) : null;
            Integer cat = lookupCategoryId("GSC", vp, t.getGameCode());
            if (com.vinplay.dal.service.UserGameBlock.isBlocked(
                    0L, req.getMemberAccount(), "GSC", vp, t.getGameCode(), null, cat)) {
                logger.warn("GscWithdrawAggregator: bet refused by user-block."
                        + " member=" + req.getMemberAccount()
                        + " product=" + txProductCode
                        + " game_code=" + t.getGameCode()
                        + " category_id=" + cat);
                return errorOutcome(SC_PRODUCT_UNDER_MAINTENANCE,
                        "Player blocked from this game");
            }
        }

        String memberAccount = req.getMemberAccount();
        int productCode = req.getProductCode();

        // Member existence check (legacy lines 137-141). Hazelcast users
        // IMap; on Hazelcast unavailable skip the check (the wallet
        // primitive's DB lookup decides). Same conservative posture as
        // Phase 3e Deposit.
        if (!memberAccountExists(memberAccount)) {
            return errorOutcome(SC_MEMBER_NOT_EXIST, "Member not found");
        }

        // SUN-1373 — GAME_INACTIVE gate.
        // When GSC_BET_WHITELIST_ENFORCE=true, reject bets on any game whose
        // gsc_game_catalog.active=0 row exists. Fail-open on any error
        // (missing row, DB outage) so the gate never blocks all bets on infra
        // hiccups. The check runs once per batch: if ANY transaction in the
        // batch targets an inactive game the whole batch is rejected.
        boolean enforceWhitelist = isWhitelistEnforced();
        if (enforceWhitelist) {
            for (GscRequest.TransactionItem t : transactions) {
                if (t == null) continue;
                String gameCode = t.getGameCode();
                boolean active;
                try {
                    active = com.vinplay.dal.service.GscGameNameResolver
                            .isGameActive(productCode, gameCode);
                } catch (Throwable ex) {
                    active = true; // fail-open
                }
                if (!active) {
                    logger.info("GscWithdrawAggregator: GAME_INACTIVE gate fired"
                            + " pc=" + productCode + " gc=" + gameCode
                            + " member=" + memberAccount);
                    return SeamlessOutcome.gameInactive();
                }
            }
        }

        // SUN-888 fish-game detection: if ANY transaction game_code is a
        // fish game, every debit amount in the batch flows through
        // resolveBetAmount which substitutes valid_bet_amount /
        // bet_amount for raw amount (legacy lines 188-197).
        GscWithdrawProviderHooks provider = providerResolver.forProduct(productCode);
        if (provider == null) provider = GscWithdrawProviderHooks.DEFAULT;
        boolean batchHasFish = false;
        for (GscRequest.TransactionItem t : transactions) {
            if (provider.isFishGame(t.getGameCode())) {
                batchHasFish = true;
                break;
            }
        }

        // Read before-balance for the response (legacy line 154).
        long beforeBalanceInternal = balanceForUser(memberAccount);

        // Precompute total bet amount and verify sufficient balance
        // upfront (legacy lines 159-167). Mirrors the legacy
        // floor-check; the per-tx wallet primitive also checks atomically
        // via UPDATE ... WHERE vin >= ? but the legacy aborts the entire
        // batch with a distinct error code (1001 INSUFFICIENT_BALANCE)
        // before any debit so we preserve that.
        double fxIn = exchangeRateIn(req.getCurrency());
        long totalBet = 0L;
        for (GscRequest.TransactionItem t : transactions) {
            totalBet += Math.round(Math.abs(t.getAmountNumber()) * fxIn);
        }
        if (totalBet > beforeBalanceInternal) {
            double beforeBalanceDisplay = beforeBalanceInternal * exchangeRateOut(req.getCurrency());
            return insufficientBalanceOutcome(beforeBalanceDisplay, beforeBalanceDisplay);
        }

        // Dispatch each transaction in order. Legacy aborts the loop on
        // the first failed debit and returns INTERNAL_SERVER_ERROR 999
        // "Server Error" (line 328). Preserve the distinction.
        //
        // SUN-1245: track DUPLICATE separately. The wallet primitive
        // (MoneyGateway.debitUser) idempotently rejects retried tx_ids
        // via the (tx_id, source) UNIQUE constraint, returning
        // SeamlessOutcome.duplicate(...). isSuccessful() treats DUPLICATE
        // as success (because the original posted) — but GSC's
        // certification "Withdrawal Duplicate" test requires the response
        // surface code 1003 "Duplicate Transaction" so the operator can
        // distinguish replay from a fresh post. Aggregate over the batch:
        // if any per-tx is DUPLICATE, the whole response carries 1003.
        boolean anyDuplicate = false;
        for (GscRequest.TransactionItem t : transactions) {
            SeamlessOutcome perTxn = dispatchOne(req, t, provider, batchHasFish, fxIn);
            if (!isSuccessful(perTxn)) {
                logger.warn("GscWithdrawAggregator: txn failed, aborting batch."
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
        // Legacy lines 451-452: BOTH before_balance and balance multiplied
        // by getExchangeRateOut so they share the same display unit.
        double beforeBalanceExternal = (double) beforeBalanceInternal * exchangeRateOut(req.getCurrency());
        double afterBalanceExternal  = currencyToExternal(afterBalanceInternal, req.getCurrency());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.WITHDRAW);
        if (anyDuplicate) {
            meta.put("code", SC_DUPLICATE_TRANSACTION);
            meta.put("message", "Duplicate Transaction");
        } else {
            meta.put("code", SC_SUCCESS);
            meta.put("message", "Withdraw processed successfully");
        }
        meta.put("before_balance", beforeBalanceExternal);
        meta.put("balance", afterBalanceExternal);
        return SeamlessOutcome.posted(afterBalanceInternal, meta);
    }

    /**
     * Dispatch a single withdraw transaction. Always a DEBIT direction;
     * post-debit side-effects vary by SUN-980/SUN-1175 (skip log /
     * skip rebate) vs normal-bet (publish + insert).
     */
    private SeamlessOutcome dispatchOne(GscRequest req,
                                        GscRequest.TransactionItem t,
                                        GscWithdrawProviderHooks provider,
                                        boolean batchHasFish,
                                        double fxIn) {
        if (t == null) {
            return SeamlessOutcome.serverError(null, "null transaction");
        }
        String id = t.getId();
        String action = t.getAction();
        String gameCode = t.getGameCode();
        int productCode = req.getProductCode();

        // Provider-strategy bet-amount resolution (SUN-888 fish override).
        double rawAmount = t.getAmountNumber();
        double validBetAmt = parseDoubleSafe(t.getValidBetAmount());
        double betAmt      = parseDoubleSafe(t.getBetAmount());
        long amountSubunit;
        try {
            amountSubunit = provider.resolveBetAmount(rawAmount, validBetAmt, betAmt, batchHasFish, fxIn);
        } catch (Throwable e) {
            // Defensive: a misbehaving provider must not break the path.
            amountSubunit = Math.round(Math.abs(rawAmount) * fxIn);
        }

        // SUN-980 / SUN-1175: detect commission-ineligible transfer-in.
        // Catalog-driven (commission_eligible=0 on gsc_game_catalog).
        // SUN-1175 layers on: fish + 0/0 + no wager_code = launch transfer.
        boolean isCq9FishTransfer = false;
        boolean catalogIneligible;
        try {
            catalogIneligible = !com.vinplay.dal.service.GscGameNameResolver
                    .isCommissionEligible(productCode, gameCode);
        } catch (Throwable t1) {
            // Catalog DB miss — fail-open (treat as eligible) to avoid
            // silently excluding traffic. Mirrors GscGameNameResolver's
            // own conservative posture.
            catalogIneligible = false;
        }
        if (catalogIneligible
                && validBetAmt == 0d
                && betAmt == 0d) {
            isCq9FishTransfer = true;
            logger.info("[SUN-980] commission-ineligible seamless-transfer tx="
                    + id + " pc=" + productCode + " gc=" + gameCode
                    + " amount=" + amountSubunit);
        }
        if (batchHasFish && validBetAmt == 0d && betAmt == 0d) {
            String wc = t.getWagerCode();
            if (wc == null || wc.trim().isEmpty()) {
                isCq9FishTransfer = true;
            }
        }

        // SUN-1184: free-spin trigger amount=0 — treat as success-skip
        // (no wallet movement, no log row, no rebate publish). The legacy
        // code's userMoneyService.bet on amount=0 returns success and
        // updateMoney short-circuits, so the wallet net is the same.
        if (amountSubunit <= 0L) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("skipped", Boolean.TRUE);
            meta.put("reason", "zero-amount-or-freespin-trigger");
            return SeamlessOutcome.posted(0L, meta);
        }

        // External-ref namespacing: "withdraw_<id>".
        if (id == null || id.isEmpty()) {
            logger.warn("GscWithdrawAggregator: transaction missing id, action=" + action);
            return SeamlessOutcome.serverError(null, "missing transaction id");
        }
        String externalRef = "withdraw_" + id;

        SeamlessTxn txn = new SeamlessTxn(
                req.getMemberAccount(),
                externalRef,
                action != null ? action : "BET",
                amountSubunit,
                req.getCurrency(),
                /*metadata*/ null);

        SeamlessOutcome out = doDebit(txn);

        // Run post-debit side effects only when the wallet actually
        // moved (POSTED). DUPLICATE means a retry — side effects already
        // ran on the original call.
        if (out != null && out.status == SeamlessOutcome.Status.POSTED) {
            // Snapshot everything afterDebit reads so the lambda is safe
            // to run on another thread (req/t are not modified after this
            // point but we keep the contract explicit).
            final GscRequest reqRef = req;
            final GscRequest.TransactionItem tRef = t;
            final GscWithdrawProviderHooks providerRef = provider;
            final boolean isCq9 = isCq9FishTransfer;
            final long amt = amountSubunit;
            final double vb = validBetAmt;
            final double b  = betAmt;
            final double fx = fxIn;
            final long postBal = out.newBalance;
            AFTER_DEBIT_SUBMITTED.incrementAndGet();
            AFTER_DEBIT_INFLIGHT.incrementAndGet();
            AFTER_DEBIT_EXECUTOR.execute(() -> {
                try {
                    afterDebit(reqRef, tRef, providerRef, isCq9, amt, vb, b, fx, postBal);
                } catch (Throwable afterErr) {
                    logger.warn("GscWithdrawAggregator: afterDebit threw (non-fatal): "
                            + afterErr.getMessage());
                } finally {
                    AFTER_DEBIT_INFLIGHT.decrementAndGet();
                }
            });
        }
        return out;
    }

    /**
     * Best-effort post-debit side effects for a single transaction.
     * <ol>
     *   <li>Resolve rebate game_code via {@link com.vinplay.dal.service.BetContextResolver}
     *       (SUN-1201) and resulting rebateActionName via the SUN-865
     *       chain.</li>
     *   <li>Publish {@link LogMoneyUserMessage} to
     *       {@code queue_log_money_user_extra} so agency commission
     *       generation runs (Phase 3e gap-fix carried forward). For
     *       SUN-980/SUN-1175 transfer-ins, use the
     *       {@code "Cq9FishTransfer"} sentinel which
     *       {@code RealTimeCommission.EXCLUDED_ACTIONS} skips. For
     *       SUN-1205/1206 hedge bets where commissionVolume != amount,
     *       set {@link LogMoneyUserMessage#setValidBetAmount} so the
     *       rebate pipeline reads the smaller volume.</li>
     *   <li>Insert / upsert {@code log_gsc_bets} row (SUN-LIVE-HIST).
     *       Skip for SUN-980 transfer-in. Skip for SUN-1184 (amount=0)
     *       — already short-circuited above. Wrapped in
     *       {@code MongoRetry.runWithRetry(...)} for SUN-1108/1110.
     *       On exhaustion: WARN log + Telegram alert.</li>
     * </ol>
     */
    private void afterDebit(GscRequest req,
                            GscRequest.TransactionItem t,
                            GscWithdrawProviderHooks provider,
                            boolean isCq9FishTransfer,
                            long amountSubunit,
                            double validBetAmt,
                            double betAmt,
                            double fxIn,
                            long postDebitBalance) {
        String memberAccount = req.getMemberAccount();
        int productCode = req.getProductCode();
        String txGameCode = t.getGameCode();

        // Single Hazelcast lookup; downstream resolveUserIdForRebate /
        // isBot tolerate null on outage.
        UserCacheModel cached;
        try {
            cached = lookupUserCache(memberAccount);
        } catch (RuntimeException ignored) {
            cached = null;
        }

        // SUN-1201 BetContextResolver — recover game_code via
        // VENDOR/SESSION/DEFAULT layers when vendor sent it empty.
        try {
            com.vinplay.dal.service.BetContextResolver.Context betCtx =
                    com.vinplay.dal.service.BetContextResolver.resolve(
                            memberAccount, productCode, txGameCode);
            if (betCtx != null && betCtx.hasGameCode()) {
                txGameCode = betCtx.gameCode;
                if (betCtx.source != com.vinplay.dal.service.BetContextResolver.Source.VENDOR) {
                    logger.info("[SUN-1201] BetContextResolver recovered game_code via "
                            + betCtx.source + " — member=" + memberAccount
                            + " pc=" + productCode + " gc=" + txGameCode);
                }
            }
        } catch (Throwable ignored) { /* best-effort */ }

        // SUN-865 / SUN-1201 rebate action_name resolution.
        String rebateActionName;
        if (isCq9FishTransfer) {
            // SUN-980 sentinel — must be in
            // RealTimeCommission.EXCLUDED_ACTIONS so downstream rebate
            // pipeline skips it (legacy line 295).
            rebateActionName = "Cq9FishTransfer";
        } else if (productCode > 0 && txGameCode != null && !txGameCode.isEmpty()) {
            rebateActionName = "gsc_" + productCode + "_" + txGameCode;
        } else {
            String fallback = productCode > 0
                    ? safeResolveGameKey(productCode, txGameCode)
                    : null;
            rebateActionName = (fallback != null && !fallback.isEmpty())
                    ? fallback
                    : Games.LIVE_CASINO.getName();
        }
        String rebateServiceName = productCode > 0
                ? String.valueOf(productCode)
                : Games.LIVE_CASINO.getName();

        // SUN-1205/1206 commission-volume override.
        long commissionVolume;
        try {
            commissionVolume = provider.resolveCommissionVolume(validBetAmt, betAmt, amountSubunit, fxIn);
        } catch (Throwable e) {
            commissionVolume = amountSubunit;
        }

        // Compute fee. Mirrors legacy lines 244-247.
        long fee = 0L;
        try {
            int taxPct = config.getTaxPercent();
            if (taxPct > 0) {
                fee = (amountSubunit * taxPct) / 100L;
            }
        } catch (Throwable ignored) { /* default fee=0 */ }

        // Build event_key for log_gsc_bets (matches legacy line 285).
        String eventKey = buildGscBetEventKey(productCode, txGameCode, t.getWagerCode(), t.getId());

        // ── Rebate publish (Phase 3e gap-fix carried to 3f) ──
        // Legacy userMoneyService.bet → UserServiceImpl.updateMoney →
        // MessageBusFactory queue_log_money to queue_log_money_user_extra.
        // MoneyGateway.debitUser does NOT publish — without this,
        // agency commission silently breaks on flag-on traffic.
        try {
            int userId = resolveUserIdForRebate(memberAccount, cached);
            if (userId <= 0) {
                logger.warn("GscWithdrawAggregator: rebate publish skipped — no user_id for "
                        + memberAccount + " wager=" + t.getWagerCode());
            } else {
                boolean isBot = cached != null && cached.isBot();
                // Match legacy WithdrawProcess.java:422-423 byte-for-byte —
                // "Withdraw - <gameCode> : <amount>[ | source=<eventKey>]".
                // The eventKey suffix appears in operator audit reports for
                // log_gsc_bets attribution; keeping the prefix change-free
                // means rebate-pipeline analytics built on this string don't
                // break on flag-flip.
                String description = "Withdraw - "
                        + (txGameCode == null ? "" : txGameCode)
                        + " : " + amountSubunit
                        + (eventKey != null ? " | source=" + eventKey : "");

                LogMoneyUserMessage msg = new LogMoneyUserMessage(
                        userId,
                        memberAccount,
                        rebateActionName,            // actionName
                        rebateServiceName,           // serviceName
                        postDebitBalance,            // currentMoney — post-debit balance
                        -amountSubunit,              // moneyExchange — NEGATIVE for debit
                        Consts.MONEY_VIN,
                        description,
                        fee,
                        true,                        // vp / playgame
                        isBot);

                // SUN-1205/1206: set validBetAmount when commissionVolume
                // differs from raw amount. The rebate pipeline
                // (LogMoneyUserExtraProcessor) reads this when > 0 and
                // computes commission against the smaller volume — money
                // flow stays unchanged (already debited at amount above).
                if (commissionVolume != amountSubunit) {
                    msg.setValidBetAmount(commissionVolume);
                }

                // Stamp referralCode for the agency chain walker (legacy
                // updateMoney line 271 equivalent).
                try {
                    if (cached != null && cached.getReferralCode() != null) {
                        msg.setReferralCode(cached.getReferralCode());
                    }
                } catch (Throwable ignored) { /* defensive */ }

                // SUN-1248 (Mr.DEAL multi-bet fix, ledger-compliant version):
                //   - source-key = per-txn id (idempotent: RMQ redelivery of
                //     the same sub-bet hits the same dedup key, no double-count)
                //   - wagerCode = round-level identifier (read-side GROUPs BY
                //     this in RebateService.queryLogs to aggregate sub-bets
                //     into ONE row per round in Rolling)
                // Both fields ride the message; the writer (vbee
                // LogMoneyUserExtraProcessor) inserts immutable per-txn rows
                // and the reader sums them at display time. This restores
                // the ledger principle "transactions are immutable" while
                // still showing the operator a unified per-round Rolling
                // entry. The earlier "set source-key to wager_code +
                // increment existing row" approach was an in-place mutation
                // that double-counted on RMQ retry.
                String txnId = t.getId();
                if (txnId != null && !txnId.isEmpty()) {
                    msg.setId("gsc:" + txnId);
                }
                String wagerCode = t.getWagerCode();
                if (wagerCode != null && !wagerCode.isEmpty()) {
                    msg.setWagerCode(wagerCode);
                }

                publishRebateMessage(msg);
            }
        } catch (Throwable rebateErr) {
            // Best-effort. Failing the publish must NOT fail the wallet
            // debit (already committed) or the Mongo insert that follows.
            logger.warn("GscWithdrawAggregator: rebate publish failed wager="
                    + t.getWagerCode() + " member=" + memberAccount
                    + " amount=" + amountSubunit
                    + " err=" + rebateErr.getMessage());
        }

        // ── log_gsc_bets insert — async via RMQ (Phase 5 prep gate 5p2) ──
        // SUN-LIVE-HIST + SUN-1196 + SUN-1108/1110 preserved; the actual
        // Mongo write + Telegram alert run in api/vbee's
        // GscBetSideEffectProcessor off the request hot path. Wallet
        // credit/debit stays synchronous (correctness invariant); only
        // the post-wallet bookkeeping moves async. See
        // GscBetSideEffectMessage class doc for the safety analysis.
        if (isCq9FishTransfer) {
            logger.info("[SUN-980] skipping log_gsc_bets insert for CQ9 fish transfer-in tx="
                    + t.getId());
            return;
        }
        // SUN-1184 already handled above (amount<=0 short-circuits dispatchOne).
        String displayName = rebateActionName;
        try {
            if (productCode > 0) {
                displayName = com.vinplay.dal.service.GscGameNameResolver
                        .displayName(productCode, txGameCode);
            }
        } catch (Throwable ignored) { /* fallback to rebateActionName */ }

        // SUN-1196 freespin chain link.
        String vendorGameId = null;
        try {
            vendorGameId = provider.resolveLinkId(t.getPayload(), t.getRoundId());
        } catch (Throwable ignored) { /* defensive */ }

        GscBetSideEffectMessage sfx = GscBetSideEffectMessage.of(
                GscBetSideEffectMessage.Op.BET_INSERT,
                "GscWithdraw",
                memberAccount,
                productCode,
                txGameCode,
                t.getWagerCode(),
                t.getId(),
                System.currentTimeMillis());
        sfx.amount = amountSubunit;
        sfx.fee = fee;
        // SUN-1248 / Phase 2: stamp pre-debit balance so the Mongo doc
        // carries current_money. postDebitBalance is the player's vin
        // AFTER the debit; pre-debit = post + amount we just deducted.
        // Powers near-real-time money_before in agency LS Cược / c=9843.
        sfx.currentMoneyBefore = postDebitBalance + amountSubunit;
        sfx.eventKey = eventKey;
        sfx.gameKey = rebateActionName;
        sfx.gameName = displayName;
        sfx.currency = req.getCurrency();
        sfx.rawAmount = t.getAmount();
        sfx.action = t.getAction();
        sfx.vendorGameId = vendorGameId;
        // P6.4 — propagate the resolved user_id so the consumer's
        // MySQL dual-write doesn't have to re-resolve via Hazelcast +
        // MySQL fallback (which can return 0 and pollute reader queries).
        // Reuse the {@code cached} loaded at the top of afterDebit (the
        // rebate publish above used the same value). One Hazelcast
        // round-trip per afterDebit instead of two.
        try {
            int resolvedUserId = resolveUserIdForRebate(memberAccount, cached);
            if (resolvedUserId > 0) sfx.userId = resolvedUserId;
        } catch (Throwable ignored) { /* leave sfx.userId = 0 — consumer falls back */ }
        // Telegram alert fires only if the consumer's MongoRetry
        // exhausts; mirrors legacy alert wording ("withdraw insertOne").
        sfx.telegramAlertSubject = "withdraw insertOne";
        publishBetSideEffect(sfx);
    }

    /**
     * Publish the {@link GscBetSideEffectMessage} so Mongo and Telegram
     * side effects run async via {@code GscBetSideEffectProcessor} in
     * {@code api/vbee}. {@code protected} so tests can override and
     * capture without touching RabbitMQ. Default delegates to
     * {@link GscBetSideEffectPublisher#publish}, which itself wraps
     * RMQ-failure with a sync-fallback.
     */
    protected void publishBetSideEffect(GscBetSideEffectMessage msg) {
        GscBetSideEffectPublisher.publish(msg);
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
     * (free if cached), MySQL fallback. Returns 0 if nothing found.
     * Mirrors Phase 3e Deposit.
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
            logger.warn("GscWithdrawAggregator.resolveUserIdForRebate fallback failed for "
                    + memberAccount + ": " + e.getMessage());
        }
        return 0;
    }

    /**
     * Publish the rebate {@link LogMoneyUserMessage} to the
     * {@code queue_log_money} pipeline. {@code protected} so tests can
     * override and capture without touching the underlying transport.
     * The default delegates to {@link MessageBusFactory}; the 3-way
     * fanout (queue_log_money cmd 601 → queue_log_money_extra cmd 1001
     * → queue_log_report_user_balance cmd 602) is encoded in
     * {@code QueueRouter.route("queue_log_money", 601)}, consulted by
     * both the RMQ and Redis-Streams adapters. M-log_money note:
     * {@code MessageBus.publish} declares no checked exceptions and
     * log-and-swallows transport errors per its contract, so the
     * {@code throws Exception} clause is kept for ABI compatibility
     * with test overrides but the default path no longer throws.
     */
    protected void publishRebateMessage(LogMoneyUserMessage msg) throws Exception {
        MessageBusFactory.get("queue_log_money").publish("queue_log_money", msg, 601);
    }

    private static double parseDoubleSafe(String s) {
        if (s == null || s.isEmpty()) return 0d;
        try {
            return Double.parseDouble(s);
        } catch (Throwable t) {
            return 0d;
        }
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
     * SUN-1373 — returns {@code true} when {@code GSC_BET_WHITELIST_ENFORCE}
     * is set to {@code "true"} or {@code "1"} in the environment.
     * Defaults OFF so existing deployments are unaffected until the flag is
     * explicitly added to {@code .env}.
     */
    private static boolean isWhitelistEnforced() {
        String v = System.getenv("GSC_BET_WHITELIST_ENFORCE");
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    /**
     * Existence check: Hazelcast {@code users} IMap fast-path, MySQL
     * fall-back.
     *
     * <p>SUN-1245 (2026-05-03): the legacy code (mirrored in v1) only
     * checked Hazelcast. That returned false for any logged-out user
     * even when MySQL held a valid row, which broke GSC withdraw
     * compliance test "Withdraw and Deposit" — `laviai` (vin=2.87M)
     * was rejected with "Member not found" simply because they
     * weren't currently in the cache. The GSC certification test
     * driver runs on a fresh staging URL where no user is ever in
     * cache, so 100% of withdraw tests fail without the DB fallback.
     *
     * <p>Cache hit short-circuits with no DB call (fast path for the
     * common case during play). Cache miss issues a single primary-
     * key-equivalent SELECT against {@code users} (indexed on
     * {@code nick_name}, also matches {@code user_name} for the
     * SUN-1227 V4 case where the two diverge).
     */
    // SUN-1378 p99 fix — short-lived in-process cache for memberAccountExists.
    // Every GSC withdraw/balance hits this; without a cache each call does a
    // Hazelcast getMap("users").containsKey() round-trip (30-80ms on a single-
    // node staging cluster) + optional MySQL fallback (30-60ms more). TTL=60s
    // is short enough that a banned/deleted account stops authorising within
    // a minute; cache.putIfAbsent only stores positive hits so a wrong
    // negative never sticks.
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> MEMBER_EXISTS_CACHE
            = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long MEMBER_EXISTS_TTL_MS = 60_000L;

    private boolean memberAccountExists(String memberAccount) {
        if (memberAccount == null || memberAccount.isEmpty()) return false;
        Long cachedExpiresAt = MEMBER_EXISTS_CACHE.get(memberAccount);
        long now = System.currentTimeMillis();
        if (cachedExpiresAt != null && cachedExpiresAt > now) return true;
        boolean exists = memberAccountExistsUncached(memberAccount);
        if (exists) MEMBER_EXISTS_CACHE.put(memberAccount, now + MEMBER_EXISTS_TTL_MS);
        return exists;
    }

    private boolean memberAccountExistsUncached(String memberAccount) {
        try {
            IMap<String, UserCacheModel> userMap =
                    HazelcastClientFactory.getInstance().getMap("users");
            if (userMap.containsKey(memberAccount)) return true;
        } catch (Throwable t) {
            // Hazelcast unavailable — drop straight through to DB
            // fallback (more conservative than the v1 "return true"
            // posture, which would let invalid usernames through).
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
            // Conservative: treat DB error as "exists" so the wallet
            // primitive's atomic UPDATE makes the final decision.
            return true;
        }
    }

    /**
     * Read a user's balance for the before/after numbers.
     *
     * <p>SUN-1xxx (2026-05-11): MySQL-only, no cache fallback. Per
     * docs/architecture/LEDGER_HARDENING_ROADMAP.md, any balance read
     * that gates a money movement must read the canonical store
     * directly. Hazelcast {@code users} IMap is a cache that drifts
     * (signup-seed staleness, lock-timeout evictions, HZ disconnect
     * leaving stale entries). The previous HZ-first ordering caused
     * a class of "ghost insufficient balance" rejections for fresh
     * accounts: testgiftcode001 had {@code users.vin = 20,000} post-
     * giftcode, HZ held the signup-seed {@code vin = 0}, every Ice
     * Fishing bet returned 1001 while the lobby correctly showed 20k.
     *
     * <p>Concrete evidence: 12 consecutive bets failed 2026-05-11
     * 22:42:12–22:53:22 KST in {@code gsc_event_log}; same fingerprint
     * hit Hongmy, Dungle111, Sibai213, fafaads, thuankr01.
     *
     * <p>If the canonical read throws, we return 0 — that flows into
     * the {@code totalBet > 0} check and rejects the bet with a clean
     * 1001 INSUFFICIENT_BALANCE. That is the correct posture: reject
     * the bet rather than fall back to a stale-prone cache and risk
     * letting through an under-funded debit. {@code MoneyGateway.debitUser}'s
     * atomic {@code UPDATE … WHERE vin >= ?} still gates the actual
     * money movement; this pre-check exists only to give GSC a single
     * fail-fast outcome for the batch, and it must read the same source
     * of truth the wallet primitive reads.
     *
     * <p>Cost: one MySQL round-trip (~1 ms) per GSC bet. No cache hit.
     */
    private long balanceForUser(String memberAccount) {
        if (memberAccount == null || memberAccount.isEmpty()) return 0L;
        try {
            SeamlessOutcome r = doReadBalance(memberAccount);
            if (r != null && r.status == SeamlessOutcome.Status.POSTED) {
                return r.newBalance;
            }
            // userNotFound / serverError → 0 — reject the bet rather than
            // guess at the balance from a possibly-stale cache.
            return 0L;
        } catch (Throwable t) {
            logger.warn("GscWithdrawAggregator.balanceForUser MySQL read failed user="
                    + memberAccount + " err=" + t.getMessage()
                    + " — returning 0 to reject the bet (no cache fallback per balance-read policy)");
            return 0L;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Outcome helpers
    // ─────────────────────────────────────────────────────────────────

    private static final int SC_SUCCESS               = 0;
    private static final int SC_INTERNAL_SERVER_ERROR = 999;
    private static final int SC_MEMBER_NOT_EXIST      = 1000;
    private static final int SC_INSUFFICIENT_BALANCE  = 1001;
    private static final int SC_INCORRECT_AGENT_KEY   = 1002;
    /** SUN-1245: GSC certification "Withdrawal Duplicate" expects code 1003. */
    private static final int SC_DUPLICATE_TRANSACTION = 1003;
    private static final int SC_INVALID_SIGNATURE     = 1004;
    /** Used to refuse bets on non-whitelisted (or blocked) tables — Evo can
     *  display "table under maintenance" without deducting the wallet. */
    private static final int SC_PRODUCT_UNDER_MAINTENANCE = 2000;
    /** SUN-1373 — game administratively disabled. Not in GSC's own spec; we use 1005 on our side. */
    private static final int SC_GAME_INACTIVE         = 1005;

    private static SeamlessOutcome errorOutcome(int code, String message) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.WITHDRAW_ERROR);
        meta.put("code", code);
        meta.put("message", message);
        return SeamlessOutcome.validationError(String.valueOf(code), message, meta);
    }

    private static SeamlessOutcome insufficientBalanceOutcome(double before, double balance) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.WITHDRAW_ERROR);
        meta.put("code", SC_INSUFFICIENT_BALANCE);
        meta.put("message", "Insufficient balance");
        meta.put("before_balance", before);
        meta.put("balance", balance);
        return SeamlessOutcome.validationError(String.valueOf(SC_INSUFFICIENT_BALANCE),
                "Insufficient balance", meta);
    }

    /**
     * Empty {@code BalanceResponse} fall-back (legacy lines 117-121).
     * Used for unknown-currency only. Wire shape:
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
        if (out == null) return withdrawErrorJson(SC_INTERNAL_SERVER_ERROR, "Server Error");
        switch (out.status) {
            case SIGNATURE_ERROR:
                return withdrawErrorJson(SC_INVALID_SIGNATURE, "Invalid sign");
            case SERVER_ERROR:
                return withdrawErrorJson(SC_INTERNAL_SERVER_ERROR, "Server Error");
            case POSTED:
            case DUPLICATE: {
                // POSTED + DUPLICATE share the same metadata-driven shape.
                // dispatch() wraps both in `posted(internalBalance, meta)` after
                // the loop; the per-tx DUPLICATE returned by doDebit is
                // collapsed into the post-loop POSTED via isSuccessful(...).
                // The DUPLICATE branch here is defense-in-depth: if a future
                // code path lets a raw doDebit-DUPLICATE bubble up without
                // metadata, we still return a balance-fallback shape rather
                // than leaking internal subunits as the visible balance.
                Map<String, Object> meta = out.metadata;
                if (meta == null) {
                    // Direct DUPLICATE without metadata (test path or future
                    // bypass) — return balance-fallback so we never leak the
                    // raw subunit as an externalized number.
                    return balanceFallbackJson();
                }
                GscResponseShape shape = GscResponseShape.from(meta, GscResponseShape.WITHDRAW);
                if (shape == GscResponseShape.BALANCE_FALLBACK) {
                    return balanceFallbackJson();
                }
                int code = meta.get("code") instanceof Integer
                        ? (Integer) meta.get("code") : SC_SUCCESS;
                String msg = meta.get("message") instanceof String
                        ? (String) meta.get("message") : "";
                double before = meta.get("before_balance") instanceof Number
                        ? ((Number) meta.get("before_balance")).doubleValue() : 0.0;
                double after = meta.get("balance") instanceof Number
                        ? ((Number) meta.get("balance")).doubleValue() : 0.0;
                if (code == SC_SUCCESS) {
                    return withdrawSuccessJson(msg, before, after);
                }
                // SUN-1245: GSC certification "Withdrawal Duplicate" expects
                // ERROR-shape (data:null) with envelope code=1003 — same wire
                // format as Insufficient/InvalidSign. The wallet primitive
                // already short-circuits the duplicate via (tx_id, source)
                // UNIQUE so balance is unchanged, but GSC's test driver
                // checks for the bare error envelope rather than a balance-
                // bearing payload.
                return withdrawErrorJson(code, msg);
            }
            case VALIDATION_ERROR: {
                Map<String, Object> meta = out.metadata;
                GscResponseShape shape = GscResponseShape.from(meta, GscResponseShape.WITHDRAW_ERROR);
                if (shape == GscResponseShape.BALANCE_FALLBACK) {
                    return balanceFallbackJson();
                }
                int code = meta != null && meta.get("code") instanceof Integer
                        ? (Integer) meta.get("code") : SC_INTERNAL_SERVER_ERROR;
                String msg = meta != null && meta.get("message") instanceof String
                        ? (String) meta.get("message") : "Server Error";
                return withdrawErrorJson(code, msg);
            }
            case INSUFFICIENT_BALANCE:
                return withdrawErrorJson(SC_INSUFFICIENT_BALANCE, "Insufficient balance");
            case USER_NOT_FOUND:
                return withdrawErrorJson(SC_MEMBER_NOT_EXIST, "Member not found");
            case GAME_INACTIVE:
                // SUN-1373 — game disabled by admin (c=9982 gsc_game_catalog.active=0).
                // Internal code 0011 / GAME_INACTIVE so FE can distinguish from
                // generic wallet rejections and show a "table closed" popup.
                return withdrawErrorJson(SC_GAME_INACTIVE, "GAME_INACTIVE");
            default:
                return withdrawErrorJson(SC_INTERNAL_SERVER_ERROR, "Server Error");
        }
    }

    /**
     * Serialize the legacy success {@code WithdrawResponse.toJson()}
     * shape verbatim. Inner data carries before_balance + balance only
     * (NO inner code/message — this is the key difference from Deposit).
     */
    private static String withdrawSuccessJson(String message, double before, double balance) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0.####", sym);
        String bb = df.format(before);
        String b  = df.format(balance);
        String msgEsc = escapeJsonString(message == null ? "" : message);
        return "{\"code\":0,\"message\":\"" + msgEsc + "\",\"data\":[{\"before_balance\":"
                + bb + ",\"balance\":" + b + "}]}";
    }


    /**
     * Serialize the legacy failure {@code WithdrawResponse.toJson()}
     * shape verbatim: {@code data:null} (NOT an empty array — this is
     * a quirk of the legacy DTO).
     */
    private static String withdrawErrorJson(int code, String message) {
        String msgEsc = escapeJsonString(message == null ? "" : message);
        return "{\"code\":" + code + ",\"message\":\"" + msgEsc + "\",\"data\":null}";
    }

    /**
     * Minimal JSON-string escaper.
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
     * {@code data} list.
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
        return GscEventLogger.tryLogRequest("withdraw", rawBody, http);
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
     * Resolve {@code games.category_id} for a (provider, vendor_platform,
     * game_code) tuple so the {@code UserGameBlock} matcher can apply
     * "block all baccarat" style rules. Returns {@code null} when no
     * row matches — the matcher then falls through to per-game / per-
     * platform predicates only.
     */
    private static Integer lookupCategoryId(String provider, String vendorPlatform, String gameCode) {
        if (provider == null || provider.isEmpty() || vendorPlatform == null || vendorPlatform.isEmpty()
                || gameCode == null || gameCode.isEmpty()) {
            return null;
        }
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                        .getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT category_id FROM games WHERE provider = ? "
                             + "AND vendor_platform COLLATE utf8mb4_unicode_ci = ? "
                             + "AND game_code COLLATE utf8mb4_unicode_ci = ? LIMIT 1")) {
            ps.setString(1, provider);
            ps.setString(2, vendorPlatform);
            ps.setString(3, gameCode);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignore) {}
        return null;
    }
}
