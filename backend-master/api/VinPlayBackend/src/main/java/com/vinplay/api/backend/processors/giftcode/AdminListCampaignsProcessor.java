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

/**
 * c=9943 — Admin list gift code campaigns.
 *
 * <p>Mỗi row = 1 đợt tạo giftcode (1 campaign = 1 bundle_id).
 * Trả về thông tin tổng hợp: tổng codes, đã dùng, còn lại, trạng thái.
 *
 * <p>Request params:
 * <ul>
 *   <li>{@code aat}         — admin access token (required)</li>
 *   <li>{@code p}           — page (default: 1)</li>
 *   <li>{@code l}           — limit (default: 20, max: 100)</li>
 *   <li>{@code created_by}  — filter by admin creator</li>
 *   <li>{@code status}      — filter: ACTIVE | EXPIRED | DEPLETED</li>
 * </ul>
 *
 * <p>Response mỗi campaign item:
 * <pre>
 * {
 *   "batch_id":       1746097123456,
 *   "total_codes":    100,
 *   "used_codes":     27,
 *   "available_codes":73,
 *   "amount":         50000,
 *   "rollover_rounds":3,
 *   "created_by":     "admin1",
 *   "created_at":     "2026-05-01 14:30:00",
 *   "expires_at":     "2026-06-01 00:00:00",
 *   "status":         "ACTIVE"   // ACTIVE | EXPIRED | DEPLETED
 * }
 * </pre>
 */
public class AdminListCampaignsProcessor implements BaseProcessor<HttpServletRequest, String> {

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
            try { String s = request.getParameter("p"); if (s != null) page = Math.max(1, Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (limit < 1 || limit > 100) limit = 20;
            int offset = (page - 1) * limit;

            // ── 3. Filters ─────────────────────────────────────────────────
            String createdBy = request.getParameter("created_by");
            String status    = request.getParameter("status"); // ACTIVE | EXPIRED | DEPLETED

            StringBuilder having = new StringBuilder();

            // Base WHERE: chỉ lấy ADMIN giftcodes có bundle_id hợp lệ
            StringBuilder where = new StringBuilder("WHERE source = 'ADMIN' AND bundle_id > 0");

            if (createdBy != null && !createdBy.isEmpty()) {
                where.append(" AND created_by = '").append(createdBy.replace("'", "''")).append("'");
            }

            // Filter status qua HAVING sau GROUP BY
            if ("ACTIVE".equals(status)) {
                having.append("HAVING MIN(exprired) > NOW() AND SUM(CASE WHEN time_used = 0 THEN 1 ELSE 0 END) > 0");
            } else if ("EXPIRED".equals(status)) {
                having.append("HAVING MIN(exprired) <= NOW()");
            } else if ("DEPLETED".equals(status)) {
                having.append("HAVING MIN(exprired) > NOW() AND SUM(CASE WHEN time_used = 0 THEN 1 ELSE 0 END) = 0");
            }

            // ── 4. Query ───────────────────────────────────────────────────
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {

                // Count tổng số campaigns
                String countSql = "SELECT COUNT(*) FROM (" +
                        "SELECT bundle_id FROM gift_codes " + where +
                        " GROUP BY bundle_id " + having +
                        ") AS t";
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getInt(1);
                }

                // Data: 1 row per campaign
                String dataSql =
                        "SELECT " +
                        "  bundle_id AS batch_id, " +
                        "  COUNT(*) AS total_codes, " +
                        "  SUM(CASE WHEN time_used >= 1 THEN 1 ELSE 0 END) AS used_codes, " +
                        "  SUM(CASE WHEN time_used = 0 THEN 1 ELSE 0 END) AS available_codes, " +
                        "  MIN(money) AS amount, " +
                        "  MIN(rollover_rounds) AS rollover_rounds, " +
                        "  MIN(created_by) AS created_by, " +
                        "  MIN(created_at) AS created_at, " +
                        "  MIN(exprired) AS expires_at, " +
                        "  CASE " +
                        "    WHEN MIN(exprired) <= NOW() THEN 'EXPIRED' " +
                        "    WHEN SUM(CASE WHEN time_used = 0 THEN 1 ELSE 0 END) = 0 THEN 'DEPLETED' " +
                        "    ELSE 'ACTIVE' " +
                        "  END AS status " +
                        "FROM gift_codes " + where +
                        " GROUP BY bundle_id " + having +
                        " ORDER BY MIN(created_at) DESC " +
                        " LIMIT ? OFFSET ?";

                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    ps.setInt(1, limit);
                    ps.setInt(2, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("batch_id",        rs.getLong("batch_id"));
                            item.put("total_codes",     rs.getInt("total_codes"));
                            item.put("used_codes",      rs.getInt("used_codes"));
                            item.put("available_codes", rs.getInt("available_codes"));
                            item.put("amount",          rs.getLong("amount"));
                            item.put("rollover_rounds", rs.getInt("rollover_rounds"));
                            item.put("created_by",      orEmpty(rs.getString("created_by")));
                            item.put("created_at",      orEmpty(rs.getString("created_at")));
                            item.put("expires_at",      orEmpty(rs.getString("expires_at")));
                            item.put("status",          orEmpty(rs.getString("status")));
                            arr.put(item);
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", arr);
                response.put("totalRecords", total);
                response.put("page", page);
                response.put("limit", limit);
                response.put("totalPages", (int) Math.ceil((double) total / limit));
            }

        } catch (Exception e) {
            logger.error("AdminListCampaignsProcessor error", e);
            return err(response, "9999", "Internal server error: " + e.getMessage());
        }
        return response.toString();
    }

    private static String orEmpty(String s) { return s != null ? s : ""; }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
