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

/**
 * c=719 - Money Log By Mobile: find user by mobile, then query their deposit/withdrawal transactions.
 * Params: m (mobile), st (status), ts/te (date range), p (page), rid (tx_code)
 */
public class MoneyLogByMobileProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("report");
    private static final int PAGE_SIZE = 20;

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String mobile = request.getParameter("m");
            String status = request.getParameter("st");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            String pageStr = request.getParameter("p");
            String txCode = request.getParameter("rid");

            if (mobile == null || mobile.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            int page = 1;
            if (pageStr != null && !pageStr.isEmpty()) {
                try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
            }
            if (page < 1) page = 1;
            int offset = (page - 1) * PAGE_SIZE;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Find user by mobile
                String nickName = null;
                long userId = 0;
                String sqlUser = "SELECT id, nick_name FROM users WHERE mobile = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                    ps.setString(1, mobile);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getLong("id");
                            nickName = rs.getString("nick_name");
                        }
                    }
                }

                if (nickName == null) {
                    response.put("success", false);
                    response.put("errorCode", "1002");
                    response.put("message", "User not found");
                    return response.toString();
                }

                // Build WHERE for deposits
                StringBuilder depositWhere = new StringBuilder(" WHERE nick_name = ?");
                List<Object> depositParams = new ArrayList<>();
                depositParams.add(nickName);

                StringBuilder withdrawalWhere = new StringBuilder(" WHERE nick_name = ?");
                List<Object> withdrawalParams = new ArrayList<>();
                withdrawalParams.add(nickName);

                if (status != null && !status.isEmpty()) {
                    depositWhere.append(" AND status = ?");
                    depositParams.add(status);
                    withdrawalWhere.append(" AND status = ?");
                    withdrawalParams.add(status);
                }
                if (txCode != null && !txCode.isEmpty()) {
                    depositWhere.append(" AND tx_code = ?");
                    depositParams.add(txCode);
                    withdrawalWhere.append(" AND tx_code = ?");
                    withdrawalParams.add(txCode);
                }
                if (startTime != null && !startTime.isEmpty()) {
                    String startDate = parseDate(startTime);
                    depositWhere.append(" AND created_at >= ?");
                    depositParams.add(startDate + " 00:00:00");
                    withdrawalWhere.append(" AND created_at >= ?");
                    withdrawalParams.add(startDate + " 00:00:00");
                }
                if (endTime != null && !endTime.isEmpty()) {
                    String endDate = parseDate(endTime);
                    depositWhere.append(" AND created_at <= ?");
                    depositParams.add(endDate + " 23:59:59");
                    withdrawalWhere.append(" AND created_at <= ?");
                    withdrawalParams.add(endDate + " 23:59:59");
                }

                // Count total
                int total = 0;
                String countSql = "SELECT (SELECT COUNT(*) FROM deposit_transactions" + depositWhere + ") + " +
                        "(SELECT COUNT(*) FROM bank_withdrawals" + withdrawalWhere + ") AS total";
                List<Object> allCountParams = new ArrayList<>();
                allCountParams.addAll(depositParams);
                allCountParams.addAll(withdrawalParams);
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    setParams(ps, allCountParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            total = rs.getInt("total");
                        }
                    }
                }

                // UNION query
                String dataSql = "SELECT * FROM (" +
                        "SELECT id, tx_code, nick_name, amount AS amount, 'DEPOSIT' AS type, status, " +
                        "user_bank_name AS bank_name, user_bank_number AS bank_number, created_at " +
                        "FROM deposit_transactions" + depositWhere +
                        " UNION ALL " +
                        "SELECT id, tx_code, nick_name, amount_krw AS amount, 'WITHDRAWAL' AS type, status, " +
                        "bank_name, bank_number, created_at " +
                        "FROM bank_withdrawals" + withdrawalWhere +
                        ") AS combined ORDER BY created_at DESC LIMIT ? OFFSET ?";

                List<Object> allDataParams = new ArrayList<>();
                allDataParams.addAll(depositParams);
                allDataParams.addAll(withdrawalParams);
                allDataParams.add(PAGE_SIZE);
                allDataParams.add(offset);

                JSONArray dataArray = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    setParams(ps, allDataParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getLong("id"));
                            item.put("tx_code", rs.getString("tx_code") != null ? rs.getString("tx_code") : "");
                            item.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                            item.put("amount", rs.getLong("amount"));
                            item.put("type", rs.getString("type"));
                            item.put("status", rs.getString("status") != null ? rs.getString("status") : "");
                            item.put("bank_name", rs.getString("bank_name") != null ? rs.getString("bank_name") : "");
                            item.put("bank_number", rs.getString("bank_number") != null ? rs.getString("bank_number") : "");
                            Timestamp createdAt = rs.getTimestamp("created_at");
                            item.put("created_at", createdAt != null ? createdAt.toString() : "");
                            dataArray.put(item);
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", dataArray);
                response.put("total", total);
                response.put("totalRecords", total);
                response.put("page", page);
                response.put("nick_name", nickName);
                response.put("mobile", mobile);
            }
        } catch (Exception e) {
            logger.error("MoneyLogByMobileProcessor error", e);
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
