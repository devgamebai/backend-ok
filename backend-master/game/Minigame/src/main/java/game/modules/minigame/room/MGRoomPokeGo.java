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
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.PokeGoService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.PokeGoServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package game.modules.minigame.room;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.PokeGoService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.PokeGoServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.minigame.PokeGoModule;
import game.modules.minigame.cmd.send.pokego.ForceStopAutoPlayPokeGoMsg;
import game.modules.minigame.cmd.send.pokego.ResultPokeGoMsg;
import game.modules.minigame.cmd.send.pokego.UpdatePotPokeGoMsg;
import game.modules.minigame.entities.pokego.AutoUserPokeGo;
import game.modules.minigame.entities.pokego.Award;
import game.modules.minigame.entities.pokego.AwardsOnLine;
import game.modules.minigame.entities.pokego.Item;
import game.modules.minigame.entities.pokego.Line;
import game.modules.minigame.entities.pokego.Lines;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.utils.PokeGoUtils;
import game.utils.ConfigGame;
import game.utils.GameUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MGRoomPokeGo
extends MGRoom {
    private long pot;
    private long fund;
    private long initPotValue;
    private int betValue;
    private short moneyType;
    private String moneyTypeStr;
    private UserService userService = new UserServiceImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();
    private PokeGoService pgService = new PokeGoServiceImpl();
    private BroadcastMessageService broadcastMsgService = new BroadcastMessageServiceImpl();
    private final Runnable gameLoopTask = new GameLoopTask();
    private Map<String, AutoUserPokeGo> usersAuto = new HashMap<String, AutoUserPokeGo>();
    private Lines lines = new Lines();
    private long lastTimeUpdatePotToRoom = 0L;
    private long lastTimeUpdateFundToRoom = 0L;
    private ThreadPoolExecutor executor;
    private int countHu = -1;
    private int countNoHuX2 = 0;
    private boolean huX2 = false;
    protected CacheService sv = new CacheServiceImpl();

    public MGRoomPokeGo(String name, short moneyType, long pot, long fund, int betValue, long initPotValue) {
        super(name);
        this.moneyType = moneyType;
        this.moneyTypeStr = this.moneyType == 1 ? "vin" : "xu";
        this.executor = moneyType == 1 ? (ThreadPoolExecutor)Executors.newFixedThreadPool(ConfigGame.getIntValue("poke_go_thread_pool_per_room_vin")) : (ThreadPoolExecutor)Executors.newFixedThreadPool(ConfigGame.getIntValue("poke_go_thread_pool_per_room_xu"));
        this.pot = pot;
        CacheServiceImpl cacheService = new CacheServiceImpl();
        cacheService.setValue(name, (int)pot);
        this.fund = fund;
        this.betValue = betValue;
        this.initPotValue = initPotValue;
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        try {
            this.countHu = this.sv.getValueInt(name + "_count_hu");
            this.countNoHuX2 = this.sv.getValueInt(name + "_count_no_hu_x2");
            this.calculatHuX2();
        }
        catch (KeyNotFoundException keyNotFoundException) {
            // empty catch block
        }
        try {
            this.mgService.savePot(name, pot, this.huX2);
        }
        catch (IOException | InterruptedException | TimeoutException exception) {
            // empty catch block
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void autoPlay(User user, String lines) {
        Map<String, AutoUserPokeGo> map;
        Map<String, AutoUserPokeGo> map2 = map = this.usersAuto;
        synchronized (map2) {
            if (this.usersAuto.containsKey(user.getName())) {
                AutoUserPokeGo entry = this.usersAuto.get(user.getName());
                this.forceStopAutoPlay(entry.getUser());
            }
            this.usersAuto.put(user.getName(), new AutoUserPokeGo(user, lines));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void stopAutoPlay(User user) {
        Map<String, AutoUserPokeGo> map;
        Map<String, AutoUserPokeGo> map2 = map = this.usersAuto;
        synchronized (map2) {
            AutoUserPokeGo entry;
//            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getUniqueId() == user.getUniqueId()) {
            if (this.usersAuto.containsKey(user.getName()) && (entry = this.usersAuto.get(user.getName())).getUser().getId() == user.getId()) {
                this.usersAuto.remove(user.getName());
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void forceStopAutoPlay(User user) {
        Map<String, AutoUserPokeGo> map;
        Map<String, AutoUserPokeGo> map2 = map = this.usersAuto;
        synchronized (map2) {
            this.usersAuto.remove(user.getName());
            ForceStopAutoPlayPokeGoMsg msg = new ForceStopAutoPlayPokeGoMsg();
            this.sendMessageToUser((BaseMsg)msg, user);
        }
    }

    public synchronized ResultPokeGoMsg play(String username, String linesStr) {
        long startTime = System.currentTimeMillis();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        long refernceId = PokeGoModule.getNewRefenceId();
        short result = 0;
        int soLanNoHu = ConfigGame.getIntValue("poke_go_so_lan_no_hu");
        Random rd;
        String[] lineArr = linesStr.split(",");
        long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        UserCacheModel u = this.userService.getUser(username);
        long totalBetValue = lineArr.length * this.betValue;
        ResultPokeGoMsg msg = new ResultPokeGoMsg();
        if (lineArr.length > 0 && !linesStr.isEmpty()) {
            if (totalBetValue > 0L) {
                if (totalBetValue <= currentMoney) {
                    MoneyResponse moneyRes = this.userService.updateMoney(username, -totalBetValue, this.moneyTypeStr, Games.POKE_GO.getName(), "Quay PokeGo", "\u0110\u1eb7t c\u01b0\u1ee3c PokeGo", 0L, Long.valueOf(refernceId), TransType.START_TRANS);
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        long fee = totalBetValue * 2L / 100L;
                        long moneyToPot = totalBetValue * 1L / 100L;
                        long moneyToFund = totalBetValue - fee - moneyToPot;
                        this.fund += moneyToFund;
                        this.pot += moneyToPot;
                        boolean enoughPair = false;
                        ArrayList<AwardsOnLine> awardsOnLines = new ArrayList<AwardsOnLine>();
                        long totalPrizes = 0L;
                        block4 :
                        while (!enoughPair) {
                            result = 0;
                            awardsOnLines.clear();
                            totalPrizes = 0L;
                            String linesWin = "";
                            String prizesOnLine = "";
                            boolean forceNoHu = false;
                            if (lineArr.length >= 5) {
                                if ((soLanNoHu > 0) && (fund > initPotValue * 2L)) {
                                    rd = new Random();
                                    if (rd.nextInt(soLanNoHu) == 0)
                                        forceNoHu = true;
                                }
                            }
                            Item[][] matrix = forceNoHu ? PokeGoUtils.generateMatrixNoHu(lineArr) : PokeGoUtils.generateMatrix();
                            for (int i = 0; i < lineArr.length; ++i) {
                                String entry = lineArr[i];
                                ArrayList<Award> awardList = new ArrayList<Award>();
                                Line line = PokeGoUtils.getLine(this.lines, matrix, Integer.parseInt(entry));
                                PokeGoUtils.calculateLine(line, awardList);
                                for (Award award : awardList) {
                                    long money = 0L;
                                    if (award != Award.TRIPLE_POKER_BALL) {
                                        money = (long)(award.getRatio() * (float)this.betValue);
                                    } else {
                                        for (AwardsOnLine e : awardsOnLines) {
                                            if (e.getAward() != Award.TRIPLE_POKER_BALL) continue;
                                            continue block4;
                                        }
                                        result = 3;
                                        money = this.huX2 ? this.pot * 2L : this.pot;
                                    }
                                    AwardsOnLine aol = new AwardsOnLine(award, money, line.getName());
                                    awardsOnLines.add(aol);
                                }
                            }
                            StringBuilder builderLinesWin = new StringBuilder();
                            StringBuilder builderPrizesOnLine = new StringBuilder();
                            for (AwardsOnLine entry2 : awardsOnLines) {
                                if (entry2.getAward() == Award.TRIPLE_POKER_BALL) {
                                    if (!u.isBot()) continue block4;
                                    totalPrizes += this.pot;
                                } else {
                                    totalPrizes += entry2.getMoney();
                                }
                                builderLinesWin.append(",");
                                builderLinesWin.append(entry2.getLineId());
                                builderPrizesOnLine.append(",");
                                builderPrizesOnLine.append(entry2.getMoney());
                            }
                            if (builderLinesWin.length() > 0) {
                                builderLinesWin.deleteCharAt(0);
                            }
                            if (builderPrizesOnLine.length() > 0) {
                                builderPrizesOnLine.deleteCharAt(0);
                            }
                            if (result == 3 ? this.fund - this.initPotValue < 0L : this.fund - totalPrizes < 0L) continue;
                            enoughPair = true;
                            if (totalPrizes > 0L) {
                                if (result == 3) {
                                    if (this.huX2) {
                                        totalPrizes += this.pot;
                                        result = 4;
                                    }
                                    this.noHuX2();
                                    if (this.moneyType == 1) {
                                        GameUtils.sendSMSToUser(username, "Chuc mung " + username + " da no hu game PokeGo phong " + this.betValue + ". So tien no hu: " + totalPrizes + " Vin");
                                    }
                                    this.pot = this.initPotValue;
                                    this.fund -= this.initPotValue;
                                } else {
                                    this.fund -= totalPrizes;
                                    result = totalPrizes >= (long)(this.betValue * 100) ? (short)2 : 1;
                                }
                            }
                            moneyRes = this.userService.updateMoney(username, totalPrizes, this.moneyTypeStr, Games.POKE_GO.getName(), "Quay PokeGo", this.buildDescription(totalBetValue, totalPrizes, result), fee, Long.valueOf(refernceId), TransType.END_TRANS);
                            long moneyExchange = totalPrizes - (long)this.betValue;
                            if (moneyRes != null && moneyRes.isSuccess()) {
                                currentMoney = moneyRes.getCurrentMoney();
                                if (this.moneyType == 1 && moneyExchange >= (long)BroadcastMessageServiceImpl.MIN_MONEY) {
                                    this.broadcastMsgService.putMessage(Games.POKE_GO.getId(), username, moneyExchange);
                                }
                            }
                            linesWin = builderLinesWin.toString();
                            prizesOnLine = builderPrizesOnLine.toString();
                            msg.matrix = PokeGoUtils.matrixToString(matrix);
                            msg.linesWin = linesWin;
                            msg.prize = totalPrizes;
                            try {
                                this.pgService.logPokeGo(refernceId, username, (long)this.betValue, linesStr, linesWin, prizesOnLine, result, totalPrizes, this.moneyType, currentTimeStr);
                                if (result == 3 || result == 4) {
                                    this.pgService.addTop(username, this.betValue, totalPrizes, (int)this.moneyType, currentTimeStr, (int)result);
                                }
                            }
                            catch (InterruptedException awardList) {
                            }
                            catch (TimeoutException awardList) {
                            }
                            catch (IOException awardList) {
                                // empty catch block
                            }
                            this.saveFund();
                            this.savePot();
                        }
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
        msg.result = (byte)result;
        msg.currentMoney = currentMoney;
        long endTime = System.currentTimeMillis();
        long handleTime = endTime - startTime;
        String ratioTime = CommonUtils.getRatioTime((long)handleTime);
        PokeGoUtils.log(refernceId, username, this.betValue, msg.matrix, result, this.moneyType, handleTime, ratioTime, currentTimeStr);
        return msg;
    }

    public short play(User user, String linesStr) {
        String username = user.getName();
        ResultPokeGoMsg msg = this.play(username, linesStr);
        this.sendMessageToUser((BaseMsg)msg, user);
        return msg.result;
    }

    private void saveFund() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdateFundToRoom >= 60000L) {
            try {
                this.mgService.saveFund(this.name, this.fund);
            }
            catch (IOException | InterruptedException | TimeoutException e) {
                Debug.trace((Object[])new Object[]{"MINI POKER: update fund poker error ", e.getMessage()});
            }
            this.lastTimeUpdateFundToRoom = currentTime;
        }
    }

    private void savePot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdatePotToRoom >= 3000L) {
            UpdatePotPokeGoMsg msg = new UpdatePotPokeGoMsg();
            msg.value = this.pot;
            msg.x2 = this.huX2 ? (byte)1 : 0;
            this.sendMessageToRoom(msg);
            this.lastTimeUpdatePotToRoom = currentTime;
            try {
                this.mgService.savePot(this.name, this.pot, this.huX2);
            }
            catch (IOException | InterruptedException | TimeoutException e) {
                Debug.trace((Object[])new Object[]{"MINI POKER: update pot poker error ", e.getMessage()});
            }
        }
    }

    public void updatePotToUser(User user) {
        UpdatePotPokeGoMsg msg = new UpdatePotPokeGoMsg();
        msg.value = this.pot;
        this.sendMessageToUser((BaseMsg)msg, user);
    }

    private boolean checkDieuKienNoHu(String username) {
        try {
            UserModel u = this.userService.getUserByUserName(username);
            return u.isBot();
        }
        catch (Exception u) {
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void gameLoop() {
        Map<String, AutoUserPokeGo> map;
        ArrayList<AutoUserPokeGo> usersPlay = new ArrayList<AutoUserPokeGo>();
        Map<String, AutoUserPokeGo> map2 = map = this.usersAuto;
        synchronized (map2) {
            for (AutoUserPokeGo user : this.usersAuto.values()) {
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
            ArrayList tmp = new ArrayList(usersPlay.subList(fromIndex, toIndex));
            PlayListPokeGoTask task = new PlayListPokeGoTask(tmp);
            this.executor.execute(task);
        }
        usersPlay.clear();
    }

    public void playListPokeGo(List<AutoUserPokeGo> users) {
        for (AutoUserPokeGo user : users) {
            short result = this.play(user.getUser(), user.getLines());
            if (result == 3 || result == 4 || result == 101 || result == 102 || result == 100) {
                this.forceStopAutoPlay(user.getUser());
                continue;
            }
            if (result == 0) {
                user.setMaxCount(4);
                continue;
            }
            user.setMaxCount(8);
        }
        users.clear();
    }

    @Override
    public boolean joinRoom(User user) {
        boolean result = super.joinRoom(user);
        if (result) {
            user.setProperty("MGROOM_POKEGO_INFO", this);
        }
        return result;
    }

    public void startHuX2() {
        Debug.trace((this.gameName + " start hu X2"));
        this.countHu = 1;
        this.sv.setValue(this.name + "_count_hu", this.countHu);
        if (this.moneyType == 1 && this.betValue == 100) {
            this.huX2 = true;
        }
    }

    public void stopHuX2() {
        Debug.trace((this.gameName + " stop hu x2"));
        this.countHu = -1;
        this.countNoHuX2 = 0;
        this.huX2 = false;
        this.sv.setValue(this.name + "_count_hu", this.countHu);
        this.sv.setValue(this.name + "_count_no_hu_x2", this.countNoHuX2);
    }

    public void noHuX2() {
        if (this.countHu > -1) {
            ++this.countHu;
            this.sv.setValue(this.name + "_count_hu", this.countHu);
            if (this.huX2) {
                ++this.countNoHuX2;
                this.sv.setValue(this.name + "_count_no_hu_x2", this.countNoHuX2);
                Debug.trace((this.gameName + " No hu X2: " + this.countHu + " , huX2= " + this.countNoHuX2));
                if (this.betValue == 100 && this.countNoHuX2 >= 10) {
                    PokeGoModule.stopX2();
                    this.stopHuX2();
                }
                if (this.betValue == 1000 && this.countNoHuX2 >= 1) {
                    PokeGoModule.stopX2();
                    this.stopHuX2();
                }
            }
            this.calculatHuX2();
        }
    }

    private void calculatHuX2() {
        if (this.countHu > -1 && this.moneyType == 1) {
            if (this.betValue == 100) {
                this.huX2 = this.countHu % 4 == 1 && this.countNoHuX2 < 10;
            } else if (this.betValue == 1000) {
                this.huX2 = this.countHu == 3 && this.countNoHuX2 < 1;
            }
        }
    }

    private String buildDescription(long totalBet, long totalPrizes, short result) {
        if (totalBet == 0L) {
            return this.resultToString(result) + ": " + totalPrizes;
        }
        return "Quay: " + totalBet + ", " + this.resultToString(result) + ": " + totalPrizes;
    }

    private String resultToString(short result) {
        switch (result) {
            case 3: {
                return "N\u1ed5 h\u0169";
            }
            case 4: {
                return "N\u1ed5 h\u0169 X2";
            }
            case 1: {
                return "Th\u1eafng";
            }
            case 2: {
                return "Th\u1eafng l\u1edbn";
            }
        }
        return "Tr\u01b0\u1ee3t";
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                MGRoomPokeGo.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public class ResultPokeGo {
        public static final short LOI_HE_THONG = 100;
        public static final short DAT_CUOC_KHONG_HOP_LE = 101;
        public static final short KHONG_DU_TIEN = 102;
        public static final short TRUOT = 0;
        public static final short THANG = 1;
        public static final short THANG_LON = 2;
        public static final short NO_HU = 3;
        public static final short NO_HU_X2 = 4;
    }

    private final class PlayListPokeGoTask
    extends Thread {
        private List<AutoUserPokeGo> users;

        private PlayListPokeGoTask(List<AutoUserPokeGo> users) {
            this.users = users;
            this.setName("AutoPlayPokeGo");
        }

        @Override
        public void run() {
            MGRoomPokeGo.this.playListPokeGo(this.users);
        }
    }

}

