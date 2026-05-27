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
 * c=9704 - List all permissions grouped by module.
 */
public class ListPermissionsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String adminNickname = RbacSupport.getAdminNicknameFromToken(request, response);
            if (adminNickname == null) {
                return response.toString();
            }
            if (!RbacSupport.canManagePermissions(adminNickname)) {
                response.put("success", false);
                response.put("errorCode", "4003");
                response.put("message", "Permission denied. You do not have permission to view permission configuration.");
                return response.toString();
            }

            JSONArray data = new JSONArray();
            JSONObject byModule = new JSONObject();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            String includeInactive = request.getParameter("include_inactive");
            boolean showInactive = "1".equals(includeInactive);

            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                String statusFilter = showInactive ? "" : " AND status = 1";
                ps = conn.prepareStatement(
                        "SELECT id, permission_key, permission_name, module, module_key, module_name, feature_key, feature_name, action, description, status " +
                                "FROM admin_permissions " +
                                "WHERE 1=1" + statusFilter + " " +
                                "ORDER BY module_name, feature_name, action, permission_key");
                rs = ps.executeQuery();

                Map<String, JSONObject> moduleMap = new LinkedHashMap<String, JSONObject>();
                Map<String, JSONObject> featureMap = new LinkedHashMap<String, JSONObject>();

                while (rs.next()) {
                    String moduleName = rs.getString("module_name");
                    if (moduleName == null || moduleName.trim().isEmpty()) {
                        moduleName = rs.getString("module");
                    }
                    if (moduleName == null || moduleName.trim().isEmpty()) {
                        moduleName = "System";
                    }
                    String moduleKey = rs.getString("module_key");
                    if (moduleKey == null || moduleKey.trim().isEmpty()) {
                        moduleKey = moduleName.trim().toLowerCase().replaceAll("\\s+", "_");
                    }
                    String featureName = rs.getString("feature_name");
                    if (featureName == null || featureName.trim().isEmpty()) {
                        featureName = "General";
                    }
                    String featureKey = rs.getString("feature_key");
                    if (featureKey == null || featureKey.trim().isEmpty()) {
                        featureKey = featureName.trim().toLowerCase().replaceAll("\\s+", "_");
                    }

                    String moduleMapKey = moduleKey + "|" + moduleName;
                    JSONObject module = moduleMap.get(moduleMapKey);
                    if (module == null) {
                        module = new JSONObject();
                        module.put("module_key", moduleKey);
                        module.put("module_name", moduleName);
                        module.put("features", new JSONArray());
                        moduleMap.put(moduleMapKey, module);
                    }

                    String featureMapKey = moduleMapKey + "|" + featureKey + "|" + featureName;
                    JSONObject feature = featureMap.get(featureMapKey);
                    if (feature == null) {
                        feature = new JSONObject();
                        feature.put("feature_key", featureKey);
                        feature.put("feature_name", featureName);
                        feature.put("permissions", new JSONArray());
                        featureMap.put(featureMapKey, feature);
                        module.getJSONArray("features").put(feature);
                    }

                    if (!byModule.has(moduleName)) {
                        byModule.put(moduleName, new JSONArray());
                    }

                    JSONObject perm = new JSONObject();
                    perm.put("id", rs.getInt("id"));
                    perm.put("permission_key", rs.getString("permission_key"));
                    perm.put("permission_name", rs.getString("permission_name"));
                    perm.put("module_key", moduleKey);
                    perm.put("module_name", moduleName);
                    perm.put("feature_key", featureKey);
                    perm.put("feature_name", featureName);
                    perm.put("action", rs.getString("action") != null ? rs.getString("action") : "");
                    perm.put("description", rs.getString("description") != null ? rs.getString("description") : "");
                    perm.put("status", rs.getInt("status"));
                    feature.getJSONArray("permissions").put(perm);
                    byModule.getJSONArray(moduleName).put(new JSONObject(perm.toString()));
                }

                for (JSONObject module : moduleMap.values()) {
                    data.put(module);
                }
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            response.put("success", true);
            response.put("data", data);
            response.put("by_module", byModule);

        } catch (Exception e) {
            logger.error("ListPermissionsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
