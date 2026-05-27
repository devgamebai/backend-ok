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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WithdrawalListProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("report");
    private static final int PAGE_SIZE = 20;

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickName = request.getParameter("nn");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            String pageStr = request.getParameter("p");
            String status = request.getParameter("st");
            String tid = request.getParameter("tid");

            int page = 1;
            if (pageStr != null && !pageStr.isEmpty()) {
                try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
            }
            if (page < 1) page = 1;
            int offset = (page - 1) * PAGE_SIZE;

            StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (nickName != null && !nickName.isEmpty()) {
                whereClause.append(" AND nick_name = ?");
                params.add(nickName);
            }
            if (status != null && !status.isEmpty()) {
                whereClause.append(" AND status = ?");
                params.add(status);
            }
            if (tid != null && !tid.isEmpty()) {
                whereClause.append(" AND (tx_code = ? OR id = ?)");
                params.add(tid);
                long idVal = 0;
                try { idVal = Long.parseLong(tid); } catch (NumberFormatException ignored) {}
                params.add(idVal);
            }
            if (startTime != null && !startTime.isEmpty()) {
                String startDate = parseDate(startTime);
                whereClause.append(" AND created_at >= ?");
                params.add(startDate + " 00:00:00");
            }
            if (endTime != null && !endTime.isEmpty()) {
                String endDate = parseDate(endTime);
                whereClause.append(" AND created_at <= ?");
                params.add(endDate + " 23:59:59");
            }

            int total = 0;
            JSONArray dataArray = new JSONArray();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                String countSql = "SELECT COUNT(*) FROM bank_withdrawals" + whereClause;
                try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                    setParams(countPs, params);
                    try (ResultSet countRs = countPs.executeQuery()) {
                        if (countRs.next()) {
                            total = countRs.getInt(1);
                        }
                    }
                }

                // Data
                String dataSql = "SELECT id, tx_code, nick_name, bank_name, bank_number, customer_name, " +
                        "amount_krw, fee_krw, status, admin_by, reject_reason, created_at, processed_at " +
                        "FROM bank_withdrawals" + whereClause +
                        " ORDER BY created_at DESC LIMIT ? OFFSET ?";
                List<Object> dataParams = new ArrayList<>(params);
                dataParams.add(PAGE_SIZE);
                dataParams.add(offset);
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    setParams(ps, dataParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getLong("id"));
                            item.put("tx_code", rs.getString("tx_code") != null ? rs.getString("tx_code") : "");
                            item.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                            item.put("bank_name", rs.getString("bank_name") != null ? rs.getString("bank_name") : "");
                            item.put("bank_number", rs.getString("bank_number") != null ? rs.getString("bank_number") : "");
                            item.put("customer_name", rs.getString("customer_name") != null ? rs.getString("customer_name") : "");
                            item.put("amount_krw", rs.getLong("amount_krw"));
                            item.put("amount", rs.getLong("amount_krw"));
                            item.put("fee_krw", rs.getLong("fee_krw"));
                            item.put("status", rs.getString("status") != null ? rs.getString("status") : "");
                            item.put("admin_by", rs.getString("admin_by") != null ? rs.getString("admin_by") : "");
                            Timestamp createdAt = rs.getTimestamp("created_at");
                            item.put("created_at", createdAt != null ? createdAt.toString() : "");
                            Timestamp processedAt = rs.getTimestamp("processed_at");
                            item.put("processed_at", processedAt != null ? processedAt.toString() : "");
                            dataArray.put(item);
                        }
                    }
                }
            }

            // Compute statistic: [countApproved, sumApproved, sumAll]
            String statSql = "SELECT " +
                    "COUNT(CASE WHEN status IN ('APPROVED','COMPLETED') THEN 1 END) as cnt_approved, " +
                    "COALESCE(SUM(CASE WHEN status IN ('APPROVED','COMPLETED') THEN amount_krw ELSE 0 END),0) as sum_approved, " +
                    "COALESCE(SUM(amount_krw),0) as sum_all " +
                    "FROM bank_withdrawals" + whereClause;
            org.json.JSONArray statArray = new org.json.JSONArray();
            try (Connection conn2 = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps2 = conn2.prepareStatement(statSql)) {
                setParams(ps2, params);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        statArray.put(rs2.getLong("cnt_approved"));
                        statArray.put(rs2.getLong("sum_approved"));
                        statArray.put(rs2.getLong("sum_all"));
                    }
                }
            } catch (Exception statErr) {
                statArray.put(0); statArray.put(0); statArray.put(0);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", dataArray);
            response.put("total", total);
            response.put("totalRecords", total);
            response.put("statistic", statArray);
            response.put("page", page);
        } catch (Exception e) {
            logger.error("WithdrawalListProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5");
        }
        return response.toString();
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            Object val = params.get(i);
            if (val instanceof Long) {
                ps.setLong(i + 1, (Long) val);
            } else if (val instanceof Integer) {
                ps.setInt(i + 1, (Integer) val);
            } else {
                ps.setString(i + 1, val.toString());
            }
        }
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
