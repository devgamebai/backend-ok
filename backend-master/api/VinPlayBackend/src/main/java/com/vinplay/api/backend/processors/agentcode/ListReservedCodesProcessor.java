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
 * c=9835 — List all reserved/blacklisted code patterns.
 * Params: aat.
 */
public class ListReservedCodesProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            JSONArray arr = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT code_pattern, reason, created_by, DATE_FORMAT(created_at,'%Y-%m-%d %H:%i:%s') created_at " +
                     "FROM agent_code_reserved ORDER BY code_pattern");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("code_pattern", rs.getString("code_pattern"));
                    row.put("reason", rs.getString("reason"));
                    row.put("created_by", rs.getString("created_by"));
                    row.put("created_at", rs.getString("created_at"));
                    arr.put(row);
                }
            }
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("total", arr.length());
        } catch (Exception e) {
            logger.error("ListReservedCodesProcessor error", e);
            response.put("success", false); response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
