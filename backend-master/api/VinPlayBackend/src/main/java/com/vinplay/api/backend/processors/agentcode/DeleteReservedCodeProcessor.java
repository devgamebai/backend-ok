package com.vinplay.api.backend.processors.agentcode;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * c=9837 — Delete a reserved code pattern.
 * Params: aat, code_pattern.
 */
public class DeleteReservedCodeProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String pattern = request.getParameter("code_pattern");
            if (pattern == null || pattern.trim().isEmpty()) {
                response.put("success", false); response.put("errorCode", "4001"); response.put("message", "code_pattern required");
                return response.toString();
            }
            String normalized = pattern.trim().toUpperCase();

            int deleted;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM agent_code_reserved WHERE code_pattern=?")) {
                ps.setString(1, normalized);
                deleted = ps.executeUpdate();
            }

            if (deleted == 0) {
                response.put("success", false); response.put("errorCode", "1002"); response.put("message", "pattern not found");
            } else {
                response.put("success", true); response.put("errorCode", "0");
                response.put("deleted", normalized);
            }
        } catch (Exception e) {
            logger.error("DeleteReservedCodeProcessor error", e);
            response.put("success", false); response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
