package com.vinplay.api.backend.processors.bank;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DeleteAdminBankProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String idStr = request.getParameter("id");

            if (idStr == null || idStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "5");
                response.put("message", "Missing id parameter");
                return response.toString();
            }

            int id;
            try {
                id = Integer.parseInt(idStr.trim());
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("errorCode", "5");
                response.put("message", "Invalid id parameter");
                return response.toString();
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                String sql = "DELETE FROM admin_banks WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        response.put("success", true);
                        response.put("errorCode", "0");
                        response.put("message", "OK");
                    } else {
                        response.put("success", false);
                        response.put("errorCode", "5");
                        response.put("message", "No record found with id " + id);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("DeleteAdminBankProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5");
            response.put("message", e.getMessage());
        }
        return response.toString();
    }
}
