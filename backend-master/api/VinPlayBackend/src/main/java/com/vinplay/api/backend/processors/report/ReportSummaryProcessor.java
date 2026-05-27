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

public class ReportSummaryProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("report");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");

            if (startTime == null || startTime.isEmpty() || endTime == null || endTime.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            String startDate = parseDate(startTime);
            String endDate = parseDate(endTime);

            long totalDeposit = 0;
            long totalWithdraw = 0;
            long totalWin = 0;
            long totalLost = 0;
            long totalFee = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Aggregate from report_money_daily
                String sqlReport = "SELECT COALESCE(SUM(money_win),0) AS total_win, " +
                        "COALESCE(SUM(money_lost),0) AS total_lost, " +
                        "COALESCE(SUM(money_other),0) AS total_other, " +
                        "COALESCE(SUM(fee),0) AS total_fee " +
                        "FROM report_money_daily WHERE date >= ? AND date <= ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlReport)) {
                    ps.setString(1, startDate);
                    ps.setString(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalWin = rs.getLong("total_win");
                            totalLost = rs.getLong("total_lost");
                            totalFee = rs.getLong("total_fee");
                        }
                    }
                }

                // Total deposits
                String sqlDeposit = "SELECT COALESCE(SUM(amount),0) AS total_deposit " +
                        "FROM deposit_transactions WHERE status='APPROVED' " +
                        "AND created_at >= ? AND created_at <= ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlDeposit)) {
                    ps.setString(1, startDate + " 00:00:00");
                    ps.setString(2, endDate + " 23:59:59");
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalDeposit = rs.getLong("total_deposit");
                        }
                    }
                }

                // Total withdrawals
                String sqlWithdraw = "SELECT COALESCE(SUM(amount_krw),0) AS total_withdraw " +
                        "FROM bank_withdrawals WHERE status IN ('APPROVED','COMPLETED') " +
                        "AND created_at >= ? AND created_at <= ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlWithdraw)) {
                    ps.setString(1, startDate + " 00:00:00");
                    ps.setString(2, endDate + " 23:59:59");
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalWithdraw = rs.getLong("total_withdraw");
                        }
                    }
                }
            }

            long net = totalDeposit - totalWithdraw + totalWin + totalLost;

            JSONObject data = new JSONObject();
            data.put("total_deposit", totalDeposit);
            data.put("total_withdraw", totalWithdraw);
            data.put("total_win", totalWin);
            data.put("total_lost", totalLost);
            data.put("total_fee", totalFee);
            data.put("net", net);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("ReportSummaryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5");
        }
        return response.toString();
    }

    private String parseDate(String input) {
        if (input == null || input.isEmpty()) return input;
        // Try yyyy-MM-dd first
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            Date d = sdf.parse(input);
            return sdf.format(d);
        } catch (Exception ignored) {}
        // Try dd-MM-yyyy
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
