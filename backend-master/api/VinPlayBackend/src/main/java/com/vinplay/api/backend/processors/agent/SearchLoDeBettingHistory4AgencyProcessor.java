package com.vinplay.api.backend.processors.agent;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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

/**
 * c=9973 — Lô Đề betting history per agent subtree (SUN-1306 Phase C).
 *
 * <p>Mirrors {@link SearchGame3rdBettingHistory4AgencyProcessor} but sourced from
 * {@code log_money_user_vin} filtered on {@code service_name="LoDe"}. Each ticket
 * produces TWO rows in that collection (debit at bet, settle confirmation at
 * draw — see {@code LotteryModule.getResultLottery}), and this endpoint emits
 * each row as its own entry so the agency dashboard mirrors the player-facing
 * 2-record split required by the QC reopener.
 *
 * <p>Params:
 * <ul>
 *   <li>{@code rc}        — agent nickname (required)</li>
 *   <li>{@code nn}        — optional player nickname filter (substring LIKE)</li>
 *   <li>{@code ft}, {@code et} — yyyy-MM-dd from/to (matched against {@code trans_time})</li>
 *   <li>{@code pg}        — page (1-indexed, default 1)</li>
 *   <li>{@code size}      — page size (1..50, default 20)</li>
 *   <li>{@code hide_bot}  — default true; pass 0/false to include bot users</li>
 * </ul>
 *
 * <p>Response shape mirrors c=9842 (3rd-party history) — each entry has
 * {@code player, provider="Lô Đề", trans_id, bet, prize, balance_before,
 * balance_after, description, time}.
 */
public class SearchLoDeBettingHistory4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    // SUN-1338: ledger rows store action_name="LoDe" (ASCII source tag) and
    // service_name="Lô Đề" (Vietnamese, collation-fragile). Filter on
    // action_name to match what BetAcceptor / LotterySettleService publish as
    // source="LoDe" — same fix as GetGamePlayHistoryProcessor (c=303).
    private static final String SOURCE_TAG = "LoDe";
    private static final String PROVIDER_LABEL = "Lô Đề";

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String agentNick = request.getParameter("rc");
            String playerFilter = request.getParameter("nn");
            String fromTime = request.getParameter("ft");
            String toTime = request.getParameter("et");

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
                response.put("errorCode", "4001");
                response.put("message", "rc (agent nickname) is required");
                return response.toString();
            }

            // Resolve subtree player nicknames (reuse SQL from c=9842 sibling).
            List<String> playerNicks = getSubtreePlayerNicknames(agentNick, playerFilter, hideBot);
            if (playerNicks.isEmpty()) {
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", new JSONArray());
                response.put("total", 0);
                response.put("page", page);
                response.put("totalPages", 1);
                return response.toString();
            }

            List<JSONObject> rows = new ArrayList<>();
            int fetchLimit = Math.min(limit * 4 * Math.max(page, 1), 5_000);

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection<Document> col = db.getCollection("log_money_user_vin");

            Document query = new Document("action_name", SOURCE_TAG)
                    .append("nick_name", new Document("$in", playerNicks));
            // trans_time is a string field — string compare works fine for
            // yyyy-MM-dd HH:mm:ss prefix matching.
            Document timeCond = new Document();
            if (fromTime != null && !fromTime.isEmpty()) timeCond.put("$gte", fromTime);
            if (toTime != null && !toTime.isEmpty())   timeCond.put("$lte", toTime + " 23:59:59");
            if (!timeCond.isEmpty()) query.put("trans_time", timeCond);

            FindIterable<Document> docs = col.find(query)
                    .sort(new Document("trans_time", -1))
                    .limit(fetchLimit);

            for (Document doc : docs) {
                JSONObject row = new JSONObject();
                String p = doc.getString("nick_name");
                String transId = doc.get("trans_id") != null ? doc.get("trans_id").toString()
                        : (doc.get("_id") != null ? doc.get("_id").toString() : "");
                long amount = toLong(doc.get("money_exchange"));
                long currentMoney = toLong(doc.get("current_money"));
                String desc = doc.getString("description");
                String actionName = doc.getString("action_name");

                row.put("player", p != null ? p : "Unknown");
                row.put("provider", PROVIDER_LABEL);
                row.put("trans_id", transId);
                // Bet row: amount < 0 → bet = abs(amount), prize = 0.
                // Settle row: amount > 0 → bet = 0, prize = amount (win).
                // Loss settle row: amount == 0 → bet = prize = 0 (the description
                //   distinguishes lose vs draw — for Lô Đề draws are extremely rare).
                row.put("bet", amount < 0 ? Math.abs(amount) : 0L);
                row.put("prize", amount > 0 ? amount : 0L);
                row.put("balance_after", currentMoney);
                row.put("balance_before", currentMoney - amount);
                row.put("description", desc != null ? desc : "");
                row.put("action_name", actionName != null ? actionName : "");
                row.put("time", doc.getString("trans_time"));
                rows.add(row);
            }

            int total = rows.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) total / limit));
            int from = (page - 1) * limit;
            int to = Math.min(from + limit, total);

            JSONArray data = new JSONArray();
            for (int i = from; i < to; i++) data.put(rows.get(i));

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
            response.put("total", total);
            response.put("page", page);
            response.put("totalPages", totalPages);
        } catch (Exception e) {
            logger.error("SearchLoDeBettingHistory4AgencyProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

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
            logger.warn("SearchLoDeBettingHistory4Agency getSubtreePlayerNicknames error", e);
        }
        return nicks;
    }

    private static long toLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
