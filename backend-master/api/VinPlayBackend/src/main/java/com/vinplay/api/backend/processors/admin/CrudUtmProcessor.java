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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CrudUtmProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final Set<String> ALLOWED_TYPES = new HashSet<String>(Arrays.asList("campain", "source", "medium"));

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String type = request.getParameter("type");
            String action = request.getParameter("action");

            if (type == null || !ALLOWED_TYPES.contains(type) || action == null || action.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            String tableName = "utm_" + type;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                switch (action) {
                    case "create": {
                        String name = request.getParameter("name");
                        if (name == null || name.isEmpty()) {
                            response.put("success", false);
                            response.put("errorCode", "1001");
                            return response.toString();
                        }
                        String sql = "INSERT INTO " + tableName + " (name) VALUES (?)";
                        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, name);
                            ps.executeUpdate();
                            try (ResultSet keys = ps.getGeneratedKeys()) {
                                JSONObject data = new JSONObject();
                                if (keys.next()) data.put("id", keys.getInt(1));
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
                        if (idStr == null || idStr.isEmpty() || name == null || name.isEmpty()) {
                            response.put("success", false);
                            response.put("errorCode", "1001");
                            return response.toString();
                        }
                        int id = Integer.parseInt(idStr);
                        String sql = "UPDATE " + tableName + " SET name = ? WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, name);
                            ps.setInt(2, id);
                            int rows = ps.executeUpdate();
                            if (rows > 0) {
                                response.put("success", true);
                                response.put("errorCode", "0");
                            } else {
                                response.put("success", false);
                                response.put("errorCode", "1005");
                            }
                        }
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
                        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
            logger.error("CrudUtmProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
