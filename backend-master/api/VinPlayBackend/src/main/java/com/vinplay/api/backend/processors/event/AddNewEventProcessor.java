package com.vinplay.api.backend.processors.event;

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
 * c=9410 — Add a new event.
 * Params: name, amount, expired_date
 */
public class AddNewEventProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String name = request.getParameter("name");
            String amount = request.getParameter("amount");
            String expiredDate = request.getParameter("expired_date");

            if (name == null || name.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                return response.toString();
            }

            long amountVal = 0;
            try { if (amount != null) amountVal = Long.parseLong(amount); } catch (NumberFormatException ignored) {}

            String sql = "INSERT INTO event (name, amount, expired_date, created_date) VALUES (?, ?, ?, NOW())";

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setLong(2, amountVal);
                    ps.setString(3, expiredDate != null ? expiredDate : "");
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
            logger.error("AddNewEventProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
