/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.UpdateOptions
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.messages.minigame.baucua.ResultBauCuaMsg
 *  com.vinplay.vbee.common.messages.minigame.baucua.ToiChonCaMsg
 *  com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaDetailMsg
 *  com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaMsg
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.vbee.common.messages.minigame.baucua.ResultBauCuaMsg;
import com.vinplay.vbee.common.messages.minigame.baucua.ToiChonCaMsg;
import com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaDetailMsg;
import com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaMsg;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.dao.BauCuaDao;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bson.Document;
import org.bson.conversions.Bson;

public class BauCuaDaoImpl
implements BauCuaDao {
    @Override
    public void saveTransactionBauCua(TransactionBauCuaMsg msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("bau_cua_transaction");
        Document doc = new Document();
        doc.append("reference_id", (Object)msg.referenceId);
        doc.append("user_name", (Object)msg.username);
        doc.append("room", (Object)msg.room);
        doc.append("dices", (Object)msg.dices);
        doc.append("bet_value", (Object)CommonUtils.arrayLongToString((long[])msg.betValues));
        doc.append("bet_bau", (Object)msg.betValues[0]);
        doc.append("bet_cua", (Object)msg.betValues[1]);
        doc.append("bet_tom", (Object)msg.betValues[2]);
        doc.append("bet_ca", (Object)msg.betValues[3]);
        doc.append("bet_ga", (Object)msg.betValues[4]);
        doc.append("bet_huou", (Object)msg.betValues[5]);
        doc.append("prize", (Object)CommonUtils.arrayLongToString((long[])msg.prizes));
        doc.append("prize_bau", (Object)msg.prizes[0]);
        doc.append("prize_cua", (Object)msg.prizes[1]);
        doc.append("prize_tom", (Object)msg.prizes[2]);
        doc.append("prize_ca", (Object)msg.prizes[3]);
        doc.append("prize_ga", (Object)msg.prizes[4]);
        doc.append("prize_huou", (Object)msg.prizes[5]);
        doc.append("money_exchange", (Object)msg.totalExchange);
        doc.append("money_type", (Object)msg.moneyType);
        doc.append("time_log", (Object)timeLog);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public void saveTransactionBauCuaDetail(TransactionBauCuaDetailMsg msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("bau_cua_transaction_detail");
        Document doc = new Document();
        doc.append("reference_id", (Object)msg.referenceId);
        doc.append("user_name", (Object)msg.username);
        doc.append("room", (Object)msg.room);
        doc.append("transaction_code", (Object)msg.transactionCode);
        doc.append("bet_value", (Object)CommonUtils.arrayLongToString((long[])msg.betValues));
        doc.append("bet_bau", (Object)msg.betValues[0]);
        doc.append("bet_cua", (Object)msg.betValues[1]);
        doc.append("bet_tom", (Object)msg.betValues[2]);
        doc.append("bet_ca", (Object)msg.betValues[3]);
        doc.append("bet_ga", (Object)msg.betValues[4]);
        doc.append("bet_huou", (Object)msg.betValues[5]);
        doc.append("money_type", (Object)msg.moneyType);
        doc.append("time_log", (Object)timeLog);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public void saveResultBauCua(ResultBauCuaMsg msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("bau_cua_results");
        Document doc = new Document();
        doc.append("reference_id", (Object)msg.referenceId);
        doc.append("room", (Object)msg.room);
        doc.append("min_bet_value", (Object)msg.minBetValue);
        doc.append("dice1", (Object)msg.dices[0]);
        doc.append("dice2", (Object)msg.dices[1]);
        doc.append("dice3", (Object)msg.dices[2]);
        doc.append("x_pot", (Object)msg.xPot);
        doc.append("x_value", (Object)msg.xValue);
        doc.append("bet_value", (Object)CommonUtils.arrayLongToString((long[])msg.totalBetValues));
        doc.append("bet_bau", (Object)msg.totalBetValues[0]);
        doc.append("bet_cua", (Object)msg.totalBetValues[1]);
        doc.append("bet_tom", (Object)msg.totalBetValues[2]);
        doc.append("bet_ca", (Object)msg.totalBetValues[3]);
        doc.append("bet_ga", (Object)msg.totalBetValues[4]);
        doc.append("bet_huou", (Object)msg.totalBetValues[5]);
        doc.append("prize", (Object)CommonUtils.arrayLongToString((long[])msg.totalPrizes));
        doc.append("prize_bau", (Object)msg.totalPrizes[0]);
        doc.append("prize_cua", (Object)msg.totalPrizes[1]);
        doc.append("prize_tom", (Object)msg.totalPrizes[2]);
        doc.append("prize_ca", (Object)msg.totalPrizes[3]);
        doc.append("prize_ga", (Object)msg.totalPrizes[4]);
        doc.append("prize_huou", (Object)msg.totalPrizes[5]);
        doc.append("time_log", (Object)timeLog);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public void updateToiChonCa(ToiChonCaMsg msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("bau_cua_toi_chon_ca");
        BasicDBObject conditions = new BasicDBObject();
        conditions.put("user_name", msg.username);
        BasicDBObject obj = new BasicDBObject();
        SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = df2.format(new Date());
        String startDate = String.valueOf(currentDate) + " 00:00:00";
        String endDate = String.valueOf(currentDate) + " 23:59:59";
        obj.put("$gte", (Object)startDate);
        obj.put("$lte", (Object)endDate);
        conditions.put("time_log", (Object)obj);
        BasicDBObject modiferObj = new BasicDBObject();
        Document docUpdated = new Document();
        docUpdated.append("user_name", (Object)msg.username);
        docUpdated.append("so_ca", (Object)msg.soCa);
        docUpdated.append("so_van", (Object)msg.soVan);
        docUpdated.append("tong_thang", (Object)msg.tongThang);
        docUpdated.append("tong_dat", (Object)msg.tongDat);
        docUpdated.append("current_phien", (Object)msg.currentPhien);
        docUpdated.append("list_phien", (Object)msg.listPhien);
        docUpdated.append("time_log", (Object)timeLog);
        docUpdated.append("create_time", (Object)new Date());
        modiferObj.put("$set", docUpdated);
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        col.updateOne((Bson)conditions, (Bson)modiferObj, options);
    }
}

