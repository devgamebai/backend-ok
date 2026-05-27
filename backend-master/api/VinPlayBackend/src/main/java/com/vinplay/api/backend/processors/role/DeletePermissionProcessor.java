package com.vinplay.api.backend.processors.role;

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
 * c=9711 - Soft delete a permission (status=0). Only super_admin can access.
 */
public class DeletePermissionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            HttpServletRequest request = param.get();
            String adminNickname = RbacSupport.getAdminNicknameFromToken(request, response);
            if (adminNickname == null) {
                return response.toString();
            }
            if (!RbacSupport.canManagePermissions(adminNickname)) {
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", "Permission denied. You do not have permission to manage permissions.");
                return response.toString();
            }

            String permissionIdStr = request.getParameter("permission_id");
            if (permissionIdStr == null || permissionIdStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "permission_id is required");
                return response.toString();
            }
            int permissionId;
            try {
                permissionId = Integer.parseInt(permissionIdStr.trim());
            } catch (Exception e) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "permission_id must be a valid integer");
                return response.toString();
            }

            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(
                    "SELECT id, permission_key, permission_name, module, module_key, module_name, feature_key, feature_name, action, description, status " +
                            "FROM admin_permissions WHERE id = ?");
            ps.setInt(1, permissionId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                response.put("success", false);
                response.put("errorCode", "4004");
                response.put("message", "Permission not found");
                return response.toString();
            }

            JSONObject before = new JSONObject();
            before.put("id", rs.getInt("id"));
            before.put("permission_key", rs.getString("permission_key"));
            before.put("status", rs.getInt("status"));
            before.put("module_name", safeString(rs.getString("module_name")));
            before.put("feature_name", safeString(rs.getString("feature_name")));
            before.put("action", safeString(rs.getString("action")));
            rs.close();
            rs = null;
            ps.close();
            ps = null;

            ps = conn.prepareStatement("UPDATE admin_permissions SET status = 0, updated_at = NOW() WHERE id = ?");
            ps.setInt(1, permissionId);
            ps.executeUpdate();
            ps.close();
            ps = null;

            JSONObject after = new JSONObject(before.toString());
            after.put("status", 0);

            RbacSupport.insertAuditLog(conn, adminNickname, "DELETE_PERMISSION", "permission",
                    String.valueOf(permissionId), before.toString(), after.toString(), RbacSupport.getRequestIp(request));

            conn.commit();

            response.put("success", true);
            response.put("data", after);
        } catch (Exception e) {
            if (conn != null) try {
                conn.rollback();
            } catch (Exception ignored) {}
            logger.error("DeletePermissionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (Exception ignored) {}
            if (ps != null) try {
                ps.close();
            } catch (Exception ignored) {}
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (Exception ignored) {}
                try {
                    conn.close();
                } catch (Exception ignored) {}
            }
        }
        return response.toString();
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }
}
