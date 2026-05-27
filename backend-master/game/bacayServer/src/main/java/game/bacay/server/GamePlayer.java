/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  org.json.JSONObject
 */
package game.bacay.server;

import bitzero.server.entities.User;
import game.bacay.server.logic.Card;
import game.bacay.server.logic.GroupCard;
import game.bacay.server.sPlayerInfo;
import game.bacay.server.sResultInfo;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import java.util.List;
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
    public boolean reqQuitRoom = false;
    public boolean choiTiepVanSau = true;
    public boolean moBai = false;
    public boolean camChuong = false;
    public User user = null;
    public PlayerInfo pInfo = null;
    public GameMoneyInfo gameMoneyInfo = null;
    public sPlayerInfo spInfo;
    public sResultInfo spRes = new sResultInfo();
    public boolean vaoCuoc = false;
    public boolean vaoGa = false;
    public int lastChair = 0;
    public int yeuCauBotDanhBien = -1;
    public int yeuCauBotRoiPhong = -1;
    private volatile int playerStatus = 0;
    public int tuDongChoiNhanh = 0;

    public GamePlayer() {
        this.spRes.reset();
        this.spInfo = new sPlayerInfo();
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

    public boolean dangChoMoBai() {
        return this.isPlaying() && !this.moBai;
    }

    public void prepareNewGame() {
        this.moBai = false;
        this.spInfo.clearInfo();
        this.spRes.reset();
        this.spRes.chair = this.chair;
        this.vaoCuoc = false;
        this.vaoGa = false;
        this.lastChair = 0;
        this.yeuCauBotDanhBien = -1;
        this.yeuCauBotRoiPhong = -1;
    }

    public int getHandCardsSize() {
        return this.spInfo.handCards.cards.size();
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

    public long calculateMoneyLost(int kieuThang, long soTienNguoiThang, long moneyBet) {
        return 0L;
    }

    public byte[] getMinCardS() {
        byte[] cards = new byte[1];
        return cards;
    }

    public boolean checkMoneyCanPlay() {
        return this.gameMoneyInfo.moneyCheck();
    }

    public boolean tuDongBoLuot() {
        return this.tuDongChoiNhanh >= 2;
    }

    public void addCards(GroupCard groupCard) {
        this.spInfo.handCards = groupCard;
    }
}

