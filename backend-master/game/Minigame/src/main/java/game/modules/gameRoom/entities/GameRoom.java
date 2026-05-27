/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.modules.gameRoom.entities;

import bitzero.server.entities.User;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameRoom {
    public static final AtomicInteger genId = new AtomicInteger(1);
    public GameRoomGroup group;
    private GameServer gameServer;
    private int id;
    public Map<Integer, User> userManager = new ConcurrentHashMap<Integer, User>();
    private final Map<String, Object> properties = new ConcurrentHashMap<String, Object>();
    public GameRoomSetting setting;

    public GameRoom(GameRoomSetting setting) {
        this.setting = setting;
        this.id = genId.getAndIncrement();
        this.gameServer = GameServer.createNewGameServer(this);
    }

    public int getUserCount() {
        return this.userManager.size();
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
}

