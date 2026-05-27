/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.gamebase.dao.impl;

import com.gamebase.dao.LogMoneyUserExtendDao;
import com.gamebase.dao.model.SumUserDepositTimeResult;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.entities.log.LogMoneyUserNapTieuVinModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LogMoneyUserExtendDaoImpl
implements LogMoneyUserExtendDao {
    private final Logger logger = Logger.getLogger((String)"base_game");

    HashMap<String, Object> queryData(String nick_name, String fromTime, String endTime, String action_name) {
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (nick_name != null && !nick_name.isEmpty()) {
            conditions.put("nick_name", nick_name);
        }
        if (action_name != null && !action_name.isEmpty()) {
            conditions.put("action_name", action_name);
        }
        if (fromTime != null && !fromTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", fromTime);
            obj.put("$lte", endTime);
            conditions.put("trans_time", obj);
        }
        return conditions;
    }

    @Override
    public LogMoneyUserNapTieuVinModel getFirstUserDepositTime(String nick_name, String fromTime, String endTime, String action_name) {
        LogMoneyUserNapTieuVinModel firstRecord = null;
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("log_money_user_nap_vin");
            BasicDBObject objsort = new BasicDBObject();
            objsort.put("_id", -1);
            HashMap<String, Object> conditions = this.queryData(nick_name, fromTime, endTime, action_name);
            FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(0).limit(1);
            for (Object _raw : iterable) {
                Document document = (Document) _raw;
                firstRecord = new LogMoneyUserNapTieuVinModel(document.getLong("trans_id"), document.getInteger("user_id"), document.getString("nick_name"), document.getString("service_name"), document.getLong("current_money"), document.getLong("money_exchange"), document.getString("description"), document.getString("trans_time"), document.getString("action_name"), document.getLong("fee"), document.getDate("create_time"));
            }
            return firstRecord;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Long countUserDepositTime(String nick_name, String fromTime, String endTime, String action_name) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection col = db.getCollection("log_money_user_nap_vin");
            HashMap<String, Object> conditions = this.queryData(nick_name, fromTime, endTime, action_name);
            return col.count((Bson)new Document(conditions));
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    @Override
    public SumUserDepositTimeResult sumUserDepositTime(String nick_name, String fromTime, String endTime, String action_name) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
            MongoCollection collection = db.getCollection("log_money_user_nap_vin");
            HashMap<String, Object> conditions = this.queryData(nick_name, fromTime, endTime, action_name);
            List<Document> aggregate = Arrays.asList(new Document("$match", new Document(conditions)), new Document("$group", new Document("_id", "$nick_name").append("sum", new Document("$sum", "$money_exchange")).append("count", new Document("$sum", 1L))));
            Document result = (Document)collection.aggregate(aggregate).first();
            Long sum = result.getLong("sum");
            Long count = result.getLong("nick_name");
            return new SumUserDepositTimeResult(sum, count);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

