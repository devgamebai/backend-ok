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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3b — GSC Transfer handler implemented on top of
 * {@link SeamlessWalletAggregator}. Mirrors the legacy
 * {@code game.third.hooks.gscSeamless.TransferProcess} at the wire-response
 * level, gated behind {@code GSC_AGGREGATOR_TRANSFER_ENABLED}.
 *
 * <h2>Why this handler is wallet-moving (unlike Phase 3a PushBet)</h2>
 * The legacy {@code TransferProcess.doExecuteInner} dispatches each
 * transaction in {@code transactions[]} through
 * {@code CommonProcess.TransactionAction(...)} which switches on a
 * 13-case action enum (BET, FREEBET, SETTLED, ROLLBACK, CANCEL,
 * ADJUSTMENT, JACKPOT, BONUS, TIP, PROMO, LEADERBOARD, BET_PRESERVE,
 * PRESERVE_REFUND). Every case calls one of two helpers:
 * {@code actionBet(...)} or {@code actionReward(...)}.
 *
 * <h2>Behavioral divergence from legacy: the {@code actionReward}-calls-
 * {@code bet} bug is NOT preserved</h2>
 * Verified in legacy {@code CommonProcess.java:123-141}: the
 * {@code actionReward} helper calls {@code userMoneyService.bet(...)},
 * not {@code reward(...)}. {@code UserMoneyServiceImpl.bet} unconditionally
 * forces the amount to be negative (lines 71-73:
 * {@code if (money > 0) money = -money;}) and updates the wallet by that
 * negative delta — i.e. it always DEBITS. As a result, every action
 * routed through {@code actionReward} (FREEBET, SETTLED, CANCEL, JACKPOT,
 * BONUS, PROMO, LEADERBOARD, PRESERVE_REFUND) actually debits the
 * player's wallet in legacy — a real platform bug. It is not currently
 * firing because verified <em>0 events in last 30 days</em> on
 * {@code TransferProcess}; the bug is dead code under current traffic.
 *
 * <p>Per the Phase 3b directive in
 * {@code docs/SEAMLESS_WALLET_AGGREGATOR_DESIGN.md} §3, the aggregator
 * MUST map each action to its correct intent (per GSC's API contract),
 * NOT replicate the bug. When the env-var flag flips on, eight actions
 * silently change direction (all currently bug-debiting → correctly
 * crediting). This is an intentional behavioral fix scoped to the
 * wallet side-effect; the response shape stays byte-equivalent so the
 * contract test passes.
 *
 * <h2>Action → direction map (the resolved table)</h2>
 * <pre>
 *   BET              → DEBIT  — new bet, money out of player. Legacy ✓.
 *   FREEBET          → CREDIT — house gives free play. Legacy DEBITS (bug).
 *   SETTLED          → CREDIT — player won, pay them. Legacy DEBITS (bug).
 *   ROLLBACK         → NO-OP  — audit-only signal (legacy behavior preserved).
 *   CANCEL           → CREDIT — refund a prior bet. Legacy DEBITS (bug).
 *   ADJUSTMENT       → NO-OP  — direction unspecified by legacy / unsafe to guess.
 *   JACKPOT          → CREDIT — player wins big. Legacy DEBITS (bug).
 *   BONUS            → CREDIT — house bonus. Legacy DEBITS (bug).
 *   TIP              → DEBIT  — player tips dealer. Legacy ✓.
 *   PROMO            → CREDIT — house promo. Legacy DEBITS (bug).
 *   LEADERBOARD      → CREDIT — tournament prize. Legacy DEBITS (bug).
 *   BET_PRESERVE     → DEBIT  — reserve money for pending bet. Legacy ✓.
 *   PRESERVE_REFUND  → CREDIT — release reserved money. Legacy DEBITS (bug).
 * </pre>
 *
 * <p>Two actions map to NO-OP — {@code ROLLBACK} (the legacy code
 * also does nothing here) and {@code ADJUSTMENT} (legacy passes it
 * through {@code actionBet} which debits, but the action is purely
 * direction-ambiguous in GSC's spec — could be either way; safer to
 * skip with a WARN log than to silently move money the wrong way; the
 * flag is OFF by default so this is a no-regression posture). NO-OP
 * actions still return {@code true} for the per-transaction success
 * boolean so the loop continues on subsequent transactions.
 *
 * <h2>External-ref namespacing (collision avoidance)</h2>
 * The {@code money_gateway_log} UNIQUE on {@code (tx_id, source, user_id)}
 * means a SETTLED for wager X and the original BET for wager X must hash
 * to different rows. We prefix the transaction id by lowercased action:
 * {@code transfer_bet_<id>}, {@code transfer_settled_<id>},
 * {@code transfer_cancel_<id>}, {@code transfer_jackpot_<id>}, …. Same
 * scheme as Phase 3a (PushBet) and the design-doc-recommended pattern
 * for AWC's settle / cancel chain.
 *
 * <h2>Multi-transaction failure semantics</h2>
 * The legacy code aborts the loop on the first failed transaction and
 * returns an empty {@code BalanceResponse} ({@code TransferProcess.java:120-122}).
 * The aggregator preserves that — we surface the failure as a
 * {@code shape="balance"} fall-back outcome that
 * {@link #serializeResponse} renders as {@code {"code":0,"message":"","data":[]}}.
 *
 * <h2>Sign verification</h2>
 * Single verb {@code "transfer"}: {@code md5(operator_code + request_time
 * + "transfer" + secret)}. Mirrors {@code TransferProcess.java:97}.
 *
 * <h2>Audit endpoint string</h2>
 * Matches the legacy: {@code "transfer"} ({@code TransferProcess.java:51}).
 */
public class GscTransferAggregator extends SeamlessWalletAggregator<GscRequest, String> {

    private final GscConfigProvider config;

    public GscTransferAggregator(GscConfigProvider config) {
        if (config == null) throw new IllegalArgumentException("config is null");
        this.config = config;
    }

    /** Tighter audit-log tag than the default class-simple-name. */
    @Override
    protected String aggregatorName() { return "GSC"; }

    /** Phase 5p3 — distinct timing-metrics bucket per handler. */
    @Override
    protected String metricsName() { return "GscTransfer"; }

    // ─────────────────────────────────────────────────────────────────
    // Direction resolution
    // ─────────────────────────────────────────────────────────────────

    enum Direction { DEBIT, CREDIT, NOOP }

    /**
     * Resolve the wallet-movement direction for an action. Public for
     * test inspection; the dispatch flow below is the only caller in
     * production. NOOP actions are logged at WARN inside dispatch (not
     * here) to avoid double-logging on a tight loop.
     */
    static Direction directionFor(String action) {
        if (action == null) return Direction.NOOP;
        switch (action) {
            case "BET":             return Direction.DEBIT;
            case "TIP":             return Direction.DEBIT;
            case "BET_PRESERVE":    return Direction.DEBIT;

            case "FREEBET":         return Direction.CREDIT;
            case "SETTLED":         return Direction.CREDIT;
            case "CANCEL":          return Direction.CREDIT;
            case "JACKPOT":         return Direction.CREDIT;
            case "BONUS":           return Direction.CREDIT;
            case "PROMO":           return Direction.CREDIT;
            case "LEADERBOARD":     return Direction.CREDIT;
            case "PRESERVE_REFUND": return Direction.CREDIT;

            // Audit-only; legacy code also returns true without moving money.
            case "ROLLBACK":        return Direction.NOOP;
            // Direction-ambiguous; safer to skip than guess. See class doc.
            case "ADJUSTMENT":      return Direction.NOOP;

            default:                return Direction.NOOP;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // parseRequest
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse the inbound JSON into a {@link GscRequest}. The legacy
     * {@code TransferRequest.fromJson} returns null on a malformed body
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

        // Legacy TransferRequest field name is plain "transactions" (no fallback).
        JSONArray txArr = root.optJSONArray("transactions");
        List<GscRequest.TransactionItem> transactions = new ArrayList<>();
        if (txArr != null) {
            for (int i = 0; i < txArr.length(); i++) {
                JSONObject t = txArr.optJSONObject(i);
                if (t == null) continue;
                String id            = optStr(t, "id");
                String action        = normalizeAction(optStr(t, "action"));
                // Transfer's legacy code always uses the top-level
                // memberAccount for every txn (the per-transaction
                // member_account is not consulted in CommonProcess).
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

    /**
     * Normalize legacy lowercase verbs to the upper-case enum values
     * we switch on. Mirrors {@code Transaction.action()} mapping. Pass-
     * through if the input is already upper-case (defensive — some
     * provider variants emit upper-case directly).
     */
    private static String normalizeAction(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        // If already canonical, return as-is.
        switch (s) {
            case "BET":             case "FREEBET":         case "SETTLED":
            case "ROLLBACK":        case "CANCEL":          case "ADJUSTMENT":
            case "JACKPOT":         case "BONUS":           case "TIP":
            case "PROMO":           case "LEADERBOARD":     case "BET_PRESERVE":
            case "PRESERVE_REFUND":
                return s;
        }
        // Otherwise lowercase-match.
        switch (s.toLowerCase()) {
            case "bet":             return "BET";
            case "freebet":         return "FREEBET";
            case "settled":         return "SETTLED";
            case "rollback":        return "ROLLBACK";
            case "cancel":          return "CANCEL";
            case "adjustment":      return "ADJUSTMENT";
            case "jackpot":         return "JACKPOT";
            case "bonus":           return "BONUS";
            case "tip":             return "TIP";
            case "promo":           return "PROMO";
            case "leaderboard":     return "LEADERBOARD";
            case "bet_preserve":    return "BET_PRESERVE";
            case "preserve_refund": return "PRESERVE_REFUND";
            default:                return s;  // unknown: fall through; dispatch will NO-OP it.
        }
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

        String expected = md5Hex(req.getOperatorCode() + req.getRequestTime() + "transfer" + secret);
        if (!req.getSign().equals(expected)) {
            return VerifyResult.fail("signature mismatch");
        }
        return VerifyResult.ok();
    }

    // ─────────────────────────────────────────────────────────────────
    // mapActionToSource / currency conversion
    // ─────────────────────────────────────────────────────────────────

    /**
     * Map the resolved direction to one of the GSC source constants.
     * The action name itself (BET / SETTLED / …) is not what the source
     * keys off — we use direction. Multiple debit actions (BET, TIP,
     * BET_PRESERVE) all share {@code SOURCE_GSC_DEBIT}; the tx-id
     * namespace prefix is what disambiguates them in
     * {@code money_gateway_log}.
     *
     * <p>Returns the empty string for NO-OP actions; the dispatch
     * flow short-circuits before this method is consulted in that
     * case, but returning a sentinel rather than null defends against
     * accidental future calls.
     */
    @Override
    protected String mapActionToSource(String aggregatorAction) {
        Direction d = directionFor(aggregatorAction);
        switch (d) {
            case DEBIT:  return MoneyGateway.SOURCE_GSC_DEBIT;
            case CREDIT: return MoneyGateway.SOURCE_GSC_CREDIT;
            case NOOP:
            default:     return "";
        }
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
    // dispatch — the per-action loop
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected SeamlessOutcome dispatch(GscRequest req) {
        // Invalid-JSON / null request → "Invalid JSON format"
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
        // Required-parameter check (mirrors TransferProcess.java:89)
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

        // Currency lookup. Unknown → empty BalanceResponse fall-back.
        int currencyFactor = config.getCurrencyExchangeRate(req.getCurrency());
        if (currencyFactor == 0) {
            return balanceFallbackOutcome();
        }

        String memberAccount = req.getMemberAccount();

        // Read before-balance for the response (in INTERNAL units, like
        // legacy line 116). The legacy uses userMoneyService.getBalance
        // (Hazelcast); we use a DB read via the base class. Both should
        // return the same value in steady state.
        long beforeBalanceInternal = balanceForUser(memberAccount);

        // Dispatch each transaction in order. Per legacy: any failure
        // aborts the loop and returns the empty BalanceResponse shape.
        for (GscRequest.TransactionItem t : transactions) {
            SeamlessOutcome perTxn = dispatchOne(req, t);
            if (!isSuccessful(perTxn)) {
                logger.warn("GscTransferAggregator: txn failed, aborting batch."
                        + " action=" + t.getAction()
                        + " id=" + t.getId()
                        + " status=" + (perTxn == null ? "null" : perTxn.status));
                return balanceFallbackOutcome();
            }
        }

        // After the loop, fetch the post-loop balance for the response.
        long afterBalanceInternal = balanceForUser(memberAccount);
        double beforeBalanceExternal = (double) beforeBalanceInternal;
        double afterBalanceExternal = currencyToExternal(afterBalanceInternal, req.getCurrency());

        // Transfer's legacy returns before_balance in INTERNAL units
        // (it never multiplies by exchangeRateOut — see line 130) but
        // after_balance multiplied by exchangeRateOut (line 125). We
        // preserve that asymmetry verbatim so the wire shape matches
        // operationally-captured Transfer responses.
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shape", GscResponseShape.TRANSFER);
        meta.put("code", SC_SUCCESS);
        meta.put("message", "Transfer processed successfully");
        meta.put("before_balance", beforeBalanceExternal);
        meta.put("balance", afterBalanceExternal);
        return SeamlessOutcome.posted(afterBalanceInternal, meta);
    }

    /**
     * Dispatch a single transaction. Returns a successful outcome (POSTED
     * or DUPLICATE) on success; anything else is a failure that aborts
     * the parent loop. NO-OP actions return POSTED with metadata
     * {@code skipped=true}.
     */
    private SeamlessOutcome dispatchOne(GscRequest req, GscRequest.TransactionItem t) {
        if (t == null) {
            return SeamlessOutcome.serverError(null, "null transaction");
        }
        String action = t.getAction();
        Direction dir = directionFor(action);
        String id = t.getId();
        if (id == null || id.isEmpty()) {
            logger.warn("GscTransferAggregator: transaction missing id, action=" + action);
            return SeamlessOutcome.serverError(null, "missing transaction id");
        }

        if (dir == Direction.NOOP) {
            // ROLLBACK / ADJUSTMENT / unknown — log + skip. Legacy returns
            // success here for ROLLBACK/ADJUSTMENT and false for an
            // unknown action; we treat unknown as success-skip too on
            // the principle that a NO-OP is safer than a failed batch
            // when the flag is OFF by default and an unknown verb is
            // by definition something we don't understand.
            logger.warn("GscTransferAggregator: NO-OP action='" + action + "' id=" + id
                    + " — skipped (no wallet movement)");
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("skipped", Boolean.TRUE);
            meta.put("action", action == null ? "" : action);
            return SeamlessOutcome.posted(0L, meta);
        }

        // Resolve amount (in internal sub-units).
        String amountRaw = t.getBetAmount() != null ? t.getBetAmount() : t.getAmount();
        double providerAmount;
        try {
            providerAmount = (amountRaw != null && !amountRaw.isEmpty())
                    ? Double.parseDouble(amountRaw) : 0.0;
        } catch (NumberFormatException e) {
            logger.warn("GscTransferAggregator: bad amount '" + amountRaw + "' for txn " + id);
            return SeamlessOutcome.serverError(null, "bad amount");
        }

        long amountSubunit = currencyToInternal(providerAmount, req.getCurrency());
        if (amountSubunit <= 0L) {
            // Zero-amount txn: nothing to move. Treat as a NO-OP-success
            // so the batch continues (legacy actionBet would call
            // userMoneyService.bet with amount=0 which short-circuits
            // with success in updateMoney).
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("skipped", Boolean.TRUE);
            meta.put("reason", "zero-amount");
            return SeamlessOutcome.posted(0L, meta);
        }

        // External-ref namespacing: prefix by lowercased action so a
        // BET and a SETTLED for the same underlying wager hash to
        // different rows in money_gateway_log.uk_tx_source.
        String externalRef = "transfer_" + action.toLowerCase() + "_" + id;
        String description = aggregatorName() + " " + action + " " + externalRef;

        SeamlessTxn txn = new SeamlessTxn(
                req.getMemberAccount(),
                externalRef,
                action,
                amountSubunit,
                req.getCurrency(),
                /*metadata*/null);

        // The base-class doDebit/doCredit handle dedup via uk_tx_source
        // and map the gateway's loose-typed errors back to typed
        // SeamlessOutcome statuses. We just call the right one based on
        // direction.
        if (dir == Direction.DEBIT) {
            return doDebit(txn);
        } else { // CREDIT
            return doCredit(txn);
        }
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
     * unreachable cluster. Returns 0 if neither path resolves the
     * user — that matches the legacy
     * {@code UserMoneyServiceImpl.getMoneyFromUsersMap} behavior.
     */
    private long balanceForUser(String memberAccount) {
        // SUN-1xxx (2026-05-11): MySQL-only, no cache. Per
        // docs/architecture/LEDGER_HARDENING_ROADMAP.md — balance reads
        // returned to external parties or used to gate money moves must
        // read the canonical store, not Hazelcast (drifts under signup-
        // seed staleness, lock-timeout eviction, HZ disconnect).
        if (memberAccount == null || memberAccount.isEmpty()) return 0L;
        try {
            SeamlessOutcome r = doReadBalance(memberAccount);
            if (r != null && r.status == SeamlessOutcome.Status.POSTED) {
                return r.newBalance;
            }
            return 0L;
        } catch (Throwable t) {
            logger.warn("GscTransferAggregator.balanceForUser MySQL read failed user="
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
        meta.put("shape", GscResponseShape.TRANSFER);
        meta.put("code", code);
        meta.put("message", message);
        meta.put("before_balance", 0.0);
        meta.put("balance", 0.0);
        // Use validationError so postAudit marks the audit row FAILED instead of
        // COMPLETED — the wire-shape comes from `meta["shape"]` which serializeResponse
        // reads, but the outcome status truthfully reflects the validation failure.
        return SeamlessOutcome.validationError(String.valueOf(code), message, meta);
    }

    /**
     * Empty {@code BalanceResponse} fall-back. Used in two cases (mirroring
     * legacy lines 105-108 and 119-122):
     * <ul>
     *   <li>Unknown currency code (currencyFactor==0).
     *   <li>Any per-transaction failure aborting the batch.
     * </ul>
     * The wire shape is {@code {"code":0,"message":"","data":[]}}.
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
        if (out == null) return transferJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
        switch (out.status) {
            case SIGNATURE_ERROR:
                return transferJson(SC_INVALID_SIGNATURE, "Invalid sign", 0.0, 0.0);
            case SERVER_ERROR:
                return transferJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
            case POSTED:
            case DUPLICATE:
            case VALIDATION_ERROR: {
                // VALIDATION_ERROR shares the metadata-driven shape with POSTED/DUPLICATE
                // because errorOutcome (validation failure: bad json, missing params, etc.)
                // wraps its wire fields in metadata. The status drives postAudit's
                // FAILED/COMPLETED mapping; the wire shape comes from metadata.
                Map<String, Object> meta = out.metadata;
                GscResponseShape shape = GscResponseShape.from(meta, GscResponseShape.TRANSFER);
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
                return transferJson(code, msg, before, after);
            }
            case USER_NOT_FOUND:
            case INSUFFICIENT_BALANCE:
            default:
                return transferJson(SC_INTERNAL_SERVER_ERROR, "Server Error", 0.0, 0.0);
        }
    }

    /**
     * Serialize the {@link game.third.hooks.gscSeamless.response.BaseResponse}-
     * shape JSON used by the legacy Transfer endpoint. Field order
     * (code / message / before_balance / balance) matches the legacy
     * Jackson-default declaration order so a string-compare against
     * captured production responses is stable.
     */
    private static String transferJson(int code, String message, double before, double balance) {
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
        return GscEventLogger.tryLogRequest("transfer", rawBody, http);
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
