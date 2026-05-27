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
 * c=9784 — Net P/L distribution histogram (user count per bucket).
 * Params: aat, window (default D7), game_code (optional — default 'ALL-AGGREGATED' across all games).
 *
 * Buckets (user POV net): ≤-10M, -10M..-1M, -1M..-100k, -100k..-10k, -10k..0, 0..10k, 10k..100k, 100k..1M, 1M..10M, >10M.
 */
public class GetPlayerDistributionProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static final long[] BOUNDS = {
        Long.MIN_VALUE, -10_000_000L, -1_000_000L, -100_000L, -10_000L, 0L,
        10_000L, 100_000L, 1_000_000L, 10_000_000L, Long.MAX_VALUE
    };
    private static final String[] LABELS = {
        "<=-10M", "-10M..-1M", "-1M..-100k", "-100k..-10k", "-10k..0",
        "0..10k", "10k..100k", "100k..1M", "1M..10M", ">10M"
    };

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();
            String window = request.getParameter("window"); if (window == null || window.isEmpty()) window = "D7";
            String gameCode = request.getParameter("game_code");

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            List<Document> pipe = new ArrayList<>();
            Document match = new Document("window", window);
            if (gameCode != null && !gameCode.isEmpty()) {
                match.append("game_code", gameCode);
            }
            pipe.add(new Document("$match", match));

            // If no game filter: aggregate user's net across all their games first
            if (gameCode == null || gameCode.isEmpty()) {
                pipe.add(new Document("$group", new Document()
                        .append("_id", "$user_name")
                        .append("net", new Document("$sum", "$net"))));
            } else {
                pipe.add(new Document("$project", new Document("net", 1).append("_id", 0)));
            }

            long[] counts = new long[LABELS.length];
            long positive = 0, negative = 0, zero = 0;
            long sumNet = 0, userCount = 0;

            for (Document d : db.getCollection("user_pnl_summary").aggregate(pipe)) {
                long net = GetHouseOverallPnlProcessor.getLong(d, "net");
                sumNet += net;
                userCount++;
                if (net > 0) positive++;
                else if (net < 0) negative++;
                else zero++;
                for (int i = 0; i < LABELS.length; i++) {
                    if (net > BOUNDS[i] && net <= BOUNDS[i + 1]) { counts[i]++; break; }
                }
            }

            JSONArray hist = new JSONArray();
            for (int i = 0; i < LABELS.length; i++) {
                JSONObject b = new JSONObject();
                b.put("bucket", LABELS[i]);
                b.put("user_count", counts[i]);
                hist.put(b);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("window", window);
            response.put("game_code", gameCode != null && !gameCode.isEmpty() ? gameCode : "ALL-GAMES");
            response.put("histogram", hist);
            response.put("total_users", userCount);
            response.put("positive_users", positive);
            response.put("negative_users", negative);
            response.put("zero_users", zero);
            response.put("avg_net", userCount > 0 ? (sumNet / userCount) : 0);
        } catch (Exception e) {
            logger.error("GetPlayerDistributionProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
            response.put("message", "Internal: " + e.getMessage());
        }
        return response.toString();
    }
}
