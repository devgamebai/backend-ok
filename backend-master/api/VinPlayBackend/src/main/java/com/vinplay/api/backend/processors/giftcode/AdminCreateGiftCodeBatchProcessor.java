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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * c=9940 — Admin create a batch of gift codes.
 *
 * <p>Request params:
 * <ul>
 *   <li>{@code aat} — admin access token (required)</li>
 *   <li>{@code qty} — number of codes to generate, 1–500 (required)</li>
 *   <li>{@code amount} — VIN value per code, &gt;0 (required)</li>
 *   <li>{@code rollover} — bet-turnover multiplier, &gt;=1 (required, PM: 0 forbidden)</li>
 *   <li>{@code expire_days} — days until expiry, 1–365 (optional, default 30)</li>
 *   <li>{@code prefix} — code prefix, max 4 chars (optional, default "SUN")</li>
 * </ul>
 *
 * <p>Tiền từ quỹ platform — không trừ balance ai, chỉ ghi log khuyến mãi.
 */
public class AdminCreateGiftCodeBatchProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    private static final String CHARSET = "0123456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int    CODE_RANDOM_LEN = 8;
    private static final int    MAX_RETRY = 10;
    private static final Random RD = new Random();

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

            // ── 2. Parse & validate params ─────────────────────────────────
            String qtyStr     = request.getParameter("qty");
            String amountStr  = request.getParameter("amount");
            String rolloverStr= request.getParameter("rollover");
            String expireDaysStr = request.getParameter("expire_days");
            String prefixParam   = request.getParameter("prefix");

            if (qtyStr == null || amountStr == null || rolloverStr == null) {
                return err(response, "4001", "qty, amount, rollover are required");
            }

            int qty;
            long amount;
            int rollover;
            int expireDays = 30;
            String prefix = "SUN";

            try { qty = Integer.parseInt(qtyStr); } catch (NumberFormatException e) {
                return err(response, "4001", "qty must be a number");
            }
            try { amount = Long.parseLong(amountStr); } catch (NumberFormatException e) {
                return err(response, "4001", "amount must be a number");
            }
            try { rollover = Integer.parseInt(rolloverStr); } catch (NumberFormatException e) {
                return err(response, "4001", "rollover must be a number");
            }
            if (expireDaysStr != null && !expireDaysStr.isEmpty()) {
                try { expireDays = Integer.parseInt(expireDaysStr); } catch (NumberFormatException e) {
                    return err(response, "4001", "expire_days must be a number");
                }
            }
            if (prefixParam != null && !prefixParam.isEmpty()) {
                prefix = prefixParam.toUpperCase().replaceAll("[^A-Z0-9]", "");
                if (prefix.length() > 4) prefix = prefix.substring(0, 4);
                if (prefix.isEmpty()) prefix = "SUN";
            }

            if (qty < 1 || qty > 500) {
                return err(response, "4003", "qty must be between 1 and 500");
            }
            if (amount <= 0 || amount > 10_000_000L) {
                return err(response, "4004", "amount must be > 0 and <= 10,000,000");
            }
            if (rollover < 1) {
                return err(response, "4002", "rollover must be >= 1 (rollover=0 not allowed per policy)");
            }
            if (expireDays < 1 || expireDays > 365) {
                return err(response, "4001", "expire_days must be between 1 and 365");
            }

            // ── 3. Generate codes into DB ──────────────────────────────────
            long now = System.currentTimeMillis();
            Timestamp fromTs    = new Timestamp(now);
            Timestamp expiredTs = new Timestamp(now + (long) expireDays * 24 * 60 * 60 * 1000L);

            // batch_id = unix-millis dùng làm định danh cho cả đợt tạo này.
            // Lưu vào bundle_id để admin sau đó có thể deactivate cả batch qua c=9942.
            long batchId = now;

            List<String> createdCodes = new ArrayList<>(qty);

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                String checkSql  = "SELECT 1 FROM gift_codes WHERE giftcode = ? LIMIT 1";
                String insertSql = "INSERT INTO gift_codes " +
                        "(giftcode, type, money, max_use, time_used, `from`, exprired, " +
                        "rollover_rounds, source, created_by, created_at, bundle_id, event, user_name) " +
                        "VALUES (?, 0, ?, 1, 0, ?, ?, ?, 'ADMIN', ?, NOW(), ?, 0, '')";

                int generated = 0;
                int failSafe  = 0;
                while (generated < qty) {
                    if (failSafe++ > qty * MAX_RETRY) {
                        logger.error("AdminCreateGiftCodeBatchProcessor: too many retries generating unique codes");
                        return err(response, "9999", "Failed to generate unique codes after too many retries");
                    }

                    String code = generateCode(prefix);

                    // Check uniqueness
                    boolean exists = false;
                    try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                        ps.setString(1, code);
                        try (ResultSet rs = ps.executeQuery()) {
                            exists = rs.next();
                        }
                    }
                    if (exists) continue;

                    // Insert với batch_id vào bundle_id
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setString(1, code);
                        ps.setLong(2, amount);
                        ps.setTimestamp(3, fromTs);
                        ps.setTimestamp(4, expiredTs);
                        ps.setInt(5, rollover);
                        ps.setString(6, adminNickname);
                        ps.setLong(7, batchId);
                        ps.executeUpdate();
                    }

                    createdCodes.add(code);
                    generated++;
                }
            }

            // ── 4. Audit log ───────────────────────────────────────────────
            logger.info(String.format(
                    "AdminCreateGiftCodeBatchProcessor: OK admin=%s qty=%d amount=%d rollover=%d expireDays=%d totalValue=%d",
                    adminNickname, qty, amount, rollover, expireDays, (long) qty * amount));

            // ── 5. Response ────────────────────────────────────────────────
            JSONArray codesArray = new JSONArray();
            for (String c : createdCodes) codesArray.put(c);

            JSONObject data = new JSONObject();
            data.put("codes", codesArray);
            data.put("count", createdCodes.size());
            data.put("amount", amount);
            data.put("rollover_rounds", rollover);
            data.put("total_value", (long) qty * amount);
            data.put("expire_days", expireDays);
            data.put("created_by", adminNickname);
            // batch_id để admin dùng khi muốn deactivate toàn bộ codes chưa dùng của đợt này (c=9942)
            data.put("batch_id", batchId);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);

        } catch (Exception e) {
            logger.error("AdminCreateGiftCodeBatchProcessor error", e);
            return err(response, "9999", "Internal server error: " + e.getMessage());
        }
        return response.toString();
    }

    private String generateCode(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        int randomLen = CODE_RANDOM_LEN - prefix.length();
        if (randomLen < 4) randomLen = 4; // luôn đảm bảo có ít nhất 4 ký tự random
        for (int i = 0; i < randomLen; i++) {
            sb.append(CHARSET.charAt(RD.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
