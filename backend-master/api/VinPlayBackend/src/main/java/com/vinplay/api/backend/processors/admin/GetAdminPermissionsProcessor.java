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
 * c=9715 — Get admin user's groups and menu permissions.
 * Replaces CMS's userrole_model->get_list_role_user().
 *
 * Params: aat, aid (admin user ID)
 * Response: {"success": true, "data": {"groups": [...], "menus": [...]}}
 */
public class GetAdminPermissionsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String aidStr = request.getParameter("aid");
            if (aidStr == null || aidStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "aid is required");
                return response.toString();
            }

            int adminId = Integer.parseInt(aidStr);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {

                // Get groups assigned to this admin
                JSONArray groups = new JSONArray();
                String sqlGroups = "SELECT g.Id, g.Name, g.Description FROM groupuser g " +
                        "INNER JOIN userrole ur ON g.Id = ur.Group_ID " +
                        "WHERE ur.User_ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlGroups)) {
                    ps.setInt(1, adminId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject g = new JSONObject();
                            g.put("id", rs.getInt("Id"));
                            g.put("name", rs.getString("Name"));
                            g.put("description", rs.getString("Description") != null ? rs.getString("Description") : "");
                            groups.put(g);
                        }
                    }
                }

                // Get menu permissions via groups
                JSONArray menus = new JSONArray();
                String sqlMenus = "SELECT DISTINCT m.id, m.Name, m.Link, m.Parrent_ID, m.Param " +
                        "FROM menu m " +
                        "INNER JOIN rolemenu rm ON m.id = rm.Menu_ID " +
                        "INNER JOIN userrole ur ON rm.Group_ID = ur.Group_ID " +
                        "WHERE ur.User_ID = ? " +
                        "ORDER BY m.id";
                try (PreparedStatement ps = conn.prepareStatement(sqlMenus)) {
                    ps.setInt(1, adminId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject m = new JSONObject();
                            m.put("id", rs.getInt("id"));
                            m.put("name", rs.getString("Name"));
                            m.put("link", rs.getString("Link") != null ? rs.getString("Link") : "");
                            m.put("parentId", rs.getObject("Parrent_ID") != null ? rs.getInt("Parrent_ID") : 0);
                            m.put("param", rs.getObject("Param") != null ? rs.getInt("Param") : 0);
                            menus.put(m);
                        }
                    }
                }

                JSONObject data = new JSONObject();
                data.put("groups", groups);
                data.put("menus", menus);

                response.put("success", true);
                response.put("data", data);
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid aid format");
        } catch (Exception e) {
            logger.error("GetAdminPermissionsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
