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

public class UserAggregateProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nn = request.getParameter("nn");

            if (nn == null || nn.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            JSONObject data = new JSONObject();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Deposits
                int depositCount = 0;
                long depositTotal = 0;
                try {
                    String sql = "SELECT COUNT(*) as cnt, COALESCE(SUM(money),0) as total FROM topup WHERE nick_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nn);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                depositCount = rs.getInt("cnt");
                                depositTotal = rs.getLong("total");
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("topup table query failed, returning zeros", e);
                }

                // Withdrawals
                int withdrawCount = 0;
                long withdrawTotal = 0;
                try {
                    String sql = "SELECT COUNT(*) as cnt, COALESCE(SUM(money),0) as total FROM history_applyfor WHERE nick_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nn);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                withdrawCount = rs.getInt("cnt");
                                withdrawTotal = rs.getLong("total");
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("history_applyfor table query failed, returning zeros", e);
                }

                JSONObject deposits = new JSONObject();
                deposits.put("count", depositCount);
                deposits.put("total", depositTotal);

                JSONObject withdrawals = new JSONObject();
                withdrawals.put("count", withdrawCount);
                withdrawals.put("total", withdrawTotal);

                data.put("deposits", deposits);
                data.put("withdrawals", withdrawals);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data.toString());
        } catch (Exception e) {
            logger.error("UserAggregateProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
