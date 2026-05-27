package game.modules.gameRoom.entities;

import bitzero.server.core.BZEvent;
import bitzero.server.entities.User;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class GameRoomGroup
{
    public static final String GAME_ROOM = "GAME_ROOM";
    public GameRoomSetting setting;
    public BlockingDeque<GameRoom> emptyRooms = new LinkedBlockingDeque();
    public BlockingDeque<GameRoom> freeRooms = new LinkedBlockingDeque();
    public BlockingDeque<GameRoom> busyRooms = new LinkedBlockingDeque();
    public Map<Integer, User> userManager = new ConcurrentHashMap();
    public int randomUserInitial = 0;

    public GameRoomGroup(GameRoomSetting setting) {
        Random rd = new Random();
        this.setting = setting;
        randomUserInitial = Math.abs(rd.nextInt() % 1000 + 100);
    }

    public int joinRoom(User user) {
        GameRoom room = (GameRoom)user.getProperty("GAME_ROOM");
        if (room != null) {
            return 1;
        }
        room = getCanJoinRoom();
        joinRoom(user, room, false);
        return 0;
    }

    public int leaveRoom(User user) {
        GameRoom room = (GameRoom)user.getProperty("GAME_ROOM");
        if (room == null) {
            Debug.trace((Object[])new Object[] { user.getName(), "leave room null" });
            return 1;
        }
        leaveRoom(user, room);
        return 0;
    }

    public void joinRoom(User user, GameRoom room, boolean isReconnect) {
        userManager.put(Integer.valueOf(user.getId()), user);
        userManager.put(Integer.valueOf(user.getId()), user);
        user.setProperty("GAME_ROOM", room);
        recycleRoom(room);
        dispatchEventJoinRoom(user, room, isReconnect);
        Debug.trace((Object[])new Object[] { user.getName(), "join room", Integer.valueOf(room.getId()) });
    }

    public void leaveRoom(User user, GameRoom room) {
        userManager.remove(Integer.valueOf(user.getId()));
        userManager.remove(Integer.valueOf(user.getId()));
        recycleRoom(room);
        dispatchEventLeaveRoom(user, room);
        Debug.trace((Object[])new Object[] { user.getName(), "leave room", Integer.valueOf(room.getId()) });
    }

    public GameRoom getCanJoinRoom() {
        GameRoom room = freeRooms.poll();
        if (room == null) {
            room = emptyRooms.poll();
        }
        if (room == null) {
            room = GameRoomManager.instance().createNewGameRoom(setting);
            room.group = this;
        }
        return room;
    }

    private void recycleRoom(GameRoom room) {
        if (room.getUserCount() == 0) {
            emptyRooms.add(room);
            freeRooms.remove(room);
            busyRooms.remove(room);
        } else if (room.getUserCount() == setting.maxUserPerRoom) {
            busyRooms.add(room);
            freeRooms.remove(room);
            emptyRooms.remove(room);
        } else {
            freeRooms.add(room);
            emptyRooms.remove(room);
            busyRooms.remove(room);
        }
    }

    private void dispatchEventJoinRoom(User user, GameRoom room, boolean isReconnect) {
        HashMap<GameEventParam, Object> evtParams = new HashMap();
        evtParams.put(GameEventParam.GAMEROOM, room);
        evtParams.put(GameEventParam.USER, user);
        evtParams.put(GameEventParam.IS_RECONNECT, Boolean.valueOf(isReconnect));
        ExtensionUtility.dispatchEvent(new BZEvent(GameEventType.GAME_ROOM_USER_JOIN, evtParams));
    }

    private void dispatchEventLeaveRoom(User user, GameRoom room) {
        Map<GameEventParam, Object> evtParams = new HashMap();
        evtParams.put(GameEventParam.GAMEROOM, room);
        evtParams.put(GameEventParam.USER, user);
        ExtensionUtility.dispatchEvent(new BZEvent(GameEventType.GAME_ROOM_USER_LEAVE, evtParams));
    }

    int getUserCount() {
        return userManager.size() + randomUserInitial;
    }
}