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

/**
 * c=164 - New Player Stats: count new users registered in date range, grouped by date.
 * Params: ts (start date), te (end date)
 */
public class NewPlayerStatsProcessor implements BaseProcessor<HttpServletRequest, String> {
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
            String segment = request.getParameter("segment");

            JSONArray dataArray = new JSONArray();
            int total = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                String sql;
                if ("hour".equalsIgnoreCase(segment)) {
                    sql = "SELECT DATE_FORMAT(create_time, '%Y-%m-%d %H:00:00') AS reg_date, COUNT(*) AS cnt " +
                            "FROM users WHERE create_time >= ? AND create_time <= ? AND is_bot = 0 " +
                            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d %H:00:00') ORDER BY reg_date ASC";
                } else {
                    sql = "SELECT DATE(create_time) AS reg_date, COUNT(*) AS cnt " +
                            "FROM users WHERE create_time >= ? AND create_time <= ? AND is_bot = 0 " +
                            "GROUP BY DATE(create_time) ORDER BY reg_date ASC";
                }
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, startDate + " 00:00:00");
                    ps.setString(2, endDate + " 23:59:59");
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("date", rs.getString("reg_date"));
                            int count = rs.getInt("cnt");
                            item.put("count", count);
                            total += count;
                            dataArray.put(item);
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
            logger.error("NewPlayerStatsProcessor error", e);
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
