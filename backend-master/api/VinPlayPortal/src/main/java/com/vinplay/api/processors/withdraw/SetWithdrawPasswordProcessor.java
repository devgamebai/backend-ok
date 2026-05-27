package com.vinplay.api.processors.withdraw;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=3031: Set withdrawal password (one-time only, 6 digits).
 * Cannot be changed after set.
 */
public class SetWithdrawPasswordProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            // Read from query params OR POST body (JSON / form-encoded)
            java.util.Map<String, String> bodyParams = parseBody(request);
            String password = request.getParameter("password");
            if (password == null) password = bodyParams.get("password");
            if (password == null) password = request.getParameter("pwd");
            if (password == null) password = bodyParams.get("pwd");

            String confirmPassword = request.getParameter("confirm_password");
            if (confirmPassword == null) confirmPassword = bodyParams.get("confirm_password");
            if (confirmPassword == null) confirmPassword = request.getParameter("confirm_pwd");
            if (confirmPassword == null) confirmPassword = bodyParams.get("confirm_pwd");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Validate password: must be exactly 6 digits
            if (password == null || !password.matches("^\\d{6}$")) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "Password must be exactly 6 digits");
                return response.toString();
            }

            // Validate confirm matches
            if (!password.equals(confirmPassword)) {
                response.put("success", false);
                response.put("errorCode", "4002");
                response.put("message", "Password and confirm password do not match");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            String nickname = tokenMap.get(accessToken);
            IMap<String, UserCacheModel> userMap = instance.getMap("users");
            UserCacheModel userCache = userMap.get(nickname);
            long userId = -1;
            if (userCache != null) {
                userId = userCache.getId();
            } else {
                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                    ps.setString(1, nickname);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getLong("id");
                    }
                }
            }
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Check if already set (cannot change)
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement checkStm = conn.prepareStatement(
                         "SELECT withdraw_password FROM users WHERE id = ?")) {
                checkStm.setLong(1, userId);
                ResultSet rs = checkStm.executeQuery();
                if (rs.next()) {
                    String existing = rs.getString("withdraw_password");
                    if (existing != null && !existing.isEmpty()) {
                        response.put("success", false);
                        response.put("errorCode", "4009");
                        response.put("message", "Withdrawal password already set and cannot be changed");
                        return response.toString();
                    }
                }
            }

            // Hash and store
            String hashedPassword = hashPassword(password);
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement stm = conn.prepareStatement(
                         "UPDATE users SET withdraw_password = ? WHERE id = ? AND (withdraw_password IS NULL OR withdraw_password = '')")) {
                stm.setString(1, hashedPassword);
                stm.setLong(2, userId);
                int rows = stm.executeUpdate();
                if (rows == 0) {
                    response.put("success", false);
                    response.put("errorCode", "4003");
                    response.put("message", "Withdrawal password already set");
                    return response.toString();
                }
            }

            response.put("success", true);
        } catch (Exception e) {
            logger.error("SetWithdrawPasswordProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private java.util.Map<String, String> parseBody(HttpServletRequest request) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        try {
            String contentType = request.getContentType();
            if (contentType == null) return params;
            StringBuilder sb = new StringBuilder();
            java.io.BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String body = sb.toString().trim();
            if (body.isEmpty()) return params;
            if (body.startsWith("{")) {
                org.json.JSONObject json = new org.json.JSONObject(body);
                for (String key : json.keySet()) params.put(key, json.optString(key, null));
            } else {
                for (String pair : body.split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) params.put(java.net.URLDecoder.decode(kv[0], "UTF-8"), java.net.URLDecoder.decode(kv[1], "UTF-8"));
                }
            }
        } catch (Exception e) { /* ignore parse errors */ }
        return params;
    }
}
