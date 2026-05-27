package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * c=9714 — Assign groups to an admin user.
 * Replaces CMS's userrole_model->create().
 *
 * Params: aat, aid (admin user ID), gids (comma-separated group IDs, e.g. "13,16")
 * Response: {"success": true}
 */
public class AssignAdminGroupsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String aidStr = request.getParameter("aid");
            String gids = request.getParameter("gids");

            if (aidStr == null || aidStr.isEmpty() || gids == null || gids.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "aid and gids are required");
                return response.toString();
            }

            int adminId = Integer.parseInt(aidStr);
            String[] groupIds = gids.split(",");

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                // Delete existing role assignments
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM userrole WHERE User_ID = ?")) {
                    ps.setInt(1, adminId);
                    ps.executeUpdate();
                }

                // Insert new assignments
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO userrole (User_ID, Group_ID) VALUES (?, ?)")) {
                    for (String gid : groupIds) {
                        String trimmed = gid.trim();
                        if (trimmed.isEmpty()) continue;
                        ps.setInt(1, adminId);
                        ps.setInt(2, Integer.parseInt(trimmed));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            response.put("success", true);
            logger.info("AssignAdminGroups: adminId=" + adminId + " groups=" + gids);

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid aid or gids format");
        } catch (Exception e) {
            logger.error("AssignAdminGroupsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
