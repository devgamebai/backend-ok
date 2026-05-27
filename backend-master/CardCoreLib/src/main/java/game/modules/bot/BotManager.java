/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.engine.sessions.ISession
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.common.business.CommonHandle
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
 *  org.json.JSONObject
 */
package game.modules.bot;

import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.util.TaskScheduler;
import bitzero.util.common.business.CommonHandle;
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
import game.entities.PlayerInfo;
import game.modules.bot.Bot;
import game.modules.bot.BotConfig;
import game.modules.bot.BotMoneyInfo;
import game.modules.bot.BotXocDiaManager;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.player.entities.VipMoneyInfo;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import game.utils.NumberUtils;
import game.xocdia.conf.XocDiaGameUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class BotManager {
    public static final int NOT_BALANCE = 0;
    public static final int UP_BALANCE = 1;
    public static final int DOWN_BALANCE = 2;
    public int countDown = 0;
    public int balanceMode = 0;
    public long maxWin = 1000000;
    public long maxLost = 1000000;
    public Random rd = new Random();
    BotService botService = new BotServiceImpl();
    public UserService userService = new UserServiceImpl();
    public GameBaiService gameService = new GameBaiServiceImpl();
    private static BotManager botMgr = null;
    private List<Bot> allBotList = new LinkedList<Bot>();
    private List<Bot> allBotVip = new LinkedList<Bot>();
    private ScheduledFuture<?> task;
    private final Runnable botLoopTask;
    private Set<GameRoom> regJoinRooms;

    private BotManager() {
        this.botLoopTask = new BotLoopTask();
        this.regJoinRooms = new HashSet<GameRoom>();
        if (XocDiaGameUtils.isXocDia()) {
            BotXocDiaManager.instance();
        } else {
            this.init();
        }
    }

    public int getRandomNumber(int range) {
        return this.rd.nextInt(range);
    }

    public static BotManager instance() {
        if (botMgr == null) {
            botMgr = new BotManager();
        }
        return botMgr;
    }

    private synchronized Bot findFitBot(GameRoom room) {
        int from;
        if (!GameUtils.isBot) {
            return null;
        }
        long requireMoney = room.setting.requiredMoney;
        int size = this.allBotList.size();
        for (int i = from = this.rd.nextInt((int)size); i < size + from; ++i) {
            boolean flag;
            int index = i % size;
            Bot bot = this.allBotList.get(index);
            long money = this.userService.getMoneyUserCache(bot.user.getName(), "vin");
            if (money > 10000000) {
                int moneyRemain = NumberUtils.randomIntLimit(500000, 3000000);
                MoneyResponse mnres = this.botService.addMoney(bot.user.getName(), (long)moneyRemain - money, "vin", GameUtils.gameName);
                if (mnres.isSuccess()) {
                    money = (int)mnres.getMoneyUse();
                }
            }
            boolean bl = flag = bot.lastRoomId != room.getId();
            if (money >= requireMoney && bot.isFree && flag) {
                bot.isFree = false;
                return bot;
            }
            if (money >= 10000) continue;
            this.botService.addMoney(bot.user.getName(), 100000, "vin", GameUtils.gameName);
            LoggerUtils.info("bot", "findFitBot:", "addMoney", bot.user.getName());
        }
        LoggerUtils.info("bot", "findFitBot:", "FAILED");
        return null;
    }

    public synchronized Bot findFitBot(int ticket) {
        int from;
        if (!GameUtils.isBot) {
            return null;
        }
        boolean vip = false;
        if (ticket >= 50000) {
            vip = true;
        }
        int size = 0;
        size = !vip ? this.allBotList.size() : this.allBotVip.size();
        for (int i = from = this.rd.nextInt((int)size); i < size + from; ++i) {
            int index = i % size;
            Bot bot = null;
            bot = !vip ? this.allBotList.get(index) : this.allBotVip.get(index);
            long money = this.userService.getMoneyUserCache(bot.user.getName(), "vin");
            if (money > 10000000) {
                int moneyRemain = NumberUtils.randomIntLimit(500000, 3000000);
                MoneyResponse mnres = this.botService.addMoney(bot.user.getName(), (long)moneyRemain - money, "vin", GameUtils.gameName);
                if (mnres.isSuccess()) {
                    money = mnres.getMoneyUse();
                }
            }
            if (money >= (long)ticket) {
                LoggerUtils.info("bot", "findFitBot:", "SUCCESS", bot.user.getName());
                return bot;
            }
            if (money >= 10000) continue;
            this.botService.addMoney(bot.user.getName(), 100000, "vin", GameUtils.gameName);
            LoggerUtils.info("bot", "findFitBot:", "addMoney", bot.user.getName());
        }
        LoggerUtils.info("bot", "findFitBot:", "FAILED");
        return null;
    }

    private synchronized void freeBot(Bot bot) {
        bot.isFree = true;
    }

    public List<Bot> getAllBotList() {
        return this.allBotList;
    }

    public void init() {
        try {
            List<String> listNickName;
            if (!GameUtils.isBot) {
                return;
            }
            UserModel model = this.botService.login("simacula");
            if (model == null) {
                LoggerUtils.info("bot", "init FAILED:", "Mama bot: simacula");
            }
            if ((listNickName = BotConfig.instance().getListBotNames()) != null) {
                for (String s : listNickName) {
                    model = this.botService.login(s);
                    if (model == null) {
                        LoggerUtils.info("bot", "init FAILED:", "bot:", s);
                        continue;
                    }
                    User user = this.createBotUser(s);
                    Bot bot = new Bot(user);
                    this.allBotList.add(bot);
                    if (this.allBotVip.size() <= 20) {
                        this.allBotVip.add(bot);
                    }
                    int money = (int)this.userService.getMoneyUserCache(user.getName(), "vin");
                    LoggerUtils.info("bot", "init:", "bot:", bot.user.getName(), "money:", money, "isFree", bot.isFree);
                    if (money > 10000000) {
                        int moneyRemain = NumberUtils.randomIntLimit(500000, 2000000);
                        MoneyResponse mnres = this.botService.addMoney(bot.user.getName(), (long)(moneyRemain - money), "vin", GameUtils.gameName);
                        if (mnres.isSuccess()) {
                            money = (int)mnres.getMoneyUse();
                        }
                    }
                    if (money % 1000 != 0 && money >= 1000000) continue;
                    if (money <= 0) {
                        money = 1000000;
                    }
                    int random = this.getRandomNumber(money) + 50000;
                    if (GameUtils.gameName.equals("PokerTour")) {
                        random += 1000000;
                    }
                    LoggerUtils.info("bot", "init:", "addMoney:", bot.user.getName(), "money:", money, "isFree", bot.isFree, "random:", random);
                    this.botService.addMoney(bot.user.getName(), (long)random, "vin", GameUtils.gameName);
                }
            }
            this.maxLost = BotConfig.instance().getMaxLost();
            this.maxWin = BotConfig.instance().getMaxWin();
            this.task = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.botLoopTask, 0, 1, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public Bot getBotByName(String nickName) {
        Bot bot = null;
        for (int i = 0; i < this.allBotList.size(); ++i) {
            bot = this.allBotList.get(i);
            if (!bot.user.getName().equalsIgnoreCase(nickName)) continue;
            return bot;
        }
        LoggerUtils.info("bot", "getBotByName:", "nickName:", nickName, "not Found");
        return null;
    }

    public boolean checkBot(User user) {
        if (user != null) {
            return user.isBot();
        }
        return false;
    }

    public boolean checkBotJoinRoom(User user, GameRoom room) {
        GameMoneyInfo moneyInfo = new GameMoneyInfo(user, room.setting);
        boolean result = moneyInfo.startGameUpdateMoney();
        if (result) {
            user.setProperty((Object)"GAME_MONEY_INFO", (Object)moneyInfo);
            return true;
        }
        return false;
    }

    public synchronized void joinRoom(GameRoom room) {
        if (!GameUtils.isBot || room.isFull() || room.isLocked() || room.setting.moneyType != 1 || room.setting.moneyBet > 10000) {
            return;
        }
        Bot bot = this.findFitBot(room);
        if (bot != null) {
            User user = bot.user;
            boolean checkMoney = this.checkBotJoinRoom(user, room);
            if (checkMoney) {
                bot.lastRoomId = room.getId();
                bot.count = 0;
                GameRoomManager.instance().joinRoom(user, room, false);
            } else {
                this.freeBot(bot);
            }
        }
    }

    public BotMoneyInfo getBotMoney() {
        int today = Calendar.getInstance().get(6);
        BotMoneyInfo bmInfo = new BotMoneyInfo();
        for (Bot b : this.allBotList) {
            String nickName = b.user.getName();
            VipMoneyInfo info = VipMoneyInfo.copyFromDB(nickName);
            if (info == null || info.lastDay != today || info.lastDay != today) continue;
            bmInfo.winMoneyToday += info.moneyWinToday;
            bmInfo.lostMoneyToday += info.moneyLostToday;
        }
        bmInfo.comissionMoneyToday += Math.round((double)bmInfo.lostMoneyToday * 0.02);
        bmInfo.totalMoneyToday = bmInfo.winMoneyToday - bmInfo.lostMoneyToday + bmInfo.comissionMoneyToday;
        return bmInfo;
    }

    public JSONObject getBotMoneyInfo() throws Exception {
        BotMoneyInfo bmInfo = this.getBotMoney();
        return GameUtils.toJSONObject(bmInfo);
    }

    private User createBotUser(String nickName) {
        User user = new User(null);
        user.setConnected(true);
        user.setIsBot(true);
        user.setName(nickName);
        PlayerInfo pInfo = PlayerInfo.getInfo(user);
        Random rd = new Random();
        return user;
    }

    public void releaseBot(User user) {
        if (XocDiaGameUtils.isXocDia()) {
            BotXocDiaManager.instance().releaseBot(user);
        } else {
            Bot bot = this.getBotByName(user.getName());
            if (bot != null) {
                this.freeBot(bot);
            }
        }
    }

    public void destroyGameRoom(GameRoom room) {
        if (XocDiaGameUtils.isXocDia()) {
            BotXocDiaManager.instance().destroyGameRoom(room);
        } else {
            this.regJoinRooms.remove(room);
        }
    }

    public int getBotCount() {
        return this.allBotList.size();
    }

    public synchronized void regJoinRoom(GameRoom room, int after) {
        after = this.countDown + after + 1;
        room.setProperty("bot_reg_time", after);
        this.regJoinRooms.add(room);
    }

    public synchronized void joinRooms() {
        for (GameRoom room : this.regJoinRooms) {
            Integer v = (Integer)room.getProperty("bot_reg_time");
            if (v == null || v != this.countDown) continue;
            this.joinRoom(room);
        }
    }

    private void putBotToRooms() {
        LoggerUtils.info("bot", "putBotToRooms");
        for (Map.Entry<String, GameRoomGroup> entry : GameRoomManager.instance().gameRoomGroups.entrySet()) {
            String groupName = entry.getKey();
            if (groupName.equalsIgnoreCase("FIGHTING_GROUP")) continue;
            GameRoomGroup group = entry.getValue();
            if (group.userManager.size() != 0 || group.setting.moneyType != 1 || group.setting.moneyBet > 10000) continue;
            LoggerUtils.info("bot", "putBotToRooms group =", group.setting.setting_name);
            int i = 0;
            while ((long)i < BotConfig.instance().getMaxInitialRoom()) {
                GameRoom room = group.getEmptyRoom();
                LoggerUtils.info("bot", "putBotToRooms roomId =", room.getId());
                this.joinRoom(room);
                ++i;
            }
        }
    }

    public void botLoop() {
        ++this.countDown;
        if (this.countDown % 60 == 0) {
            this.checkMoneyBalance();
        }
        this.joinRooms();
    }

    private void checkMoneyBalance() {
        if (GameUtils.gameName.equalsIgnoreCase("PokerTour")) {
            return;
        }
        ReportMoneySystemModel report = this.gameService.getReportGameToday(GameUtils.gameName);
        long diff = report.fee + report.revenuePlayGame;
        this.balanceMode = diff > BotConfig.instance().getMaxLost() ? 1 : (- diff > BotConfig.instance().getMaxWin() ? 2 : 0);
        Debug.trace((Object)("checkMoneyBalance: " + report.fee + " " + report.revenuePlayGame + " " + diff + " " + this.balanceMode));
    }

    public List<User> getListInviteBot(int num) {
        if (!XocDiaGameUtils.isXocDia()) {
            ArrayList<User> listInvite;
            block6 : {
                listInvite = new ArrayList<User>();
                try {
                    ArrayList<User> listBotFree = new ArrayList<User>();
                    for (Bot bot : this.allBotList) {
                        if (!bot.isFree) continue;
                        listBotFree.add(bot.user);
                    }
                    int size = listBotFree.size();
                    if (size > num) {
                        for (int i = 0; i < num; ++i) {
                            int index = NumberUtils.randomInt(size);
                            listInvite.add(listBotFree.get(index));
                            listBotFree.remove(index);
                            --size;
                        }
                        break block6;
                    }
                    return listBotFree;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return listInvite;
        }
        return BotXocDiaManager.instance().getListInviteBot(num);
    }

    private class BotLoopTask
    implements Runnable {
        private BotLoopTask() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                BotManager.this.botLoop();
            }
            catch (Throwable e) {
                CommonHandle.writeErrLog((Throwable)e);
            }
        }
    }

}

