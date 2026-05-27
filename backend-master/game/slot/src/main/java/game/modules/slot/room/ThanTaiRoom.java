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
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.GameConfig.GameConfig;
import game.modules.ThanTai.ThanTaiTableInfo;
import game.modules.ThanTai.ThanTaiUtil;
import game.modules.SlotDescription.SlotDescriptionUtils;
import game.modules.SlotUtils.GiftType;
import game.modules.slot.ThanTaiModule;
import game.modules.slot.cmd.send.thantai.*;
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

public class ThanTaiRoom extends SlotRoom {
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

    public ThanTaiRoom(ThanTaiModule module, byte id, String name, short moneyType, long pot, long fund, int betValue,
                       long initPotValue, long fundJackPot, String fundJackPotName, long fundMinigame, String fundMinigameName) {
        super(id, name, betValue, moneyType, pot, fund, initPotValue);
        this.module = module;
        this.moneyType = moneyType;
        this.gameName = Games.SPARTAN.getName();
        this.gameID = Games.SPARTAN.getId() + "";
        this.cacheFreeName = this.gameName + betValue;
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

    public boolean isMultiJackpot() {
        return this.huX2;
    }

    public boolean isUserBigWin(String username) {
        return this.userService.isUserBigWin(username);
    }

    public boolean isUserJackpot(String username) {
        return this.userService.isUserJackpot(username);
    }

    protected void checkResetPot() {
        try {
            CacheServiceImpl sv = new CacheServiceImpl();
            int isReset = sv.getValueInt("reset_pot_" + this.gn + "_" + this.betValue);
            if (isReset == 1) {
                this.pot = this.initPotValue;
                this.fund = 0;
                this.savePotInternal();
                this.saveFundInternal();
                sv.removeKey("reset_pot_" + this.gn + "_" + this.betValue);
            }
        } catch (Exception e) {
        }
    }

    protected final class CheckResetPotTask implements Runnable {
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick(); ThanTaiRoom.this.checkResetPot(); } catch (Throwable e) { e.printStackTrace(); }
        }
    }

    @Override
    public void forceStopAutoPlay(User user) {
        super.forceStopAutoPlay(user);
        Map map = this.usersAuto;
        synchronized (map) {
            this.usersAuto.remove(user.getName());
            ForceStopAutoPlayThanTaiMsg msg = new ForceStopAutoPlayThanTaiMsg();
            SlotUtils.sendMessageToUser((BaseMsg) msg, user);
        }
    }

    public ResultThanTaiMsg play(String username, String linesStr) {
        long referenceId = this.module.getNewReferenceId();
        try {
            SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(this.cacheFreeName, username);
            int luotQuayFree = freeSpin.getNum();
            if (luotQuayFree > 0) {
                linesStr = freeSpin.getLines();
                return this.playFree(username, linesStr, luotQuayFree, referenceId);
            }
        } catch (Exception e) {
            Debug.trace("error get free spin thantai");
        }
        return this.playNormal(username, linesStr, referenceId);
    }

    public synchronized void addMoneyToPot(long money) {
        this.pot += money;
        this.savePotInternal();
    }

    public void resetPot() {
        this.pot = this.initPotValue;
        this.savePotInternal();
    }

    public void botEatJackpot(String keyBot, long nextTime, String username) {
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        int result = 3;
        long totalPrizes = this.pot;
        if (this.isMultiJackpot()) {
            result = 4;
            totalPrizes = this.pot * GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT;
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
        BigWinThanTaiMsg bigWinMsg = new BigWinThanTaiMsg();
        bigWinMsg.username = username;
        bigWinMsg.type = result;
        bigWinMsg.betValue = (short) this.betValue;
        bigWinMsg.totalPrizes = totalPrizes;
        bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
        this.module.sendMsgToAllUsers(bigWinMsg);
    }

    public ResultThanTaiMsg playNormal(String username, String linesStr, long referenceId) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long moneyAvailable = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        UserCacheModel u = this.userService.getUser(username);
        long totalBetValue = lineArr.length * this.betValue;
        ResultThanTaiMsg msg = new ResultThanTaiMsg();

        if (lineArr.length > 0 && !linesStr.isEmpty()) {
            if (totalBetValue > 0L) {
                if (totalBetValue <= moneyAvailable) {
                    long fee = totalBetValue * GameConfig.getInstance().thanTaiConfig.FEE / 100;
                    MoneyResponse moneyRes = new MoneyResponse(false, "1001");
                    if (!u.isBot()) {
                        moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName, this.gameID,
                                SlotDescriptionUtils.getBetDescription(this.gameID),
                                fee, Long.valueOf(referenceId), TransType.START_TRANS);
                        DailyQuestUtils.playerPlayGame(username, Games.SPARTAN.getId(), totalBetValue);
                    } else {
                        moneyRes.setSuccess(true);
                    }
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        currentMoney = moneyRes.getCurrentMoney();
                        long moneyToFundJackpot = totalBetValue * (GameConfig.getInstance().thanTaiConfig.RATE_TO_FUND_JACKPOT + GameConfig.getInstance().thanTaiConfig.RATE_TO_JACKPOT) / 100;
                        long moneyToFundMinigame = totalBetValue * GameConfig.getInstance().thanTaiConfig.RATE_TO_FUND_MINIGAME / 100;
                        long moneyToPot = totalBetValue * GameConfig.getInstance().thanTaiConfig.RATE_TO_JACKPOT / 100;
                        long moneyToFund = totalBetValue - fee - moneyToFundJackpot - moneyToFundMinigame;

                        boolean isUserBigWin = this.isUserBigWin(username);
                        if (!isUserBigWin) {
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

                        ThanTaiTableInfo tableInfo = ThanTaiUtil.getTableInfo(rowIndex, this.betValue, this.fund,
                                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                this.boxValues, false, maxiumWin);

                        // Check for admin-forced result
                        int[][] forcedIds = game.modules.slot.utils.SlotForceResultHelper.checkAndConsume("THANTAI", username, 3, 5);
                        if (forcedIds != null) {
                            byte[] forcedTable = new byte[15];
                            for (int fi = 0; fi < 3; fi++)
                                for (int fj = 0; fj < 5; fj++)
                                    forcedTable[fi * 5 + fj] = (byte) forcedIds[fi][fj];
                            tableInfo.table = forcedTable;
                            tableInfo.tableCalculate = forcedTable.clone();
                            tableInfo.calculate(rowIndex);
                        }

                        if (isUserBigWin) {
                            tableInfo = ThanTaiUtil.getBigWinTableInfo(rowIndex, this.betValue, this.fund,
                                    this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                                    this.boxValues, false, maxiumWin);
                        }

                        if (this.isUserJackpot(username)) {
                            tableInfo = new ThanTaiTableInfo(GiftType.JACKPOT, this.betValue,
                                    ThanTaiUtil.getMoneyJackPot(this.pot, this.isMultiJackpot(), this.initPotValue),
                                    this.boxValues);
                            tableInfo.calculate(rowIndex);
                        }

                        totalPrizes += tableInfo.money * this.betValue;
                        if (!isUserBigWin) {
                            // Phase 3c — pct-aware force-lose (SLOT_USE_DYNAMIC_RTP flag)
                            totalPrizes = game.modules.slot.utils.SlotHouseEdge.maybeForceLose(totalPrizes, result, username, "slot_thantai");
                            this.fund -= totalPrizes;
                        }
                        if (tableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                totalPrizes += this.pot * GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT;
                                if (!isUserBigWin) {
                                    this.fundJackPot -= this.pot * GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT;
                                }
                                tienThuongX2 = this.pot * (GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT - 1);
                            } else {
                                totalPrizes += this.pot;
                                if (!isUserBigWin) {
                                    this.fundJackPot -= this.pot;
                                }
                            }
                        }
                        if (tableInfo.miniGame > 0) {
                            totalPrizes += tableInfo.miniGameSlotResponse.getTotalPrize();
                            if (!isUserBigWin) {
                                this.fundMinigame -= tableInfo.miniGameSlotResponse.getTotalPrize();
                            }
                        }
                        if (tableInfo.jackpot) {
                            if (this.isMultiJackpot()) {
                                result = 4;
                                this.noHuX2();
                            } else {
                                result = 3;
                            }
                            this.resetPot();
                        } else {
                            if (tableInfo.miniGame > 0) {
                                result = 5;
                            } else {
                                result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
                            }
                        }
                        // [SUN-589 Phase 3c] RTP Engine — zero prize if force-lose fires (jackpot exempt above)
                        if (result != 3 && result != 4 && totalPrizes > 0
                                && SlotHouseEdge.shouldForceLoseByPctOnly(username, "slot_thantai")) {
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

                        String linesWin = tableInfo.lineWinToString();
                        String prizesOnLine = tableInfo.moneyWinToString();
                        countFreeSpin = tableInfo.freeSpin;
                        msg.referenceId = referenceId;
                        msg.matrix = tableInfo.matrixToString();
                        msg.linesWin = linesWin;
                        msg.prize = totalPrizes;
                        msg.haiSao = "";
                        if (tableInfo.miniGame > 0) {
                            msg.haiSao = tableInfo.miniGameSlotResponse.getPrizes();
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
                            if (!u.isBot()) {
                                this.slotService.logChiemtinh(referenceId, username, this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
                            }
                            if (result == 3 || result == 4) {
                                this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, result);
                            }
                            if (result == 3 || result == 2 || result == 4) {
                                this.broadcastBigWin(username, (byte) result, totalPrizes);
                            }
                        } catch (InterruptedException | TimeoutException | IOException ignored) {
                        }
                        this.saveFundInternal();
                        this.savePotInternal();
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
        Debug.trace("Normal Spin thantai", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund,
                "fund Jackpot = " + this.fundJackPot, "fund Minigame = " + this.fundMinigame);
        return msg;
    }

    public ResultThanTaiMsg playFree(String username, String linesStr, int ratio, long referenceId) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long totalBetValue = 0L;
        ResultThanTaiMsg msg = new ResultThanTaiMsg();
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

        ThanTaiTableInfo tableInfo = ThanTaiUtil.getTableInfo(rowIndex, this.betValue, this.fund,
                this.fundJackPot, this.fundMinigame, this.isMultiJackpot(), this.pot, this.initPotValue,
                this.boxValues, true, maxiumWin);

        totalPrizes += tableInfo.money * this.betValue;
        this.fund -= totalPrizes;
        if (tableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                totalPrizes += this.pot * GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT;
                this.fundJackPot -= this.pot * GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT - (this.pot - this.initPotValue);
                tienThuongX2 = this.pot * (GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT - 1);
            } else {
                totalPrizes += this.pot;
                this.fundJackPot -= this.initPotValue;
            }
        }
        if (tableInfo.miniGame > 0) {
            totalPrizes += tableInfo.miniGameSlotResponse.getTotalPrize();
            this.fundMinigame -= tableInfo.miniGameSlotResponse.getTotalPrize();
        }
        if (tableInfo.jackpot) {
            if (this.isMultiJackpot()) {
                result = 4;
                this.noHuX2();
            } else {
                result = 3;
            }
            this.resetPot();
        } else {
            if (tableInfo.miniGame > 0) {
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

        String linesWin = tableInfo.lineWinToString();
        String prizesOnLine = tableInfo.moneyWinToString();
        countFreeSpin = tableInfo.freeSpin;
        msg.referenceId = referenceId;
        msg.matrix = tableInfo.matrixToString();
        msg.linesWin = linesWin;
        msg.prize = totalPrizes;
        msg.haiSao = "";
        if (tableInfo.miniGame > 0) {
            msg.haiSao = tableInfo.miniGameSlotResponse.getPrizes();
        }
        if (countFreeSpin > 0) {
            msg.freeSpin = 1;
            msg.ratio = (byte) countFreeSpin;
            msg.currentNumberFreeSpin = (byte) (countFreeSpin + ratio - 1);
            this.slotService.setLuotQuayFreeSlot(this.cacheFreeName, username, linesStr, countFreeSpin, countFreeSpin);
        } else {
            msg.freeSpin = 0;
        }
        try {
            if (!u.isBot()) {
                this.slotService.logChiemtinh(referenceId, username, this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
            }
            if (result == 3 || result == 4) {
                this.slotService.addTop(gn, username, this.betValue, totalPrizes, currentTimeStr, result);
            }
            if (result == 3 || result == 2 || result == 4) {
                this.broadcastBigWin(username, (byte) result, totalPrizes);
            }
        } catch (InterruptedException | TimeoutException | IOException ignored) {
        }
        this.saveFundInternal();
        this.savePotInternal();

        msg.result = (byte) result;
        msg.currentMoney = currentMoney;
        msg.isFreeSpin = true;
        return msg;
    }

    public short play(User user, String linesStr) {
        ResultThanTaiMsg msg = this.play(user.getName(), linesStr);
        msg.currentMoney = this.userService.getMoneyUserCache(user.getName(), this.moneyTypeStr);
        SlotUtils.sendMessageToUser((BaseMsg) msg, user);
        return msg.result;
    }

    public void autoPlay(final User user, final String lines, short result) {
        Map map = this.usersAuto;
        synchronized (map) {
            this.usersAuto.put(user.getName(), new AutoUser(user, lines));
        }
    }

    public void updatePot(User user) {
        UpdatePotThanTaiMsg msg = new UpdatePotThanTaiMsg();
        msg.value = this.pot;
        msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
        SlotUtils.sendMessageToUser((BaseMsg) msg, user);
    }

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
            if (toIndex > usersPlay.size()) toIndex = usersPlay.size();
            ArrayList<AutoUser> tmp = new ArrayList<AutoUser>(usersPlay.subList(fromIndex, toIndex));
            this.executor.execute(new PlayListAutoUserTask(tmp));
        }
        usersPlay.clear();
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
                if (result == 0) { user.setMaxCount(5); continue; }
                if (result == 5) { user.setMaxCount(20); continue; }
                user.setMaxCount(8);
            } catch (Exception ex) {
                Logger.getLogger(ThanTaiRoom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        users.clear();
    }

    private class PlayListAutoUserTask implements Runnable {
        private List users;
        PlayListAutoUserTask(List users) { this.users = users; }
        @Override
        public void run() {
            try { ThanTaiRoom.this.playListAuto(this.users); } catch (Throwable e) { Debug.trace("AutoPlay thantai error: " + e.getMessage()); }
        }
    }

    protected final class GameLoopTask implements Runnable {
        public void run() {
            try { ThanTaiRoom.this.gameLoop(); } catch (Throwable e) { e.printStackTrace(); }
        }
    }

    private void saveFundInternal() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdateFundToRoom >= 60000L) {
            try {
                this.mgService.saveFund(this.name, this.fund);
                this.mgService.saveFund(this.fundMinigameName, this.fundMinigame);
                this.mgService.saveFund(this.fundJackPotName, this.fundJackPot);
            } catch (IOException | InterruptedException | TimeoutException e) {
                Debug.trace(this.gameName + ": update fund error " + e.getMessage());
            }
            this.lastTimeUpdateFundToRoom = currentTime;
        }
    }

    private void savePotInternal() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdatePotToRoom >= 3000L) {
            this.lastTimeUpdatePotToRoom = currentTime;
            try {
                this.mgService.savePot(this.name, this.pot, this.isMultiJackpot());
            } catch (IOException | InterruptedException | TimeoutException e) {
                Debug.trace(this.gameName + ": update pot error " + e.getMessage());
            }
            UpdatePotThanTaiMsg msg = new UpdatePotThanTaiMsg();
            msg.value = this.pot;
            msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
            this.sendMessageToRoom(msg);
        }
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        ThanTaiFreeDailyMsg freeDailyMsg = new ThanTaiFreeDailyMsg();
        SlotUtils.sendMessageToUser((BaseMsg) freeDailyMsg, user);
        if (result) {
            user.setProperty(("MGROOM_" + this.gameName + "_INFO"), this);
        }
        return result;
    }
}
