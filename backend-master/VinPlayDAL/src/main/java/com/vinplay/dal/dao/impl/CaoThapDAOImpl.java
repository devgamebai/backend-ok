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
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.CaoThapDAO;
import com.vinplay.dal.entities.caothap.LSGDCaoThap;
import com.vinplay.dal.entities.caothap.TopCaoThap;
import com.vinplay.dal.entities.caothap.VinhDanhCaoThap;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class CaoThapDAOImpl
implements CaoThapDAO {
    @Override
    public long[] getPotCaoThap(String potName) throws SQLException {
        ArrayList<Long> result = new ArrayList<Long>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT value FROM minigame_pots WHERE minigame_pots.pot_name like '" + potName + "%'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("value"));
            }
            rs.close();
            stmt.close();
        }
        long[] arr = new long[result.size()];
        for (int i = 0; i < result.size(); ++i) {
            arr[i] = (Long)result.get(i);
        }
        return arr;
    }

    @Override
    public long[] getFundCaoThap(String fundName) throws SQLException {
        ArrayList<Long> result = new ArrayList<Long>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT value FROM minigame_funds WHERE minigame_funds.fund_name like '" + fundName + "%'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong("value"));
            }
            rs.close();
            stmt.close();
        }
        long[] arr = new long[result.size()];
        for (int i = 0; i < result.size(); ++i) {
            arr[i] = (Long)result.get(i);
        }
        return arr;
    }

    @Override
    public int countLichSuGiaoDich(String nickname, int moneyType) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("nick_name", nickname);
        conditions.put("money_type", moneyType);
        long totalRows = db.getCollection("log_cao_thap").count((Bson)conditions);
        return (int)totalRows;
    }

    @Override
    public List<LSGDCaoThap> getLichSuGiaoDich(String nickname, int pageNumber, int moneyType) {
        int pageSize = 10;
        int skipNumber = (pageNumber - 1) * 10;
        final ArrayList<LSGDCaoThap> results = new ArrayList<LSGDCaoThap>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("nick_name", nickname);
        conditions.put("money_type", moneyType);
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("trans_id", -1);
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("log_cao_thap").find((Bson)conditions).sort((Bson)sortCondtions).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LSGDCaoThap entry = new LSGDCaoThap();
                entry.transId = document.getLong("trans_id");
                entry.potBet = document.getInteger("pot_bet");
                entry.step = document.getInteger("step");
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
    public List<VinhDanhCaoThap> getBangVinhDanh(int pageNumber, int moneyType) {
        int pageSize = 10;
        int skipNumber = (pageNumber - 1) * 10;
        final ArrayList<VinhDanhCaoThap> results = new ArrayList<VinhDanhCaoThap>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("money_type", moneyType);
        conditions.put("money_win", new BasicDBObject("$gt", 0));
        BasicDBObject query1 = new BasicDBObject("prize", new BasicDBObject("$gte", 100000));
        BasicDBObject query2 = new BasicDBObject("result", 7);
        ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
        myList.add(query1);
        myList.add(query2);
        conditions.put("$or", myList);
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("log_cao_thap_win").find((Bson)conditions).sort((Bson)sortCondtions).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                VinhDanhCaoThap entry = new VinhDanhCaoThap();
                entry.nickname = document.getString("nick_name");
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
    public int countVinhDanh(int moneyType) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("money_type", moneyType);
        conditions.put("money_win", new BasicDBObject("$gt", 0));
        long totalRows = db.getCollection("log_cao_thap_win").count((Bson)conditions);
        return (int)totalRows;
    }

    @Override
    public long getLastReferenceId() {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap conditions = new HashMap();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        FindIterable iterable = db.getCollection("log_cao_thap").find((Bson)new Document(conditions)).sort((Bson)objsort).limit(1);
        Document document = iterable != null ? (Document)iterable.first() : null;
        long transId = document == null ? 0L : document.getLong("trans_id");
        return transId;
    }

    @Override
    public List<TopCaoThap> getTop(String startTime, String endTime) {
        final ArrayList<TopCaoThap> results = new ArrayList<TopCaoThap>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("money_type", 1);
        conditions.put("group_type", new BasicDBObject("$gte", 0));
        BasicDBObject obj = new BasicDBObject();
        obj.put("$gte", startTime);
        obj.put("$lte", endTime);
        conditions.put("time_log", obj);
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("group_type", -1);
        sortCondtions.put("rank_1", -1);
        sortCondtions.put("rank_2", -1);
        sortCondtions.put("rank_3", -1);
        sortCondtions.put("rank_4", -1);
        sortCondtions.put("rank_5", -1);
        sortCondtions.put("prize", -1);
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("log_cao_thap_win").find((Bson)conditions).sort((Bson)sortCondtions).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TopCaoThap entry = new TopCaoThap();
                entry.nickname = document.getString("nick_name");
                entry.hand = document.getString("hand");
                entry.money = document.getLong("prize");
                entry.timestamp = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

}

