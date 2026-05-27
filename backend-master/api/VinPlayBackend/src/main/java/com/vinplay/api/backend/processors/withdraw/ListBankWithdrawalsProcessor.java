package com.vinplay.api.backend.processors.withdraw;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
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
 * c=9642 — List all withdrawals (bank + crypto) with optional filters (admin).
 * Params: status, nn (nickname), ts (start date yyyy-MM-dd), te (end date), type (bank/crypto/empty=all), page, limit
 */
public class ListBankWithdrawalsProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            if (accessToken == null || accessToken.isEmpty()) accessToken = request.getParameter("aat");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            String status   = request.getParameter("status");
            String nickName = request.getParameter("nn");
            String startTime = request.getParameter("ts");
            String endTime   = request.getParameter("te");
            String type      = request.getParameter("type"); // bank / crypto / empty = all

            int page = 1, limit = 20;
            try { String v = request.getParameter("page");  if (v != null && !v.isEmpty()) page  = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
            try { String v = request.getParameter("limit"); if (v != null && !v.isEmpty()) limit = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;
            int offset = (page - 1) * limit;

            boolean includeBank   = type == null || type.isEmpty() || type.equalsIgnoreCase("bank");
            boolean includeCrypto = type == null || type.isEmpty() || type.equalsIgnoreCase("crypto");
            boolean includeAdminRevoke = type == null || type.isEmpty()
                    || type.equalsIgnoreCase("admin_revoke") || type.equalsIgnoreCase("admin");

            // Build per-table WHERE + params
            StringBuilder bankWhere   = new StringBuilder(" WHERE 1=1");
            StringBuilder cryptoWhere = new StringBuilder(" WHERE 1=1");
            StringBuilder adminWhere  = new StringBuilder(" WHERE type='ADMIN_REVOKE' AND direction='DEBIT'");
            List<Object> bankP   = new ArrayList<>();
            List<Object> cryptoP = new ArrayList<>();
            List<Object> adminP  = new ArrayList<>();

            if (nickName != null && !nickName.isEmpty()) {
                bankWhere.append(" AND nick_name = ?");   bankP.add(nickName);
                cryptoWhere.append(" AND nick_name = ?"); cryptoP.add(nickName);
                adminWhere.append(" AND agent_nickname = ?"); adminP.add(nickName);
            }
            if (status != null && !status.isEmpty()) {
                bankWhere.append(" AND status = ?");   bankP.add(status);
                cryptoWhere.append(" AND status = ?"); cryptoP.add(status);
                if (!"APPROVED".equalsIgnoreCase(status)) {
                    includeAdminRevoke = false;
                }
            }
            if (startTime != null && !startTime.isEmpty()) {
                bankWhere.append(" AND created_at >= ?");   bankP.add(startTime + " 00:00:00");
                cryptoWhere.append(" AND created_at >= ?"); cryptoP.add(startTime + " 00:00:00");
                adminWhere.append(" AND created_at >= ?");  adminP.add(startTime + " 00:00:00");
            }
            if (endTime != null && !endTime.isEmpty()) {
                bankWhere.append(" AND created_at <= ?");   bankP.add(endTime + " 23:59:59");
                cryptoWhere.append(" AND created_at <= ?"); cryptoP.add(endTime + " 23:59:59");
                adminWhere.append(" AND created_at <= ?");  adminP.add(endTime + " 23:59:59");
            }

            // Assemble UNION
            List<String> parts = new ArrayList<>();
            List<Object> unionParams = new ArrayList<>();
            JSONArray dataArr = new JSONArray();

            if (includeBank) {
                parts.add(
                    "SELECT id, tx_code, nick_name, bank_name, bank_number, customer_name, " +
                    "amount_krw, fee_krw, CAST(0 AS DECIMAL(20,6)) AS amount_usdt, " +
                    "'' AS to_address, '' AS gateway_tx_id, " +
                    "status, admin_by, reject_reason, created_at, processed_at, 'bank' AS type " +
                    "FROM bank_withdrawals" + bankWhere
                );
                unionParams.addAll(bankP);
            }
            if (includeCrypto) {
                parts.add(
                    "SELECT id, tx_code, nick_name, '' AS bank_name, '' AS bank_number, '' AS customer_name, " +
                    "amount_krw, 0 AS fee_krw, amount_usdt, " +
                    "to_address, IFNULL(gateway_tx_id,'') AS gateway_tx_id, " +
                    "status, admin_by, reject_reason, created_at, processed_at, 'crypto' AS type " +
                    "FROM crypto_withdrawals" + cryptoWhere
                );
                unionParams.addAll(cryptoP);
            }
            if (includeAdminRevoke) {
                parts.add(
                    "SELECT id, CONCAT('ADMINREVOKE-',id) AS tx_code, agent_nickname AS nick_name, " +
                    "'Admin Credit Wallet' AS bank_name, '' AS bank_number, '' AS customer_name, " +
                    "amount AS amount_krw, 0 AS fee_krw, CAST(0 AS DECIMAL(20,6)) AS amount_usdt, " +
                    "'' AS to_address, '' AS gateway_tx_id, " +
                    "'APPROVED' AS status, IFNULL(related_user,'') AS admin_by, IFNULL(note,'') AS reject_reason, " +
                    "created_at, created_at AS processed_at, 'admin_revoke' AS type " +
                    "FROM credit_wallet_transactions" + adminWhere
                );
                unionParams.addAll(adminP);
            }

            if (parts.isEmpty()) {
                response.put("success", true);
                response.put("data", dataArr);
                response.put("total", 0);
                response.put("totalRecords", 0);
                response.put("statistic", new JSONArray().put(0).put(0).put(0));
                response.put("page", page);
                return response.toString();
            }

            String unionSql = String.join(" UNION ALL ", parts);

            int total = 0;
            long sumApproved = 0;
            int countApproved = 0;
            long sumAll = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count + statistics
                String countSql = "SELECT COUNT(*) AS cnt, " +
                    "SUM(CASE WHEN status IN ('APPROVED','COMPLETED') THEN amount_krw ELSE 0 END) AS sum_approved, " +
                    "SUM(CASE WHEN status IN ('APPROVED','COMPLETED') THEN 1 ELSE 0 END) AS cnt_approved, " +
                    "SUM(amount_krw) AS sum_all " +
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
                            row.put("id",           rs.getLong("id"));
                            row.put("tx_code",      nvl(rs.getString("tx_code")));
                            row.put("nick_name",    nvl(rs.getString("nick_name")));
                            row.put("type",         nvl(rs.getString("type")));
                            row.put("bank_name",    nvl(rs.getString("bank_name")));
                            row.put("bank_number",  nvl(rs.getString("bank_number")));
                            row.put("customer_name",nvl(rs.getString("customer_name")));
                            row.put("amount_krw",   rs.getLong("amount_krw"));
                            row.put("fee_krw",      rs.getLong("fee_krw"));
                            row.put("amount_usdt",  rs.getDouble("amount_usdt"));
                            row.put("to_address",   nvl(rs.getString("to_address")));
                            row.put("gateway_tx_id",nvl(rs.getString("gateway_tx_id")));
                            row.put("status",       nvl(rs.getString("status")));
                            row.put("admin_by",     nvl(rs.getString("admin_by")));
                            row.put("reject_reason",nvl(rs.getString("reject_reason")));
                            row.put("created_at",   nvl(rs.getString("created_at")));
                            row.put("processed_at", nvl(rs.getString("processed_at")));
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

        } catch (Exception e) {
            logger.error("ListBankWithdrawalsProcessor error", e);
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
