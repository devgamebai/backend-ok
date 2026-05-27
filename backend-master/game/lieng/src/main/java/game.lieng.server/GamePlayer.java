/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  org.json.JSONObject
 */
package game.lieng.server;

import bitzero.server.entities.User;
import game.entities.PlayerInfo;
import game.lieng.server.LiengGameServer;
import game.lieng.server.logic.GroupCard;
import game.lieng.server.logic.LiengPlayerInfo;
import game.lieng.server.sPlayerInfo;
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
    public boolean choiTiepVanSau = true;
    public User user = null;
    public PlayerInfo pInfo = null;
    public GameMoneyInfo gameMoneyInfo = null;
    public sPlayerInfo spInfo = new sPlayerInfo();
    public LiengGameServer gameServer;
    public boolean autoBuyIn = false;
    public volatile boolean standUp = false;
    public volatile long lastTimeBuyIn = 0L;
    private volatile int playerStatus = 0;

    public void prepareNewGame() {
        this.spInfo.pokerInfo.clearNewGame();
    }

    public byte[] getHandCards() {
        return this.spInfo.handCards.toByteArray();
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

    public void addCards(GroupCard groupCard) {
        this.spInfo.handCards = groupCard;
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
            if (this.pInfo != null) {
                json.put("nickName", (Object)this.pInfo.nickName);
            }
            if (this.gameMoneyInfo != null) {
                json.put("gameMoneyInfo", (Object)this.gameMoneyInfo.toJSONObject());
            }
            if (this.spInfo.handCards != null) {
                json.put("handCards", (Object)this.spInfo.handCards.toString());
            } else {
                json.put("handCards", (Object)"");
            }
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

