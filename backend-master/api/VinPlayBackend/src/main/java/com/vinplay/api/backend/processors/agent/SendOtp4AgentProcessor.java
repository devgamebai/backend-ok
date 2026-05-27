package com.vinplay.api.backend.processors.agent;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

/**
 * c=9464 — Generate + deliver OTP for agent actions (e.g. before c=9462 TopupUser).
 *
 * Path: /api_agent
 * Params:
 *   code = agent code (from session)
 *   nn   = agent nickname
 *   tp   = delivery type ("Telegram" | "SMS", default "Telegram")
 *
 * Flow:
 *   1. Generate 6-digit OTP
 *   2. Cache in Hazelcast map "cacheAgentOtp" key="{code}" value="{otp}|{tp}" TTL 5min
 *   3. Deliver via Telegram bot (reads chat_id from useragent.telegram column)
 *   4. Return success
 *
 * Bot token: TELEGRAM_BOT_TOKEN env var (set in docker-compose)
 * Chat ID:   useragent.telegram column — must be configured per agent by admin
 */
public class SendOtp4AgentProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");
    private static final String OTP_CACHE_MAP = "cacheAgentOtp";
    private static final int OTP_TTL_MINUTES = 5;
    private static final SecureRandom RNG = new SecureRandom();

    // Resolve bot token at class load time from env vars
    private static final String BOT_TOKEN;
    static {
        String t = System.getenv("TELEGRAM_BOT_TOKEN");
        if (t == null || t.isEmpty() || t.startsWith("YOUR_")) {
            t = System.getenv("TELEGRAM_DEPOSIT_BOT_TOKEN");
        }
        BOT_TOKEN = (t != null && !t.startsWith("YOUR_")) ? t : null;
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("errorCode", "1001");

        try {
            HttpServletRequest request = param.get();

            // Path gating
            String serPath = request.getServletPath();
            if (serPath == null || !"/api_agent".equals(serPath)) {
                response.put("errorCode", "5");
                response.put("message", "Not allow access this api");
                return response.toString();
            }

            String agentCode = request.getParameter("code");
            String nickname  = request.getParameter("nn");
            String type      = request.getParameter("tp");
            if (type == null || type.isEmpty()) type = "Telegram";

            if (agentCode == null || agentCode.isEmpty()) {
                response.put("errorCode", "4001");
                response.put("message", "Agent code required");
                return response.toString();
            }

            // Generate 6-digit OTP
            int otpInt = 100000 + RNG.nextInt(900000);
            String otp = String.valueOf(otpInt);

            // Cache via DistCache (Hazelcast or Redis per cache.routing.properties)
            com.vinplay.vbee.common.cache.DistCache<String, String> otpMap =
                    com.vinplay.vbee.common.cache.CacheFactory.get(OTP_CACHE_MAP, String.class);
            otpMap.put(agentCode, otp + "|" + type, OTP_TTL_MINUTES, TimeUnit.MINUTES);

            // Fetch agent info from DB
            String telegramChatId = null;
            String agentNickname  = nickname;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT nickname, telegram FROM vinplay_admin.useragent WHERE code = ? LIMIT 1")) {
                ps.setString(1, agentCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        agentNickname  = rs.getString("nickname");
                        telegramChatId = rs.getString("telegram");
                    }
                }
            } catch (Exception dbErr) {
                logger.warn("SendOtp4AgentProcessor: DB lookup failed for code=" + agentCode, dbErr);
            }

            // Deliver OTP via Telegram
            boolean sent = false;
            String deliveryNote;

            if ("Telegram".equalsIgnoreCase(type)) {
                if (telegramChatId != null && !telegramChatId.trim().isEmpty()) {
                    sent = sendTelegramMessage(telegramChatId.trim(), buildOtpMessage(agentNickname, otp));
                    deliveryNote = sent
                            ? "Telegram delivered to chatId=" + telegramChatId
                            : "Telegram send FAILED for chatId=" + telegramChatId;
                } else {
                    deliveryNote = "No Telegram chat_id configured for agent code=" + agentCode
                                 + " (set useragent.telegram column)";
                    logger.warn("SendOtp4AgentProcessor: " + deliveryNote);
                }
            } else {
                deliveryNote = "Delivery type '" + type + "' not implemented";
            }

            logger.info("AgentOTP generated code=" + agentCode + " nn=" + agentNickname
                    + " type=" + type + " delivered=" + sent + " | " + deliveryNote);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("message", sent ? "OTP sent" : "OTP generated but not delivered — " + deliveryNote);
            response.put("type", type);
            response.put("ttlMinutes", OTP_TTL_MINUTES);
            response.put("delivered", sent);
            // DEV/STAGING ONLY — remove in production:
            response.put("otp", otp);

        } catch (Exception e) {
            logger.error("SendOtp4AgentProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Internal error");
        }
        return response.toString();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String buildOtpMessage(String agentName, String otp) {
        return "\uD83D\uDD10 *Ma OTP cua ban*\n\n"
             + "Dai ly: *" + agentName + "*\n"
             + "Ma OTP: *" + otp + "*\n"
             + "Hieu luc: *5 phut*\n\n"
             + "_Khong chia se ma nay cho bat ky ai._";
    }

    private boolean sendTelegramMessage(String chatId, String text) {
        if (BOT_TOKEN == null) {
            logger.warn("SendOtp4AgentProcessor: TELEGRAM_BOT_TOKEN not configured");
            return false;
        }
        try {
            URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int httpCode = conn.getResponseCode();
            if (httpCode == 200) {
                return true;
            }
            logger.warn("SendOtp4AgentProcessor: Telegram API HTTP " + httpCode + " chatId=" + chatId);
            return false;
        } catch (Exception e) {
            logger.error("SendOtp4AgentProcessor: Telegram send error chatId=" + chatId, e);
            return false;
        }
    }
}
