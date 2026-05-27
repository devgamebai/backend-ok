package com.vinplay.api.backend.processors.game3rd;

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

/**
 * Shared helper for 3rd party game log MongoDB queries.
 */
public class Game3rdLogHelper {

    public static String executeMongoQuery(Param<HttpServletRequest> param, String collectionName, int pageSize, Logger logger, String processorName) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String nickname = request.getParameter("nn");
            String timeStart = request.getParameter("ts");
            String timeEnd = request.getParameter("te");
            int page = 1;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;

            JSONArray dataArr = new JSONArray();
            long total = 0;

            try {
                MongoDatabase db = MongoDBConnectionFactory.getDB();
                boolean collectionExists = db.listCollectionNames()
                        .into(new java.util.ArrayList<String>())
                        .contains(collectionName);

                if (collectionExists) {
                    MongoCollection<Document> col = db.getCollection(collectionName);
                    Document filter = new Document();
                    if (nickname != null && !nickname.isEmpty()) {
                        filter.append("username", nickname);
                    }
                    if (timeStart != null && timeEnd != null && !timeStart.isEmpty() && !timeEnd.isEmpty()) {
                        filter.append("time_log", new Document("$gte", timeStart).append("$lte", timeEnd));
                    }

                    total = col.countDocuments(filter);
                    int skip = (page - 1) * pageSize;

                    FindIterable<Document> docs = col.find(filter)
                            .sort(new Document("_id", -1))
                            .skip(skip)
                            .limit(pageSize);

                    for (Document doc : docs) {
                        JSONObject item = new JSONObject(doc.toJson());
                        item.remove("_id");
                        dataArr.put(item);
                    }
                }
            } catch (Exception e) {
                logger.debug(processorName + " MongoDB query: " + e.getMessage());
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", dataArr);
            response.put("total", total);

        } catch (Exception e) {
            logger.error(processorName + " error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
