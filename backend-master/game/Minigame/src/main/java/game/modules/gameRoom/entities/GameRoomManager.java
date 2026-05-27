/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.gameRoom.entities;

import bitzero.util.common.business.CommonHandle;
import game.modules.gameRoom.cmd.rev.JoinGameRoomCmd;
import game.modules.gameRoom.config.GameRoomConfig;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GameRoomManager {
    private static GameRoomManager ins = null;
    public List<GameRoomSetting> roomSettingList = new ArrayList<GameRoomSetting>();
    public ConcurrentHashMap<String, GameRoomGroup> gameRoomGroups = new ConcurrentHashMap();
    public ConcurrentHashMap<Integer, GameRoom> allGameRooms = new ConcurrentHashMap();

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
            for (int i = 0; i < roomConfigList.length(); ++i) {
                JSONObject roomConfig = roomConfigList.getJSONObject(i);
                GameRoomSetting setting = new GameRoomSetting(roomConfig);
                this.roomSettingList.add(setting);
                GameRoomGroup group = new GameRoomGroup(setting);
                this.gameRoomGroups.put(setting.getSettingName(), group);
            }
        }
        catch (JSONException e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public GameRoomGroup getGroupBySetting(GameRoomSetting setting) {
        GameRoomGroup group = this.gameRoomGroups.get(setting.getSettingName());
        return group;
    }

    public GameRoom getGameRoomById(int roomId) {
        return this.allGameRooms.get(roomId);
    }

    public GameRoom createNewGameRoom(GameRoomSetting setting) {
        GameRoom room = new GameRoom(setting);
        this.allGameRooms.put(room.getId(), room);
        return room;
    }

    public GameRoomGroup getGroup(JoinGameRoomCmd cmd) {
        GameRoomSetting setting = new GameRoomSetting(cmd);
        return this.getGroupBySetting(setting);
    }

    public int getUserCount(GameRoomSetting setting) {
        return this.getGroupBySetting(setting).getUserCount();
    }
}

