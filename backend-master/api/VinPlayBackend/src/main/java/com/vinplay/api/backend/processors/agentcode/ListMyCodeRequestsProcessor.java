package com.vinplay.api.backend.processors.agentcode;

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
 * c=9831 — Agent views their own code request history.
 * Params: rc (agent nickname).
 */
public class ListMyCodeRequestsProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentNick = request.getParameter("rc");
            if (agentNick == null || agentNick.isEmpty()) {
                response.put("success", false); response.put("errorCode", "1001"); response.put("message", "rc required");
                return response.toString();
            }

            // Also return current active code
            String currentCode = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT code FROM useragent WHERE nickname=?")) {
                    ps.setString(1, agentNick);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) currentCode = rs.getString(1); }
                }

                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, desired_code, normalized_code, status, reviewed_by, " +
                        "DATE_FORMAT(reviewed_at,'%Y-%m-%d %H:%i:%s') reviewed_at, " +
                        "reject_reason, DATE_FORMAT(created_at,'%Y-%m-%d %H:%i:%s') created_at " +
                        "FROM agent_code_request WHERE agent_nickname=? ORDER BY created_at DESC LIMIT 20")) {
                    ps.setString(1, agentNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id", rs.getLong("id"));
                            row.put("desired_code", rs.getString("desired_code"));
                            row.put("status", rs.getString("status"));
                            row.put("reviewed_by", rs.getString("reviewed_by") != null ? rs.getString("reviewed_by") : "");
                            row.put("reviewed_at", rs.getString("reviewed_at") != null ? rs.getString("reviewed_at") : "");
                            row.put("reject_reason", rs.getString("reject_reason") != null ? rs.getString("reject_reason") : "");
                            row.put("created_at", rs.getString("created_at"));
                            arr.put(row);
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("current_code", currentCode != null ? currentCode : "");
                response.put("data", arr);
                response.put("total", arr.length());
            }
        } catch (Exception e) {
            logger.error("ListMyCodeRequestsProcessor error", e);
            response.put("success", false); response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
