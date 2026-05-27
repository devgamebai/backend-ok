package com.vinplay.api.backend.processors.admin;

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
 * c=9713 — Get admin detail by admin ID.
 * Returns admin info + RBAC roles + permissions.
 */
public class GetAdminDetailProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String aidStr = request.getParameter("aid");

            if (aidStr == null || aidStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            int aid = Integer.parseInt(aidStr);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // 1. Get admin user info
                String userSql = "SELECT ID, UserName, FullName, Status, ParentID, Is2FAEnabled FROM vinplay_admin.user WHERE ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                    ps.setInt(1, aid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            response.put("success", false);
                            response.put("errorCode", "1005");
                            return response.toString();
                        }

                        JSONObject data = new JSONObject();
                        data.put("id", rs.getInt("ID"));
                        data.put("userName", rs.getString("UserName") != null ? rs.getString("UserName") : "");
                        data.put("fullName", rs.getString("FullName") != null ? rs.getString("FullName") : "");
                        data.put("status", rs.getString("Status") != null ? rs.getString("Status") : "");
                        data.put("parentId", rs.getObject("ParentID") != null ? rs.getInt("ParentID") : 0);
                        data.put("is2FAEnabled", rs.getInt("Is2FAEnabled") == 1);

                        // 2. Get RBAC role (single role per admin)
                        JSONObject roleObj = null;
                        String roleSql = "SELECT ar.id, ar.name, ar.description " +
                                "FROM admin_user_roles aur " +
                                "JOIN admin_roles ar ON ar.id = aur.role_id " +
                                "WHERE aur.admin_id = ? AND ar.status = 1 LIMIT 1";
                        try (PreparedStatement rps = conn.prepareStatement(roleSql)) {
                            rps.setInt(1, aid);
                            try (ResultSet rrs = rps.executeQuery()) {
                                if (rrs.next()) {
                                    roleObj = new JSONObject();
                                    roleObj.put("id", rrs.getInt("id"));
                                    roleObj.put("name", rrs.getString("name"));
                                    roleObj.put("description", rrs.getString("description") != null ? rrs.getString("description") : "");
                                }
                            }
                        }
                        data.put("role", roleObj);

                        // 3. Get permissions — admin role check is now purely by RBAC role name
                        JSONArray permArray = new JSONArray();
                        boolean isAdminRole = roleObj != null &&
                                "admin".equals(roleObj.optString("name"));
                        String permSql;
                        if (isAdminRole) {
                            // Admin role gets all permissions
                            permSql = "SELECT permission_key FROM admin_permissions WHERE status = 1 ORDER BY module, permission_key";
                            try (PreparedStatement pps = conn.prepareStatement(permSql);
                                 ResultSet prs = pps.executeQuery()) {
                                while (prs.next()) {
                                    permArray.put(prs.getString("permission_key"));
                                }
                            }
                        } else {
                            // Other roles: permissions via role_permissions mapping
                            permSql = "SELECT DISTINCT ap.permission_key " +
                                    "FROM admin_user_roles aur " +
                                    "JOIN role_permissions rp ON rp.role_id = aur.role_id " +
                                    "JOIN admin_permissions ap ON ap.id = rp.permission_id " +
                                    "JOIN admin_roles ar ON ar.id = aur.role_id " +
                                    "WHERE aur.admin_id = ? AND ar.status = 1 AND ap.status = 1 " +
                                    "ORDER BY ap.permission_key";
                            try (PreparedStatement pps = conn.prepareStatement(permSql)) {
                                pps.setInt(1, aid);
                                try (ResultSet prs = pps.executeQuery()) {
                                    while (prs.next()) {
                                        permArray.put(prs.getString("permission_key"));
                                    }
                                }
                            }
                        }
                        data.put("permissions", permArray);

                        response.put("success", true);
                        response.put("errorCode", "0");
                        response.put("data", data); // Object, not String
                    }
                }
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "1001");
        } catch (Exception e) {
            logger.error("GetAdminDetailProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
