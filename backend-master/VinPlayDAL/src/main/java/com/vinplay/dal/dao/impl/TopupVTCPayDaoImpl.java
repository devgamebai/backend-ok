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
 *  com.vinplay.vbee.common.response.topupVTCPay.LogTopupVTCPay
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.TopupVTCPayDao;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import com.vinplay.vbee.common.response.topupVTCPay.LogTopupVTCPay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class TopupVTCPayDaoImpl
implements TopupVTCPayDao {
    private long money10K = 0L;
    private int money10KQuantity = 0;
    private long money20K = 0L;
    private int money20KQuantity = 0;
    private long money50K = 0L;
    private int money50KQuantity = 0;
    private long money100K = 0L;
    private int money100KQuantity = 0;
    private long money200K = 0L;
    private int money200KQuantity = 0;
    private long money500K = 0L;
    private int money500KQuantity = 0;

    @Override
    public List<LogTopupVTCPay> getLogTopupVtcPay(String nickname, String price, String transId, String startTime, String endTime, String page) {
        final ArrayList<LogTopupVTCPay> response = new ArrayList<LogTopupVTCPay>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_topup_vtcpay");
        int numStart = (Integer.parseInt(page) - 1) * 50;
        int numEnd = 50;
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (nickname != null && !nickname.isEmpty()) {
            String pattern = ".*" + nickname + ".*";
            conditions.put("nick_name", new BasicDBObject().append("$regex", pattern).append("$options", "i"));
        }
        if (price != null && !price.isEmpty()) {
            conditions.put("price", Integer.parseInt(price));
        }
        if (transId != null && !transId.isEmpty()) {
            conditions.put("partner_trans_id", transId);
        }
        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("time_request", obj);
        }
        FindIterable iterable = col.find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                LogTopupVTCPay model = new LogTopupVTCPay(document.getString("partner_trans_id"), document.getString("nick_name"), document.getInteger("price").intValue(), document.getInteger("status").intValue(), document.getString("response_code"), document.getString("description"), TopupVTCPayDaoImpl.this.formatTime(document.getString("time_request")), TopupVTCPayDaoImpl.this.formatTime(document.getString("time_response")));
                response.add(model);
            }
        });
        return response;
    }

    @Override
    public List<MoneyTotalFollowFaceValue> doiSoatTopupVtcPay(String startTime, String endTime) {
        this.money10K = 0L;
        this.money10KQuantity = 0;
        this.money20K = 0L;
        this.money20KQuantity = 0;
        this.money50K = 0L;
        this.money50KQuantity = 0;
        this.money100K = 0L;
        this.money100KQuantity = 0;
        this.money200K = 0L;
        this.money200KQuantity = 0;
        this.money500K = 0L;
        this.money500KQuantity = 0;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        Document conditions = new Document();
        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("time_request", obj);
        }
        conditions.put("status", 1);
        FindIterable iterable = db.getCollection("log_topup_vtcpay").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                switch (document.getInteger("price")) {
                    case 10000: {
                        TopupVTCPayDaoImpl.this.money10KQuantity++;
                        TopupVTCPayDaoImpl topupVTCPayDaoImpl = TopupVTCPayDaoImpl.this;
                        topupVTCPayDaoImpl.money10K = topupVTCPayDaoImpl.money10K + 10000L;
                        break;
                    }
                    case 20000: {
                        TopupVTCPayDaoImpl.this.money20KQuantity++;
                        TopupVTCPayDaoImpl topupVTCPayDaoImpl = TopupVTCPayDaoImpl.this;
                        topupVTCPayDaoImpl.money20K = topupVTCPayDaoImpl.money20K + 20000L;
                        break;
                    }
                    case 50000: {
                        TopupVTCPayDaoImpl.this.money50KQuantity++;
                        TopupVTCPayDaoImpl topupVTCPayDaoImpl = TopupVTCPayDaoImpl.this;
                        topupVTCPayDaoImpl.money50K = topupVTCPayDaoImpl.money50K + 50000L;
                        break;
                    }
                    case 100000: {
                        TopupVTCPayDaoImpl.this.money100KQuantity++;
                        TopupVTCPayDaoImpl topupVTCPayDaoImpl = TopupVTCPayDaoImpl.this;
                        topupVTCPayDaoImpl.money100K = topupVTCPayDaoImpl.money100K + 100000L;
                        break;
                    }
                    case 200000: {
                        TopupVTCPayDaoImpl.this.money200KQuantity++;
                        TopupVTCPayDaoImpl topupVTCPayDaoImpl = TopupVTCPayDaoImpl.this;
                        topupVTCPayDaoImpl.money200K = topupVTCPayDaoImpl.money200K + 200000L;
                        break;
                    }
                    case 500000: {
                        TopupVTCPayDaoImpl.this.money500KQuantity++;
                        TopupVTCPayDaoImpl topupVTCPayDaoImpl = TopupVTCPayDaoImpl.this;
                        topupVTCPayDaoImpl.money500K = topupVTCPayDaoImpl.money500K + 500000L;
                    }
                }
            }
        });
        ArrayList<MoneyTotalFollowFaceValue> response = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue money10KFollowFace = new MoneyTotalFollowFaceValue();
        money10KFollowFace.setFaceValue(10000);
        money10KFollowFace.setQuantity(this.money10KQuantity);
        money10KFollowFace.setMoneyTotal(this.money10K);
        response.add(money10KFollowFace);
        MoneyTotalFollowFaceValue money20KFollowFace = new MoneyTotalFollowFaceValue();
        money20KFollowFace.setFaceValue(20000);
        money20KFollowFace.setQuantity(this.money20KQuantity);
        money20KFollowFace.setMoneyTotal(this.money20K);
        response.add(money20KFollowFace);
        MoneyTotalFollowFaceValue money50KFollowFace = new MoneyTotalFollowFaceValue();
        money50KFollowFace.setFaceValue(50000);
        money50KFollowFace.setQuantity(this.money50KQuantity);
        money50KFollowFace.setMoneyTotal(this.money50K);
        response.add(money50KFollowFace);
        MoneyTotalFollowFaceValue money100KFollowFace = new MoneyTotalFollowFaceValue();
        money100KFollowFace.setFaceValue(100000);
        money100KFollowFace.setQuantity(this.money100KQuantity);
        money100KFollowFace.setMoneyTotal(this.money100K);
        response.add(money100KFollowFace);
        MoneyTotalFollowFaceValue money200KFollowFace = new MoneyTotalFollowFaceValue();
        money200KFollowFace.setFaceValue(200000);
        money200KFollowFace.setQuantity(this.money200KQuantity);
        money200KFollowFace.setMoneyTotal(this.money200K);
        response.add(money200KFollowFace);
        MoneyTotalFollowFaceValue money500KFollowFace = new MoneyTotalFollowFaceValue();
        money500KFollowFace.setFaceValue(500000);
        money500KFollowFace.setQuantity(this.money500KQuantity);
        money500KFollowFace.setMoneyTotal(this.money500K);
        response.add(money500KFollowFace);
        return response;
    }

    private String formatTime(String inputTime) {
        if (inputTime == null) {
            return inputTime;
        }
        if (inputTime.length() > 12) {
            String year = inputTime.substring(0, 4);
            String month = inputTime.substring(4, 6);
            String day = inputTime.substring(6, 8);
            String hour = inputTime.substring(8, 10);
            String min = inputTime.substring(10, 12);
            String sec = inputTime.substring(12);
            return year + "-" + month + "-" + day + " " + hour + ":" + min + ":" + sec;
        }
        return inputTime;
    }

}

