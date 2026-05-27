/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.LuckyVipHistoryResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.LuckyVipHistoryDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.LuckyVipHistoryResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LuckyVipHistoryDAOImpl
implements LuckyVipHistoryDAO {
    @Override
    public List<LuckyVipHistoryResponse> searchLuckyVipHistory(String nickName, String timeStart, String timeEnd, int page) {
        final ArrayList<LuckyVipHistoryResponse> results = new ArrayList<LuckyVipHistoryResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        int numStart = (page - 1) * 50;
        int numEnd = 50;
        objsort.put("_id", -1);
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("lucky_vip_history").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LuckyVipHistoryResponse lucky = new LuckyVipHistoryResponse();
                lucky.trans_id = document.getLong("trans_id");
                lucky.nick_name = document.getString("nick_name");
                lucky.month = document.getString("month");
                lucky.result_vin = document.getInteger("result_vin");
                lucky.result_mutil = document.getInteger("result_multi");
                lucky.timelog = document.getString("time_log");
                results.add(lucky);
            }
        });
        return results;
    }

}

