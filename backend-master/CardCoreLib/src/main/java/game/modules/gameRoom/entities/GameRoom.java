/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package game.modules.gameRoom.entities;

import bitzero.server.entities.User;
import game.modules.bot.BotManager;
import game.modules.gameRoom.entities.BossManager;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.utils.GameUtils;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

public class GameRoom {
    public static final AtomicInteger genId = new AtomicInteger(1);
    public static final String NEW_JOIN_ROOM = "NEW_JOIN_ROOM";
    public GameRoomGroup group;
    private GameServer gameServer;
    private int id;
    public volatile boolean isCombining = false;
    public Map<String, User> userManager = new ConcurrentHashMap<String, User>();
    private final Map<String, Object> properties = new ConcurrentHashMap<String, Object>();
    public GameRoomSetting setting;

    public GameRoom(GameRoomSetting setting) {
        this.setting = setting;
        do {
            this.id = genId.getAndIncrement();
        } while (BossManager.instance().checkRoomId(this.id));
        this.gameServer = GameServer.createNewGameServer(this);
    }

    public GameRoom(GameRoomSetting setting, int roomId) {
        this.setting = setting;
        this.id = roomId;
        this.gameServer = GameServer.createNewGameServer(this);
    }

    public int getUserCount() {
        return this.userManager.size();
    }

    public int getBotCount() {
        int cnt = 0;
        for (Map.Entry<String, User> entry : this.userManager.entrySet()) {
            if (!entry.getValue().isBot()) continue;
            ++cnt;
        }
        return cnt;
    }

    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    public int getId() {
        return this.id;
    }

    public GameServer getGameServer() {
        return this.gameServer;
    }

    public String toString() {
        JSONObject json = this.toJSONObject();
        if (json != null) {
            return json.toString();
        }
        return "{}";
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("setting", (Object)this.setting.toJONObject());
            json.put("serverInfo", (Object)this.gameServer.toJONObject());
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, User> entry : this.userManager.entrySet()) {
                arr.put((Object)entry.getValue().getName());
            }
            json.put("users:", (Object)arr);
            json.put("isInGroup:", this.group != null);
        }
        catch (Exception e) {
            return null;
        }
        return json;
    }

    public boolean isFull() {
        return this.userManager.size() >= this.setting.limitPlayer;
    }

    public boolean almostFull() {
        int x = BotManager.instance().getRandomNumber(3);
        return this.userManager.size() + x >= this.setting.limitPlayer;
    }

    public int hash() {
        return this.id;
    }

    public boolean equal(Object obj) {
        if (obj instanceof GameRoom) {
            GameRoom r = (GameRoom)obj;
            return this.id == r.getId();
        }
        return false;
    }

    public void resetRoom() {
        this.setting.password = "";
        if (!GameUtils.gameName.equalsIgnoreCase("XocDia")) {
            this.setting.roomName = "San Bang Tat Ca";
        }
    }

    public boolean isLocked() {
        return this.setting.password.length() > 0;
    }

    public boolean hasUser(String userName) {
        for (Map.Entry<String, User> entry : this.userManager.entrySet()) {
            String name = entry.getKey();
            if (!name.equalsIgnoreCase(userName)) continue;
            return true;
        }
        return false;
    }

    public boolean canCombine() {
        return this.userManager.size() != 0 && !this.isFull();
    }
}

