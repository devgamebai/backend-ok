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
 * c=9785 — Player × Game heatmap cells (top 200 precomputed).
 * Params: aat, window (default D7).
 * Returns list of cells {user_name, game_code, net, bet_count} plus lists of unique users/games for axes.
 */
public class GetPlayerGameHeatmapProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String window = request.getParameter("window"); if (window == null || window.isEmpty()) window = "D7";

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            JSONArray cells = new JSONArray();
            java.util.LinkedHashSet<String> users = new java.util.LinkedHashSet<>();
            java.util.LinkedHashSet<String> games = new java.util.LinkedHashSet<>();
            for (Document d : db.getCollection("game_heatmap_cache")
                    .find(new Document("window", window))
                    .sort(new Document("net", -1))) {
                JSONObject row = new JSONObject();
                String u = d.getString("user_name");
                String g = d.getString("game_code");
                row.put("user_name", u);
                row.put("game_code", g);
                row.put("net", GetHouseOverallPnlProcessor.getLong(d, "net"));
                row.put("total_bet", GetHouseOverallPnlProcessor.getLong(d, "total_bet"));
                row.put("bet_count", GetHouseOverallPnlProcessor.getLong(d, "bet_count"));
                cells.put(row);
                users.add(u);
                games.add(g);
            }
            response.put("success", true);
            response.put("errorCode", "0");
            response.put("window", window);
            response.put("cells", cells);
            response.put("users", new JSONArray(users));
            response.put("games", new JSONArray(games));
            response.put("cell_count", cells.length());
        } catch (Exception e) {
            logger.error("GetPlayerGameHeatmapProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
