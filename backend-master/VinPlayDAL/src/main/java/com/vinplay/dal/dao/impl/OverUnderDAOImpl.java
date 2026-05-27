/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.models.cache.ThanhDuTXModel
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dal.dao.OverUnderDAO;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.models.cache.ThanhDuTXModel;
import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;

public class OverUnderDAOImpl
implements OverUnderDAO {
    @Override
    public List<ResultTaiXiu> getLichSuPhien(int number, int moneyType) throws SQLException {
        ArrayList<ResultTaiXiu> results = new ArrayList<ResultTaiXiu>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT * FROM result_over_under WHERE money_type=" + moneyType + " ORDER BY `timestamp` DESC LIMIT 0," + number;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ResultTaiXiu entry = new ResultTaiXiu();
                entry.referenceId = rs.getLong("reference_id");
                entry.result = rs.getInt("result");
                entry.dice1 = rs.getInt("dice1");
                entry.dice2 = rs.getInt("dice2");
                entry.dice3 = rs.getInt("dice3");
                entry.totalTai = rs.getLong("total_tai");
                entry.totalXiu = rs.getLong("total_xiu");
                entry.numBetTai = rs.getInt("num_bet_tai");
                entry.numBetXiu = rs.getInt("num_bet_xiu");
                entry.totalPrize = rs.getLong("total_prize");
                entry.totalRefundTai = rs.getLong("total_refund_tai");
                entry.totalRefundXiu = rs.getLong("total_refund_xiu");
                entry.totalRevenue = rs.getLong("total_revenue");
                entry.moneyType = rs.getInt("money_type");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                entry.timestamp = CommonUtils.convertTimestampToString((Date)timestamp);
                results.add(0, entry);
            }
            rs.close();
            stmt.close();
        }
        return results;
    }

    @Override
    public List<TransactionTaiXiu> getLichSuGiaoDich(String nickname, int number, int moneyType) throws SQLException {
        ArrayList<TransactionTaiXiu> results = new ArrayList<TransactionTaiXiu>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL ou_get_lich_su_giao_dich(?,?,?)")) {
            int param = 1;
            call.setString(param++, nickname);
            call.setInt(param++, number);
            call.setByte(param++, (byte)moneyType);
            try (ResultSet rs = call.executeQuery()) {
                while (rs.next()) {
                    TransactionTaiXiu entry = new TransactionTaiXiu();
                    entry.referenceId = rs.getLong("reference_id");
                    entry.userId = rs.getInt("user_id");
                    entry.username = rs.getString("user_name");
                    entry.betValue = rs.getLong("bet_value");
                    entry.betSide = rs.getInt("bet_side");
                    entry.totalPrize = rs.getLong("total_prize");
                    entry.totalRefund = rs.getLong("total_refund");
                    Timestamp date = rs.getTimestamp("timestamp");
                    entry.timestamp = CommonUtils.convertTimestampToString((Date)date);
                    byte dice1 = rs.getByte("dice1");
                    byte dice2 = rs.getByte("dice2");
                    byte dice3 = rs.getByte("dice3");
                    int total = dice1 + dice2 + dice3;
                    entry.resultPhien = dice1 + " - " + dice2 + " - " + dice3 + "   " + total;
                    results.add(entry);
                }
            }
        }
        catch (SQLException e) {
            throw e;
        }
        return results;
    }

    @Override
    public List<TopWin> getTopTaiXiu(int moneyType) throws SQLException {
        ArrayList<TopWin> result = new ArrayList<TopWin>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL ou_get_top_win(?)");){
            call.setByte(1, (byte)moneyType);
            ResultSet rs = call.executeQuery();
            while (rs.next()) {
                TopWin entry = new TopWin();
                entry.setUsername(rs.getString("user_name"));
                entry.setMoney(rs.getLong("money"));
                result.add(entry);
            }
            rs.close();
        }
        return result;
    }

    @Override
    public int countLichSuGiaoDichTX(String nickname, int moneyType) throws SQLException {
        int totalRecords = -1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL ou_count_lich_su_giao_dich(?, ?,?)")) {
            int param = 1;
            call.setString(param++, nickname);
            call.setByte(param++, (byte)moneyType);
            call.registerOutParameter(param++, 4);
            call.execute();
            totalRecords = call.getInt(3);
        }
        catch (SQLException e) {
            throw e;
        }
        return totalRecords;
    }

    @Override
    public List<TransactionTaiXiuDetail> getChiTietPhien(long referenceId, int moneyType) throws SQLException {
        ArrayList<TransactionTaiXiuDetail> results = new ArrayList<TransactionTaiXiuDetail>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL ou_get_chi_tiet_phien(?,?)")) {
            int param = 1;
            call.setLong(param++, referenceId);
            call.setByte(param++, (byte)moneyType);
            try (ResultSet rs = call.executeQuery()) {
                while (rs.next()) {
                    TransactionTaiXiuDetail entry = new TransactionTaiXiuDetail();
                    entry.referenceId = rs.getLong("reference_id");
                    entry.userId = rs.getInt("user_id");
                    entry.username = rs.getString("user_name");
                    entry.betValue = rs.getLong("bet_value");
                    entry.betSide = rs.getInt("bet_side");
                    entry.prize = rs.getLong("prize");
                    entry.refund = rs.getLong("refund");
                    entry.inputTime = rs.getInt("input_time");
                    entry.moneyType = rs.getByte("money_type");
                    entry.timestamp = rs.getDate("timestamp");
                    results.add(entry);
                }
            }
        }
        catch (SQLException e) {
            throw e;
        }
        return results;
    }

    @Override
    public ResultTaiXiu getKetQuaPhien(long referenceId, int moneyType) throws SQLException {
        ResultTaiXiu entry = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT * FROM result_over_under WHERE reference_id=" + referenceId + " AND money_type=" + moneyType;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                entry = new ResultTaiXiu();
                entry.referenceId = rs.getLong("reference_id");
                entry.result = rs.getInt("result");
                entry.dice1 = rs.getInt("dice1");
                entry.dice2 = rs.getInt("dice2");
                entry.dice3 = rs.getInt("dice3");
                entry.totalTai = rs.getLong("total_tai");
                entry.totalXiu = rs.getLong("total_xiu");
                entry.numBetTai = rs.getInt("num_bet_tai");
                entry.numBetXiu = rs.getInt("num_bet_xiu");
                entry.totalPrize = rs.getLong("total_prize");
                entry.totalRefundTai = rs.getLong("total_refund_tai");
                entry.totalRefundXiu = rs.getLong("total_refund_xiu");
                entry.totalRevenue = rs.getLong("total_revenue");
                entry.moneyType = rs.getInt("money_type");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                entry.timestamp = CommonUtils.convertTimestampToString((Date)timestamp);
            }
            rs.close();
            stmt.close();
        }
        return entry;
    }

    @Override
    public List<ThanhDuTXModel> getTopThanhDuDaily(String startTime, String endTime, short type) throws SQLException {
        ArrayList<ThanhDuTXModel> results = new ArrayList<ThanhDuTXModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement call = conn.prepareCall("CALL ou_get_top_thanh_du(?, ?, ?)")) {
            int param = 1;
            call.setString(param++, startTime);
            call.setString(param++, endTime);
            call.setByte(param++, (byte)type);
            try (ResultSet rs = call.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("user_name");
                    ThanhDuTXModel entry = new ThanhDuTXModel(username);
                    entry.number = rs.getInt("number");
                    entry.totalValue = rs.getLong("total_betting");
                    entry.currentReferenceId = rs.getLong("last_reference");
                    entry.parseReferences(rs.getString("references"));
                    results.add(entry);
                }
            }
        }
        catch (SQLException e) {
            throw e;
        }
        return results;
    }

    @Override
    public int getMaxThanhDu(String username, short type) throws SQLException {
        int max = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT `number` FROM thanh_du_ou WHERE user_name='" + username + "' AND `type`=" + type + " AND DATE(`last_update`)=CURDATE()";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                max = rs.getInt("number");
            }
            rs.close();
            stmt.close();
        }
        return max;
    }

    @Override
    public int getSoLanRutLoc(String username) throws SQLException {
        int soLanRut = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String sql = "SELECT so_lan_rut FROM user_rut_loc_ou WHERE user_name='" + username + "'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                soLanRut = rs.getInt("so_lan_rut");
            }
            rs.close();
            stmt.close();
        }
        return soLanRut;
    }

    public List<VinhDanhRLTLModel> getVinhDanhTLRL(String collectionName) {
        final ArrayList<VinhDanhRLTLModel> results = new ArrayList<VinhDanhRLTLModel>();
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        FindIterable iterable = null;
        BasicDBObject sortCondtions = new BasicDBObject();
        sortCondtions.put("_id", -1);
        iterable = db.getCollection(collectionName).find().sort((Bson)sortCondtions).limit(10);
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                VinhDanhRLTLModel entry = new VinhDanhRLTLModel();
                entry.username = document.getString("user_name");
                entry.money = document.getLong("money");
                entry.time = document.getString("time_log");
                results.add(entry);
            }
        });
        return results;
    }

    private List<XepHangRLTLModel> getBangXepHangTLRL(String collectionName) {
        final ArrayList<XepHangRLTLModel> results = new ArrayList<XepHangRLTLModel>();
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        AggregateIterable iterable = db.getCollection(collectionName).aggregate(Arrays.asList(new Document("$group", new Document("_id", "$user_name").append("total", new Document("$sum", "$money"))), new Document("$sort", new Document("total", -1)), new Document("$limit", 10)));
        iterable.forEach((Block)new Block<Document>(){

            public void apply(Document document) {
                XepHangRLTLModel model = new XepHangRLTLModel();
                model.username = document.getString("_id");
                model.money = document.getLong("total");
                results.add(model);
            }
        });
        return results;
    }

    private long getTienTLRL(String username, String collectionName) {
        long tongTien = 0L;
        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
        HashMap<String, String> condition = new HashMap<String, String>();
        condition.put("user_name", username);
        AggregateIterable iterable = db.getCollection(collectionName).aggregate(Arrays.asList(new Document("$match", condition), new Document("$group", new Document("_id", "null").append("total", new Document("$sum", "$money")))));
        Document document = (Document)iterable.first();
        if (document != null) {
            tongTien = document.getLong("total");
        }
        return tongTien;
    }

    @Override
    public List<XepHangRLTLModel> getXepHangTanLoc() {
        return this.getBangXepHangTLRL("tan_loc_ou");
    }

    @Override
    public List<VinhDanhRLTLModel> getVinhDanhTanLoc() {
        return this.getVinhDanhTLRL("tan_loc_ou");
    }

    @Override
    public long getTongTienTanLoc(String username) {
        return this.getTienTLRL(username, "tan_loc_ou");
    }

    @Override
    public List<XepHangRLTLModel> getXepHangRutLoc() {
        return this.getBangXepHangTLRL("rut_loc_ou");
    }

    @Override
    public List<VinhDanhRLTLModel> getVinhDanhRutLoc() {
        return this.getVinhDanhTLRL("rut_loc_ou");
    }

    @Override
    public long getTongTienRutLoc(String username) {
        return this.getTienTLRL(username, "rut_loc_ou");
    }

    @Override
    public ReportMoneySystemModel getReportTXToDay() {
        ReportMoneySystemModel res = new ReportMoneySystemModel();
        String today = VinPlayUtils.getCurrentDate();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap reportMap = client.getMap("cacheReports");
        for (Object _e : reportMap.entrySet()) {
            Map.Entry entry = (Map.Entry) _e;
            if (!((String)entry.getKey()).contains(today) || !((String)entry.getKey()).contains("OverUnder")) continue;
            ReportModel model = (ReportModel)entry.getValue();
            if (model.isBot) continue;
            ReportMoneySystemModel reportMoneySystemModel = res;
            reportMoneySystemModel.moneyWin += model.moneyWin;
            ReportMoneySystemModel reportMoneySystemModel2 = res;
            reportMoneySystemModel2.moneyLost += model.moneyLost;
            ReportMoneySystemModel reportMoneySystemModel3 = res;
            reportMoneySystemModel3.moneyOther += model.moneyOther;
            ReportMoneySystemModel reportMoneySystemModel4 = res;
            reportMoneySystemModel4.fee += model.fee;
            ReportMoneySystemModel reportMoneySystemModel5 = res;
            reportMoneySystemModel5.revenuePlayGame += model.moneyWin + model.moneyLost;
            ReportMoneySystemModel reportMoneySystemModel6 = res;
            reportMoneySystemModel6.revenue += model.moneyWin + model.moneyLost + model.moneyOther;
        }
        return res;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ReportMoneySystemModel getReportTX(String startDate, String endDate) {
        ReportMoneySystemModel res = new ReportMoneySystemModel();
        String sql = "SELECT SUM(money_win) as total_win, SUM(money_lost) as total_lost, SUM(money_other) as total_other, SUM(fee) as total_fee FROM vinplay.report_money_daily WHERE `date` >= '" + startDate + "?' and `date` <= '" + endDate + "' and action_name = 'OverUnder'";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                res.moneyWin = rs.getLong("total_win");
                res.moneyLost = rs.getLong("total_lost");
                res.moneyOther = rs.getLong("total_other");
                res.fee = rs.getLong("total_fee");
            }
            res.revenuePlayGame = res.moneyWin + res.moneyLost;
            res.revenue = res.moneyWin + res.moneyLost + res.moneyOther;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }
}

