/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.BZEvent
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.util.ExtensionUtility
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package game.modules.gameRoom.entities;

import bitzero.server.core.BZEvent;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.util.ExtensionUtility;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.xocdia.conf.XocDiaGameUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import org.json.JSONArray;
import org.json.JSONObject;

public class GameRoomGroup {
    public static final String GAME_ROOM = "GAME_ROOM";
    public GameRoomSetting setting;
    public BlockingDeque<GameRoom> emptyRooms = new LinkedBlockingDeque<GameRoom>();
    public BlockingDeque<GameRoom> freeRooms = new LinkedBlockingDeque<GameRoom>();
    public BlockingDeque<GameRoom> busyRooms = new LinkedBlockingDeque<GameRoom>();
    public BlockingDeque<GameRoom> lockedRooms = new LinkedBlockingDeque<GameRoom>();
    public Map<String, User> userManager = new ConcurrentHashMap<String, User>();
    public int randomUserInitial = 0;

    public GameRoomGroup(GameRoomSetting setting) {
        this.setting = setting;
    }

    public GameRoomGroup() {
    }

    public int joinRoom(User user, GameRoomSetting setting, boolean isBossCreate, boolean isBossJoin) {
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        if (room != null && !isBossCreate && !isBossJoin) {
            GameRoomManager.instance().joinRoom(user, room, true);
        } else {
            if (setting.createRoom) {
                room = this.getEmptyRoom();
                room.setting.limitPlayer = setting.limitPlayer;
                room.setting.createRoom = true;
                room.setting.password = setting.password;
                room.setting.roomName = setting.roomName;
            } else {
                room = this.getCanJoinRoom();
            }
            GameRoomManager.instance().joinRoom(user, room, false);
        }
        return 0;
    }

    public int joinEmptyRoom(User user) {
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        if (room != null) {
            GameRoomManager.instance().joinRoom(user, room, true);
        } else {
            room = this.getCanJoinRoom();
            GameRoomManager.instance().joinRoom(user, room, false);
        }
        return 0;
    }

    public void leaveRoom(User user, GameRoom room) {
        this.userManager.remove(user.getName());
        room.userManager.remove(user.getName());
        this.recycleRoom(room);
        this.dispatchEventLeaveRoom(user, room);
    }

    public GameRoom getCanJoinRoom() {
        GameRoom room = this.freeRooms.poll();
        if (room != null || (room = this.emptyRooms.poll()) != null) {
            // empty if block
        }
        if (room == null) {
            room = GameRoomManager.instance().createNewGameRoom(this.setting);
            room.group = this;
        }
        return room;
    }

    public void makeEmptyRoom() {
        GameRoom room = GameRoomManager.instance().createNewGameRoom(this.setting);
        boolean add = this.emptyRooms.add(room);
    }

    public GameRoom getCanJoinEmptyRoom() {
        GameRoom room = this.freeRooms.poll();
        if (room == null) {
            room = this.emptyRooms.poll();
        }
        if (room == null) {
            room = GameRoomManager.instance().createNewGameRoom(this.setting);
            room.group = this;
        }
        return room;
    }

    public void recycleRoom(GameRoom room) {
        if (room.getUserCount() == 0) {
            room.resetRoom();
            this.emptyRooms.remove(room);
            this.freeRooms.remove(room);
            this.busyRooms.remove(room);
            this.emptyRooms.add(room);
            this.lockedRooms.remove(room);
        } else if (room.isFull()) {
            this.emptyRooms.remove(room);
            this.freeRooms.remove(room);
            this.busyRooms.remove(room);
            this.busyRooms.add(room);
            this.lockedRooms.remove(room);
        } else {
            this.emptyRooms.remove(room);
            this.freeRooms.remove(room);
            this.busyRooms.remove(room);
            this.lockedRooms.remove(room);
            if (room.setting.password.length() > 0) {
                this.lockedRooms.add(room);
            } else {
                this.freeRooms.add(room);
            }
        }
        if (this.emptyRooms.size() == 0 && this.freeRooms.size() == 0) {
            if (XocDiaGameUtils.isXocDia() && this.setting.rule != 0) {
                return;
            }
            this.makeEmptyRoom();
        }
    }

    private void dispatchEventJoinRoom(User user, GameRoom room, boolean isReconnect) {
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.GAMEROOM, room);
        evtParams.put(GameEventParam.USER, (Object)user);
        evtParams.put(GameEventParam.IS_RECONNECT, isReconnect);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.GAME_ROOM_USER_JOIN, evtParams));
    }

    private void dispatchEventLeaveRoom(User user, GameRoom room) {
        HashMap<GameEventParam, Object> evtParams = new HashMap<GameEventParam, Object>();
        evtParams.put(GameEventParam.GAMEROOM, room);
        evtParams.put(GameEventParam.USER, user);
        ExtensionUtility.dispatchEvent((IBZEvent)new BZEvent((IBZEventType)GameEventType.GAME_ROOM_USER_LEAVE, evtParams));
    }

    public int getUserCount() {
        return this.userManager.size();
    }

    public JSONArray listId(BlockingDeque<GameRoom> list) {
        try {
            JSONArray arr = new JSONArray();
            for (GameRoom room : list) {
                JSONObject json = new JSONObject();
                json.put("users", room.getUserCount());
                json.put("id", room.getId());
                arr.put((Object)json);
            }
            return arr;
        }
        catch (Exception e) {
            return null;
        }
    }

    public String toString() {
        try {
            JSONObject json = this.toJSONObject();
            if (json != null) {
                return json.toString();
            }
            return "{}";
        }
        catch (Exception e) {
            return "{}";
        }
    }

    public JSONObject toJSONObject() {
        try {
            JSONObject json = new JSONObject();
            json.put("name", (Object)this.setting.getSettingName());
            json.put("emptyRooms", (Object)this.listId(this.emptyRooms));
            json.put("freeRooms", (Object)this.listId(this.freeRooms));
            json.put("busyRooms", (Object)this.listId(this.busyRooms));
            json.put("users", this.userManager.size());
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }

    public GameRoom getEmptyRoom() {
        GameRoom room = null;
        if (room == null) {
            room = this.emptyRooms.poll();
        }
        if (room == null) {
            room = GameRoomManager.instance().createNewGameRoom(this.setting);
            room.group = this;
        }
        return room;
    }

    public GameRoom createRoom(GameRoomSetting setting) {
        GameRoom room = GameRoomManager.instance().createNewGameRoom(setting);
        room.group = this;
        return room;
    }

    public GameRoom createRoom(GameRoomSetting setting, int roomId) {
        GameRoom room = GameRoomManager.instance().createNewGameRoom(setting, roomId);
        room.group = this;
        return room;
    }

    public void destroyGameRoom(GameRoom room) {
        this.emptyRooms.remove(room);
        this.freeRooms.remove(room);
        this.busyRooms.remove(room);
        this.lockedRooms.remove(room);
    }
}

