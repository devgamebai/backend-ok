/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.BZEvent
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.gameRoom.entities;

import bitzero.server.core.BZEvent;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.bot.BotManager;
import game.modules.gameRoom.cmd.rev.JoinGameRoomCmd;
import game.modules.gameRoom.cmd.send.JoinGameRoomFailMsg;
import game.modules.gameRoom.config.GameRoomConfig;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.ListGameMoneyInfo;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import game.xocdia.conf.XocDiaGameUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GameRoomManager {
    public static final String FREE_GROUP = "FIGHTING_GROUP";
    public static final int FREE_PLAYERS = 2;
    private static GameRoomManager ins = null;
    public List<GameRoomSetting> roomSettingList = new ArrayList<GameRoomSetting>();
    public ConcurrentHashMap<String, GameRoomGroup> gameRoomGroups = new ConcurrentHashMap();
    public ConcurrentHashMap<Integer, GameRoom> allGameRooms = new ConcurrentHashMap();
    private UserService userService = new UserServiceImpl();

    public static GameRoomManager instance() {
        if (ins == null) {
            ins = new GameRoomManager();
        }
        return ins;
    }

    private GameRoomManager() {
        this.init();
    }

    public void init() {
        JSONObject config = GameRoomConfig.instance().config;
        try {
            JSONArray roomConfigList = config.getJSONArray("roomList");
            GameRoomSetting setting = null;
            for (int i = 0; i < roomConfigList.length(); ++i) {
                JSONObject roomConfig = roomConfigList.getJSONObject(i);
                setting = new GameRoomSetting(roomConfig);
                this.roomSettingList.add(setting);
                GameRoomGroup group = new GameRoomGroup(setting);
                this.gameRoomGroups.put(setting.getSettingName(), group);
                this.createInitialRoom(group);
            }
            if (setting != null) {
                GameRoomSetting freeSetting = new GameRoomSetting(setting);
                freeSetting.maxUserPerRoom = 2;
                GameRoomGroup freeGroup = new GameRoomGroup(freeSetting);
                this.gameRoomGroups.put("FIGHTING_GROUP", freeGroup);
            }
        }
        catch (JSONException e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public void createInitialRoom(GameRoomGroup group) {
        for (int i = 0; i < group.setting.numberOfInitialRoom; ++i) {
            GameRoomSetting newSetting = new GameRoomSetting(group.setting);
            GameRoom room = new GameRoom(newSetting);
            this.allGameRooms.put(room.getId(), room);
            room.group = group;
            group.recycleRoom(room);
        }
    }

    public GameRoomGroup getGroup(GameRoomSetting setting) {
        GameRoomGroup group = this.gameRoomGroups.get(setting.getSettingName());
        return group;
    }

    public GameRoomGroup getGroupBySetting(String settingName) {
        GameRoomGroup group = this.gameRoomGroups.get(settingName);
        return group;
    }

    public GameRoom getGameRoomById(int roomId) {
        return this.allGameRooms.get(roomId);
    }

    public GameRoom createNewGameRoom(GameRoomSetting setting) {
        GameRoomSetting newSetting = new GameRoomSetting(setting);
        GameRoom room = new GameRoom(newSetting);
        this.allGameRooms.put(room.getId(), room);
        return room;
    }

    public GameRoom createNewGameRoom(GameRoomSetting setting, int roomId) {
        GameRoomSetting newSetting = new GameRoomSetting(setting);
        GameRoom room = new GameRoom(newSetting, roomId);
        this.allGameRooms.put(room.getId(), room);
        return room;
    }

    public GameRoom createEmptyGameRoom(GameRoomSetting setting) {
        GameRoomGroup freeGroups = this.gameRoomGroups.get("FIGHTING_GROUP");
        GameRoom room = freeGroups.getEmptyRoom();
        room.setting = new GameRoomSetting(setting);
        this.allGameRooms.put(room.getId(), room);
        return room;
    }

    public GameRoomGroup getGroup(JoinGameRoomCmd cmd) {
        GameRoomSetting setting = new GameRoomSetting(cmd);
        return this.getGroup(setting);
    }

    public int getUserCount(GameRoomSetting setting) {
        return this.getGroup(setting).getUserCount();
    }

    public JSONObject toJSONObject() {
        try {
            JSONObject json = new JSONObject();
            for (int i = 0; i < this.roomSettingList.size(); ++i) {
                GameRoomSetting setting = this.roomSettingList.get(i);
                GameRoomGroup group = this.getGroup(setting);
                json.put(setting.getSettingName(), (Object)group.toJSONObject());
            }
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }

    public String toString() {
        JSONObject json = this.toJSONObject();
        if (json != null) {
            return json.toString();
        }
        return "{}";
    }

    public int getRoomCount() {
        return this.allGameRooms.size();
    }

    public synchronized List<GameRoomSetting> getRoomConfigList() {
        ArrayList<GameRoomSetting> list = new ArrayList<GameRoomSetting>();
        for (GameRoomSetting setting : this.roomSettingList) {
            list.add(setting);
        }
        return list;
    }

    public synchronized void joinRoom(User user, GameRoom room, boolean isReconnect) {
        LoggerUtils.debug("tour", user.getName(), "joinRoom", "roomId = ", room.getId(), "is Full?", room.isFull(), "isReconect", isReconnect);
        if (room.isFull()) {
            boolean hasUserInside = room.hasUser(user.getName());
            if (hasUserInside) {
                room.userManager.put(user.getName(), user);
                user.setProperty((Object)"GAME_ROOM", (Object)room);
                if (room.group != null) {
                    room.group.userManager.put(user.getName(), user);
                    room.group.recycleRoom(room);
                }
                this.dispatchEventJoinRoom(user, room, true);
            } else {
                JoinGameRoomFailMsg msg = new JoinGameRoomFailMsg();
                msg.Error = Byte.valueOf("9");
                ExtensionUtility.instance().send((BaseMsg)msg, user);
                GameMoneyInfo moneyInfo = (GameMoneyInfo)user.getProperty((Object)"GAME_MONEY_INFO");
                if (moneyInfo != null) {
                    ListGameMoneyInfo.instance().removeGameMoneyInfo(moneyInfo, room.getId());
                }
            }
        } else {
            room.userManager.put(user.getName(), user);
            user.setProperty((Object)"GAME_ROOM", (Object)room);
            if (room.group != null) {
                room.group.userManager.put(user.getName(), user);
                room.group.recycleRoom(room);
            }
            this.dispatchEventJoinRoom(user, room, isReconnect);
        }
    }

    public void leaveRoom(User user, GameRoom room) {
        user.removeProperty((Object)"GAME_ROOM");
        room.userManager.remove(user.getName());
        if (room.group != null) {
            room.group.userManager.remove(user.getName());
            room.group.recycleRoom(room);
        }
        this.dispatchEventLeaveRoom(user, room);
    }

    public void leaveRoom(User user) {
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        if (room != null) {
            this.leaveRoom(user, room);
        }
    }

    private void dispatchEventJoinRoom(User user, GameRoom room, boolean isReconnect) {
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.GAMEROOM, room);
        evtParams.put(GameEventParam.USER, (Object)user);
        evtParams.put(GameEventParam.IS_RECONNECT, isReconnect);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.GAME_ROOM_USER_JOIN, evtParams));
        LoggerUtils.debug("tour", "dispatchEventJoinRoom", user.getName(), "room", room.getId(), "isReconnect", isReconnect);
    }

    private void dispatchEventLeaveRoom(User user, GameRoom room) {
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.GAMEROOM, room);
        evtParams.put(GameEventParam.USER, user);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.GAME_ROOM_USER_LEAVE, evtParams));
    }

    public GameRoom getGameRoomToJoin(int roomId, String password, User user) {
        boolean checkPass;
        JoinGameRoomFailMsg msg = new JoinGameRoomFailMsg();
        GameRoom room = this.allGameRooms.get(roomId);
        if (room == null) {
            msg.Error = Byte.valueOf("7");
            ExtensionUtility.instance().send((BaseMsg)msg, user);
            return null;
        }
        if (room.isFull()) {
            msg.Error = Byte.valueOf("9");
            ExtensionUtility.instance().send((BaseMsg)msg, user);
            return null;
        }
        boolean bl = checkPass = room.setting.password.length() == 0 || room.setting.password.equalsIgnoreCase(password);
        if (checkPass) {
            return room;
        }
        msg.Error = Byte.valueOf("8");
        ExtensionUtility.instance().send((BaseMsg)msg, user);
        LoggerUtils.info("game_room", "joinGameRoom", user.getName(), roomId, password, "error:", msg.Error);
        return null;
    }

    public GameRoomGroup findSuitableGroup(User user, GameRoomSetting gameRoomSetting) {
        long moneyRequire;
        int i;
        GameRoomSetting setting;
        GameRoomGroup group;
        int moneyType = gameRoomSetting.moneyType;
        long maxUserPerRoom = gameRoomSetting.maxUserPerRoom;
        int rule = gameRoomSetting.rule;
        if (XocDiaGameUtils.isXocDia() && moneyType == 1) {
            return this.findSuitableGroupXocDia(user, moneyType);
        }
        String moneyTypeName = moneyType == 1 ? "vin" : "xu";
        long money = this.userService.getMoneyUserCache(user.getName(), moneyTypeName);
        long maxMoney = -1;
        GameRoomGroup fitGroup = null;
        for (i = 0; i < this.roomSettingList.size(); ++i) {
            setting = this.roomSettingList.get(i);
            moneyRequire = setting.requiredMoney;
            if (moneyRequire == 0 && GameUtils.gameName.equalsIgnoreCase("Poker")) {
                moneyRequire = 40 * setting.moneyBet;
            } else if (moneyRequire == 0 && GameUtils.gameName.equalsIgnoreCase("Lieng")) {
                moneyRequire = 5 * setting.moneyBet;
            }
            if (setting.moneyType != moneyType || (long)setting.maxUserPerRoom != maxUserPerRoom || setting.rule != rule || moneyRequire > money) continue;
            group = this.gameRoomGroups.get(setting.setting_name);
            if (group.freeRooms.size() <= 0 || moneyRequire <= maxMoney) continue;
            maxMoney = moneyRequire;
            fitGroup = group;
        }
        if (fitGroup != null) {
            return fitGroup;
        }
        for (i = 0; i < this.roomSettingList.size(); ++i) {
            setting = this.roomSettingList.get(i);
            moneyRequire = setting.requiredMoney;
            if (moneyRequire == 0 && GameUtils.gameName.equalsIgnoreCase("Poker")) {
                moneyRequire = 40 * setting.moneyBet;
            } else if (moneyRequire == 0 && GameUtils.gameName.equalsIgnoreCase("Lieng")) {
                moneyRequire = 5 * setting.moneyBet;
            }
            if (setting.moneyType != moneyType || (long)setting.maxUserPerRoom != maxUserPerRoom || setting.rule != rule || moneyRequire > money) continue;
            group = this.gameRoomGroups.get(setting.setting_name);
            if (moneyRequire <= maxMoney) continue;
            maxMoney = moneyRequire;
            fitGroup = group;
        }
        return fitGroup;
    }

    public GameRoomGroup findSuitableGroup(User user, int moneyType) {
        long moneyRequire;
        GameRoomSetting setting;
        int i;
        GameRoomGroup group;
        if (XocDiaGameUtils.isXocDia() && moneyType == 1) {
            return this.findSuitableGroupXocDia(user, moneyType);
        }
        String moneyTypeName = moneyType == 1 ? "vin" : "xu";
        long money = this.userService.getMoneyUserCache(user.getName(), moneyTypeName);
        long maxMoney = -1;
        GameRoomGroup fitGroup = null;
        for (i = 0; i < this.roomSettingList.size(); ++i) {
            setting = this.roomSettingList.get(i);
            moneyRequire = setting.requiredMoney;
            if (moneyRequire == 0 && GameUtils.gameName.equalsIgnoreCase("Poker")) {
                moneyRequire = 40 * setting.moneyBet;
            } else if (moneyRequire == 0 && GameUtils.gameName.equalsIgnoreCase("Lieng")) {
                moneyRequire = 5 * setting.moneyBet;
            }
            if (setting.moneyType != moneyType || moneyRequire > money) continue;
            group = this.gameRoomGroups.get(setting.setting_name);
            if (group.freeRooms.size() <= 0 || moneyRequire <= maxMoney) continue;
            maxMoney = moneyRequire;
            fitGroup = group;
        }
        if (fitGroup != null) {
            return fitGroup;
        }
        for (i = 0; i < this.roomSettingList.size(); ++i) {
            setting = this.roomSettingList.get(i);
            moneyRequire = setting.requiredMoney;
            if (moneyRequire == 0 && GameUtils.gameName.equalsIgnoreCase("Poker")) {
                moneyRequire = 40 * setting.moneyBet;
            } else if (moneyRequire == 0 && GameUtils.gameName.equalsIgnoreCase("Lieng")) {
                moneyRequire = 5 * setting.moneyBet;
            }
            if (setting.moneyType != moneyType || moneyRequire > money) continue;
            group = this.gameRoomGroups.get(setting.setting_name);
            if (moneyRequire <= maxMoney) continue;
            maxMoney = moneyRequire;
            fitGroup = group;
        }
        return fitGroup;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public GameRoomGroup findSuitableGroupXocDia(User user, int moneyType) {
        GameRoomSetting setting;
        int i;
        int rule = XocDiaGameUtils.getRuleJoin();
        GameRoomGroup fitGroup = null;
        String moneyTypeName = "vin";
        long money = this.userService.getMoneyUserCache(user.getName(), moneyTypeName);
        if (money < 100) return fitGroup;
        if (rule == 1) {
            int i2 = 0;
            while (i2 < this.roomSettingList.size()) {
                GameRoomSetting setting2 = this.roomSettingList.get(i2);
                if (setting2.moneyType == moneyType && setting2.requiredMoney <= money && rule == setting2.rule) {
                    return this.gameRoomGroups.get(setting2.setting_name);
                }
                ++i2;
            }
            return fitGroup;
        }
        long maxMoney = -1;
        for (i = 0; i < this.roomSettingList.size(); ++i) {
            setting = this.roomSettingList.get(i);
            if (setting.moneyType != moneyType || setting.requiredMoney > money || rule != setting.rule) continue;
            GameRoomGroup group = this.gameRoomGroups.get(setting.setting_name);
            if (group.freeRooms.size() <= 0 || setting.requiredMoney <= maxMoney) continue;
            maxMoney = setting.requiredMoney;
            fitGroup = group;
        }
        if (fitGroup != null) {
            return fitGroup;
        }
        rule = 1;
        i = 0;
        while (i < this.roomSettingList.size()) {
            setting = this.roomSettingList.get(i);
            if (setting.moneyType == moneyType && setting.requiredMoney <= money && rule == setting.rule) {
                return this.gameRoomGroups.get(setting.setting_name);
            }
            ++i;
        }
        return fitGroup;
    }

    public void destroyGameRoom(int roomId) {
        GameRoom room = this.allGameRooms.get(roomId);
        if (room != null) {
            if (room.group != null) {
                room.group.destroyGameRoom(room);
            }
            this.allGameRooms.remove(roomId);
            BotManager.instance().destroyGameRoom(room);
        }
    }
}

