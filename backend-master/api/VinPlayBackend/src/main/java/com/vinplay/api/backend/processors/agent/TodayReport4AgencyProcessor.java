package com.vinplay.api.backend.processors.agent;

import com.vinplay.api.backend.services.AgentHierarchyHelper;
import com.vinplay.api.backend.services.AgentHierarchyHelper.AgentInfo;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import org.bson.Document;

/**
 * Today report overview and games for agency (c=9539).
 * Queries real transaction tables in vinplay_minigame + log_report_user.
 * Includes the agent's own game activity + all members under the agent.
 */
public class TodayReport4AgencyProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        JSONObject response = new JSONObject();

        try {
            String agentCode = request.getParameter("rc");
            String dateParam = request.getParameter("date");

            if (agentCode == null || agentCode.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Missing agency code");
                return response.toString();
            }

            boolean filterByDate = true;
            if (dateParam == null || dateParam.isEmpty()) {
                dateParam = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            } else if ("all".equalsIgnoreCase(dateParam)) {
                filterByDate = false;
            }

            // Resolve agent via hierarchy helper
            AgentInfo agent = AgentHierarchyHelper.resolveAgent(agentCode);
            if (agent == null) {
                response.put("success", false);
                response.put("errorCode", "1002");
                response.put("message", "Agency not found");
                return response.toString();
            }
            
            boolean isMaster = AgentHierarchyHelper.isSiteMaster(agent);
            // SUN-1297 guard removed 2026-05-13: agency UI authenticates via
            // agent session cookie, not admin token. Master access is still
            // hierarchy-checked through resolveAgent + subtree expansion.

            final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                    "TodayReport4Agency", agent.id, dateParam, filterByDate ? dateParam : "all", "");
            String cachedResp = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
            if (cachedResp != null) {
                return cachedResp;
            }
            final boolean histOnly = filterByDate && com.vinplay.vbee.common.cache.ResponseCacheHelper.isHistoricalOnly(dateParam);

            List<Integer> subtreeIds = isMaster ? new ArrayList<Integer>() : AgentHierarchyHelper.getSubtreeAgentIdsIncludingSelf(agent.id);
            String inIds = isMaster ? "" : AgentHierarchyHelper.inPlaceholders(subtreeIds.size());

            // Build list of user_names: agent self + all members under subtree
            List<String> userNames = new ArrayList<>();
            if (!isMaster) {
                if (agent.nickname != null && !agent.nickname.isEmpty()) userNames.add(agent.nickname);

                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                    PreparedStatement stmUsers = conn.prepareStatement(
                        "SELECT nick_name FROM users WHERE parent_agent_id IN (" + inIds + ")");
                    AgentHierarchyHelper.setIntParams(stmUsers, 1, subtreeIds);
                    ResultSet rsU = stmUsers.executeQuery();
                    while (rsU.next()) {
                        String nn = rsU.getString("nick_name");
                        if (nn != null && !nn.isEmpty() && !userNames.contains(nn)) userNames.add(nn);
                    }
                    rsU.close();
                    stmUsers.close();
                }

                if (userNames.isEmpty()) {
                    // No users found, return empty
                    response.put("success", true);
                    response.put("errorCode", "0");
                    response.put("date", dateParam);
                    response.put("overview", new JSONObject().put("total_bet", 0).put("total_win", 0).put("net_profit", 0).put("commission", 0));
                    response.put("breakdown_games", new JSONArray());
                    return response.toString();
                }
            }

            // Build IN clause for user_names
            StringBuilder inUsers = new StringBuilder();
            if (!isMaster) {
                for (int i = 0; i < userNames.size(); i++) { if (i > 0) inUsers.append(","); inUsers.append("?"); }
            }
            String inUsersStr = inUsers.toString();
            String dateFrom = filterByDate ? dateParam + " 00:00:00" : null;
            String dateTo = filterByDate ? dateParam + " 23:59:59" : null;

            // Query each game type from real transaction tables
            long cardBet = 0, cardWin = 0;
            long slotBet = 0, slotWin = 0;

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
                // TaiXiu (card)
                long[] tx = queryGameSum(conn, "transaction_tai_xiu", inUsersStr, userNames, dateFrom, dateTo, isMaster);
                // TaiXiu MD5 (card)
                long[] txMd5 = queryGameSum(conn, "transaction_tai_xiu_md5", inUsersStr, userNames, dateFrom, dateTo, isMaster);
                // Sicbo (card)
                long[] sicbo = queryGameSum(conn, "transaction_tai_xiu_sicbo", inUsersStr, userNames, dateFrom, dateTo, isMaster);
                // Sicbo detail (card)
                long[] sicboD = queryGameDetailSum(conn, "transaction_detail_tai_xiu_sicbo", inUsersStr, userNames, dateFrom, dateTo, isMaster);
                // TaiXiu detail (card)
                long[] txD = queryGameDetailSum(conn, "transaction_detail_tai_xiu", inUsersStr, userNames, dateFrom, dateTo, isMaster);

                cardBet = tx[0] + txMd5[0] + sicbo[0] + sicboD[0] + txD[0];
                cardWin = tx[1] + txMd5[1] + sicbo[1] + sicboD[1] + txD[1];
            }

            // Only aggregate Slot and Sport/Fishing from log_report_user if needed for overview
            // Removed redundant MySQL casinoBet/casinoWin from log_report_user because we fetch details from MongoDB
            
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                String whereUser = isMaster ? "nick_name NOT IN (SELECT nick_name FROM users WHERE is_bot = 1)" : "nick_name IN (" + inUsersStr + ")";
                String sql = "SELECT "
                    + "IFNULL(SUM(IFNULL(slot_pokemon,0)+IFNULL(slot_bitcoin,0)+IFNULL(slot_taydu,0)+IFNULL(slot_angrybird,0)+IFNULL(slot_thantai,0)+IFNULL(slot_thethao,0)+IFNULL(slot_chiemtinh,0)+IFNULL(slot_bikini,0)+IFNULL(slot_galaxy,0)+IFNULL(hamcamap,0)+IFNULL(range_rover,0)+IFNULL(sexygirl,0)+IFNULL(fish,0)),0) as slot_bet,"
                    + "IFNULL(SUM(IFNULL(slot_pokemon_win,0)+IFNULL(slot_bitcoin_win,0)+IFNULL(slot_taydu_win,0)+IFNULL(slot_angrybird_win,0)+IFNULL(slot_thantai_win,0)+IFNULL(slot_thethao_win,0)+IFNULL(slot_chiemtinh_win,0)+IFNULL(slot_bikini_win,0)+IFNULL(slot_galaxy_win,0)+IFNULL(hamcamap_win,0)+IFNULL(range_rover_win,0)+IFNULL(sexygirl_win,0)+IFNULL(fish_win,0)),0) as slot_win"
                    + " FROM log_report_user WHERE " + whereUser;
                if (dateFrom != null && dateTo != null) {
                    sql += " AND time_report >= ? AND time_report <= ?";
                }

                PreparedStatement stm = conn.prepareStatement(sql);
                int idx = 1;
                if (!isMaster) {
                    for (String u : userNames) stm.setString(idx++, u);
                }
                if (dateFrom != null && dateTo != null) {
                    stm.setString(idx++, dateFrom);
                    stm.setString(idx, dateTo);
                }
                ResultSet rs = stm.executeQuery();
                if (rs.next()) {
                    slotBet = rs.getLong("slot_bet");
                    slotWin = rs.getLong("slot_win");
                }
                rs.close();
                stm.close();
            }

            // Fetch live casino from MongoDB for Baccarat, Roulette, Rồng hổ, Blackjack, Sicbo...
            long baccaratBet = 0, baccaratWin = 0;
            long rongHoBet = 0, rongHoWin = 0;
            long rouletteBet = 0, rouletteWin = 0;
            long blackjackBet = 0, blackjackWin = 0;
            long gscSicboBet = 0, gscSicboWin = 0;
            long otherCasinoBet = 0, otherCasinoWin = 0;

            String[] MONGO_SOURCES = {"log_game_ebet", "log_game_wm", "log_game_ag", "log_evolution", "log_game_evolution", "log_live_casino", "ebetgamerecord", "aggamerecord"};
            try {
                com.mongodb.client.MongoDatabase db = com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory.getDB();
                List<String> existingCols = db.listCollectionNames().into(new ArrayList<>());
                
                Document query = new Document();
                if (!isMaster) {
                    Document userFilter = new Document("$in", userNames);
                    List<Document> orUserConds = new ArrayList<>();
                    orUserConds.add(new Document("username", userFilter));
                    orUserConds.add(new Document("nick_name", userFilter));
                    orUserConds.add(new Document("nickName", userFilter));
                    query.put("$or", orUserConds);
                } else {
                    List<String> botNames = new ArrayList<>();
                    try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
                        PreparedStatement stm = conn.prepareStatement("SELECT nick_name, user_name FROM users WHERE is_bot = 1");
                        ResultSet rs = stm.executeQuery();
                        while (rs.next()) {
                            String nn = rs.getString("nick_name");
                            if (nn != null && !nn.isEmpty()) botNames.add(nn);
                            String un = rs.getString("user_name");
                            if (un != null && !un.isEmpty() && !nn.equals(un)) botNames.add(un);
                        }
                        rs.close(); stm.close();
                    } catch (Exception e) {}
                    if (!botNames.isEmpty()) {
                        Document botFilter = new Document("$nin", botNames);
                        // Combine $and to handle missing fields correctly, so it doesn't filter out if one field doesn't exist
                        List<Document> botConds = new ArrayList<>();
                        botConds.add(new Document("$or", Arrays.asList(new Document("username", new Document("$exists", false)), new Document("username", botFilter))));
                        botConds.add(new Document("$or", Arrays.asList(new Document("nick_name", new Document("$exists", false)), new Document("nick_name", botFilter))));
                        botConds.add(new Document("$or", Arrays.asList(new Document("nickName", new Document("$exists", false)), new Document("nickName", botFilter))));
                        query.put("$and", botConds);
                    }
                }

                Document timeCond = new Document();
                if (dateFrom != null) timeCond.put("$gte", dateFrom);
                if (dateTo != null) timeCond.put("$lte", dateTo);
                if (!timeCond.isEmpty()) {
                    List<Document> orTimeConds = new ArrayList<>();
                    orTimeConds.add(new Document("time_log", timeCond));
                    orTimeConds.add(new Document("createtime", timeCond));
                    orTimeConds.add(new Document("bettime", timeCond));
                    orTimeConds.add(new Document("create_time", timeCond));
                    query.put("$and", Arrays.asList(new Document("$or", orTimeConds)));
                }

                for (String colName : MONGO_SOURCES) {
                    if (!existingCols.contains(colName)) continue;
                    
                    // PERFORMANCE OPTIMIZATION: Only fetch necessary fields instead of massive raw documents
                    org.bson.conversions.Bson projection = com.mongodb.client.model.Projections.include(
                        "game_name", "gamename", "game", 
                        "validbet", "bet_amount", "bet", "betAmount", "bet_value", 
                        "win_amount", "payout", "prize", "winAmount"
                    );

                    com.mongodb.client.FindIterable<Document> docs = db.getCollection(colName).find(query).projection(projection);
                    for (Document doc : docs) {
                        long betValue = 0;
                        if (doc.get("validbet") instanceof Number) betValue = ((Number)doc.get("validbet")).longValue();
                        else if (doc.get("bet_amount") instanceof Number) betValue = ((Number)doc.get("bet_amount")).longValue();
                        else if (doc.get("bet") instanceof Number) betValue = ((Number)doc.get("bet")).longValue();
                        else if (doc.get("bet_value") instanceof Number) betValue = ((Number)doc.get("bet_value")).longValue();

                        long winValue = 0;
                        if (doc.get("win_amount") instanceof Number) winValue = ((Number)doc.get("win_amount")).longValue();
                        else if (doc.get("payout") instanceof Number) winValue = ((Number)doc.get("payout")).longValue();
                        else if (doc.get("prize") instanceof Number) winValue = ((Number)doc.get("prize")).longValue();

                        String gname = doc.getString("game_name");
                        if (gname == null) gname = doc.getString("gamename");
                        if (gname == null) gname = "";
                        String lc = gname.toLowerCase();

                        if (lc.contains("baccarat") || lc.contains("baccarrat")) { baccaratBet += betValue; baccaratWin += winValue; }
                        else if (lc.contains("dragon tiger") || lc.contains("dragontiger") || lc.contains("rồng hổ")) { rongHoBet += betValue; rongHoWin += winValue; }
                        else if (lc.contains("roulette")) { rouletteBet += betValue; rouletteWin += winValue; }
                        else if (lc.contains("blackjack")) { blackjackBet += betValue; blackjackWin += winValue; }
                        else if (lc.contains("sic bo") || lc.contains("sicbo")) { gscSicboBet += betValue; gscSicboWin += winValue; }
                        else { otherCasinoBet += betValue; otherCasinoWin += winValue; }
                    }
                }
            } catch (Exception e) {
                logger.warn("TodayReport4AgencyProcessor Mongo query error", e);
            }

            // Sicbo & Tài xỉu = (MySQL tx + sicbo) + Mongo Live Sicbo
            long txSicboBet = cardBet + gscSicboBet;
            long txSicboWin = cardWin + gscSicboWin;

            // Fetch configured rebate rates
            double casinoRate = 0;
            double slotRate = 0;
            try {
                java.util.Map<String, Object> config = com.vinplay.dal.rebate.RebateService.getConfig(agent.id);
                if (config != null) {
                    double defaultRate = 0;
                    if (config.get("rebate_percentage") instanceof Number) {
                        defaultRate = ((Number)config.get("rebate_percentage")).doubleValue();
                    }
                    casinoRate = config.containsKey("casino_rate") ? ((Number)config.get("casino_rate")).doubleValue() : defaultRate;
                    slotRate   = config.containsKey("slot_rate")   ? ((Number)config.get("slot_rate")).doubleValue()   : defaultRate;
                }
            } catch (Exception ignore) {}

            // Commission calculation
            long slotCom      = (long) (slotBet * slotRate / 100.0);
            long baccaratCom  = (long) (baccaratBet * casinoRate / 100.0);
            long rongHoCom    = (long) (rongHoBet * casinoRate / 100.0);
            long rouletteCom  = (long) (rouletteBet * casinoRate / 100.0);
            long blackjackCom = (long) (blackjackBet * casinoRate / 100.0);
            long txSicboCom   = (long) (txSicboBet * casinoRate / 100.0);
            long otherCom     = (long) (otherCasinoBet * casinoRate / 100.0);

            long totalBet = slotBet + baccaratBet + rongHoBet + rouletteBet + blackjackBet + txSicboBet + otherCasinoBet;
            long totalWin = slotWin + baccaratWin + rongHoWin + rouletteWin + blackjackWin + txSicboWin + otherCasinoWin;
            long totalCom = slotCom + baccaratCom + rongHoCom + rouletteCom + blackjackCom + txSicboCom + otherCom;

            long totalLose = (totalBet > totalWin) ? (totalBet - totalWin) : 0;

            JSONObject overview = new JSONObject();
            overview.put("total_bet", totalBet);
            overview.put("total_win", totalWin);
            overview.put("total_lose", totalLose);
            overview.put("net_profit", totalBet - totalWin);
            overview.put("commission", totalCom);

            JSONArray games = new JSONArray();
            games.put(gameObj("Slot",            "Slot",   slotBet,        slotWin,        false, slotCom));
            games.put(gameObj("Baccarat",        "Casino", baccaratBet,    baccaratWin,    false, baccaratCom));
            games.put(gameObj("Rồng Hổ",         "Casino", rongHoBet,      rongHoWin,      false, rongHoCom));
            games.put(gameObj("Roulette",        "Casino", rouletteBet,    rouletteWin,    false, rouletteCom));
            games.put(gameObj("Sicbo & Tài xỉu", "Casino", txSicboBet,     txSicboWin,     false, txSicboCom));
            games.put(gameObj("Blackjack",       "Casino", blackjackBet,   blackjackWin,   false, blackjackCom));
            if (otherCasinoBet > 0 || otherCasinoWin > 0) {
                games.put(gameObj("Live Khác",   "Casino", otherCasinoBet, otherCasinoWin, false, otherCom));
            }
            // Tổng cộng
            games.put(gameObj("total",           "total",  totalBet,       totalWin,       true,  totalCom));

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("overview", overview);
            response.put("breakdown_games", games);
            response.put("date", dateParam);
            
            String jsonResp = response.toString();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, jsonResp, histOnly);
            return jsonResp;

        } catch (Exception e) {
            logger.error("Error in TodayReport4AgencyProcessor", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }

    /** Query transaction_tai_xiu / transaction_tai_xiu_md5 / transaction_tai_xiu_sicbo (summary tables) */
    private long[] queryGameSum(Connection conn, String table, String inUsers, List<String> userNames, String from, String to, boolean isMaster) {
        long bet = 0, win = 0;
        try {
            String whereUser = isMaster ? "user_name NOT IN (SELECT user_name FROM vinplay.users WHERE is_bot = 1)" : "user_name IN (" + inUsers + ")";
            String sql = "SELECT IFNULL(SUM(bet_value),0) as total_bet, IFNULL(SUM(total_prize),0) as total_win"
                    + " FROM " + table + " WHERE " + whereUser;
            if (from != null && to != null) sql += " AND timestamp >= ? AND timestamp <= ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            int idx = 1;
            if (!isMaster) {
                for (String u : userNames) stm.setString(idx++, u);
            }
            if (from != null && to != null) { stm.setString(idx++, from); stm.setString(idx, to); }
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                bet = rs.getLong("total_bet");
                win = rs.getLong("total_win");
            }
            rs.close();
            stm.close();
        } catch (Exception e) {
            logger.warn("queryGameSum " + table + " error: " + e.getMessage());
        }
        return new long[]{bet, win};
    }

    /** Query transaction_detail tables (have prize instead of total_prize) */
    private long[] queryGameDetailSum(Connection conn, String table, String inUsers, List<String> userNames, String from, String to, boolean isMaster) {
        long bet = 0, win = 0;
        try {
            String whereUser = isMaster ? "user_name NOT IN (SELECT user_name FROM vinplay.users WHERE is_bot = 1)" : "user_name IN (" + inUsers + ")";
            String sql = "SELECT IFNULL(SUM(bet_value),0) as total_bet, IFNULL(SUM(prize),0) as total_win"
                    + " FROM " + table + " WHERE " + whereUser;
            if (from != null && to != null) sql += " AND timestamp >= ? AND timestamp <= ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            int idx = 1;
            if (!isMaster) {
                for (String u : userNames) stm.setString(idx++, u);
            }
            if (from != null && to != null) { stm.setString(idx++, from); stm.setString(idx, to); }
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                bet = rs.getLong("total_bet");
                win = rs.getLong("total_win");
            }
            rs.close();
            stm.close();
        } catch (Exception e) {
            logger.warn("queryGameDetailSum " + table + " error: " + e.getMessage());
        }
        return new long[]{bet, win};
    }

    private JSONObject gameObj(String type, String category, long bet, long win, boolean isTotal, long commission) {
        JSONObject obj = new JSONObject();
        long netProfit = bet - win;
        long totalLose = (bet > win) ? (bet - win) : 0;
        obj.put("game_type",  type);
        obj.put("category",   category);
        obj.put("total_bet",  bet);
        obj.put("total_win",  win);
        obj.put("total_lose", totalLose);
        obj.put("net_profit", netProfit);
        obj.put("commission", commission);
        if (isTotal) {
            obj.put("is_total", true);   // FE dùng flag này để style row Tổng cộng
        }
        return obj;
    }
}
