/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.ReportModel
 *  com.vinplay.vbee.common.models.cache.ThanhDuTXModel
 *  com.vinplay.vbee.common.models.minigame.CurrentTransactionSicboDetails
 *  com.vinplay.vbee.common.models.minigame.HistorySicbo
 *  com.vinplay.vbee.common.models.minigame.HistorySicboDetails
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.dal.dao.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.PotSicbo;
import com.vinplay.dal.dao.TaiXiuDAO;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.ReportModel;
import com.vinplay.vbee.common.models.cache.ThanhDuTXModel;
import com.vinplay.vbee.common.models.minigame.CurrentTransactionSicboDetails;
import com.vinplay.vbee.common.models.minigame.HistorySicbo;
import com.vinplay.vbee.common.models.minigame.HistorySicboDetails;
import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel;
import com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel;

public class TaiXiuSicboDAOImpl
implements TaiXiuDAO {
    @Override
    public List<ResultTaiXiu> getLichSuPhien(int n, int n2) throws SQLException {
        ArrayList<ResultTaiXiu> arrayList = new ArrayList<ResultTaiXiu>();
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String string = "SELECT * FROM result_tai_xiu_sicbo WHERE money_type=" + n2 + " ORDER BY `timestamp` DESC LIMIT 0," + n;
            PreparedStatement preparedStatement = connection.prepareStatement(string);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                ResultTaiXiu resultTaiXiu = new ResultTaiXiu();
                resultTaiXiu.referenceId = resultSet.getLong("reference_id");
                resultTaiXiu.result = resultSet.getInt("result");
                resultTaiXiu.dice1 = resultSet.getInt("dice1");
                resultTaiXiu.dice2 = resultSet.getInt("dice2");
                resultTaiXiu.dice3 = resultSet.getInt("dice3");
                resultTaiXiu.totalTai = resultSet.getLong("total_tai");
                resultTaiXiu.totalXiu = resultSet.getLong("total_xiu");
                resultTaiXiu.numBetTai = resultSet.getInt("num_bet_tai");
                resultTaiXiu.numBetXiu = resultSet.getInt("num_bet_xiu");
                resultTaiXiu.totalPrize = resultSet.getLong("total_prize");
                resultTaiXiu.totalRefundTai = resultSet.getLong("total_refund_tai");
                resultTaiXiu.totalRefundXiu = resultSet.getLong("total_refund_xiu");
                resultTaiXiu.totalRevenue = resultSet.getLong("total_revenue");
                resultTaiXiu.moneyType = resultSet.getInt("money_type");
                Timestamp timestamp = resultSet.getTimestamp("timestamp");
                resultTaiXiu.timestamp = CommonUtils.convertTimestampToString((java.util.Date)timestamp);
                arrayList.add(0, resultTaiXiu);
            }
            resultSet.close();
            preparedStatement.close();
        }
        return arrayList;
    }

    @Override
    public List<TransactionTaiXiu> getLichSuGiaoDich(String string, int n, int n2) throws SQLException {
        ArrayList<TransactionTaiXiu> arrayList = new ArrayList<TransactionTaiXiu>();
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement callableStatement = connection.prepareCall("CALL tx_get_lich_su_giao_dich_sicbo(?,?,?)")) {
            int n3 = 1;
            callableStatement.setString(n3++, string);
            callableStatement.setInt(n3++, n);
            callableStatement.setByte(n3++, (byte)n2);
            try (ResultSet resultSet = callableStatement.executeQuery()) {
                while (resultSet.next()) {
                    TransactionTaiXiu transactionTaiXiu = new TransactionTaiXiu();
                    transactionTaiXiu.referenceId = resultSet.getLong("reference_id");
                    transactionTaiXiu.userId = resultSet.getInt("user_id");
                    transactionTaiXiu.username = resultSet.getString("user_name");
                    transactionTaiXiu.betValue = resultSet.getLong("bet_value");
                    transactionTaiXiu.betSide = resultSet.getInt("bet_side");
                    transactionTaiXiu.totalPrize = resultSet.getLong("total_prize");
                    transactionTaiXiu.totalRefund = resultSet.getLong("total_refund");
                    Timestamp timestamp = resultSet.getTimestamp("timestamp");
                    transactionTaiXiu.timestamp = CommonUtils.convertTimestampToString((java.util.Date)timestamp);
                    byte by = resultSet.getByte("dice1");
                    byte by2 = resultSet.getByte("dice2");
                    byte by3 = resultSet.getByte("dice3");
                    int n4 = by + by2 + by3;
                    transactionTaiXiu.resultPhien = by + " - " + by2 + " - " + by3 + "   " + n4;
                    transactionTaiXiu.before_md5 = resultSet.getString("before_md5");
                    transactionTaiXiu.md5 = resultSet.getString("md5");
                    arrayList.add(transactionTaiXiu);
                }
            }
        }
        catch (SQLException sQLException) {
            throw sQLException;
        }
        return arrayList;
    }

    @Override
    public List<TopWin> getTopTaiXiu(int n) throws SQLException {
        ArrayList<TopWin> arrayList = new ArrayList<TopWin>();
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement callableStatement = connection.prepareCall("CALL tx_get_top_win_sicbo(?)")) {
            int n2 = 1;
            callableStatement.setByte(n2++, (byte)n);
            try (ResultSet resultSet = callableStatement.executeQuery()) {
                while (resultSet.next()) {
                    TopWin topWin = new TopWin();
                    topWin.setUsername(resultSet.getString("user_name"));
                    topWin.setMoney(resultSet.getLong("money"));
                    arrayList.add(topWin);
                }
            }
        }
        catch (SQLException sQLException) {
            throw sQLException;
        }
        return arrayList;
    }

    @Override
    public int countLichSuGiaoDichTX(String string, int n) throws SQLException {
        int n2 = -1;
        return n2;
    }

    @Override
    public List<TransactionTaiXiuDetail> getChiTietPhien(long l, int n) throws SQLException {
        ArrayList<TransactionTaiXiuDetail> arrayList = new ArrayList<TransactionTaiXiuDetail>();
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             CallableStatement callableStatement = connection.prepareCall("CALL tx_get_chi_tiet_phien_sicbo(?,?)")) {
            int n2 = 1;
            callableStatement.setLong(n2++, l);
            callableStatement.setByte(n2++, (byte)n);
            try (ResultSet resultSet = callableStatement.executeQuery()) {
                while (resultSet.next()) {
                    TransactionTaiXiuDetail transactionTaiXiuDetail = new TransactionTaiXiuDetail();
                    transactionTaiXiuDetail.referenceId = resultSet.getLong("reference_id");
                    transactionTaiXiuDetail.userId = resultSet.getInt("user_id");
                    transactionTaiXiuDetail.username = resultSet.getString("user_name");
                    transactionTaiXiuDetail.betValue = resultSet.getLong("bet_value");
                    transactionTaiXiuDetail.betSide = resultSet.getInt("bet_side");
                    transactionTaiXiuDetail.prize = resultSet.getLong("prize");
                    transactionTaiXiuDetail.refund = resultSet.getLong("refund");
                    transactionTaiXiuDetail.inputTime = resultSet.getInt("input_time");
                    transactionTaiXiuDetail.moneyType = resultSet.getByte("money_type");
                    transactionTaiXiuDetail.timestamp = resultSet.getDate("timestamp");
                    arrayList.add(transactionTaiXiuDetail);
                }
            }
        }
        catch (SQLException sQLException) {
            throw sQLException;
        }
        return arrayList;
    }

    @Override
    public ResultTaiXiu getKetQuaPhien(long l, int n) throws SQLException {
        ResultTaiXiu resultTaiXiu = null;
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");){
            String string = "SELECT * FROM result_tai_xiu_sicbo WHERE reference_id=" + l + " AND money_type=" + n;
            PreparedStatement preparedStatement = connection.prepareStatement(string);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                resultTaiXiu = new ResultTaiXiu();
                resultTaiXiu.referenceId = resultSet.getLong("reference_id");
                resultTaiXiu.result = resultSet.getInt("result");
                resultTaiXiu.dice1 = resultSet.getInt("dice1");
                resultTaiXiu.dice2 = resultSet.getInt("dice2");
                resultTaiXiu.dice3 = resultSet.getInt("dice3");
                resultTaiXiu.totalTai = resultSet.getLong("total_tai");
                resultTaiXiu.totalXiu = resultSet.getLong("total_xiu");
                resultTaiXiu.numBetTai = resultSet.getInt("num_bet_tai");
                resultTaiXiu.numBetXiu = resultSet.getInt("num_bet_xiu");
                resultTaiXiu.totalPrize = resultSet.getLong("total_prize");
                resultTaiXiu.totalRefundTai = resultSet.getLong("total_refund_tai");
                resultTaiXiu.totalRefundXiu = resultSet.getLong("total_refund_xiu");
                resultTaiXiu.totalRevenue = resultSet.getLong("total_revenue");
                resultTaiXiu.moneyType = resultSet.getInt("money_type");
                resultTaiXiu.before_md5 = resultSet.getString("before_md5");
                resultTaiXiu.md5 = resultSet.getString("md5");
                Timestamp timestamp = resultSet.getTimestamp("timestamp");
                resultTaiXiu.timestamp = CommonUtils.convertTimestampToString((java.util.Date)timestamp);
            }
            resultSet.close();
            preparedStatement.close();
        }
        return resultTaiXiu;
    }

    @Override
    public List<ThanhDuTXModel> getTopThanhDuDaily(String string, String string2, short s) throws SQLException {
        ArrayList<ThanhDuTXModel> arrayList = new ArrayList<ThanhDuTXModel>();
        return arrayList;
    }

    @Override
    public int getMaxThanhDu(String string, short s) throws SQLException {
        int n = 0;
        return n;
    }

    @Override
    public ReportMoneySystemModel getReportTXToDay() {
        ReportMoneySystemModel reportMoneySystemModel = new ReportMoneySystemModel();
        String string = VinPlayUtils.getCurrentDate();
        HazelcastInstance hazelcastInstance = HazelcastClientFactory.getInstance();
        IMap iMap = hazelcastInstance.getMap("cacheReports");
        for (Object e : iMap.entrySet()) {
            Map.Entry entry = (Map.Entry)e;
            if (!((String)entry.getKey()).contains(string) || !((String)entry.getKey()).contains("TaiXiuMD5")) continue;
            ReportModel reportModel = (ReportModel)entry.getValue();
            if (reportModel.isBot) continue;
            ReportMoneySystemModel reportMoneySystemModel2 = reportMoneySystemModel;
            reportMoneySystemModel2.moneyWin += reportModel.moneyWin;
            ReportMoneySystemModel reportMoneySystemModel3 = reportMoneySystemModel;
            reportMoneySystemModel3.moneyLost += reportModel.moneyLost;
            ReportMoneySystemModel reportMoneySystemModel4 = reportMoneySystemModel;
            reportMoneySystemModel4.moneyOther += reportModel.moneyOther;
            ReportMoneySystemModel reportMoneySystemModel5 = reportMoneySystemModel;
            reportMoneySystemModel5.fee += reportModel.fee;
            ReportMoneySystemModel reportMoneySystemModel6 = reportMoneySystemModel;
            reportMoneySystemModel6.revenuePlayGame += reportModel.moneyWin + reportModel.moneyLost;
            ReportMoneySystemModel reportMoneySystemModel7 = reportMoneySystemModel;
            reportMoneySystemModel7.revenue += reportModel.moneyWin + reportModel.moneyLost + reportModel.moneyOther;
        }
        return reportMoneySystemModel;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ReportMoneySystemModel getReportTX(String string, String string2) {
        ReportMoneySystemModel reportMoneySystemModel = new ReportMoneySystemModel();
        String string3 = "SELECT SUM(money_win) as total_win, SUM(money_lost) as total_lost, SUM(money_other) as total_other, SUM(fee) as total_fee FROM vinplay.report_money_daily WHERE `date` >= '" + string + "?' and `date` <= '" + string2 + "' and action_name = 'TaiXiuMD5'";
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement preparedStatement = connection.prepareStatement(string3);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                reportMoneySystemModel.moneyWin = resultSet.getLong("total_win");
                reportMoneySystemModel.moneyLost = resultSet.getLong("total_lost");
                reportMoneySystemModel.moneyOther = resultSet.getLong("total_other");
                reportMoneySystemModel.fee = resultSet.getLong("total_fee");
            }
            reportMoneySystemModel.revenuePlayGame = reportMoneySystemModel.moneyWin + reportMoneySystemModel.moneyLost;
            reportMoneySystemModel.revenue = reportMoneySystemModel.moneyWin + reportMoneySystemModel.moneyLost + reportMoneySystemModel.moneyOther;
        }
        catch (SQLException sQLException) {
            sQLException.printStackTrace();
        }
        return reportMoneySystemModel;
    }

    public List<HistorySicbo> getHistorySicbo(String nickname, int page, int moneyType) throws SQLException {
        // SUN-848: reference_id resets daily, so joining result_tai_xiu_sicbo
        // by reference_id alone returns multiple matches across days. The SP
        // now returns the bet's epoch timestamp so we can narrow the result
        // JOIN to the same round (±120s of the bet's time) and pick the right
        // day's dice roll.
        // FIX: added moneyType param — SP now filters by coin type (VND vs xu)
        // so bets from different money_type configs don't bleed into each other.
        List<HistorySicbo> finalList = new ArrayList<>();
        List<HistorySicbo> sessions = new ArrayList<>();
        // Map key: reference_id + "#" + session-day (seconds / 86400) — isolates
        // cycled reference_id values across days.
        java.util.Map<String, Long> sessionTsByKey = new java.util.LinkedHashMap<>();
        long minTs = Long.MAX_VALUE;
        long maxTs = Long.MIN_VALUE;

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement ps = conn.prepareStatement("{CALL tx_get_lich_su_giao_dich_chi_tiet_sicbo(?, ?, ?)}")) {
            ps.setString(1, nickname);
            ps.setInt(2, page);
            ps.setInt(3, moneyType);   // FIX: filter by coin type

            // Row stream grouped by (reference_id, bet_side). bet_timestamp (epoch
            // seconds) comes along so we can disambiguate the day per session.
            HistorySicbo currentSession = null;
            long currentRefId = Long.MIN_VALUE;
            String currentKey = null;

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistorySicboDetails det = new HistorySicboDetails();
                    det.setBet(rs.getLong("total_bet_value"));
                    det.setWin(rs.getLong("total_prize"));
                    det.setSlot(PotSicbo.getEnumById(rs.getInt("bet_side")).getName());
                    long refId = rs.getLong("reference_id");
                    det.setReferenceId(refId);

                    long betTs = 0L;
                    try { betTs = rs.getLong("bet_timestamp"); } catch (Exception ignored) {}

                    // New session whenever reference_id OR calendar-day changes.
                    String key = refId + "#" + (betTs / 86400L);
                    if (currentSession == null || !key.equals(currentKey)) {
                        currentSession = new HistorySicbo();
                        currentSession.setBets(new ArrayList<>());
                        currentSession.setGameSessionId(refId);
                        sessions.add(currentSession);
                        sessionTsByKey.put(key, betTs);
                        currentRefId = refId;
                        currentKey = key;
                        if (betTs > 0) {
                            if (betTs < minTs) minTs = betTs;
                            if (betTs > maxTs) maxTs = betTs;
                        }
                    }
                    currentSession.getBets().add(det);
                }
            }

            if (sessions.isEmpty()) {
                return finalList;
            }

            // Narrowed JOIN: only fetch result rows in the same time window as
            // the bets. ±3600s on each end covers game-settle delay; combined
            // with exact per-session matching below, wrong-day results can't
            // leak in.
            if (minTs == Long.MAX_VALUE) { minTs = 0L; maxTs = Long.MAX_VALUE; }
            String resultSql =
                "SELECT reference_id, dice1, dice2, dice3, timestamp, UNIX_TIMESTAMP(timestamp) AS ts_epoch " +
                "FROM result_tai_xiu_sicbo " +
                "WHERE reference_id IN (" + sqlInClause(sessions) + ") " +
                "  AND money_type = " + moneyType +           // FIX: filter result by same coin type
                "  AND UNIX_TIMESTAMP(timestamp) BETWEEN ? AND ?";

            java.util.Map<String, HistorySicbo> resultsByKey = new java.util.HashMap<>();
            try (PreparedStatement rps = conn.prepareStatement(resultSql)) {
                rps.setLong(1, minTs - 3600L);
                rps.setLong(2, maxTs + 3600L);
                try (ResultSet rrs = rps.executeQuery()) {
                    while (rrs.next()) {
                        long refId = rrs.getLong("reference_id");
                        long tsEpoch = rrs.getLong("ts_epoch");
                        HistorySicbo marker = new HistorySicbo();
                        marker.setGameSessionId(refId);
                        marker.setGameSessionResult(new int[]{
                            rrs.getInt("dice1"), rrs.getInt("dice2"), rrs.getInt("dice3")});
                        marker.setCreateDate(TaiXiuSicboDAOImpl.formatDate(
                            new Date(rrs.getTimestamp("timestamp").getTime())));
                        // Key by (refId, day-of-round) so the cycled reference_id
                        // on a different day doesn't overwrite today's result.
                        String rkey = refId + "#" + (tsEpoch / 86400L);
                        resultsByKey.put(rkey, marker);
                    }
                }
            }

            // Attach the correct dice result to each session by (refId, day).
            for (HistorySicbo s : sessions) {
                long ts = 0L;
                for (java.util.Map.Entry<String, Long> e : sessionTsByKey.entrySet()) {
                    if (e.getKey().startsWith(s.getGameSessionId() + "#")) {
                        ts = e.getValue();
                        break;
                    }
                }
                String rkey = s.getGameSessionId() + "#" + (ts / 86400L);
                HistorySicbo r = resultsByKey.get(rkey);
                if (r != null) {
                    s.setGameSessionResult(r.getGameSessionResult());
                    s.setCreateDate(r.getCreateDate());
                    finalList.add(s);
                }
                // If no matching result (unsettled round), skip — same as old
                // behaviour which required a join hit to emit the session.
            }
        }

        // SP already orders by bet_timestamp DESC; finalList preserves that order.
        return finalList;
    }

    /** Build a SQL IN (…) literal list from the distinct session reference_ids. */
    private static String sqlInClause(List<HistorySicbo> sessions) {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (HistorySicbo s : sessions) ids.add(s.getGameSessionId());
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        return sb.toString();
    }

    private static String formatDate(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        return simpleDateFormat.format(date);
    }

    public List<CurrentTransactionSicboDetails> getCurrentSessionInfo(long l) throws SQLException {
        ArrayList<CurrentTransactionSicboDetails> arrayList = new ArrayList<CurrentTransactionSicboDetails>();
        String string = "SELECT reference_id, user_name, bet_side, SUM(bet_value) AS total_bet FROM transaction_detail_tai_xiu_sicbo WHERE user_id <> 0 and reference_id = " + l + "  GROUP BY reference_id, user_name, bet_side";
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpool_minigame");
             PreparedStatement preparedStatement = connection.prepareStatement(string)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    CurrentTransactionSicboDetails currentTransactionSicboDetails = new CurrentTransactionSicboDetails();
                    currentTransactionSicboDetails.setUserName(resultSet.getString("user_name"));
                    currentTransactionSicboDetails.setBet(resultSet.getLong("total_bet"));
                    currentTransactionSicboDetails.setBetSide(PotSicbo.getEnumById(resultSet.getInt("bet_side")).getName());
                    currentTransactionSicboDetails.setReferenceId(resultSet.getLong("reference_id"));
                    arrayList.add(currentTransactionSicboDetails);
                }
            }
        }
        catch (SQLException sQLException) {
            throw sQLException;
        }
        return arrayList;
    }

    @Override
    public int getSoLanRutLoc(String username) throws SQLException {
        return 0;
    }

    @Override
    public List<XepHangRLTLModel> getXepHangTanLoc() {
        return new ArrayList<XepHangRLTLModel>();
    }

    @Override
    public List<VinhDanhRLTLModel> getVinhDanhTanLoc() {
        return new ArrayList<VinhDanhRLTLModel>();
    }

    @Override
    public long getTongTienTanLoc(String username) {
        return 0L;
    }

    @Override
    public List<XepHangRLTLModel> getXepHangRutLoc() {
        return new ArrayList<XepHangRLTLModel>();
    }

    @Override
    public List<VinhDanhRLTLModel> getVinhDanhRutLoc() {
        return new ArrayList<VinhDanhRLTLModel>();
    }

    @Override
    public long getTongTienRutLoc(String username) {
        return 0L;
    }
}

