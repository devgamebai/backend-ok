package com.vinplay.api.backend.processors.hoantra;

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
 * c=8900 -- Calculate hoan tra (cashback/rebate) for users.
 * Params: nn (optional nickname), ts (time start), te (time end)
 * Calculates from log_hoan_tra data and marks as processed.
 */
public class CalculateDataHoanTraProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            String timeStart = request.getParameter("ts");
            String timeEnd = request.getParameter("te");

            int processed = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count records to process
                StringBuilder where = new StringBuilder(" WHERE send_success IS NULL OR send_success = 0");
                if (nickname != null && !nickname.isEmpty()) {
                    where.append(" AND nick_name = ?");
                }
                if (timeStart != null && !timeStart.isEmpty()) {
                    where.append(" AND time >= ?");
                }
                if (timeEnd != null && !timeEnd.isEmpty()) {
                    where.append(" AND time <= ?");
                }

                String countSql = "SELECT COUNT(*) FROM log_hoan_tra" + where;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    int idx = 1;
                    if (nickname != null && !nickname.isEmpty()) ps.setString(idx++, nickname);
                    if (timeStart != null && !timeStart.isEmpty()) ps.setString(idx++, timeStart);
                    if (timeEnd != null && !timeEnd.isEmpty()) ps.setString(idx++, timeEnd);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) processed = rs.getInt(1);
                    }
                }

                // Copy to histories and mark as sent
                if (processed > 0) {
                    String insertSql = "INSERT INTO log_hoan_tra_histories " +
                            "(nick_name, time, vip_point, total_money_sport, hoan_tra_sport, " +
                            "total_money_casino, hoan_tra_casino, total_money_game, hoan_tra_game, " +
                            "vip_index, send_success, message) " +
                            "SELECT nick_name, time, vip_point, total_money_sport, hoan_tra_sport, " +
                            "total_money_casino, hoan_tra_casino, total_money_game, hoan_tra_game, " +
                            "vip_index, 1, 'Calculated'" +
                            " FROM log_hoan_tra" + where;
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        int idx = 1;
                        if (nickname != null && !nickname.isEmpty()) ps.setString(idx++, nickname);
                        if (timeStart != null && !timeStart.isEmpty()) ps.setString(idx++, timeStart);
                        if (timeEnd != null && !timeEnd.isEmpty()) ps.setString(idx++, timeEnd);
                        ps.executeUpdate();
                    }

                    // Mark as processed
                    String updateSql = "UPDATE log_hoan_tra SET send_success = 1, message = 'Calculated'" + where.toString().replace("WHERE", "WHERE 1=1 AND (") + ")";
                    // Simpler approach: update matching records
                    StringBuilder updateWhere = new StringBuilder(" WHERE send_success IS NULL OR send_success = 0");
                    if (nickname != null && !nickname.isEmpty()) {
                        updateWhere.append(" AND nick_name = ?");
                    }
                    if (timeStart != null && !timeStart.isEmpty()) {
                        updateWhere.append(" AND time >= ?");
                    }
                    if (timeEnd != null && !timeEnd.isEmpty()) {
                        updateWhere.append(" AND time <= ?");
                    }
                    String updateSqlClean = "UPDATE log_hoan_tra SET send_success = 1, message = 'Calculated'" + updateWhere;
                    try (PreparedStatement ps = conn.prepareStatement(updateSqlClean)) {
                        int idx = 1;
                        if (nickname != null && !nickname.isEmpty()) ps.setString(idx++, nickname);
                        if (timeStart != null && !timeStart.isEmpty()) ps.setString(idx++, timeStart);
                        if (timeEnd != null && !timeEnd.isEmpty()) ps.setString(idx++, timeEnd);
                        ps.executeUpdate();
                    }
                }
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("processed", processed);

        } catch (Exception e) {
            logger.error("CalculateDataHoanTraProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
