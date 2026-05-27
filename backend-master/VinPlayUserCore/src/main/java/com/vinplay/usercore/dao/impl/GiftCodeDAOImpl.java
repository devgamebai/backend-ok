/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.hazelcast.core.IMap
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.response.GiftCodeByNickNameResponse
 *  com.vinplay.vbee.common.response.GiftCodeCountResponse
 *  com.vinplay.vbee.common.response.GiftCodeDeleteResponse
 *  com.vinplay.vbee.common.response.GiftCodeResponse
 *  com.vinplay.vbee.common.response.GiftCodeUpdateResponse
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.response.ReportGiftCodeResponse
 *  com.vinplay.vbee.common.response.giftcode.GiftcodeFollowFaceValue
 *  com.vinplay.vbee.common.response.giftcode.GiftcodeStatisticObj
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.usercore.dao.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.usercore.dao.GiftCodeDAO;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GiftCodeByNickNameResponse;
import com.vinplay.vbee.common.response.GiftCodeCountResponse;
import com.vinplay.vbee.common.response.GiftCodeDeleteResponse;
import com.vinplay.vbee.common.response.GiftCodeResponse;
import com.vinplay.vbee.common.response.GiftCodeUpdateResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.response.ReportGiftCodeResponse;
import com.vinplay.vbee.common.response.giftcode.GiftcodeFollowFaceValue;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.response.giftcode.GiftcodeStatisticObj;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class GiftCodeDAOImpl
implements GiftCodeDAO {
    private static final Logger logger = Logger.getLogger((String)"backend");
    private long money = 0L;
    private String moneyType = null;
    private String source = null;
    private UserModel usermodel = null;
    private int MKTQuantity10K = 0;
    private int MKTUsed10K = 0;
    private int MKTLock10K = 0;
    private int MKTQuantity20K = 0;
    private int MKTUsed20K = 0;
    private int MKTLock20K = 0;
    private int MKTQuantity50K = 0;
    private int MKTUsed50K = 0;
    private int MKTLock50K = 0;
    private int MKTQuantity100K = 0;
    private int MKTUsed100K = 0;
    private int MKTLock100K = 0;
    private int MKTQuantity200K = 0;
    private int MKTUsed200K = 0;
    private int MKTLock200K = 0;
    private int MKTQuantity500K = 0;
    private int MKTUsed500K = 0;
    private int MKTLock500K = 0;
    private int MKTQuantity1M = 0;
    private int MKTUsed1M = 0;
    private int MKTLock1M = 0;
    private int MKTQuantity3M = 0;
    private int MKTUsed3M = 0;
    private int MKTLock3M = 0;
    private int MKTQuantity5M = 0;
    private int MKTUsed5M = 0;
    private int MKTLock5M = 0;
    private int MKTQuantity9M = 0;
    private int MKTUsed9M = 0;
    private int MKTLock9M = 0;
    private int MKTQuantity10M = 0;
    private int MKTUsed10M = 0;
    private int MKTLock10M = 0;
    private int VHQuantity10K = 0;
    private int VHUsed10K = 0;
    private int VHLock10K = 0;
    private int VHQuantity20K = 0;
    private int VHUsed20K = 0;
    private int VHLock20K = 0;
    private int VHQuantity50K = 0;
    private int VHUsed50K = 0;
    private int VHLock50K = 0;
    private int VHQuantity100K = 0;
    private int VHUsed100K = 0;
    private int VHLock100K = 0;
    private int VHQuantity200K = 0;
    private int VHUsed200K = 0;
    private int VHLock200K = 0;
    private int VHQuantity500K = 0;
    private int VHUsed500K = 0;
    private int VHLock500K = 0;
    private int VHQuantity1M = 0;
    private int VHUsed1M = 0;
    private int VHLock1M = 0;
    private int VHQuantity3M = 0;
    private int VHUsed3M = 0;
    private int VHLock3M = 0;
    private int VHQuantity5M = 0;
    private int VHUsed5M = 0;
    private int VHLock5M = 0;
    private int VHQuantity9M = 0;
    private int VHUsed9M = 0;
    private int VHLock9M = 0;
    private int VHQuantity10M = 0;
    private int VHUsed10M = 0;
    private int VHLock10M = 0;
    private int DLQuantity10K = 0;
    private int DLUsed10K = 0;
    private int DLLock10K = 0;
    private int DLQuantity20K = 0;
    private int DLUsed20K = 0;
    private int DLLock20K = 0;
    private int DLQuantity50K = 0;
    private int DLUsed50K = 0;
    private int DLLock50K = 0;
    private int DLQuantity100K = 0;
    private int DLUsed100K = 0;
    private int DLLock100K = 0;
    private int DLQuantity200K = 0;
    private int DLUsed200K = 0;
    private int DLLock200K = 0;
    private int DLQuantity500K = 0;
    private int DLUsed500K = 0;
    private int DLLock500K = 0;
    private int DLQuantity1M = 0;
    private int DLUsed1M = 0;
    private int DLLock1M = 0;
    private int DLQuantity3M = 0;
    private int DLUsed3M = 0;
    private int DLLock3M = 0;
    private int DLQuantity5M = 0;
    private int DLUsed5M = 0;
    private int DLLock5M = 0;
    private int DLQuantity9M = 0;
    private int DLUsed9M = 0;
    private int DLLock9M = 0;
    private int DLQuantity10M = 0;
    private int DLUsed10M = 0;
    private int DLLock10M = 0;

    @Override
    public boolean xuatGiftCode(final GiftCodeMessage msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        final MongoCollection giftCodeDB = db.getCollection("gift_code");
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("count_use", 0);
        conditions.put("price", msg.getPrice());
        conditions.put("type", msg.getType());
        conditions.put("release", msg.getRelease());
        long count = db.getCollection("gift_code_store").count((Bson)new Document(conditions));
        if (count >= (long)msg.getQuantity()) {
            FindIterable iterable = db.getCollection("gift_code_store").find((Bson)new Document(conditions)).limit(msg.getQuantity());
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    Document doc = new Document();
                    doc.append("giftcode", document.getString("giftcode"));
                    doc.append("price", msg.getPrice());
                    doc.append("quantity", msg.getQuantity());
                    doc.append("source", msg.getSource());
                    doc.append("count_use", 0);
                    doc.append("create_time", timeLog);
                    doc.append("money_type", msg.getMoneyType());
                    doc.append("release", msg.getRelease());
                    doc.append("nick_name", "");
                    doc.append("user_name", "");
                    doc.append("mobile", "");
                    doc.append("block", 0);
                    doc.append("type", msg.getType());
                    doc.append("giftcodefull", (msg.getRelease() + msg.getPrice() + msg.getSource() + document.getString("giftcode")));
                    doc.append("update_time", "");
                    doc.append("agent", "0");
                    giftCodeDB.insertOne(doc);
                    try {
                        GiftCodeMessage message = new GiftCodeMessage();
                        message.setGiftCode(document.getString("giftcode"));
                        MessageBusFactory.get("queue_gift_code").publish("queue_gift_code", (BaseMessage)message, (int)1200);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean genGiftCode(GiftCodeMessage msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("gift_code_store");
        Document doc = new Document();
        doc.append("giftcode", msg.getGiftCode());
        doc.append("price", msg.getPrice());
        doc.append("quantity", msg.getQuantity());
        doc.append("source", msg.getSource());
        doc.append("count_use", 0);
        doc.append("create_time", timeLog);
        doc.append("money_type", msg.getMoneyType());
        doc.append("release", msg.getRelease());
        doc.append("type", msg.getType());
        col.insertOne(doc);
        return true;
    }

    @Override
    public synchronized GiftCodeUpdateResponse updateGiftCode(final String nickName, String giftCode) throws SQLException {
        final ArrayList results = new ArrayList();
        final GiftCodeUpdateResponse response = new GiftCodeUpdateResponse(false, "1001");
        FindIterable iterable = null;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String updatetime = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("gift_code");
        MongoCollection colgame = db.getCollection("gift_code_game");
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        HashMap<String, Object> conditions2 = new HashMap<String, Object>();
        conditions.put("giftcodefull", giftCode);
        conditions.put("block", 0);
        UserServiceImpl userService = new UserServiceImpl();
        this.usermodel = userService.getUserByNickName(nickName);
        long currentMoneyVin = this.usermodel.getVinTotal();
        long currentMoneyXu = this.usermodel.getXuTotal();
        String codeGame = giftCode.substring(0, 1);
        if (codeGame.equals("G")) {
            iterable = db.getCollection("gift_code_game").find((Bson)new Document(conditions));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    if (document.getInteger("money_type") == 1) {
                        response.moneyType = 1;
                        GiftCodeDAOImpl.this.moneyType = "vin";
                    } else if (document.getInteger("money_type") == 0) {
                        response.moneyType = 0;
                        GiftCodeDAOImpl.this.moneyType = "xu";
                    }
                    GiftCodeDAOImpl.this.source = document.getString("source");
                    response.use = document.getInteger("count_use", 1);
                    results.add(response);
                }
            });
        } else {
            iterable = db.getCollection("gift_code").find((Bson)new Document(conditions));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    if (document.getInteger("money_type") == 1) {
                        response.moneyType = 1;
                        response.moneyGiftCodeVin = Long.parseLong(document.getString("price")) * 1000L;
                        response.moneyGiftCodeXu = 0L;
                        GiftCodeDAOImpl.this.money = response.moneyGiftCodeVin;
                        GiftCodeDAOImpl.this.moneyType = "vin";
                    } else if (document.getInteger("money_type") == 0) {
                        response.moneyType = 0;
                        response.moneyGiftCodeVin = 0L;
                        response.moneyGiftCodeXu = Long.parseLong(document.getString("price")) * 1000000L;
                        GiftCodeDAOImpl.this.money = response.moneyGiftCodeXu;
                        GiftCodeDAOImpl.this.moneyType = "xu";
                    }
                    GiftCodeDAOImpl.this.source = document.getString("source");
                    response.use = document.getInteger("count_use", 1);
                    response.agent = document.getString("agent");
                    response.type = document.getString("type");
                    results.add(response);
                }
            });
        }
        if (response.use != 0) {
            response.setErrorCode("10002");
            return response;
        }
        if (results.size() == 1) {
            response.setErrorCode("0");
            GiftCodeUpdateResponse strgiftCode = (GiftCodeUpdateResponse)results.get(0);
            if (codeGame.equals("G")) {
                BasicDBObject nickname = new BasicDBObject("nick_name", nickName);
                BasicDBObject mobile = new BasicDBObject("mobile", this.usermodel.getMobile());
                ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
                myList.add(nickname);
                myList.add(mobile);
                conditions2.put("$or", myList);
                conditions2.put("count_use", 1);
                conditions2.put("source", this.source);
                conditions2.put("money_type", strgiftCode.moneyType);
                FindIterable iterable2 = db.getCollection("gift_code_game").find((Bson)new Document(conditions2));
                iterable2.forEach((Block)new Block<Document>(){

                    public void apply(Document document) {
                        if (document.getString("nick_name").equals(nickName)) {
                            response.setErrorCode("10004");
                        } 
                    }
                });
                if (response.getErrorCode().equals("0")) {
                    colgame.updateOne((Bson)new Document("giftcodefull", giftCode), (Bson)new Document("$set", new Document("nick_name", nickName).append("count_use", 1).append("mobile", this.usermodel.getMobile()).append("user_name", this.usermodel.getUsername()).append("update_time", updatetime)));
                    response.setErrorCode("0");
                    response.setSuccess(true);
                }
            } else {
                if (!(strgiftCode.agent != null && strgiftCode.agent.equals("1") || this.source.equals("MK4") || this.source.equals("CL"))) {
                    if (strgiftCode.moneyType == 1) {
                        conditions2.put("price", String.valueOf(strgiftCode.moneyGiftCodeVin / 1000L));
                    } else {
                        conditions2.put("price", String.valueOf(strgiftCode.moneyGiftCodeXu / 1000000L));
                    }
                    BasicDBObject nickname = new BasicDBObject("nick_name", nickName);
                    BasicDBObject mobile = new BasicDBObject("mobile", this.usermodel.getMobile());
                    ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
                    myList.add(nickname);
                    myList.add(mobile);
                    conditions2.put("$or", myList);
                    conditions2.put("count_use", 1);
                    conditions2.put("source", this.source);
                    conditions2.put("money_type", strgiftCode.moneyType);
                    FindIterable iterable2 = db.getCollection("gift_code").find((Bson)new Document(conditions2));
                    iterable2.forEach((Block)new Block<Document>(){

                        public void apply(Document document) {
                            if (document.getString("nick_name").equals(nickName)) {
                                response.setErrorCode("10004");
                            } else if (document.getString("mobile").equals(GiftCodeDAOImpl.this.usermodel.getMobile())) {
                                response.setErrorCode("10004");
                            }
                            if (document.getString("price").equals(document.getString("price"))) {
                                response.setErrorCode("10004");
                            }
                        }
                    });
                }
                if (response.getErrorCode().equals("0")) {
                    col.updateOne((Bson)new Document("giftcodefull", giftCode), (Bson)new Document("$set", new Document("nick_name", nickName).append("count_use", 1).append("mobile", this.usermodel.getMobile()).append("user_name", this.usermodel.getUsername()).append("update_time", updatetime).append("block", 0)));
                    MoneyResponse mnres = null;
                    if (strgiftCode.agent != null) {
                        if (strgiftCode.agent.equals("0")) {
                            if (strgiftCode.type.equals("2")) {
                                mnres = userService.updateMoney(nickName, this.money, this.moneyType, "GiftCodeMKT", "GiftCode", "M\u00e3: " + giftCode, 0L, null, TransType.NO_VIPPOINT);
                            } else if (strgiftCode.type.equals("3")) {
                                mnres = userService.updateMoney(nickName, this.money, this.moneyType, "GiftCodeVH", "GiftCode", "M\u00e3: " + giftCode, 0L, null, TransType.NO_VIPPOINT);
                            } else if (strgiftCode.type.equals("1")) {
                                mnres = userService.updateMoney(nickName, this.money, this.moneyType, "GiftCode", "GiftCode", "M\u00e3: " + giftCode, 0L, null, TransType.NO_VIPPOINT);
                            }
                        } else if (strgiftCode.agent.equals("1")) {
                            mnres = userService.updateMoney(nickName, this.money, this.moneyType, "GcAgentImport", "GcAgent", "M\u00e3: " + giftCode, 0L, null, TransType.NO_VIPPOINT);
                        }
                    } else {
                        if (strgiftCode.type.equals("2")) {
                            mnres = userService.updateMoney(nickName, this.money, this.moneyType, "GiftCodeMKT", "GiftCode", "M\u00e3: " + giftCode, 0L, null, TransType.NO_VIPPOINT);
                        } else if (strgiftCode.type.equals("3")) {
                            mnres = userService.updateMoney(nickName, this.money, this.moneyType, "GiftCodeVH", "GiftCode", "M\u00e3: " + giftCode, 0L, null, TransType.NO_VIPPOINT);
                        }
                        if (strgiftCode.type.equals("1")) {
                            mnres = userService.updateMoney(nickName, this.money, this.moneyType, "GiftCode", "GiftCode", "M\u00e3: " + giftCode, 0L, null, TransType.NO_VIPPOINT);
                        }
                    }
                    if (mnres != null && mnres.isSuccess()) {
                        if (this.moneyType.equals("vin")) {
                            response.currentMoneyVin = mnres.getCurrentMoney();
                            response.currentMoneyXu = currentMoneyXu;
                        } else {
                            response.currentMoneyVin = currentMoneyVin;
                            response.currentMoneyXu = mnres.getCurrentMoney();
                        }
                        response.setErrorCode("0");
                        response.setSuccess(true);
                    }
                }
            }
            return response;
        }
        response.setErrorCode("10001");
        return response;
    }

    @Override
    public List<GiftCodeResponse> searchAllGiftCode(String nickName, String giftcode, String price, String source, String timeStart, String timeEnd, String moneyType, String usegift, int page, int totalRecord, String type, String release, String timeType, String block) {
        int num_start = (page - 1) * totalRecord;
        final ArrayList<GiftCodeResponse> results = new ArrayList<GiftCodeResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        conditions.put("money_type", Integer.parseInt(moneyType));
        if (price != null && !price.equals("")) {
            conditions.put("price", price);
        }
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        if (release != null && !release.equals("")) {
            conditions.put("release", release);
        }
        if (block != null && !block.equals("")) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (timeType.equals("1") && timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("create_time", obj);
        }
        if (timeType.equals("2") && timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("update_time", obj);
        }
        if (usegift != null && !usegift.equals("")) {
            conditions.put("count_use", Integer.parseInt(usegift));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", type);
        }
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (giftcode != null && !giftcode.equals("")) {
            conditions.put("giftcodefull", giftcode);
        }
        FindIterable iterable = db.getCollection("gift_code").find((Bson)new Document(conditions)).skip(num_start).limit(totalRecord).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                GiftCodeResponse giftcode = new GiftCodeResponse();
                giftcode.price = document.getString("price");
                giftcode.source = document.getString("source");
                giftcode.quantity = document.getInteger("quantity").intValue();
                giftcode.giftCode = document.getString("giftcodefull");
                giftcode.createTime = document.getString("create_time");
                giftcode.updateTime = document.getString("update_time");
                giftcode.useGiftCode = document.getInteger("count_use");
                giftcode.nickName = document.getString("nick_name");
                giftcode.userName = document.getString("user_name");
                giftcode.block = document.getInteger("block");
                results.add(giftcode);
            }
        });
        return results;
    }

    @Override
    public List<String> loadAllGiftcode() {
        final ArrayList<String> results = new ArrayList<String>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = db.getCollection("gift_code_store").find();
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                results.add(document.getString("giftcode"));
            }
        });
        return results;
    }

    @Override
    public List<GiftCodeResponse> searchAllGiftCodeAdmin(String price, String timeStart, String timeEnd, String moneyType, String usegift, String source, int page, int totalRecord, String block) {
        int num_start = (page - 1) * totalRecord;
        final ArrayList<GiftCodeResponse> results = new ArrayList<GiftCodeResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        conditions.put("money_type", Integer.parseInt(moneyType));
        if (price != null && !price.equals("")) {
            conditions.put("price", price);
        }
        if (block != null && !block.equals("")) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("create_time", obj);
        }
        if (usegift != null && !usegift.equals("")) {
            conditions.put("count_use", Integer.parseInt(usegift));
        }
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        FindIterable iterable = db.getCollection("gift_code_store").find((Bson)new Document(conditions)).skip(num_start).limit(totalRecord);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                GiftCodeResponse giftcode = new GiftCodeResponse();
                giftcode.price = document.getString("price");
                giftcode.source = document.getString("source");
                giftcode.quantity = document.getInteger("quantity").intValue();
                giftcode.giftCode = document.getString("giftcode");
                giftcode.createTime = document.getString("create_time");
                giftcode.updateTime = document.getString("update_time");
                giftcode.useGiftCode = document.getInteger("count_use");
                results.add(giftcode);
            }
        });
        return results;
    }

    @Override
    public GiftCodeCountResponse countGiftCodeByPrice(String price, String source, String timeStart, String timeEnd, String moneyType, String type, String timeType, String block) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        conditions.put("money_type", Integer.parseInt(moneyType));
        if (price != null && !price.equals("")) {
            conditions.put("price", price);
        }
        if (block != null && !block.equals("")) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            if (timeType.equals("1")) {
                conditions.put("create_time", obj);
            } else {
                conditions.put("update_time", obj);
            }
        }
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", type);
        }
        long quantity = db.getCollection("gift_code").count((Bson)new Document(conditions));
        long giftcodeuse = db.getCollection("gift_code").count((Bson)new Document(conditions).append("count_use", 1));
        long giftcodeblock = db.getCollection("gift_code").count((Bson)new Document(conditions).append("block", 1));
        long remain = quantity - giftcodeuse;
        GiftCodeCountResponse response = new GiftCodeCountResponse();
        response.giftCodeUse = giftcodeuse;
        response.remain = remain;
        response.quantity = quantity;
        response.price = price;
        response.block = giftcodeblock;
        return response;
    }

    @Override
    public List<GiftcodeStatisticObj> thongKeGiftcodeDaXuat(String source, String timeStart, String timeEnd, String moneyType, String timeType) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        Document conditions = new Document();
        this.MKTQuantity10K = 0;
        this.MKTUsed10K = 0;
        this.MKTLock10K = 0;
        this.MKTQuantity20K = 0;
        this.MKTUsed20K = 0;
        this.MKTLock20K = 0;
        this.MKTQuantity50K = 0;
        this.MKTUsed50K = 0;
        this.MKTLock50K = 0;
        this.MKTQuantity100K = 0;
        this.MKTUsed100K = 0;
        this.MKTLock100K = 0;
        this.MKTQuantity200K = 0;
        this.MKTUsed200K = 0;
        this.MKTLock200K = 0;
        this.MKTQuantity500K = 0;
        this.MKTUsed500K = 0;
        this.MKTLock500K = 0;
        this.MKTQuantity1M = 0;
        this.MKTUsed1M = 0;
        this.MKTLock1M = 0;
        this.MKTQuantity3M = 0;
        this.MKTUsed3M = 0;
        this.MKTLock3M = 0;
        this.MKTQuantity5M = 0;
        this.MKTUsed5M = 0;
        this.MKTLock5M = 0;
        this.MKTQuantity9M = 0;
        this.MKTUsed9M = 0;
        this.MKTLock9M = 0;
        this.MKTQuantity10M = 0;
        this.MKTUsed10M = 0;
        this.MKTLock10M = 0;
        this.VHQuantity10K = 0;
        this.VHUsed10K = 0;
        this.VHLock10K = 0;
        this.VHQuantity20K = 0;
        this.VHUsed20K = 0;
        this.VHLock20K = 0;
        this.VHQuantity50K = 0;
        this.VHUsed50K = 0;
        this.VHLock50K = 0;
        this.VHQuantity100K = 0;
        this.VHUsed100K = 0;
        this.VHLock100K = 0;
        this.VHQuantity200K = 0;
        this.VHUsed200K = 0;
        this.VHLock200K = 0;
        this.VHQuantity500K = 0;
        this.VHUsed500K = 0;
        this.VHLock500K = 0;
        this.VHQuantity1M = 0;
        this.VHUsed1M = 0;
        this.VHLock1M = 0;
        this.VHQuantity3M = 0;
        this.VHUsed3M = 0;
        this.VHLock3M = 0;
        this.VHQuantity5M = 0;
        this.VHUsed5M = 0;
        this.VHLock5M = 0;
        this.VHQuantity9M = 0;
        this.VHUsed9M = 0;
        this.VHLock9M = 0;
        this.VHQuantity10M = 0;
        this.VHUsed10M = 0;
        this.VHLock10M = 0;
        this.DLQuantity10K = 0;
        this.DLUsed10K = 0;
        this.DLLock10K = 0;
        this.DLQuantity20K = 0;
        this.DLUsed20K = 0;
        this.DLLock20K = 0;
        this.DLQuantity50K = 0;
        this.DLUsed50K = 0;
        this.DLLock50K = 0;
        this.DLQuantity100K = 0;
        this.DLUsed100K = 0;
        this.DLLock100K = 0;
        this.DLQuantity200K = 0;
        this.DLUsed200K = 0;
        this.DLLock200K = 0;
        this.DLQuantity500K = 0;
        this.DLUsed500K = 0;
        this.DLLock500K = 0;
        this.DLQuantity1M = 0;
        this.DLUsed1M = 0;
        this.DLLock1M = 0;
        this.DLQuantity3M = 0;
        this.DLUsed3M = 0;
        this.DLLock3M = 0;
        this.DLQuantity5M = 0;
        this.DLUsed5M = 0;
        this.DLLock5M = 0;
        this.DLQuantity9M = 0;
        this.DLUsed9M = 0;
        this.DLLock9M = 0;
        this.DLQuantity10M = 0;
        this.DLUsed10M = 0;
        this.DLLock10M = 0;
        if (moneyType != null && !moneyType.equals("")) {
            conditions.put("money_type", Integer.parseInt(moneyType));
        }
        if (!(timeStart == null || timeStart.equals("") || timeEnd == null || timeEnd.equals("") || timeType == null || timeType.equals(""))) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            if (timeType.equals("1")) {
                conditions.put("create_time", obj);
            } else {
                conditions.put("update_time", obj);
            }
        }
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        objsort.put("_id", -1);
        FindIterable iterable = null;
        iterable = db.getCollection("gift_code").find((Bson)new Document((Map)conditions)).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                block0 : switch (document.getInteger("money_type")) {
                    case 1: {
                        String string;
                        switch (string = document.getString("type")) {
                            case "1": {
                                String string2;
                                switch (string2 = document.getString("price")) {
                                    case "10": {
                                        GiftCodeDAOImpl.this.DLQuantity10K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed10K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock10K++;
                                        break block0;
                                    }
                                    case "20": {
                                        GiftCodeDAOImpl.this.DLQuantity20K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed20K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock20K++;
                                        break block0;
                                    }
                                    case "50": {
                                        GiftCodeDAOImpl.this.DLQuantity50K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed50K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock50K++;
                                        break block0;
                                    }
                                    case "100": {
                                        GiftCodeDAOImpl.this.DLQuantity100K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed100K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock100K++;
                                        break block0;
                                    }
                                    case "200": {
                                        GiftCodeDAOImpl.this.DLQuantity200K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed200K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock200K++;
                                        break block0;
                                    }
                                    case "500": {
                                        GiftCodeDAOImpl.this.DLQuantity500K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed500K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock500K++;
                                    }
                                }
                                break block0;
                            }
                            case "2": {
                                String string3;
                                switch (string3 = document.getString("price")) {
                                    case "10": {
                                        GiftCodeDAOImpl.this.MKTQuantity10K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed10K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock10K++;
                                        break block0;
                                    }
                                    case "20": {
                                        GiftCodeDAOImpl.this.MKTQuantity20K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed20K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock20K++;
                                        break block0;
                                    }
                                    case "50": {
                                        GiftCodeDAOImpl.this.MKTQuantity50K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed50K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock50K++;
                                        break block0;
                                    }
                                    case "100": {
                                        GiftCodeDAOImpl.this.MKTQuantity100K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed100K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock100K++;
                                        break block0;
                                    }
                                    case "200": {
                                        GiftCodeDAOImpl.this.MKTQuantity200K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed200K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock200K++;
                                        break block0;
                                    }
                                    case "500": {
                                        GiftCodeDAOImpl.this.MKTQuantity500K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed500K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock500K++;
                                    }
                                }
                                break block0;
                            }
                            case "3": {
                                String string4;
                                switch (string4 = document.getString("price")) {
                                    case "10": {
                                        GiftCodeDAOImpl.this.VHQuantity10K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed10K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock10K++;
                                        break block0;
                                    }
                                    case "20": {
                                        GiftCodeDAOImpl.this.VHQuantity20K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed20K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock20K++;
                                        break block0;
                                    }
                                    case "50": {
                                        GiftCodeDAOImpl.this.VHQuantity50K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed50K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock50K++;
                                        break block0;
                                    }
                                    case "100": {
                                        GiftCodeDAOImpl.this.VHQuantity100K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed100K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock100K++;
                                        break block0;
                                    }
                                    case "200": {
                                        GiftCodeDAOImpl.this.VHQuantity200K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed200K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock200K++;
                                        break block0;
                                    }
                                    case "500": {
                                        GiftCodeDAOImpl.this.VHQuantity500K++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed500K++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock500K++;
                                    }
                                }
                                break block0;
                            }
                        }
                        break;
                    }
                    case 0: {
                        String string5;
                        switch (string5 = document.getString("type")) {
                            case "1": {
                                String string6;
                                switch (string6 = document.getString("price")) {
                                    case "1": {
                                        GiftCodeDAOImpl.this.DLQuantity1M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed1M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock1M++;
                                        break block0;
                                    }
                                    case "3": {
                                        GiftCodeDAOImpl.this.DLQuantity3M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed3M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock3M++;
                                        break block0;
                                    }
                                    case "5": {
                                        GiftCodeDAOImpl.this.DLQuantity5M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed5M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock5M++;
                                        break block0;
                                    }
                                    case "9": {
                                        GiftCodeDAOImpl.this.DLQuantity9M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed9M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock9M++;
                                        break block0;
                                    }
                                    case "10": {
                                        GiftCodeDAOImpl.this.DLQuantity10M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.DLUsed10M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.DLLock10M++;
                                    }
                                }
                                break block0;
                            }
                            case "2": {
                                String string7;
                                switch (string7 = document.getString("price")) {
                                    case "1": {
                                        GiftCodeDAOImpl.this.MKTQuantity1M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed1M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock1M++;
                                        break block0;
                                    }
                                    case "3": {
                                        GiftCodeDAOImpl.this.MKTQuantity3M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed3M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock3M++;
                                        break block0;
                                    }
                                    case "5": {
                                        GiftCodeDAOImpl.this.MKTQuantity5M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed5M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock5M++;
                                        break block0;
                                    }
                                    case "9": {
                                        GiftCodeDAOImpl.this.MKTQuantity9M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed9M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock9M++;
                                        break block0;
                                    }
                                    case "10": {
                                        GiftCodeDAOImpl.this.MKTQuantity10M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.MKTUsed10M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.MKTLock10M++;
                                    }
                                }
                                break block0;
                            }
                            case "3": {
                                String string8;
                                switch (string8 = document.getString("price")) {
                                    case "1": {
                                        GiftCodeDAOImpl.this.VHQuantity1M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed1M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock1M++;
                                        break block0;
                                    }
                                    case "3": {
                                        GiftCodeDAOImpl.this.VHQuantity3M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed3M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock3M++;
                                        break block0;
                                    }
                                    case "5": {
                                        GiftCodeDAOImpl.this.VHQuantity5M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed5M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock5M++;
                                        break block0;
                                    }
                                    case "9": {
                                        GiftCodeDAOImpl.this.VHQuantity9M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed9M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock9M++;
                                        break block0;
                                    }
                                    case "10": {
                                        GiftCodeDAOImpl.this.VHQuantity10M++;
                                        if (document.getInteger("count_use") == 1) {
                                            GiftCodeDAOImpl.this.VHUsed10M++;
                                        }
                                        if (document.getInteger("block") != 1) break;
                                        GiftCodeDAOImpl.this.VHLock10M++;
                                    }
                                }
                                break block0;
                            }
                        }
                        break;
                    }
                }
            }
        });
        ArrayList<GiftcodeStatisticObj> listResponse = new ArrayList<GiftcodeStatisticObj>();
        GiftcodeStatisticObj daiLyVin = new GiftcodeStatisticObj();
        ArrayList<GiftcodeFollowFaceValue> listDaiLyVinFollowFace = new ArrayList<GiftcodeFollowFaceValue>();
        GiftcodeFollowFaceValue daiLyVin10K = new GiftcodeFollowFaceValue();
        daiLyVin10K.setFaceValue(10000);
        daiLyVin10K.setQuantity(this.DLQuantity10K);
        daiLyVin10K.setUsed(this.DLUsed10K);
        daiLyVin10K.setLock(this.DLLock10K);
        listDaiLyVinFollowFace.add(daiLyVin10K);
        GiftcodeFollowFaceValue daiLyVin20K = new GiftcodeFollowFaceValue();
        daiLyVin20K.setFaceValue(20000);
        daiLyVin20K.setQuantity(this.DLQuantity20K);
        daiLyVin20K.setUsed(this.DLUsed20K);
        daiLyVin20K.setLock(this.DLLock20K);
        listDaiLyVinFollowFace.add(daiLyVin20K);
        GiftcodeFollowFaceValue daiLyVin50K = new GiftcodeFollowFaceValue();
        daiLyVin50K.setFaceValue(50000);
        daiLyVin50K.setQuantity(this.DLQuantity50K);
        daiLyVin50K.setUsed(this.DLUsed50K);
        daiLyVin50K.setLock(this.DLLock50K);
        listDaiLyVinFollowFace.add(daiLyVin50K);
        GiftcodeFollowFaceValue daiLyVin100K = new GiftcodeFollowFaceValue();
        daiLyVin100K.setFaceValue(100000);
        daiLyVin100K.setQuantity(this.DLQuantity100K);
        daiLyVin100K.setUsed(this.DLUsed100K);
        daiLyVin100K.setLock(this.DLLock100K);
        listDaiLyVinFollowFace.add(daiLyVin100K);
        GiftcodeFollowFaceValue daiLyVin200K = new GiftcodeFollowFaceValue();
        daiLyVin200K.setFaceValue(200000);
        daiLyVin200K.setQuantity(this.DLQuantity200K);
        daiLyVin200K.setUsed(this.DLUsed200K);
        daiLyVin200K.setLock(this.DLLock200K);
        listDaiLyVinFollowFace.add(daiLyVin200K);
        GiftcodeFollowFaceValue daiLyVin500K = new GiftcodeFollowFaceValue();
        daiLyVin500K.setFaceValue(500000);
        daiLyVin500K.setQuantity(this.DLQuantity500K);
        daiLyVin500K.setUsed(this.DLUsed500K);
        daiLyVin500K.setLock(this.DLLock500K);
        listDaiLyVinFollowFace.add(daiLyVin500K);
        daiLyVin.setName("DaiLyVin");
        daiLyVin.setTrans(listDaiLyVinFollowFace);
        listResponse.add(daiLyVin);
        GiftcodeStatisticObj daiLyXu = new GiftcodeStatisticObj();
        ArrayList<GiftcodeFollowFaceValue> listDaiLyXuFollowFace = new ArrayList<GiftcodeFollowFaceValue>();
        GiftcodeFollowFaceValue daiLyXu1M = new GiftcodeFollowFaceValue();
        daiLyXu1M.setFaceValue(1000000);
        daiLyXu1M.setQuantity(this.DLQuantity1M);
        daiLyXu1M.setUsed(this.DLUsed1M);
        daiLyXu1M.setLock(this.DLLock1M);
        listDaiLyXuFollowFace.add(daiLyXu1M);
        GiftcodeFollowFaceValue daiLyXu3M = new GiftcodeFollowFaceValue();
        daiLyXu3M.setFaceValue(3000000);
        daiLyXu3M.setQuantity(this.DLQuantity3M);
        daiLyXu3M.setUsed(this.DLUsed3M);
        daiLyXu3M.setLock(this.DLLock3M);
        listDaiLyXuFollowFace.add(daiLyXu3M);
        GiftcodeFollowFaceValue daiLyXu5M = new GiftcodeFollowFaceValue();
        daiLyXu5M.setFaceValue(5000000);
        daiLyXu5M.setQuantity(this.DLQuantity5M);
        daiLyXu5M.setUsed(this.DLUsed5M);
        daiLyXu5M.setLock(this.DLLock5M);
        listDaiLyXuFollowFace.add(daiLyXu5M);
        GiftcodeFollowFaceValue daiLyXu9M = new GiftcodeFollowFaceValue();
        daiLyXu9M.setFaceValue(9000000);
        daiLyXu9M.setQuantity(this.DLQuantity9M);
        daiLyXu9M.setUsed(this.DLUsed9M);
        daiLyXu9M.setLock(this.DLLock9M);
        listDaiLyXuFollowFace.add(daiLyXu9M);
        GiftcodeFollowFaceValue daiLyXu10M = new GiftcodeFollowFaceValue();
        daiLyXu10M.setFaceValue(10000000);
        daiLyXu10M.setQuantity(this.DLQuantity10M);
        daiLyXu10M.setUsed(this.DLUsed10M);
        daiLyXu10M.setLock(this.DLLock10M);
        listDaiLyXuFollowFace.add(daiLyXu10M);
        daiLyXu.setName("DaiLyXu");
        daiLyXu.setTrans(listDaiLyXuFollowFace);
        listResponse.add(daiLyXu);
        GiftcodeStatisticObj marketingVin = new GiftcodeStatisticObj();
        ArrayList<GiftcodeFollowFaceValue> listMarketingVinFollowFace = new ArrayList<GiftcodeFollowFaceValue>();
        GiftcodeFollowFaceValue marketingVin10K = new GiftcodeFollowFaceValue();
        marketingVin10K.setFaceValue(10000);
        marketingVin10K.setQuantity(this.MKTQuantity10K);
        marketingVin10K.setUsed(this.MKTUsed10K);
        marketingVin10K.setLock(this.MKTLock10K);
        listMarketingVinFollowFace.add(marketingVin10K);
        GiftcodeFollowFaceValue marketingVin20K = new GiftcodeFollowFaceValue();
        marketingVin20K.setFaceValue(20000);
        marketingVin20K.setQuantity(this.MKTQuantity20K);
        marketingVin20K.setUsed(this.MKTUsed20K);
        marketingVin20K.setLock(this.MKTLock20K);
        listMarketingVinFollowFace.add(marketingVin20K);
        GiftcodeFollowFaceValue marketingVin50K = new GiftcodeFollowFaceValue();
        marketingVin50K.setFaceValue(50000);
        marketingVin50K.setQuantity(this.MKTQuantity50K);
        marketingVin50K.setUsed(this.MKTUsed50K);
        marketingVin50K.setLock(this.MKTLock50K);
        listMarketingVinFollowFace.add(marketingVin50K);
        GiftcodeFollowFaceValue marketingVin100K = new GiftcodeFollowFaceValue();
        marketingVin100K.setFaceValue(100000);
        marketingVin100K.setQuantity(this.MKTQuantity100K);
        marketingVin100K.setUsed(this.MKTUsed100K);
        marketingVin100K.setLock(this.MKTLock100K);
        listMarketingVinFollowFace.add(marketingVin100K);
        GiftcodeFollowFaceValue marketingVin200K = new GiftcodeFollowFaceValue();
        marketingVin200K.setFaceValue(200000);
        marketingVin200K.setQuantity(this.MKTQuantity200K);
        marketingVin200K.setUsed(this.MKTUsed200K);
        marketingVin200K.setLock(this.MKTLock200K);
        listMarketingVinFollowFace.add(marketingVin200K);
        GiftcodeFollowFaceValue marketingVin500K = new GiftcodeFollowFaceValue();
        marketingVin500K.setFaceValue(500000);
        marketingVin500K.setQuantity(this.MKTQuantity500K);
        marketingVin500K.setUsed(this.MKTUsed500K);
        marketingVin500K.setLock(this.MKTLock500K);
        listMarketingVinFollowFace.add(marketingVin500K);
        marketingVin.setName("MarketingVin");
        marketingVin.setTrans(listMarketingVinFollowFace);
        listResponse.add(marketingVin);
        GiftcodeStatisticObj marketingXu = new GiftcodeStatisticObj();
        ArrayList<GiftcodeFollowFaceValue> listMarketingXuFollowFace = new ArrayList<GiftcodeFollowFaceValue>();
        GiftcodeFollowFaceValue marketingXu1M = new GiftcodeFollowFaceValue();
        marketingXu1M.setFaceValue(1000000);
        marketingXu1M.setQuantity(this.MKTQuantity1M);
        marketingXu1M.setUsed(this.MKTUsed1M);
        marketingXu1M.setLock(this.MKTLock1M);
        listMarketingXuFollowFace.add(marketingXu1M);
        GiftcodeFollowFaceValue marketingXu3M = new GiftcodeFollowFaceValue();
        marketingXu3M.setFaceValue(3000000);
        marketingXu3M.setQuantity(this.MKTQuantity3M);
        marketingXu3M.setUsed(this.MKTUsed3M);
        marketingXu3M.setLock(this.MKTLock3M);
        listMarketingXuFollowFace.add(marketingXu3M);
        GiftcodeFollowFaceValue marketingXu5M = new GiftcodeFollowFaceValue();
        marketingXu5M.setFaceValue(5000000);
        marketingXu5M.setQuantity(this.MKTQuantity5M);
        marketingXu5M.setUsed(this.MKTUsed5M);
        marketingXu5M.setLock(this.MKTLock5M);
        listMarketingXuFollowFace.add(marketingXu5M);
        GiftcodeFollowFaceValue marketingXu9M = new GiftcodeFollowFaceValue();
        marketingXu9M.setFaceValue(9000000);
        marketingXu9M.setQuantity(this.MKTQuantity9M);
        marketingXu9M.setUsed(this.MKTUsed9M);
        marketingXu9M.setLock(this.MKTLock9M);
        listMarketingXuFollowFace.add(marketingXu9M);
        GiftcodeFollowFaceValue marketingXu10M = new GiftcodeFollowFaceValue();
        marketingXu10M.setFaceValue(10000000);
        marketingXu10M.setQuantity(this.MKTQuantity10M);
        marketingXu10M.setUsed(this.MKTUsed10M);
        marketingXu10M.setLock(this.MKTLock10M);
        listMarketingXuFollowFace.add(marketingXu10M);
        marketingXu.setName("MarketingXu");
        marketingXu.setTrans(listMarketingXuFollowFace);
        listResponse.add(marketingXu);
        GiftcodeStatisticObj vanHanhVin = new GiftcodeStatisticObj();
        ArrayList<GiftcodeFollowFaceValue> listVanHanhVinFollowFace = new ArrayList<GiftcodeFollowFaceValue>();
        GiftcodeFollowFaceValue vanHanhVin10K = new GiftcodeFollowFaceValue();
        vanHanhVin10K.setFaceValue(10000);
        vanHanhVin10K.setQuantity(this.VHQuantity10K);
        vanHanhVin10K.setUsed(this.VHUsed10K);
        vanHanhVin10K.setLock(this.VHLock10K);
        listVanHanhVinFollowFace.add(vanHanhVin10K);
        GiftcodeFollowFaceValue vanHanhVin20K = new GiftcodeFollowFaceValue();
        vanHanhVin20K.setFaceValue(20000);
        vanHanhVin20K.setQuantity(this.VHQuantity20K);
        vanHanhVin20K.setUsed(this.VHUsed20K);
        vanHanhVin20K.setLock(this.VHLock20K);
        listVanHanhVinFollowFace.add(vanHanhVin20K);
        GiftcodeFollowFaceValue vanHanhVin50K = new GiftcodeFollowFaceValue();
        vanHanhVin50K.setFaceValue(50000);
        vanHanhVin50K.setQuantity(this.VHQuantity50K);
        vanHanhVin50K.setUsed(this.VHUsed50K);
        vanHanhVin50K.setLock(this.VHLock50K);
        listVanHanhVinFollowFace.add(vanHanhVin50K);
        GiftcodeFollowFaceValue vanHanhVin100K = new GiftcodeFollowFaceValue();
        vanHanhVin100K.setFaceValue(100000);
        vanHanhVin100K.setQuantity(this.VHQuantity100K);
        vanHanhVin100K.setUsed(this.VHUsed100K);
        vanHanhVin100K.setLock(this.VHLock100K);
        listVanHanhVinFollowFace.add(vanHanhVin100K);
        GiftcodeFollowFaceValue vanHanhVin200K = new GiftcodeFollowFaceValue();
        vanHanhVin200K.setFaceValue(200000);
        vanHanhVin200K.setQuantity(this.VHQuantity200K);
        vanHanhVin200K.setUsed(this.VHUsed200K);
        vanHanhVin200K.setLock(this.VHLock200K);
        listVanHanhVinFollowFace.add(vanHanhVin200K);
        GiftcodeFollowFaceValue vanHanhVin500K = new GiftcodeFollowFaceValue();
        vanHanhVin500K.setFaceValue(500000);
        vanHanhVin500K.setQuantity(this.VHQuantity500K);
        vanHanhVin500K.setUsed(this.VHUsed500K);
        vanHanhVin500K.setLock(this.VHLock500K);
        listVanHanhVinFollowFace.add(vanHanhVin500K);
        vanHanhVin.setName("VanHanhVin");
        vanHanhVin.setTrans(listVanHanhVinFollowFace);
        listResponse.add(vanHanhVin);
        GiftcodeStatisticObj vanHanhXu = new GiftcodeStatisticObj();
        ArrayList<GiftcodeFollowFaceValue> listVanHanhXuFollowFace = new ArrayList<GiftcodeFollowFaceValue>();
        GiftcodeFollowFaceValue vanHanhXu1M = new GiftcodeFollowFaceValue();
        vanHanhXu1M.setFaceValue(1000000);
        vanHanhXu1M.setQuantity(this.VHQuantity1M);
        vanHanhXu1M.setUsed(this.VHUsed1M);
        vanHanhXu1M.setLock(this.VHLock1M);
        listVanHanhXuFollowFace.add(vanHanhXu1M);
        GiftcodeFollowFaceValue vanHanhXu3M = new GiftcodeFollowFaceValue();
        vanHanhXu3M.setFaceValue(3000000);
        vanHanhXu3M.setQuantity(this.VHQuantity3M);
        vanHanhXu3M.setUsed(this.VHUsed3M);
        vanHanhXu3M.setLock(this.VHLock3M);
        listVanHanhXuFollowFace.add(vanHanhXu3M);
        GiftcodeFollowFaceValue vanHanhXu5M = new GiftcodeFollowFaceValue();
        vanHanhXu5M.setFaceValue(5000000);
        vanHanhXu5M.setQuantity(this.VHQuantity5M);
        vanHanhXu5M.setUsed(this.VHUsed5M);
        vanHanhXu5M.setLock(this.VHLock5M);
        listVanHanhXuFollowFace.add(vanHanhXu5M);
        GiftcodeFollowFaceValue vanHanhXu9M = new GiftcodeFollowFaceValue();
        vanHanhXu9M.setFaceValue(9000000);
        vanHanhXu9M.setQuantity(this.VHQuantity9M);
        vanHanhXu9M.setUsed(this.VHUsed9M);
        vanHanhXu9M.setLock(this.VHLock9M);
        listVanHanhXuFollowFace.add(vanHanhXu9M);
        GiftcodeFollowFaceValue vanHanhXu10M = new GiftcodeFollowFaceValue();
        vanHanhXu10M.setFaceValue(10000000);
        vanHanhXu10M.setQuantity(this.VHQuantity10M);
        vanHanhXu10M.setUsed(this.VHUsed10M);
        vanHanhXu10M.setLock(this.VHLock10M);
        listVanHanhXuFollowFace.add(vanHanhXu10M);
        vanHanhXu.setName("VanHanhXu");
        vanHanhXu.setTrans(listVanHanhXuFollowFace);
        listResponse.add(vanHanhXu);
        return listResponse;
    }

    @Override
    public List<ReportGiftCodeResponse> ToolReportGiftCode(String nickName, String source, String timeStart, String timeEnd, String moneyType, String timeType, String block) throws SQLException, ParseException, JsonProcessingException {
        ArrayList<ReportGiftCodeResponse> results = new ArrayList<ReportGiftCodeResponse>();
        final ArrayList<GiftCodeResponse> giftcoderes = new ArrayList();
        BasicDBObject obj = new BasicDBObject();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        String dateEnd = null;
        ArrayList<Long> cntlogin = new ArrayList<Long>();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (block != null && !block.equals("")) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            if (timeType.equals("1")) {
                conditions.put("create_time", obj);
            } else {
                conditions.put("update_time", obj);
            }
        }
        conditions.put("money_type", Integer.parseInt(moneyType));
        FindIterable iterable = null;
        iterable = db.getCollection("gift_code").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                GiftCodeResponse giftcode = new GiftCodeResponse();
                giftcode.giftCode = document.getString("giftcodefull");
                giftcode.source = document.getString("source");
                giftcoderes.add(giftcode);
            }
        });
        String giftCode = "";
        String Source = "";
        for (GiftCodeResponse strgiftCode : giftcoderes) {
            if (strgiftCode.giftCode.isEmpty()) continue;
            giftCode = giftCode + strgiftCode.giftCode + ",";
            Source = Source + strgiftCode.source + ",";
        }
        String stime = null;
        stime = timeStart.isEmpty() ? VinPlayUtils.getCurrentDateMarketing() : timeStart;
        HashMap<String, Object> conditions2 = new HashMap<String, Object>();
//        HashMap<String, String> conditions2 = new HashMap<String, String>();
        conditions2.put("user_name", nickName);
        BasicDBObject obj2 = new BasicDBObject();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(format.parse(stime));
        long count = 0L;
        for (int i = 0; i <= 30; ++i) {
            if (i == 0) {
                cal1.add(5, i);
                dateEnd = format.format(cal1.getTime());
                obj2.put("$gte", stime);
                obj2.put("$lte", dateEnd);
                conditions2.put("time_login", obj2);
                count = db.getCollection("login_daily_marketing").count((Bson)new Document(conditions2));
                cntlogin.add(count);
            }
            if (i == 5) {
                cal1.add(5, i);
                dateEnd = format.format(cal1.getTime());
                obj2.put("$gte", stime);
                obj2.put("$lte", dateEnd);
                conditions2.put("time_login", obj2);
                count = db.getCollection("login_daily_marketing").count((Bson)new Document(conditions2));
                cntlogin.add(count);
            }
            if (i != 30) continue;
            cal1.add(5, i);
            dateEnd = format.format(cal1.getTime());
            obj2.put("$gte", stime);
            obj2.put("$lte", dateEnd);
            conditions2.put("time_login", obj2);
            count = db.getCollection("login_daily_marketing").count((Bson)new Document(conditions2));
            cntlogin.add(count);
        }
        UserServiceImpl service = new UserServiceImpl();
        long doanhthu = service.getTotalRechargeMoney(nickName);
        long fee = doanhthu * 2L / 100L;
        int k = 1;
        HashMap<String, Long> conditionslogin = new HashMap<String, Long>();
        for (Long clg : cntlogin) {
            conditionslogin.put("A" + k, clg);
            ++k;
        }
        TreeMap treeMap = new TreeMap(conditionslogin);
        String LoginDay = new ObjectMapper().writeValueAsString(treeMap);
        ReportGiftCodeResponse reportgiftcode = new ReportGiftCodeResponse();
        reportgiftcode.loginDay = LoginDay;
        reportgiftcode.fee = String.valueOf(fee);
        reportgiftcode.nickName = nickName;
        reportgiftcode.totalMoney = String.valueOf(doanhthu);
        reportgiftcode.giftCodeUse = giftCode;
        reportgiftcode.giftCodeSource = Source;
        results.add(reportgiftcode);
        return results;
    }

    @Override
    public List<String> ListAllPrice(int moneyType) throws SQLException {
        ArrayList<String> lstprice = new ArrayList<String>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");){
            String sql = "SELECT price FROM price_giftcode where money_type = ?";
            PreparedStatement stmt = conn.prepareStatement("SELECT price FROM price_giftcode where money_type = ?");
            stmt.setInt(1, moneyType);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lstprice.add(rs.getString("price"));
            }
            rs.close();
            stmt.close();
        }
        return lstprice;
    }

    @Override
    public GiftCodeCountResponse countGiftCodeByPriceAdmin(String price, String timeStart, String timeEnd, String moneyType, String type, String block) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("update_time", -1);
        conditions.put("money_type", Integer.parseInt(moneyType));
        if (price != null && !price.equals("")) {
            conditions.put("price", price);
        }
        if (block != null && !block.equals("")) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", type);
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("create_time", obj);
        }
        long quantity = db.getCollection("gift_code_store").count((Bson)new Document(conditions));
        long giftcodeuse = db.getCollection("gift_code_store").count((Bson)new Document(conditions).append("count_use", 1));
        long remain = quantity - giftcodeuse;
        GiftCodeCountResponse response = new GiftCodeCountResponse();
        response.giftCodeUse = giftcodeuse;
        response.remain = remain;
        response.quantity = quantity;
        response.price = price;
        return response;
    }

    @Override
    public long countsearchAllGiftCode(String nickName, String giftcode, String price, String source, String timeStart, String timeEnd, String moneyType, String usegift, String type, String release, String timeType, String block) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        conditions.put("money_type", Integer.parseInt(moneyType));
        if (price != null && !price.equals("")) {
            conditions.put("price", price);
        }
        if (block != null && !block.equals("")) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        if (release != null && !release.equals("")) {
            conditions.put("release", release);
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            if (timeType.equals("1")) {
                conditions.put("create_time", obj);
            } else {
                conditions.put("update_time", obj);
            }
        }
        if (usegift != null && !usegift.equals("")) {
            conditions.put("count_use", Integer.parseInt(usegift));
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", type);
        }
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        if (giftcode != null && !giftcode.equals("")) {
            conditions.put("giftcode", giftcode);
        }
        return db.getCollection("gift_code").count((Bson)new Document(conditions));
    }

    @Override
    public long countsearchAllGiftCodeAdmin(String price, String source, String timeStart, String timeEnd, String moneyType, String usegift, String block) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        conditions.put("money_type", Integer.parseInt(moneyType));
        if (price != null && !price.equals("")) {
            conditions.put("price", price);
        }
        if (block != null && !block.equals("")) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (usegift != null && !usegift.equals("")) {
            conditions.put("count_use", Integer.parseInt(usegift));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("create_time", obj);
        }
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        return db.getCollection("gift_code_store").count((Bson)new Document(conditions));
    }

    @Override
    public List<ReportGiftCodeResponse> ToolReportGiftCodeBySource(String source, String timeStart, String timeEnd, String moneyType, String type, int page, String timeType, String block) throws SQLException, ParseException, JsonProcessingException {
        ArrayList<ReportGiftCodeResponse> results = new ArrayList<ReportGiftCodeResponse>();
        final ArrayList<GiftCodeResponse> giftcoderes = new ArrayList();
        FindIterable iterable = null;
        BasicDBObject obj = new BasicDBObject();
        int num_start = (page - 1) * 50;
        int num_end = 50;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        if (type != null && !type.equals("")) {
            conditions.put("type", type);
        }
        if (block != null && !block.equals("")) {
            conditions.put("block", Integer.parseInt(block));
        }
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            if (timeType.equals("1")) {
                conditions.put("create_time", obj);
            } else {
                conditions.put("update_time", obj);
            }
        }
        conditions.put("money_type", Integer.parseInt(moneyType));
        conditions.put("count_use", 1);
        iterable = db.getCollection("gift_code").find((Bson)new Document(conditions)).skip(num_start).limit(50);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                GiftCodeResponse giftcode = new GiftCodeResponse();
                giftcode.giftCode = document.getString("giftcodefull");
                giftcode.source = document.getString("source");
                giftcode.nickName = document.getString("nick_name");
                giftcoderes.add(giftcode);
            }
        });
        String giftCode = "";
        String nickName = "";
        String giftsource = "";
        String money = "";
        String fee = "";
        UserServiceImpl service = new UserServiceImpl();
        for (GiftCodeResponse strgiftCode : giftcoderes) {
            giftCode = giftCode + strgiftCode.giftCode + ",";
            nickName = nickName + strgiftCode.nickName + ",";
            giftsource = giftsource + strgiftCode.source + ",";
            money = money + service.getTotalRechargeMoney(strgiftCode.nickName) + ",";
            fee = fee + service.getTotalRechargeMoney(strgiftCode.nickName) * 2L / 100L + ",";
        }
        ReportGiftCodeResponse reportgiftcode = new ReportGiftCodeResponse();
        reportgiftcode.nickName = nickName;
        reportgiftcode.totalMoney = money;
        reportgiftcode.giftCodeUse = giftCode;
        reportgiftcode.giftCodeSource = giftsource;
        reportgiftcode.fee = fee;
        results.add(reportgiftcode);
        return results;
    }

    @Override
    public String uploadFileGiftCode(String lstnickName, long vin, long xu) {
        int userId = 0;
        boolean check = true;
        String lstnn = "";
        String[] myData = lstnickName.split(",");
        UserServiceImpl userService = new UserServiceImpl();
        try {
            String[] arrstring = myData;
            int n = arrstring.length;
            for (int i = 0; i < n; ++i) {
                UserDaoImpl userDao = new UserDaoImpl();
                String nickName = arrstring[i];
                userId = userDao.getIdByNickname(nickName);
                if (userId != 0) continue;
                check = false;
                lstnn = lstnn + nickName + ",";
            }
            if (!check) {
                return lstnn;
            }
            for (String nickName : myData) {
                if (vin > 0L) {
                    userService.updateMoneyFromAdmin(nickName, vin, "vin", "GiftCode", "", "T\u1eb7ng gift code" + vin + " vin");
                }
                if (xu <= 0L) continue;
                userService.updateMoneyFromAdmin(nickName, xu, "xu", "GiftCode", "", "T\u1eb7ng gift code" + xu + " xu");
            }
        }
        catch (SQLException e) {
            logger.debug(e);
        }
        return "success";
    }

    @Override
    public boolean RestoreGiftCode(String price, String source, String giftcodeList, String release) {
        String[] split;
        boolean isSuccess = false;
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        String[] giftcodeSplit = split = giftcodeList.split(",");
        for (String giftcode : split) {
            HashMap<String, Object> conditions = new HashMap<String, Object>();
            conditions.put("giftcodefull", giftcode);
            conditions.put("count_use", 0);
            if (db.getCollection("gift_code").count((Bson)new Document(conditions)) > 0L) {
                isSuccess = true;
            }
            FindIterable iterable = db.getCollection("gift_code").find((Bson)new Document(conditions));
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    try {
                        GiftCodeMessage message = new GiftCodeMessage();
                        message.setGiftCode(document.getString("giftcode"));
                        MessageBusFactory.get("queue_gift_code").publish("queue_gift_code", (BaseMessage)message, (int)1201);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return isSuccess;
    }

    @Override
    public List<GiftCodeResponse> searchAllGiftCodeByNickName(String nickName, int page) {
        final ArrayList<GiftCodeResponse> results = new ArrayList<GiftCodeResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        BasicDBObject objsort = new BasicDBObject();
        int num_start = (page - 1) * 50;
        int num_end = 50;
        objsort.put("_id", -1);
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        FindIterable iterable = db.getCollection("gift_code").find((Bson)new Document(conditions)).skip(num_start).limit(50).sort((Bson)objsort);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                GiftCodeResponse giftcode = new GiftCodeResponse();
                giftcode.price = document.getString("price");
                giftcode.source = document.getString("source");
                giftcode.quantity = document.getInteger("quantity").intValue();
                giftcode.giftCode = document.getString("giftcodefull");
                giftcode.createTime = document.getString("create_time");
                giftcode.updateTime = document.getString("update_time");
                giftcode.useGiftCode = document.getInteger("count_use");
                giftcode.nickName = document.getString("nick_name");
                results.add(giftcode);
            }
        });
        return results;
    }

    @Override
    public long countAllGiftCodeByNickName(String nickName) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        if (nickName != null && !nickName.equals("")) {
            conditions.put("nick_name", nickName);
        }
        long record = db.getCollection("gift_code").count((Bson)new Document(conditions));
        return record;
    }

    @Override
    public GiftCodeByNickNameResponse getUserInfoByGiftCode(final String giftCode, IMap<String, UserCacheModel> userMap, int page, int totalRecord) {
        final GiftCodeByNickNameResponse result = new GiftCodeByNickNameResponse();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        final UserServiceImpl service = new UserServiceImpl();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        BasicDBObject objsort = new BasicDBObject();
        int num_start = (page - 1) * totalRecord;
        objsort.put("_id", -1);
        if (giftCode != null && !giftCode.equals("")) {
            conditions.put("giftcodefull", giftCode);
            FindIterable iterable = db.getCollection("gift_code").find((Bson)new Document(conditions)).skip(num_start).limit(totalRecord).sort((Bson)objsort);
            iterable.forEach((Block)new Block<Document>(){

                public void apply(Document document) {
                    String nickName;
                    result.giftcode = giftCode;
                    result.nickName = nickName = document.getString("nick_name");
                    try {
                        if (nickName != null && !nickName.equals("")) {
                            UserModel users = service.getUserByNickName(nickName);
                            result.totalRecharge = users.getRechargeMoney();
                            result.phone = users.getMobile();
                            result.createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(users.getCreateTime());
                        } else {
                            result.nickName = null;
                        }
                    }
                    catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return result;
    }

    @Override
    public GiftCodeDeleteResponse DeleteGiftCode(String timeStart, String timeEnd, String source, String price) {
        GiftCodeDeleteResponse giftcode = new GiftCodeDeleteResponse();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        BasicDBObject obj = new BasicDBObject();
        conditions.put("agent", "0");
        conditions.put("count_use", 0);
        if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
            obj.put("$gte", timeStart);
            obj.put("$lte", timeEnd);
            conditions.put("create_time", obj);
        }
        if (source != null && !source.equals("")) {
            conditions.put("source", source);
        }
        if (price != null && !price.equals("")) {
            conditions.put("price", price);
        }
        giftcode.countGiftCode = db.getCollection("gift_code").count((Bson)new Document(conditions));
        FindIterable iterable = db.getCollection("gift_code").find((Bson)new Document(conditions));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                try {
                    GiftCodeMessage message = new GiftCodeMessage();
                    message.setGiftCode(document.getString("giftcode"));
                    MessageBusFactory.get("queue_gift_code").publish("queue_gift_code", (BaseMessage)message, (int)1201);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return giftcode;
    }

}

