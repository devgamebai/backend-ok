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
import java.util.ArrayList;
import java.util.List;

/**
 * c=77 -- Search agency profit by month.
 * Params: nn (nickname), ts (time start), te (time end), p (page), month
 */
public class SearchAgencyProfitProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    private static final int PAGE_SIZE = 20;

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            String timeStart = request.getParameter("ts");
            String timeEnd = request.getParameter("te");
            String month = request.getParameter("month");
            int page = 1;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            int offset = (page - 1) * PAGE_SIZE;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (nickname != null && !nickname.isEmpty()) {
                where.append(" AND (nick_name_send LIKE ? OR nick_name_receive LIKE ?)");
                params.add("%" + nickname + "%");
                params.add("%" + nickname + "%");
            }
            if (timeStart != null && !timeStart.isEmpty()) {
                where.append(" AND trans_time >= ?");
                params.add(timeStart);
            }
            if (timeEnd != null && !timeEnd.isEmpty()) {
                where.append(" AND trans_time <= ?");
                params.add(timeEnd);
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                String countSql = "SELECT COUNT(*) FROM log_tranfer_agent" + where;
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) ps.setString(i + 1, String.valueOf(params.get(i)));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Aggregate
                String sumSql = "SELECT COALESCE(SUM(money_send), 0) as total_send, " +
                        "COALESCE(SUM(money_receive), 0) as total_receive, " +
                        "COALESCE(SUM(fee), 0) as total_fee " +
                        "FROM log_tranfer_agent" + where;
                long totalSend = 0, totalReceive = 0, totalFee = 0;
                try (PreparedStatement ps = conn.prepareStatement(sumSql)) {
                    for (int i = 0; i < params.size(); i++) ps.setString(i + 1, String.valueOf(params.get(i)));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalSend = rs.getLong("total_send");
                            totalReceive = rs.getLong("total_receive");
                            totalFee = rs.getLong("total_fee");
                        }
                    }
                }

                // Data
                String dataSql = "SELECT id, transaction_no, agent_level1, nick_name_send, nick_name_receive, " +
                        "money_send, money_receive, fee, status, top_ds, trans_time " +
                        "FROM log_tranfer_agent" + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    for (Object p : params) ps.setString(idx++, String.valueOf(p));
                    ps.setInt(idx++, PAGE_SIZE);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("transaction_no", rs.getString("transaction_no") != null ? rs.getString("transaction_no") : "");
                            item.put("agent_level1", rs.getString("agent_level1") != null ? rs.getString("agent_level1") : "");
                            item.put("nick_name_send", rs.getString("nick_name_send") != null ? rs.getString("nick_name_send") : "");
                            item.put("nick_name_receive", rs.getString("nick_name_receive") != null ? rs.getString("nick_name_receive") : "");
                            item.put("money_send", rs.getLong("money_send"));
                            item.put("money_receive", rs.getLong("money_receive"));
                            item.put("fee", rs.getLong("fee"));
                            item.put("status", rs.getInt("status"));
                            item.put("top_ds", rs.getInt("top_ds"));
                            item.put("trans_time", rs.getString("trans_time") != null ? rs.getString("trans_time") : "");
                            arr.put(item);
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", arr);
                response.put("total", total);
                response.put("totalRecords", total);
                response.put("totalSend", totalSend);
                response.put("totalReceive", totalReceive);
                response.put("totalFee", totalFee);
            }

        } catch (Exception e) {
            logger.error("SearchAgencyProfitProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
