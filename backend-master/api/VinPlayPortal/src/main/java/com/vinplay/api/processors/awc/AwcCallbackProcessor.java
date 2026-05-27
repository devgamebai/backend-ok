package com.vinplay.api.processors.awc;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.config.AwcConfig;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * c=3097 — AWC seamless wallet callback handler.
 *
 * AWC POSTs JSON with {message: {action, txns/userId}, key} for every wallet event.
 * We authenticate via key === AWC_CERT, then dispatch by action.
 *
 * Actions requiring balance response: getBalance, bet, betNSettle, cancelBet,
 * cancelBetNSettle, adjustBet, tip, cancelTip.
 *
 * Actions NOT requiring balance: settle, refund, unsettle, voidBet, voidSettle,
 * unvoidBet, unvoidSettle, resettle, freeSpin, give.
 *
 * IDEMPOTENCY: duplicate platformTxId → return 0000, do NOT change balance.
 */
public class AwcCallbackProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = LoggerFactory.getLogger("awc");

    /**
     * SXB-22: AWC bulk-settle batches up to ~50 txns can blow the 20s timeout.
     * Defer non-critical per-txn work (Mongo audit log + RMQ commission publish)
     * to this pool so the response returns as soon as the wallet credit + audit
     * INSERT are durable. The work still happens — just doesn't gate the response.
     */
    private static final ExecutorService BATCH_ASYNC =
            Executors.newFixedThreadPool(8, r -> {
                Thread t = new Thread(r, "awc-batch-async");
                t.setDaemon(true);
                return t;
            });
    private static final DateTimeFormatter ISO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        try {
            if (!AwcConfig.isEnabled()) {
                return errResp("9999", "AWC disabled");
            }
            // SUN-1236 Phase 4: single entry through SeamlessWalletAggregator.
            // The aggregator owns the full request lifecycle (readBody →
            // preAudit → parseRequest → verifySignature → validateBusinessRules
            // → dispatch → serializeResponse → postAudit). dispatchByAction
            // below remains the per-action implementation that aggregator's
            // dispatch delegates to. Phases 5+ progressively rewrite each
            // handler onto base primitives (doDebit/doCredit/doReadBalance).
            return new AwcSeamlessAggregator().handle(param.get());
        } catch (Exception e) {
            logger.error("AWC callback error", e);
            return errResp("9999", "Internal: " + e.getMessage());
        }
    }

    /**
     * SUN-1236 Phase 2: extracted action switch. Package-private so the
     * aggregator delegate ({@link AwcSeamlessAggregator}) can dispatch
     * through the same code path. Each handler here is the legacy
     * implementation — Phase 3 migrates them onto base primitives one by
     * one. Callable from a fresh {@code AwcCallbackProcessor()} instance
     * since handlers don't hold instance state.
     */
    String dispatchByAction(String action, JSONObject message, String rawBody) {
        switch (action) {
            case "getBalance":
                return handleGetBalance(message);
            case "bet":
                return handleBet(message, rawBody);
            case "betNSettle":
                return handleBetNSettle(message, rawBody);
            case "settle":
                return handleSettle(message, rawBody);
            case "cancelBet":
                return handleCancelBet(message, rawBody);
            case "cancelBetNSettle":
                return handleCancelBetNSettle(message, rawBody);
            case "refund":
                return handleRefund(message, rawBody);
            case "voidBet":
                return handleVoidBet(message, rawBody);
            case "voidSettle":
                return handleVoidSettle(message, rawBody);
            case "unvoidBet":
                return handleUnvoidBet(message, rawBody);
            case "unvoidSettle":
                return handleUnvoidSettle(message, rawBody);
            case "unsettle":
                return handleUnsettle(message, rawBody);
            case "resettle":
                return handleResettle(message, rawBody);
            case "freeSpin":
                return handleFreeSpin(message, rawBody);
            case "adjustBet":
                return handleAdjustBet(message, rawBody);
            case "tip":
                return handleTip(message, rawBody);
            case "cancelTip":
                return handleCancelTip(message, rawBody);
            case "give":
                return handleGive(message, rawBody);
            default:
                logger.warn("AWC callback unknown action: {}", action);
                return errResp("9999", "Unknown action: " + action);
        }
    }

    // ===== BALANCE-RETURNING HANDLERS =====

    private String handleGetBalance(JSONObject message) {
        String awcUserId = message.optString("userId", "");
        String username = awcUserIdToUsername(awcUserId);
        if (username == null) return errResp("1000", "Invalid userId");
        // 2026-05-16: REVERTED the Hazelcast-first fast path. With the
        // money_account ledger, MySQL is the only authoritative balance
        // source; Hazelcast is an eventually-consistent mirror written by
        // MoneyGateway AFTER the MySQL commit. A getBalance served from a
        // lagging cache (post-deposit, post-withdraw, or after a missed
        // HZ.put) makes the vendor iframe display the wrong amount, and
        // worse: the player's subsequent bet hits getPlayerBalance (MySQL
        // primary) which sees the correct balance, so the BET succeeds
        // while the iframe insufficient-funds banner says it shouldn't —
        // a confidence-destroying discrepancy.
        //
        // GSC's balance path (GscBalanceAggregator) is MySQL-primary; AWC
        // now matches. getPlayerBalance still has the SUN-1340 Hazelcast
        // safety-net for when MySQL is the one that fails — that path
        // preserves the user-visible balance during pool blips without
        // making HZ the default source.
        long balance = getPlayerBalance(username);
        long userId = resolveUserId(username);
        return balanceRespDecimal(userId, balance);
    }

    private String handleBet(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-22/23: AWC sends multi-user batches. Resolve username per-txn,
        // accumulate debits per-user, defer Mongo log to BATCH_ASYNC.
        Map<String, Long> debitByUser = new HashMap<>();
        Map<String, Long> userIdByName = new HashMap<>();
        String firstUsername = null;
        long firstUserId = -1;

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String platformTxId = txn.optString("platformTxId", "");
            String awcUserId = txn.optString("userId", "");
            String username = awcUserIdToUsername(awcUserId);
            if (username == null) continue;
            if (firstUsername == null) {
                firstUsername = username;
                firstUserId = resolveUserId(username);
            }

            if (isDuplicateTxn(platformTxId)
                    || isDuplicateTxn("cancelBet_" + platformTxId)
                    || isDuplicateTxn("cancelBetNSettle_" + platformTxId)) {
                continue;
            }

            // Catalog gate: refuse bet when admin has marked the game (or
            // its parent platform stub) as inactive in vinplay.games via
            // c=9982. Same idempotency: BET only — SETTLE/CANCEL pass.
            // FE shows the game with `active=false` so the player sees
            // why they can't place; this is the server-side enforcement.
            String platformCode = txn.optString("platform", "");
            String gameCodeAwc  = txn.optString("gameCode", "");
            String roundIdAwc   = txn.optString("roundId", "");
            String tableTagAwc  = com.vinplay.dal.service.AwcGameNameResolver.parseTableSuffix(platformCode, roundIdAwc);
            if (!isAwcGameActive(platformCode, gameCodeAwc, tableTagAwc)) {
                logger.warn("AWC bet refused — game inactive in catalog. user={} platform={} gameCode={} tableTag={}",
                        username, platformCode, gameCodeAwc, tableTagAwc);
                return errResp("1098", "Game under maintenance");
            }

            // Per-user block list (admin c=9985). Refuse the entire batch
            // when any txn would be blocked — partial accept would leave
            // AWC's seamless ledger out of sync. SETTLE / CANCEL /
            // BONUS callbacks ride a different handler so already-open
            // wagers can still close even after a block is added.
            Integer catAwc      = lookupAwcCategoryId(platformCode, gameCodeAwc, tableTagAwc);
            long resolvedUserId = userIdByName.computeIfAbsent(username, this::resolveUserId);
            if (com.vinplay.dal.service.UserGameBlock.isBlocked(
                    resolvedUserId, username, "AWC", platformCode, gameCodeAwc, tableTagAwc, catAwc)) {
                logger.warn("AWC bet refused by user-block. user={} platform={} gameCode={} tableTag={} category={}",
                        username, platformCode, gameCodeAwc, tableTagAwc, catAwc);
                return errResp("1099", "Player blocked from this game");
            }

            long betMilliVnd = parseMilliVnd(txn.optString("betAmount", "0"));
            long betAmtVnd = Math.floorDiv(betMilliVnd, 1000L);
            saveTxnWithMilli(txn, "bet", betAmtVnd, 0, 0, betMilliVnd, 0, 0, rawBody);
            debitByUser.merge(username, betMilliVnd, Long::sum);

            // Defer Mongo log to async pool — not response-critical.
            final JSONObject txnRef = txn;
            BATCH_ASYNC.submit(() -> {
                try { writeMongoLog(txnRef, "bet", betAmtVnd, 0, 0); }
                catch (Throwable t) { logger.warn("handleBet async mongo failed: {}", t.getMessage()); }
            });
        }

        // Apply aggregated debits per user.
        long lastBalance = -1;
        for (Map.Entry<String, Long> e : debitByUser.entrySet()) {
            String uName = e.getKey();
            long totalDebitMilli = e.getValue();
            if (totalDebitMilli <= 0) continue;
            long uId = userIdByName.computeIfAbsent(uName, this::resolveUserId);
            // SUN-1340 perf: skip the getPlayerBalance pre-check — it was a
            // redundant SELECT before deductBalance. MoneyGateway.debitUser
            // already returns "Insufficient" atomically when the balance
            // is short, and the `newBal < 0` branch below reverts the
            // residue and returns 1018. Net saving: one MySQL SELECT per
            // handleBet per user under load.
            long vinDebit;
            try {
                vinDebit = -AwcResidueTracker.applyMilliDelta(uId, -totalDebitMilli);
            } catch (Exception ex) {
                logger.error("handleBet applyMilliDelta failed user={} total={}", uName, totalDebitMilli, ex);
                return errResp("9999", "Internal: residue tracker error");
            }
            long newBal = vinDebit > 0 ? deductBalance(uName, vinDebit) : getPlayerBalance(uName);
            if (newBal < 0) {
                try { AwcResidueTracker.applyMilliDelta(uId, totalDebitMilli); } catch (Exception ignored) {}
                if (vinDebit > 0) { try { addBalance(uName, vinDebit); } catch (Exception ignored) {} }
                return errResp("1018", "Insufficient balance");
            }
            lastBalance = newBal;
        }
        if (firstUsername == null) return errResp("1000", "Invalid userId");
        if (lastBalance < 0) lastBalance = getPlayerBalance(firstUsername);
        return balanceRespDecimal(firstUserId, lastBalance);
    }

    private String handleBetNSettle(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-16: aggregate net (win - bet) across batch; single wallet op.
        String username = null;
        long userId = -1;
        long totalNetMilli = 0;

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String platformTxId = txn.optString("platformTxId", "");
            String awcUserId = txn.optString("userId", "");
            if (username == null) {
                username = awcUserIdToUsername(awcUserId);
                if (username == null) return errResp("1000", "Invalid userId");
                userId = resolveUserId(username);
            }

            if (isDuplicateTxn("cancelBet_" + platformTxId)
                    || isDuplicateTxn("cancelBetNSettle_" + platformTxId)) {
                continue;
            }

            long betMilliVnd = parseMilliVnd(txn.optString("betAmount", "0"));
            // JLS-5-4: jackpotWinAmount is paid on top of winAmount (JILI-SLOT cert).
            // Fold both into winMilliVnd so wallet credit + audit + downstream
            // voidSettle (which reads back win_milli) all stay consistent.
            long winMilliVnd = parseMilliVnd(txn.optString("winAmount", "0"))
                    + parseMilliVnd(txn.optString("jackpotWinAmount", "0"));
            long turnoverMilliVnd = parseMilliVnd(txn.optString("turnover", "0"));

            long betAmtVnd = Math.floorDiv(betMilliVnd, 1000L);
            long winAmtVnd = Math.floorDiv(winMilliVnd, 1000L);
            long turnoverVnd = Math.floorDiv(turnoverMilliVnd, 1000L);

            // JLS-4 (Network) BetNSettle 5x: cert sends 5 concurrent
            // requests with the SAME platformTxId. Old code did
            // SELECT-then-INSERT-then-credit; the SELECT (isDuplicateTxn)
            // had a TOCTOU window where two concurrent requests both
            // read "no row exists", both proceeded, INSERT IGNORE made
            // one row but the other still credited from totalNetMilli.
            // Switch to atomic audit-first: gate the credit on
            // saveTxnWithMilli's isNew return — only the request whose
            // INSERT actually inserted the row is allowed to credit.
            // Aligns with the pattern already used in handleSettle and
            // handleResettle.
            boolean isNew = saveTxnWithMilli(txn, "betNSettle", betAmtVnd, winAmtVnd, turnoverVnd,
                    betMilliVnd, winMilliVnd, 0, rawBody);
            if (!isNew) continue;
            totalNetMilli += (winMilliVnd - betMilliVnd);
            writeMongoLog(txn, "betNSettle", betAmtVnd, winAmtVnd, 0);
            triggerCommission(username, txn, betAmtVnd, winAmtVnd, turnoverVnd);
        }

        long newBalance;
        if (totalNetMilli == 0) {
            newBalance = getPlayerBalance(username);
        } else if (totalNetMilli > 0) {
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(userId, totalNetMilli);
            } catch (Exception e) {
                logger.error("handleBetNSettle applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            newBalance = vinCredit > 0 ? addBalance(username, vinCredit) : getPlayerBalance(username);
        } else {
            long vinDebit;
            try {
                vinDebit = -AwcResidueTracker.applyMilliDelta(userId, totalNetMilli);
            } catch (Exception e) {
                logger.error("handleBetNSettle applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinDebit > 0) {
                newBalance = deductBalance(username, vinDebit);
                if (newBalance < 0) {
                    try { AwcResidueTracker.applyMilliDelta(userId, -totalNetMilli); } catch (Exception ignored) {}
                    try { addBalance(username, vinDebit); } catch (Exception ignored) {}
                    return errResp("1018", "Insufficient balance");
                }
            } else {
                newBalance = getPlayerBalance(username);
            }
        }
        return balanceRespDecimal(userId, newBalance);
    }

    private String handleCancelBet(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-16: AWC sends up to 30 txns per batch. Aggregate wallet mutation
        // into ONE applyMilliDelta + ONE addBalance after the loop — avoids 30×
        // wallet round-trips that pushed response time past the 6000ms timeout.
        String username = null;
        long userId = -1;
        long totalRefundMilli = 0;
        long balanceAfter = -1;
        java.util.List<JSONObject> auditedTxns = new java.util.ArrayList<>();
        java.util.List<Long> auditedBetMilli = new java.util.ArrayList<>();

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String platformTxId = txn.optString("platformTxId", "");
            String awcUserId = txn.optString("userId", "");
            if (username == null) {
                username = awcUserIdToUsername(awcUserId);
                if (username == null) return errResp("1000", "Invalid userId");
                userId = resolveUserId(username);
            }

            if (isDuplicateTxn("cancelBet_" + platformTxId)) continue;

            long betMilliVnd = 0;
            boolean orphanCancel = false;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT bet_milli FROM awc_transactions " +
                         "WHERE platform_tx_id = ? AND action = 'bet' LIMIT 1")) {
                ps.setString(1, platformTxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) betMilliVnd = rs.getLong("bet_milli");
                    else orphanCancel = true;
                }
            } catch (Exception e) {
                logger.error("handleCancelBet: lookup failed platformTxId={}", platformTxId, e);
                return errResp("9999", "Internal: bet lookup failed");
            }

            // Audit row first (placeholder balance — corrected post-batch).
            // INSERT IGNORE return value already gates retries; we don't read balance_after.
            saveTxn(txn, "cancelBet", Math.floorDiv(betMilliVnd, 1000L), 0, 0, 0, rawBody);
            if (!orphanCancel) {
                totalRefundMilli += betMilliVnd;
                auditedTxns.add(txn);
                auditedBetMilli.add(betMilliVnd);
            }
            reverseAwcCommission(platformTxId, "cancel-bet");
        }

        // Apply aggregated refund
        if (totalRefundMilli > 0) {
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(userId, totalRefundMilli);
            } catch (Exception e) {
                logger.error("handleCancelBet applyMilliDelta failed user={} total={}", username, totalRefundMilli, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            balanceAfter = vinCredit > 0 ? addBalance(username, vinCredit) : getPlayerBalance(username);
        } else {
            balanceAfter = getPlayerBalance(username);
        }
        return balanceRespDecimal(userId, balanceAfter);
    }

    private String handleCancelBetNSettle(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-16: aggregate net (bet - win); single wallet op.
        String username = null;
        long userId = -1;
        long totalNetMilli = 0;

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String awcUserId = txn.optString("userId", "");
            String platformTxId = txn.optString("platformTxId", "");
            if (username == null) {
                username = awcUserIdToUsername(awcUserId);
                if (username == null) return errResp("1000", "Invalid userId");
                userId = resolveUserId(username);
            }

            if (isDuplicateTxn("cancelBetNSettle_" + platformTxId)) continue;

            long betMilliVnd = 0;
            long winMilliVnd = 0;
            boolean orphanCancel = false;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT bet_milli, win_milli FROM awc_transactions " +
                         "WHERE platform_tx_id IN (?, ?) AND action IN ('betNSettle','bet','settle') LIMIT 1")) {
                ps.setString(1, platformTxId);
                ps.setString(2, "settle_" + platformTxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        betMilliVnd = rs.getLong("bet_milli");
                        winMilliVnd = rs.getLong("win_milli");
                    } else {
                        orphanCancel = true;
                    }
                }
            } catch (Exception e) {
                logger.error("handleCancelBetNSettle: lookup failed platformTxId={}", platformTxId, e);
                return errResp("9999", "Internal: bet lookup failed");
            }

            saveTxn(txn, "cancelBetNSettle", Math.floorDiv(betMilliVnd, 1000L), Math.floorDiv(winMilliVnd, 1000L), 0, 0, rawBody);
            if (!orphanCancel) totalNetMilli += (betMilliVnd - winMilliVnd);
            reverseAwcCommission(platformTxId, "cancel-bet-n-settle");
        }

        long newBalance;
        if (totalNetMilli == 0) {
            newBalance = getPlayerBalance(username);
        } else if (totalNetMilli > 0) {
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(userId, totalNetMilli);
            } catch (Exception e) {
                logger.error("handleCancelBetNSettle applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            newBalance = vinCredit > 0 ? addBalance(username, vinCredit) : getPlayerBalance(username);
        } else {
            long vinDebit;
            try {
                vinDebit = -AwcResidueTracker.applyMilliDelta(userId, totalNetMilli);
            } catch (Exception e) {
                logger.error("handleCancelBetNSettle applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinDebit > 0) {
                newBalance = deductBalance(username, vinDebit);
                if (newBalance < 0) {
                    try { AwcResidueTracker.applyMilliDelta(userId, -totalNetMilli); } catch (Exception ignored) {}
                    try { addBalance(username, vinDebit); } catch (Exception ignored) {}
                    return errResp("1018", "Insufficient balance");
                }
            } else {
                newBalance = getPlayerBalance(username);
            }
        }
        return balanceRespDecimal(userId, newBalance);
    }

    private String handleAdjustBet(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        JSONObject txn = txns.getJSONObject(0);
        String awcUserId = txn.optString("userId", "");
        String username = awcUserIdToUsername(awcUserId);
        if (username == null) return errResp("1000", "Invalid userId");

        String platformTxId = txn.optString("platformTxId", "");
        if (isDuplicateTxn("adjustBet_" + platformTxId)) {
            long bal = getPlayerBalance(username);
            return balanceRespDecimal(resolveUserId(username), bal);
        }

        long adjustMilliVnd = parseMilliVnd(txn.optString("adjustAmount", "0"));
        long userId = resolveUserId(username);
        long vinDelta;
        try {
            vinDelta = AwcResidueTracker.applyMilliDelta(userId, adjustMilliVnd);
        } catch (Exception e) {
            logger.error("handleAdjustBet applyMilliDelta failed user={}", username, e);
            return errResp("9999", "Internal: residue tracker error");
        }
        long newBalance;
        if (vinDelta >= 0) {
            newBalance = vinDelta > 0 ? addBalance(username, vinDelta) : getPlayerBalance(username);
        } else {
            long vinDebit = -vinDelta;
            newBalance = deductBalance(username, vinDebit);
            if (newBalance < 0) {
                try { AwcResidueTracker.applyMilliDelta(userId, -adjustMilliVnd); } catch (Exception ignored) {}
                if (vinDebit > 0) {
                    try { addBalance(username, vinDebit); } catch (Exception ignored) {}
                }
                return errResp("1018", "Insufficient balance");
            }
        }

        saveTxnCustom(txn, "adjustBet", 0, 0, 0, Math.floorDiv(adjustMilliVnd, 1000L), 0, newBalance, rawBody);

        return balanceRespDecimal(userId, newBalance);
    }

    private String handleTip(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-17: AWC sends field "tip" (not "tipAmount"). Batch supported.
        String username = null;
        long userId = -1;
        long totalTipMilli = 0;

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String awcUserId = txn.optString("userId", "");
            String platformTxId = txn.optString("platformTxId", "");
            if (username == null) {
                username = awcUserIdToUsername(awcUserId);
                if (username == null) return errResp("1000", "Invalid userId");
                userId = resolveUserId(username);
            }

            // SXB-21: tip after cancelTip on same txId is no-op (cancelTip claims).
            if (isDuplicateTxn("tip_" + platformTxId)
                    || isDuplicateTxn("cancelTip_" + platformTxId)) continue;

            // Field priority: tip → tipAmount → betAmount (legacy fallbacks)
            long tipMilliVnd = parseMilliVnd(
                    txn.optString("tip",
                    txn.optString("tipAmount",
                    txn.optString("betAmount", "0"))));
            saveTxnCustom(txn, "tip", 0, 0, 0, 0, Math.floorDiv(tipMilliVnd, 1000L), 0, rawBody);
            totalTipMilli += tipMilliVnd;
        }

        long newBalance;
        if (totalTipMilli > 0) {
            long vinDebit;
            try {
                vinDebit = -AwcResidueTracker.applyMilliDelta(userId, -totalTipMilli);
            } catch (Exception e) {
                logger.error("handleTip applyMilliDelta failed user={} total={}", username, totalTipMilli, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            newBalance = vinDebit > 0 ? deductBalance(username, vinDebit) : getPlayerBalance(username);
            if (newBalance < 0) {
                try { AwcResidueTracker.applyMilliDelta(userId, totalTipMilli); } catch (Exception ignored) {}
                if (vinDebit > 0) { try { addBalance(username, vinDebit); } catch (Exception ignored) {} }
                return errResp("1018", "Insufficient balance");
            }
        } else {
            newBalance = getPlayerBalance(username);
        }
        return balanceRespDecimal(userId, newBalance);
    }

    private String handleCancelTip(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-17/18: AWC sends "tip" field; cancelTip omits amount → look up audit.
        String username = null;
        long userId = -1;
        long totalRefundMilli = 0;

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String awcUserId = txn.optString("userId", "");
            String platformTxId = txn.optString("platformTxId", "");
            if (username == null) {
                username = awcUserIdToUsername(awcUserId);
                if (username == null) return errResp("1000", "Invalid userId");
                userId = resolveUserId(username);
            }

            if (isDuplicateTxn("cancelTip_" + platformTxId)) continue;

            long tipMilliVnd = parseMilliVnd(
                    txn.optString("tip",
                    txn.optString("tipAmount",
                    txn.optString("betAmount", "0"))));
            // If body amount missing, look up prior tip row.
            if (tipMilliVnd == 0) {
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT tip_amount FROM awc_transactions " +
                             "WHERE platform_tx_id = ? AND action = 'tip' LIMIT 1")) {
                    ps.setString(1, "tip_" + platformTxId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) tipMilliVnd = rs.getLong("tip_amount") * 1000L;
                    }
                } catch (Exception e) {
                    logger.error("handleCancelTip: lookup failed platformTxId={}", platformTxId, e);
                }
            }

            saveTxnCustom(txn, "cancelTip", 0, 0, 0, 0, Math.floorDiv(tipMilliVnd, 1000L), 0, rawBody);
            totalRefundMilli += tipMilliVnd;
        }

        if (totalRefundMilli > 0) {
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(userId, totalRefundMilli);
            } catch (Exception e) {
                logger.error("handleCancelTip applyMilliDelta failed user={} total={}", username, totalRefundMilli, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinCredit > 0) addBalance(username, vinCredit);
        }
        return balanceRespDecimal(userId, getPlayerBalance(username));
    }

    // ===== NON-BALANCE HANDLERS =====

    private String handleSettle(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-22: AWC sends multi-user batches (e.g. 3 players × ~16 settle txns each).
        // Resolve username per-txn, accumulate credits per-user, defer non-critical
        // work (Mongo log + commission publish) to BATCH_ASYNC so response stays
        // under the 20s timeout.
        Map<String, Long> creditByUser = new HashMap<>();
        Map<String, Long> userIdByName = new HashMap<>();

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String awcUserId = txn.optString("userId", "");
            String username = awcUserIdToUsername(awcUserId);
            if (username == null) continue;

            // JLS-5-4: jackpotWinAmount is paid on top of winAmount (JILI-SLOT
            // cert). Fold both into winMilliVnd so wallet credit + audit row +
            // downstream voidSettle (which reads back win_milli) all stay
            // consistent.
            long winMilliVnd = parseMilliVnd(txn.optString("winAmount", "0"))
                    + parseMilliVnd(txn.optString("jackpotWinAmount", "0"));
            long betMilliVnd = parseMilliVnd(txn.optString("betAmount", "0"));
            long turnoverMilliVnd = parseMilliVnd(txn.optString("turnover", "0"));

            long betAmtVnd = Math.floorDiv(betMilliVnd, 1000L);
            long winAmtVnd = Math.floorDiv(winMilliVnd, 1000L);
            long turnoverVnd = Math.floorDiv(turnoverMilliVnd, 1000L);

            // Audit-first INSERT IGNORE — gates retries.
            boolean isNew = saveTxnWithMilli(txn, "settle", betAmtVnd, winAmtVnd, turnoverVnd,
                    betMilliVnd, winMilliVnd, 0, rawBody);
            if (!isNew) continue;

            creditByUser.merge(username, winMilliVnd, Long::sum);

            // Defer non-critical work — these still happen, just don't block the response.
            final String uName = username;
            final JSONObject txnRef = txn;
            BATCH_ASYNC.submit(() -> {
                try {
                    writeMongoLog(txnRef, "settle", betAmtVnd, winAmtVnd, 0);
                    triggerCommission(uName, txnRef, betAmtVnd, winAmtVnd, turnoverVnd);
                } catch (Throwable t) {
                    logger.warn("handleSettle async work failed user={}: {}", uName, t.getMessage());
                }
            });
        }

        // Apply aggregated credits per user.
        for (Map.Entry<String, Long> e : creditByUser.entrySet()) {
            String uName = e.getKey();
            long totalCreditMilli = e.getValue();
            if (totalCreditMilli <= 0) continue;
            long uId = userIdByName.computeIfAbsent(uName, this::resolveUserId);
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(uId, totalCreditMilli);
            } catch (Exception ex) {
                logger.error("handleSettle applyMilliDelta failed user={} total={}", uName, totalCreditMilli, ex);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinCredit > 0) addBalance(uName, vinCredit);
        }
        return okResp();
    }

    private String handleRefund(JSONObject message, String rawBody) {
        return handleSimpleRefund(message, rawBody, "refund");
    }

    private String handleUnsettle(JSONObject message, String rawBody) {
        // Settled → Bet: claw back winAmount
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        JSONObject txn = txns.getJSONObject(0);
        String awcUserId = txn.optString("userId", "");
        String username = awcUserIdToUsername(awcUserId);
        if (username == null) return errResp("1000", "Invalid userId");

        String platformTxId = txn.optString("platformTxId", "");
        if (isDuplicateTxn("unsettle_" + platformTxId)) return okResp();

        long winMilliVnd = parseMilliVnd(txn.optString("winAmount", "0"));
        if (winMilliVnd > 0) {
            long userId = resolveUserId(username);
            long vinDebit;
            try {
                vinDebit = -AwcResidueTracker.applyMilliDelta(userId, -winMilliVnd);
            } catch (Exception e) {
                logger.error("handleUnsettle applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinDebit > 0) deductBalance(username, vinDebit);
        }

        long balanceAfter = getPlayerBalance(username);
        saveTxn(txn, "unsettle", 0, Math.floorDiv(winMilliVnd, 1000L), 0, balanceAfter, rawBody);

        // SUN-1182: settle pushed commission via triggerCommission;
        // unsettle must reverse it so the agent doesn't keep commission
        // on a bet whose result was undone.
        reverseAwcCommission(platformTxId, "unsettle");

        return okResp();
    }

    private String handleVoidBet(JSONObject message, String rawBody) {
        return handleSimpleRefund(message, rawBody, "voidBet");
    }

    private String handleVoidSettle(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-16: aggregate clawback (winLoss = win - bet); single wallet op.
        String username = null;
        long userId = -1;
        long totalClawbackMilli = 0;

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String awcUserId = txn.optString("userId", "");
            String platformTxId = txn.optString("platformTxId", "");
            if (username == null) {
                username = awcUserIdToUsername(awcUserId);
                if (username == null) return errResp("1000", "Invalid userId");
                userId = resolveUserId(username);
            }

            if (isDuplicateTxn("voidSettle_" + platformTxId)) continue;

            long origBetMilli = 0;
            long origWinMilli = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT bet_milli, win_milli FROM awc_transactions " +
                         "WHERE platform_tx_id IN (?, ?) AND action IN ('settle','betNSettle') LIMIT 1")) {
                ps.setString(1, "settle_" + platformTxId);
                ps.setString(2, platformTxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        origBetMilli = rs.getLong("bet_milli");
                        origWinMilli = rs.getLong("win_milli");
                    } else {
                        return errResp("0001", "SETTLE_NOT_FOUND");
                    }
                }
            } catch (Exception e) {
                logger.error("handleVoidSettle: lookup failed platformTxId={}", platformTxId, e);
                return errResp("9999", "Internal: settle lookup failed");
            }

            saveTxn(txn, "voidSettle", Math.floorDiv(origBetMilli, 1000L), Math.floorDiv(origWinMilli, 1000L), 0, 0, rawBody);
            totalClawbackMilli += (origWinMilli - origBetMilli);
            reverseAwcCommission(platformTxId, "void-settle");
        }

        if (totalClawbackMilli > 0) {
            long vinDebit;
            try {
                vinDebit = -AwcResidueTracker.applyMilliDelta(userId, -totalClawbackMilli);
            } catch (Exception e) {
                logger.error("handleVoidSettle applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinDebit > 0) deductBalance(username, vinDebit);
        } else if (totalClawbackMilli < 0) {
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(userId, -totalClawbackMilli);
            } catch (Exception e) {
                logger.error("handleVoidSettle applyMilliDelta credit failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinCredit > 0) addBalance(username, vinCredit);
        }
        return okResp();
    }

    private String handleUnvoidBet(JSONObject message, String rawBody) {
        // Void → Bet: re-deduct betAmount
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        JSONObject txn = txns.getJSONObject(0);
        String awcUserId = txn.optString("userId", "");
        String username = awcUserIdToUsername(awcUserId);
        if (username == null) return errResp("1000", "Invalid userId");

        String platformTxId = txn.optString("platformTxId", "");
        if (isDuplicateTxn("unvoidBet_" + platformTxId)) return okResp();

        long betMilliVnd = parseMilliVnd(txn.optString("betAmount", "0"));
        if (betMilliVnd > 0) {
            long userId = resolveUserId(username);
            long vinDebit;
            try {
                vinDebit = -AwcResidueTracker.applyMilliDelta(userId, -betMilliVnd);
            } catch (Exception e) {
                logger.error("handleUnvoidBet applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinDebit > 0) deductBalance(username, vinDebit);
        }

        long balanceAfter = getPlayerBalance(username);
        saveTxn(txn, "unvoidBet", Math.floorDiv(betMilliVnd, 1000L), 0, 0, balanceAfter, rawBody);

        return okResp();
    }

    private String handleUnvoidSettle(JSONObject message, String rawBody) {
        // Void → Settled: pay winAmount
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        JSONObject txn = txns.getJSONObject(0);
        String awcUserId = txn.optString("userId", "");
        String username = awcUserIdToUsername(awcUserId);
        if (username == null) return errResp("1000", "Invalid userId");

        String platformTxId = txn.optString("platformTxId", "");
        if (isDuplicateTxn("unvoidSettle_" + platformTxId)) return okResp();

        long winMilliVnd = parseMilliVnd(txn.optString("winAmount", "0"));
        if (winMilliVnd > 0) {
            long userId = resolveUserId(username);
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(userId, winMilliVnd);
            } catch (Exception e) {
                logger.error("handleUnvoidSettle applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinCredit > 0) addBalance(username, vinCredit);
        }

        long balanceAfter = getPlayerBalance(username);
        saveTxn(txn, "unvoidSettle", 0, Math.floorDiv(winMilliVnd, 1000L), 0, balanceAfter, rawBody);

        return okResp();
    }

    private String handleResettle(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-38: AWC sends multi-txn (and multi-user) resettle batches;
        // accumulate per-user net delta, single wallet op per user. Mirrors
        // the per-txn audit-first idempotency pattern from handleVoidSettle.
        Map<String, Long> deltaMilliByUser = new HashMap<>();
        Map<String, Long> userIdByName = new HashMap<>();

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String awcUserId = txn.optString("userId", "");
            String username = awcUserIdToUsername(awcUserId);
            if (username == null) return errResp("1000", "Invalid userId");

            String platformTxId = txn.optString("platformTxId", "");
            long betMilliVnd = parseMilliVnd(txn.optString("betAmount", "0"));
            long winMilliVnd = parseMilliVnd(txn.optString("winAmount", "0"));
            long turnoverMilliVnd = parseMilliVnd(txn.optString("turnover", "0"));

            // Look up old settle/betNSettle's winMilli for this txn.
            long oldWinMilli = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT win_milli FROM awc_transactions " +
                         "WHERE platform_tx_id IN (?, ?) AND action IN ('settle','betNSettle','resettle') " +
                         "ORDER BY id DESC LIMIT 1")) {
                ps.setString(1, "settle_" + platformTxId);
                ps.setString(2, platformTxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) oldWinMilli = rs.getLong("win_milli");
                }
            } catch (Exception e) {
                logger.error("handleResettle: lookup failed platformTxId={}", platformTxId, e);
                return errResp("9999", "Internal: settle lookup failed");
            }

            // Audit-first INSERT IGNORE — per-txn idempotency gate.
            // SXB-38: saveTxnWithMilli to preserve sub-VND in win_milli; the
            // saved row becomes oldWin for subsequent resettle iterations.
            long balancePlaceholder = getPlayerBalance(username);
            boolean isNew = saveTxnWithMilli(txn, "resettle",
                    Math.floorDiv(betMilliVnd, 1000L), Math.floorDiv(winMilliVnd, 1000L),
                    Math.floorDiv(turnoverMilliVnd, 1000L),
                    betMilliVnd, winMilliVnd,
                    balancePlaceholder, rawBody);
            if (!isNew) {
                logger.info("AWC resettle duplicate (audit-first): platformTxId=resettle_{}", platformTxId);
                continue;
            }

            long deltaMilli = winMilliVnd - oldWinMilli;
            if (deltaMilli == 0) continue;

            deltaMilliByUser.merge(username, deltaMilli, Long::sum);
            userIdByName.computeIfAbsent(username, this::resolveUserId);
        }

        // Apply the aggregated delta per user. Single residue/wallet op per
        // user keeps the residue tracker honest under multi-txn batches.
        for (Map.Entry<String, Long> e : deltaMilliByUser.entrySet()) {
            String username = e.getKey();
            long deltaMilli = e.getValue();
            long userId = userIdByName.get(username);
            if (deltaMilli > 0) {
                long vinCredit;
                try {
                    vinCredit = AwcResidueTracker.applyMilliDelta(userId, deltaMilli);
                } catch (Exception ex) {
                    logger.error("handleResettle applyMilliDelta credit failed user={}", username, ex);
                    return errResp("9999", "Internal: residue tracker error");
                }
                if (vinCredit > 0) addBalance(username, vinCredit);
            } else if (deltaMilli < 0) {
                long vinDebit;
                try {
                    vinDebit = -AwcResidueTracker.applyMilliDelta(userId, deltaMilli);
                } catch (Exception ex) {
                    logger.error("handleResettle applyMilliDelta debit failed user={}", username, ex);
                    return errResp("9999", "Internal: residue tracker error");
                }
                // SXB-36: resettle-lose may force balance negative (player
                // won, then result corrected → claw back the win, balance
                // can owe). Bypass MoneyGateway's "Insufficient" guard to
                // honor AWC's contract.
                if (vinDebit > 0) deductBalanceAllowNegative(username, vinDebit);
            }
        }
        return okResp();
    }

    private String handleFreeSpin(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        JSONObject txn = txns.getJSONObject(0);
        String awcUserId = txn.optString("userId", "");
        String username = awcUserIdToUsername(awcUserId);
        if (username == null) return errResp("1000", "Invalid userId");

        String platformTxId = txn.optString("platformTxId", "");
        if (isDuplicateTxn("freeSpin_" + platformTxId)) return okResp();

        long winMilliVnd = parseMilliVnd(txn.optString("winAmount", "0"));
        if (winMilliVnd > 0) {
            long userId = resolveUserId(username);
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(userId, winMilliVnd);
            } catch (Exception e) {
                logger.error("handleFreeSpin applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinCredit > 0) addBalance(username, vinCredit);
        }

        long balanceAfter = getPlayerBalance(username);
        saveTxn(txn, "freeSpin", 0, Math.floorDiv(winMilliVnd, 1000L), 0, balanceAfter, rawBody);

        return okResp();
    }

    private String handleGive(JSONObject message, String rawBody) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        JSONObject txn = txns.getJSONObject(0);
        String awcUserId = txn.optString("userId", "");
        String username = awcUserIdToUsername(awcUserId);
        if (username == null) return errResp("1000", "Invalid userId");

        // JLS-13 (JILI promotional give): AWC ships `promotionTxId` instead of
        // `platformTxId`. Both fields share the same role (provider-side unique
        // id used for idempotency) so we accept either as the dedup key. The
        // saveTxnCustomFull persistence layer reads `platformTxId` first too,
        // and JIRA confirms the cert harness retries the SAME id 5x to verify
        // single-credit semantics.
        String txKey = txn.optString("platformTxId", "");
        if (txKey.isEmpty()) txKey = txn.optString("promotionTxId", "");
        if (txKey.isEmpty()) return errResp("9999", "give: missing platformTxId/promotionTxId");
        if (isDuplicateTxn("give_" + txKey)) return okResp();

        long amountMilliVnd = parseMilliVnd(txn.optString("amount", "0"));

        // Audit-first INSERT IGNORE so concurrent retries with the same txKey
        // collapse to a single wallet credit even if isDuplicateTxn() races.
        // Stash promotionTxId back onto the json under platformTxId so
        // saveTxnCustomFull's UNIQUE platform_tx_id key gates correctly.
        if (!txn.has("platformTxId") || txn.optString("platformTxId", "").isEmpty()) {
            txn.put("platformTxId", txKey);
        }
        long balancePlaceholder = getPlayerBalance(username);
        boolean isNew = saveTxnCustomFull(txn, "give", 0, Math.floorDiv(amountMilliVnd, 1000L),
                0, 0, 0, 0, amountMilliVnd, balancePlaceholder, rawBody);
        if (!isNew) {
            logger.info("AWC give duplicate (audit-first): txKey=give_{}", txKey);
            return okResp();
        }

        if (amountMilliVnd > 0) {
            long userId = resolveUserId(username);
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(userId, amountMilliVnd);
            } catch (Exception e) {
                logger.error("handleGive applyMilliDelta failed user={}", username, e);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinCredit > 0) addBalance(username, vinCredit);
        }

        return okResp();
    }

    private String handleSimpleRefund(JSONObject message, String rawBody, String action) {
        JSONArray txns = message.optJSONArray("txns");
        if (txns == null || txns.length() == 0) return errResp("9999", "No txns");

        // SXB-23: AWC sends multi-user batches (e.g. 138 voidBet txns covering
        // 3 players × 46 each). Per-user accumulation prevents one player's
        // refund from being credited to another.
        Map<String, Long> refundByUser = new HashMap<>();
        Map<String, Long> userIdByName = new HashMap<>();

        for (int i = 0; i < txns.length(); i++) {
            JSONObject txn = txns.getJSONObject(i);
            String awcUserId = txn.optString("userId", "");
            String platformTxId = txn.optString("platformTxId", "");
            String username = awcUserIdToUsername(awcUserId);
            if (username == null) continue;

            if (isDuplicateTxn(action + "_" + platformTxId)) continue;

            long betMilliVnd = parseMilliVnd(txn.optString("betAmount", "0"));
            saveTxn(txn, action, Math.floorDiv(betMilliVnd, 1000L), 0, 0, 0, rawBody);
            refundByUser.merge(username, betMilliVnd, Long::sum);
            reverseAwcCommission(platformTxId, action);
        }

        for (Map.Entry<String, Long> e : refundByUser.entrySet()) {
            String uName = e.getKey();
            long totalRefundMilli = e.getValue();
            if (totalRefundMilli <= 0) continue;
            long uId = userIdByName.computeIfAbsent(uName, this::resolveUserId);
            long vinCredit;
            try {
                vinCredit = AwcResidueTracker.applyMilliDelta(uId, totalRefundMilli);
            } catch (Exception ex) {
                logger.error("handleSimpleRefund({}) applyMilliDelta failed user={} total={}", action, uName, totalRefundMilli, ex);
                return errResp("9999", "Internal: residue tracker error");
            }
            if (vinCredit > 0) addBalance(uName, vinCredit);
        }
        return okResp();
    }

    // ===== WALLET OPERATIONS (via Hazelcast + MySQL) =====

    /**
     * AWC userId is built as `prefix + nickname` by AwcApiClient.createMember /
     * doLoginAndLaunchGame (both called with nickname as the first arg in
     * LaunchAwcGameProcessor). So the stripped suffix is the player's NICKNAME,
     * not user_name. Balance / deduct / add / triggerCommission all query by
     * user_name, so resolve nickname → user_name here once and pass user_name
     * downstream.
     *
     * Fallback: if the DB lookup misses (caller sent a raw user_name, or a
     * legacy account where nickname=user_name), return the stripped value
     * unchanged.
     */
    private String awcUserIdToUsername(String awcUserId) {
        if (awcUserId == null || awcUserId.isEmpty()) return null;
        String prefix = AwcConfig.prefix();
        String candidate = awcUserId.startsWith(prefix) ? awcUserId.substring(prefix.length()) : awcUserId;
        candidate = AwcConfig.reverseNicknameOverride(candidate);
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT user_name FROM vinplay.users WHERE nick_name = ? LIMIT 1")) {
            ps.setString(1, candidate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("user_name");
            }
        } catch (Exception e) {
            logger.warn("awcUserIdToUsername: nickname→user_name lookup failed for {}: {}",
                    awcUserId, e.getMessage());
        }
        return candidate;
    }

    /**
     * Read player balance directly from MySQL (canonical), no cache.
     *
     * <p>SUN-1xxx (2026-05-11): policy per docs/architecture/LEDGER_HARDENING_ROADMAP.md
     * — any balance read that gates a money movement must read the canonical
     * store directly. Hazelcast users IMap drifts (signup-seed staleness,
     * lock-timeout evictions, HZ disconnect leaving stale entries). Previous
     * HZ-first code returned vin=0 for fresh accounts whose cache had not yet
     * been refreshed after first credit; AWC bet gate at line 226 then rejected
     * every bet with "Insufficient balance" while users.vin had real money.
     *
     * <p>On MySQL error we fall back to the Hazelcast {@code users} IMap
     * last-known-good before returning 0. The cache is updated on every
     * wallet mutation via {@link #updateHzBalance(String, long)} so it
     * lags by at most one in-flight tx — strictly better than the prior
     * "return 0 on any MySQL hiccup" behaviour which cache-poisoned the
     * SEXY vendor iframe (SUN-1340: random "ví 0.00"). When both MySQL
     * and Hazelcast fail we still return 0; the per-call ERROR/WARN log
     * is the operator signal. Cost: one DB round-trip per AWC bet (~1ms),
     * plus a single Hazelcast {@code get} only on the failure path.
     */
    private long getPlayerBalance(String username) {
        // First attempt: MySQL primary.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            return readDbBalance(conn, username);
        } catch (Exception e1) {
            // SXB getBalance hardening 2026-05-17: single immediate retry
            // before any Hazelcast read. Most "MySQL hiccup" cases (pool
            // checkout race, brief row lock contention, transient socket
            // timeout) clear by the time we acquire a fresh connection;
            // the retry trades one extra round-trip on the failure path
            // for avoiding HZ touches that could return slightly-stale
            // values. HZ fallback is preserved for the genuine outage
            // case (MySQL gone for seconds).
            //
            // 2026-05-18 (MR !434 review): removed Thread.sleep(50) — we
            // are on a Jetty request thread; sleeping risks pool exhaust
            // under load. ConnectionPool.getConnection() blocks anyway
            // when MySQL is slow, providing natural backoff without
            // dedicating a request thread to a sleep.
            logger.warn("getPlayerBalance MySQL primary failed for {} — single immediate retry (will fall to HZ if retry also fails)", username);
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                long bal = readDbBalance(conn, username);
                logger.info("getPlayerBalance MySQL retry succeeded for {} — recovered without HZ", username);
                return bal;
            } catch (Exception ignored) {
                // fall through to SUN-1340 HZ fallback below
            }
            logger.error("getPlayerBalance MySQL primary + retry both failed for {} — trying Hazelcast fallback", username, e1);
            try {
                String nickname = resolveNickname(username);
                if (nickname != null && !nickname.isEmpty()) {
                    HazelcastInstance hz = HazelcastClientFactory.getInstance();
                    IMap<String, UserCacheModel> users = hz.getMap("users");
                    UserCacheModel u = users.get(nickname);
                    if (u != null) {
                        long cached = u.getVin();
                        logger.warn("getPlayerBalance Hazelcast fallback for {} (nick={}) -> {}",
                                username, nickname, cached);
                        return cached;
                    }
                }
            } catch (Exception cacheEx) {
                logger.warn("getPlayerBalance Hazelcast fallback failed for {}: {}",
                        username, cacheEx.getMessage());
            }
            // Both reads failed — 0 still cache-poisons vendor but we
            // have nothing better. Log lines above identify the user.
            return 0L;
        }
    }

    private long deductBalance(String username, long amount) {
        UserRef ref = lookupUser(username);
        if (ref == null) {
            logger.error("deductBalance: cannot resolve userId for username={}", username);
            return -1;
        }
        com.vinplay.dal.service.MoneyGateway.CreditResult cr =
                com.vinplay.dal.service.MoneyGateway.debitUser(ref.id, ref.nickname, amount,
                        com.vinplay.dal.service.MoneyGateway.SOURCE_AWC_DEBIT, null,
                        "AWC seamless wallet debit");
        if (!cr.success) {
            if (cr.error != null && cr.error.contains("Insufficient")) return -1;
            logger.error("deductBalance gateway failed user={} amount={} err={}", username, amount, cr.error);
            return -1;
        }
        return cr.newBalance;
    }

    /**
     * SXB-36: AWC seamless wallet contract allows the operator wallet to go
     * negative during resettle-lose / voidSettle clawback. Routes through
     * MoneyGateway.debitUserAllowNegative so audit + ledger + Hazelcast
     * cache stay consistent (only the floor check is dropped).
     */
    private long deductBalanceAllowNegative(String username, long amount) {
        UserRef ref = lookupUser(username);
        if (ref == null) {
            logger.error("deductBalanceAllowNegative: cannot resolve userId for username={}", username);
            return -1;
        }
        com.vinplay.dal.service.MoneyGateway.CreditResult cr =
                com.vinplay.dal.service.MoneyGateway.debitUserAllowNegative(ref.id, ref.nickname, amount,
                        com.vinplay.dal.service.MoneyGateway.SOURCE_AWC_DEBIT, null,
                        "AWC seamless wallet debit (negative-balance allowed)");
        if (!cr.success) {
            logger.error("deductBalanceAllowNegative gateway failed user={} amount={} err={}",
                    username, amount, cr.error);
            return getPlayerBalance(username);
        }
        return cr.newBalance;
    }

    private long addBalance(String username, long amount) {
        UserRef ref = lookupUser(username);
        if (ref == null) {
            logger.error("addBalance: cannot resolve userId for username={}", username);
            return getPlayerBalance(username);
        }
        com.vinplay.dal.service.MoneyGateway.CreditResult cr =
                com.vinplay.dal.service.MoneyGateway.creditUser(ref.id, ref.nickname, amount,
                        com.vinplay.dal.service.MoneyGateway.SOURCE_AWC_CREDIT, null,
                        "AWC seamless wallet credit");
        if (!cr.success) {
            logger.error("addBalance gateway failed user={} amount={} err={}", username, amount, cr.error);
            return getPlayerBalance(username);
        }
        return cr.newBalance;
    }

    /**
     * Resolve (users.id, users.nick_name) from users.user_name in one query.
     * Replaces the previous lookupUserId + resolveNickname combo so the AWC
     * hot path does one round-trip instead of two.
     */
    private UserRef lookupUser(String username) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, nick_name FROM vinplay.users WHERE user_name = ? LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nick = rs.getString("nick_name");
                    return new UserRef(rs.getLong("id"),
                            nick != null && !nick.isEmpty() ? nick : username);
                }
            }
        } catch (Exception e) {
            logger.warn("lookupUser failed for {}: {}", username, e.getMessage());
        }
        return null;
    }

    private static final class UserRef {
        final long id;
        final String nickname;
        UserRef(long id, String nickname) { this.id = id; this.nickname = nickname; }
    }

    private long readDbBalance(Connection conn, String username) throws Exception {
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("readDbBalance: null/empty username");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT vin FROM vinplay.users WHERE user_name = ? LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("vin");
            }
        }
        // SXB getBalance hardening 2026-05-17: was `return 0L` here, which
        // silently returned 0 when the SELECT found no row (deleted user,
        // stale reverse-override mapping, never-registered AWC member). AWC
        // vendor caches whatever we return, so a single missed lookup
        // turned into a sticky "balance 0" display until next mutation.
        // Throw instead — the caller's retry + HZ fallback will fire and
        // at minimum log lines identify the user for ops.
        throw new IllegalStateException("readDbBalance: no row for user_name=" + username);
    }

    private void updateHzBalance(String username, long newBalance) {
        // Hazelcast `users` IMap is keyed by nick_name (per
        // MoneyGateway.creditUser line 121, PortalBalanceConsumer line
        // 81). Earlier code keyed by user_name → silent no-op every AWC
        // settle, so the FE never got a balance push and players had to
        // refresh manually. Resolve nickname first.
        String nickname = resolveNickname(username);
        try {
            if (nickname != null && !nickname.isEmpty()) {
                HazelcastInstance hz = HazelcastClientFactory.getInstance();
                IMap<String, UserCacheModel> users = hz.getMap("users");
                UserCacheModel u = users.get(nickname);
                if (u != null) {
                    u.setVin(newBalance);
                    users.put(nickname, u);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to update Hazelcast balance for {} (nick={}): {}",
                    username, nickname, e.getMessage());
        }
        // Push balance to FE via the canonical helper. Routes to
        // queue_action_portal (PortalBalanceConsumer → WebSocket) +
        // queue_action_minigame in one call so AWC seamless wallet
        // matches every other money-mutation path.
        if (nickname == null || nickname.isEmpty()) {
            logger.warn("AWC balance WS push: no nickname for {}, skip", username);
            return;
        }
        com.vinplay.dal.service.MoneyGateway.publishBalanceUpdate(nickname);
    }

    /**
     * user_name → nick_name. Hazelcast users IMap is keyed by nickname
     * (not user_name), so we have to resolve before any IMap op or WS
     * push. Falls back to null on lookup error — caller skips push if
     * null rather than spraying garbage.
     */
    private String resolveNickname(String username) {
        if (username == null || username.isEmpty()) return null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT nick_name FROM vinplay.users WHERE user_name = ? LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("nick_name");
            }
        } catch (Exception e) {
            logger.warn("resolveNickname failed for {}: {}", username, e.getMessage());
        }
        return null;
    }

    // ===== PERSISTENCE =====

    private boolean isDuplicateTxn(String platformTxId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM awc_transactions WHERE platform_tx_id = ? LIMIT 1")) {
            ps.setString(1, platformTxId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.error("isDuplicateTxn check failed for {}", platformTxId, e);
            return false;
        }
    }

    private boolean saveTxn(JSONObject txn, String action, long betAmount, long winAmount,
                         long turnover, long balanceAfter, String rawBody) {
        return saveTxnCustom(txn, action, betAmount, winAmount, turnover, 0, 0, balanceAfter, rawBody);
    }

    /**
     * Overload that accepts raw milli-VND values for bet and win.
     * Used by callers that already have the milli amounts (e.g. handleBet, handleSettle).
     * betAmountVnd / winAmountVnd are the integer vin values written to bet_amount / win_amount;
     * betMilliVnd / winMilliVnd are stored in bet_milli / win_milli for sub-VND precision.
     *
     * Returns true if the row was newly inserted (isNew), false if it was a duplicate.
     * Callers use this for the audit-first idempotency pattern: INSERT first, gate
     * wallet mutation on the return value to close the SELECT-then-act race window.
     */
    private boolean saveTxnWithMilli(JSONObject txn, String action,
                                  long betAmountVnd, long winAmountVnd, long turnover,
                                  long betMilliVnd, long winMilliVnd,
                                  long balanceAfter, String rawBody) {
        return saveTxnCustomFull(txn, action, betAmountVnd, winAmountVnd, turnover, 0, 0,
                betMilliVnd, winMilliVnd, balanceAfter, rawBody);
    }

    private boolean saveTxnCustom(JSONObject txn, String action, long betAmount, long winAmount,
                               long turnover, long adjustAmount, long tipAmount,
                               long balanceAfter, String rawBody) {
        return saveTxnCustomFull(txn, action, betAmount, winAmount, turnover, adjustAmount, tipAmount,
                betAmount * 1000L, winAmount * 1000L, balanceAfter, rawBody);
    }

    /**
     * Core INSERT IGNORE for idempotent transaction recording.
     * Returns true if the row was newly inserted (rowsAffected == 1),
     * false if the platform_tx_id already existed (rowsAffected == 0, silently ignored).
     * INSERT IGNORE is unambiguous regardless of MySQL Connector/J CLIENT_FOUND_ROWS flag
     * (useAffectedRows=false default). ON DUPLICATE KEY UPDATE returned rowsAffected=1 for
     * ALL retries when the status column was already 'duplicate' (no change → matched-rows
     * count=1 via CLIENT_FOUND_ROWS), causing wallet double-credits on retries 3+.
     */
    private boolean saveTxnCustomFull(JSONObject txn, String action, long betAmount, long winAmount,
                                   long turnover, long adjustAmount, long tipAmount,
                                   long betMilli, long winMilli,
                                   long balanceAfter, String rawBody) {
        String platformTxId = txn.optString("platformTxId", action + "_" + System.currentTimeMillis());
        // Prefix action to make idempotency keys unique per action type
        if (!"bet".equals(action) && !"betNSettle".equals(action)) {
            platformTxId = action + "_" + platformTxId;
        }

        String awcUserId = txn.optString("userId", "");
        String username = awcUserIdToUsername(awcUserId);
        long userId = resolveUserId(username);

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT IGNORE INTO awc_transactions (platform_tx_id, round_id, user_id, user_name, " +
                     "platform, game_code, game_name, game_type, action, bet_amount, win_amount, " +
                     "turnover, adjust_amount, tip_amount, bet_milli, win_milli, currency, money_type, " +
                     "bet_time, tx_time, status, balance_after, raw_json) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ")) {
            ps.setString(1, platformTxId);
            ps.setString(2, txn.optString("roundId", null));
            ps.setLong(3, userId);
            ps.setString(4, username != null ? username : "");
            ps.setString(5, txn.optString("platform", ""));
            ps.setString(6, txn.optString("gameCode", null));
            ps.setString(7, txn.optString("gameName", null));
            ps.setString(8, txn.optString("gameType", null));
            ps.setString(9, action);
            ps.setLong(10, betAmount);
            ps.setLong(11, winAmount);
            ps.setLong(12, turnover);
            ps.setLong(13, adjustAmount);
            ps.setLong(14, tipAmount);
            ps.setLong(15, betMilli);
            ps.setLong(16, winMilli);
            ps.setString(17, txn.optString("currency", "VND"));
            ps.setInt(18, 1); // money_type = vin
            ps.setString(19, txn.optString("betTime", null));
            ps.setString(20, txn.optString("txTime", null));
            ps.setString(21, "success");
            ps.setLong(22, balanceAfter);
            ps.setString(23, rawBody != null && rawBody.length() > 4000 ? rawBody.substring(0, 4000) : rawBody);
            // INSERT IGNORE: rowsAffected=1 → new insert, 0 → duplicate-key conflict (silently ignored)
            int rowsAffected = ps.executeUpdate();
            return rowsAffected == 1;
        } catch (Exception e) {
            logger.error("saveTxn failed: action={} platformTxId={}", action, platformTxId, e);
            // On DB error treat as duplicate to avoid double-crediting on retry
            return false;
        }
    }

    private void writeMongoLog(JSONObject txn, String action, long betAmount, long winAmount, long balanceAfter) {
        try {
            String awcUserId = txn.optString("userId", "");
            String username = awcUserIdToUsername(awcUserId);
            long userId = resolveUserId(username);

            // Resolve nick_name with the same priority used by
            // triggerCommission so the mongo doc and the rebate_logs row
            // for the same txn agree on the player's display name:
            //   1. Strip AwcConfig.prefix() from awcUserId — AWC builds
            //      it as (prefix + nickname).
            //   2. If that yielded the raw user_name (no prefix matched
            //      / smoke test path / legacy callback), fall back to
            //      cached MySQL user_name → nick_name lookup.
            // Without this fallback, smoke-test and legacy traffic land
            // in mongo with user_name=<raw> while rebate_logs has
            // player_nickname=<nick>, so the agency LS Cược query
            // (filters by nickname) misses what LS Rolling shows.
            String nickName = "";
            if (awcUserId != null && !awcUserId.isEmpty()) {
                String prefix = AwcConfig.prefix();
                if (awcUserId.startsWith(prefix)) {
                    nickName = AwcConfig.reverseNicknameOverride(awcUserId.substring(prefix.length()));
                } else {
                    String resolved = lookupNickByUsername(username);
                    nickName = (resolved != null && !resolved.isEmpty()) ? resolved : awcUserId;
                }
            }

            com.mongodb.client.MongoDatabase db = com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory.getDB();
            // SUN-1248 / GSC-parity: ledger-style ONE-ROW-PER-TXN upsert
            // model.  Mirrors GscBetSideEffectPublisher — every callback
            // for the same platformTxId mutates the SAME mongo doc.
            //
            // - filter by platform_tx_id (unique per txn).
            // - wager_code = AWC roundId so multi-bet sub-bets sharing
            //   one round can be collapsed by GameHistoryService's reader
            //   (gscHistoryGroupKey priority — wager_code wins).
            // - bet writes betAmount; settle writes win_amount + action.
            //   $setOnInsert seeds bet-side defaults so a settle that
            //   arrives before bet still leaves a queryable row.
            //
            // Earlier `insertOne` produced TWO docs per txn (one for
            // action=bet, one for action=settle), each carrying bet_amount,
            // which doubled volumes everywhere downstream — the agency LS
            // Cược 2× bug Phuong/Mr.DEAL flagged. Upsert collapses to one.
            String platformTxId = txn.optString("platformTxId", "");
            String roundId      = txn.optString("roundId", "");
            java.util.Date now  = new java.util.Date();

            org.bson.Document setFields = new org.bson.Document()
                    .append(com.vinplay.dal.service.seamless.SeamlessProvider.FIELD,
                            com.vinplay.dal.service.seamless.SeamlessProvider.AWC)
                    .append("user_id", userId)
                    .append("user_name", nickName)
                    .append("account_name", username)
                    .append("nick_name", nickName)
                    .append("platform", txn.optString("platform", ""))
                    .append("game_code", txn.optString("gameCode", ""))
                    .append("game_name", txn.optString("gameName", ""))
                    .append("game_type", txn.optString("gameType", ""))
                    .append("action", action)
                    .append("currency", txn.optString("currency", "VND"))
                    .append("bet_time", txn.optString("betTime", ""))
                    .append("tx_time", txn.optString("txTime", ""));

            // Per-action field ownership:
            //   bet         → owns bet_amount (stake debit)
            //   betNSettle  → owns BOTH bet_amount AND win_amount (instant)
            //   settle      → owns win_amount + balance_after; preserves bet
            //   resettle    → overrides win_amount + balance_after
            //   cancelBet/voidBet/refund/etc → action stamp + balance_after
            //
            // SUN-1275: also persist the milli-VND raw value (× 1000) so the
            // agency LS Cược can render Win/Lose with the same 2-decimal
            // precision as the player-facing transaction report. The integer
            // bet_amount/win_amount columns floorDiv away the .45 in
            // 314.45 — keeping bet_amount_milli=314450 lets the reader
            // recover it (winMilli / 1000.0 = 314.45). Wallet credit/debit
            // still rides the floorDiv'd integer per the AWC seamless
            // contract.
            long betMilli = parseMilliVnd(txn.optString("betAmount", "0"));
            long winMilli = parseMilliVnd(txn.optString("winAmount", "0"))
                          + parseMilliVnd(txn.optString("jackpotWinAmount", "0"));
            if ("bet".equals(action) || "betNSettle".equals(action)) {
                setFields.append("bet_amount", betAmount);
                setFields.append("bet_amount_milli", betMilli);
            }
            if ("settle".equals(action) || "betNSettle".equals(action) || "resettle".equals(action)) {
                setFields.append("win_amount", winAmount);
                setFields.append("win_amount_milli", winMilli);
            }
            if (balanceAfter > 0) setFields.append("balance_after", balanceAfter);
            // turnover only meaningful on settle/betNSettle (round total).
            if ("settle".equals(action) || "betNSettle".equals(action)) {
                long turnMilli = parseMilliVnd(txn.optString("turnover", "0"));
                long turn      = Math.floorDiv(turnMilli, 1000L);
                if (turn > 0) {
                    setFields.append("turnover", turn);
                    setFields.append("turnover_milli", turnMilli);
                }
            }

            org.bson.Document setOnInsert = new org.bson.Document()
                    .append("platform_tx_id", platformTxId)
                    .append("round_id", roundId)
                    .append("wager_code", roundId.isEmpty() ? platformTxId : roundId)
                    .append("create_time", now)
                    .append("created_at", now);
            // Only seed defaults for the OPPOSITE-side field. Mongo rejects
            // ($set, $setOnInsert) targeting the same path with code 40
            // ("would create a conflict at <path>"), so we put bet_amount
            // in exactly one of them per action.
            boolean ownsBet = "bet".equals(action) || "betNSettle".equals(action);
            boolean ownsWin = "settle".equals(action) || "betNSettle".equals(action) || "resettle".equals(action);
            if (!ownsBet) setOnInsert.append("bet_amount", 0L);
            if (!ownsWin) setOnInsert.append("win_amount", 0L);

            org.bson.Document update = new org.bson.Document()
                    .append("$set", setFields)
                    .append("$setOnInsert", setOnInsert);

            org.bson.Document filter = new org.bson.Document("platform_tx_id", platformTxId);
            db.getCollection("log_awc_bets")
                    .updateOne(filter, update, new com.mongodb.client.model.UpdateOptions().upsert(true));
        } catch (Exception e) {
            logger.warn("MongoDB log_awc_bets write failed: {}", e.getMessage());
        }
    }

    /**
     * SUN-1100: trigger commission for settled AWC bets via the same RMQ
     * pipeline the GSC integration uses (UserMoneyServiceImpl.bet) and the
     * native game servers use (PotServiceImpl, BotServiceImpl, etc.).
     *
     * <p>Previously this method tried to {@code INSERT INTO
     * vinplay.log_money_user_extra} — but {@code log_money_user_extra} is a
     * MongoDB collection, not a MySQL table. Every AWC settle hit the catch
     * branch with "table doesn't exist" and no rebate row was ever written.
     *
     * <p>Now publishes a {@link LogMoneyUserMessage} to the RMQ
     * {@code queue_log_money_user_extra} consumed by vbee's
     * LogMoneyUserExtraProcessor, which calls triggerAutoCommission and
     * walks the agent chain. Same path GSC uses → identical commission
     * behaviour for AWC and GSC bets.
     *
     * <p>moneyExchange is set to {@code -turnover} so the consumer sees a
     * "loss" event with volume = turnover, regardless of whether the
     * specific txn was a win or loss. Commission is paid on turnover, not
     * net P&L.
     */
    /**
     * SUN-AWC-COMM: build the bet's commission lookup key as
     * {@code awc_<platform>_<gameCode>} so vbee's CommissionRateResolver
     * (Layer 2 = CATEGORY) can look the platform up in
     * {@code awc_platform_map} and resolve it to the existing
     * {@code live_cat_<Category>} rates that ops already maintain for
     * GSC live casino.
     *
     * <p>Earlier this method bucketed to {@code awc_live}/{@code awc_slot}/
     * etc. to keep the per-agent rate table small, but those keys never hit
     * the {@code live_cat_*} cascade — every AWC bet resolved at rate 0
     * with no rebate_logs row, so AWC traffic was invisible in agency LS
     * Rolling. Per-game EXACT seeding still works as an override, falling
     * through to CATEGORY when no row matches.
     */
    private static String awcCommissionKey(JSONObject txn) {
        String platform = txn.optString("platform", "").toLowerCase();
        String gameCode = txn.optString("gameCode", "").toLowerCase();
        if (platform.isEmpty()) {
            // Fallback bucketing for malformed callbacks lacking platform —
            // the legacy gameType bucket is better than no key at all.
            String gameType = txn.optString("gameType", "").toUpperCase();
            if (gameType.contains("LIVE"))   return "awc_live";
            if (gameType.contains("SLOT"))   return "awc_slot";
            if (gameType.contains("FH") || gameType.contains("FISH")) return "awc_fish";
            if (gameType.contains("ESPORTS") || gameType.contains("SPORT")) return "awc_sport";
            if (gameType.contains("EGAME"))  return "awc_egame";
            return "awc_other";
        }
        if (gameCode.isEmpty()) gameCode = "unknown";
        return "awc_" + platform + "_" + gameCode;
    }

    private void triggerCommission(String username, JSONObject txn, long betAmount, long winAmount, long turnover) {
        try {
            String platform = txn.optString("platform", "").toLowerCase();
            String gameCode = txn.optString("gameCode", "").toLowerCase();
            String actionKey = awcCommissionKey(txn);

            // SUN-1248 / multi-bet baccarat: commission volume is THIS sub-bet's
            // stake, not AWC's cumulative `turnover` field. AWC stamps turnover
            // = round-level cumulative wager on every sub-bet's settle, so a
            // 220 + 40 round emits 260 (sub-bet 1 turnover) + 40 (sub-bet 2
            // betAmount, since sub-bet 2's turnover sometimes 0 → falls back)
            // = 300 in rebate_logs, which double-counts sub-bet 2.
            //
            // Per-txn betAmount matches log_awc_bets.bet_amount and the
            // agency LS Cược totals exactly. Single-bet rounds are
            // unaffected (betAmount == turnover).
            long volume = betAmount;
            if (volume <= 0L) return; // nothing to base commission on

            int userId = (int) resolveUserId(username);
            if (userId <= 0) {
                logger.warn("AWC triggerCommission: no user_id for {}, skipping commission publish", username);
                return;
            }

            // vbee LogMoneyUserExtraProcessor.triggerAutoCommission queries
            // vinplay.users WHERE nick_name=? and useragent.nickname=? against
            // message.getNickname(). Pass the player's NICKNAME, not user_name.
            //
            // Resolution priority:
            //  1. Strip AwcConfig.prefix() from awcUserId — AWC builds it as
            //     (prefix + nickname), so the suffix IS the nickname.
            //  2. If awcUserId carried no prefix (legacy callback / smoke
            //     test sending raw user_name), fall back to a cached SQL
            //     lookup user_name → nick_name. Without this fallback the
            //     chain would resolve against the user_name string and miss.
            String awcUserId = txn.optString("userId", "");
            String prefix    = AwcConfig.prefix();
            String nickName;
            if (!awcUserId.isEmpty() && awcUserId.startsWith(prefix)) {
                nickName = AwcConfig.reverseNicknameOverride(awcUserId.substring(prefix.length()));
            } else {
                nickName = lookupNickByUsername(username);
                if (nickName == null || nickName.isEmpty()) nickName = username;
            }

            LogMoneyUserMessage msg = new LogMoneyUserMessage(
                    userId,
                    nickName,
                    actionKey,            // gameName / actionName — matches game_commission_rate.game_key
                    "80",                 // serviceName — numeric game_id matching MONGO_SOURCES
                                          // ("80" = Live Casino (AWC)). Must be parseable by
                                          // Long.parseLong: vbee's isCommissionEligibleMessage and
                                          // LogSumReportUserSQL both reject the message when this
                                          // field is non-numeric, dropping it before
                                          // triggerAutoCommission can fire.
                    0L,                   // currentMoney — informational, vbee reads message.getMoneyExchange()
                    -volume,              // moneyExchange: negative so consumer reads volume = -me
                    "vin",
                    "AWC " + actionKey + " bet=" + betAmount + " win=" + winAmount + " turnover=" + turnover,
                    0L,                   // fee
                    true,                 // vp / playgame flag
                    false                 // isBot — AWC users are real players, never bots
            );
            // SUN-1182: pin the message id to the platform tx so vbee
            // LogMoneyUserExtraProcessor uses it as the deterministic
            // sourceKey (note = "AUTO_COMMISSION source=awc:<platformTxId> ...").
            // Cancel/void/unsettle handlers below match on this exact key
            // to reverse the corresponding rebate_logs rows.
            String platformTxId = txn.optString("platformTxId", "");
            if (!platformTxId.isEmpty()) {
                msg.setId(awcSourceKey(platformTxId));
            }
            // SUN-1250 reopen: set wager_code = AWC roundId so LogMoneyUserExtraProcessor
            // stamps it on the rebate_logs row. GSC already does this via
            // GscWithdrawAggregator (SUN-1248). Without wager_code on AWC rows
            // the agency LS Rolling reader (queryLogsAggregated) GROUPs BY
            // wager_code and falls back to per-id grouping when null — so a
            // multi-bet Sexy Live Baccarat round (300 + 50 add-on) renders
            // as two rolling rows instead of one collapsed row of 350.
            // Mirror the mongo writer (line 1489): roundId when present, else
            // platformTxId so single-bet rounds still get a value.
            String roundIdForRebate = txn.optString("roundId", "");
            msg.setWagerCode(roundIdForRebate.isEmpty() ? platformTxId : roundIdForRebate);
            MessageBusFactory.get("queue_log_money").publish("queue_log_money", msg, 601);
        } catch (Throwable t) {
            // Non-fatal — wallet update already succeeded. Commission failure
            // here is a soft incident, not a player-facing bug.
            logger.warn("AWC triggerCommission failed for {}: {}", username, t.getMessage());
        }
    }

    /**
     * SUN-1182: stable source key for AWC bets. Embedded in the rebate
     * message's id (becomes the {@code source=...} segment of
     * {@code rebate_logs.note}) at bet/settle time, so cancel/void/unsettle
     * can find and reverse the exact rows via
     * {@code RebateService.reverseBySourceKey}.
     */
    private static String awcSourceKey(String platformTxId) {
        return "awc:" + platformTxId;
    }

    /**
     * SUN-1182: reverse the agency-side rebate rows produced when this
     * bet was originally settled. Idempotent — safe on duplicate cancel
     * callbacks. Best-effort: failure is logged but does not affect the
     * AWC-facing balance response (provider already saw the refund).
     */
    private void reverseAwcCommission(String platformTxId, String reason) {
        if (platformTxId == null || platformTxId.isEmpty()) return;
        try {
            java.util.Set<Integer> agents = com.vinplay.dal.rebate.RebateService
                    .reverseBySourceKey(awcSourceKey(platformTxId), reason);
            for (Integer agentId : agents) {
                try {
                    com.vinplay.vbee.common.cache.ResponseCacheHelper
                            .invalidateForAgent(agentId);
                } catch (Throwable cacheErr) {
                    logger.warn("AWC reverseAwcCommission cache invalidate failed agentId={} err={}",
                            agentId, cacheErr.getMessage());
                }
            }
        } catch (Throwable t) {
            logger.warn("AWC reverseAwcCommission failed platformTxId={} reason={} err={}",
                    platformTxId, reason, t.getMessage());
        }
    }

    private long resolveUserId(String username) {
        if (username == null) return 0L;
        try {
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, UserCacheModel> users = hz.getMap("users");
            UserCacheModel u = users.get(username);
            if (u != null) return u.getId();
        } catch (Exception ignore) {}
        // Fall back to MySQL. users.id is BIGINT — FK requires a real row.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM vinplay.users WHERE user_name = ? LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("resolveUserId DB fallback failed for {}: {}", username, e.getMessage());
        }
        return 0L;
    }

    // Cache of user_name → nick_name. Used by triggerCommission's fallback
    // path when the awcUserId callback omits the configured prefix
    // (legacy / smoke-test traffic). Bounded by total unique players,
    // entries never invalidate — a player's nickname is effectively
    // immutable for the lifetime of the JVM.
    private static final java.util.concurrent.ConcurrentHashMap<String, String> NICK_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private String lookupNickByUsername(String username) {
        if (username == null || username.isEmpty()) return null;
        String cached = NICK_CACHE.get(username);
        if (cached != null) return cached;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT nick_name FROM vinplay.users WHERE user_name = ? LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nick = rs.getString(1);
                    if (nick != null && !nick.isEmpty()) {
                        NICK_CACHE.put(username, nick);
                        return nick;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("lookupNickByUsername failed for {}: {}", username, e.getMessage());
        }
        return null;
    }

    // ===== RESPONSE BUILDERS =====

    /**
     * Balance response including sub-VND fractional residue.
     * Returns balance as decimal: e.g. vin=27191159, residue=500 → 27191159.500
     */
    private String balanceRespDecimal(long userId, long vinBalance) {
        JSONObject r = new JSONObject();
        r.put("status", "0000");
        int residue = AwcResidueTracker.getResidue(userId);
        BigDecimal full = BigDecimal.valueOf(vinBalance)
                .add(BigDecimal.valueOf(residue).movePointLeft(3));
        r.put("balance", full);
        r.put("balanceTs", OffsetDateTime.now(ZoneOffset.ofHours(8)).format(ISO_TS));
        return r.toString();
    }

    /** Legacy integer balance response — kept for internal fallback uses. */
    private String balanceResp(long balance) {
        JSONObject r = new JSONObject();
        r.put("status", "0000");
        // AWC expects balance as decimal (e.g. 2023.25). Our balance is in integer units.
        r.put("balance", balance);
        r.put("balanceTs", OffsetDateTime.now(ZoneOffset.ofHours(8)).format(ISO_TS));
        return r.toString();
    }

    private String okResp() {
        JSONObject r = new JSONObject();
        r.put("status", "0000");
        return r.toString();
    }

    private String errResp(String code, String desc) {
        JSONObject r = new JSONObject();
        r.put("status", code);
        r.put("desc", desc);
        return r.toString();
    }

    private long parseMoney(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            double d = Double.parseDouble(s);
            return (long) d;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parse a decimal AWC amount string as milli-VND (×1000).
     * Uses BigDecimal to avoid double-precision loss.
     * Examples: "19.5" → 19500, "10" → 10000, "0.001" → 1
     */
    private long parseMilliVnd(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            BigDecimal bd = new BigDecimal(s);
            return bd.movePointRight(3).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Resolve {@code games.category_id} for an AWC bet so the
     * {@code UserGameBlock} matcher can apply category-level rules
     * (e.g. "block all baccarat"). Tries the per-table row first
     * (matches when the round prefix yielded a table_tag), then
     * the per-game row (table_tag=''). Returns null when no row
     * matches — the matcher falls through to the lower predicates.
     */
    /**
     * Returns {@code true} when the (platform, gameCode, tableTag) game is
     * marked active in {@code vinplay.games}. Falls through to
     * (platform, gameCode, '') row when no per-table row exists, then to
     * the platform stub. Returns {@code true} when no row at all matches —
     * fail-open so a brand-new platform AWC just enabled doesn't block bets
     * before ops seeds a games row.
     */
    private static boolean isAwcGameActive(String platform, String gameCode, String tableTag) {
        if (platform == null || platform.isEmpty() || gameCode == null || gameCode.isEmpty()) return true;
        String tag = tableTag == null ? "" : tableTag;
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                        .getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT is_active, table_tag FROM vinplay.games WHERE provider = 'AWC' "
                             + "AND vendor_platform COLLATE utf8mb4_unicode_ci = ? "
                             + "AND game_code COLLATE utf8mb4_unicode_ci = ? "
                             + "AND table_tag COLLATE utf8mb4_unicode_ci IN (?, '') "
                             + "ORDER BY (table_tag = ?) DESC LIMIT 1")) {
            ps.setString(1, platform);
            ps.setString(2, gameCode);
            ps.setString(3, tag);
            ps.setString(4, tag);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 1;
            }
        } catch (Exception ignore) {}
        return true; // fail-open
    }

    private static Integer lookupAwcCategoryId(String platform, String gameCode, String tableTag) {
        if (platform == null || platform.isEmpty() || gameCode == null || gameCode.isEmpty()) return null;
        String tag = tableTag == null ? "" : tableTag;
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                        .getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT category_id FROM vinplay.games WHERE provider = 'AWC' "
                             + "AND vendor_platform COLLATE utf8mb4_unicode_ci = ? "
                             + "AND game_code COLLATE utf8mb4_unicode_ci = ? "
                             + "AND table_tag COLLATE utf8mb4_unicode_ci IN (?, '') "
                             + "ORDER BY (table_tag = ?) DESC, table_tag DESC LIMIT 1")) {
            ps.setString(1, platform);
            ps.setString(2, gameCode);
            ps.setString(3, tag);
            ps.setString(4, tag);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignore) {}
        return null;
    }
}
