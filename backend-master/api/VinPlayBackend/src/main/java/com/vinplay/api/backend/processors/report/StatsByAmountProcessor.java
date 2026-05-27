package com.vinplay.api.backend.processors.report;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * c=165 - Stats by Amount: count users with balance >= amount, plus deposit/withdrawal counts.
 * Params: ts (start date), te (end date), pri (minimum balance amount)
 */
public class StatsByAmountProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("report");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            String priStr = request.getParameter("pri");

            long minAmount = 0;
            if (priStr != null && !priStr.isEmpty()) {
                try { minAmount = Long.parseLong(priStr); } catch (NumberFormatException ignored) {}
            }

            String startDate = null;
            String endDate = null;
            if (startTime != null && !startTime.isEmpty()) {
                startDate = parseDate(startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDate = parseDate(endTime);
            }

            JSONObject data = new JSONObject();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count users with vin >= minAmount (non-bot)
                String sqlUsersAbove = "SELECT COUNT(*) AS cnt FROM users WHERE vin >= ? AND is_bot = 0";
                try (PreparedStatement ps = conn.prepareStatement(sqlUsersAbove)) {
                    ps.setLong(1, minAmount);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            data.put("users_above_amount", rs.getInt("cnt"));
                        }
                    }
                }

                // Count total non-bot users
                String sqlTotalUsers = "SELECT COUNT(*) AS cnt FROM users WHERE is_bot = 0";
                try (PreparedStatement ps = conn.prepareStatement(sqlTotalUsers)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            data.put("total_users", rs.getInt("cnt"));
                        }
                    }
                }

                // Count deposits in date range
                if (startDate != null && endDate != null) {
                    String sqlDeposits = "SELECT COUNT(*) AS cnt FROM deposit_transactions WHERE created_at >= ? AND created_at <= ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlDeposits)) {
                        ps.setString(1, startDate + " 00:00:00");
                        ps.setString(2, endDate + " 23:59:59");
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                data.put("total_deposits", rs.getInt("cnt"));
                            }
                        }
                    }

                    // Count withdrawals in date range
                    String sqlWithdrawals = "SELECT COUNT(*) AS cnt FROM bank_withdrawals WHERE created_at >= ? AND created_at <= ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlWithdrawals)) {
                        ps.setString(1, startDate + " 00:00:00");
                        ps.setString(2, endDate + " 23:59:59");
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                data.put("total_withdrawals", rs.getInt("cnt"));
                            }
                        }
                    }
                } else {
                    data.put("total_deposits", 0);
                    data.put("total_withdrawals", 0);
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("StatsByAmountProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5");
        }
        return response.toString();
    }

    private String parseDate(String input) {
        if (input == null || input.isEmpty()) return input;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            Date d = sdf.parse(input);
            return sdf.format(d);
        } catch (Exception ignored) {}
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("dd-MM-yyyy");
            sdfIn.setLenient(false);
            Date d = sdfIn.parse(input);
            SimpleDateFormat sdfOut = new SimpleDateFormat("yyyy-MM-dd");
            return sdfOut.format(d);
        } catch (Exception ignored) {}
        return input;
    }
}
