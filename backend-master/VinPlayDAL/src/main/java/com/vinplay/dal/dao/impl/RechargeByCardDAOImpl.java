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
 *  com.vinplay.vbee.common.response.RechargeByCardReponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.RechargeByCardDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.RechargeByCardReponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.bson.conversions.Bson;

public class RechargeByCardDAOImpl
implements RechargeByCardDAO {
    private long totalMoney = 0L;
    private long webMoney = 0L;
    private long androidMoney = 0L;
    private long iosMoney = 0L;
    private long winphoneMoney = 0L;
    private long facebookMoney = 0L;
    private long desktopMoney = 0L;
    private long otherMoney = 0L;
    private long viettelMoney = 0L;
    private long viettel5MMoney = 0L;
    private int viettel5MQuantity = 0;
    private long viettel2MMoney = 0L;
    private int viettel2MQuantity = 0;
    private long viettel1MMoney = 0L;
    private int viettel1MQuantity = 0;
    private long viettel500KMoney = 0L;
    private int viettel500KQuantity = 0;
    private long viettel300KMoney = 0L;
    private int viettel300KQuantity = 0;
    private long viettel200KMoney = 0L;
    private int viettel200KQuantity = 0;
    private long viettel100KMoney = 0L;
    private int viettel100KQuantity = 0;
    private long viettel50KMoney = 0L;
    private int viettel50KQuantity = 0;
    private long viettel30KMoney = 0L;
    private int viettel30KQuantity = 0;
    private long viettel20KMoney = 0L;
    private int viettel20KQuantity = 0;
    private long viettel10KMoney = 0L;
    private int viettel10KQuantity = 0;
    private long vinaphoneMoney = 0L;
    private long vinaphone5MMoney = 0L;
    private int vinaphone5MQuantity = 0;
    private long vinaphone2MMoney = 0L;
    private int vinaphone2MQuantity = 0;
    private long vinaphone1MMoney = 0L;
    private int vinaphone1MQuantity = 0;
    private long vinaphone500KMoney = 0L;
    private int vinaphone500KQuantity = 0;
    private long vinaphone300KMoney = 0L;
    private int vinaphone300KQuantity = 0;
    private long vinaphone200KMoney = 0L;
    private int vinaphone200KQuantity = 0;
    private long vinaphone100KMoney = 0L;
    private int vinaphone100KQuantity = 0;
    private long vinaphone50KMoney = 0L;
    private int vinaphone50KQuantity = 0;
    private long vinaphone30KMoney = 0L;
    private int vinaphone30KQuantity = 0;
    private long vinaphone20KMoney = 0L;
    private int vinaphone20KQuantity = 0;
    private long vinaphone10KMoney = 0L;
    private int vinaphone10KQuantity = 0;
    private long mobifoneMoney = 0L;
    private long mobifone5MMoney = 0L;
    private int mobifone5MQuantity = 0;
    private long mobifone2MMoney = 0L;
    private int mobifone2MQuantity = 0;
    private long mobifone1MMoney = 0L;
    private int mobifone1MQuantity = 0;
    private long mobifone500KMoney = 0L;
    private int mobifone500KQuantity = 0;
    private long mobifone300KMoney = 0L;
    private int mobifone300KQuantity = 0;
    private long mobifone200KMoney = 0L;
    private int mobifone200KQuantity = 0;
    private long mobifone100KMoney = 0L;
    private int mobifone100KQuantity = 0;
    private long mobifone50KMoney = 0L;
    private int mobifone50KQuantity = 0;
    private long mobifone30KMoney = 0L;
    private int mobifone30KQuantity = 0;
    private long mobifone20KMoney = 0L;
    private int mobifone20KQuantity = 0;
    private long mobifone10KMoney = 0L;
    private int mobifone10KQuantity = 0;
    private long gateMoney = 0L;
    private long gate5MMoney = 0L;
    private int gate5MQuantity = 0;
    private long gate2MMoney = 0L;
    private int gate2MQuantity = 0;
    private long gate1MMoney = 0L;
    private int gate1MQuantity = 0;
    private long gate500KMoney = 0L;
    private int gate500KQuantity = 0;
    private long gate300KMoney = 0L;
    private int gate300KQuantity = 0;
    private long gate200KMoney = 0L;
    private int gate200KQuantity = 0;
    private long gate100KMoney = 0L;
    private int gate100KQuantity = 0;
    private long gate50KMoney = 0L;
    private int gate50KQuantity = 0;
    private long gate30KMoney = 0L;
    private int gate30KQuantity = 0;
    private long gate20KMoney = 0L;
    private int gate20KQuantity = 0;
    private long gate10KMoney = 0L;
    private int gate10KQuantity = 0;
    private long megaMoney = 0L;
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
    private long megaMoneyVat = 0L;
    private long money5MVat = 0L;
    private int money5MQuantityVat = 0;
    private long money3MVat = 0L;
    private int money3MQuantityVat = 0;
    private long money2MVat = 0L;
    private int money2MQuantityVat = 0;
    private long money1MVat = 0L;
    private int money1MQuantityVat = 0;
    private long money500KVat = 0L;
    private int money500KQuantityVat = 0;
    private long money300KVat = 0L;
    private int money300KQuantityVat = 0;
    private long money200KVat = 0L;
    private int money200KQuantityVat = 0;
    private long money100KVat = 0L;
    private int money100KQuantityVat = 0;
    private long money50KVat = 0L;
    private int money50KQuantityVat = 0;
    private long money20KVat = 0L;
    private int money20KQuantityVat = 0;
    private long money10KVat = 0L;
    private int money10KQuantityVat = 0;
    private long vcoinMoney = 0L;
    private long vcoin10MMoney = 0L;
    private int vcoin10MQuantity = 0;
    private long vcoin5MMoney = 0L;
    private int vcoin5MQuantity = 0;
    private long vcoin2MMoney = 0L;
    private int vcoin2MQuantity = 0;
    private long vcoin1MMoney = 0L;
    private int vcoin1MQuantity = 0;
    private long vcoin500KMoney = 0L;
    private int vcoin500KQuantity = 0;
    private long vcoin300KMoney = 0L;
    private int vcoin300KQuantity = 0;
    private long vcoin200KMoney = 0L;
    private int vcoin200KQuantity = 0;
    private long vcoin100KMoney = 0L;
    private int vcoin100KQuantity = 0;
    private long vcoin50KMoney = 0L;
    private int vcoin50KQuantity = 0;
    private long vcoin30KMoney = 0L;
    private int vcoin30KQuantity = 0;
    private long vcoin20KMoney = 0L;
    private int vcoin20KQuantity = 0;
    private long vcoin10KMoney = 0L;
    private int vcoin10KQuantity = 0;

    @Override
    public List<RechargeByCardReponse> searchRechargeByCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, int page, String transId) {
        final ArrayList<RechargeByCardReponse> results = new ArrayList<RechargeByCardReponse>();
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
        iterable = db.getCollection("dvt_recharge_by_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50).maxTime(30L, TimeUnit.SECONDS);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByCardReponse bank = new RechargeByCardReponse();
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
                bank.partner = document.getString("partner");
                results.add(bank);
            }
        });
        return results;
    }

    @Override
    public List<RechargeByCardReponse> exportDataRechargeByCard(String provider, String timeStart, String timeEnd, String amount, String code) {
        final ArrayList<RechargeByCardReponse> results = new ArrayList<RechargeByCardReponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        if (provider != null && !provider.equals("")) {
            conditions.put("provider", provider);
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        if (amount != null && !amount.equals("")) {
            conditions.put("amount", Integer.parseInt(amount));
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        iterable = db.getCollection("dvt_recharge_by_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByCardReponse bank = new RechargeByCardReponse();
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
                bank.updateTime = document.getString("update_time");
                bank.partner = document.getString("partner");
                results.add(bank);
            }
        });
        return results;
    }

    @Override
    public int countSearchRechargeByCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
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
        if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        int record = (int)db.getCollection("dvt_recharge_by_card").count((Bson)new Document((Map)conditions));
        return record;
    }

    @Override
    public long moneyTotal(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        this.totalMoney = 0L;
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
        iterable = db.getCollection("dvt_recharge_by_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByCardDAOImpl rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                rechargeByCardDAOImpl.totalMoney = rechargeByCardDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
            }
        });
        return this.totalMoney;
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> doiSoatRechargeByCard(int code, String timeStart, String timeEnd) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        this.totalMoney = 0L;
        this.viettelMoney = 0L;
        this.viettel5MMoney = 0L;
        this.viettel5MQuantity = 0;
        this.viettel2MMoney = 0L;
        this.viettel2MQuantity = 0;
        this.viettel1MMoney = 0L;
        this.viettel1MQuantity = 0;
        this.viettel500KMoney = 0L;
        this.viettel500KQuantity = 0;
        this.viettel300KMoney = 0L;
        this.viettel300KQuantity = 0;
        this.viettel200KMoney = 0L;
        this.viettel200KQuantity = 0;
        this.viettel100KMoney = 0L;
        this.viettel100KQuantity = 0;
        this.viettel50KMoney = 0L;
        this.viettel50KQuantity = 0;
        this.viettel30KMoney = 0L;
        this.viettel30KQuantity = 0;
        this.viettel20KMoney = 0L;
        this.viettel20KQuantity = 0;
        this.viettel10KMoney = 0L;
        this.viettel10KQuantity = 0;
        this.vinaphoneMoney = 0L;
        this.vinaphone5MMoney = 0L;
        this.vinaphone5MQuantity = 0;
        this.vinaphone2MMoney = 0L;
        this.vinaphone2MQuantity = 0;
        this.vinaphone1MMoney = 0L;
        this.vinaphone1MQuantity = 0;
        this.vinaphone500KMoney = 0L;
        this.vinaphone500KQuantity = 0;
        this.vinaphone300KMoney = 0L;
        this.vinaphone300KQuantity = 0;
        this.vinaphone200KMoney = 0L;
        this.vinaphone200KQuantity = 0;
        this.vinaphone100KMoney = 0L;
        this.vinaphone100KQuantity = 0;
        this.vinaphone50KMoney = 0L;
        this.vinaphone50KQuantity = 0;
        this.vinaphone30KMoney = 0L;
        this.vinaphone30KQuantity = 0;
        this.vinaphone20KMoney = 0L;
        this.vinaphone20KQuantity = 0;
        this.vinaphone10KMoney = 0L;
        this.vinaphone10KQuantity = 0;
        this.mobifoneMoney = 0L;
        this.mobifone5MMoney = 0L;
        this.mobifone5MQuantity = 0;
        this.mobifone2MMoney = 0L;
        this.mobifone2MQuantity = 0;
        this.mobifone1MMoney = 0L;
        this.mobifone1MQuantity = 0;
        this.mobifone500KMoney = 0L;
        this.mobifone500KQuantity = 0;
        this.mobifone300KMoney = 0L;
        this.mobifone300KQuantity = 0;
        this.mobifone200KMoney = 0L;
        this.mobifone200KQuantity = 0;
        this.mobifone100KMoney = 0L;
        this.mobifone100KQuantity = 0;
        this.mobifone50KMoney = 0L;
        this.mobifone50KQuantity = 0;
        this.mobifone30KMoney = 0L;
        this.mobifone30KQuantity = 0;
        this.mobifone20KMoney = 0L;
        this.mobifone20KQuantity = 0;
        this.mobifone10KMoney = 0L;
        this.mobifone10KQuantity = 0;
        this.gateMoney = 0L;
        this.gate5MMoney = 0L;
        this.gate5MQuantity = 0;
        this.gate2MMoney = 0L;
        this.gate2MQuantity = 0;
        this.gate1MMoney = 0L;
        this.gate1MQuantity = 0;
        this.gate500KMoney = 0L;
        this.gate500KQuantity = 0;
        this.gate300KMoney = 0L;
        this.gate300KQuantity = 0;
        this.gate200KMoney = 0L;
        this.gate200KQuantity = 0;
        this.gate100KMoney = 0L;
        this.gate100KQuantity = 0;
        this.gate50KMoney = 0L;
        this.gate50KQuantity = 0;
        this.gate30KMoney = 0L;
        this.gate30KQuantity = 0;
        this.gate20KMoney = 0L;
        this.gate20KQuantity = 0;
        this.gate10KMoney = 0L;
        this.gate10KQuantity = 0;
        this.megaMoney = 0L;
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
        this.megaMoneyVat = 0L;
        this.money5MVat = 0L;
        this.money5MQuantityVat = 0;
        this.money3MVat = 0L;
        this.money3MQuantityVat = 0;
        this.money2MVat = 0L;
        this.money2MQuantityVat = 0;
        this.money1MVat = 0L;
        this.money1MQuantityVat = 0;
        this.money500KVat = 0L;
        this.money500KQuantityVat = 0;
        this.money300KVat = 0L;
        this.money300KQuantityVat = 0;
        this.money200KVat = 0L;
        this.money200KQuantityVat = 0;
        this.money100KVat = 0L;
        this.money100KQuantityVat = 0;
        this.money50KVat = 0L;
        this.money50KQuantityVat = 0;
        this.money20KVat = 0L;
        this.money20KQuantityVat = 0;
        this.money10KVat = 0L;
        this.money10KQuantityVat = 0;
        this.vcoinMoney = 0L;
        this.vcoin10MMoney = 0L;
        this.vcoin10MQuantity = 0;
        this.vcoin5MMoney = 0L;
        this.vcoin5MQuantity = 0;
        this.vcoin2MMoney = 0L;
        this.vcoin2MQuantity = 0;
        this.vcoin1MMoney = 0L;
        this.vcoin1MQuantity = 0;
        this.vcoin500KMoney = 0L;
        this.vcoin500KQuantity = 0;
        this.vcoin300KMoney = 0L;
        this.vcoin300KQuantity = 0;
        this.vcoin200KMoney = 0L;
        this.vcoin200KQuantity = 0;
        this.vcoin100KMoney = 0L;
        this.vcoin100KQuantity = 0;
        this.vcoin50KMoney = 0L;
        this.vcoin50KQuantity = 0;
        this.vcoin30KMoney = 0L;
        this.vcoin30KQuantity = 0;
        this.vcoin20KMoney = 0L;
        this.vcoin20KQuantity = 0;
        this.vcoin10KMoney = 0L;
        this.vcoin10KQuantity = 0;
        objsort.put("_id", -1);
        conditions.put("code", code);
        if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        FindIterable iterable = null;
        iterable = db.getCollection("epay_recharge_by_mega_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByCardDAOImpl rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                rechargeByCardDAOImpl.totalMoney = rechargeByCardDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
                rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                rechargeByCardDAOImpl.megaMoney = rechargeByCardDAOImpl.megaMoney + (long)document.getInteger("amount").intValue();
                switch (document.getInteger("amount")) {
                    case 5000000: {
                        RechargeByCardDAOImpl.this.money5MQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money5M = rechargeByCardDAOImpl.money5M + 5000000L;
                        break;
                    }
                    case 3000000: {
                        RechargeByCardDAOImpl.this.money3MQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money3M = rechargeByCardDAOImpl.money3M + 3000000L;
                        break;
                    }
                    case 2000000: {
                        RechargeByCardDAOImpl.this.money2MQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money2M = rechargeByCardDAOImpl.money2M + 2000000L;
                        break;
                    }
                    case 1000000: {
                        RechargeByCardDAOImpl.this.money1MQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money1M = rechargeByCardDAOImpl.money1M + 1000000L;
                        break;
                    }
                    case 500000: {
                        RechargeByCardDAOImpl.this.money500KQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money500K = rechargeByCardDAOImpl.money500K + 500000L;
                        break;
                    }
                    case 300000: {
                        RechargeByCardDAOImpl.this.money300KQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money300K = rechargeByCardDAOImpl.money300K + 300000L;
                        break;
                    }
                    case 200000: {
                        RechargeByCardDAOImpl.this.money200KQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money200K = rechargeByCardDAOImpl.money200K + 200000L;
                        break;
                    }
                    case 100000: {
                        RechargeByCardDAOImpl.this.money100KQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money100K = rechargeByCardDAOImpl.money100K + 100000L;
                        break;
                    }
                    case 50000: {
                        RechargeByCardDAOImpl.this.money50KQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money50K = rechargeByCardDAOImpl.money50K + 50000L;
                        break;
                    }
                    case 20000: {
                        RechargeByCardDAOImpl.this.money20KQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money20K = rechargeByCardDAOImpl.money20K + 20000L;
                        break;
                    }
                    case 10000: {
                        RechargeByCardDAOImpl.this.money10KQuantity++;
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.money10K = rechargeByCardDAOImpl.money10K + 10000L;
                    }
                }
            }
        });
        iterable = db.getCollection("dvt_recharge_by_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByCardDAOImpl rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                rechargeByCardDAOImpl.totalMoney = rechargeByCardDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
                block8 : switch (document.getString("provider")) {
                    case "MegaCard": {
                        String userNameMega = "";
                        userNameMega = !document.containsKey("user_mega") ? "CTT_VINPLAY" : document.getString("user_mega");
                        switch (userNameMega) {
                            case "CTT_VINPLAY": {
                                RechargeByCardDAOImpl rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                rechargeByCardDAOImpl2.megaMoney = rechargeByCardDAOImpl2.megaMoney + (long)document.getInteger("amount").intValue();
                                switch (document.getInteger("amount")) {
                                    case 5000000: {
                                        RechargeByCardDAOImpl.this.money5MQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money5M = rechargeByCardDAOImpl2.money5M + 5000000L;
                                        break block8;
                                    }
                                    case 3000000: {
                                        RechargeByCardDAOImpl.this.money3MQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money3M = rechargeByCardDAOImpl2.money3M + 3000000L;
                                        break block8;
                                    }
                                    case 2000000: {
                                        RechargeByCardDAOImpl.this.money2MQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money2M = rechargeByCardDAOImpl2.money2M + 2000000L;
                                        break block8;
                                    }
                                    case 1000000: {
                                        RechargeByCardDAOImpl.this.money1MQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money1M = rechargeByCardDAOImpl2.money1M + 1000000L;
                                        break block8;
                                    }
                                    case 500000: {
                                        RechargeByCardDAOImpl.this.money500KQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money500K = rechargeByCardDAOImpl2.money500K + 500000L;
                                        break block8;
                                    }
                                    case 300000: {
                                        RechargeByCardDAOImpl.this.money300KQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money300K = rechargeByCardDAOImpl2.money300K + 300000L;
                                        break block8;
                                    }
                                    case 200000: {
                                        RechargeByCardDAOImpl.this.money200KQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money200K = rechargeByCardDAOImpl2.money200K + 200000L;
                                        break block8;
                                    }
                                    case 100000: {
                                        RechargeByCardDAOImpl.this.money100KQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money100K = rechargeByCardDAOImpl2.money100K + 100000L;
                                        break block8;
                                    }
                                    case 50000: {
                                        RechargeByCardDAOImpl.this.money50KQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money50K = rechargeByCardDAOImpl2.money50K + 50000L;
                                        break block8;
                                    }
                                    case 20000: {
                                        RechargeByCardDAOImpl.this.money20KQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money20K = rechargeByCardDAOImpl2.money20K + 20000L;
                                        break block8;
                                    }
                                    case 10000: {
                                        RechargeByCardDAOImpl.this.money10KQuantity++;
                                        rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl2.money10K = rechargeByCardDAOImpl2.money10K + 10000L;
                                        break block8;
                                    }
                                }
                                break block8;
                            }
                            case "VINPLAY": {
                                RechargeByCardDAOImpl rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                rechargeByCardDAOImpl3.megaMoneyVat = rechargeByCardDAOImpl3.megaMoneyVat + (long)document.getInteger("amount").intValue();
                                switch (document.getInteger("amount")) {
                                    case 5000000: {
                                        RechargeByCardDAOImpl.this.money5MQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money5MVat = rechargeByCardDAOImpl3.money5MVat + 5000000L;
                                        break block8;
                                    }
                                    case 3000000: {
                                        RechargeByCardDAOImpl.this.money3MQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money3MVat = rechargeByCardDAOImpl3.money3MVat + 3000000L;
                                        break block8;
                                    }
                                    case 2000000: {
                                        RechargeByCardDAOImpl.this.money2MQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money2MVat = rechargeByCardDAOImpl3.money2MVat + 2000000L;
                                        break block8;
                                    }
                                    case 1000000: {
                                        RechargeByCardDAOImpl.this.money1MQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money1MVat = rechargeByCardDAOImpl3.money1MVat + 1000000L;
                                        break block8;
                                    }
                                    case 500000: {
                                        RechargeByCardDAOImpl.this.money500KQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money500KVat = rechargeByCardDAOImpl3.money500KVat + 500000L;
                                        break block8;
                                    }
                                    case 300000: {
                                        RechargeByCardDAOImpl.this.money300KQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money300KVat = rechargeByCardDAOImpl3.money300KVat + 300000L;
                                        break block8;
                                    }
                                    case 200000: {
                                        RechargeByCardDAOImpl.this.money200KQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money200KVat = rechargeByCardDAOImpl3.money200KVat + 200000L;
                                        break block8;
                                    }
                                    case 100000: {
                                        RechargeByCardDAOImpl.this.money100KQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money100KVat = rechargeByCardDAOImpl3.money100KVat + 100000L;
                                        break block8;
                                    }
                                    case 50000: {
                                        RechargeByCardDAOImpl.this.money50KQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money50KVat = rechargeByCardDAOImpl3.money50KVat + 50000L;
                                        break block8;
                                    }
                                    case 20000: {
                                        RechargeByCardDAOImpl.this.money20KQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money20KVat = rechargeByCardDAOImpl3.money20KVat + 20000L;
                                        break block8;
                                    }
                                    case 10000: {
                                        RechargeByCardDAOImpl.this.money10KQuantityVat++;
                                        rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                                        rechargeByCardDAOImpl3.money10KVat = rechargeByCardDAOImpl3.money10KVat + 10000L;
                                        break block8;
                                    }
                                }
                                break block8;
                            }
                        }
                        break;
                    }
                    case "Viettel": {
                        RechargeByCardDAOImpl userNameMega = RechargeByCardDAOImpl.this;
                        userNameMega.viettelMoney = userNameMega.viettelMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                RechargeByCardDAOImpl.this.viettel5MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel5MMoney = userNameMega.viettel5MMoney + 5000000L;
                                break block8;
                            }
                            case 2000000: {
                                RechargeByCardDAOImpl.this.viettel2MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel2MMoney = userNameMega.viettel2MMoney + 2000000L;
                                break block8;
                            }
                            case 1000000: {
                                RechargeByCardDAOImpl.this.viettel1MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel1MMoney = userNameMega.viettel1MMoney + 1000000L;
                                break block8;
                            }
                            case 500000: {
                                RechargeByCardDAOImpl.this.viettel500KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel500KMoney = userNameMega.viettel500KMoney + 500000L;
                                break block8;
                            }
                            case 300000: {
                                RechargeByCardDAOImpl.this.viettel300KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel300KMoney = userNameMega.viettel300KMoney + 300000L;
                                break block8;
                            }
                            case 200000: {
                                RechargeByCardDAOImpl.this.viettel200KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel200KMoney = userNameMega.viettel200KMoney + 200000L;
                                break block8;
                            }
                            case 100000: {
                                RechargeByCardDAOImpl.this.viettel100KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel100KMoney = userNameMega.viettel100KMoney + 100000L;
                                break block8;
                            }
                            case 50000: {
                                RechargeByCardDAOImpl.this.viettel50KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel50KMoney = userNameMega.viettel50KMoney + 50000L;
                                break block8;
                            }
                            case 30000: {
                                RechargeByCardDAOImpl.this.viettel30KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel30KMoney = userNameMega.viettel30KMoney + 30000L;
                                break block8;
                            }
                            case 20000: {
                                RechargeByCardDAOImpl.this.viettel20KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel20KMoney = userNameMega.viettel20KMoney + 20000L;
                                break block8;
                            }
                            case 10000: {
                                RechargeByCardDAOImpl.this.viettel10KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.viettel10KMoney = userNameMega.viettel10KMoney + 10000L;
                                break block8;
                            }
                        }
                        break;
                    }
                    case "Vinaphone": {
                        RechargeByCardDAOImpl userNameMega = RechargeByCardDAOImpl.this;
                        userNameMega.vinaphoneMoney = userNameMega.vinaphoneMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                RechargeByCardDAOImpl.this.vinaphone5MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone5MMoney = userNameMega.vinaphone5MMoney + 5000000L;
                                break block8;
                            }
                            case 2000000: {
                                RechargeByCardDAOImpl.this.vinaphone2MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone2MMoney = userNameMega.vinaphone2MMoney + 2000000L;
                                break block8;
                            }
                            case 1000000: {
                                RechargeByCardDAOImpl.this.vinaphone1MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone1MMoney = userNameMega.vinaphone1MMoney + 1000000L;
                                break block8;
                            }
                            case 500000: {
                                RechargeByCardDAOImpl.this.vinaphone500KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone500KMoney = userNameMega.vinaphone500KMoney + 500000L;
                                break block8;
                            }
                            case 300000: {
                                RechargeByCardDAOImpl.this.vinaphone300KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone300KMoney = userNameMega.vinaphone300KMoney + 300000L;
                                break block8;
                            }
                            case 200000: {
                                RechargeByCardDAOImpl.this.vinaphone200KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone200KMoney = userNameMega.vinaphone200KMoney + 200000L;
                                break block8;
                            }
                            case 100000: {
                                RechargeByCardDAOImpl.this.vinaphone100KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone100KMoney = userNameMega.vinaphone100KMoney + 100000L;
                                break block8;
                            }
                            case 50000: {
                                RechargeByCardDAOImpl.this.vinaphone50KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone50KMoney = userNameMega.vinaphone50KMoney + 50000L;
                                break block8;
                            }
                            case 30000: {
                                RechargeByCardDAOImpl.this.vinaphone30KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone30KMoney = userNameMega.vinaphone30KMoney + 30000L;
                                break block8;
                            }
                            case 20000: {
                                RechargeByCardDAOImpl.this.vinaphone20KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone20KMoney = userNameMega.vinaphone20KMoney + 20000L;
                                break block8;
                            }
                            case 10000: {
                                RechargeByCardDAOImpl.this.vinaphone10KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vinaphone10KMoney = userNameMega.vinaphone10KMoney + 10000L;
                                break block8;
                            }
                        }
                        break;
                    }
                    case "Mobifone": {
                        RechargeByCardDAOImpl userNameMega = RechargeByCardDAOImpl.this;
                        userNameMega.mobifoneMoney = userNameMega.mobifoneMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                RechargeByCardDAOImpl.this.mobifone5MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone5MMoney = userNameMega.mobifone5MMoney + 5000000L;
                                break block8;
                            }
                            case 2000000: {
                                RechargeByCardDAOImpl.this.mobifone2MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone2MMoney = userNameMega.mobifone2MMoney + 2000000L;
                                break block8;
                            }
                            case 1000000: {
                                RechargeByCardDAOImpl.this.mobifone1MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone1MMoney = userNameMega.mobifone1MMoney + 1000000L;
                                break block8;
                            }
                            case 500000: {
                                RechargeByCardDAOImpl.this.mobifone500KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone500KMoney = userNameMega.mobifone500KMoney + 500000L;
                                break block8;
                            }
                            case 300000: {
                                RechargeByCardDAOImpl.this.mobifone300KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone300KMoney = userNameMega.mobifone300KMoney + 300000L;
                                break block8;
                            }
                            case 200000: {
                                RechargeByCardDAOImpl.this.mobifone200KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone200KMoney = userNameMega.mobifone200KMoney + 200000L;
                                break block8;
                            }
                            case 100000: {
                                RechargeByCardDAOImpl.this.mobifone100KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone100KMoney = userNameMega.mobifone100KMoney + 100000L;
                                break block8;
                            }
                            case 50000: {
                                RechargeByCardDAOImpl.this.mobifone50KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone50KMoney = userNameMega.mobifone50KMoney + 50000L;
                                break block8;
                            }
                            case 30000: {
                                RechargeByCardDAOImpl.this.mobifone30KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone30KMoney = userNameMega.mobifone30KMoney + 30000L;
                                break block8;
                            }
                            case 20000: {
                                RechargeByCardDAOImpl.this.mobifone20KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone20KMoney = userNameMega.mobifone20KMoney + 20000L;
                                break block8;
                            }
                            case 10000: {
                                RechargeByCardDAOImpl.this.mobifone10KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.mobifone10KMoney = userNameMega.mobifone10KMoney + 10000L;
                                break block8;
                            }
                        }
                        break;
                    }
                    case "Gate": {
                        RechargeByCardDAOImpl userNameMega = RechargeByCardDAOImpl.this;
                        userNameMega.gateMoney = userNameMega.gateMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                RechargeByCardDAOImpl.this.gate5MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate5MMoney = userNameMega.gate5MMoney + 5000000L;
                                break block8;
                            }
                            case 2000000: {
                                RechargeByCardDAOImpl.this.gate2MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate2MMoney = userNameMega.gate2MMoney + 2000000L;
                                break block8;
                            }
                            case 1000000: {
                                RechargeByCardDAOImpl.this.gate1MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate1MMoney = userNameMega.gate1MMoney + 1000000L;
                                break block8;
                            }
                            case 500000: {
                                RechargeByCardDAOImpl.this.gate500KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate500KMoney = userNameMega.gate500KMoney + 500000L;
                                break block8;
                            }
                            case 300000: {
                                RechargeByCardDAOImpl.this.gate300KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate300KMoney = userNameMega.gate300KMoney + 300000L;
                                break block8;
                            }
                            case 200000: {
                                RechargeByCardDAOImpl.this.gate200KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate200KMoney = userNameMega.gate200KMoney + 200000L;
                                break block8;
                            }
                            case 100000: {
                                RechargeByCardDAOImpl.this.gate100KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate100KMoney = userNameMega.gate100KMoney + 100000L;
                                break block8;
                            }
                            case 50000: {
                                RechargeByCardDAOImpl.this.gate50KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate50KMoney = userNameMega.gate50KMoney + 50000L;
                                break block8;
                            }
                            case 30000: {
                                RechargeByCardDAOImpl.this.gate30KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate30KMoney = userNameMega.gate30KMoney + 30000L;
                                break block8;
                            }
                            case 20000: {
                                RechargeByCardDAOImpl.this.gate20KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate20KMoney = userNameMega.gate20KMoney + 20000L;
                                break block8;
                            }
                            case 10000: {
                                RechargeByCardDAOImpl.this.gate10KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.gate10KMoney = userNameMega.gate10KMoney + 10000L;
                                break block8;
                            }
                        }
                        break;
                    }
                    case "Vcoin": {
                        RechargeByCardDAOImpl userNameMega = RechargeByCardDAOImpl.this;
                        userNameMega.vcoinMoney = userNameMega.vcoinMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 10000000: {
                                RechargeByCardDAOImpl.this.vcoin10MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin10MMoney = userNameMega.vcoin10MMoney + 10000000L;
                                break block8;
                            }
                            case 5000000: {
                                RechargeByCardDAOImpl.this.vcoin5MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin5MMoney = userNameMega.vcoin5MMoney + 5000000L;
                                break block8;
                            }
                            case 2000000: {
                                RechargeByCardDAOImpl.this.vcoin2MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin2MMoney = userNameMega.vcoin2MMoney + 2000000L;
                                break block8;
                            }
                            case 1000000: {
                                RechargeByCardDAOImpl.this.vcoin1MQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin1MMoney = userNameMega.vcoin1MMoney + 1000000L;
                                break block8;
                            }
                            case 500000: {
                                RechargeByCardDAOImpl.this.vcoin500KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin500KMoney = userNameMega.vcoin500KMoney + 500000L;
                                break block8;
                            }
                            case 300000: {
                                RechargeByCardDAOImpl.this.vcoin300KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin300KMoney = userNameMega.vcoin300KMoney + 300000L;
                                break block8;
                            }
                            case 200000: {
                                RechargeByCardDAOImpl.this.vcoin200KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin200KMoney = userNameMega.vcoin200KMoney + 200000L;
                                break block8;
                            }
                            case 100000: {
                                RechargeByCardDAOImpl.this.vcoin100KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin100KMoney = userNameMega.vcoin100KMoney + 100000L;
                                break block8;
                            }
                            case 50000: {
                                RechargeByCardDAOImpl.this.vcoin50KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin50KMoney = userNameMega.vcoin50KMoney + 50000L;
                                break block8;
                            }
                            case 30000: {
                                RechargeByCardDAOImpl.this.vcoin30KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin30KMoney = userNameMega.vcoin30KMoney + 30000L;
                                break block8;
                            }
                            case 20000: {
                                RechargeByCardDAOImpl.this.vcoin20KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin20KMoney = userNameMega.vcoin20KMoney + 20000L;
                                break block8;
                            }
                            case 10000: {
                                RechargeByCardDAOImpl.this.vcoin10KQuantity++;
                                userNameMega = RechargeByCardDAOImpl.this;
                                userNameMega.vcoin10KMoney = userNameMega.vcoin10KMoney + 10000L;
                                break block8;
                            }
                        }
                        break;
                    }
                }
            }
        });
        ArrayList<MoneyTotalRechargeByCardReponse> listReponse = new ArrayList<MoneyTotalRechargeByCardReponse>();
        MoneyTotalRechargeByCardReponse tong = new MoneyTotalRechargeByCardReponse();
        tong.setName("Tong");
        tong.setValue(this.totalMoney);
        tong.setTrans((List)null);
        listReponse.add(tong);
        MoneyTotalRechargeByCardReponse viettel = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listViettelMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue viettelMoney5MFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney5MFollowFace.setFaceValue(5000000);
        viettelMoney5MFollowFace.setQuantity(this.viettel5MQuantity);
        viettelMoney5MFollowFace.setMoneyTotal(this.viettel5MMoney);
        listViettelMoneyFollowFace.add(viettelMoney5MFollowFace);
        MoneyTotalFollowFaceValue viettelMoney2MFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney2MFollowFace.setFaceValue(2000000);
        viettelMoney2MFollowFace.setQuantity(this.viettel2MQuantity);
        viettelMoney2MFollowFace.setMoneyTotal(this.viettel2MMoney);
        listViettelMoneyFollowFace.add(viettelMoney2MFollowFace);
        MoneyTotalFollowFaceValue viettelMoney1MFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney1MFollowFace.setFaceValue(1000000);
        viettelMoney1MFollowFace.setQuantity(this.viettel1MQuantity);
        viettelMoney1MFollowFace.setMoneyTotal(this.viettel1MMoney);
        listViettelMoneyFollowFace.add(viettelMoney1MFollowFace);
        MoneyTotalFollowFaceValue viettelMoney500KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney500KFollowFace.setFaceValue(500000);
        viettelMoney500KFollowFace.setQuantity(this.viettel500KQuantity);
        viettelMoney500KFollowFace.setMoneyTotal(this.viettel500KMoney);
        listViettelMoneyFollowFace.add(viettelMoney500KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney300KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney300KFollowFace.setFaceValue(300000);
        viettelMoney300KFollowFace.setQuantity(this.viettel300KQuantity);
        viettelMoney300KFollowFace.setMoneyTotal(this.viettel300KMoney);
        listViettelMoneyFollowFace.add(viettelMoney300KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney200KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney200KFollowFace.setFaceValue(200000);
        viettelMoney200KFollowFace.setQuantity(this.viettel200KQuantity);
        viettelMoney200KFollowFace.setMoneyTotal(this.viettel200KMoney);
        listViettelMoneyFollowFace.add(viettelMoney200KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney100KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney100KFollowFace.setFaceValue(100000);
        viettelMoney100KFollowFace.setQuantity(this.viettel100KQuantity);
        viettelMoney100KFollowFace.setMoneyTotal(this.viettel100KMoney);
        listViettelMoneyFollowFace.add(viettelMoney100KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney50KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney50KFollowFace.setFaceValue(50000);
        viettelMoney50KFollowFace.setQuantity(this.viettel50KQuantity);
        viettelMoney50KFollowFace.setMoneyTotal(this.viettel50KMoney);
        listViettelMoneyFollowFace.add(viettelMoney50KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney30KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney30KFollowFace.setFaceValue(30000);
        viettelMoney30KFollowFace.setQuantity(this.viettel30KQuantity);
        viettelMoney30KFollowFace.setMoneyTotal(this.viettel30KMoney);
        listViettelMoneyFollowFace.add(viettelMoney30KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney20KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney20KFollowFace.setFaceValue(20000);
        viettelMoney20KFollowFace.setQuantity(this.viettel20KQuantity);
        viettelMoney20KFollowFace.setMoneyTotal(this.viettel20KMoney);
        listViettelMoneyFollowFace.add(viettelMoney20KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney10KFollowFace.setFaceValue(10000);
        viettelMoney10KFollowFace.setQuantity(this.viettel10KQuantity);
        viettelMoney10KFollowFace.setMoneyTotal(this.viettel10KMoney);
        listViettelMoneyFollowFace.add(viettelMoney10KFollowFace);
        viettel.setName("Viettel");
        viettel.setValue(this.viettelMoney);
        viettel.setTrans(listViettelMoneyFollowFace);
        listReponse.add(viettel);
        MoneyTotalRechargeByCardReponse vinaphone = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listVinaphoneMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue vinaphoneMoney5MFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney5MFollowFace.setFaceValue(5000000);
        vinaphoneMoney5MFollowFace.setQuantity(this.vinaphone5MQuantity);
        vinaphoneMoney5MFollowFace.setMoneyTotal(this.vinaphone5MMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney5MFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney2MFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney2MFollowFace.setFaceValue(2000000);
        vinaphoneMoney2MFollowFace.setQuantity(this.vinaphone2MQuantity);
        vinaphoneMoney2MFollowFace.setMoneyTotal(this.vinaphone2MMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney2MFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney1MFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney1MFollowFace.setFaceValue(1000000);
        vinaphoneMoney1MFollowFace.setQuantity(this.vinaphone1MQuantity);
        vinaphoneMoney1MFollowFace.setMoneyTotal(this.vinaphone1MMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney1MFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney500KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney500KFollowFace.setFaceValue(500000);
        vinaphoneMoney500KFollowFace.setQuantity(this.vinaphone500KQuantity);
        vinaphoneMoney500KFollowFace.setMoneyTotal(this.vinaphone500KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney500KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney300KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney300KFollowFace.setFaceValue(300000);
        vinaphoneMoney300KFollowFace.setQuantity(this.vinaphone300KQuantity);
        vinaphoneMoney300KFollowFace.setMoneyTotal(this.vinaphone300KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney300KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney200KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney200KFollowFace.setFaceValue(200000);
        vinaphoneMoney200KFollowFace.setQuantity(this.vinaphone200KQuantity);
        vinaphoneMoney200KFollowFace.setMoneyTotal(this.vinaphone200KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney200KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney100KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney100KFollowFace.setFaceValue(100000);
        vinaphoneMoney100KFollowFace.setQuantity(this.vinaphone100KQuantity);
        vinaphoneMoney100KFollowFace.setMoneyTotal(this.vinaphone100KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney100KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney50KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney50KFollowFace.setFaceValue(50000);
        vinaphoneMoney50KFollowFace.setQuantity(this.vinaphone50KQuantity);
        vinaphoneMoney50KFollowFace.setMoneyTotal(this.vinaphone50KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney50KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney30KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney30KFollowFace.setFaceValue(30000);
        vinaphoneMoney30KFollowFace.setQuantity(this.vinaphone30KQuantity);
        vinaphoneMoney30KFollowFace.setMoneyTotal(this.vinaphone30KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney30KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney20KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney20KFollowFace.setFaceValue(20000);
        vinaphoneMoney20KFollowFace.setQuantity(this.vinaphone20KQuantity);
        vinaphoneMoney20KFollowFace.setMoneyTotal(this.vinaphone20KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney20KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney10KFollowFace.setFaceValue(10000);
        vinaphoneMoney10KFollowFace.setQuantity(this.vinaphone10KQuantity);
        vinaphoneMoney10KFollowFace.setMoneyTotal(this.vinaphone10KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney10KFollowFace);
        vinaphone.setName("Vinaphone");
        vinaphone.setValue(this.vinaphoneMoney);
        vinaphone.setTrans(listVinaphoneMoneyFollowFace);
        listReponse.add(vinaphone);
        MoneyTotalRechargeByCardReponse mobifone = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listMobifoneMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue mobifoneMoney5MFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney5MFollowFace.setFaceValue(5000000);
        mobifoneMoney5MFollowFace.setQuantity(this.mobifone5MQuantity);
        mobifoneMoney5MFollowFace.setMoneyTotal(this.mobifone5MMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney5MFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney2MFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney2MFollowFace.setFaceValue(2000000);
        mobifoneMoney2MFollowFace.setQuantity(this.mobifone2MQuantity);
        mobifoneMoney2MFollowFace.setMoneyTotal(this.mobifone2MMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney2MFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney1MFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney1MFollowFace.setFaceValue(1000000);
        mobifoneMoney1MFollowFace.setQuantity(this.mobifone1MQuantity);
        mobifoneMoney1MFollowFace.setMoneyTotal(this.mobifone1MMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney1MFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney500KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney500KFollowFace.setFaceValue(500000);
        mobifoneMoney500KFollowFace.setQuantity(this.mobifone500KQuantity);
        mobifoneMoney500KFollowFace.setMoneyTotal(this.mobifone500KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney500KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney300KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney300KFollowFace.setFaceValue(300000);
        mobifoneMoney300KFollowFace.setQuantity(this.mobifone300KQuantity);
        mobifoneMoney300KFollowFace.setMoneyTotal(this.mobifone300KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney300KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney200KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney200KFollowFace.setFaceValue(200000);
        mobifoneMoney200KFollowFace.setQuantity(this.mobifone200KQuantity);
        mobifoneMoney200KFollowFace.setMoneyTotal(this.mobifone200KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney200KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney100KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney100KFollowFace.setFaceValue(100000);
        mobifoneMoney100KFollowFace.setQuantity(this.mobifone100KQuantity);
        mobifoneMoney100KFollowFace.setMoneyTotal(this.mobifone100KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney100KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney50KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney50KFollowFace.setFaceValue(50000);
        mobifoneMoney50KFollowFace.setQuantity(this.mobifone50KQuantity);
        mobifoneMoney50KFollowFace.setMoneyTotal(this.mobifone50KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney50KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney30KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney30KFollowFace.setFaceValue(30000);
        mobifoneMoney30KFollowFace.setQuantity(this.mobifone30KQuantity);
        mobifoneMoney30KFollowFace.setMoneyTotal(this.mobifone30KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney30KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney20KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney20KFollowFace.setFaceValue(20000);
        mobifoneMoney20KFollowFace.setQuantity(this.mobifone20KQuantity);
        mobifoneMoney20KFollowFace.setMoneyTotal(this.mobifone20KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney20KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney10KFollowFace.setFaceValue(10000);
        mobifoneMoney10KFollowFace.setQuantity(this.mobifone10KQuantity);
        mobifoneMoney10KFollowFace.setMoneyTotal(this.mobifone10KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney10KFollowFace);
        mobifone.setName("Mobifone");
        mobifone.setValue(this.mobifoneMoney);
        mobifone.setTrans(listMobifoneMoneyFollowFace);
        listReponse.add(mobifone);
        MoneyTotalRechargeByCardReponse gate = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listGateMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue gateMoney5MFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney5MFollowFace.setFaceValue(5000000);
        gateMoney5MFollowFace.setQuantity(this.gate5MQuantity);
        gateMoney5MFollowFace.setMoneyTotal(this.gate5MMoney);
        listGateMoneyFollowFace.add(gateMoney5MFollowFace);
        MoneyTotalFollowFaceValue gateMoney2MFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney2MFollowFace.setFaceValue(2000000);
        gateMoney2MFollowFace.setQuantity(this.gate2MQuantity);
        gateMoney2MFollowFace.setMoneyTotal(this.gate2MMoney);
        listGateMoneyFollowFace.add(gateMoney2MFollowFace);
        MoneyTotalFollowFaceValue gateMoney1MFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney1MFollowFace.setFaceValue(1000000);
        gateMoney1MFollowFace.setQuantity(this.gate1MQuantity);
        gateMoney1MFollowFace.setMoneyTotal(this.gate1MMoney);
        listGateMoneyFollowFace.add(gateMoney1MFollowFace);
        MoneyTotalFollowFaceValue gateMoney500KFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney500KFollowFace.setFaceValue(500000);
        gateMoney500KFollowFace.setQuantity(this.gate500KQuantity);
        gateMoney500KFollowFace.setMoneyTotal(this.gate500KMoney);
        listGateMoneyFollowFace.add(gateMoney500KFollowFace);
        MoneyTotalFollowFaceValue gateMoney300KFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney300KFollowFace.setFaceValue(300000);
        gateMoney300KFollowFace.setQuantity(this.gate300KQuantity);
        gateMoney300KFollowFace.setMoneyTotal(this.gate300KMoney);
        listGateMoneyFollowFace.add(gateMoney300KFollowFace);
        MoneyTotalFollowFaceValue gateMoney200KFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney200KFollowFace.setFaceValue(200000);
        gateMoney200KFollowFace.setQuantity(this.gate200KQuantity);
        gateMoney200KFollowFace.setMoneyTotal(this.gate200KMoney);
        listGateMoneyFollowFace.add(gateMoney200KFollowFace);
        MoneyTotalFollowFaceValue gateMoney100KFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney100KFollowFace.setFaceValue(100000);
        gateMoney100KFollowFace.setQuantity(this.gate100KQuantity);
        gateMoney100KFollowFace.setMoneyTotal(this.gate100KMoney);
        listGateMoneyFollowFace.add(gateMoney100KFollowFace);
        MoneyTotalFollowFaceValue gateMoney50KFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney50KFollowFace.setFaceValue(50000);
        gateMoney50KFollowFace.setQuantity(this.gate50KQuantity);
        gateMoney50KFollowFace.setMoneyTotal(this.gate50KMoney);
        listGateMoneyFollowFace.add(gateMoney50KFollowFace);
        MoneyTotalFollowFaceValue gateMoney30KFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney30KFollowFace.setFaceValue(30000);
        gateMoney30KFollowFace.setQuantity(this.gate30KQuantity);
        gateMoney30KFollowFace.setMoneyTotal(this.gate30KMoney);
        listGateMoneyFollowFace.add(gateMoney30KFollowFace);
        MoneyTotalFollowFaceValue gateMoney20KFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney20KFollowFace.setFaceValue(20000);
        gateMoney20KFollowFace.setQuantity(this.gate20KQuantity);
        gateMoney20KFollowFace.setMoneyTotal(this.gate20KMoney);
        listGateMoneyFollowFace.add(gateMoney20KFollowFace);
        MoneyTotalFollowFaceValue gateMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        gateMoney10KFollowFace.setFaceValue(10000);
        gateMoney10KFollowFace.setQuantity(this.gate10KQuantity);
        gateMoney10KFollowFace.setMoneyTotal(this.gate10KMoney);
        listGateMoneyFollowFace.add(gateMoney10KFollowFace);
        gate.setName("Gate");
        gate.setValue(this.gateMoney);
        gate.setTrans(listGateMoneyFollowFace);
        listReponse.add(gate);
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
        megacard.setValue(this.megaMoney);
        megacard.setTrans(moneyFollowFace);
        listReponse.add(megacard);
        MoneyTotalRechargeByCardReponse megacardVat = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> moneyFollowFaceVat = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue money5MFollowFaceVat = new MoneyTotalFollowFaceValue();
        money5MFollowFaceVat.setFaceValue(5000000);
        money5MFollowFaceVat.setQuantity(this.money5MQuantityVat);
        money5MFollowFaceVat.setMoneyTotal(this.money5MVat);
        moneyFollowFaceVat.add(money5MFollowFaceVat);
        MoneyTotalFollowFaceValue money3MFollowFaceVat = new MoneyTotalFollowFaceValue();
        money3MFollowFaceVat.setFaceValue(3000000);
        money3MFollowFaceVat.setQuantity(this.money3MQuantityVat);
        money3MFollowFaceVat.setMoneyTotal(this.money3MVat);
        moneyFollowFaceVat.add(money3MFollowFaceVat);
        MoneyTotalFollowFaceValue money2MFollowFaceVat = new MoneyTotalFollowFaceValue();
        money2MFollowFaceVat.setFaceValue(2000000);
        money2MFollowFaceVat.setQuantity(this.money2MQuantityVat);
        money2MFollowFaceVat.setMoneyTotal(this.money2MVat);
        moneyFollowFaceVat.add(money2MFollowFaceVat);
        MoneyTotalFollowFaceValue money1MFollowFaceVat = new MoneyTotalFollowFaceValue();
        money1MFollowFaceVat.setFaceValue(1000000);
        money1MFollowFaceVat.setQuantity(this.money1MQuantityVat);
        money1MFollowFaceVat.setMoneyTotal(this.money1MVat);
        moneyFollowFaceVat.add(money1MFollowFaceVat);
        MoneyTotalFollowFaceValue money500KFollowFaceVat = new MoneyTotalFollowFaceValue();
        money500KFollowFaceVat.setFaceValue(500000);
        money500KFollowFaceVat.setQuantity(this.money500KQuantityVat);
        money500KFollowFaceVat.setMoneyTotal(this.money500KVat);
        moneyFollowFaceVat.add(money500KFollowFaceVat);
        MoneyTotalFollowFaceValue money300KFollowFaceVat = new MoneyTotalFollowFaceValue();
        money300KFollowFaceVat.setFaceValue(300000);
        money300KFollowFaceVat.setQuantity(this.money300KQuantityVat);
        money300KFollowFaceVat.setMoneyTotal(this.money300KVat);
        moneyFollowFaceVat.add(money300KFollowFaceVat);
        MoneyTotalFollowFaceValue money200KFollowFaceVat = new MoneyTotalFollowFaceValue();
        money200KFollowFaceVat.setFaceValue(200000);
        money200KFollowFaceVat.setQuantity(this.money200KQuantityVat);
        money200KFollowFaceVat.setMoneyTotal(this.money200KVat);
        moneyFollowFaceVat.add(money200KFollowFaceVat);
        MoneyTotalFollowFaceValue money100KFollowFaceVat = new MoneyTotalFollowFaceValue();
        money100KFollowFaceVat.setFaceValue(100000);
        money100KFollowFaceVat.setQuantity(this.money100KQuantityVat);
        money100KFollowFaceVat.setMoneyTotal(this.money100KVat);
        moneyFollowFaceVat.add(money100KFollowFaceVat);
        MoneyTotalFollowFaceValue money50KFollowFaceVat = new MoneyTotalFollowFaceValue();
        money50KFollowFaceVat.setFaceValue(50000);
        money50KFollowFaceVat.setQuantity(this.money50KQuantityVat);
        money50KFollowFaceVat.setMoneyTotal(this.money50KVat);
        moneyFollowFaceVat.add(money50KFollowFaceVat);
        MoneyTotalFollowFaceValue money20KFollowFaceVat = new MoneyTotalFollowFaceValue();
        money20KFollowFaceVat.setFaceValue(20000);
        money20KFollowFaceVat.setQuantity(this.money20KQuantityVat);
        money20KFollowFaceVat.setMoneyTotal(this.money20KVat);
        moneyFollowFaceVat.add(money20KFollowFaceVat);
        MoneyTotalFollowFaceValue money10KFollowFaceVat = new MoneyTotalFollowFaceValue();
        money10KFollowFaceVat.setFaceValue(10000);
        money10KFollowFaceVat.setQuantity(this.money10KQuantityVat);
        money10KFollowFaceVat.setMoneyTotal(this.money10KVat);
        moneyFollowFaceVat.add(money10KFollowFaceVat);
        megacardVat.setName("MegaCard_VAT");
        megacardVat.setValue(this.megaMoneyVat);
        megacardVat.setTrans(moneyFollowFaceVat);
        listReponse.add(megacardVat);
        MoneyTotalRechargeByCardReponse vcoin = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> vcoinFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue vcoin10MFollowFace = new MoneyTotalFollowFaceValue();
        vcoin10MFollowFace.setFaceValue(10000000);
        vcoin10MFollowFace.setQuantity(this.vcoin10MQuantity);
        vcoin10MFollowFace.setMoneyTotal(this.vcoin10MMoney);
        vcoinFollowFace.add(vcoin10MFollowFace);
        MoneyTotalFollowFaceValue vcoin5MFollowFace = new MoneyTotalFollowFaceValue();
        vcoin5MFollowFace.setFaceValue(5000000);
        vcoin5MFollowFace.setQuantity(this.vcoin5MQuantity);
        vcoin5MFollowFace.setMoneyTotal(this.vcoin5MMoney);
        vcoinFollowFace.add(vcoin5MFollowFace);
        MoneyTotalFollowFaceValue vcoin2MFollowFace = new MoneyTotalFollowFaceValue();
        vcoin2MFollowFace.setFaceValue(2000000);
        vcoin2MFollowFace.setQuantity(this.vcoin2MQuantity);
        vcoin2MFollowFace.setMoneyTotal(this.vcoin2MMoney);
        vcoinFollowFace.add(vcoin2MFollowFace);
        MoneyTotalFollowFaceValue vcoin1MFollowFace = new MoneyTotalFollowFaceValue();
        vcoin1MFollowFace.setFaceValue(1000000);
        vcoin1MFollowFace.setQuantity(this.vcoin1MQuantity);
        vcoin1MFollowFace.setMoneyTotal(this.vcoin1MMoney);
        vcoinFollowFace.add(vcoin1MFollowFace);
        MoneyTotalFollowFaceValue vcoin500KFollowFace = new MoneyTotalFollowFaceValue();
        vcoin500KFollowFace.setFaceValue(500000);
        vcoin500KFollowFace.setQuantity(this.vcoin500KQuantity);
        vcoin500KFollowFace.setMoneyTotal(this.vcoin500KMoney);
        vcoinFollowFace.add(vcoin500KFollowFace);
        MoneyTotalFollowFaceValue vcoin300KFollowFace = new MoneyTotalFollowFaceValue();
        vcoin300KFollowFace.setFaceValue(300000);
        vcoin300KFollowFace.setQuantity(this.vcoin300KQuantity);
        vcoin300KFollowFace.setMoneyTotal(this.vcoin300KMoney);
        vcoinFollowFace.add(vcoin300KFollowFace);
        MoneyTotalFollowFaceValue vcoin200KFollowFace = new MoneyTotalFollowFaceValue();
        vcoin200KFollowFace.setFaceValue(200000);
        vcoin200KFollowFace.setQuantity(this.vcoin200KQuantity);
        vcoin200KFollowFace.setMoneyTotal(this.vcoin200KMoney);
        vcoinFollowFace.add(vcoin200KFollowFace);
        MoneyTotalFollowFaceValue vcoin100KFollowFace = new MoneyTotalFollowFaceValue();
        vcoin100KFollowFace.setFaceValue(100000);
        vcoin100KFollowFace.setQuantity(this.vcoin100KQuantity);
        vcoin100KFollowFace.setMoneyTotal(this.vcoin100KMoney);
        vcoinFollowFace.add(vcoin100KFollowFace);
        MoneyTotalFollowFaceValue vcoin50KFollowFace = new MoneyTotalFollowFaceValue();
        vcoin50KFollowFace.setFaceValue(50000);
        vcoin50KFollowFace.setQuantity(this.vcoin50KQuantity);
        vcoin50KFollowFace.setMoneyTotal(this.vcoin50KMoney);
        vcoinFollowFace.add(vcoin50KFollowFace);
        MoneyTotalFollowFaceValue vcoin30KFollowFace = new MoneyTotalFollowFaceValue();
        vcoin30KFollowFace.setFaceValue(30000);
        vcoin30KFollowFace.setQuantity(this.vcoin30KQuantity);
        vcoin30KFollowFace.setMoneyTotal(this.vcoin30KMoney);
        vcoinFollowFace.add(vcoin30KFollowFace);
        MoneyTotalFollowFaceValue vcoin20KFollowFace = new MoneyTotalFollowFaceValue();
        vcoin20KFollowFace.setFaceValue(20000);
        vcoin20KFollowFace.setQuantity(this.vcoin20KQuantity);
        vcoin20KFollowFace.setMoneyTotal(this.vcoin20KMoney);
        vcoinFollowFace.add(vcoin20KFollowFace);
        MoneyTotalFollowFaceValue vcoin10KFollowFace = new MoneyTotalFollowFaceValue();
        vcoin10KFollowFace.setFaceValue(10000);
        vcoin10KFollowFace.setQuantity(this.vcoin10KQuantity);
        vcoin10KFollowFace.setMoneyTotal(this.vcoin10KMoney);
        vcoinFollowFace.add(vcoin10KFollowFace);
        vcoin.setName("Vcoin");
        vcoin.setValue(this.vcoinMoney);
        vcoin.setTrans(vcoinFollowFace);
        listReponse.add(vcoin);
        return listReponse;
    }

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeByCard(String nickName, String provider, String serial, String pin, String code, String timeStart, String timeEnd, String transId) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        this.totalMoney = 0L;
        this.webMoney = 0L;
        this.androidMoney = 0L;
        this.iosMoney = 0L;
        this.winphoneMoney = 0L;
        this.facebookMoney = 0L;
        this.desktopMoney = 0L;
        this.otherMoney = 0L;
        this.viettelMoney = 0L;
        this.vinaphoneMoney = 0L;
        this.mobifoneMoney = 0L;
        this.gateMoney = 0L;
        this.megaMoney = 0L;
        this.vcoinMoney = 0L;
        objsort.put("_id", -1);
        if (transId != null && !transId.isEmpty()) {
            conditions.put("reference_id", transId);
        }
        if (!nickName.isEmpty()) {
            conditions.put("nick_name", nickName);
        }
        if (!provider.isEmpty()) {
            conditions.put("provider", provider);
        }
        if (!serial.isEmpty()) {
            conditions.put("serial", serial);
        }
        if (!pin.isEmpty()) {
            conditions.put("pin", pin);
        }
        if (!code.isEmpty()) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        FindIterable iterable = null;
        iterable = db.getCollection("epay_recharge_by_mega_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                Object string;
                RechargeByCardDAOImpl rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                rechargeByCardDAOImpl.totalMoney = rechargeByCardDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
                rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                rechargeByCardDAOImpl.megaMoney = rechargeByCardDAOImpl.megaMoney + (long)document.getInteger("amount").intValue();
                if (document.containsKey("platform")) {
                    switch (document.getString("platform")) {
                        case "web": {
                            RechargeByCardDAOImpl rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl2.webMoney = rechargeByCardDAOImpl2.webMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "ad": {
                            RechargeByCardDAOImpl rechargeByCardDAOImpl3 = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl3.androidMoney = rechargeByCardDAOImpl3.androidMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "ios": {
                            RechargeByCardDAOImpl rechargeByCardDAOImpl4 = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl4.iosMoney = rechargeByCardDAOImpl4.iosMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "wp": {
                            RechargeByCardDAOImpl rechargeByCardDAOImpl5 = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl5.winphoneMoney = rechargeByCardDAOImpl5.winphoneMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "fb": {
                            RechargeByCardDAOImpl rechargeByCardDAOImpl6 = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl6.facebookMoney = rechargeByCardDAOImpl6.facebookMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "dt": {
                            RechargeByCardDAOImpl rechargeByCardDAOImpl7 = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl7.desktopMoney = rechargeByCardDAOImpl7.desktopMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "ot": {
                            RechargeByCardDAOImpl rechargeByCardDAOImpl8 = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl8.otherMoney = rechargeByCardDAOImpl8.otherMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        default: {
                            RechargeByCardDAOImpl rechargeByCardDAOImpl9 = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl9.otherMoney = rechargeByCardDAOImpl9.otherMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                    }
                } else {
                    string = RechargeByCardDAOImpl.this;
                    ((RechargeByCardDAOImpl)string).otherMoney = ((RechargeByCardDAOImpl)string).otherMoney + (long)document.getInteger("amount").intValue();
                }
            }
        });
        iterable = db.getCollection("dvt_recharge_by_card").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByCardDAOImpl rechargeByCardDAOImpl;
                Object string;
                RechargeByCardDAOImpl rechargeByCardDAOImpl2 = RechargeByCardDAOImpl.this;
                rechargeByCardDAOImpl2.totalMoney = rechargeByCardDAOImpl2.totalMoney + (long)document.getInteger("amount").intValue();
                if (document.containsKey("platform")) {
                    switch (document.getString("platform")) {
                        case "web": {
                            rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl.webMoney = rechargeByCardDAOImpl.webMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "ad": {
                            rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl.androidMoney = rechargeByCardDAOImpl.androidMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "ios": {
                            rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl.iosMoney = rechargeByCardDAOImpl.iosMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "wp": {
                            rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl.winphoneMoney = rechargeByCardDAOImpl.winphoneMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "fb": {
                            rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl.facebookMoney = rechargeByCardDAOImpl.facebookMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "dt": {
                            rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl.desktopMoney = rechargeByCardDAOImpl.desktopMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        case "ot": {
                            rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl.otherMoney = rechargeByCardDAOImpl.otherMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                        default: {
                            rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                            rechargeByCardDAOImpl.otherMoney = rechargeByCardDAOImpl.otherMoney + (long)document.getInteger("amount").intValue();
                            break;
                        }
                    }
                } else {
                    string = RechargeByCardDAOImpl.this;
                    ((RechargeByCardDAOImpl)string).otherMoney = ((RechargeByCardDAOImpl)string).otherMoney + (long)document.getInteger("amount").intValue();
                }
                String string2 = document.getString("provider");
                switch (string2) {
                    case "Viettel": {
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.viettelMoney = rechargeByCardDAOImpl.viettelMoney + (long)document.getInteger("amount").intValue();
                        break;
                    }
                    case "Vinaphone": {
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.vinaphoneMoney = rechargeByCardDAOImpl.vinaphoneMoney + (long)document.getInteger("amount").intValue();
                        break;
                    }
                    case "Mobifone": {
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.mobifoneMoney = rechargeByCardDAOImpl.mobifoneMoney + (long)document.getInteger("amount").intValue();
                        break;
                    }
                    case "Gate": {
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.gateMoney = rechargeByCardDAOImpl.gateMoney + (long)document.getInteger("amount").intValue();
                        break;
                    }
                    case "MegaCard": {
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.megaMoney = rechargeByCardDAOImpl.megaMoney + (long)document.getInteger("amount").intValue();
                        break;
                    }
                    case "Vcoin": {
                        rechargeByCardDAOImpl = RechargeByCardDAOImpl.this;
                        rechargeByCardDAOImpl.vcoinMoney = rechargeByCardDAOImpl.vcoinMoney + (long)document.getInteger("amount").intValue();
                    }
                }
            }
        });
        ArrayList<MoneyTotalRechargeByCardReponse> listReponse = new ArrayList<MoneyTotalRechargeByCardReponse>();
        MoneyTotalRechargeByCardReponse tong = new MoneyTotalRechargeByCardReponse();
        tong.setName("Tong");
        tong.setValue(this.totalMoney);
        listReponse.add(tong);
        MoneyTotalRechargeByCardReponse web = new MoneyTotalRechargeByCardReponse();
        web.setName("web");
        web.setValue(this.webMoney);
        listReponse.add(web);
        MoneyTotalRechargeByCardReponse android = new MoneyTotalRechargeByCardReponse();
        android.setName("ad");
        android.setValue(this.androidMoney);
        listReponse.add(android);
        MoneyTotalRechargeByCardReponse ios = new MoneyTotalRechargeByCardReponse();
        ios.setName("ios");
        ios.setValue(this.iosMoney);
        listReponse.add(ios);
        MoneyTotalRechargeByCardReponse winphone = new MoneyTotalRechargeByCardReponse();
        winphone.setName("wp");
        winphone.setValue(this.winphoneMoney);
        listReponse.add(winphone);
        MoneyTotalRechargeByCardReponse facebook = new MoneyTotalRechargeByCardReponse();
        facebook.setName("fb");
        facebook.setValue(this.facebookMoney);
        listReponse.add(facebook);
        MoneyTotalRechargeByCardReponse desktop = new MoneyTotalRechargeByCardReponse();
        desktop.setName("dt");
        desktop.setValue(this.desktopMoney);
        listReponse.add(desktop);
        MoneyTotalRechargeByCardReponse other = new MoneyTotalRechargeByCardReponse();
        other.setName("ot");
        other.setValue(this.otherMoney);
        listReponse.add(other);
        MoneyTotalRechargeByCardReponse viettel = new MoneyTotalRechargeByCardReponse();
        viettel.setName("Viettel");
        viettel.setValue(this.viettelMoney);
        listReponse.add(viettel);
        MoneyTotalRechargeByCardReponse vinaphone = new MoneyTotalRechargeByCardReponse();
        vinaphone.setName("Vinaphone");
        vinaphone.setValue(this.vinaphoneMoney);
        listReponse.add(vinaphone);
        MoneyTotalRechargeByCardReponse mobifone = new MoneyTotalRechargeByCardReponse();
        mobifone.setName("Mobifone");
        mobifone.setValue(this.mobifoneMoney);
        listReponse.add(mobifone);
        MoneyTotalRechargeByCardReponse gate = new MoneyTotalRechargeByCardReponse();
        gate.setName("Gate");
        gate.setValue(this.gateMoney);
        listReponse.add(gate);
        MoneyTotalRechargeByCardReponse megacard = new MoneyTotalRechargeByCardReponse();
        megacard.setName("MegaCard");
        megacard.setValue(this.megaMoney);
        listReponse.add(megacard);
        MoneyTotalRechargeByCardReponse vcoin = new MoneyTotalRechargeByCardReponse();
        vcoin.setName("Vcoin");
        vcoin.setValue(this.vcoinMoney);
        listReponse.add(vcoin);
        return listReponse;
    }

}

