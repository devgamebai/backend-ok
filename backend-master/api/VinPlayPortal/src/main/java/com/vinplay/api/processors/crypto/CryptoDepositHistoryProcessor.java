package com.vinplay.api.processors.crypto;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=3022 — Get unified deposit history (bank + crypto + bonus) for the authenticated user.
 */
public class CryptoDepositHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");

            if (accessToken == null || accessToken.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Resolve user from token
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = instance.getMap("cacheToken");
            if (!tokenMap.containsKey(accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }
            String nickname = tokenMap.get(accessToken);
            IMap<String, UserCacheModel> userMap = instance.getMap("users");
            UserCacheModel userCache = userMap.get(nickname);
            long userId = -1;
            if (userCache != null) {
                userId = userCache.getId();
            } else {
                try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                    ps.setString(1, nickname);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getLong("id");
                    }
                }
            }
            if (userId <= 0) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // Parse pagination
            int page = 1;
            int limit = 20;
            try {
                String pageStr = request.getParameter("page");
                if (pageStr != null && !pageStr.isEmpty()) page = Integer.parseInt(pageStr);
            } catch (NumberFormatException ignored) {}
            try {
                String limitStr = request.getParameter("limit");
                if (limitStr != null && !limitStr.isEmpty()) limit = Integer.parseInt(limitStr);
            } catch (NumberFormatException ignored) {}

            if (page < 1) page = 1;
            if (limit < 1) limit = 1;
            // SUN-XXXX: upper cap removed per ops request. Caller is
            // responsible for reasonable page size; very large values
            // (e.g. limit > 100k) can OOM the JVM on big result sets.
            int offset = (page - 1) * limit;

            // Unified deposit history: bank + crypto + bonus
            JSONArray dataArr = new JSONArray();
            int total = 0;
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");

                // Count total matching unified data query (bank + crypto + bonus + giftcode)
                ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM (" +
                        "SELECT dt.id FROM deposit_transactions dt WHERE dt.user_id = ? " +
                        "UNION ALL " +
                        "SELECT cd.id FROM crypto_deposits cd WHERE cd.user_id = ? " +
                        "UNION ALL " +
                        "SELECT dpl.id FROM deposit_promotion_logs dpl WHERE dpl.user_id = ? " +
                        "UNION ALL " +
                        "SELECT gcu.giftcode_id as id FROM gift_code_useds gcu " +
                        "JOIN gift_codes gc ON gc.id = gcu.giftcode_id " +
                        "WHERE gcu.user_id = ? AND gc.source = 'ADMIN' " +
                        ") AS unified_count");
                ps.setLong(1, userId);
                ps.setLong(2, userId);
                ps.setLong(3, userId);
                ps.setLong(4, userId);
                rs = ps.executeQuery();
                if (rs.next()) total = rs.getInt(1);
                rs.close();
                ps.close();

                // Union bank + crypto + bonus deposits.
                // 2026-05-12: fixed admin_banks JOIN bug (was `ab.status = 1`,
                // which arbitrarily attached whichever admin_bank was active
                // to every deposit). Now joins on dt.company_bank_id so each
                // row shows the exact receiving bank used at deposit time.
                // Also joins admin_banks → banks via the new admin_banks.bank_id
                // FK (2026-05-12 admin_banks_fk_bank_id migration) so the
                // canonical Korean bank_name ("씨티은행") shows in the player
                // history instead of the stored short code ("CITI").
                ps = conn.prepareStatement(
                        "(SELECT dt.tx_code, 'bank' as type, " +
                        // 2026-05-13: bank_name combined as "Korean (CODE)" so
                        // player history shows both, fallback to ab.bank_name
                        // or dt.user_bank_name if no canonical resolution.
                        "CASE WHEN b.bank_name IS NOT NULL AND b.code IS NOT NULL AND b.code <> '' " +
                        "     THEN CONCAT(b.bank_name, ' (', b.code, ')') " +
                        "     WHEN b.bank_name IS NOT NULL THEN b.bank_name " +
                        "     ELSE COALESCE(ab.bank_name, dt.user_bank_name) END as bank_name, " +
                        "COALESCE(ab.bank_number, dt.user_bank_number) as bank_number, " +
                        "COALESCE(ab.customer_name, '') as account_holder, " +
                        "dt.amount as amount_krw, 0 as amount_usdt, '' as tx_hash, '' as address, dt.status, " +
                        "COALESCE(dpl_agg.bonus_amount, 0) as bonus_amount, " +
                        "dt.amount as deposit_amount, " +
                        "COALESCE(dpl_agg.turnover_factor, 0) as turnover_factor, " +
                        "dt.created_at " +
                        "FROM deposit_transactions dt " +
                        "LEFT JOIN admin_banks ab ON ab.id = dt.company_bank_id " +
                        "LEFT JOIN banks b ON b.id = ab.bank_id " +
                        "LEFT JOIN (" +
                        "SELECT dpl.deposit_tx_id, " +
                        "SUM(dpl.bonus_amount) as bonus_amount, " +
                        "MAX(COALESCE(dp.turnover_factor, 0)) as turnover_factor " +
                        "FROM deposit_promotion_logs dpl " +
                        "LEFT JOIN deposit_promotions dp ON dp.id = dpl.promo_id " +
                        "GROUP BY dpl.deposit_tx_id" +
                        ") dpl_agg ON dpl_agg.deposit_tx_id = dt.id " +
                        "WHERE dt.user_id = ?) " +
                        "UNION ALL " +
                        "(SELECT gateway_tx_id as tx_code, 'crypto' as type, 'crypto' as bank_name, address as bank_number, " +
                        "'' as account_holder, " +
                        "amount_krw, amount_usdt, IFNULL(tx_hash,'') as tx_hash, address, " +
                        // SUN-prod: normalize crypto deposit status to the enum the FE
                        // already knows. crypto_deposits historically wrote 'SUCCESS'
                        // (NotifyCryptoDepositProcessor) — bank uses 'APPROVED', bonus
                        // uses 'COMPLETED'. The player FE on play.sunkr.club only maps
                        // APPROVED/COMPLETED to 'Thành công'; raw 'SUCCESS' fell through
                        // and rendered as 'Đang chờ' for every successful crypto deposit.
                        "CASE WHEN status = 'SUCCESS' THEN 'COMPLETED' ELSE status END as status, " +
                        "0 as bonus_amount, amount_krw as deposit_amount, 0 as turnover_factor, created_at " +
                        "FROM crypto_deposits WHERE user_id = ?) " +
                        "UNION ALL " +
                        "(SELECT CONCAT('BONUS-', dpl.id) as tx_code, 'bonus' as type, " +
                        "'Khuyến mãi' as bank_name, '' as bank_number, '' as account_holder, " +
                        "dpl.bonus_amount as amount_krw, 0 as amount_usdt, '' as tx_hash, '' as address, " +
                        "CASE WHEN dpl.is_completed = 1 THEN 'COMPLETED' ELSE 'PENDING' END as status, " +
                        "dpl.bonus_amount as bonus_amount, dpl.deposit_amount as deposit_amount, " +
                        "COALESCE(dp.turnover_factor, 0) as turnover_factor, " +
                        "dpl.created_at " +
                        "FROM deposit_promotion_logs dpl " +
                        "LEFT JOIN deposit_promotions dp ON dp.id = dpl.promo_id " +
                        "WHERE dpl.user_id = ?) " +
                        "UNION ALL " +
                        "(SELECT CONCAT('GC-', gc.giftcode) as tx_code, " +
                        "'bonus' as type, " +
                        "'Giftcode' as bank_name, '' as bank_number, '' as account_holder, " +
                        "gc.money as amount_krw, 0 as amount_usdt, '' as tx_hash, '' as address, " +
                        "'COMPLETED' as status, " +
                        "gc.money as bonus_amount, 0 as deposit_amount, " +
                        "gc.rollover_rounds as turnover_factor, " +
                        "gcu.created_at " +
                        "FROM gift_code_useds gcu " +
                        "JOIN gift_codes gc ON gc.id = gcu.giftcode_id " +
                        "WHERE gcu.user_id = ? AND gc.source = 'ADMIN') " +
                        "ORDER BY created_at DESC LIMIT ? OFFSET ?");
                ps.setLong(1, userId);
                ps.setLong(2, userId);
                ps.setLong(3, userId);
                ps.setLong(4, userId);
                ps.setInt(5, limit);
                ps.setInt(6, offset);
                rs = ps.executeQuery();

                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    row.put("tx_code", rs.getString("tx_code"));
                    row.put("type", rs.getString("type"));
                    row.put("bank_name", rs.getString("bank_name"));
                    row.put("bank_number", rs.getString("bank_number"));
                    row.put("account_holder", rs.getString("account_holder"));
                    row.put("amount_krw", rs.getLong("amount_krw"));
                    row.put("amount_usdt", rs.getDouble("amount_usdt"));
                    row.put("tx_hash", rs.getString("tx_hash"));
                    row.put("address", rs.getString("address"));
                    row.put("status", rs.getString("status"));
                    row.put("bonus_amount", rs.getLong("bonus_amount"));
                    row.put("deposit_amount", rs.getLong("deposit_amount"));
                    row.put("turnover_factor", rs.getDouble("turnover_factor"));
                    row.put("created_at", rs.getString("created_at"));
                    dataArr.put(row);
                }
            } finally {
                if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                if (ps != null) try { ps.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }

            int totalPages = Math.max(1, (total + limit - 1) / limit);

            response.put("success", true);
            response.put("data", dataArr);
            response.put("total", total);
            response.put("page", page);
            response.put("limit", limit);
            response.put("totalPages", totalPages);

        } catch (Exception e) {
            logger.error("CryptoDepositHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
