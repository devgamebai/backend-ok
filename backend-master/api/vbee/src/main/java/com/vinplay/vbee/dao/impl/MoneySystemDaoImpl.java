package com.vinplay.vbee.dao.impl;

/**
 * SUN-1340 (2026-05-16): rewrite of the decompiled-CFR source to use
 * try-with-resources on every JDBC call. The original file had no finally
 * blocks — any SQLException thrown between getConnection() and the manual
 * close() at the end of the method leaked the Connection permanently.
 * Both JDBC methods are rewritten.
 */

import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.dao.MoneySystemDao;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MoneySystemDaoImpl
implements MoneySystemDao {
    @Override
    public boolean updateMoneySystem(String name, long money) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL update_money_system(?,?,?)")) {
            int param = 1;
            call.setString(param++, name);
            call.setLong(param++, money);
            call.registerOutParameter(param, -6);
            call.executeUpdate();
            return money > 0L ? call.getInt(param) == 2 : call.getInt(param) == 1;
        }
    }

    @Override
    public long getMoneySystem(String name) throws SQLException {
        long money = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement("SELECT money FROM money_system WHERE name=?")) {
            stm.setString(1, name);
            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    money = rs.getLong("money");
                }
            }
        }
        return money;
    }
}
