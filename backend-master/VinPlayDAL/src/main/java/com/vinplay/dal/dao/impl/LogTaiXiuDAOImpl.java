/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.TaiXiuDetailReponse
 *  com.vinplay.vbee.common.response.TaiXiuResponse
 *  com.vinplay.vbee.common.response.TaiXiuResultResponse
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.LogTaiXiuDAO;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.TaiXiuDetailReponse;
import com.vinplay.vbee.common.response.TaiXiuResponse;
import com.vinplay.vbee.common.response.TaiXiuResultResponse;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogTaiXiuDAOImpl
implements LogTaiXiuDAO {
    @Override
    public List<TaiXiuResponse> listLogTaiXiu(String referentId, String userName, String betSide, String moneyType, String timeStart, String timeEnd, int page) throws SQLException {
        ArrayList<TaiXiuResponse> results = new ArrayList<TaiXiuResponse>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            int num_start = (page - 1) * 50;
            int num_end = 50;
            // SUN: switch to the live MD5 transaction table. Non-MD5 table is
            // dead since 2026-03-05 — only 747 stale rows, no new writes.
            String query = "SELECT reference_id,user_id,bet_value,user_name,bet_side,total_prize,total_refund,total_exchange,money_type,timestamp FROM transaction_tai_xiu_md5 where 1=1 ";
            String condition = "";
            String limit = " ORDER BY reference_id DESC LIMIT " + num_start + ", " + 50 + "";
            if (referentId != null && !referentId.equals("")) {
                condition = condition + " AND reference_id=" + referentId;
            }
            if (userName != null && !userName.equals("")) {
                condition = condition + " AND user_name = '" + userName + "'";
            }
            if (betSide != null && !betSide.equals("")) {
                condition = condition + " AND bet_side=" + betSide;
            }
            if (moneyType != null && !moneyType.equals("")) {
                condition = condition + " AND money_type=" + moneyType;
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                condition = condition + " AND timestamp BETWEEN '" + timeStart + "' AND '" + timeEnd + "'";
            }
            String sql = "SELECT reference_id,user_id,bet_value,user_name,bet_side,total_prize,total_refund,total_exchange,money_type,timestamp FROM transaction_tai_xiu_md5 where 1=1 " + condition + limit;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                TaiXiuResponse entry = new TaiXiuResponse();
                entry.referenceId = rs.getLong("reference_id");
                entry.user_id = rs.getInt("user_id");
                entry.user_name = rs.getString("user_name");
                entry.bet_value = rs.getInt("bet_value");
                entry.bet_side = rs.getInt("bet_side");
                entry.total_prize = rs.getLong("total_prize");
                entry.total_refund = rs.getLong("total_refund");
                entry.total_exchange = rs.getLong("total_exchange");
                entry.money_type = rs.getInt("money_type");
                entry.time_log = VinPlayUtils.formatDate((String)rs.getString("timestamp"));
                results.add(entry);
            }
            rs.close();
            stmt.close();
        }
        return results;
    }

    @Override
    public int countLogTaiXiu(String referentId, String userName, String betSide, String moneyType, String timeStart, String timeEnd) throws SQLException {
        int res = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            ResultSet rs;
            String sql;
            PreparedStatement stmt;
            String query = "SELECT count(*) as total FROM transaction_tai_xiu_md5 where 1=1 ";
            String condition = "";
            // SUN: fix inverted filter checks — original decompile had `.equals("")`
            // which only appended the WHERE clause when the param was empty.
            // That made countLogTaiXiu always return the FULL table count even
            // when callers filtered by reference_id / user_name / bet_side, so
            // pagination metadata never matched the filtered list. Flipped each
            // branch to the intended `!equals("")` semantics (same as listLogTaiXiu).
            if (referentId != null && !referentId.equals("")) {
                condition = condition + " AND reference_id=" + referentId;
            }
            if (userName != null && !userName.equals("")) {
                condition = condition + " AND user_name = '" + userName + "'";
            }
            if (betSide != null && !betSide.equals("")) {
                condition = condition + " AND bet_side=" + betSide;
            }
            if (moneyType != null && !moneyType.equals("")) {
                condition = condition + " AND money_type=" + moneyType;
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                condition = condition + " AND timestamp BETWEEN '" + timeStart + "' AND '" + timeEnd + "'";
            }
            if ((rs = (stmt = conn.prepareStatement(sql = "SELECT count(*) as total FROM transaction_tai_xiu_md5 where 1=1 " + condition)).executeQuery()).next()) {
                res = rs.getInt("total");
            }
            rs.close();
            stmt.close();
        }
        return res;
    }

    @Override
    public List<TaiXiuDetailReponse> getLogTaiXiuDetail(String referent_id, String betSide, String money_type, String nickName, int page) throws SQLException {
        ArrayList<TaiXiuDetailReponse> results = new ArrayList<TaiXiuDetailReponse>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            int num_start = (page - 1) * 50;
            int num_end = 50;
            // SUN: admin history reads — point at the live MD5 table instead of the
            // legacy transaction_detail_tai_xiu (dead since 2026-03-05 when the game
            // server stopped writing to non-MD5 tables). Same schema, _md5 suffix.
            String query = "SELECT reference_id,transaction_code,user_id,user_name,bet_value,bet_side,prize,refund,input_time,money_type,timestamp FROM transaction_detail_tai_xiu_md5 where 1=1 ";
            String condition = "";
            String limit = " ORDER BY reference_id DESC LIMIT " + num_start + ", " + 50 + "";
            if (referent_id != null && !referent_id.equals("")) {
                condition = condition + " AND reference_id=" + referent_id;
            }
            if (betSide != null && !betSide.equals("")) {
                condition = condition + " AND bet_side=" + betSide;
            }
            if (nickName != null && !nickName.equals("")) {
                condition = condition + " AND user_name = '" + nickName + "'";
            }
            condition = condition + " AND money_type=" + money_type;
            String sql = "SELECT reference_id,transaction_code,user_id,user_name,bet_value,bet_side,prize,refund,input_time,money_type,timestamp FROM transaction_detail_tai_xiu_md5 where 1=1 " + condition + limit;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                TaiXiuDetailReponse entry = new TaiXiuDetailReponse();
                entry.reference_id = rs.getLong("reference_id");
                entry.transaction_code = rs.getString("transaction_code");
                entry.user_id = rs.getInt("user_id");
                entry.user_name = rs.getString("user_name");
                entry.bet_value = rs.getLong("bet_value");
                entry.bet_side = rs.getLong("bet_side");
                entry.prize = rs.getLong("prize");
                entry.refund = rs.getLong("refund");
                entry.input_time = rs.getInt("input_time");
                entry.money_type = rs.getInt("money_type");
                entry.time_log = rs.getString("timestamp");
                results.add(entry);
            }
            rs.close();
            stmt.close();
        }
        return results;
    }

    @Override
    public int countLogTaiXiuDetail(String referent_id, String betSide, String money_type, String nickName) throws SQLException {
        int res = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String query = "SELECT count(*) as total FROM transaction_detail_tai_xiu_md5 where 1=1 ";
            String condition = "";
            if (referent_id != null && !referent_id.equals("")) {
                condition = condition + " AND reference_id=" + referent_id;
            }
            if (betSide != null && !betSide.equals("")) {
                condition = condition + " AND bet_side=" + betSide;
            }
            if (nickName != null && !nickName.equals("")) {
                condition = condition + " AND user_name = '" + nickName + "'";
            }
            condition = condition + " AND money_type=" + money_type;
            String sql = "SELECT count(*) as total FROM transaction_detail_tai_xiu_md5 where 1=1 " + condition;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = rs.getInt("total");
            }
            rs.close();
            stmt.close();
        }
        return res;
    }

    @Override
    public List<TaiXiuResultResponse> listLogTaiXiuResult(String referentId, String moneyType, String timeStart, String timeEnd, int page) throws SQLException {
        ArrayList<TaiXiuResultResponse> result = new ArrayList<TaiXiuResultResponse>();
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        String sql = "";
        String query = "";
        String condition = "";
        int num_start = (page - 1) * 50;
        int num_end = 50;
        String limit = " ORDER BY reference_id DESC LIMIT " + num_start + ", " + 50 + "";
        try {
            // SUN: read from the live MD5 result table. The legacy `result_tai_xiu`
            // table has been dead since 2026-03-05 — only 1 stale row — which is
            // why the admin history page showed only one entry on the Xỉu side
            // (that lone row has result=0) and nothing on Tài.
            query = "SELECT reference_id,result,dice1,dice2,dice3,total_tai,total_xiu,num_bet_tai,num_bet_xiu,total_prize,total_refund_tai,total_refund_xiu,total_revenue,money_type,timestamp from result_tai_xiu_md5 where 1=1 ";
            if (referentId != null && !referentId.equals("")) {
                condition = condition + " AND reference_id=" + referentId;
            }
            if (moneyType != null && !moneyType.equals("")) {
                condition = condition + " AND money_type=" + moneyType;
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                condition = condition + " AND timestamp BETWEEN '" + timeStart + "' AND '" + timeEnd + "'";
            }
            sql = query + condition + limit;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                TaiXiuResultResponse taixiu = new TaiXiuResultResponse();
                taixiu.reference_id = rs.getLong("reference_id");
                taixiu.result = rs.getInt("result");
                taixiu.dice1 = rs.getInt("dice1");
                taixiu.dice2 = rs.getInt("dice2");
                taixiu.dice3 = rs.getInt("dice3");
                taixiu.total_tai = rs.getLong("total_tai");
                taixiu.total_xiu = rs.getLong("total_xiu");
                taixiu.num_bet_tai = rs.getInt("num_bet_tai");
                taixiu.num_bet_xiu = rs.getInt("num_bet_xiu");
                taixiu.total_prize = rs.getLong("total_prize");
                taixiu.total_refund_tai = rs.getLong("total_refund_tai");
                taixiu.total_refund_xiu = rs.getLong("total_refund_xiu");
                taixiu.total_revenue = rs.getLong("total_revenue");
                taixiu.money_type = rs.getInt("money_type");
                taixiu.timestamp = rs.getString("timestamp");
                result.add(taixiu);
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
        return result;
    }

    @Override
    public int countLogTaiXiuResult(String referentId, String moneyType, String timeStart, String timeEnd) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
        String sql = "";
        String query = "";
        String condition = "";
        int res = 0;
        try {
            PreparedStatement stmt;
            ResultSet rs;
            query = "SELECT count(*) as total from result_tai_xiu_md5 where 1=1 ";
            if (referentId != null && !referentId.equals("")) {
                condition = condition + " AND reference_id=" + referentId;
            }
            if (moneyType != null && !moneyType.equals("")) {
                condition = condition + " AND money_type=" + moneyType;
            }
            if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
                condition = condition + " AND timestamp BETWEEN '" + timeStart + "' AND '" + timeEnd + "'";
            }
            if ((rs = (stmt = conn.prepareStatement(sql = query + condition)).executeQuery()).next()) {
                res = rs.getInt("total");
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
        return res;
    }
}

