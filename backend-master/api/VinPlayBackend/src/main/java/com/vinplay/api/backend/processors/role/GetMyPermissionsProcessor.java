package com.vinplay.api.backend.processors.role;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9708 - Get all permissions for the current logged-in admin.
 * Used by CMS to build the sidebar/menu.
 */
public class GetMyPermissionsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = request.getParameter("aat");
            }
            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            String adminNickname = tokenMap.get(accessToken);
            if (adminNickname == null) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            JSONObject data = new JSONObject();
            JSONArray permissions = new JSONArray();
            JSONArray roles = new JSONArray();

            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");

                // Get roles for this admin
                String roleSql = "SELECT ar.id, ar.name, ar.description " +
                        "FROM admin_user_roles aur " +
                        "JOIN vinplay_admin.user au ON au.ID = aur.admin_id " +
                        "JOIN admin_roles ar ON ar.id = aur.role_id " +
                        "WHERE au.UserName = ? AND ar.status = 1";
                ps = conn.prepareStatement(roleSql);
                ps.setString(1, adminNickname);
                rs = ps.executeQuery();
                while (rs.next()) {
                    JSONObject role = new JSONObject();
                    role.put("id", rs.getInt("id"));
                    role.put("name", rs.getString("name"));
                    role.put("description", rs.getString("description") != null ? rs.getString("description") : "");
                    roles.put(role);
                }
                rs.close();
                ps.close();

                // Admin role gets ALL active permissions; other roles get only their assigned permissions
                boolean isAdminRole = RbacSupport.hasSuperAdminRole(adminNickname);

                String permSql;
                if (isAdminRole) {
                    permSql = "SELECT permission_key, permission_name, module, module_name, feature_key, feature_name, action " +
                            "FROM admin_permissions WHERE status = 1 ORDER BY module, permission_key";
                    ps = conn.prepareStatement(permSql);
                } else {
                    permSql = "SELECT DISTINCT ap.permission_key, ap.permission_name, ap.module, ap.module_name, ap.feature_key, ap.feature_name, ap.action " +
                            "FROM admin_user_roles aur " +
                            "JOIN vinplay_admin.user au ON au.ID = aur.admin_id " +
                            "JOIN role_permissions rp ON rp.role_id = aur.role_id " +
                            "JOIN admin_permissions ap ON ap.id = rp.permission_id " +
                            "JOIN admin_roles ar ON ar.id = aur.role_id " +
                            "WHERE au.UserName = ? AND ar.status = 1 AND ap.status = 1 " +
                            "ORDER BY ap.module, ap.permission_key";
                    ps = conn.prepareStatement(permSql);
                    ps.setString(1, adminNickname);
                }
                rs = ps.executeQuery();
                while (rs.next()) {
                    JSONObject perm = new JSONObject();
                    perm.put("permission_key", rs.getString("permission_key"));
                    perm.put("permission_name", rs.getString("permission_name"));
                    perm.put("module", rs.getString("module"));
                    perm.put("module_name", rs.getString("module_name") != null ? rs.getString("module_name") : "");
                    perm.put("feature_key", rs.getString("feature_key") != null ? rs.getString("feature_key") : "");
                    perm.put("feature_name", rs.getString("feature_name") != null ? rs.getString("feature_name") : "");
                    perm.put("action", rs.getString("action") != null ? rs.getString("action") : "");
                    permissions.put(perm);
                }
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            data.put("username", adminNickname);
            data.put("roles", roles);
            data.put("permissions", permissions);

            response.put("success", true);
            response.put("data", data);

        } catch (Exception e) {
            logger.error("GetMyPermissionsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
