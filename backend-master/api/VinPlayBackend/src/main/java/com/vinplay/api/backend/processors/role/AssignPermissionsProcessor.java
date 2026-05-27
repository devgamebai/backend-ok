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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * c=9703 - Assign permissions to a role.
 * Params: role_id, permissions (comma-separated permission_keys)
 * Deletes old permissions, inserts new ones.
 */
public class AssignPermissionsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String adminNickname = RbacSupport.getAdminNicknameFromToken(request, response);
            if (adminNickname == null) {
                return response.toString();
            }
            if (!RbacSupport.canManageRoles(adminNickname)) {
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", "Permission denied. You do not have permission to assign permissions to roles.");
                return response.toString();
            }

            String roleIdStr = request.getParameter("role_id");
            String permissions = request.getParameter("permissions");

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

            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            PreparedStatement psCurrentPerm = null;
            PreparedStatement psAuditRoleName = null;

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                conn.setAutoCommit(false);

                // Check role exists
                ps = conn.prepareStatement("SELECT id FROM admin_roles WHERE id = ?");
                ps.setInt(1, roleId);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    response.put("success", false);
                    response.put("errorCode", "4004");
                    response.put("message", "Role not found");
                    return response.toString();
                }
                rs.close();
                ps.close();

                // Get current permission set before change for audit log
                JSONArray beforePermissions = new JSONArray();
                psCurrentPerm = conn.prepareStatement(
                        "SELECT ap.permission_key FROM role_permissions rp " +
                                "JOIN admin_permissions ap ON ap.id = rp.permission_id " +
                                "WHERE rp.role_id = ? ORDER BY ap.permission_key");
                psCurrentPerm.setInt(1, roleId);
                rs = psCurrentPerm.executeQuery();
                while (rs.next()) {
                    beforePermissions.put(rs.getString("permission_key"));
                }
                rs.close();
                rs = null;
                psCurrentPerm.close();
                psCurrentPerm = null;

                // Validate permission keys BEFORE deleting old permissions
                int insertedCount = 0;
                List<String> validKeys = new ArrayList<String>();
                if (permissions != null && !permissions.trim().isEmpty()) {
                    String[] permKeys = permissions.split(",");
                    Set<String> dedupKeys = new LinkedHashSet<String>();
                    for (String key : permKeys) {
                        String trimmed = key.trim();
                        if (!trimmed.isEmpty()) {
                            dedupKeys.add(trimmed);
                        }
                    }
                    validKeys = new ArrayList<String>(dedupKeys);

                    if (!validKeys.isEmpty()) {
                        StringBuilder checkClause = new StringBuilder();
                        for (int i = 0; i < validKeys.size(); i++) {
                            if (i > 0) checkClause.append(",");
                            checkClause.append("?");
                        }

                        PreparedStatement psCheck = conn.prepareStatement(
                                "SELECT permission_key FROM admin_permissions " +
                                        "WHERE status = 1 AND permission_key IN (" + checkClause.toString() + ")");
                        for (int i = 0; i < validKeys.size(); i++) {
                            psCheck.setString(i + 1, validKeys.get(i));
                        }
                        ResultSet rsCheck = psCheck.executeQuery();
                        Set<String> foundKeys = new LinkedHashSet<String>();
                        while (rsCheck.next()) {
                            foundKeys.add(rsCheck.getString("permission_key"));
                        }
                        rsCheck.close();
                        psCheck.close();

                        JSONArray invalidKeys = new JSONArray();
                        for (String key : validKeys) {
                            if (!foundKeys.contains(key)) {
                                invalidKeys.put(key);
                            }
                        }
                        if (invalidKeys.length() > 0) {
                            conn.rollback();
                            response.put("success", false);
                            response.put("errorCode", "4004");
                            response.put("message", "One or more permission keys are invalid/inactive");
                            response.put("invalid_permissions", invalidKeys);
                            return response.toString();
                        }
                    }
                }

                // Delete old permissions for this role (safe: validation already passed)
                ps = conn.prepareStatement("DELETE FROM role_permissions WHERE role_id = ?");
                ps.setInt(1, roleId);
                ps.executeUpdate();
                ps.close();

                // Insert new permissions
                if (!validKeys.isEmpty()) {
                    StringBuilder inClause = new StringBuilder();
                    for (int i = 0; i < validKeys.size(); i++) {
                        if (i > 0) inClause.append(",");
                        inClause.append("?");
                    }

                    ps = conn.prepareStatement(
                            "INSERT INTO role_permissions (role_id, permission_id, assigned_by) " +
                            "SELECT ?, id, ? FROM admin_permissions WHERE status = 1 AND permission_key IN (" + inClause.toString() + ")");
                    ps.setInt(1, roleId);
                    ps.setString(2, adminNickname);
                    for (int i = 0; i < validKeys.size(); i++) {
                        ps.setString(i + 3, validKeys.get(i));
                    }
                    insertedCount = ps.executeUpdate();
                    ps.close();
                }

                JSONArray afterPermissions = new JSONArray();
                psCurrentPerm = conn.prepareStatement(
                        "SELECT ap.permission_key FROM role_permissions rp " +
                                "JOIN admin_permissions ap ON ap.id = rp.permission_id " +
                                "WHERE rp.role_id = ? ORDER BY ap.permission_key");
                psCurrentPerm.setInt(1, roleId);
                rs = psCurrentPerm.executeQuery();
                while (rs.next()) {
                    afterPermissions.put(rs.getString("permission_key"));
                }
                rs.close();
                rs = null;
                psCurrentPerm.close();
                psCurrentPerm = null;

                String roleName = "";
                psAuditRoleName = conn.prepareStatement("SELECT name FROM admin_roles WHERE id = ?");
                psAuditRoleName.setInt(1, roleId);
                rs = psAuditRoleName.executeQuery();
                if (rs.next()) {
                    roleName = rs.getString("name");
                }
                rs.close();
                rs = null;
                psAuditRoleName.close();
                psAuditRoleName = null;

                JSONObject before = new JSONObject();
                before.put("role_id", roleId);
                before.put("role_name", roleName != null ? roleName : "");
                before.put("permissions", beforePermissions);

                JSONObject after = new JSONObject();
                after.put("role_id", roleId);
                after.put("role_name", roleName != null ? roleName : "");
                after.put("permissions", afterPermissions);
                after.put("permissions_assigned", insertedCount);

                RbacSupport.insertAuditLog(conn, adminNickname, "ASSIGN_ROLE_PERMISSIONS", "role",
                        String.valueOf(roleId), before.toString(), after.toString(), RbacSupport.getRequestIp(request));

                conn.commit();

                JSONObject data = new JSONObject();
                data.put("role_id", roleId);
                data.put("permissions_assigned", insertedCount);

                response.put("success", true);
                response.put("data", data);

            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
                throw e;
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (psCurrentPerm != null) try { psCurrentPerm.close(); } catch (Exception ignored) {}
                if (psAuditRoleName != null) try { psAuditRoleName.close(); } catch (Exception ignored) {}
                if (conn != null) {
                    try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                    try { conn.close(); } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            logger.error("AssignPermissionsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
