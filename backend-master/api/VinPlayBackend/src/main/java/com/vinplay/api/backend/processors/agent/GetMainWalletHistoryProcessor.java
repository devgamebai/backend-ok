package com.vinplay.api.backend.processors.agent;

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
 * c=9927 — Main wallet (users.vin) money-movement history for an agent's
 * OWN account.
 *
 * Reads MySQL {@code money_gateway_log} (the canonical audit log written
 * by {@link com.vinplay.dal.service.MoneyGateway#creditUser}) filtered by
 * the agent's user_id. Same request/response shape as c=9891 (agency
 * wallet history) and c=9925 (credit wallet history).
 *
 * Scope: this surfaces every deposit, withdraw, internal-convert, promo
 * bonus, admin topup, and credit-to-vin event. Game bets are NOT in this
 * stream — they live in Mongo {@code log_money_user_vin} and are
 * surfaced via the existing player history endpoints (c=303 etc.).
 *
 * Params:
 *   rc       agent code OR nickname OR username (FE typically passes user.code)
 *   source   optional exact source filter (e.g. CONVERT_AGENCY_TO_VIN, DEPOSIT_BANK,
 *            ADMIN_TOPUP, PROMO_BONUS, nap_credit_dai_ly)
 *   category optional shortcut filter:
 *              - "deposit"   → source LIKE 'DEPOSIT_%' OR 'nap_%' OR 'ADMIN_TOPUP'
 *              - "convert"   → source LIKE 'CONVERT_%'
 *              - "promo"     → source = 'PROMO_BONUS'
 *              - "credit"    → source LIKE '%credit%' (credit-related)
 *              - "all"       → no filter (default)
 *   ft       from datetime "yyyy-MM-dd" or "yyyy-MM-dd HH:mm:ss"
 *   et       to datetime (inclusive)
 *   pg       page (default 1)
 *   size     page size (default 20, max 100)
 *
 * Response shape mirrors c=9891 / c=9925:
 *   { success, errorCode, data: { list: [...], total, page, size } }
 *
 * Per-row fields (from money_gateway_log):
 *   id, source, amount (unsigned), direction (always "CREDIT"), tx_id,
 *   description, balance_after, promo_applied, bonus_amount, created_at.
 *
 * Direction is hard-coded "CREDIT" because money_gateway_log only stores
 * credits to vin (the canonical writer is MoneyGateway.creditUser).
 * Withdrawals are tracked in a separate flow and not in this audit table.
 */
public class GetMainWalletHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String rc = request.getParameter("rc");
            String sourceFilter = request.getParameter("source");
            String category = request.getParameter("category");
            String ft = request.getParameter("ft");
            String et = request.getParameter("et");

            int page = 1, limit = 20;
            try { if (request.getParameter("pg") != null) page = Integer.parseInt(request.getParameter("pg")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("size") != null) limit = Integer.parseInt(request.getParameter("size")); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1) limit = 20;
            if (limit > 100) limit = 100;

            if (rc == null || rc.isEmpty()) return err(response, "1001", "rc required");

            ResolvedAgent agent = resolveAgent(rc);
            if (agent == null) return err(response, "1002", "Agent not found");

            // Build dynamic WHERE
            StringBuilder where = new StringBuilder(" WHERE user_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(agent.userId);

            if (sourceFilter != null && !sourceFilter.isEmpty()) {
                where.append(" AND source = ?");
                params.add(sourceFilter);
            } else if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
                String catSql = categoryToSqlClause(category, params);
                if (catSql != null) where.append(catSql);
            }
            if (ft != null && !ft.isEmpty()) {
                where.append(" AND created_at >= ?");
                params.add(ft.contains(" ") ? ft : ft + " 00:00:00");
            }
            if (et != null && !et.isEmpty()) {
                where.append(" AND created_at <= ?");
                params.add(et.contains(" ") ? et : et + " 23:59:59");
            }

            int total = 0;
            JSONArray list = new JSONArray();
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                try (PreparedStatement countPs = conn.prepareStatement(
                        "SELECT COUNT(*) FROM money_gateway_log" + where)) {
                    bindParams(countPs, params);
                    try (ResultSet rs = countPs.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }
                // Page
                int offset = (page - 1) * limit;
                String listSql = "SELECT id, source, amount, tx_id, description, balance_after, " +
                        "promo_applied, bonus_amount, created_at " +
                        "FROM money_gateway_log" + where +
                        " ORDER BY id DESC LIMIT ? OFFSET ?";
                try (PreparedStatement listPs = conn.prepareStatement(listSql)) {
                    int idx = bindParams(listPs, params);
                    listPs.setInt(idx++, limit);
                    listPs.setInt(idx, offset);
                    try (ResultSet rs = listPs.executeQuery()) {
                        while (rs.next()) {
                            JSONObject row = new JSONObject();
                            row.put("id", rs.getLong("id"));
                            row.put("source", rs.getString("source"));
                            row.put("amount", rs.getLong("amount"));
                            row.put("direction", "CREDIT");
                            row.put("tx_id", rs.getString("tx_id"));
                            row.put("description", rs.getString("description"));
                            row.put("balance_after", rs.getLong("balance_after"));
                            row.put("promo_applied", rs.getInt("promo_applied") == 1);
                            row.put("bonus_amount", rs.getLong("bonus_amount"));
                            row.put("created_at", rs.getString("created_at"));
                            list.put(row);
                        }
                    }
                }
            }

            JSONObject data = new JSONObject();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("size", limit);
            data.put("nick_name", agent.nickname);
            data.put("user_id", agent.userId);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
        } catch (Exception e) {
            logger.error("GetMainWalletHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    /**
     * Resolve a rc param (code | nickname | username) into both users.id
     * and users.nick_name via the useragent → users join. Null on miss.
     */
    private static ResolvedAgent resolveAgent(String rc) {
        String nickname = null;
        try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement ps = adminConn.prepareStatement(
                     "SELECT nickname FROM useragent WHERE nickname = ? OR code = ? OR username = ? LIMIT 1")) {
            ps.setString(1, rc);
            ps.setString(2, rc);
            ps.setString(3, rc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) nickname = rs.getString("nickname");
            }
        } catch (Exception e) {
            logger.warn("resolveAgent (useragent) failed rc=" + rc, e);
            return null;
        }
        if (nickname == null) return null;

        try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = userConn.prepareStatement(
                     "SELECT id FROM users WHERE nick_name = ? LIMIT 1")) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new ResolvedAgent(rs.getLong("id"), nickname);
            }
        } catch (Exception e) {
            logger.warn("resolveAgent (users) failed nickname=" + nickname, e);
        }
        return null;
    }

    private static String categoryToSqlClause(String category, List<Object> params) {
        switch (category.toLowerCase()) {
            case "deposit":
                params.add("DEPOSIT_%");
                params.add("nap_%");
                params.add("ADMIN_TOPUP");
                return " AND (source LIKE ? OR source LIKE ? OR source = ?)";
            case "convert":
                params.add("CONVERT_%");
                return " AND source LIKE ?";
            case "promo":
                params.add("PROMO_BONUS");
                return " AND source = ?";
            case "credit":
                params.add("%credit%");
                return " AND source LIKE ?";
            default:
                return null;
        }
    }

    private static int bindParams(PreparedStatement ps, List<Object> params) throws java.sql.SQLException {
        int i = 1;
        for (Object p : params) {
            if (p instanceof Long) ps.setLong(i++, (Long) p);
            else if (p instanceof Integer) ps.setInt(i++, (Integer) p);
            else ps.setString(i++, String.valueOf(p));
        }
        return i;
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    private static final class ResolvedAgent {
        final long userId;
        final String nickname;
        ResolvedAgent(long userId, String nickname) {
            this.userId = userId;
            this.nickname = nickname;
        }
    }
}
