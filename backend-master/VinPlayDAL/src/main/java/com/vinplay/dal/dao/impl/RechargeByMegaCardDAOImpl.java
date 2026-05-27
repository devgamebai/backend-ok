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
 *  com.vinplay.vbee.common.response.megacard.MegaCardResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.RechargeByMegaCardDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.megacard.MegaCardResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class RechargeByMegaCardDAOImpl
implements RechargeByMegaCardDAO {
    private long totalMoney = 0L;
    private long money5M = 0L;
    private int money5MQuantity = 0;
    private long money3M = 0L;
    private int money3MQuantity = 0;
    private long money2M = 0L;
    private int money2MQuantity = 0;
    private long money1M = 0L;
    private int money1MQuantity = 0;
    private long money500K = 0L;
    private int money500KQuantity = 0;
    private long money300K = 0L;
    private int money300KQuantity = 0;
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
    public List<MegaCardResponse> searchRechargeByMegaCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, int page, String transId) {
        final ArrayList<MegaCardResponse> result = new ArrayList<MegaCardResponse>();
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
        iterable = db.getCollection("epay_recharge_by_mega_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                MegaCardResponse obj = new MegaCardResponse(document.getString("reference_id"), document.getString("nick_name"), document.getString("provider"), document.getString("pin"), document.getString("serial"), document.getInteger("amount", -1), document.getInteger("money", -1), document.getInteger("status", -1), document.getString("message"), document.getInteger("code", -1), document.getString("time_log"), document.getString("update_time"), document.getString("partner"), document.getString("platform"));
                result.add(obj);
            }
        });
        return result;
    }

    @Override
    public int countSearchRechargeByMegaCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
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
        int record = (int)db.getCollection("epay_recharge_by_mega_card").count((Bson)new Document((Map)conditions));
        return record;
    }

    @Override
    public long moneyTotal(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        this.totalMoney = 0L;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        FindIterable iterable = null;
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
        iterable = db.getCollection("epay_recharge_by_mega_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByMegaCardDAOImpl rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                rechargeByMegaCardDAOImpl.totalMoney = rechargeByMegaCardDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
            }
        });
        return this.totalMoney;
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByMegaCard(String timeStart, String timeEnd, String code) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        FindIterable iterable = null;
        this.totalMoney = 0L;
        this.money5M = 0L;
        this.money5MQuantity = 0;
        this.money3M = 0L;
        this.money3MQuantity = 0;
        this.money2M = 0L;
        this.money2MQuantity = 0;
        this.money1M = 0L;
        this.money1MQuantity = 0;
        this.money500K = 0L;
        this.money500KQuantity = 0;
        this.money300K = 0L;
        this.money300KQuantity = 0;
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
        iterable = db.getCollection("epay_recharge_by_mega_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByMegaCardDAOImpl rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                rechargeByMegaCardDAOImpl.totalMoney = rechargeByMegaCardDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
                switch (document.getInteger("amount")) {
                    case 5000000: {
                        RechargeByMegaCardDAOImpl.this.money5MQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money5M = rechargeByMegaCardDAOImpl.money5M + 5000000L;
                        break;
                    }
                    case 3000000: {
                        RechargeByMegaCardDAOImpl.this.money3MQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money3M = rechargeByMegaCardDAOImpl.money3M + 3000000L;
                        break;
                    }
                    case 2000000: {
                        RechargeByMegaCardDAOImpl.this.money2MQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money2M = rechargeByMegaCardDAOImpl.money2M + 2000000L;
                        break;
                    }
                    case 1000000: {
                        RechargeByMegaCardDAOImpl.this.money1MQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money1M = rechargeByMegaCardDAOImpl.money1M + 1000000L;
                        break;
                    }
                    case 500000: {
                        RechargeByMegaCardDAOImpl.this.money500KQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money500K = rechargeByMegaCardDAOImpl.money500K + 500000L;
                        break;
                    }
                    case 300000: {
                        RechargeByMegaCardDAOImpl.this.money300KQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money300K = rechargeByMegaCardDAOImpl.money300K + 300000L;
                        break;
                    }
                    case 200000: {
                        RechargeByMegaCardDAOImpl.this.money200KQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money200K = rechargeByMegaCardDAOImpl.money200K + 200000L;
                        break;
                    }
                    case 100000: {
                        RechargeByMegaCardDAOImpl.this.money100KQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money100K = rechargeByMegaCardDAOImpl.money100K + 100000L;
                        break;
                    }
                    case 50000: {
                        RechargeByMegaCardDAOImpl.this.money50KQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money50K = rechargeByMegaCardDAOImpl.money50K + 50000L;
                        break;
                    }
                    case 20000: {
                        RechargeByMegaCardDAOImpl.this.money20KQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money20K = rechargeByMegaCardDAOImpl.money20K + 20000L;
                        break;
                    }
                    case 10000: {
                        RechargeByMegaCardDAOImpl.this.money10KQuantity++;
                        rechargeByMegaCardDAOImpl = RechargeByMegaCardDAOImpl.this;
                        rechargeByMegaCardDAOImpl.money10K = rechargeByMegaCardDAOImpl.money10K + 10000L;
                    }
                }
            }
        });
        ArrayList<MoneyTotalRechargeByCardReponse> response = new ArrayList<MoneyTotalRechargeByCardReponse>();
        MoneyTotalRechargeByCardReponse megacard = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> moneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue money5MFollowFace = new MoneyTotalFollowFaceValue();
        money5MFollowFace.setFaceValue(5000000);
        money5MFollowFace.setQuantity(this.money5MQuantity);
        money5MFollowFace.setMoneyTotal(this.money5M);
        moneyFollowFace.add(money5MFollowFace);
        MoneyTotalFollowFaceValue money3MFollowFace = new MoneyTotalFollowFaceValue();
        money3MFollowFace.setFaceValue(3000000);
        money3MFollowFace.setQuantity(this.money3MQuantity);
        money3MFollowFace.setMoneyTotal(this.money3M);
        moneyFollowFace.add(money3MFollowFace);
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
        MoneyTotalFollowFaceValue money300KFollowFace = new MoneyTotalFollowFaceValue();
        money300KFollowFace.setFaceValue(300000);
        money300KFollowFace.setQuantity(this.money300KQuantity);
        money300KFollowFace.setMoneyTotal(this.money300K);
        moneyFollowFace.add(money300KFollowFace);
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
        megacard.setName("MegaCard");
        megacard.setValue(this.totalMoney);
        megacard.setTrans(moneyFollowFace);
        response.add(megacard);
        return response;
    }

}

