package com.vinplay.api.backend.processors.banner;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9400 — Add a new banner.
 * Params: title, image_path, url, action, status
 */
public class AddNewBannerProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String title = request.getParameter("title");
            String imagePath = request.getParameter("image_path");
            String url = request.getParameter("url");
            String action = request.getParameter("action");
            String status = request.getParameter("status");

            if (title == null || title.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                return response.toString();
            }

            int statusVal = 1;
            try { if (status != null) statusVal = Integer.parseInt(status); } catch (NumberFormatException ignored) {}

            String sql = "INSERT INTO banner (title, image_path, url, action, status) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, title);
                    ps.setString(2, imagePath != null ? imagePath : "");
                    ps.setString(3, url != null ? url : "");
                    ps.setString(4, action != null ? action : "");
                    ps.setInt(5, statusVal);
                    ps.executeUpdate();

                    int id = 0;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) id = rs.getInt(1);
                    }

                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("id", id);
                }
            }

        } catch (Exception e) {
            logger.error("AddNewBannerProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
