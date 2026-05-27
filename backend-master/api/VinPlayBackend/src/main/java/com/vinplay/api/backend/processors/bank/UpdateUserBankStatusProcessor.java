package com.vinplay.api.backend.processors.bank;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class UpdateUserBankStatusProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String idStr = request.getParameter("id");
            String statusStr = request.getParameter("status");

            if (idStr == null || idStr.isEmpty() || statusStr == null || statusStr.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            int id = Integer.parseInt(idStr);
            int status = Integer.parseInt(statusStr);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users_bank SET status = ?, update_date = NOW() WHERE id = ?")) {
                ps.setInt(1, status);
                ps.setInt(2, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    response.put("success", true);
                    response.put("errorCode", "0");
                } else {
                    response.put("success", false);
                    response.put("errorCode", "1002");
                }
            }
        } catch (NumberFormatException e) {
            logger.error("UpdateUserBankStatusProcessor: invalid param", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        } catch (Exception e) {
            logger.error("UpdateUserBankStatusProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
