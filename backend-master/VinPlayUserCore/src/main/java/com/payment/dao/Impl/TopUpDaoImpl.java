/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.payment.dao.Impl;

import com.payment.dao.TopUpDao;
import com.payment.entities.TopUpEntity;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TopUpDaoImpl
implements TopUpDao {
    @Override
    public boolean create(TopUpEntity entity) throws SQLException {
        String sql = "INSERT INTO topup (fid, key_id, nick_name, serial, code, cash, cash_real, type, text, status, day, month, year, time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
        String sql = "UPDATE topup SET status = ?, cash_real = ? , text = ? WHERE key_id = ?";
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
        String sql = "UPDATE topup SET status = ?, text = ? WHERE key_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setString(2, text);
            pstmt.setString(3, requestId);
            boolean bl = pstmt.executeUpdate() > 0;
            return bl;
        }
    }

    @Override
    public TopUpEntity getByRequestId(String requestId) throws SQLException {
        String sql = "SELECT * FROM topup WHERE key_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, requestId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                TopUpEntity entity = new TopUpEntity();
                entity.setFid(rs.getString("fid"));
                entity.setRequest_id(rs.getString("key_id"));
                entity.setNick_name(rs.getString("nick_name"));
                entity.setSerial(rs.getString("serial"));
                entity.setCode(rs.getString("code"));
                entity.setCash(rs.getInt("cash"));
                entity.setCash_real(rs.getInt("cash_real"));
                entity.setType(rs.getString("type"));
                entity.setText(rs.getString("text"));
                entity.setStatus(rs.getInt("status"));
                entity.setDay(rs.getInt("day"));
                entity.setMonth(rs.getInt("month"));
                entity.setYear(rs.getInt("year"));
                entity.setTime(rs.getLong("time"));
                TopUpEntity topUpEntity = entity;
                return topUpEntity;
            }
        }
        return null;
    }

    @Override
    public List<TopUpEntity> getAllByNickname(String nickname) throws SQLException {
        String sql = "SELECT * FROM topup WHERE nick_name = ? ORDER BY time DESC";
        ArrayList<TopUpEntity> list = new ArrayList<TopUpEntity>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);){
            stm.setString(1, nickname);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                TopUpEntity entity = new TopUpEntity();
                entity.setFid(rs.getString("fid"));
                entity.setRequest_id(rs.getString("key_id"));
                entity.setNick_name(rs.getString("nick_name"));
                entity.setSerial(rs.getString("serial"));
                entity.setCode(rs.getString("code"));
                entity.setCash(rs.getInt("cash"));
                entity.setCash_real(rs.getInt("cash_real"));
                entity.setType(rs.getString("type"));
                entity.setText(rs.getString("text"));
                entity.setStatus(rs.getInt("status"));
                entity.setDay(rs.getInt("day"));
                entity.setMonth(rs.getInt("month"));
                entity.setYear(rs.getInt("year"));
                entity.setTime(rs.getLong("time"));
                list.add(entity);
            }
            rs.close();
        }
        return list;
    }
}

