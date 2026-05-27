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
 *  com.vinplay.vbee.common.response.CashOutByTopUpResponse
 *  com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue
 *  com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.CashOutByTopUpDAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.CashOutByTopUpResponse;
import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class CashOutByTopUpDAOImpl
implements CashOutByTopUpDAO {
    private long totalMoney = 0L;
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
    private long vietNamMobileMoney = 0L;
    private long vietNamMobile5MMoney = 0L;
    private int vietNamMobile5MQuantity = 0;
    private long vietNamMobile2MMoney = 0L;
    private int vietNamMobile2MQuantity = 0;
    private long vietNamMobile1MMoney = 0L;
    private int vietNamMobile1MQuantity = 0;
    private long vietNamMobile500KMoney = 0L;
    private int vietNamMobile500KQuantity = 0;
    private long vietNamMobile300KMoney = 0L;
    private int vietNamMobile300KQuantity = 0;
    private long vietNamMobile200KMoney = 0L;
    private int vietNamMobile200KQuantity = 0;
    private long vietNamMobile100KMoney = 0L;
    private int vietNamMobile100KQuantity = 0;
    private long vietNamMobile50KMoney = 0L;
    private int vietNamMobile50KQuantity = 0;
    private long vietNamMobile30KMoney = 0L;
    private int vietNamMobile30KQuantity = 0;
    private long vietNamMobile20KMoney = 0L;
    private int vietNamMobile20KQuantity = 0;
    private long vietNamMobile10KMoney = 0L;
    private int vietNamMobile10KQuantity = 0;
    private long beeLineMoney = 0L;
    private long beeLine5MMoney = 0L;
    private int beeLine5MQuantity = 0;
    private long beeLine2MMoney = 0L;
    private int beeLine2MQuantity = 0;
    private long beeLine1MMoney = 0L;
    private int beeLine1MQuantity = 0;
    private long beeLine500KMoney = 0L;
    private int beeLine500KQuantity = 0;
    private long beeLine300KMoney = 0L;
    private int beeLine300KQuantity = 0;
    private long beeLine200KMoney = 0L;
    private int beeLine200KQuantity = 0;
    private long beeLine100KMoney = 0L;
    private int beeLine100KQuantity = 0;
    private long beeLine50KMoney = 0L;
    private int beeLine50KQuantity = 0;
    private long beeLine30KMoney = 0L;
    private int beeLine30KQuantity = 0;
    private long beeLine20KMoney = 0L;
    private int beeLine20KQuantity = 0;
    private long beeLine10KMoney = 0L;
    private int beeLine10KQuantity = 0;

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalCashOutByTopup(String timeStart, String timeEnd, String partner, String code, String type) {
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
        this.vietNamMobileMoney = 0L;
        this.vietNamMobile5MMoney = 0L;
        this.vietNamMobile5MQuantity = 0;
        this.vietNamMobile2MMoney = 0L;
        this.vietNamMobile2MQuantity = 0;
        this.vietNamMobile1MMoney = 0L;
        this.vietNamMobile1MQuantity = 0;
        this.vietNamMobile500KMoney = 0L;
        this.vietNamMobile500KQuantity = 0;
        this.vietNamMobile300KMoney = 0L;
        this.vietNamMobile300KQuantity = 0;
        this.vietNamMobile200KMoney = 0L;
        this.vietNamMobile200KQuantity = 0;
        this.vietNamMobile100KMoney = 0L;
        this.vietNamMobile100KQuantity = 0;
        this.vietNamMobile50KMoney = 0L;
        this.vietNamMobile50KQuantity = 0;
        this.vietNamMobile30KMoney = 0L;
        this.vietNamMobile30KQuantity = 0;
        this.vietNamMobile20KMoney = 0L;
        this.vietNamMobile20KQuantity = 0;
        this.vietNamMobile10KMoney = 0L;
        this.vietNamMobile10KQuantity = 0;
        this.beeLineMoney = 0L;
        this.beeLine5MMoney = 0L;
        this.beeLine5MQuantity = 0;
        this.beeLine2MMoney = 0L;
        this.beeLine2MQuantity = 0;
        this.beeLine1MMoney = 0L;
        this.beeLine1MQuantity = 0;
        this.beeLine500KMoney = 0L;
        this.beeLine500KQuantity = 0;
        this.beeLine300KMoney = 0L;
        this.beeLine300KQuantity = 0;
        this.beeLine200KMoney = 0L;
        this.beeLine200KQuantity = 0;
        this.beeLine100KMoney = 0L;
        this.beeLine100KQuantity = 0;
        this.beeLine50KMoney = 0L;
        this.beeLine50KQuantity = 0;
        this.beeLine30KMoney = 0L;
        this.beeLine30KQuantity = 0;
        this.beeLine20KMoney = 0L;
        this.beeLine20KQuantity = 0;
        this.beeLine10KMoney = 0L;
        this.beeLine10KQuantity = 0;
        objsort.put("_id", -1);
        if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        if (!partner.isEmpty()) {
            conditions.put("partner", partner);
        }
        if (!code.isEmpty()) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (!type.isEmpty()) {
            conditions.put("type", Integer.parseInt(type));
        }
        FindIterable iterable = null;
        iterable = db.getCollection("dvt_cash_out_by_topup").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                CashOutByTopUpDAOImpl cashOutByTopUpDAOImpl = CashOutByTopUpDAOImpl.this;
                cashOutByTopUpDAOImpl.totalMoney = cashOutByTopUpDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
                block7 : switch (CashOutByTopUpDAOImpl.mapProvider(document.getString("provider"))) {
                    case "Viettel": {
                        CashOutByTopUpDAOImpl cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                        cashOutByTopUpDAOImpl2.viettelMoney = cashOutByTopUpDAOImpl2.viettelMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                CashOutByTopUpDAOImpl.this.viettel5MQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel5MMoney = cashOutByTopUpDAOImpl2.viettel5MMoney + 5000000L;
                                break block7;
                            }
                            case 2000000: {
                                CashOutByTopUpDAOImpl.this.viettel2MQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel2MMoney = cashOutByTopUpDAOImpl2.viettel2MMoney + 2000000L;
                                break block7;
                            }
                            case 1000000: {
                                CashOutByTopUpDAOImpl.this.viettel1MQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel1MMoney = cashOutByTopUpDAOImpl2.viettel1MMoney + 1000000L;
                                break block7;
                            }
                            case 500000: {
                                CashOutByTopUpDAOImpl.this.viettel500KQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel500KMoney = cashOutByTopUpDAOImpl2.viettel500KMoney + 500000L;
                                break block7;
                            }
                            case 300000: {
                                CashOutByTopUpDAOImpl.this.viettel300KQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel300KMoney = cashOutByTopUpDAOImpl2.viettel300KMoney + 300000L;
                                break block7;
                            }
                            case 200000: {
                                CashOutByTopUpDAOImpl.this.viettel200KQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel200KMoney = cashOutByTopUpDAOImpl2.viettel200KMoney + 200000L;
                                break block7;
                            }
                            case 100000: {
                                CashOutByTopUpDAOImpl.this.viettel100KQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel100KMoney = cashOutByTopUpDAOImpl2.viettel100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                CashOutByTopUpDAOImpl.this.viettel50KQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel50KMoney = cashOutByTopUpDAOImpl2.viettel50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                CashOutByTopUpDAOImpl.this.viettel30KQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel30KMoney = cashOutByTopUpDAOImpl2.viettel30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                CashOutByTopUpDAOImpl.this.viettel20KQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel20KMoney = cashOutByTopUpDAOImpl2.viettel20KMoney + 20000L;
                                break block7;
                            }
                            case 10000: {
                                CashOutByTopUpDAOImpl.this.viettel10KQuantity++;
                                cashOutByTopUpDAOImpl2 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl2.viettel10KMoney = cashOutByTopUpDAOImpl2.viettel10KMoney + 10000L;
                                break block7;
                            }
                        }
                        break;
                    }
                    case "Vinaphone": {
                        CashOutByTopUpDAOImpl cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                        cashOutByTopUpDAOImpl3.vinaphoneMoney = cashOutByTopUpDAOImpl3.vinaphoneMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                CashOutByTopUpDAOImpl.this.vinaphone5MQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone5MMoney = cashOutByTopUpDAOImpl3.vinaphone5MMoney + 5000000L;
                                break block7;
                            }
                            case 2000000: {
                                CashOutByTopUpDAOImpl.this.vinaphone2MQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone2MMoney = cashOutByTopUpDAOImpl3.vinaphone2MMoney + 2000000L;
                                break block7;
                            }
                            case 1000000: {
                                CashOutByTopUpDAOImpl.this.vinaphone1MQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone1MMoney = cashOutByTopUpDAOImpl3.vinaphone1MMoney + 1000000L;
                                break block7;
                            }
                            case 500000: {
                                CashOutByTopUpDAOImpl.this.vinaphone500KQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone500KMoney = cashOutByTopUpDAOImpl3.vinaphone500KMoney + 500000L;
                                break block7;
                            }
                            case 300000: {
                                CashOutByTopUpDAOImpl.this.vinaphone300KQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone300KMoney = cashOutByTopUpDAOImpl3.vinaphone300KMoney + 300000L;
                                break block7;
                            }
                            case 200000: {
                                CashOutByTopUpDAOImpl.this.vinaphone200KQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone200KMoney = cashOutByTopUpDAOImpl3.vinaphone200KMoney + 200000L;
                                break block7;
                            }
                            case 100000: {
                                CashOutByTopUpDAOImpl.this.vinaphone100KQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone100KMoney = cashOutByTopUpDAOImpl3.vinaphone100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                CashOutByTopUpDAOImpl.this.vinaphone50KQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone50KMoney = cashOutByTopUpDAOImpl3.vinaphone50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                CashOutByTopUpDAOImpl.this.vinaphone30KQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone30KMoney = cashOutByTopUpDAOImpl3.vinaphone30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                CashOutByTopUpDAOImpl.this.vinaphone20KQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone20KMoney = cashOutByTopUpDAOImpl3.vinaphone20KMoney + 20000L;
                                break block7;
                            }
                            case 10000: {
                                CashOutByTopUpDAOImpl.this.vinaphone10KQuantity++;
                                cashOutByTopUpDAOImpl3 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl3.vinaphone10KMoney = cashOutByTopUpDAOImpl3.vinaphone10KMoney + 10000L;
                                break block7;
                            }
                        }
                        break;
                    }
                    case "Mobifone": {
                        CashOutByTopUpDAOImpl cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                        cashOutByTopUpDAOImpl4.mobifoneMoney = cashOutByTopUpDAOImpl4.mobifoneMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                CashOutByTopUpDAOImpl.this.mobifone5MQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone5MMoney = cashOutByTopUpDAOImpl4.mobifone5MMoney + 5000000L;
                                break block7;
                            }
                            case 2000000: {
                                CashOutByTopUpDAOImpl.this.mobifone2MQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone2MMoney = cashOutByTopUpDAOImpl4.mobifone2MMoney + 2000000L;
                                break block7;
                            }
                            case 1000000: {
                                CashOutByTopUpDAOImpl.this.mobifone1MQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone1MMoney = cashOutByTopUpDAOImpl4.mobifone1MMoney + 1000000L;
                                break block7;
                            }
                            case 500000: {
                                CashOutByTopUpDAOImpl.this.mobifone500KQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone500KMoney = cashOutByTopUpDAOImpl4.mobifone500KMoney + 500000L;
                                break block7;
                            }
                            case 300000: {
                                CashOutByTopUpDAOImpl.this.mobifone300KQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone300KMoney = cashOutByTopUpDAOImpl4.mobifone300KMoney + 300000L;
                                break block7;
                            }
                            case 200000: {
                                CashOutByTopUpDAOImpl.this.mobifone200KQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone200KMoney = cashOutByTopUpDAOImpl4.mobifone200KMoney + 200000L;
                                break block7;
                            }
                            case 100000: {
                                CashOutByTopUpDAOImpl.this.mobifone100KQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone100KMoney = cashOutByTopUpDAOImpl4.mobifone100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                CashOutByTopUpDAOImpl.this.mobifone50KQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone50KMoney = cashOutByTopUpDAOImpl4.mobifone50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                CashOutByTopUpDAOImpl.this.mobifone30KQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone30KMoney = cashOutByTopUpDAOImpl4.mobifone30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                CashOutByTopUpDAOImpl.this.mobifone20KQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone20KMoney = cashOutByTopUpDAOImpl4.mobifone20KMoney + 20000L;
                                break block7;
                            }
                            case 10000: {
                                CashOutByTopUpDAOImpl.this.mobifone10KQuantity++;
                                cashOutByTopUpDAOImpl4 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl4.mobifone10KMoney = cashOutByTopUpDAOImpl4.mobifone10KMoney + 10000L;
                                break block7;
                            }
                        }
                        break;
                    }
                    case "VietNamMobile": {
                        CashOutByTopUpDAOImpl cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                        cashOutByTopUpDAOImpl5.vietNamMobileMoney = cashOutByTopUpDAOImpl5.vietNamMobileMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile5MQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile5MMoney = cashOutByTopUpDAOImpl5.vietNamMobile5MMoney + 5000000L;
                                break block7;
                            }
                            case 2000000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile2MQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile2MMoney = cashOutByTopUpDAOImpl5.vietNamMobile2MMoney + 2000000L;
                                break block7;
                            }
                            case 1000000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile1MQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile1MMoney = cashOutByTopUpDAOImpl5.vietNamMobile1MMoney + 1000000L;
                                break block7;
                            }
                            case 500000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile500KQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile500KMoney = cashOutByTopUpDAOImpl5.vietNamMobile500KMoney + 500000L;
                                break block7;
                            }
                            case 300000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile300KQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile300KMoney = cashOutByTopUpDAOImpl5.vietNamMobile300KMoney + 300000L;
                                break block7;
                            }
                            case 200000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile200KQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile200KMoney = cashOutByTopUpDAOImpl5.vietNamMobile200KMoney + 200000L;
                                break block7;
                            }
                            case 100000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile100KQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile100KMoney = cashOutByTopUpDAOImpl5.vietNamMobile100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile50KQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile50KMoney = cashOutByTopUpDAOImpl5.vietNamMobile50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile30KQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile30KMoney = cashOutByTopUpDAOImpl5.vietNamMobile30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile20KQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile20KMoney = cashOutByTopUpDAOImpl5.vietNamMobile20KMoney + 20000L;
                                break block7;
                            }
                            case 10000: {
                                CashOutByTopUpDAOImpl.this.vietNamMobile10KQuantity++;
                                cashOutByTopUpDAOImpl5 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl5.vietNamMobile10KMoney = cashOutByTopUpDAOImpl5.vietNamMobile10KMoney + 10000L;
                                break block7;
                            }
                        }
                        break;
                    }
                    case "GMobile": {
                        CashOutByTopUpDAOImpl cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                        cashOutByTopUpDAOImpl6.beeLineMoney = cashOutByTopUpDAOImpl6.beeLineMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 5000000: {
                                CashOutByTopUpDAOImpl.this.beeLine5MQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine5MMoney = cashOutByTopUpDAOImpl6.beeLine5MMoney + 5000000L;
                                break block7;
                            }
                            case 2000000: {
                                CashOutByTopUpDAOImpl.this.beeLine2MQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine2MMoney = cashOutByTopUpDAOImpl6.beeLine2MMoney + 2000000L;
                                break block7;
                            }
                            case 1000000: {
                                CashOutByTopUpDAOImpl.this.beeLine1MQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine1MMoney = cashOutByTopUpDAOImpl6.beeLine1MMoney + 1000000L;
                                break block7;
                            }
                            case 500000: {
                                CashOutByTopUpDAOImpl.this.beeLine500KQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine500KMoney = cashOutByTopUpDAOImpl6.beeLine500KMoney + 500000L;
                                break block7;
                            }
                            case 300000: {
                                CashOutByTopUpDAOImpl.this.beeLine300KQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine300KMoney = cashOutByTopUpDAOImpl6.beeLine300KMoney + 300000L;
                                break block7;
                            }
                            case 200000: {
                                CashOutByTopUpDAOImpl.this.beeLine200KQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine200KMoney = cashOutByTopUpDAOImpl6.beeLine200KMoney + 200000L;
                                break block7;
                            }
                            case 100000: {
                                CashOutByTopUpDAOImpl.this.beeLine100KQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine100KMoney = cashOutByTopUpDAOImpl6.beeLine100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                CashOutByTopUpDAOImpl.this.beeLine50KQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine50KMoney = cashOutByTopUpDAOImpl6.beeLine50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                CashOutByTopUpDAOImpl.this.beeLine30KQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine30KMoney = cashOutByTopUpDAOImpl6.beeLine30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                CashOutByTopUpDAOImpl.this.beeLine20KQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine20KMoney = cashOutByTopUpDAOImpl6.beeLine20KMoney + 20000L;
                                break block7;
                            }
                            case 10000: {
                                CashOutByTopUpDAOImpl.this.beeLine10KQuantity++;
                                cashOutByTopUpDAOImpl6 = CashOutByTopUpDAOImpl.this;
                                cashOutByTopUpDAOImpl6.beeLine10KMoney = cashOutByTopUpDAOImpl6.beeLine10KMoney + 10000L;
                                break block7;
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
        MoneyTotalRechargeByCardReponse vietNamMobile = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listVietNamMobileMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue vietNamMobileMoney5MFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney5MFollowFace.setFaceValue(5000000);
        vietNamMobileMoney5MFollowFace.setQuantity(this.vietNamMobile5MQuantity);
        vietNamMobileMoney5MFollowFace.setMoneyTotal(this.vietNamMobile5MMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney5MFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney2MFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney2MFollowFace.setFaceValue(2000000);
        vietNamMobileMoney2MFollowFace.setQuantity(this.vietNamMobile2MQuantity);
        vietNamMobileMoney2MFollowFace.setMoneyTotal(this.vietNamMobile2MMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney2MFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney1MFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney1MFollowFace.setFaceValue(1000000);
        vietNamMobileMoney1MFollowFace.setQuantity(this.vietNamMobile1MQuantity);
        vietNamMobileMoney1MFollowFace.setMoneyTotal(this.vietNamMobile1MMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney1MFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney500KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney500KFollowFace.setFaceValue(500000);
        vietNamMobileMoney500KFollowFace.setQuantity(this.vietNamMobile500KQuantity);
        vietNamMobileMoney500KFollowFace.setMoneyTotal(this.vietNamMobile500KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney500KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney300KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney300KFollowFace.setFaceValue(300000);
        vietNamMobileMoney300KFollowFace.setQuantity(this.vietNamMobile300KQuantity);
        vietNamMobileMoney300KFollowFace.setMoneyTotal(this.vietNamMobile300KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney300KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney200KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney200KFollowFace.setFaceValue(200000);
        vietNamMobileMoney200KFollowFace.setQuantity(this.vietNamMobile200KQuantity);
        vietNamMobileMoney200KFollowFace.setMoneyTotal(this.vietNamMobile200KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney200KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney100KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney100KFollowFace.setFaceValue(100000);
        vietNamMobileMoney100KFollowFace.setQuantity(this.vietNamMobile100KQuantity);
        vietNamMobileMoney100KFollowFace.setMoneyTotal(this.vietNamMobile100KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney100KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney50KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney50KFollowFace.setFaceValue(50000);
        vietNamMobileMoney50KFollowFace.setQuantity(this.vietNamMobile50KQuantity);
        vietNamMobileMoney50KFollowFace.setMoneyTotal(this.vietNamMobile50KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney50KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney30KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney30KFollowFace.setFaceValue(30000);
        vietNamMobileMoney30KFollowFace.setQuantity(this.vietNamMobile30KQuantity);
        vietNamMobileMoney30KFollowFace.setMoneyTotal(this.vietNamMobile30KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney30KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney20KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney20KFollowFace.setFaceValue(20000);
        vietNamMobileMoney20KFollowFace.setQuantity(this.vietNamMobile20KQuantity);
        vietNamMobileMoney20KFollowFace.setMoneyTotal(this.vietNamMobile20KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney20KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney10KFollowFace.setFaceValue(10000);
        vietNamMobileMoney10KFollowFace.setQuantity(this.vietNamMobile10KQuantity);
        vietNamMobileMoney10KFollowFace.setMoneyTotal(this.vietNamMobile10KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney10KFollowFace);
        vietNamMobile.setName("VietNamMobile");
        vietNamMobile.setValue(this.vietNamMobileMoney);
        vietNamMobile.setTrans(listVietNamMobileMoneyFollowFace);
        listReponse.add(vietNamMobile);
        MoneyTotalRechargeByCardReponse beeLine = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listBeeLineMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue beeLineMoney5MFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney5MFollowFace.setFaceValue(5000000);
        beeLineMoney5MFollowFace.setQuantity(this.beeLine5MQuantity);
        beeLineMoney5MFollowFace.setMoneyTotal(this.beeLine5MMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney5MFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney2MFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney2MFollowFace.setFaceValue(2000000);
        beeLineMoney2MFollowFace.setQuantity(this.beeLine2MQuantity);
        beeLineMoney2MFollowFace.setMoneyTotal(this.beeLine2MMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney2MFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney1MFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney1MFollowFace.setFaceValue(1000000);
        beeLineMoney1MFollowFace.setQuantity(this.beeLine1MQuantity);
        beeLineMoney1MFollowFace.setMoneyTotal(this.beeLine1MMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney1MFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney500KFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney500KFollowFace.setFaceValue(500000);
        beeLineMoney500KFollowFace.setQuantity(this.beeLine500KQuantity);
        beeLineMoney500KFollowFace.setMoneyTotal(this.beeLine500KMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney500KFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney300KFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney300KFollowFace.setFaceValue(300000);
        beeLineMoney300KFollowFace.setQuantity(this.beeLine300KQuantity);
        beeLineMoney300KFollowFace.setMoneyTotal(this.beeLine300KMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney300KFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney200KFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney200KFollowFace.setFaceValue(200000);
        beeLineMoney200KFollowFace.setQuantity(this.beeLine200KQuantity);
        beeLineMoney200KFollowFace.setMoneyTotal(this.beeLine200KMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney200KFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney100KFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney100KFollowFace.setFaceValue(100000);
        beeLineMoney100KFollowFace.setQuantity(this.beeLine100KQuantity);
        beeLineMoney100KFollowFace.setMoneyTotal(this.beeLine100KMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney100KFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney50KFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney50KFollowFace.setFaceValue(50000);
        beeLineMoney50KFollowFace.setQuantity(this.beeLine50KQuantity);
        beeLineMoney50KFollowFace.setMoneyTotal(this.beeLine50KMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney50KFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney30KFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney30KFollowFace.setFaceValue(30000);
        beeLineMoney30KFollowFace.setQuantity(this.beeLine30KQuantity);
        beeLineMoney30KFollowFace.setMoneyTotal(this.beeLine30KMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney30KFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney20KFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney20KFollowFace.setFaceValue(20000);
        beeLineMoney20KFollowFace.setQuantity(this.beeLine20KQuantity);
        beeLineMoney20KFollowFace.setMoneyTotal(this.beeLine20KMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney20KFollowFace);
        MoneyTotalFollowFaceValue beeLineMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        beeLineMoney10KFollowFace.setFaceValue(10000);
        beeLineMoney10KFollowFace.setQuantity(this.beeLine10KQuantity);
        beeLineMoney10KFollowFace.setMoneyTotal(this.beeLine10KMoney);
        listBeeLineMoneyFollowFace.add(beeLineMoney10KFollowFace);
        beeLine.setName("GMobile");
        beeLine.setValue(this.beeLineMoney);
        beeLine.setTrans(listBeeLineMoneyFollowFace);
        listReponse.add(beeLine);
        return listReponse;
    }

    @Override
    public List<CashOutByTopUpResponse> searchCashOutByTopUp(String nickName, String target, String status, String code, String timeStart, String timeEnd, int page, String transId, String partner, String type) {
        final ArrayList<CashOutByTopUpResponse> results = new ArrayList<CashOutByTopUpResponse>();
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
        if (target != null && !target.equals("")) {
            conditions.put("target", target);
        }
        if (partner != null && !partner.equals("")) {
            conditions.put("partner", partner);
        }
        if (status != null && !status.equals("")) {
            conditions.put("status", Integer.parseInt(status));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", Integer.parseInt(type));
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("dvt_cash_out_by_topup").find((Bson)new Document((Map)conditions)).sort((Bson)objsort).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                CashOutByTopUpResponse bank = new CashOutByTopUpResponse();
                bank.referenceId = document.getString("reference_id");
                bank.nickName = document.getString("nick_name");
                bank.target = document.getString("target");
                bank.amount = document.getInteger("amount");
                bank.status = document.getInteger("status");
                bank.message = document.getString("message");
                bank.sign = document.getString("sign");
                bank.code = document.getInteger("code");
                bank.timeLog = document.getString("time_log");
                bank.updateTime = document.getString("update_time");
                bank.partner = document.getString("partner");
                bank.provider = CashOutByTopUpDAOImpl.mapProvider(document.getString("provider"));
                bank.type = document.getInteger("type", -1);
                results.add(bank);
            }
        });
        return results;
    }

    private static String mapProvider(String provider) {
        if (provider == null) {
            return "";
        }
        switch (provider) {
            case "vtt": {
                return "Viettel";
            }
            case "vnp": {
                return "Vinaphone";
            }
            case "vms": {
                return "Mobifone";
            }
            case "vnm": {
                return "VietNamMobile";
            }
            case "bee": {
                return "BeeLine";
            }
            case "gate": {
                return "Gate";
            }
            case "zing": {
                return "Zing";
            }
            case "vcoin": {
                return "Vcoin";
            }
            case "sfone": {
                return "SFone";
            }
            case "gtel": {
                return "GMobile";
            }
            case "garena": {
                return "Garena";
            }
        }
        return provider;
    }

    private static String mapProviderReverse(String provider) {
        if (provider == null) {
            return "";
        }
        switch (provider) {
            case "Viettel": {
                return "vtt";
            }
            case "Vinaphone": {
                return "vnp";
            }
            case "Mobifone": {
                return "vms";
            }
            case "VietNamMobile": {
                return "vnm";
            }
            case "BeeLine": {
                return "bee";
            }
            case "Gate": {
                return "gate";
            }
            case "Zing": {
                return "zing";
            }
            case "Vcoin": {
                return "vcoin";
            }
            case "SFone": {
                return "sfone";
            }
            case "GMobile": {
                return "gtel";
            }
            case "Garena": {
                return "garena";
            }
        }
        return provider;
    }

    @Override
    public int countSearchCashOutByTopUp(String nickName, String target, String status, String code, String timeStart, String timeEnd, String transId, String partner, String type) {
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
        if (target != null && !target.equals("")) {
            conditions.put("target", target);
        }
        if (partner != null && !partner.equals("")) {
            conditions.put("partner", partner);
        }
        if (status != null && !status.equals("")) {
            conditions.put("status", Integer.parseInt(status));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", Integer.parseInt(type));
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        int record = (int)db.getCollection("dvt_cash_out_by_topup").count((Bson)new Document((Map)conditions));
        return record;
    }

    @Override
    public long moneyTotal(String nickName, String target, String status, String code, String timeStart, String timeEnd, String transId, String partner, String type) {
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
        if (target != null && !target.equals("")) {
            conditions.put("target", target);
        }
        if (partner != null && !partner.equals("")) {
            conditions.put("partner", partner);
        }
        if (status != null && !status.equals("")) {
            conditions.put("status", Integer.parseInt(status));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", Integer.parseInt(type));
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
        iterable = db.getCollection("dvt_cash_out_by_topup").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                CashOutByTopUpDAOImpl cashOutByTopUpDAOImpl = CashOutByTopUpDAOImpl.this;
                cashOutByTopUpDAOImpl.totalMoney = cashOutByTopUpDAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
            }
        });
        return this.totalMoney;
    }

    @Override
    public List<CashOutByTopUpResponse> exportDataCashOutByTopup(String provider, String code, String timeStart, String timeEnd, String partner, String amount, String type) {
        final ArrayList<CashOutByTopUpResponse> results = new ArrayList<CashOutByTopUpResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
        if (provider != null && !provider.equals("")) {
            conditions.put("provider", CashOutByTopUpDAOImpl.mapProviderReverse(provider));
        }
        if (partner != null && !partner.equals("")) {
            conditions.put("partner", partner);
        }
        if (amount != null && !amount.equals("")) {
            conditions.put("amount", Integer.parseInt(amount));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", Integer.parseInt(type));
        }
        if (code != null && !code.equals("")) {
            conditions.put("code", Integer.parseInt(code));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("dvt_cash_out_by_topup").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                CashOutByTopUpResponse bank = new CashOutByTopUpResponse();
                bank.referenceId = document.getString("reference_id");
                bank.nickName = document.getString("nick_name");
                bank.target = document.getString("target");
                bank.amount = document.getInteger("amount");
                bank.status = document.getInteger("status");
                bank.message = document.getString("message");
                bank.sign = document.getString("sign");
                bank.code = document.getInteger("code");
                bank.timeLog = document.getString("time_log");
                bank.updateTime = document.getString("update_time");
                bank.partner = document.getString("partner");
                bank.provider = CashOutByTopUpDAOImpl.mapProvider(document.getString("provider"));
                bank.type = document.getInteger("type", -1);
                results.add(bank);
            }
        });
        return results;
    }

}

