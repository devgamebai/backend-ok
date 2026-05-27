package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * c=2000 - Quick Register: check username existence or create a new user.
 * Check mode: ?c=2000&un={username}
 * Register mode: ?c=2000&un={username}&secret={password}&act=register
 */
public class QuickRegisterProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String username = request.getParameter("un");
            String action = request.getParameter("act");
            String secret = request.getParameter("secret");

            if (username == null || username.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Sanitize username: lowercase, trim
            username = username.trim().toLowerCase();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                if (action != null && !action.isEmpty() && "register".equalsIgnoreCase(action)) {
                    // Register mode
                    if (secret == null || secret.isEmpty()) {
                        response.put("success", false);
                        response.put("errorCode", "1001");
                        response.put("message", "secret is required for registration");
                        return response.toString();
                    }

                    // Check if username already exists
                    String checkSql = "SELECT COUNT(*) AS cnt FROM users WHERE user_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                        ps.setString(1, username);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt("cnt") > 0) {
                                response.put("success", false);
                                response.put("errorCode", "10001");
                                response.put("message", "Username already exists");
                                return response.toString();
                            }
                        }
                    }

                    // Generate nick_name from username
                    String nickName = username;

                    // Check if nick_name already taken
                    String checkNickSql = "SELECT COUNT(*) AS cnt FROM users WHERE nick_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkNickSql)) {
                        ps.setString(1, nickName);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt("cnt") > 0) {
                                // Append random suffix
                                nickName = username + ((int)(Math.random() * 9000) + 1000);
                            }
                        }
                    }

                    // Insert new user - password stored as MD5
                    String insertSql = "INSERT INTO users (user_name, nick_name, password, vin, status, create_time, is_bot) " + // SUN-13xx: xu dropped from users table
                            "VALUES (?, ?, MD5(?), 0, 0, NOW(), 0)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, username);
                        ps.setString(2, nickName);
                        ps.setString(3, secret);
                        int rows = ps.executeUpdate();
                        if (rows > 0) {
                            long userId = 0;
                            try (ResultSet keys = ps.getGeneratedKeys()) {
                                if (keys.next()) {
                                    userId = keys.getLong(1);
                                }
                            }
                            JSONObject data = new JSONObject();
                            data.put("userId", userId);
                            data.put("username", username);
                            data.put("nickName", nickName);
                            response.put("success", true);
                            response.put("errorCode", "0");
                            response.put("data", data);
                            logger.info("Quick registered user: " + username + " (nick: " + nickName + ", id: " + userId + ")");

                            // === AUTO-ASSIGN → Company Agent (code='1') ===
                            if (userId > 0) {
                                try {
                                    int companyAgentId = 0;
                                    try (PreparedStatement caPs = conn.prepareStatement(
                                            "SELECT id FROM vinplay_admin.useragent WHERE code = '1' LIMIT 1")) {
                                        try (ResultSet caRs = caPs.executeQuery()) {
                                            if (caRs.next()) companyAgentId = caRs.getInt(1);
                                        }
                                    }
                                    if (companyAgentId > 0) {
                                        try (PreparedStatement upPs = conn.prepareStatement(
                                                "UPDATE users SET parent_agent_id = ?, referral_code = '1' WHERE id = ?")) {
                                            upPs.setInt(1, companyAgentId);
                                            upPs.setLong(2, userId);
                                            upPs.executeUpdate();
                                        }
                                        logger.info("Admin QuickRegister: assigned user " + username + " to CompanyAgent (id=" + companyAgentId + ")");
                                    } else {
                                        logger.warn("Admin QuickRegister: CompanyAgent (code=1) not found! User " + username + " is orphaned.");
                                    }
                                } catch (Exception agentEx) {
                                    logger.error("Admin QuickRegister: failed to assign agent for user=" + username, agentEx);
                                }
                            }
                        } else {
                            response.put("success", false);
                            response.put("errorCode", "5");
                        }
                    }
                } else {
                    // Check mode - just check if username exists
                    String checkSql = "SELECT COUNT(*) AS cnt FROM users WHERE user_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                        ps.setString(1, username);
                        try (ResultSet rs = ps.executeQuery()) {
                            boolean exists = false;
                            if (rs.next()) {
                                exists = rs.getInt("cnt") > 0;
                            }
                            response.put("success", true);
                            response.put("errorCode", "0");
                            response.put("exists", exists);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("QuickRegisterProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
