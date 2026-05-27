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
 *  com.vinplay.vbee.common.response.RechardByIAPResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.RechargeByIAPDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.RechardByIAPResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class RechargeByIAPDAOImpl
implements RechargeByIAPDAO {
    private long totalMoney = 0L;

    @Override
    public List<RechardByIAPResponse> ListRechargeIAP(String nickName, String code, String timeStart, String timeEnd, String amount, String orderId, int page) {
        final ArrayList<RechardByIAPResponse> results = new ArrayList<RechardByIAPResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        int num_start = (page - 1) * 50;
        int num_end = 50;
        Document conditions = new Document();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("trans_time", obj);
        }
        if (amount != null && !amount.equals("")) {
            conditions.put("amount", Integer.parseInt(amount));
        }
        if (orderId != null && !orderId.equals("")) {
            conditions.put("order_id", orderId);
        }
        iterable = db.getCollection("dvt_recharge_by_iap").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechardByIAPResponse iap = new RechardByIAPResponse();
                iap.nick_name = document.getString("nick_name");
                iap.amount = document.getInteger("amount");
                iap.code = document.getInteger("code");
                iap.description = document.getString("description");
                iap.trans_time = document.getString("trans_time");
                iap.order_id = document.getString("order_id");
                iap.package_name = document.getString("package_name");
                iap.product_id = document.getString("product_id");
                iap.purchase_time = document.getLong("purchase_time");
                iap.purchase_state = document.getInteger("purchase_state");
                iap.developer_payload = document.getString("developer_payload");
                iap.token = document.getString("token");
                iap.signture = document.getString("signature");
                results.add(iap);
            }
        });
        return results;
    }

    @Override
    public long totalMoney(String nickName, String code, String timeStart, String timeEnd, String amount, String orderId) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        Document conditions = new Document();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("trans_time", obj);
        }
        if (amount != null && !amount.equals("")) {
            conditions.put("amount", Integer.parseInt(amount));
        }
        if (orderId != null && !orderId.equals("")) {
            conditions.put("order_id", orderId);
        }
        iterable = db.getCollection("dvt_recharge_by_iap").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeByIAPDAOImpl rechargeByIAPDAOImpl = RechargeByIAPDAOImpl.this;
                rechargeByIAPDAOImpl.totalMoney = rechargeByIAPDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
            }
        });
        return this.totalMoney;
    }

    @Override
    public long countListRechargeIAP(String nickName, String code, String timeStart, String timeEnd, String amount, String orderId) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        Document conditions = new Document();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("trans_time", obj);
        }
        if (amount != null && !amount.equals("")) {
            conditions.put("amount", Integer.parseInt(amount));
        }
        if (orderId != null && !orderId.equals("")) {
            conditions.put("order_id", orderId);
        }
        long totalRecord = db.getCollection("dvt_recharge_by_iap").count((Bson)new Document((Map)conditions));
        return totalRecord;
    }

}

