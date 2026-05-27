package com.vinplay.api.backend.processors.giftcode;

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
 * c=9941 — Admin list gift codes with filters and pagination.
 *
 * <p>Request params:
 * <ul>
 *   <li>{@code aat} — admin access token (required)</li>
 *   <li>{@code p} — page number (default 1)</li>
 *   <li>{@code l} — page size, max 100 (default 20)</li>
 *   <li>{@code code} — filter by giftcode (LIKE %code%)</li>
 *   <li>{@code created_by} — filter by creator admin username</li>
 *   <li>{@code used} — filter by used status: 0=unused, 1=used (omit for all)</li>
 *   <li>{@code rollover} — filter by exact rollover_rounds value</li>
 *   <li>{@code source} — filter by source (default: "ADMIN")</li>
 * </ul>
 */
public class AdminListGiftCodesProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            // ── 1. Auth ────────────────────────────────────────────────────
            String adminToken = request.getParameter("aat");
            if (adminToken == null || adminToken.isEmpty()) {
                adminToken = request.getParameter("at");
            }
            if (adminToken == null || adminToken.isEmpty()) {
                return err(response, "1001", "Admin token required");
            }
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            if (!tokenMap.containsKey(adminToken)) {
                return err(response, "1001", "Admin token expired or invalid");
            }

            // ── 2. Pagination ──────────────────────────────────────────────
            int page = 1, limit = 20;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;
            int offset = (page - 1) * limit;

            // ── 3. Filters ─────────────────────────────────────────────────
            String code       = request.getParameter("code");
            String createdBy  = request.getParameter("created_by");
            String usedStr    = request.getParameter("used");
            String rolloverStr= request.getParameter("rollover");
            String source     = request.getParameter("source");

            StringBuilder where = new StringBuilder(" WHERE source = ?");
            List<Object> params = new ArrayList<>();
            params.add(source != null && !source.isEmpty() ? source : "ADMIN");

            if (code != null && !code.isEmpty()) {
                where.append(" AND giftcode LIKE ?");
                params.add("%" + code + "%");
            }
            if (createdBy != null && !createdBy.isEmpty()) {
                where.append(" AND created_by = ?");
                params.add(createdBy);
            }
            if (usedStr != null && !usedStr.isEmpty()) {
                if ("0".equals(usedStr)) {
                    where.append(" AND time_used = 0");
                } else if ("1".equals(usedStr)) {
                    where.append(" AND time_used >= 1");
                }
            }
            if (rolloverStr != null && !rolloverStr.isEmpty()) {
                try {
                    int rolloverFilter = Integer.parseInt(rolloverStr);
                    where.append(" AND rollover_rounds = ?");
                    params.add(rolloverFilter);
                } catch (NumberFormatException ignored) {}
            }

            // ── 4. Query ───────────────────────────────────────────────────
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count
                String countSql = "SELECT COUNT(*) FROM gift_codes" + where;
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    bindParams(ps, params);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                String dataSql = "SELECT id, giftcode, money, time_used, max_use, " +
                        "`from`, exprired, created_at, created_by, rollover_rounds, source " +
                        "FROM gift_codes" + where +
                        " ORDER BY id DESC LIMIT ? OFFSET ?";
                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    int idx = bindParams(ps, params);
                    ps.setInt(idx++, limit);
                    ps.setInt(idx, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id", rs.getInt("id"));
                            item.put("giftcode", orEmpty(rs.getString("giftcode")));
                            item.put("money", rs.getLong("money"));
                            item.put("time_used", rs.getInt("time_used"));
                            item.put("max_use", rs.getInt("max_use"));
                            item.put("used", rs.getInt("time_used") >= rs.getInt("max_use"));
                            item.put("from", orEmpty(rs.getString("from")));
                            item.put("exprired", orEmpty(rs.getString("exprired")));
                            item.put("created_at", orEmpty(rs.getString("created_at")));
                            item.put("created_by", orEmpty(rs.getString("created_by")));
                            item.put("rollover_rounds", rs.getInt("rollover_rounds"));
                            item.put("source", orEmpty(rs.getString("source")));
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
                response.put("totalPages", (int) Math.ceil((double) total / limit));
            }

        } catch (Exception e) {
            logger.error("AdminListGiftCodesProcessor error", e);
            return err(response, "9999", "Internal server error: " + e.getMessage());
        }
        return response.toString();
    }

    private int bindParams(PreparedStatement ps, List<Object> params) throws Exception {
        int idx = 1;
        for (Object p : params) {
            if (p instanceof Integer) ps.setInt(idx, (Integer) p);
            else ps.setString(idx, (String) p);
            idx++;
        }
        return idx;
    }

    private static String orEmpty(String s) { return s != null ? s : ""; }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
