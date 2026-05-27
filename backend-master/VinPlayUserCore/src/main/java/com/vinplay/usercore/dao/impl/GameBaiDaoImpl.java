/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.GameBaiDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GameBaiDaoImpl
implements GameBaiDao {
    @Override
    public String getString(String key) throws SQLException {
        String res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT `value` FROM game_data WHERE `key`=?";
            PreparedStatement stm = conn.prepareStatement("SELECT `value` FROM game_data WHERE `key`=?");
            stm.setString(1, key);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getString("value");
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean saveString(String key, String value) throws SQLException {
        boolean res = false;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL save_game_data(?,?)");
            int param = 1;
            call.setString(param++, key);
            call.setString(param++, value);
            call.executeUpdate();
            res = true;
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
        return res;
    }
}

