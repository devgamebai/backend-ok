package com.vinplay.api.backend.processors.role;

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
 * c=9712 - List RBAC audit logs. Only super_admin can access.
 */
public class ListRbacAuditLogsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement psCount = null;
        ResultSet rs = null;
        ResultSet rsCount = null;
        try {
            HttpServletRequest request = param.get();
            String adminNickname = RbacSupport.getAdminNicknameFromToken(request, response);
            if (adminNickname == null) {
                return response.toString();
            }
            if (!RbacSupport.canManagePermissions(adminNickname)) {
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", "Permission denied. You do not have permission to view RBAC audit logs.");
                return response.toString();
            }

            int page = parseInt(request.getParameter("page"), 1);
            int limit = parseInt(request.getParameter("limit"), 20);
            if (page < 1) page = 1;
            if (limit < 1) limit = 20;
            if (limit > 200) limit = 200;

            String actionType = trim(request.getParameter("action_type"));
            String targetType = trim(request.getParameter("target_type"));
            String actorAdmin = trim(request.getParameter("actor_admin"));

            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            if (!actionType.isEmpty()) where.append(" AND action_type = ? ");
            if (!targetType.isEmpty()) where.append(" AND target_type = ? ");
            if (!actorAdmin.isEmpty()) where.append(" AND actor_admin = ? ");

            int offset = (page - 1) * limit;

            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");

            psCount = conn.prepareStatement("SELECT COUNT(*) AS total FROM admin_rbac_audit_logs" + where.toString());
            int idxCount = 1;
            if (!actionType.isEmpty()) psCount.setString(idxCount++, actionType);
            if (!targetType.isEmpty()) psCount.setString(idxCount++, targetType);
            if (!actorAdmin.isEmpty()) psCount.setString(idxCount, actorAdmin);
            rsCount = psCount.executeQuery();
            int total = 0;
            if (rsCount.next()) total = rsCount.getInt("total");

            ps = conn.prepareStatement(
                    "SELECT id, actor_admin, action_type, target_type, target_id, before_json, after_json, ip_address, created_at " +
                            "FROM admin_rbac_audit_logs " + where.toString() +
                            "ORDER BY id DESC LIMIT ? OFFSET ?");
            int idx = 1;
            if (!actionType.isEmpty()) ps.setString(idx++, actionType);
            if (!targetType.isEmpty()) ps.setString(idx++, targetType);
            if (!actorAdmin.isEmpty()) ps.setString(idx++, actorAdmin);
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            rs = ps.executeQuery();

            JSONArray items = new JSONArray();
            while (rs.next()) {
                JSONObject row = new JSONObject();
                row.put("id", rs.getInt("id"));
                row.put("actor_admin", safe(rs.getString("actor_admin")));
                row.put("action_type", safe(rs.getString("action_type")));
                row.put("target_type", safe(rs.getString("target_type")));
                row.put("target_id", safe(rs.getString("target_id")));
                row.put("before_json", safe(rs.getString("before_json")));
                row.put("after_json", safe(rs.getString("after_json")));
                row.put("ip_address", safe(rs.getString("ip_address")));
                row.put("created_at", safe(rs.getString("created_at")));
                items.put(row);
            }

            JSONObject data = new JSONObject();
            data.put("items", items);
            data.put("page", page);
            data.put("limit", limit);
            data.put("total", total);
            response.put("success", true);
            response.put("data", data);
        } catch (Exception e) {
            logger.error("ListRbacAuditLogsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (Exception ignored) {}
            if (rsCount != null) try {
                rsCount.close();
            } catch (Exception ignored) {}
            if (ps != null) try {
                ps.close();
            } catch (Exception ignored) {}
            if (psCount != null) try {
                psCount.close();
            } catch (Exception ignored) {}
            if (conn != null) try {
                conn.close();
            } catch (Exception ignored) {}
        }
        return response.toString();
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
