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
import game.modules.Slot11IconWildLienTuc.Slot11IconWildLienTucTableInfo;
import game.modules.Slot11IconWildLienTuc.Slot11IconWildLienTucUtil;
import game.modules.SlotDescription.SlotDescriptionUtils;
import game.modules.SlotUtils.GiftType;
import game.modules.slot.SpartanModule;
import game.modules.slot.cmd.send.spartan.*;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.entities.slot.spartan.*;
import game.modules.slot.utils.SlotHouseEdge;
import game.modules.slot.utils.SlotUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpartanRoom
        extends SlotRoom {
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable checkResetPotTask = new CheckResetPot();
    private SpartanLines lines = new SpartanLines();
    private long lastTimeUpdatePotToRoom = 0L;
    private long lastTimeUpdateFundToRoom = 0L;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8); // Reduced from 10 to 8
    private String gn;
    private int countNoHu = 0;
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("slot");

    private List<Integer> boxValues = new ArrayList<Integer>();

    public SpartanRoom(SpartanModule module, byte id, String name, short moneyType, long pot, long fund, int betValue,
                       long initPotValue, long fundJackPot, String fundJackPotName, long fundMinigame, String fundMinigameName) {
        super(id, name, betValue, moneyType, pot, fund, initPotValue);
        this.module = module;
        this.moneyType = moneyType;
        this.gameName = Games.SPARTAN.getName();
        this.gameID = Games.SPARTAN.getId() + "";
        this.cacheFreeName = String.valueOf(this.gameName) + betValue;
        CacheServiceImpl cacheService = new CacheServiceImpl();
        cacheService.setValue(name, pot);
        this.betValue = betValue;
        this.initPotValue = initPotValue;
        gn = this.gameName;
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.checkResetPotTask, 10, 10, TimeUnit.SECONDS);
        this.boxValues.add(10);
        this.boxValues.add(10);
        this.boxValues.add(10);
        this.boxValues.add(15);
        this.boxValues.add(20);

        this.fundJackPot = fundJackPot;
        this.fundJackPotName = fundJackPotName;

        this.fundMinigame = fundMinigame;
        this.fundMinigameName = fundMinigameName;
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
            ForceStopAutoPlaySpartanMsg msg = new ForceStopAutoPlaySpartanMsg();
            SlotUtils.sendMessageToUser((BaseMsg) msg, user);
        }
    }

    public ResultSpartanMsg play(String username, String linesStr) {
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
            Debug.trace("error get free spin TamHung");
        }
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
        BigWinSpartanMsg bigWinMsg = new BigWinSpartanMsg();
        bigWinMsg.username = username;
        bigWinMsg.type = (byte) result;
        bigWinMsg.betValue = (short) this.betValue;
        bigWinMsg.totalPrizes = totalPrizes;
        bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
        this.module.sendMsgToAllUsers(bigWinMsg);
    }

    public ResultSpartanMsg playNormal(String username, String linesStr, long referenceId) {

//        Debug.trace("Spartan play");
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long moneyAvailable = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        UserCacheModel u = this.userService.getUser(username);
        long totalBetValue = lineArr.length * this.betValue;
        ResultSpartanMsg msg = new ResultSpartanMsg();

        boolean forceJackpotByUser = false;
        //get force user jackpot
        CacheServiceImpl cacheService = new CacheServiceImpl();
        String userForce = "";
        try {
            userForce = cacheService.getValueStr(CACHE_NAME_USER_SPOT + this.gn);
        } catch (Exception e) {
            userForce = "";
        }

        if (lineArr.length > 0 && !linesStr.isEmpty()) {
            if (totalBetValue > 0L) {
                if (totalBetValue <= moneyAvailable) {
                    long fee = totalBetValue * GameConfig.getInstance().slot11IconWildLienTucConfig.FEE / 100;
                    MoneyResponse moneyRes = new MoneyResponse(false, "1001");
                    if (!u.isBot()) {
                        moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName,
                                this.gameID, SlotDescriptionUtils.getBetDescription(this.gameID),
                                fee, Long.valueOf(referenceId), TransType.START_TRANS);
                        DailyQuestUtils.playerPlayGame(username,Games.SPARTAN.getId(),totalBetValue);

                    } else {
                        moneyRes.setSuccess(true);
                    }
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        currentMoney = moneyRes.getCurrentMoney();
                        long moneyToPot = totalBetValue * GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_JACKPOT / 100;
//                        long moneyToFundJackpot = totalBetValue * GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_FUND_JACKPOT / 100;
                        long moneyToFundJackpot = totalBetValue * (GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_FUND_JACKPOT + GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_JACKPOT) / 100;
                        long moneyToFundMinigame = totalBetValue * GameConfig.getInstance().slot11IconWildLienTucConfig.RATE_TO_FUND_MINIGAME / 100;
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

                        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = Slot11IconWildLienTucUtil.getSlot11IconLienTucTableInfo(rowIndex, this.betValue, this.fund,
                                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                this.boxValues, false, maxiumWin);

                        if(isUserBigWin){
                            slot11IconWildLienTucTableInfo = Slot11IconWildLienTucUtil.getSlot11IconLienTucBigWinTableInfo(rowIndex, this.betValue, this.fund,
                                    this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                    this.boxValues, false,maxiumWin);
                        }

                        if (this.isUserJackpot(username)) {
                            slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(GiftType.JACKPOT, this.betValue,
                                    Slot11IconWildLienTucUtil.getMoneyJackPot(this.pot, this.isMultiJackpot(), this.initPotValue), this.boxValues);
                            slot11IconWildLienTucTableInfo.calculate(rowIndex);
                        }
//                        slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(GiftType.FREE_SPIN, this.betValue,
//                                this.pot, this.boxValues);
//                        slot11IconWildLienTucTableInfo.calculate(rowIndex);

                        totalPrizes += slot11IconWildLienTucTableInfo.money * this.betValue;
                        if(!isUserBigWin) {
                            // Phase 3c — pct-aware force-lose (SLOT_USE_DYNAMIC_RTP flag)
                            totalPrizes = game.modules.slot.utils.SlotHouseEdge.maybeForceLose(totalPrizes, result, username, "slot_spartan");
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
                                    this.broadcastMsgService.putMessage(Games.SPARTAN.getId(), username, moneyExchange - totalBetValue);
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
                            this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, username, linesStr, countFreeSpin, countFreeSpin);
                        } else {
                            msg.freeSpin = 0;
                        }

                        try {
                            // only save real user
                            if (!u.isBot()) {
                                this.slotService.logSpartan(referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                            }
                            if (result == 3 || result == 4) {
                                this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, (int) result);
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
        SlotUtils.logSpartan(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);

        Debug.trace("Normal Spin ", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

    public ResultSpartanMsg playFreeDaily(String username) throws Exception {
        long refernceId = this.module.getNewReferenceId();
        // short result = 0;
        String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25";
        return this.playFree(username, linesStr, 0, refernceId);
    }

    public ResultSpartanMsg playFree(String username, String linesStr, int ratio, long referenceId) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long totalBetValue = 0L;
        ResultSpartanMsg msg = new ResultSpartanMsg();

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
        Debug.trace(" get user value maxiumWin  ", userValue, maxiumWin);

        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = Slot11IconWildLienTucUtil.getSlot11IconLienTucTableInfo(rowIndex, this.betValue, this.fund,
                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                this.boxValues, true, maxiumWin);

        totalPrizes += slot11IconWildLienTucTableInfo.money * this.betValue;
        this.fund -= totalPrizes;
        if (slot11IconWildLienTucTableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                totalPrizes += this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT;
//                this.fundJackPot -= this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                this.fundJackPot -= this.pot * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT;
                tienThuongX2 = this.pot * (GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT - 1);
            } else {
                totalPrizes += this.pot;
//                this.fundJackPot -= this.initPotValue;
                this.fundJackPot -= this.pot;
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
                    this.gameID, SlotDescriptionUtils.getPayDescription(this.gameID, totalBetValue,totalPrizes,result),
                    0L, Long.valueOf(referenceId), TransType.END_TRANS)) != null && moneyRes.isSuccess()) {
                currentMoney = moneyRes.getCurrentMoney();
                if (this.moneyType == 1 && moneyExchange >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
                    this.broadcastMsgService.putMessage(Games.SPARTAN.getId(), username, moneyExchange - totalBetValue);
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
            this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, username, linesStr, countFreeSpin, countFreeSpin);
        } else {
            msg.freeSpin = 0;
            msg.ratio = 0;
            msg.currentNumberFreeSpin = (byte) (Math.max(ratio - 1,0)) ;
        }

        try {
            // only save real user
            if (!u.isBot()) {
                this.slotService.logSpartan(referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
            }
            if (result == 3 || result == 4) {
                this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, (int) result);
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
        SlotUtils.logSpartan(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);

        Debug.trace("Free Spin ", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

//    public ResultSpartanMsg playFree(String username, String linesStr, int ratio, long referenceId) {
//        long startTime = System.currentTimeMillis();
//        String currentTimeStr = DateTimeUtils.getCurrentTime();
//        short result = 0;
//        String[] lineArr = linesStr.split(",");
//        long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
//        long totalBetValue = 0L;
//        ResultSpartanMsg msg = new ResultSpartanMsg();
//        long fee = 0L;
//        long moneyToPot = 0L;
//        long moneyToFund = 0L;
//        this.fund += 0L;
//        this.pot += 0L;
//        boolean enoughPair = false;
//        ArrayList<AwardsOnLine<SpartanFreeSpinAward>> awardsOnLines = new ArrayList<AwardsOnLine<SpartanFreeSpinAward>>();
//        long totalPrizes = 0L;
//        while (!enoughPair) {
//            SlotFreeSpin freeSpin;
//            result = 0;
//            awardsOnLines.clear();
//            totalPrizes = 0L;
//            String linesWin = "";
//            String prizesOnLine = "";
//            String haiSao = "";
//            SpartanItem[][] matrix = SpartanUtils.generateMatrix();
//            for (String entry : lineArr) {
//                ArrayList<SpartanFreeSpinAward> awardList = new ArrayList<SpartanFreeSpinAward>();
//                Line line = SpartanUtils.getLine(this.lines, matrix, Integer.parseInt(entry));
//                SpartanUtils.calculateFreeSpinLine(line, awardList);
//                for (SpartanFreeSpinAward award : awardList) {
//                    long moneyOnLine = 0L;
//                    if (!(award.getRatio() > 0.0f)) continue;
//                    moneyOnLine = (long) (award.getRatio() * (float) this.betValue);
//                    AwardsOnLine<SpartanFreeSpinAward> aol = new AwardsOnLine<SpartanFreeSpinAward>(award, moneyOnLine, line.getName());
//                    awardsOnLines.add(aol);
//                }
//            }
//            StringBuilder builderLinesWin = new StringBuilder();
//            StringBuilder builderPrizesOnLine = new StringBuilder();
//            for (AwardsOnLine entry2 : awardsOnLines) {
//                totalPrizes += entry2.getMoney();
//                builderLinesWin.append(",");
//                builderLinesWin.append(entry2.getLineId());
//                builderPrizesOnLine.append(",");
//                builderPrizesOnLine.append(entry2.getMoney());
//            }
//            if (builderLinesWin.length() > 0) {
//                builderLinesWin.deleteCharAt(0);
//            }
//            if (builderPrizesOnLine.length() > 0) {
//                builderPrizesOnLine.deleteCharAt(0);
//            }
//            int tmpPrizes = (int) totalPrizes;
//            if (result == 3 ? this.fund - totalPrizes < 0L : this.fund - (totalPrizes *= (long) ratio) < this.initPotValue * 2L && totalPrizes - 0L >= 0L)
//                continue;
//
// [DISABLED-MR28]             // [SUN-589 Phase 3c] RTP Engine — exempt jackpot (result==3||4)
// [DISABLED-MR28]             if (result != 3 && result != 4) {
// [DISABLED-MR28]                 if (SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_spartan")) {
// [DISABLED-MR28]                     
// [DISABLED-MR28]                                        totalPrizes = 0;
// [DISABLED-MR28]                                        awardsOnLines.clear();
// [DISABLED-MR28]                                        builderLinesWin = new StringBuilder();
// [DISABLED-MR28]                                        builderPrizesOnLine = new StringBuilder();
// [DISABLED-MR28]                                        result = 0;
// [DISABLED-MR28]                                    }
// [DISABLED-MR28]             }
// [DISABLED-MR28]             enoughPair = true;
// [DISABLED-MR28] //            if (totalPrizes > 0L) {
//                this.fund -= totalPrizes;
//                if (result == 0) {
//                    result = 1;
//                }
//            }
//            long moneyExchange = totalPrizes - 0L;
//            String des = gn + " - Free";
//            MoneyResponse moneyRes = this.userService.updateMoney(username, moneyExchange, this.moneyTypeStr, this.gameName, gn + " - Free", this.buildDescription(0L, totalPrizes, result), 0L, (Long) null, TransType.VIPPOINT);
//            if (moneyRes != null && moneyRes.isSuccess()) {
//                currentMoney = moneyRes.getCurrentMoney();
//                if (this.moneyType == 1 && moneyExchange >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
//                    this.broadcastMsgService.putMessage(Games.SPARTAN.getId(), username, moneyExchange);
//                }
//                this.slotService.addPrizes(this.cacheFreeName, username, tmpPrizes);
//            }
//            if ((freeSpin = this.slotService.updateLuotQuaySlotFree(this.cacheFreeName, username)).getNum() == 0) {
//                SpartanTotalFreeSpin totalFreeSpinMsg = new SpartanTotalFreeSpin();
//                totalFreeSpinMsg.prize = freeSpin.getPrizes();
//                totalFreeSpinMsg.ratio = (byte) ratio;
//                SlotUtils.sendMessageToUser((BaseMsg) totalFreeSpinMsg, username);
//            }
//            StringBuilder sb = new StringBuilder();
//            for (int i = 0; i < 3; ++i) {
//                for (int j = 0; j < 5; ++j) {
//                    if (matrix[i][j] != SpartanItem.WILD) continue;
//                    sb.append(String.valueOf(i) + "," + j + ",");
//                }
//            }
//            if (sb.length() > 0) {
//                sb.deleteCharAt(sb.length() - 1);
//            }
//            this.slotService.setItemsWild(this.cacheFreeName, username, sb.toString());
//            linesWin = builderLinesWin.toString();
//            prizesOnLine = builderPrizesOnLine.toString();
//            msg.referenceId = referenceId;
//            msg.matrix = SpartanUtils.matrixToString(matrix);
//            msg.linesWin = linesWin;
//            msg.prize = totalPrizes;
//            msg.haiSao = "";
//            msg.freeSpin = (byte) freeSpin.getNum();
//            msg.isFreeSpin = true;
//            msg.itemsWild = sb.toString();
//            msg.ratio = (byte) ratio;
//            try {
//                this.slotService.logSpartan(referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
//            } catch (InterruptedException i) {
//            } catch (TimeoutException i) {
//            } catch (IOException i) {
//                // empty catch block
//            }
//            this.saveFund();
//            this.savePot();
//        }
//        msg.result = (byte) result;
//        msg.currentMoney = currentMoney;
//        long endTime = System.currentTimeMillis();
//        long handleTime = endTime - startTime;
//        String ratioTime = CommonUtils.getRatioTime((long) handleTime);
//        SlotUtils.logSpartan(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
//        return msg;
//    }

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
        if (user.getProperty((Object) "numFreeDaily") != null) {
            numFree = (Integer) user.getProperty((Object) "numFreeDaily");
        }
        ResultSpartanMsg msg = null;
        SpartanFreeDailyMsg freeDailyMsg = new SpartanFreeDailyMsg();
        freeDailyMsg.remain = 0;
        if (numFree > 0) {
            Debug.trace("numFree:" + numFree);
            try {
                msg = this.playFreeDaily(username);
                freeDailyMsg.remain = (byte) (--numFree);
                if (numFree > 0) {
                    user.setProperty((Object) "numFreeDaily", (Object) numFree);
                } else {
                    user.removeProperty((Object) "numFreeDaily");
                }
                this.slotService.updateLuotQuayFreeDaily(this.gameName, username, this.betValue);
            } catch (Exception ex) {
                throw ex;
            }
        } else {
            msg = this.play(username, linesStr);
        }
        if (this.isUserMinimize(user)) {
            MinimizeResultSpartanMsg miniMsg = new MinimizeResultSpartanMsg();
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
        /*if (currentTime - this.lastTimeUpdateFundToRoom >= 60000L) {
            try {
                this.mgService.saveFund(this.name, this.fund);
            } catch (IOException | InterruptedException | TimeoutException ex2) {
                Exception ex;
                Exception e = ex = ex2;
                Debug.trace((Object[]) new Object[]{String.valueOf(this.gameName) + ": update fund error ", e.getMessage()});
            }
            this.lastTimeUpdateFundToRoom = currentTime;
        }*/
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
            UpdatePotSpartanMsg msg = new UpdatePotSpartanMsg();
            msg.value = this.pot;
            msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
            this.sendMessageToRoom(msg);
        }
    }

    public void updatePot(User user) {
        UpdatePotSpartanMsg msg = new UpdatePotSpartanMsg();
        msg.value = this.pot;
        msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
        SlotUtils.sendMessageToUser((BaseMsg) msg, user);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void gameLoop() {
        // Early exit if no auto users to optimize CPU
        if (this.usersAuto == null || this.usersAuto.isEmpty()) {
            return;
        }
        ArrayList<AutoUser> usersPlay = new ArrayList<AutoUser>();
        Map map = this.usersAuto;
        synchronized (map) {
            for (AutoUser user : this.usersAuto.values()) {
                boolean play = user.incCount();
                if (!play) continue;
                usersPlay.add(user);
            }
        }
        // Early exit if no users to play
        if (usersPlay.isEmpty()) {
            return;
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

    @Override
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

    @Override
    protected void playListAuto(List<AutoUser> users) {
        for (AutoUser user : users) {
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
                Logger.getLogger(SpartanRoom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        users.clear();
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        SlotFreeDaily model = this.slotService.getLuotQuayFreeDaily(this.gameName, user.getName(), this.betValue);
        SpartanFreeDailyMsg freeDailyMsg = new SpartanFreeDailyMsg();
        if (model != null && model.getRotateFree() > 0) {
            user.setProperty((Object) "numFreeDaily", (Object) model.getRotateFree());
            freeDailyMsg.remain = (byte) model.getRotateFree();
        } else {
            user.removeProperty((Object) "numFreeDaily");
        }
        SlotUtils.sendMessageToUser((BaseMsg) freeDailyMsg, user);
        if (result) {
            user.setProperty((Object) ("MGROOM_" + this.gameName + "_INFO"), (Object) this);
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
                // Wait for existing tasks to complete, max 5 seconds
                if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    // Force shutdown if tasks don't complete
                    this.executor.shutdownNow();
                    // Wait again for forced shutdown
                    if (!this.executor.awaitTermination(2, TimeUnit.SECONDS)) {
                        logger.warn("Thread pool did not terminate for room: " + this.name);
                    }
                }
            } catch (InterruptedException e) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

