package com.vinplay.vbee.dao.impl;

import com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.dao.TaiXiuDao;
import com.vinplay.vbee.common.messages.minigame.ThanhDuMessage;
import com.vinplay.vbee.common.messages.minigame.UpdatePotMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateFundMessage;
import com.vinplay.vbee.common.messages.minigame.LogTanLocMessage;
import com.vinplay.vbee.common.messages.minigame.LogRutLocMessge;
import com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * DAO for Sicbo (TaiXiu Sicbo) — calls _sicbo stored procedures.
 * Missing class that caused sicbo transactions to silently drop from RMQ.
 */
public class TaiXiuSicboDaoImpl implements TaiXiuDao {

    @Override
    public boolean saveResultTaiXiu(ResultTaiXiuMessage message) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = conn.prepareCall("{CALL save_result_tai_xiu_sicbo(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            int p = 1;
            call.setLong(p++, message.referenceId);
            call.setByte(p++, (byte) message.result);
            call.setByte(p++, (byte) message.dice1);
            call.setByte(p++, (byte) message.dice2);
            call.setByte(p++, (byte) message.dice3);
            call.setLong(p++, message.totalTai);
            call.setLong(p++, message.totalXiu);
            call.setInt(p++, message.numBetTai);
            call.setInt(p++, message.numBetXiu);
            call.setLong(p++, message.totalPrize);
            call.setLong(p++, message.totalRefundTai);
            call.setLong(p++, message.totalRefundXiu);
            call.setLong(p++, message.totalRevenue);
            call.setByte(p++, (byte) message.moneyType);
            call.setString(p++, message.before_md5);
            call.setString(p++, message.md5);
            call.execute();
            call.close();
            return true;
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean saveTransactionTaiXiu(TransactionTaiXiuMessage message) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = conn.prepareCall("CALL save_transaction_tai_xiu_sicbo(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int p = 1;
            call.setLong(p++, message.referenceId);
            call.setInt(p++, message.userId);
            call.setString(p++, message.username);
            call.setLong(p++, message.betValue);
            call.setByte(p++, (byte) message.betSide);
            call.setLong(p++, message.prize);
            call.setLong(p++, message.refund);
            call.setByte(p++, (byte) message.moneyType);
            call.setLong(p++, 0L); // total_jackpot (9th param added to SP)
            call.execute();
            call.close();
            return true;
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean saveTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage message) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = conn.prepareCall("CALL save_transaction_detail_tai_xiu_sicbo(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int p = 1;
            call.setLong(p++, message.referenceId);
            call.setString(p++, message.transactionCode);
            call.setInt(p++, message.userId);
            call.setString(p++, message.username);
            call.setLong(p++, message.betValue);
            call.setInt(p++, message.betSide);
            call.setLong(p++, message.prize);
            call.setLong(p++, message.refund);
            call.setInt(p++, message.inputTime);
            call.setByte(p++, (byte) message.moneyType);
            call.execute();
            call.close();
            return true;
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean updateTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage message) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        try {
            CallableStatement call = conn.prepareCall("CALL update_transaction_detail_tai_xiu_sicbo(?, ?, ?)");
            int p = 1;
            call.setString(p++, message.transactionCode);
            call.setLong(p++, message.prize);
            call.setLong(p++, message.refund);
            call.execute();
            call.close();
            return true;
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    @Override public boolean updateThanhDu(ThanhDuMessage m) throws SQLException { return false; }
    @Override public boolean updatePot(UpdatePotMessage m) throws SQLException { return false; }
    @Override public boolean updateFund(UpdateFundMessage m) throws SQLException { return false; }
    @Override public void logTanLoc(LogTanLocMessage m) {}
    @Override public void logRutLoc(LogRutLocMessge m) {}
    @Override public boolean updateLuotRutLoc(UpdateLuotRutLocMessage m) throws SQLException { return false; }
}
