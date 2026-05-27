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
 *  com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  com.vinplay.vbee.common.response.VinCardResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.RechargeByVinCardDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.VinCardResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class RechargeByVinCardDAOImpl
implements RechargeByVinCardDAO {
    private long totalMoney = 0L;
    private long money2M = 0L;
    private int money2MQuantity = 0;
    private long money1M = 0L;
    private int money1MQuantity = 0;
    private long money500K = 0L;
    private int money500KQuantity = 0;
    private long money200K = 0L;
    private int money200KQuantity = 0;
    private long money100K = 0L;
    private int money100KQuantity = 0;
    private long money50K = 0L;
    private int money50KQuantity = 0;
    private long money20K = 0L;
    private int money20KQuantity = 0;
    private long money10K = 0L;
    private int money10KQuantity = 0;

    @Override
    public List<VinCardResponse> searchRechargeByVinCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, int page, String transId) {
        final ArrayList<VinCardResponse> results = new ArrayList<VinCardResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        int num_start = (page - 1) * 50;
        int num_end = 50;
        if (transId != null && !transId.equals("")) {
            conditions.put("reference_id", transId);
        }
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (provider != null && !provider.equals("")) {
            conditions.put("provider", provider);
        }
        if (serial != null && !serial.equals("")) {
            conditions.put("serial", serial);
        }
        if (pin != null && !pin.equals("")) {
            conditions.put("pin", pin);
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("dvt_recharge_by_vin_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                VinCardResponse bank = new VinCardResponse();
                bank.referenceId = document.getString("reference_id");
                bank.nickName = document.getString("nick_name");
                bank.provider = document.getString("provider");
                bank.serial = document.getString("serial");
                bank.pin = document.getString("pin");
                bank.amount = document.getInteger("amount");
                bank.status = document.getInteger("status");
                bank.message = document.getString("message");
                bank.code = document.getInteger("code");
                bank.timelog = document.getString("time_log");
                results.add(bank);
            }
        });
        return results;
    }

    @Override
    public int countSearchRechargeByVinCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        if (transId != null && !transId.equals("")) {
            conditions.put("reference_id", transId);
        }
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (provider != null && !provider.equals("")) {
            conditions.put("provider", provider);
        }
        if (serial != null && !serial.equals("")) {
            conditions.put("serial", serial);
        }
        if (pin != null && !pin.equals("")) {
            conditions.put("pin", pin);
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        int record = (int)db.getCollection("dvt_recharge_by_vin_card").count((Bson)new Document((Map)conditions));
        return record;
    }

    @Override
    public long moneyTotal(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        this.totalMoney = 0L;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        if (transId != null && !transId.equals("")) {
            conditions.put("reference_id", transId);
        }
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (provider != null && !provider.equals("")) {
            conditions.put("provider", provider);
        }
        if (serial != null && !serial.equals("")) {
            conditions.put("serial", serial);
        }
        if (pin != null && !pin.equals("")) {
            conditions.put("pin", pin);
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        FindIterable iterable = null;
        iterable = db.getCollection("dvt_recharge_by_vin_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByVinCardDAOImpl rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                rechargeByVinCardDAOImpl.totalMoney = rechargeByVinCardDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
            }
        });
        return this.totalMoney;
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByVinplayCard(String timeStart, String timeEnd, String code) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        this.totalMoney = 0L;
        this.money2M = 0L;
        this.money2MQuantity = 0;
        this.money1M = 0L;
        this.money1MQuantity = 0;
        this.money500K = 0L;
        this.money500KQuantity = 0;
        this.money200K = 0L;
        this.money200KQuantity = 0;
        this.money100K = 0L;
        this.money100KQuantity = 0;
        this.money50K = 0L;
        this.money50KQuantity = 0;
        this.money20K = 0L;
        this.money20KQuantity = 0;
        this.money10K = 0L;
        this.money10KQuantity = 0;
        objsort.put("_id", -1);
        if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        if (!code.isEmpty()) {
            conditions.put("code", Integer.parseInt(code));
        }
        FindIterable iterable = null;
        iterable = db.getCollection("dvt_recharge_by_vin_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByVinCardDAOImpl rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                rechargeByVinCardDAOImpl.totalMoney = rechargeByVinCardDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
                switch (document.getInteger("amount")) {
                    case 2000000: {
                        RechargeByVinCardDAOImpl.this.money2MQuantity++;
                        rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                        rechargeByVinCardDAOImpl.money2M = rechargeByVinCardDAOImpl.money2M + 2000000L;
                        break;
                    }
                    case 1000000: {
                        RechargeByVinCardDAOImpl.this.money1MQuantity++;
                        rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                        rechargeByVinCardDAOImpl.money1M = rechargeByVinCardDAOImpl.money1M + 1000000L;
                        break;
                    }
                    case 500000: {
                        RechargeByVinCardDAOImpl.this.money500KQuantity++;
                        rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                        rechargeByVinCardDAOImpl.money500K = rechargeByVinCardDAOImpl.money500K + 500000L;
                        break;
                    }
                    case 200000: {
                        RechargeByVinCardDAOImpl.this.money200KQuantity++;
                        rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                        rechargeByVinCardDAOImpl.money200K = rechargeByVinCardDAOImpl.money200K + 200000L;
                        break;
                    }
                    case 100000: {
                        RechargeByVinCardDAOImpl.this.money100KQuantity++;
                        rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                        rechargeByVinCardDAOImpl.money100K = rechargeByVinCardDAOImpl.money100K + 100000L;
                        break;
                    }
                    case 50000: {
                        RechargeByVinCardDAOImpl.this.money50KQuantity++;
                        rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                        rechargeByVinCardDAOImpl.money50K = rechargeByVinCardDAOImpl.money50K + 50000L;
                        break;
                    }
                    case 20000: {
                        RechargeByVinCardDAOImpl.this.money20KQuantity++;
                        rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                        rechargeByVinCardDAOImpl.money20K = rechargeByVinCardDAOImpl.money20K + 20000L;
                        break;
                    }
                    case 10000: {
                        RechargeByVinCardDAOImpl.this.money10KQuantity++;
                        rechargeByVinCardDAOImpl = RechargeByVinCardDAOImpl.this;
                        rechargeByVinCardDAOImpl.money10K = rechargeByVinCardDAOImpl.money10K + 10000L;
                    }
                }
            }
        });
        ArrayList<MoneyTotalRechargeByCardReponse> response = new ArrayList<MoneyTotalRechargeByCardReponse>();
        MoneyTotalRechargeByCardReponse vinplay = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> moneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue money2MFollowFace = new MoneyTotalFollowFaceValue();
        money2MFollowFace.setFaceValue(2000000);
        money2MFollowFace.setQuantity(this.money2MQuantity);
        money2MFollowFace.setMoneyTotal(this.money2M);
        moneyFollowFace.add(money2MFollowFace);
        MoneyTotalFollowFaceValue money1MFollowFace = new MoneyTotalFollowFaceValue();
        money1MFollowFace.setFaceValue(1000000);
        money1MFollowFace.setQuantity(this.money1MQuantity);
        money1MFollowFace.setMoneyTotal(this.money1M);
        moneyFollowFace.add(money1MFollowFace);
        MoneyTotalFollowFaceValue money500KFollowFace = new MoneyTotalFollowFaceValue();
        money500KFollowFace.setFaceValue(500000);
        money500KFollowFace.setQuantity(this.money500KQuantity);
        money500KFollowFace.setMoneyTotal(this.money500K);
        moneyFollowFace.add(money500KFollowFace);
        MoneyTotalFollowFaceValue money200KFollowFace = new MoneyTotalFollowFaceValue();
        money200KFollowFace.setFaceValue(200000);
        money200KFollowFace.setQuantity(this.money200KQuantity);
        money200KFollowFace.setMoneyTotal(this.money200K);
        moneyFollowFace.add(money200KFollowFace);
        MoneyTotalFollowFaceValue money100KFollowFace = new MoneyTotalFollowFaceValue();
        money100KFollowFace.setFaceValue(100000);
        money100KFollowFace.setQuantity(this.money100KQuantity);
        money100KFollowFace.setMoneyTotal(this.money100K);
        moneyFollowFace.add(money100KFollowFace);
        MoneyTotalFollowFaceValue money50KFollowFace = new MoneyTotalFollowFaceValue();
        money50KFollowFace.setFaceValue(50000);
        money50KFollowFace.setQuantity(this.money50KQuantity);
        money50KFollowFace.setMoneyTotal(this.money50K);
        moneyFollowFace.add(money50KFollowFace);
        MoneyTotalFollowFaceValue money20KFollowFace = new MoneyTotalFollowFaceValue();
        money20KFollowFace.setFaceValue(20000);
        money20KFollowFace.setQuantity(this.money20KQuantity);
        money20KFollowFace.setMoneyTotal(this.money20K);
        moneyFollowFace.add(money20KFollowFace);
        MoneyTotalFollowFaceValue money10KFollowFace = new MoneyTotalFollowFaceValue();
        money10KFollowFace.setFaceValue(10000);
        money10KFollowFace.setQuantity(this.money10KQuantity);
        money10KFollowFace.setMoneyTotal(this.money10K);
        moneyFollowFace.add(money10KFollowFace);
        vinplay.setName("vinplay");
        vinplay.setValue(this.totalMoney);
        vinplay.setTrans(moneyFollowFace);
        response.add(vinplay);
        return response;
    }

}

