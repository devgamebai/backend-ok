/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.ListAgentLevel2DAO;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListAgentLevel2DAOImpl
implements ListAgentLevel2DAO {
    @Override
    public List<String> listAgentLevel2(String nickName) throws SQLException {
        ArrayList<String> results = new ArrayList<String>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");){
            String sql = " SELECT a2.nickname AS dlcap2 FROM vinplay_admin.useragent a1, vinplay_admin.useragent a2  WHERE a1.nickname = ?    AND a1.id = a2.parentid    AND a1.status = 'D'    AND a1.active = 1    AND a2.status = 'D'    AND a2.active = 1 ";
            PreparedStatement stmt = conn.prepareStatement(" SELECT a2.nickname AS dlcap2 FROM vinplay_admin.useragent a1, vinplay_admin.useragent a2  WHERE a1.nickname = ?    AND a1.id = a2.parentid    AND a1.status = 'D'    AND a1.active = 1    AND a2.status = 'D'    AND a2.active = 1 ");
            stmt.setString(1, nickName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("dlcap2"));
            }
            rs.close();
            stmt.close();
        }
        return results;
    }

    @Override
    public String getAgentLevel1ByNickName(String nickName) throws SQLException {
        String results = "";
        String parentId = "";
        String sqlGetParentId = " SELECT parentid  FROM vinplay_admin.useragent  WHERE nickname = ? ";
        String sqlGetAgentLevel1 = " SELECT a1.nickname AS dlcap1 FROM vinplay_admin.useragent a1, vinplay_admin.useragent a2  WHERE a2.nickname = ?    AND a1.id = a2.parentid ";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(" SELECT parentid  FROM vinplay_admin.useragent  WHERE nickname = ? ");
            stmt.setString(1, nickName);
            rs = stmt.executeQuery();
            while (rs.next()) {
                parentId = rs.getString("parentid");
            }
            if ("-1".equals(parentId)) {
                String string = nickName;
                return string;
            }
            stmt = conn.prepareStatement(" SELECT a1.nickname AS dlcap1 FROM vinplay_admin.useragent a1, vinplay_admin.useragent a2  WHERE a2.nickname = ?    AND a1.id = a2.parentid ");
            stmt.setString(1, nickName);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String string = rs.getString("dlcap1");
                return string;
            }
            rs.close();
            stmt.close();
        }
        catch (SQLException e) {
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
        return "";
    }
}

