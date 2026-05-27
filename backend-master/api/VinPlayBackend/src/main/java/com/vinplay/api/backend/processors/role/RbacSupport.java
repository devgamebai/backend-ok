package com.vinplay.api.backend.processors.role;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class RbacSupport {

    private static final Logger logger = Logger.getLogger("api");

    private RbacSupport() {
    }

    public static String getAdminNicknameFromToken(HttpServletRequest request, JSONObject response) {
        try {
            String accessToken = request.getParameter("at");
            // Also accept aat (admin access token) — CMS sends aat, not at
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = request.getParameter("aat");
            }
            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return null;
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            String nickname = tokenMap.get(accessToken);
            if (nickname == null) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return null;
            }
            return nickname;
        } catch (Exception e) {
            logger.error("getAdminNicknameFromToken error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            return null;
        }
    }

    public static String getAdminNicknameFromToken(String accessToken) {
        try {
            if (accessToken == null || accessToken.isEmpty()) {
                return null;
            }
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            return tokenMap.get(accessToken);
        } catch (Exception e) {
            logger.error("getAdminNicknameFromToken(token) error", e);
            return null;
        }
    }

    public static boolean hasPermission(String nickname, String permissionKey) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
            String sql = "SELECT 1 FROM admin_user_roles aur " +
                    "JOIN vinplay_admin.user au ON au.ID = aur.admin_id " +
                    "JOIN role_permissions rp ON rp.role_id = aur.role_id " +
                    "JOIN admin_permissions ap ON ap.id = rp.permission_id " +
                    "JOIN admin_roles ar ON ar.id = aur.role_id " +
                    "WHERE au.UserName = ? AND ap.permission_key = ? AND ar.status = 1 AND ap.status = 1 LIMIT 1";
            ps = conn.prepareStatement(sql);
            ps.setString(1, nickname);
            ps.setString(2, permissionKey);
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            logger.error("hasPermission error", e);
            return false;
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (Exception ignored) {}
            if (ps != null) try {
                ps.close();
            } catch (Exception ignored) {}
            if (conn != null) try {
                conn.close();
            } catch (Exception ignored) {}
        }
    }

    public static boolean hasSuperAdminRole(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return false;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");

            // SUN-1244: tightened. Previously this granted "super admin" to
            // anyone with the 'admin' role in admin_user_roles — the recently
            // disabled 'admin' (id=23), 'admin1' (id=22), 'mrdeal' (id=24)
            // accounts all qualified, despite UI/audit treating them as
            // sub-admins. The 'admin' account was implicated in the
            // money-laundering investigation. Locking the gate to:
            //   1. UserName = 'superadmin' (case-insensitive)
            //   2. AND Status = 'A'
            //   3. AND has the 'admin' role in admin_user_roles
            //
            // Other admins keep their UI access via per-permission checks
            // (RbacSupport.hasPermission) — but they CANNOT pass the
            // super-admin gate that guards admin-CRUD, role mutations, etc.
            String sql = "SELECT 1 FROM admin_user_roles aur " +
                    "JOIN vinplay_admin.user au ON au.ID = aur.admin_id " +
                    "JOIN admin_roles ar ON ar.id = aur.role_id " +
                    "WHERE LOWER(au.UserName) = 'superadmin' AND au.UserName = ? " +
                    "  AND au.Status = 'A' " +
                    "  AND ar.name = 'admin' AND ar.status = 1 LIMIT 1";
            ps = conn.prepareStatement(sql);
            ps.setString(1, nickname);
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            logger.error("hasSuperAdminRole error", e);
            return false;
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (Exception ignored) {}
            if (ps != null) try {
                ps.close();
            } catch (Exception ignored) {}
            if (conn != null) try {
                conn.close();
            } catch (Exception ignored) {}
        }
    }

    public static Integer getAdminIdByUsername(Connection conn, String username) throws Exception {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement("SELECT ID FROM vinplay_admin.user WHERE UserName = ? LIMIT 1");
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("ID");
            }
            return null;
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
        }
    }

    public static boolean isAdminActive(String nickname) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
            ps = conn.prepareStatement("SELECT Status FROM vinplay_admin.user WHERE UserName = ? LIMIT 1");
            ps.setString(1, nickname);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return false;
            }
            String status = rs.getString("Status");
            if (status == null) {
                return false;
            }
            String normalized = status.trim();
            return "A".equalsIgnoreCase(normalized)
                    || "ACTIVE".equalsIgnoreCase(normalized)
                    || "1".equals(normalized);
        } catch (Exception e) {
            logger.error("isAdminActive error", e);
            return false;
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }

    public static boolean canManageRoles(String nickname) {
        if (hasSuperAdminRole(nickname)) {
            return true;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
            String sql = "SELECT 1 FROM admin_user_roles aur " +
                    "JOIN vinplay_admin.user au ON au.ID = aur.admin_id " +
                    "JOIN admin_roles ar ON ar.id = aur.role_id " +
                    "LEFT JOIN role_permissions rp ON rp.role_id = aur.role_id " +
                    "LEFT JOIN admin_permissions ap ON ap.id = rp.permission_id " +
                    "WHERE au.UserName = ? AND ar.status = 1 " +
                    "AND (ar.name = 'admin' OR (ap.permission_key = 'system.roles' AND ap.status = 1)) " +
                    "LIMIT 1";
            ps = conn.prepareStatement(sql);
            ps.setString(1, nickname);
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            logger.error("canManageRoles error", e);
            return false;
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }

    public static boolean canManagePermissions(String nickname) {
        return hasSuperAdminRole(nickname);
    }

    public static String getRequestIp(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip != null && !ip.isEmpty()) {
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        } else {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip : "";
    }

    public static void insertAuditLog(Connection conn, String actorAdmin, String actionType,
                                      String targetType, String targetId, String beforeJson,
                                      String afterJson, String ipAddress) throws Exception {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(
                    "INSERT INTO admin_rbac_audit_logs " +
                            "(actor_admin, action_type, target_type, target_id, before_json, after_json, ip_address, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())");
            ps.setString(1, actorAdmin != null ? actorAdmin : "");
            ps.setString(2, actionType != null ? actionType : "");
            ps.setString(3, targetType != null ? targetType : "");
            ps.setString(4, targetId != null ? targetId : "");
            ps.setString(5, beforeJson != null ? beforeJson : "");
            ps.setString(6, afterJson != null ? afterJson : "");
            ps.setString(7, ipAddress != null ? ipAddress : "");
            ps.executeUpdate();
        } finally {
            if (ps != null) try {
                ps.close();
            } catch (Exception ignored) {}
        }
    }
}
