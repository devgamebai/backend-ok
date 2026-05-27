package com.vinplay.api.processors.cashback;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Player API (c=3083): Claim all pending cashback for the logged-in user.
 *
 * Flow:
 *  1. Validate session token → resolve nickname.
 *  2. Compute total pending amount for this user's SELF rebate logs
 *     (SELECT ... FOR UPDATE for row-level isolation).
 *  3. Atomically mark all matching rows as PAID
 *     (UPDATE ... WHERE status='PENDING' prevents double-claim races).
 *  4. Credit the total into the player's game wallet via the canonical
 *     Hazelcast cache + RMQ pattern (matches RechargeServiceImpl.rechargeByCard
 *     and UserServiceImpl.updateMoney):
 *       - lock(users:{nick}) → get UserCacheModel
 *       - user.setVin(vin + claimed); user.setVinTotal(vinTotal + claimed)
 *       - publish MoneyMessageInMinigame + LogMoneyUserMessage (so the credit
 *         lands in log_money_user as "hoàn cược" per SUN-751)
 *       - userMap.put → unlock
 *  5. Return new balance + claimed amount + rows_affected.
 *
 * Concurrency: the UPDATE is the atomic barrier — a second simultaneous claim
 * will affect 0 rows and produce claimed=0 in the response.
 *
 * SUN-764 / SUN-750 / SUN-751 — player cashback claim flow.
 */
public class ClaimCashbackProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) {
                return err(response, "1001", "access token required");
            }

            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                return err(response, "1001", "invalid session");
            }
            String nickname = tokenMap.get(accessToken);

            // SUN-1099: SpecialAccount is read-only — block claim attempt.
            if (com.vinplay.dal.security.RoleGuard.isSpecialAccount(nickname)) {
                return err(response, com.vinplay.dal.security.RoleGuard.ERR_CODE_SPECIAL_ACCOUNT,
                        com.vinplay.dal.security.RoleGuard.ERR_MSG_SPECIAL_ACCOUNT);
            }

            // Rolling 7-day expiry check: wipe if aged-out BEFORE computing the
            // claim total, so the player never claims already-expired pending.
            CashbackExpiryHelper.wipeIfExpired(nickname);

            long claimed = 0;
            int rowsAffected = 0;
            long newBalance = -1;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                conn.setAutoCommit(false);
                try {
                    // 1. Compute total first so we know how much to credit.
                    //    SELECT FOR UPDATE locks the rows so a concurrent
                    //    claim can't undercut us. SUN-1180: scope to
                    //    rebate_amount > 0 — 0-amount placeholders (games
                    //    at 0% commission) stay PENDING in agent rolling
                    //    history; player has nothing to claim for them.
                    try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COALESCE(SUM(rebate_amount),0) AS total, COUNT(*) AS cnt " +
                        "FROM rebate_logs WHERE agent_nickname = ? " +
                        "AND rebate_type = 'SELF' AND status = 'PENDING' AND rebate_amount > 0 FOR UPDATE")) {
                        ps.setString(1, nickname);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                claimed = rs.getLong("total");
                            }
                        }
                    }

                    if (claimed <= 0) {
                        conn.rollback();
                        response.put("success", true);
                        response.put("errorCode", "0");
                        response.put("claimed", 0);
                        response.put("message", "No pending cashback to claim");
                        return response.toString();
                    }

                    // 2. Atomic: mark only the >0 pending rows as PAID.
                    try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE rebate_logs SET status = 'PAID', " +
                        "approved_by = ?, approved_at = NOW() " +
                        "WHERE agent_nickname = ? " +
                        "AND rebate_type = 'SELF' AND status = 'PENDING' AND rebate_amount > 0")) {
                        ps.setString(1, nickname);
                        ps.setString(2, nickname);
                        rowsAffected = ps.executeUpdate();
                    }

                    if (rowsAffected == 0) {
                        // Lost the race to another claim — roll back and report 0.
                        conn.rollback();
                        response.put("success", true);
                        response.put("errorCode", "0");
                        response.put("claimed", 0);
                        response.put("message", "Nothing to claim (race)");
                        return response.toString();
                    }

                    conn.commit();
                } catch (Exception txErr) {
                    try { conn.rollback(); } catch (Exception ignored) {}
                    throw txErr;
                } finally {
                    try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                }
            }

            // SUN-1155: drop cached c=9541 / c=9843 responses for this agent
            // so the next /api/rolling fetch sees the fresh PAID rows
            // immediately, not after the 30 s TTL window. Best-effort —
            // failures are logged inside invalidateForAgent and never fail
            // the claim. No-op for non-agent players (no useragent row).
            try {
                int agentId = lookupAgentId(nickname);
                if (agentId > 0) {
                    int dropped = com.vinplay.vbee.common.cache.ResponseCacheHelper
                            .invalidateForAgent(agentId);
                    if (dropped > 0) {
                        logger.info("ClaimCashbackProcessor: invalidated " + dropped
                                + " cached agency responses for agent=" + nickname
                                + " (id=" + agentId + ")");
                    }
                }
            } catch (Exception cacheErr) {
                logger.warn("ClaimCashbackProcessor: cache invalidation failed (non-fatal) user="
                        + nickname + " err=" + cacheErr.getMessage());
            }

            // 3. SUN-1387 fix 2026-05-18 — credit via MoneyGateway.creditUser
            //    instead of the legacy Hazelcast-lock + setVin + RMQ pattern.
            //    The old path had a silent-fail bug: when the player wasn't
            //    in the Hazelcast `users` cache (eviction, never-cached,
            //    cluster restart), the lookup (`userMap.get(nickname)`)
            //    returned null, the credit was skipped, and the player saw
            //    error 9998 — but rebate_logs were already committed as
            //    PAID in step 2, with no rollback. 26 players, 9.2M KRW
            //    lost to this path before discovery.
            //
            //    MoneyGateway.creditUser does an atomic MySQL UPDATE WHERE
            //    id=? (no cache dependency), writes the canonical
            //    money_gateway_log row (every-tx-has-a-record contract),
            //    evicts the cache so the next read reloads fresh, and is
            //    idempotent against retries via the unique (tx_id, source)
            //    constraint. Batch tx_id keys by the claim moment so a
            //    double-click can't double-credit.
            long userId;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps2 = conn.prepareStatement(
                         "SELECT id FROM vinplay.users WHERE nick_name = ? LIMIT 1")) {
                ps2.setString(1, nickname);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (!rs2.next()) {
                        logger.error("ClaimCashbackProcessor: users row missing for nick="
                                + nickname + " amount=" + claimed + " rows=" + rowsAffected
                                + " — rebate_logs marked PAID but no user row. Investigate.");
                        return err(response, "9998",
                                "Claim recorded but user record missing; please contact support");
                    }
                    userId = rs2.getLong("id");
                }
            } catch (Exception dbErr) {
                logger.error("ClaimCashbackProcessor: userId lookup failed nick=" + nickname
                        + " amount=" + claimed + " rows=" + rowsAffected, dbErr);
                return err(response, "9998",
                        "Claim recorded but wallet update failed; please contact support");
            }

            // SUN-1387 follow-up (MR !434 review CRITICAL): rebate_logs
            // were already committed PAID above. If creditUser fails here
            // the player sees no balance change AND the rolling-period
            // pool is gone \u2014 the bug we just fixed. Two layers of defence:
            //
            //  1. bounded immediate retry (this loop) \u2014 covers transient
            //     DB blips / pool checkout race. The txId is the same
            //     across attempts, so the unique (tx_id, source)
            //     constraint on money_gateway_log makes retries
            //     idempotent: a successful first attempt followed by a
            //     retry-after-network-error returns success on the second
            //     try without double-crediting.
            //
            //  2. RebateCashbackReconciler \u2014 background scanner that
            //     picks up rebate_logs.status='PAID' rows that have no
            //     money_gateway_log entry with the matching txId and
            //     auto-credits via SOURCE_REBATE_RECOVERY. Catches every
            //     failure mode the retry doesn't (process kill, JVM
            //     pause, all retries exhausted).
            String txId = "rebate_claim_" + nickname + "_" + System.currentTimeMillis();
            String description = "Nh\u1eadn ti\u1ec1n ho\u00e0n c\u01b0\u1ee3c (" + rowsAffected + " l\u1ec7nh)";
            com.vinplay.dal.service.MoneyGateway.CreditResult cr = null;
            int maxAttempts = 3;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                cr = com.vinplay.dal.service.MoneyGateway.creditUser(
                        userId, nickname, claimed,
                        com.vinplay.dal.service.MoneyGateway.SOURCE_REBATE_CLAIM,
                        txId, description);
                if (cr.success) break;
                logger.warn("ClaimCashbackProcessor: creditUser attempt " + attempt + "/" + maxAttempts
                        + " failed user=" + nickname + " userId=" + userId + " amount=" + claimed
                        + " err=" + cr.error);
            }
            if (cr == null || !cr.success) {
                logger.error("ClaimCashbackProcessor: MoneyGateway.creditUser FAILED after "
                        + maxAttempts + " attempts user=" + nickname + " userId=" + userId
                        + " amount=" + claimed + " rows=" + rowsAffected
                        + " err=" + (cr == null ? "null" : cr.error)
                        + " — rebate_logs already PAID! Reconciler will pick up via "
                        + "SOURCE_REBATE_RECOVERY on next scan.");
                return err(response, "9998",
                        "Claim recorded but wallet update failed; please contact support");
            }
            newBalance = cr.newBalance;

            logger.info("ClaimCashbackProcessor: OK user=" + nickname
                    + " claimed=" + claimed + " rows=" + rowsAffected + " newBalance=" + newBalance);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("claimed", claimed);
            response.put("rows_affected", rowsAffected);
            if (newBalance >= 0) response.put("balance", newBalance);
        } catch (Exception e) {
            logger.error("ClaimCashbackProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    /**
     * Resolve a login string (nickname / username / agent code) to its
     * {@code useragent.id} so cache invalidation hits the same key
     * GetRebateLogs4AgencyProcessor used to populate it. Returns -1 when
     * the player has no useragent row.
     */
    private static int lookupAgentId(String login) {
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance()
                .getConnection("mysqlpool_admin");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM vinplay_admin.useragent WHERE code = ? OR nickname = ? OR username = ? LIMIT 1")) {
            ps.setString(1, login);
            ps.setString(2, login);
            ps.setString(3, login);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            logger.warn("ClaimCashbackProcessor.lookupAgentId failed login=" + login
                    + " err=" + e.getMessage());
        }
        return -1;
    }
}
