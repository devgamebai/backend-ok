package com.vinplay.api.backend.processors.abuse;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9766 — Scan users for abuse patterns (spec §8.2).
 *
 * Detects:
 *   1. Self-referral: user.referral_code resolves to useragent whose nickname = user.nick_name
 *      (agent creates a user account under themselves to farm commission on own bets)
 *   2. Multi-account farming: 3+ distinct users sharing same device_fingerprint (via tbl_device_install)
 *   3. Multi-account via IP: 5+ distinct users from same ip_address (via tbl_device_install)
 *
 * Admin triggers manually; flagged users get abuse_flag=1 + abuse_reason populated.
 *
 * Params:
 *   apply = 1 to UPDATE users.abuse_flag (otherwise dry run/preview)
 *   min_devices = threshold for multi-account via device (default 3)
 *   min_ips = threshold for multi-account via IP (default 5)
 *
 * Response: { success, selfReferral: [...], multiDevice: [...], multiIp: [...], flaggedCount }
 *
 * NOTE: Commission calculation (c=9758) SHOULD check abuse_flag to skip flagged users.
 * That wiring is a follow-up — this processor only detects + flags.
 */
public class CheckAbusePatternsProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("errorCode", "1001");

        try {
            HttpServletRequest request = param.get();
            boolean apply = "1".equals(request.getParameter("apply"));
            int minDevices = parseInt(request.getParameter("min_devices"), 3);
            int minIps = parseInt(request.getParameter("min_ips"), 5);

            JSONArray selfRef = detectSelfReferral(apply);
            JSONArray multiDevice = detectMultiAccountDevice(minDevices, apply);
            JSONArray multiIp = detectMultiAccountIp(minIps, apply);

            int flaggedCount = selfRef.length() + multiDevice.length() + multiIp.length();

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("selfReferral", selfRef);
            response.put("multiDevice", multiDevice);
            response.put("multiIp", multiIp);
            response.put("flaggedCount", flaggedCount);
            response.put("applied", apply);
            response.put("thresholds", new JSONObject().put("minDevices", minDevices).put("minIps", minIps));

            logger.info("AbuseCheck flagged=" + flaggedCount + " applied=" + apply
                    + " selfRef=" + selfRef.length() + " multiDevice=" + multiDevice.length()
                    + " multiIp=" + multiIp.length());
        } catch (Exception e) {
            logger.error("CheckAbusePatternsProcessor error", e);
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private JSONArray detectSelfReferral(boolean apply) throws Exception {
        JSONArray out = new JSONArray();
        // Find users where user.nick_name matches the agent behind their referral_code
        // (i.e. user registered with a code that resolves to an agent with same nickname as the user)
        String sql =
            "SELECT u.id, u.user_name, u.nick_name, u.referral_code " +
            "FROM vinplay.users u " +
            "JOIN vinplay_admin.useragent ua ON ua.code COLLATE utf8mb4_general_ci = u.referral_code COLLATE utf8mb4_general_ci " +
            "WHERE u.nick_name COLLATE utf8mb4_general_ci = ua.nickname COLLATE utf8mb4_general_ci AND u.abuse_flag = 0 LIMIT 200";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject j = new JSONObject();
                j.put("userId", rs.getInt("id"));
                j.put("username", rs.getString("user_name"));
                j.put("nickname", rs.getString("nick_name"));
                j.put("referralCode", rs.getString("referral_code"));
                out.put(j);
                if (apply) flagUser(rs.getInt("id"), "SELF_REFERRAL");
            }
        }
        return out;
    }

    private JSONArray detectMultiAccountDevice(int threshold, boolean apply) throws Exception {
        JSONArray out = new JSONArray();
        String sql =
            "SELECT device_fingerprint, COUNT(DISTINCT nick_name) as cnt, GROUP_CONCAT(DISTINCT nick_name SEPARATOR ',') as nicks " +
            "FROM vinplay.tbl_device_install WHERE nick_name IS NOT NULL AND nick_name <> '' " +
            "GROUP BY device_fingerprint HAVING cnt >= ? LIMIT 100";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject j = new JSONObject();
                    j.put("deviceFingerprint", rs.getString("device_fingerprint"));
                    j.put("accountCount", rs.getInt("cnt"));
                    j.put("nicknames", rs.getString("nicks"));
                    out.put(j);
                    if (apply) {
                        flagUsersByNicknames(rs.getString("nicks"), "MULTI_DEVICE");
                    }
                }
            }
        }
        return out;
    }

    private JSONArray detectMultiAccountIp(int threshold, boolean apply) throws Exception {
        JSONArray out = new JSONArray();
        String sql =
            "SELECT ip_address, COUNT(DISTINCT nick_name) as cnt, GROUP_CONCAT(DISTINCT nick_name SEPARATOR ',') as nicks " +
            "FROM vinplay.tbl_device_install WHERE ip_address IS NOT NULL AND nick_name IS NOT NULL AND nick_name <> '' " +
            "GROUP BY ip_address HAVING cnt >= ? LIMIT 100";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject j = new JSONObject();
                    j.put("ipAddress", rs.getString("ip_address"));
                    j.put("accountCount", rs.getInt("cnt"));
                    j.put("nicknames", rs.getString("nicks"));
                    out.put(j);
                    if (apply) {
                        flagUsersByNicknames(rs.getString("nicks"), "MULTI_IP");
                    }
                }
            }
        }
        return out;
    }

    private void flagUser(int userId, String reason) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE vinplay.users SET abuse_flag=1, abuse_reason=?, abuse_flagged_at=NOW() WHERE id=? AND abuse_flag=0")) {
            ps.setString(1, reason);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void flagUsersByNicknames(String csvNicks, String reason) throws Exception {
        if (csvNicks == null || csvNicks.isEmpty()) return;
        String[] nicks = csvNicks.split(",");
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            String sql = "UPDATE vinplay.users SET abuse_flag=1, abuse_reason=?, abuse_flagged_at=NOW() WHERE nick_name=? AND abuse_flag=0";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (String nick : nicks) {
                    ps.setString(1, reason);
                    ps.setString(2, nick.trim());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private int parseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
