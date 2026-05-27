/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.AgentDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AgentDaoImpl
implements AgentDao {
    @Override
    public boolean checkSMSAgent(String nickname, long money) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");){
            int cnt;
            String sql = "SELECT COUNT(1) as cnt FROM useragent WHERE nickname=? AND status='D' AND active=1 AND sms >= 0 AND sms <= ?";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(1) as cnt FROM useragent WHERE nickname=? AND status='D' AND active=1 AND sms >= 0 AND sms <= ?");
            stm.setString(1, nickname);
            stm.setLong(2, money);
            ResultSet rs = stm.executeQuery();
            if (rs.next() && (cnt = rs.getInt("cnt")) == 1) {
                res = true;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public String getNicknameAgent1(String agent2) throws SQLException {
        String res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");){
            String sql = "SELECT nickname FROM useragent WHERE id = (SELECT parentId FROM useragent WHERE nickname=?)";
            PreparedStatement stm = conn.prepareStatement("SELECT nickname FROM useragent WHERE id = (SELECT parentId FROM useragent WHERE nickname=?)");
            stm.setString(1, agent2);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getString("nickname");
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public String getAgentLevel1ByNickName(String nickName) throws SQLException {
        String results = "";
        String parentId = "";
        String sqlGetParentId = " SELECT parentid  FROM vinplay_admin.useragent  WHERE nickname = ?    AND status = 'D'    AND active = 1 ";
        String sqlGetAgentLevel1 = " SELECT a1.nickname AS dlcap1  FROM vinplay_admin.useragent a1, vinplay_admin.useragent a2  WHERE a2.nickname = ?    AND a1.id = a2.parentid    AND a1.status = 'D'    AND a1.active = 1    AND a2.status = 'D'    AND a2.active = 1 ";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(" SELECT parentid  FROM vinplay_admin.useragent  WHERE nickname = ?    AND status = 'D'    AND active = 1 ");
            stmt.setString(1, nickName);
            rs = stmt.executeQuery();
            while (rs.next()) {
                parentId = rs.getString("parentid");
            }
            if ("-1".equals(parentId)) {
                String string = nickName;
                return string;
            }
            stmt = conn.prepareStatement(" SELECT a1.nickname AS dlcap1  FROM vinplay_admin.useragent a1, vinplay_admin.useragent a2  WHERE a2.nickname = ?    AND a1.id = a2.parentid    AND a1.status = 'D'    AND a1.active = 1    AND a2.status = 'D'    AND a2.active = 1 ");
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
            e.printStackTrace();
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

