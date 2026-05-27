package com.vinplay.api.backend.processors.agent;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.CreditWalletDao;
import com.vinplay.dal.dao.impl.CreditWalletDaoImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SearchGame3rdBettingHistory4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final String[][] MONGO_SOURCES = {
        // SUN-1364 — modern GSC seamless writes all third-party bets (Dream Gaming,
        // Evolution, Pragmatic Play, SABA Sports, etc) into a single Mongo collection
        // log_gsc_bets via GscBetSideEffectProcessor. Legacy per-provider collections
        // below remain for pre-GSC data and other aggregators (e.g. AWC).
        {"log_gsc_bets", "GSC"},
        {"log_game_ebet", "EBET Casino"},
        {"log_game_wm", "WM Casino"},
        {"log_game_ag", "AG Casino"},
        {"log_game_ibc", "IBC Sports"},
        {"log_game_sbo", "SBO Sports"},
        {"log_game_cmd", "CMD Sports"},
        {"log_game_fish", "Bắn Cá"},
        {"ebetgamerecord", "EBET Casino"},
        {"aggamerecord", "AG Casino"},
        {"log_evolution", "Evolution Casino"},
        {"log_game_evolution", "Evolution Casino"},
        {"log_live_casino", "Live Casino"}
    };

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentNick = request.getParameter("rc");
            String playerFilter = request.getParameter("nn");
            String providerFilter = request.getParameter("provider");
            String fromTime = request.getParameter("ft");
            String toTime = request.getParameter("et");

            // hide_bot: ẩn lịch sử cược của bot. Mặc định true (ẩn bot).
            // Truyền hide_bot=0 hoặc hide_bot=false để xem cả bot.
            String hideBotParam = request.getParameter("hide_bot");
            final boolean hideBot = hideBotParam == null
                    || !("0".equals(hideBotParam) || "false".equalsIgnoreCase(hideBotParam));

            int page = 1, limit = 20;
            try { if (request.getParameter("pg") != null) page = Integer.parseInt(request.getParameter("pg")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("size") != null) limit = Integer.parseInt(request.getParameter("size")); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 50) limit = 20;

            if (agentNick == null || agentNick.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1001");
                response.put("message", "rc (agent nickname) is required");
                return response.toString();
            }

            // SUN-863: resolve agent id for credit_balance lookup
            long agentCreditBalance = 0L;
            try (java.sql.Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 java.sql.PreparedStatement agPs = adminConn.prepareStatement(
                         "SELECT id FROM vinplay_admin.useragent WHERE nickname = ? LIMIT 1")) {
                agPs.setString(1, agentNick);
                try (java.sql.ResultSet agRs = agPs.executeQuery()) {
                    if (agRs.next()) {
                        int agentId = agRs.getInt(1);
                        try {
                            CreditWalletDao cwd = new CreditWalletDaoImpl();
                            agentCreditBalance = cwd.getBalance(agentId);
                        } catch (Exception cwErr) {
                            logger.warn("SearchGame3rdBettingHistory4AgencyProcessor: credit_wallet lookup failed agentNick=" + agentNick, cwErr);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Non-fatal — agent credit_balance defaults to 0
            }

            // 1. Lấy danh sách sub-trees (đại lý tuyến dưới)
            List<String> playerNicks = getSubtreePlayerNicknames(agentNick, playerFilter, hideBot);
            if (playerNicks.isEmpty()) {
                response.put("success", true); response.put("errorCode", "0");
                response.put("data", new JSONArray()); response.put("total", 0);
                response.put("page", page); response.put("totalPages", 1);
                response.put("credit_balance", 0);
                return response.toString();
            }

            List<JSONObject> allBets = new ArrayList<>();
            int fetchLimit = limit * 4;

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            List<String> existingCols = db.listCollectionNames().into(new ArrayList<>());

            for (String[] src : MONGO_SOURCES) {
                String colName = src[0];
                String providerLabel = src[1];
                
                if (providerFilter != null && !providerFilter.isEmpty() && !"all".equalsIgnoreCase(providerFilter)) {
                    if (!providerLabel.toLowerCase().contains(providerFilter.toLowerCase())) {
                        continue;
                    }
                }

                if (!existingCols.contains(colName)) continue;

                try {
                    MongoCollection<Document> col = db.getCollection(colName);
                    
                    // Build filter for multiple possible user fields
                    Document userFilter = new Document("$in", playerNicks);
                    List<Document> orUserConds = new ArrayList<>();
                    orUserConds.add(new Document("username", userFilter));
                    orUserConds.add(new Document("nick_name", userFilter));
                    orUserConds.add(new Document("nickName", userFilter));
                    
                    Document query = new Document("$or", orUserConds);

                    // Build date filter (trying multiple fields)
                    Document timeCond = new Document();
                    if (fromTime != null && !fromTime.isEmpty()) timeCond.put("$gte", fromTime);
                    if (toTime != null && !toTime.isEmpty()) timeCond.put("$lte", toTime + " 23:59:59");
                    
                    if (!timeCond.isEmpty()) {
                        List<Document> orTimeConds = new ArrayList<>();
                        orTimeConds.add(new Document("time_log", timeCond));
                        orTimeConds.add(new Document("createtime", timeCond));
                        orTimeConds.add(new Document("bettime", timeCond));
                        orTimeConds.add(new Document("create_time", timeCond));
                        query.put("$and", java.util.Arrays.asList(new Document("$or", orTimeConds)));
                    }

                    FindIterable<Document> docs = col.find(query).limit(fetchLimit);
                    for (Document doc : docs) {
                        JSONObject bet = new JSONObject();
                        
                        // User mapping
                        String p = doc.getString("username");
                        if (p == null) p = doc.getString("nick_name");
                        if (p == null) p = doc.getString("nickName");
                        
                        // TransID mapping
                        String transId = "";
                        if (doc.get("trans_id") != null) transId = doc.get("trans_id").toString();
                        else if (doc.get("billno") != null) transId = doc.get("billno").toString();
                        else if (doc.get("bethistoryid") != null) transId = doc.get("bethistoryid").toString();
                        else if (doc.get("ref_no") != null) transId = doc.get("ref_no").toString();
                        else if (doc.get("_id") != null) transId = doc.get("_id").toString();

                        // Bet / Prize / Net
                        // SUN-1370 — per-source bet extractor. Each collection has its
                        // own field naming (legacy log_evolution = bet_amount, log_game_*
                        // = bet/validbet, modern log_gsc_bets = bet_value with optional
                        // valid_bet_value override). Per-source switch avoids a unified
                        // fallback chain matching the wrong field on a future provider.
                        long betValue = extractBetValue(doc, colName);
                        
                        long prizeValue = toLong(doc.get("win_amount"));
                        if (prizeValue == 0) prizeValue = toLong(doc.get("payout"));
                        if (prizeValue == 0) prizeValue = toLong(doc.get("prize"));

                        // Time mapping
                        String time = doc.getString("time_log");
                        if (time == null) time = doc.getString("createtime");
                        if (time == null) time = doc.getString("bettime");
                        if (time == null && doc.get("create_time") != null) time = doc.get("create_time").toString();

                        bet.put("player", p != null ? p : "Unknown");
                        bet.put("provider", providerLabel);
                        bet.put("trans_id", transId);
                        bet.put("bet", betValue);
                        bet.put("prize", prizeValue);
                        bet.put("net", prizeValue - betValue); // Thắng/Thua
                        bet.put("result", prizeValue > betValue ? "Thắng" : "Thua");
                        bet.put("time", time != null ? time : "");
                        
                        allBets.add(bet);
                    }
                } catch (Exception e) {
                    logger.warn("SearchGame3rdBettingHistory4AgencyProcessor: skip " + colName + " - " + e.getMessage());
                }
            }

            // Sort + paginate
            allBets.sort((a, b) -> b.optString("time", "0").compareTo(a.optString("time", "0")));
            int total = allBets.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) total / limit));
            int from = (page - 1) * limit, to = Math.min(from + limit, total);

            JSONArray data = new JSONArray();
            for (int i = from; i < to; i++) {
                data.put(allBets.get(i));
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
            response.put("total", total);
            response.put("page", page);
            response.put("totalPages", totalPages);
            // SUN-863: agent credit_balance in response header
            response.put("credit_balance", agentCreditBalance);

        } catch (Exception e) {
            logger.error("SearchGame3rdBettingHistory4AgencyProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal Error: " + e.getMessage());
        }
        return response.toString();
    }

    /**
     * Lấy nick_name của tất cả player trong subtree của agent.
     * @param hideBot nếu true, thêm AND is_bot = 0 vào SQL để loại tài khoản bot.
     */
    private List<String> getSubtreePlayerNicknames(String agentNick, String playerFilter, boolean hideBot) {
        List<String> nicks = new ArrayList<>();
        try (Connection adminConn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
            int agentId = -1;
            try (PreparedStatement ps = adminConn.prepareStatement("SELECT id FROM useragent WHERE nickname=?")) {
                ps.setString(1, agentNick);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) agentId = rs.getInt(1); }
            }
            if (agentId <= 0) return nicks;

            List<Integer> ids = new ArrayList<>();
            ids.add(agentId);
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT id FROM useragent WHERE FIND_IN_SET(?, ancestors) > 0")) {
                ps.setInt(1, agentId);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getInt(1)); }
            }

            List<String> codes = new ArrayList<>();
            StringBuilder ph = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) { if (i > 0) ph.append(","); ph.append("?"); }
            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT code FROM useragent WHERE id IN (" + ph + ") AND code IS NOT NULL AND code!=''")) {
                for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) codes.add(rs.getString(1)); }
            }

            try (PreparedStatement ps = adminConn.prepareStatement(
                    "SELECT old_code FROM agent_code_history WHERE agent_id IN (" + ph + ")")) {
                for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) codes.add(rs.getString(1)); }
            }

            StringBuilder idPh = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) { if (i > 0) idPh.append(","); idPh.append("?"); }

            // botCond: điều kiện lọc bot tái sử dụng cho cả 2 vế UNION
            final String botCond = hideBot ? " AND is_bot = 0" : "";

            String sql = "SELECT nick_name FROM users WHERE " +
                    "(parent_agent_id IN (" + idPh + ")" + botCond;

            boolean hasCodes = !codes.isEmpty();
            if (hasCodes) {
                StringBuilder codePh = new StringBuilder();
                for (int i = 0; i < codes.size(); i++) { if (i > 0) codePh.append(","); codePh.append("?"); }
                sql += " OR referral_code COLLATE utf8mb4_general_ci IN (" + codePh + ")" + botCond;
            }
            sql += ")";

            if (playerFilter != null && !playerFilter.isEmpty()) sql += " AND nick_name LIKE ?";

            try (Connection userConn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = userConn.prepareStatement(sql)) {
                int idx = 1;
                for (int id : ids) ps.setInt(idx++, id);
                if (hasCodes) {
                    for (String c : codes) ps.setString(idx++, c);
                }
                if (playerFilter != null && !playerFilter.isEmpty()) ps.setString(idx, "%" + playerFilter + "%");
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) nicks.add(rs.getString(1)); }
            }
        } catch (Exception e) {
            logger.warn("getSubtreePlayerNicknames error", e);
        }
        return nicks;
    }

    private static long toLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * SUN-1370 — per-source bet extractor. Each Mongo collection has its own
     * field naming. Audited 2026-05-16 against real payloads in
     * {@code gsc_event_log.raw_payload} for products 1002 (Evolution) and
     * 1052 (Dream Gaming) — both expose flat {@code bet_amount} +
     * {@code valid_bet_amount} on the SETTLE deposit; the side-effect
     * writer ({@code GscBetSideEffectPublisher}) projects these into
     * {@code log_gsc_bets.bet_value} (via {@code $set} or {@code $inc}
     * depending on the event_key/multi-tx path) and into
     * {@code log_gsc_bets.valid_bet_value} on SETTLE_UPDATE.
     *
     * <p>Per-source switch (not a unified fallback chain) so a future
     * provider with a conflicting field name cannot silently match.
     */
    private static long extractBetValue(Document doc, String colName) {
        if ("log_gsc_bets".equals(colName)) {
            // Prefer commission-eligible valid_bet_value over the raw total
            // (SUN-1370 Dream Gaming multi-side hands push/void).
            long v = toLong(doc.get("valid_bet_value"));
            if (v == 0) v = toLong(doc.get("bet_value"));
            return v;
        }
        // Legacy per-provider collections (log_evolution, log_game_ag,
        // log_game_ibc, log_game_sbo, log_game_cmd, log_game_fish,
        // ebetgamerecord, aggamerecord, log_live_casino).
        long v = toLong(doc.get("bet_amount"));
        if (v == 0) v = toLong(doc.get("bet"));
        if (v == 0) v = toLong(doc.get("validbet"));
        return v;
    }
}
