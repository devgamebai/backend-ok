package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.dal.audit.GscEventLogger;
import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.dal.service.seamless.SeamlessOutcome;
import com.vinplay.dal.service.seamless.SeamlessTxn;
import com.vinplay.dal.service.seamless.SeamlessWalletAggregator;
import com.vinplay.dal.service.seamless.VerifyResult;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.statics.Consts;
import com.hazelcast.core.IMap;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3c — GSC Cancel handler implemented on top of
 * {@link SeamlessWalletAggregator}. Mirrors the legacy
 * {@code game.third.hooks.gscSeamless.CancelProcess} at the wire-response
 * level, gated behind {@code GSC_AGGREGATOR_CANCEL_ENABLED}.
 *
 * <h2>Why Cancel is a CREDIT (refund-only) handler</h2>
 * Unlike {@code TransferProcess}, the legacy {@code CancelProcess} does
 * NOT route through {@code CommonProcess.TransactionAction} — it calls
 * {@code userMoneyService.reward(...)} directly inside its loop
 * ({@code CancelProcess.java:125}), passing a positive amount. That is
 * the correct CREDIT direction: a cancel reverses a prior bet by
 * returning money to the player.
 *
 * <p>The 13-case TransactionAction switch in {@code CommonProcess.java}
 * does include a {@code "CANCEL"} branch (line 52-56), but that branch
 * is only reachable through {@code TransferProcess}, never through
 * {@code CancelProcess}. The {@code TransferProcess} CANCEL routing is
 * one of the eight {@code actionReward}-bug-affected paths that Phase 3b
 * already corrected (legacy debits, aggregator credits — see
 * {@link GscTransferAggregator} class doc). Phase 3c does not need to
 * worry about that path.
 *
 * <p>So {@code GscCancelAggregator} maps every transaction in its
 * {@code transactions[]} array unconditionally to a CREDIT (refund) —
 * there is no per-transaction action verb to switch on. The
 * {@code CancelRequest.Transaction} DTO does not even have an
 * {@code action} field; its discriminators are
 * {@code wager_status} / {@code wager_type} which are informational, not
 * directional.
 *
 * <h2>External-ref namespacing</h2>
 * The {@code money_gateway_log} UNIQUE on {@code (tx_id, source, user_id)}
 * means a cancel and the original Withdraw (BET) for the same wager
 * must hash to different rows. Cancel-side events use the wager_code
 * (the GSC-stable id we have on the cancel payload — there is no
 * per-event {@code id} on {@code CancelRequest.Transaction}) prefixed
 * by {@code "cancel_"}: {@code cancel_<wager_code>}. This both
 * disambiguates from the Withdraw row ({@code <wager_code>} or
 * {@code transfer_*_<id>} from Phase 3b) and namespaces against any
 * future Phase 3d Rollback handler that might also key off wager_code.
 *
 * <h2>Multi-transaction failure semantics</h2>
 * Legacy aborts the loop on the first failed transaction and returns
 * an empty {@code BalanceResponse} ({@code CancelProcess.java:129-131}).
 * The aggregator preserves that — we surface the failure as a
 * {@code shape="balance"} fall-back outcome that
 * {@link #serializeResponse} renders as
 * {@code {"code":0,"message":"","data":[]}}.
 *
 * <h2>Sign verification</h2>
 * Single verb {@code "cancel"}: {@code md5(operator_code + request_time
 * + "cancel" + secret)}. Mirrors {@code CancelProcess.java:92} verbatim.
 *
 * <h2>Audit endpoint string</h2>
 * Matches the legacy: {@code "cancel"} ({@code CancelProcess.java:51}).
 *
 * <h2>Production volume</h2>
 * Verified: 0 events on {@code cancel} endpoint in the
 * {@code gsc_event_log} table. The fixtures are therefore synthetic
 * (built from the documented {@code CancelRequest} schema), same as
 * Phase 3b's TransferProcess fixtures.
 *
 * <h2>Side-effects NOT migrated</h2>
 * The legacy {@code CancelProcess} additionally:
 * <ul>
 *   <li>Calls {@code RebateService.reverseGscByWagerCode} to roll back
 *       agency commission rows ({@code CancelProcess.java:139-150}).
 *   <li>Drops {@code log_gsc_bets} rows so cancelled bets disappear
 *       from c=303 / c=9843 history ({@code CancelProcess.java:160-175}).
 *   <li>Invalidates response-cache for affected agents
 *       ({@code CancelProcess.java:181-189}).
 * </ul>
 *
 * <p>Per the Phase 3 design directive ({@code design.md} §3 — "preserves
 * all current per-provider quirks by delegating into the existing ...
 * inside dispatch"), these side-effects need to be threaded through the
 * aggregator before the flag flips ON in production. They are NOT
 * threaded through in this initial Phase 3c PR — the flag stays OFF in
 * prod, and a follow-up commit will wire the rebate reverse + log_gsc_bets
 * cleanup once the dispatch surface is reviewed. The wallet-refund call
 * (the user-facing contract) IS migrated in this PR; only the auxiliary
 * agency / history clean-up steps are deferred.
 */
public class GscCancelAggregator extends SeamlessWalletAggregator<GscRequest, String> {

    private final GscConfigProvider config;

    public GscCancelAggregator(GscConfigProvider config) {
        if (config == null) throw new IllegalArgumentException("config is null");
        this.config = config;
    }

    /** Tighter audit-log tag than the default class-simple-name. */
    @Override
    protected String aggregatorName() { return "GSC"; }

    /** Phase 5p3 — distinct timing-metrics bucket per handler. */
    @Override
    protected String metricsName() { return "GscCancel"; }

    // ─────────────────────────────────────────────────────────────────
    // parseRequest
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse the inbound JSON into a {@link GscRequest}. The legacy
     * {@code CancelRequest.fromJson} returns null on a malformed body
     * which is then mapped to {@code "Invalid JSON format"}; we mirror
     * that by returning an all-null {@link GscRequest} and letting
     * {@link #dispatch} flag it.
     *
     * <p><b>Field-mapping note.</b> {@code CancelRequest.Transaction}
     * has no {@code id} / {@code action} / {@code amount} / {@code bet_amount}
     * (string) fields — its shape is
     * {@code {member_account, wager_code, wager_status, wager_type,
     * product_code, bet_amount(double), valid_bet_amount, prize_amount,
     * tip_amount, game_type, settled_at}}. We project that into the
     * shared {@link GscRequest.TransactionItem} DTO by:
     * <ul>
     *   <li>{@code id} ← {@code wager_code} (only stable id we have);
     *   <li>{@code action} ← hard-coded {@code "CANCEL"} (every cancel
     *       transaction is a refund — no per-transaction direction
     *       discriminator);
     *   <li>{@code betAmount} ← {@code bet_amount} stringified;
     *   <li>{@code productCode} ← {@code product_code} parsed as int
     *       (legacy's {@code product_code} on Cancel is a String —
     *       weakly typed; we parse defensively).
     * </ul>
     */
    @Override
    protected GscRequest parseRequest(String body, HttpServletRequest http) {
        if (body == null || body.isEmpty()) {
            return new GscRequest(null, null, null, null, null, null, null,
                    null, java.util.Collections.<GscRequest.TransactionItem>emptyList());
        }
        JSONObject root;
        try {
            root = new JSONObject(body);
        } catch (Throwable t) {
            return new GscRequest(null, null, null, null, null, null, null,
                    null, java.util.Collections.<GscRequest.TransactionItem>emptyList());
        }

        String operatorCode = optStr(root, "operator_code");
        String currency     = optStr(root, "currency");
        String sign         = optStr(root, "sign");
        String requestTime  = optStr(root, "request_time");

        JSONArray txArr = root.optJSONArray("transactions");
        List<GscRequest.TransactionItem> transactions = new ArrayList<>();
        if (txArr != null) {
            for (int i = 0; i < txArr.length(); i++) {
                JSONObject t = txArr.optJSONObject(i);
                if (t == null) continue;
                String memberAccount = optStr(t, "member_account");
                String wagerCode     = optStr(t, "wager_code");
                String productCodeS  = optStr(t, "product_code");
                int productCode      = parseIntDefault(productCodeS, 0);
                String gameCode      = optStr(t, "game_type"); // CancelRequest uses game_type, not game_code
                // bet_amount on CancelRequest is a JSON number (double) per the DTO,
                // but we tolerate string form too (defensive — fixtures may emit either).
                String betAmount     = optNumberAsString(t, "bet_amount");
                transactions.add(new GscRequest.TransactionItem(
                        /*id*/ wagerCode,
                        /*action*/ "CANCEL",
                        memberAccount,
                        productCode,
                        gameCode,
                        /*currency*/ null,
                        wagerCode,
                        /*amount*/ null,
                        betAmount));
            }
        }

        // Top-level memberAccount: CancelRequest doesn't have one — the legacy
        // code reads transactions.get(0).getMember_account(). We mirror that
        // by promoting the first transaction's member_account up.
        String memberAccount = transactions.isEmpty() ? null : transactions.get(0).getMemberAccount();

        return new GscRequest(operatorCode, currency, sign, requestTime,
                memberAccount, /*productCode*/ null, /*gameType*/ null,
                /*batchMembers*/ null, transactions);
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

    /**
     * Read {@code bet_amount} as a string regardless of whether the JSON
     * encodes it as a number or a string. Legacy {@code CancelRequest}
     * declares {@code bet_amount} as a {@code double} so Gson would parse
     * a JSON number directly; the aggregator's {@code GscRequest.TransactionItem}
     * only stores strings (mirroring Withdraw/Transfer's wire shape), so
     * we coerce here.
     */
    private static String optNumberAsString(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return null;
        try {
            // Try string first (most permissive).
            String s = o.optString(key, null);
            if (s != null && !s.isEmpty()) return s;
        } catch (Throwable ignored) {}
        try {
            double d = o.getDouble(key);
            // Avoid scientific notation / trailing .0 noise on integral values.
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int parseIntDefault(String s, int fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return Integer.parseInt(s.trim()); }
        catch (Throwable t) { return fallback; }
    }

    // ─────────────────────────────────────────────────────────────────
    // verifySignature
    // ─────────────────────────────────────────────────────────────────

    /**
     * Single verb {@code "cancel"} — verified against
     * {@code CancelProcess.java:92}: {@code HashUtil.md5(operatorCode +
     * requestTime + "cancel" + secretKey)}. No fall-back verbs (unlike
     * Phase 3a PushBet which accepts both {@code "pushbet"} and
     * {@code "pushbetdata"}).
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

        String expected = md5Hex(req.getOperatorCode() + req.getRequestTime() + "cancel" + secret);
        if (!req.getSign().equals(expected)) {
            return VerifyResult.fail("signature mismatch");
        }
        return VerifyResult.ok();
    }

    // ─────────────────────────────────────────────────────────────────
    // mapActionToSource / currency conversion
    // ─────────────────────────────────────────────────────────────────

    /**
     * Cancel is unconditionally a CREDIT (refund). The base-class
     * {@link SeamlessWalletAggregator#doCredit} is the only wallet
     * primitive this aggregator invokes, so this method always returns
     * {@link MoneyGateway#SOURCE_GSC_CREDIT}. The {@code aggregatorAction}
     * argument is ignored — it will always be the literal string
     * {@code "CANCEL"} that {@link #parseRequest} hard-coded.
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
    // dispatch — the per-transaction loop
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected SeamlessOutcome dispatch(GscRequest req) {
        // Invalid-JSON / null request → "Invalid JSON format"
        // (CancelProcess.java:71-74). We use INCORRECT_AGENT_KEY (1002)
        // as the legacy code does on this branch.
        if (req == null
                || (req.getOperatorCode() == null
                && req.getCurrency() == null
                && req.getTransactions().isEmpty()
                && req.getSign() == null
                && req.getRequestTime() == null)) {
            return errorOutcome(SC_INCORRECT_AGENT_KEY, "Invalid JSON format");
        }

        List<GscRequest.TransactionItem> transactions = req.getTransactions();
        // Required-parameter check (mirrors CancelProcess.java:85-88).
        // Note: CancelProcess does NOT validate member_account separately —
        // it consults transactions[0].member_account at use-time. We still
        // require the transactions array to be non-null.
        if (req.getOperatorCode() == null
                || req.getCurrency() == null
                || transactions == null
                || req.getSign() == null
                || req.getRequestTime() == null) {
            return errorOutcome(SC_INCORRECT_AGENT_KEY, "Missing required parameters");
        }

        // Currency lookup. Unknown → empty BalanceResponse fall-back
        // (CancelProcess.java:101-105).
        int currencyFactor = config.getCurrencyExchangeRate(req.getCurrency());
        if (currencyFactor == 0) {
            return balanceFallbackOutcome();
        }

        // Empty transactions list — legacy doesn't explicitly handle this
        // (it would fall through the for-loop and try to read
        // transactions.get(0).getMember_account() which throws). We treat
        // an empty list as a missing-required-parameter — safer than the
        // legacy NPE.
        if (transactions.isEmpty()) {
            return errorOutcome(SC_INCORRECT_AGENT_KEY, "Missing required parameters");
        }

        String memberAccount = req.getMemberAccount();
        if (memberAccount == null) {
            // First transaction's member_account was null → no one to
            // refund. Legacy would NPE here; we fail cleanly.
            return errorOutcome(SC_INCORRECT_AGENT_KEY, "Missing required parameters");
        }

        // Wager-code existence check (CancelProcess.java:108-115). The
        // legacy code consults a Hazelcast IMap "gsc_wager_codes" and
        // rejects with BET_NOT_EXIST on the first miss. We preserve that
        // when Hazelcast is reachable; we fall back to skipping the check
        // when the cluster is unavailable (test harness — same
        // conservative posture as Phase 3a/3b).
        for (GscRequest.TransactionItem t : transactions) {
            String wc = t.getWagerCode();
            if (wc == null || !wagerCodeExists(wc)) {
                return errorOutcome(SC_BET_NOT_EXIST, "Bet not exist");
            }
        }

        // Read before-balance for the response (in INTERNAL units, like
        // legacy line 118). The legacy uses userMoneyService.getBalance
        // (Hazelcast); we use the same Hazelcast-first / DB-fallback
        // pattern as Phase 3b.
        long beforeBalanceInternal = balanceForUser(memberAccount);

        // Dispatch each transaction: each is a CREDIT. Per legacy: any
        // failure aborts the loop and returns the empty BalanceResponse
        // shape (CancelProcess.java:128-131).
        for (GscRequest.TransactionItem t : transactions) {
            SeamlessOutcome perTxn = dispatchOne(req, t);
            if (!isSuccessful(perTxn)) {
                logger.warn("GscCancelAggregator: txn failed, aborting batch."
                        + " wager_code=" + t.getWagerCode()
                        + " status=" + (perTxn == null ? "null" : perTxn.status));
                return balanceFallbackOutcome();
            }
        }

        // After the loop, fetch the post-loop balance for the response.
        long afterBalanceInternal = balanceForUser(memberAccount);

        // Cancel's legacy returns before_balance in INTERNAL units
        // (CancelProcess.java:118 — userMoneyService.getBalance, no
        // multiplication) and after_balance in EXTERNAL units (line 191
        // multiplies by getExchangeRateOut). We preserve that asymmetry
        // verbatim so the wire shape matches operationally-captured
        // Cancel responses.
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.CANCEL);
        meta.put("code", SC_SUCCESS);
        meta.put("message", "Cancel processed successfully");
        meta.put("before_balance", (double) beforeBalanceInternal);
        meta.put("balance", currencyToExternal(afterBalanceInternal, req.getCurrency()));
        return SeamlessOutcome.posted(afterBalanceInternal, meta);
    }

    /**
     * Dispatch a single cancel transaction. Always a CREDIT; returns
     * a successful outcome (POSTED or DUPLICATE) on success — anything
     * else is a failure that aborts the parent loop.
     */
    private SeamlessOutcome dispatchOne(GscRequest req, GscRequest.TransactionItem t) {
        if (t == null) {
            return SeamlessOutcome.serverError(null, "null transaction");
        }
        String wagerCode = t.getWagerCode();
        if (wagerCode == null || wagerCode.isEmpty()) {
            logger.warn("GscCancelAggregator: transaction missing wager_code");
            return SeamlessOutcome.serverError(null, "missing wager_code");
        }

        // Resolve amount. CancelRequest.bet_amount is a number (double)
        // but our DTO holds the string form; either way it parses here.
        String amountRaw = t.getBetAmount();
        double providerAmount;
        try {
            providerAmount = (amountRaw != null && !amountRaw.isEmpty())
                    ? Double.parseDouble(amountRaw) : 0.0;
        } catch (NumberFormatException e) {
            logger.warn("GscCancelAggregator: bad bet_amount '" + amountRaw + "' for wager " + wagerCode);
            return SeamlessOutcome.serverError(null, "bad amount");
        }

        long amountSubunit = currencyToInternal(providerAmount, req.getCurrency());
        if (amountSubunit <= 0L) {
            // Zero-amount cancel — nothing to credit. Treat as a
            // success-skip so the batch continues (legacy
            // userMoneyService.reward with 0 short-circuits with success
            // in updateMoney).
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("skipped", Boolean.TRUE);
            meta.put("reason", "zero-amount");
            return SeamlessOutcome.posted(0L, meta);
        }

        // External-ref namespacing: prefix the wager_code by "cancel_" so
        // a Cancel and the original Withdraw (BET) for the same wager
        // hash to different rows in money_gateway_log.uk_tx_source.
        // Phase 3d Rollback (when it lands) should pick a different
        // prefix (e.g. "rollback_") to keep all three orthogonal.
        String externalRef = "cancel_" + wagerCode;

        // memberAccount on each cancel transaction is the player to
        // refund; legacy uses transaction.getMember_account() per-loop
        // (CancelProcess.java:125), NOT the top-level field. We mirror.
        String memberAccount = t.getMemberAccount();
        if (memberAccount == null || memberAccount.isEmpty()) {
            // Fall back to top-level — the parser promotes the first
            // tx's member_account, but later tx's member_account being
            // null shouldn't break the batch; we use the resolved
            // top-level value the legacy implicitly relied on.
            memberAccount = req.getMemberAccount();
        }

        SeamlessTxn txn = new SeamlessTxn(
                memberAccount,
                externalRef,
                "CANCEL",
                amountSubunit,
                req.getCurrency(),
                /*metadata*/null);

        // Always CREDIT. The base-class doCredit handles dedup via
        // uk_tx_source and maps gateway errors back to typed
        // SeamlessOutcome statuses.
        return doCredit(txn);
    }

    private static boolean isSuccessful(SeamlessOutcome out) {
        return out != null
                && (out.status == SeamlessOutcome.Status.POSTED
                || out.status == SeamlessOutcome.Status.DUPLICATE);
    }

    /**
     * True iff {@code wagerCode} is registered in the Hazelcast
     * {@code gsc_wager_codes} IMap. Mirrors {@code CancelProcess.java:108-115}.
     * When Hazelcast is unreachable (test harness, transient cluster
     * outage), we return {@code true} to avoid a blanket BET_NOT_EXIST
     * rejection — the wallet primitive will still dedup on
     * {@code uk_tx_source}, so a refund of a non-existent wager
     * silently no-ops on retry. (The legacy code's behavior in the same
     * Hazelcast-down scenario is undefined: it would NPE on
     * {@code containsKey} of a null map.)
     */
    private boolean wagerCodeExists(String wagerCode) {
        try {
            IMap<String, String> wagerMap =
                    HazelcastClientFactory.getInstance().getMap("gsc_wager_codes");
            return wagerMap.containsKey(wagerCode);
        } catch (Throwable t) {
            // Hazelcast unavailable. Conservative posture: skip the
            // check and let the wallet primitive's dedup do the
            // protection. WARN so ops sees outages — silent swallow
            // would mask sustained Hazelcast failures.
            logger.warn("GscCancelAggregator: gsc_wager_codes Hazelcast lookup failed,"
                    + " skipping wager-existence check: " + t.getMessage());
            return true;
        }
    }

    /**
     * Read a user's balance for the before/after numbers. Tries the
     * Hazelcast {@code users} cache first (legacy parity, sub-ms hot
     * path); falls back to the base-class DB read on cache miss /
     * unreachable cluster. Returns 0 if neither path resolves the
     * user — that matches the legacy
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
            logger.warn("GscCancelAggregator.balanceForUser MySQL read failed user="
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
    private static final int SC_INCORRECT_AGENT_KEY   = 1002;
    private static final int SC_INVALID_SIGNATURE     = 1004;
    private static final int SC_BET_NOT_EXIST         = 1006;

    /**
     * Validation/error outcome carrying the wire-shape via metadata.
     * Uses {@link SeamlessOutcome#validationError(String, String, Map)}
     * (NOT {@code posted(0, meta)}) so {@link #postAudit} marks the
     * audit row {@code FAILED} instead of {@code COMPLETED} — the wire
     * shape comes from {@code meta["shape"]} which
     * {@link #serializeResponse} reads, but the outcome status
     * truthfully reflects the validation failure.
     */
    private static SeamlessOutcome errorOutcome(int code, String message) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.CANCEL);
        meta.put("code", code);
        meta.put("message", message);
        meta.put("before_balance", 0.0);
        meta.put("balance", 0.0);
        return SeamlessOutcome.validationError(String.valueOf(code), message, meta);
    }

    /**
     * Empty {@code BalanceResponse} fall-back. Used in two cases
     * (mirroring legacy lines 102-104 and 128-131):
     * <ul>
     *   <li>Unknown currency code (currencyFactor == 0).
     *   <li>Any per-transaction failure aborting the batch.
     * </ul>
     * The wire shape is {@code {"code":0,"message":"","data":[]}}.
     *
     * <p>This is a SUCCESS-shaped outcome (POSTED) at the gateway level
     * because the legacy emits {@code SeamlessWalletCode.SUCCESS}=0 here —
     * even though semantically it's a failure indication. The audit row
     * is therefore marked COMPLETED, matching legacy behavior.
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
        if (out == null) return cancelJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
        switch (out.status) {
            case SIGNATURE_ERROR:
                return cancelJson(SC_INVALID_SIGNATURE, "Invalid sign", 0.0, 0.0);
            case SERVER_ERROR:
                return cancelJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
            case POSTED:
            case DUPLICATE:
            case VALIDATION_ERROR: {
                // VALIDATION_ERROR shares the metadata-driven shape with POSTED/DUPLICATE
                // because errorOutcome (validation failure: bad json, missing params, etc.)
                // wraps its wire fields in metadata. The status drives postAudit's
                // FAILED/COMPLETED mapping; the wire shape comes from metadata.
                Map<String, Object> meta = out.metadata;
                GscResponseShape shape = GscResponseShape.from(meta, GscResponseShape.CANCEL);
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
                return cancelJson(code, msg, before, after);
            }
            case USER_NOT_FOUND:
            case INSUFFICIENT_BALANCE:
            default:
                return cancelJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
        }
    }

    /**
     * Serialize the {@link game.third.hooks.gscSeamless.response.CancelResponse}-
     * shape JSON used by the legacy Cancel endpoint. Field order
     * ({@code code} / {@code message} / {@code before_balance} /
     * {@code balance}) matches the legacy Jackson-default declaration
     * order so a string-compare against captured production responses
     * is stable.
     */
    private static String cancelJson(int code, String message, double before, double balance) {
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
        return GscEventLogger.tryLogRequest("cancel", rawBody, http);
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
