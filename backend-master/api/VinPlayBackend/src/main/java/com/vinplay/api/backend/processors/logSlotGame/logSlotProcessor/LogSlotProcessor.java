package com.vinplay.api.backend.processors.logSlotGame.logSlotProcessor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * c=516 -- Log Slot games (MongoDB).
 * Params: nn (nickname), ts (time start), te (time end), p (page), gn (game name)
 */
public class LogSlotProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    private static final int PAGE_SIZE = 20;

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            String timeStart = request.getParameter("ts");
            String timeEnd = request.getParameter("te");
            String gameName = request.getParameter("gn");
            String gameType = request.getParameter("gameType");
            int page = 1;
            try { String s = request.getParameter("p"); if (s != null && !s.isEmpty()) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;

            // CMS sends gameType (numeric), map to collection name
            if ((gameName == null || gameName.isEmpty()) && gameType != null && !gameType.isEmpty()) {
                switch (gameType) {
                    case "1": gameName = "kimbinhmai"; break;
                    case "6": gameName = "pokemon"; break;
                    case "110": gameName = "taydu"; break;
                    case "120": gameName = "thantai"; break;
                    case "150": gameName = "thethao"; break;
                    case "160": gameName = "chimdien"; break;
                    case "170": gameName = "crypto"; break;
                    default: gameName = "slot_" + gameType; break;
                }
            }

            if (gameName == null || gameName.isEmpty()) {
                // Default: return empty success instead of error
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("data", new JSONArray());
                response.put("total", 0);
                response.put("totalRecords", 0);
                return response.toString();
            }

            // Try collection names: log_game_{gn}, then log_slot_{gn}
            String collectionName = "log_game_" + gameName;
            JSONArray dataArr = new JSONArray();
            long total = 0;

            try {
                MongoDatabase db = MongoDBConnectionFactory.getDB();
                MongoCollection<Document> col = null;

                if (db.listCollectionNames().into(new java.util.ArrayList<String>()).contains(collectionName)) {
                    col = db.getCollection(collectionName);
                } else {
                    String altName = "log_slot_" + gameName;
                    if (db.listCollectionNames().into(new java.util.ArrayList<String>()).contains(altName)) {
                        col = db.getCollection(altName);
                    }
                }

                if (col != null) {
                    Document filter = new Document();
                    if (nickname != null && !nickname.isEmpty()) {
                        filter.append("username", nickname);
                    }
                    if (timeStart != null && timeEnd != null && !timeStart.isEmpty() && !timeEnd.isEmpty()) {
                        filter.append("time_log", new Document("$gte", timeStart).append("$lte", timeEnd));
                    }

                    total = col.countDocuments(filter);
                    int skip = (page - 1) * PAGE_SIZE;

                    FindIterable<Document> docs = col.find(filter)
                            .sort(new Document("_id", -1))
                            .skip(skip)
                            .limit(PAGE_SIZE);

                    for (Document doc : docs) {
                        JSONObject item = new JSONObject(doc.toJson());
                        item.remove("_id");
                        dataArr.put(item);
                    }
                }
            } catch (Exception e) {
                logger.debug("LogSlotProcessor MongoDB query error: " + e.getMessage());
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", dataArr);
            response.put("total", total);

        } catch (Exception e) {
            logger.error("LogSlotProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
