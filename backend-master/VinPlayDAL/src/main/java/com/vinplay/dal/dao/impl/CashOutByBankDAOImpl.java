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
 *  com.vinplay.vbee.common.response.CashOutByBankReponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.CashOutByBankDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.CashOutByBankReponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class CashOutByBankDAOImpl
implements CashOutByBankDAO {
    private long totalMoney = 0L;

    @Override
    public List<CashOutByBankReponse> searchCashOutByBank(String nickName, String bank, String status, String code, String timeStart, String timeEnd, int page, String transId) {
        final ArrayList<CashOutByBankReponse> result = new ArrayList<CashOutByBankReponse>();
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
        if (bank != null && !bank.equals("")) {
            conditions.put("bank", bank);
        }
        if (status != null && !status.equals("")) {
            conditions.put("status", Integer.parseInt(status));
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("dvt_cash_out_by_bank").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                CashOutByBankReponse bank = new CashOutByBankReponse();
                bank.refenrenceId = document.getString("reference_id");
                bank.nickName = document.getString("nick_name");
                bank.bank = document.getString("bank");
                bank.account = document.getString("account");
                bank.name = document.getString("name");
                bank.amount = document.getInteger("amount");
                bank.description = document.getString("description");
                bank.status = document.getInteger("status");
                bank.message = document.getString("message");
                bank.sign = document.getString("sign");
                bank.code = document.getInteger("code");
                bank.description = document.getString("description");
                bank.timeLog = document.getString("time_log");
                bank.updateTime = document.getString("update_time");
                result.add(bank);
            }
        });
        return result;
    }

    @Override
    public int countSearchCashOutByBank(String nickName, String bank, String status, String code, String timeStart, String timeEnd, String transId) {
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
        if (bank != null && !bank.equals("")) {
            conditions.put("bank", bank);
        }
        if (status != null && !status.equals("")) {
            conditions.put("status", Integer.parseInt(status));
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        int record = (int)db.getCollection("dvt_cash_out_by_bank").count((Bson)new Document((Map)conditions));
        return record;
    }

    @Override
    public long moneyTotal(String nickName, String bank, String status, String code, String timeStart, String timeEnd, String transId) {
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
        if (bank != null && !bank.equals("")) {
            conditions.put("bank", bank);
        }
        if (status != null && !status.equals("")) {
            conditions.put("status", Integer.parseInt(status));
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        } else {
            BasicDBObject query1 = new BasicDBObject("code", Integer.parseInt("34"));
            BasicDBObject query2 = new BasicDBObject("code", Integer.parseInt("0"));
            ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
            myList.add(query1);
            myList.add(query2);
            conditions.put("$or", myList);
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        FindIterable iterable = null;
        iterable = db.getCollection("dvt_cash_out_by_bank").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                CashOutByBankDAOImpl cashOutByBankDAOImpl = CashOutByBankDAOImpl.this;
                cashOutByBankDAOImpl.totalMoney = cashOutByBankDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
            }
        });
        return this.totalMoney;
    }

}

