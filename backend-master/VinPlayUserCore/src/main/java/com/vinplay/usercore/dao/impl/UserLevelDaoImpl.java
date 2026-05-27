/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.UserLevelDao;
import com.vinplay.usercore.entities.UserLevel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

public class UserLevelDaoImpl
implements UserLevelDao {
    @Override
    public String insert(UserLevel userLevel) throws SQLException {
        String result = "failed";
        String sql = "INSERT INTO vinplay.user_level (nick_name,code,ancestor,parent_user) VALUE (?,?,?,?)";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, userLevel.getNick_name());
            stmt.setString(param++, userLevel.getCode());
            stmt.setString(param++, userLevel.getAncestor());
            stmt.setString(param++, userLevel.getParent_user());
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public String Update(UserLevel userLevel) throws SQLException {
        String result = "failed";
        String sql = "UPDATE vinplay.user_level set nick_name=?,ancestor=? where id=?";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, userLevel.getNick_name());
            stmt.setString(param++, userLevel.getAncestor());
            stmt.setLong(param++, userLevel.getId());
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public UserLevel getByNickName(String nickname, String parent_user) throws SQLException {
        UserLevel userLevel = new UserLevel();
        String sql = "select * from user_level where nick_name = ? and parent_user=?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nickname);
            stmt.setString(2, parent_user);
            rs = stmt.executeQuery();
            while (rs.next()) {
                userLevel = new UserLevel(rs);
            }
            rs.close();
            stmt.close();
            UserLevel userLevel2 = userLevel.getId() == 0L ? null : userLevel;
            return userLevel2;
        }
        catch (SQLException e) {
            e.printStackTrace();
            userLevel = null;
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Override
    public UserLevel getByNickName(String nickname) throws SQLException {
        UserLevel userLevel = new UserLevel();
        String sql = "select * from user_level where nick_name = ?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nickname);
            rs = stmt.executeQuery();
            while (rs.next()) {
                userLevel = new UserLevel(rs);
            }
            rs.close();
            stmt.close();
            UserLevel userLevel2 = userLevel.getId() == 0L ? null : userLevel;
            return userLevel2;
        }
        catch (SQLException e) {
            e.printStackTrace();
            userLevel = null;
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Override
    public UserLevel getById(long id) throws SQLException {
        UserLevel userLevel = new UserLevel();
        String sql = "select * from user_level where id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            rs = stmt.executeQuery();
            while (rs.next()) {
                userLevel = new UserLevel(rs);
            }
            rs.close();
            stmt.close();
            UserLevel userLevel2 = userLevel.getId() == 0L ? null : userLevel;
            return userLevel2;
        }
        catch (SQLException e) {
            e.printStackTrace();
            userLevel = null;
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Override
    public Map<String, Object> findChilds(String nickname, String startDate, String endDate, int pageIndex, int limit) throws SQLException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        pageIndex = pageIndex < 1 ? 0 : pageIndex - 1;
        String paginateCondition = pageIndex == -1 || limit == -1 ? "" : " limit ?,?";
        String sql = "select a.*, IFNULL(b.totalBonus,0) totalBonus from user_level a LEFT JOIN (select sum(IFNULL(bonus,0)) totalBonus, nick_name  from user_wages GROUP BY nick_name) b ON a.nick_name = b.nick_name WHERE a.parent_user = ? and created_at >= ? and created_at <= ? ORDER BY b.totalBonus desc, a.created_at DESC" + paginateCondition;
        String sqlCount = "select count(*) total, sum(IFNULL(b.totalBonus,0)) as totalBonus from user_level a LEFT JOIN (select sum(IFNULL(bonus,0)) totalBonus, nick_name  from user_wages GROUP BY nick_name) b ON a.nick_name = b.nick_name WHERE a.parent_user = ? and created_at >= ? and created_at <= ? ORDER BY b.totalBonus desc, a.created_at DESC";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        PreparedStatement stmtCount = null;
        ResultSet rsCount = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmtCount = conn.prepareStatement(sqlCount);
            int index = 1;
            if (StringUtils.isBlank((String)nickname)) {
                stmt.setString(index, "(1=1)");
                stmtCount.setString(index, "(1=1)");
                ++index;
            } else {
                stmt.setString(index, nickname);
                stmtCount.setString(index, nickname);
                ++index;
            }
            if (StringUtils.isBlank((String)startDate)) {
                stmt.setString(index, "1900-01-01 00:00:00");
                stmtCount.setString(index, "1900-01-01 00:00:00");
                ++index;
            } else {
                stmt.setString(index, startDate + " 00:00:00");
                stmtCount.setString(index, startDate + " 00:00:00");
                ++index;
            }
            if (StringUtils.isBlank((String)endDate)) {
                stmt.setString(index, new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 23:59:59");
                stmtCount.setString(index, new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 23:59:59");
                ++index;
            } else {
                stmt.setString(index, endDate + " 23:59:59");
                stmtCount.setString(index, endDate + " 23:59:59");
                ++index;
            }
            if (-1 != pageIndex && -1 != limit) {
                stmt.setInt(index++, pageIndex * limit);
                stmt.setInt(index++, limit);
            }
            rs = stmt.executeQuery();
            ArrayList userLevels = new ArrayList();
            while (rs.next()) {
                try {
                    HashMap<String, Object> obj = new HashMap<String, Object>();
                    obj.put("id", rs.getObject("id"));
                    obj.put("nick_name", rs.getObject("nick_name"));
                    obj.put("code", rs.getObject("code"));
                    obj.put("ancestor", rs.getObject("ancestor"));
                    obj.put("created_at", rs.getString("created_at"));
                    obj.put("parent_user", rs.getObject("parent_user"));
                    obj.put("totalBonus", rs.getObject("totalBonus"));
                    userLevels.add(obj);
                }
                catch (Exception exception) {}
            }
            map.put("userLevels", userLevels);
            rsCount = stmtCount.executeQuery();
            while (rsCount.next()) {
                map.put("totalRecord", rsCount.getObject("total") == null ? 0 : rsCount.getInt("total"));
                map.put("totalBonus", rsCount.getObject("totalBonus") == null ? 0L : rsCount.getLong("totalBonus"));
            }
            rs.close();
            stmt.close();
            rsCount.close();
            stmtCount.close();
            HashMap<String, Object> hashMap = map;
            return hashMap;
        }
        catch (SQLException e) {
            e.printStackTrace();
            map.put("userLevels", new ArrayList());
            map.put("totalRecord", 0);
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}

