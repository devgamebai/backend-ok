/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.entities.User
 *  bitzero.server.entities.managers.IUserManager
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.common.business.Debug
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.gamebai.entities.PokerFreeTicket
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.NumberUtils
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package game.modules.tour.control;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.util.TaskScheduler;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.gamebai.entities.PokerFreeTicket;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.NumberUtils;
import game.modules.bot.Bot;
import game.modules.bot.BotManager;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.tour.control.PrizeRate;
import game.modules.tour.control.RoomTourGroup;
import game.modules.tour.control.TourManager;
import game.modules.tour.control.TourPrize;
import game.modules.tour.control.TourSetting;
import game.modules.tour.control.TourUserInfo;
import game.modules.tour.control.cmd.send.SendEndTour;
import game.modules.tour.control.cmd.send.SendRegisterTour;
import game.modules.tour.control.cmd.send.SendTourCancel;
import game.modules.tour.control.cmd.send.SendTourInfoInRoom;
import game.modules.tour.control.cmd.send.SendUpdateTourLevel;
import game.modules.tour.control.config.TourConfig;
import game.modules.tour.log.LogEntry;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class Tour {
    public static final String TOUR_INFO = "TOUR_INFO";
    public static final int DAILY_TOUR = 1;
    public static final int VIP_TOUR = 2;
    public static final int STATE_TO_BEGIN = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_END_REGISTER = 2;
    public static final int STATE_END_TOUR = 3;
    public static final int STATE_CANCEL_TOUR = 4;
    public static final int STATE_NOT_TO_BEGIN = 5;
    public volatile int tourState = 5;
    public int lastDay = 0;
    public int index;
    public int tourId;
    public int tourType;
    public TourSetting tourSetting;
    public GameRoomSetting roomSetting;
    public int prizePool = 0;
    public Calendar startTime;
    public Calendar endRegisterTime;
    public Calendar endTime;
    public Calendar cancelTime;
    public int countDownToStart = 0;
    public int startDay;
    public int startHour;
    public int startMinute;
    public int endRegisterHour;
    public int endRegisterMinute;
    public int level = 1;
    public int countTimeUpLevel = 0;
    public int ticket = 0;
    public int maxNumberOfBot = 0;
    public boolean isBotRegistered = false;
    public UserService userService = new UserServiceImpl();
    public Vector<TourUserInfo> allRegisteredPlayers = new Vector();
    public RoomTourGroup roomGroup;
    public volatile boolean isRegisterLoop = false;
    public ScheduledFuture<?> task;
    public final Runnable gameLoopTask;
    private List<LogEntry> logEntries;
    private boolean alerted;
    public BlockingDeque<User> users;
    public List<String> topPrize;
    public volatile boolean canPlayGame;
    public volatile boolean canJoinRoom;

    public Tour(TourSetting setting) {
        this.gameLoopTask = new TourLoopTask();
        this.logEntries = new LinkedList<LogEntry>();
        this.alerted = false;
        this.users = new LinkedBlockingDeque<User>();
        this.topPrize = new LinkedList<String>();
        this.canPlayGame = false;
        this.canJoinRoom = false;
        this.tourSetting = setting;
        int moneyBet = this.tourSetting.levels[this.level];
        int maxUserPerRoom = this.tourSetting.maxPlayerPerRoom;
        int rule = 1;
        this.roomSetting = new GameRoomSetting(1, moneyBet, maxUserPerRoom, rule);
        this.roomGroup = new RoomTourGroup();
        this.roomGroup.tour = this;
    }

    public int playingCount() {
        int playingCount = 0;
        for (TourUserInfo info : this.allRegisteredPlayers) {
            if (info.timeBuyTicket <= info.timeOutTour) continue;
            ++playingCount;
        }
        return playingCount;
    }

    public TourUserInfo getChampionTour() {
        int playingCount = 0;
        TourUserInfo tourUserInfo = null;
        for (TourUserInfo info : this.allRegisteredPlayers) {
            if (info.timeBuyTicket <= info.timeOutTour || info.outTourTimeStamp == 1) continue;
            ++playingCount;
            tourUserInfo = info;
        }
        if (playingCount == 1) {
            return tourUserInfo;
        }
        return null;
    }

    public int totalCount() {
        int playingCount = 0;
        for (TourUserInfo info : this.allRegisteredPlayers) {
            playingCount += info.timeBuyTicket;
        }
        return playingCount;
    }

    public synchronized void notifyTourInfo() {
        SendTourInfoInRoom msg = new SendTourInfoInRoom();
        msg.prizePool = this.prizePool;
        msg.level = (byte)this.level;
        msg.userCount = this.playingCount();
        msg.registerCount = this.totalCount();
        msg.endRegisterHour = this.endRegisterHour;
        msg.endRegisterMinute = this.endRegisterMinute;
        this.sendAllTour(msg);
        this.printTourInfo();
    }

    public void send(BaseMsg msg, User user) {
        if (user != null) {
            ExtensionUtility.instance().send(msg, user);
        }
    }

    public synchronized void endTour() {
        if (this.tourState != 3) {
            LoggerUtils.debug("tour", "endTour", this.tourId);
            this.tourState = 3;
            this.sortTourRank();
            this.updateScoreAndPayPrize();
            this.endTime = Calendar.getInstance();
            TourManager.instance().updateVipTourPlayers();
            TourManager.instance().updateJackpot(this, this.prizePool);
            TourManager.instance().unlockPlayingUser(this);
            TourManager.instance().destroy(this);
            this.roomGroup.endTour();
            this.dumgLog();
        }
    }

    private void updateScoreAndPayPrize() {
        int numberOfPlayer = this.allRegisteredPlayers.size();
        for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
            TourUserInfo info = this.allRegisteredPlayers.get(i);
            int score = this.calculateScore(info.ticket, info.rank, numberOfPlayer);
            TourManager.instance().updateScore(this, info, score, numberOfPlayer);
        }
    }

    private int calculateScore(int ticket, int rank, int numberOfPlayer) {
        double rate = 1.0 * (double)ticket / 10000.0;
        double score = rate * (double)numberOfPlayer / (double)rank;
        return (int)score;
    }

    public void setStartTime(int hour, int minute) {
        this.startHour = hour;
        this.startMinute = minute;
        if (this.tourType == 2) {
            this.startDay = Calendar.getInstance().get(7);
            TourManager.instance().createVipTourFreeTicket(this);
            ++this.startMinute;
        }
        if (this.tourType == 2) {
            LoggerUtils.debug("tour", "tour_id", this.tourId, "VIP TOUR setStartTime", this.startHour, this.startMinute, this.startDay);
        }
    }

    public void addLogEntry(LogEntry entry) {
        entry.tourId = this.tourId;
        this.logEntries.add(entry);
        LoggerUtils.debug("tour", entry.toString());
    }

    public void dumgLog() {
    }

    public void putUserToTourInLoop() {
        int coutDown = this.getCountDownToStart();
        if (coutDown > 3600) {
            return;
        }
        int random = 1;
        if (this.tourState == 0 || this.tourState == 5) {
            random = GameUtils.rd.nextInt(20);
        }
        if (this.tourState == 1) {
            random = GameUtils.rd.nextInt(5);
        }
        if (this.users.size() > 0 && random == 0) {
            User user = this.users.poll();
            TourManager.instance().registerTour(user, this);
            LoggerUtils.debug("tour", "putUserToTourInLoop", user.getName(), "tour", this.tourId);
        }
    }

    public synchronized void repickTopPrize() {
        TourUserInfo info;
        String name;
        if (this.topPrize.size() > 0 && (info = this.findTourInfoByName(name = this.topPrize.get(0))) != null && info.timeBuyTicket > info.timeOutTour) {
            return;
        }
        for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
            info = this.allRegisteredPlayers.get(i);
            if (info.chip <= 0 || info.timeBuyTicket <= info.timeOutTour || BotManager.instance().getBotByName(info.nickName) == null) continue;
            this.topPrize.add(info.nickName);
            return;
        }
    }

    public void fixTopPrize() {
        int random = GameUtils.rd.nextInt(10);
        if (random >= 0) {
            int rate = GameUtils.rd.nextInt(100);
            boolean flag = false;
            if (this.ticket == 10000) {
                boolean bl = flag = rate >= 60;
            }
            if (this.ticket == 50000) {
                boolean bl = flag = rate >= 50;
            }
            if (this.ticket == 100000) {
                boolean bl = flag = rate >= 35;
            }
            if (this.ticket == 200000) {
                flag = rate >= 25;
            }
            int index = GameUtils.rd.nextInt(this.users.size());
            int c = 0;
            for (User user : this.users) {
                if (c == index) {
                    this.topPrize.add(user.getName());
                    LoggerUtils.debug("tour", "fixTopPrize", "tour", this.tourId, "user", user.getName());
                    break;
                }
                ++c;
            }
        }
    }

    public void init() {
        if (!this.isRegisterLoop) {
            this.task = BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameLoopTask, 0, 1, TimeUnit.SECONDS);
            this.isRegisterLoop = true;
        }
    }

    public void printTourInfo() {
        if (this.tourState != 1 || this.tourState != 2) {
            return;
        }
        LoggerUtils.debug("tour", "--------------------------printTourInfo--------------------------: ", this.tourId);
        for (Map.Entry<Integer, GameRoom> entry : this.roomGroup.allGameRoom.entrySet()) {
            GameRoom room = entry.getValue();
            int size1 = room.getUserCount();
            LoggerUtils.debug("tour", "room", room.getId(), "size=:", size1);
        }
        int totalChip = 0;
        int ticketCount = 0;
        for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
            TourUserInfo info = this.allRegisteredPlayers.get(i);
            LoggerUtils.debug("tour", new Object[]{"room", GameUtils.toJSONObject(info)});
            totalChip = (int)((long)totalChip + info.chip);
            ticketCount += info.timeBuyTicket;
        }
        int correctChip = 10000 * ticketCount;
        if (totalChip != correctChip) {
            LoggerUtils.debug("tour", "ERROR CHIP actual", totalChip, "should be:", correctChip);
        }
        LoggerUtils.debug("tour", "--------------------------printTourInfo--------------------------");
    }

    public void destroy() {
        if (this.task != null && !this.task.isCancelled()) {
            this.task.cancel(false);
            this.isRegisterLoop = false;
        }
    }

    public synchronized void tourLoop() {
        try {
            int countDown;
            if (GameUtils.isMainTain) {
                if (this.tourType == 1) {
                    this.cancelTour();
                }
                this.destroy();
                TourManager.instance().clearRegister(this);
                TourManager.instance().updateTour(this);
            }
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(11);
            int minute = cal.get(12);
            int day = cal.get(7);
            if (this.tourState == 5 && (countDown = this.getCountDownToStart()) <= 3600) {
                this.tourState = 0;
            }
            if (this.tourState == 0) {
                countDown = this.getCountDownToStart();
                if (countDown > 3600) {
                    this.tourState = 5;
                }
                this.checkCanJoinRoom();
                if (hour == this.startHour && minute == this.startMinute && day == this.startDay) {
                    this.startTour();
                    TourManager.instance().updateTour(this);
                }
            }
            this.putUserToTourInLoop();
            if (this.tourState == 1 || this.tourState == 2) {
                ++this.countTimeUpLevel;
                if (hour == this.endRegisterHour && minute == this.endRegisterMinute) {
                    this.endRegister();
                }
                if (this.countTimeUpLevel == this.tourSetting.timeLevelUp) {
                    this.countTimeUpLevel = 0;
                    this.updateTourLevel();
                    TourManager.instance().updateTour(this);
                }
                if (this.countTimeUpLevel % 60 == 0) {
                    this.sendUpdateLevel();
                }
                if (this.countTimeUpLevel % 5 == 0) {
                    this.checkEndTour();
                }
                this.sortTourRank();
            }
            this.sendSMSStartTourToUsers();
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((String)"Error in game loop");
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public void checkEndTour() {
        TourUserInfo champion = this.getChampionTour();
        if (champion != null && (this.tourState == 1 || this.tourState == 2)) {
            this.endTour();
            TourManager.instance().updateTour(this);
            User user = ExtensionUtility.globalUserManager.getUserByName(champion.nickName);
            if (user != null) {
                SendEndTour msg = new SendEndTour();
                if (champion != null) {
                    msg.prize = champion.prize;
                }
                this.send(msg, user);
            }
            if (GameUtils.isMainTain) {
                this.destroy();
            }
        }
    }

    public void handleFailedJoinRoom() {
        for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
            long interval;
            TourUserInfo info = this.allRegisteredPlayers.get(i);
            if (info.timeBuyTicket <= info.timeOutTour || info.roomId != 0 || (interval = System.currentTimeMillis() - info.outRoomTimeStamp) < 3000) continue;
            this.putPlayerToRoom(info);
            TourManager.instance().playingOtherTour(info.nickName, this.tourId);
        }
    }

    public synchronized void updateTourLevel() {
        if (this.level < this.tourSetting.levels.length - 1) {
            ++this.level;
        }
        this.roomSetting.moneyBet = this.tourSetting.levels[this.level];
        for (Map.Entry<Integer, GameRoom> entry : this.roomGroup.allGameRoom.entrySet()) {
            GameRoom room = entry.getValue();
            room.setting.moneyBet = this.roomSetting.moneyBet;
        }
    }

    private synchronized void sendUpdateLevel() {
        SendUpdateTourLevel msg = new SendUpdateTourLevel();
        msg.countDownNextLevel = this.tourSetting.timeLevelUp - this.countTimeUpLevel;
        msg.level = (byte)this.level;
        msg.smallBlind = (int)this.roomSetting.moneyBet;
        msg.tourId = this.tourId;
        this.sendAllTour(msg);
    }

    public synchronized void sendUpdateLevel(User user) {
        SendUpdateTourLevel msg = new SendUpdateTourLevel();
        msg.countDownNextLevel = this.tourSetting.timeLevelUp - this.countTimeUpLevel;
        msg.level = (byte)this.level;
        msg.smallBlind = (int)this.roomSetting.moneyBet;
        msg.tourId = this.tourId;
        this.send(msg, user);
    }

    public synchronized void sendAllTour(BaseMsg msg) {
        for (TourUserInfo info : this.allRegisteredPlayers) {
            User user;
            if (info.timeBuyTicket <= info.timeOutTour || (user = ExtensionUtility.globalUserManager.getUserByName(info.nickName)) == null) continue;
            ExtensionUtility.instance().send(msg, user);
        }
    }

    private synchronized void sortTourRank() {
        Collections.sort(this.allRegisteredPlayers);
        PrizeRate rate = TourPrize.findPrizeRate(this.totalCount());
        for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
            TourUserInfo info = this.allRegisteredPlayers.get(i);
            info.rank = i + 1;
            info.prize = TourPrize.getPrize(rate, info.rank, this.prizePool);
        }
    }

    public String getTourRank(int from, int to) {
        try {
            if (this.tourType == 2 && (this.tourState == 0 || this.tourState == 5)) {
                return TourManager.instance().getVipTourRankNotStart();
            }
            JSONArray arr = new JSONArray();
            int size = this.allRegisteredPlayers.size();
            if (from < 0 || from >= size) {
                from = 0;
            }
            if (to < from || to > size) {
                to = size;
            }
            for (int i = from; i < to; ++i) {
                TourUserInfo info = this.allRegisteredPlayers.get(i);
                JSONObject json = info.toRankJson();
                if (json == null) continue;
                arr.put((Object)json);
            }
            return arr.toString();
        }
        catch (Exception e) {
            return "[]";
        }
    }

    public synchronized void startTour() {
        if (this.tourState != 0) {
            return;
        }
        this.tourState = 1;
        LoggerUtils.debug("tour", "startTour", this.tourId, this.startHour, this.startMinute);
        this.refundTour();
        int size = this.allRegisteredPlayers.size();
        if (size < this.tourSetting.maxPlayerPerRoom) {
            this.cancelTour();
            TourManager.instance().destroy(this);
        } else {
            this.startTime = Calendar.getInstance();
            LoggerUtils.debug("tour", "startTour Sucess", this.tourId, this.startHour, this.startMinute);
            this.putPlayersToRoom();
            this.canPlayGame = true;
        }
    }

    private void refundTour() {
        TourUserInfo info;
        int i;
        LoggerUtils.debug("tour", "refundTour tourid=", this.tourId);
        LinkedList<TourUserInfo> playingOtherTour = new LinkedList<TourUserInfo>();
        for (i = 0; i < this.allRegisteredPlayers.size(); ++i) {
            info = this.allRegisteredPlayers.get(i);
            if (!TourManager.instance().checkUserPlayManyTour(info.nickName, this)) continue;
            playingOtherTour.add(info);
        }
        for (i = 0; i < playingOtherTour.size(); ++i) {
            info = (TourUserInfo)playingOtherTour.get(i);
            this.refundMoney(info);
            this.allRegisteredPlayers.remove(info);
            TourManager.instance().updatePlayerTour(this, info);
        }
    }

    private void putPlayersToRoom() {
        this.repickTopPrize();
        boolean flag = false;
        for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
            TourUserInfo info = this.allRegisteredPlayers.get(i);
            if (info.timeBuyTicket > info.timeOutTour) {
                String top = this.topPrize.get(0);
                if (info.nickName.equalsIgnoreCase(top)) {
                    info.fixRank = 1;
                }
                this.putPlayerToRoom(info);
                flag = true;
                TourManager.instance().playingOtherTour(info.nickName, this.tourId);
                continue;
            }
            LoggerUtils.debug("tour", "putPlayersToRoom FAILED", info.nickName, info.timeBuyTicket, info.timeOutTour, "tour", this.tourId);
        }
        if (!flag) {
            this.tourState = 3;
        }
    }

    public boolean putPlayerToRoom(TourUserInfo info) {
        if (info.timeBuyTicket > info.timeOutTour) {
            User user = ExtensionUtility.globalUserManager.getUserByName(info.nickName);
            if (user != null) {
                this.roomGroup.putUserToRoom(user, info);
                return true;
            }
            Bot bot = BotManager.instance().getBotByName(info.nickName);
            if (bot != null && bot.user != null) {
                this.roomGroup.putUserToRoom(bot.user, info);
                return true;
            }
        }
        return false;
    }

    public void initRoomSetting() {
        int moneyBet = this.tourSetting.levels[this.level];
        int maxUserPerRoom = this.tourSetting.maxPlayerPerRoom;
        int rule = 1;
        if (this.roomSetting == null) {
            this.roomSetting = new GameRoomSetting(1, moneyBet, maxUserPerRoom, rule);
        } else {
            this.roomSetting.moneyBet = moneyBet;
            this.roomSetting.maxUserPerRoom = maxUserPerRoom;
            this.roomSetting.rule = rule;
            this.roomSetting.commisionRate = 0;
        }
    }

    public void endRegister() {
        this.tourState = 2;
        this.endRegisterTime = Calendar.getInstance();
    }

    public void cancelTour() {
        LoggerUtils.debug("tour", "cancelTour", this.tourId, this.startHour, this.startMinute);
        if (this.tourState == 1 || this.tourState == 2) {
            this.tourState = 4;
            this.cancelTime = Calendar.getInstance();
        }
        this.notifyTourCancel();
        this.refundMoney();
    }

    public void refundMoney() {
        for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
            TourUserInfo info = this.allRegisteredPlayers.get(i);
            this.refundMoney(info);
        }
    }

    public void refundMoney(TourUserInfo info) {
        boolean cannotRefund = this.tourType == 2 && info.timeBuyTicket == 1;
        LogEntry entry = new LogEntry(LogEntry.ACTION.REFUND);
        if (!cannotRefund) {
            StringBuilder sb = new StringBuilder();
            if (GameUtils.isMainTain) {
                sb.append("HE THONG BAO TRI: ");
            }
            sb.append("Hoan Tien PokerTour #").append(info.tourId);
            MoneyResponse res = TourManager.instance().userService.updateMoney(info.nickName, (long)this.ticket, "vin", GameUtils.gameName, "PokerTour", sb.toString(), 0, Long.valueOf(this.tourId), TransType.END_TRANS);
            if (!res.isSuccess()) {
                CommonHandle.writeErrLog((String)sb.toString());
                entry.error = "FAIL";
            } else {
                entry.error = "OK";
            }
        } else {
            entry.error = "NOT_REFUND";
        }
        if (this.tourType != 2 || this.prizePool > TourConfig.instance().getMinVipPrizePool()) {
            this.prizePool -= this.ticket;
        }
        info.timeOutTour = info.timeBuyTicket;
        info.prize = 0;
        info.outTourTimeStamp = System.currentTimeMillis() / 1000;
        TourManager.instance().updatePlayerTour(this, info);
        entry.initTourInfo(info);
        this.addLogEntry(entry);
    }

    public void notifyTourCancel() {
        SendTourCancel msg = new SendTourCancel();
        msg.tourId = this.tourId;
        for (TourUserInfo info : this.allRegisteredPlayers) {
            User user = ExtensionUtility.globalUserManager.getUserByName(info.nickName);
            if (user == null) continue;
            this.send(msg, user);
        }
    }

    public boolean checkReconnect(User user) {
        for (GameRoom room : this.roomGroup.playingRooms) {
            for (Map.Entry<String, User> entry : room.userManager.entrySet()) {
                if (!entry.getKey().equalsIgnoreCase(user.getName())) continue;
                return true;
            }
        }
        return false;
    }

    public synchronized SendRegisterTour.ERR register(User user) {
        block20 : {
            try {
                if (GameUtils.isMainTain) {
                    return SendRegisterTour.ERR.GAME_MAIN_TAIN;
                }
                int coutDown = this.getCountDownToStart();
                if (coutDown > 3600) {
                    return SendRegisterTour.ERR.UPCOMING_TOUR;
                }
                if (this.tourState == 3 || this.tourState == 4) {
                    return SendRegisterTour.ERR.END_TIME;
                }
                boolean reconnect = this.checkReconnect(user);
                if (this.stopRegister() && !reconnect) {
                    return SendRegisterTour.ERR.FINAL_TABLE;
                }
                TourUserInfo info = this.findTourInfoByUser(user);
                if (info != null) {
                    if (info.timeOutTour < info.timeBuyTicket) {
                        if (this.tourState == 0 || reconnect) {
                            if (this.canJoinRoom) {
                                this.putPlayerToRoom(info);
                                TourManager.instance().playingOtherTour(info.nickName, this.tourId);
                                return SendRegisterTour.ERR.SUCCES;
                            }
                            return SendRegisterTour.ERR.REGISTERED_BUT_TOUR_NOT_STARTED;
                        }
                        if (this.tourState == 1 || reconnect) {
                            this.putPlayerToRoom(info);
                            TourManager.instance().playingOtherTour(info.nickName, this.tourId);
                            this.sortTourRank();
                            return SendRegisterTour.ERR.SUCCES;
                        }
                        if (this.tourState == 2) {
                            return SendRegisterTour.ERR.END_REGISTER;
                        }
                        break block20;
                    }
                    if (this.level >= this.tourSetting.maxLevelRebuy) {
                        return SendRegisterTour.ERR.LIMIT_LEVEL;
                    }
                    if (this.tourState == 2) {
                        return SendRegisterTour.ERR.END_REGISTER;
                    }
                    if (info.timeBuyTicket >= this.tourSetting.rebuy) {
                        return SendRegisterTour.ERR.LIMIT_REBUY;
                    }
                    boolean res = this.chargeTourFee(user);
                    if (res) {
                        ++info.timeBuyTicket;
                        info.chip = this.tourSetting.chip;
                        if (this.tourState == 1) {
                            this.putPlayerToRoom(info);
                            TourManager.instance().playingOtherTour(info.nickName, this.tourId);
                        }
                        break block20;
                    }
                    return SendRegisterTour.ERR.CHARGE_MONEY_ERROR;
                }
                if (this.tourState == 2) {
                    return SendRegisterTour.ERR.END_REGISTER;
                }
                boolean res = this.chargeTourFee(user);
                if (res) {
                    TourUserInfo newInfo = this.newTourUserInfo(user);
                    ++newInfo.timeBuyTicket;
                    newInfo.chip = this.tourSetting.chip;
                    this.sortTourRank();
                    if (this.tourState == 1) {
                        this.putPlayerToRoom(newInfo);
                        TourManager.instance().playingOtherTour(newInfo.nickName, this.tourId);
                    }
                    break block20;
                }
                return SendRegisterTour.ERR.CHARGE_MONEY_ERROR;
            }
            catch (Exception e) {
                CommonHandle.writeErrLog((Throwable)e);
                return SendRegisterTour.ERR.UNDEFINED_ERROR;
            }
        }
        return SendRegisterTour.ERR.SUCCES;
    }

    public synchronized SendRegisterTour.ERR register(String nickName) {
        block17 : {
            try {
                if (GameUtils.isMainTain) {
                    return SendRegisterTour.ERR.GAME_MAIN_TAIN;
                }
                if (this.tourState == 3 || this.tourState == 4) {
                    return SendRegisterTour.ERR.END_TIME;
                }
                TourUserInfo info = this.findTourInfoByName(nickName);
                if (info != null) {
                    if (info.timeOutTour < info.timeBuyTicket) {
                        if (this.tourState == 0) {
                            return SendRegisterTour.ERR.REGISTERED_BUT_TOUR_NOT_STARTED;
                        }
                        if (this.tourState == 1) {
                            this.putPlayerToRoom(info);
                            TourManager.instance().playingOtherTour(info.nickName, this.tourId);
                            this.sortTourRank();
                            return SendRegisterTour.ERR.SUCCES;
                        }
                        if (this.tourState == 2) {
                            return SendRegisterTour.ERR.END_REGISTER;
                        }
                        break block17;
                    }
                    if (this.level >= this.tourSetting.maxLevelRebuy) {
                        return SendRegisterTour.ERR.LIMIT_LEVEL;
                    }
                    if (this.tourState == 2) {
                        return SendRegisterTour.ERR.END_REGISTER;
                    }
                    if (info.timeBuyTicket >= this.tourSetting.rebuy) {
                        return SendRegisterTour.ERR.LIMIT_REBUY;
                    }
                    boolean res = this.chargeTourFeeVipTourOnly(nickName);
                    if (res) {
                        ++info.timeBuyTicket;
                        info.chip = this.tourSetting.chip;
                        if (this.tourState == 1) {
                            this.putPlayerToRoom(info);
                            TourManager.instance().playingOtherTour(info.nickName, this.tourId);
                        }
                        break block17;
                    }
                    return SendRegisterTour.ERR.CHARGE_MONEY_ERROR;
                }
                if (this.tourState == 2) {
                    return SendRegisterTour.ERR.END_REGISTER;
                }
                boolean res = this.chargeTourFeeVipTourOnly(nickName);
                if (res) {
                    TourUserInfo newInfo = this.newTourUserInfo(nickName);
                    ++newInfo.timeBuyTicket;
                    newInfo.chip = this.tourSetting.chip;
                    this.sortTourRank();
                    if (this.tourState == 1) {
                        this.putPlayerToRoom(newInfo);
                        TourManager.instance().playingOtherTour(newInfo.nickName, this.tourId);
                    }
                    break block17;
                }
                return SendRegisterTour.ERR.CHARGE_MONEY_ERROR;
            }
            catch (Exception e) {
                CommonHandle.writeErrLog((Throwable)e);
                return SendRegisterTour.ERR.UNDEFINED_ERROR;
            }
        }
        return SendRegisterTour.ERR.SUCCES;
    }

    public boolean stopRegister() {
        int count = 0;
        for (GameRoom room : this.roomGroup.playingRooms) {
            count += room.getUserCount();
        }
        return this.tourState == 1 && count <= this.tourSetting.maxPlayerPerRoom;
    }

    private boolean chargeTourFee(User user) {
        PokerFreeTicket freeTicket = TourManager.instance().useFreeTicket(user.getName(), this.ticket, this.tourType, this.tourId);
        if (freeTicket != null) {
            LogEntry entry = new LogEntry(LogEntry.ACTION.CHARGE_FEE);
            if (this.tourType == 1) {
                this.prizePool += this.ticket;
            } else {
                int count = 0;
                for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
                    TourUserInfo info = this.allRegisteredPlayers.get(i);
                    count += info.timeBuyTicket;
                }
                this.prizePool = count <= 100 ? TourConfig.instance().getMinVipPrizePool() : this.ticket * count;
            }
            entry.error = "OK";
            this.addLogEntry(entry);
            return true;
        }
        StringBuilder sb = new StringBuilder("Mua ve PokerTour #");
        sb.append(this.tourId);
        MoneyResponse res = TourManager.instance().userService.updateMoney(user.getName(), (long)(- this.ticket), "vin", GameUtils.gameName, "PokerTour", sb.toString(), 0, Long.valueOf(this.tourId), TransType.START_TRANS);
        LogEntry entry = new LogEntry(LogEntry.ACTION.CHARGE_FEE);
        TourUserInfo info = this.findTourInfoByUser(user);
        if (info != null) {
            entry.initTourInfo(info);
        } else {
            entry.user = user.getName();
        }
        entry.fee = this.ticket;
        if (res.isSuccess()) {
            if (this.tourType == 1) {
                this.prizePool += this.ticket;
            }
            entry.error = "OK";
            this.addLogEntry(entry);
            return true;
        }
        entry.error = "FAIL";
        this.addLogEntry(entry);
        return false;
    }

    private boolean chargeTourFeeVipTourOnly(String nicnName) {
        PokerFreeTicket freeTicket = TourManager.instance().useFreeTicket(nicnName, this.ticket, this.tourType, this.tourId);
        if (freeTicket != null) {
            LogEntry entry = new LogEntry(LogEntry.ACTION.FREE_TICKET);
            if (this.tourType == 1) {
                this.prizePool += this.ticket;
            } else {
                int count = 0;
                for (int i = 0; i < this.allRegisteredPlayers.size(); ++i) {
                    TourUserInfo info = this.allRegisteredPlayers.get(i);
                    count += info.timeBuyTicket;
                }
                this.prizePool = count <= 100 ? TourConfig.instance().getMinVipPrizePool() : this.ticket * count;
            }
            TourUserInfo info = this.findTourInfoByName(nicnName);
            if (info != null) {
                entry.initTourInfo(info);
            } else {
                entry.user = nicnName;
            }
            entry.error = "OK";
            this.addLogEntry(entry);
            return true;
        }
        return false;
    }

    public void userEnterTour(User user) {
        if (this.tourState == 0) {
            this.userEnterTourToStart(user);
        }
        if (this.tourState == 1) {
            this.userEnterTourPlaying(user);
        }
    }

    public void userEnterTourToStart(User user) {
    }

    public void userEnterTourPlaying(User user) {
    }

    public void notifyRebuyFailed(User user) {
    }

    public TourUserInfo findTourInfoByUser(User user) {
        return this.findTourInfoByName(user.getName());
    }

    public boolean checkUserInRoom(User user) {
        return this.roomGroup.getRoomByUser(user) != null;
    }

    public TourUserInfo findTourInfoByName(String userName) {
        for (TourUserInfo info : this.allRegisteredPlayers) {
            if (!info.nickName.equalsIgnoreCase(userName)) continue;
            return info;
        }
        return null;
    }

    public synchronized TourUserInfo newTourUserInfo(User user) {
        return this.newTourUserInfo(user.getName());
    }

    public synchronized TourUserInfo newTourUserInfo(String nickName) {
        TourUserInfo newInfo = new TourUserInfo();
        this.allRegisteredPlayers.add(newInfo);
        newInfo.nickName = nickName;
        newInfo.ticket = this.ticket;
        newInfo.timeBuyTicket = 0;
        newInfo.tourId = this.tourId;
        return newInfo;
    }

    public int getCountDownToStart() {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        start.set(7, this.startDay);
        start.set(11, this.startHour);
        start.set(12, this.startMinute);
        start.set(13, 0);
        int today = now.get(7);
        if (today != 1 && this.tourType == 2 && start.before(now)) {
            start.add(7, 7);
        }
        long interval = start.getTimeInMillis() - now.getTimeInMillis();
        this.countDownToStart = (int)(interval / 1000);
        if (this.countDownToStart < 0) {
            this.countDownToStart = 0;
        }
        return this.countDownToStart;
    }

    public void checkCanJoinRoom() {
        this.canJoinRoom = this.getCountDownToStart() <= 600;
    }

    public void sendSMSStartTourToUsers() {
        try {
            int countDown;
            if (!this.alerted && this.ticket > 10000 && this.tourState == 0 && (countDown = this.getCountDownToStart()) >= 60 && countDown <= 600) {
                this.alerted = true;
                Debug.trace((Object)("alertTour: " + this.tourId));
                AlertServiceImpl alertSer = new AlertServiceImpl();
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                Calendar cal = Calendar.getInstance();
                cal.set(7, this.startDay);
                cal.set(11, this.startHour);
                cal.set(12, this.startMinute);
                cal.set(13, 0);
                String preFix = "Poker Tour #";
                if (this.tourType == 2) {
                    preFix = "Poker Tour Vip #";
                }
                String pre = preFix + this.tourId + " (Phi: " + NumberUtils.formatNumber((String)String.valueOf(this.ticket)) + ") se bat dau vao luc " + format.format(cal.getTime()) + ". Tai khoan ";
                String post = " vui long co mat dung gio de tham gia tour.";
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap userMap = client.getMap("cache_user");
                for (TourUserInfo uInfo : this.allRegisteredPlayers) {
                    try {
                        UserCacheModel user;
                        if (!userMap.containsKey((Object)uInfo.nickName) || (user = (UserCacheModel)userMap.get((Object)uInfo.nickName)).isBot() || !user.isHasMobileSecurity() || user.getMobile() == null || user.getMobile().isEmpty()) continue;
                        Debug.trace((Object)("alertUser: " + uInfo.nickName + " " + this.tourId));
                        String content = pre + uInfo.nickName + post;
                        alertSer.sendSMS2One(user.getMobile(), content, false);                        
                    }
                    catch (Exception e) {
                        Debug.trace((Object)e);
                    }
                }
            }
        }
        catch (Exception e) {
            Debug.trace((Object)e);
        }
    }

    public final class TourLoopTask
    implements Runnable {
        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                Tour.this.tourLoop();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

}

