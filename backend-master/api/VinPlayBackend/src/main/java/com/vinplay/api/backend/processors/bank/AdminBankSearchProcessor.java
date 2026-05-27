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
import java.sql.Timestamp;

public class AdminBankSearchProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            int page = 1;
            int limit = 10;
            try {
                String pnStr = request.getParameter("pn");
                if (pnStr != null && !pnStr.isEmpty()) {
                    page = Integer.parseInt(pnStr);
                }
            } catch (NumberFormatException ignored) {
            }
            try {
                String lStr = request.getParameter("l");
                if (lStr != null && !lStr.isEmpty()) {
                    limit = Integer.parseInt(lStr);
                }
            } catch (NumberFormatException ignored) {
            }
            if (page < 1) page = 1;
            if (limit < 1) limit = 10;
            if (limit > 100) limit = 100;

            int offset = (page - 1) * limit;
            int total = 0;
            JSONArray dataArray = new JSONArray();

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count total
                try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) FROM admin_banks")) {
                    try (ResultSet countRs = countPs.executeQuery()) {
                        if (countRs.next()) {
                            total = countRs.getInt(1);
                        }
                    }
                }

                // Fetch page. 2026-05-12: JOIN banks via admin_banks.bank_id
                // FK so admin search shows canonical Korean bank_name even if
                // the row still stores a short code.
                // 2026-05-13: bank_name now combined "Korean (CODE)".
                String sql = "SELECT ab.id, ab.customer_name, " +
                        "CASE WHEN b.bank_name IS NOT NULL AND b.code IS NOT NULL AND b.code <> '' " +
                        "     THEN CONCAT(b.bank_name, ' (', b.code, ')') " +
                        "     WHEN b.bank_name IS NOT NULL THEN b.bank_name " +
                        "     ELSE ab.bank_name END AS bank_name, " +
                        "b.code AS bank_code, " +
                        "ab.bank_number, ab.branch, ab.status, ab.last_editor, ab.created_at, ab.updated_at " +
                        "FROM admin_banks ab " +
                        "LEFT JOIN banks b ON b.id = ab.bank_id " +
                        "ORDER BY ab.id DESC LIMIT ? OFFSET ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, limit);
                    ps.setInt(2, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("customerName", rs.getString("customer_name"));
                            item.put("bankName", rs.getString("bank_name"));
                            item.put("bankCode", rs.getString("bank_code") != null ? rs.getString("bank_code") : "");
                            item.put("bankNumber", rs.getString("bank_number"));
                            item.put("branch", rs.getString("branch") != null ? rs.getString("branch") : "");
                            item.put("status", rs.getInt("status"));
                            item.put("lastEditor", rs.getString("last_editor") != null ? rs.getString("last_editor") : "");

                            Timestamp createDate = rs.getTimestamp("created_at");
                            item.put("createDate", createDate != null ? createDate.getTime() : 0);

                            Timestamp updateDate = rs.getTimestamp("updated_at");
                            item.put("updateDate", updateDate != null ? updateDate.getTime() : 0);

                            dataArray.put(item);
                        }
                    }
                }
            }

            JSONObject innerData = new JSONObject();
            innerData.put("total", total);
            innerData.put("data", dataArray);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", innerData.toString());
        } catch (Exception e) {
            logger.error("AdminBankSearchProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "5");
            response.put("data", "");
        }
        return response.toString();
    }
}
