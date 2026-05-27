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
 * c=9104 — Deposit log (bank + crypto merged).
 * Params: nn, st (status), ts, te, p (page), type (bank/crypto/empty=all)
 */
public class DepositLogProcessor implements BaseProcessor<HttpServletRequest, String> {
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
            String type = request.getParameter("type"); // bank / crypto / empty = all

            int page = 1;
            if (pageStr != null && !pageStr.isEmpty()) {
                try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
            }
            if (page < 1) page = 1;
            int offset = (page - 1) * PAGE_SIZE;

            boolean includeBank   = type == null || type.isEmpty() || type.equalsIgnoreCase("bank");
            boolean includeCrypto = type == null || type.isEmpty() || type.equalsIgnoreCase("crypto");
            // 2026-05-15 SCOPE CORRECTION: admin top-ups in this report must be
            // those to the PLAYER MAIN WALLET (users.vin) only. The earlier
            // SUN-1329 wiring pulled from credit_wallet_transactions which is
            // the AGENT credit wallet — different concept, different audience.
            // Operator confirmed deposit-history should mirror MAIN wallet flow
            // only (bank + crypto + admin top-up-to-vin), excluding agent
            // credit/agency-wallet movements. Source now: vinplay_admin.log_admin
            // WHERE money_type='vin' AND status=1 (the "Cộng tiền Admin" log).
            boolean includeAdminTopup = type == null || type.isEmpty()
                    || type.equalsIgnoreCase("admin_topup") || type.equalsIgnoreCase("admin");

            // Build per-table WHERE clauses
            // 2026-05-15 followup: deposit_transactions table holds both BANK
            // and CREDIT_WALLET deposit_type rows. Without the explicit type
            // pin, CREDIT_WALLET rows (agent-via-credit-wallet payments)
            // leaked into the main-wallet deposit-history report. Force the
            // bank UNION to BANK-only.
            StringBuilder bankWhere   = new StringBuilder(" WHERE deposit_type='BANK'");
            StringBuilder cryptoWhere = new StringBuilder(" WHERE 1=1");
            StringBuilder adminWhere  = new StringBuilder(" WHERE money_type='vin' AND `status`='1'");
            List<Object> bankP   = new ArrayList<>();
            List<Object> cryptoP = new ArrayList<>();
            List<Object> adminP  = new ArrayList<>();

            if (nickName != null && !nickName.isEmpty()) {
                bankWhere.append(" AND nick_name = ?");   bankP.add(nickName);
                cryptoWhere.append(" AND nick_name = ?"); cryptoP.add(nickName);
                adminWhere.append(" AND account_name = ?"); adminP.add(nickName);
            }
            if (status != null && !status.isEmpty()) {
                bankWhere.append(" AND status = ?");   bankP.add(status);
                cryptoWhere.append(" AND status = ?"); cryptoP.add(status);
                // Admin topup rows are always treated as APPROVED — skip
                // when caller asks for any non-APPROVED status.
                if (!"APPROVED".equalsIgnoreCase(status)) includeAdminTopup = false;
            }
            if (startTime != null && !startTime.isEmpty()) {
                String d = parseDate(startTime);
                bankWhere.append(" AND created_at >= ?");   bankP.add(d + " 00:00:00");
                cryptoWhere.append(" AND created_at >= ?"); cryptoP.add(d + " 00:00:00");
                adminWhere.append(" AND `timestamp` >= ?"); adminP.add(d + " 00:00:00");
            }
            if (endTime != null && !endTime.isEmpty()) {
                String d = parseDate(endTime);
                bankWhere.append(" AND created_at <= ?");   bankP.add(d + " 23:59:59");
                cryptoWhere.append(" AND created_at <= ?"); cryptoP.add(d + " 23:59:59");
                adminWhere.append(" AND `timestamp` <= ?"); adminP.add(d + " 23:59:59");
            }

            // Build UNION SQL
            List<String> parts = new ArrayList<>();
            List<Object> unionParams = new ArrayList<>();

            if (includeBank) {
                parts.add(
                    "SELECT id, tx_code, nick_name, amount, CAST(0 AS DECIMAL(20,6)) AS amount_usdt, " +
                    "deposit_type, status, credit_status, " +
                    "user_bank_name AS bank_name, user_bank_number AS bank_number, " +
                    "'' AS address, '' AS tx_hash, " +
                    "processed_by, created_at, processed_at " +
                    "FROM deposit_transactions" + bankWhere
                );
                unionParams.addAll(bankP);
            }
            if (includeCrypto) {
                parts.add(
                    "SELECT id, IFNULL(gateway_tx_id, CONCAT('CRYPTO-',id)) AS tx_code, nick_name, " +
                    "amount_krw AS amount, amount_usdt, " +
                    "'CRYPTO' AS deposit_type, status, '' AS credit_status, " +
                    "'' AS bank_name, '' AS bank_number, " +
                    "address, IFNULL(tx_hash,'') AS tx_hash, " +
                    "'' AS processed_by, created_at, NULL AS processed_at " +
                    "FROM crypto_deposits" + cryptoWhere
                );
                unionParams.addAll(cryptoP);
            }
            if (includeAdminTopup) {
                // Source: vinplay_admin.log_admin — the canonical "Cộng tiền
                // Admin" trail for player MAIN wallet (users.vin). The earlier
                // credit_wallet_transactions UNION was AGENT wallet — wrong
                // scope for a player deposit-history report. bank_name carries
                // a friendly label, address surfaces the admin's free-text
                // reason so the operator can see audit context inline.
                parts.add(
                    "SELECT id, CONCAT('ADMINTOPUP-',id) AS tx_code, account_name AS nick_name, " +
                    "money AS amount, CAST(0 AS DECIMAL(20,6)) AS amount_usdt, " +
                    "'ADMIN_TOPUP' AS deposit_type, 'APPROVED' AS status, 'CREDITED' AS credit_status, " +
                    "'Admin Top-up' AS bank_name, '' AS bank_number, " +
                    "IFNULL(reason,'') AS address, '' AS tx_hash, " +
                    "IFNULL(username,'') AS processed_by, `timestamp` AS created_at, `timestamp` AS processed_at " +
                    "FROM vinplay_admin.log_admin" + adminWhere
                );
                unionParams.addAll(adminP);
            }

            if (parts.isEmpty()) {
                response.put("success", true);
                response.put("data", new JSONArray());
                response.put("total", 0);
                response.put("totalRecords", 0);
                response.put("statistic", new JSONArray().put(0).put(0).put(0));
                return response.toString();
            }

            String unionSql = String.join(" UNION ALL ", parts);
            int total = 0;
            JSONArray dataArray = new JSONArray();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                String countSql = "SELECT COUNT(*) FROM (" + unionSql + ") _t";
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    setParams(ps, unionParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT * FROM (" + unionSql + ") _t ORDER BY created_at DESC LIMIT ? OFFSET ?";
                List<Object> dataParams = new ArrayList<>(unionParams);
                dataParams.add(PAGE_SIZE);
                dataParams.add(offset);
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    setParams(ps, dataParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getLong("id"));
                            item.put("tx_code", nvl(rs.getString("tx_code")));
                            item.put("nick_name", nvl(rs.getString("nick_name")));
                            item.put("amount", rs.getLong("amount"));
                            item.put("amount_usdt", rs.getDouble("amount_usdt"));
                            item.put("deposit_type", nvl(rs.getString("deposit_type")));
                            item.put("status", nvl(rs.getString("status")));
                            item.put("credit_status", nvl(rs.getString("credit_status")));
                            item.put("bank_name", nvl(rs.getString("bank_name")));
                            item.put("bank_number", nvl(rs.getString("bank_number")));
                            item.put("address", nvl(rs.getString("address")));
                            item.put("tx_hash", nvl(rs.getString("tx_hash")));
                            item.put("processed_by", nvl(rs.getString("processed_by")));
                            Timestamp createdAt = rs.getTimestamp("created_at");
                            item.put("created_at", createdAt != null ? createdAt.toString() : "");
                            Timestamp processedAt = rs.getTimestamp("processed_at");
                            item.put("processed_at", processedAt != null ? processedAt.toString() : "");
                            dataArray.put(item);
                        }
                    }
                }

                // Statistic: [countSuccessful, sumSuccessful, sumAll]
                String statSql = "SELECT " +
                    "COUNT(CASE WHEN status IN ('APPROVED','COMPLETED','SUCCESS') THEN 1 END) AS cnt_approved, " +
                    "COALESCE(SUM(CASE WHEN status IN ('APPROVED','COMPLETED','SUCCESS') THEN amount ELSE 0 END),0) AS sum_approved, " +
                    "COALESCE(SUM(amount),0) AS sum_all " +
                    "FROM (" + unionSql + ") _t";
                try (PreparedStatement ps = conn.prepareStatement(statSql)) {
                    setParams(ps, unionParams);
                    try (ResultSet rs = ps.executeQuery()) {
                        JSONArray stat = new JSONArray();
                        if (rs.next()) {
                            stat.put(rs.getLong("cnt_approved"));
                            stat.put(rs.getLong("sum_approved"));
                            stat.put(rs.getLong("sum_all"));
                        }
                        response.put("statistic", stat);
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
            logger.error("DepositLogProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5");
        }
        return response.toString();
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            Object val = params.get(i);
            if (val instanceof Long) ps.setLong(i + 1, (Long) val);
            else if (val instanceof Integer) ps.setInt(i + 1, (Integer) val);
            else ps.setString(i + 1, val.toString());
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }

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
            return new SimpleDateFormat("yyyy-MM-dd").format(d);
        } catch (Exception ignored) {}
        return input;
    }
}
