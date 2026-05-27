package com.vinplay.api.backend.processors.awc;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * c=9895 — Admin endpoint: query AWC bet history from MongoDB log_awc_bets.
 *
 * Params: ?nn=username &platform=PP &game_type=SLOT &ft=2026-04-01 &et=2026-04-18
 *         &pg=1 &mi=20 &sort=bet_time &dir=desc
 */
public class GetAwcBetHistoryProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            String username = request.getParameter("nn");
            String platform = request.getParameter("platform");
            String gameType = request.getParameter("game_type");
            String dateFrom = request.getParameter("ft");
            String dateTo = request.getParameter("et");
            int page = parseIntOr(request.getParameter("pg"), 1);
            int limit = parseIntOr(request.getParameter("mi"), 20);
            if (limit > 200) limit = 200;
            if (limit < 1) limit = 20;

            String sortField = request.getParameter("sort");
            if (sortField == null || sortField.isEmpty()) sortField = "created_at";
            int sortDir = "asc".equalsIgnoreCase(request.getParameter("dir")) ? 1 : -1;

            com.mongodb.client.MongoDatabase db = MongoDBConnectionFactory.getDB();
            com.mongodb.client.MongoCollection<Document> col = db.getCollection("log_awc_bets");

            Document filter = new Document();
            if (username != null && !username.isEmpty()) {
                filter.append("user_name", username);
            }
            if (platform != null && !platform.isEmpty()) {
                filter.append("platform", platform);
            }
            if (gameType != null && !gameType.isEmpty()) {
                filter.append("game_type", gameType);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (dateFrom != null && !dateFrom.isEmpty()) {
                filter.append("created_at", new Document("$gte", sdf.parse(dateFrom)));
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                Date to = sdf.parse(dateTo);
                to.setTime(to.getTime() + 86400000L); // end of day
                Document createdFilter = (Document) filter.get("created_at");
                if (createdFilter != null) {
                    createdFilter.append("$lt", to);
                } else {
                    filter.append("created_at", new Document("$lt", to));
                }
            }

            long total = col.countDocuments(filter);
            int skip = (page - 1) * limit;

            List<Document> docs = new ArrayList<>();
            col.find(filter)
                    .sort(new Document(sortField, sortDir))
                    .skip(skip)
                    .limit(limit)
                    .into(docs);

            JSONArray arr = new JSONArray();
            for (Document doc : docs) {
                JSONObject row = new JSONObject();
                row.put("platform_tx_id", doc.getString("platform_tx_id"));
                row.put("round_id", doc.getString("round_id"));
                row.put("user_name", doc.getString("user_name"));
                row.put("platform", doc.getString("platform"));
                row.put("game_code", doc.getString("game_code"));
                row.put("game_name", doc.getString("game_name"));
                row.put("game_type", doc.getString("game_type"));
                row.put("action", doc.getString("action"));
                row.put("bet_amount", doc.get("bet_amount"));
                row.put("win_amount", doc.get("win_amount"));
                row.put("turnover", doc.get("turnover"));
                row.put("balance_after", doc.get("balance_after"));
                row.put("bet_time", doc.getString("bet_time"));
                row.put("created_at", doc.getDate("created_at") != null ? doc.getDate("created_at").toString() : "");
                arr.put(row);
            }

            long totalPages = Math.max(1L, (total + limit - 1) / limit);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", arr);
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", total);
            response.put("totalPages", totalPages);

        } catch (Exception e) {
            logger.error("GetAwcBetHistoryProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }

    private int parseIntOr(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
