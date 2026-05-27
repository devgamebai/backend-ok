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
import bitzero.util.ExtensionUtility;
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
import game.modules.Slot7IconWild.Slot7IconWildTableInfo;
import game.modules.Slot7IconWild.Slot7IconWildUtil;
import game.modules.SlotDescription.SlotDescriptionUtils;
import game.modules.SlotUtils.GiftType;
import game.modules.slot.AuditionModule;

import game.modules.slot.cmd.send.audition.*;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.PickStarGift;
import game.modules.slot.entities.slot.PickStarGiftItem;
import game.modules.slot.entities.slot.PickStarGifts;
import game.modules.slot.entities.slot.khobau.KhoBauLines;
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

public class AuditionRoom
        extends SlotRoom {
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable checkResetPotTask = new CheckResetPot();
    //    private final Runnable gameLoopTask = new GameLoopTask(this);
    private KhoBauLines lines = new KhoBauLines();
    private long lastTimeUpdatePotToRoom = 0L;
    private long lastTimeUpdateFundToRoom = 0L;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8); // Reduced from 10 to 8
    private List<Integer> boxValues = new ArrayList<Integer>();
    private String gn;
    private int countNoHu = 0;
    private static final org.apache.log4j.Logger logger = Logger.getLogger("slot");

    public AuditionRoom(AuditionModule module, byte id, String name, short moneyType, long pot, long fund, int betValue,
                        long initPotValue, long fundJackPot, String fundJackPotName, long fundMinigame, String fundMinigameName) {
        super(id, name, betValue, moneyType, pot, fund, initPotValue);
        this.gameName = Games.AUDITION.getName();
        this.gameID = Games.AUDITION.getId() + "";
        this.module = module;
        this.moneyTypeStr = this.moneyType == 1 ? "vin" : "xu";
        CacheServiceImpl cacheService = new CacheServiceImpl();
        cacheService.setValue(name, pot);
        this.betValue = betValue;
        this.initPotValue = initPotValue;
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        this.boxValues.add(10);
        this.boxValues.add(10);
        this.boxValues.add(10);
        this.boxValues.add(15);
        this.boxValues.add(20);
        gn = Games.AUDITION.name();
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
            ForceStopAutoPlayAuditionMsg msg = new ForceStopAutoPlayAuditionMsg();
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
            totalPrizes = this.pot * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT;
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
        BigWinAuditionMsg bigWinMsg = new BigWinAuditionMsg();
        bigWinMsg.username = username;
        bigWinMsg.type = (byte) result;
        bigWinMsg.betValue = (short) this.betValue;
        bigWinMsg.totalPrizes = totalPrizes;
        bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
        this.module.sendMsgToAllUsers(bigWinMsg);
    }

    public synchronized ResultSlotAuditionMsg playNormal(String username, String linesStr, long referenceId) {
        return this.play(username, linesStr);
    }

    public synchronized ResultSlotAuditionMsg play(String username, String linesStr) {
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
        ResultSlotAuditionMsg msg = new ResultSlotAuditionMsg();
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
                    long fee = totalBetValue * GameConfig.getInstance().slot7IconWildConfig.FEE / 100;
                    Debug.trace(" Fee = " +  fee, " slot7IconWildConfig.FEE = " + GameConfig.getInstance().slot7IconWildConfig.FEE);
                    MoneyResponse moneyRes = new MoneyResponse(false, "1001");
                    if (!u.isBot()) {
                        moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName,
                                this.gameID, SlotDescriptionUtils.getBetDescription(this.gameID),
                                fee, Long.valueOf(referenceId), TransType.START_TRANS);
                        DailyQuestUtils.playerPlayGame(username,Games.AUDITION.getId(),totalBetValue);
                    } else {
                        moneyRes.setSuccess(true);
                    }
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        currentMoney = moneyRes.getCurrentMoney();
                        long moneyToPot = totalBetValue * GameConfig.getInstance().slot7IconWildConfig.RATE_TO_JACKPOT / 100;
//                        long moneyToFundJackpot = totalBetValue * GameConfig.getInstance().slot7IconWildConfig.RATE_TO_FUND_JACKPOT / 100;
                        long moneyToFundJackpot = totalBetValue * (GameConfig.getInstance().slot7IconWildConfig.RATE_TO_FUND_JACKPOT + GameConfig.getInstance().slot7IconWildConfig.RATE_TO_JACKPOT) / 100;
                        long moneyToFundMinigame = totalBetValue * GameConfig.getInstance().slot7IconWildConfig.RATE_TO_FUND_MINIGAME / 100;
//                        long moneyToFund = totalBetValue - fee - moneyToPot - moneyToFundJackpot - moneyToFundMinigame;
                        long moneyToFund = totalBetValue - fee - moneyToFundJackpot - moneyToFundMinigame;
                        Debug.trace(" Money cal  ", totalBetValue, moneyToPot, moneyToFundJackpot, moneyToFundMinigame, fee, moneyToFund);

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

                        Slot7IconWildTableInfo slot7IconWildTableInfo = Slot7IconWildUtil.getSlot7IconTableInfo(rowIndex, this.betValue, this.fund,
                                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                this.boxValues, false, maxiumWin);

                        if(isUserBigWin){
                            slot7IconWildTableInfo = Slot7IconWildUtil.getSlot7IconWildBigWinTableInfo(rowIndex, this.betValue, this.fund,
                                    this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                    this.boxValues, false, maxiumWin);

                        }
                        if (this.isUserJackpot(username)) {
                            slot7IconWildTableInfo = new Slot7IconWildTableInfo(GiftType.JACKPOT, this.betValue,
                                    Slot7IconWildUtil.getMoneyJackPot(this.pot, this.isMultiJackpot(), this.initPotValue),
                                    this.boxValues);
                            slot7IconWildTableInfo.calculate(rowIndex);
                        }
//                        slot7IconWildTableInfo = new Slot7IconWildTableInfo(GiftType.MINI_GAME,this.betValue,
//                                Slot7IconWildUtil.getMoneyJackPot(this.pot, this.isMultiJackpot(), this.initPotValue),
//                                this.boxValues);
//                        slot7IconWildTableInfo.calculate(rowIndex);

                        totalPrizes += slot7IconWildTableInfo.money * this.betValue;
                        if(!isUserBigWin) {
                            // Phase 3c — pct-aware force-lose (SLOT_USE_DYNAMIC_RTP flag)
                            totalPrizes = game.modules.slot.utils.SlotHouseEdge.maybeForceLose(totalPrizes, result, username, "slot_audition");
                            this.fund -= totalPrizes;
                        }

                        if (slot7IconWildTableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                totalPrizes += this.pot * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT;
                                if(!isUserBigWin) {
//                                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT;
                                }
                                tienThuongX2 = this.pot * (GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT - 1);
                            } else {
                                totalPrizes += this.pot;
                                if(!isUserBigWin) {
//                                    this.fundJackPot -= this.initPotValue;
                                    this.fundJackPot -= this.pot;
                                }
                            }
                        }

                        if (slot7IconWildTableInfo.miniGame > 0) {
                            totalPrizes += slot7IconWildTableInfo.miniGameSlotResponse.getTotalPrize();
                            if(!isUserBigWin) {
                                this.fundMinigame -= slot7IconWildTableInfo.miniGameSlotResponse.getTotalPrize();
                            }
                        }

                        if (slot7IconWildTableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                result = 4;
                                this.noHuX2();
                                //TODO
                                //SlotUtils.sendMessageToAllUser(msg);
                                //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "GS");
                            } else {
                                result = 3;
                            }
                            this.resetPot();
                        } else {
                            if (slot7IconWildTableInfo.miniGame > 0) {
                                result = 5;
                            } else {
                                result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
                            }
                        }


                        // [SUN-589 Phase 3c] RTP Engine — zero prize if force-lose fires (jackpot exempt above)
                        if (result != 3 && result != 4 && totalPrizes > 0
                                && game.modules.slot.utils.SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_audition")) {
                            this.fund += totalPrizes; // return prize to fund
                            totalPrizes = 0;
                            result = 1;
                        }
                        long moneyExchange = totalPrizes - tienThuongX2;
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
                                    this.broadcastMsgService.putMessage(Games.AUDITION.getId(), username, moneyExchange - totalBetValue);
                                }
                            }
                        }
                        String linesWin = slot7IconWildTableInfo.lineWinToString();
                        String prizesOnLine = slot7IconWildTableInfo.moneyWinToString();
                        msg.referenceId = referenceId;
                        msg.matrix = slot7IconWildTableInfo.matrixToString();
                        msg.linesWin = linesWin;
                        msg.prize = totalPrizes;
                        msg.haiSao = "";
                        if (slot7IconWildTableInfo.miniGame > 0) {
                            msg.haiSao = slot7IconWildTableInfo.miniGameSlotResponse.getPrizes();
                        }
                        try {
                            if (!u.isBot()) {
                                this.slotService.logAudition(referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
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
        SlotUtils.logKhoBau(referenceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
        Debug.trace("Normal Spin ", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

    public synchronized ResultSlotAuditionMsg playFreeDaily(String username) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        long referenceId = this.module.getNewReferenceId();
        short result = 0;
        String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
        String[] lineArr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20".split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        ResultSlotAuditionMsg msg = new ResultSlotAuditionMsg();


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

        Slot7IconWildTableInfo slot7IconWildTableInfo = Slot7IconWildUtil.getSlot7IconTableInfo(rowIndex, this.betValue, this.fund,
                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                this.boxValues, true, maxiumWin);
        totalPrizes += slot7IconWildTableInfo.money * this.betValue;
        this.fund -= totalPrizes;

        if (slot7IconWildTableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                totalPrizes += this.pot * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT;
//                this.fundJackPot -= this.pot * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                this.fundJackPot -= this.pot * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT;
                tienThuongX2 = this.pot * (GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT - 1);
            } else {
                totalPrizes += this.pot;
//                this.fundJackPot -= this.initPotValue;
                this.fundJackPot -= this.pot;
            }
        }

        if (slot7IconWildTableInfo.miniGame > 0) {
            totalPrizes += slot7IconWildTableInfo.miniGameSlotResponse.getTotalPrize();
            this.fundMinigame -= slot7IconWildTableInfo.miniGameSlotResponse.getTotalPrize();
        }

        if (slot7IconWildTableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                result = 4;
                this.noHuX2();
                //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "GS");
            } else {
                result = 3;
            }
            this.resetPot();
        } else {
            if (slot7IconWildTableInfo.miniGame > 0) {
                result = 5;
            } else {
                result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
            }
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
                    this.broadcastMsgService.putMessage(Games.AUDITION.getId(), username, moneyExchange - totalBetValue);
                }
            }
        }
        String linesWin = slot7IconWildTableInfo.lineWinToString();
        String prizesOnLine = slot7IconWildTableInfo.moneyWinToString();
        msg.referenceId = referenceId;
        msg.matrix = slot7IconWildTableInfo.matrixToString();
        msg.linesWin = linesWin;
        msg.prize = totalPrizes;
        msg.haiSao = "";
        if (slot7IconWildTableInfo.miniGame > 0) {
            msg.haiSao = slot7IconWildTableInfo.miniGameSlotResponse.getPrizes();
        }
        try {
            if (!u.isBot()) {
                this.slotService.logAudition(referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
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
        //SlotUtils.logKhoBau(refernceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
        return msg;
    }

    public boolean isBot(String nickName) {
        try {
            UserCacheModel u = this.userService.getUser(nickName);
            return u.isBot();
        } catch (Exception e) {
            return true;
        }
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
        ResultSlotAuditionMsg msg = null;
        AuditionFreeDailyMsg freeDailyMsg = new AuditionFreeDailyMsg();
        if (numFree > 0) {
            msg = this.playFreeDaily(username);
            numFree--;
            freeDailyMsg.remain = (byte) (numFree);
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
            MinimizeResultAuditionMsg miniMsg = new MinimizeResultAuditionMsg();
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
                Debug.trace((Object[]) new Object[]{"Audition: update fund audition bau error ", e.getMessage()});
            }
            this.lastTimeUpdateFundToRoom = currentTime;
        }
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

    private void resetPotFund() {
        try {
            this.fund = this.initPotValue;
            this.pot = this.initPotValue;
            this.saveFund();
            this.saveFund();
        } catch (Exception e) {
            Debug.trace(e);
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
                Debug.trace(this.gameName + ": update pot poker error ", e.getMessage());
            }
            byte x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
            ((AuditionModule) this.module).updatePot(this.id, this.pot, x2);
        }
    }

    private boolean checkDieuKienNo(String username) {
        try {
            UserModel u = this.userService.getUserByUserName(username);
            return u.isBot();
        } catch (Exception u) {
            return false;
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
            AuditionFreeDailyMsg freeDailyMsg = new AuditionFreeDailyMsg();
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

