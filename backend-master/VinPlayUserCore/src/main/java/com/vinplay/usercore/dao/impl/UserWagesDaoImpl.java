/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.UserWagesDao;
import com.vinplay.usercore.entities.UserWages;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.CallableStatement;
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
import org.apache.log4j.Logger;

public class UserWagesDaoImpl
implements UserWagesDao {
    private static final Logger logger = Logger.getLogger(UserWagesDaoImpl.class);

    @Override
    public String insert(UserWages userWages) throws SQLException {
        String result = "failed";
        String sql = "INSERT INTO vinplay.user_wages (nick_name,created_at,bonus,status,parent_user) VALUE (?,?,?,?,?)";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, userWages.getNick_name());
            stmt.setString(param++, userWages.getCreated_at());
            stmt.setLong(param++, userWages.getBonus());
            stmt.setInt(param++, userWages.getStatus());
            stmt.setString(param++, userWages.getParent_user());
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public boolean insertByJob(String date) throws SQLException {
        boolean result = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL user_wages_insert(?)")) {
            call.setString(1, date);
            try (ResultSet rs = call.executeQuery()) {
                result = true;
            }
        }
        catch (SQLException e) {
            logger.error(("Error user_wages insertByJob: " + e.getMessage()));
            throw e;
        }
        return result;
    }

    @Override
    public String update(UserWages userWages) throws SQLException {
        String result = "failed";
        String sql = "UPDATE vinplay.user_wages set nick_name=?,bonus=?,status=?,parent_user=? where id=?";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, userWages.getNick_name());
            stmt.setLong(param++, userWages.getBonus());
            stmt.setInt(param++, userWages.getStatus());
            stmt.setLong(param++, userWages.getId());
            stmt.setString(param++, userWages.getParent_user());
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public String updateStatus(long id, int status) throws SQLException {
        String result = "failed";
        String sql = "UPDATE vinplay.user_wages set status=? where id=?";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setInt(param++, status);
            stmt.setLong(param++, id);
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public String updateAllStatusToReceivedBonus(String nickname) throws SQLException {
        String result = "failed";
        String sql = "UPDATE vinplay.user_wages set status=1 where status=0 and parent_user=?";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, nickname);
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public UserWages getById(long id) throws SQLException {
        String sql = "select * from user_wages where id = ?";
        UserWages userWages = new UserWages();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userWages = new UserWages(rs);
                }
            }
            return userWages;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public UserWages getByDate(String date) throws SQLException {
        String sql = "select * from user_wages where date(create_date)=?";
        UserWages userWages = new UserWages();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, date);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userWages = new UserWages(rs);
                }
            }
            return userWages;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public long getSumBonusByStatus(String nickname, int status) throws SQLException {
        long totalBonus = 0L;
        String sql = "select sum(bonus) as totalBonus from user_wages where parent_user=? and status=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nickname);
            stmt.setInt(2, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    totalBonus = rs.getObject("totalBonus") == null ? 0L : (long)rs.getInt("totalBonus");
                }
            }
            return totalBonus;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Map<String, Object> history(String nickname, String startDate, String endDate, int status, int pageIndex, int limit) throws SQLException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        pageIndex = pageIndex < 1 ? 0 : pageIndex - 1;
        String paginateCondition = pageIndex == -1 || limit == -1 ? "" : " limit ?,?";
        String statusCondition = status == -1 ? " status in (0,1) " : " status = " + status;
        String sql = "select * from user_wages where parent_user = ? and " + statusCondition + " and created_at >= ? and created_at <= ? order by bonus desc, created_at desc" + paginateCondition;
        String sqlCount = "select count(id) as total, sum(bonus) as totalBonus from user_wages where parent_user = ? and " + statusCondition + " and created_at >= ? and created_at <= ? order by bonus desc, created_at desc";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);
             PreparedStatement stmtCount = conn.prepareStatement(sqlCount)) {
            int param = 1;
            if (StringUtils.isBlank((String)nickname)) {
                stmt.setString(param, "(1=1)");
                stmtCount.setString(param, "(1=1)");
                ++param;
            } else {
                stmt.setString(param, nickname);
                stmtCount.setString(param, nickname);
                ++param;
            }
            if (StringUtils.isBlank((String)startDate)) {
                stmt.setString(param, "1900-01-01 00:00:00");
                stmtCount.setString(param, "1900-01-01 00:00:00");
                ++param;
            } else {
                stmt.setString(param, startDate + " 00:00:00");
                stmtCount.setString(param, startDate + " 00:00:00");
                ++param;
            }
            if (StringUtils.isBlank((String)endDate)) {
                stmt.setString(param, new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "23:59:59");
                stmtCount.setString(param, new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "23:59:59");
                ++param;
            } else {
                stmt.setString(param, endDate + " 23:59:59");
                stmtCount.setString(param, endDate + " 23:59:59");
                ++param;
            }
            if (-1 != pageIndex && -1 != limit) {
                stmt.setInt(param++, pageIndex * limit);
                stmt.setInt(param++, limit);
            }
            ArrayList<UserWages> userWages = new ArrayList<UserWages>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        UserWages userWage = new UserWages(rs);
                        userWages.add(userWage);
                    }
                    catch (Exception exception) {}
                }
            }
            map.put("userWages", userWages);
            try (ResultSet rsCount = stmtCount.executeQuery()) {
                while (rsCount.next()) {
                    map.put("totalRecord", rsCount.getObject("total") == null ? 0 : rsCount.getInt("total"));
                    map.put("totalBonus", rsCount.getObject("totalBonus") == null ? 0L : rsCount.getLong("totalBonus"));
                }
            }
            return map;
        }
        catch (SQLException e) {
            e.printStackTrace();
            map.put("userWages", new ArrayList());
            map.put("totalRecord", 0);
            map.put("totalBonus", 0);
            throw e;
        }
    }
}

