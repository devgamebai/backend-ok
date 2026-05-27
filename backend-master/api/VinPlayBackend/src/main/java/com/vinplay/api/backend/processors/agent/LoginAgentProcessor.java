package com.vinplay.api.backend.processors.agent;

import com.google.gson.JsonObject;
import com.vinplay.vbee.common.security.SmokeTestBypass;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.AgencySession;
import com.vinplay.vbee.common.pools.ConnectionPool;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

public class LoginAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static boolean captchaRequired() {
        String v = System.getenv("CAPTCHA_REQUIRED");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    @Override
    public String execute(Param<HttpServletRequest> parameter) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", "1001");

        HttpServletRequest request = (HttpServletRequest) parameter.get();
        String username = request.getParameter("un");
        String password = request.getParameter("ps");
        String captcha = request.getParameter("cp");
        String captchaId = request.getParameter("cid");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            result.addProperty("errorCode", "1002");
            return result.toString();
        }

        // Internal smoke-test bypass — see SmokeTestBypass for the env var
        // + IP allowlist contract. Disabled by default in production.
        boolean smokeBypass = SmokeTestBypass.isAllowed(request, "agent-login");

        if (captchaRequired() && !smokeBypass) {
            if (captcha == null || captcha.isEmpty() || captchaId == null || captchaId.isEmpty()) {
                result.addProperty("errorCode", "1002");
                return result.toString();
            }

            boolean isCaptchaValid = false;
            com.vinplay.vbee.common.cache.DistCache<String, String> captchaCache =
                    com.vinplay.vbee.common.cache.CacheFactory.get("cacheCaptcha", String.class);
            captcha = captcha.trim().toLowerCase();

            if (captchaCache.containsKey(captchaId)) {
                String storedCaptcha = captchaCache.get(captchaId);
                // Always remove after check to prevent replay attacks
                captchaCache.remove(captchaId);

                if (storedCaptcha != null && storedCaptcha.toLowerCase().equals(captcha)) {
                    isCaptchaValid = true;
                }
            }

            if (!isCaptchaValid) {
                result.addProperty("errorCode", "115");
                return result.toString();
            }
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL);
            if (conn == null) {
                logger.error("LoginAgentProcessor: cannot get DB connection");
                return result.toString();
            }

            // Strategy: verify password against vinplay.users (same account as game),
            // then check if user is an agent in useragent table.
            boolean passwordValid = false;
            try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement uPs = userConn.prepareStatement(
                        "SELECT password FROM users WHERE user_name = ?")) {
                    uPs.setString(1, username);
                    try (ResultSet uRs = uPs.executeQuery()) {
                        if (uRs.next()) {
                            String storedPw = uRs.getString("password");
                            if (storedPw != null && storedPw.equals(password)) {
                                passwordValid = true;
                            }
                        }
                    }
                }
            }

            // SUN-866: chỉ cho phép role='agent' login agency portal. Default
            // của cột là 'agent' nên mọi ĐL hiện tại vẫn login bình thường;
            // SpecialAccount (level=0) đã được set role='admin' ở migration
            // → bị block. Ngoài ra nếu admin CMS đặt role=disabled (hoặc bất
            // kỳ giá trị non-agent nào) cho 1 row, account đó cũng bị từ chối.
            String sql = "SELECT id, nickname, code, level, role, ancestors, percent_bonus_vincard, commission_rate "
                       + "FROM useragent WHERE username=? AND active=1 AND role='agent'";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();

            if (rs.next()) {
                // Check password: either game password matches OR agent-specific password matches
                if (!passwordValid) {
                    // Try agent password from useragent table (SUN-866: same
                    // role='agent' guard as the row-existence query above so
                    // an admin/disabled account can't sneak in via this path).
                    try (PreparedStatement apPs = conn.prepareStatement(
                            "SELECT password FROM useragent WHERE username=? AND password=? AND active=1 AND role='agent'")) {
                        apPs.setString(1, username);
                        apPs.setString(2, password);
                        try (ResultSet apRs = apPs.executeQuery()) {
                            if (apRs.next()) passwordValid = true;
                        }
                    }
                }
                if (!passwordValid) {
                    result.addProperty("errorCode", "1004");
                    result.addProperty("message", "Tài khoản hoặc mật khẩu không đúng");
                    return result.toString();
                }
                result.addProperty("success", true);
                result.addProperty("errorCode", "0");

                int agentId = rs.getInt("id");
                String agentNickname = rs.getString("nickname");

                // SUN-767: issue a fresh session id and store in Hazelcast
                // `agencySessions` (key = agentId). Overwriting the previous
                // entry invalidates any older session for the same agent,
                // so a second-device login kicks the first device on its next
                // API call (the validator compares the caller's sid to the
                // current one stored here).
                String sessionId = UUID.randomUUID().toString();
                try {
                    long now = System.currentTimeMillis();
                    String ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
                    String ua = request.getHeader("User-Agent");
                    AgencySession session = new AgencySession(sessionId, now, ip, ua);
                    com.vinplay.vbee.common.cache.DistCache<Integer, AgencySession> sessionMap =
                            com.vinplay.vbee.common.cache.CacheFactory.get("agencySessions", AgencySession.class);
                    sessionMap.put(agentId, session, 24L, TimeUnit.HOURS);
                } catch (Exception sessErr) {
                    logger.warn("LoginAgentProcessor: agencySessions put failed agentId="
                            + agentId + " err=" + sessErr.getMessage());
                }

                JsonObject data = new JsonObject();
                data.addProperty("id", agentId);
                data.addProperty("nickname", agentNickname);
                data.addProperty("code", rs.getString("code"));
                data.addProperty("level", rs.getInt("level"));
                // SUN-866: surface role to FE/agency UI so dashboard can
                // gate features by role (e.g. only role='agent' gets the
                // commission tab). Today only 'agent' can reach this point
                // because of the WHERE clause above; field is reserved for
                // future roles (e.g. 'agent_lead', 'support', 'admin').
                data.addProperty("role", rs.getString("role"));
                data.addProperty("ancestors", rs.getString("ancestors"));
                data.addProperty("percent_bonus_vincard", rs.getDouble("percent_bonus_vincard"));
                // SUN-1098: emit as 2-decimal string for FE rate display.
                data.addProperty("commission_rate", com.vinplay.dal.utils.PctFormatter.formatRs(rs, "commission_rate"));
                data.addProperty("sessionId", sessionId);

                result.add("data", data);

                // Try update login stats
                try (PreparedStatement uPs = conn.prepareStatement("UPDATE useragent SET last_login_time = NOW(), login_times = login_times + 1 WHERE id = ?")) {
                    uPs.setInt(1, rs.getInt("id"));
                    uPs.executeUpdate();
                } catch (Exception e) {
                    logger.error("LoginAgentProcessor update stats error", e);
                }
            } else {
                result.addProperty("errorCode", "1004"); // wrong username or password
            }

        } catch (Exception e) {
            logger.error("LoginAgentProcessor error", e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            ConnectionPool.releaseConnection(conn);
        }

        return result.toString();
    }
}
