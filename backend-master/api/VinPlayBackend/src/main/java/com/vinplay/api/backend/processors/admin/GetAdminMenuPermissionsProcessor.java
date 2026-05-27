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
import java.util.*;

public class GetAdminMenuPermissionsProcessor implements BaseProcessor<HttpServletRequest, String> {
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
                // Step 1: Get user's group IDs
                List<Integer> groupIds = new ArrayList<Integer>();
                String sqlGroups = "SELECT Group_ID FROM userrole WHERE User_ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlGroups)) {
                    ps.setInt(1, aid);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            groupIds.add(rs.getInt("Group_ID"));
                        }
                    }
                }

                if (groupIds.isEmpty()) {
                    JSONObject data = new JSONObject();
                    data.put("menuTree", new JSONArray());
                    data.put("permissions", new JSONArray());
                    data.put("groups", new JSONArray());
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", data.toString());
                    return response.toString();
                }

                // Step 2: Get menus for each group
                Set<String> permissionSet = new LinkedHashSet<String>();
                Map<Integer, List<JSONObject>> menusByParent = new LinkedHashMap<Integer, List<JSONObject>>();
                Set<Integer> seenMenuIds = new HashSet<Integer>();

                for (int gid : groupIds) {
                    String sqlMenus = "SELECT m.* FROM rolemenu rm JOIN menu m ON rm.Menu_ID = m.id WHERE rm.Group_ID = ? AND m.isThuong = 1 ORDER BY m.Parrent_ID, m.Param, m.Name";
                    try (PreparedStatement ps = conn.prepareStatement(sqlMenus)) {
                        ps.setInt(1, gid);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int menuId = rs.getInt("id");
                                if (seenMenuIds.contains(menuId)) continue;
                                seenMenuIds.add(menuId);

                                String link = rs.getString("Link");
                                if (link != null && !link.isEmpty()) {
                                    permissionSet.add(link);
                                }

                                int parentId = rs.getInt("Parrent_ID");
                                JSONObject menuItem = new JSONObject();
                                menuItem.put("id", menuId);
                                menuItem.put("name", rs.getString("Name") != null ? rs.getString("Name") : "");
                                menuItem.put("link", link != null ? link : "");
                                menuItem.put("parentId", parentId);
                                menuItem.put("param", rs.getInt("Param"));
                                menuItem.put("isThuong", rs.getInt("isThuong"));

                                if (!menusByParent.containsKey(parentId)) {
                                    menusByParent.put(parentId, new ArrayList<JSONObject>());
                                }
                                menusByParent.get(parentId).add(menuItem);
                            }
                        }
                    }
                }

                // Step 3: Build menu tree
                JSONArray menuTree = new JSONArray();
                List<JSONObject> roots = menusByParent.containsKey(0) ? menusByParent.get(0) : new ArrayList<JSONObject>();
                for (JSONObject root : roots) {
                    int rootId = root.getInt("id");
                    if (menusByParent.containsKey(rootId)) {
                        JSONArray children = new JSONArray();
                        for (JSONObject child : menusByParent.get(rootId)) {
                            children.put(child);
                        }
                        root.put("children", children);
                    } else {
                        root.put("children", new JSONArray());
                    }
                    menuTree.put(root);
                }

                // Step 4: Build response
                JSONArray permissions = new JSONArray();
                for (String perm : permissionSet) {
                    permissions.put(perm);
                }

                JSONArray groups = new JSONArray();
                for (int gid : groupIds) {
                    groups.put(gid);
                }

                JSONObject data = new JSONObject();
                data.put("menuTree", menuTree);
                data.put("permissions", permissions);
                data.put("groups", groups);

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", data.toString());
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "1001");
        } catch (Exception e) {
            logger.error("GetAdminMenuPermissionsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
