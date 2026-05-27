/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.UserAttendanceDao;
import com.vinplay.usercore.entities.UserAttendance;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class UserAttendanceDaoImpl
implements UserAttendanceDao {
    @Override
    public String insert(UserAttendance userAttendance) throws SQLException {
        String result = "failed";
        String sql = "INSERT INTO vinplay.user_attendance (attend_id,nick_name,date_attend,consecutive,bonus_basic,bonus_consecutive,bonus_vip,spin,result, ip) VALUE (?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setInt(param++, userAttendance.getAttend_id());
            stmt.setString(param++, userAttendance.getNick_name());
            stmt.setString(param++, userAttendance.getDate_attend());
            stmt.setInt(param++, userAttendance.getConsecutive());
            stmt.setLong(param++, userAttendance.getBonus_basic());
            stmt.setLong(param++, userAttendance.getBonus_consecutive());
            stmt.setLong(param++, userAttendance.getBonus_vip());
            stmt.setString(param++, userAttendance.getSpin());
            stmt.setString(param++, userAttendance.getResult());
            stmt.setString(param++, userAttendance.getIp());
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public String delete(int id) throws SQLException {
        String result = "failed";
        String sql = "delete from vinplay.user_attendance where id=?";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setInt(param++, id);
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public UserAttendance getLastest(String nickname) throws SQLException {
        UserAttendance userAttendance = new UserAttendance();
        String sql = "select * from user_attendance where nick_name = ? order by date_attend desc limit 1";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nickname);
            rs = stmt.executeQuery();
            while (rs.next()) {
                userAttendance.setId(rs.getInt("id"));
                userAttendance.setAttend_id(rs.getInt("attend_id"));
                userAttendance.setNick_name(rs.getString("nick_name"));
                userAttendance.setDate_attend(rs.getString("date_attend"));
                userAttendance.setConsecutive(rs.getInt("consecutive"));
                userAttendance.setBonus_basic(rs.getLong("bonus_basic"));
                userAttendance.setBonus_consecutive(rs.getLong("bonus_consecutive"));
                userAttendance.setBonus_vip(rs.getLong("bonus_vip"));
                userAttendance.setSpin(rs.getString("spin"));
                userAttendance.setResult(rs.getString("result"));
            }
            rs.close();
            stmt.close();
            UserAttendance userAttendance2 = userAttendance.getId() < 1 ? null : userAttendance;
            return userAttendance2;
        }
        catch (SQLException e) {
            e.printStackTrace();
            userAttendance = null;
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Override
    public UserAttendance getDetail(String nickname, int attendanceId, String date) throws SQLException {
        UserAttendance userAttendance = new UserAttendance();
        String sql = "SELECT * from user_attendance where nick_name=? and DATE(date_attend)=? and attend_id=?";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nickname);
            stmt.setString(2, date);
            stmt.setInt(3, attendanceId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                userAttendance.setId(rs.getInt("id"));
                userAttendance.setAttend_id(rs.getInt("attend_id"));
                userAttendance.setNick_name(rs.getString("nick_name"));
                userAttendance.setDate_attend(rs.getString("date_attend"));
                userAttendance.setConsecutive(rs.getInt("consecutive"));
                userAttendance.setBonus_basic(rs.getLong("bonus_basic"));
                userAttendance.setBonus_consecutive(rs.getLong("bonus_consecutive"));
                userAttendance.setBonus_vip(rs.getLong("bonus_vip"));
                userAttendance.setSpin(rs.getString("spin"));
                userAttendance.setResult(rs.getString("result"));
            }
            rs.close();
            stmt.close();
            UserAttendance userAttendance2 = userAttendance.getId() < 1 ? null : userAttendance;
            return userAttendance2;
        }
        catch (SQLException e) {
            e.printStackTrace();
            userAttendance = null;
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Override
    public Map<String, Object> search(Integer attendId, String nickname, String fromTime, String endTime, int pageIndex, int limit) throws SQLException {
        pageIndex = pageIndex < 1 ? 0 : pageIndex - 1;
        String paginateCondition = pageIndex == -1 || limit == -1 ? "" : " limit ?,?";
        String condition = "";
        if (attendId > 0) {
            condition = condition + " and (attend_id = ?)";
        }
        if (!StringUtils.isBlank((CharSequence)nickname)) {
            condition = condition + " and (nick_name = ?)";
        }
        if (!StringUtils.isBlank((CharSequence)fromTime) && !StringUtils.isBlank((CharSequence)endTime)) {
            condition = condition + " and (date_attend >= ? and date_attend <= ?)";
        }
        String sql = "select * from user_attendance where (1=1)" + condition + " order by date_attend desc, nick_name asc" + paginateCondition;
        String sqlCount = "select count(id) totalRecord from user_attendance where (1=1)" + condition + " order by date_attend desc, nick_name asc";
        String sqlSumPlayer = "select count(*) totalPlayer from (select nick_name from user_attendance where (1=1)" + condition + " GROUP BY nick_name) as totalPlayer";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stmt = conn.prepareStatement(sql);
            PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
            PreparedStatement stmtSumPlayer = conn.prepareStatement(sqlSumPlayer);
            int index = 1;
            if (attendId > 0) {
                stmt.setInt(index, attendId);
                stmtCount.setInt(index, attendId);
                stmtSumPlayer.setInt(index, attendId);
                ++index;
            }
            if (!StringUtils.isBlank((CharSequence)nickname)) {
                stmt.setString(index, nickname);
                stmtCount.setString(index, nickname);
                stmtSumPlayer.setString(index, nickname);
                ++index;
            }
            if (!StringUtils.isBlank((CharSequence)fromTime) && !StringUtils.isBlank((CharSequence)endTime)) {
                stmt.setString(index, fromTime);
                stmtCount.setString(index, fromTime);
                stmtSumPlayer.setString(index, fromTime);
                stmt.setString(++index, endTime);
                stmtCount.setString(index, endTime);
                stmtSumPlayer.setString(index, endTime);
                ++index;
            }
            if (-1 != pageIndex && -1 != limit) {
                stmt.setInt(index++, pageIndex * limit);
                stmt.setInt(index, limit);
            }
            HashMap<String, Object> data = new HashMap<String, Object>();
            ArrayList<UserAttendance> userAttendances = new ArrayList<UserAttendance>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserAttendance userAttendance = new UserAttendance();
                userAttendance.setId(rs.getInt("id"));
                userAttendance.setAttend_id(rs.getInt("attend_id"));
                userAttendance.setNick_name(rs.getString("nick_name"));
                userAttendance.setDate_attend(rs.getString("date_attend"));
                userAttendance.setConsecutive(rs.getInt("consecutive"));
                userAttendance.setBonus_basic(rs.getLong("bonus_basic"));
                userAttendance.setBonus_consecutive(rs.getLong("bonus_consecutive"));
                userAttendance.setBonus_vip(rs.getLong("bonus_vip"));
                userAttendance.setSpin(rs.getString("spin"));
                userAttendance.setResult(rs.getString("result"));
                userAttendances.add(userAttendance);
            }
            data.put("userAttendances", userAttendances);
            rs.close();
            stmt.close();
            ResultSet rsCount = stmtCount.executeQuery();
            while (rsCount.next()) {
                data.put("totalRecord", rsCount.getLong("totalRecord"));
            }
            rsCount.close();
            stmtCount.close();
            ResultSet rsSumPlayer = stmtSumPlayer.executeQuery();
            while (rsSumPlayer.next()) {
                data.put("totalPlayer", rsSumPlayer.getLong("totalPlayer"));
            }
            rsSumPlayer.close();
            stmtSumPlayer.close();
            HashMap<String, Object> hashMap = data;
            return hashMap;
        }
    }

    @Override
    public Map<String, Object> search4BO(Integer attendId, String nickname, String fromTime, String endTime, int pageIndex, int limit) throws SQLException {
        pageIndex = pageIndex < 1 ? 0 : pageIndex - 1;
        String paginateCondition = pageIndex == -1 || limit == -1 ? "" : " limit ?,?";
        String condition = "";
        if (attendId > 0) {
            condition = condition + " and (attend_id = ?)";
        }
        if (!StringUtils.isBlank((CharSequence)nickname)) {
            condition = condition + " and (nick_name = ?)";
        }
        if (!StringUtils.isBlank((CharSequence)fromTime) && !StringUtils.isBlank((CharSequence)endTime)) {
            condition = condition + " and (date_attend >= ? and date_attend <= ?)";
        }
        String sql = "select a.id, a.attend_id, a.nick_name, a.date_attend, a.consecutive, b.money money_unit, a.bonus_basic, a.bonus_consecutive, a.bonus_vip, a.spin, a.result, b.start_date, b.end_date from user_attendance a INNER JOIN attendance_config b ON a.attend_id = b.id where (1=1)" + condition + " order by date_attend desc, nick_name asc" + paginateCondition;
        String sqlTotalRecord = "select count(a.id) totalRecord from user_attendance a INNER JOIN attendance_config b ON a.attend_id = b.id where (1=1)" + condition;
        String sqlTotalMoney = "select sum(a.bonus_basic + a.bonus_consecutive + a.bonus_vip) totalMoney from user_attendance a INNER JOIN attendance_config b ON a.attend_id = b.id where (1=1)" + condition;
        String sqlTotalPlayer = "select count(*) totalPlayer from (select a.nick_name from user_attendance a INNER JOIN attendance_config b ON a.attend_id = b.id where (1=1)" + condition + " GROUP BY nick_name) as totalPlayer";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stmt = conn.prepareStatement(sql);
            PreparedStatement stmtTotalRecord = conn.prepareStatement(sqlTotalRecord);
            PreparedStatement stmtTotalMoney = conn.prepareStatement(sqlTotalMoney);
            PreparedStatement stmtTotalPlayer = conn.prepareStatement(sqlTotalPlayer);
            int index = 1;
            if (attendId > 0) {
                stmt.setInt(index, attendId);
                stmtTotalRecord.setInt(index, attendId);
                stmtTotalMoney.setInt(index, attendId);
                stmtTotalPlayer.setInt(index, attendId);
                ++index;
            }
            if (!StringUtils.isBlank((CharSequence)nickname)) {
                stmt.setString(index, nickname);
                stmtTotalRecord.setString(index, nickname);
                stmtTotalMoney.setString(index, nickname);
                stmtTotalPlayer.setString(index, nickname);
                ++index;
            }
            if (!StringUtils.isBlank((CharSequence)fromTime) && !StringUtils.isBlank((CharSequence)endTime)) {
                stmt.setString(index, fromTime);
                stmtTotalRecord.setString(index, fromTime);
                stmtTotalMoney.setString(index, fromTime);
                stmtTotalPlayer.setString(index, fromTime);
                stmt.setString(++index, endTime);
                stmtTotalRecord.setString(index, endTime);
                stmtTotalMoney.setString(index, endTime);
                stmtTotalPlayer.setString(index, endTime);
                ++index;
            }
            if (-1 != pageIndex && -1 != limit) {
                stmt.setInt(index++, pageIndex * limit);
                stmt.setInt(index++, limit);
            }
            HashMap<String, Object> data = new HashMap<String, Object>();
            ArrayList<UserAttendance> userAttendances = new ArrayList<UserAttendance>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserAttendance userAttendance = new UserAttendance();
                userAttendance.setId(rs.getInt("id"));
                userAttendance.setAttend_id(rs.getInt("attend_id"));
                userAttendance.setNick_name(rs.getString("nick_name"));
                userAttendance.setDate_attend(rs.getString("date_attend"));
                userAttendance.setConsecutive(rs.getInt("consecutive"));
                userAttendance.setBonus_basic(rs.getLong("bonus_basic"));
                userAttendance.setBonus_consecutive(rs.getLong("bonus_consecutive"));
                userAttendance.setBonus_vip(rs.getLong("bonus_vip"));
                userAttendance.setSpin(rs.getString("spin"));
                userAttendance.setResult(rs.getString("result"));
                userAttendances.add(userAttendance);
            }
            data.put("userAttendances", userAttendances);
            rs.close();
            stmt.close();
            ResultSet rsTotalRecord = stmtTotalRecord.executeQuery();
            while (rsTotalRecord.next()) {
                data.put("totalRecord", rsTotalRecord.getLong("totalRecord"));
            }
            rsTotalRecord.close();
            stmtTotalRecord.close();
            ResultSet rsTotalMoney = stmtTotalMoney.executeQuery();
            while (rsTotalMoney.next()) {
                data.put("totalMoney", rsTotalMoney.getLong("totalMoney"));
            }
            rsTotalMoney.close();
            stmtTotalMoney.close();
            ResultSet rsTotalPlayer = stmtTotalPlayer.executeQuery();
            while (rsTotalPlayer.next()) {
                data.put("totalPlayer", rsTotalPlayer.getLong("totalPlayer"));
            }
            rsTotalPlayer.close();
            stmtTotalPlayer.close();
            HashMap<String, Object> hashMap = data;
            return hashMap;
        }
    }
}

