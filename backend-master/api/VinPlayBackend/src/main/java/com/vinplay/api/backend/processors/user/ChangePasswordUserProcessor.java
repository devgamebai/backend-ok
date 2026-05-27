package com.vinplay.api.backend.processors.user;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9101 — Change/reset password for a player (from admin CMS).
 * Params: nn (nickname), np (new password, plaintext or MD5), aat (admin token)
 *
 * Updates both users.password AND useragent.password (if player is also an agent).
 */
public class ChangePasswordUserProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String nickname = request.getParameter("nn");
            String newPassword = request.getParameter("np");

            if (nickname == null || nickname.isEmpty()) {
                return err(response, "4001", "Missing nn (nickname)");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                return err(response, "4001", "Missing np (new password)");
            }

            // Auto-detect: if not 32-char hex, MD5 hash it
            String hashedPw = newPassword;
            if (newPassword.length() != 32 || !newPassword.matches("[a-fA-F0-9]+")) {
                hashedPw = md5(newPassword);
            }

            // 1. Update users.password
            int userUpdated = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET password = ? WHERE nick_name = ?");
                ps.setString(1, hashedPw);
                ps.setString(2, nickname);
                userUpdated = ps.executeUpdate();
                ps.close();
            }

            if (userUpdated == 0) {
                return err(response, "1002", "User not found: " + nickname);
            }

            // 2. Also update useragent.password if player is an agent (keep in sync)
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE useragent SET password = ? WHERE nickname = ?");
                ps.setString(1, hashedPw);
                ps.setString(2, nickname);
                ps.executeUpdate();
                ps.close();
            } catch (Exception e) {
                // Not critical — player may not be an agent
                logger.debug("ChangePasswordUserProcessor: useragent update skipped for " + nickname);
            }

            logger.info("ChangePasswordUserProcessor: password changed for " + nickname);

            response.put("success", true);
            response.put("errorCode", "0");

        } catch (Exception e) {
            logger.error("ChangePasswordUserProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 error", e);
        }
    }

    private String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
