package com.vinplay.api.backend.processors.bank;

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

/**
 * c=8802 (remapped): Search player bank accounts for admin CMS.
 * Returns paginated list from users_bank table.
 * Response format matches legacy JS: {success:true, data: "{\"total\":N,\"data\":[...]}"}
 */
public class AdminSearchPlayerBankProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String nickName = request.getParameter("nn");
            String customerName = request.getParameter("cn");
            String bankName = request.getParameter("bn");
            String bankNumber = request.getParameter("bnum");

            int page = 1, limit = 10;
            try {
                String pn = request.getParameter("pn");
                if (pn != null && !pn.isEmpty()) page = Integer.parseInt(pn);
                String l = request.getParameter("l");
                if (l != null && !l.isEmpty()) limit = Integer.parseInt(l);
            } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1) limit = 10;
            if (limit > 100) limit = 100;
            int offset = (page - 1) * limit;

            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            if (nickName != null && !nickName.isEmpty()) where.append(" AND ub.nick_name LIKE ? ");
            if (customerName != null && !customerName.isEmpty()) where.append(" AND ub.customer_name LIKE ? ");
            if (bankName != null && !bankName.isEmpty()) where.append(" AND ub.bank_name LIKE ? ");
            if (bankNumber != null && !bankNumber.isEmpty()) where.append(" AND ub.bank_number LIKE ? ");

            String countSql = "SELECT COUNT(*) as cnt FROM users_bank ub " + where;
            // Prefer the live banks.bank_name via the bank_id FK so admin sees
            // the up-to-date name after any rename. Fall back to ub.bank_name
            // snapshot for legacy rows (bank_id IS NULL).
            // 2026-05-13: combine bank_name as "Korean (CODE)" for admin
            // search display consistency.
            String dataSql = "SELECT ub.*, b.bank_name AS canonical_bank_name, b.code, b.logo, " +
                    "CASE WHEN b.bank_name IS NOT NULL AND b.code IS NOT NULL AND b.code <> '' " +
                    "     THEN CONCAT(b.bank_name, ' (', b.code, ')') " +
                    "     WHEN b.bank_name IS NOT NULL THEN b.bank_name " +
                    "     ELSE ub.bank_name END AS bank_name_combined " +
                    "FROM users_bank ub " +
                    "LEFT JOIN banks b ON (ub.bank_id IS NOT NULL AND b.id = ub.bank_id) " +
                    "                  OR (ub.bank_id IS NULL AND b.bank_name = ub.bank_name) " +
                    where + " ORDER BY ub.id DESC LIMIT ? OFFSET ?";

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                int total = 0;
                try (PreparedStatement stm = conn.prepareStatement(countSql)) {
                    int idx = 1;
                    if (nickName != null && !nickName.isEmpty()) stm.setString(idx++, "%" + nickName + "%");
                    if (customerName != null && !customerName.isEmpty()) stm.setString(idx++, "%" + customerName + "%");
                    if (bankName != null && !bankName.isEmpty()) stm.setString(idx++, "%" + bankName + "%");
                    if (bankNumber != null && !bankNumber.isEmpty()) stm.setString(idx++, "%" + bankNumber + "%");
                    ResultSet rs = stm.executeQuery();
                    if (rs.next()) total = rs.getInt("cnt");
                }

                JSONArray dataArray = new JSONArray();
                try (PreparedStatement stm = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    if (nickName != null && !nickName.isEmpty()) stm.setString(idx++, "%" + nickName + "%");
                    if (customerName != null && !customerName.isEmpty()) stm.setString(idx++, "%" + customerName + "%");
                    if (bankName != null && !bankName.isEmpty()) stm.setString(idx++, "%" + bankName + "%");
                    if (bankNumber != null && !bankNumber.isEmpty()) stm.setString(idx++, "%" + bankNumber + "%");
                    stm.setInt(idx++, limit);
                    stm.setInt(idx++, offset);

                    ResultSet rs = stm.executeQuery();
                    while (rs.next()) {
                        JSONObject row = new JSONObject();
                        row.put("id", rs.getLong("id"));
                        row.put("userId", rs.getLong("user_id"));
                        row.put("nickName", rs.getString("nick_name"));
                        String combined = rs.getString("bank_name_combined");
                        row.put("bankName", combined != null ? combined : rs.getString("bank_name"));
                        row.put("bankCode", rs.getString("code"));
                        row.put("customerName", rs.getString("customer_name"));
                        row.put("bankNumber", rs.getString("bank_number"));
                        row.put("status", rs.getInt("status"));
                        row.put("branch", rs.getString("branch"));
                        row.put("lastEditor", rs.getString("last_editor"));
                        java.sql.Timestamp updateTs = rs.getTimestamp("update_date");
                        row.put("updateDate", updateTs != null ? updateTs.getTime() : 0);
                        java.sql.Timestamp createTs = rs.getTimestamp("create_date");
                        row.put("createDate", createTs != null ? createTs.getTime() : 0);
                        dataArray.put(row);
                    }
                }

                // Legacy format: data is a JSON string (JS does JSON.parse(response.data))
                JSONObject innerData = new JSONObject();
                innerData.put("total", total);
                innerData.put("data", dataArray);

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", innerData.toString());
            }
        } catch (Exception e) {
            logger.error("AdminSearchPlayerBankProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
