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
 *  com.vinplay.vbee.common.response.LogCaoThapResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.LogCaoThapDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.LogCaoThapResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LogCaoThapDAOImpl
implements LogCaoThapDAO {
    @Override
    public List<LogCaoThapResponse> listCaoThap(String nickName, String transId, String bet_value, String timeStart, String timeEnd, String moneyType, int page) {
        final ArrayList<LogCaoThapResponse> results = new ArrayList<LogCaoThapResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        int num_start = (page - 1) * 50;
        int num_end = 50;
        objsort.put("_id", -1);
        if (nickName != null && !nickName.equals("")) {
            String pattern = ".*" + nickName + ".*";
            conditions.put("nick_name", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
        }
        if (transId != null && !transId.equals("")) {
            conditions.put("trans_id", Long.parseLong(transId));
        }
        if (bet_value != null && !bet_value.equals("")) {
            conditions.put("bet_value", Long.parseLong(bet_value));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        if (moneyType != null && !moneyType.equals("")) {
            conditions.put("money_type", Integer.parseInt(moneyType));
        }
        iterable = db.getCollection("log_cao_thap").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogCaoThapResponse caothap = new LogCaoThapResponse();
                caothap.tran_id = document.getLong("trans_id");
                caothap.nickName = document.getString("nick_name");
                caothap.pot_bet = document.getInteger("pot_bet");
                caothap.step = document.getInteger("step");
                caothap.bet_value = document.getLong("bet_value");
                caothap.result = document.getInteger("result");
                caothap.prize = document.getLong("prize");
                caothap.cards = document.getString("cards");
                caothap.current_pot = document.getLong("current_pot");
                caothap.current_fund = document.getLong("current_fund");
                caothap.money_type = document.getInteger("money_type");
                caothap.time_log = document.getString("time_log");
                results.add(caothap);
            }
        });
        return results;
    }

    @Override
    public int countCaoThap(String nickName, String transId, String bet_value, String timeStart, String timeEnd, String moneyType) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (nickName != null && !nickName.equals("")) {
            String pattern = ".*" + nickName + ".*";
            conditions.put("nick_name", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
        }
        if (transId != null && !transId.equals("")) {
            conditions.put("trans_id", Long.parseLong(transId));
        }
        if (bet_value != null && !bet_value.equals("")) {
            conditions.put("bet_value", Long.parseLong(bet_value));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", (timeStart + " 00:00:00"));
            obj.put("$lte", (timeEnd + " 23:59:59"));
            conditions.put("time_log", obj);
        }
        if (moneyType != null && !moneyType.equals("")) {
            conditions.put("money_type", Integer.parseInt(moneyType));
        }
        int record = (int)db.getCollection("log_cao_thap").count((Bson)new Document(conditions));
        return record;
    }

}

