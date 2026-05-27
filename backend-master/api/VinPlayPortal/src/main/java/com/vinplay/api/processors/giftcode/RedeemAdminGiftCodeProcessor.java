package com.vinplay.api.processors.giftcode;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.withdraw.VolumeTrackingService;
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
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

/**
 * c=3070 — User redeem an admin-created gift code.
 *
 * <p>Request params:
 * <ul>
 *   <li>{@code at}   — user access token (required)</li>
 *   <li>{@code code} — giftcode string (required)</li>
 * </ul>
 *
 * <p>Flow:
 * <ol>
 *   <li>Validate user session token → resolve nickname.</li>
 *   <li>Lookup gift code atomically (SELECT … FOR UPDATE).</li>
 *   <li>Validate: exists, not expired, not already used (time_used &lt; max_use).</li>
 *   <li>Check: this user hasn't redeemed this specific code before.</li>
 *   <li>Atomic UPDATE gift_codes SET time_used+1.</li>
 *   <li>INSERT gift_code_useds record.</li>
 *   <li>Credit VIN via Hazelcast cache + RMQ (canonical pattern, same as ClaimCashback).</li>
 *   <li>If rollover_rounds &gt;= 1: add rollover requirement via VolumeTrackingService
 *       (accumulate — PM: cộng dồn, not reset).</li>
 *   <li>Return new balance.</li>
 * </ol>
 *
 * <p>Nguồn tiền: quỹ platform — không trừ ai (PM confirmed).
 */
public class RedeemAdminGiftCodeProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    /** Hazelcast map name for tracking wrong-giftcode attempts per user. */
    private static final String FAIL_MAP      = "giftcode_fail_attempts";
    /** Max consecutive failures before blocking the user. */
    private static final int    MAX_FAIL      = 10;
    /** Block duration in hours after reaching MAX_FAIL. */
    private static final long   BLOCK_HOURS   = 6;

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            // ── 1. Auth ────────────────────────────────────────────────────
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) {
                return err(response, "1001", "Access token required");
            }
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            String nickname = tokenMap.get(accessToken);
            if (nickname == null) {
                return err(response, "1001", "Invalid or expired session");
            }

            // ── 1b. Block check — fail-attempt guard ───────────────────────
            IMap<String, Integer> failMap = hz.getMap(FAIL_MAP);
            Integer failCount = failMap.get(nickname);
            if (failCount != null && failCount >= MAX_FAIL) {
                logger.warn("RedeemAdminGiftCodeProcessor: user BLOCKED by fail-attempt guard nick=" + nickname
                        + " failCount=" + failCount);
                return err(response, "10010",
                        "Tài khoản của bạn đã tạm khóa tính năng nhập Giftcode trong 6 tiếng do nhập sai quá nhiều lần. Vui lòng thử lại sau.");
            }

            // ── 2. Param ───────────────────────────────────────────────────
            String codeParam = request.getParameter("code");
            if (codeParam == null || codeParam.isEmpty()) {
                return err(response, "4001", "code is required");
            }
            String code = codeParam.trim().toUpperCase();

            // ── 3. Resolve user ID from cache ──────────────────────────────
            IMap<String, UserCacheModel> userMap = hz.getMap("users");
            UserCacheModel user = userMap.get(nickname);
            if (user == null) {
                return err(response, "1002", "User not found in cache. Please re-login.");
            }
            int userId = user.getId();

            // ── 4. Lookup + validate gift code (transaction) ───────────────
            long giftMoney;
            int  rolloverRounds;
            int  giftCodeId;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                conn.setAutoCommit(false);
                try {
                    // 4a. Lock & read gift code row
                    String selectSql = "SELECT id, money, rollover_rounds, max_use, time_used, exprired " +
                            "FROM gift_codes WHERE giftcode = ? AND source = 'ADMIN' LIMIT 1 FOR UPDATE";
                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                        ps.setString(1, code);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                recordFailAttempt(failMap, nickname);
                                return err(response, "10001", "Giftcode không tồn tại");
                            }
                            giftCodeId     = rs.getInt("id");
                            giftMoney      = rs.getLong("money");
                            rolloverRounds = rs.getInt("rollover_rounds");
                            int  maxUse    = rs.getInt("max_use");
                            int  timeUsed  = rs.getInt("time_used");
                            Timestamp exprired = rs.getTimestamp("exprired");

                            // 4b. Check expiry
                            if (exprired != null && exprired.before(new Timestamp(System.currentTimeMillis()))) {
                                conn.rollback();
                                recordFailAttempt(failMap, nickname);
                                return err(response, "10002", "Giftcode đã hết hạn sử dụng");
                            }
                            // 4c. Check used quota
                            if (timeUsed >= maxUse) {
                                conn.rollback();
                                recordFailAttempt(failMap, nickname);
                                return err(response, "10003", "Giftcode đã được sử dụng hết");
                            }
                        }
                    }

                    // 4d. Check user hasn't used this code before
                    String checkUsedSql = "SELECT 1 FROM gift_code_useds WHERE giftcode_id = ? AND username = ? LIMIT 1";
                    try (PreparedStatement ps = conn.prepareStatement(checkUsedSql)) {
                        ps.setInt(1, giftCodeId);
                        ps.setString(2, nickname);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                conn.rollback();
                                // Security tradeoff: "already used by you" does NOT count as a failure
                                // to prevent double-clicks from eating strikes.
                                return err(response, "10004", "Bạn đã sử dụng giftcode này rồi");
                            }
                        }
                    }

                    // 4e. Increment time_used
                    String updateSql = "UPDATE gift_codes SET time_used = time_used + 1 WHERE id = ? AND time_used < max_use";
                    int rows;
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setInt(1, giftCodeId);
                        rows = ps.executeUpdate();
                    }
                    if (rows == 0) {
                        conn.rollback();
                        return err(response, "10003", "Giftcode đã được sử dụng hết (race condition)");
                    }

                    // 4f. Record usage (include user_id for deposit history JOIN — see CryptoDepositHistoryProcessor c=3022)
                    String insertUsedSql = "INSERT INTO gift_code_useds (giftcode_id, username, user_id, created_at) VALUES (?, ?, ?, NOW())";
                    try (PreparedStatement ps = conn.prepareStatement(insertUsedSql)) {
                        ps.setInt(1, giftCodeId);
                        ps.setString(2, nickname);
                        ps.setInt(3, userId);
                        ps.executeUpdate();
                    }

                    conn.commit();
                } catch (Exception txErr) {
                    try { conn.rollback(); } catch (Exception ignored) {}
                    throw txErr;
                } finally {
                    try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                }
            }

            // ── 4g. Successful redeem — clear fail-attempt counter ─────────
            failMap.remove(nickname);

            // ── 5. Credit VIN via canonical MoneyGateway path ──────────────
            // SUN-1xxx (2026-05-11): the legacy cache+RMQ pattern relied on
            // UpdateMoneyProcessor to write the credit to MySQL. That processor
            // skips its MySQL UPDATE when the user has been active in vin within
            // the last 30 seconds (SUN-796 throughput-protection). For giftcode
            // redemptions — typically the user's FIRST action after login, with
            // no follow-up vin activity — the skip path means the credit lands
            // in the HZ cache only and never reaches MySQL. In-game shows the
            // bonus; admin / DB-direct readers show 0. 68 affected redemptions
            // (1.36M KRW) backfilled by ops on 2026-05-11 before this fix.
            //
            // MoneyGateway.creditUserWithCumulative does the SQL UPDATE
            // synchronously in the request thread, evicts the HZ cache on
            // commit, and writes an audit row with idempotency on
            // (tx_id, source) — three properties the legacy path lacked.
            long newBalance = -1;
            int userIdInt = user.getId();
            try {
                String txId = "GC-" + giftCodeId + "-" + userIdInt;
                String description = "Nhập giftcode: " + code;
                com.vinplay.dal.service.MoneyGateway.CreditResultWithCumulative cr =
                        com.vinplay.dal.service.MoneyGateway.creditUserWithCumulative(
                                userIdInt, nickname, "vin", giftMoney,
                                "GIFTCODE_ADMIN", txId, description);
                if (cr == null || !cr.success) {
                    logger.error("RedeemAdminGiftCodeProcessor: MoneyGateway credit failed nick=" + nickname
                            + " code=" + code + " money=" + giftMoney
                            + " err=" + (cr != null ? cr.error : "null result")
                            + " — gift_code_useds recorded but wallet NOT updated, manual fix required");
                    return err(response, "9998", "Nhận tiền thành công nhưng cập nhật ví thất bại, vui lòng liên hệ hỗ trợ");
                }
                newBalance = cr.newBalance;

                // RMQ side-effects kept from the legacy path:
                // - queue_payment carries the balance-push notification (game
                //   server reloads the user's session balance).
                // - queue_log_money writes the LogMoneyUser audit + drives
                //   commission attribution / VIP-point accrual.
                // MoneyGateway already wrote the canonical money_gateway_log
                // audit row + handled the SQL commit, so these publishes are
                // strictly best-effort downstream notifications. Failures are
                // non-fatal — see Phase 2C of the LEDGER_HARDENING_ROADMAP
                // for the outbox migration that will retire these.
                try {
                    UserCacheModel freshUser = userMap.get(nickname);
                    if (freshUser != null) {
                        LogMoneyUserMessage msgLog = new LogMoneyUserMessage(
                                freshUser.getId(), nickname,
                                "GiftCodeAdmin", "Khuyến mãi",
                                cr.newTotal, giftMoney,
                                "vin", description,
                                0L, false, freshUser.isBot());
                        msgLog.setReferralCode(freshUser.getReferralCode());
                        // 2026-05-16 Phase B C-fix — drop redundant queue_payment publish.
                        // Wallet write already committed atomically above by
                        // MoneyGateway.creditUserWithCumulative (SOURCE=GIFTCODE_ADMIN).
                        // The legacy queue_payment route fed vbee's
                        // UpdateMoneyProcessor → CALL update_money_user → a
                        // SECOND atomic delta on users.vin, doubling Vithoang's
                        // 20k giftcode to 40k. Removing the publish eliminates
                        // the dual-write; LogMoneyUserMessage (queue_log_money)
                        // stays for Mongo audit / commission pipeline.
                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", msgLog, 601);
                    }
                } catch (Exception rmqErr) {
                    logger.warn("RedeemAdminGiftCodeProcessor: RMQ publish failed (non-fatal, MySQL already committed) nick="
                            + nickname + " err=" + rmqErr.getMessage());
                }
            } catch (Exception walletErr) {
                logger.error("RedeemAdminGiftCodeProcessor: unexpected wallet error nick=" + nickname
                        + " code=" + code + " money=" + giftMoney, walletErr);
                return err(response, "9999", "Lỗi cập nhật ví, vui lòng liên hệ hỗ trợ");
            }

            // ── 6. Apply rollover requirement (cộng dồn — PM confirmed) ───
            if (rolloverRounds >= 1) {
                long requiredVolume = giftMoney * rolloverRounds;
                VolumeTrackingService.addRequiredVolumeDirect(userId, nickname, requiredVolume, false);
                VolumeTrackingService.updateWithdrawalStatus(userId);
                logger.info(String.format(
                        "RedeemAdminGiftCodeProcessor: rollover applied nick=%s code=%s money=%d rollover=%d required=%d",
                        nickname, code, giftMoney, rolloverRounds, requiredVolume));
            }

            logger.info(String.format(
                    "RedeemAdminGiftCodeProcessor: OK nick=%s code=%s money=%d rollover=%d newBalance=%d",
                    nickname, code, giftMoney, rolloverRounds, newBalance));

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("money", giftMoney);
            response.put("rollover_rounds", rolloverRounds);
            if (newBalance >= 0) response.put("balance", newBalance);

        } catch (Exception e) {
            logger.error("RedeemAdminGiftCodeProcessor error", e);
            return err(response, "9999", "Internal server error: " + e.getMessage());
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
     * Increments the wrong-attempt counter for {@code nickname} in Hazelcast.
     * The Hazelcast TTL is refreshed to {@value #BLOCK_HOURS}h on every failed
     * attempt, so the 6-hour lockout window starts from the <em>last</em> failure.
     * When the counter reaches {@value #MAX_FAIL} the next request will be blocked
     * before any DB work is done.
     */
    private static void recordFailAttempt(IMap<String, Integer> failMap, String nickname) {
        try {
            failMap.lock(nickname);
            try {
                Integer current = failMap.get(nickname);
                int next = (current == null ? 0 : current) + 1;
                // put with TTL — Hazelcast will auto-evict after BLOCK_HOURS of inactivity
                failMap.put(nickname, next, BLOCK_HOURS, TimeUnit.HOURS);
                if (next >= MAX_FAIL) {
                    logger.warn("RedeemAdminGiftCodeProcessor: fail-attempt threshold reached nick="
                            + nickname + " attempts=" + next + " — user blocked for " + BLOCK_HOURS + "h");
                } else {
                    logger.debug("RedeemAdminGiftCodeProcessor: fail-attempt recorded nick="
                            + nickname + " attempts=" + next + "/" + MAX_FAIL);
                }
            } finally {
                failMap.unlock(nickname);
            }
        } catch (Exception e) {
            logger.warn("RedeemAdminGiftCodeProcessor: could not record fail attempt for nick="
                    + nickname + " — " + e.getMessage());
        }
    }
}
