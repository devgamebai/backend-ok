package com.vinplay.vbee.dao.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.FreezeMoneyMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInGame;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.dao.MoneyInGameDao;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SUN-1340 (2026-05-17): rewrite of the decompiled-CFR source to use
 * try-with-resources on every JDBC call. The previous file (originally
 * decompiled bytecode that re-emerged into source) released the
 * Connection only on the success path — any thrown SQLException leaked
 * the Connection forever. Under sustained load with realistic error
 * volume (e.g. {@code freeze_money: insufficient balance} thrown for
 * every short-balance bet, ~50 stuck conns inside 10 min on staging)
 * the vbee {@code mysqlpoolname} pool reached maxPoolSize and every
 * downstream commission/rebate write started timing out. Rebate_logs
 * went silent for 44 hours before ops noticed because the pool
 * "exhaustion" symptom looked exactly like a temporary DB hiccup,
 * but Hikari's self-heal cannot return leaked conns to the pool —
 * the references are still alive in dead-stack frames.
 *
 * <p>Every method below now closes Connection + Statement deterministically
 * regardless of whether the stored procedure throws. SQLException is still
 * propagated so the calling RMQ consumer's existing retry/DLQ logic keeps
 * working unchanged.
 */
public class MoneyInGameDaoImpl implements MoneyInGameDao {

    @Override
    public boolean freezeMoneyInGame(FreezeMoneyMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL freeze_money(?,?,?,?,?,?,?,?,?)")) {
            int param = 1;
            call.setString(param++, message.getSessionId());
            call.setInt(param++, message.getUserId());
            call.setString(param++, message.getGameName());
            call.setString(param++, message.getRoomId());
            call.setLong(param++, message.getMoneyUse());
            call.setLong(param++, message.getMoneyTotal());
            call.setLong(param++, message.getMoney());
            call.setString(param++, message.getMoneyType());
            call.setString(param++, message.getNickname());
            call.executeUpdate();
            return true;
        }
    }

    @Override
    public boolean updateTranferAgent(String id, int isFreezeMoney, int topDsFreeze, String sessionIdFreezeMoney) throws SQLException {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("is_freeze_money", (Object) isFreezeMoney);
        updateFields.append("session_id_freeze_money", (Object) sessionIdFreezeMoney);
        db.getCollection("log_chuyen_tien_dai_ly").updateOne(
                (Bson) new Document("transaction_no", (Object) id),
                (Bson) new Document("$set", (Object) updateFields));
        return true;
    }

    @Override
    public void updateTranferAgentMySQL(String id, int isFreezeMoney, int topDsFreeze, String sessionIdFreezeMoney) throws SQLException {
        String sql = " UPDATE vinplay.log_tranfer_agent  SET is_freeze_money = ?,  session_id_freeze_money = ?,  update_time = ?  WHERE transaction_no = ? ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, isFreezeMoney);
            stmt.setString(2, sessionIdFreezeMoney);
            stmt.setString(3, DateTimeUtils.getCurrentTime((String) "yyyy-MM-dd HH:mm:ss"));
            stmt.setString(4, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public boolean restoreMoneyInGame(FreezeMoneyMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL restore_money(?,?,?,?,?,?)")) {
            int param = 1;
            call.setString(param++, message.getSessionId());
            call.setInt(param++, message.getUserId());
            call.setLong(param++, message.getMoneyUse());
            call.setLong(param++, message.getMoneyTotal());
            call.setLong(param++, message.getMoney());
            call.setString(param++, message.getMoneyType());
            call.executeUpdate();
            return true;
        }
    }

    @Override
    public boolean updateMoneyInGame(MoneyMessageInGame message) throws SQLException {
        // 11th param (p_money) added in migration 20260515_atomic_money_procs.sql so the
        // proc can do an atomic delta update (vin = vin + p_money) instead of the previous
        // blind absolute write that caused the Sunkr888 silent debit on 2026-05-15.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL update_money_in_game(?,?,?,?,?,?,?,?,?,?,?)")) {
            int param = 1;
            call.setString(param++, message.getSessionId());
            call.setInt(param++, message.getUserId());
            call.setString(param++, message.getActionName());
            call.setLong(param++, message.getAfterMoneyUse());
            call.setLong(param++, message.getAfterMoney());
            call.setLong(param++, message.getFreezeMoney());
            call.setString(param++, message.getMoneyType());
            call.setLong(param++, message.getFee());
            call.setInt(param++, message.getMoneyVP());
            call.setInt(param++, message.getVp());
            call.setLong(param++, message.getMoneyExchange());
            call.executeUpdate();
            return true;
        }
    }
}
