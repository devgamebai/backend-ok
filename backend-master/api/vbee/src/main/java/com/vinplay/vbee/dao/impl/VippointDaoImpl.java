package com.vinplay.vbee.dao.impl;

/**
 * SUN-1340 (2026-05-16): rewrite of the decompiled-CFR source to use
 * try-with-resources on every JDBC call. The original file had no finally
 * block around the single JDBC method (updateVippointEvent) — any SQLException
 * thrown between getConnection() and the manual close() leaked the Connection
 * permanently.
 */

import com.vinplay.vbee.common.messages.vippoint.VippointEventMessage;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.dao.VippointDao;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class VippointDaoImpl
implements VippointDao {
    @Override
    public boolean updateVippointEvent(VippointEventMessage message, int isBot) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL update_vippoint_event(?,?,?,?,?,?,?,?,?)")) {
            int param = 1;
            call.setInt(param++, message.getUserId());
            call.setString(param++, message.getNickname());
            if (message.getType() == 0) {
                call.setInt(param++, message.getVpReal());
                call.setInt(param++, message.getVpEvent());
                call.setInt(param++, 0);
            } else if (message.getType() == 2) {
                call.setInt(param++, message.getNumSub());
                call.setInt(param++, message.getVpSub());
                call.setInt(param++, message.getVpEvent());
            } else if (message.getType() == 1) {
                call.setInt(param++, message.getNumAdd());
                call.setInt(param++, message.getVpAdd());
                call.setInt(param++, message.getVpEvent());
            } else if (message.getType() == 3) {
                call.setInt(param++, message.getNumAdd());
                call.setInt(param++, message.getVp());
                call.setInt(param++, 0);
            }
            call.setInt(param++, message.getPlace());
            call.setInt(param++, message.getPlaceMax());
            call.setInt(param++, message.getType());
            call.setInt(param++, isBot);
            call.executeUpdate();
            return true;
        }
    }
}
