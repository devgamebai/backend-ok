/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.engine.sessions.ISession
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.entities.report.ReportMoneySystemModel
 *  com.vinplay.dal.service.BotService
 *  com.vinplay.dal.service.GameBaiService
 *  com.vinplay.dal.service.impl.BotServiceImpl
 *  com.vinplay.dal.service.impl.GameBaiServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 */
package game.modules.bot;

import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.service.BotService;
import com.vinplay.dal.service.GameBaiService;
import com.vinplay.dal.service.impl.BotServiceImpl;
import com.vinplay.dal.service.impl.GameBaiServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.entities.PlayerInfo;
import game.modules.bot.Bot;
import game.modules.bot.BotConfig;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.utils.GameUtils;
import game.utils.NumberUtils;
import game.xocdia.conf.XocDiaBetBotModel;
import game.xocdia.conf.XocDiaConfig;
import game.xocdia.conf.XocDiaForceResult;
import game.xocdia.conf.XocDiaRoomBotModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BotXocDiaManager {
    private Random rd = new Random();
    private BotService botService = new BotServiceImpl();
    private UserService userService = new UserServiceImpl();
    private GameBaiService gbSer = new GameBaiServiceImpl();
    private final Runnable botLoopTask;
    private ScheduledFuture<?> task;
    private static BotXocDiaManager botMgr = null;
    private Map<Integer, Bot> allBotList;
    private List<GameRoom> regJoinRooms;
    private Map<Integer, XocDiaRoomBotModel> roomBotMap;
    private Set<Integer> botBankerList;
    private int timeCheck;
    private int timeAtJoin;
    private Long totalFeeUser;
    private long totalReveneuUser;
    private int gameIdStart;
    private long updateTime;

    public static BotXocDiaManager instance() {
        if (botMgr == null) {
            botMgr = new BotXocDiaManager();
        }
        return botMgr;
    }

    private BotXocDiaManager() {
        this.botLoopTask = new BotLoopTask();
        this.allBotList = new HashMap<Integer, Bot>();
        this.regJoinRooms = new LinkedList<GameRoom>();
        this.roomBotMap = new HashMap<Integer, XocDiaRoomBotModel>();
        this.botBankerList = new HashSet<Integer>();
        this.init();
    }

    public void init() {
        try {
            this.totalFeeUser = 0L;
            if (!GameUtils.isBot) {
                return;
            }
            List<String> listNickName = BotConfig.instance().getListBotNames();
            int i = 1;
            if (listNickName != null) {
                for (String s : listNickName) {
                    UserModel userModel = this.botService.login(s);
                    if (userModel == null || !userModel.isBot()) continue;
                    User user = this.createBotUser(s);
                    user.setId((int)userModel.getId());
                    Bot bot = new Bot(user);
                    this.allBotList.put(i, bot);
                    ++i;
                    long money = (int)this.userService.getMoneyUserCache(user.getName(), "vin");
                    long addMoney = 0;
                    if (money < (long)XocDiaConfig.moneyBotMin) {
                        addMoney = NumberUtils.randomIntLimit(XocDiaConfig.moneyBotMin, XocDiaConfig.moneyBotMax);
                    } else if (money > (long)XocDiaConfig.moneyBotMax) {
                        addMoney = - this.rd.nextInt((int)money - XocDiaConfig.moneyBotMin);
                    } else if (this.rd.nextInt(4) == 0) {
                        addMoney = (long)NumberUtils.randomIntLimit(XocDiaConfig.moneyBotMin, XocDiaConfig.moneyBotMax) - money;
                    }
                    if (addMoney == 0) continue;
                    this.botService.addMoney(bot.user.getName(), addMoney, "vin", GameUtils.gameName);
                }
            }
            this.task = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.botLoopTask, XocDiaConfig.timeTaskBotStart, XocDiaConfig.timeTaskBotJoinRoom, TimeUnit.SECONDS);
            ReportMoneySystemModel rpModel = this.gbSer.getReportGameToday(GameUtils.gameName);
            this.totalFeeUser = rpModel.fee;
            this.totalReveneuUser = rpModel.revenuePlayGame;
            this.gameIdStart = -1;
            this.updateTime = System.currentTimeMillis();
            this.timeCheck = 0;
            this.timeAtJoin = 0;
            Debug.trace((Object)("Init bot xoc dia, size: " + this.allBotList.size() + ", totalFeeUser: " + this.totalFeeUser + ", totalReveneuUser: " + this.totalReveneuUser + ", updateTime: " + this.updateTime));
        }
        catch (Exception e) {
            Debug.trace((Object)("Init bot xoc dia error: " + e.getMessage()));
            Debug.trace((Object)e);
        }
    }

    private void botLoop() {
        this.joinRooms();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void updateBanker(int roomId, boolean destroy) {
        try {
            Set<Integer> set = this.botBankerList;
            synchronized (set) {
                if (destroy) {
                    this.botBankerList.remove(roomId);
                } else {
                    this.botBankerList.add(roomId);
                }
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean checkRequestBanker() {
        Set<Integer> set = this.botBankerList;
        synchronized (set) {
            try {
                if (NumberUtils.isDoWithRatio(XocDiaConfig.bkRatioRequestBanker) && (long)this.botBankerList.size() < Math.round((double)this.regJoinRooms.size() * XocDiaConfig.bkRatioBanker)) {
                    return true;
                }
            }
            catch (Exception e) {
                Debug.trace((Object)e);
            }
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void regJoinRoom(GameRoom room) {
        List<GameRoom> list = this.regJoinRooms;
        synchronized (list) {
            try {
                if (room != null) {
                    this.regJoinRooms.add(room);
                    int maxBotInMatch = 0;
                    maxBotInMatch = room.setting.rule == 1 ? NumberUtils.randomIntLimit(XocDiaConfig.maxBotInMatchMinGlobal, XocDiaConfig.maxBotInMatchMaxGlobal) : NumberUtils.randomIntLimit(XocDiaConfig.maxBotInMatchMin, XocDiaConfig.maxBotInMatchMax);
                    this.roomBotMap.put(room.getId(), new XocDiaRoomBotModel(maxBotInMatch, System.currentTimeMillis()));
                }
            }
            catch (Exception e) {
                Debug.trace((Object)e);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void joinRooms() {
        List<GameRoom> list = this.regJoinRooms;
        synchronized (list) {
            try {
                if (this.regJoinRooms.size() > 0) {
                    if (this.timeAtJoin <= this.timeCheck) {
                        if (this.timeAtJoin == this.timeCheck) {
                            GameRoom room;
                            int r;
                            for (int i = 0; i <= this.regJoinRooms.size() && !this.joinRoom(room = this.regJoinRooms.get(r = this.rd.nextInt(this.regJoinRooms.size()))); ++i) {
                            }
                        }
                        this.timeCheck = 0;
                        this.timeAtJoin = NumberUtils.randomIntLimit(XocDiaConfig.timeBotJoinRoomMin, XocDiaConfig.timeBotJoinRoomMax);
                    } else {
                        ++this.timeCheck;
                    }
                }
            }
            catch (Exception e) {
                Debug.trace((Object)e);
            }
        }
    }

    /**
     * Force join a number of bots to a specific room immediately.
     * Used when a real user enters an empty room to fill it with bots.
     */
    public void forceJoinBots(GameRoom room, int count) {
        if (room == null || count <= 0) return;
        if (!GameUtils.isBot) {
            Debug.trace((Object)("[BotXocDia] forceJoinBots skipped: isBot=false"));
            return;
        }
        Debug.trace((Object[])new Object[]{"[BotXocDia] forceJoinBots roomId=" + room.getId() + " count=" + count});
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(200); // small delay between joins to avoid race conditions
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (!this.joinRoom(room)) {
                Debug.trace((Object)("[BotXocDia] forceJoinBots stopped after " + i + " bots (no more free bots or room full)"));
                break;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean joinRoom(GameRoom room) {
        try {
            Map<Integer, Bot> map = this.allBotList;
            synchronized (map) {
                User user;
                boolean checkMoney;
                if (GameUtils.isMainTain || !GameUtils.isBot) {
                    return true;
                }
                XocDiaRoomBotModel roomModel = this.roomBotMap.get(room.getId());
                if (roomModel == null || System.currentTimeMillis() - roomModel.getTime() >= (long)(XocDiaConfig.timeChangeMax * 60000)) {
                    int maxBotInMatch = 0;
                    maxBotInMatch = room.setting.rule == 0 ? NumberUtils.randomIntLimit(XocDiaConfig.maxBotInMatchMin, XocDiaConfig.maxBotInMatchMax) : NumberUtils.randomIntLimit(XocDiaConfig.maxBotInMatchMinGlobal, XocDiaConfig.maxBotInMatchMaxGlobal);
                    this.roomBotMap.put(room.getId(), new XocDiaRoomBotModel(maxBotInMatch, System.currentTimeMillis()));
                    roomModel = this.roomBotMap.get(room.getId());
                }
                if (room == null || room.getBotCount() >= roomModel.getMaxBotInRoom()) {
                    return false;
                }
                Bot bot = this.findFitBot(room);
                if (bot != null && (checkMoney = this.checkBotJoinRoom(user = bot.user, room))) {
                    bot.lastRoomId = room.getId();
                    bot.count = 0;
                    GameRoomManager.instance().joinRoom(user, room, false);
                    this.busyBot(bot.user.getName());
                    return true;
                }
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
        return false;
    }

    private void busyBot(String botName) {
        try {
            for (Map.Entry<Integer, Bot> entry : this.allBotList.entrySet()) {
                Bot bot = entry.getValue();
                if (!bot.user.getName().equals(botName)) continue;
                bot.isFree = false;
                this.allBotList.put(entry.getKey(), bot);
                return;
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    private synchronized Bot findFitBot(GameRoom room) {
        try {
            long requireMoney = room.setting.requiredMoney;
            Bot bot = this.getBotFree();
            if (bot != null) {
                long money = this.userService.getCurrentMoneyUserCache(bot.user.getName(), "vin");
                boolean flag = true;
                if (money >= requireMoney && bot.isFree && flag) {
                    return bot;
                }
                if (money < (long)XocDiaConfig.moneyBotMin) {
                    long addMoney = NumberUtils.randomIntLimit(XocDiaConfig.moneyBotMin, XocDiaConfig.moneyBotMax);
                    this.botService.addMoney(bot.user.getName(), addMoney, "vin", GameUtils.gameName);
                }
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
        return null;
    }

    private Bot getBotFree() {
        List<Integer> listIndex = this.getListIndexFree();
        if (listIndex.size() > 0) {
            return this.allBotList.get(this.rd.nextInt(listIndex.size()));
        }
        return null;
    }

    private List<Integer> getListIndexFree() {
        ArrayList<Integer> listIndex = new ArrayList<Integer>();
        for (Map.Entry<Integer, Bot> entry : this.allBotList.entrySet()) {
            if (!entry.getValue().isFree) continue;
            listIndex.add(entry.getKey());
        }
        return listIndex;
    }

    private boolean checkBotJoinRoom(User user, GameRoom room) {
        try {
            GameMoneyInfo moneyInfo = new GameMoneyInfo(user, room.setting);
            boolean result = moneyInfo.startGameUpdateMoney();
            if (result) {
                user.setProperty((Object)"GAME_MONEY_INFO", (Object)moneyInfo);
                return true;
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
        return false;
    }

    private User createBotUser(String nickName) {
        User user = new User(null);
        try {
            user.setConnected(true);
            user.setIsBot(true);
            user.setName(nickName);
            PlayerInfo.getInfo(user);
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
        return user;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void releaseBot(User user) {
        Map<Integer, Bot> map = this.allBotList;
        synchronized (map) {
            try {
                for (Map.Entry<Integer, Bot> entry : this.allBotList.entrySet()) {
                    Bot bot = entry.getValue();
                    if (!bot.user.getName().equals(user.getName())) continue;
                    bot.isFree = true;
                    this.allBotList.put(entry.getKey(), bot);
                    return;
                }
            }
            catch (Exception e) {
                Debug.trace((Object)e);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void destroyGameRoom(GameRoom room) {
        List<GameRoom> list = this.regJoinRooms;
        synchronized (list) {
            try {
                this.regJoinRooms.remove(room);
                this.roomBotMap.remove(room.getId());
            }
            catch (Exception e) {
                Debug.trace((Object)e);
            }
        }
    }

    public List<User> getListInviteBot(int num) {
        ArrayList<User> listInvite;
        block5 : {
            listInvite = new ArrayList<User>();
            try {
                ArrayList<User> listBotFree = new ArrayList<User>();
                for (Map.Entry<Integer, Bot> entry : this.allBotList.entrySet()) {
                    if (!entry.getValue().isFree) continue;
                    listBotFree.add(entry.getValue().user);
                }
                int size = listBotFree.size();
                if (size > num) {
                    for (int i = 0; i < num; ++i) {
                        int index = NumberUtils.randomInt(size);
                        listInvite.add(listBotFree.get(index));
                        listBotFree.remove(index);
                        --size;
                    }
                    break block5;
                }
                return listBotFree;
            }
            catch (Exception e) {
                Debug.trace((Object)e);
            }
        }
        return listInvite;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public XocDiaForceResult getForceResultForBot(int roomId, int gameId, boolean isBankerIsBot, boolean hasBanker, List<XocDiaBetBotModel> potBetList) {
        Long l = this.totalFeeUser;
        synchronized (l) {
            int forceType = -1;
            ArrayList<Byte> listPWin = null;
            try {
                long revenueSystemMin = this.totalFeeUser - (long)XocDiaConfig.revenueSysMin;
                long revenueSystemMax = this.totalFeeUser + (long)XocDiaConfig.revenueSysMax;
                long revenueSystem = - this.totalReveneuUser;
                boolean hasBetVi = false;
                if (hasBanker) {
                    for (XocDiaBetBotModel xdBet : potBetList) {
                        if (xdBet.id <= 1 || xdBet.money <= 0) continue;
                        hasBetVi = true;
                        break;
                    }
                }
                Debug.trace((Object)("" + roomId + " " + gameId + ", getForceResultForBot, revenueSystem: " + revenueSystem + ", revenueSystemMin: " + revenueSystemMin + ", revenueSystemMax: " + revenueSystemMax + ", hasBanker: " + hasBanker + ", isBankerIsBot: " + isBankerIsBot + ", hasBetVi: " + hasBetVi));
                ArrayList<Byte> listPotBotWin = new ArrayList<Byte>();
                ArrayList<Byte> listPotBotLost = new ArrayList<Byte>();
                if (hasBetVi) {
                    int sign = isBankerIsBot ? -1 : 1;
                    for (byte i = 0; i < potBetList.size(); i = (byte)(i + 1)) {
                        XocDiaBetBotModel xdBet = potBetList.get(i);
                        ArrayList<Byte> listPot = new ArrayList();
                        switch (i) {
                            case 0: {
                                listPot.add(Byte.valueOf("0"));
                                xdBet.moneyWin = (long)sign * this.getMoneyWin(listPot, potBetList);
                                break;
                            }
                            case 2: {
                                listPot.add(Byte.valueOf("0"));
                                listPot.add(Byte.valueOf("2"));
                                xdBet.moneyWin = (long)sign * this.getMoneyWin(listPot, potBetList);
                                break;
                            }
                            case 3: {
                                listPot.add(Byte.valueOf("0"));
                                listPot.add(Byte.valueOf("3"));
                                xdBet.moneyWin = (long)sign * this.getMoneyWin(listPot, potBetList);
                                break;
                            }
                            case 4: {
                                listPot.add(Byte.valueOf("1"));
                                listPot.add(Byte.valueOf("4"));
                                xdBet.moneyWin = (long)sign * this.getMoneyWin(listPot, potBetList);
                                break;
                            }
                            case 5: {
                                listPot.add(Byte.valueOf("1"));
                                listPot.add(Byte.valueOf("5"));
                                xdBet.moneyWin = (long)sign * this.getMoneyWin(listPot, potBetList);
                                break;
                            }
                        }
                        if (xdBet.moneyWin >= 0) {
                            listPotBotWin.add(Byte.valueOf(i));
                        } else {
                            listPotBotLost.add(Byte.valueOf(i));
                        }
                        potBetList.set(i, xdBet);
                    }
                    long maxBotWin = 0;
                    long maxBotLost = 0;
                    for (XocDiaBetBotModel xdBet : potBetList) {
                        if (xdBet.moneyWin > maxBotWin) {
                            maxBotWin = xdBet.moneyWin;
                            continue;
                        }
                        if (xdBet.moneyWin >= maxBotLost) continue;
                        maxBotLost = xdBet.moneyWin;
                    }
                    if (maxBotWin > 0 && maxBotLost < 0 && listPotBotWin.size() > 0 && listPotBotLost.size() > 0) {
                        long revenueSystemNewMax = revenueSystem + maxBotWin;
                        long revenueSystemNewMin = revenueSystem + maxBotLost;
                        if (this.totalReveneuUser >= revenueSystemMin || revenueSystem <= revenueSystemMin || revenueSystemNewMin <= revenueSystemMin) {
                            Debug.trace((Object)("" + roomId + " " + gameId + " FORCE Bot Win"));
                            forceType = 3;
                            listPWin = listPotBotWin;
                            if (listPWin.contains(Byte.valueOf("4")) || listPWin.contains(Byte.valueOf("5"))) {
                                listPWin.add(Byte.valueOf("1"));
                            }
                        } else if (revenueSystemNewMin >= revenueSystemMax || revenueSystemNewMax >= revenueSystemMax && revenueSystemNewMin >= revenueSystemMin) {
                            Debug.trace((Object)("" + roomId + " " + gameId + " FORCE User Win"));
                            if (NumberUtils.isDoWithRatio(XocDiaConfig.ratioGetRevenueSysMax)) {
                                forceType = 3;
                                listPWin = listPotBotLost;
                                if (listPWin.contains(Byte.valueOf("4")) || listPWin.contains(Byte.valueOf("5"))) {
                                    listPWin.add(Byte.valueOf("1"));
                                }
                            } else {
                                Debug.trace((Object)("" + roomId + " " + gameId + " FORCE User Win But Without Ratio => NO FORCE"));
                            }
                        }
                    } else {
                        Debug.trace((Object)("" + roomId + " " + gameId + " ERROR get pot win => NO FORCE, maxBotWin: " + maxBotWin + ", maxBotLost: " + maxBotLost + ", listPotBotWin: " + listPotBotWin.size() + ", listPotBotLost: " + listPotBotLost.size()));
                    }
                } else {
                    long moneyDif = potBetList.get((int)0).money - potBetList.get((int)1).money;
                    if (moneyDif != 0) {
                        long revenueSystemNew = revenueSystem - Math.abs(moneyDif);
                        if (this.totalReveneuUser >= revenueSystemMin || revenueSystem <= revenueSystemMin || revenueSystemNew <= revenueSystemMin) {
                            forceType = this.getForceType(moneyDif, true, isBankerIsBot);
                            Debug.trace((Object)("" + roomId + " " + gameId + " FORCE Bot Win, moneyDif: " + moneyDif));
                        } else if (revenueSystemNew >= revenueSystemMax || revenueSystem >= revenueSystemMax && revenueSystemNew >= revenueSystemMin) {
                            if (NumberUtils.isDoWithRatio(XocDiaConfig.ratioGetRevenueSysMax)) {
                                forceType = this.getForceType(moneyDif, false, isBankerIsBot);
                                Debug.trace((Object)("" + roomId + " " + gameId + " FORCE User Win, moneyDif: " + moneyDif));
                            } else {
                                Debug.trace((Object)("" + roomId + " " + gameId + " FORCE User Win But Without Ratio => NO FORCE, moneyDif: " + moneyDif));
                            }
                        }
                    } else {
                        Debug.trace((Object)("" + roomId + " " + gameId + " moneyDif = 0 => NO FORCE"));
                    }
                }
            }
            catch (Exception e) {
                Debug.trace((Object)e);
            }
            return new XocDiaForceResult(forceType, listPWin);
        }
    }

    private long getMoneyWin(List<Byte> listPot, List<XocDiaBetBotModel> potBetList) {
        long moneyWin = 0;
        for (byte j = 0; j < potBetList.size(); j = (byte)(j + 1)) {
            if (listPot.contains(Byte.valueOf(j))) {
                moneyWin += Math.round((double)potBetList.get((int)j).money * (potBetList.get((int)j).ratio - 1.0) * 0.98);
                continue;
            }
            moneyWin -= potBetList.get((int)j).money;
        }
        return moneyWin;
    }

    private int getForceType(long moneyDif, boolean botWin, boolean isBankerIsBot) {
        if (moneyDif > 0) {
            if (botWin) {
                if (isBankerIsBot) {
                    return 1;
                }
                return 0;
            }
            if (isBankerIsBot) {
                return 0;
            }
            return 1;
        }
        if (botWin) {
            if (isBankerIsBot) {
                return 0;
            }
            return 1;
        }
        if (isBankerIsBot) {
            return 1;
        }
        return 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public synchronized void finishGame(int roomId, int gameId, long totalFeeUserInGame, long totalReveneuUserInGame) {
        Long l = this.totalFeeUser;
        synchronized (l) {
            try {
                if (gameId >= this.gameIdStart) {
                    Debug.trace((Object)("" + roomId + " " + gameId + ", finishGame, totalFeeUserInGame: " + totalFeeUserInGame + ", totalReveneuUserInGame: " + totalReveneuUserInGame));
                    BotXocDiaManager botXocDiaManager = this;
                    botXocDiaManager.totalFeeUser = botXocDiaManager.totalFeeUser + totalFeeUserInGame;
                    this.totalReveneuUser += totalReveneuUserInGame;
                }
                Debug.trace((Object)("totalFeeUser: " + this.totalFeeUser));
                Debug.trace((Object)("totalReveneuUser: " + this.totalReveneuUser));
            }
            catch (Exception e) {
                Debug.trace((Object)e);
            }
        }
    }

    public synchronized void startNewGame(int roomId, int gameId) {
        this.updateTime(roomId, gameId);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateTime(int roomId, int gameId) {
        Long l = this.totalFeeUser;
        synchronized (l) {
            if (this.updateTime <= DateTimeUtils.getStartTimeToDayAsLong()) {
                this.gameIdStart = gameId;
                this.totalFeeUser = 0L;
                this.totalReveneuUser = 0;
                Debug.trace((Object)("" + roomId + " " + gameId + ", updateTime, gameIdStart: " + gameId));
                Debug.trace((Object)("totalFeeUser: " + this.totalFeeUser));
                Debug.trace((Object)("totalReveneuUser: " + this.totalReveneuUser));
            }
            this.updateTime = System.currentTimeMillis();
        }
    }

    private class BotLoopTask
    implements Runnable {
        private BotLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                if (!GameUtils.isBot) {
                    if (BotXocDiaManager.this.task != null && !BotXocDiaManager.this.task.isCancelled()) {
                        BotXocDiaManager.this.task.cancel(false);
                    }
                } else {
                    BotXocDiaManager.this.botLoop();
                }
            }
            catch (Throwable e) {
                Debug.trace((Object)e);
            }
        }
    }

}

