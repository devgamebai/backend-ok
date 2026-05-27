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
import java.sql.Statement;

/**
 * c=9821 — Create a new admin user.
 * Inserts into vinplay_admin.user + admin_user_roles (single role per admin).
 * Only super_admin can create new admins.
 */
public class CreateAdminUserProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        Connection conn = null;
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            // Auth: only super_admin can create
            String actorNickname = RbacSupport.getAdminNicknameFromToken(request, response);
            if (actorNickname == null) return response.toString();
            if (!RbacSupport.hasSuperAdminRole(actorNickname)) {
                response.put("success", false);
                response.put("errorCode", "403");
                response.put("message", "Permission denied");
                return response.toString();
            }

            // Input
            String userName = request.getParameter("un");
            String password = request.getParameter("pw");
            String fullName = request.getParameter("fn");
            String status = request.getParameter("status");
            String roleIdStr = request.getParameter("role_id"); // single role ID

            // Validate required fields
            if (userName == null || userName.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "userName and password are required");
                return response.toString();
            }

            userName = userName.trim();
            if (fullName == null) fullName = "";
            if (status == null || status.trim().isEmpty()) status = "A";

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

            // Check duplicate userName
            String checkSql = "SELECT COUNT(*) FROM vinplay_admin.user WHERE UserName = ?";
            try (PreparedStatement cps = conn.prepareStatement(checkSql)) {
                cps.setString(1, userName);
                try (ResultSet crs = cps.executeQuery()) {
                    if (crs.next() && crs.getInt(1) > 0) {
                        response.put("success", false);
                        response.put("errorCode", "1003");
                        response.put("message", "UserName already exists");
                        return response.toString();
                    }
                }
            }

            // Insert user
            String insertSql = "INSERT INTO vinplay_admin.user (UserName, Password, FullName, Status) VALUES (?, ?, ?, ?)";
            int newAdminId;
            try (PreparedStatement ips = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ips.setString(1, userName);
                ips.setString(2, password);
                ips.setString(3, fullName.trim());
                ips.setString(4, status.trim());
                ips.executeUpdate();

                try (ResultSet gk = ips.getGeneratedKeys()) {
                    if (gk.next()) {
                        newAdminId = gk.getInt(1);
                    } else {
                        throw new RuntimeException("Failed to get generated admin ID");
                    }
                }
            }

            // Assign single role (one role per admin — matches AssignRoleToAdminProcessor design)
            if (roleId > 0) {
                String insertRoleSql = "INSERT IGNORE INTO admin_user_roles (admin_id, role_id, assigned_by) VALUES (?, ?, ?)";
                try (PreparedStatement rps = conn.prepareStatement(insertRoleSql)) {
                    rps.setInt(1, newAdminId);
                    rps.setInt(2, roleId);
                    rps.setString(3, actorNickname);
                    rps.executeUpdate();
                }
            }

            conn.commit();

            // Audit log
            try {
                JSONObject afterJson = new JSONObject();
                afterJson.put("userName", userName);
                afterJson.put("fullName", fullName);
                afterJson.put("status", status);
                afterJson.put("role_id", roleId > 0 ? roleId : "none");
                RbacSupport.insertAuditLog(conn, actorNickname, "CREATE_ADMIN",
                        "admin_user", String.valueOf(newAdminId), null,
                        afterJson.toString(), RbacSupport.getRequestIp(request));
            } catch (Exception auditEx) {
                logger.warn("Audit log failed for CreateAdminUser", auditEx);
            }

            JSONObject data = new JSONObject();
            data.put("id", newAdminId);
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);

        } catch (Exception e) {
            logger.error("CreateAdminUserProcessor error", e);
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
