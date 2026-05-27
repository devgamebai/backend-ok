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
import java.util.ArrayList;
import java.util.List;

/**
 * c=8820 — List bank names with pagination and filters.
 * Params: bn (bank name filter), bc (bank code filter), pn (page), l (limit)
 */
public class BankNameListProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String bankName = request.getParameter("bn");
            String bankCode = request.getParameter("bc");
            int page = 1, limit = 20;
            try { String s = request.getParameter("pn"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 0) page = 0;
            if (limit < 1 || limit > 100) limit = 20;
            int offset = page * limit;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (bankName != null && !bankName.isEmpty()) {
                where.append(" AND bank_name LIKE ?");
                params.add("%" + bankName + "%");
            }
            if (bankCode != null && !bankCode.isEmpty()) {
                where.append(" AND code LIKE ?");
                params.add("%" + bankCode + "%");
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                String countSql = "SELECT COUNT(*) FROM banks" + where;
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) ps.setString(i + 1, (String) params.get(i));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT id, bank_name, code, logo, status, add_by, create_date, update_date FROM banks"
                        + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = 1;
                    for (Object p : params) ps.setString(idx++, (String) p);
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("bank_name", rs.getString("bank_name") != null ? rs.getString("bank_name") : "");
                            item.put("code", rs.getString("code") != null ? rs.getString("code") : "");
                            item.put("bank_short_name", rs.getString("code") != null ? rs.getString("code") : "");
                            item.put("logo", rs.getString("logo") != null ? rs.getString("logo") : "");
                            item.put("status", rs.getInt("status"));
                            item.put("add_by", rs.getString("add_by") != null ? rs.getString("add_by") : "");
                            item.put("create_date", rs.getString("create_date") != null ? rs.getString("create_date") : "");
                            item.put("update_date", rs.getString("update_date") != null ? rs.getString("update_date") : "");
                            arr.put(item);
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", arr);
                response.put("total", total);
                response.put("totalRecords", total);
                response.put("page", page);
                response.put("limit", limit);
            }

        } catch (Exception e) {
            logger.error("BankNameListProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
