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
 *  com.vinplay.vbee.common.response.PokegoResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.LogPoKeGoDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.PokegoResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LogPokegoDAOImpl
implements LogPoKeGoDAO {
    @Override
    public List<PokegoResponse> listLogPokego(String referentId, String userName, String moneyType, String betValue, String timeStart, String timeEnd, int page) throws SQLException {
        final ArrayList<PokegoResponse> results = new ArrayList<PokegoResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        int numStart = (page - 1) * 50;
        int numEnd = 50;
        if (referentId != null && !referentId.equals("")) {
            conditions.put("reference_id", Long.parseLong(referentId));
        }
        if (userName != null && !userName.equals("")) {
            conditions.put("user_name", userName);
        }
        if (betValue != null && !betValue.equals("")) {
            conditions.put("bet_value", Long.parseLong(betValue));
        }
        if (moneyType != null && !moneyType.equals("")) {
            conditions.put("money_type", Integer.parseInt(moneyType));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("log_poke_go").find((Bson)new Document(conditions)).skip(numStart).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                PokegoResponse pokego = new PokegoResponse();
                pokego.reference_id = document.getLong("reference_id");
                pokego.user_name = document.getString("user_name");
                pokego.bet_value = document.getLong("bet_value");
                pokego.lines_betting = document.getString("lines_betting");
                pokego.lines_win = document.getString("lines_win");
                pokego.prizes_on_line = document.getString("prizes_on_line");
                pokego.prize = document.getLong("prize");
                pokego.result = document.getInteger("result");
                pokego.money_type = document.getInteger("money_type");
                pokego.time_log = document.getString("time_log");
                results.add(pokego);
            }
        });
        return results;
    }

    @Override
    public long countLogPokego(String referentId, String userName, String moneyType, String betValue, String timeStart, String timeEnd) throws SQLException {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject objsort = new BasicDBObject();
        BasicDBObject obj = new BasicDBObject();
        objsort.put("_id", -1);
        if (referentId != null && !referentId.equals("")) {
            conditions.put("reference_id", Long.parseLong(referentId));
        }
        if (userName != null && !userName.equals("")) {
            conditions.put("user_name", userName);
        }
        if (betValue != null && !betValue.equals("")) {
            conditions.put("bet_value", Long.parseLong(betValue));
        }
        if (moneyType != null && !moneyType.equals("")) {
            conditions.put("money_type", Integer.parseInt(moneyType));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        long totalRecord = db.getCollection("log_poke_go").count((Bson)new Document(conditions));
        return totalRecord;
    }

}

