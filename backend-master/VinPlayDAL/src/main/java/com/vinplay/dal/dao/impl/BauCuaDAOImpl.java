/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.models.minigame.baucua.ResultBauCua
 *  com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa
 *  com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.BauCuaDAO;
import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.models.minigame.baucua.ResultBauCua;
import com.vinplay.vbee.common.models.minigame.baucua.ToiChonCa;
import com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;

public class BauCuaDAOImpl
implements BauCuaDAO {
    @Override
    public List<TransactionBauCua> getLSGDBauCua(String username, int page, byte moneyType) {
        int pageSize = 10;
        int skipNumber = (page - 1) * 10;
        final ArrayList<TransactionBauCua> results = new ArrayList<TransactionBauCua>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("user_name", username);
        conditions.put("money_type", moneyType);
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("bau_cua_transaction").find((Bson)conditions).sort((Bson)sortCondtions).skip(skipNumber).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TransactionBauCua entry = new TransactionBauCua();
                entry.username = document.getString("user_name");
                entry.room = document.getInteger("room");
                entry.referenceId = document.getLong("reference_id");
                entry.dices = document.getString("dices");
                entry.betValues = CommonUtils.stringToLongArr((String)document.getString("bet_value"));
                entry.prizes = CommonUtils.stringToLongArr((String)document.getString("prize"));
                entry.timestamp = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

    @Override
    public int countLSGDBauCua(String username, byte moneyType) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        Document conditions = new Document();
        conditions.put("user_name", username);
        conditions.put("money_type", moneyType);
        long totalRows = db.getCollection("bau_cua_transaction").count((Bson)conditions);
        return (int)totalRows;
    }

    @Override
    public ResultBauCua getPhienBauCua(long referenceId) {
        final ResultBauCua result = new ResultBauCua();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("reference_id", referenceId);
        iterable = db.getCollection("bau_cua_transaction").find((Bson)conditions).limit(1);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                result.referenceId = document.getLong("reference_id");
                result.dices[0] = document.getInteger("dice1").byteValue();
                result.dices[1] = document.getInteger("dice2").byteValue();
                result.dices[2] = document.getInteger("dice3").byteValue();
                result.xPot = document.getInteger("x_pot").byteValue();
                result.xValue = document.getInteger("x_value").byteValue();
            }
        });
        return result;
    }

    @Override
    public List<TopWin> getTopBauCua(byte moneyType, String startDate, String endDate) {
        int pageSize = 10;
        final ArrayList<TopWin> results = new ArrayList<TopWin>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        obj.put("$gte", startDate);
        obj.put("$lte", endDate);
        conditions.put("time_log", obj);
        conditions.put("money_type", moneyType);
        AggregateIterable iterable = db.getCollection("bau_cua_transaction").aggregate(Arrays.asList(new Document[]{new Document("$match", conditions), new Document("$group", new Document("_id", "$user_name").append("total", new Document("$sum", "$money_exchange"))), new Document("$sort", new Document("total", -1)), new Document("$limit", 10)}));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                TopWin entry = new TopWin();
                entry.setUsername(document.getString("_id"));
                entry.setMoney(document.getLong("total").longValue());
                if (entry.getMoney() > 0L) {
                    results.add(entry);
                }
            }
        });
        return results;
    }

    @Override
    public List<ResultBauCua> getLichSuPhien(int size, byte room) {
        final ArrayList<ResultBauCua> results = new ArrayList<ResultBauCua>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("room", room);
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        iterable = db.getCollection("bau_cua_results").find((Bson)conditions).sort((Bson)sortCondtions).limit(size);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                ResultBauCua entry = new ResultBauCua();
                entry.referenceId = document.getLong("reference_id");
                entry.room = document.getInteger("room").byteValue();
                entry.dices[0] = document.getInteger("dice1").byteValue();
                entry.dices[1] = document.getInteger("dice2").byteValue();
                entry.dices[2] = document.getInteger("dice3").byteValue();
                entry.xPot = document.getInteger("x_pot").byteValue();
                entry.xValue = document.getInteger("x_value").byteValue();
                results.add(0, entry);
            }
        });
        return results;
    }

    @Override
    public List<ToiChonCa> getTopToiChonCa(String startDate, String endDate) {
        final ArrayList<ToiChonCa> results = new ArrayList<ToiChonCa>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, BasicDBObject> conditions = new HashMap<String, BasicDBObject>();
        BasicDBObject obj = new BasicDBObject();
        obj.put("$gte", startDate);
        obj.put("$lte", endDate);
        conditions.put("time_log", obj);
        Document sortObj = new Document();
        sortObj.put("so_ca", -1);
        sortObj.put("so_van", -1);
        sortObj.put("tong_thang", -1);
        sortObj.put("tong_dat", -1);
        sortObj.put("time_log", -1);
        AggregateIterable iterable = db.getCollection("bau_cua_toi_chon_ca").aggregate(Arrays.asList(new Document[]{new Document("$match", conditions), new Document("$sort", sortObj), new Document("$limit", 10)}));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                ToiChonCa entry = new ToiChonCa();
                entry.username = document.getString("user_name");
                entry.soCa = (short)document.getInteger("so_ca", 0);
                entry.soVan = (short)document.getInteger("so_van", 0);
                entry.tongThang = document.getLong("tong_thang");
                entry.tongDat = document.getLong("tong_dat");
                entry.currentPhien = document.getLong("current_phien");
                entry.timestamp = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

    @Override
    public short getHighScore(String username) {
        short result = 0;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("user_name", username);
        BasicDBObject obj = new BasicDBObject();
        obj.put("$gte", DateTimeUtils.getStartTimeToDay());
        obj.put("$lte", DateTimeUtils.getEndTimeToDay());
        conditions.put("time_log", obj);
        iterable = db.getCollection("bau_cua_toi_chon_ca").find((Bson)conditions);
        Document doc = (Document)iterable.first();
        if (doc != null) {
            result = (short) ((Number) doc.get("so_ca", 0)).intValue();
        }
        return result;
    }

}

