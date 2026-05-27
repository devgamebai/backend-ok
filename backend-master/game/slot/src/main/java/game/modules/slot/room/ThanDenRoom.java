/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.SlotMachineService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.cache.SlotFreeDaily
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.models.slot.SlotFreeSpin
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package game.modules.slot.room;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.common.business.Debug;
import com.vinplay.dailyQuest.DailyQuestUtils;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.operator_maxium.MaxiumWinConfig;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.GameConfig.GameConfig;
import game.modules.slot.entities.slot.thanden.ThanDenTableInfo;
import game.modules.slot.entities.slot.thanden.ThanDenUtil;
import game.modules.SlotDescription.SlotDescriptionUtils;
import game.modules.SlotUtils.GiftType;
import game.modules.slot.ThanDenModule;
import game.modules.slot.cmd.send.thanden.*;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.entities.slot.avengers.AvengersLines;
import game.modules.slot.utils.SlotHouseEdge;
import game.modules.slot.utils.SlotUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThanDenRoom extends SlotRoom {
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable checkResetPotTask = new CheckResetPotTask();
    private AvengersLines lines = new AvengersLines();
    private long lastTimeUpdatePotToRoom = 0L;
    private long lastTimeUpdateFundToRoom = 0L;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    private String gn;
    private String gameID;
    private long fundJackPot;
    private String fundJackPotName;
    private long fundMinigame;
    private String fundMinigameName;
    private int countNoHu = 0;
    private List<Integer> boxValues = new ArrayList<Integer>();
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("slot");

    protected boolean huX2 = false;

    public ThanDenRoom(ThanDenModule module, byte id, String name, short moneyType, long pot, long fund, int betValue,
                      long initPotValue, long fundJackPot, String fundJackPotName, long fundMinigame, String fundMinigameName) {
        super(id, name, betValue, moneyType, pot, fund, initPotValue);
        this.module = module;
        this.moneyType = moneyType;
        this.gameName = Games.THAN_DEN.getName();
        this.gameID = Games.THAN_DEN.getId() + "";
        this.cacheFreeName = String.valueOf(this.gameName) + betValue;
        CacheServiceImpl cacheService = new CacheServiceImpl();
        cacheService.setValue(name, (int) pot);
        this.betValue = betValue;
        this.initPotValue = initPotValue;
        this.gn = this.gameName;

        this.boxValues.add(10);
        this.boxValues.add(10);
        this.boxValues.add(10);
        this.boxValues.add(15);
        this.boxValues.add(20);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.checkResetPotTask, 10, 10, TimeUnit.SECONDS);

        this.fundJackPot = fundJackPot;
        this.fundJackPotName = fundJackPotName;

        this.fundMinigame = fundMinigame;
        this.fundMinigameName = fundMinigameName;
    }

    /**
     * Returns whether the current room is in multi-jackpot (x2) mode.
     */
    public boolean isMultiJackpot() {
        return this.huX2;
    }

    /**
     * Checks if a user is flagged for forced big win.
     */
    public boolean isUserBigWin(String username) {
        return this.userService.isUserBigWin(username);
    }

    /**
     * Checks if a user is flagged for forced jackpot.
     */
    public boolean isUserJackpot(String username) {
        return false; // stubbed
    }

    /**
     * Inner class for periodic pot reset check task.
     */
    protected final class CheckResetPotTask implements Runnable {
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                ThanDenRoom.this.checkResetPot();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void forceStopAutoPlay(User user) {
        super.forceStopAutoPlay(user);
        Map map = this.usersAuto;
        synchronized (map) {
            this.usersAuto.remove(user.getName());
            ForceStopAutoPlayThanDenMsg msg = new ForceStopAutoPlayThanDenMsg();
            SlotUtils.sendMessageToUser((BaseMsg) msg, user);
        }
    }

    public ResultThanDenMsg play(String username, String linesStr) {
        long referenceId = this.module.getNewReferenceId();
        try {
            SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(this.cacheFreeName, username);
            int luotQuayFree = freeSpin.getNum();
            int ratioFree = freeSpin.getRatio();
            if (luotQuayFree > 0) {
                linesStr = freeSpin.getLines();
                return this.playFree(username, linesStr, luotQuayFree, referenceId);
            }
        } catch (Exception e) {
            Debug.trace("error get free spin thanden");
        }
        Debug.trace("thanden referenceId="+referenceId);
        return this.playNormal(username, linesStr, referenceId);
    }

    public synchronized void addMoneyToPot(long money) {
        this.pot += money;

//        Debug.trace("addMoneyToPot  " + money, "pot  " + this.pot);
        this.savePot();
    }

    public void resetPot() {
        this.pot = this.initPotValue;
        this.savePot();
    }

    public void botEatJackpot(String keyBot, long nextTime, String username) {
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        int result = 3;
        long totalPrizes = this.pot;
        if (this.isMultiJackpot()) {
            result = 4;
            totalPrizes = this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT;
        }
        try {
            this.slotService.addTop(this.gn, username, this.betValue, totalPrizes, currentTimeStr, result);
        } catch (Exception e) {
            Debug.warn(e);
        }
        this.broadcastBigWin(username, (byte) result, totalPrizes);
        this.resetPot();
        try {
            this.mgService.saveFund(keyBot, nextTime);
        } catch (Exception e) {
            Debug.warn(e);
        }

    }

    private void broadcastBigWin(String username, byte result, long totalPrizes) {
        BigWinThanDenMsg bigWinMsg = new BigWinThanDenMsg();
        bigWinMsg.username = username;
        bigWinMsg.type = (byte) result;
        bigWinMsg.betValue = (short) this.betValue;
        bigWinMsg.totalPrizes = totalPrizes;
        bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
        this.module.sendMsgToAllUsers(bigWinMsg);
    }

    public ResultThanDenMsg playNormal(String username, String linesStr, long referenceId) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long moneyAvailable = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        UserCacheModel u = this.userService.getUser(username);
        long totalBetValue = lineArr.length * this.betValue;
        ResultThanDenMsg msg = new ResultThanDenMsg();

//        boolean forceJackpotByUser = false;
        //get force user jackpot
//        CacheServiceImpl cacheService = new CacheServiceImpl();
//        String userForce = "";
//        try {
//            userForce = cacheService.getValueStr(CACHE_NAME_USER_SPOT + this.gn);
//        } catch (Exception e) {
//            userForce = "";
//        }

        if (lineArr.length > 0 && !linesStr.isEmpty()) {
            if (totalBetValue > 0L) {
                if (totalBetValue <= moneyAvailable) {
                    long fee = totalBetValue * GameConfig.getInstance().slot11IconWildLienTucConfig.FEE / 100;
                    MoneyResponse moneyRes = new MoneyResponse(false, "1001");
                    if (!u.isBot()) {
                        moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName, this.gameID,
                                SlotDescriptionUtils.getBetDescription(this.gameID),
                                fee, Long.valueOf(referenceId), TransType.START_TRANS);
                        DailyQuestUtils.playerPlayGame(username,Games.THAN_DEN.getId(),totalBetValue);

                    } else {
                        moneyRes.setSuccess(true);
                    }
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        currentMoney = moneyRes.getCurrentMoney();
                        long moneyToPot = totalBetValue * GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_JACKPOT / 100;
//                        long moneyToFundJackpot = totalBetValue * GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_FUND_JACKPOT / 100;
                        long moneyToFundJackpot = totalBetValue * (GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_FUND_JACKPOT + GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_JACKPOT) / 100;
                        long moneyToFundMinigame = totalBetValue * GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_FUND_MINIGAME / 100;
//                        long moneyToFund = totalBetValue - fee - moneyToPot - moneyToFundJackpot - moneyToFundMinigame;
                        long moneyToFund = totalBetValue - fee - moneyToFundJackpot - moneyToFundMinigame;

                        boolean isUserBigWin = this.isUserBigWin(username);
                        if(!isUserBigWin){
                            this.fund += moneyToFund;
                            this.fundJackPot += moneyToFundJackpot;
                            this.fundMinigame += moneyToFundMinigame;
                            this.pot += moneyToPot;
                        }

                        int[] rowIndex = new int[lineArr.length];
                        for (int i = 0; i < lineArr.length; i++) {
                            rowIndex[i] = Integer.parseInt(lineArr[i]);
                        }

                        long totalPrizes = 0;
                        long tienThuongX2 = 0;
                        int countFreeSpin = 0;

                        long userValue = this.userService.getUserValue(username);
                        long maxiumWin = MaxiumWinConfig.getMaxiumMoneyWin(userValue) - moneyRes.getCurrentMoney();
//                        Debug.trace(" get user value maxiumWin  ", userValue, maxiumWin);

                        ThanDenTableInfo slot11IconWildLienTucTableInfo = ThanDenUtil.getThanDenTableInfo(rowIndex, this.betValue, this.fund,
                                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                this.boxValues, false,maxiumWin, false, 1);

                        // Check for admin-forced result (SUN-68/69)
                        int[][] forcedIds = game.modules.slot.utils.SlotForceResultHelper.checkAndConsume("THANDEN", username, 3, 5);
                        if (forcedIds != null) {
                            byte[] forcedTable = new byte[15];
                            for (int fi = 0; fi < 3; fi++)
                                for (int fj = 0; fj < 5; fj++)
                                    forcedTable[fi * 5 + fj] = (byte) forcedIds[fi][fj];
                            slot11IconWildLienTucTableInfo.table = forcedTable;
                            slot11IconWildLienTucTableInfo.tableCalculate = forcedTable.clone();
                        }

                        if(isUserBigWin){
                            slot11IconWildLienTucTableInfo = ThanDenUtil.getThanDenBigWinTableInfo(rowIndex, this.betValue, this.fund,
                                    this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                    this.boxValues, false,maxiumWin, false, 1);
                        }

                        if (this.isUserJackpot(username)) {
                            slot11IconWildLienTucTableInfo = new ThanDenTableInfo(GiftType.JACKPOT, this.betValue,
                                    ThanDenUtil.getMoneyJackPot(this.pot, this.isMultiJackpot(), this.initPotValue),
                                    this.boxValues);
                            slot11IconWildLienTucTableInfo.calculate(rowIndex, false, 1);
                        }
//                        slot11IconWildLienTucTableInfo = new ThanDenTableInfo(GiftType.MINI_GAME, this.betValue,
//                                this.pot, this.boxValues);
//                        slot11IconWildLienTucTableInfo.calculate(rowIndex);

                        totalPrizes += slot11IconWildLienTucTableInfo.money * this.betValue;
                        if(!isUserBigWin) {
                            // Phase 3c — pct-aware force-lose (SLOT_USE_DYNAMIC_RTP flag)
                            totalPrizes = game.modules.slot.utils.SlotHouseEdge.maybeForceLose(totalPrizes, result, username, "slot_thanden");
                            this.fund -= totalPrizes;
                        }
                        if (slot11IconWildLienTucTableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                totalPrizes += this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT;
                                if(!isUserBigWin) {
//                                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT;
                                }
                                tienThuongX2 = this.pot * (GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT - 1);
                            } else {
                                totalPrizes += this.pot;
                                if(!isUserBigWin) {
//                                    this.fundJackPot -= this.initPotValue;
                                    this.fundJackPot -= this.pot;
                                }
                            }
                        }
                        if (slot11IconWildLienTucTableInfo.miniGame > 0) {
                            totalPrizes += slot11IconWildLienTucTableInfo.miniGameSlotResponse.getTotalPrize();
                            if(!isUserBigWin) {
                                this.fundMinigame -= slot11IconWildLienTucTableInfo.miniGameSlotResponse.getTotalPrize();
                            }
                        }
                        if (slot11IconWildLienTucTableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                result = 4;
                                this.noHuX2();
                                //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "GS");
                            } else {
                                result = 3;
                            }
                            this.resetPot();
                        } else {
                            if (slot11IconWildLienTucTableInfo.miniGame > 0) {
                                result = 5;
                            } else {
                                result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
                            }
                        }
                        long moneyExchange = totalPrizes - tienThuongX2;
                        if (tienThuongX2 > 0L && !u.isBot()) {
                            moneyRes = this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName,
                                    this.gameID, SlotDescriptionUtils.getMultiJackpotDescription(this.gameID),
                                    0L, (Long) null, TransType.NO_VIPPOINT);
                            currentMoney = moneyRes.getCurrentMoney();
                        }

                        if (totalPrizes != 0 && !u.isBot()) {
                            if ((moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, this.gameName,
                                    this.gameID, SlotDescriptionUtils.getPayDescription(this.gameID, totalBetValue, totalPrizes, result),
                                    0L, Long.valueOf(referenceId), TransType.END_TRANS)) != null && moneyRes.isSuccess()) {
                                currentMoney = moneyRes.getCurrentMoney();
                                if (this.moneyType == 1 && moneyExchange >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
                                    this.broadcastMsgService.putMessage(Games.THAN_DEN.getId(), username, moneyExchange - totalBetValue);
                                }
                            }
                        }

                        String linesWin = slot11IconWildLienTucTableInfo.lineWinToString();
                        String prizesOnLine = slot11IconWildLienTucTableInfo.moneyWinToString();
                        countFreeSpin = slot11IconWildLienTucTableInfo.freeSpin;
                        msg.referenceId = referenceId;
                        msg.matrix = slot11IconWildLienTucTableInfo.matrixToString();
                        msg.linesWin = linesWin;
                        msg.prize = totalPrizes;
                        msg.haiSao = "";
                        if (slot11IconWildLienTucTableInfo.miniGame > 0) {
                            msg.haiSao = slot11IconWildLienTucTableInfo.miniGameSlotResponse.getPrizes();
                        }
                        if (countFreeSpin > 0) {
                            msg.freeSpin = 1;
                            msg.ratio = (byte) countFreeSpin;
                            msg.currentNumberFreeSpin = (byte) countFreeSpin;
                            this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, username, linesStr, countFreeSpin, slot11IconWildLienTucTableInfo.freeSpinRatio);
                        } else {
                            msg.freeSpin = 0;
                        }
                        try {
                            if (!u.isBot()) {
                                this.slotService.logThanDen(referenceId, username, this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);

                            }
                            if (result == 3 || result == 4) {
                                this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, result);
                            }
                            if (result == 3 || result == 2 || result == 4) {
                                this.broadcastBigWin(username, (byte) result, totalPrizes);
                            }
                        } catch (InterruptedException bigWinMsg) {
                        } catch (TimeoutException bigWinMsg) {
                        } catch (IOException bigWinMsg) {
                            // empty catch block
                        }
                        this.saveFund();
                        this.savePot();
                    }
                } else {
                    result = 102;
                }
            } else {
                result = 101;
            }
        } else {
            result = 101;
        }
        msg.result = (byte) result;
        msg.currentMoney = currentMoney;
        long endTime = System.currentTimeMillis();
        long handleTime = endTime - startTime;
        String ratioTime = CommonUtils.getRatioTime((long) handleTime);
        // SlotUtils.logAvengers(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
        Debug.trace("Normal Spin thanden", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

//    public ResultThanDenMsg playNormal(String username, String linesStr, long referenceId) {
//        long startTime = System.currentTimeMillis();
//        String currentTimeStr = DateTimeUtils.getCurrentTime();
//        short result = 0;
//        String[] lineArr = linesStr.split(",");
//        long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
//        UserCacheModel u = this.userService.getUser(username);
//        long totalBetValue = lineArr.length * this.betValue;
//        ResultThanDenMsg msg = new ResultThanDenMsg();
//
//        boolean forceJackpotByUser = false;
//        //get force user jackpot
//        CacheServiceImpl cacheService = new CacheServiceImpl();
//        String userForce = "";
//        try{
//            userForce = cacheService.getValueStr(CACHE_NAME_USER_SPOT + this.gn);
//        }catch (Exception e){
//            userForce = "";
//        }
//
//        if (lineArr.length > 0 && !linesStr.isEmpty()) {
//            if (totalBetValue > 0L) {
//                if (totalBetValue <= currentMoney) {
//                    long fee = totalBetValue * 2L / 100L;
//                    MoneyResponse moneyRes = new MoneyResponse(false, "1001");
//                    if(!u.isBot()){
//                        moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName, "Quay " + gn, "\u0110\u1eb7t c\u01b0\u1ee3c " + gn, fee, Long.valueOf(referenceId), TransType.START_TRANS);
//                    }else{
//                        moneyRes.setSuccess(true);
//                    }
//                    if (moneyRes != null && moneyRes.isSuccess()) {
//                        long moneyToPot = totalBetValue * 1L / 100L;
//                        long moneyToFund = totalBetValue - fee - moneyToPot;
//                        if (!u.isBot()) {
//                            this.fund += moneyToFund;
//                        }
//                        this.pot += moneyToPot;
//                        boolean enoughPair = false;
//                        ArrayList<AwardsOnLine<AvengersAward>> awardsOnLines = new ArrayList<AwardsOnLine<AvengersAward>>();
//                        long totalPrizes = 0L;
//                        long soTienNoHuKhongTruQuy = 0L;
//                        long tienThuongX2 = 0L;
//                        int countScatter = 0;
//                        int countBonus = 0;
//                        MiniGameSlotResponse miniGameSlot = null;
//                        block4 : while (!enoughPair) {
//                            int n;
//                            int soLanNoHu;
//                            Random rd;
//                            result = 0;
//                            awardsOnLines.clear();
//                            totalPrizes = 0L;
//                            soTienNoHuKhongTruQuy = 0L;
//                            tienThuongX2 = 0L;
//                            String linesWin = "";
//                            String prizesOnLine = "";
//                            miniGameSlot = null;
//                            countScatter = 0;
//                            countBonus = 0;
//                            boolean forceNoHu = false;
////                            if (lineArr.length >= 5 && (soLanNoHu = ConfigGame.getIntValue(String.valueOf(this.gameName) + "_so_lan_no_hu")) > 0 && this.fund > this.initPotValue * 2L && (n = (rd = new Random()).nextInt(soLanNoHu)) == 0) {
////                                forceNoHu = true;
////                            }
//                            if (betValue == 100)
//                            {
//                                soLanNoHu = ConfigGame.getIntValue(String.valueOf(this.gameName) + "_so_lan_no_hu_100");
//                                if (lineArr.length >= 20 && soLanNoHu > 0 && this.fund > this.pot * 2L && (n = (rd = new Random()).nextInt(soLanNoHu)) == 0 && countNoHu >= soLanNoHu) {
//                                    forceNoHu = true;
//                                }
//                                if (userForce.equals(username) && lineArr.length >= 20 && soLanNoHu > 0 && this.fund > this.pot * 2L  && countNoHu >= soLanNoHu) {
//                                    forceNoHu = true;
//                                    forceJackpotByUser = true;
//                                }
//                            }
//                            else if (betValue == 1000)
//                            {
//                                soLanNoHu = ConfigGame.getIntValue(String.valueOf(this.gameName) + "_so_lan_no_hu_1000");
//                                if (lineArr.length >= 25 && soLanNoHu > 0 && this.fund > this.pot * 3L && (n = (rd = new Random()).nextInt(soLanNoHu)) == 0 && countNoHu >= soLanNoHu) {
//                                    forceNoHu = true;
//                                }
//                                if (userForce.equals(username) && lineArr.length >= 25 && soLanNoHu > 0 && this.fund > this.pot * 3L  && countNoHu >= soLanNoHu) {
//                                    forceNoHu = true;
//                                    forceJackpotByUser = true;
//                                }
//                            }
//                            else
//                            {
//                                soLanNoHu = ConfigGame.getIntValue(String.valueOf(this.gameName) + "_so_lan_no_hu_10000");
//                                if (lineArr.length >= 25 && soLanNoHu > 0 && this.fund > this.pot * 3L && (n = (rd = new Random()).nextInt(soLanNoHu)) == 0 && countNoHu >= soLanNoHu) {
//                                    forceNoHu = true;
//                                }
//                                if (userForce.equals(username) && lineArr.length >= 25 && soLanNoHu > 0 && this.fund > this.pot * 3L  && countNoHu >= soLanNoHu) {
//                                    forceNoHu = true;
//                                    forceJackpotByUser = true;
//                                }
//                            }
//                            //logger.info(gn+" username: "+username + " forceNoHu:"+forceNoHu);
//                            // kiem tra neu no hu
//                            if (forceNoHu)
//                            {
//                                try
//                                {
//                                    LogMoneyUserDao logService = new LogMoneyUserDaoImpl();
//                                    long total_user_receive = 0;
//                                    long total_agency_receive = 0;
//
//                                    AgentServiceImpl service = new AgentServiceImpl();
//                                    List<AgentResponse> agents = service.listAgent();
//                                    ArrayList<String> agentNames = new ArrayList<String>();
//                                    if (agents != null && agents.size() > 0) {
//                                        for (AgentResponse agent : agents) {
//                                            agentNames.add(agent.nickName);
//                                        }
//                                    }
//                                    List<LogUserMoneyResponse> resulReceive = logService.searchAllLogMoneyUser(username, "RECEIVE",false);
//                                    if (resulReceive != null && resulReceive.size() > 0) {
//                                        for (LogUserMoneyResponse trans : resulReceive) {
//                                            boolean matchAgent = false;
//                                            for (String s : agentNames) {
//                                                if (trans.description.contains(s)) {
//                                                    matchAgent = true;
//                                                }
//                                            }
//                                            if (matchAgent) {
//                                                total_agency_receive += trans.moneyExchange;
//                                            } else {
//                                                total_user_receive += trans.moneyExchange;
//                                            }
//                                        }
//                                    }
//
//                                    long total_recharge_card_money = 0;//total_agency_receive - userModel.getRechargeMoney();
//                                    List<LogUserMoneyResponse> resultCard = logService.searchAllLogMoneyUser(username, "CARD",false);
//                                    if (resultCard != null && resultCard.size() > 0) {
//
//
//                                        for (LogUserMoneyResponse trans : resultCard) {
//                                            total_recharge_card_money += trans.moneyExchange;
//                                        }
//
//                                    }
//
//                                    long total_deposit_bank = 0;
//                                    long total_deposit_momo = 0;
//
//                                    //search total deposit bank
//                                    List<LogUserMoneyResponse> resultBank = logService.searchAllLogMoneyUser(username, "BANK",false);
//                                    if (resultBank != null && resultBank.size() > 0) {
//
//                                        for (LogUserMoneyResponse trans : resultBank) {
//                                            total_deposit_bank += trans.moneyExchange;
//                                        }
//                                    }
//                                    //search total deposit momo
//
//                                    List<LogUserMoneyResponse> resultMomo = logService.searchAllLogMoneyUser(username, "MOMO",false);
//                                    if (resultMomo != null && resultMomo.size() > 0) {
//
//                                        for (LogUserMoneyResponse trans : resultMomo) {
//                                            total_deposit_momo += trans.moneyExchange;
//                                        }
//
//                                    }
//
//                                    if ((total_agency_receive == 0 && total_recharge_card_money == 0 && total_deposit_momo == 0 && total_deposit_bank == 0)
//                                            || (total_agency_receive + total_recharge_card_money + total_deposit_momo + total_deposit_bank) < 1000000)
//                                    {
//                                        forceNoHu = false;
//                                    }
//                                }
//                                catch (Exception ex)
//                                {
//                                    Debug.trace(ex.getMessage());
//                                    StringWriter sw = new StringWriter();
//                                    PrintWriter pw = new PrintWriter(sw);
//                                    ex.printStackTrace(pw);
//                                    String sStackTrace = sw.toString(); // stack trace as a string
//                                    Debug.trace((Object)sStackTrace);
//                                    forceNoHu = false;
//                                }
//                            }
//                            AvengersItem[][] matrix = forceNoHu ? AvengersUtils.generateMatrixNoHu(lineArr) : AvengersUtils.generateMatrix();
//                            //AvengersItem[][] matrix = AvengersUtils.generateMatrixNoHu(lineArr);
//                            for (int i = 0; i < 3; ++i) {
//                                for (int j = 0; j < 5; ++j) {
//                                    if (matrix[i][j] == AvengersItem.SCATTER) {
//                                        ++countScatter;
//                                        continue;
//                                    }
//                                    if (matrix[i][j] != AvengersItem.BONUS) continue;
//                                    ++countBonus;
//                                }
//                            }
//                            if (countBonus >= 3 || countScatter >= 3) {
//                                Random rd2 = new Random();
//                                int tiLeAn = lineArr.length * 100 / 25;
//                                int n2 = rd2.nextInt(100);
//                                if (n2 >= tiLeAn) continue;
//                            }
//                            if (countBonus >= 3) {
//                                miniGameSlot = AvengersUtils.addMiniGameSlot(this.betValue, countBonus);
//                                AvengersAward award = AvengersAwards.getAward(AvengersItem.BONUS, countBonus);
//                                AwardsOnLine<AvengersAward> aol = new AwardsOnLine<AvengersAward>(award, miniGameSlot.getTotalPrize(), "line0");
//                                awardsOnLines.add(aol);
//                                result = 5;
//                            }
//                            AvengersItem[][] matrixWild = AvengersUtils.revertMatrix(matrix);
//                            for (String entry2 : lineArr) {
//                                ArrayList<AvengersAward> awardList = new ArrayList<AvengersAward>();
//                                Line line = AvengersUtils.getLine(this.lines, matrixWild, Integer.parseInt(entry2));
//                                AvengersUtils.calculateLine(line, awardList);
//                                for (AvengersAward award2 : awardList) {
//                                    long moneyOnLine = 0L;
//                                    if (award2.getRatio() > 0.0f) {
//                                        moneyOnLine = (long)(award2.getRatio() * (float)this.betValue);
//                                    } else if (award2 == AvengersAward.PENTA_JACK_POT) {
//                                        if (result == 3) {
//                                            moneyOnLine = this.initPotValue;
//                                        } else {
//                                            if (this.huX2) {
//                                                moneyOnLine = this.pot * 2L;
//                                                tienThuongX2 = this.pot;
//                                                soTienNoHuKhongTruQuy += this.pot;
//                                            } else {
//                                                moneyOnLine = this.pot;
//                                            }
//                                            result = 3;
//                                            soTienNoHuKhongTruQuy += this.pot - this.initPotValue;
//                                        }
//                                    }
//                                    AwardsOnLine<AvengersAward> aol2 = new AwardsOnLine<AvengersAward>(award2, moneyOnLine, line.getName());
//                                    awardsOnLines.add(aol2);
//                                }
//                            }
//                            StringBuilder builderLinesWin = new StringBuilder();
//                            StringBuilder builderPrizesOnLine = new StringBuilder();
//                            for (AwardsOnLine entry2 : awardsOnLines) {
//                                if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && !forceNoHu && !u.isBot())
//                                    continue block4;
//
//                                if (betValue == 100)
//                                {
//                                    if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_100") == 0) // cho ca nguoi va bot no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT)) continue block4;
//                                    }
//                                    else if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_100") == 1) // chi cho bot no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && !u.isBot()) continue block4;
//                                    }
//                                    else if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_100") == -1) // chi cho nguoi no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && u.isBot()) continue block4;
//                                    }
//                                    else
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && !u.isBot()) continue block4;
//                                    }
//                                }
//                                else if (betValue == 1000)
//                                {
//                                    if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_1000") == 0) // cho ca nguoi va bot no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT)) continue block4;
//                                    }
//                                    else if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_1000") == 1) // chi cho bot no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && !u.isBot()) continue block4;
//                                    }
//                                    else if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_1000") == -1) // chi cho nguoi no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && u.isBot()) continue block4;
//                                    }
//                                    else
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && !u.isBot()) continue block4;
//                                    }
//                                }
//                                else
//                                {
//                                    if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_10000") == 0) // cho ca nguoi va bot no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT)) continue block4;
//                                    }
//                                    else if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_10000") == 1) //chi cho bot no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && !u.isBot()) continue block4;
//                                    }
//                                    else if (ConfigGame.getIntValue(this.gameName + "_cho_bot_no_hu_10000") == -1) // chi cho nguoi no hu
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && u.isBot()) continue block4;
//                                    }
//                                    else
//                                    {
//                                        if ((entry2.getAward() == AvengersAward.PENTA_JACK_POT || entry2.getAward() == AvengersAward.QUADAR_JACK_POT || entry2.getAward() == AvengersAward.TRIPLE_JACK_POT) && !u.isBot()) continue block4;
//                                    }
//                                }
//                                // bot or not if (entry2.getAward() == AvengersAward.PENTA_JACK_POT && !u.isBot()) continue block4;
//                                totalPrizes += entry2.getMoney();
//                                builderLinesWin.append(",");
//                                builderLinesWin.append(entry2.getLineId());
//                                builderPrizesOnLine.append(",");
//                                builderPrizesOnLine.append(entry2.getMoney());
//                            }
//                            if (builderLinesWin.length() > 0) {
//                                builderLinesWin.deleteCharAt(0);
//                            }
//                            if (builderPrizesOnLine.length() > 0) {
//                                builderPrizesOnLine.deleteCharAt(0);
//                            }
//                            if(!u.isBot()){
//                                if (result == 3 ? this.fund - (totalPrizes - soTienNoHuKhongTruQuy) < 0L : this.fund - totalPrizes < this.pot * 2L && totalPrizes - totalBetValue >= 0L) continue;
//                            }
//        
// [DISABLED-MR28]                     // [SUN-589 Phase 3c] RTP Engine — exempt jackpot (result==3||4)
// [DISABLED-MR28]                     if (result != 3 && result != 4) {
// [DISABLED-MR28]                         if (SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_thanden")) {
// [DISABLED-MR28]                             
// [DISABLED-MR28]                                        totalPrizes = 0;
// [DISABLED-MR28]                                        awardsOnLines.clear();
// [DISABLED-MR28]                                        builderLinesWin = new StringBuilder();
// [DISABLED-MR28]                                        builderPrizesOnLine = new StringBuilder();
// [DISABLED-MR28]                                        result = 0;
// [DISABLED-MR28]                                    }
// [DISABLED-MR28]                     }
// [DISABLED-MR28]                     enoughPair = true;
// [DISABLED-MR28] //                            String matrixStr = AvengersUtils.matrixToString(matrix);
//                            if (totalPrizes > 0L) {
//                                if (result == 3) {
//                                    if (this.huX2) {
//                                        if (!u.isBot() && this.countNoHu < soLanNoHu * 2) continue;
//                                        result = 4;
//                                    }
//                                    else
//                                    {
//                                        if (!u.isBot() && this.countNoHu < soLanNoHu) continue;
//                                    }
//                                    countNoHu = 0;
//                                    this.noHuX2();
//                                    this.pot = this.initPotValue;
//                                    //this.fund -= totalPrizes - soTienNoHuKhongTruQuy;
//                                    this.fund = 0;
//                                    if (this.moneyType == 1) {
//                                        //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " vin");
//                                    }
//                                    // get usercache
//                                    HazelcastInstance client = HazelcastClientFactory.getInstance();
//                                    IMap<String, UserModel> userMap = client.getMap("users");
//                                    UserModel model = null;
//                                    String displayName = username;
//                                    if (userMap.containsKey((Object)username)) {
//                                        model = (UserModel)userMap.get((Object)displayName);
//                                        if (model.getClient() != null && model.getClient() != "")
//                                        {
//                                            displayName = "[" + model.getClient() + "] " + username;
//                                        }
//                                        else
//                                        {
//                                            displayName = "[X] " + username;
//                                        }
//                                    }
//                                    else
//                                    {
//                                        UserDaoImpl dao = new UserDaoImpl();
//                                        try {
//                                            model = dao.getUserByNickName(username);
//                                            if (model.getClient() != null && model.getClient() != "")
//                                            {
//                                                displayName = "[" + model.getClient() + "] " + username;
//                                            }
//                                            else
//                                            {
//                                                displayName = "[X] " + username;
//                                            }
//                                        } catch (SQLException ex) {
//
//                                        }
//                                    }
//
//                                    if(forceJackpotByUser){
//                                        try{
//                                            cacheService.removeKey(CACHE_NAME_USER_SPOT + this.gn);
//                                        }catch (Exception e){
//
//                                        }
//                                    }
//                                    this.slotService.logNoHu(referenceId, this.gameName, displayName, this.betValue, linesStr, matrixStr, builderLinesWin.toString(), builderPrizesOnLine.toString(), totalPrizes, result, currentTimeStr);
//                                } else {
//                                    countNoHu++;
//                                    if (!u.isBot()) {
//                                        this.fund -= totalPrizes;
//                                    }
//                                    if (result == 0) {
//                                        result = totalPrizes >= (long)(this.betValue * 100) ? (short)2 : 1;
//                                    }
//                                }
//                            }
//                            msg.freeSpin = 0;//(byte)this.setFreeSpin(username, linesStr, countScatter);
////                            this.setFreeSpin(username, linesStr, countScatter);
//                            long moneyExchange = totalPrizes - tienThuongX2;
//                            String des = "Quay " + gn;
//                            //update when x2
//                            // only save real user
//                            if (tienThuongX2 > 0L && !u.isBot()) {
//                                this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName, des, "Th\u01b0\u1edfng h\u0169 X2", 0L, (Long) null, TransType.NO_VIPPOINT);
//                            }
//                            // only save real user
//                            if (moneyExchange != 0 && !u.isBot()) {
//                                moneyRes = this.userService.updateMoney(username, moneyExchange, this.moneyTypeStr, this.gameName, "Quay " + gn, this.buildDescription(totalBetValue, totalPrizes, result), 0L, Long.valueOf(referenceId), TransType.END_TRANS);
//                                if (moneyRes != null && moneyRes.isSuccess()) {
//                                    currentMoney = moneyRes.getCurrentMoney();
//                                    if (this.moneyType == 1 && moneyExchange - (long) this.betValue >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
//                                        this.broadcastMsgService.putMessage(Games.THAN_DEN.getId(), username, moneyExchange - (long) this.betValue);
//                                    }
//                                }
//                            }
//                            linesWin = builderLinesWin.toString();
//                            prizesOnLine = builderPrizesOnLine.toString();
//                            msg.referenceId = referenceId;
//                            msg.matrix = AvengersUtils.matrixToString(matrix);
//                            msg.linesWin = linesWin;
//                            msg.prize = totalPrizes;
//                            msg.isFreeSpin = false;
//                            if (miniGameSlot != null) {
//                                msg.haiSao = miniGameSlot.getPrizes();
//                            }
//                            try {
//                                if(!u.isBot()){
//                                    this.slotService.logChiemtinh(referenceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
//
//                                }
//                                if (result == 3 || result == 4) {
//                                    this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, (int)result);
//                                }
//                                if (result == 3 || result == 2 || result == 4) {
//                                    this.broadcastBigWin(username, (byte)result, totalPrizes);
//                                }
//                            }
//                            catch (InterruptedException bigWinMsg) {
//                            }
//                            catch (TimeoutException bigWinMsg) {
//                            }
//                            catch (IOException bigWinMsg) {
//                                // empty catch block
//                            }
//                            this.saveFund();
//                            this.savePot();
//                        }
//                    }
//                } else {
//                    result = 102;
//                }
//            } else {
//                result = 101;
//            }
//        } else {
//            result = 101;
//        }
//        msg.result = (byte)result;
//        msg.currentMoney = currentMoney;
//        long endTime = System.currentTimeMillis();
//        long handleTime = endTime - startTime;
//        String ratioTime = CommonUtils.getRatioTime((long)handleTime);
//       // SlotUtils.logAvengers(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
//        return msg;
//    }

    public ResultThanDenMsg playFreeDaily(String username) throws Exception {
        //long startTime = System.currentTimeMillis();
        // String currentTimeStr = DateTimeUtils.getCurrentTime();
        long refernceId = this.module.getNewReferenceId();
        // short result = 0;
        String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25";
        return this.playFree(username, linesStr, 0, refernceId);
    }

    public ResultThanDenMsg playFree(String username, String linesStr, int ratio, long referenceId) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long totalBetValue = 0L;
        ResultThanDenMsg msg = new ResultThanDenMsg();
        UserCacheModel u = this.userService.getUser(username);

        MoneyResponse moneyRes = new MoneyResponse(false, "1001");
        if (ratio > 0) {
            this.slotService.updateLuotQuaySlotFree(this.cacheFreeName, username);
        }
        int[] rowIndex = new int[lineArr.length];
        for (int i = 0; i < lineArr.length; i++) {
            rowIndex[i] = Integer.parseInt(lineArr[i]);
        }

        long totalPrizes = 0;
        long tienThuongX2 = 0;
        int countFreeSpin = 0;
        long userValue = this.userService.getUserValue(username);
        long maxiumWin = MaxiumWinConfig.getMaxiumMoneyWin(userValue) - moneyRes.getCurrentMoney();
//        Debug.trace(" get user value maxiumWin  ", userValue, maxiumWin);

        ThanDenTableInfo slot11IconWildLienTucTableInfo = ThanDenUtil.getThanDenTableInfo(rowIndex, this.betValue, this.fund,
                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                this.boxValues, true,maxiumWin, true, ratio);

        totalPrizes += slot11IconWildLienTucTableInfo.money * this.betValue;
        this.fund -= totalPrizes;
        if (slot11IconWildLienTucTableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                totalPrizes += this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT;
                this.fundJackPot -= this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                tienThuongX2 = this.pot * (GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT - 1);
            } else {
                totalPrizes += this.pot;
                this.fundJackPot -= this.initPotValue;
            }
        }
        if (slot11IconWildLienTucTableInfo.miniGame > 0) {
            totalPrizes += slot11IconWildLienTucTableInfo.miniGameSlotResponse.getTotalPrize();
            this.fundMinigame -= slot11IconWildLienTucTableInfo.miniGameSlotResponse.getTotalPrize();
        }
        if (slot11IconWildLienTucTableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                result = 4;
                this.noHuX2();
                //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "GS");
            } else {
                result = 3;
            }
            this.resetPot();
        } else {
            if (slot11IconWildLienTucTableInfo.miniGame > 0) {
                result = 5;
            } else {
                result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
            }
        }
        long moneyExchange = totalPrizes - tienThuongX2;
        if (tienThuongX2 > 0L && !u.isBot()) {
            moneyRes = this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName,
                    this.gameID, SlotDescriptionUtils.getMultiJackpotDescription(this.gameID),
                    0L, (Long) null, TransType.NO_VIPPOINT);
            currentMoney = moneyRes.getCurrentMoney();
        }

        if (totalPrizes != 0 && !u.isBot()) {
            if ((moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, this.gameName,
                    this.gameID, SlotDescriptionUtils.getPayDescription(this.gameID, totalBetValue, totalPrizes, result),
                    0L, Long.valueOf(referenceId), TransType.END_TRANS)) != null && moneyRes.isSuccess()) {
                currentMoney = moneyRes.getCurrentMoney();
                if (this.moneyType == 1 && moneyExchange >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
                    this.broadcastMsgService.putMessage(Games.THAN_DEN.getId(), username, moneyExchange - totalBetValue);
                }
            }
        }

        String linesWin = slot11IconWildLienTucTableInfo.lineWinToString();
        String prizesOnLine = slot11IconWildLienTucTableInfo.moneyWinToString();
        countFreeSpin = slot11IconWildLienTucTableInfo.freeSpin;
        msg.referenceId = referenceId;
        msg.matrix = slot11IconWildLienTucTableInfo.matrixToString();
        msg.linesWin = linesWin;
        msg.prize = totalPrizes;
        msg.haiSao = "";
        if (slot11IconWildLienTucTableInfo.miniGame > 0) {
            msg.haiSao = slot11IconWildLienTucTableInfo.miniGameSlotResponse.getPrizes();
        }
        if (countFreeSpin > 0) {
            msg.freeSpin = 1;
            msg.ratio = (byte) countFreeSpin;
            msg.currentNumberFreeSpin = (byte) (countFreeSpin + ratio - 1);
            this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, username, linesStr, countFreeSpin, slot11IconWildLienTucTableInfo.freeSpinRatio);
        } else {
            msg.freeSpin = 0;
            msg.ratio = 0;
            msg.currentNumberFreeSpin = (byte) (Math.max(ratio - 1,0)) ;
        }
        try {
            if (!u.isBot()) {
                this.slotService.logThanDen(referenceId, username, this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);

            }
            if (result == 3 || result == 4) {
                this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, result);
            }
            if (result == 3 || result == 2 || result == 4) {
                this.broadcastBigWin(username, (byte) result, totalPrizes);
            }
        } catch (InterruptedException bigWinMsg) {
        } catch (TimeoutException bigWinMsg) {
        } catch (IOException bigWinMsg) {
            // empty catch block
        }
        this.saveFund();
        this.savePot();

        msg.result = (byte) result;
        msg.currentMoney = currentMoney;
        long endTime = System.currentTimeMillis();
        long handleTime = endTime - startTime;
        String ratioTime = CommonUtils.getRatioTime((long) handleTime);
        Debug.trace("Free Spin ", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

    private int setFreeSpin(String nickName, String lines, int countFreeSpin) {
        int soLuot = 0;
        switch (countFreeSpin) {
            case 3: {
                soLuot = 8;
                this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, nickName, lines, soLuot, 1);
                break;
            }
            case 4: {
                soLuot = 8;
                this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, nickName, lines, soLuot, 2);
                break;
            }
            case 5: {
                soLuot = 8;
                this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, nickName, lines, soLuot, 3);
            }
        }
        return soLuot;
    }

    public short play(User user, String linesStr) throws Exception {
        String username = user.getName();
        int numFree = 0;
        if (user.getProperty("numFreeDaily") != null) {
            numFree = (Integer) user.getProperty("numFreeDaily");
        }
        ResultThanDenMsg msg = null;
        ThanDenFreeDailyMsg freeDailyMsg = new ThanDenFreeDailyMsg();
        freeDailyMsg.remain = 0;
        if (numFree > 0) {
            msg = this.playFreeDaily(username);
            freeDailyMsg.remain = (byte) (--numFree);
            if (numFree > 0) {
                user.setProperty("numFreeDaily", numFree);
            } else {
                user.removeProperty("numFreeDaily");
            }
            this.slotService.updateLuotQuayFreeDaily(this.gameName, username, this.betValue);
        } else {
            msg = this.play(username, linesStr);
        }
        if (this.isUserMinimize(user)) {
            MinimizeResultThanDenMsg miniMsg = new MinimizeResultThanDenMsg();
            miniMsg.prize = msg.prize;
            miniMsg.curretMoney = msg.currentMoney;
            miniMsg.result = msg.result;
            SlotUtils.sendMessageToUser((BaseMsg) miniMsg, user);
        } else {
            SlotUtils.sendMessageToUser((BaseMsg) msg, user);
            SlotUtils.sendMessageToUser((BaseMsg) freeDailyMsg, user);
        }
        return msg.result;
    }

    private void saveFund() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdateFundToRoom >= 60000L) {
            try {
                this.mgService.saveFund(this.name, this.fund);
                this.mgService.saveFund(this.fundMinigameName, this.fundMinigame);
                this.mgService.saveFund(this.fundJackPotName, this.fundJackPot);
            } catch (IOException | InterruptedException | TimeoutException ex2) {
                Exception ex;
                Exception e = ex = ex2;
                Debug.trace((Object[]) new Object[]{String.valueOf(this.gameName) + ": update fund error ", e.getMessage()});
            }
            this.lastTimeUpdateFundToRoom = currentTime;
        }
    }

    private void savePot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdatePotToRoom >= 3000L) {
            this.lastTimeUpdatePotToRoom = currentTime;
            try {
                this.mgService.savePot(this.name, this.pot, this.isMultiJackpot());
            } catch (IOException | InterruptedException | TimeoutException ex2) {
                Exception ex;
                Exception e = ex = ex2;
                Debug.trace((Object[]) new Object[]{String.valueOf(this.gameName) + ": update pot error ", e.getMessage()});
            }
            UpdatePotThanDenMsg msg = new UpdatePotThanDenMsg();
            msg.value = this.pot;
            msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
            this.sendMessageToRoom(msg);
        }
    }

    public void updatePot(User user) {
        UpdatePotThanDenMsg msg = new UpdatePotThanDenMsg();
        msg.value = this.pot;
        msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
        SlotUtils.sendMessageToUser((BaseMsg) msg, user);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void gameLoop() {
        ArrayList<AutoUser> usersPlay = new ArrayList<AutoUser>();
        Map map = this.usersAuto;
        synchronized (map) {
            for (Object obj : this.usersAuto.values()) {
                AutoUser user = (AutoUser) obj;
                boolean play = user.incCount();
                if (!play) continue;
                usersPlay.add(user);
            }
        }
        int numThreads = usersPlay.size() / 100 + 1;
        for (int i = 1; i <= numThreads; ++i) {
            int fromIndex = (i - 1) * 100;
            int toIndex = i * 100;
            if (toIndex > usersPlay.size()) {
                toIndex = usersPlay.size();
            }
            ArrayList<AutoUser> tmp = new ArrayList<AutoUser>(usersPlay.subList(fromIndex, toIndex));
            PlayListAutoUserTask task = new PlayListAutoUserTask(tmp);
//            PlayListAutoUserTask task = new PlayListAutoUserTask(this, tmp);
            this.executor.execute(task);
        }
        usersPlay.clear();
    }

    protected void checkResetPot() {
        try {

            int isReset = sv.getValueInt("reset_pot_" + this.gn + "_" + this.betValue);
            if (isReset == 1) {
                this.pot = this.initPotValue;
                this.fund = 0;
                this.savePot();
                this.saveFund();
                this.sv.removeKey("reset_pot_" + this.gn + "_" + this.betValue);

            }
        } catch (Exception e) {

        }
    }

    public boolean isBot(String nickName) {
        try {
            UserCacheModel u = this.userService.getUser(nickName);
            return u.isBot();
        } catch (Exception e) {
            return true;
        }
    }

    protected void playListAuto(List users) {
        for (Object obj : users) {
            AutoUser user = (AutoUser) obj;
            try {
                short result = this.play(user.getUser(), user.getLines());
                if (result == 3 || result == 4 || result == 101 || result == 102 || result == 100) {
                    this.forceStopAutoPlay(user.getUser());
                    continue;
                }
                if (result == 0) {
                    user.setMaxCount(5);
                    continue;
                }
                if (result == 5) {
                    user.setMaxCount(20);
                    continue;
                }
                user.setMaxCount(8);
            } catch (Exception ex) {
                Logger.getLogger(ThanDenRoom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        users.clear();
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        SlotFreeDaily model = this.slotService.getLuotQuayFreeDaily(this.gameName, user.getName(), this.betValue);
        ThanDenFreeDailyMsg freeDailyMsg = new ThanDenFreeDailyMsg();
        if (model != null && model.getRotateFree() > 0) {
            user.setProperty((Object) "numFreeDaily", (Object) model.getRotateFree());
            freeDailyMsg.remain = (byte) model.getRotateFree();
        } else {
            user.removeProperty((Object) "numFreeDaily");
        }
        SlotUtils.sendMessageToUser((BaseMsg) freeDailyMsg, user);
        if (result) {
            user.setProperty(("MGROOM_" + this.gameName + "_INFO"), this);
        }
        return result;
    }

    /**
     * Cleanup thread pool when room is destroyed or recycled
     */
    public void cleanup() {
        if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.shutdown();
            try {
                if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                    if (!this.executor.awaitTermination(2, TimeUnit.SECONDS)) {
                        org.apache.log4j.Logger.getLogger("slot").warn("Thread pool did not terminate for room: " + this.name);
                    }
                }
            } catch (InterruptedException e) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

