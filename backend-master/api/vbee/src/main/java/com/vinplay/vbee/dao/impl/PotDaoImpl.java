package com.vinplay.vbee.dao.impl;

/**
 * SUN-1340 (2026-05-16): rewrite of the decompiled-CFR source to use
 * try-with-resources on every JDBC call. The original file had no finally
 * blocks — any SQLException thrown between getConnection() and the manual
 * close() at the end of the method leaked the Connection permanently.
 * All 3 JDBC methods are rewritten; the Mongo-only method (logHuGameBai)
 * is unchanged.
 */

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.NoHuMessage;
import com.vinplay.vbee.common.messages.PotMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.dao.PotDao;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.bson.Document;

public class PotDaoImpl
implements PotDao {
    @Override
    public boolean addMoneyPot(PotMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL cong_tien_hu_game_bai(?,?,?)")) {
            int param = 1;
            call.setString(param++, message.getPotName());
            call.setLong(param++, message.getValuePot());
            call.setLong(param++, message.getValuePotSystem());
            call.executeUpdate();
            return true;
        }
    }

    @Override
    public boolean nohu(NoHuMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL no_hu_game_bai(?,?,?,?,?,?,?)")) {
            int param = 1;
            call.setString(param++, message.getSessionId());
            call.setInt(param++, message.getUserId());
            call.setLong(param++, message.getAfterMoneyUse());
            call.setLong(param++, message.getAfterMoney());
            call.setLong(param++, message.getFreezeMoney());
            call.setLong(param++, message.getPotValue());
            call.setString(param++, message.getPotName());
            call.executeUpdate();
            return true;
        }
    }

    @Override
    public long getPotValue(String potName) throws SQLException {
        long value = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stm = conn.prepareStatement("SELECT value FROM hu_game_bai WHERE pot_name=?")) {
            stm.setString(1, potName);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    value = rs.getLong("value");
                }
            }
        }
        return value;
    }

    @Override
    public boolean logHuGameBai(PotMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_hu_game_bai");
        Document doc = new Document();
        doc.append("pot_name", (Object)message.getPotName());
        doc.append("money_exchange", (Object)message.getMoneyExchange());
        doc.append("value", (Object)message.getValuePot());
        doc.append("value_pot_system", (Object)message.getValuePotSystem());
        doc.append("time_log", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
        return true;
    }
}
