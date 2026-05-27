package com.vinplay.usercore.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.vinplay.usercore.dao.UserLoginActivityDao;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.UserLoginLogModel;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class UserLoginActivityDaoImpl implements UserLoginActivityDao {

    @Override
    public List<UserLoginLogModel> searchLoginLogs(String username, String dateFrom, String dateTo, int page, int limit) {
        List<UserLoginLogModel> results = new ArrayList<>();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection<Document> col = db.getCollection("user_login_info");

            BasicDBObject query = new BasicDBObject();
            if (username != null && !username.isEmpty()) {
                query.put("user_name", username);
            }
            if (dateFrom != null && !dateFrom.isEmpty() && dateTo != null && !dateTo.isEmpty()) {
                BasicDBObject dateQuery = new BasicDBObject();
                dateQuery.put("$gte", dateFrom + " 00:00:00");
                dateQuery.put("$lte", dateTo + " 23:59:59");
                query.put("time_log", dateQuery);
            }

            MongoCursor<Document> cursor = col.find(query)
                    .sort(new BasicDBObject("time_log", -1)) // Optional: "created_at_ts" if it existed, but time_log covers all history.
                    .skip((page - 1) * limit)
                    .limit(limit)
                    .iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                UserLoginLogModel log = new UserLoginLogModel();
                log.setUsername(doc.getString("user_name"));
                log.setDevice_name(doc.getString("device_name"));
                log.setIp(doc.getString("ip"));
                log.setLocation(doc.getString("location"));
                log.setTime_log(doc.getString("time_log"));
                log.setPlatform(doc.getString("platform"));
                Boolean isAbnormal = doc.getBoolean("is_abnormal");
                log.setIs_abnormal(isAbnormal != null ? isAbnormal : false);
                results.add(log);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public long countLoginLogs(String username, String dateFrom, String dateTo) {
        long count = 0;
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection<Document> col = db.getCollection("user_login_info");

            BasicDBObject query = new BasicDBObject();
            if (username != null && !username.isEmpty()) {
                query.put("user_name", username);
            }
            if (dateFrom != null && !dateFrom.isEmpty() && dateTo != null && !dateTo.isEmpty()) {
                BasicDBObject dateQuery = new BasicDBObject();
                dateQuery.put("$gte", dateFrom + " 00:00:00");
                dateQuery.put("$lte", dateTo + " 23:59:59");
                query.put("time_log", dateQuery);
            }
            count = col.countDocuments(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
}
