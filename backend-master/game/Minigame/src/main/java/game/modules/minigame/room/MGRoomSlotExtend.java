/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.common.business.Debug
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.service.BroadcastMessageService
 *  com.vinplay.dal.service.CacheService
 *  com.vinplay.dal.service.MiniGameService
 *  com.vinplay.dal.service.SlotMachineService
 *  com.vinplay.dal.service.impl.BroadcastMessageServiceImpl
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.dal.service.impl.MiniGameServiceImpl
 *  com.vinplay.dal.service.impl.SlotMachineServiceImpl
 *  com.vinplay.usercore.dao.impl.UserDaoImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.CommonUtils
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.apache.log4j.Logger
 */
package game.modules.minigame.room;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.common.business.Debug;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.service.BroadcastMessageService;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.SlotMachineService;
import com.vinplay.dal.service.impl.BroadcastMessageServiceImpl;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import com.vinplay.dal.service.impl.SlotMachineServiceImpl;
import com.vinplay.usercore.dao.impl.UserDaoImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.CommonUtils;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.SlotExtendService;
import game.SlotExtendServiceImplement;
import game.modules.minigame.GroupItem;
import game.modules.minigame.LineWin;
import game.modules.minigame.Slot3x3ExtendModule;
import game.modules.minigame.cmd.send.slot3x3.ForceStopAutoPlaySlotExtendMsg;
import game.modules.minigame.cmd.send.slot3x3.ResultSlotExtendMsg;
import game.modules.minigame.cmd.send.slot3x3.UpdatePotSlotExtend;
import game.modules.minigame.entities.AutoUserSlotExtend;
import game.modules.minigame.room.MGRoom;
import game.modules.minigame.utils.RandomUtil;
import game.modules.minigame.utils.SlotExtendUtils;
import game.utils.ConfigGame;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class MGRoomSlotExtend
extends MGRoom {
    private long pot;
    private long fund;
    private long initPotValue;
    private int betValue;
    private short moneyType;
    private String moneyTypeStr;
    private UserService userService = new UserServiceImpl();
    private MiniGameService mgService = new MiniGameServiceImpl();
    private SlotExtendService slexService = new SlotExtendServiceImplement();
    private SlotMachineService slotMachineService = new SlotMachineServiceImpl();
    private BroadcastMessageService broadcastMsgService = new BroadcastMessageServiceImpl();
    private final Runnable gameLoopTask;
    private Map<String, AutoUserSlotExtend> usersAuto;
    private long lastTimeUpdatePotToRoom;
    private long lastTimeUpdateFundToRoom;
    private ThreadPoolExecutor executor;
    private int countHu;
    private int countNoHuX2;
    private boolean huX2;
    protected CacheService sv;
    public static final int MINI_GAME_WIN_TYPE_FAIL = 0;
    public static final int MINI_GAME_WIN_TYPE_NORMAL = 1;
    public static final int MINI_GAME_WIN_TYPE_BIG_WIN = 2;
    public static final int MINI_GAME_WIN_TYPE_JACKPOT_BROKEN = 3;
    public static final int MAX_NUMBER_GET_FAIL = 15;
    public static final int DIAMOND_MAX_NUMBER_RANDOM = 5;
    private final int[] arrItemValue = new int[]{1, 2, 3, 4, 5, 6};
    private final int[] arrItemValue1 = new int[]{1, 6, 5, 3, 4, 5, 2, 5, 3, 4, 3, 5, 2, 5, 4, 6, 4, 5, 6, 2};
    private final int[] arrItemValue2 = new int[]{6, 5, 2, 5, 3, 6, 5, 5, 3, 4, 3, 2, 6, 6, 6, 1, 3, 5, 2, 4};
    private final int[] arrItemValue3 = new int[]{3, 5, 2, 5, 3, 6, 5, 2, 6, 4, 4, 5, 5, 6, 6, 3, 6, 2, 1, 5};
    private int[][] retVal = new int[][]{{3, 1, 5, 3, 5, 3, 4, 5, 2}, {3, 1, 5, 3, 5, 5, 4, 5, 2}, {3, 1, 5, 4, 5, 6, 4, 5, 5}, {3, 1, 5, 3, 5, 1, 4, 5, 5}, {3, 1, 5, 3, 6, 5, 4, 5, 5}, {3, 4, 5, 3, 5, 3, 4, 4, 2}, {3, 4, 5, 3, 2, 3, 4, 4, 2}};
    int[][] arrLines = new int[][]{{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 7, 2}, {6, 1, 8}, {0, 4, 2}, {0, 4, 8}, {6, 4, 2}, {3, 7, 5}, {3, 1, 5}, {6, 4, 8}, {0, 1, 5}, {3, 4, 8}, {3, 4, 2}, {6, 7, 5}, {3, 1, 2}, {6, 4, 5}, {0, 4, 5}, {3, 7, 8}, {0, 7, 5}, {0, 1, 8}, {0, 7, 8}, {6, 1, 2}, {3, 1, 8}, {3, 7, 2}, {6, 1, 5}, {6, 7, 2}};
    int[] arrMutil = new int[]{1, 3, 5, 10};

    public MGRoomSlotExtend(String name, short moneyType, long pot, long fund, int betValue, long initPotValue) {
        super(name);
        this.gameName = "candy";
        this.gameLoopTask = new GameLoopTask();
        this.usersAuto = new HashMap<String, AutoUserSlotExtend>();
        this.lastTimeUpdatePotToRoom = 0L;
        this.lastTimeUpdateFundToRoom = 0L;
        this.countHu = -1;
        this.countNoHuX2 = 0;
        this.huX2 = false;
        this.sv = new CacheServiceImpl();
        this.moneyType = moneyType;
        this.moneyTypeStr = this.moneyType == 1 ? "vin" : "xu";
        this.executor = moneyType == 1 ? (ThreadPoolExecutor)Executors.newFixedThreadPool(ConfigGame.getIntValue("slot_extend_thread_pool_per_room_vin")) : (ThreadPoolExecutor)Executors.newFixedThreadPool(ConfigGame.getIntValue("slot_extend_thread_pool_per_room_xu"));
        this.pot = pot;
        CacheServiceImpl cacheService = new CacheServiceImpl();
        cacheService.setValue(name, pot);
        this.fund = fund;
        this.betValue = betValue;
        this.initPotValue = initPotValue;
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 10, 1, TimeUnit.SECONDS);
        try {
            this.countHu = this.sv.getValueInt(name + "_count_hu");
            this.countNoHuX2 = this.sv.getValueInt(name + "_count_no_hu_x2");
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
    public void autoPlay(User user, long gold) {
        Map<String, AutoUserSlotExtend> map;
        Map<String, AutoUserSlotExtend> map2 = map = this.usersAuto;
        synchronized (map2) {
            if (this.usersAuto.containsKey(user.getName())) {
                AutoUserSlotExtend entry = this.usersAuto.get(user.getName());
                this.forceStopAutoPlay(entry.getUser());
            }
            this.usersAuto.put(user.getName(), new AutoUserSlotExtend(user, gold));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void stopAutoPlay(User user) {
        Map<String, AutoUserSlotExtend> map;
        Map<String, AutoUserSlotExtend> map2 = map = this.usersAuto;
        synchronized (map2) {
            if (this.usersAuto.containsKey(user.getName()) && this.usersAuto.get(user.getName()).getUser().getId() == user.getId()) {
                this.usersAuto.remove(user.getName());
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void forceStopAutoPlay(User user) {
        Map<String, AutoUserSlotExtend> map;
        Map<String, AutoUserSlotExtend> map2 = map = this.usersAuto;
        synchronized (map2) {
            this.usersAuto.remove(user.getName());
            ForceStopAutoPlaySlotExtendMsg msg = new ForceStopAutoPlaySlotExtendMsg();
            this.sendMessageToUser((BaseMsg)msg, user);
        }
    }

    public synchronized ResultSlotExtendMsg play(String username, long gold) {
        long startTime = System.currentTimeMillis();
        long refernceId = Slot3x3ExtendModule.getNewRefenceId();
        String currentTimeStr = DateTimeUtils.getCurrentTime();
        short result = 0;
        long currentMoney = this.userService.getMoneyUserCache(username, this.moneyTypeStr);
        boolean isValid = false;
        long prizeAmount = 0L;
        int[] validShowItem = new int[9];
        boolean isJackpotBroken = false;
        ArrayList<LineWin> listLineWin = new ArrayList<LineWin>();
        StringBuilder strLineWin = new StringBuilder();
        int rd1 = 1;
        int rd2 = 1;
        int mutil = 1;
        int winType = 0;
        if (gold == (long)this.betValue) {
            if (gold > 0L) {
                if (gold <= currentMoney) {
                    MoneyResponse moneyRes = this.userService.updateMoney(username, -gold, this.moneyTypeStr, "SlotExtend", "Quay SlotExtend", "\u0110\u1eb7t c\u01b0\u1ee3c SlotExtend", 0L, Long.valueOf(refernceId), TransType.START_TRANS);
                    if (moneyRes != null && moneyRes.isSuccess()) {
                        block39: {
                            long fee = gold * 2L / 100L;
                            long moneyToPot = gold / 100L;
                            long moneyToFund = gold - fee - moneyToPot;
                            this.fund += moneyToFund;
                            this.pot += moneyToPot;
                            block8: for (int i = 0; !isValid && i < 5; ++i) {
                                boolean isUserJackpot = this.isUserJackpot(username);
                                int[] showItem = this.initShuffleCollectItem();
                                if (isUserJackpot) {
                                    showItem = this.getCollectItemJackpot();
                                }
                                long checkPrizeAmount = 0L;
                                ArrayList<LineWin> checkLineWin = new ArrayList<LineWin>();
                                boolean isJackpot = false;
                                for (int j = 0; j < this.arrLines.length; ++j) {
                                    GroupItem groupItem = null;
                                    int line = j;
                                    int retryCount = 0;
                                    int maxRetries = 10000;
                                    while (retryCount < maxRetries) {
                                        rd1 = this.arrMutil[RandomUtil.randInt(4)];
                                        rd2 = this.arrMutil[RandomUtil.randInt(4)];
                                        mutil = 1;
                                        if (rd1 != 1) {
                                            mutil = rd1 * rd2;
                                        }
                                        if ((groupItem = new GroupItem(this.getItemValueByLineIndex(showItem, line), mutil)).isJackpot() && isUserJackpot || !groupItem.isJackpot()) break;
                                        if (++retryCount % 100 != 0) continue;
                                        try {
                                            Thread.sleep(1L);
                                        }
                                        catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    }
                                    if (groupItem == null || retryCount >= maxRetries) {
                                        rd1 = this.arrMutil[RandomUtil.randInt(4)];
                                        rd2 = this.arrMutil[RandomUtil.randInt(4)];
                                        mutil = 1;
                                        if (rd1 != 1) {
                                            mutil = rd1 * rd2;
                                        }
                                        groupItem = new GroupItem(this.getItemValueByLineIndex(showItem, line), mutil);
                                    }
                                    if (groupItem.isJackpot()) {
                                        isJackpot = true;
                                        if (!this.isJackpotBroken()) break;
                                        isJackpotBroken = true;
                                        prizeAmount = this.initPotValue;
                                        LineWin lineWin = new LineWin();
                                        lineWin.setLine(line);
                                        lineWin.setJackpot(true);
                                        lineWin.setPrizeAmount(prizeAmount);
                                        listLineWin.add(lineWin);
                                        strLineWin.append(line + ",");
                                        validShowItem = showItem;
                                        break block8;
                                    }
                                    if (groupItem.getPrizeAmount() <= 0L) continue;
                                    long itemPrizeAmount = groupItem.getPrizeAmount();
                                    long prizeAmount1 = groupItem.getPrizeAmount() * (long)this.betValue;
                                    LineWin lineWin = new LineWin();
                                    lineWin.setLine(line);
                                    checkPrizeAmount += itemPrizeAmount;
                                    lineWin.setPrizeAmount(prizeAmount1);
                                    checkLineWin.add(lineWin);
                                }
                                if (isJackpot) continue;
                                prizeAmount = checkPrizeAmount * (long)this.betValue * (long)mutil;
                                if (prizeAmount >= (long)(this.betValue * 100)) {
                                    if (prizeAmount <= this.fund && prizeAmount <= this.fund * 50L / 100L) {
                                        validShowItem = showItem;
                                        isValid = true;
                                        listLineWin = checkLineWin;
                                        continue;
                                    }
                                    prizeAmount = 0L;
                                    continue;
                                }
                                if (prizeAmount > 0L && prizeAmount <= this.fund * 80L / 100L) {
                                    validShowItem = showItem;
                                    isValid = true;
                                    listLineWin = checkLineWin;
                                    continue;
                                }
                                prizeAmount = 0L;
                            }
                            boolean bigWin = false;
                            if (prizeAmount >= (long)this.betValue * 100L) {
                                bigWin = true;
                            }
                            refernceId = Slot3x3ExtendModule.getNewRefenceId();
                            if (isJackpotBroken) {
                                winType = 3;
                            } else if (isValid && bigWin) {
                                winType = 2;
                            } else if (isValid) {
                                winType = 1;
                            } else {
                                prizeAmount = 0L;
                                winType = 0;
                                validShowItem = this.getCollectItemFail();
                            }
                            moneyRes = this.userService.updateMoney(username, prizeAmount, this.moneyTypeStr, "SlotExtend", "Quay SlotExtend", this.buildDescription(this.betValue, prizeAmount, result), fee, Long.valueOf(refernceId), TransType.END_TRANS);
                            double moneyExchange = prizeAmount - (long)this.betValue;
                            if (moneyRes != null && moneyRes.isSuccess()) {
                                currentMoney = moneyRes.getCurrentMoney();
                                if (this.moneyType == 1 && moneyExchange >= (double)BroadcastMessageServiceImpl.MIN_MONEY) {
                                    this.broadcastMsgService.putMessage(31, username, (long)moneyExchange);
                                }
                            }
                            try {
                                this.slexService.logSlotExtend(refernceId, username, this.betValue, "20", Arrays.toString(validShowItem), "prizeAmount", result, prizeAmount, this.moneyType, currentTimeStr);
                                if (result != 3 && result != 4) break block39;
                                HazelcastInstance client = HazelcastClientFactory.getInstance();
                                IMap userMap = client.getMap("users");
                                UserModel model = null;
                                String displayName = username;
                                if (userMap.containsKey(username)) {
                                    model = (UserModel)userMap.get(displayName);
                                    displayName = model.getClient() != null && model.getClient() != "" ? "[" + model.getClient() + "] " + username : "[X] " + username;
                                } else {
                                    UserDaoImpl dao = new UserDaoImpl();
                                    try {
                                        model = dao.getUserByNickName(username);
                                        displayName = model.getClient() != null && model.getClient() != "" ? "[" + model.getClient() + "] " + username : "[X] " + username;
                                    }
                                    catch (SQLException sQLException) {
                                        // empty catch block
                                    }
                                }
                                this.slexService.addTop(displayName, this.betValue, prizeAmount, this.moneyType, currentTimeStr, result);
                            }
                            catch (InterruptedException interruptedException) {
                            }
                            catch (TimeoutException timeoutException) {
                            }
                            catch (IOException iOException) {
                                // empty catch block
                            }
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
        long endTime = System.currentTimeMillis();
        long handleTime = endTime - startTime;
        String ratioTime = CommonUtils.getRatioTime((long)handleTime);
        SlotExtendUtils.log(refernceId, username, this.betValue, Arrays.toString(validShowItem), result, this.moneyType, handleTime, ratioTime, currentTimeStr);
        if (isJackpotBroken) {
            this.pot = this.initPotValue;
            this.fund -= this.initPotValue;
        } else {
            this.fund -= prizeAmount;
        }
        ResultSlotExtendMsg msg = this.sendDiamondNewSpinSuccess(result, (int)refernceId, mutil, rd1, rd2, listLineWin, validShowItem, prizeAmount, winType, currentMoney);
        return msg;
    }

    private ResultSlotExtendMsg sendDiamondNewSpinSuccess(int result, int spinId, int mutil, int mutil1, int mutil2, List<LineWin> listLineWin, int[] showItem, long moneyPrize, int winType, long currMoney) {
        ResultSlotExtendMsg res = new ResultSlotExtendMsg();
        res.result = result;
        res.prize = moneyPrize;
        res.mutil = mutil;
        res.mutil1 = mutil1;
        res.mutil2 = mutil2;
        res.winType = winType;
        res.spinId = spinId;
        res.listLineWin = listLineWin;
        res.showItem = showItem;
        res.currMoney = currMoney;
        return res;
    }

    public short play(User user, long betvalue) {
        String username = user.getName();
        ResultSlotExtendMsg msg = this.play(username, betvalue);
        this.sendMessageToUser((BaseMsg)msg, user);
        return (short)msg.result;
    }

    private void saveFund() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdateFundToRoom >= 60000L) {
            try {
                this.mgService.saveFund(this.name, this.fund);
            }
            catch (IOException | InterruptedException | TimeoutException e) {
                Debug.trace((Object[])new Object[]{"Slot extend: update fund Slot extend error ", e.getMessage()});
            }
            this.lastTimeUpdateFundToRoom = currentTime;
        }
    }

    private void savePot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastTimeUpdatePotToRoom >= 3000L) {
            UpdatePotSlotExtend msg = new UpdatePotSlotExtend();
            msg.value1 = this.pot;
            msg.value2 = this.pot;
            msg.value3 = this.pot;
            this.sendMessageToRoom(msg);
            this.lastTimeUpdatePotToRoom = currentTime;
            try {
                this.mgService.savePot(this.name, this.pot, this.huX2);
            }
            catch (IOException | InterruptedException | TimeoutException e) {
                Debug.trace((Object[])new Object[]{"Slot extend: update pot Slot extend error ", e.getMessage()});
            }
        }
    }

    public void updatePotToUser(User user) {
        UpdatePotSlotExtend msg = new UpdatePotSlotExtend();
        msg.value1 = this.pot;
        msg.value2 = this.pot;
        msg.value3 = this.pot;
        this.sendMessageToUser((BaseMsg)msg, user);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void gameLoop() {
        Map<String, AutoUserSlotExtend> map;
        ArrayList<AutoUserSlotExtend> usersPlay = new ArrayList<AutoUserSlotExtend>();
        Map<String, AutoUserSlotExtend> map2 = map = this.usersAuto;
        synchronized (map2) {
            for (AutoUserSlotExtend user : this.usersAuto.values()) {
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
            PlayListSlotExtendTask task = new PlayListSlotExtendTask(tmp);
            this.executor.execute(task);
        }
        usersPlay.clear();
    }

    public void playListSlotExtend(List<AutoUserSlotExtend> users) {
        for (AutoUserSlotExtend user : users) {
            short result = this.play(user.getUser(), user.getBet());
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
            user.setProperty("MGROOM_SLOT_EXTEND_INFO", this);
        }
        return result;
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

    private int[] getCollectItemJackpot() {
        int[] collection = new int[9];
        for (int j = 0; j < 15; ++j) {
            for (int i = 0; i < 9; ++i) {
                collection[i] = this.arrItemValue[RandomUtil.randInt(0, this.arrItemValue.length - 1)];
            }
            for (int[] line : this.arrLines) {
                int[] arrItem = new int[]{collection[line[0]], collection[line[1]], collection[line[2]]};
                GroupItem groupItem = new GroupItem(arrItem, 100);
                if (!groupItem.isJackpot()) continue;
                return collection;
            }
        }
        int[] retVal = new int[]{1, 1, 1, 1, 5, 3, 4, 5, 4};
        return retVal;
    }

    private boolean isJackpotBroken() {
        return this.fund > this.initPotValue * 2L;
    }

    private int[] initShuffleCollectItem() {
        int[] collection = new int[9];
        try {
            int id1 = RandomUtil.randInt(0, this.arrItemValue1.length);
            int id2 = RandomUtil.randInt(0, this.arrItemValue2.length);
            int id3 = RandomUtil.randInt(0, this.arrItemValue3.length);
            collection[0] = this.arrItemValue1[id1];
            collection[1] = this.arrItemValue1[(id1 + 1) % this.arrItemValue1.length];
            collection[2] = this.arrItemValue1[(id1 + 2) % this.arrItemValue1.length];
            collection[3] = this.arrItemValue2[id2];
            collection[4] = this.arrItemValue2[(id2 + 1) % this.arrItemValue2.length];
            collection[5] = this.arrItemValue2[(id2 + 2) % this.arrItemValue2.length];
            collection[6] = this.arrItemValue3[id3];
            collection[7] = this.arrItemValue3[(id3 + 1) % this.arrItemValue3.length];
            collection[8] = this.arrItemValue3[(id3 + 2) % this.arrItemValue3.length];
        }
        catch (Exception e) {
            int[] v = new int[]{3, 4, 5, 3, 2, 3, 4, 4, 2};
            return v;
        }
        return collection;
    }

    private int[] getCollectItemFail() {
        int[] collection = new int[9];
        for (int j = 0; j < 15; ++j) {
            for (int i = 0; i < 9; ++i) {
                collection[i] = this.arrItemValue[RandomUtil.randInt(0, this.arrItemValue.length - 1)];
            }
            double prizeAmount = 0.0;
            for (int[] line : this.arrLines) {
                int[] arrItem = new int[]{collection[line[0]], collection[line[1]], collection[line[2]]};
                GroupItem groupItem = new GroupItem(arrItem);
                if (groupItem.isJackpot()) {
                    prizeAmount += 1.0;
                    break;
                }
                prizeAmount += (double)groupItem.getPrizeAmount();
            }
            if (!(prizeAmount <= 0.0)) continue;
            return collection;
        }
        int rd = RandomUtil.randInt(0, 6);
        int[] rv = this.retVal[rd];
        return rv;
    }

    private int[] getItemValueByLineIndex(int[] showItem, int index) {
        int[] line = this.arrLines[index];
        int[] retVal = new int[]{showItem[line[0]], showItem[line[1]], showItem[line[2]]};
        return retVal;
    }

    public boolean isUserJackpot(String userName) {
        return this.slotMachineService.isSetJackpotForUser(this.gameName, userName, this.betValue);
    }

    @Override
    public void cleanup() {
        if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.shutdown();
            try {
                if (!this.executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                    if (!this.executor.awaitTermination(2L, TimeUnit.SECONDS)) {
                        Logger.getLogger((String)"backend").warn(("Thread pool did not terminate for room: " + this.name));
                    }
                }
            }
            catch (InterruptedException e) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        super.cleanup();
    }

    private final class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                MGRoomSlotExtend.this.gameLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private final class PlayListSlotExtendTask
    extends Thread {
        private List<AutoUserSlotExtend> users;

        private PlayListSlotExtendTask(List<AutoUserSlotExtend> users) {
            this.users = users;
            this.setName("AutoPlaySlotExtend");
        }

        @Override
        public void run() {
            MGRoomSlotExtend.this.playListSlotExtend(this.users);
        }
    }
}

