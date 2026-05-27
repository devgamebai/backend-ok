/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.usercore.response.LogExchangeMoneyResponse
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.payment.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.payment.dao.PaymentDao;
import com.vinplay.usercore.response.LogExchangeMoneyResponse;
import com.vinplay.vbee.common.messages.pay.ExchangeMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;

public class PaymentDaoImpl
implements PaymentDao {
    @Override
    public boolean logExchangeMoney(ExchangeMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_exchange_money");
        Document doc = new Document();
        doc.append("nick_name", message.nickname);
        doc.append("merchant_id", message.merchantId);
        doc.append("trans_id", message.merchantTransId);
        doc.append("transaction_no", message.transNo);
        doc.append("money", message.money);
        doc.append("money_type", message.moneyType);
        doc.append("type", message.type);
        doc.append("money_exchange", message.exchangeMoney);
        doc.append("fee", message.fee);
        doc.append("code", message.code);
        doc.append("ip", message.ip);
        doc.append("trans_time", message.getCreateTime());
        doc.append("create_time", new Date());
        col.insertOne(doc);
        return true;
    }

    @Override
    public boolean checkMerchantTransId(String merchantId, String merchantTransId) {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        Document conditions = new Document();
        conditions.put("merchant_id", merchantId);
        conditions.put("trans_id", merchantTransId);
        Document dc = (Document)db.getCollection("log_exchange_money").find((Bson)conditions).first();
        return dc != null;
    }

    @Override
    public LogExchangeMoneyResponse getLogExchangeMoney(String nickname, String merchantId, String transId, String transNo, String type, int code, String startTime, String endTime, int page) throws Exception {
        final ArrayList trans = new ArrayList();
        final List<Long> num = Arrays.asList(0L, 0L, 0L, 0L);
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        int numStart = (page - 1) * 50;
        int numEnd = 50;
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (!nickname.isEmpty()) {
            conditions.put("nick_name", nickname);
        }
        if (!merchantId.isEmpty()) {
            conditions.put("merchant_id", merchantId);
        }
        if (!transId.isEmpty()) {
            conditions.put("trans_id", transId);
        }
        if (!transNo.isEmpty()) {
            conditions.put("transaction_no", transNo);
        }
        if (!type.isEmpty()) {
            conditions.put("type", type);
        }
        if (code >= 0) {
            conditions.put("code", code);
        }
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("trans_time", obj);
        }
        FindIterable iterable = db.getCollection("log_exchange_money").find((Bson)new Document(conditions)).sort((Bson)objsort).skip(numStart).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                String nn = document.getString("nick_name");
                String mid = document.getString("merchant_id");
                String mtid = document.getString("trans_id");
                String tn = document.getString("transaction_no");
                long mn = document.getLong("money");
                String mt = document.getString("money_type");
                String t = document.getString("type");
                long exm = document.getLong("money_exchange");
                long f = document.getLong("fee");
                int c = document.getInteger("code");
                String ip = document.getString("ip");
                trans.add(new ExchangeMessage(nn, mid, mtid, tn, mn, mt, t, exm, f, c, ip));
            }
        });
        FindIterable iterable2 = db.getCollection("log_exchange_money").find((Bson)new Document(conditions));
        iterable2.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                long mn = document.getLong("money");
                long exm = document.getLong("money_exchange");
                long f = document.getLong("fee");
                num.set(0, (Long)num.get(0) + mn);
                num.set(1, (Long)num.get(1) + exm);
                num.set(2, (Long)num.get(2) + f);
                num.set(3, (Long)num.get(3) + 1L);
            }
        });
        return new LogExchangeMoneyResponse(num.get(0).longValue(), num.get(1).longValue(), num.get(2).longValue(), num.get(3).longValue(), trans);
    }

    @Override
    public long getTotalMoney(String merchantId, String nickname, String startTime, String endTime, String type) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        MongoCollection collection = db.getCollection("log_exchange_money");
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("trans_time", obj);
        }
        if (merchantId != null && !merchantId.isEmpty()) {
            conditions.put("merchant_id", merchantId);
        }
        if (nickname != null && !nickname.isEmpty()) {
            conditions.put("nick_name", nickname);
        }
        if (type != null && !type.isEmpty()) {
            conditions.put("type", type);
        }
        long money = 0L;
        Document dc = (Document)collection.aggregate(Arrays.asList(new Document("$match", new Document(conditions)), new Document("$group", new Document("_id", null).append("money", new Document("$sum", "$money"))))).first();
        if (dc != null) {
            money = dc.getLong("money");
        }
        return money;
    }

    @Override
    public List<ExchangeMessage> getExchangeMoney(String nickname, String merchantId, String transId, String transNo, String type, int code, String startTime, String endTime) throws Exception {
        final ArrayList<ExchangeMessage> trans = new ArrayList<ExchangeMessage>();
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (!nickname.isEmpty()) {
            conditions.put("nick_name", nickname);
        }
        if (!merchantId.isEmpty()) {
            conditions.put("merchant_id", merchantId);
        }
        if (!transId.isEmpty()) {
            conditions.put("trans_id", transId);
        }
        if (!transNo.isEmpty()) {
            conditions.put("transaction_no", transNo);
        }
        if (!type.isEmpty()) {
            conditions.put("type", type);
        }
        if (code >= 0) {
            conditions.put("code", code);
        }
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("trans_time", obj);
        }
        FindIterable iterable = db.getCollection("log_exchange_money").find((Bson)new Document(conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                String nn = document.getString("nick_name");
                String mid = document.getString("merchant_id");
                String mtid = document.getString("trans_id");
                String tn = document.getString("transaction_no");
                long mn = document.getLong("money");
                String mt = document.getString("money_type");
                String t = document.getString("type");
                long exm = document.getLong("money_exchange");
                long f = document.getLong("fee");
                int c = document.getInteger("code");
                String ip = document.getString("ip");
                String tt = document.getString("trans_time");
                ExchangeMessage ms = new ExchangeMessage(nn, mid, mtid, tn, mn, mt, t, exm, f, c, ip);
                ms.setCreateTime(tt);
                trans.add(ms);
            }
        });
        return trans;
    }
}

