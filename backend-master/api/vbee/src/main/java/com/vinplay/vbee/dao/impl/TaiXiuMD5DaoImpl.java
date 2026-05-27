/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.minigame.LogRutLocMessge
 *  com.vinplay.vbee.common.messages.minigame.LogTanLocMessage
 *  com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage
 *  com.vinplay.vbee.common.messages.minigame.ThanhDuMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateFundMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdatePotMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.minigame.*;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.dao.TaiXiuDao;
import org.bson.Document;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TaiXiuMD5DaoImpl
implements TaiXiuDao {
    @Override
    public boolean saveResultTaiXiu(ResultTaiXiuMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("{CALL save_result_tai_xiu_md5(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            int param = 1;
            call.setLong(param++, message.referenceId);
            call.setByte(param++, (byte) message.result);
            call.setByte(param++, (byte) message.dice1);
            call.setByte(param++, (byte) message.dice2);
            call.setByte(param++, (byte) message.dice3);
            call.setLong(param++, message.totalTai);
            call.setLong(param++, message.totalXiu);
            call.setInt(param++, message.numBetTai);
            call.setInt(param++, message.numBetXiu);
            call.setLong(param++, message.totalPrize);
            call.setLong(param++, message.totalRefundTai);
            call.setLong(param++, message.totalRefundXiu);
            call.setLong(param++, message.totalRevenue);
            call.setByte(param++, (byte) message.moneyType);
            call.setString(param++, message.before_md5);
            call.setString(param++, message.md5);
            call.execute();
            success = true; // SP returns no ResultSet — call.execute() returns false even on success
            if (call != null) {
                call.close();
            }
        }finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return success;
    }

    @Override
    public boolean saveTransactionTaiXiu(TransactionTaiXiuMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL save_transaction_tai_xiu_md5(?, ?, ?, ?, ?, ?, ?, ?)");
            int param = 1;
            call.setLong(param++, message.referenceId);
            call.setInt(param++, message.userId);
            call.setString(param++, message.username);
            call.setLong(param++, message.betValue);
            call.setByte(param++, (byte) message.betSide);
            call.setLong(param++, message.prize);
            call.setLong(param++, message.refund);
            call.setByte(param++, (byte) message.moneyType);
            call.execute();
            success = true; // SP returns no ResultSet — call.execute() returns false even on success
            if (call != null) {
                call.close();
            }
        }finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return success;
    }

    @Override
    public boolean saveTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL save_transaction_detail_tai_xiu_md5(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
            call.setByte(param++, (byte) message.moneyType);
            call.execute();
            success = true; // SP returns no ResultSet — call.execute() returns false even on success
            if (call != null) {
                call.close();
            }
        }finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return success;
    }

    @Override
    public boolean updateTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL update_transaction_tai_xiu_md5_detail(?, ?, ?)");
            int param = 1;
            call.setString(param++, message.transactionCode);
            call.setLong(param++, message.prize);
            call.setLong(param++, message.refund);
            call.execute();
            success = true; // SP returns no ResultSet — call.execute() returns false even on success
            if (call != null) {
                call.close();
            }
        }finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return success;
    }

    @Override
    public boolean updateThanhDu(ThanhDuMessage message) throws SQLException {
        boolean success = false;

        return success;
    }

    @Override
    public boolean updatePot(UpdatePotMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL update_pot(?, ?)");
            int param = 1;
            call.setString(param++, message.potName);
            call.setLong(param++, message.newValue);
            call.execute();
            success = true; // SP returns no ResultSet — call.execute() returns false even on success
            if (call != null) {
                call.close();
            }
        }finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return success;
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
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL update_luot_rut_loc(?, ?)");
            int param = 1;
            call.setString(param++, message.username);
            call.setLong(param++, message.soLuotThem);
            call.execute();
            success = true; // SP returns no ResultSet — call.execute() returns false even on success
            if (call != null) {
                call.close();
            }
        }finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return success;
    }

    @Override
    public boolean updateFund(UpdateFundMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL update_fund(?, ?)");
            int param = 1;
            call.setString(param++, message.fundName);
            call.setLong(param++, message.newValue);
            call.execute();
            success = true; // SP returns no ResultSet — call.execute() returns false even on success
            if (call != null) {
                call.close();
            }
        }finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return success;
    }
}

