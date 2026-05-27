/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.modules.gameRoom.fight;

import bitzero.server.entities.User;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import java.util.HashMap;
import java.util.Map;

public class FightingManager {
    public static final String GAME_ROOM_SETTING = "GAME_ROOM_SETTING";
    public static final String ENEMY_USER = "ENEMY_USER";
    private Map<String, User> onFightUsers = new HashMap<String, User>();
    private static FightingManager ins = null;

    public static FightingManager instance() {
        if (ins == null) {
            ins = new FightingManager();
        }
        return ins;
    }

    private FightingManager() {
    }

    public synchronized boolean checkUser(User user) {
        if (this.onFightUsers.containsKey(user.getName())) {
            if (user.isConnected()) {
                return true;
            }
            this.onFightUsers.remove(user.getName());
            return false;
        }
        return false;
    }

    public synchronized void prepareFight(User user1, User user2, GameRoomSetting setting) {
    }

    public synchronized void addOnFightUser(User user) {
        GameRoomSetting setting = (GameRoomSetting)user.getProperty((Object)"GAME_ROOM_SETTING");
        User enemy = (User)user.getProperty((Object)"ENEMY_USER");
        if (setting != null && enemy != null) {
            if (this.checkUser(enemy)) {
                this.removeOnFightUser(enemy);
                GameRoom room = GameRoomManager.instance().createEmptyGameRoom(setting);
                if (this.checkUserJoinRoom(user, room) && this.checkUserJoinRoom(enemy, room)) {
                    GameRoomManager.instance().joinRoom(user, room, false);
                }
            } else {
                this.onFightUsers.put(user.getName(), user);
            }
        }
    }

    public boolean checkUserJoinRoom(User user, GameRoom room) {
        GameMoneyInfo moneyInfo = new GameMoneyInfo(user, room.setting);
        boolean result = moneyInfo.startGameUpdateMoney();
        if (result) {
            user.setProperty((Object)"GAME_MONEY_INFO", (Object)moneyInfo);
            return true;
        }
        return false;
    }

    public synchronized void removeOnFightUser(User user) {
        this.onFightUsers.remove(user.getName());
    }
}

