/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.LogTaiXiuSieuTocDAO;
import com.vinplay.dal.entities.taixiu.DetailLogTaiXiuSieuToc;
import com.vinplay.dal.entities.taixiu.LogTaiXiuSieuToc;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogTaiXiuSieuTocDAOImpl
implements LogTaiXiuSieuTocDAO {
    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public List<LogTaiXiuSieuToc> search(String fromTime, String endTime, int status, int page, int maxItem) throws SQLException {
        ArrayList<LogTaiXiuSieuToc> results = new ArrayList<LogTaiXiuSieuToc>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            if (page == -1) {
                page = 1;
            }
            maxItem = maxItem == -1 ? 50 : (maxItem > 1000 ? 1000 : maxItem);
            String paginate = page == -1 || maxItem == -1 ? "" : " limit ?,?";
            String conditionFromTime = fromTime == null || fromTime.trim().isEmpty() ? "" : " and opentime >= ?";
            String conditionEndTime = endTime == null || endTime.trim().isEmpty() ? "" : " and opentime <= ?";
            String conditionStatus = status == -1 ? "" : " and status = ?";
            String sqlCount = "SELECT count(id) total FROM taixiu where (1=1)" + conditionFromTime + conditionEndTime + conditionStatus;
            String sql = "SELECT * FROM taixiu where 1=1 " + conditionFromTime + conditionEndTime + conditionStatus + " ORDER BY id DESC" + paginate;
            PreparedStatement stmt = conn.prepareStatement(sql);
            PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
            int index = 1;
            if (fromTime != null && !fromTime.trim().isEmpty()) {
                stmt.setString(index, fromTime);
                stmtCount.setString(index, fromTime);
                ++index;
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                stmt.setString(index, endTime);
                stmtCount.setString(index, endTime);
                ++index;
            }
            if (status != -1) {
                stmt.setInt(index, status);
                stmtCount.setInt(index, status);
                ++index;
            }
            if (page != -1 && maxItem != -1) {
                page = page - 1 < 0 ? 0 : page - 1;
                stmt.setInt(index++, page *= maxItem);
                stmt.setInt(index++, maxItem);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LogTaiXiuSieuToc entry = new LogTaiXiuSieuToc();
                entry.id = rs.getLong("id");
                entry.openTime = rs.getString("opentime");
                entry.endTime = rs.getString("endtime");
                entry.status = rs.getInt("status");
                entry.result = rs.getString("result");
                entry.resultAmount = rs.getLong("result_amount");
                entry.twin = rs.getLong("twin");
                results.add(entry);
            }
            ResultSet rsCount = stmtCount.executeQuery();
            while (rsCount.next()) {
                LogTaiXiuSieuToc entry = new LogTaiXiuSieuToc();
                entry.status = rsCount.getInt("total");
                results.add(entry);
            }
            rs.close();
            rsCount.close();
            stmt.close();
            stmtCount.close();
            ArrayList<LogTaiXiuSieuToc> arrayList = results;
            return arrayList;
        }
        catch (Exception e) {
            return new ArrayList<LogTaiXiuSieuToc>();
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public Map<String, Object> getDetailByLogId(long referenceId, String fromTime, String endTime, int status, int type, int userType, String nickname, int page, int maxItem) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            if (page == -1) {
                page = 1;
            }
            maxItem = maxItem == -1 ? 50 : (maxItem > 1000 ? 1000 : maxItem);
            String conditionRefId = referenceId == -1L ? "" : " and taixiu_id = ?";
            String conditionFromTime = fromTime == null || fromTime.trim().isEmpty() ? "" : " and bettime >= ?";
            String conditionEndTime = endTime == null || endTime.trim().isEmpty() ? "" : " and bettime <= ?";
            String conditionStatus = status == -1 ? "" : " and status = ?";
            String conditionType = type == -1 ? "" : " and typed = ?";
            String conditionUserType = userType == -1 ? "" : " and usertype = ?";
            String conditionNickname = nickname == null || nickname.trim().isEmpty() ? "" : " and nick_name = ?";
            String paginate = page == -1 || maxItem == -1 ? "" : " limit ?,?";
            String sqlCount = "SELECT count(id) total FROM taixiu_record where (1=1)" + conditionRefId + conditionFromTime + conditionEndTime + conditionStatus + conditionType + conditionUserType + conditionNickname;
            String sql = "SELECT * FROM taixiu_record where (1=1) " + conditionRefId + conditionFromTime + conditionEndTime + conditionStatus + conditionType + conditionUserType + conditionNickname + " ORDER BY taixiu_id DESC, id DESC" + paginate;
            String sqlTotalPlayer = "select count(*) total from (SELECT nick_name FROM taixiu_record where (1=1) " + conditionRefId + conditionFromTime + conditionEndTime + conditionStatus + conditionType + conditionUserType + conditionNickname + " GROUP BY nick_name) as total";
            PreparedStatement stmt = conn.prepareStatement(sql);
            PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
            PreparedStatement stmtTotalPlayer = conn.prepareStatement(sqlTotalPlayer);
            int index = 1;
            if (referenceId != -1L) {
                stmt.setLong(index, referenceId);
                stmtCount.setLong(index, referenceId);
                stmtTotalPlayer.setLong(index, referenceId);
                ++index;
            }
            if (fromTime != null && !fromTime.trim().isEmpty()) {
                stmt.setString(index, fromTime + " 00:00:00");
                stmtCount.setString(index, fromTime + " 00:00:00");
                stmtTotalPlayer.setString(index, fromTime + " 00:00:00");
                ++index;
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                stmt.setString(index, endTime + " 23:59:59");
                stmtCount.setString(index, endTime + " 23:59:59");
                stmtTotalPlayer.setString(index, endTime + " 23:59:59");
                ++index;
            }
            if (status != -1) {
                stmt.setInt(index, status);
                stmtCount.setInt(index, status);
                stmtTotalPlayer.setInt(index, status);
                ++index;
            }
            if (type != -1) {
                stmt.setInt(index, type);
                stmtCount.setInt(index, type);
                stmtTotalPlayer.setInt(index, type);
                ++index;
            }
            if (userType != -1) {
                stmt.setInt(index, userType);
                stmtCount.setInt(index, userType);
                stmtTotalPlayer.setInt(index, userType);
                ++index;
            }
            if (nickname != null && !nickname.trim().isEmpty()) {
                stmt.setString(index, nickname);
                stmtCount.setString(index, nickname);
                stmtTotalPlayer.setString(index, nickname);
                ++index;
            }
            if (page != -1 && maxItem != -1) {
                page = page - 1 < 0 ? 0 : page - 1;
                stmt.setInt(index++, page *= maxItem);
                stmt.setInt(index++, maxItem);
            }
            ResultSet rs = stmt.executeQuery();
            ArrayList<DetailLogTaiXiuSieuToc> details = new ArrayList<DetailLogTaiXiuSieuToc>();
            while (rs.next()) {
                DetailLogTaiXiuSieuToc entry = new DetailLogTaiXiuSieuToc();
                entry.id = rs.getLong("id");
                entry.referenceId = rs.getLong("taixiu_id");
                entry.userId = rs.getLong("user_id");
                entry.betAmount = rs.getLong("betamount");
                entry.winAmount = rs.getLong("winamount");
                entry.typed = rs.getInt("typed");
                entry.status = rs.getInt("status");
                entry.betTime = rs.getString("bettime");
                entry.result = rs.getString("result");
                entry.description = rs.getString("description");
                entry.refundAmount = rs.getLong("refundamount");
                entry.ip = rs.getString("ip");
                entry.updateDate = rs.getString("updatedate");
                entry.userType = rs.getInt("usertype");
                entry.nickname = rs.getString("nick_name");
                details.add(entry);
            }
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("details", details);
            rs.close();
            stmt.close();
            ResultSet rsCount = stmtCount.executeQuery();
            long totalRecord = 0L;
            while (rsCount.next()) {
                totalRecord = rsCount.getLong("total");
            }
            data.put("totalRecord", totalRecord);
            rsCount.close();
            stmtCount.close();
            ResultSet rsTotalPlayer = stmtTotalPlayer.executeQuery();
            long totalPlayer = 0L;
            while (rsTotalPlayer.next()) {
                totalPlayer = rsTotalPlayer.getLong("total");
            }
            data.put("totalPlayer", totalPlayer);
            rsCount.close();
            stmtCount.close();
            HashMap<String, Object> hashMap = data;
            return hashMap;
        }
        catch (Exception e) {
            return new HashMap<String, Object>();
        }
    }
}

