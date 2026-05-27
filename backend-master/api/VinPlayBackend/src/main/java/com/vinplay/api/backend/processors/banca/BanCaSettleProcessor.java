package com.vinplay.api.backend.processors.banca;

import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SUN-1054 / Wallet Phase 5b — HTTP bridge for BanCa unified-wallet session settle.
 *
 * <p>The BanCa C# {@code MoneyGatewayClient.SettleAsync} (see
 * {@code banca/Core/Libs/UnifiedWallet/MoneyGatewayClient.cs}) POSTs one
 * JSON body per session-boundary settle (quit, periodic 5s flush, big-bet
 * threshold, openTx cross-game, Revive crash recovery) to this command.
 * The processor delegates to {@link MoneyGateway#creditUser} /
 * {@link MoneyGateway#debitUser} so the existing dual-write infrastructure
 * (money_gateway_log audit + dual-write to money_ledger PLAYER_VIN) is
 * exercised exactly as for every other money mutation.
 *
 * <p><b>Command ID:</b> 9998. Lives on {@code backend-api} (internal admin
 * surface; reachable only from inside the Docker network).
 *
 * <p><b>Auth:</b> shared secret header {@code X-Service-Token} must match
 * env {@code BANCA_SERVICE_TOKEN} (reused from SUN-1054 LogBetCommission).
 * Service-to-service — no aat involved. Reject every call when the env var
 * is missing (fail closed).
 *
 * <p><b>Currency conversion:</b> BanCa works in milli-VND (Player.Cash is
 * VND × 1000); the unified wallet ledger is VND. The processor receives
 * {@code amount_milli} as a signed long, divides by 1000, and passes the
 * result to credit/debit. Sub-VND remainders are floored toward zero so a
 * losing player can never be charged a partial sub-unit; the residue is
 * carried over by the next periodic flush via the in-memory reservation
 * pattern documented in {@code WALLET_PHASE5B_BANCA_GAME_LOOP_IMPL.md}.
 *
 * <p><b>Idempotency:</b> {@code external_ref} =
 * {@code banca:settle:{user_id}:{session_id}:{checkpoint_ms}} — matches the
 * C# client format byte-for-byte. The {@code (tx_id, source)} UNIQUE on
 * {@code money_gateway_log} dedupes a replay from the 3-retry loop.
 * A duplicate POST returns the original {@code ledger_tx_id} +
 * {@code balance_after_vnd} (deduped no-op).
 *
 * <p><b>Failure semantics:</b>
 * <ul>
 *   <li>4xx (validation, insufficient balance, missing user) — HTTP 200,
 *       {@code success=false}, so the C# 3-retry does NOT replay (replaying
 *       a validation failure cannot succeed). The legacy {@code QueueFailed}
 *       on the C# side surfaces a 4xx as terminal.</li>
 *   <li>5xx (DB outage, ledger inconsistency) — HTTP 500 so C# retries.</li>
 * </ul>
 *
 * <p>Hot-path latency target: ≤100ms p99 inside the Docker network. The
 * processor delegates to {@link MoneyGateway} which is the same code path
 * exercised by GSC/AWC seamless wallets and has been measured at ≤30ms p99
 * for the SQL transaction alone.
 *
 * <p><b>Input (JSON body):</b>
 * <pre>
 * {
 *   "user_id":       12345,
 *   "amount_milli":  -870000,          // signed; positive = credit, negative = debit
 *   "session_id":    "bc-12345-7",
 *   "tx_type":       "WAGER_DEBIT_BANCA",
 *   "nick_name":     "laviai",         // optional, used for audit only
 *   "checkpoint_ms": 1713750000123
 * }
 * </pre>
 *
 * <p><b>Output (success):</b>
 * <pre>
 * { "success": true, "ledger_tx_id": 4815162342, "balance_after_vnd": 1500000 }
 * </pre>
 *
 * <p>Errors:
 * <table border="1">
 *   <tr><td>1001</td><td>X-Service-Token missing / wrong</td></tr>
 *   <tr><td>4001</td><td>missing required field (user_id, session_id, tx_type, checkpoint_ms, amount_milli)</td></tr>
 *   <tr><td>4002</td><td>tx_type not one of WAGER_DEBIT_BANCA / WAGER_CREDIT_BANCA / EMERGENCY_BANCA</td></tr>
 *   <tr><td>4003</td><td>tx_type / amount sign mismatch (e.g. credit with negative amount)</td></tr>
 *   <tr><td>4004</td><td>insufficient balance (debit only) — terminal, do not retry</td></tr>
 *   <tr><td>1002</td><td>user not found</td></tr>
 *   <tr><td>9999</td><td>internal — see logs</td></tr>
 * </table>
 */
public class BanCaSettleProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);

        try {
            HttpServletRequest request = param.get();

            // --- Auth (X-Service-Token, fail-closed) ---
            String expectedToken = System.getenv("BANCA_SERVICE_TOKEN");
            String providedToken = request.getHeader("X-Service-Token");
            if (expectedToken == null || expectedToken.isEmpty()) {
                return err(response, "1001", "service token not configured on server");
            }
            if (providedToken == null || !expectedToken.equals(providedToken)) {
                return err(response, "1001", "invalid service token");
            }

            // --- Parse JSON body ---
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = request.getReader()) {
                if (br != null) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            String body = sb.toString().trim();
            if (body.isEmpty() || !body.startsWith("{")) {
                return err(response, "4001", "JSON body required");
            }

            JSONObject in;
            try {
                in = new JSONObject(body);
            } catch (Exception e) {
                return err(response, "4001", "invalid JSON: " + e.getMessage());
            }

            long userId       = in.optLong("user_id", 0L);
            long amountMilli  = in.optLong("amount_milli", Long.MIN_VALUE);
            String sessionId  = in.optString("session_id", "").trim();
            String txType     = in.optString("tx_type", "").trim();
            long checkpointMs = in.optLong("checkpoint_ms", 0L);
            String nickHint   = in.optString("nick_name", "").trim();

            // SUN-13xx Phase 5c audit fix: BanCa C# passes cgame.users.user_id,
            // NOT vinplay.users.id. Resolve via nickname-join (cgame.user_id
            // is NOT 1:1 with vinplay.users.id; the FK vinplay_user_id is also
            // unreliable for legacy accounts).
            if (userId > 0) {
                Long resolved = resolveCgameUserIdToVinplay(userId);
                if (resolved != null && resolved > 0) {
                    userId = resolved;
                }
            }

            if (userId <= 0)            return err(response, "4001", "user_id required");
            if (sessionId.isEmpty())    return err(response, "4001", "session_id required");
            if (txType.isEmpty())       return err(response, "4001", "tx_type required");
            if (checkpointMs <= 0)      return err(response, "4001", "checkpoint_ms required");
            if (amountMilli == Long.MIN_VALUE) return err(response, "4001", "amount_milli required");

            // --- tx_type whitelist ---
            // Phase 5c audit fix (CRITICAL 1, SUN-1054): EMERGENCY_BANCA is
            // direction-agnostic — it is reused for daily-bonus credits, IAP
            // top-ups, cancel-cashout refunds, admin-credits, cashout-rollbacks
            // AND Revive crash-recovery debits. Hard-coding it to debit-only
            // blocked every credit usage with a 4003. Derive direction from
            // the sign of amount_milli for EMERGENCY_BANCA only; keep
            // WAGER_DEBIT_BANCA / WAGER_CREDIT_BANCA strict.
            boolean isCredit;
            String mgSource;
            switch (txType) {
                case "WAGER_CREDIT_BANCA":
                    isCredit = true;
                    mgSource = MoneyGateway.SOURCE_WAGER_CREDIT_BANCA;
                    break;
                case "WAGER_DEBIT_BANCA":
                    isCredit = false;
                    mgSource = MoneyGateway.SOURCE_WAGER_DEBIT_BANCA;
                    break;
                case "EMERGENCY_BANCA":
                    isCredit = (amountMilli > 0);
                    mgSource = MoneyGateway.SOURCE_EMERGENCY_BANCA;
                    break;
                default:
                    return err(response, "4002", "unsupported tx_type: " + txType);
            }

            // --- Sign / direction consistency ---
            // For the two strict tx_types the C# client encodes direction in
            // BOTH the tx_type AND the sign of amount_milli; reject mismatches
            // so a bug in the BanCa side surfaces as a 4xx (no retry) instead
            // of silently moving money the wrong direction. EMERGENCY_BANCA
            // skips this check because direction was derived from the sign
            // above (no mismatch is possible).
            if (!"EMERGENCY_BANCA".equals(txType)) {
                if (isCredit && amountMilli < 0) {
                    return err(response, "4003", "credit tx_type with negative amount_milli");
                }
                if (!isCredit && amountMilli > 0) {
                    return err(response, "4003", "debit tx_type with positive amount_milli");
                }
            }
            if (amountMilli == 0) {
                // Zero-amount tick: idempotent no-op, return success without
                // touching the ledger so periodic flushes for idle players
                // are cheap.
                response.put("success", true);
                response.put("ledger_tx_id", 0L);
                response.put("balance_after_vnd", lookupBalanceVnd(userId));
                response.put("reason", "zero_amount");
                return response.toString();
            }

            // --- milli-VND -> VND (floor toward zero) ---
            // BanCa's hot-path arithmetic is milli-VND. The unified wallet
            // ledger is VND. Floor toward zero so a sub-VND residue cannot
            // sneak across as a free fractional credit; the residue is
            // re-flushed on the next tick via the C# reservation pattern.
            long absMilli = amountMilli < 0 ? -amountMilli : amountMilli;
            long amountVnd = absMilli / 1000L;
            if (amountVnd == 0) {
                // Sub-VND payload: nothing for the ledger to do; ack so the
                // C# caller can advance its checkpoint.
                response.put("success", true);
                response.put("ledger_tx_id", 0L);
                response.put("balance_after_vnd", lookupBalanceVnd(userId));
                response.put("reason", "sub_vnd_residue");
                return response.toString();
            }

            // --- Resolve nickname ---
            // Prefer the hint from the C# caller (avoids a DB round-trip on
            // the hot path); fall back to MySQL when absent or empty.
            String nickname = nickHint;
            if (nickname.isEmpty()) {
                nickname = lookupNickname(userId);
            }
            if (nickname == null || nickname.isEmpty()) {
                return err(response, "1002", "user not found: id=" + userId);
            }

            // --- external_ref (matches C# format exactly) ---
            String externalRef = "banca:settle:" + userId + ":" + sessionId + ":" + checkpointMs;
            String description = "BanCa " + txType + " session=" + sessionId;

            // --- Delegate to MoneyGateway (dual-write infra handles dedup + audit + ledger) ---
            MoneyGateway.CreditResult result;
            if (isCredit) {
                result = MoneyGateway.creditUser(userId, nickname, amountVnd, mgSource, externalRef, description);
            } else {
                result = MoneyGateway.debitUser(userId, nickname, amountVnd, mgSource, externalRef, description);
            }

            if (result == null) {
                return err(response, "9999", "MoneyGateway returned null");
            }

            if (!result.success) {
                // Idempotency hit: MoneyGateway treats a duplicate (tx_id, source)
                // as a failure ("Duplicate transaction") so we'd never see it on
                // the legacy path. The C# replay must observe the SAME ledger
                // outcome as the original POST — look up the original audit
                // row and reply with its balance_after.
                if ("Duplicate transaction".equals(result.error)
                        || MoneyGateway.CreditResult.ERROR_DUPLICATE_TRANSACTION.equals(result.failureCode)) {
                    long bal = lookupBalanceAfterFromAudit(externalRef, mgSource, userId);
                    if (bal < 0) bal = lookupBalanceVnd(userId);
                    response.put("success", true);
                    response.put("ledger_tx_id", 0L);
                    response.put("balance_after_vnd", bal);
                    response.put("reason", "deduped");
                    return response.toString();
                }
                // Insufficient balance — terminal 4xx-style (do NOT retry on the
                // C# side). Surface HTTP 200 / success:false so the retry loop
                // sees a final outcome.
                if (MoneyGateway.CreditResult.ERROR_INSUFFICIENT_BALANCE.equals(result.failureCode)) {
                    return err(response, "4004", "insufficient balance");
                }
                if (MoneyGateway.CreditResult.ERROR_USER_NOT_FOUND.equals(result.failureCode)) {
                    return err(response, "1002", "user not found: id=" + userId);
                }
                // Unknown failure — bubble up as 9999 (5xx-style; retried).
                logger.error("BanCaSettleProcessor: MoneyGateway failed user=" + nickname
                        + " amountVnd=" + amountVnd + " source=" + mgSource
                        + " ref=" + externalRef + " err=" + result.error);
                return err(response, "9999", "wallet update failed: " + result.error);
            }

            response.put("success", true);
            // ledger_tx_id is informational — the unified-wallet audit happens
            // inside MoneyGateway; the canonical idempotency anchor is the
            // external_ref. Return 0 as a sentinel until SUN-1248-style
            // ledger_tx_id propagation lands on CreditResult.
            response.put("ledger_tx_id", 0L);
            response.put("balance_after_vnd", result.newBalance);
            response.put("reason", "posted");
            return response.toString();

        } catch (Exception e) {
            logger.error("BanCaSettleProcessor unhandled error", e);
            return err(response, "9999", "internal: " + e.getMessage());
        }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    /**
     * Hazelcast-first user lookup; falls back to MySQL on cache miss.
     * Returns null when the user truly does not exist.
     */
    /**
     * SUN-13xx Phase 5c audit fix: resolve a BanCa cgame.users.user_id to the
     * canonical vinplay.users.id via nickname-join. cgame.user_id is NOT 1:1
     * with vinplay.users.id (laviai cgame=184 → vinplay=50002 per audit).
     * The cgame.vinplay_user_id FK is also unreliable for legacy accounts.
     *
     * Returns null when the input is already a valid vinplay.users.id (i.e.
     * no nickname match in cgame, OR the same row exists in vinplay).
     * Returns the resolved vinplay.users.id when the input is clearly a
     * cgame-only id mapping to a different vinplay id.
     */
    private static Long resolveCgameUserIdToVinplay(long inputId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT v.id FROM cgame.users c " +
                     "INNER JOIN vinplay.users v ON v.nick_name = c.nickname " +
                     "WHERE c.user_id = ? LIMIT 1")) {
            ps.setLong(1, inputId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("BanCaSettleProcessor.resolveCgameUserIdToVinplay failed inputId=" + inputId
                    + ": " + e.getMessage());
        }
        return null;
    }

    private static String lookupNickname(long userId) {
        // Hazelcast 'users' map is keyed by nickname — there is no userId-keyed
        // map, so the cache path is not directly useful here. Fall straight to
        // a single indexed SELECT.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT nick_name FROM users WHERE id = ? LIMIT 1")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            logger.warn("BanCaSettleProcessor.lookupNickname failed userId=" + userId
                    + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Current PLAYER_VIN balance in VND. Used on dedup hits + zero-amount
     * acks so the C# caller always gets a fresh balance_after value.
     * Returns 0 on lookup failure (informational only — never affects ledger).
     */
    private static long lookupBalanceVnd(long userId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT vin FROM users WHERE id = ? LIMIT 1")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("BanCaSettleProcessor.lookupBalanceVnd failed userId=" + userId
                    + ": " + e.getMessage());
        }
        return 0L;
    }

    /**
     * On dedup, look up the original {@code balance_after} from
     * {@code money_gateway_log} so the C# replay gets the SAME response as
     * the original POST. Returns -1 on lookup failure (caller falls back to
     * current balance).
     */
    private static long lookupBalanceAfterFromAudit(String txId, String source, long userId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT balance_after FROM money_gateway_log " +
                     "WHERE tx_id = ? AND source = ? AND user_id = ? LIMIT 1")) {
            ps.setString(1, txId);
            ps.setString(2, source);
            ps.setLong(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("BanCaSettleProcessor.lookupBalanceAfterFromAudit failed ref=" + txId
                    + ": " + e.getMessage());
        }
        return -1L;
    }
}
