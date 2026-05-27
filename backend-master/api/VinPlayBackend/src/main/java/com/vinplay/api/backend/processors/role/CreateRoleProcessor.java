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
 * c=9701 - Create a new role. Only super_admin can access.
 * Params: name, description
 */
public class CreateRoleProcessor implements BaseProcessor<HttpServletRequest, String> {

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
            if (!RbacSupport.canManagePermissions(adminNickname)) {
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", "Permission denied. You do not have permission to create roles.");
                return response.toString();
            }

            String name = request.getParameter("name");
            String description = request.getParameter("description");

            if (name == null || name.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "name is required");
                return response.toString();
            }

            name = name.trim().toLowerCase().replaceAll("\\s+", "_");

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                conn.setAutoCommit(false);

                // Check duplicate name
                ps = conn.prepareStatement("SELECT id FROM admin_roles WHERE name = ?");
                ps.setString(1, name);
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

                // Insert new role
                ps = conn.prepareStatement("INSERT INTO admin_roles (name, description, is_default) VALUES (?, ?, 0)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setString(2, description != null ? description.trim() : "");
                ps.executeUpdate();

                rs = ps.getGeneratedKeys();
                int newId = 0;
                if (rs.next()) {
                    newId = rs.getInt(1);
                }

                JSONObject data = new JSONObject();
                data.put("id", newId);
                data.put("name", name);
                data.put("description", description != null ? description.trim() : "");

                RbacSupport.insertAuditLog(conn, adminNickname, "CREATE_ROLE", "role", String.valueOf(newId),
                        "", data.toString(), RbacSupport.getRequestIp(request));
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
            logger.error("CreateRoleProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
