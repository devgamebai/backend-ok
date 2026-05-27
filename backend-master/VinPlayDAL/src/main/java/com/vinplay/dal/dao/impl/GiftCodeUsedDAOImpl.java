/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.GiftCodeUsedDAO;
import com.vinplay.dal.entities.giftcode.GiftCodeUsedModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GiftCodeUsedDAOImpl
implements GiftCodeUsedDAO {
    @Override
    public List<GiftCodeUsedModel> showListGiftCodeUsed(String gid, String nickname, Integer type, Integer event, int flagtime, String startTime, String endTime, int page, int maxItem) throws SQLException {
        ArrayList<GiftCodeUsedModel> listGiftCodeUsed = new ArrayList<GiftCodeUsedModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            page = page - 1 < 0 ? 0 : page - 1;
            String from = (startTime == null || startTime.isEmpty() ? "" : " and from >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and from <= ? ");
            String exprired = (startTime == null || startTime.isEmpty() ? "" : " and exprired >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and exprired <= ? ");
            String sql = "select gift_code_useds.*,g.giftcode, g.type, g.money, g.time_used, g.exprired, g.max_use, g.from from vinplay.gift_code_useds join vinplay.gift_codes g on g.id = gift_code_useds.giftcode_id where 1=1 " + (gid == null || gid.isEmpty() ? "" : " and giftcode_id = ? ") + (nickname == null || nickname.isEmpty() ? "" : " and username = ? ") + (flagtime == 1 ? from : "") + (flagtime == 2 ? exprired : "") + (type == null ? "" : " and type = ? ") + (event == null ? "" : " and event = ? ") + " order by gift_code_useds.created_at desc limit ?,?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            int index = 1;
            if (gid != null && !gid.isEmpty()) {
                stmt.setString(index++, gid);
            }
            if (nickname != null && !nickname.isEmpty()) {
                stmt.setString(index++, nickname);
            }
            if (flagtime == 1) {
                if (startTime != null && !startTime.isEmpty()) {
                    stmt.setString(index++, startTime);
                }
                if (endTime != null && !endTime.isEmpty()) {
                    stmt.setString(index++, endTime);
                }
            }
            if (flagtime == 2) {
                if (startTime != null && !startTime.isEmpty()) {
                    stmt.setString(index++, startTime);
                }
                if (endTime != null && !endTime.isEmpty()) {
                    stmt.setString(index++, endTime);
                }
            }
            if (type != null) {
                stmt.setInt(index++, type);
            }
            if (event != null) {
                stmt.setInt(index++, event);
            }
            stmt.setInt(index++, page * maxItem);
            stmt.setInt(index, maxItem);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GiftCodeUsedModel giftCodeUsedModel = new GiftCodeUsedModel();
                giftCodeUsedModel.setGiftcode_id(rs.getInt("giftcode_id"));
                giftCodeUsedModel.setUsername(rs.getString("username"));
                giftCodeUsedModel.setCreated_at(rs.getTimestamp("created_at"));
                giftCodeUsedModel.setUpdated_at(rs.getTimestamp("updated_at"));
                giftCodeUsedModel.setId_number(rs.getString("id_number"));
                giftCodeUsedModel.giftcode = rs.getString("giftcode");
                giftCodeUsedModel.type = rs.getInt("type");
                giftCodeUsedModel.money = rs.getInt("money");
                giftCodeUsedModel.time_used = rs.getInt("time_used");
                giftCodeUsedModel.max_use = rs.getInt("max_use");
                giftCodeUsedModel.from = rs.getTimestamp("from");
                giftCodeUsedModel.exprired = rs.getTimestamp("exprired");
                giftCodeUsedModel.event = rs.getInt("event");
                listGiftCodeUsed.add(giftCodeUsedModel);
            }
            rs.close();
            stmt.close();
            ArrayList<GiftCodeUsedModel> arrayList = listGiftCodeUsed;
            return arrayList;
        }
    }

    @Override
    public long countGiftCodeUsed(String gid, String nickname, Integer type, Integer event, int flagtime, String startTime, String endTime) throws SQLException {
        long count = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            String from = (startTime == null || startTime.isEmpty() ? "" : " and from >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and from <= ? ");
            String exprired = (startTime == null || startTime.isEmpty() ? "" : " and exprired >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and exprired <= ? ");
            String sql = "select count(*) as cnt from vinplay.gift_code_useds where 1=1 " + (gid == null || gid.isEmpty() ? "" : " and giftcode_id = ? ") + (nickname == null || nickname.isEmpty() ? "" : " and username = ? ") + (flagtime == 1 ? from : "") + (flagtime == 2 ? exprired : "") + (type == null ? "" : " and type = ? ") + (event == null ? "" : " and event = ? ");
            PreparedStatement stmt = conn.prepareStatement(sql);
            int index = 1;
            if (gid != null && !gid.isEmpty()) {
                stmt.setString(index++, gid);
            }
            if (nickname != null && !nickname.isEmpty()) {
                stmt.setString(index, nickname);
            }
            if (flagtime == 1) {
                if (startTime != null && !startTime.isEmpty()) {
                    stmt.setString(index++, startTime);
                }
                if (endTime != null && !endTime.isEmpty()) {
                    stmt.setString(index++, endTime);
                }
            }
            if (flagtime == 2) {
                if (startTime != null && !startTime.isEmpty()) {
                    stmt.setString(index++, startTime);
                }
                if (endTime != null && !endTime.isEmpty()) {
                    stmt.setString(index++, endTime);
                }
            }
            if (type != null) {
                stmt.setInt(index++, type);
            }
            if (event != null) {
                stmt.setInt(index++, event);
            }
            if ((rs = stmt.executeQuery()).next()) {
                count = rs.getLong("cnt");
            }
            rs.close();
            stmt.close();
            long l = count;
            return l;
        }
    }

    @Override
    public long countValueGiftCodeUsed(String gid, String nickname, Integer type, Integer event, int flagtime, String startTime, String endTime) throws SQLException {
        long countValue = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            String from = (startTime == null || startTime.isEmpty() ? "" : " and from >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and from <= ? ");
            String exprired = (startTime == null || startTime.isEmpty() ? "" : " and exprired >= ? ") + (endTime == null || endTime.isEmpty() ? "" : " and exprired <= ? ");
            String sql = "select SUM(g.money) as cnt_value from vinplay.gift_code_useds join vinplay.gift_codes g on g.id = gift_code_useds.giftcode_id where 1=1 " + (gid == null || gid.isEmpty() ? "" : " and giftcode_id = ? ") + (nickname == null || nickname.isEmpty() ? "" : " and username = ? ") + (flagtime == 1 ? from : "") + (flagtime == 2 ? exprired : "") + (type == null ? "" : " and type = ? ") + (event == null ? "" : " and event = ? ");
            PreparedStatement stmt = conn.prepareStatement(sql);
            int index = 1;
            if (gid != null && !gid.isEmpty()) {
                stmt.setString(index++, gid);
            }
            if (nickname != null && !nickname.isEmpty()) {
                stmt.setString(index, nickname);
            }
            if (flagtime == 1) {
                if (startTime != null && !startTime.isEmpty()) {
                    stmt.setString(index++, startTime);
                }
                if (endTime != null && !endTime.isEmpty()) {
                    stmt.setString(index++, endTime);
                }
            }
            if (flagtime == 2) {
                if (startTime != null && !startTime.isEmpty()) {
                    stmt.setString(index++, startTime);
                }
                if (endTime != null && !endTime.isEmpty()) {
                    stmt.setString(index++, endTime);
                }
            }
            if (type != null) {
                stmt.setInt(index++, type);
            }
            if (event != null) {
                stmt.setInt(index++, event);
            }
            if ((rs = stmt.executeQuery()).next()) {
                countValue = rs.getLong("cnt_value");
            }
            rs.close();
            stmt.close();
            long l = countValue;
            return l;
        }
    }
}

