/*
 * Decompiled with CFR 0.144.
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
import com.vinplay.dal.dao.TaiXiuDAO;
import com.vinplay.dal.dao.impl.MiniGameDAOImpl;
import com.vinplay.dal.dao.impl.TaiXiuDAOImpl;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel;
import com.vinplay.dal.service.TaiXiuService;
import com.vinplay.vbee.common.cache.CacheFactory;
import com.vinplay.vbee.common.cache.DistCache;
import com.vinplay.vbee.common.cache.LockHandle;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.LogRutLocMessge;
import com.vinplay.vbee.common.messages.minigame.LogTanLocMessage;
import com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.ThanhDuMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage;
import com.vinplay.vbee.common.messages.minigame.UpdatePotMessage;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class TaiXiuServiceImpl
        implements TaiXiuService {
    private Logger logger = Logger.getLogger((String)"rmq");
    private TaiXiuDAO dao = new TaiXiuDAOImpl();

    @Override
    public String getLichSuPhien(int soPhien, int moneyType) throws SQLException {
        List<ResultTaiXiu> results = this.dao.getLichSuPhien(soPhien, moneyType);
        return this.buildLichSuPhien(results, soPhien);
    }

    public List<ResultTaiXiu> getListLichSuPhien(int soPhien, int moneyType) {
        try {
            return this.dao.getLichSuPhien(soPhien, moneyType);
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    @Override
    public List<TopWin> getTopWin(int moneyType) throws SQLException {
        TopWinCache topTXCache;
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap topMap = client.getMap("cacheTop");
        if (topMap.containsKey((Games.TAI_XIU.getName() + "_" + moneyType)) && (topTXCache = (TopWinCache)topMap.get((Games.TAI_XIU.getName() + "_" + moneyType))) != null) {
            return topTXCache.getResult();
        }
        return new ArrayList<TopWin>();
    }

    @Override
    public void updateAllTop() {
        try {
            List<TopWin> topWinVin = this.dao.getTopTaiXiu(1);
            this.logger.debug(("TOP WIN VIN: " + topWinVin.size()));
            List<TopWin> topWinXu = this.dao.getTopTaiXiu(0);
            this.logger.debug(("TOP WIN XU: " + topWinXu.size()));
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap topMap = client.getMap("cacheTop");
            TopWinCache cacheVin = (TopWinCache)topMap.get((Games.TAI_XIU.getName() + "_1"));
            if (cacheVin == null) {
                cacheVin = new TopWinCache();
            }
            cacheVin.setResult(topWinVin);
            topMap.put((Games.TAI_XIU.getName() + "_1"), cacheVin);
            TopWinCache cacheXu = (TopWinCache)topMap.get((Games.TAI_XIU.getName() + "_0"));
            if (cacheXu == null) {
                cacheXu = new TopWinCache();
            }
            cacheXu.setResult(topWinXu);
            topMap.put((Games.TAI_XIU.getName() + "_0"), cacheXu);
        }
        catch (SQLException e) {
            this.logger.error("UPDATE ALL TOP exception: ", (Throwable)e);
            e.printStackTrace();
        }
    }

    @Override
    public ResultTaiXiu getKetQuaPhien(long referenceId, int moneyType) throws SQLException {
        System.out.println("TAIXIUDEBUG TaiXiuServiceImpl getKetQuaPhien: " + referenceId + " " + moneyType);
        ResultTaiXiu resultTX = this.dao.getKetQuaPhien(referenceId, moneyType);
        System.out.println("TAIXIUDEBUG TaiXiuServiceImpl getKetQuaPhien result: " + resultTX);
        return resultTX;
    }

    

    @Override
    public void calculateThanhDu(long referenceId, List<TransactionTaiXiu> transacntions, int result) throws IOException, TimeoutException, InterruptedException {
        DistCache<String, ThanhDuTXModel> winMap = CacheFactory.get("cacheWinThanhDuTX", ThanhDuTXModel.class);
        DistCache<String, ThanhDuTXModel> lossMap = CacheFactory.get("cacheLossThanhDuTX", ThanhDuTXModel.class);
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
        if (moneyExchange < 2000L) return;
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
                    MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", (BaseMessage)message, (int)104);
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
                ThanhDuMessage message2 = new ThanhDuMessage(model.username, model.number, model.totalValue, model.currentReferenceId, model.getReferences(), (short)type);
                MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", (BaseMessage)message2, (int)104);
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
        return miniGameDAO.getPot("tan_loc");
    }

    @Override
    public void logTanLoc(String username, long money) throws IOException, TimeoutException, InterruptedException {
        LogTanLocMessage message = new LogTanLocMessage();
        message.username = username;
        message.value = money;
        MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", (BaseMessage)message, (int)107);
    }

    @Override
    public void updatePotTanLoc(long newValue) throws IOException, TimeoutException, InterruptedException {
        UpdatePotMessage message = new UpdatePotMessage();
        message.newValue = newValue;
        message.potName = "tan_loc";
        MessageBusFactory.get("queue_pot").publish("queue_pot", (BaseMessage)message, (int)106);
    }

    @Override
    public void logRutLoc(String username, long prize, int timeRequest, long currentFund) throws IOException, TimeoutException, InterruptedException {
        LogRutLocMessge message = new LogRutLocMessge();
        message.username = username;
        message.prize = prize;
        message.timeRequest = timeRequest;
        message.currentFund = currentFund;
        MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", (BaseMessage)message, (int)108);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int updateLuotRutLoc(String username, int soLuotThem) throws IOException, TimeoutException, InterruptedException {
        UpdateLuotRutLocMessage message = new UpdateLuotRutLocMessage();
        message.username = username;
        message.soLuotThem = soLuotThem;
        MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", (BaseMessage)message, (int)109);
        int soLuotRut = soLuotThem;
        DistCache<String, RutLocCacheModel> userMap = CacheFactory.get("cacheRutLocTX", RutLocCacheModel.class);
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

    @Override
    public int updateDealerProfit(long phienid, int result, long total_money_tai, long total_money_xiu, long total_profit, long last_balance) throws IOException, TimeoutException, InterruptedException {
        return 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int getLuotRutLoc(String username) throws SQLException {
        int soLuot = 0;
        DistCache<String, RutLocCacheModel> userMap = CacheFactory.get("cacheRutLocTX", RutLocCacheModel.class);
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
            TopRLTLModel topTanLoc = (TopRLTLModel)topMap.get("TopTanLoc");
            if (topTanLoc == null) {
                topTanLoc = new TopRLTLModel();
            }
            if (topTanLoc.getResults().size() == 0) {
                List<XepHangRLTLModel> results = this.dao.getXepHangTanLoc();
                topTanLoc.setResults(results);
                topMap.put("TopTanLoc", topTanLoc);
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
            TopRLTLModel topRutLoc = (TopRLTLModel)topMap.get("TopRutLoc");
            if (topRutLoc == null) {
                topRutLoc = new TopRLTLModel();
            }
            if (topRutLoc.getResults().size() == 0) {
                List<XepHangRLTLModel> results = this.dao.getXepHangRutLoc();
                topRutLoc.setResults(results);
                topMap.put("TopRutLoc", topRutLoc);
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

    @Override
    public List getChiTietPhienTX(long referenceId, int moneyType) {
        // return null;
        try {
            return ((TaiXiuDAO) this.dao).getChiTietPhien(referenceId, moneyType);
        } catch (Exception e) {
            org.apache.log4j.Logger.getLogger("dao").error("getChiTietPhienTX failed: " + e.getMessage(), e);
            return new java.util.ArrayList();
        }
    }

    @Override
    public List getLichSuGiaoDich(String username, int page, int moneyType) {
        // SUN-672: was a null-returning stub that made c=100 (TaiXiu history)
        // always respond with transactions:null. Delegate to the real DAO.
        try {
            return this.dao.getLichSuGiaoDich(username, page, moneyType);
        } catch (java.sql.SQLException e) {
            logger.error("TaiXiuServiceImpl.getLichSuGiaoDich failed for " + username, e);
            return new java.util.ArrayList();
        }
    }

    @Override
    public void setKetQuaTaiXiu(short[] ketQua) {
        com.hazelcast.core.HazelcastInstance client = com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance();
        com.hazelcast.core.IMap map = client.getMap("ketquataixiu");
        map.put("ketquataixiu", ketQua);
    }

    @Override
    public short[] getKetQuaTaiXiu() {
        com.hazelcast.core.HazelcastInstance client = com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance();
        com.hazelcast.core.IMap map = client.getMap("ketquataixiu");
        if (map.containsKey("ketquataixiu")) {
            return (short[]) map.get("ketquataixiu");
        }
        return null;
    }

    @Override
    public short[] suaKetQuaTaiXiu() {
        com.hazelcast.core.HazelcastInstance client = com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance();
        com.hazelcast.core.IMap map = client.getMap("ketquataixiu");
        if (map.containsKey("ketquataixiu")) {
            return (short[]) map.remove("ketquataixiu");
        }
        return null;
    }

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
        msg.jackpot = jp;
        MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", (BaseMessage)msg, (int)100);
        return true;
    }

    public boolean saveResultTaiXiu(long referenceId, int result, int dice1, int dice2, int dice3, long totalTai, long totalXiu, int numBetTai, int numBetXiu, long totalPrize, long totalRefundTai, long totalRefundXiu, long totalRevenue, int moneyType, long totalJp) throws Exception {
        try {
        System.out.println("TAIXIUDEBUG TaiXiuServiceImpl saveResultTaiXiu: " + referenceId);
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
        MessageBusFactory.get("queue_taixiu").publishOrThrow("queue_taixiu", (BaseMessage)msg, (int)101);
        System.out.println("TAIXIUDEBUG TaiXiuServiceImpl saveResultTaiXiu published: " + referenceId);
        return true;
        }
        catch (Exception e) {
            System.out.println("TAIXIUDEBUG TaiXiuServiceImpl saveResultTaiXiu failed: " + referenceId);
            throw e;
        }
    }

    public boolean saveResultTaiXiu(ResultTaiXiu rs) throws Exception {
        return this.saveResultTaiXiu(rs.referenceId, rs.result, rs.dice1, rs.dice2, rs.dice3, rs.totalTai, rs.totalXiu, rs.numBetTai, rs.numBetXiu, rs.totalPrize, rs.totalRefundTai, rs.totalRefundXiu, rs.totalRevenue, rs.moneyType, rs.totalJp);
    }

    public boolean saveTransactionTaiXiu(List<TransactionTaiXiu> trans) throws IOException, TimeoutException, InterruptedException {
        for (TransactionTaiXiu tran : trans) {
            this.saveTransactionTaiXiu(tran.referenceId, tran.userId, tran.username, tran.moneyType, tran.betValue, (short)tran.betSide, tran.totalPrize + tran.totalJp, tran.totalRefund, tran.totalJp);
        }
        return false;
    }

    public boolean saveTransactionTaiXiuDetail(TransactionTaiXiuDetail tran) throws IOException, TimeoutException, InterruptedException {
        TransactionTaiXiuDetailMessage msg = new TransactionTaiXiuDetailMessage();
        msg.referenceId = tran.referenceId;
        msg.transactionCode = tran.transactionCode;
        msg.userId = tran.userId;
        msg.username = tran.username;
        msg.betValue = tran.betValue;
        msg.betSide = tran.betSide;
        msg.prize = tran.prize + tran.jpAmount;
        msg.refund = tran.refund;
        msg.inputTime = tran.inputTime;
        msg.moneyType = tran.moneyType;
        msg.jackpot = tran.jpAmount;
        msg.currentMoney = tran.currentMoney;
        MessageBusFactory.get("queue_taixiu").publish("queue_taixiu", (BaseMessage)msg, (int)102);
        return true;
    }

    public short checkJackpotTaiXiu() {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap("jackpottaixiu");
        String key = "jackpottaixiu";
        if (map.containsKey(key)) {
            return (Short)map.remove(key);
        }
        return 0;
    }

    public short checkJackpotTaiXiuNotRemove() {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap("jackpottaixiu");
        String key = "jackpottaixiu";
        if (map.containsKey(key)) {
            return (Short)map.get(key);
        }
        return 0;
    }

    @Override
    public void updateJackpotTaiXiu(short ketQua) {
        com.hazelcast.core.HazelcastInstance client = com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance();
        com.hazelcast.core.IMap map = client.getMap("jackpottaixiu");
        map.put("jackpottaixiu", ketQua);
}
}
