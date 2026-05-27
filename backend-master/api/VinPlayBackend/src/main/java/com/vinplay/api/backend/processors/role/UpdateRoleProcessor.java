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
 * c=9702 - Update role name, description, status.
 * Cannot modify default roles' names.
 * Params: role_id, name (optional), description (optional), status (optional)
 */
public class UpdateRoleProcessor implements BaseProcessor<HttpServletRequest, String> {

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
                response.put("message", "Permission denied. You do not have permission to update roles.");
                return response.toString();
            }

            String roleIdStr = request.getParameter("role_id");
            if (roleIdStr == null || roleIdStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "role_id is required");
                return response.toString();
            }
            int roleId;
            try {
                roleId = Integer.parseInt(roleIdStr);
            } catch (Exception e) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "role_id must be a valid integer");
                return response.toString();
            }

            String name = request.getParameter("name");
            String description = request.getParameter("description");
            String statusStr = request.getParameter("status");

            // Validate status before opening transaction
            Integer parsedStatus = null;
            if (statusStr != null && !statusStr.isEmpty()) {
                try {
                    parsedStatus = Integer.parseInt(statusStr);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("errorCode", "4001");
                    response.put("message", "status must be a valid integer");
                    return response.toString();
                }
            }

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                conn.setAutoCommit(false);

                // Check role exists and get current data
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
                String beforeName = rs.getString("name");
                String beforeDescription = rs.getString("description");
                int beforeStatus = rs.getInt("status");
                int isDefault = rs.getInt("is_default");
                rs.close();
                ps.close();

                // Cannot modify default role names
                if (isDefault == 1 && name != null && !name.trim().isEmpty()) {
                    conn.rollback();
                    response.put("success", false);
                    response.put("errorCode", "4005");
                    response.put("message", "Cannot modify default role name");
                    return response.toString();
                }

                if (name != null && !name.trim().isEmpty()) {
                    String normalizedName = name.trim().toLowerCase().replaceAll("\\s+", "_");
                    ps = conn.prepareStatement("SELECT id FROM admin_roles WHERE name = ? AND id <> ?");
                    ps.setString(1, normalizedName);
                    ps.setInt(2, roleId);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        conn.rollback();
                        response.put("success", false);
                        response.put("errorCode", "4002");
                        response.put("message", "Role name already exists");
                        return response.toString();
                    }
                    rs.close();
                    ps.close();
                }

                // Build dynamic update
                StringBuilder sql = new StringBuilder("UPDATE admin_roles SET ");
                boolean hasUpdate = false;

                if (name != null && !name.trim().isEmpty()) {
                    sql.append("name = ?");
                    hasUpdate = true;
                }
                if (description != null) {
                    if (hasUpdate) sql.append(", ");
                    sql.append("description = ?");
                    hasUpdate = true;
                }
                if (statusStr != null && !statusStr.isEmpty()) {
                    if (hasUpdate) sql.append(", ");
                    sql.append("status = ?");
                    hasUpdate = true;
                }

                if (!hasUpdate) {
                    conn.rollback();
                    response.put("success", false);
                    response.put("errorCode", "4001");
                    response.put("message", "No fields to update");
                    return response.toString();
                }

                sql.append(" WHERE id = ?");
                ps = conn.prepareStatement(sql.toString());

                int idx = 1;
                if (name != null && !name.trim().isEmpty()) {
                    ps.setString(idx++, name.trim().toLowerCase().replaceAll("\\s+", "_"));
                }
                if (description != null) {
                    ps.setString(idx++, description.trim());
                }
                if (statusStr != null && !statusStr.isEmpty()) {
                    ps.setInt(idx++, parsedStatus);
                }
                ps.setInt(idx, roleId);
                ps.executeUpdate();

                JSONObject before = new JSONObject();
                before.put("role_id", roleId);
                before.put("name", beforeName != null ? beforeName : "");
                before.put("description", beforeDescription != null ? beforeDescription : "");
                before.put("status", beforeStatus);

                ps.close();
                ps = conn.prepareStatement("SELECT id, name, description, status FROM admin_roles WHERE id = ?");
                ps.setInt(1, roleId);
                rs = ps.executeQuery();
                JSONObject after = new JSONObject();
                if (rs.next()) {
                    after.put("role_id", rs.getInt("id"));
                    after.put("name", rs.getString("name") != null ? rs.getString("name") : "");
                    after.put("description", rs.getString("description") != null ? rs.getString("description") : "");
                    after.put("status", rs.getInt("status"));
                }

                RbacSupport.insertAuditLog(conn, adminNickname, "UPDATE_ROLE", "role",
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
            logger.error("UpdateRoleProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
