package com.vinplay.api.backend.processors.admin;

import com.vinplay.api.backend.processors.role.RbacSupport;
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
import java.util.List;

/**
 * c=9820 — List admin users with RBAC roles and permissions.
 * Supports pagination and keyword search.
 */
public class ListAdminUsersProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            // Auth: only super_admin or admin role can list admin users
            String nickname = RbacSupport.getAdminNicknameFromToken(request, response);
            if (nickname == null) return response.toString();
            if (!RbacSupport.hasSuperAdminRole(nickname)) {
                response.put("success", false);
                response.put("errorCode", "403");
                response.put("message", "Permission denied");
                return response.toString();
            }

            // Pagination
            int page = 1;
            int pageSize = 20;
            try {
                if (request.getParameter("page") != null) page = Integer.parseInt(request.getParameter("page"));
                if (request.getParameter("pageSize") != null) pageSize = Integer.parseInt(request.getParameter("pageSize"));
            } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 20;
            int offset = (page - 1) * pageSize;

            // Filters
            String keyword = request.getParameter("keyword");
            String statusFilter = request.getParameter("status");

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {

                // Build WHERE clause
                StringBuilder where = new StringBuilder("WHERE 1=1");
                List<Object> params = new ArrayList<>();

                if (keyword != null && !keyword.trim().isEmpty()) {
                    where.append(" AND (u.UserName LIKE ? OR u.FullName LIKE ?)");
                    String like = "%" + keyword.trim() + "%";
                    params.add(like);
                    params.add(like);
                }
                if (statusFilter != null && !statusFilter.trim().isEmpty()) {
                    where.append(" AND u.Status = ?");
                    params.add(statusFilter.trim());
                }

                // Count total
                String countSql = "SELECT COUNT(*) FROM vinplay_admin.user u " + where;
                int total = 0;
                try (PreparedStatement cps = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) {
                        cps.setObject(i + 1, params.get(i));
                    }
                    try (ResultSet crs = cps.executeQuery()) {
                        if (crs.next()) total = crs.getInt(1);
                    }
                }

                // Query users
                String listSql = "SELECT u.ID, u.UserName, u.FullName, u.Status, u.ParentID, u.Is2FAEnabled " +
                        "FROM vinplay_admin.user u " + where +
                        " ORDER BY u.ID ASC LIMIT ? OFFSET ?";
                JSONArray list = new JSONArray();

                try (PreparedStatement lps = conn.prepareStatement(listSql)) {
                    int idx = 1;
                    for (Object p : params) {
                        lps.setObject(idx++, p);
                    }
                    lps.setInt(idx++, pageSize);
                    lps.setInt(idx, offset);

                    try (ResultSet lrs = lps.executeQuery()) {
                        while (lrs.next()) {
                            JSONObject admin = new JSONObject();
                            int adminId = lrs.getInt("ID");
                            admin.put("id", adminId);
                            admin.put("userName", lrs.getString("UserName") != null ? lrs.getString("UserName") : "");
                            admin.put("fullName", lrs.getString("FullName") != null ? lrs.getString("FullName") : "");
                            admin.put("status", lrs.getString("Status") != null ? lrs.getString("Status") : "");
                            admin.put("parentId", lrs.getObject("ParentID") != null ? lrs.getInt("ParentID") : 0);
                            admin.put("is2FAEnabled", lrs.getInt("Is2FAEnabled") == 1);

                            // Get role for this admin (single role per admin)
                            JSONObject roleObj = getRole(conn, adminId);
                            admin.put("role", roleObj);

                            // Admin role check is now purely by RBAC role name
                            boolean isAdminRole = roleObj != null &&
                                    "admin".equals(roleObj.optString("name"));

                            // Get permissions for this admin
                            JSONArray permArray = getPermissions(conn, adminId, isAdminRole);
                            admin.put("permissions", permArray);

                            list.put(admin);
                        }
                    }
                }

                JSONObject data = new JSONObject();
                data.put("list", list);
                data.put("total", total);
                data.put("page", page);
                data.put("pageSize", pageSize);

                response.put("success", true);
                response.put("data", data);
            }
        } catch (Exception e) {
            logger.error("ListAdminUsersProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }

    private JSONObject getRole(Connection conn, int adminId) throws Exception {
        String sql = "SELECT ar.id, ar.name, ar.description " +
                "FROM admin_user_roles aur " +
                "JOIN admin_roles ar ON ar.id = aur.role_id " +
                "WHERE aur.admin_id = ? AND ar.status = 1 LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JSONObject role = new JSONObject();
                    role.put("id", rs.getInt("id"));
                    role.put("name", rs.getString("name"));
                    role.put("description", rs.getString("description") != null ? rs.getString("description") : "");
                    return role;
                }
            }
        }
        return null;
    }

    private JSONArray getPermissions(Connection conn, int adminId, boolean isAdminRole) throws Exception {
        JSONArray perms = new JSONArray();
        if (isAdminRole) {
            String sql = "SELECT permission_key FROM admin_permissions WHERE status = 1 ORDER BY module, permission_key";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    perms.put(rs.getString("permission_key"));
                }
            }
        } else {
            String sql = "SELECT DISTINCT ap.permission_key " +
                    "FROM admin_user_roles aur " +
                    "JOIN role_permissions rp ON rp.role_id = aur.role_id " +
                    "JOIN admin_permissions ap ON ap.id = rp.permission_id " +
                    "JOIN admin_roles ar ON ar.id = aur.role_id " +
                    "WHERE aur.admin_id = ? AND ar.status = 1 AND ap.status = 1 " +
                    "ORDER BY ap.permission_key";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, adminId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        perms.put(rs.getString("permission_key"));
                    }
                }
            }
        }
        return perms;
    }
}
