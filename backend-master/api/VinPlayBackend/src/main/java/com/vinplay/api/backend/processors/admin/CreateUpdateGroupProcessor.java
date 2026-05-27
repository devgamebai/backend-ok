package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class CreateUpdateGroupProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String idStr = request.getParameter("id");
            String name = request.getParameter("name");
            String desc = request.getParameter("desc");
            String menus = request.getParameter("menus");

            if (name == null || name.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                conn.setAutoCommit(false);
                try {
                    int groupId;

                    if (idStr != null && !idStr.isEmpty()) {
                        // Update
                        groupId = Integer.parseInt(idStr);
                        try (PreparedStatement ps = conn.prepareStatement("UPDATE groupuser SET Name = ?, Description = ? WHERE Id = ?")) {
                            ps.setString(1, name);
                            ps.setString(2, desc != null ? desc : "");
                            ps.setInt(3, groupId);
                            ps.executeUpdate();
                        }
                        // Delete old menus
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM rolemenu WHERE Group_ID = ?")) {
                            ps.setInt(1, groupId);
                            ps.executeUpdate();
                        }
                    } else {
                        // Create
                        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO groupuser (Name, Description) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, name);
                            ps.setString(2, desc != null ? desc : "");
                            ps.executeUpdate();
                            try (ResultSet keys = ps.getGeneratedKeys()) {
                                if (keys.next()) {
                                    groupId = keys.getInt(1);
                                } else {
                                    conn.rollback();
                                    response.put("success", false);
                                    response.put("errorCode", "9999");
                                    return response.toString();
                                }
                            }
                        }
                    }

                    // Insert menu assignments
                    if (menus != null && !menus.isEmpty()) {
                        String[] menuIds = menus.split(",");
                        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO rolemenu (Group_ID, Menu_ID) VALUES (?,?)")) {
                            for (String mid : menuIds) {
                                mid = mid.trim();
                                if (!mid.isEmpty()) {
                                    try {
                                        ps.setInt(1, groupId);
                                        ps.setInt(2, Integer.parseInt(mid));
                                        ps.addBatch();
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                            ps.executeBatch();
                        }
                    }

                    conn.commit();

                    JSONObject data = new JSONObject();
                    data.put("id", groupId);
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", data.toString());
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            logger.error("CreateUpdateGroupProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
