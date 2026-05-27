/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  org.json.JSONObject
 */
package game.caro.server;

import bitzero.server.entities.User;
import game.caro.server.CaroGameServer;
import game.caro.server.sPlayerInfo;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import org.json.JSONObject;

public class GamePlayer {
    public static final int psNO_LOGIN = 0;
    public static final int psVIEW = 1;
    public static final int psSIT = 2;
    public static final int psPLAY = 3;
    public static final int THANG_SAM = 1;
    public static final int THANG_TRANG = 2;
    public static final int THANG_THUONG = 3;
    public int chair;
    public long timeJoinRoom = 0L;
    public int countToOutRoom = 0;
    public boolean reqQuitRoom = false;
    public volatile boolean standUp = false;
    public boolean choiTiepVanSau = true;
    public User user = null;
    public PlayerInfo pInfo = null;
    public GameMoneyInfo gameMoneyInfo = null;
    public sPlayerInfo spInfo = new sPlayerInfo();
    public CaroGameServer gameServer;
    public boolean autoBuyIn = true;
    public volatile long lastTimeBuyIn = 0L;
    public int tickType = 0;
    private volatile int playerStatus = 0;

    public void prepareNewGame() {
    }

    public void setPlayerStatus(int playerStatus) {
        this.playerStatus = playerStatus;
    }

    public int getPlayerStatus() {
        return this.playerStatus;
    }

    public User getUser() {
        return this.user;
    }

    public PlayerInfo getPlayerInfo() {
        return this.pInfo;
    }

    public void takeChair(User user, PlayerInfo pInfo, GameMoneyInfo moneyInfo) {
        this.user = user;
        this.pInfo = pInfo;
        this.gameMoneyInfo = moneyInfo;
        this.reqQuitRoom = false;
        user.setProperty((Object)"user_chair", (Object)this.chair);
    }

    public boolean isPlaying() {
        return this.playerStatus == 3;
    }

    public boolean canPlayNextGame() {
        return !this.reqQuitRoom && this.checkMoneyCanPlay();
    }

    public boolean hasUser() {
        return this.playerStatus != 0;
    }

    public boolean checkMoneyCanPlay() {
        return this.gameMoneyInfo.moneyCheck();
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
            json.put("reqQuitRoom", this.reqQuitRoom);
            json.put("playerStatus", this.playerStatus);
            if (this.gameMoneyInfo != null) {
                json.put("gameMoneyInfo", (Object)this.gameMoneyInfo.toJSONObject());
            }
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

