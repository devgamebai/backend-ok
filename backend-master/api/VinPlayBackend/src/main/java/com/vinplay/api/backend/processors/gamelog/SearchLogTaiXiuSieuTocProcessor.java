package com.vinplay.api.backend.processors.gamelog;

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

/**
 * c=162 -- Search log tai xiu sieu toc (list view, MongoDB).
 * Params: ts, te, p
 */
public class SearchLogTaiXiuSieuTocProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("backend");
    private static final int PAGE_SIZE = 20;
    private static final String COLLECTION_NAME = "log_taixiu_sieutoc";

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();
            String timeStart = request.getParameter("ts");
            String timeEnd = request.getParameter("te");
            String nickname = request.getParameter("nn");
            int page = 1;
            try { String s = request.getParameter("p"); if (s != null) page = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            if (page < 1) page = 1;

            JSONArray dataArr = new JSONArray();
            long total = 0;

            try {
                MongoDatabase db = MongoDBConnectionFactory.getDB();
                boolean exists = db.listCollectionNames()
                        .into(new java.util.ArrayList<String>())
                        .contains(COLLECTION_NAME);

                if (exists) {
                    MongoCollection<Document> col = db.getCollection(COLLECTION_NAME);
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
                logger.debug("SearchLogTaiXiuSieuTocProcessor MongoDB: " + e.getMessage());
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", dataArr);
            response.put("total", total);
                response.put("totalRecords", total);

        } catch (Exception e) {
            logger.error("SearchLogTaiXiuSieuTocProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "9999");
        }
        return response.toString();
    }
}
