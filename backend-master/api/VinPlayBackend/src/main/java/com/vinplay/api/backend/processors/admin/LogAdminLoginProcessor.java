package com.vinplay.api.backend.processors.admin;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class LogAdminLoginProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String un = request.getParameter("un");
            String nn = request.getParameter("nn");
            String ip = request.getParameter("ip");
            String status = request.getParameter("status");
            String action = request.getParameter("action");
            String agent = request.getParameter("agent");

            if (un == null || un.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
                String sql = "INSERT INTO log_loginadmin (username, nickname, ip, status, action, agent) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, un);
                    ps.setString(2, nn != null ? nn : "");
                    ps.setString(3, ip != null ? ip : "");
                    ps.setString(4, status != null ? status : "");
                    ps.setString(5, action != null ? action : "");
                    ps.setString(6, agent != null ? agent : "");
                    ps.executeUpdate();
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
        } catch (Exception e) {
            logger.error("LogAdminLoginProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
