/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage
 *  com.vinplay.vbee.common.models.cache.ThanhDuTXModel
 *  com.vinplay.vbee.common.models.cache.TopWinCache
 *  com.vinplay.vbee.common.models.minigame.CurrentTransactionSicboDetails
 *  com.vinplay.vbee.common.models.minigame.HistorySicbo
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  org.apache.log4j.Logger
 */
package com.vinplay.dal.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.impl.TaiXiuSicboDAOImpl;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.TaiXiuService;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.cache.ThanhDuTXModel;
import com.vinplay.vbee.common.models.cache.TopWinCache;
import com.vinplay.vbee.common.models.minigame.CurrentTransactionSicboDetails;
import com.vinplay.vbee.common.models.minigame.HistorySicbo;
import com.vinplay.vbee.common.models.minigame.TopWin;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class TaiXiuSicboServiceImpl
implements TaiXiuService {
    private Logger logger = Logger.getLogger((String)"rmq");
    private TaiXiuSicboDAOImpl dao = new TaiXiuSicboDAOImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();

    
    public boolean saveTransactionTaiXiu(long referenceId, int userId, String username, int moneyType, long betValue, short betSide, long prize, long refund, long jp) throws IOException, TimeoutException, InterruptedException {
        TransactionTaiXiuMessage msg = new TransactionTaiXiuMessage();
        msg.referenceId = referenceId;
        msg.userId = userId;
        msg.username = username;
        msg.moneyType = moneyType;
        msg.betValue = betValue;
        msg.betSide = betSide;
        msg.prize = prize;
        msg.refund = refund;
        msg.isMD5 = true;
        MessageBusFactory.get("queue_taixiu_sicbo").publish("queue_taixiu_sicbo", (BaseMessage)msg, 28100);
        return true;
    }

    
    public boolean saveResultTaiXiu(long referenceId, int result, int dice1, int dice2, int dice3, long totalTai, long totalXiu, int numBetTai, int numBetXiu, long totalPrize, long totalRefundTai, long totalRefundXiu, long totalRevenue, int moneyType) throws Exception {
        return true;
    }

    
    public boolean saveResultTaiXiu(long referenceId, int result, int dice1, int dice2, int dice3, long totalTai, long totalXiu, int numBetTai, int numBetXiu, long totalPrize, long totalRefundTai, long totalRefundXiu, long totalRevenue, int moneyType, String before_md5, String md5) throws Exception {
        ResultTaiXiuMessage msg = new ResultTaiXiuMessage();
        msg.referenceId = referenceId;
        msg.result = result;
        msg.dice1 = dice1;
        msg.dice2 = dice2;
        msg.dice3 = dice3;
        msg.totalTai = totalTai;
        msg.totalXiu = totalXiu;
        msg.numBetTai = numBetTai;
        msg.numBetXiu = numBetXiu;
        msg.totalPrize = totalPrize;
        msg.totalRefundTai = totalRefundTai;
        msg.totalRefundXiu = totalRefundXiu;
        msg.totalRevenue = totalRevenue;
        msg.moneyType = moneyType;
        msg.isMD5 = true;
        msg.before_md5 = before_md5;
        msg.md5 = md5;
        MessageBusFactory.get("queue_taixiu_sicbo").publish("queue_taixiu_sicbo", (BaseMessage)msg, 28101);
        this.logger.debug(("saveResultTaiXiu Test2: " + referenceId + " md5:" + msg.isMD5));
        return true;
    }

    @Override
    public boolean saveResultTaiXiu(long referenceId, int result, int dice1, int dice2, int dice3, long totalTai, long totalXiu, int numBetTai, int numBetXiu, long totalPrize, long totalRefundTai, long totalRefundXiu, long totalRevenue, int moneyType, long totalJp) throws Exception {
        ResultTaiXiuMessage msg = new ResultTaiXiuMessage();
        msg.referenceId = referenceId;
        msg.result = result;
        msg.dice1 = dice1;
        msg.dice2 = dice2;
        msg.dice3 = dice3;
        msg.totalTai = totalTai;
        msg.totalXiu = totalXiu;
        msg.numBetTai = numBetTai;
        msg.numBetXiu = numBetXiu;
        msg.totalPrize = totalPrize;
        msg.totalRefundTai = totalRefundTai;
        msg.totalRefundXiu = totalRefundXiu;
        msg.totalRevenue = totalRevenue;
        msg.moneyType = moneyType;
        msg.totalJackpot = totalJp;
        msg.isMD5 = true;
        MessageBusFactory.get("queue_taixiu_sicbo").publish("queue_taixiu_sicbo", (BaseMessage)msg, 28101);
        this.logger.debug(("saveResultTaiXiu Test2: " + referenceId + " md5:" + msg.isMD5));
        return true;
    }

    @Override
    public String getLichSuPhien(int soPhien, int moneyType) throws SQLException {
        List<ResultTaiXiu> results = this.dao.getLichSuPhien(soPhien, moneyType);
        return this.buildLichSuPhien(results, soPhien);
    }

    @Override
    public List<TopWin> getTopWin(int moneyType) throws SQLException {
        TopWinCache topTXCache;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap topMap = client.getMap("cacheTop");
        if (topMap.containsKey((Games.TAI_XIU_SICBO.getName() + "_" + moneyType)) && (topTXCache = (TopWinCache)topMap.get((Games.TAI_XIU_SICBO.getName() + "_" + moneyType))) != null) {
            return topTXCache.getResult();
        }
        return new ArrayList<TopWin>();
    }

    @Override
    public void updateAllTop() {
        try {
            List<TopWin> topWinVin = this.dao.getTopTaiXiu(1);
            this.logger.debug(("TOP WIN VIN SICBO: " + topWinVin.size()));
            List<TopWin> topWinXu = this.dao.getTopTaiXiu(0);
            this.logger.debug(("TOP WIN XU SICBO: " + topWinXu.size()));
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap topMap = client.getMap("cacheTop");
            TopWinCache cacheVin = (TopWinCache)topMap.get((Games.TAI_XIU_SICBO.getName() + "_1"));
            if (cacheVin == null) {
                cacheVin = new TopWinCache();
            }
            cacheVin.setResult(topWinVin);
            topMap.put((Games.TAI_XIU_SICBO.getName() + "_1"), cacheVin);
            TopWinCache cacheXu = (TopWinCache)topMap.get((Games.TAI_XIU_SICBO.getName() + "_0"));
            if (cacheXu == null) {
                cacheXu = new TopWinCache();
            }
            cacheXu.setResult(topWinXu);
            topMap.put((Games.TAI_XIU_SICBO.getName() + "_0"), cacheXu);
        }
        catch (SQLException e) {
            this.logger.error("UPDATE ALL TOP  MD5 exception: ", (Throwable)e);
            e.printStackTrace();
        }
    }

    @Override
    public List<TransactionTaiXiu> getLichSuGiaoDich(String username, int page, int moneyType) throws SQLException {
        return this.dao.getLichSuGiaoDich(username, page, moneyType);
    }

    @Override
    public List<ResultTaiXiu> getListLichSuPhien(int soPhien, int moneyType) throws SQLException {
        List<ResultTaiXiu> results = this.dao.getLichSuPhien(soPhien, moneyType);
        return results;
    }

    
    public boolean saveTransactionTaiXiuDetails(List<TransactionTaiXiuDetail> trans) throws IOException, TimeoutException, InterruptedException {
        boolean success = true;
        for (TransactionTaiXiuDetail tran : trans) {
            success = success && this.saveTransactionTaiXiuDetail(tran);
        }
        return success;
    }

    @Override
    public boolean saveResultTaiXiu(ResultTaiXiu rs) throws Exception {
        this.logger.debug("Save result tx sicbo");
        return this.saveResultTaiXiu(rs.referenceId, rs.result, rs.dice1, rs.dice2, rs.dice3, rs.totalTai, rs.totalXiu, rs.numBetTai, rs.numBetXiu, rs.totalPrize, rs.totalRefundTai, rs.totalRefundXiu, rs.totalRevenue, rs.moneyType, rs.before_md5, rs.md5);
    }

    @Override
    public boolean saveTransactionTaiXiu(List<TransactionTaiXiu> trans) throws IOException, TimeoutException, InterruptedException {
        for (TransactionTaiXiu tran : trans) {
            this.saveTransactionTaiXiu(tran.referenceId, tran.userId, tran.username, tran.moneyType, tran.betValue, (short)tran.betSide, tran.totalPrize, tran.totalRefund, tran.totalRefund);
        }
        return false;
    }

    @Override
    public boolean saveTransactionTaiXiuDetail(TransactionTaiXiuDetail tran) throws IOException, TimeoutException, InterruptedException {
        TransactionTaiXiuDetailMessage msg = new TransactionTaiXiuDetailMessage();
        msg.referenceId = tran.referenceId;
        msg.transactionCode = tran.transactionCode;
        msg.userId = tran.userId;
        msg.username = tran.username;
        msg.betValue = tran.betValue;
        msg.betSide = tran.betSide;
        msg.prize = tran.prize;
        msg.refund = tran.refund;
        msg.inputTime = tran.inputTime;
        msg.moneyType = tran.moneyType;
        msg.currentMoney = tran.currentMoney;
        msg.isMD5 = true;
        MessageBusFactory.get("queue_taixiu_sicbo").publish("queue_taixiu_sicbo", (BaseMessage)msg, 28102);
        return true;
    }

    @Override
    public int countLichSuGiaoDich(String nickname, int moneyType) throws SQLException {
        int totalRecords = this.dao.countLichSuGiaoDichTX(nickname, moneyType);
        return totalRecords / 10 + 1;
    }

    @Override
    public List<TransactionTaiXiuDetail> getChiTietPhienTX(long referenceId, int moneyType) throws SQLException {
        List<TransactionTaiXiuDetail> results = this.dao.getChiTietPhien(referenceId, moneyType);
        return results;
    }

    @Override
    public ResultTaiXiu getKetQuaPhien(long referenceId, int moneyType) throws SQLException {
        ResultTaiXiu resultTX = this.dao.getKetQuaPhien(referenceId, moneyType);
        return resultTX;
    }

    @Override
    public boolean updateTransactionTaiXiuDetail(TransactionTaiXiuDetail tran) throws IOException, TimeoutException, InterruptedException {
        TransactionTaiXiuDetailMessage msg = new TransactionTaiXiuDetailMessage();
        msg.referenceId = tran.referenceId;
        msg.transactionCode = tran.transactionCode;
        msg.userId = tran.userId;
        msg.username = tran.username;
        msg.betValue = tran.betValue;
        msg.betSide = tran.betSide;
        msg.prize = tran.prize;
        msg.refund = tran.refund;
        msg.inputTime = tran.inputTime;
        msg.moneyType = tran.moneyType;
        msg.isMD5 = true;
        MessageBusFactory.get("queue_taixiu_sicbo").publish("queue_taixiu_sicbo", (BaseMessage)msg, 28103);
        return true;
    }

    @Override
    public void calculateThanhDu(long referenceId, List<TransactionTaiXiu> transacntions, int result) throws IOException, TimeoutException, InterruptedException {
    }

    @Override
    public List<ThanhDuTXModel> getTopThanhDuDaily(String dateStr, int type) throws SQLException {
        return new ArrayList<ThanhDuTXModel>();
    }

    @Override
    public List<ThanhDuTXModel> getTopThanhDuMonthly(String dateStr, int type) throws SQLException, ParseException {
        return new ArrayList<ThanhDuTXModel>();
    }

    @Override
    public boolean updatePot(long pot, String potName) {
        return false;
    }

    @Override
    public boolean updateFund(long fund, String potName) {
        return false;
    }

    private String buildLichSuPhien(List<ResultTaiXiu> input, int number) {
        int end = input.size();
        int start = end - number > 0 ? end - number : 0;
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; ++i) {
            ResultTaiXiu entry = input.get(i);
            builder.append(entry.dice1);
            builder.append(",");
            builder.append(entry.dice2);
            builder.append(",");
            builder.append(entry.dice3);
            builder.append(",");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    @Override
    public ReportMoneySystemModel getReportTXToday() {
        return this.dao.getReportTXToDay();
    }

    @Override
    public ReportMoneySystemModel getReportTX(int range) {
        ReportMoneySystemModel reportTX;
        ReportMoneySystemModel todayModel = this.getReportTXToday();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        String endDate = sdf.format(cal.getTime());
        int date = cal.get(5);
        int month = cal.get(2);
        int dateBefore = date % range - 1;
        cal.add(5, -dateBefore);
        String startDate = sdf.format(cal.getTime());
        int dateAfter = range - dateBefore;
        Calendar calTmp = Calendar.getInstance();
        calTmp.add(5, dateAfter);
        String dateReset = sdf.format(calTmp.getTime());
        int monthTmp = calTmp.get(2);
        if (monthTmp != month) {
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/01");
            dateReset = sdf2.format(calTmp.getTime());
        }
        ReportMoneySystemModel result = reportTX = this.dao.getReportTX(startDate, endDate);
        reportTX.moneyWin += todayModel.moneyWin;
        ReportMoneySystemModel reportMoneySystemModel = result;
        reportMoneySystemModel.moneyLost += todayModel.moneyLost;
        ReportMoneySystemModel reportMoneySystemModel2 = result;
        reportMoneySystemModel2.fee += todayModel.fee;
        ReportMoneySystemModel reportMoneySystemModel3 = result;
        reportMoneySystemModel3.moneyOther += todayModel.moneyOther;
        ReportMoneySystemModel reportMoneySystemModel4 = result;
        reportMoneySystemModel4.revenuePlayGame += todayModel.revenuePlayGame;
        ReportMoneySystemModel reportMoneySystemModel5 = result;
        reportMoneySystemModel5.revenue += todayModel.revenue;
        result.dateReset = dateReset;
        return result;
    }

    @Override
    public void setKetQuaTaiXiu(short[] ketQuaTaiXiu) {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap("ketquataixiusicbo");
        String key = "ketquataixiusicbo";
        map.put(key, ketQuaTaiXiu);
    }

    @Override
    public short[] getKetQuaTaiXiu() {
        String key;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap("ketquataixiusicbo");
        if (map.containsKey((key = "ketquataixiusicbo"))) {
            return (short[])map.get(key);
        }
        return null;
    }

    @Override
    public short[] suaKetQuaTaiXiu() {
        String key;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap("ketquataixiusicbo");
        if (map.containsKey((key = "ketquataixiusicbo"))) {
            return (short[])map.remove(key);
        }
        return null;
    }

    public List<HistorySicbo> getHistorySicbo(String userName, int page, int moneyType) throws SQLException {
        return this.dao.getHistorySicbo(userName, page, moneyType);
    }

    public List<CurrentTransactionSicboDetails> getCurrentSessionInfo() throws SQLException {
        return this.dao.getCurrentSessionInfo(this.mgService.getReferenceId(31));
    }

    @Override
    public void updateJackpotTaiXiu(short ketQua) {
    }

    @Override
    public int updateDealerProfit(long phienid, int result, long total_money_tai, long total_money_xiu, long total_profit, long last_balance) throws java.io.IOException, java.util.concurrent.TimeoutException, InterruptedException {
        return 0;
    }

    @Override
    public long getSoTienRutLoc(String var1) { return 0L; }

    @Override
    public long getSoTienTanLoc(String var1) { return 0L; }

    @Override
    public java.util.List<com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel> getVinhDanhTanLoc() { return new java.util.ArrayList<>(); }

    @Override
    public java.util.List<com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel> getVinhDanhRutLoc() { return new java.util.ArrayList<>(); }

    @Override
    public java.util.List<com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel> getXepHangTanLoc() { return new java.util.ArrayList<>(); }

    @Override
    public java.util.List<com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel> getXepHangRutLoc() { return new java.util.ArrayList<>(); }

    @Override
    public long getPotTanLoc() throws java.sql.SQLException { return 0L; }

    @Override
    public void logTanLoc(String var1, long var2) throws java.io.IOException, java.util.concurrent.TimeoutException, InterruptedException { }

    @Override
    public void logRutLoc(String var1, long var2, int var4, long var5) throws java.io.IOException, java.util.concurrent.TimeoutException, InterruptedException { }

    @Override
    public void updatePotTanLoc(long var1) throws java.io.IOException, java.util.concurrent.TimeoutException, InterruptedException { }

    @Override
    public int updateLuotRutLoc(String var1, int var2) throws java.io.IOException, java.util.concurrent.TimeoutException, InterruptedException { return 0; }

    @Override
    public int getLuotRutLoc(String var1) throws java.sql.SQLException { return 0; }

}

