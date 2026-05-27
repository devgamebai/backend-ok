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
 * c=9710 - Update a permission. Only super_admin can access.
 */
public class UpdatePermissionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement psCheckDup = null;
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

            String permissionKey = cleanKey(request.getParameter("permission_key"));
            String moduleKey = cleanKey(request.getParameter("module_key"));
            String moduleName = cleanText(request.getParameter("module_name"));
            String featureKey = cleanKey(request.getParameter("feature_key"));
            String featureName = cleanText(request.getParameter("feature_name"));
            String action = cleanKey(request.getParameter("action"));
            String permissionName = cleanText(request.getParameter("permission_name"));
            String description = request.getParameter("description");
            String statusStr = request.getParameter("status");
            Integer parsedStatus = null;
            if (statusStr != null && !statusStr.trim().isEmpty()) {
                try {
                    parsedStatus = Integer.parseInt(statusStr.trim());
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("errorCode", "4001");
                    response.put("message", "status must be an integer (0 or 1)");
                    return response.toString();
                }
                if (parsedStatus != 0 && parsedStatus != 1) {
                    response.put("success", false);
                    response.put("errorCode", "4001");
                    response.put("message", "status must be 0 or 1");
                    return response.toString();
                }
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
            before.put("permission_name", safeString(rs.getString("permission_name")));
            before.put("module", safeString(rs.getString("module")));
            before.put("module_key", safeString(rs.getString("module_key")));
            before.put("module_name", safeString(rs.getString("module_name")));
            before.put("feature_key", safeString(rs.getString("feature_key")));
            before.put("feature_name", safeString(rs.getString("feature_name")));
            before.put("action", safeString(rs.getString("action")));
            before.put("description", safeString(rs.getString("description")));
            before.put("status", rs.getInt("status"));

            rs.close();
            rs = null;
            ps.close();
            ps = null;

            if (!permissionKey.isEmpty()) {
                psCheckDup = conn.prepareStatement("SELECT id FROM admin_permissions WHERE permission_key = ? AND id <> ?");
                psCheckDup.setString(1, permissionKey);
                psCheckDup.setInt(2, permissionId);
                rs = psCheckDup.executeQuery();
                if (rs.next()) {
                    conn.rollback();
                    response.put("success", false);
                    response.put("errorCode", "4002");
                    response.put("message", "permission_key already exists");
                    return response.toString();
                }
                rs.close();
                rs = null;
            }

            StringBuilder sql = new StringBuilder("UPDATE admin_permissions SET updated_at = NOW()");
            boolean hasChange = false;
            if (!permissionKey.isEmpty()) {
                sql.append(", permission_key = ?");
                hasChange = true;
            }
            if (!permissionName.isEmpty()) {
                sql.append(", permission_name = ?");
                hasChange = true;
            }
            if (!moduleKey.isEmpty()) {
                sql.append(", module_key = ?");
                hasChange = true;
            }
            if (!moduleName.isEmpty()) {
                sql.append(", module_name = ?, module = ?");
                hasChange = true;
            }
            if (!featureKey.isEmpty()) {
                sql.append(", feature_key = ?");
                hasChange = true;
            }
            if (!featureName.isEmpty()) {
                sql.append(", feature_name = ?");
                hasChange = true;
            }
            if (!action.isEmpty()) {
                sql.append(", action = ?");
                hasChange = true;
            }
            if (description != null) {
                sql.append(", description = ?");
                hasChange = true;
            }
            if (parsedStatus != null) {
                sql.append(", status = ?");
                hasChange = true;
            }
            if (!hasChange) {
                conn.rollback();
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "No fields to update");
                return response.toString();
            }
            sql.append(" WHERE id = ?");

            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (!permissionKey.isEmpty()) ps.setString(idx++, permissionKey);
            if (!permissionName.isEmpty()) ps.setString(idx++, permissionName);
            if (!moduleKey.isEmpty()) ps.setString(idx++, moduleKey);
            if (!moduleName.isEmpty()) {
                ps.setString(idx++, moduleName);
                ps.setString(idx++, moduleName);
            }
            if (!featureKey.isEmpty()) ps.setString(idx++, featureKey);
            if (!featureName.isEmpty()) ps.setString(idx++, featureName);
            if (!action.isEmpty()) ps.setString(idx++, action);
            if (description != null) ps.setString(idx++, description.trim());
            if (parsedStatus != null) ps.setInt(idx++, parsedStatus);
            ps.setInt(idx, permissionId);
            ps.executeUpdate();
            ps.close();
            ps = null;

            ps = conn.prepareStatement(
                    "SELECT id, permission_key, permission_name, module, module_key, module_name, feature_key, feature_name, action, description, status " +
                            "FROM admin_permissions WHERE id = ?");
            ps.setInt(1, permissionId);
            rs = ps.executeQuery();
            JSONObject after = new JSONObject();
            if (rs.next()) {
                after.put("id", rs.getInt("id"));
                after.put("permission_key", rs.getString("permission_key"));
                after.put("permission_name", safeString(rs.getString("permission_name")));
                after.put("module", safeString(rs.getString("module")));
                after.put("module_key", safeString(rs.getString("module_key")));
                after.put("module_name", safeString(rs.getString("module_name")));
                after.put("feature_key", safeString(rs.getString("feature_key")));
                after.put("feature_name", safeString(rs.getString("feature_name")));
                after.put("action", safeString(rs.getString("action")));
                after.put("description", safeString(rs.getString("description")));
                after.put("status", rs.getInt("status"));
            }

            RbacSupport.insertAuditLog(conn, adminNickname, "UPDATE_PERMISSION", "permission",
                    String.valueOf(permissionId), before.toString(), after.toString(), RbacSupport.getRequestIp(request));

            conn.commit();

            response.put("success", true);
            response.put("data", after);
        } catch (Exception e) {
            if (conn != null) try {
                conn.rollback();
            } catch (Exception ignored) {}
            logger.error("UpdatePermissionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (Exception ignored) {}
            if (ps != null) try {
                ps.close();
            } catch (Exception ignored) {}
            if (psCheckDup != null) try {
                psCheckDup.close();
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

    private String cleanKey(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private String cleanText(String value) {
        if (value == null) return "";
        return value.trim();
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }
}
