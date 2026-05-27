/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.LogGameMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.LogGameDAO;
import com.vinplay.vbee.common.messages.LogGameMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LogGameDAOImpl
implements LogGameDAO {
    @Override
    public List<LogGameMessage> searchLogGameByNickName(String sessionId, String nickName, String gameName, String timeStart, String timeEnd, String moneyType, int page) {
        final ArrayList<LogGameMessage> results = new ArrayList<LogGameMessage>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject objsort = new BasicDBObject();
        BasicDBObject obj = new BasicDBObject();
        int num_start = (page - 1) * 50;
        int num_end = 50;
        FindIterable iterable = null;
        objsort.put("_id", -1);
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (gameName != null && !gameName.equals("")) {
            conditions.put("game_name", gameName);
        }
        if (sessionId != null && !sessionId.equals("")) {
            conditions.put("session_id", sessionId);
        }
        if (moneyType != null && !moneyType.equals("")) {
            conditions.put("money_type", moneyType);
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("log_game").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogGameMessage loggame = new LogGameMessage();
                loggame.sessionId = document.getString("session_id");
                loggame.gameName = document.getString("game_name");
                loggame.timeLog = document.getString("time_log");
                loggame.nickName = document.getString("nick_name");
                loggame.moneyType = document.getString("money_type");
                results.add(loggame);
            }
        });
        return results;
    }

    @Override
    public int countSearchLogGameByNickName(String sessionId, String nickName, String gameName, String timeStart, String timeEnd, String moneyType) {
        return 500;
    }

    @Override
    public LogGameMessage getLogGameDetailBySessionID(String sessionId, String gameName, String timelog) {
        final LogGameMessage loggame = new LogGameMessage();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("session_id", sessionId);
        conditions.put("game_name", gameName);
        conditions.put("time_log", timelog);
        FindIterable iterable = db.getCollection("log_game_detail").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                loggame.sessionId = document.getString("session_id");
                loggame.gameName = document.getString("game_name");
                loggame.timeLog = document.getString("time_log");
                loggame.logDetail = document.getString("log_detail");
            }
        });
        return loggame;
    }

}

