/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.CommonUtils
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.CommonUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaiXiuLiveDaoImpl {
    public List<TransactionTaiXiu> getLichSuGiaoDich(String nickname, int number, int moneyType) throws SQLException {
        ArrayList<TransactionTaiXiu> results = new ArrayList<TransactionTaiXiu>();
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL tx_get_lich_su_giao_dich_live(?,?,?)");
            int param = 1;
            call.setString(param++, nickname);
            call.setInt(param++, number);
            call.setByte(param++, (byte)moneyType);
            ResultSet rs = call.executeQuery();
            while (rs.next()) {
                TransactionTaiXiu entry = new TransactionTaiXiu();
                entry.referenceId = rs.getLong("reference_id");
                entry.userId = rs.getInt("user_id");
                entry.username = rs.getString("user_name");
                entry.betValue = rs.getLong("bet_value");
                entry.betSide = rs.getInt("bet_side");
                entry.totalPrize = rs.getLong("prize");
                entry.totalRefund = 0L;
                Timestamp date = rs.getTimestamp("timestamp");
                entry.timestamp = CommonUtils.convertTimestampToString((Date)date);
                byte dice1 = rs.getByte("dice1");
                byte dice2 = rs.getByte("dice2");
                byte dice3 = rs.getByte("dice3");
                int total = dice1 + dice2 + dice3;
                entry.resultPhien = dice1 + " - " + dice2 + " - " + dice3 + "   " + total;
                entry.before_md5 = rs.getString("before_md5");
                entry.md5 = rs.getString("md5");
                results.add(entry);
            }
            rs.close();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (call != null) {
                call.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return results;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean saveTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL save_transaction_detail_tai_xiu_live(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
            success = call.execute();
            if (call != null) {
                call.close();
            }
        }
        finally {
            ConnectionPool.getInstance();
            ConnectionPool.releaseConnection((Connection)conn);
        }
        return success;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean updateTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage message) throws SQLException {
        boolean success = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = null;
            call = conn.prepareCall("CALL update_transaction_detail_tai_xiu_live(?, ?, ?)");
            int param = 1;
            call.setString(param++, message.transactionCode);
            call.setLong(param++, message.prize);
            call.setLong(param++, message.refund);
            success = call.execute();
            if (call != null) {
                call.close();
            }
        }
        finally {
            ConnectionPool.getInstance();
            ConnectionPool.releaseConnection((Connection)conn);
        }
        return success;
    }
}

