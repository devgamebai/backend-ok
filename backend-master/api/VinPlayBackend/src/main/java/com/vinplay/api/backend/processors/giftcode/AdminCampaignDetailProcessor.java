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
 * c=9944 — Admin get detail of a gift code campaign.
 *
 * <p>Trả về danh sách tất cả gift codes trong một campaign (bundle_id),
 * kèm thông tin ai đã sử dụng và thời gian dùng.
 *
 * <p>Request params:
 * <ul>
 *   <li>{@code aat}      — admin access token (required)</li>
 *   <li>{@code batch_id} — campaign ID (bundle_id) từ c=9943 (required)</li>
 *   <li>{@code p}        — page (default: 1)</li>
 *   <li>{@code l}        — limit (default: 50, max: 200)</li>
 *   <li>{@code used}     — filter: 0=chưa dùng, 1=đã dùng (omit for all)</li>
 * </ul>
 *
 * <p>Response mỗi gift code item:
 * <pre>
 * {
 *   "id":             123,
 *   "giftcode":       "SUN3K9X1",
 *   "money":          50000,
 *   "rollover_rounds":3,
 *   "status":         "USED",      // USED | AVAILABLE | EXPIRED
 *   "used_by":        "user123",   // null nếu chưa dùng
 *   "used_at":        "2026-05-01 10:30:00",  // null nếu chưa dùng
 *   "expires_at":     "2026-06-01 00:00:00"
 * }
 * </pre>
 */
public class AdminCampaignDetailProcessor implements BaseProcessor<HttpServletRequest, String> {

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

            // ── 2. batch_id (required) ─────────────────────────────────────
            String batchIdStr = request.getParameter("batch_id");
            if (batchIdStr == null || batchIdStr.isEmpty()) {
                return err(response, "4001", "batch_id is required");
            }
            long batchId;
            try {
                batchId = Long.parseLong(batchIdStr);
            } catch (NumberFormatException e) {
                return err(response, "4001", "batch_id must be a number");
            }

            // ── 3. Pagination ──────────────────────────────────────────────
            int page = 1, limit = 50;
            try { String s = request.getParameter("p"); if (s != null) page = Math.max(1, Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            try { String s = request.getParameter("l"); if (s != null) limit = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (limit < 1 || limit > 200) limit = 50;
            int offset = (page - 1) * limit;

            // ── 4. Filter ──────────────────────────────────────────────────
            String usedFilter = request.getParameter("used");

            String usedWhere = "";
            if ("0".equals(usedFilter)) {
                usedWhere = " AND gc.time_used = 0";
            } else if ("1".equals(usedFilter)) {
                usedWhere = " AND gc.time_used >= 1";
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {

                // Verify batch exists
                String verifySql = "SELECT COUNT(*) FROM gift_codes WHERE bundle_id = ? AND source = 'ADMIN'";
                try (PreparedStatement ps = conn.prepareStatement(verifySql)) {
                    ps.setLong(1, batchId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) == 0) {
                            return err(response, "4004", "Campaign not found: batch_id=" + batchId);
                        }
                    }
                }

                // Count
                String countSql = "SELECT COUNT(*) FROM gift_codes gc " +
                        "WHERE gc.bundle_id = ? AND gc.source = 'ADMIN'" + usedWhere;
                int total = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    ps.setLong(1, batchId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Detail: pull latest gift_code_useds row per code via a single
                // self-joined derived table. Two correlated subqueries (one per
                // column) would run 2× the page size; this folds to a single
                // aggregate + lookup join.
                //
                // FIX: added "AND u.created_at >= gc.created_at" to the ON clause.
                // gift_codes rows can be deleted and re-inserted with the same AUTO_INCREMENT
                // id (after a table cleanup/TRUNCATE). Without this guard the LEFT JOIN
                // would attach gift_code_useds records belonging to the OLD deleted code
                // to the NEW code with the same id, causing AVAILABLE codes to show a
                // used_by/used_at from a past campaign.
                String dataSql =
                        "SELECT " +
                        "  gc.id, gc.giftcode, gc.money, gc.rollover_rounds, " +
                        "  gc.time_used, gc.max_use, gc.exprired, gc.created_at AS gc_created_at, " +
                        "  u.username AS used_by, " +
                        "  u.created_at AS used_at, " +
                        "  CASE " +
                        "    WHEN gc.time_used >= gc.max_use THEN 'USED' " +
                        "    WHEN gc.exprired <= NOW() THEN 'EXPIRED' " +
                        "    ELSE 'AVAILABLE' " +
                        "  END AS status " +
                        "FROM gift_codes gc " +
                        "LEFT JOIN ( " +
                        "  SELECT u1.giftcode_id, u1.username, u1.created_at " +
                        "  FROM gift_code_useds u1 " +
                        "  INNER JOIN ( " +
                        "    SELECT giftcode_id, MAX(created_at) AS latest " +
                        "    FROM gift_code_useds GROUP BY giftcode_id " +
                        "  ) u2 ON u2.giftcode_id = u1.giftcode_id AND u2.latest = u1.created_at " +
                        ") u ON u.giftcode_id = gc.id AND u.created_at >= gc.created_at " +
                        "WHERE gc.bundle_id = ? AND gc.source = 'ADMIN'" + usedWhere +
                        " ORDER BY gc.id ASC " +
                        " LIMIT ? OFFSET ?";

                JSONArray arr = new JSONArray();
                try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                    ps.setLong(1, batchId);
                    ps.setInt(2, limit);
                    ps.setInt(3, offset);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject item = new JSONObject();
                            item.put("id",             rs.getInt("id"));
                            item.put("giftcode",       orEmpty(rs.getString("giftcode")));
                            item.put("money",          rs.getLong("money"));
                            item.put("rollover_rounds",rs.getInt("rollover_rounds"));
                            String status = orEmpty(rs.getString("status"));
                            item.put("status",         status);
                            item.put("expires_at",     orEmpty(rs.getString("exprired")));
                            // used_by/used_at only valid when status=USED (time_used >= max_use).
                            // Defensive guard: if SQL JOIN still leaks an orphaned record from
                            // a recycled id, the non-USED status here forces null output.
                            boolean isUsed = "USED".equals(status);
                            String usedBy = isUsed ? rs.getString("used_by") : null;
                            String usedAt = isUsed ? rs.getString("used_at") : null;
                            item.put("used_by", usedBy != null ? usedBy : JSONObject.NULL);
                            item.put("used_at", usedAt != null ? usedAt : JSONObject.NULL);
                            arr.put(item);
                        }
                    }
                }

                // Summary của campaign
                String summarySql =
                        "SELECT COUNT(*) AS total, " +
                        "SUM(CASE WHEN time_used >= 1 THEN 1 ELSE 0 END) AS used_count, " +
                        "SUM(CASE WHEN time_used = 0 AND exprired > NOW() THEN 1 ELSE 0 END) AS available_count, " +
                        "MIN(money) AS amount, MIN(rollover_rounds) AS rollover_rounds, " +
                        "MIN(created_by) AS created_by, MIN(created_at) AS created_at, MIN(exprired) AS expires_at " +
                        "FROM gift_codes WHERE bundle_id = ? AND source = 'ADMIN'";
                JSONObject summary = new JSONObject();
                try (PreparedStatement ps = conn.prepareStatement(summarySql)) {
                    ps.setLong(1, batchId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            summary.put("batch_id",       batchId);
                            summary.put("total_codes",    rs.getInt("total"));
                            summary.put("used_codes",     rs.getInt("used_count"));
                            summary.put("available_codes",rs.getInt("available_count"));
                            summary.put("amount",         rs.getLong("amount"));
                            summary.put("rollover_rounds",rs.getInt("rollover_rounds"));
                            summary.put("created_by",     orEmpty(rs.getString("created_by")));
                            summary.put("created_at",     orEmpty(rs.getString("created_at")));
                            summary.put("expires_at",     orEmpty(rs.getString("expires_at")));
                        }
                    }
                }

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("campaign", summary);
                response.put("data", arr);
                response.put("totalRecords", total);
                response.put("page", page);
                response.put("limit", limit);
                response.put("totalPages", (int) Math.ceil((double) total / limit));
            }

        } catch (Exception e) {
            logger.error("AdminCampaignDetailProcessor error", e);
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
