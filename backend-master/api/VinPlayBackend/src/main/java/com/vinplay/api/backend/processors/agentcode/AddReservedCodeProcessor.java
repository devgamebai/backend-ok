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
 * c=9836 — Add a reserved/blacklisted code pattern.
 * Params: aat, code_pattern, reason.
 * Pattern ending with * = prefix match (e.g. SUNKR* blocks SUNKR888, SUNKRVIP, etc.)
 */
public class AddReservedCodeProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String pattern = request.getParameter("code_pattern");
            String reason = request.getParameter("reason");
            if (pattern == null || pattern.trim().isEmpty()) {
                response.put("success", false); response.put("errorCode", "4001"); response.put("message", "code_pattern required");
                return response.toString();
            }
            if (reason == null || reason.trim().isEmpty()) {
                response.put("success", false); response.put("errorCode", "4002"); response.put("message", "reason required");
                return response.toString();
            }
            String normalized = pattern.trim().toUpperCase();
            String actor = ApproveCodeRequestProcessor.resolveAdmin(request);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO agent_code_reserved (code_pattern, reason, created_by) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE reason=VALUES(reason), created_by=VALUES(created_by)")) {
                ps.setString(1, normalized);
                ps.setString(2, reason.trim());
                ps.setString(3, actor);
                ps.executeUpdate();
            }

            response.put("success", true);
            response.put("errorCode", "0");
            JSONObject data = new JSONObject();
            data.put("code_pattern", normalized);
            data.put("reason", reason.trim());
            data.put("created_by", actor);
            response.put("data", data);
        } catch (Exception e) {
            logger.error("AddReservedCodeProcessor error", e);
            response.put("success", false); response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
