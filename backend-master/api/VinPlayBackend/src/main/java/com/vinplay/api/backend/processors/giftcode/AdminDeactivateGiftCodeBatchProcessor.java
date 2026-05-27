package com.vinplay.api.backend.processors.giftcode;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=9942 — Admin deactivate a gift code batch.
 *
 * <p>Deactivate tất cả codes chưa được dùng ({@code time_used = 0}) thuộc
 * một batch ({@code bundle_id}) bằng cách set {@code exprired = NOW() - 1 SECOND}.
 * Codes đã được user dùng ({@code time_used >= 1}) giữ nguyên, không bị ảnh hưởng.
 *
 * <p>Request params:
 * <ul>
 *   <li>{@code aat}      — admin access token (required)</li>
 *   <li>{@code batch_id} — batch ID trả về từ c=9940 (required)</li>
 * </ul>
 *
 * <p>Response:
 * <pre>
 * {
 *   "success": true,
 *   "deactivated": 45,   // số codes bị deactivate
 *   "skipped": 5         // số codes đã dùng rồi, không bị ảnh hưởng
 * }
 * </pre>
 */
public class AdminDeactivateGiftCodeBatchProcessor implements BaseProcessor<HttpServletRequest, String> {

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
            String adminNickname = tokenMap.get(adminToken);
            if (adminNickname == null) {
                return err(response, "1001", "Admin token expired or invalid");
            }

            // ── 2. Param ───────────────────────────────────────────────────
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

            // ── 3. Verify batch exists & belongs to source=ADMIN ──────────
            int totalInBatch = 0;
            int alreadyUsed  = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                // Count tổng codes và số đã dùng trong batch này
                String countSql = "SELECT COUNT(*) AS total, " +
                        "SUM(CASE WHEN time_used >= 1 THEN 1 ELSE 0 END) AS used_count " +
                        "FROM gift_codes WHERE bundle_id = ? AND source = 'ADMIN'";
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    ps.setLong(1, batchId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalInBatch = rs.getInt("total");
                            alreadyUsed  = rs.getInt("used_count");
                        }
                    }
                }

                if (totalInBatch == 0) {
                    return err(response, "4004", "Batch not found: batch_id=" + batchId);
                }

                // ── 4. Deactivate: set exprired = NOW()-1s cho codes chưa dùng
                // Codes đã dùng (time_used >= 1) KHÔNG bị ảnh hưởng.
                String deactivateSql = "UPDATE gift_codes " +
                        "SET exprired = DATE_SUB(NOW(), INTERVAL 1 SECOND) " +
                        "WHERE bundle_id = ? AND source = 'ADMIN' AND time_used = 0 " +
                        "AND (exprired IS NULL OR exprired > NOW())";
                int deactivated;
                try (PreparedStatement ps = conn.prepareStatement(deactivateSql)) {
                    ps.setLong(1, batchId);
                    deactivated = ps.executeUpdate();
                }

                logger.info(String.format(
                        "AdminDeactivateGiftCodeBatchProcessor: OK admin=%s batchId=%d total=%d used=%d deactivated=%d",
                        adminNickname, batchId, totalInBatch, alreadyUsed, deactivated));

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("batch_id", batchId);
                response.put("total_in_batch", totalInBatch);
                response.put("deactivated", deactivated);   // codes bị tắt (chưa dùng → hết hạn ngay)
                response.put("skipped", alreadyUsed);       // codes đã dùng rồi, giữ nguyên
            }

        } catch (Exception e) {
            logger.error("AdminDeactivateGiftCodeBatchProcessor error", e);
            return err(response, "9999", "Internal server error: " + e.getMessage());
        }
        return response.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
