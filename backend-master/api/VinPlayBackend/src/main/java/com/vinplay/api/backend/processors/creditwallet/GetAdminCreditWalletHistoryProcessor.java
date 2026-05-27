package com.vinplay.api.backend.processors.creditwallet;

import com.vinplay.dal.service.CreditWalletService;
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

public class GetAdminCreditWalletHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String nn = request.getParameter("nn"); // Optional: nickname or agent code
            String typeFilter = request.getParameter("type");
            String ft = request.getParameter("ft");
            String et = request.getParameter("et");

            int page = parsePage(request.getParameter("pg"), 1);
            int limit = parsePage(request.getParameter("size"), 30);
            if (limit > 500) limit = 500;
            if (page < 1) page = 1;

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (!isEmpty(nn)) {
                CreditWalletService.AgentInfo agent = CreditWalletService.getAgentInfoByNick(nn);
                if (agent == null) agent = CreditWalletService.getAgentInfoByCode(nn);
                if (agent == null) {
                    // Agent không tồn tại → trả empty list, không phải lỗi
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("data", new JSONArray());
                    response.put("total", 0);
                    response.put("page", page);
                    response.put("limit", limit);
                    return response.toString();
                }
                where.append(" AND t.agent_id = ?");
                params.add(agent.id);
            }

            if (!isEmpty(typeFilter)) {
                where.append(" AND t.type = ?");
                params.add(typeFilter);
            }
            if (!isEmpty(ft)) {
                where.append(" AND t.created_at >= ?");
                params.add(ft.contains(" ") ? ft : ft + " 00:00:00");
            }
            if (!isEmpty(et)) {
                where.append(" AND t.created_at <= ?");
                params.add(et.contains(" ") ? et : et + " 23:59:59");
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                int total = 0;
                String countSql = "SELECT COUNT(*) FROM vinplay.credit_wallet_transactions t " + where;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    bindParams(ps, params);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                JSONArray arr = new JSONArray();
                // Join to get agent nickname for display
                String dataSql = "SELECT t.*, a.nickname AS agent_nick " +
                        "FROM vinplay.credit_wallet_transactions t " +
                        "LEFT JOIN vinplay_admin.useragent a ON t.agent_id = a.id " +
                        where + " ORDER BY t.created_at DESC LIMIT ? OFFSET ?";

                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    bindParams(ps, params);
                    int idx = params.size() + 1;
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, (page - 1) * limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id", rs.getLong("id")); // Note: "id" instead of "t.id" to avoid driver ambiguity. The resultSet resolves the column.
                            row.put("agent_id", rs.getInt("agent_id"));
                            row.put("agent_nick", nvl(rs.getString("agent_nick")));
                            row.put("type", rs.getString("type"));
                            row.put("amount", rs.getLong("amount"));
                            row.put("direction", rs.getString("direction"));
                            row.put("balance_after", rs.getLong("balance_after"));
                            row.put("related_user", nvl(rs.getString("related_user")));
                            row.put("related_agent_id", rs.getObject("related_agent_id") != null ? rs.getInt("related_agent_id") : 0);
                            row.put("note", nvl(rs.getString("note")));
                            row.put("created_at", nvl(rs.getString("created_at")));
                            arr.put(row);
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", arr);
                response.put("total", total);
                response.put("page", page);
                response.put("limit", limit);
            }
        } catch (Exception e) {
            logger.error("GetAdminCreditWalletHistoryProcessor error", e);
            return fail(response, "9999", "Internal error");
        }
        return response.toString();
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
            else ps.setString(i + 1, p.toString());
        }
    }

    private int parsePage(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
    private String nvl(String s) { return s != null ? s : ""; }

    private String fail(JSONObject r, String code, String msg) {
        r.put("success", false); r.put("errorCode", code); r.put("message", msg);
        return r.toString();
    }
}
