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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * c=9706 - List all admin users with their assigned roles.
 */
public class ListAdminRolesProcessor implements BaseProcessor<HttpServletRequest, String> {

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
                response.put("message", "Permission denied. You do not have permission to view admin role assignments.");
                return response.toString();
            }

            JSONArray admins = new JSONArray();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                String sql = "SELECT au.ID as admin_id, au.UserName as username, au.FullName as full_name, " +
                        "au.Status as status, " +
                        "ar.id as role_id, ar.name as role_name, ar.description as role_description, ar.status as role_status, " +
                        "aur.assigned_by, aur.assigned_at " +
                        "FROM vinplay_admin.user au " +
                        "LEFT JOIN admin_user_roles aur ON au.ID = aur.admin_id " +
                        "LEFT JOIN admin_roles ar ON aur.role_id = ar.id " +
                        "ORDER BY au.ID, ar.id";
                ps = conn.prepareStatement(sql);
                rs = ps.executeQuery();

                Map<Integer, JSONObject> adminMap = new LinkedHashMap<Integer, JSONObject>();
                while (rs.next()) {
                    int adminId = rs.getInt("admin_id");
                    JSONObject admin = adminMap.get(adminId);
                    if (admin == null) {
                        admin = new JSONObject();
                        admin.put("admin_id", adminId);
                        admin.put("username", rs.getString("username") != null ? rs.getString("username") : "");
                        admin.put("full_name", rs.getString("full_name") != null ? rs.getString("full_name") : "");
                        admin.put("status", rs.getString("status") != null ? rs.getString("status") : "");
                        admin.put("role_ids", new JSONArray());
                        admin.put("roles", new JSONArray());
                        adminMap.put(adminId, admin);
                    }

                    if (rs.getObject("role_id") != null) {
                        JSONObject role = new JSONObject();
                        role.put("role_id", rs.getInt("role_id"));
                        role.put("role_name", rs.getString("role_name") != null ? rs.getString("role_name") : "");
                        role.put("role_description", rs.getString("role_description") != null ? rs.getString("role_description") : "");
                        role.put("role_status", rs.getInt("role_status"));
                        role.put("assigned_by", rs.getString("assigned_by") != null ? rs.getString("assigned_by") : "");
                        role.put("assigned_at", rs.getString("assigned_at") != null ? rs.getString("assigned_at") : "");
                        admin.getJSONArray("role_ids").put(rs.getInt("role_id"));
                        admin.getJSONArray("roles").put(role);
                    }
                }

                for (JSONObject admin : adminMap.values()) {
                    admins.put(admin);
                }
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            response.put("success", true);
            response.put("data", admins);

        } catch (Exception e) {
            logger.error("ListAdminRolesProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
