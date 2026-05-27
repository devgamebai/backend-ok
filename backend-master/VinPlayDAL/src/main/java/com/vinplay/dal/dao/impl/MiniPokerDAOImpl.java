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
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.MiniPokerDAO;
import com.vinplay.dal.entities.minipoker.LSGDMiniPoker;
import com.vinplay.dal.entities.minipoker.VinhDanhMiniPoker;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MiniPokerDAOImpl
implements MiniPokerDAO {
    @Override
    public List<LSGDMiniPoker> getLichSuGiaoDich(String username, int pageNumber, int moneyType) {
        int pageSize = 10;
        int skipNumber = (pageNumber - 1) * 10;
        final ArrayList<LSGDMiniPoker> results = new ArrayList<LSGDMiniPoker>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("user_name", username);
        conditions.put("money_type", moneyType);
        iterable = db.getCollection("log_mini_poker").find((Bson)conditions).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LSGDMiniPoker entry = new LSGDMiniPoker();
                entry.username = document.getString("user_name");
                entry.betValue = document.getLong("bet_value");
                entry.prize = document.getLong("prize");
                entry.cards = document.getString("cards");
                entry.timestamp = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

    @Override
    public List<VinhDanhMiniPoker> getBangVinhDanh(int moneyType, int page) {
        int pageSize = 10;
        int skipNumber = (page - 1) * 10;
        final ArrayList<VinhDanhMiniPoker> results = new ArrayList<VinhDanhMiniPoker>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("$or", Arrays.asList(new Document[]{new Document("result", 1), new Document("result", 2), new Document("result", 12)}));
        conditions.put("money_type", moneyType);
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("log_mini_poker").find((Bson)conditions).sort((Bson)sortCondtions).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                VinhDanhMiniPoker entry = new VinhDanhMiniPoker();
                entry.username = document.getString("user_name");
                entry.betValue = document.getLong("bet_value");
                entry.prize = document.getLong("prize");
                entry.result = document.getInteger("result");
                entry.timestamp = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

    @Override
    public int countLichSuGiaoDich(String username, int moneyType) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("user_name", username);
        conditions.put("money_type", moneyType);
        long totalRows = db.getCollection("log_mini_poker").count((Bson)conditions);
        return (int)totalRows;
    }

}

