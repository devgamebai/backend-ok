/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.FindOneAndUpdateOptions
 *  com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage
 *  com.vinplay.vbee.common.messages.dvt.CashoutByCardMessage
 *  com.vinplay.vbee.common.messages.dvt.CashoutByTopUpMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage;
import com.vinplay.vbee.common.messages.dvt.CashoutByCardMessage;
import com.vinplay.vbee.common.messages.dvt.CashoutByTopUpMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vbee.dao.CashoutDao;
import java.text.ParseException;
import java.util.Date;
import org.bson.Document;
import org.bson.conversions.Bson;

public class CashoutDaoImpl
implements CashoutDao {
    @Override
    public void logCashoutByCard(CashoutByCardMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("dvt_cash_out_by_card");
        Document doc = new Document();
        doc.append("reference_id", (Object)message.getReferenceId());
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("provider", (Object)message.getProvider());
        doc.append("amount", (Object)message.getAmount());
        doc.append("quantity", (Object)message.getQuantity());
        doc.append("softpin", (Object)message.getSoftpin().toString());
        doc.append("status", (Object)message.getStatus());
        doc.append("message", (Object)message.getMessage());
        doc.append("sign", (Object)message.getSign());
        doc.append("code", (Object)message.getCode());
        doc.append("time_log", (Object)message.getCreateTime());
        doc.append("update_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        doc.append("partner", (Object)message.getPartner());
        doc.append("partner_trans_id", (Object)message.getPartnerTransId());
        doc.append("is_scanned", (Object)1);
        col.insertOne((Object)doc);
        if (message.getCode() == 0) {
            try {
                this.logMoneyCashoutUser(message.getNickname(), message.getAmount() * message.getQuantity(), message.getCreateTime());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void logCashoutByTopUp(CashoutByTopUpMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("dvt_cash_out_by_topup");
        Document doc = new Document();
        doc.append("reference_id", (Object)message.getReferenceId());
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("target", (Object)message.getTarget());
        doc.append("amount", (Object)message.getAmount());
        doc.append("status", (Object)message.getStatus());
        doc.append("message", (Object)message.getMessage());
        doc.append("sign", (Object)message.getSign());
        doc.append("code", (Object)message.getCode());
        doc.append("time_log", (Object)message.getCreateTime());
        doc.append("update_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        doc.append("partner", (Object)message.getPartner());
        doc.append("partner_trans_id", (Object)message.getPartnerTransId());
        doc.append("provider", (Object)message.getProvider());
        doc.append("type", (Object)message.getType());
        col.insertOne((Object)doc);
        if (message.getCode() == 0) {
            try {
                this.logMoneyCashoutUser(message.getNickname(), message.getAmount(), message.getCreateTime());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void logCashoutByBank(CashoutByBankMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("dvt_cash_out_by_bank");
        Document doc = new Document();
        doc.append("reference_id", (Object)message.getReferenceId());
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("bank", (Object)message.getBank());
        doc.append("account", (Object)message.getAccount());
        doc.append("name", (Object)message.getName());
        doc.append("amount", (Object)message.getAmount());
        doc.append("status", (Object)message.getStatus());
        doc.append("message", (Object)message.getMessage());
        doc.append("sign", (Object)message.getSign());
        doc.append("code", (Object)message.getCode());
        doc.append("description", (Object)message.getDesc());
        doc.append("time_log", (Object)message.getCreateTime());
        doc.append("update_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    private void logMoneyCashoutUser(String nickname, int money, String datetime) throws ParseException {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_cash_out_user_daily");
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("money", (Object)money);
        BasicDBObject conditions = new BasicDBObject();
        conditions.append("nick_name", (Object)nickname);
        conditions.append("date", (Object)VinPlayUtils.getDateFromDateTime((String)datetime));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(true);
        col.findOneAndUpdate((Bson)conditions, (Bson)new Document("$inc", (Object)updateFields), options);
    }
}

