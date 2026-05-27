package com.vinplay.api.backend.processors.agent;

import com.google.gson.JsonObject;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.apache.log4j.Logger;

public class DeleteUserAgentProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> parameter) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", "1001");

        HttpServletRequest request = (HttpServletRequest) parameter.get();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.isEmpty()) {
                result.addProperty("errorCode", "1002");
                return result.toString();
            }

            int id = Integer.parseInt(idStr);

            conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL);
            if (conn == null) {
                logger.error("DeleteUserAgentProcessor: cannot get DB connection");
                return result.toString();
            }

            // Block deletion if agent has child agents (must reassign/delete children first)
            try (PreparedStatement checkPs = conn.prepareStatement(
                    "SELECT COUNT(*) FROM useragent WHERE FIND_IN_SET(?, ancestors) > 0")) {
                checkPs.setInt(1, id);
                try (java.sql.ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        result.addProperty("errorCode", "4010");
                        result.addProperty("message", "agent has " + rs.getInt(1) + " child agent(s) — reassign or delete them first");
                        return result.toString();
                    }
                }
            }

            // Get agent's nickname + username before deletion (for dai_ly sync)
            String agentNick = null, agentUsername = null;
            try (PreparedStatement nickPs = conn.prepareStatement(
                    "SELECT nickname, username, code FROM useragent WHERE id=?")) {
                nickPs.setInt(1, id);
                try (java.sql.ResultSet rs = nickPs.executeQuery()) {
                    if (rs.next()) {
                        agentNick = rs.getString("nickname");
                        agentUsername = rs.getString("username");
                    }
                }
            }

            // SUN-924/926: soft-delete (active=0) instead of hard-delete so
            // the row remains for audit trail + commission history integrity.
            // ListUserAgentProcessor filters on active=1 so soft-deleted
            // agents are hidden from the admin list.
            String sql = "UPDATE useragent SET active = 0 WHERE id = ? AND active = 1";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                // Sync: reset users.dai_ly=0 for the demoted agent
                try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    // By username match
                    if (agentUsername != null) {
                        try (PreparedStatement syncPs = userConn.prepareStatement(
                                "UPDATE users SET dai_ly=0 WHERE user_name=? AND dai_ly > 0")) {
                            syncPs.setString(1, agentUsername);
                            syncPs.executeUpdate();
                        }
                    }
                    // Reassign orphaned players (parent_agent_id=deleted agent) to the deleted agent's parent
                    try (PreparedStatement reassignPs = userConn.prepareStatement(
                            "UPDATE users SET parent_agent_id=NULL WHERE parent_agent_id=?")) {
                        reassignPs.setInt(1, id);
                        reassignPs.executeUpdate();
                    }
                } catch (Exception syncEx) {
                    logger.warn("DeleteUserAgentProcessor: dai_ly sync failed for agent " + id, syncEx);
                }

                result.addProperty("success", true);
                result.addProperty("errorCode", "0");
            } else {
                result.addProperty("errorCode", "1003");
            }

        } catch (NumberFormatException e) {
            logger.error("DeleteUserAgentProcessor: invalid id", e);
            result.addProperty("errorCode", "1002");
        } catch (Exception e) {
            logger.error("DeleteUserAgentProcessor error", e);
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            ConnectionPool.releaseConnection(conn);
        }

        return result.toString();
    }
}
