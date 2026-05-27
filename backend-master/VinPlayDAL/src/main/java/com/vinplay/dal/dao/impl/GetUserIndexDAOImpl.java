/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.GetUserIndexDAO;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetUserIndexDAOImpl
implements GetUserIndexDAO {
    @Override
    public int getRegister(String timeStart, String timeEnd, String referralCode) throws SQLException {
        int res = 0;
        String sql = " SELECT COUNT(*) AS total  FROM vinplay.users  WHERE is_bot = 0 and dai_ly = 0 and `create_time` >= ?    AND `create_time` <= ? "
            + (referralCode != null && !referralCode.isEmpty() ? " AND refferal_code = ? " : "");
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, timeStart);
            stmt.setString(2, timeEnd);
            if (referralCode != null && !referralCode.isEmpty()) {
                stmt.setString(3, referralCode);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    res = rs.getInt("total");
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return res;
    }

    @Override
    public int getRecharge(String timeStart, String timeEnd, String referralCode) throws SQLException {
        int res = 0;
        String sql = " SELECT COUNT(*) AS total  FROM vinplay.users  WHERE `create_time` >= ?    AND `create_time` <= ?    AND EXISTS (SELECT 1 FROM v_derived_deposit_total d WHERE d.user_id = users.id AND d.deposit_total > 0) ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, timeStart);
            stmt.setString(2, timeEnd);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    res = rs.getInt("total");
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return res;
    }

    @Override
    public int getSecMobile(String timeStart, String timeEnd, String referralCode) throws SQLException {
        int res = 0;
        String sql = " SELECT COUNT(*) AS total  FROM vinplay.users  WHERE `create_time` >= ?    AND `create_time` <= ?    AND (`status` & 16) ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, timeStart);
            stmt.setString(2, timeEnd);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    res = rs.getInt("total");
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return res;
    }

    @Override
    public int getBoth(String timeStart, String timeEnd, String referralCode) throws SQLException {
        int res = 0;
        String sql = " SELECT COUNT(*) AS total  FROM vinplay.users  WHERE `create_time` >= ?    AND `create_time` <= ?    AND EXISTS (SELECT 1 FROM v_derived_deposit_total d WHERE d.user_id = users.id AND d.deposit_total > 0)    AND (`status` & 16) ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, timeStart);
            stmt.setString(2, timeEnd);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    res = rs.getInt("total");
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return res;
    }
}
