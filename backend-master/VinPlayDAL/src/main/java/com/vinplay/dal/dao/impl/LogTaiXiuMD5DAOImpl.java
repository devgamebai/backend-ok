/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.LogTaiXiuMD5DAO;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LogTaiXiuMD5DAOImpl
implements LogTaiXiuMD5DAO {
    @Override
    public int deleteLogTaiXiuMD5ByDay(int soNgay) throws SQLException {
        int row;
        PreparedStatement stmt;
        String sql;
        long second = soNgay * 86400;
        int total = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
            sql = "DELETE FROM vinplay_minigame.result_tai_xiu_md5 where CURRENT_TIMESTAMP - timestamp > ? ORDER BY id LIMIT 10000";
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, second * 14L);
            row = 1;
            while (row > 0) {
                row = stmt.executeUpdate();
            }
            stmt.close();
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
            sql = "DELETE FROM vinplay_minigame.transaction_detail_tai_xiu_md5 where CURRENT_TIMESTAMP - timestamp > ? and user_id = 0 ORDER BY id LIMIT 10000";
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, second);
            row = 1;
            while (row > 0) {
                row = stmt.executeUpdate();
                total += row;
            }
            stmt.close();
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame")) {
            sql = "DELETE FROM vinplay_minigame.transaction_tai_xiu_md5 where CURRENT_TIMESTAMP - timestamp > ? and user_id = 0 ORDER BY id LIMIT 10000";
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, second);
            row = 1;
            while (row > 0) {
                row = stmt.executeUpdate();
                total += row;
            }
            stmt.close();
        }
        return total;
    }
}
