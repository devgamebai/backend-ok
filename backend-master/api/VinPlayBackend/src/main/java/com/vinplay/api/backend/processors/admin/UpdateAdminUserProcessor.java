package com.vinplay.api.backend.processors.admin;

import com.vinplay.api.backend.processors.role.RbacSupport;
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
 * c=9822 — Update an existing admin user.
 * Updates vinplay_admin.user fields + replaces single role in admin_user_roles.
 * Only super_admin can update. Enforces single-role-per-admin design.
 */
public class UpdateAdminUserProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        Connection conn = null;
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            // Auth: only super_admin can update
            String actorNickname = RbacSupport.getAdminNicknameFromToken(request, response);
            if (actorNickname == null) return response.toString();
            if (!RbacSupport.hasSuperAdminRole(actorNickname)) {
                response.put("success", false);
                response.put("errorCode", "403");
                response.put("message", "Permission denied");
                return response.toString();
            }

            // Input
            String aidStr = request.getParameter("aid");
            String fullName = request.getParameter("fn");
            String status = request.getParameter("status");
            if (status == null || status.trim().isEmpty()) {
                status = request.getParameter("st");
            }
            String password = request.getParameter("pw");
            String roleIdStr = request.getParameter("role_id"); // single role ID

            if (aidStr == null || aidStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "aid is required");
                return response.toString();
            }

            int aid = Integer.parseInt(aidStr);

            // Validate role_id if provided
            int roleId = -1;
            if (roleIdStr != null && !roleIdStr.trim().isEmpty()) {
                try {
                    roleId = Integer.parseInt(roleIdStr.trim());
                } catch (NumberFormatException e) {
                    response.put("success", false);
                    response.put("errorCode", "1001");
                    response.put("message", "role_id must be a valid integer");
                    return response.toString();
                }
            }

            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
            conn.setAutoCommit(false);

            // Check admin exists and get current data for audit
            JSONObject beforeJson = new JSONObject();
            String checkSql = "SELECT ID, UserName, FullName, Status FROM vinplay_admin.user WHERE ID = ?";
            try (PreparedStatement cps = conn.prepareStatement(checkSql)) {
                cps.setInt(1, aid);
                try (ResultSet crs = cps.executeQuery()) {
                    if (!crs.next()) {
                        response.put("success", false);
                        response.put("errorCode", "1005");
                        response.put("message", "Admin user not found");
                        return response.toString();
                    }
                    beforeJson.put("userName", crs.getString("UserName"));
                    beforeJson.put("fullName", crs.getString("FullName"));
                    beforeJson.put("status", crs.getString("Status"));

                    // Prevent deactivating own super_admin account
                    String targetUserName = crs.getString("UserName");
                    if (targetUserName != null && targetUserName.equals(actorNickname)) {
                        if (status != null && !"A".equalsIgnoreCase(status.trim())) {
                            response.put("success", false);
                            response.put("errorCode", "1010");
                            response.put("message", "Cannot deactivate your own account");
                            return response.toString();
                        }
                    }
                }
            }

            // Build dynamic UPDATE
            StringBuilder updateSql = new StringBuilder("UPDATE vinplay_admin.user SET ");
            java.util.List<Object> updateParams = new java.util.ArrayList<>();
            boolean hasUpdate = false;

            if (fullName != null) {
                updateSql.append("FullName = ?");
                updateParams.add(fullName.trim());
                hasUpdate = true;
            }
            if (status != null && !status.trim().isEmpty()) {
                if (hasUpdate) updateSql.append(", ");
                updateSql.append("Status = ?");
                updateParams.add(status.trim());
                hasUpdate = true;
            }
            if (password != null && !password.trim().isEmpty()) {
                if (hasUpdate) updateSql.append(", ");
                updateSql.append("Password = ?");
                updateParams.add(password.trim());
                hasUpdate = true;
            }

            if (hasUpdate) {
                updateSql.append(" WHERE ID = ?");
                updateParams.add(aid);
                try (PreparedStatement ups = conn.prepareStatement(updateSql.toString())) {
                    for (int i = 0; i < updateParams.size(); i++) {
                        ups.setObject(i + 1, updateParams.get(i));
                    }
                    ups.executeUpdate();
                }
            }

            // Update single role: delete old → insert new (one role per admin)
            if (roleIdStr != null) {
                // Get old role for audit
                String oldRoleSql = "SELECT role_id FROM admin_user_roles WHERE admin_id = ?";
                try (PreparedStatement orp = conn.prepareStatement(oldRoleSql)) {
                    orp.setInt(1, aid);
                    try (ResultSet ors = orp.executeQuery()) {
                        if (ors.next()) {
                            beforeJson.put("role_id", ors.getInt("role_id"));
                        }
                    }
                }

                // Delete old role(s)
                String deleteSql = "DELETE FROM admin_user_roles WHERE admin_id = ?";
                try (PreparedStatement dps = conn.prepareStatement(deleteSql)) {
                    dps.setInt(1, aid);
                    dps.executeUpdate();
                }

                // Insert new single role
                if (roleId > 0) {
                    String insertRoleSql = "INSERT INTO admin_user_roles (admin_id, role_id, assigned_by) VALUES (?, ?, ?)";
                    try (PreparedStatement rps = conn.prepareStatement(insertRoleSql)) {
                        rps.setInt(1, aid);
                        rps.setInt(2, roleId);
                        rps.setString(3, actorNickname);
                        rps.executeUpdate();
                    }
                }
            }

            conn.commit();

            // Audit log
            try {
                JSONObject afterJson = new JSONObject();
                if (fullName != null) afterJson.put("fullName", fullName.trim());
                if (status != null) afterJson.put("status", status.trim());
                if (password != null && !password.trim().isEmpty()) afterJson.put("passwordChanged", true);
                if (roleIdStr != null) afterJson.put("role_id", roleId > 0 ? roleId : "removed");
                RbacSupport.insertAuditLog(conn, actorNickname, "UPDATE_ADMIN",
                        "admin_user", String.valueOf(aid), beforeJson.toString(),
                        afterJson.toString(), RbacSupport.getRequestIp(request));
            } catch (Exception auditEx) {
                logger.warn("Audit log failed for UpdateAdminUser", auditEx);
            }

            response.put("success", true);
            response.put("errorCode", "0");

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "1001");
            response.put("message", "Invalid parameter format");
            rollback(conn);
        } catch (Exception e) {
            logger.error("UpdateAdminUserProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            rollback(conn);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
        return response.toString();
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (Exception ignored) {}
        }
    }
}
