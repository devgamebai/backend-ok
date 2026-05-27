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
 *  com.vinplay.vbee.common.response.NganLuongFollowFaceValue
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.DoiSoatNganLuongDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.NganLuongFollowFaceValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class DoiSoatNganLuongDAOImpl
implements DoiSoatNganLuongDAO {
    private long _10KMoney = 0L;
    private int _10KQuantity = 0;
    private long _20KMoney = 0L;
    private int _20KQuantity = 0;
    private long _100KMoney = 0L;
    private int _100KQuantity = 0;
    private long _200KMoney = 0L;
    private int _200KQuantity = 0;
    private long _500KMoney = 0L;
    private int _500KQuantity = 0;
    private long _1MMoney = 0L;
    private int _1MQuantity = 0;
    private long _2MMoney = 0L;
    private int _2MQuantity = 0;
    private long _5MMoney = 0L;
    private int _5MQuantity = 0;
    private long _10MMoney = 0L;
    private int _10MQuantity = 0;
    private long _20MMoney = 0L;
    private int _20MQuantity = 0;

    @Override
    public List<NganLuongFollowFaceValue> getDoiSoatData(String timeStart, String timeEnd) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        this._10KMoney = 0L;
        this._10KQuantity = 0;
        this._20KMoney = 0L;
        this._20KQuantity = 0;
        this._100KMoney = 0L;
        this._100KQuantity = 0;
        this._200KMoney = 0L;
        this._200KQuantity = 0;
        this._500KMoney = 0L;
        this._500KQuantity = 0;
        this._1MMoney = 0L;
        this._1MQuantity = 0;
        this._2MMoney = 0L;
        this._2MQuantity = 0;
        this._5MMoney = 0L;
        this._5MQuantity = 0;
        this._10MMoney = 0L;
        this._10MQuantity = 0;
        this._20MMoney = 0L;
        this._20MQuantity = 0;
        objsort.put("_id", -1);
        if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("trans_time", obj);
        }
        conditions.put("error_code_return", "00");
        FindIterable iterable = null;
        iterable = db.getCollection("ngan_luong_transaction").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                switch (document.getInteger("total_amount")) {
                    case 10000: {
                        DoiSoatNganLuongDAOImpl.this._10KQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._10KMoney = doiSoatNganLuongDAOImpl._10KMoney + 10000L;
                        break;
                    }
                    case 20000: {
                        DoiSoatNganLuongDAOImpl.this._20KQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._20KMoney = doiSoatNganLuongDAOImpl._20KMoney + 20000L;
                        break;
                    }
                    case 100000: {
                        DoiSoatNganLuongDAOImpl.this._100KQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._100KMoney = doiSoatNganLuongDAOImpl._100KMoney + 100000L;
                        break;
                    }
                    case 200000: {
                        DoiSoatNganLuongDAOImpl.this._200KQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._200KMoney = doiSoatNganLuongDAOImpl._200KMoney + 200000L;
                        break;
                    }
                    case 500000: {
                        DoiSoatNganLuongDAOImpl.this._500KQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._500KMoney = doiSoatNganLuongDAOImpl._500KMoney + 500000L;
                        break;
                    }
                    case 1000000: {
                        DoiSoatNganLuongDAOImpl.this._1MQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._1MMoney = doiSoatNganLuongDAOImpl._1MMoney + 1000000L;
                        break;
                    }
                    case 2000000: {
                        DoiSoatNganLuongDAOImpl.this._2MQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._2MMoney = doiSoatNganLuongDAOImpl._2MMoney + 2000000L;
                        break;
                    }
                    case 5000000: {
                        DoiSoatNganLuongDAOImpl.this._5MQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._5MMoney = doiSoatNganLuongDAOImpl._5MMoney + 5000000L;
                        break;
                    }
                    case 10000000: {
                        DoiSoatNganLuongDAOImpl.this._10MQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._10MMoney = doiSoatNganLuongDAOImpl._10MMoney + 10000000L;
                        break;
                    }
                    case 20000000: {
                        DoiSoatNganLuongDAOImpl.this._20MQuantity++;
                        DoiSoatNganLuongDAOImpl doiSoatNganLuongDAOImpl = DoiSoatNganLuongDAOImpl.this;
                        doiSoatNganLuongDAOImpl._20MMoney = doiSoatNganLuongDAOImpl._20MMoney + 20000000L;
                    }
                }
            }
        });
        ArrayList<NganLuongFollowFaceValue> listResponse = new ArrayList<NganLuongFollowFaceValue>();
        NganLuongFollowFaceValue _10K = new NganLuongFollowFaceValue();
        _10K.setFaceValue(10000);
        _10K.setMoneyTotal(this._10KMoney);
        _10K.setQuantity(this._10KQuantity);
        listResponse.add(_10K);
        NganLuongFollowFaceValue _20K = new NganLuongFollowFaceValue();
        _20K.setFaceValue(20000);
        _20K.setMoneyTotal(this._20KMoney);
        _20K.setQuantity(this._20KQuantity);
        listResponse.add(_20K);
        NganLuongFollowFaceValue _100K = new NganLuongFollowFaceValue();
        _100K.setFaceValue(100000);
        _100K.setMoneyTotal(this._100KMoney);
        _100K.setQuantity(this._100KQuantity);
        listResponse.add(_100K);
        NganLuongFollowFaceValue _200K = new NganLuongFollowFaceValue();
        _200K.setFaceValue(200000);
        _200K.setMoneyTotal(this._200KMoney);
        _200K.setQuantity(this._200KQuantity);
        listResponse.add(_200K);
        NganLuongFollowFaceValue _500K = new NganLuongFollowFaceValue();
        _500K.setFaceValue(500000);
        _500K.setMoneyTotal(this._500KMoney);
        _500K.setQuantity(this._500KQuantity);
        listResponse.add(_500K);
        NganLuongFollowFaceValue _1M = new NganLuongFollowFaceValue();
        _1M.setFaceValue(1000000);
        _1M.setMoneyTotal(this._1MMoney);
        _1M.setQuantity(this._1MQuantity);
        listResponse.add(_1M);
        NganLuongFollowFaceValue _2M = new NganLuongFollowFaceValue();
        _2M.setFaceValue(2000000);
        _2M.setMoneyTotal(this._2MMoney);
        _2M.setQuantity(this._2MQuantity);
        listResponse.add(_2M);
        NganLuongFollowFaceValue _5M = new NganLuongFollowFaceValue();
        _5M.setFaceValue(5000000);
        _5M.setMoneyTotal(this._5MMoney);
        _5M.setQuantity(this._5MQuantity);
        listResponse.add(_5M);
        NganLuongFollowFaceValue _10M = new NganLuongFollowFaceValue();
        _10M.setFaceValue(10000000);
        _10M.setMoneyTotal(this._10MMoney);
        _10M.setQuantity(this._10MQuantity);
        listResponse.add(_10M);
        NganLuongFollowFaceValue _20M = new NganLuongFollowFaceValue();
        _20M.setFaceValue(20000000);
        _20M.setMoneyTotal(this._20MMoney);
        _20M.setQuantity(this._20MQuantity);
        listResponse.add(_20M);
        return listResponse;
    }

}

