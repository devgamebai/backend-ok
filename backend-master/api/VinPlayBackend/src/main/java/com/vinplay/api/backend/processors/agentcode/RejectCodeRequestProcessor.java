package com.vinplay.api.backend.processors.agentcode;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9834 — Admin rejects an agent code request.
 * Params: aat, request_id, reject_reason.
 */
public class RejectCodeRequestProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String idStr = request.getParameter("request_id");
            String reason = request.getParameter("reject_reason");
            if (idStr == null || idStr.isEmpty()) return err(response, "4001", "request_id required");
            if (reason == null || reason.trim().isEmpty()) return err(response, "4002", "reject_reason required");
            long requestId;
            try { requestId = Long.parseLong(idStr); }
            catch (NumberFormatException e) { return err(response, "4003", "request_id not numeric"); }
            String actor = ApproveCodeRequestProcessor.resolveAdmin(request);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                String currentStatus = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status FROM agent_code_request WHERE id=?")) {
                    ps.setLong(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return err(response, "1002", "request not found");
                        currentStatus = rs.getString(1);
                    }
                }
                if (!"PENDING".equals(currentStatus)) {
                    return err(response, "4004", "request not PENDING (current: " + currentStatus + ")");
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE agent_code_request SET status='REJECTED', reviewed_by=?, reviewed_at=NOW(), reject_reason=? WHERE id=?")) {
                    ps.setString(1, actor);
                    ps.setString(2, reason.trim());
                    ps.setLong(3, requestId);
                    ps.executeUpdate();
                }
            }

            JSONObject data = new JSONObject();
            data.put("request_id", requestId);
            data.put("status", "REJECTED");
            data.put("reviewed_by", actor);
            data.put("reject_reason", reason.trim());
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("RejectCodeRequestProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
