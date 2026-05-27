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
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.GameConfig.GameConfig;
import game.modules.Slot7Icon.Slot7IconTableInfo;
import game.modules.Slot7Icon.Slot7IconUtil;
import game.modules.SlotDescription.SlotDescriptionUtils;
import game.modules.SlotUtils.GiftType;
import game.modules.slot.RollRoyModule;
import game.modules.slot.cmd.send.rollRoy.*;
import game.modules.slot.entities.slot.*;
import game.modules.slot.utils.SlotHouseEdge;
import game.modules.slot.utils.SlotUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RollRoyRoom extends SlotRoom {
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable checkResetPotTask = new CheckResetPot();
    private long lastTimeUpdatePotToRoom = 0L;
    private long lastTimeUpdateFundToRoom = 0L;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8); // Reduced from 10 to 8
    private List<Integer> boxValues = new ArrayList<Integer>();
    private String gn;
    private static final Logger logger = Logger.getLogger("slot");

    public RollRoyRoom(RollRoyModule module, byte id, String name, short moneyType, long pot, long fund, int betValue,
                       long initPotValue, long fundJackPot, String fundJackPotName, long fundMinigame, String fundMinigameName) {
        super(id, name, betValue, moneyType, pot, fund, initPotValue);
        this.gameName = Games.ROLL_ROYE.getName();
        this.gameID = Games.ROLL_ROYE.getId() + "";
        this.cacheFreeName = String.valueOf(this.gameName) + betValue;
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
        this.gn = this.gameName;
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
            ForceStopAutoPlayRollRoyMsg msg = new ForceStopAutoPlayRollRoyMsg();
            SlotUtils.sendMessageToUser((BaseMsg) msg, user);
        }
    }

    private void broadcastBigWin(String username, byte result, long totalPrizes) {
        BigWinRollRoyMsg bigWinMsg = new BigWinRollRoyMsg();
        bigWinMsg.username = username;
        bigWinMsg.type = result;
        bigWinMsg.betValue = (short) this.betValue;
        bigWinMsg.totalPrizes = totalPrizes;
        bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
        this.module.sendMsgToAllUsers(bigWinMsg);
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
            totalPrizes = this.pot * GameConfig.getInstance().slot7IconConfig.MULTI_JACKPOT;
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

    public synchronized ResultSlotMsg play(String username, String linesStr) {
//        Debug.trace("play   Rollroyce");
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
            Debug.trace("error get free spin RollRoyce");
        }
        return this.playNormal(username, linesStr, referenceId);
    }

    public synchronized ResultSlotMsg playNormal(String username, String linesStr, long referenceId) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long moneyAvailable = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        UserCacheModel u = this.userService.getUser(username);
        long totalBetValue = lineArr.length * this.betValue;
        ResultSlotMsg msg = new ResultSlotMsg();
//        boolean forceJackpotByUser = false;
//        //get force user jackpot
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
                    long fee = totalBetValue * GameConfig.getInstance().slot7IconConfig.FEE / 100;
                    MoneyResponse moneyRes = new MoneyResponse(false, "1001");
                    if (!u.isBot()) {
                        moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName,
                                this.gameID, SlotDescriptionUtils.getBetDescription(this.gameID),
                                fee, Long.valueOf(referenceId), TransType.START_TRANS);
                        //add free spint
                        DailyQuestUtils.playerPlayGame(username,Games.ROLL_ROYE.getId(),totalBetValue);
                    } else {
                        moneyRes.setSuccess(true);
                    }
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        currentMoney = moneyRes.getCurrentMoney();
                        long moneyToPot = totalBetValue * GameConfig.getInstance().slot7IconConfig.RATE_TO_JACKPOT / 100;
//                        long moneyToFundJackpot = totalBetValue * GameConfig.getInstance().slot7IconConfig.RATE_TO_FUND_JACKPOT / 100;
                        long moneyToFundJackpot = totalBetValue * (GameConfig.getInstance().slot7IconConfig.RATE_TO_FUND_JACKPOT + GameConfig.getInstance().slot7IconConfig.RATE_TO_JACKPOT) / 100;
                        long moneyToFundMinigame = totalBetValue * GameConfig.getInstance().slot7IconConfig.RATE_TO_FUND_MINIGAME / 100;
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
                        Debug.trace(" get user value maxiumWin  ", userValue, maxiumWin);

                        Slot7IconTableInfo slot7IconTableInfo = Slot7IconUtil.getSlot7IconTableInfo(rowIndex, this.betValue, this.fund,
                                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                this.boxValues, false, maxiumWin);
                        if(isUserBigWin){
                            slot7IconTableInfo = Slot7IconUtil.getSlot7IconBigWinTableInfo(rowIndex, this.betValue, this.fund,
                                    this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                    this.boxValues, false, maxiumWin);
                        }
                        if (this.isUserJackpot(username)) {
                            slot7IconTableInfo = new Slot7IconTableInfo(GiftType.JACKPOT, this.betValue,
                                    Slot7IconUtil.getMoneyJackPot(this.pot, this.isMultiJackpot(), this.initPotValue)
                                    , this.boxValues);
                            slot7IconTableInfo.calculate(rowIndex);
                        }
//                        slot7IconTableInfo = new Slot7IconTableInfo(GiftType.JACKPOT, this.betValue,
//                                this.pot, this.boxValues);
//                        slot7IconTableInfo.calculate(rowIndex);

                        totalPrizes += slot7IconTableInfo.money * this.betValue;
                        if(!isUserBigWin) {
                            // Phase 3c — pct-aware force-lose (SLOT_USE_DYNAMIC_RTP flag)
                            totalPrizes = game.modules.slot.utils.SlotHouseEdge.maybeForceLose(totalPrizes, result, username, "slot_rollroy");
                            this.fund -= totalPrizes;
                        }
                        if (slot7IconTableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                totalPrizes += this.pot * GameConfig.getInstance().slot7IconConfig.MULTI_JACKPOT;
                                if(!isUserBigWin) {
//                                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot7IconConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot7IconConfig.MULTI_JACKPOT;
                                }
                                tienThuongX2 = this.pot * (GameConfig.getInstance().slot7IconConfig.MULTI_JACKPOT - 1);
                            } else {
                                totalPrizes += this.pot;
                                if(!isUserBigWin) {
//                                    this.fundJackPot -= this.initPotValue;
                                    this.fundJackPot -= this.pot;
                                }
                            }
                        }
                        if (slot7IconTableInfo.miniGame > 0) {
                            totalPrizes += slot7IconTableInfo.miniGameSlotResponse.getTotalPrize();
                            if(!isUserBigWin) {
                                this.fundMinigame -= slot7IconTableInfo.miniGameSlotResponse.getTotalPrize();
                            }
                        }
                        if (slot7IconTableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                result = 4;
                                //this.noHuX2();
                                //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "GS");
                            } else {
                                result = 3;
                            }
                            this.resetPot();
                        } else {
                            if (slot7IconTableInfo.miniGame > 0) {
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
                                    this.broadcastMsgService.putMessage(Games.ROLL_ROYE.getId(), username, moneyExchange - totalBetValue);
                                }
                            }
                        }
                        String linesWin = slot7IconTableInfo.lineWinToString();
                        String prizesOnLine = slot7IconTableInfo.moneyWinToString();
                        countFreeSpin = slot7IconTableInfo.freeSpin;
                        msg.referenceId = referenceId;
                        msg.matrix = slot7IconTableInfo.matrixToString();
                        msg.linesWin = linesWin;
                        msg.prize = totalPrizes;
                        msg.haiSao = "";
                        if (slot7IconTableInfo.miniGame > 0) {
                            msg.haiSao = slot7IconTableInfo.miniGameSlotResponse.getPrizes();
                        }
                        if (countFreeSpin > 0) {
                            msg.isFreeSpin = 1;
                            msg.ratio = (byte) countFreeSpin;
                            msg.currentNumberFreeSpin = (byte) countFreeSpin;
                            this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, username, linesStr, countFreeSpin, countFreeSpin);
                        } else {
                            msg.isFreeSpin = 0;
                        }
                        try {
                            if (!u.isBot()) {
                                this.slotService.logRollRoye(referenceId, username, this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                            }
                            if (result == 3 || result == 4) {
                                this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, (int) result);
                            }
                            if (result == 3 || result == 2 || result == 4) {
                                this.broadcastBigWin(username, (byte) result, totalPrizes);
                            }
                        } catch (InterruptedException award) {
                        } catch (TimeoutException award) {
                        } catch (IOException award) {
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
        Debug.trace("Normal Spin ", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

    public synchronized ResultSlotMsg playFree(String username, String linesStr, int ratio, long referenceId) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long totalBetValue = 0;
        ResultSlotMsg msg = new ResultSlotMsg();
        UserCacheModel u = this.userService.getUser(username);
        if (lineArr.length > 0 && !linesStr.isEmpty()) {
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
            Slot7IconTableInfo slot7IconTableInfo = Slot7IconUtil.getSlot7IconTableInfo(rowIndex, this.betValue, this.fund,
                    this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                    this.boxValues, true, maxiumWin);

            totalPrizes += slot7IconTableInfo.money * this.betValue;
            this.fund -= totalPrizes;
            if (slot7IconTableInfo.jackpot) {
                if (this.isMultiJackpot()) {
                    totalPrizes += this.pot * GameConfig.getInstance().slot7IconConfig.MULTI_JACKPOT;
                    this.fundJackPot -= this.pot * GameConfig.getInstance().slot7IconConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                    tienThuongX2 = this.pot * (GameConfig.getInstance().slot7IconConfig.MULTI_JACKPOT - 1);
                } else {
                    totalPrizes += this.pot;
                    this.fundJackPot -= this.initPotValue;
                }
            }
            if (slot7IconTableInfo.miniGame > 0) {
                totalPrizes += slot7IconTableInfo.miniGameSlotResponse.getTotalPrize();
                this.fundMinigame -= slot7IconTableInfo.miniGameSlotResponse.getTotalPrize();
            }
            if (slot7IconTableInfo.jackpot) {
                if (this.isMultiJackpot()) {
                    result = 4;
                    this.noHuX2();
                    //GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game " + gn + " phong " + this.betValue + ". So tien no hu: " + totalPrizes + " " + "GS");
                } else {
                    result = 3;
                }
                this.resetPot();
            } else {
                if (slot7IconTableInfo.miniGame > 0) {
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
                        this.gameID, SlotDescriptionUtils.getPayDescription(this.gameID, totalBetValue, totalPrizes, result), 0L, Long.valueOf(referenceId), TransType.END_TRANS)) != null && moneyRes.isSuccess()) {
                    currentMoney = moneyRes.getCurrentMoney();
                    if (this.moneyType == 1 && moneyExchange >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
                        this.broadcastMsgService.putMessage(Games.ROLL_ROYE.getId(), username, moneyExchange - totalBetValue);
                    }
                }
            }
            String linesWin = slot7IconTableInfo.lineWinToString();
            String prizesOnLine = slot7IconTableInfo.moneyWinToString();
            countFreeSpin = slot7IconTableInfo.freeSpin;
            msg.referenceId = referenceId;
            msg.matrix = slot7IconTableInfo.matrixToString();
            msg.linesWin = linesWin;
            msg.prize = totalPrizes;
            msg.haiSao = "";
            if (slot7IconTableInfo.miniGame > 0) {
                msg.haiSao = slot7IconTableInfo.miniGameSlotResponse.getPrizes();
            }
            if (countFreeSpin > 0) {
                msg.isFreeSpin = 1;
                msg.ratio = (byte) countFreeSpin;
                msg.currentNumberFreeSpin = (byte) (countFreeSpin + ratio - 1);
                this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, username, linesStr, countFreeSpin, countFreeSpin);
            } else {
                msg.isFreeSpin = 0;
                msg.ratio = 0;
                msg.currentNumberFreeSpin = (byte) (Math.max(ratio - 1,0)) ;;
            }
            try {
                if (!u.isBot()) {
                    this.slotService.logRollRoye(referenceId, username, this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                }
                if (result == 3 || result == 4) {
                    this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, (int) result);
                }
                if (result == 3 || result == 2 || result == 4) {
                    this.broadcastBigWin(username, (byte) result, totalPrizes);
                }
            } catch (InterruptedException award) {
            } catch (TimeoutException award) {
            } catch (IOException award) {
                // empty catch block
            }
            this.saveFund();
            this.savePot();
        } else {
            result = 101;
        }
        msg.result = (byte) result;
        msg.currentMoney = currentMoney;
        long endTime = System.currentTimeMillis();
        long handleTime = endTime - startTime;
        String ratioTime = CommonUtils.getRatioTime((long) handleTime);
        Debug.trace("Free Spin ", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

//    public synchronized ResultSlotMsg playFree(String username, String linesStr, int ratio, long referenceId) {
//        long startTime = System.currentTimeMillis();
//        String currentTimeStr = DateTimeUtils.getCurrentTime();
//        short result = 0;
//        String[] lineArr = linesStr.split(",");
//        long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
//        long totalBetValue = lineArr.length * this.betValue;
//        ResultSlotMsg msg = new ResultSlotMsg();
//        if (lineArr.length > 0 && !linesStr.isEmpty()) {
//            if (totalBetValue > 0L) {
//                boolean enoughPair = false;
//                ArrayList<AwardsOnLine> awardsOnLines = new ArrayList<AwardsOnLine>();
//                long totalPrizes = 0L;
//                block4 : while (!enoughPair) {
//                    Random rd;
//                    MoneyResponse moneyRes;
//                    int soLanNoHu;
//                    String des232;
//                    long moneyExchange;
//                    int n;
//                    result = 0;
//                    awardsOnLines.clear();
//                    totalPrizes = 0L;
//                    String linesWin = "";
//                    String prizesOnLine = "";
//                    String haiSao = "";
//                    boolean forceNoHu = false;
////                    if (lineArr.length >= 5 && (soLanNoHu = ConfigGame.getIntValue(String.valueOf(this.gameName) + "_so_lan_no_hu")) > 0 && this.fund > this.initPotValue * 2L && (n = (rd = new Random()).nextInt(soLanNoHu)) == 0) {
////                        forceNoHu = true;
////                    }
//                    VQVItem[][] matrix = forceNoHu ? VQVUtils.generateMatrixNoHu(lineArr) : VQVUtils.generateMatrix();
//                    for (String entry2 : lineArr) {
//                        ArrayList<VQVAward> awardList = new ArrayList<VQVAward>();
//                        Line line = VQVUtils.getLine(this.lines, matrix, Integer.parseInt(entry2));
//                        VQVUtils.calculateLine(line, awardList);
//                        for (VQVAward award : awardList) {
//                            long moneyOnLine = 0L;
//                            if (award.getRatio() <= 0.0f) continue block4;
//                            moneyOnLine = (long)(award.getRatio() * (float)this.betValue);
//                            AwardsOnLine aol = new AwardsOnLine(award, moneyOnLine, line.getName());
//                            awardsOnLines.add(aol);
//                        }
//                    }
//                    StringBuilder builderLinesWin = new StringBuilder();
//                    StringBuilder builderPrizesOnLine = new StringBuilder();
//                    for (AwardsOnLine entry2 : awardsOnLines) {
//                        if (entry2.getAward() == VQVAward.PENTA_JACKPOT && !this.isBot(username)) continue block4;
//                        totalPrizes += entry2.getMoney();
//                        builderLinesWin.append(",");
//                        builderLinesWin.append(entry2.getLineId());
//                        builderPrizesOnLine.append(",");
//                        builderPrizesOnLine.append(entry2.getMoney());
//                    }
//                    if (builderLinesWin.length() > 0) {
//                        builderLinesWin.deleteCharAt(0);
//                    }
//                    if (builderPrizesOnLine.length() > 0) {
//                        builderPrizesOnLine.deleteCharAt(0);
//                    }
//                    if (this.fund - (totalPrizes *= (long)ratio) < this.initPotValue * 2L && totalPrizes - totalBetValue >= 0L) continue;
//
// [DISABLED-MR28]                     // [SUN-589 Phase 3c] RTP Engine — exempt jackpot (result==3||4)
// [DISABLED-MR28]                     if (result != 3 && result != 4) {
// [DISABLED-MR28]                         if (SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_rollroy")) {
// [DISABLED-MR28]                             
// [DISABLED-MR28]                                        totalPrizes = 0;
// [DISABLED-MR28]                                        awardsOnLines.clear();
// [DISABLED-MR28]                                        builderLinesWin = new StringBuilder();
// [DISABLED-MR28]                                        builderPrizesOnLine = new StringBuilder();
// [DISABLED-MR28]                                        result = 0;
// [DISABLED-MR28]                                    }
// [DISABLED-MR28]                     }
// [DISABLED-MR28]                     enoughPair = true;
// [DISABLED-MR28] //                    if (totalPrizes > 0L) {
//                        if (result == 3) {
//                            if (this.huX2) {
//                                result = 4;
//                            }
//                            this.noHuX2();
//                            this.pot = this.initPotValue;
//                            this.fund -= totalPrizes;
//                        } else {
//                            this.fund -= totalPrizes;
//                            if (result == 0) {
//                                result = totalPrizes >= (long)(this.betValue * 100) ? (short)2 : 1;
//                            }
//                        }
//                    }
//                    if ((moneyExchange = totalPrizes) > 0L && (moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, this.gameName, des232 = gn + " - Free", this.buildDescription(totalBetValue, totalPrizes, result), 0L, (Long)null, TransType.VIPPOINT)) != null && moneyRes.isSuccess()) {
//                        currentMoney = moneyRes.getCurrentMoney();
//                        if (this.moneyType == 1 && moneyExchange >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
//                            this.broadcastMsgService.putMessage(Games.ROLL_ROYE.getId(), username, moneyExchange - totalBetValue);
//                        }
//                    }
//                    this.slotService.updateLuotQuaySlotFree(this.cacheFreeName, username);
//                    linesWin = builderLinesWin.toString();
//                    prizesOnLine = builderPrizesOnLine.toString();
//                    msg.referenceId = referenceId;
//                    msg.matrix = VQVUtils.matrixToString(matrix);
//                    msg.linesWin = linesWin;
//                    msg.prize = totalPrizes / (long)ratio;
//                    msg.haiSao = "";
//                    msg.isFreeSpin = 0;
//                    msg.ratio = (byte)ratio;
//                    try {
//                        if(!isBot(username))
//                            this.slotService.logRollRoye(referenceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
//                        if (result == 3 || result == 4) {
//                            this.slotService.addTop(this.gameName, username, this.betValue, totalPrizes, currentTimeStr, (int)result);
//                        }
//                        if (result == 3 || result == 2 || result == 4) {
//                            this.broadcastBigWin(username, (byte)result, totalPrizes);
//                        }
//                    }
//                    catch (InterruptedException des232_0) {
//                    }
//                    catch (TimeoutException des232_1) {
//                    }
//                    catch (IOException des232_2) {
//                        // empty catch block
//                    }
//                    this.saveFund();
//                    this.savePot();
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
//        return msg;
//    }

    public synchronized ResultSlotMsg playFreeDaily(String username) {
        long refernceId = this.module.getNewReferenceId();
        // short result = 0;
        String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
        return this.playFree(username, linesStr, 0, refernceId);
    }
//    public synchronized ResultSlotMsg playFreeDaily(String username) {
//        long startTime = System.currentTimeMillis();
//        String currentTimeStr = DateTimeUtils.getCurrentTime();
//        long refernceId = this.module.getNewReferenceId();
//        short result = 0;
//        String linesStr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
//        String[] lineArr = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20".split(",");
//        long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
//        ResultSlotMsg msg = new ResultSlotMsg();
//        boolean enoughPair = false;
//        ArrayList<AwardsOnLine> awardsOnLines = new ArrayList<AwardsOnLine>();
//        long totalPrizes = 0L;
//        block4 : while (!enoughPair) {
//            result = 0;
//            awardsOnLines.clear();
//            totalPrizes = 0L;
//            String linesWin = "";
//            String prizesOnLine = "";
//            String haiSao = "";
//            VQVItem[][] matrix = VQVUtils.generateMatrix();
//            for (String entry2 : lineArr) {
//                ArrayList<VQVAward> awardList = new ArrayList<VQVAward>();
//                Line line = VQVUtils.getLine(this.lines, matrix, Integer.parseInt(entry2));
//                VQVUtils.calculateLine(line, awardList);
//                for (VQVAward award : awardList) {
//                    long money = 0L;
//                    if (award.getRatio() <= 0.0f) continue block4;
//                    money = (long)(award.getRatio() * (float)this.betValue);
//                    AwardsOnLine aol = new AwardsOnLine(award, money, line.getName());
//                    awardsOnLines.add(aol);
//                }
//            }
//            StringBuilder builderLinesWin = new StringBuilder();
//            StringBuilder builderPrizesOnLine = new StringBuilder();
//            for (AwardsOnLine entry2 : awardsOnLines) {
//                if ((entry2.getAward() == VQVAward.PENTA_JACKPOT || entry2.getAward() == VQVAward.QUADRA_JACKPOT || entry2.getAward() == VQVAward.TRIPLE_JACKPOT)) continue block4;
//
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
//            if (this.fund - totalPrizes < 0L || totalPrizes > (long)ConfigGame.getIntValue("max_prize_free_daily", 2000)) continue;
//            enoughPair = true;
//            boolean updated = this.slotService.updateLuotQuayFreeDaily(this.gameName, username, this.betValue);
//            if (!updated) {
//                result = 103;
//            } else {
//                MoneyResponse moneyRes;
//                String des;
//                long moneyExchange = totalPrizes;
//                if (moneyExchange > 0L && (moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, "ThoDanVqFree", des = gn + " Free", "C\u01b0\u1ee3c: 0, Th\u1eafng: " + totalPrizes, 0L, (Long)null, TransType.NO_VIPPOINT)) != null && moneyRes.isSuccess()) {
//                    currentMoney = moneyRes.getCurrentMoney();
//                }
//            }
//            linesWin = builderLinesWin.toString();
//            prizesOnLine = builderPrizesOnLine.toString();
//            msg.referenceId = refernceId;
//            msg.matrix = VQVUtils.matrixToString(matrix);
//            msg.linesWin = linesWin;
//            msg.prize = totalPrizes;
//            msg.haiSao = "";
//            try {
//                if(!isBot(username))
//                    this.slotService.logRollRoye(refernceId, username, (long)this.betValue, "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20", linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
//            }
//            catch (InterruptedException moneyExchange) {
//            }
//            catch (TimeoutException moneyExchange) {
//            }
//            catch (IOException moneyExchange) {}
//        }
//        msg.result = (byte)result;
//        msg.currentMoney = currentMoney;
//        long endTime = System.currentTimeMillis();
//        long handleTime = endTime - startTime;
//        String ratioTime = CommonUtils.getRatioTime((long)handleTime);
//        SlotUtils.logVQV(refernceId, username, this.betValue, msg.matrix, msg.haiSao, result, handleTime, ratioTime, currentTimeStr);
//        return msg;
//    }

    private MiniGameSlotResponse generatePickStars(int ratio) {
        MiniGameSlotResponse response = new MiniGameSlotResponse();
        int totalMoney = 0;
        ArrayList<PickStarGift> gifts = new ArrayList<PickStarGift>();
        PickStarGifts pickStarGifts = new PickStarGifts();
        String responsePickStars = "";
        boolean totalKeys = true;
        block5:
        for (int numPicks = 10; numPicks > 0; --numPicks) {
            PickStarGiftItem gift = pickStarGifts.pickRandomAndRandomGift();
            switch (gift) {
                case GOLD: {
                    totalMoney += 4 * this.betValue * 1;
                    gifts.add(new PickStarGift(PickStarGiftItem.GOLD, 0));
                    continue block5;
                }
                case KEY: {
                    gifts.add(new PickStarGift(PickStarGiftItem.KEY, 0));
                    continue block5;
                }
                case BOX: {
                    int boxValue = this.randomBoxValue();
                    totalMoney += boxValue * this.betValue * 1;
                    gifts.add(new PickStarGift(PickStarGiftItem.BOX, boxValue));
                    break;
                }
            }
        }
        totalMoney *= ratio;
        responsePickStars = String.valueOf(responsePickStars) + ratio;
        for (PickStarGift pickStarGift : gifts) {
            if (responsePickStars.length() == 0) {
                responsePickStars = String.valueOf(responsePickStars) + pickStarGift.getName();
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
        if (user.getProperty("numFreeDaily") != null) {
            numFree = (Integer) user.getProperty("numFreeDaily");
        }
        ResultSlotMsg msg = null;
        RollRoyFreeDailyMsg freeDailyMsg = new RollRoyFreeDailyMsg();
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
            MinimizeResultRollRoyMsg miniMsg = new MinimizeResultRollRoyMsg();
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
                Debug.trace(this.gameName + ": update fund error ", e.getMessage());
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
                Debug.trace((Object[]) new Object[]{String.valueOf(this.gameName) + ": update pot poker error ", e.getMessage()});
            }
            byte x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
            ((RollRoyModule) this.module).updatePot(this.id, this.pot, x2);
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
            user.setProperty("numFreeDaily", model.getRotateFree());
            RollRoyFreeDailyMsg freeDailyMsg = new RollRoyFreeDailyMsg();
            freeDailyMsg.remain = (byte) model.getRotateFree();
            SlotUtils.sendMessageToUser((BaseMsg) freeDailyMsg, user);
        } else {
            user.removeProperty("numFreeDaily");
        }
        if (result) {
            user.setProperty(("MGROOM_" + this.gameName + "_INFO"), this);
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

