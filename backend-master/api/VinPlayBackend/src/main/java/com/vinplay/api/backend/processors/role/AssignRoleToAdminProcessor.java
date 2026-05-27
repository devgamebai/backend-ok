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
 * c=9705 - Assign a single role to admin user.
 * Params: admin_id, role_id (single role only — one role per admin).
 * Replaces the admin's current role with the provided role.
 */
public class AssignRoleToAdminProcessor implements BaseProcessor<HttpServletRequest, String> {

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
                response.put("message", "Permission denied. You do not have permission to assign roles to admin users.");
                return response.toString();
            }

            String adminIdStr = request.getParameter("admin_id");
            String roleIdStr = request.getParameter("role_id");

            if (adminIdStr == null || adminIdStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "admin_id is required");
                return response.toString();
            }
            if (roleIdStr == null || roleIdStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "role_id is required (single role per admin)");
                return response.toString();
            }

            int adminId;
            try {
                adminId = Integer.parseInt(adminIdStr);
            } catch (Exception e) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "admin_id must be a valid integer");
                return response.toString();
            }

            // Single role only — one role per admin
            Set<Integer> roleIdSet = new LinkedHashSet<Integer>();
            try {
                roleIdSet.add(Integer.parseInt(roleIdStr.trim()));
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
                JSONArray beforeRoles = new JSONArray();
                JSONArray afterRoles = new JSONArray();

                // Verify admin user exists
                ps = conn.prepareStatement("SELECT ID FROM vinplay_admin.user WHERE ID = ?");
                ps.setInt(1, adminId);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    response.put("success", false);
                    response.put("errorCode", "4004");
                    response.put("message", "Admin user not found");
                    return response.toString();
                }
                rs.close();
                ps.close();

                ps = conn.prepareStatement(
                        "SELECT aur.role_id, ar.name role_name, ar.description role_description, aur.assigned_by, aur.assigned_at " +
                                "FROM admin_user_roles aur " +
                                "LEFT JOIN admin_roles ar ON ar.id = aur.role_id " +
                                "WHERE aur.admin_id = ? " +
                                "ORDER BY ar.id");
                ps.setInt(1, adminId);
                rs = ps.executeQuery();
                while (rs.next()) {
                    JSONObject role = new JSONObject();
                    role.put("role_id", rs.getInt("role_id"));
                    role.put("role_name", rs.getString("role_name") != null ? rs.getString("role_name") : "");
                    role.put("role_description", rs.getString("role_description") != null ? rs.getString("role_description") : "");
                    role.put("assigned_by", rs.getString("assigned_by") != null ? rs.getString("assigned_by") : "");
                    role.put("assigned_at", rs.getString("assigned_at") != null ? rs.getString("assigned_at") : "");
                    beforeRoles.put(role);
                }
                before.put("admin_id", adminId);
                before.put("roles", beforeRoles);
                rs.close();
                ps.close();

                List<Integer> targetRoleIds = new ArrayList<Integer>(roleIdSet);
                if (!targetRoleIds.isEmpty()) {
                    StringBuilder inClause = new StringBuilder();
                    for (int i = 0; i < targetRoleIds.size(); i++) {
                        if (i > 0) {
                            inClause.append(",");
                        }
                        inClause.append("?");
                    }

                    ps = conn.prepareStatement(
                            "SELECT id, name, description FROM admin_roles WHERE status = 1 AND id IN (" + inClause.toString() + ") ORDER BY id");
                    for (int i = 0; i < targetRoleIds.size(); i++) {
                        ps.setInt(i + 1, targetRoleIds.get(i));
                    }
                    rs = ps.executeQuery();

                    Set<Integer> foundRoleIds = new LinkedHashSet<Integer>();
                    while (rs.next()) {
                        int foundRoleId = rs.getInt("id");
                        foundRoleIds.add(foundRoleId);

                        JSONObject role = new JSONObject();
                        role.put("role_id", foundRoleId);
                        role.put("role_name", rs.getString("name") != null ? rs.getString("name") : "");
                        role.put("role_description", rs.getString("description") != null ? rs.getString("description") : "");
                        role.put("assigned_by", adminNickname);
                        afterRoles.put(role);
                    }
                    rs.close();
                    ps.close();

                    JSONArray invalidRoleIds = new JSONArray();
                    for (Integer targetRoleId : targetRoleIds) {
                        if (!foundRoleIds.contains(targetRoleId)) {
                            invalidRoleIds.put(targetRoleId);
                        }
                    }
                    if (invalidRoleIds.length() > 0) {
                        conn.rollback();
                        response.put("success", false);
                        response.put("errorCode", "4004");
                        response.put("message", "One or more roles were not found or inactive");
                        response.put("invalid_role_ids", invalidRoleIds);
                        return response.toString();
                    }
                }

                // Replace the admin's current role set with the requested role set.
                ps = conn.prepareStatement("DELETE FROM admin_user_roles WHERE admin_id = ?");
                ps.setInt(1, adminId);
                ps.executeUpdate();
                ps.close();

                if (!targetRoleIds.isEmpty()) {
                    ps = conn.prepareStatement(
                            "INSERT INTO admin_user_roles (admin_id, role_id, assigned_by) VALUES (?, ?, ?)");
                    for (Integer targetRoleId : targetRoleIds) {
                        ps.setInt(1, adminId);
                        ps.setInt(2, targetRoleId.intValue());
                        ps.setString(3, adminNickname);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    ps.close();
                }

                JSONObject data = new JSONObject();
                data.put("admin_id", adminId);
                data.put("role_ids", new JSONArray(targetRoleIds));
                data.put("roles", afterRoles);
                data.put("assigned_by", adminNickname);

                after.put("admin_id", adminId);
                after.put("roles", afterRoles);
                after.put("assigned_by", adminNickname);

                RbacSupport.insertAuditLog(conn, adminNickname, "ASSIGN_ROLE_TO_ADMIN", "admin_user",
                        String.valueOf(adminId), before.toString(), after.toString(), RbacSupport.getRequestIp(request));

                conn.commit();
                response.put("success", true);
                response.put("data", data);

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
            logger.error("AssignRoleToAdminProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
