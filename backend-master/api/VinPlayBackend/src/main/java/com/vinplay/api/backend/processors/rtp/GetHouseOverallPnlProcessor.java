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
import java.util.Arrays;
import java.util.List;

/**
 * c=9780 — House overall P/L by game + window.
 * Params: aat, window (D1|D7|D30|ALL, default D7).
 * Returns per game_code: total_bet, total_payout, house_net, bet_count, unique_users.
 */
public class GetHouseOverallPnlProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String window = request.getParameter("window");
            if (window == null || window.isEmpty()) window = "D7";

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", new Document("window", window)));
            pipeline.add(new Document("$group", new Document()
                    .append("_id", "$game_code")
                    .append("total_bet", new Document("$sum", "$total_bet"))
                    .append("total_payout", new Document("$sum", "$total_payout"))
                    .append("bet_count", new Document("$sum", "$bet_count"))
                    .append("unique_users", new Document("$addToSet", "$user_name"))));
            pipeline.add(new Document("$project", new Document()
                    .append("game_code", "$_id")
                    .append("_id", 0)
                    .append("total_bet", 1)
                    .append("total_payout", 1)
                    .append("bet_count", 1)
                    .append("house_net", new Document("$subtract", Arrays.asList("$total_bet", "$total_payout")))
                    .append("unique_users", new Document("$size", "$unique_users"))));
            pipeline.add(new Document("$sort", new Document("house_net", -1)));

            JSONArray arr = new JSONArray();
            long totalBet = 0, totalPayout = 0, totalHouseNet = 0, totalRounds = 0;
            for (Document d : db.getCollection("user_pnl_summary").aggregate(pipeline)) {
                JSONObject row = new JSONObject();
                long bet = getLong(d, "total_bet"), pay = getLong(d, "total_payout");
                long hn = bet - pay;
                long cnt = getLong(d, "bet_count");
                row.put("game_code", d.getString("game_code"));
                row.put("total_bet", bet);
                row.put("total_payout", pay);
                row.put("house_net", hn);
                row.put("bet_count", cnt);
                row.put("unique_users", d.getInteger("unique_users", 0));
                arr.put(row);
                totalBet += bet; totalPayout += pay; totalHouseNet += hn; totalRounds += cnt;
            }

            JSONObject tot = new JSONObject();
            tot.put("total_bet", totalBet);
            tot.put("total_payout", totalPayout);
            tot.put("house_net", totalHouseNet);
            tot.put("bet_count", totalRounds);
            tot.put("achieved_rtp_pct", totalBet > 0 ? Math.round((totalPayout * 10000.0) / totalBet) / 100.0 : 0);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("window", window);
            response.put("by_game", arr);
            response.put("overall", tot);
        } catch (Exception e) {
            logger.error("GetHouseOverallPnlProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    static long getLong(Document d, String k) {
        Object v = d.get(k);
        if (v instanceof Number) return ((Number) v).longValue();
        return 0L;
    }
}
