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
import java.sql.Timestamp;

public class TransactionDetailProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("report");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String tid = request.getParameter("tid");
            String action = request.getParameter("action");

            if (tid == null || tid.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            if (action == null || action.isEmpty()) {
                action = "get";
            }

            if ("get".equalsIgnoreCase(action)) {
                JSONObject data = getTransactionDetail(tid);
                if (data != null) {
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", data);
                } else {
                    response.put("success", false);
                    response.put("errorCode", "1002");
                }
            } else {
                response.put("success", false);
                response.put("errorCode", "1003");
            }
        } catch (Exception e) {
            logger.error("TransactionDetailProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5");
        }
        return response.toString();
    }

    private JSONObject getTransactionDetail(String tid) throws Exception {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            // Try deposit_transactions first
            JSONObject data = queryDeposit(conn, tid);
            if (data != null) {
                data.put("type", "deposit");
                return data;
            }
            // Try bank_withdrawals
            data = queryWithdrawal(conn, tid);
            if (data != null) {
                data.put("type", "withdrawal");
                return data;
            }
        }
        return null;
    }

    private JSONObject queryDeposit(Connection conn, String tid) throws Exception {
        String sql = "SELECT id, tx_code, nick_name, amount, status, credit_status, " +
                "user_bank_name, user_bank_number, deposit_type, created_at, processed_at " +
                "FROM deposit_transactions WHERE tx_code = ? OR id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tid);
            long idVal = 0;
            try { idVal = Long.parseLong(tid); } catch (NumberFormatException ignored) {}
            ps.setLong(2, idVal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JSONObject item = new JSONObject();
                    item.put("id", rs.getLong("id"));
                    item.put("tx_code", rs.getString("tx_code") != null ? rs.getString("tx_code") : "");
                    item.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                    item.put("amount", rs.getLong("amount"));
                    item.put("status", rs.getString("status") != null ? rs.getString("status") : "");
                    item.put("credit_status", rs.getString("credit_status") != null ? rs.getString("credit_status") : "");
                    item.put("bank_name", rs.getString("user_bank_name") != null ? rs.getString("user_bank_name") : "");
                    item.put("bank_number", rs.getString("user_bank_number") != null ? rs.getString("user_bank_number") : "");
                    item.put("type", "deposit");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    item.put("created_at", createdAt != null ? createdAt.toString() : "");
                    Timestamp processedAt = rs.getTimestamp("processed_at");
                    item.put("processed_at", processedAt != null ? processedAt.toString() : "");
                    return item;
                }
            }
        }
        return null;
    }

    private JSONObject queryWithdrawal(Connection conn, String tid) throws Exception {
        String sql = "SELECT id, tx_code, nick_name, bank_name, bank_number, customer_name, amount_krw, " +
                "fee_krw, status, admin_by, reject_reason, created_at, processed_at " +
                "FROM bank_withdrawals WHERE tx_code = ? OR id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tid);
            long idVal = 0;
            try { idVal = Long.parseLong(tid); } catch (NumberFormatException ignored) {}
            ps.setLong(2, idVal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JSONObject item = new JSONObject();
                    item.put("id", rs.getLong("id"));
                    item.put("tx_code", rs.getString("tx_code") != null ? rs.getString("tx_code") : "");
                    item.put("nick_name", rs.getString("nick_name") != null ? rs.getString("nick_name") : "");
                    item.put("bank_name", rs.getString("bank_name") != null ? rs.getString("bank_name") : "");
                    item.put("bank_number", rs.getString("bank_number") != null ? rs.getString("bank_number") : "");
                    item.put("amount_krw", rs.getLong("amount_krw"));
                    item.put("status", rs.getString("status") != null ? rs.getString("status") : "");
                    item.put("type", "withdrawal");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    item.put("created_at", createdAt != null ? createdAt.toString() : "");
                    Timestamp processedAt = rs.getTimestamp("processed_at");
                    item.put("processed_at", processedAt != null ? processedAt.toString() : "");
                    return item;
                }
            }
        }
        return null;
    }
}
