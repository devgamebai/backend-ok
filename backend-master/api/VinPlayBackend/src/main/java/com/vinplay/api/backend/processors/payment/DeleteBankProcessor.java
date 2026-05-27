package com.vinplay.api.backend.processors.payment;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * c=8803 — Delete player bank account by ID.
 */
public class DeleteBankProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String idStr = request.getParameter("id");

            if (idStr == null || idStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "id is required");
                return response.toString();
            }

            long id = Long.parseLong(idStr);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM users_bank WHERE id = ?")) {
                ps.setLong(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    response.put("success", true);
                    response.put("errorCode", "0");
                } else {
                    response.put("success", false);
                    response.put("errorCode", "1005");
                    response.put("message", "Bank account not found");
                }
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("errorCode", "4001");
            response.put("message", "Invalid id");
        } catch (Exception e) {
            logger.error("DeleteBankProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
