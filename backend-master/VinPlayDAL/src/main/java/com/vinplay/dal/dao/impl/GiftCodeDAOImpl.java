/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.GiftCodeDAO;
import com.vinplay.dal.entities.giftcode.GiftCodeModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GiftCodeDAOImpl
implements GiftCodeDAO {
    @Override
    public List<GiftCodeModel> showListGiftCode(String giftcode, String user_name, String created_by, Integer event, String startTime, String endTime, int page, int maxItem) throws SQLException {
        ArrayList<GiftCodeModel> listGiftCode = new ArrayList<GiftCodeModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            Date time;
            page = page - 1 < 0 ? 0 : page - 1;
            int index = 1;
            Boolean b_giftcode = giftcode == null || giftcode.trim().isEmpty();
            Boolean b_user_name = user_name == null || user_name.trim().isEmpty();
            Boolean b_created_by = created_by == null || created_by.trim().isEmpty();
            Boolean b_event = event == null;
            Boolean b_startTime = startTime == null || startTime.trim().isEmpty();
            Boolean b_endTime = endTime == null || endTime.trim().isEmpty();
            String sql = "Select * from vinplay.gift_codes where 1=1 " + (b_giftcode != false ? "" : " and giftcode = ?") + (b_user_name != false ? "" : " and user_name = ?") + (b_created_by != false ? "" : " and created_by = ?") + (b_event != false ? "" : " and event = ?") + (b_startTime != false ? "" : " and created_at >= ?") + (b_endTime != false ? "" : " and created_at <= ?") + " order by id desc limit ?,?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_giftcode.booleanValue()) {
                stmt.setString(index++, giftcode);
            }
            if (!b_user_name.booleanValue()) {
                stmt.setString(index++, user_name);
            }
            if (!b_created_by.booleanValue()) {
                stmt.setString(index++, created_by);
            }
            if (!b_event.booleanValue()) {
                stmt.setInt(index++, event);
            }
            if (!b_startTime.booleanValue()) {
                time = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(startTime);
                stmt.setDate(index++, new java.sql.Date(time.getTime()));
            }
            if (!b_endTime.booleanValue()) {
                time = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(endTime);
                stmt.setDate(index++, new java.sql.Date(time.getTime()));
            }
            stmt.setInt(index++, page * maxItem);
            stmt.setInt(index, maxItem);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GiftCodeModel giftCodeModel = new GiftCodeModel();
                giftCodeModel.setId(rs.getInt("id"));
                giftCodeModel.setGiftcode(rs.getString("giftcode"));
                giftCodeModel.setType(rs.getInt("type"));
                giftCodeModel.setMoney(rs.getLong("money"));
                giftCodeModel.setTime_used(rs.getInt("time_used"));
                giftCodeModel.setMax_use(rs.getInt("max_use"));
                giftCodeModel.setFrom(rs.getDate("from"));
                giftCodeModel.setExpired(rs.getDate("exprired"));
                giftCodeModel.setCreated_at(rs.getDate("created_at"));
                giftCodeModel.setCreated_by(rs.getString("created_by"));
                giftCodeModel.setEvent(rs.getInt("event"));
                giftCodeModel.setUser_name(rs.getString("user_name"));
                listGiftCode.add(giftCodeModel);
            }
            rs.close();
            stmt.close();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return listGiftCode;
    }

    @Override
    public Long countGiftCode(String giftcode, String user_name, String created_by, Integer event, String startTime, String endTime) throws SQLException {
        Long count = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            Date time;
            int index = 1;
            Boolean b_giftcode = giftcode == null || giftcode.trim().isEmpty();
            Boolean b_user_name = user_name == null || user_name.trim().isEmpty();
            Boolean b_created_by = created_by == null || created_by.trim().isEmpty();
            Boolean b_event = event == null;
            Boolean b_startTime = startTime == null || startTime.trim().isEmpty();
            Boolean b_endTime = endTime == null || endTime.trim().isEmpty();
            String sql = "Select count(*) as cnt from vinplay.gift_codes where 1=1 " + (b_giftcode != false ? "" : "and giftcode = ?") + (b_user_name != false ? "" : "and user_name = ?") + (b_created_by != false ? "" : " and created_by = ?") + (b_event != false ? "" : " and event = ?") + (b_startTime != false ? "" : " and created_at >= ?") + (b_endTime != false ? "" : " and created_at <= ?");
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_giftcode.booleanValue()) {
                stmt.setString(index++, giftcode);
            }
            if (!b_user_name.booleanValue()) {
                stmt.setString(index++, user_name);
            }
            if (!b_created_by.booleanValue()) {
                stmt.setString(index++, created_by);
            }
            if (!b_event.booleanValue()) {
                stmt.setInt(index++, event);
            }
            if (!b_startTime.booleanValue()) {
                time = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(startTime);
                stmt.setDate(index++, new java.sql.Date(time.getTime()));
            }
            if (!b_endTime.booleanValue()) {
                time = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(endTime);
                stmt.setDate(index++, new java.sql.Date(time.getTime()));
            }
            if ((rs = stmt.executeQuery()).next()) {
                count = rs.getLong("cnt");
            }
            rs.close();
            stmt.close();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public Long countValueGiftCode(String giftcode, String user_name, String created_by, Integer event, String startTime, String endTime) throws SQLException {
        Long countValue = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            Date time;
            int index = 1;
            Boolean b_giftcode = giftcode == null || giftcode.trim().isEmpty();
            Boolean b_user_name = user_name == null || user_name.trim().isEmpty();
            Boolean b_created_by = created_by == null || created_by.trim().isEmpty();
            Boolean b_event = event == null;
            Boolean b_startTime = startTime == null || startTime.trim().isEmpty();
            Boolean b_endTime = endTime == null || endTime.trim().isEmpty();
            String sql = "Select SUM(gift_codes.money) as cnt_value from vinplay.gift_codes where 1=1 " + (b_giftcode != false ? "" : "and giftcode = ?") + (b_user_name != false ? "" : "and user_name = ?") + (b_created_by != false ? "" : " and created_by = ?") + (b_event != false ? "" : " and event = ?") + (b_startTime != false ? "" : " and created_at >= ?") + (b_endTime != false ? "" : " and created_at <= ?");
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_giftcode.booleanValue()) {
                stmt.setString(index++, giftcode);
            }
            if (!b_user_name.booleanValue()) {
                stmt.setString(index++, user_name);
            }
            if (!b_created_by.booleanValue()) {
                stmt.setString(index++, created_by);
            }
            if (!b_event.booleanValue()) {
                stmt.setInt(index++, event);
            }
            if (!b_startTime.booleanValue()) {
                time = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(startTime);
                stmt.setDate(index++, new java.sql.Date(time.getTime()));
            }
            if (!b_endTime.booleanValue()) {
                time = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(endTime);
                stmt.setDate(index++, new java.sql.Date(time.getTime()));
            }
            if ((rs = stmt.executeQuery()).next()) {
                countValue = rs.getLong("cnt_value");
            }
            rs.close();
            stmt.close();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return countValue;
    }

    @Override
    public Long countUsedGiftCode(String bundleId) throws SQLException {
        Long countValue = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            int index = 1;
            Boolean b_bundleId = bundleId == null || bundleId.trim().isEmpty();
            String sql = "Select COUNT(*) as cnt_value from vinplay.gift_codes where 1=1 " + (b_bundleId != false ? "" : " and bundle_id = ?") + " AND time_used > 0";
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_bundleId.booleanValue()) {
                stmt.setString(index++, bundleId);
            }
            if ((rs = stmt.executeQuery()).next()) {
                countValue = rs.getLong("cnt_value");
            }
            rs.close();
            stmt.close();
        }
        return countValue;
    }
}

