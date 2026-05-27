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
 * c=9709 - Create a permission. Only super_admin can access.
 */
public class CreatePermissionProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement psInsert = null;
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

            String permissionKey = cleanKey(request.getParameter("permission_key"));
            String moduleKey = cleanKey(request.getParameter("module_key"));
            String moduleName = cleanText(request.getParameter("module_name"));
            String featureKey = cleanKey(request.getParameter("feature_key"));
            String featureName = cleanText(request.getParameter("feature_name"));
            String action = cleanKey(request.getParameter("action"));
            String permissionName = cleanText(request.getParameter("permission_name"));
            String description = cleanText(request.getParameter("description"));

            if (permissionKey.isEmpty() || moduleKey.isEmpty() || moduleName.isEmpty()
                    || featureKey.isEmpty() || featureName.isEmpty() || action.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "permission_key/module_key/module_name/feature_key/feature_name/action are required");
                return response.toString();
            }

            if (permissionName.isEmpty()) {
                permissionName = featureName + " - " + action.toUpperCase();
            }

            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
            conn.setAutoCommit(false);

            ps = conn.prepareStatement("SELECT id FROM admin_permissions WHERE permission_key = ?");
            ps.setString(1, permissionKey);
            rs = ps.executeQuery();
            if (rs.next()) {
                conn.rollback();
                response.put("success", false);
                response.put("errorCode", "4002");
                response.put("message", "permission_key already exists");
                return response.toString();
            }
            rs.close();
            rs = null;
            ps.close();
            ps = null;

            psInsert = conn.prepareStatement(
                    "INSERT INTO admin_permissions " +
                            "(permission_key, permission_name, module, module_key, module_name, feature_key, feature_name, action, description, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, NOW(), NOW())",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            psInsert.setString(1, permissionKey);
            psInsert.setString(2, permissionName);
            psInsert.setString(3, moduleName);
            psInsert.setString(4, moduleKey);
            psInsert.setString(5, moduleName);
            psInsert.setString(6, featureKey);
            psInsert.setString(7, featureName);
            psInsert.setString(8, action);
            psInsert.setString(9, description);
            psInsert.executeUpdate();

            int newId = 0;
            rs = psInsert.getGeneratedKeys();
            if (rs.next()) {
                newId = rs.getInt(1);
            }

            JSONObject after = new JSONObject();
            after.put("id", newId);
            after.put("permission_key", permissionKey);
            after.put("module_key", moduleKey);
            after.put("module_name", moduleName);
            after.put("feature_key", featureKey);
            after.put("feature_name", featureName);
            after.put("action", action);
            after.put("permission_name", permissionName);
            after.put("description", description);
            after.put("status", 1);

            RbacSupport.insertAuditLog(conn, adminNickname, "CREATE_PERMISSION", "permission",
                    String.valueOf(newId), "", after.toString(), RbacSupport.getRequestIp(request));

            conn.commit();

            response.put("success", true);
            response.put("data", after);
        } catch (Exception e) {
            if (conn != null) try {
                conn.rollback();
            } catch (Exception ignored) {}
            logger.error("CreatePermissionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (Exception ignored) {}
            if (ps != null) try {
                ps.close();
            } catch (Exception ignored) {}
            if (psInsert != null) try {
                psInsert.close();
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
}
