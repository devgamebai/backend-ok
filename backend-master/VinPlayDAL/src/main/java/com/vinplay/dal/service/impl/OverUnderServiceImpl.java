/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.LogRutLocMessge
 *  com.vinplay.vbee.common.messages.minigame.LogTanLocMessage
 *  com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage
 *  com.vinplay.vbee.common.messages.minigame.ThanhDuMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdatePotMessage
 *  com.vinplay.vbee.common.models.cache.RutLocCacheModel
 *  com.vinplay.vbee.common.models.cache.ThanhDuTXModel
 *  com.vinplay.vbee.common.models.cache.TopRLTLModel
 *  com.vinplay.vbee.common.models.cache.TopWinCache
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  org.apache.log4j.Logger
 */
package com.vinplay.dal.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.OverUnderDAO;
import com.vinplay.dal.dao.impl.MiniGameDAOImpl;
import com.vinplay.dal.dao.impl.OverUnderDAOImpl;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel;
import com.vinplay.dal.service.OverUnderService;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.cache.LockHandle;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.LogRutLocMessge;
import com.vinplay.vbee.common.messages.minigame.LogTanLocMessage;
import com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.ThanhDuMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage;
import com.vinplay.vbee.common.messages.minigame.UpdatePotMessage;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.cache.RutLocCacheModel;
import com.vinplay.vbee.common.models.cache.ThanhDuTXModel;
import com.vinplay.vbee.common.models.cache.TopRLTLModel;
import com.vinplay.vbee.common.models.cache.TopWinCache;
import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel;
import com.vinplay.vbee.common.rmq.RMQApi;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class OverUnderServiceImpl
implements OverUnderService {
    private Logger logger = Logger.getLogger((String)"rmq");
    private OverUnderDAO dao = new OverUnderDAOImpl();

    @Override
    public boolean saveTransactionTaiXiu(long referenceId, int userId, String username, int moneyType, long betValue, short betSide, long prize, long refund) throws IOException, TimeoutException, InterruptedException {
        TransactionTaiXiuMessage msg = new TransactionTaiXiuMessage();
        msg.referenceId = referenceId;
        msg.userId = userId;
        msg.username = username;
        msg.moneyType = moneyType;
        msg.betValue = betValue;
        msg.betSide = betSide;
        msg.prize = prize;
        msg.refund = refund;
        MessageBusFactory.get("queue_overunder").publish("queue_overunder", (BaseMessage)msg, (int)10100);
        return true;
    }

    @Override
    public boolean saveResultTaiXiu(long referenceId, int result, int dice1, int dice2, int dice3, long totalTai, long totalXiu, int numBetTai, int numBetXiu, long totalPrize, long totalRefundTai, long totalRefundXiu, long totalRevenue, int moneyType) throws Exception {
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
        MessageBusFactory.get("queue_overunder").publish("queue_overunder", (BaseMessage)msg, (int)10101);
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
        if (topMap.containsKey((Games.OVER_UNDER.getName() + "_" + moneyType)) && (topTXCache = (TopWinCache)topMap.get((Games.OVER_UNDER.getName() + "_" + moneyType))) != null) {
            return topTXCache.getResult();
        }
        List<TopWin> topWins = this.dao.getTopTaiXiu(moneyType);
        return topWins;
    }

    @Override
    public void updateAllTop() {
        try {
            this.logger.debug("Run updateAllTop");
            List<TopWin> topWinVin = this.dao.getTopTaiXiu(1);
            this.logger.debug(("TOP WIN VIN: " + topWinVin.size()));
            List<TopWin> topWinXu = this.dao.getTopTaiXiu(0);
            this.logger.debug(("TOP WIN XU: " + topWinXu.size()));
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap topMap = client.getMap("cacheTop");
            TopWinCache cacheVin = (TopWinCache)topMap.get((Games.OVER_UNDER.getName() + "_1"));
            if (cacheVin == null) {
                cacheVin = new TopWinCache();
            }
            cacheVin.setResult(topWinVin);
            topMap.put((Games.OVER_UNDER.getName() + "_1"), cacheVin);
            TopWinCache cacheXu = (TopWinCache)topMap.get((Games.OVER_UNDER.getName() + "_0"));
            if (cacheXu == null) {
                cacheXu = new TopWinCache();
            }
            cacheXu.setResult(topWinXu);
            topMap.put((Games.OVER_UNDER.getName() + "_0"), cacheXu);
        }
        catch (SQLException e) {
            this.logger.error("UPDATE ALL TOP exception: ", (Throwable)e);
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

    @Override
    public boolean saveTransactionTaiXiuDetails(List<TransactionTaiXiuDetail> trans) throws IOException, TimeoutException, InterruptedException {
        boolean success = true;
        for (TransactionTaiXiuDetail tran : trans) {
            success = success && this.saveTransactionTaiXiuDetail(tran);
        }
        return success;
    }

    @Override
    public boolean saveResultTaiXiu(ResultTaiXiu rs) throws Exception {
        this.logger.debug("Save result tx");
        return this.saveResultTaiXiu(rs.referenceId, rs.result, rs.dice1, rs.dice2, rs.dice3, rs.totalTai, rs.totalXiu, rs.numBetTai, rs.numBetXiu, rs.totalPrize, rs.totalRefundTai, rs.totalRefundXiu, rs.totalRevenue, rs.moneyType);
    }

    @Override
    public boolean saveTransactionTaiXiu(List<TransactionTaiXiu> trans) throws IOException, TimeoutException, InterruptedException {
        for (TransactionTaiXiu tran : trans) {
            this.saveTransactionTaiXiu(tran.referenceId, tran.userId, tran.username, tran.moneyType, tran.betValue, (short)tran.betSide, tran.totalPrize, tran.totalRefund);
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
        MessageBusFactory.get("queue_overunder").publish("queue_overunder", (BaseMessage)msg, (int)10102);
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
        MessageBusFactory.get("queue_overunder").publish("queue_overunder", (BaseMessage)msg, (int)10103);
        return true;
    }

    @Override
    public void calculateThanhDu(long referenceId, List<TransactionTaiXiu> transacntions, int result) throws IOException, TimeoutException, InterruptedException {
        DistCache<String, ThanhDuTXModel> winMap = CacheFactory.get("cacheWinThanhDuOU", ThanhDuTXModel.class);
        DistCache<String, ThanhDuTXModel> lossMap = CacheFactory.get("cacheLossThanhDuOU", ThanhDuTXModel.class);
        for (TransactionTaiXiu tran : transacntions) {
            long moneyExchange;
            if (tran.betSide == result) {
                moneyExchange = tran.betValue - tran.totalRefund;
                if (moneyExchange >= 2000L) {
                    this.incrementThanhDu(referenceId, winMap, tran.username, moneyExchange, 1);
                }
                if (moneyExchange <= 0L) continue;
                this.clearThanhDu(lossMap, tran.username, 0);
                continue;
            }
            moneyExchange = tran.betValue - tran.totalRefund;
            if (moneyExchange >= 2000L) {
                this.incrementThanhDu(referenceId, lossMap, tran.username, moneyExchange, 0);
            }
            if (moneyExchange <= 0L) continue;
            this.clearThanhDu(winMap, tran.username, 1);
        }
    }

    private void incrementThanhDu(long referenceId, DistCache<String, ThanhDuTXModel> map, String username, long moneyExchange, int type) throws IOException, TimeoutException, InterruptedException {
        if (moneyExchange < 2000L) {
            return;
        }
        if (map.containsKey(username)) {
            try (LockHandle h = map.acquireLock(username, 5, TimeUnit.SECONDS)) {
                if (h == null) {
                    logger.warn("incrementThanhDu: lock timeout for user=" + username + " map=" + map.getName());
                    return;
                }
                ThanhDuTXModel model = map.get(username);
                if (model == null) return;
                if (!model.playOnToday()) {
                    model = new ThanhDuTXModel(username);
                } else {
                    ++model.number;
                }
                model.addReference(referenceId);
                model.totalValue += moneyExchange;
                if (!model.valid && moneyExchange >= 10000L) {
                    model.valid = true;
                }
                if (model.number > model.maxNumber && model.valid) {
                    model.maxNumber = model.number;
                    ThanhDuMessage message = new ThanhDuMessage(model.username, model.number, model.totalValue, model.currentReferenceId, model.getReferences(), (short)type);
                    MessageBusFactory.get("queue_overunder").publish("queue_overunder", message, 10104);
                }
                map.put(username, model);
            }
        } else {
            ThanhDuTXModel model = new ThanhDuTXModel(username);
            model.totalValue = moneyExchange;
            model.addReference(referenceId);
            int max = 0;
            try {
                max = this.dao.getMaxThanhDu(username, (short)type);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            model.maxNumber = max;
            if (moneyExchange >= 10000L) {
                model.valid = true;
                ThanhDuMessage message = new ThanhDuMessage(model.username, model.number, model.totalValue, model.currentReferenceId, model.getReferences(), (short)type);
                MessageBusFactory.get("queue_overunder").publish("queue_overunder", message, 10104);
            }
            map.put(username, model);
        }
    }

    private void clearThanhDu(DistCache<String, ThanhDuTXModel> map, String username, int type) {
        ThanhDuTXModel model;
        if (map.containsKey(username) && (model = map.get(username)) != null) {
            model.clear();
            map.put(username, model);
        }
    }

    @Override
    public List<ThanhDuTXModel> getTopThanhDuDaily(String dateStr, int type) throws SQLException {
        String startTime = dateStr + " 00:00:00";
        String endTime = dateStr + " 23:59:59";
        return this.dao.getTopThanhDuDaily(startTime, endTime, (short)type);
    }

    @Override
    public List<ThanhDuTXModel> getTopThanhDuMonthly(String dateStr, int type) throws SQLException, ParseException {
        String string = dateStr + "-01";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date dt = sdf.parse(string);
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        String startDate = sdf.format(c.getTime());
        String startTime = startDate + " 00:00:00";
        c.add(2, 1);
        c.add(5, -1);
        String endDate = sdf.format(c.getTime());
        String endTime = endDate + " 23:59:59";
        return this.dao.getTopThanhDuDaily(startTime, endTime, (short)type);
    }

    @Override
    public long getPotTanLoc() throws SQLException {
        MiniGameDAOImpl miniGameDAO = new MiniGameDAOImpl();
        return miniGameDAO.getPot("tan_loc_ou");
    }

    @Override
    public void logTanLoc(String username, long money) throws IOException, TimeoutException, InterruptedException {
        LogTanLocMessage message = new LogTanLocMessage();
        message.username = username;
        message.value = money;
        MessageBusFactory.get("queue_overunder").publish("queue_overunder", (BaseMessage)message, (int)10107);
    }

    @Override
    public void updatePotTanLoc(long newValue) throws IOException, TimeoutException, InterruptedException {
        UpdatePotMessage message = new UpdatePotMessage();
        message.newValue = newValue;
        message.potName = "tan_loc_ou";
        MessageBusFactory.get("queue_pot").publish("queue_pot", (BaseMessage)message, (int)106);
    }

    @Override
    public void logRutLoc(String username, long prize, int timeRequest, long currentFund) throws IOException, TimeoutException, InterruptedException {
        LogRutLocMessge message = new LogRutLocMessge();
        message.username = username;
        message.prize = prize;
        message.timeRequest = timeRequest;
        message.currentFund = currentFund;
        MessageBusFactory.get("queue_overunder").publish("queue_overunder", (BaseMessage)message, (int)10108);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int updateLuotRutLoc(String username, int soLuotThem) throws IOException, TimeoutException, InterruptedException {
        UpdateLuotRutLocMessage message = new UpdateLuotRutLocMessage();
        message.username = username;
        message.soLuotThem = soLuotThem;
        MessageBusFactory.get("queue_overunder").publish("queue_overunder", (BaseMessage)message, (int)10109);
        int soLuotRut = soLuotThem;
        DistCache<String, RutLocCacheModel> userMap = CacheFactory.get("cacheRutLocOU", RutLocCacheModel.class);
        if (userMap.containsKey(username)) {
            try (LockHandle h = userMap.acquireLock(username, 5, TimeUnit.SECONDS)) {
                if (h == null) {
                    logger.warn("updateLuotRutLoc: lock timeout for user=" + username);
                    return soLuotRut;
                }
                RutLocCacheModel model = userMap.get(username);
                soLuotRut = model.addSoLuotRut(soLuotThem);
                userMap.put(username, model);
            }
            catch (Exception e) {
                this.logger.error(e);
            }
        } else {
            RutLocCacheModel model = new RutLocCacheModel(soLuotThem);
            userMap.put(username, model);
        }
        return soLuotRut;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int getLuotRutLoc(String username) throws SQLException {
        int soLuot = 0;
        DistCache<String, RutLocCacheModel> userMap = CacheFactory.get("cacheRutLocOU", RutLocCacheModel.class);
        if (userMap.containsKey(username)) {
            try (LockHandle h = userMap.acquireLock(username, 5, TimeUnit.SECONDS)) {
                if (h == null) {
                    logger.warn("getLuotRutLoc: lock timeout for user=" + username);
                    return 0;
                }
                RutLocCacheModel model = userMap.get(username);
                soLuot = model.getSoLuotRut();
            }
            catch (Exception e) {
                this.logger.error(e);
            }
        } else {
            soLuot = this.dao.getSoLanRutLoc(username);
            RutLocCacheModel model = new RutLocCacheModel(soLuot);
            userMap.put(username, model);
        }
        return soLuot;
    }

    @Override
    public List<XepHangRLTLModel> getXepHangTanLoc() {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap topMap = client.getMap("cacheTop");
        if (topMap != null) {
            TopRLTLModel topTanLoc = (TopRLTLModel)topMap.get("TopTanLocOU");
            if (topTanLoc == null) {
                topTanLoc = new TopRLTLModel();
            }
            if (topTanLoc.getResults().size() == 0) {
                List<XepHangRLTLModel> results = this.dao.getXepHangTanLoc();
                topTanLoc.setResults(results);
                topMap.put("TopTanLocOU", topTanLoc);
            }
            return topTanLoc.getResults();
        }
        return this.dao.getXepHangTanLoc();
    }

    @Override
    public List<VinhDanhRLTLModel> getVinhDanhTanLoc() {
        return this.dao.getVinhDanhTanLoc();
    }

    @Override
    public long getSoTienTanLoc(String username) {
        return this.dao.getTongTienTanLoc(username);
    }

    @Override
    public List<XepHangRLTLModel> getXepHangRutLoc() {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap topMap = client.getMap("cacheTop");
        if (topMap != null) {
            TopRLTLModel topRutLoc = (TopRLTLModel)topMap.get("TopRutLocOU");
            if (topRutLoc == null) {
                topRutLoc = new TopRLTLModel();
            }
            if (topRutLoc.getResults().size() == 0) {
                List<XepHangRLTLModel> results = this.dao.getXepHangRutLoc();
                topRutLoc.setResults(results);
                topMap.put("TopRutLocOU", topRutLoc);
            }
            return topRutLoc.getResults();
        }
        return this.dao.getXepHangRutLoc();
    }

    @Override
    public List<VinhDanhRLTLModel> getVinhDanhRutLoc() {
        return this.dao.getVinhDanhRutLoc();
    }

    @Override
    public long getSoTienRutLoc(String username) {
        return this.dao.getTongTienRutLoc(username);
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
}

