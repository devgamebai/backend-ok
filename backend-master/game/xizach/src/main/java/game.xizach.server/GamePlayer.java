/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  org.json.JSONObject
 */
package game.xizach.server;

import bitzero.server.entities.User;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.xizach.server.logic.Card;
import game.xizach.server.logic.GroupCard;
import game.xizach.server.sPlayerInfo;
import game.xizach.server.sResultInfo;
import java.util.List;
import org.json.JSONObject;

public class GamePlayer {
    public static final int psNO_LOGIN = 0;
    public static final int psVIEW = 1;
    public static final int psSIT = 2;
    public static final int psPLAY = 3;
    public int chair;
    public boolean reqQuitRoom = false;
    public boolean choiTiepVanSau = true;
    public boolean moBai = false;
    public volatile boolean hasSoBai = false;
    public boolean camChuong = false;
    public boolean hasDanBai = false;
    public User user = null;
    public PlayerInfo pInfo = null;
    public GameMoneyInfo gameMoneyInfo = null;
    public sPlayerInfo spInfo;
    public sResultInfo spRes = new sResultInfo();
    public int lastChair = 0;
    public boolean needShowWinLostMoney = false;
    public int numGameNotActive = 0;
    public boolean active = false;
    public boolean boQuaNotActive = false;
    public boolean needShowBai = false;
    public boolean needUpdateXizach = false;
    public int yeuCauBotRoiPhong = -1;
    public int countDownRutBai = -1;
    private volatile int playerStatus = 0;
    public int tuDongChoiNhanh = 0;

    public GamePlayer() {
        this.spRes.reset();
        this.spInfo = new sPlayerInfo();
        this.numGameNotActive = 0;
        this.needShowBai = false;
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
            if (this.user != null) {
                json.put("user", (Object)this.user.getName());
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

    public boolean dangChoXetBai() {
        return this.isPlaying() && !this.hasSoBai;
    }

    public void prepareNewGame() {
        this.moBai = false;
        this.hasSoBai = false;
        this.hasDanBai = false;
        this.spInfo.clearInfo();
        this.spRes.reset();
        this.spRes.chair = this.chair;
        this.lastChair = 0;
        this.needShowWinLostMoney = false;
        this.active = false;
        this.needShowBai = false;
        this.needUpdateXizach = false;
        this.boQuaNotActive = false;
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

    public boolean isBot() {
        return this.getUser() != null && this.getUser().isBot();
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

    public byte[] getMinCardS() {
        byte[] cards = new byte[1];
        return cards;
    }

    public boolean checkMoneyCanPlay() {
        return this.gameMoneyInfo.moneyCheck();
    }

    public long getCurrentMoney() {
        return this.gameMoneyInfo.getCurrentMoneyFromCache();
    }

    public boolean tuDongBoLuot() {
        return this.tuDongChoiNhanh >= 2;
    }

    public void addCards(GroupCard groupCard) {
        this.spInfo.handCards = groupCard;
    }

    public void addStorageCard(GroupCard groupCard) {
        this.spInfo.storageCards = groupCard;
    }

    public boolean canDanBai() {
        return this.isPlaying() && this.spInfo.handCards != null && this.spInfo.handCards.canDanBai();
    }

    public boolean canRutBai() {
        return this.isPlaying() && this.spInfo.handCards != null && this.spInfo.handCards.canRutBai();
    }

    public boolean canXetBai() {
        return this.isPlaying() && this.camChuong && this.spInfo.handCards != null && this.spInfo.handCards.canDanBai();
    }

    public boolean finishRutBai() {
        return this.isPlaying() && this.spInfo.handCards != null && (this.spInfo.handCards.isQuac() || this.spInfo.handCards.isNguLinh());
    }

    public int kiemTraBotCanRutBai() {
        return this.spInfo.kiemTraBotCanRutBai(this.camChuong);
    }
}

