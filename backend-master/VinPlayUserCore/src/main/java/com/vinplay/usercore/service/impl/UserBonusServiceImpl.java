/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.service.UserBonusService;
import com.vinplay.vbee.common.models.UserBonusModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class UserBonusServiceImpl
implements UserBonusService {
    private static final Logger logger = Logger.getLogger((String)"user_core");

    @Override
    public void insertBonus(UserBonusModel model) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = " INSERT INTO user_bonus (nick_name,bonus_type,amount,create_date,ip, bonus_name)  VALUES  (?, ?, ?, ?, ? ,?) ";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, model.getNick_name());
            stm.setInt(2, model.getBonus_type());
            stm.setDouble(3, model.getAmount());
            stm.setString(4, DateTimeUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss"));
            stm.setString(5, model.getIp());
            stm.setString(6, model.getBonusName());
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            logger.error(e);
        }
    }

    @Override
    public List<UserBonusModel> search(String nickName, int bonusType, int page, int totalrecord, String fromTime, String toTime) throws SQLException {
        ArrayList<UserBonusModel> userB = new ArrayList<UserBonusModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM user_bonus WHERE 1=1 ";
            int num_start = (page - 1) * totalrecord;
            int index = 1;
            String limit = " LIMIT " + num_start + ", " + totalrecord + "";
            if (nickName != null && !"".equals(nickName)) {
                sql = sql + " and nick_name like ?";
            }
            if (fromTime != null && !"".equals(fromTime)) {
                sql = sql + " and create_date >= ?";
            }
            if (toTime != null && !"".equals(toTime)) {
                sql = sql + " and create_date <= ?";
            }
            if (bonusType >= 0) {
                sql = sql + " and bonus_type = ?";
            }
            sql = sql + " order by id DESC" + limit;
            PreparedStatement stm = conn.prepareStatement(sql);
            if (nickName != null && !"".equals(nickName)) {
                stm.setString(index, '%' + nickName + '%');
                ++index;
            }
            if (fromTime != null && !"".equals(fromTime)) {
                stm.setString(index, fromTime);
                ++index;
            }
            if (toTime != null && !"".equals(toTime)) {
                stm.setString(index, toTime);
                ++index;
            }
            if (bonusType >= 0) {
                stm.setInt(index, bonusType);
                ++index;
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                UserBonusModel u = new UserBonusModel(rs);
                userB.add(u);
            }
            rs.close();
            stm.close();
        }
        return userB;
    }

    @Override
    public List<UserBonusModel> search(String nick_name, String ip, Integer bonusType, String fromTime, String endTime, int page, int maxItem) throws SQLException {
        ArrayList<UserBonusModel> userB = new ArrayList<UserBonusModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            page = page - 1 < 0 ? 0 : page - 1;
            String sql = "select * from vinplay.user_bonus where 1=1 " + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ? ") + (ip == null || ip.isEmpty() ? "" : " and ip = ? ") + (bonusType == null ? "" : " and bonus_type = ? ") + (fromTime == null || fromTime.isEmpty() ? "" : " and create_date >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and create_date <= ? ") + " order by created_at desc limit ?,?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            int index = 1;
            if (nick_name != null && !nick_name.isEmpty()) {
                stmt.setString(index++, nick_name);
            }
            if (ip != null && !ip.isEmpty()) {
                stmt.setString(index++, ip);
            }
            if (bonusType != null) {
                stmt.setInt(index++, bonusType);
            }
            if (fromTime != null && !fromTime.isEmpty()) {
                stmt.setString(index++, fromTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                stmt.setString(index++, endTime);
            }
            stmt.setInt(index++, page * maxItem);
            stmt.setInt(index, maxItem);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserBonusModel userBonusModel = new UserBonusModel();
                userBonusModel.setBonus_type(rs.getInt("bonus_type"));
                userBonusModel.setBonusName(rs.getString("bonus_name"));
                userBonusModel.setAmount(rs.getDouble("amount"));
                userBonusModel.setCreate_date(rs.getString("create_date"));
                userBonusModel.setIp(rs.getString("ip"));
                userBonusModel.setNick_name(rs.getString("nick_name"));
                userB.add(userBonusModel);
            }
            rs.close();
            stmt.close();
            ArrayList<UserBonusModel> arrayList = userB;
            return arrayList;
        }
    }

    @Override
    public Long count(String nick_name, String ip, Integer bonusType, String fromTime, String endTime) throws SQLException {
        Long count = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "select count(*) as cnt from vinplay.user_bonus where 1=1 " + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ? ") + (ip == null || ip.isEmpty() ? "" : " and ip = ? ") + (bonusType == null ? "" : " and bonus_type = ? ") + (fromTime == null || fromTime.isEmpty() ? "" : " and create_date >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and create_date <= ? ");
            PreparedStatement stmt = conn.prepareStatement(sql);
            int index = 1;
            if (nick_name != null && !nick_name.isEmpty()) {
                stmt.setString(index++, nick_name);
            }
            if (ip != null && !ip.isEmpty()) {
                stmt.setString(index++, ip);
            }
            if (bonusType != null) {
                stmt.setInt(index++, bonusType);
            }
            if (fromTime != null && !fromTime.isEmpty()) {
                stmt.setString(index++, fromTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                stmt.setString(index++, endTime);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                count = rs.getLong("cnt");
            }
            rs.close();
            stmt.close();
            Long l = count;
            return l;
        }
    }

    @Override
    public double sumAmount(String nick_name, String ip, Integer bonusType, String fromTime, String endTime) throws SQLException {
        double count = 0.0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "select sum(user_bonus.amount) as amount from vinplay.user_bonus where 1=1 " + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ? ") + (ip == null || ip.isEmpty() ? "" : " and ip = ? ") + (bonusType == null ? "" : " and bonus_type = ? ") + (fromTime == null || fromTime.isEmpty() ? "" : " and create_date >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and create_date <= ? ");
            PreparedStatement stmt = conn.prepareStatement(sql);
            int index = 1;
            if (nick_name != null && !nick_name.isEmpty()) {
                stmt.setString(index++, nick_name);
            }
            if (ip != null && !ip.isEmpty()) {
                stmt.setString(index++, ip);
            }
            if (bonusType != null) {
                stmt.setInt(index++, bonusType);
            }
            if (fromTime != null && !fromTime.isEmpty()) {
                stmt.setString(index++, fromTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                stmt.setString(index++, endTime);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                count = rs.getDouble("amount");
            }
            rs.close();
            stmt.close();
            double d = count;
            return d;
        }
    }

    @Override
    public boolean isReceivedBonus(String nickName, int typeBonus) {
        long count = 0L;
        String sql = "SELECT count(*)  as cnt from user_bonus WHERE nick_name=? and bonus_type =? ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, nickName);
            stm.setInt(2, typeBonus);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                count = rs.getInt("cnt");
            }
        }
        catch (Exception e) {
            logger.error(e);
        }
        return count > 0L;
    }

    @Override
    public boolean isSameIP(String ip, int bonusType) {
        long count = 0L;
        String sql = "SELECT count(id)  as cnt from user_bonus WHERE bonus_type =? and ip like '%" + ip + "%'";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, bonusType);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                count = rs.getInt("cnt");
            }
        }
        catch (Exception e) {
            logger.error(e);
        }
        return count > 0L;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public boolean checkConditionsByCurrentTime(String nickname) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentDateTime = LocalDateTime.now().plusSeconds(-1L).format(formatter);
        String fromDateTime = LocalDateTime.now().plusSeconds(-1L).plusDays(-7L).format(formatter);
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "select count(id) as total from log_report_user where (time_report < '" + currentDateTime + "' and time_report > '" + fromDateTime + "' and deposit > 99999 and nick_name = ?) and (wm + ibc + ag + tlmn + bacay + xocdia + minipoker + slot_pokemon + baucua + taixiu + caothap + slot_bitcoin + slot_taydu + slot_angrybird + slot_thantai + slot_thethao + cmd + slot_chiemtinh + taixiu_st + fish + slot_thanbai + ebet + sbo + slot_bikini + slot_galaxy) > 0";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nickname);
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                try {
                    count = rs.getInt("total");
                }
                catch (Exception e) {
                    count = 0;
                }
            }
            rs.close();
            stmt.close();
            if (conn != null) {
                conn.close();
            }
            boolean bl = count > 0;
            return bl;
        }
        catch (SQLException e) {
            return false;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public boolean checkExit(String nickname, int bonusType) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
            String fromDate = formatter.format(new Date()) + " 00:00:00";
            String endDate = formatter.format(new Date()) + " 23:59:59";
            String sql = "SELECT count(id) total from user_bonus WHERE bonus_type = ? and nick_name = ? and created_at >= ? and created_at <= ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bonusType);
            stmt.setString(2, nickname);
            stmt.setString(3, fromDate);
            stmt.setString(4, endDate);
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                try {
                    count = rs.getInt("total");
                }
                catch (Exception e) {
                    count = 0;
                }
            }
            rs.close();
            stmt.close();
            if (conn != null) {
                conn.close();
            }
            boolean bl = count > 0;
            return bl;
        }
        catch (SQLException e) {
            return false;
        }
    }
}

