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
 * c=9700 - List all roles with their permissions.
 */
public class ListRolesProcessor implements BaseProcessor<HttpServletRequest, String> {

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
                response.put("message", "Permission denied. You do not have permission to view role configuration.");
                return response.toString();
            }

            JSONArray roles = new JSONArray();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                String sql = "SELECT r.id, r.name, r.description, r.is_default, r.status, " +
                        "r.created_at, r.updated_at, " +
                        "GROUP_CONCAT(ap.permission_key ORDER BY ap.permission_key) as permissions " +
                        "FROM admin_roles r " +
                        "LEFT JOIN role_permissions rp ON r.id = rp.role_id " +
                        "LEFT JOIN admin_permissions ap ON rp.permission_id = ap.id AND ap.status = 1 " +
                        "GROUP BY r.id, r.name, r.description, r.is_default, r.status, r.created_at, r.updated_at " +
                        "ORDER BY r.id";
                ps = conn.prepareStatement(sql);
                rs = ps.executeQuery();
                while (rs.next()) {
                    JSONObject role = new JSONObject();
                    role.put("id", rs.getInt("id"));
                    role.put("name", rs.getString("name"));
                    role.put("description", rs.getString("description") != null ? rs.getString("description") : "");
                    role.put("is_default", rs.getInt("is_default"));
                    role.put("status", rs.getInt("status"));
                    role.put("created_at", rs.getString("created_at") != null ? rs.getString("created_at") : "");
                    role.put("updated_at", rs.getString("updated_at") != null ? rs.getString("updated_at") : "");
                    String perms = rs.getString("permissions");
                    JSONArray permArray = new JSONArray();
                    if (perms != null && !perms.isEmpty()) {
                        String[] permList = perms.split(",");
                        for (String p : permList) {
                            permArray.put(p.trim());
                        }
                    }
                    role.put("permissions", permArray);
                    roles.put(role);
                }
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            response.put("success", true);
            response.put("data", roles);

        } catch (Exception e) {
            logger.error("ListRolesProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
