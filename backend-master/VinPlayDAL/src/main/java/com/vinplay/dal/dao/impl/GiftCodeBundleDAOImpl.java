/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.GiftCodeBundleDAO;
import com.vinplay.dal.entities.giftcode.BundleUsedGiftCodeModel;
import com.vinplay.dal.entities.giftcode.GiftCodeBundleModel;
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

public class GiftCodeBundleDAOImpl
implements GiftCodeBundleDAO {
    @Override
    public List<GiftCodeBundleModel> showListGiftCodeBundle(String created_by, int page, int maxItem) throws SQLException {
        ArrayList<GiftCodeBundleModel> listGiftCodeBundle = new ArrayList<GiftCodeBundleModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            page = page - 1 < 0 ? 0 : page - 1;
            int index = 1;
            Boolean b_created_by = created_by == null || created_by.trim().isEmpty();
            String sql = "Select * from vinplay.gift_code_bundles where 1=1 " + (b_created_by != false ? "" : " and created_by = ?") + " order by id desc limit ?,?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_created_by.booleanValue()) {
                stmt.setString(index++, created_by);
            }
            stmt.setInt(index++, page * maxItem);
            stmt.setInt(index, maxItem);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GiftCodeBundleModel giftCodeBundleModel = new GiftCodeBundleModel();
                giftCodeBundleModel.setId(rs.getInt("id"));
                giftCodeBundleModel.setName(rs.getString("name"));
                giftCodeBundleModel.setCreated_by(rs.getString("created_by"));
                giftCodeBundleModel.setCreated_at(rs.getDate("created_at"));
                giftCodeBundleModel.setUpdated_at(rs.getDate("updated_at"));
                listGiftCodeBundle.add(giftCodeBundleModel);
            }
            rs.close();
            stmt.close();
        }
        return listGiftCodeBundle;
    }

    @Override
    public GiftCodeBundleModel showGiftCodeBundle(String created_by, String bundle_id) throws SQLException {
        GiftCodeBundleModel giftCodeBundleModel = new GiftCodeBundleModel();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            int index = 1;
            Boolean b_created_by = created_by == null || created_by.trim().isEmpty();
            Boolean b_bundle_id = bundle_id == null || bundle_id.trim().isEmpty();
            String sql = "Select * from vinplay.gift_code_bundles where 1=1 " + (b_created_by != false ? "" : " and created_by = ?") + (b_bundle_id != false ? "" : " and id = ?");
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_created_by.booleanValue()) {
                stmt.setString(index++, created_by);
            }
            if (!b_bundle_id.booleanValue()) {
                stmt.setString(index++, bundle_id);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                giftCodeBundleModel.setId(rs.getInt("id"));
                giftCodeBundleModel.setName(rs.getString("name"));
                giftCodeBundleModel.setCreated_by(rs.getString("created_by"));
                giftCodeBundleModel.setCreated_at(rs.getDate("created_at"));
                giftCodeBundleModel.setUpdated_at(rs.getDate("updated_at"));
            }
            rs.close();
            stmt.close();
        }
        return giftCodeBundleModel;
    }

    @Override
    public Long countGiftCodeBundle(String created_by) throws SQLException {
        Long count = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            int index = 1;
            Boolean b_created_by = created_by == null || created_by.trim().isEmpty();
            String sql = "Select COUNT(*) as cnt from vinplay.gift_code_bundles where 1=1 " + (b_created_by != false ? "" : " and created_by = ?");
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_created_by.booleanValue()) {
                stmt.setString(index++, created_by);
            }
            if ((rs = stmt.executeQuery()).next()) {
                count = rs.getLong("cnt");
            }
            rs.close();
            stmt.close();
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
    public List<BundleUsedGiftCodeModel> showUsedGiftCodeInBundle(int bundleId) throws SQLException {
        ArrayList<BundleUsedGiftCodeModel> listGiftCode = new ArrayList<BundleUsedGiftCodeModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "select gc.id, gc.giftcode, gcu.username, gcu.created_at from vinplay.gift_codes as gc inner join vinplay.gift_code_useds as gcu on gc.id = gcu.giftcode_id where gc.bundle_id = ? order by gcu.created_at desc";
            int index = 1;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(index, bundleId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                BundleUsedGiftCodeModel usedGiftCodeModel = new BundleUsedGiftCodeModel();
                usedGiftCodeModel.setId(rs.getInt("id"));
                usedGiftCodeModel.setUsername(rs.getString("username"));
                usedGiftCodeModel.setGiftcode(rs.getString("giftcode"));
                usedGiftCodeModel.setCreated_at(rs.getTimestamp("created_at"));
                listGiftCode.add(usedGiftCodeModel);
            }
            rs.close();
            stmt.close();
        }
        return listGiftCode;
    }
}

