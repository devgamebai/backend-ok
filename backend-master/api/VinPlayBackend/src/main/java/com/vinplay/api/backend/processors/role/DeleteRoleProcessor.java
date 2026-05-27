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
 * c=9716 - Delete a role.
 * Soft delete role metadata and remove all role bindings/permissions.
 * Params: role_id
 */
public class DeleteRoleProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String adminNickname = RbacSupport.getAdminNicknameFromToken(request, response);
            if (adminNickname == null) {
                return response.toString();
            }
            if (!RbacSupport.canManageRoles(adminNickname)) {
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", "Permission denied. You do not have permission to delete roles.");
                return response.toString();
            }

            String roleIdStr = request.getParameter("role_id");
            if (roleIdStr == null || roleIdStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "role_id is required");
                return response.toString();
            }

            int roleId;
            try {
                roleId = Integer.parseInt(roleIdStr.trim());
            } catch (Exception e) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "role_id must be a valid integer");
                return response.toString();
            }

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                conn.setAutoCommit(false);

                JSONObject before = new JSONObject();
                JSONObject after = new JSONObject();
                JSONArray beforePermissions = new JSONArray();

                ps = conn.prepareStatement("SELECT id, name, description, is_default, status FROM admin_roles WHERE id = ?");
                ps.setInt(1, roleId);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    response.put("success", false);
                    response.put("errorCode", "4004");
                    response.put("message", "Role not found");
                    return response.toString();
                }

                String roleName = rs.getString("name");
                String roleDescription = rs.getString("description");
                int isDefault = rs.getInt("is_default");
                int currentStatus = rs.getInt("status");
                rs.close();
                ps.close();

                if (isDefault == 1) {
                    conn.rollback();
                    response.put("success", false);
                    response.put("errorCode", "4005");
                    response.put("message", "Cannot delete default role");
                    return response.toString();
                }

                ps = conn.prepareStatement(
                        "SELECT ap.permission_key FROM role_permissions rp " +
                                "JOIN admin_permissions ap ON ap.id = rp.permission_id " +
                                "WHERE rp.role_id = ? ORDER BY ap.permission_key");
                ps.setInt(1, roleId);
                rs = ps.executeQuery();
                while (rs.next()) {
                    beforePermissions.put(rs.getString("permission_key"));
                }
                rs.close();
                ps.close();

                int assignedAdmins = 0;
                ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM admin_user_roles WHERE role_id = ?");
                ps.setInt(1, roleId);
                rs = ps.executeQuery();
                if (rs.next()) {
                    assignedAdmins = rs.getInt("total");
                }
                rs.close();
                ps.close();

                before.put("role_id", roleId);
                before.put("name", roleName != null ? roleName : "");
                before.put("description", roleDescription != null ? roleDescription : "");
                before.put("status", currentStatus);
                before.put("assigned_admins", assignedAdmins);
                before.put("permissions", beforePermissions);

                ps = conn.prepareStatement("DELETE FROM admin_user_roles WHERE role_id = ?");
                ps.setInt(1, roleId);
                int adminBindingsRemoved = ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("DELETE FROM role_permissions WHERE role_id = ?");
                ps.setInt(1, roleId);
                int permissionBindingsRemoved = ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("UPDATE admin_roles SET status = 0, updated_at = NOW() WHERE id = ?");
                ps.setInt(1, roleId);
                ps.executeUpdate();
                ps.close();

                after.put("role_id", roleId);
                after.put("name", roleName != null ? roleName : "");
                after.put("description", roleDescription != null ? roleDescription : "");
                after.put("status", 0);
                after.put("admin_bindings_removed", adminBindingsRemoved);
                after.put("permission_bindings_removed", permissionBindingsRemoved);

                RbacSupport.insertAuditLog(conn, adminNickname, "DELETE_ROLE", "role",
                        String.valueOf(roleId), before.toString(), after.toString(), RbacSupport.getRequestIp(request));

                conn.commit();

                response.put("success", true);
                response.put("data", after);
            } catch (Exception ex) {
                if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
                throw ex;
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) {
                    try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                    try { conn.close(); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            logger.error("DeleteRoleProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
