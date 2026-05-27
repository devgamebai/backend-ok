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
 * c=115 - Bank Report: combined deposit_transactions + bank_withdrawals search.
 * Params: nn (nick_name), co (status), ts/te (date range), tid (tx_code), p (page)
 */
public class BankReportProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("report");
    private static final int PAGE_SIZE = 20;

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickName = request.getParameter("nn");
            String status = request.getParameter("co");
            String startTime = request.getParameter("ts");
            String endTime = request.getParameter("te");
            String txCode = request.getParameter("tid");
            String pageStr = request.getParameter("p");

            int page = 1;
            if (pageStr != null && !pageStr.isEmpty()) {
                try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
            }
            if (page < 1) page = 1;
            int offset = (page - 1) * PAGE_SIZE;

            // Build WHERE clauses for both tables
            StringBuilder depositWhere = new StringBuilder(" WHERE 1=1");
            StringBuilder withdrawalWhere = new StringBuilder(" WHERE 1=1");
            List<Object> depositParams = new ArrayList<>();
            List<Object> withdrawalParams = new ArrayList<>();

            if (nickName != null && !nickName.isEmpty()) {
                depositWhere.append(" AND nick_name = ?");
                depositParams.add(nickName);
                withdrawalWhere.append(" AND nick_name = ?");
                withdrawalParams.add(nickName);
            }
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

            int total = 0;
            JSONArray dataArray = new JSONArray();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count from both tables
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

                // UNION query for data
                String dataSql = "SELECT * FROM (" +
                        "SELECT id, tx_code, nick_name, amount AS amount, 'DEPOSIT' AS type, status, " +
                        "user_bank_name AS bank_name, user_bank_number AS bank_number, " +
                        "processed_by AS admin_by, created_at " +
                        "FROM deposit_transactions" + depositWhere +
                        " UNION ALL " +
                        "SELECT id, tx_code, nick_name, amount_krw AS amount, 'WITHDRAWAL' AS type, status, " +
                        "bank_name, bank_number, " +
                        "admin_by, created_at " +
                        "FROM bank_withdrawals" + withdrawalWhere +
                        ") AS combined ORDER BY created_at DESC LIMIT ? OFFSET ?";

                List<Object> allDataParams = new ArrayList<>();
                allDataParams.addAll(depositParams);
                allDataParams.addAll(withdrawalParams);
                allDataParams.add(PAGE_SIZE);
                allDataParams.add(offset);

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
                            item.put("admin_by", rs.getString("admin_by") != null ? rs.getString("admin_by") : "");
                            Timestamp createdAt = rs.getTimestamp("created_at");
                            item.put("created_at", createdAt != null ? createdAt.toString() : "");
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
            response.put("page", page);
        } catch (Exception e) {
            logger.error("BankReportProcessor error", e);
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
