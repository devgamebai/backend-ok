package com.vinplay.api.backend.processors.rtp;

import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * c=9781 — Top net-positive players (targeting list).
 * Params: aat, window (D1|D7|D30|ALL, default D7), game_code (optional),
 *         min_bets (default 30), limit (default 50).
 */
public class ListTopWinnersProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        return listByNet(param, true);
    }

    static String listByNet(Param<HttpServletRequest> param, boolean winnersFirst) {
        JSONObject response = new JSONObject();
        Logger log = Logger.getLogger("backend");
        try {
            HttpServletRequest request = param.get();
            String window = request.getParameter("window"); if (window == null || window.isEmpty()) window = "D7";
            String gameCode = request.getParameter("game_code");
            int minBets = 30, limit = 50;
            try { if (request.getParameter("min_bets") != null) minBets = Integer.parseInt(request.getParameter("min_bets")); } catch (NumberFormatException ignored) {}
            try { if (request.getParameter("limit") != null) limit = Integer.parseInt(request.getParameter("limit")); } catch (NumberFormatException ignored) {}
            if (limit < 1 || limit > 500) limit = 50;

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            List<Document> pipe = new ArrayList<>();
            Document match = new Document("window", window).append("bet_count", new Document("$gte", minBets));
            if (gameCode != null && !gameCode.isEmpty()) match.append("game_code", gameCode);
            // For winners: net > 0. For losers: net < 0.
            match.append("net", winnersFirst
                    ? new Document("$gt", 0)
                    : new Document("$lt", 0));
            pipe.add(new Document("$match", match));
            pipe.add(new Document("$sort", new Document("net", winnersFirst ? -1 : 1)));
            pipe.add(new Document("$limit", limit));

            JSONArray arr = new JSONArray();
            for (Document d : db.getCollection("user_pnl_summary").aggregate(pipe)) {
                JSONObject row = new JSONObject();
                row.put("user_name", d.getString("user_name"));
                row.put("game_code", d.getString("game_code"));
                row.put("total_bet", GetHouseOverallPnlProcessor.getLong(d, "total_bet"));
                row.put("total_payout", GetHouseOverallPnlProcessor.getLong(d, "total_payout"));
                row.put("net", GetHouseOverallPnlProcessor.getLong(d, "net"));
                row.put("bet_count", GetHouseOverallPnlProcessor.getLong(d, "bet_count"));
                row.put("first_seen", d.getString("first_seen"));
                row.put("last_seen", d.getString("last_seen"));
                arr.put(row);
            }
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("window", window);
            response.put("data", arr);
            response.put("count", arr.length());
        } catch (Exception e) {
            log.error("ListTopWinners/Losers error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
