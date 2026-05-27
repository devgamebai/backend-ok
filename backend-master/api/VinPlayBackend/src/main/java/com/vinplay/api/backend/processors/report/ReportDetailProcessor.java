package com.vinplay.api.backend.processors.report;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportDetailProcessor implements BaseProcessor<HttpServletRequest, String> {
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

            JSONArray dataArray = new JSONArray();
            int total = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                String sql = "SELECT action_name, money_win, money_lost, money_other, fee, date " +
                        "FROM report_money_daily WHERE date >= ? AND date <= ? " +
                        "ORDER BY date DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, startDate);
                    ps.setString(2, endDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("action_name", rs.getString("action_name") != null ? rs.getString("action_name") : "");
                            item.put("money_win", rs.getLong("money_win"));
                            item.put("money_lost", rs.getLong("money_lost"));
                            item.put("money_other", rs.getLong("money_other"));
                            item.put("fee", rs.getLong("fee"));
                            item.put("date", rs.getString("date") != null ? rs.getString("date") : "");
                            dataArray.put(item);
                            total++;
                        }
                    }
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", dataArray);
            response.put("total", total);
                response.put("totalRecords", total);
        } catch (Exception e) {
            logger.error("ReportDetailProcessor error", e);
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
