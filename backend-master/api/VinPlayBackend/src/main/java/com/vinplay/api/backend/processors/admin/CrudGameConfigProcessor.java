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

public class CrudGameConfigProcessor implements BaseProcessor<HttpServletRequest, String> {
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

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                switch (action) {
                    case "create": {
                        String name = request.getParameter("name");
                        String value = request.getParameter("value");
                        String version = request.getParameter("version");
                        String platform = request.getParameter("platform");

                        if (name == null || name.isEmpty()) {
                            response.put("success", false);
                            response.put("errorCode", "1001");
                            return response.toString();
                        }

                        String sql = "INSERT INTO game_config (name, value, version, platform) VALUES (?,?,?,?)";
                        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, name);
                            ps.setString(2, value != null ? value : "");
                            ps.setString(3, version != null ? version : "");
                            ps.setString(4, platform != null ? platform : "");
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
                        if (idStr == null || idStr.isEmpty()) {
                            response.put("success", false);
                            response.put("errorCode", "1001");
                            return response.toString();
                        }
                        int id = Integer.parseInt(idStr);
                        String name = request.getParameter("name");
                        String value = request.getParameter("value");
                        String version = request.getParameter("version");
                        String platform = request.getParameter("platform");

                        String sql = "UPDATE game_config SET name = ?, value = ?, version = ?, platform = ? WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, name != null ? name : "");
                            ps.setString(2, value != null ? value : "");
                            ps.setString(3, version != null ? version : "");
                            ps.setString(4, platform != null ? platform : "");
                            ps.setInt(5, id);
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
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM game_config WHERE id = ?")) {
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
            logger.error("CrudGameConfigProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
