package com.vinplay.api.backend.processors.deposit;

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
 * c=9615 — List all deposits (bank + crypto) with optional filters.
 * Merges deposit_transactions (bank) and crypto_deposits (crypto) via UNION ALL.
 * Params: status, nn (nickname), ts (start date yyyy-MM-dd), te (end date),
 *         type (bank/crypto/empty=all), page, limit
 */
public class ListAllDepositsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String status    = request.getParameter("status");
            String nickName  = request.getParameter("nn");
            String startTime = request.getParameter("ts");
            String endTime   = request.getParameter("te");
            String type      = request.getParameter("type"); // bank / crypto / empty = all

            int page = 1, limit = 20;
            try { String v = request.getParameter("page");  if (v != null && !v.isEmpty()) page  = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
            try { String v = request.getParameter("limit"); if (v != null && !v.isEmpty()) limit = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
            // Also accept legacy params p/l
            try { String v = request.getParameter("p"); if (v != null && !v.isEmpty()) page = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
            try { String v = request.getParameter("l"); if (v != null && !v.isEmpty()) limit = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;
            int offset = (page - 1) * limit;

            boolean includeBank   = type == null || type.isEmpty() || type.equalsIgnoreCase("bank");
            boolean includeCrypto = type == null || type.isEmpty() || type.equalsIgnoreCase("crypto");

            // Build per-table WHERE + params
            StringBuilder bankWhere   = new StringBuilder(" WHERE 1=1");
            StringBuilder cryptoWhere = new StringBuilder(" WHERE 1=1");
            List<Object> bankP   = new ArrayList<>();
            List<Object> cryptoP = new ArrayList<>();

            if (nickName != null && !nickName.isEmpty()) {
                bankWhere.append(" AND nick_name = ?");   bankP.add(nickName);
                cryptoWhere.append(" AND nick_name = ?"); cryptoP.add(nickName);
            }
            if (status != null && !status.isEmpty()) {
                bankWhere.append(" AND status = ?");   bankP.add(status);
                cryptoWhere.append(" AND status = ?"); cryptoP.add(status);
            }
            if (startTime != null && !startTime.isEmpty()) {
                bankWhere.append(" AND created_at >= ?");   bankP.add(startTime + " 00:00:00");
                cryptoWhere.append(" AND created_at >= ?"); cryptoP.add(startTime + " 00:00:00");
            }
            if (endTime != null && !endTime.isEmpty()) {
                bankWhere.append(" AND created_at <= ?");   bankP.add(endTime + " 23:59:59");
                cryptoWhere.append(" AND created_at <= ?"); cryptoP.add(endTime + " 23:59:59");
            }

            // Assemble UNION
            List<String> parts = new ArrayList<>();
            List<Object> unionParams = new ArrayList<>();

            if (includeBank) {
                parts.add(
                    "SELECT id, tx_code, nick_name, amount, CAST(0 AS DECIMAL(20,6)) AS amount_usdt, " +
                    "user_bank_name AS bank_name, user_bank_number AS bank_number, " +
                    "'' AS address, '' AS tx_hash, " +
                    "status, credit_status, deposit_type, " +
                    "locked_by, processed_by, reject_reason, created_at, processed_at " +
                    "FROM deposit_transactions" + bankWhere
                );
                unionParams.addAll(bankP);
            }
            if (includeCrypto) {
                parts.add(
                    "SELECT id, IFNULL(gateway_tx_id, CONCAT('CRYPTO-',id)) AS tx_code, nick_name, " +
                    "amount_krw AS amount, amount_usdt, " +
                    "'' AS bank_name, '' AS bank_number, " +
                    "address, IFNULL(tx_hash,'') AS tx_hash, " +
                    "status, '' AS credit_status, 'CRYPTO' AS deposit_type, " +
                    "'' AS locked_by, '' AS processed_by, '' AS reject_reason, created_at, " +
                    "NULL AS processed_at " +
                    "FROM crypto_deposits" + cryptoWhere
                );
                unionParams.addAll(cryptoP);
            }

            if (parts.isEmpty()) {
                response.put("success", true);
                response.put("data", new JSONArray());
                response.put("total", 0);
                response.put("page", page);
                return response.toString();
            }

            String unionSql = String.join(" UNION ALL ", parts);

            JSONArray dataArr = new JSONArray();
            int total = 0;
            long sumApproved = 0;
            int countApproved = 0;
            long sumAll = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count + statistics
                String countSql = "SELECT COUNT(*) AS cnt, " +
                    "SUM(CASE WHEN status = 'APPROVED' THEN amount ELSE 0 END) AS sum_approved, " +
                    "SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS cnt_approved, " +
                    "SUM(amount) AS sum_all " +
                    "FROM (" + unionSql + ") _t";
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    setParams(ps, unionParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            total = rs.getInt("cnt");
                            sumApproved = rs.getLong("sum_approved");
                            countApproved = rs.getInt("cnt_approved");
                            sumAll = rs.getLong("sum_all");
                        }
                    }
                }

                // Data
                String dataSql = "SELECT * FROM (" + unionSql + ") _t ORDER BY created_at DESC LIMIT ? OFFSET ?";
                List<Object> dataParams = new ArrayList<>(unionParams);
                dataParams.add(limit);
                dataParams.add(offset);
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    setParams(ps, dataParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id",            rs.getLong("id"));
                            row.put("tx_code",       nvl(rs.getString("tx_code")));
                            row.put("nick_name",     nvl(rs.getString("nick_name")));
                            row.put("deposit_type",  nvl(rs.getString("deposit_type")));
                            row.put("amount",        rs.getLong("amount"));
                            row.put("amount_usdt",   rs.getDouble("amount_usdt"));
                            row.put("bank_name",     nvl(rs.getString("bank_name")));
                            row.put("bank_number",   nvl(rs.getString("bank_number")));
                            row.put("address",       nvl(rs.getString("address")));
                            row.put("tx_hash",       nvl(rs.getString("tx_hash")));
                            row.put("status",        nvl(rs.getString("status")));
                            row.put("credit_status", nvl(rs.getString("credit_status")));
                            row.put("locked_by",     nvl(rs.getString("locked_by")));
                            row.put("processed_by",  nvl(rs.getString("processed_by")));
                            row.put("reject_reason", nvl(rs.getString("reject_reason")));
                            row.put("created_at",    nvl(rs.getString("created_at")));
                            row.put("processed_at",  nvl(rs.getString("processed_at")));
                            dataArr.put(row);
                        }
                    }
                }
            }

            response.put("success", true);
            response.put("data", dataArr);
            response.put("total", total);
            response.put("totalRecords", total);
            response.put("statistic", new JSONArray().put(countApproved).put(sumApproved).put(sumAll));
            response.put("page", page);
            response.put("limit", limit);

        } catch (Exception e) {
            logger.error("ListAllDepositsProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            Object val = params.get(i);
            if (val instanceof Long)    ps.setLong(i + 1, (Long) val);
            else if (val instanceof Integer) ps.setInt(i + 1, (Integer) val);
            else ps.setString(i + 1, val.toString());
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
