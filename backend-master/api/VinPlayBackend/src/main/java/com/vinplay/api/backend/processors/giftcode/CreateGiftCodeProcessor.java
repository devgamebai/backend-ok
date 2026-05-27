package com.vinplay.api.backend.processors.giftcode;

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
 * c=9000 — Create a new gift code.
 * Params: code, type, money, max_use, from_date, expired_date, event, user_name, bundle_id
 */
public class CreateGiftCodeProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String code = request.getParameter("code");
            String type = request.getParameter("type");
            String money = request.getParameter("money");
            String maxUse = request.getParameter("max_use");
            String fromDate = request.getParameter("from_date");
            String expiredDate = request.getParameter("expired_date");
            String event = request.getParameter("event");
            String userName = request.getParameter("user_name");
            String bundleId = request.getParameter("bundle_id");

            if (code == null || code.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                return response.toString();
            }

            int typeVal = 0;
            try { if (type != null) typeVal = Integer.parseInt(type); } catch (NumberFormatException ignored) {}
            long moneyVal = 0;
            try { if (money != null) moneyVal = Long.parseLong(money); } catch (NumberFormatException ignored) {}
            int maxUseVal = 1;
            try { if (maxUse != null) maxUseVal = Integer.parseInt(maxUse); } catch (NumberFormatException ignored) {}
            int bundleIdVal = 0;
            try { if (bundleId != null) bundleIdVal = Integer.parseInt(bundleId); } catch (NumberFormatException ignored) {}

            String sql = "INSERT INTO gift_codes (giftcode, type, money, max_use, `from`, exprired, event, user_name, bundle_id, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, code);
                    ps.setInt(2, typeVal);
                    ps.setLong(3, moneyVal);
                    ps.setInt(4, maxUseVal);
                    ps.setString(5, fromDate != null ? fromDate : "");
                    ps.setString(6, expiredDate != null ? expiredDate : "");
                    ps.setString(7, event != null ? event : "");
                    ps.setString(8, userName != null ? userName : "");
                    ps.setInt(9, bundleIdVal);
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
            logger.error("CreateGiftCodeProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
