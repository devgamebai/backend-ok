/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.response.GiftCodeMachineMessage
 *  com.vinplay.vbee.common.response.GiftCodeUpdateResponse
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.usercore.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.usercore.dao.GiftCodeMachineDAO;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.response.GiftCodeMachineMessage;
import com.vinplay.vbee.common.response.GiftCodeUpdateResponse;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class GiftCodeMachineDAOImpl
implements GiftCodeMachineDAO {
    private UserModel usermodel = null;

    @Override
    public boolean exportGiftCodeMachine(GiftCodeMachineMessage msg) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
        conditions.put("giftcode", msg.GiftCode);
        conditions.put("price", msg.Price);
        conditions.put("source", msg.Source);
        long count = db.getCollection("gift_code_machine").count((Bson)new Document(conditions));
        MongoCollection col = db.getCollection("gift_code_machine");
        if (count > 0L) {
            col.updateOne((Bson)new Document("giftcode", msg.GiftCode), (Bson)new Document("$set", new Document("price", msg.Price).append("source", msg.Source)));
        } else {
            Document doc = new Document();
            doc.append("giftcode", msg.GiftCode);
            doc.append("price", msg.Price);
            doc.append("quantity", msg.Quantity);
            doc.append("source", msg.Source);
            doc.append("count_use", 0);
            doc.append("create_time", timeLog);
            doc.append("tranfer", 0);
            col.insertOne(doc);
        }
        return true;
    }

    @Override
    public GiftCodeUpdateResponse updateGiftCode(String nickName, String giftCode) throws SQLException {
        GiftCodeUpdateResponse response = new GiftCodeUpdateResponse(false, "1001");
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        long currentMoneyVin = 0L;
        long money = 1000000L;
        UserServiceImpl userService = new UserServiceImpl();
        this.usermodel = userService.getUserByNickName(nickName);
        if (this.usermodel != null) {
            currentMoneyVin = this.usermodel.getVinTotal();
        }
//        HashMap<String, String> conditions = new HashMap<String, String>();
        HashMap<String, Object> conditions = new HashMap<String, Object>();
        conditions.put("nick_name", nickName);
        MongoCollection col = db.getCollection("gift_code_machine_use");
        long count = db.getCollection("gift_code_machine").count((Bson)new Document(conditions));
        if (count > 0L) {
            response.setErrorCode("10001");
            return response;
        }
        long count2 = db.getCollection("gift_code_machine_use").count((Bson)new Document(conditions));
        if (count2 > 0L) {
            response.setErrorCode("10002");
            return response;
        }
        Document doc = new Document();
        doc.append("giftcode", giftCode);
        doc.append("nick_name", nickName);
        col.insertOne(doc);
        MoneyResponse mnres = userService.updateMoney(nickName, 1000000L, "0", "GiftCode", "GiftCode", "M\u00e3: " + giftCode, 0L, null, TransType.NO_VIPPOINT);
        if (mnres != null && mnres.isSuccess()) {
            response.currentMoneyVin = currentMoneyVin;
            response.currentMoneyXu = mnres.getCurrentMoney();
            response.setErrorCode("0");
            response.setSuccess(true);
        }
        return response;
    }
}

