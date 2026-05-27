package game.modules.slot.room;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.slot.SlotFreeSpin;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.GameConfig.GameConfig;
import game.modules.Slot3x3.Slot3x3TableInfo;
import game.modules.Slot3x3.Slot3x3Util;
import game.modules.SlotDescription.SlotDescriptionUtils;
import game.modules.slot.GalaxyModule;
import game.modules.slot.cmd.send.galaxy.*;
import game.modules.slot.entities.slot.AutoUser;
import game.modules.slot.utils.SlotHouseEdge;
import game.modules.slot.utils.SlotLogHelper;
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

/**
 * Galaxy (Kim Cuong) slot room — 3x3 matrix, 6 symbols (0-5), 3 paylines, 20 client lines.
 */
public class GalaxyRoom extends SlotRoom {
    private final Runnable gameLoopTask = new GameLoopTask();
    private final Runnable checkResetPotTask = new CheckResetPotTask();
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
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("slot");

    private int[] parseSelectedLines(String linesStr) {
        if (linesStr == null) return null;
        String trimmed = linesStr.trim();
        if (trimmed.isEmpty()) return null;
        String[] lineArr = trimmed.split(",");
        int[] result = new int[lineArr.length];
        for (int i = 0; i < lineArr.length; i++) {
            String line = lineArr[i].trim();
            if (line.isEmpty()) return null;
            try {
                int parsed = Integer.parseInt(line);
                if (parsed < 1 || parsed > 20) return null;
                result[i] = parsed;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }

    public GalaxyRoom(GalaxyModule module, byte id, String name, short moneyType, long pot, long fund, int betValue,
                      long initPotValue, long fundJackPot, String fundJackPotName, long fundMinigame, String fundMinigameName) {
        super(id, name, betValue, moneyType, pot, fund, initPotValue);
        this.module = module;
        this.moneyType = moneyType;
        this.gameName = Games.GALAXY.getName();
        this.gameID = Games.GALAXY.getId() + "";
        this.cacheFreeName = this.gameName + betValue;
        CacheServiceImpl cacheService = new CacheServiceImpl();
        cacheService.setValue(name, (int) pot);
        this.betValue = betValue;
        this.initPotValue = initPotValue;
        this.gn = this.gameName;

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
                com.vinplay.vbee.common.utils.GameHealthServer.tick(); GalaxyRoom.this.checkResetPot(); } catch (Throwable e) { e.printStackTrace(); }
        }
    }

    @Override
    public void forceStopAutoPlay(User user) {
        super.forceStopAutoPlay(user);
        Map map = this.usersAuto;
        synchronized (map) {
            this.usersAuto.remove(user.getName());
            ForceStopAutoPlayGalaxyMsg msg = new ForceStopAutoPlayGalaxyMsg();
            SlotUtils.sendMessageToUser((BaseMsg) msg, user);
        }
    }

    public ResultGalaxyMsg play(String username, String linesStr) {
        long referenceId = this.module.getNewReferenceId();
        try {
            SlotFreeSpin freeSpin = this.slotService.getLuotQuayFreeSlot(this.cacheFreeName, username);
            int luotQuayFree = freeSpin.getNum();
            if (luotQuayFree > 0) {
                linesStr = freeSpin.getLines();
                return this.playFree(username, linesStr, luotQuayFree, referenceId);
            }
        } catch (Exception e) {
            Debug.trace("error get free spin galaxy");
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
            totalPrizes = this.pot * 2;
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
        BigWinGalaxyMsg bigWinMsg = new BigWinGalaxyMsg();
        bigWinMsg.username = username;
        bigWinMsg.type = result;
        bigWinMsg.betValue = (short) this.betValue;
        bigWinMsg.totalPrizes = totalPrizes;
        bigWinMsg.timestamp = DateTimeUtils.getCurrentTime();
        this.module.sendMsgToAllUsers(bigWinMsg);
    }

    public ResultGalaxyMsg playNormal(String username, String linesStr, long referenceId) {
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        long moneyAvailable = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        UserCacheModel u = this.userService.getUser(username);
        ResultGalaxyMsg msg = new ResultGalaxyMsg();
        int[] clientLines = this.parseSelectedLines(linesStr);

        if (clientLines == null || clientLines.length == 0) {
            Debug.trace("galaxy invalid lines playNormal username=" + username + " lines=" + linesStr);
            msg.result = (byte) 101;
            msg.currentMoney = currentMoney;
            return msg;
        }

        long totalBetValue = clientLines.length * this.betValue;

        if (clientLines.length > 0 && totalBetValue > 0L) {
            if (totalBetValue <= moneyAvailable) {
                long fee = totalBetValue * 3 / 100;
                MoneyResponse moneyRes = new MoneyResponse(false, "1001");
                if (!u.isBot()) {
                    moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, this.gameName, this.gameID,
                            SlotDescriptionUtils.getBetDescription(this.gameID),
                            fee, Long.valueOf(referenceId), TransType.START_TRANS);
                } else {
                    moneyRes.setSuccess(true);
                }
                if (moneyRes != null && moneyRes.isSuccess()) {
                    currentMoney = moneyRes.getCurrentMoney();
                    long moneyToFundJackpot = totalBetValue * (1 + 1) / 100;
                    long moneyToPot = totalBetValue * 1 / 100;
                    long moneyToFund = totalBetValue - fee - moneyToFundJackpot;

                    this.fund += moneyToFund;
                    this.fundJackPot += moneyToFundJackpot;
                    this.pot += moneyToPot;

                    long totalPrizes = 0;
                    long tienThuongX2 = 0;
                    long maxWin = Long.MAX_VALUE;

                    // Dynamic house edge: force lose based on RTP config for this game
                    boolean forceLose = SlotHouseEdge.shouldForceLoseWithPct(this.fund, this.fundJackPot, totalBetValue, this.betValue, username, "slot_galaxy");

                    Slot3x3TableInfo tableInfo;
                    if (forceLose) {
                        tableInfo = Slot3x3Util.rollLose0(clientLines, this.betValue, 0);
                    } else {
                        tableInfo = Slot3x3Util.getSlot3x1TableInfo(
                                clientLines, this.betValue, this.fund, this.fundJackPot,
                                this.isMultiJackpot(), this.pot, this.initPotValue, maxWin);
                    }

                    // Check for admin-forced result (3x3)
                    int[][] forcedIds = game.modules.slot.utils.SlotForceResultHelper.checkAndConsume("GALAXY", username, 3, 3);
                    if (forcedIds != null) {
                        byte[][] forcedTable = new byte[3][3];
                        for (int fi = 0; fi < 3; fi++)
                            for (int fj = 0; fj < 3; fj++)
                                forcedTable[fi][fj] = (byte) forcedIds[fi][fj];
                        tableInfo = new Slot3x3TableInfo(forcedTable, this.betValue, 0);
                        tableInfo.calculateRowIndex(clientLines);
                    }

                    totalPrizes += tableInfo.money * this.betValue;
                    // Cap prize to protect fund
                    totalPrizes = SlotHouseEdge.capPrize(totalPrizes, this.fund);
                    this.fund -= totalPrizes;

                    if (tableInfo.isJackPot) {
                        if (this.isMultiJackpot()) {
                            totalPrizes += this.pot * 2;
                            this.fundJackPot -= this.pot * 2;
                            tienThuongX2 = this.pot * (2 - 1);
                        } else {
                            totalPrizes += this.pot;
                            this.fundJackPot -= this.pot;
                        }
                    }

                    if (tableInfo.isJackPot) {
                        if (this.isMultiJackpot()) {
                            result = 4;
                            this.noHuX2();
                        } else {
                            result = 3;
                        }
                        this.resetPot();
                    } else {
                        result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
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
                                this.broadcastMsgService.putMessage(Games.GALAXY.getId(), username, moneyExchange - totalBetValue);
                            }
                        }
                    }

                    String linesWin = tableInfo.lineWinToString();
                    String prizesOnLine = tableInfo.moneyWinToString();
                    msg.referenceId = referenceId;
                    msg.matrix = tableInfo.matrixToString();
                    msg.linesWin = linesWin;
                    msg.prize = totalPrizes;
                    msg.haiSao = "";
                    msg.freeSpin = 0;

                    try {
                        if (!u.isBot()) {
                            SlotLogHelper.logSpin(this.gameName, referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
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
        msg.result = (byte) result;
        msg.currentMoney = currentMoney;
        Debug.trace("Normal Spin galaxy 3x3", "totalWin = " + msg.prize, "  pot = " + this.pot, " fund =" + this.fund);
        return msg;
    }

    public ResultGalaxyMsg playFree(String username, String linesStr, int ratio, long referenceId) {
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        long currentMoney = this.userService.getCurrentMoneyUserCache(username, this.moneyTypeStr);
        ResultGalaxyMsg msg = new ResultGalaxyMsg();
        UserCacheModel u = this.userService.getUser(username);
        int[] clientLines = this.parseSelectedLines(linesStr);

        if (clientLines == null || clientLines.length == 0) {
            msg.result = (byte) 101;
            msg.currentMoney = currentMoney;
            return msg;
        }

        MoneyResponse moneyRes = new MoneyResponse(false, "1001");
        if (ratio > 0) {
            this.slotService.updateLuotQuaySlotFree(this.cacheFreeName, username);
        }

        long totalPrizes = 0;
        long tienThuongX2 = 0;
        long maxWin = Long.MAX_VALUE;

        Slot3x3TableInfo tableInfo = Slot3x3Util.getSlot3x1TableInfo(
                clientLines, this.betValue, this.fund, this.fundJackPot,
                this.isMultiJackpot(), this.pot, this.initPotValue, maxWin);

        totalPrizes += tableInfo.money * this.betValue;
        this.fund -= totalPrizes;

        if (tableInfo.isJackPot) {
            if (this.isMultiJackpot()) {
                totalPrizes += this.pot * 2;
                this.fundJackPot -= this.pot * 2 - (this.pot - this.initPotValue);
                tienThuongX2 = this.pot * (2 - 1);
            } else {
                totalPrizes += this.pot;
                this.fundJackPot -= this.initPotValue;
            }
        }

        if (tableInfo.isJackPot) {
            if (this.isMultiJackpot()) {
                result = 4;
                this.noHuX2();
            } else {
                result = 3;
            }
            this.resetPot();
        } else {
            result = totalPrizes >= (long) (this.betValue * 100) ? (short) 2 : 1;
        }

        long moneyExchange = totalPrizes - tienThuongX2;
        if (tienThuongX2 > 0L && !u.isBot()) {
            moneyRes = this.userService.updateMoney(username, tienThuongX2, this.moneyTypeStr, this.gameName,
                    this.gameID, SlotDescriptionUtils.getMultiJackpotDescription(this.gameID),
                    0L, (Long) null, TransType.NO_VIPPOINT);
            currentMoney = moneyRes.getCurrentMoney();
        }
        if (totalPrizes != 0 && !u.isBot()) {
            long totalBetValue = 0L;
            if ((moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, this.gameName,
                    this.gameID, SlotDescriptionUtils.getPayDescription(this.gameID, totalBetValue, totalPrizes, result),
                    0L, Long.valueOf(referenceId), TransType.END_TRANS)) != null && moneyRes.isSuccess()) {
                currentMoney = moneyRes.getCurrentMoney();
                if (this.moneyType == 1 && moneyExchange >= (long) BroadcastMessageServiceImpl.MIN_MONEY) {
                    this.broadcastMsgService.putMessage(Games.GALAXY.getId(), username, moneyExchange - totalBetValue);
                }
            }
        }

        String linesWin = tableInfo.lineWinToString();
        String prizesOnLine = tableInfo.moneyWinToString();
        msg.referenceId = referenceId;
        msg.matrix = tableInfo.matrixToString();
        msg.linesWin = linesWin;
        msg.prize = totalPrizes;
        msg.haiSao = "";
        msg.freeSpin = 0;

        try {
            if (!u.isBot()) {
                SlotLogHelper.logSpin(this.gameName, referenceId, username, (long) this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, currentTimeStr);
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
        ResultGalaxyMsg msg = this.play(user.getName(), linesStr);
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
        UpdatePotGalaxyMsg msg = new UpdatePotGalaxyMsg();
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
                user.setMaxCount(8);
            } catch (Exception ex) {
                Logger.getLogger(GalaxyRoom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        users.clear();
    }

    private class PlayListAutoUserTask implements Runnable {
        private List users;
        PlayListAutoUserTask(List users) { this.users = users; }
        @Override
        public void run() {
            try { GalaxyRoom.this.playListAuto(this.users); } catch (Throwable e) { Debug.trace("AutoPlay galaxy error: " + e.getMessage()); }
        }
    }

    protected final class GameLoopTask implements Runnable {
        public void run() {
            try { GalaxyRoom.this.gameLoop(); } catch (Throwable e) { e.printStackTrace(); }
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
            UpdatePotGalaxyMsg msg = new UpdatePotGalaxyMsg();
            msg.value = this.pot;
            msg.x2 = (byte) (this.isMultiJackpot() ? 1 : 0);
            this.sendMessageToRoom(msg);
        }
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        GalaxyFreeDailyMsg freeDailyMsg = new GalaxyFreeDailyMsg();
        SlotUtils.sendMessageToUser((BaseMsg) freeDailyMsg, user);
        if (result) {
            user.setProperty(("MGROOM_" + this.gameName + "_INFO"), this);
        }
        return result;
    }
}
