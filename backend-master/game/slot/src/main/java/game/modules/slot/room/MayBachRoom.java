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
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.SlotFreeDaily
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
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
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.SlotFreeDaily;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.GameConfig.GameConfig;
import game.modules.Slot9Icon.Slot9IconTableInfo;
import game.modules.Slot9Icon.Slot9IconUtil;
import game.modules.SlotDescription.SlotDescriptionUtils;
import game.modules.SlotUtils.GiftType;
import game.modules.slot.MayBachModule;
import game.modules.slot.cmd.send.maybach.*;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.entities.slot.AwardsOnLine;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.PickStarGift;
import game.modules.slot.entities.slot.PickStarGiftItem;
import game.modules.slot.entities.slot.PickStarGifts;
import game.modules.slot.entities.slot.ndv.NDVAward;
import game.modules.slot.entities.slot.ndv.NDVLines;
import game.modules.slot.utils.SlotHouseEdge;
import game.modules.slot.utils.SlotUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

public class MayBachRoom
        extends SlotRoom {
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable checkResetPotTask = new CheckResetPot();
    private NDVLines lines = new NDVLines();
    private long lastTimeUpdatePotToRoom = 0L;
    private long lastTimeUpdateFundToRoom = 0L;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8); // Reduced from 10 to 8
    private List<Integer> boxValues = new ArrayList<Integer>();
    private String gn;
    private int countNoHu = 0;
    private static final org.apache.log4j.Logger logger = Logger.getLogger((String) "slot");

    public MayBachRoom(MayBachModule module, byte id, String name, short moneyType, long pot, long fund, int betValue,
                       long initPotValue, long fundJackPot, String fundJackPotName, long fundMinigame, String fundMinigameName) {
        super(id, name, betValue, moneyType, pot, fund, initPotValue);
        this.module = module;
        this.moneyType = moneyType;
        this.gameName = Games.MAYBACH.getName();
        this.gameID = Games.MAYBACH.getId() + "";
        this.pot = pot;
        CacheServiceImpl cacheService = new CacheServiceImpl();
        cacheService.setValue(name, pot);
        this.fund = fund;
        this.betValue = betValue;
        this.initPotValue = initPotValue;
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        this.boxValues.add(10);
        this.boxValues.add(10);
        this.boxValues.add(10);
        this.boxValues.add(15);
        this.boxValues.add(20);
        gn = this.gameName;
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.checkResetPotTask, 10, 10, TimeUnit.SECONDS);

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
            ForceStopAutoPlayMaybachMsg msg = new ForceStopAutoPlayMaybachMsg();
            SlotUtils.sendMessageToUser((BaseMsg) msg, user);
        }
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
            totalPrizes = this.pot * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT;
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
        BigWinMaybachMsg bigWinMsg = new BigWinMaybachMsg();
        bigWinMsg.username = username;
        bigWinMsg.type = (byte) result;
        bigWinMsg.betValue = (short) this.betValue;
        bigWinMsg.totalPrizes = totalPrizes;
        bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
        this.module.sendMsgToAllUsers(bigWinMsg);
    }

    public synchronized ResultMaybachMsg play(String username, String linesStr) {
        boolean forceJackpotByUser = false;
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        long referenceId = this.module.getNewReferenceId();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long moneyAvailable = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        UserCacheModel u = this.userService.getUser(username);
        long totalBetValue = lineArr.length * this.betValue;
        ResultMaybachMsg msg = new ResultMaybachMsg();

        //get force user jackpot
        CacheServiceImpl cacheService = new CacheServiceImpl();
        String userForce = "";
        try {
            userForce = cacheService.getValueStr(CACHE_NAME_USER_SPOT + this.gn);
        } catch (Exception e) {
            userForce = "";
        }

        long fee = totalBetValue * GameConfig.getInstance().slot9IconConfig.FEE / 100;
        if (lineArr.length > 0 && !linesStr.isEmpty()) {
            if (totalBetValue > 0L) {
                if (totalBetValue <= moneyAvailable) {
                    MoneyResponse moneyRes = new MoneyResponse(false, "1001");
                    if (!u.isBot()) {
                        moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName,
                                this.gameID, SlotDescriptionUtils.getBetDescription(this.gameID),
                                fee, Long.valueOf(referenceId), TransType.START_TRANS);
                        DailyQuestUtils.playerPlayGame(username,Games.MAYBACH.getId(),totalBetValue);
                    } else {
                        moneyRes.setSuccess(true);
                    }
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        currentMoney = moneyRes.getCurrentMoney();
                        long moneyToPot = totalBetValue * GameConfig.getInstance().slot9IconConfig.RATE_TO_JACKPOT / 100;
//                        long moneyToFundJackpot = totalBetValue * GameConfig.getInstance().slot9IconConfig.RATE_TO_FUND_JACKPOT / 100;
                        long moneyToFundJackpot = totalBetValue * (GameConfig.getInstance().slot9IconConfig.RATE_TO_FUND_JACKPOT + GameConfig.getInstance().slot9IconConfig.RATE_TO_JACKPOT) / 100;
                        long moneyToFundMinigame = totalBetValue * GameConfig.getInstance().slot9IconConfig.RATE_TO_FUND_MINIGAME / 100;
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
                        long userValue = this.userService.getUserValue(username);
                        long maxiumWin = MaxiumWinConfig.getMaxiumMoneyWin(userValue) - moneyRes.getCurrentMoney();
                        Debug.trace(" get user value maxiumWin  ", userValue, maxiumWin);

                        Slot9IconTableInfo slot9IconTableInfo = Slot9IconUtil.getSlot9IconTableInfo(rowIndex, this.betValue, this.fund,
                                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                this.boxValues, false, maxiumWin);

                        if(this.isUserBigWin(username)){
                            slot9IconTableInfo = Slot9IconUtil.getSlot9IconBigWinTableInfo(rowIndex, this.betValue, this.fund,
                                    this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                    this.boxValues, false, maxiumWin);
                        }

                        if (this.isUserJackpot(username)) {
                            slot9IconTableInfo = new Slot9IconTableInfo(GiftType.JACKPOT, this.betValue,
                                    Slot9IconUtil.getMoneyJackPot(this.pot, this.isMultiJackpot(), this.initPotValue), this.boxValues);
                            slot9IconTableInfo.calculate(rowIndex);
                        }
//                        slot9IconTableInfo = new Slot9IconTableInfo(GiftType.JACKPOT, this.betValue,
//                                this.pot, this.boxValues);
//                        slot9IconTableInfo.calculate(rowIndex);

                        totalPrizes += slot9IconTableInfo.money * this.betValue;

                        if(!isUserBigWin) {
                            // Phase 3c — pct-aware force-lose (SLOT_USE_DYNAMIC_RTP flag)
                            totalPrizes = game.modules.slot.utils.SlotHouseEdge.maybeForceLose(totalPrizes, result, username, "slot_maybach");
                            this.fund -= totalPrizes;
                        }
                        if (slot9IconTableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                totalPrizes += this.pot * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT;
                                if(!isUserBigWin) {
//                                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT;
                                }
                                tienThuongX2 = this.pot * (GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT - 1);
                            } else {
                                totalPrizes += this.pot;
                                if(!isUserBigWin) {
//                                    this.fundJackPot -= this.initPotValue;
                                    this.fundJackPot -= this.pot;
                                }
                            }
                        }

                        if (slot9IconTableInfo.miniGame > 0) {
                            totalPrizes += slot9IconTableInfo.miniGameSlotResponse.getTotalPrize();
                            if(!isUserBigWin) {
                                this.fundMinigame -= slot9IconTableInfo.miniGameSlotResponse.getTotalPrize();
                            }
                        }

                        if (slot9IconTableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                result = 4;
                                this.noHuX2();
                                //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "GS");
                            } else {
                                result = 3;
                            }
                            this.resetPot();
                        } else {
                            if (slot9IconTableInfo.miniGame > 0) {
                                result = 5;
                            } else {
                                result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
                            }
                        }

                        // [SUN-589 Phase 3c] RTP Engine — zero prize if force-lose fires (jackpot exempt above)
                        if (result != 3 && result != 4 && totalPrizes > 0
                                && game.modules.slot.utils.SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_maybach")) {
                            this.fund += totalPrizes; // return prize to fund
                            totalPrizes = 0;
                            result = 1;
                        }
                        long moneyExchange = totalPrizes - tienThuongX2;
                        String des = "Quay " + gn;
                        if (tienThuongX2 > 0L && !u.isBot()) {
                            moneyRes = this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName,
                                    this.gameID, SlotDescriptionUtils.getMultiJackpotDescription(this.gameID),
                                    0L, (Long) null, TransType.NO_VIPPOINT);
                            currentMoney = moneyRes.getCurrentMoney();
                        }
                        if (moneyExchange != 0 && !u.isBot()) {
                            if ((moneyRes = this.userService.updateMoney(username, moneyExchange, this.moneyTypeStr, this.gameName,
                                    this.gameID, SlotDescriptionUtils.getPayDescription(this.gameID, totalBetValue, totalPrizes, result),
                                    0L, Long.valueOf(referenceId), TransType.END_TRANS)) != null && moneyRes.isSuccess()) {
                                currentMoney = moneyRes.getCurrentMoney();
                                if (this.moneyType == 1 && moneyExchange - totalBetValue >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
                                    this.broadcastMsgService.putMessage(Games.MAYBACH.getId(), username, moneyExchange - totalBetValue);
                                }
                            }
                        }

                        String linesWin = slot9IconTableInfo.lineWinToString();
                        String prizesOnLine = slot9IconTableInfo.moneyWinToString();
                        msg.referenceId = referenceId;
                        msg.matrix = slot9IconTableInfo.matrixToString();
                        msg.linesWin = linesWin;
                        msg.prize = totalPrizes;
                        msg.haiSao = "";
                        if (slot9IconTableInfo.miniGame > 0) {
                            msg.haiSao = slot9IconTableInfo.miniGameSlotResponse.getPrizes();
                        }
                        try {
                            if (!u.isBot()) {
                                this.slotService.logSlot(this.gameName, referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);

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
                    } else {
                        result = 102;
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
        SlotUtils.logNuDiepVien(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);

        Debug.trace("Normal Spin ", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

    public synchronized ResultMaybachMsg playFreeDaily(String username) {
        String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        long referenceId = this.module.getNewReferenceId();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        ResultMaybachMsg msg = new ResultMaybachMsg();
        boolean enoughPair = false;
        ArrayList<AwardsOnLine> awardsOnLines = new ArrayList<AwardsOnLine>();
        UserCacheModel u = this.userService.getUser(username);
        MoneyResponse moneyRes = new MoneyResponse(false, "1001");
        long totalBetValue = 0;
        int[] rowIndex = new int[lineArr.length];
        for (int i = 0; i < lineArr.length; i++) {
            rowIndex[i] = Integer.parseInt(lineArr[i]);
        }

        long totalPrizes = 0;
        long tienThuongX2 = 0;

        long userValue = this.userService.getUserValue(username);
        long maxiumWin = MaxiumWinConfig.getMaxiumMoneyWin(userValue) - moneyRes.getCurrentMoney();
        Debug.trace(" get user value maxiumWin  ", userValue, maxiumWin);

        Slot9IconTableInfo slot9IconTableInfo = Slot9IconUtil.getSlot9IconTableInfo(rowIndex, this.betValue, this.fund,
                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                this.boxValues, true, maxiumWin);
        totalPrizes += slot9IconTableInfo.money * this.betValue;
        this.fund -= totalPrizes;

        if (slot9IconTableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                totalPrizes += this.pot * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT;
//                this.fundJackPot -= this.pot * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                this.fundJackPot -= this.pot * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT;
                tienThuongX2 = this.pot * (GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT - 1);
            } else {
                totalPrizes += this.pot;
//                this.fundJackPot -= this.initPotValue;
                this.fundJackPot -= this.pot;
            }
        }

        if (slot9IconTableInfo.miniGame > 0) {
            totalPrizes += slot9IconTableInfo.miniGameSlotResponse.getTotalPrize();
            this.fundMinigame -= slot9IconTableInfo.miniGameSlotResponse.getTotalPrize();
        }

        if (slot9IconTableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                result = 4;
                this.noHuX2();
                //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "GS");
            } else {
                result = 3;
            }
            this.resetPot();
        } else {
            if (slot9IconTableInfo.miniGame > 0) {
                result = 5;
            } else {
                result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
            }
        }
        long moneyExchange = totalPrizes - tienThuongX2;
        String des = gn + " - Free";
        if (tienThuongX2 > 0L && !u.isBot()) {
            moneyRes = this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName,
                    this.gameID, SlotDescriptionUtils.getMultiJackpotDescription(this.gameID),
                    0L, (Long) null, TransType.NO_VIPPOINT);
            currentMoney = moneyRes.getCurrentMoney();
        }
        if (moneyExchange != 0 && !u.isBot()) {
            if ((moneyRes = this.userService.updateMoney(username, moneyExchange, this.moneyTypeStr, this.gameName,
                    this.gameID, SlotDescriptionUtils.getPayDescription(this.gameID,totalBetValue, totalPrizes, result),
                    0L, Long.valueOf(referenceId), TransType.END_TRANS)) != null && moneyRes.isSuccess()) {
                currentMoney = moneyRes.getCurrentMoney();
                if (this.moneyType == 1 && moneyExchange - totalBetValue >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
                    this.broadcastMsgService.putMessage(Games.MAYBACH.getId(), username, moneyExchange - totalBetValue);
                }
            }
        }

        String linesWin = slot9IconTableInfo.lineWinToString();
        String prizesOnLine = slot9IconTableInfo.moneyWinToString();
        msg.referenceId = referenceId;
        msg.matrix = slot9IconTableInfo.matrixToString();
        msg.linesWin = linesWin;
        msg.prize = totalPrizes;
        msg.haiSao = "";
        if (slot9IconTableInfo.miniGame > 0) {
            msg.haiSao = slot9IconTableInfo.miniGameSlotResponse.getPrizes();
        }
        try {
            if (!u.isBot()) {
                this.slotService.logSlot(this.gameName, referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);

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
        //SlotUtils.logNuDiepVien(refernceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
        return msg;
    }

    private MiniGameSlotResponse generatePickStars() {
        MiniGameSlotResponse response = new MiniGameSlotResponse();
        int totalMoney = 0;
        ArrayList<PickStarGift> gifts = new ArrayList<PickStarGift>();
        PickStarGifts pickStarGifts = new PickStarGifts();
        String responsePickStars = "";
        int totalKeys = 1;
        block5:
        for (int numPicks = 10; numPicks > 0; --numPicks) {
            PickStarGiftItem gift = pickStarGifts.pickRandomAndRandomGift();
            switch (gift) {
                case GOLD: {
                    totalMoney += 4 * this.betValue * totalKeys;
                    gifts.add(new PickStarGift(PickStarGiftItem.GOLD, 0));
                    continue block5;
                }
                case KEY: {
                    ++numPicks;
                    ++totalKeys;
                    gifts.add(new PickStarGift(PickStarGiftItem.KEY, 0));
                    continue block5;
                }
                case BOX: {
                    int boxValue = this.randomBoxValue();
                    totalMoney += boxValue * this.betValue * totalKeys;
                    gifts.add(new PickStarGift(PickStarGiftItem.BOX, boxValue));
                    break;
                }
            }
        }
        for (PickStarGift pickStarGift : gifts) {
            if (responsePickStars.length() == 0) {
                responsePickStars = pickStarGift.getName();
                continue;
            }
            responsePickStars = String.valueOf(responsePickStars) + "," + pickStarGift.getName();
        }
        response.setTotalPrize(totalMoney);
        response.setPrizes(responsePickStars);
        return response;
    }

    private int randomBoxValue() {
        Random rd = new Random();
        int n = rd.nextInt(this.boxValues.size());
        return this.boxValues.get(n);
    }

    public short play(User user, String linesStr) {
        String username = user.getName();
        int numFree = 0;
        if (user.getProperty((Object) "numFreeDaily") != null) {
            numFree = (Integer) user.getProperty((Object) "numFreeDaily");
        }
        ResultMaybachMsg msg = null;
        MaybachFreeDailyMsg freeDailyMsg = new MaybachFreeDailyMsg();
        if (numFree > 0) {
            msg = this.playFreeDaily(username);
            freeDailyMsg.remain = (byte) (--numFree);
            if (numFree > 0) {
                user.setProperty((Object) "numFreeDaily", (Object) numFree);
            } else {
                user.removeProperty((Object) "numFreeDaily");
            }
            this.slotService.updateLuotQuayFreeDaily(this.gameName, username, this.betValue);
        } else {
            msg = this.play(username, linesStr);
        }
        if (this.isUserMinimize(user)) {
            MinimizeResultMaybachMsg miniMsg = new MinimizeResultMaybachMsg();
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
                Debug.trace((Object[]) new Object[]{this.gameName + ": update fund error ", e.getMessage()});
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
                Debug.trace((Object[]) new Object[]{this.gameName + ": update pot error ", e.getMessage()});
            }
            UpdatePotMaybachMsg msg = new UpdatePotMaybachMsg();
            msg.value = this.pot;
            msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
            this.sendMessageToRoom(msg);
        }
    }

    public void updatePot(User user) {
        UpdatePotMaybachMsg msg = new UpdatePotMaybachMsg();
        msg.value = this.pot;
        msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
        SlotUtils.sendMessageToUser((BaseMsg) msg, user);
    }

    private boolean checkDieuKienNo(String username) {
        try {
            UserModel u = this.userService.getUserByUserName(username);
            return u.isBot();
        } catch (Exception u) {
            return false;
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void gameLoop() {
        ArrayList<AutoUser> usersPlay = new ArrayList<AutoUser>();
        Map map = this.usersAuto;
        synchronized (map) {
            for (AutoUser user : this.usersAuto.values()) {
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
            short result = this.play(user.getUser(), user.getLines());
            if (result == 3 || result == 4 || result == 101 || result == 102 || result == 100) {
                this.forceStopAutoPlay(user.getUser());
                continue;
            }
            if (result == 0) {
                user.setMaxCount(4);
                continue;
            }
            if (result == 5) {
                user.setMaxCount(15);
                continue;
            }
            user.setMaxCount(8);
        }
        users.clear();
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        SlotFreeDaily model = this.slotService.getLuotQuayFreeDaily(this.gameName, user.getName(), this.betValue);
        if (model != null && model.getRotateFree() > 0) {
            user.setProperty((Object) "numFreeDaily", (Object) model.getRotateFree());
            MaybachFreeDailyMsg freeDailyMsg = new MaybachFreeDailyMsg();
            freeDailyMsg.remain = (byte) model.getRotateFree();
            SlotUtils.sendMessageToUser((BaseMsg) freeDailyMsg, user);
        } else {
            user.removeProperty((Object) "numFreeDaily");
        }
        if (result) {
            user.setProperty((Object) "MGROOM_" + this.gameName + "_INFO", (Object) this);
        }
        return result;
    }

    /**
     * Cleanup thread pool when room is destroyed or recycled
     */
    @Override
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
        super.cleanup();
    }

}

