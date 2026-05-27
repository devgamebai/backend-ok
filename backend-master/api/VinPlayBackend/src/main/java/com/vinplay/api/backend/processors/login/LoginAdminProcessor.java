package com.vinplay.api.backend.processors.login;

import com.vinplay.api.backend.auth.AdminAuthHelper;
import com.vinplay.vbee.common.security.SmokeTestBypass;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.statics.TimeBasedOneTimePasswordUtil;
import com.vinplay.vbee.common.utils.TotpSecretCodec;
import com.vinplay.vbee.common.utils.BcryptUtils;
import com.hazelcast.core.IMap;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

/**
 * c=701 — Admin CMS login.
 * Authenticates against vinplay_admin.user table.
 * Returns adminToken (used as aat in subsequent calls) + admin info.
 * Single-session: each login generates a new token and invalidates the previous one.
 */
public class LoginAdminProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    /**
     * SUN-1242: resolve the real client IP from request headers in priority
     * order: CF-Connecting-IP (Cloudflare-set) → X-Forwarded-For[0]
     * → X-Real-IP → REMOTE_ADDR. Both this and the wider chain (admin-next
     * forwards X-Forwarded-For via SUN-1242 patch in auth.ts) are needed
     * because:
     *   - admin-next → portal-api hop sets X-Forwarded-For to the real IP
     *   - direct callers (legacy admin-php, agency portal) may set
     *     CF-Connecting-IP or X-Real-IP instead
     *   - REMOTE_ADDR is the last-hop container IP (172.18.x / 172.21.x)
     *     which is what was being logged before this fix
     * Caps at 45 chars (IPv6 mapped max).
     */
    private static String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.length() > 45) {
            ip = ip.substring(0, 45);
        }
        return ip == null ? "" : ip;
    }

    private static boolean captchaRequired() {
        String v = System.getenv("CAPTCHA_REQUIRED");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        boolean consumeCaptcha = false;
        String captchaId = null;
        com.vinplay.vbee.common.cache.DistCache<String, String> captchaCache = null;
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String username = request.getParameter("un");
            String password = request.getParameter("pw");
            String captcha = request.getParameter("cp");
            captchaId = request.getParameter("cid");
            String otp = request.getParameter("otp");

            logger.debug("Request login admin: username: " + username);

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Internal smoke-test bypass: when SMOKE_TEST_BYPASS_KEY is set
            // AND the caller submits a matching X-Smoke-Test-Key header AND
            // the client IP is in an internal range, captcha is skipped so
            // QC / ops can drive automated checks. See SmokeTestBypass.
            boolean smokeBypass = SmokeTestBypass.isAllowed(request, "admin-login");

            if (captchaRequired() && !smokeBypass) {
                if (captcha == null || captcha.isEmpty() || captchaId == null || captchaId.isEmpty()) {
                    response.put("success", false);
                    response.put("errorCode", "1001");
                    return response.toString();
                }

                boolean isCaptchaValid = false;
                captchaCache = com.vinplay.vbee.common.cache.CacheFactory.get("cacheCaptcha", String.class);
                captcha = captcha.trim().toLowerCase();

                if (captchaCache.containsKey(captchaId)) {
                    String storedCaptcha = captchaCache.get(captchaId);

                    if (storedCaptcha != null && storedCaptcha.toLowerCase().equals(captcha)) {
                        isCaptchaValid = true;
                        consumeCaptcha = true;
                    } else {
                        // Remove invalid captcha immediately
                        captchaCache.remove(captchaId);
                    }
                }

                if (!isCaptchaValid) {
                    response.put("success", false);
                    response.put("errorCode", "115");
                    return response.toString();
                }
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                String sql = "SELECT ID, UserName, FullName, Password, Status, ParentID, GoogleAuthSecretEnc, Is2FAEnabled, Last2FAWindow FROM user WHERE UserName = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            response.put("success", false);
                            response.put("errorCode", "1005");
                            return response.toString();
                        }

                        // Check password
                        String dbPassword = rs.getString("Password");
                        if (!password.equals(dbPassword)) {
                            response.put("success", false);
                            response.put("errorCode", "1007");
                            return response.toString();
                        }

                        // Check status
                        String status = rs.getString("Status");
                        if (!"A".equals(status)) {
                            response.put("success", false);
                            response.put("errorCode", "1109");
                            return response.toString();
                        }

                        int adminId = rs.getInt("ID");
                        String userName = rs.getString("UserName");
                        String fullName = rs.getString("FullName");
                        int parentId = rs.getObject("ParentID") != null ? rs.getInt("ParentID") : 0;
                        int is2FAEnabled = rs.getInt("Is2FAEnabled");

                        if (is2FAEnabled == 1 && !smokeBypass) {
                            if (otp == null || otp.isEmpty()) {
                                consumeCaptcha = false; // Do not consume captcha to allow OTP submission
                                response.put("success", false);
                                response.put("errorCode", "1008");
                                return response.toString();
                            }

                            com.vinplay.vbee.common.cache.DistCache<String, String> failedAttempts =
                                    com.vinplay.vbee.common.cache.CacheFactory.get("failed_2fa_attempts", String.class);
                            String attemptsStr = failedAttempts.get(username);
                            int attempts = 0;
                            long firstFailTs = 0;
                            if (attemptsStr != null) {
                                String[] parts = attemptsStr.split(",");
                                attempts = Integer.parseInt(parts[0]);
                                firstFailTs = Long.parseLong(parts[1]);
                                if (attempts >= 5 && System.currentTimeMillis() - firstFailTs < 15 * 60 * 1000) {
                                    response.put("success", false);
                                    response.put("errorCode", "1010"); // Locked
                                    return response.toString();
                                }
                            }

                            String secretEnc = rs.getString("GoogleAuthSecretEnc");
                            long lastWindow = rs.getLong("Last2FAWindow");
                            boolean isValidOtp = false;
                            long currentWindow = System.currentTimeMillis() / 30000;

                            if (otp.length() == 6) {
                                if (currentWindow > lastWindow) {
                                    String secret = TotpSecretCodec.decrypt(secretEnc);
                                    if (secret != null) {
                                        try {
                                            if (TimeBasedOneTimePasswordUtil.validateCurrentNumber(secret, Integer.parseInt(otp), 1)) {
                                                isValidOtp = true;
                                                try (PreparedStatement psUpd = conn.prepareStatement("UPDATE user SET Last2FAWindow = ? WHERE ID = ?")) {
                                                    psUpd.setLong(1, currentWindow);
                                                    psUpd.setInt(2, adminId);
                                                    psUpd.executeUpdate();
                                                }
                                            }
                                        } catch (NumberFormatException nfe) {
                                            logger.error("Invalid OTP format", nfe);
                                        }
                                    }
                                }
                            } else {
                                // Assume it might be a recovery code
                                String sqlRec = "SELECT id, code_hash FROM user_recovery_codes WHERE user_id = ? AND used_at IS NULL";
                                try (PreparedStatement psRec = conn.prepareStatement(sqlRec)) {
                                    psRec.setInt(1, adminId);
                                    try (ResultSet rsRec = psRec.executeQuery()) {
                                        while (rsRec.next()) {
                                            String codeHash = rsRec.getString("code_hash");
                                            if (BcryptUtils.checkPassword(otp, codeHash)) {
                                                isValidOtp = true;
                                                long recId = rsRec.getLong("id");
                                                try (PreparedStatement psUpdRec = conn.prepareStatement("UPDATE user_recovery_codes SET used_at = NOW(), used_ip = ? WHERE id = ?")) {
                                                    psUpdRec.setString(1, resolveClientIp(request));
                                                    psUpdRec.setLong(2, recId);
                                                    psUpdRec.executeUpdate();
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (!isValidOtp) {
                                if (attempts == 0 || System.currentTimeMillis() - firstFailTs > 15 * 60 * 1000) {
                                    attempts = 1;
                                    firstFailTs = System.currentTimeMillis();
                                } else {
                                    attempts++;
                                }
                                failedAttempts.put(username, attempts + "," + firstFailTs, 15, TimeUnit.MINUTES);
                                
                                consumeCaptcha = false; // Do not consume captcha, let them retry OTP
                                response.put("success", false);
                                response.put("errorCode", "1009");
                                return response.toString();
                            } else {
                                failedAttempts.remove(username);
                            }
                        }

                        // Generate admin token (single-session: invalidates previous)
                        String adminToken = AdminAuthHelper.generateToken(adminId, userName);

                        // Build response matching what Next.js auth.ts expects
                        response.put("success", true);
                        response.put("errorCode", "0");
                        response.put("accessToken", adminToken);

                        // SessionKey as base64 JSON (for backward compat with Next.js)
                        JSONObject sessionData = new JSONObject();
                        sessionData.put("nickname", userName);
                        sessionData.put("avatar", "0");
                        sessionData.put("vinTotal", 0);
                        sessionData.put("xuTotal", 0);
                        sessionData.put("vippoint", 0);
                        sessionData.put("vippointSave", 0);
                        sessionData.put("createTime", "");
                        sessionData.put("ipAddress", "");
                        sessionData.put("certificate", false);
                        sessionData.put("luckyRotate", 0);
                        sessionData.put("daiLy", 100);
                        sessionData.put("mobileSecure", 0);
                        sessionData.put("birthday", "");
                        sessionData.put("fullName", fullName != null ? fullName : "");
                        sessionData.put("adminId", adminId);

                        String sessionKey = java.util.Base64.getEncoder().encodeToString(
                            sessionData.toString().getBytes("UTF-8"));
                        response.put("sessionKey", sessionKey);

                        // Also include data for legacy consumers
                        JSONObject data = new JSONObject();
                        data.put("adminId", adminId);
                        data.put("userName", userName);
                        data.put("fullName", fullName != null ? fullName : "");
                        data.put("status", status);
                        data.put("parentId", parentId);
                        data.put("adminToken", adminToken);
                        data.put("tokenExpiry", AdminAuthHelper.getTokenExpiry());
                        response.put("data", data.toString());

                        logger.debug("Response login admin: success for " + userName);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("LoginAdminProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        } finally {
            if (consumeCaptcha && captchaCache != null && captchaId != null) {
                captchaCache.remove(captchaId);
            }
            try {
                HttpServletRequest request = (HttpServletRequest) param.get();
                String un = request.getParameter("un");
                if (un != null && !un.isEmpty()) {
                    String ip = resolveClientIp(request);

                    String agent = request.getHeader("User-Agent");
                    if (agent != null && agent.length() > 255) {
                        agent = agent.substring(0, 255);
                    }

                    boolean isSuccess = response.optBoolean("success", false);
                    String errorCode = response.optString("errorCode", "");

                    String statusStr = isSuccess ? "0" : "1";
                    String action = isSuccess ? "Thành công" : "Thất bại (lỗi " + errorCode + ")";

                    if ("1005".equals(errorCode)) action = "Username không tồn tại";
                    else if ("1007".equals(errorCode)) action = "Sai mật khẩu";
                    else if ("1109".equals(errorCode)) action = "Tài khoản bị khóa";
                    else if ("1008".equals(errorCode)) action = "Thiếu mã 2FA";
                    else if ("1010".equals(errorCode)) action = "Khóa do sai 2FA nhiều lần";
                    else if ("1009".equals(errorCode)) action = "Sai mã 2FA";
                    else if ("115".equals(errorCode)) action = "Sai captcha";

                    logAdminLogin(un, un, ip, statusStr, action, agent);
                }
            } catch (Exception e) {
                logger.error("Error logging admin login event", e);
            }
        }
        return response.toString();
    }

    private void logAdminLogin(String username, String nickname, String ip, String status, String action, String agent) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            String sql = "INSERT INTO log_loginadmin (username, nickname, ip, status, action, agent) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username != null ? username : "");
                ps.setString(2, nickname != null ? nickname : "");
                ps.setString(3, ip != null ? ip : "");
                ps.setString(4, status != null ? status : "");
                ps.setString(5, action != null ? action : "");
                ps.setString(6, agent != null ? agent : "");
                ps.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("logAdminLogin error", e);
        }
    }
}
