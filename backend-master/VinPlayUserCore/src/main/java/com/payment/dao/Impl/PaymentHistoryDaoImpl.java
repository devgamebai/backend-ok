/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.payment.dao.Impl;

import com.payment.dao.PaymentHistoryDao;
import com.payment.entities.PaymentHistoryEntity;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryDaoImpl
implements PaymentHistoryDao {
    @Override
    public List<PaymentHistoryEntity> getAllByNickname(String nickname, int page, int maxItem) throws SQLException {
        ArrayList<PaymentHistoryEntity> list = new ArrayList<PaymentHistoryEntity>();
        String sql = "SELECT * FROM (SELECT 'bank' as type, fid, key_id as request_id, nick_name, NULL as serial, NULL as code, cash, cash_real, status, text, day, month, year, time, number FROM history_bank WHERE nick_name = ? UNION ALL SELECT 'topup' as type, fid, key_id as request_id, nick_name, serial, code, cash, cash_real, status, text, day, month, year, time, NULL as number FROM topup WHERE nick_name = ? ) as combined ORDER BY time DESC LIMIT ? OFFSET ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);){
            int offset = page * maxItem;
            stm.setString(1, nickname);
            stm.setString(2, nickname);
            stm.setInt(3, maxItem);
            stm.setInt(4, offset);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                PaymentHistoryEntity entity = new PaymentHistoryEntity();
                entity.setType(rs.getString("type"));
                entity.setFid(rs.getString("fid"));
                entity.setRequest_id(rs.getString("request_id"));
                entity.setNick_name(rs.getString("nick_name"));
                entity.setSerial(rs.getString("serial"));
                entity.setCode(rs.getString("code"));
                entity.setCash(rs.getInt("cash"));
                entity.setCash_real(rs.getInt("cash_real"));
                entity.setStatus(rs.getInt("status"));
                entity.setText(rs.getString("text"));
                entity.setDay(rs.getInt("day"));
                entity.setMonth(rs.getInt("month"));
                entity.setYear(rs.getInt("year"));
                entity.setTime(rs.getLong("time"));
                int number = rs.getInt("number");
                if (!rs.wasNull()) {
                    entity.setNumber(number);
                }
                list.add(entity);
            }
            rs.close();
        }
        return list;
    }

    @Override
    public int getTotalCount(String nickname) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM (SELECT key_id FROM history_bank WHERE nick_name = ? UNION ALL SELECT key_id FROM topup WHERE nick_name = ?) as combined";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);){
            stm.setString(1, nickname);
            stm.setString(2, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                int n = rs.getInt("total");
                return n;
            }
            rs.close();
        }
        return 0;
    }
}

