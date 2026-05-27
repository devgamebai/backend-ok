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

/**
 * c=9783 — Per-user drill-down across all games for given window.
 * Params: aat, user_name (required), window (default D7).
 */
public class GetUserPnlDetailProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String userName = request.getParameter("user_name");
            String window = request.getParameter("window"); if (window == null || window.isEmpty()) window = "D7";
            if (userName == null || userName.isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "user_name required");
                return response.toString();
            }

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            JSONArray games = new JSONArray();
            long totalBet = 0, totalPayout = 0, totalNet = 0, totalCount = 0;
            for (Document d : db.getCollection("user_pnl_summary").find(
                    new Document("user_name", userName).append("window", window)).sort(new Document("net", -1))) {
                JSONObject row = new JSONObject();
                long b = GetHouseOverallPnlProcessor.getLong(d, "total_bet");
                long p = GetHouseOverallPnlProcessor.getLong(d, "total_payout");
                long n = GetHouseOverallPnlProcessor.getLong(d, "net");
                long c = GetHouseOverallPnlProcessor.getLong(d, "bet_count");
                row.put("game_code", d.getString("game_code"));
                row.put("total_bet", b);
                row.put("total_payout", p);
                row.put("net", n);
                row.put("bet_count", c);
                row.put("avg_bet", c > 0 ? (b / c) : 0);
                games.put(row);
                totalBet += b; totalPayout += p; totalNet += n; totalCount += c;
            }

            JSONObject tot = new JSONObject();
            tot.put("total_bet", totalBet);
            tot.put("total_payout", totalPayout);
            tot.put("net", totalNet);
            tot.put("bet_count", totalCount);
            tot.put("personal_rtp_pct", totalBet > 0 ? Math.round((totalPayout * 10000.0) / totalBet) / 100.0 : 0);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("user_name", userName);
            response.put("window", window);
            response.put("games", games);
            response.put("overall", tot);
        } catch (Exception e) {
            logger.error("GetUserPnlDetailProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
