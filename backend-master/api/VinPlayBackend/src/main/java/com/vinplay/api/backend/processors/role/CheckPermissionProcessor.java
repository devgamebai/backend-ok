package com.vinplay.api.backend.processors.role;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9707 - Check if current admin has a specific permission.
 * Params: permission_key
 * Returns: {hasPermission: true/false}
 */
public class CheckPermissionProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            String permissionKey = request.getParameter("permission_key");
            if (permissionKey == null || permissionKey.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "permission_key is required");
                return response.toString();
            }

            boolean hasPerm = RbacSupport.hasSuperAdminRole(adminNickname) ||
                    RbacSupport.hasPermission(adminNickname, permissionKey.trim());

            JSONObject data = new JSONObject();
            data.put("hasPermission", hasPerm);
            data.put("permission_key", permissionKey.trim());

            response.put("success", true);
            response.put("data", data);

        } catch (Exception e) {
            logger.error("CheckPermissionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
