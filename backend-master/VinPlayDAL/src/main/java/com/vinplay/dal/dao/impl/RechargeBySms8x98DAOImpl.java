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
 *  com.vinplay.vbee.common.response.RechargeBySmsResponse
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.RechargeBySms8x98DAO;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.RechargeBySmsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class RechargeBySms8x98DAOImpl
implements RechargeBySms8x98DAO {
    private long totalMoney = 0L;
    private long viettelMoney = 0L;
    private long viettel100KMoney = 0L;
    private int viettel100KQuantity = 0;
    private long viettel50KMoney = 0L;
    private int viettel50KQuantity = 0;
    private long viettel30KMoney = 0L;
    private int viettel30KQuantity = 0;
    private long viettel20KMoney = 0L;
    private int viettel20KQuantity = 0;
    private long viettel15KMoney = 0L;
    private int viettel15KQuantity = 0;
    private long viettel10KMoney = 0L;
    private int viettel10KQuantity = 0;
    private long viettel5KMoney = 0L;
    private int viettel5KQuantity = 0;
    private long viettel4KMoney = 0L;
    private int viettel4KQuantity = 0;
    private long viettel3KMoney = 0L;
    private int viettel3KQuantity = 0;
    private long viettel2KMoney = 0L;
    private int viettel2KQuantity = 0;
    private long viettel1KMoney = 0L;
    private int viettel1KQuantity = 0;
    private long vinaphoneMoney = 0L;
    private long vinaphone100KMoney = 0L;
    private int vinaphone100KQuantity = 0;
    private long vinaphone50KMoney = 0L;
    private int vinaphone50KQuantity = 0;
    private long vinaphone30KMoney = 0L;
    private int vinaphone30KQuantity = 0;
    private long vinaphone20KMoney = 0L;
    private int vinaphone20KQuantity = 0;
    private long vinaphone15KMoney = 0L;
    private int vinaphone15KQuantity = 0;
    private long vinaphone10KMoney = 0L;
    private int vinaphone10KQuantity = 0;
    private long vinaphone5KMoney = 0L;
    private int vinaphone5KQuantity = 0;
    private long vinaphone4KMoney = 0L;
    private int vinaphone4KQuantity = 0;
    private long vinaphone3KMoney = 0L;
    private int vinaphone3KQuantity = 0;
    private long vinaphone2KMoney = 0L;
    private int vinaphone2KQuantity = 0;
    private long vinaphone1KMoney = 0L;
    private int vinaphone1KQuantity = 0;
    private long mobifoneMoney = 0L;
    private long mobifone100KMoney = 0L;
    private int mobifone100KQuantity = 0;
    private long mobifone50KMoney = 0L;
    private int mobifone50KQuantity = 0;
    private long mobifone30KMoney = 0L;
    private int mobifone30KQuantity = 0;
    private long mobifone20KMoney = 0L;
    private int mobifone20KQuantity = 0;
    private long mobifone15KMoney = 0L;
    private int mobifone15KQuantity = 0;
    private long mobifone10KMoney = 0L;
    private int mobifone10KQuantity = 0;
    private long mobifone5KMoney = 0L;
    private int mobifone5KQuantity = 0;
    private long mobifone4KMoney = 0L;
    private int mobifone4KQuantity = 0;
    private long mobifone3KMoney = 0L;
    private int mobifone3KQuantity = 0;
    private long mobifone2KMoney = 0L;
    private int mobifone2KQuantity = 0;
    private long mobifone1KMoney = 0L;
    private int mobifone1KQuantity = 0;
    private long vietNamMobileMoney = 0L;
    private long vietNamMobile100KMoney = 0L;
    private int vietNamMobile100KQuantity = 0;
    private long vietNamMobile50KMoney = 0L;
    private int vietNamMobile50KQuantity = 0;
    private long vietNamMobile30KMoney = 0L;
    private int vietNamMobile30KQuantity = 0;
    private long vietNamMobile20KMoney = 0L;
    private int vietNamMobile20KQuantity = 0;
    private long vietNamMobile15KMoney = 0L;
    private int vietNamMobile15KQuantity = 0;
    private long vietNamMobile10KMoney = 0L;
    private int vietNamMobile10KQuantity = 0;
    private long vietNamMobile5KMoney = 0L;
    private int vietNamMobile5KQuantity = 0;
    private long vietNamMobile4KMoney = 0L;
    private int vietNamMobile4KQuantity = 0;
    private long vietNamMobile3KMoney = 0L;
    private int vietNamMobile3KQuantity = 0;
    private long vietNamMobile2KMoney = 0L;
    private int vietNamMobile2KQuantity = 0;
    private long vietNamMobile1KMoney = 0L;
    private int vietNamMobile1KQuantity = 0;
    private long gMobileMoney = 0L;
    private long gMobile100KMoney = 0L;
    private int gMobile100KQuantity = 0;
    private long gMobile50KMoney = 0L;
    private int gMobile50KQuantity = 0;
    private long gMobile30KMoney = 0L;
    private int gMobile30KQuantity = 0;
    private long gMobile20KMoney = 0L;
    private int gMobile20KQuantity = 0;
    private long gMobile15KMoney = 0L;
    private int gMobile15KQuantity = 0;
    private long gMobile10KMoney = 0L;
    private int gMobile10KQuantity = 0;
    private long gMobile5KMoney = 0L;
    private int gMobile5KQuantity = 0;
    private long gMobile4KMoney = 0L;
    private int gMobile4KQuantity = 0;
    private long gMobile3KMoney = 0L;
    private int gMobile3KQuantity = 0;
    private long gMobile2KMoney = 0L;
    private int gMobile2KQuantity = 0;
    private long gMobile1KMoney = 0L;
    private int gMobile1KQuantity = 0;

    @Override
    public List<MoneyTotalRechargeByCardReponse> moneyTotalRechargeBySms8x98(String timeStart, String timeEnd, String code) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        this.totalMoney = 0L;
        this.viettelMoney = 0L;
        this.viettel100KMoney = 0L;
        this.viettel100KQuantity = 0;
        this.viettel50KMoney = 0L;
        this.viettel50KQuantity = 0;
        this.viettel30KMoney = 0L;
        this.viettel30KQuantity = 0;
        this.viettel20KMoney = 0L;
        this.viettel20KQuantity = 0;
        this.viettel15KMoney = 0L;
        this.viettel15KQuantity = 0;
        this.viettel10KMoney = 0L;
        this.viettel10KQuantity = 0;
        this.viettel5KMoney = 0L;
        this.viettel5KQuantity = 0;
        this.viettel4KMoney = 0L;
        this.viettel4KQuantity = 0;
        this.viettel3KMoney = 0L;
        this.viettel3KQuantity = 0;
        this.viettel2KMoney = 0L;
        this.viettel2KQuantity = 0;
        this.viettel1KMoney = 0L;
        this.viettel1KQuantity = 0;
        this.vinaphoneMoney = 0L;
        this.vinaphone100KMoney = 0L;
        this.vinaphone100KQuantity = 0;
        this.vinaphone50KMoney = 0L;
        this.vinaphone50KQuantity = 0;
        this.vinaphone30KMoney = 0L;
        this.vinaphone30KQuantity = 0;
        this.vinaphone20KMoney = 0L;
        this.vinaphone20KQuantity = 0;
        this.vinaphone15KMoney = 0L;
        this.vinaphone15KQuantity = 0;
        this.vinaphone10KMoney = 0L;
        this.vinaphone10KQuantity = 0;
        this.vinaphone5KMoney = 0L;
        this.vinaphone5KQuantity = 0;
        this.vinaphone4KMoney = 0L;
        this.vinaphone4KQuantity = 0;
        this.vinaphone3KMoney = 0L;
        this.vinaphone3KQuantity = 0;
        this.vinaphone2KMoney = 0L;
        this.vinaphone2KQuantity = 0;
        this.vinaphone1KMoney = 0L;
        this.vinaphone1KQuantity = 0;
        this.mobifoneMoney = 0L;
        this.mobifone100KMoney = 0L;
        this.mobifone100KQuantity = 0;
        this.mobifone50KMoney = 0L;
        this.mobifone50KQuantity = 0;
        this.mobifone30KMoney = 0L;
        this.mobifone30KQuantity = 0;
        this.mobifone20KMoney = 0L;
        this.mobifone20KQuantity = 0;
        this.mobifone15KMoney = 0L;
        this.mobifone15KQuantity = 0;
        this.mobifone10KMoney = 0L;
        this.mobifone10KQuantity = 0;
        this.mobifone5KMoney = 0L;
        this.mobifone5KQuantity = 0;
        this.mobifone4KMoney = 0L;
        this.mobifone4KQuantity = 0;
        this.mobifone3KMoney = 0L;
        this.mobifone3KQuantity = 0;
        this.mobifone2KMoney = 0L;
        this.mobifone2KQuantity = 0;
        this.mobifone1KMoney = 0L;
        this.mobifone1KQuantity = 0;
        this.vietNamMobileMoney = 0L;
        this.vietNamMobile100KMoney = 0L;
        this.vietNamMobile100KQuantity = 0;
        this.vietNamMobile50KMoney = 0L;
        this.vietNamMobile50KQuantity = 0;
        this.vietNamMobile30KMoney = 0L;
        this.vietNamMobile30KQuantity = 0;
        this.vietNamMobile20KMoney = 0L;
        this.vietNamMobile20KQuantity = 0;
        this.vietNamMobile15KMoney = 0L;
        this.vietNamMobile15KQuantity = 0;
        this.vietNamMobile10KMoney = 0L;
        this.vietNamMobile10KQuantity = 0;
        this.vietNamMobile5KMoney = 0L;
        this.vietNamMobile5KQuantity = 0;
        this.vietNamMobile4KMoney = 0L;
        this.vietNamMobile4KQuantity = 0;
        this.vietNamMobile3KMoney = 0L;
        this.vietNamMobile3KQuantity = 0;
        this.vietNamMobile2KMoney = 0L;
        this.vietNamMobile2KQuantity = 0;
        this.vietNamMobile1KMoney = 0L;
        this.vietNamMobile1KQuantity = 0;
        this.gMobileMoney = 0L;
        this.gMobile100KMoney = 0L;
        this.gMobile100KQuantity = 0;
        this.gMobile50KMoney = 0L;
        this.gMobile50KQuantity = 0;
        this.gMobile30KMoney = 0L;
        this.gMobile30KQuantity = 0;
        this.gMobile20KMoney = 0L;
        this.gMobile20KQuantity = 0;
        this.gMobile15KMoney = 0L;
        this.gMobile15KQuantity = 0;
        this.gMobile10KMoney = 0L;
        this.gMobile10KQuantity = 0;
        this.gMobile5KMoney = 0L;
        this.gMobile5KQuantity = 0;
        this.gMobile4KMoney = 0L;
        this.gMobile4KQuantity = 0;
        this.gMobile3KMoney = 0L;
        this.gMobile3KQuantity = 0;
        this.gMobile2KMoney = 0L;
        this.gMobile2KQuantity = 0;
        this.gMobile1KMoney = 0L;
        this.gMobile1KQuantity = 0;
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
        iterable = db.getCollection("dvt_recharge_by_sms").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeBySms8x98DAOImpl rechargeBySms8x98DAOImpl = RechargeBySms8x98DAOImpl.this;
                rechargeBySms8x98DAOImpl.totalMoney = rechargeBySms8x98DAOImpl.totalMoney + (long)document.getInteger("amount").intValue();
                block7 : switch (RechargeBySms8x98DAOImpl.mapProviderByTarget(document.getString("mobile"))) {
                    case "Viettel": {
                        RechargeBySms8x98DAOImpl rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                        rechargeBySms8x98DAOImpl2.viettelMoney = rechargeBySms8x98DAOImpl2.viettelMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 100000: {
                                RechargeBySms8x98DAOImpl.this.viettel100KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel100KMoney = rechargeBySms8x98DAOImpl2.viettel100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                RechargeBySms8x98DAOImpl.this.viettel50KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel50KMoney = rechargeBySms8x98DAOImpl2.viettel50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                RechargeBySms8x98DAOImpl.this.viettel30KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel30KMoney = rechargeBySms8x98DAOImpl2.viettel30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                RechargeBySms8x98DAOImpl.this.viettel20KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel20KMoney = rechargeBySms8x98DAOImpl2.viettel20KMoney + 20000L;
                                break block7;
                            }
                            case 15000: {
                                RechargeBySms8x98DAOImpl.this.viettel15KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel15KMoney = rechargeBySms8x98DAOImpl2.viettel15KMoney + 15000L;
                                break block7;
                            }
                            case 10000: {
                                RechargeBySms8x98DAOImpl.this.viettel10KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel10KMoney = rechargeBySms8x98DAOImpl2.viettel10KMoney + 10000L;
                                break block7;
                            }
                            case 5000: {
                                RechargeBySms8x98DAOImpl.this.viettel5KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel5KMoney = rechargeBySms8x98DAOImpl2.viettel5KMoney + 5000L;
                                break block7;
                            }
                            case 4000: {
                                RechargeBySms8x98DAOImpl.this.viettel4KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel4KMoney = rechargeBySms8x98DAOImpl2.viettel4KMoney + 4000L;
                                break block7;
                            }
                            case 3000: {
                                RechargeBySms8x98DAOImpl.this.viettel3KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel3KMoney = rechargeBySms8x98DAOImpl2.viettel3KMoney + 3000L;
                                break block7;
                            }
                            case 2000: {
                                RechargeBySms8x98DAOImpl.this.viettel2KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel2KMoney = rechargeBySms8x98DAOImpl2.viettel2KMoney + 2000L;
                                break block7;
                            }
                            case 1000: {
                                RechargeBySms8x98DAOImpl.this.viettel1KQuantity++;
                                rechargeBySms8x98DAOImpl2 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl2.viettel1KMoney = rechargeBySms8x98DAOImpl2.viettel1KMoney + 1000L;
                                break block7;
                            }
                        }
                        break;
                    }
                    case "Vinaphone": {
                        RechargeBySms8x98DAOImpl rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                        rechargeBySms8x98DAOImpl3.vinaphoneMoney = rechargeBySms8x98DAOImpl3.vinaphoneMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 100000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone100KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone100KMoney = rechargeBySms8x98DAOImpl3.vinaphone100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone50KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone50KMoney = rechargeBySms8x98DAOImpl3.vinaphone50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone30KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone30KMoney = rechargeBySms8x98DAOImpl3.vinaphone30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone20KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone20KMoney = rechargeBySms8x98DAOImpl3.vinaphone20KMoney + 20000L;
                                break block7;
                            }
                            case 15000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone15KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone15KMoney = rechargeBySms8x98DAOImpl3.vinaphone15KMoney + 15000L;
                                break block7;
                            }
                            case 10000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone10KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone10KMoney = rechargeBySms8x98DAOImpl3.vinaphone10KMoney + 10000L;
                                break block7;
                            }
                            case 5000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone5KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone5KMoney = rechargeBySms8x98DAOImpl3.vinaphone5KMoney + 5000L;
                                break block7;
                            }
                            case 4000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone4KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone4KMoney = rechargeBySms8x98DAOImpl3.vinaphone4KMoney + 4000L;
                                break block7;
                            }
                            case 3000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone3KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone3KMoney = rechargeBySms8x98DAOImpl3.vinaphone3KMoney + 3000L;
                                break block7;
                            }
                            case 2000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone2KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone2KMoney = rechargeBySms8x98DAOImpl3.vinaphone2KMoney + 2000L;
                                break block7;
                            }
                            case 1000: {
                                RechargeBySms8x98DAOImpl.this.vinaphone1KQuantity++;
                                rechargeBySms8x98DAOImpl3 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl3.vinaphone1KMoney = rechargeBySms8x98DAOImpl3.vinaphone1KMoney + 1000L;
                                break block7;
                            }
                        }
                        break;
                    }
                    case "Mobifone": {
                        RechargeBySms8x98DAOImpl rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                        rechargeBySms8x98DAOImpl4.mobifoneMoney = rechargeBySms8x98DAOImpl4.mobifoneMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 100000: {
                                RechargeBySms8x98DAOImpl.this.mobifone100KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone100KMoney = rechargeBySms8x98DAOImpl4.mobifone100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                RechargeBySms8x98DAOImpl.this.mobifone50KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone50KMoney = rechargeBySms8x98DAOImpl4.mobifone50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                RechargeBySms8x98DAOImpl.this.mobifone30KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone30KMoney = rechargeBySms8x98DAOImpl4.mobifone30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                RechargeBySms8x98DAOImpl.this.mobifone20KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone20KMoney = rechargeBySms8x98DAOImpl4.mobifone20KMoney + 20000L;
                                break block7;
                            }
                            case 15000: {
                                RechargeBySms8x98DAOImpl.this.mobifone15KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone15KMoney = rechargeBySms8x98DAOImpl4.mobifone15KMoney + 15000L;
                                break block7;
                            }
                            case 10000: {
                                RechargeBySms8x98DAOImpl.this.mobifone10KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone10KMoney = rechargeBySms8x98DAOImpl4.mobifone10KMoney + 10000L;
                                break block7;
                            }
                            case 5000: {
                                RechargeBySms8x98DAOImpl.this.mobifone5KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone5KMoney = rechargeBySms8x98DAOImpl4.mobifone5KMoney + 5000L;
                                break block7;
                            }
                            case 4000: {
                                RechargeBySms8x98DAOImpl.this.mobifone4KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone4KMoney = rechargeBySms8x98DAOImpl4.mobifone4KMoney + 4000L;
                                break block7;
                            }
                            case 3000: {
                                RechargeBySms8x98DAOImpl.this.mobifone3KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone3KMoney = rechargeBySms8x98DAOImpl4.mobifone3KMoney + 3000L;
                                break block7;
                            }
                            case 2000: {
                                RechargeBySms8x98DAOImpl.this.mobifone2KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone2KMoney = rechargeBySms8x98DAOImpl4.mobifone2KMoney + 2000L;
                                break block7;
                            }
                            case 1000: {
                                RechargeBySms8x98DAOImpl.this.mobifone1KQuantity++;
                                rechargeBySms8x98DAOImpl4 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl4.mobifone1KMoney = rechargeBySms8x98DAOImpl4.mobifone1KMoney + 1000L;
                                break block7;
                            }
                        }
                        break;
                    }
                    case "VietNamMobile": {
                        RechargeBySms8x98DAOImpl rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                        rechargeBySms8x98DAOImpl5.vietNamMobileMoney = rechargeBySms8x98DAOImpl5.vietNamMobileMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 100000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile100KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile100KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile50KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile50KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile30KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile30KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile20KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile20KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile20KMoney + 20000L;
                                break block7;
                            }
                            case 15000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile15KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile15KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile15KMoney + 15000L;
                                break block7;
                            }
                            case 10000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile10KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile10KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile10KMoney + 10000L;
                                break block7;
                            }
                            case 5000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile5KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile5KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile5KMoney + 5000L;
                                break block7;
                            }
                            case 4000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile4KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile4KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile4KMoney + 4000L;
                                break block7;
                            }
                            case 3000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile3KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile3KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile3KMoney + 3000L;
                                break block7;
                            }
                            case 2000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile2KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile2KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile2KMoney + 2000L;
                                break block7;
                            }
                            case 1000: {
                                RechargeBySms8x98DAOImpl.this.vietNamMobile1KQuantity++;
                                rechargeBySms8x98DAOImpl5 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl5.vietNamMobile1KMoney = rechargeBySms8x98DAOImpl5.vietNamMobile1KMoney + 1000L;
                                break block7;
                            }
                        }
                        break;
                    }
                    case "GMobile": {
                        RechargeBySms8x98DAOImpl rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                        rechargeBySms8x98DAOImpl6.gMobileMoney = rechargeBySms8x98DAOImpl6.gMobileMoney + (long)document.getInteger("amount").intValue();
                        switch (document.getInteger("amount")) {
                            case 100000: {
                                RechargeBySms8x98DAOImpl.this.gMobile100KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile100KMoney = rechargeBySms8x98DAOImpl6.gMobile100KMoney + 100000L;
                                break block7;
                            }
                            case 50000: {
                                RechargeBySms8x98DAOImpl.this.gMobile50KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile50KMoney = rechargeBySms8x98DAOImpl6.gMobile50KMoney + 50000L;
                                break block7;
                            }
                            case 30000: {
                                RechargeBySms8x98DAOImpl.this.gMobile30KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile30KMoney = rechargeBySms8x98DAOImpl6.gMobile30KMoney + 30000L;
                                break block7;
                            }
                            case 20000: {
                                RechargeBySms8x98DAOImpl.this.gMobile20KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile20KMoney = rechargeBySms8x98DAOImpl6.gMobile20KMoney + 20000L;
                                break block7;
                            }
                            case 15000: {
                                RechargeBySms8x98DAOImpl.this.gMobile15KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile15KMoney = rechargeBySms8x98DAOImpl6.gMobile15KMoney + 15000L;
                                break block7;
                            }
                            case 10000: {
                                RechargeBySms8x98DAOImpl.this.gMobile10KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile10KMoney = rechargeBySms8x98DAOImpl6.gMobile10KMoney + 10000L;
                                break block7;
                            }
                            case 5000: {
                                RechargeBySms8x98DAOImpl.this.gMobile5KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile5KMoney = rechargeBySms8x98DAOImpl6.gMobile5KMoney + 5000L;
                                break block7;
                            }
                            case 4000: {
                                RechargeBySms8x98DAOImpl.this.gMobile4KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile4KMoney = rechargeBySms8x98DAOImpl6.gMobile4KMoney + 4000L;
                                break block7;
                            }
                            case 3000: {
                                RechargeBySms8x98DAOImpl.this.gMobile3KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile3KMoney = rechargeBySms8x98DAOImpl6.gMobile3KMoney + 3000L;
                                break block7;
                            }
                            case 2000: {
                                RechargeBySms8x98DAOImpl.this.gMobile2KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile2KMoney = rechargeBySms8x98DAOImpl6.gMobile2KMoney + 2000L;
                                break block7;
                            }
                            case 1000: {
                                RechargeBySms8x98DAOImpl.this.gMobile1KQuantity++;
                                rechargeBySms8x98DAOImpl6 = RechargeBySms8x98DAOImpl.this;
                                rechargeBySms8x98DAOImpl6.gMobile1KMoney = rechargeBySms8x98DAOImpl6.gMobile1KMoney + 1000L;
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
        MoneyTotalFollowFaceValue viettelMoney15KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney15KFollowFace.setFaceValue(15000);
        viettelMoney15KFollowFace.setQuantity(this.viettel15KQuantity);
        viettelMoney15KFollowFace.setMoneyTotal(this.viettel15KMoney);
        listViettelMoneyFollowFace.add(viettelMoney15KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney10KFollowFace.setFaceValue(10000);
        viettelMoney10KFollowFace.setQuantity(this.viettel10KQuantity);
        viettelMoney10KFollowFace.setMoneyTotal(this.viettel10KMoney);
        listViettelMoneyFollowFace.add(viettelMoney10KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney5KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney5KFollowFace.setFaceValue(5000);
        viettelMoney5KFollowFace.setQuantity(this.viettel5KQuantity);
        viettelMoney5KFollowFace.setMoneyTotal(this.viettel5KMoney);
        listViettelMoneyFollowFace.add(viettelMoney5KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney4KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney4KFollowFace.setFaceValue(4000);
        viettelMoney4KFollowFace.setQuantity(this.viettel4KQuantity);
        viettelMoney4KFollowFace.setMoneyTotal(this.viettel4KMoney);
        listViettelMoneyFollowFace.add(viettelMoney4KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney3KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney3KFollowFace.setFaceValue(3000);
        viettelMoney3KFollowFace.setQuantity(this.viettel3KQuantity);
        viettelMoney3KFollowFace.setMoneyTotal(this.viettel3KMoney);
        listViettelMoneyFollowFace.add(viettelMoney3KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney2KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney2KFollowFace.setFaceValue(2000);
        viettelMoney2KFollowFace.setQuantity(this.viettel2KQuantity);
        viettelMoney2KFollowFace.setMoneyTotal(this.viettel2KMoney);
        listViettelMoneyFollowFace.add(viettelMoney2KFollowFace);
        MoneyTotalFollowFaceValue viettelMoney1KFollowFace = new MoneyTotalFollowFaceValue();
        viettelMoney1KFollowFace.setFaceValue(1000);
        viettelMoney1KFollowFace.setQuantity(this.viettel1KQuantity);
        viettelMoney1KFollowFace.setMoneyTotal(this.viettel1KMoney);
        listViettelMoneyFollowFace.add(viettelMoney1KFollowFace);
        viettel.setName("Viettel");
        viettel.setValue(this.viettelMoney);
        viettel.setTrans(listViettelMoneyFollowFace);
        listReponse.add(viettel);
        MoneyTotalRechargeByCardReponse vinaphone = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listVinaphoneMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
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
        MoneyTotalFollowFaceValue vinaphoneMoney15KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney15KFollowFace.setFaceValue(15000);
        vinaphoneMoney15KFollowFace.setQuantity(this.vinaphone15KQuantity);
        vinaphoneMoney15KFollowFace.setMoneyTotal(this.vinaphone15KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney15KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney10KFollowFace.setFaceValue(10000);
        vinaphoneMoney10KFollowFace.setQuantity(this.vinaphone10KQuantity);
        vinaphoneMoney10KFollowFace.setMoneyTotal(this.vinaphone10KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney10KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney5KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney5KFollowFace.setFaceValue(5000);
        vinaphoneMoney5KFollowFace.setQuantity(this.vinaphone5KQuantity);
        vinaphoneMoney5KFollowFace.setMoneyTotal(this.vinaphone5KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney5KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney4KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney4KFollowFace.setFaceValue(4000);
        vinaphoneMoney4KFollowFace.setQuantity(this.vinaphone4KQuantity);
        vinaphoneMoney4KFollowFace.setMoneyTotal(this.vinaphone4KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney4KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney3KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney3KFollowFace.setFaceValue(3000);
        vinaphoneMoney3KFollowFace.setQuantity(this.vinaphone3KQuantity);
        vinaphoneMoney3KFollowFace.setMoneyTotal(this.vinaphone3KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney3KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney2KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney2KFollowFace.setFaceValue(2000);
        vinaphoneMoney2KFollowFace.setQuantity(this.vinaphone2KQuantity);
        vinaphoneMoney2KFollowFace.setMoneyTotal(this.vinaphone2KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney2KFollowFace);
        MoneyTotalFollowFaceValue vinaphoneMoney1KFollowFace = new MoneyTotalFollowFaceValue();
        vinaphoneMoney1KFollowFace.setFaceValue(1000);
        vinaphoneMoney1KFollowFace.setQuantity(this.vinaphone1KQuantity);
        vinaphoneMoney1KFollowFace.setMoneyTotal(this.vinaphone1KMoney);
        listVinaphoneMoneyFollowFace.add(vinaphoneMoney1KFollowFace);
        vinaphone.setName("Vinaphone");
        vinaphone.setValue(this.vinaphoneMoney);
        vinaphone.setTrans(listVinaphoneMoneyFollowFace);
        listReponse.add(vinaphone);
        MoneyTotalRechargeByCardReponse mobifone = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listMobifoneMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
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
        MoneyTotalFollowFaceValue mobifoneMoney15KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney15KFollowFace.setFaceValue(15000);
        mobifoneMoney15KFollowFace.setQuantity(this.mobifone15KQuantity);
        mobifoneMoney15KFollowFace.setMoneyTotal(this.mobifone15KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney15KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney10KFollowFace.setFaceValue(10000);
        mobifoneMoney10KFollowFace.setQuantity(this.mobifone10KQuantity);
        mobifoneMoney10KFollowFace.setMoneyTotal(this.mobifone10KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney10KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney5KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney5KFollowFace.setFaceValue(5000);
        mobifoneMoney5KFollowFace.setQuantity(this.mobifone5KQuantity);
        mobifoneMoney5KFollowFace.setMoneyTotal(this.mobifone5KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney5KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney4KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney4KFollowFace.setFaceValue(4000);
        mobifoneMoney4KFollowFace.setQuantity(this.mobifone4KQuantity);
        mobifoneMoney4KFollowFace.setMoneyTotal(this.mobifone4KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney4KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney3KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney3KFollowFace.setFaceValue(3000);
        mobifoneMoney3KFollowFace.setQuantity(this.mobifone3KQuantity);
        mobifoneMoney3KFollowFace.setMoneyTotal(this.mobifone3KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney3KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney2KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney2KFollowFace.setFaceValue(2000);
        mobifoneMoney2KFollowFace.setQuantity(this.mobifone2KQuantity);
        mobifoneMoney2KFollowFace.setMoneyTotal(this.mobifone2KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney2KFollowFace);
        MoneyTotalFollowFaceValue mobifoneMoney1KFollowFace = new MoneyTotalFollowFaceValue();
        mobifoneMoney1KFollowFace.setFaceValue(1000);
        mobifoneMoney1KFollowFace.setQuantity(this.mobifone1KQuantity);
        mobifoneMoney1KFollowFace.setMoneyTotal(this.mobifone1KMoney);
        listMobifoneMoneyFollowFace.add(mobifoneMoney1KFollowFace);
        mobifone.setName("Mobifone");
        mobifone.setValue(this.mobifoneMoney);
        mobifone.setTrans(listMobifoneMoneyFollowFace);
        listReponse.add(mobifone);
        MoneyTotalRechargeByCardReponse vietNamMobile = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listVietNamMobileMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
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
        MoneyTotalFollowFaceValue vietNamMobileMoney15KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney15KFollowFace.setFaceValue(15000);
        vietNamMobileMoney15KFollowFace.setQuantity(this.vietNamMobile15KQuantity);
        vietNamMobileMoney15KFollowFace.setMoneyTotal(this.vietNamMobile15KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney15KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney10KFollowFace.setFaceValue(10000);
        vietNamMobileMoney10KFollowFace.setQuantity(this.vietNamMobile10KQuantity);
        vietNamMobileMoney10KFollowFace.setMoneyTotal(this.vietNamMobile10KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney10KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney5KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney5KFollowFace.setFaceValue(5000);
        vietNamMobileMoney5KFollowFace.setQuantity(this.vietNamMobile5KQuantity);
        vietNamMobileMoney5KFollowFace.setMoneyTotal(this.vietNamMobile5KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney5KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney4KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney4KFollowFace.setFaceValue(4000);
        vietNamMobileMoney4KFollowFace.setQuantity(this.vietNamMobile4KQuantity);
        vietNamMobileMoney4KFollowFace.setMoneyTotal(this.vietNamMobile4KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney4KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney3KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney3KFollowFace.setFaceValue(3000);
        vietNamMobileMoney3KFollowFace.setQuantity(this.vietNamMobile3KQuantity);
        vietNamMobileMoney3KFollowFace.setMoneyTotal(this.vietNamMobile3KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney3KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney2KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney2KFollowFace.setFaceValue(2000);
        vietNamMobileMoney2KFollowFace.setQuantity(this.vietNamMobile2KQuantity);
        vietNamMobileMoney2KFollowFace.setMoneyTotal(this.vietNamMobile2KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney2KFollowFace);
        MoneyTotalFollowFaceValue vietNamMobileMoney1KFollowFace = new MoneyTotalFollowFaceValue();
        vietNamMobileMoney1KFollowFace.setFaceValue(1000);
        vietNamMobileMoney1KFollowFace.setQuantity(this.vietNamMobile1KQuantity);
        vietNamMobileMoney1KFollowFace.setMoneyTotal(this.vietNamMobile1KMoney);
        listVietNamMobileMoneyFollowFace.add(vietNamMobileMoney1KFollowFace);
        vietNamMobile.setName("VietNamMobile");
        vietNamMobile.setValue(this.vietNamMobileMoney);
        vietNamMobile.setTrans(listVietNamMobileMoneyFollowFace);
        listReponse.add(vietNamMobile);
        MoneyTotalRechargeByCardReponse gMobile = new MoneyTotalRechargeByCardReponse();
        ArrayList<MoneyTotalFollowFaceValue> listgMobileMoneyFollowFace = new ArrayList<MoneyTotalFollowFaceValue>();
        MoneyTotalFollowFaceValue gMobileMoney100KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney100KFollowFace.setFaceValue(100000);
        gMobileMoney100KFollowFace.setQuantity(this.gMobile100KQuantity);
        gMobileMoney100KFollowFace.setMoneyTotal(this.gMobile100KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney100KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney50KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney50KFollowFace.setFaceValue(50000);
        gMobileMoney50KFollowFace.setQuantity(this.gMobile50KQuantity);
        gMobileMoney50KFollowFace.setMoneyTotal(this.gMobile50KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney50KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney30KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney30KFollowFace.setFaceValue(30000);
        gMobileMoney30KFollowFace.setQuantity(this.gMobile30KQuantity);
        gMobileMoney30KFollowFace.setMoneyTotal(this.gMobile30KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney30KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney20KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney20KFollowFace.setFaceValue(20000);
        gMobileMoney20KFollowFace.setQuantity(this.gMobile20KQuantity);
        gMobileMoney20KFollowFace.setMoneyTotal(this.gMobile20KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney20KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney15KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney15KFollowFace.setFaceValue(15000);
        gMobileMoney15KFollowFace.setQuantity(this.gMobile15KQuantity);
        gMobileMoney15KFollowFace.setMoneyTotal(this.gMobile15KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney15KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney10KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney10KFollowFace.setFaceValue(10000);
        gMobileMoney10KFollowFace.setQuantity(this.gMobile10KQuantity);
        gMobileMoney10KFollowFace.setMoneyTotal(this.gMobile10KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney10KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney5KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney5KFollowFace.setFaceValue(5000);
        gMobileMoney5KFollowFace.setQuantity(this.gMobile5KQuantity);
        gMobileMoney5KFollowFace.setMoneyTotal(this.gMobile5KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney5KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney4KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney4KFollowFace.setFaceValue(4000);
        gMobileMoney4KFollowFace.setQuantity(this.gMobile4KQuantity);
        gMobileMoney4KFollowFace.setMoneyTotal(this.gMobile4KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney4KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney3KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney3KFollowFace.setFaceValue(3000);
        gMobileMoney3KFollowFace.setQuantity(this.gMobile3KQuantity);
        gMobileMoney3KFollowFace.setMoneyTotal(this.gMobile3KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney3KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney2KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney2KFollowFace.setFaceValue(2000);
        gMobileMoney2KFollowFace.setQuantity(this.gMobile2KQuantity);
        gMobileMoney2KFollowFace.setMoneyTotal(this.gMobile2KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney2KFollowFace);
        MoneyTotalFollowFaceValue gMobileMoney1KFollowFace = new MoneyTotalFollowFaceValue();
        gMobileMoney1KFollowFace.setFaceValue(1000);
        gMobileMoney1KFollowFace.setQuantity(this.gMobile1KQuantity);
        gMobileMoney1KFollowFace.setMoneyTotal(this.gMobile1KMoney);
        listgMobileMoneyFollowFace.add(gMobileMoney1KFollowFace);
        gMobile.setName("GMobile");
        gMobile.setValue(this.gMobileMoney);
        gMobile.setTrans(listgMobileMoneyFollowFace);
        listReponse.add(gMobile);
        return listReponse;
    }

    @Override
    public List<RechargeBySmsResponse> exportDataRechargeBySms(String timeStart, String timeEnd, String amount, String code) {
        final ArrayList<RechargeBySmsResponse> results = new ArrayList<RechargeBySmsResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        objsort.put("_id", -1);
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
        iterable = db.getCollection("dvt_recharge_by_sms").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                RechargeBySmsResponse bank = new RechargeBySmsResponse();
                bank.nickname = document.getString("nick_name");
                bank.mobile = document.getString("mobile");
                bank.moMessage = document.getString("message_MO");
                bank.amount = document.getInteger("amount");
                bank.shortCode = document.getString("short_code");
                bank.requestId = document.getString("request_id");
                bank.requestTime = document.getString("time_request");
                bank.code = document.getInteger("code");
                bank.des = document.getString("description");
                bank.money = document.getInteger("money");
                bank.timeLog = document.getString("time_log");
                bank.provider = RechargeBySms8x98DAOImpl.mapProviderByTarget(document.getString("mobile"));
                results.add(bank);
            }
        });
        return results;
    }

    private static String mapProviderByTarget(String target) {
        if (target == null) {
            return "null";
        }
        if (target.matches("^(096|097|098|0162|0163|0164|0165|0166|0167|0168|0169|086|082)[\\d]{7}$")) {
            return "Viettel";
        }
        if (target.matches("^(091|094|0123|0124|0125|0127|0129|088)[\\d]{7}$")) {
            return "Vinaphone";
        }
        if (target.matches("^(090|093|0120|0121|0122|0126|0128|089)[\\d]{7}$")) {
            return "Mobifone";
        }
        if (target.matches("^(092|0188|0186)[\\d]{7}$")) {
            return "VietNamMobile";
        }
        if (target.matches("^(099|0199)[\\d]{7}$")) {
            return "GMobile";
        }
        return "other";
    }

}

