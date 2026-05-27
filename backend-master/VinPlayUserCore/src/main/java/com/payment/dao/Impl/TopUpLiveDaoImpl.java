/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.payment.dao.Impl;

import com.payment.dao.TopUpLiveDao;
import com.payment.entities.TopUpEntity;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TopUpLiveDaoImpl
implements TopUpLiveDao {
    @Override
    public boolean create(TopUpEntity entity) throws SQLException {
        String sql = "INSERT INTO topup_live (fid, key_id, nick_name, serial, code, cash, cash_real, type, text, status, day, month, year, time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, entity.getFid());
            pstmt.setString(2, entity.getRequest_id());
            pstmt.setString(3, entity.getNick_name());
            pstmt.setString(4, entity.getSerial());
            pstmt.setString(5, entity.getCode());
            pstmt.setLong(6, entity.getCash());
            pstmt.setLong(7, entity.getCash_real());
            pstmt.setString(8, entity.getType());
            pstmt.setString(9, entity.getText());
            pstmt.setInt(10, entity.getStatus());
            pstmt.setInt(11, entity.getDay());
            pstmt.setInt(12, entity.getMonth());
            pstmt.setInt(13, entity.getYear());
            pstmt.setLong(14, entity.getTime());
            boolean bl = pstmt.executeUpdate() > 0;
            return bl;
        }
    }

    @Override
    public boolean updateStatus(String requestId, int status, String text, long cash) throws SQLException {
        String sql = "UPDATE topup_live SET status = ?, cash_real = ? , text = ? WHERE key_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setLong(2, cash);
            pstmt.setString(3, text);
            pstmt.setString(4, requestId);
            boolean bl = pstmt.executeUpdate() > 0;
            return bl;
        }
    }

    @Override
    public boolean updateStatus(String requestId, int status, String text) throws SQLException {
        String sql = "UPDATE topup_live SET status = ?, text = ? WHERE key_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setString(2, text);
            pstmt.setString(3, requestId);
            boolean bl = pstmt.executeUpdate() > 0;
            return bl;
        }
    }
}

