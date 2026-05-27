package com.vinplay.api.backend.processors.taixiu;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * c=8807 — admin operator console endpoint for live bet monitoring on TaiXiu + Sicbo.
 *
 * <p>Returns the list of real players currently betting in the ACTIVE round
 * along with their bet side, total volume, and current wallet balance. Bots
 * excluded by default (filterable via {@code ?include_bot=1}).
 *
 * <p>Params:
 * <ul>
 *   <li>{@code aat}          admin token (required)
 *   <li>{@code game}         {@code taixiu} | {@code sicbo} (default taixiu)
 *   <li>{@code money_type}   {@code 1} (vin, default) | {@code 2} (xu)
 *   <li>{@code include_bot}  {@code 1} to include bots (default 0)
 *   <li>{@code limit}        max rows (default 200, cap 500)
 * </ul>
 *
 * <p>Data sources (stable, no game-engine hot-path change):
 * <ul>
 *   <li>Current {@code reference_id} — MySQL {@code vinplay_minigame.references}
 *       (single-row lookup, indexed by {@code game_id})
 *   <li>Bet list — Mongo live collections {@code user_bet_tai_xiu} /
 *       {@code user_bet_tai_xiu_sicbo} filtered by current {@code referentId}
 *       + {@code money_type}. Aggregated per (user_name, bet_side) so a user
 *       betting multiple times shows a single row with their cumulative volume.
 *   <li>Balance + bot flag — Hazelcast {@code users} IMap (UserCacheModel).
 *       Same cache every other processor already reads, so 0 extra load.
 * </ul>
 *
 * <p>Note: during a round transition (dice rolling phase) the {@code references.value}
 * may briefly lag the in-memory round counter by ≤ 1 s. This is acceptable
 * for an operator console that refreshes every 1-2 s.
 */
public class GetLiveBetListProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    // MySQL game_id → live-bet Mongo collection used to look up current bets.
    private static final int GAME_TAIXIU = 2;      // user_bet_tai_xiu
    private static final int GAME_SICBO = 31;      // user_bet_tai_xiu_sicbo

    // Sicbo bet-side id → human label. Mirrors the PotSicbo enum in
    // game.modules.minigame.entities (game/Minigame module) — we can't import
    // that class from the backend-api classpath, so the mapping is duplicated
    // here and kept in sync by convention. IDs 1-52.
    private static final String[] SICBO_LABELS = {
            /* 0  */ "UNKNOWN_0",
            /* 1  */ "POINT_4",   "POINT_5",   "POINT_6",   "POINT_7",   "POINT_8",
            /* 6  */ "POINT_9",   "POINT_10",  "POINT_11",  "POINT_12",  "POINT_13",
            /* 11 */ "POINT_14",  "POINT_15",  "POINT_16",  "POINT_17",
            /* 15 */ "ONE_DICE_1", "ONE_DICE_2", "ONE_DICE_3", "ONE_DICE_4", "ONE_DICE_5", "ONE_DICE_6",
            /* 21 */ "DOUBLE_DICES_1_1","DOUBLE_DICES_1_2","DOUBLE_DICES_1_3","DOUBLE_DICES_1_4","DOUBLE_DICES_1_5","DOUBLE_DICES_1_6",
            /* 27 */ "DOUBLE_DICES_2_2","DOUBLE_DICES_2_3","DOUBLE_DICES_2_4","DOUBLE_DICES_2_5","DOUBLE_DICES_2_6",
            /* 32 */ "DOUBLE_DICES_3_3","DOUBLE_DICES_3_4","DOUBLE_DICES_3_5","DOUBLE_DICES_3_6",
            /* 36 */ "DOUBLE_DICES_4_4","DOUBLE_DICES_4_5","DOUBLE_DICES_4_6",
            /* 39 */ "DOUBLE_DICES_5_5","DOUBLE_DICES_5_6",
            /* 41 */ "DOUBLE_DICES_6_6",
            /* 42 */ "TRIPLE_DICES_1","TRIPLE_DICES_2","TRIPLE_DICES_3","TRIPLE_DICES_4","TRIPLE_DICES_5","TRIPLE_DICES_6",
            /* 48 */ "TAI", "XIU", "CHAN", "LE",
            /* 52 */ "ANY_TRIPLE_DICES"
    };
    private static final int SICBO_TAI_ID = 48;
    private static final int SICBO_XIU_ID = 49;

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            // Admin auth (same pattern as ForceResultTaiXiu)
            String aat = request.getParameter("aat");
            if (aat == null || aat.isEmpty()) aat = request.getParameter("at");
            if (aat == null || aat.isEmpty()) return err(response, "1001", "Missing admin token");
            HazelcastInstance hz = HazelcastClientFactory.getInstance();
            IMap<String, String> tokenMap = hz.getMap("cacheToken");
            String adminNick = tokenMap.get(aat);
            if (adminNick == null) return err(response, "1001", "Invalid admin token");

            // Params
            String game = request.getParameter("game");
            if (game == null || game.isEmpty()) game = "taixiu";
            game = game.trim().toLowerCase();
            int gameId;
            String collectionName;
            if ("taixiu".equals(game)) {
                gameId = GAME_TAIXIU;
                collectionName = "user_bet_tai_xiu";
            } else if ("sicbo".equals(game)) {
                gameId = GAME_SICBO;
                collectionName = "user_bet_tai_xiu_sicbo";
            } else {
                return err(response, "4001", "game must be taixiu or sicbo");
            }

            int moneyType = parseIntOr(request.getParameter("money_type"), 1);
            if (moneyType != 1 && moneyType != 2) moneyType = 1;

            boolean includeBot = "1".equals(request.getParameter("include_bot"));

            int limit = parseIntOr(request.getParameter("limit"), 200);
            if (limit > 500) limit = 500;
            if (limit < 1) limit = 200;

            // Resolve current reference_id
            long referenceId = resolveReferenceId(gameId);
            if (referenceId < 0) {
                return err(response, "9999", "reference_id unavailable for game=" + game);
            }

            // Aggregate live bets from mongo per (user_name, bet_side).
            // Do not use log_taixiu/log_sicbo here: those are async history
            // collections and can differ from the in-round operator view.
            MongoDatabase mongoDb = MongoDBConnectionFactory.getDB();
            MongoCollection<Document> col = mongoDb.getCollection(collectionName);
            Document query = new Document("referentId", referenceId)
                    .append("money_type", moneyType);

            // key = user_name|bet_side → aggregated row
            Map<String, BetAggregate> aggMap = new LinkedHashMap<>();
            for (Document doc : col.find(query)) {
                String user = doc.getString("nick_name");
                if (user == null || user.isEmpty()) user = doc.getString("user_name");
                if (user == null || user.isEmpty()) continue;
                int betSide = toInt(doc.get("betSide"));
                long betValue = toLong(doc.get("betValue"));
                if (betValue <= 0) continue;

                String aggKey = user + "|" + betSide;
                BetAggregate agg = aggMap.get(aggKey);
                if (agg == null) {
                    agg = new BetAggregate();
                    agg.user = user;
                    agg.betSide = betSide;
                    aggMap.put(aggKey, agg);
                }
                agg.totalBet += betValue;
                agg.betCount++;
                long liveBalance = toLong(doc.get("balance"));
                if (liveBalance > 0L) {
                    agg.balance = liveBalance;
                }
            }

            // Balance + bot filter via Hazelcast users IMap
            IMap<String, UserCacheModel> userMap = hz.getMap("users");

            JSONArray bets = new JSONArray();
            long totalTaiReal = 0L, totalXiuReal = 0L;
            int numTaiReal = 0, numXiuReal = 0;
            int numBotRows = 0;

            int emitted = 0;
            for (BetAggregate agg : aggMap.values()) {
                UserCacheModel um = userMap.get(agg.user);
                boolean isBot = (um != null) ? um.isBot() : false;
                long balance = agg.balance > 0L ? agg.balance : ((um != null) ? um.getVin() : 0L);
                if (moneyType == 2) balance = agg.balance > 0L ? agg.balance : ((um != null) ? um.getXu() : 0L);

                if (isBot) numBotRows++;
                if (isBot && !includeBot) continue;

                // Totals per side (real only — matches operator risk view)
                if (!isBot) {
                    if (isTaiSide(gameId, agg.betSide)) {
                        totalTaiReal += agg.totalBet;
                        numTaiReal++;
                    } else if (isXiuSide(gameId, agg.betSide)) {
                        totalXiuReal += agg.totalBet;
                        numXiuReal++;
                    }
                }

                if (emitted >= limit) continue;
                JSONObject row = new JSONObject();
                row.put("nickname", agg.user);
                row.put("bet_side_id", agg.betSide);
                row.put("bet_side", labelOf(gameId, agg.betSide));
                row.put("total_bet", agg.totalBet);
                row.put("bet_count", agg.betCount);
                row.put("balance", balance);
                row.put("is_bot", isBot);
                bets.put(row);
                emitted++;
            }

            JSONObject totals = new JSONObject();
            JSONObject tai = new JSONObject();
            tai.put("total_bet", totalTaiReal); tai.put("num_real", numTaiReal);
            JSONObject xiu = new JSONObject();
            xiu.put("total_bet", totalXiuReal); xiu.put("num_real", numXiuReal);
            totals.put("tai", tai); totals.put("xiu", xiu);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("game", game);
            response.put("money_type", moneyType);
            response.put("reference_id", referenceId);
            response.put("include_bot", includeBot);
            response.put("total_rows", bets.length());
            response.put("real_bettors", numTaiReal + numXiuReal);
            response.put("bot_rows_excluded", includeBot ? 0 : numBotRows);
            response.put("totals", totals);
            response.put("bets", bets);

        } catch (Exception e) {
            logger.error("GetLiveBetListProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private long resolveReferenceId(int gameId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT value FROM `references` WHERE game_id = ? LIMIT 1")) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("value");
            }
        } catch (Exception e) {
            logger.warn("resolveReferenceId failed game=" + gameId + " err=" + e.getMessage());
        }
        return -1L;
    }

    private boolean isTaiSide(int gameId, int betSide) {
        if (gameId == GAME_TAIXIU) return betSide == 1;                  // 1 = TAI
        if (gameId == GAME_SICBO)  return betSide == SICBO_TAI_ID;
        return false;
    }

    private boolean isXiuSide(int gameId, int betSide) {
        if (gameId == GAME_TAIXIU) return betSide == 0;                  // 0 = XIU
        if (gameId == GAME_SICBO)  return betSide == SICBO_XIU_ID;
        return false;
    }

    private String labelOf(int gameId, int betSide) {
        if (gameId == GAME_TAIXIU) return betSide == 1 ? "TAI" : "XIU";
        if (gameId == GAME_SICBO) {
            if (betSide >= 0 && betSide < SICBO_LABELS.length) return SICBO_LABELS[betSide];
            return "UNKNOWN_" + betSide;
        }
        return "";
    }

    private int parseIntOr(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0L; }
    }

    private String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }

    private static class BetAggregate {
        String user;
        int betSide;
        long totalBet;
        int betCount;
        long balance;
    }
}
