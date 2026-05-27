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

public class CrudMenuProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String action = request.getParameter("action");

            if (action == null || action.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                switch (action) {
                    case "create": {
                        String name = request.getParameter("name");
                        String link = request.getParameter("link");
                        String parentIdStr = request.getParameter("parentId");
                        String paramStr = request.getParameter("param");
                        String isThuongStr = request.getParameter("isThuong");

                        if (name == null || name.isEmpty()) {
                            response.put("success", false);
                            response.put("errorCode", "1001");
                            return response.toString();
                        }

                        int parentId = 0;
                        int sortOrder = 0;
                        int isThuong = 1;
                        try { if (parentIdStr != null) parentId = Integer.parseInt(parentIdStr); } catch (NumberFormatException ignored) {}
                        try { if (paramStr != null) sortOrder = Integer.parseInt(paramStr); } catch (NumberFormatException ignored) {}
                        try { if (isThuongStr != null) isThuong = Integer.parseInt(isThuongStr); } catch (NumberFormatException ignored) {}

                        String sql = "INSERT INTO menu (Name, Link, Parrent_ID, Param, isThuong) VALUES (?,?,?,?,?)";
                        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, name);
                            ps.setString(2, link != null ? link : "");
                            ps.setInt(3, parentId);
                            ps.setInt(4, sortOrder);
                            ps.setInt(5, isThuong);
                            ps.executeUpdate();

                            try (ResultSet keys = ps.getGeneratedKeys()) {
                                JSONObject data = new JSONObject();
                                if (keys.next()) {
                                    data.put("id", keys.getInt(1));
                                }
                                response.put("success", true);
                                response.put("errorCode", "0");
                                response.put("data", data.toString());
                            }
                        }
                        break;
                    }
                    case "update": {
                        String idStr = request.getParameter("id");
                        String name = request.getParameter("name");
                        String link = request.getParameter("link");
                        String parentIdStr = request.getParameter("parentId");
                        String paramStr = request.getParameter("param");
                        String isThuongStr = request.getParameter("isThuong");

                        if (idStr == null || idStr.isEmpty()) {
                            response.put("success", false);
                            response.put("errorCode", "1001");
                            return response.toString();
                        }

                        int id = Integer.parseInt(idStr);
                        String sql = "UPDATE menu SET Name = ?, Link = ?, Parrent_ID = ?, Param = ?, isThuong = ? WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, name != null ? name : "");
                            ps.setString(2, link != null ? link : "");
                            int parentId = 0;
                            int sortOrder = 0;
                            int isThuong = 1;
                            try { if (parentIdStr != null) parentId = Integer.parseInt(parentIdStr); } catch (NumberFormatException ignored) {}
                            try { if (paramStr != null) sortOrder = Integer.parseInt(paramStr); } catch (NumberFormatException ignored) {}
                            try { if (isThuongStr != null) isThuong = Integer.parseInt(isThuongStr); } catch (NumberFormatException ignored) {}
                            ps.setInt(3, parentId);
                            ps.setInt(4, sortOrder);
                            ps.setInt(5, isThuong);
                            ps.setInt(6, id);
                            ps.executeUpdate();
                        }
                        response.put("success", true);
                        response.put("errorCode", "0");
                        break;
                    }
                    case "delete": {
                        String idStr = request.getParameter("id");
                        if (idStr == null || idStr.isEmpty()) {
                            response.put("success", false);
                            response.put("errorCode", "1001");
                            return response.toString();
                        }
                        int id = Integer.parseInt(idStr);

                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM menu WHERE id = ?")) {
                            ps.setInt(1, id);
                            ps.executeUpdate();
                        }
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM rolemenu WHERE Menu_ID = ?")) {
                            ps.setInt(1, id);
                            ps.executeUpdate();
                        }
                        response.put("success", true);
                        response.put("errorCode", "0");
                        break;
                    }
                    default:
                        response.put("success", false);
                        response.put("errorCode", "1001");
                }
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "1001");
        } catch (Exception e) {
            logger.error("CrudMenuProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
