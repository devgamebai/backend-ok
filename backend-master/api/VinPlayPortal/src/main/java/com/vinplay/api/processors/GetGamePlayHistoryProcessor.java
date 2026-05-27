package com.vinplay.api.processors;

import com.hazelcast.core.IMap;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.service.impl.UserServiceImpl;
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
import java.util.Comparator;
import java.util.List;

/**
 * c=303 — "Lịch sử chơi game" — unified game play history for a player.
 *
 * Queries per-game log collections in Mongo (win123club) + MySQL (vinplay_minigame)
 * and returns a normalized, time-sorted, paginated list of game play records.
 *
 * Sources:
 *   Mongo: log_KhoBau, log_VuongQuocVin, log_SieuAnhHung, log_NuDiepVien,
 *          log_mini_poker, log_cao_thap, bau_cua_transaction, log_no_hu_slot
 *   MySQL: transaction_tai_xiu_md5, transaction_tai_xiu_sicbo
 *
 * Params: at (access token), nn (nickname), p (page, default 1), l (limit, default 15),
 *         game (optional filter: "slot", "taixiu", "baucua", "minipoker", "caothap", "sicbo", "all")
 *
 * Response: {success, totalPages, page, plays: [{game, bet, prize, result, time, detail}]}
 */
public class GetGamePlayHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");
    private static final int DEFAULT_LIMIT = 15;

    // Mongo source: {collection, game_label, user_field, bet_field, prize_field, game_id}
    // game_id matches Games enum + FE PopupHistoryPlay.GameName keys
    //
    // SUN-777 — Slot module names here are legacy placeholders that no longer match
    // the product name on the lobby icon. For now only ChiemTinhModule has been
    // remapped: it is the engine for "Pirate King" now. Full mapping + other slots
    // + proper fix plan: docs/SUN-777_slot-game-name-mapping.md
    //
    // ⚠ Also note: log_ChiemTinh receives cross-routed bets from ThanTaiRoom
    // (legacy logChiemtinh() write path) — those will also display as Pirate King
    // until the write path is fixed separately.
    private static final String[][] MONGO_SOURCES = {
        {"log_KhoBau",        "Kho Báu",       "user_name",  "bet_value", "prize", "20"},
        {"log_VuongQuocVin",  "Vương Quốc Vin","user_name",  "bet_value", "prize", "22"},
        {"log_SieuAnhHung",   "Siêu Anh Hùng","user_name",  "bet_value", "prize", "18"},
        {"log_NuDiepVien",    "Nữ Điệp Viên", "user_name",  "bet_value", "prize", "21"},
        {"log_ChiemTinh",     "Pirate King",   "user_name",  "bet_value", "prize", "55"}, // SUN-777: was "Chiêm Tinh"
        {"log_mini_poker",    "MiniPoker",     "user_name",  "bet_value", "prize", "1"},
        {"log_cao_thap",      "Cao Thấp",      "nick_name",  "bet_value", "prize", "4"},
    };

    /** Card games served from log_game / log_game_detail. Keyed by raw
     *  game_name as stored in the log; value carries display label and
     *  the game_id the FE uses. */
    private static final java.util.Map<String, String[]> CARD_GAME_META;
    static {
        java.util.LinkedHashMap<String, String[]> m = new java.util.LinkedHashMap<>();
        m.put("XocDia",  new String[]{"Xóc Đĩa",   "15"});
        m.put("Binh",    new String[]{"Bình",      "10"});
        m.put("Lieng",   new String[]{"Liêng",     "13"});
        m.put("TienLen", new String[]{"Tiến Lên",  "11"});
        m.put("BaCay",   new String[]{"Ba Cây",    "9"});
        m.put("BaiCao",  new String[]{"Bài Cào",   "16"});
        m.put("Sam",     new String[]{"Sâm",       "8"});
        m.put("Poker",   new String[]{"Poker",     "17"});
        m.put("XiZach",  new String[]{"Xì Zách",   "23"});
        m.put("Coup",    new String[]{"Coup",      "0"});
        m.put("Caro",    new String[]{"Caro",      "24"});
        m.put("CoTuong", new String[]{"Cờ Tướng",  "25"});
        CARD_GAME_META = java.util.Collections.unmodifiableMap(m);
    }

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String accessToken = request.getParameter("at");
            String nickName = request.getParameter("nn");
            int page = 1, limit = DEFAULT_LIMIT;
            try { if (request.getParameter("p") != null) page = Integer.parseInt(request.getParameter("p")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("l") != null) limit = Integer.parseInt(request.getParameter("l")); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;
            if (limit < 1 || limit > 50) limit = DEFAULT_LIMIT;
            String gameFilter = request.getParameter("game"); // optional: "slot","taixiu","baucua",...

            // Auth check
            UserServiceImpl userSer = new UserServiceImpl();
            if (!userSer.checkAccesstoken(nickName, accessToken)) {
                response.put("success", false);
                response.put("errorCode", "1001");
                return response.toString();
            }

            // SUN-1178: response cache. Same player viewing the same page +
            // filter pulls from cache (TTL 30 s for "current period" — matches
            // c=9541 / c=9843). Player only sees their own history; cache key
            // includes nick so isolation is automatic. New bets land via the
            // seamless API but the player typically polls c=303 every few s,
            // so a 30 s window absorbs traffic with no perceptible staleness.
            // Cache failures are silent (helper returns null on miss).
            final String cacheKey = com.vinplay.vbee.common.cache.ResponseCacheHelper.key(
                    "303", nickName, gameFilter, page, limit);
            String cachedResp = com.vinplay.vbee.common.cache.ResponseCacheHelper.get(cacheKey);
            if (cachedResp != null) {
                return cachedResp;
            }

            List<JSONObject> allPlays = new ArrayList<>();

            // === Core game data via shared GameHistoryService (same source as c=9843) ===
            // Floor of 60 so FE sees the player's realistic history depth on
            // page 1; cap grows with the requested page (page+1 buffer); top
            // bound 200 keeps the parallel mongo fan-out cheap. Players who
            // need older history should filter by date.
            int sourceFetchCap = Math.min(Math.max((page + 1) * limit, 60), 200);
            List<String> nicknames = java.util.Collections.singletonList(nickName);
            allPlays.addAll(com.vinplay.dal.service.GameHistoryService.fetchAll(
                    nicknames, gameFilter, null, null, sourceFetchCap));

            // === Card games (log_game / log_game_detail) — not in shared service yet ===
            MongoDatabase db = MongoDBConnectionFactory.getDB();

            // === Lô Đề (SUN-1306) — 2-record split per ticket from log_money_user_vin ===
            // Bet row: money_exchange < 0 (debit at buyTicket)
            // Settle row: money_exchange > 0 (win) OR == 0 (loss confirmation, SUN-1306 fix)
            // Each Mongo row is emitted as its own play entry so the FE sees both records.
            boolean wantLoDe = gameFilter == null || gameFilter.isEmpty()
                    || "all".equals(gameFilter) || "lode".equals(gameFilter)
                    || "lo de".equalsIgnoreCase(gameFilter) || "lô đề".equalsIgnoreCase(gameFilter);
            if (wantLoDe) {
                try {
                    // Filter on action_name (the legacy "source" tag) — the source value passed
                    // to UserServiceImpl.updateMoney(...) lands in Mongo {@code action_name},
                    // while {@code service_name} carries the human-readable gameId "Lô Đề"
                    // (UTF-8 with diacritics; collation-fragile). Filtering by action_name is
                    // ASCII-safe and matches what BetAcceptor / LotterySettleService pass as
                    // {@code source="LoDe"}.
                    for (Document doc : db.getCollection("log_money_user_vin")
                            .find(new Document("nick_name", nickName)
                                    .append("action_name", "LoDe"))
                            .sort(new Document("trans_time", -1))
                            .limit(limit * 3)) {
                        JSONObject play = new JSONObject();
                        play.put("game", "Lô Đề");
                        play.put("game_id", "67");
                        long amount = toLong(doc.get("money_exchange"));
                        // Debit row carries the bet; credit row carries the prize. Loss settle
                        // row has both bet=0 + prize=0 — the description distinguishes it.
                        play.put("bet",   amount < 0 ? Math.abs(amount) : 0L);
                        play.put("prize", amount > 0 ? amount : 0L);
                        long currentMoney = toLong(doc.get("current_money"));
                        play.put("balance_after", currentMoney);
                        play.put("balance_before", currentMoney - amount);
                        String desc = doc.get("description") != null ? doc.get("description").toString() : "";
                        play.put("description", desc);
                        play.put("result", desc); // FE rendering field — mode + ticket detail
                        play.put("time", normalizeTime(doc.get("create_time"), doc.get("trans_time")));
                        play.put("time_ms", extractTimeMs(doc.get("create_time"), doc.get("trans_time")));
                        allPlays.add(play);
                    }
                } catch (Exception lodeErr) {
                    logger.warn("c=303 LoDe history fetch failed: " + lodeErr.getMessage());
                }
            }

            // Old slot/BauCua/TaiXiu/Sicbo blocks removed — now via GameHistoryService.fetchAll() above
            if (false) { // dead code block — original MONGO_SOURCES loop removed
            for (String[] src : MONGO_SOURCES) {
                String collection = src[0], gameLabel = src[1], userField = src[2], betField = src[3], prizeField = src[4], gameId = src[5];

                // Apply game filter
                if (gameFilter != null && !gameFilter.isEmpty() && !"all".equals(gameFilter)) {
                    if ("slot".equals(gameFilter) && !collection.startsWith("log_") ) continue;
                    if ("slot".equals(gameFilter) && (collection.contains("mini_poker") || collection.contains("cao_thap"))) continue;
                    if ("minipoker".equals(gameFilter) && !collection.contains("mini_poker")) continue;
                    if ("caothap".equals(gameFilter) && !collection.contains("cao_thap")) continue;
                }

                try {
                    for (Document doc : db.getCollection(collection)
                            .find(new Document(userField, nickName))
                            .sort(new Document("create_time", -1))
                            .limit(limit * 3)) { // over-fetch then trim after merge-sort
                        JSONObject play = new JSONObject();
                        play.put("game", gameLabel);
                        play.put("game_id", gameId);
                        play.put("bet", toLong(doc.get(betField)));
                        play.put("prize", toLong(doc.get(prizeField)));
                        play.put("result", doc.get("result") != null ? doc.get("result").toString() : "");
                        play.put("time", normalizeTime(doc.get("create_time"), doc.get("time_log")));
                        play.put("time_ms", extractTimeMs(doc.get("create_time"), doc.get("time_log")));

                        // SUN-671: enrich per-game detail + current balance
                        StringBuilder detailSb = new StringBuilder();
                        if (collection.equals("log_mini_poker")) {
                            // MiniPoker: cards + result
                            if (doc.get("cards") != null) detailSb.append("cards: ").append(doc.get("cards"));
                        } else if (collection.equals("log_cao_thap")) {
                            // CaoThap: card + step
                            if (doc.get("cards") != null) detailSb.append("card: ").append(doc.get("cards"));
                            if (doc.get("step") != null) detailSb.append(" | step: ").append(toLong(doc.get("step")));
                        } else {
                            // Slots: lines_win + payout breakdown
                            if (doc.get("lines_win") != null && !doc.get("lines_win").toString().isEmpty()) {
                                detailSb.append("lines win: ").append(doc.get("lines_win"));
                                if (doc.get("prizes_on_line") != null) {
                                    detailSb.append(" | prizes: ").append(doc.get("prizes_on_line"));
                                }
                            } else if (doc.get("lines_betting") != null) {
                                detailSb.append("lines bet: ").append(doc.get("lines_betting"));
                            }
                        }
                        if (detailSb.length() > 0) play.put("detail", detailSb.toString());

                        // Note: current_fund is the GAME POT (nhà cái), NOT user balance.
                        // User balance is not stored in minigame Mongo logs.

                        allPlays.add(play);
                    }
                } catch (Exception e) {
                    logger.warn("GetGamePlayHistory: skip " + collection + " — " + e.getMessage());
                }
            }

            // === BauCua (special: sum bet/prize from 6 face fields) ===
            if (gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "baucua".equals(gameFilter)) {
                try {
                    for (Document doc : db.getCollection("bau_cua_transaction")
                            .find(new Document("user_name", nickName))
                            .sort(new Document("create_time", -1))
                            .limit(limit * 3)) {
                        long bet = toLong(doc.get("bet_bau")) + toLong(doc.get("bet_cua")) + toLong(doc.get("bet_tom"))
                                + toLong(doc.get("bet_ca")) + toLong(doc.get("bet_ga")) + toLong(doc.get("bet_huou"));
                        long prize = toLong(doc.get("prize_bau")) + toLong(doc.get("prize_cua")) + toLong(doc.get("prize_tom"))
                                + toLong(doc.get("prize_ca")) + toLong(doc.get("prize_ga")) + toLong(doc.get("prize_huou"));
                        JSONObject play = new JSONObject();
                        play.put("game", "Bầu Cua");
                        play.put("game_id", "3");
                        play.put("bet", bet);
                        play.put("prize", prize);
                        play.put("result", prize > bet ? "win" : (prize < bet ? "lose" : "draw"));
                        play.put("time", normalizeTime(doc.get("create_time"), doc.get("time_log")));
                        play.put("time_ms", extractTimeMs(doc.get("create_time"), doc.get("time_log")));
                        // SUN-671: BauCua detail — bets per face + dice result
                        StringBuilder bcDetail = new StringBuilder();
                        long bBau = toLong(doc.get("bet_bau")), bCua = toLong(doc.get("bet_cua")),
                             bTom = toLong(doc.get("bet_tom")), bCa = toLong(doc.get("bet_ca")),
                             bGa = toLong(doc.get("bet_ga")), bHuou = toLong(doc.get("bet_huou"));
                        if (bBau > 0) bcDetail.append("Bầu:").append(bBau).append(" ");
                        if (bCua > 0) bcDetail.append("Cua:").append(bCua).append(" ");
                        if (bTom > 0) bcDetail.append("Tôm:").append(bTom).append(" ");
                        if (bCa > 0) bcDetail.append("Cá:").append(bCa).append(" ");
                        if (bGa > 0) bcDetail.append("Gà:").append(bGa).append(" ");
                        if (bHuou > 0) bcDetail.append("Hươu:").append(bHuou).append(" ");
                        if (doc.get("dices") != null) bcDetail.append("| dice: ").append(doc.get("dices"));
                        if (bcDetail.length() > 0) play.put("detail", bcDetail.toString().trim());
                        allPlays.add(play);
                    }
                } catch (Exception e) {
                    logger.warn("GetGamePlayHistory: skip bau_cua — " + e.getMessage());
                }
            }

            // === Tài Xỉu (SUN-669: Mongo log_taixiu, MySQL fallback for old data) ===
            if (gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "taixiu".equals(gameFilter)) {
                int taixiuMongoCount = 0;
                try {
                    for (Document doc : db.getCollection("log_taixiu")
                            .find(new Document("user_name", nickName).append("money_type", 1))
                            .sort(new Document("create_time", -1))
                            .limit(limit * 3)) {
                        JSONObject play = new JSONObject();
                        play.put("game", "Tài Xỉu");
                        play.put("game_id", "33");
                        play.put("bet", toLong(doc.get("bet_value")));
                        long prize = toLong(doc.get("prize"));
                        play.put("prize", prize);
                        play.put("result", prize > 0 ? "win" : "lose");
                        play.put("detail", "side=" + (toLong(doc.get("bet_side")) == 0 ? "Xỉu" : "Tài"));
                        play.put("time", normalizeTime(doc.get("create_time"), null));
                        play.put("time_ms", extractTimeMs(doc.get("create_time"), null));
                        if (doc.get("current_money") != null) play.put("current_money", toLong(doc.get("current_money")));
                        allPlays.add(play);
                        taixiuMongoCount++;
                    }
                } catch (Exception e) {
                    logger.warn("GetGamePlayHistory: skip log_taixiu mongo — " + e.getMessage());
                }
                // Fallback to MySQL only if Mongo has no records (old historical data not yet migrated)
                if (taixiuMongoCount == 0) {
                    try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT bet_value, total_prize, bet_side, timestamp FROM transaction_tai_xiu_md5 " +
                                "WHERE user_name=? AND money_type=1 ORDER BY timestamp DESC LIMIT ?")) {
                            ps.setString(1, nickName);
                            ps.setInt(2, limit * 3);
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    JSONObject play = new JSONObject();
                                    play.put("game", "Tài Xỉu");
                                    play.put("game_id", "33");
                                    play.put("bet", rs.getLong("bet_value"));
                                    play.put("prize", rs.getLong("total_prize"));
                                    play.put("result", rs.getLong("total_prize") > 0 ? "win" : "lose");
                                    java.sql.Timestamp txTs = rs.getTimestamp("timestamp");
                                    play.put("time", normalizeTime(txTs, rs.getString("timestamp")));
                                    play.put("time_ms", extractTimeMs(txTs, null));
                                    play.put("detail", "side=" + (rs.getInt("bet_side") == 0 ? "Xỉu" : "Tài"));
                                    allPlays.add(play);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("GetGamePlayHistory: skip taixiu_md5 mysql fallback — " + e.getMessage());
                    }
                }
            }

            // === Sicbo (SUN-669: Mongo log_sicbo, MySQL fallback for old data) ===
            int sicboMongoCount = 0;
            if (gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "sicbo".equals(gameFilter)) {
                try {
                    for (Document doc : db.getCollection("log_sicbo")
                            .find(new Document("user_name", nickName).append("money_type", 1))
                            .sort(new Document("create_time", -1))
                            .limit(limit * 3)) {
                        JSONObject play = new JSONObject();
                        play.put("game", "Sicbo");
                        play.put("game_id", "30");
                        play.put("bet", toLong(doc.get("bet_value")));
                        long prize = toLong(doc.get("prize"));
                        play.put("prize", prize);
                        play.put("result", prize > 0 ? "win" : "lose");
                        play.put("detail", "ô " + toLong(doc.get("bet_side")));
                        play.put("time", normalizeTime(doc.get("create_time"), null));
                        play.put("time_ms", extractTimeMs(doc.get("create_time"), null));
                        if (doc.get("current_money") != null) play.put("current_money", toLong(doc.get("current_money")));
                        allPlays.add(play);
                        sicboMongoCount++;
                    }
                } catch (Exception e) {
                    logger.warn("GetGamePlayHistory: skip log_sicbo mongo — " + e.getMessage());
                }
            }
            // MySQL fallback for sicbo only if no Mongo records
            if ((gameFilter == null || gameFilter.isEmpty() || "all".equals(gameFilter) || "sicbo".equals(gameFilter)) && sicboMongoCount == 0) {
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT d.reference_id, d.bet_side, SUM(d.bet_value) AS bet, " +
                            "r.timestamp, r.dice1, r.dice2, r.dice3 " +
                            "FROM transaction_detail_tai_xiu_sicbo d " +
                            "LEFT JOIN result_tai_xiu_sicbo r ON r.reference_id = d.reference_id AND r.money_type = 1 " +
                            "WHERE d.user_name=? AND d.money_type=1 " +
                            "GROUP BY d.reference_id, d.bet_side, r.timestamp, r.dice1, r.dice2, r.dice3 " +
                            "ORDER BY d.reference_id DESC LIMIT ?")) {
                        ps.setString(1, nickName);
                        ps.setInt(2, limit * 3);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                JSONObject play = new JSONObject();
                                play.put("game", "Sicbo");
                                play.put("game_id", "30");
                                play.put("bet", rs.getLong("bet"));
                                int d1 = rs.getInt("dice1"), d2 = rs.getInt("dice2"), d3 = rs.getInt("dice3");
                                play.put("result", d1 + "+" + d2 + "+" + d3 + "=" + (d1 + d2 + d3));
                                play.put("detail", "ô " + rs.getInt("bet_side"));
                                java.sql.Timestamp sbTs = rs.getTimestamp("timestamp");
                                play.put("time", normalizeTime(sbTs, rs.getString("timestamp")));
                                play.put("time_ms", extractTimeMs(sbTs, null));
                                allPlays.add(play);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("GetGamePlayHistory: skip sicbo — " + e.getMessage());
                }
            }
            } // end if(false) dead code block

            // Card games — log_game (sessions) + log_game_detail (per-session
            // log). One $in query each instead of 12+N find()s. Backed by
            // idx_user_game_time_v1 on log_game.{nick_name,game_name,create_time}.
            java.util.List<String> allowedTypes = new java.util.ArrayList<>();
            for (String gType : CARD_GAME_META.keySet()) {
                if (gameFilter != null && !gameFilter.isEmpty() && !"all".equals(gameFilter)) {
                    String label = CARD_GAME_META.get(gType)[0];
                    if (!gType.toLowerCase().contains(gameFilter.toLowerCase())
                            && !label.toLowerCase().contains(gameFilter.toLowerCase())) continue;
                }
                allowedTypes.add(gType);
            }

            if (!allowedTypes.isEmpty()) {
                try {
                    java.util.List<Document> sessions = new java.util.ArrayList<>();
                    java.util.List<String> sessionIds = new java.util.ArrayList<>();
                    // Need enough rows to cover the requested page after the
                    // global time-merge with other sources. (page*limit) is
                    // the minimum slice; cap at 200 to bound memory/network.
                    int fetchCap = Math.min(Math.max(limit * 3, page * limit + limit), 200);
                    for (Document session : db.getCollection("log_game")
                            .find(new Document("nick_name", nickName)
                                    .append("game_name", new Document("$in", allowedTypes)))
                            .sort(new Document("create_time", -1))
                            .limit(fetchCap)) {
                        sessions.add(session);
                        String sid = session.getString("session_id");
                        if (sid != null && !sid.isEmpty()) sessionIds.add(sid);
                    }

                    java.util.Map<String, String> detailBySession = new java.util.HashMap<>();
                    if (!sessionIds.isEmpty()) {
                        for (Document detail : db.getCollection("log_game_detail")
                                .find(new Document("session_id", new Document("$in", sessionIds)))) {
                            String sid = detail.getString("session_id");
                            String log = detail.getString("log_detail");
                            if (sid != null && log != null && !detailBySession.containsKey(sid)) {
                                detailBySession.put(sid, log.substring(0, Math.min(200, log.length())));
                            }
                        }
                    }

                    for (Document session : sessions) {
                        String gType = session.getString("game_name");
                        String[] meta = gType == null ? null : CARD_GAME_META.get(gType);
                        if (meta == null) continue;
                        JSONObject play = new JSONObject();
                        play.put("game", meta[0]);
                        play.put("game_id", meta[1]);
                        play.put("bet", 0L);
                        play.put("prize", 0L);
                        play.put("result", "");
                        String sid = session.getString("session_id");
                        if (sid != null && detailBySession.containsKey(sid)) {
                            play.put("detail", detailBySession.get(sid));
                        }
                        play.put("time", normalizeTime(session.get("create_time"), session.get("time_log")));
                        play.put("time_ms", extractTimeMs(session.get("create_time"), session.get("time_log")));
                        allPlays.add(play);
                    }
                } catch (Exception e) {
                    logger.warn("GetGamePlayHistory: card games batch query failed — " + e.getMessage());
                }
            }

            // Sort all by time descending
            allPlays.sort((a, b) -> Long.compare(b.optLong("time_ms", 0), a.optLong("time_ms", 0)));

            // Backward-calculate currentMoney for records that don't have it.
            // Start from user's current balance and walk backwards through sorted plays.
            // Each play's balance = next play's balance + net(bet - prize) of current play.
            // This is approximate — deposits/withdrawals between bets are not accounted for.
            long runningBalance = 0;
            try {
                IMap<String, com.vinplay.vbee.common.models.cache.UserCacheModel> userMap =
                        com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance().getMap("users");
                com.vinplay.vbee.common.models.cache.UserCacheModel uc = userMap.get(nickName);
                if (uc != null) {
                    runningBalance = uc.getVin();
                } else {
                    try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                         PreparedStatement ps = conn.prepareStatement("SELECT vin FROM users WHERE nick_name = ?")) {
                        ps.setString(1, nickName);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) runningBalance = rs.getLong("vin");
                        }
                    }
                }
            } catch (Exception balErr) {
                logger.warn("GetGamePlayHistory: cannot get user balance for backward calc: " + balErr.getMessage());
            }
            // Walk from newest to oldest: runningBalance is the balance AFTER the most recent play.
            // When a record already has a writer-stamped balance, trust it and re-anchor the
            // backward walk so adjacent missing records stay consistent.
            //
            // SUN-1248: GameHistoryService canonically emits `money_before` for stamped rows
            // (log_gsc_bets / log_taixiu / log_sicbo with current_money populated). Older code
            // expected `current_money`. Check BOTH so the writer-stamped value is preferred
            // regardless of which field name carries it. Without this, GSC bets on c=303 fell
            // through to the backward-walk and drifted ~25M for active players whose total
            // wallet activity exceeded the records visible in `allPlays`.
            for (int i = 0; i < allPlays.size(); i++) {
                JSONObject p = allPlays.get(i);
                long storedMoney = p.optLong("current_money", 0);
                if (storedMoney <= 0) storedMoney = p.optLong("money_before", 0);
                if (storedMoney > 0) {
                    runningBalance = storedMoney;
                    p.put("current_money", storedMoney); // ensure FE-visible field carries it
                } else {
                    p.put("current_money", runningBalance);
                }
                long net = p.optLong("prize", 0) - p.optLong("bet", 0);
                runningBalance -= net;
            }

            // Paginate
            int totalRecords = allPlays.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / limit));
            int fromIdx = (page - 1) * limit;
            int toIdx = Math.min(fromIdx + limit, totalRecords);

            // Build response in BOTH new format (plays[]) AND legacy format (transactions[])
            // so existing client PopupHistoryPlay.loadPage() works without client change.
            JSONArray plays = new JSONArray();
            JSONArray transactions = new JSONArray();
            for (int i = fromIdx; i < toIdx; i++) {
                JSONObject p = allPlays.get(i);
                long timeMs = p.optLong("time_ms", 0);
                p.remove("time_ms");
                plays.put(p);

                // Legacy-compatible transaction format for PopupHistoryPlay.loadPage()
                JSONObject tx = new JSONObject();
                tx.put("transId", i + 1);
                tx.put("transactionTime", p.optString("time", ""));
                tx.put("serviceName", p.optString("game", ""));
                long net = p.optLong("prize", 0) - p.optLong("bet", 0);
                tx.put("moneyExchange", net);
                // SUN-671: surface current_money when game log has it (mini_poker / cao_thap have current_fund)
                tx.put("currentMoney", p.optLong("current_money", 0L));
                // Build description JSON that parseDescriptionJson can handle
                JSONObject desc = new JSONObject();
                // Use numeric game_id for FE switch(gameID) compatibility, fall back to game name
                desc.put("gameID", p.has("game_id") ? p.optString("game_id", "") : p.optString("game", ""));
                desc.put("type", 99); // custom type = raw display
                desc.put("totalbet", p.optLong("bet", 0));
                desc.put("totalPrizes", p.optLong("prize", 0));
                desc.put("result", p.optString("result", ""));
                // SUN-671: include detail (cards, lines win, dice, animals bet, etc.)
                if (p.has("detail")) desc.put("detail", p.optString("detail", ""));
                tx.put("description", desc.toString());
                transactions.put(tx);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("totalRecords", totalRecords);
            response.put("transactions", transactions);

            // SUN-1178: cache the rendered response for 30 s (live) / 300 s
            // (purely-historical date range — n/a here since c=303 has no
            // explicit date filter; treated as "current period"). Helper
            // swallows write errors and never fails the request.
            String json = response.toString();
            com.vinplay.vbee.common.cache.ResponseCacheHelper.put(cacheKey, json, false);
            return json;
        } catch (Exception e) {
            logger.error("GetGamePlayHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private static long toLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private static final java.text.SimpleDateFormat OUT_FMT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static { OUT_FMT.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul")); }

    /**
     * Normalize any time value to "yyyy-MM-dd HH:mm:ss" (KST).
     * Handles: Date object, epoch ms string, various string formats.
     */
    private static String normalizeTime(Object createTime, Object timeLog) {
        // Prefer create_time if it's a Date
        if (createTime instanceof java.util.Date) {
            synchronized (OUT_FMT) { return OUT_FMT.format((java.util.Date) createTime); }
        }
        // Try time_log
        if (timeLog != null) {
            String tl = timeLog.toString().trim();
            // Epoch ms (e.g. "1775283114644")
            if (tl.matches("^\\d{13}$")) {
                synchronized (OUT_FMT) { return OUT_FMT.format(new java.util.Date(Long.parseLong(tl))); }
            }
            // Already yyyy-MM-dd HH:mm:ss
            if (tl.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")) {
                return tl;
            }
            // HH:mm:ss dd-MM-yyyy
            if (tl.matches("^\\d{2}:\\d{2}:\\d{2} \\d{2}-\\d{2}-\\d{4}$")) {
                try {
                    java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    synchronized (OUT_FMT) { return OUT_FMT.format(in.parse(tl)); }
                } catch (Exception e) { return tl; }
            }
            return tl;
        }
        // Fallback: toString
        if (createTime != null) return createTime.toString();
        return "";
    }

    /** Extract time_ms (epoch) for sorting. */
    private static long extractTimeMs(Object createTime, Object timeLog) {
        if (createTime instanceof java.util.Date) return ((java.util.Date) createTime).getTime();
        if (timeLog != null) {
            String tl = timeLog.toString().trim();
            if (tl.matches("^\\d{13}$")) return Long.parseLong(tl);
        }
        return 0L;
    }
}
