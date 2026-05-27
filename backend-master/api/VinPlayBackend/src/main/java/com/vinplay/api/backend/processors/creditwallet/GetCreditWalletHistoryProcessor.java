package com.vinplay.api.backend.processors.creditwallet;

import com.vinplay.dal.dao.CreditWalletDao;
import com.vinplay.dal.dao.impl.CreditWalletDaoImpl;
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

/**
 * c=9925 — Lịch sử giao dịch Credit Wallet.
 * Path: /api_agent hoặc /api_backend
 * Params:
 *   rc   = agent code hoặc nickname
 *   ft   = from date "yyyy-MM-dd" hoặc "yyyy-MM-dd HH:mm:ss" (optional)
 *   et   = to date (optional)
 *   type = filter loại giao dịch: ADMIN_CREDIT|TRANSFER_OUT|TRANSFER_IN|DEPOSIT_TO_AGENT|DEPOSIT_TO_USER (optional)
 *   pg   = page (default: 1)
 *   size = page size (default: 20, max: 100)
 */
public class GetCreditWalletHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String rc = request.getParameter("rc");
            String typeFilter = request.getParameter("type");
            String ft = request.getParameter("ft");
            String et = request.getParameter("et");

            int page = parsePage(request.getParameter("pg"), 1);
            int limit = parsePage(request.getParameter("size"), 20);
            if (limit > 100) limit = 100;
            if (page < 1) page = 1;

            if (isEmpty(rc)) {
                return fail(response, "4001", "rc (agent code or nickname) required");
            }

            // Resolve agent: try code first, then nickname
            CreditWalletService.AgentInfo agent = CreditWalletService.getAgentInfoByCode(rc);
            if (agent == null) agent = CreditWalletService.getAgentInfoByNick(rc);
            if (agent == null) return fail(response, "1002", "Agent not found");

            final int agentId = agent.id;

            // Build WHERE clause + params
            StringBuilder where = new StringBuilder(" WHERE agent_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(agentId);

            if (!isEmpty(typeFilter)) {
                where.append(" AND type = ?");
                params.add(typeFilter);
            }
            if (!isEmpty(ft)) {
                where.append(" AND created_at >= ?");
                params.add(ft.contains(" ") ? ft : ft + " 00:00:00");
            }
            if (!isEmpty(et)) {
                where.append(" AND created_at <= ?");
                params.add(et.contains(" ") ? et : et + " 23:59:59");
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count total
                int total = 0;
                String countSql = "SELECT COUNT(*) FROM vinplay.credit_wallet_transactions" + where;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    bindParams(ps, params);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Fetch page
                JSONArray arr = new JSONArray();
                String dataSql = "SELECT * FROM vinplay.credit_wallet_transactions" + where +
                        " ORDER BY created_at DESC LIMIT ? OFFSET ?";
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    bindParams(ps, params);
                    int idx = params.size() + 1;
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, (page - 1) * limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id", rs.getLong("id"));
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

                // Current balance
                CreditWalletDao dao = new CreditWalletDaoImpl();
                long currentBalance = dao.getBalance(agentId);

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", arr);
                response.put("total", total);
                response.put("page", page);
                response.put("limit", limit);
                response.put("current_balance", currentBalance);
                response.put("agent_id", agentId);
                response.put("nick_name", agent.nickname);
            }
        } catch (Exception e) {
            logger.error("GetCreditWalletHistoryProcessor error", e);
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
