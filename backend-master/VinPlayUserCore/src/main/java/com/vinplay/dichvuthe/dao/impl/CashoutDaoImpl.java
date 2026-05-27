/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dichvuthe.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.vinplay.dichvuthe.dao.CashoutDao;
import com.vinplay.dichvuthe.entities.BankAccountInfo;
import com.vinplay.dichvuthe.response.CashoutTransResponse;
import com.vinplay.dichvuthe.response.CashoutUserDailyResponse;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.messages.dvt.CashoutByBankMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class CashoutDaoImpl
implements CashoutDao {
    @Override
    public long getSystemCashout() throws SQLException {
        long money = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT money FROM system_cashout WHERE date=?";
            PreparedStatement stm = conn.prepareStatement("SELECT money FROM system_cashout WHERE date=?");
            stm.setString(1, VinPlayUtils.getCurrentDate());
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                money = rs.getLong("money");
            }
            rs.close();
            stm.close();
        }
        return money;
    }

    @Override
    public boolean updateSystemCashout(long money) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "INSERT INTO system_cashout(date, money, update_time) VALUES(?, ?, now()) ON DUPLICATE KEY UPDATE money=money+?, update_time=now()";
            PreparedStatement stm = conn.prepareStatement("INSERT INTO system_cashout(date, money, update_time) VALUES(?, ?, now()) ON DUPLICATE KEY UPDATE money=money+?, update_time=now()");
            stm.setString(1, VinPlayUtils.getCurrentDate());
            stm.setLong(2, money);
            stm.setLong(3, money);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }
        return res;
    }

    @Override
    public void logCashoutByBank(CashoutByBankMessage message) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("dvt_cash_out_by_bank");
        Document doc = new Document();
        doc.append("reference_id", message.getReferenceId());
        doc.append("nick_name", message.getNickname());
        doc.append("bank", message.getBank());
        doc.append("account", message.getAccount());
        doc.append("name", message.getName());
        doc.append("amount", message.getAmount());
        doc.append("status", message.getStatus());
        doc.append("message", message.getMessage());
        doc.append("sign", message.getSign());
        doc.append("code", message.getCode());
        doc.append("description", message.getDesc());
        doc.append("time_log", message.getCreateTime());
        doc.append("update_time", message.getCreateTime());
        col.insertOne(doc);
    }

    @Override
    public BankAccountInfo getBankAccountInfo(String nickname) throws SQLException {
        BankAccountInfo info = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");){
            String sql = "SELECT * FROM useragent WHERE nickname=? AND parentid = -1 AND active = 1";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM useragent WHERE nickname=? AND parentid = -1 AND active = 1");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                String bank = rs.getString("namebank");
                String name = rs.getString("nameaccount");
                String account = rs.getString("numberaccount");
                info = new BankAccountInfo(bank, name, account);
            }
            rs.close();
            stm.close();
        }
        return info;
    }

    @Override
    public CashoutUserDailyResponse getCashoutUserToday(String nickname) {
        CashoutUserDailyResponse model = new CashoutUserDailyResponse(new Date(), 0);
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            MongoCollection col = db.getCollection("log_cash_out_user_daily");
            if (col != null) {
                HashMap<String, Object> conditions = new HashMap<String, Object>();
//                HashMap<String, String> conditions = new HashMap<String, String>();
                conditions.put("nick_name", nickname);
                conditions.put("date", VinPlayUtils.getCurrentDate());
                Document doccument = (Document)col.find((Bson)new Document(conditions)).first();
                if (doccument != null) {
                    model = new CashoutUserDailyResponse(VinPlayUtils.getDateTimeFromDate((String)doccument.getString("date")), doccument.getInteger("money"));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }

    @Override
    public List<CashoutTransResponse> getListCashoutByCardPending(String partner, String startTime, String endTime) throws Exception {
        final ArrayList<CashoutTransResponse> response = new ArrayList<CashoutTransResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("code", 30);
        conditions.put("partner", partner);
        if (!startTime.isEmpty() && !endTime.isEmpty()) {
            BasicDBObject obj = new BasicDBObject();
            obj.put("$gte", startTime);
            obj.put("$lte", endTime);
            conditions.put("time_log", obj);
        }
        iterable = db.getCollection("dvt_cash_out_by_card").find((Bson)conditions);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                CashoutTransResponse message = new CashoutTransResponse(document.getString("reference_id"), document.getString("provider"), document.getInteger("amount"), document.getInteger("quantity"), document.getString("partner"), document.getString("partner_trans_id"), document.getInteger("is_scanned"), document.getInteger("code"), document.getString("message"), document.getInteger("status"), document.getString("softpin"), document.getString("nick_name"));
                response.add(message);
            }
        });
        return response;
    }

    @Override
    public List<CashoutTransResponse> getListCashoutByCardPending() throws Exception {
        final ArrayList<CashoutTransResponse> response = new ArrayList<CashoutTransResponse>();
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        FindIterable iterable = null;
        Document conditions = new Document();
        conditions.put("code", 30);
        conditions.put("time_log", new BasicDBObject("$gte", VinPlayUtils.parseDateTimeToString((Date)VinPlayUtils.subtractDay((Date)new Date(), (int)GameCommon.getValueInt("TIME_RECHECK_CASHOUT_BY_CARD")))));
        iterable = db.getCollection("dvt_cash_out_by_card").find((Bson)conditions);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                CashoutTransResponse message = new CashoutTransResponse(document.getString("reference_id"), document.getString("provider"), document.getInteger("amount"), document.getInteger("quantity"), document.getString("partner"), document.getString("partner_trans_id"), document.getInteger("is_scanned"), document.getInteger("code"), document.getString("message"), document.getInteger("status"), document.getString("softpin"), document.getString("nick_name"));
                response.add(message);
            }
        });
        return response;
    }

    @Override
    public void updateCashOutByCard(String referenceId, String softpin, int status, String message, int code, int isScanned) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("softpin", softpin);
        updateFields.append("status", status);
        updateFields.append("message", message);
        updateFields.append("code", code);
        updateFields.append("is_scanned", isScanned);
        updateFields.append("update_time", VinPlayUtils.getCurrentDateTime());
        db.getCollection("dvt_cash_out_by_card").updateOne((Bson)new Document("reference_id", referenceId), (Bson)new Document("$set", updateFields));
    }

    @Override
    public void insertCardIntoDB(String provider, int amount, String serial, String pin, String expire) throws Exception {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("card_store");
        Document doc = new Document();
        doc.append("reference_id", VinPlayUtils.genMessageId());
        doc.append("provider", provider);
        doc.append("amount", amount);
        doc.append("serial", serial);
        doc.append("pin", pin);
        doc.append("expire_time", expire);
        doc.append("status", 0);
        doc.append("message", "Th\u00e1\u00ba\u00bb ch\u00c6\u00b0a s\u00e1\u00bb\u00ad d\u00e1\u00bb\u00a5ng");
        doc.append("create_time", VinPlayUtils.getCurrentDateTime());
        doc.append("update_time", VinPlayUtils.getCurrentDateTime());
        doc.append("partner_trans_id", "");
        col.insertOne(doc);
    }

}

