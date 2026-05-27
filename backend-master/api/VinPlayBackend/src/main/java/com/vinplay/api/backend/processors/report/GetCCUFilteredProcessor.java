package com.vinplay.api.backend.processors.report;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * c=1080 - CCU with filters and pagination.
 * Params: ts (start, yyyy-MM-dd), te (end, yyyy-MM-dd), page, limit, platform, min_ccu, group_by
 */
public class GetCCUFilteredProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String startDate = request.getParameter("ts");
            String endDate = request.getParameter("te");
            String pageStr = request.getParameter("page");
            String limitStr = request.getParameter("limit");
            String platform = request.getParameter("platform");
            String minCcuStr = request.getParameter("min_ccu");

            // Defaults
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String today = sdf.format(new Date());
            if (startDate == null || startDate.isEmpty()) startDate = today;
            if (endDate == null || endDate.isEmpty()) endDate = today;
            // Ensure end date covers the full day
            if (endDate.length() == 10) endDate = endDate + " 23:59:59";
            if (startDate.length() == 10) startDate = startDate + " 00:00:00";

            int page = 1;
            int limit = 50;
            if (pageStr != null && !pageStr.isEmpty()) {
                page = Math.max(1, Integer.parseInt(pageStr));
            }
            if (limitStr != null && !limitStr.isEmpty()) {
                limit = Math.min(200, Math.max(1, Integer.parseInt(limitStr)));
            }
            int skip = (page - 1) * limit;

            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection<Document> coll = db.getCollection("log_ccu");

            // Build query
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            BasicDBObject timeRange = new BasicDBObject();
            timeRange.put("$gte", startDate);
            timeRange.put("$lte", endDate);
            conditions.put("time_log", timeRange);

            if (minCcuStr != null && !minCcuStr.isEmpty()) {
                int minCcu = Integer.parseInt(minCcuStr);
                conditions.put("ccu", new BasicDBObject("$gte", minCcu));
            }

            Bson query = new Document(conditions);

            // Count total
            long totalRecords = coll.count(query);

            // Fetch with pagination, sorted by time DESC
            BasicDBObject sort = new BasicDBObject("time_log", -1);
            FindIterable<Document> iterable = coll.find(query).sort(sort).skip(skip).limit(limit);

            JSONArray data = new JSONArray();
            for (Document doc : iterable) {
                JSONObject entry = new JSONObject();
                int ccu = doc.getInteger("ccu", 0);
                int web = doc.getInteger("web", 0);
                int ad = doc.getInteger("ad", 0);
                int ios = doc.getInteger("ios", 0);
                int wp = doc.getInteger("wp", 0);
                int fb = doc.getInteger("fb", 0);
                int dt = doc.getInteger("dt", 0);
                int ot = doc.getInteger("ot", 0);
                String timeLog = doc.getString("time_log");

                int phone = ad + ios;
                int desktop = dt;
                int other = wp + fb + ot;

                // Platform filter (post-query for simplicity)
                if (platform != null && !platform.isEmpty()) {
                    entry.put("total", ccu);
                    switch (platform.toLowerCase()) {
                        case "web": entry.put("platform_ccu", web); break;
                        case "phone": entry.put("platform_ccu", phone); break;
                        case "desktop": case "dt": entry.put("platform_ccu", desktop); break;
                        default: entry.put("platform_ccu", ccu); break;
                    }
                } else {
                    entry.put("total", ccu);
                    entry.put("web", web);
                    entry.put("phone", phone);
                    entry.put("desktop", desktop);
                    entry.put("other", other);
                }
                entry.put("time_log", timeLog != null ? timeLog : "");
                data.put(entry);
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("data", data);
            response.put("totalRecords", totalRecords);
            response.put("page", page);
            response.put("limit", limit);
            response.put("totalPages", (int) Math.ceil((double) totalRecords / limit));

        } catch (NumberFormatException e) {
            logger.error("GetCCUFilteredProcessor: invalid param", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        } catch (Exception e) {
            logger.error("GetCCUFilteredProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
