/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.BitZeroServer
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventListener
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.entities.managers.IUserManager
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.server.util.TaskScheduler
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  com.vinplay.usercore.service.UserService
 *  org.json.JSONObject
 */
package game.modules.gameRoom;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.util.TaskScheduler;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import com.vinplay.usercore.service.UserService;
import game.entities.PlayerInfo;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.bot.BotManager;
import game.modules.gameRoom.cmd.rev.ChatRoomCmd;
import game.modules.gameRoom.cmd.rev.CreateGameRoomCmd;
import game.modules.gameRoom.cmd.rev.GetRoomInfoById;
import game.modules.gameRoom.cmd.rev.JoinGameRoomCmd;
import game.modules.gameRoom.cmd.rev.JoinRoomByRoomId;
import game.modules.gameRoom.cmd.rev.RevAcceptInvite;
import game.modules.gameRoom.cmd.rev.RevGetRoomList;
import game.modules.gameRoom.cmd.rev.RevInvite;
import game.modules.gameRoom.cmd.send.ChatRoomMsg;
import game.modules.gameRoom.cmd.send.CreateGameRoomFailMsg;
import game.modules.gameRoom.cmd.send.GameRoomConfigMsg;
import game.modules.gameRoom.cmd.send.JoinGameRoomFailMsg;
import game.modules.gameRoom.cmd.send.ReconnectGameRoomFailMsg;
import game.modules.gameRoom.cmd.send.SendGameRoomInfo;
import game.modules.gameRoom.cmd.send.SendInvite;
import game.modules.gameRoom.cmd.send.SendListInvite;
import game.modules.gameRoom.cmd.send.SendRoomList;
import game.modules.gameRoom.cmd.send.SendThongTinHu;
import game.modules.gameRoom.cmd.send.XocDiaConfigMsg;
import game.modules.gameRoom.config.GameRoomConfig;
import game.modules.gameRoom.config.HuVangConfig;
import game.modules.gameRoom.entities.BanUserManager;
import game.modules.gameRoom.entities.BlockingData;
import game.modules.gameRoom.entities.BossManager;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomIdGenerator;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.modules.gameRoom.entities.ListGameMoneyInfo;
import game.modules.gameRoom.entities.ThongTinHuVang;
import game.modules.gameRoom.entities.ThongTinThangLon;
import game.modules.gameRoom.fight.FightingManager;
import game.modules.tour.control.TourManager;
import game.utils.GameCommonUtils;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import game.utils.NumberUtils;
import game.xocdia.conf.XocDiaConfig;
import game.xocdia.conf.XocDiaGameUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class GameRoomModule
extends BaseClientRequestHandler {
    private final Runnable gameRoomLoopTask;

    public GameRoomModule() {
        this.gameRoomLoopTask = new GameLoopTask();
    }

    private void gameRoomLoop() {
        try {
            if (GameUtils.isHuVang) {
                ThongTinHuVang.instance().addMoneyInLoop();
            }
            if (GameUtils.gameName.equalsIgnoreCase("PokerTour")) {
                int today = Calendar.getInstance().get(7);
                if (TourManager.instance().lastDay != today) {
                    TourManager.instance().lastDay = today;
                    TourManager.instance().init();
                    TourManager.instance().giveDailyPrize(today);
                }
                TourManager.instance().registerTourForBot();
                TourManager.instance().updateVipTourPlayers();
            }
        }
        catch (Exception e) {
            CommonHandle.writeInfoLog((Throwable)e);
        }
    }

    public void init() {
        if (GameUtils.isBot) {
            BotManager.instance();
        }
        GameRoomIdGenerator.instance();
        BossManager.instance();
        GameRoomManager.instance();
        ListGameMoneyInfo.instance();
        BanUserManager.instance();
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        this.getParentExtension().addEventListener((IBZEventType)GameEventType.GAME_ROOM_USER_JOIN, (IBZEventListener)this);
        this.getParentExtension().addEventListener((IBZEventType)GameEventType.GAME_ROOM_USER_LEAVE, (IBZEventListener)this);
        this.getParentExtension().addEventListener((IBZEventType)GameEventType.THANG_LON, (IBZEventListener)this);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.gameRoomLoopTask, 3, 60, TimeUnit.SECONDS);
        BossManager.instance().initialBoss();
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        GameRoom room;
        User user;
        if (ibzevent.getType() == GameEventType.GAME_ROOM_USER_JOIN) {
            user = (User)ibzevent.getParameter((IBZEventParam)GameEventParam.USER);
            room = (GameRoom)ibzevent.getParameter((IBZEventParam)GameEventParam.GAMEROOM);
            Boolean isReconnect = (Boolean)ibzevent.getParameter((IBZEventParam)GameEventParam.IS_RECONNECT);
            this.userJoinRoomSuccess(user, room, isReconnect);
        }
        if (ibzevent.getType() == GameEventType.THANG_LON) {
            user = (User)ibzevent.getParameter((IBZEventParam)GameEventParam.USER);
            room = (GameRoom)ibzevent.getParameter((IBZEventParam)GameEventParam.GAMEROOM);
            ThongTinThangLon info = (ThongTinThangLon)ibzevent.getParameter((IBZEventParam)GameEventParam.THONG_TIN_THANG_LON);
            this.xuLiThangLon(user, room, info);
        }
        if (ibzevent.getType() == GameEventType.GAME_ROOM_USER_LEAVE) {
            user = (User)ibzevent.getParameter((IBZEventParam)GameEventParam.USER);
            room = (GameRoom)ibzevent.getParameter((IBZEventParam)GameEventParam.GAMEROOM);
            this.userLeaveRoom(user, room);
        }
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDisconnected(user);
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 3001: {
                this.joinGameRoom(user, dataCmd);
                break;
            }
            case 3002: {
                this.reconnectGameRoom(user, dataCmd);
                break;
            }
            case 3003: {
                this.requestConfig(user, dataCmd);
                break;
            }
            case 3008: {
                this.onChatRoom(user, dataCmd);
                break;
            }
            case 3009: {
                this.thongTinHuVang(user, dataCmd);
                break;
            }
            case 3010: {
                this.listInvite(user, dataCmd);
                break;
            }
            case 3012: {
                this.accept(user, dataCmd);
                break;
            }
            case 3011: {
                this.invite(user, dataCmd);
                break;
            }
            case 3013: {
                this.create_room(user, dataCmd);
                break;
            }
            case 3014: {
                this.getRoomList(user, dataCmd);
                break;
            }
            case 3015: {
                this.joinGameRoomById(user, dataCmd);
                break;
            }
            case 3016: {
                this.getGameRoomById(user, dataCmd);
                break;
            }
            case 3017: {
                this.getXocDiaConfig(user, dataCmd);
                break;
            }
            default: {
                this.sendMessageToGameServer(user, dataCmd);
            }
        }
    }

    private void sendMessageToGameServer(User user, DataCmd dataCmd) {
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        if (room != null) {
            GameServer gs = room.getGameServer();
            gs.onGameMessage(user, dataCmd);
        }
    }

    private void getXocDiaConfig(User user, DataCmd dataCmd) {
        XocDiaConfigMsg msg = new XocDiaConfigMsg();
        msg.fundVipMinRegis = XocDiaConfig.fundVipMinRegis;
        this.send((BaseMsg)msg, user);
    }

    public void getGameRoomById(User user, DataCmd data) {
        GetRoomInfoById cmd = new GetRoomInfoById(data);
        LoggerUtils.info("game_room", "getGameRoomById", user.getName(), cmd.roomId);
        GameRoom room = GameRoomManager.instance().getGameRoomById(cmd.roomId);
        SendGameRoomInfo msg = new SendGameRoomInfo();
        if (room != null) {
            msg.room = room;
        } else {
            msg.Error = Byte.valueOf("1");
        }
        this.send((BaseMsg)msg, user);
    }

    public void joinGameRoomById(User user, DataCmd data) {
        JoinRoomByRoomId cmd = new JoinRoomByRoomId(data);
        GameRoom room = GameRoomManager.instance().getGameRoomToJoin(cmd.roomId, cmd.password, user);
        if (room != null) {
            if (room.isFull()) {
                this.notifyJoinRoomFail(user, (byte)9, false);
                return;
            }
            if (BanUserManager.instance().isBan(room.getId(), user.getName())) {
                this.notifyJoinRoomFail(user, (byte)10, false);
                return;
            }
            boolean check = this.preJoinRoom(user, room.getId(), false);
            if (!check) {
                return;
            }
            boolean moneyCheck = this.checkMoneyJoinRoom(user, room.setting, -1);
            if (moneyCheck) {
                GameRoomManager.instance().joinRoom(user, room, false);
            } else {
                this.notifyJoinRoomFail(user, (byte)3, false);
            }
        }
    }

    public void create_room(User user, DataCmd dataCmd) {
        CreateGameRoomCmd cmd = new CreateGameRoomCmd(dataCmd);
        if (cmd.maxUserPerRoom > 2 && !GameCommonUtils.canCreateRoom()) {
            this.notifyCreateJoinRoomFail(user, (byte)14);
            return;
        }
        GameRoomSetting setting = new GameRoomSetting(cmd);
        long moneyRequire = -1;
        if (XocDiaGameUtils.isXocDia()) {
            int error = XocDiaGameUtils.isCanCreateBoss(setting, cmd.moneyRequire, user.getName());
            if (error == 0) {
                moneyRequire = cmd.moneyRequire;
                setting.roomName = user.getName();
            } else {
                this.notifyCreateJoinRoomFail(user, (byte)error);
                return;
            }
        }
        this.joinGameRoom(user, setting, moneyRequire, true, false);
    }

    public void invite(User user, DataCmd dataCmd) {
        boolean check = BlockingData.instance().preventSpamInvite(user.getName());
        if (check) {
            return;
        }
        RevInvite cmd = new RevInvite(dataCmd);
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        if (room != null && cmd.users != null) {
            for (int i = 0; i < cmd.users.length; ++i) {
                GameRoom room2;
                User u = ExtensionUtility.globalUserManager.getUserByName(cmd.users[i]);
                if (u == null || (room2 = (GameRoom)u.getProperty((Object)"GAME_ROOM")) != null) continue;
                SendInvite msg = new SendInvite();
                msg.inviter = user.getName();
                msg.maxUserPerRoom = room.setting.maxUserPerRoom;
                msg.roomID = room.getId();
                msg.moneyBet = room.setting.moneyBet;
                msg.rule = room.setting.rule;
                this.send((BaseMsg)msg, u);
            }
        }
    }

    public void accept(User user, DataCmd dataCmd) {
        RevAcceptInvite cmd = new RevAcceptInvite(dataCmd);
        User u = ExtensionUtility.globalUserManager.getUserByName(cmd.inviter);
        if (user != null) {
            GameRoom room1 = (GameRoom)user.getProperty((Object)"GAME_ROOM");
            GameRoom room2 = (GameRoom)u.getProperty((Object)"GAME_ROOM");
            if (room1 == null && room2 != null && this.checkJoinRoom(user, room2)) {
                GameRoomManager.instance().joinRoom(user, room2, false);
            }
        }
    }

    public boolean checkJoinRoom(User user, GameRoom room) {
        if (room.isFull()) {
            return false;
        }
        if (user.getProperty((Object)"GAME_ROOM") != null) {
            return false;
        }
        GameMoneyInfo moneyInfo = new GameMoneyInfo(user, room.setting);
        boolean result = moneyInfo.startGameUpdateMoney();
        if (result) {
            user.setProperty((Object)"GAME_MONEY_INFO", (Object)moneyInfo);
            return true;
        }
        return false;
    }

    public void listInvite(User user, DataCmd dataCmd) {
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        if (room == null || room.setting.moneyType != 1 || room.isFull() || room.isLocked()) {
            return;
        }
        long expected = room.setting.requiredMoney;
        if (GameUtils.gameName.equalsIgnoreCase("Poker")) {
            expected = room.setting.moneyBet * 40;
        }
        if (GameUtils.gameName.equalsIgnoreCase("Lieng")) {
            expected = room.setting.moneyBet * 5;
        }
        List<User> users = ExtensionUtility.globalUserManager.getAllUsers();
        List<User> listBotInvite = BotManager.instance().getListInviteBot(NumberUtils.randomIntLimit(3, 6));
        for (User bot : listBotInvite) {
            users.add(bot);
        }
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Long> moneyList = new ArrayList<Long>();
        int c = 0;
        for (User u : users) {
            long money;
            GameRoom r = (GameRoom)u.getProperty((Object)"GAME_ROOM");
            if (r != null || (money = GameMoneyInfo.userService.getMoneyUserCache(u.getName(), "vin")) < expected) continue;
            names.add(u.getName());
            moneyList.add(money);
            if (++c != 10) continue;
            break;
        }
        SendListInvite msg = new SendListInvite();
        msg.listMoney = new long[c];
        msg.listName = new String[c];
        for (int i = 0; i < c; ++i) {
            msg.listName[i] = (String)names.get(i);
            msg.listMoney[i] = (Long)moneyList.get(i);
        }
        this.send((BaseMsg)msg, user);
    }

    public void notifyJoinRoomFail(User user, byte error, boolean isCreate) {
        if (!isCreate) {
            JoinGameRoomFailMsg msg = new JoinGameRoomFailMsg();
            msg.Error = Byte.valueOf(error);
            this.send((BaseMsg)msg, user);
        } else {
            this.notifyCreateJoinRoomFail(user, error);
        }
    }

    public void notifyCreateJoinRoomFail(User user, byte error) {
        CreateGameRoomFailMsg msg = new CreateGameRoomFailMsg();
        msg.Error = Byte.valueOf(error);
        this.send((BaseMsg)msg, user);
    }

    public boolean preJoinRoom(User user, int roomId, boolean isCreate) {
        boolean check;
        boolean isHold = PlayerInfo.getIsHold(user.getName());
        boolean isBoss = BossManager.instance().checkBossName(user.getName());
        if (isHold && (!isBoss || BossManager.instance().checkBossNameAndId(user.getName(), roomId))) {
            roomId = isBoss ? roomId : -1;
            this.reconnectGameRoomUser(user, roomId);
            return false;
        }
        if (GameUtils.isMainTain) {
            this.notifyJoinRoomFail(user, (byte)6, isCreate);
            return false;
        }
        long last = user.getLastJoinRoomTime();
        long now = System.currentTimeMillis();
        long interval = now - last;
        if (interval < GameRoomConfig.instance().getJoinRoomIntervalTime()) {
            this.notifyJoinRoomFail(user, (byte)5, isCreate);
            return false;
        }
        user.setLastJoinRoomTime(now);
        if (!isBoss && !(check = GameUtils.infoCheck(user))) {
            this.notifyJoinRoomFail(user, (byte)1, isCreate);
            return false;
        }
        return true;
    }

    public void joinGameRoom(User user, GameRoomSetting setting, long moneyRequire, boolean isCreate, boolean isBossJoin) {
        boolean check = this.preJoinRoom(user, -1, isCreate);
        if (!check) {
            return;
        }
        GameRoomGroup group = GameRoomManager.instance().getGroup(setting);
        if (group == null) {
            LoggerUtils.debug("game_room", "Finding Group: ", setting.moneyType, setting.maxUserPerRoom);
            group = GameRoomManager.instance().findSuitableGroup(user, setting);
            if (group != null) {
                LoggerUtils.debug("game_room", "Finding Group: ", group.setting.moneyType, group.setting.maxUserPerRoom);
            }
        }
        if (group == null) {
            this.notifyJoinRoomFail(user, (byte)4, isCreate);
            return;
        }
        boolean result = this.checkMoneyJoinRoom(user, group.setting, moneyRequire);
        if (!result) {
            this.notifyJoinRoomFail(user, (byte)3, isCreate);
            return;
        }
        boolean isBossCreate = moneyRequire > 0;
        int res = group.joinRoom(user, setting, isBossCreate, isBossJoin);
        if (res != 0) {
            this.notifyJoinRoomFail(user, (byte)4, isCreate);
            GameMoneyInfo info = (GameMoneyInfo)user.getProperty((Object)"GAME_MONEY_INFO");
            info.restoreMoney(-1);
            return;
        }
    }

    public void joinGameRoom(User user, DataCmd dataCmd) {
        JoinGameRoomCmd cmd = new JoinGameRoomCmd(dataCmd);
        GameRoomSetting setting = new GameRoomSetting(cmd);
        LoggerUtils.debug("game_room", "joinGameRoom", cmd.moneyType, cmd.maxUserPerRoom, setting.maxUserPerRoom, setting.moneyType, setting.rule);
        this.joinGameRoom(user, setting, -1, false, BossManager.instance().checkBossName(user.getName()));
    }

    public boolean checkMoneyJoinRoom(User user, GameRoomSetting setting, long moneyRequire) {
        boolean result;
        GameMoneyInfo moneyInfo = new GameMoneyInfo(user, setting);
        if (moneyRequire > 0) {
            moneyInfo.requireMoney = moneyRequire;
        }
        if (result = moneyInfo.startGameUpdateMoney()) {
            user.setProperty((Object)"GAME_MONEY_INFO", (Object)moneyInfo);
            return true;
        }
        return false;
    }

    public void userJoinRoomSuccess(User user, GameRoom room, boolean isReconnect) {
        LoggerUtils.debug("tour", "userJoinRoomSuccess", user.getName(), "room", room.getId(), "isReconnect", isReconnect);
        GameServer gs = room.getGameServer();
        if (isReconnect) {
            gs.onGameUserReturn(user);
            LoggerUtils.debug("tour", "userJoinRoomSuccess onGameUserReturn", user.getName(), "room", room.getId(), "isReconnect", isReconnect);
        } else {
            GameMoneyInfo info = (GameMoneyInfo)user.getProperty((Object)"GAME_MONEY_INFO");
            if (info != null) {
                info.roomId = room.getId();
            }
            gs.onGameUserEnter(user);
            LoggerUtils.debug("tour", "userJoinRoomSuccess onGameUserEnter", user.getName(), "room", room.getId(), "isReconnect", isReconnect);
            User enemy = (User)user.getProperty((Object)"ENEMY_USER");
            if (enemy != null) {
                user.removeProperty((Object)"ENEMY_USER");
                enemy.removeProperty((Object)"ENEMY_USER");
                FightingManager.instance().removeOnFightUser(enemy);
                FightingManager.instance().removeOnFightUser(user);
                GameRoomManager.instance().joinRoom(enemy, room, false);
            }
        }
    }

    public void userLeaveRoom(User user, GameRoom room) {
        GameRoom newRoom;
        GameServer gs = room.getGameServer();
        gs.onGameUserExit(user);
        if (GameUtils.isBot && user.isBot()) {
            BotManager.instance().releaseBot(user);
        }
        if ((newRoom = (GameRoom)user.getProperty((Object)"NEW_JOIN_ROOM")) != null) {
            GameRoomManager.instance().joinRoom(user, newRoom, false);
            LoggerUtils.debug("tour", "userLeaveRoom Join New Room", user.getName(), "room", room.getId());
            user.removeProperty((Object)"NEW_JOIN_ROOM");
        } else {
            FightingManager.instance().addOnFightUser(user);
        }
    }

    public void userDisconnected(User user) {
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        if (room != null) {
            GameServer gs = room.getGameServer();
            gs.onGameUserDis(user);
        }
    }

    private void reconnectGameRoom(User user, DataCmd dataCmd) {
        boolean isHold = PlayerInfo.getIsHold(user.getName());
        if (isHold && !BossManager.instance().checkBossName(user.getName())) {
            this.reconnectGameRoomUser(user, -1);
        } else {
            ReconnectGameRoomFailMsg msg = new ReconnectGameRoomFailMsg();
            this.send((BaseMsg)msg, user);
        }
    }

    private void reconnectGameRoomUser(User user, int roomId) {
        GameRoom room;
        if (roomId == -1) {
            roomId = PlayerInfo.getHoldRoom(user.getName());
        }
        if ((room = GameRoomManager.instance().getGameRoomById(roomId)) != null) {
            GameRoomManager.instance().joinRoom(user, room, true);
        }
    }

    private void requestConfig(User user, DataCmd dataCmd) {
        GameRoomConfigMsg msg = new GameRoomConfigMsg();
        this.send((BaseMsg)msg, user);
    }

    private void thongTinHuVang(User user, DataCmd dataCmd) {
        if (!GameUtils.isHuVang) {
            return;
        }
        JSONObject json = HuVangConfig.instance().getConfigCurrentGame();
        try {
            SendThongTinHu msg = new SendThongTinHu();
            int remain = HuVangConfig.instance().kiemTraHuVangTheoThoiGian(json);
            if (remain >= 600 || remain == 0) {
                json = HuVangConfig.instance().timHuVangDangChay();
            }
            if (json != null) {
                msg.gameName = json.getString("gameName");
                msg.remainTime = HuVangConfig.instance().kiemTraHuVangTheoThoiGian(json);
                msg.goldAmmount = ThongTinHuVang.instance().getGoldAmount(msg.gameName);
                this.send((BaseMsg)msg, user);
            }
        }
        catch (Exception e) {
            if (json != null) {
                CommonHandle.writeErrLog((String)json.toString());
            }
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    private void xuLiThangLon(User user, GameRoom room, ThongTinThangLon info) {
        try {
            if (info.noHu) {
                this.congTienThangHu(user, room, info);
            }
        }
        catch (Exception e) {
            return;
        }
    }

    private void congTienThangHu(User user, GameRoom room, ThongTinThangLon info) {
        info = ThongTinHuVang.instance().addMoneyForUser(user, info);
        if (info != null) {
            GameServer gs = room.getGameServer();
            gs.onNoHu(info);
        }
    }

    private void onChatRoom(User user, DataCmd data) {
        long delta;
        Long lastChat = (Long)user.getProperty((Object)"last_chat_time");
        long now = System.currentTimeMillis();
        if (lastChat != null && (delta = now - lastChat) < 3000) {
            return;
        }
        user.setProperty((Object)"last_chat_time", (Object)now);
        ChatRoomCmd cmd = new ChatRoomCmd(data);
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        Integer chair = (Integer)user.getProperty((Object)"user_chair");
        if (chair == null) {
            chair = 0;
        }
        if (room != null) {
            ChatRoomMsg msg = new ChatRoomMsg();
            msg.chair = chair;
            msg.isIcon = cmd.isIcon;
            msg.content = cmd.content;
            msg.nickName = user.getName();
            for (Map.Entry<String, User> entry : room.userManager.entrySet()) {
                User u = entry.getValue();
                if (u == null || u.isBot() || !u.isConnected()) continue;
                this.send((BaseMsg)msg, u);
            }
        }
    }

    private void getRoomList(User user, DataCmd data) {
        RevGetRoomList cmd = new RevGetRoomList(data);
        GameRoomSetting setting = new GameRoomSetting(cmd);
        GameRoomGroup group = GameRoomManager.instance().getGroup(setting);
        if (cmd.from < 0 || cmd.to < 0 || cmd.from > cmd.to) {
            cmd.from = 0;
            cmd.to = 50;
        }
        if (cmd.to > cmd.from + 50) {
            cmd.to = cmd.from + 50;
        }
        if (group != null) {
            int from = -1;
            int to = -1;
            SendRoomList msg = new SendRoomList();
            for (GameRoom room222 : group.freeRooms) {
                ++to;
                if (++from < cmd.from) continue;
                if (to > cmd.to) break;
                msg.roomList.add(room222);
            }
            for (GameRoom room222 : group.lockedRooms) {
                ++to;
                if (++from < cmd.from) continue;
                if (to > cmd.to) break;
                msg.roomList.add(room222);
            }
            for (GameRoom room222 : group.busyRooms) {
                ++to;
                if (++from < cmd.from) continue;
                if (to > cmd.to) break;
                msg.roomList.add(room222);
            }
            for (GameRoom room222 : group.emptyRooms) {
                ++to;
                if (++from < cmd.from) continue;
                if (to > cmd.to) break;
                msg.roomList.add(room222);
                break;
            }
            this.send((BaseMsg)msg, user);
        } else {
            int from = -1;
            int to = -1;
            SendRoomList msg = new SendRoomList();
            block4 : for (Map.Entry<String, GameRoomGroup> entry : GameRoomManager.instance().gameRoomGroups.entrySet()) {
                group = entry.getValue();
                if (group.setting.moneyType != setting.moneyType || group.setting.rule != setting.rule && !XocDiaGameUtils.isXocDia() || group.setting.maxUserPerRoom != setting.maxUserPerRoom && !XocDiaGameUtils.isXocDia()) continue;
                for (GameRoom room322 : group.freeRooms) {
                    ++to;
                    if (++from < cmd.from) continue;
                    if (to > cmd.to) break;
                    msg.roomList.add(room322);
                }
                for (GameRoom room322 : group.lockedRooms) {
                    ++to;
                    if (++from < cmd.from) continue;
                    if (to > cmd.to) break;
                    msg.roomList.add(room322);
                }
                for (GameRoom room322 : group.busyRooms) {
                    ++to;
                    if (++from < cmd.from) continue;
                    if (to > cmd.to) break;
                    msg.roomList.add(room322);
                }
                for (GameRoom room322 : group.emptyRooms) {
                    ++to;
                    if (++from < cmd.from) continue;
                    if (to > cmd.to) continue block4;
                    msg.roomList.add(room322);
                    continue block4;
                }
            }
            this.send((BaseMsg)msg, user);
        }
    }

    private class GameLoopTask
    implements Runnable {
        private GameLoopTask() {
        }

        @Override
        public void run() {
            GameRoomModule.this.gameRoomLoop();
        }
    }

}

