package com.vinplay.vbee.dao.impl;

/**
 * SUN-1340 (2026-05-16): rewrite of the decompiled-CFR source to use
 * try-with-resources on every JDBC call. The original file had no finally
 * blocks — any SQLException thrown between getConnection() and the manual
 * close() at the end of the method leaked the Connection permanently.
 * All 8 JDBC methods are rewritten; the 2 Mongo-only methods (logTanLoc,
 * logRutLoc) are unchanged.
 */

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.minigame.LogRutLocMessge;
import com.vinplay.vbee.common.messages.minigame.LogTanLocMessage;
import com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.ThanhDuMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateFundMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage;
import com.vinplay.vbee.common.messages.minigame.UpdatePotMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.dao.TaiXiuDao;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bson.Document;

public class TaiXiuDaoImpl
implements TaiXiuDao {
    @Override
    public boolean saveResultTaiXiu(ResultTaiXiuMessage message) throws SQLException {
        System.out.println("TAIXIUDEBUG TaiXiuDaoImpl saveResultTaiXiu: " + message.referenceId);
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("{CALL save_result_tai_xiu(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
            int param = 1;
            call.setLong(param++, message.referenceId);
            call.setByte(param++, (byte)message.result);
            call.setByte(param++, (byte)message.dice1);
            call.setByte(param++, (byte)message.dice2);
            call.setByte(param++, (byte)message.dice3);
            call.setLong(param++, message.totalTai);
            call.setLong(param++, message.totalXiu);
            call.setInt(param++, message.numBetTai);
            call.setInt(param++, message.numBetXiu);
            call.setLong(param++, message.totalPrize);
            call.setLong(param++, message.totalRefundTai);
            call.setLong(param++, message.totalRefundXiu);
            call.setLong(param++, message.totalRevenue);
            call.setByte(param++, (byte)message.moneyType);
            call.setLong(param++, message.totalJackpot);
            call.execute();
            return true; // SP returns no ResultSet — call.execute() returns false even on success
        }
    }

    @Override
    public boolean saveTransactionTaiXiu(TransactionTaiXiuMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL save_transaction_tai_xiu(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int param = 1;
            call.setLong(param++, message.referenceId);
            call.setInt(param++, message.userId);
            call.setString(param++, message.username);
            call.setLong(param++, message.betValue);
            call.setByte(param++, (byte)message.betSide);
            call.setLong(param++, message.prize);
            call.setLong(param++, message.refund);
            call.setLong(param++, message.prize - message.betValue); // p_total_exchange = net win/loss
            call.setByte(param++, (byte)message.moneyType);
            call.execute();
            return true; // SP returns no ResultSet — call.execute() returns false even on success
        }
    }

    @Override
    public boolean saveTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL save_transaction_detail_tai_xiu(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int param = 1;
            call.setLong(param++, message.referenceId);
            call.setString(param++, message.transactionCode);
            call.setInt(param++, message.userId);
            call.setString(param++, message.username);
            call.setLong(param++, message.betValue);
            call.setInt(param++, message.betSide);
            call.setLong(param++, message.prize);
            call.setLong(param++, message.refund);
            call.setInt(param++, message.inputTime);
            call.setByte(param++, (byte)message.moneyType);
            call.setLong(param++, message.jackpot);
            call.execute();
            return true; // SP returns no ResultSet — call.execute() returns false even on success
        }
    }

    @Override
    public boolean updateTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL update_transaction_tai_xiu_detail(?, ?, ?)")) {
            int param = 1;
            call.setString(param++, message.transactionCode);
            call.setLong(param++, message.prize);
            call.setLong(param++, message.refund);
            call.execute();
            return true; // SP returns no ResultSet — call.execute() returns false even on success
        }
    }

    @Override
    public boolean updateThanhDu(ThanhDuMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL tx_update_thanh_du(?, ?, ?, ?, ?, ?)")) {
            int param = 1;
            call.setString(param++, message.getUsername());
            call.setInt(param++, message.getNumber());
            call.setLong(param++, message.getTotalValue());
            call.setLong(param++, message.getCurrentReferenceId());
            call.setString(param++, message.getReferences());
            call.setByte(param++, (byte)message.getType());
            call.execute();
            return true; // SP returns no ResultSet — call.execute() returns false even on success
        }
    }

    @Override
    public boolean updatePot(UpdatePotMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL update_pot_md5(?, ?)")) {
            int param = 1;
            call.setString(param++, message.potName);
            call.setLong(param++, message.newValue);
            call.execute();
            return true; // SP returns no ResultSet — call.execute() returns false even on success
        }
    }

    @Override
    public void logTanLoc(LogTanLocMessage message) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("tan_loc");
        Document doc = new Document();
        doc.append("user_name", (Object)message.username);
        doc.append("money", (Object)message.value);
        doc.append("time_log", (Object)timeLog);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public void logRutLoc(LogRutLocMessge message) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeLog = df.format(new Date());
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("rut_loc");
        Document doc = new Document();
        doc.append("user_name", (Object)message.username);
        doc.append("money", (Object)message.prize);
        doc.append("time_request", (Object)message.timeRequest);
        doc.append("current_fund", (Object)message.currentFund);
        doc.append("time_log", (Object)timeLog);
        doc.append("create_time", (Object)new Date());
        col.insertOne((Object)doc);
    }

    @Override
    public boolean updateLuotRutLoc(UpdateLuotRutLocMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL update_luot_rut_loc(?, ?)")) {
            int param = 1;
            call.setString(param++, message.username);
            call.setLong(param++, message.soLuotThem);
            call.execute();
            return true; // SP returns no ResultSet — call.execute() returns false even on success
        }
    }

    @Override
    public boolean updateFund(UpdateFundMessage message) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL update_fund(?, ?)")) {
            int param = 1;
            call.setString(param++, message.fundName);
            call.setLong(param++, message.newValue);
            call.execute();
            return true; // SP returns no ResultSet — call.execute() returns false even on success
        }
    }
}
