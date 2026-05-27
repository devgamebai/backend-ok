/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.FindOneAndUpdateOptions
 *  com.vinplay.vbee.common.messages.LogChuyenTienDaiLyMessage
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 *  com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage
 *  com.vinplay.vbee.common.messages.pay.ExchangeMessage
 *  com.vinplay.vbee.common.models.TopUserPlayGame
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.vinplay.vbee.common.messages.LogChuyenTienDaiLyMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage;
import com.vinplay.vbee.common.messages.pay.ExchangeMessage;
import com.vinplay.vbee.common.models.TopUserPlayGame;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.dao.LogMoneyUserDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class LogMoneyUserDaoImpl
        implements LogMoneyUserDao {
    @Override
    public boolean saveLogMoneyUser(LogMoneyUserMessage message, long transId, boolean isBot, boolean playGame) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        if (message.getMoneyType().equals("vin")) {
            col = db.getCollection("log_money_user_vin");
        } else if (message.getMoneyType().equals("xu")) {
            col = db.getCollection("log_money_user_xu");
        }
        UserDaoImpl userDao = new UserDaoImpl();
        String ref_code = null;
        try {
            ref_code = userDao.getRefCodeByUserId(message.getUserId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Document doc = new Document();
        doc.append("trans_id", (Object)transId);
        doc.append("user_id", (Object)message.getUserId());
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("service_name", (Object)message.getServiceName());
        doc.append("current_money", (Object)message.getCurrentMoney());
        doc.append("money_exchange", (Object)message.getMoneyExchange());
        doc.append("description", (Object)message.getDescription());
        doc.append("trans_time", (Object)message.getCreateTime());
        doc.append("action_name", (Object)message.getActionName());
        doc.append("fee", (Object)message.getFee());
        doc.append("is_bot", (Object)isBot);
        doc.append("play_game", (Object)playGame);
        doc.append("create_time", (Object)new Date());
        doc.append("ref_code", (Object)ref_code);
        col.insertOne((Object)doc);

        // SUN-OPT: removed calls to UpdateIndexsAffiliate / UpdateIndexsAffiliateMD5.
        // These stored procedures do NOT exist in the database (confirmed on staging +
        // production SQL dumps). Every call wasted a MySQL connection from mysqlpool_minigame
        // only to get SQLSyntaxErrorException, silently swallowed. Under load this drained
        // the pool and added ~1ms latency per message. If the affiliate indexing feature
        // is ever needed, create the stored procedures first then re-enable these calls.

        return true;
    }

    @Override
    public boolean saveLogMoneyUserVinOther(LogMoneyUserMessage message, long transId, int type) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        UserDaoImpl userDao = new UserDaoImpl();
        String ref_code = null;
        try {
            ref_code = userDao.getRefCodeByUserId(message.getUserId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        col = type == 3 ? db.getCollection("log_money_user_nap_vin") : db.getCollection("log_money_user_tieu_vin");
        Document doc = new Document();
        doc.append("trans_id", (Object)transId);
        doc.append("user_id", (Object)message.getUserId());
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("service_name", (Object)message.getServiceName());
        doc.append("current_money", (Object)message.getCurrentMoney());
        doc.append("money_exchange", (Object)message.getMoneyExchange());
        doc.append("description", (Object)message.getDescription());
        doc.append("trans_time", (Object)message.getCreateTime());
        doc.append("action_name", (Object)message.getActionName());
        doc.append("fee", (Object)message.getFee());
        doc.append("create_time", (Object)new Date());
        doc.append("ref_code", ref_code);
        col.insertOne((Object)doc);

        // SUN-OPT: removed UpdateIndexesAffiliate call — stored procedure doesn't exist (see above).

        return true;
    }

    @Override
    public long getLastReferenceId(String moneyType) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        HashMap conditions = new HashMap();
        BasicDBObject objsort = new BasicDBObject();
        objsort.put("_id", -1);
        FindIterable iterable = null;
        if (moneyType.equals("vin")) {
            iterable = db.getCollection("log_money_user_vin").find((Bson)new Document(conditions)).sort((Bson)objsort).limit(1);
        } else if (moneyType.equals("xu")) {
            iterable = db.getCollection("log_money_user_xu").find((Bson)new Document(conditions)).sort((Bson)objsort).limit(1);
        }
        Document document = iterable != null ? (Document)iterable.first() : null;
        long transId = document == null ? 0L : document.getLong((Object)"trans_id");
        return transId;
    }

    @Override
    public void saveLogMoneySystem(String name, long currentMoney, long moneyExchange, String transTime) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_money_system");
        Document doc = new Document();
        doc.append("name", (Object)name);
        doc.append("current_money", (Object)currentMoney);
        doc.append("money_exchange", (Object)moneyExchange);
        doc.append("trans_time", (Object)transTime);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public boolean logTopUserPlayGame(TopUserPlayGame userWin) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = null;
        col = userWin.getMoneyType().equals("vin") ? db.getCollection("top_user_play_game_vin") : db.getCollection("top_user_play_game_xu");
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("money_win", (Object)userWin.getMoneyWin());
        BasicDBObject conditions = new BasicDBObject();
        conditions.append("nick_name", (Object)userWin.getNickname());
        conditions.append("money_type", (Object)userWin.getMoneyType());
        conditions.append("date", (Object)userWin.getDate());
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(true);
        col.findOneAndUpdate((Bson)conditions, (Bson)new Document("$inc", (Object)updateFields), options);
        return true;
    }

    @Override
    public boolean logChuyenTienDaiLy(LogChuyenTienDaiLyMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_chuyen_tien_dai_ly");
        Document doc = new Document();
        doc.append("nick_name_send", (Object)message.getNicknameSend());
        doc.append("nick_name_receive", (Object)message.getNicknameReceive());
        doc.append("money_send", (Object)message.getMoneySend());
        doc.append("money_receive", (Object)message.getMoneyReceive());
        doc.append("status", (Object)message.getStatus());
        doc.append("fee", (Object)message.getFee());
        doc.append("trans_time", (Object)message.getTransTime());
        doc.append("top_ds", (Object)1);
        doc.append("process", (Object)0);
        doc.append("des_send", (Object)message.getDesSend());
        doc.append("des_receive", (Object)message.getDesReceive());
        doc.append("process", (Object)0);
        doc.append("create_time", (Object)new Date());
        doc.append("transaction_no", (Object)message.getTransactionId());
        doc.append("is_freeze_money", (Object)message.getIsFreezeMoney());
        doc.append("agent_level1", (Object)message.getAgentLevel1());
        doc.append("session_id_freeze_money", (Object)message.getSessionIdFreezeMoney());
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public void logChuyenTienDaiLyMySQL(LogChuyenTienDaiLyMessage message) throws SQLException {
        String sql = " INSERT INTO vinplay.log_tranfer_agent  (  transaction_no,  agent_level1,  nick_name_send,  nick_name_receive,  money_send,  money_receive,  status,  fee,  top_ds,  process,  ti_gia,  is_freeze_money,  des_send,  des_receive,  session_id_freeze_money,  trans_time,  update_time  )  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(" INSERT INTO vinplay.log_tranfer_agent  (  transaction_no,  agent_level1,  nick_name_send,  nick_name_receive,  money_send,  money_receive,  status,  fee,  top_ds,  process,  ti_gia,  is_freeze_money,  des_send,  des_receive,  session_id_freeze_money,  trans_time,  update_time  )  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
            stmt.setString(1, message.getTransactionId());
            stmt.setString(2, message.getAgentLevel1());
            stmt.setString(3, message.getNicknameSend());
            stmt.setString(4, message.getNicknameReceive());
            stmt.setLong(5, message.getMoneySend());
            stmt.setLong(6, message.getMoneyReceive());
            stmt.setInt(7, message.getStatus());
            stmt.setLong(8, message.getFee());
            stmt.setInt(9, 1);
            stmt.setInt(10, 0);
            stmt.setInt(11, 0);
            stmt.setInt(12, message.getIsFreezeMoney());
            stmt.setString(13, message.getDesSend());
            stmt.setString(14, message.getDesReceive());
            stmt.setString(15, message.getSessionIdFreezeMoney());
            stmt.setString(16, message.getTransTime());
            stmt.setString(17, DateTimeUtils.getCurrentTime((String)"yyyy-MM-dd HH:mm:ss"));
            stmt.executeUpdate();
            stmt.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean logNoHuGameBai(LogNoHuGameBaiMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_no_hu_game_bai");
        Document doc = new Document();
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("room", (Object)message.getRoom());
        doc.append("pot_value", (Object)message.getPotValue());
        doc.append("money_win", (Object)message.getMoneyWin());
        doc.append("game_name", (Object)message.getGamename());
        doc.append("description", (Object)message.getDescription());
        doc.append("trans_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        doc.append("tour_id", (Object)message.getTourId());
        col.insertOne((Object)doc);
        return true;
    }

    @Override
    public boolean checkBot(String nickname) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            String sql = "SELECT is_bot FROM users WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement("SELECT is_bot FROM users WHERE nick_name=?");
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next() && rs.getInt("is_bot") == 1) {
                res = true;
            }
            rs.close();
            stm.close();
        }finally{
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return res;
    }

    @Override
    public boolean logExchangeMoney(ExchangeMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_exchange_money");
        Document doc = new Document();
        doc.append("nick_name", (Object)message.nickname);
        doc.append("merchant_id", (Object)message.merchantId);
        doc.append("trans_id", (Object)message.merchantTransId);
        doc.append("money", (Object)message.money);
        doc.append("money_type", (Object)message.moneyType);
        doc.append("type", (Object)message.type);
        doc.append("money_exchange", (Object)message.exchangeMoney);
        doc.append("fee", (Object)message.fee);
        doc.append("code", (Object)message.code);
        doc.append("ip", (Object)message.ip);
        doc.append("trans_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
        return true;
    }
}

