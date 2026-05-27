/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.dichvuthe.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dichvuthe.dao.ExchangeDao;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;

public class ExchangeDaoImpl
implements ExchangeDao {
    @Override
    public long getExchangeMoney(String merchantId, String nickname, String startTime, String endTime) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection collection = db.getCollection("log_exchange_money");
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        if (merchantId != null && !merchantId.isEmpty()) {
            conditions.put("merchant_id", merchantId);
        }
        if (nickname != null && !nickname.isEmpty()) {
            conditions.put("nick_name", nickname);
        }
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("trans_time", obj);
        }
        conditions.put("type", "0");
        Document dc = (Document)collection.aggregate(Arrays.asList(new Document[]{new Document("$match", new Document(conditions)), new Document("$group", new Document("_id", null).append("money", new Document("$sum", "$money")))})).first();
        long money = 0L;
        if (dc != null) {
            money = dc.getLong("money");
        }
        return money;
    }
}

