/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  net.sf.json.JSONObject
 *  org.json.JSONObject
 */
package game.binh.server;

import bitzero.server.entities.User;
import game.binh.server.logic.Card;
import game.binh.server.logic.GroupCard;
import game.binh.server.logic.KetQuaSoBai;
import game.binh.server.logic.PlayerCard;
import game.binh.server.sPlayerInfo;
import game.binh.server.sResultInfo;
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
    public boolean sochi = false;
    public int tuDongChoi = 0;
    public int boSoChi = 0;
    public  User user = null;
    public PlayerInfo pInfo = null;
    public GameMoneyInfo gameMoneyInfo = null;
    public sPlayerInfo spInfo = new sPlayerInfo();
    public sResultInfo spRes = new sResultInfo();
    private volatile int playerStatus = 0;
    public boolean isSoChi = false;
    public boolean sapLang = false;
    public int yeuCauBotRoiPhong = -1;

    public String toString() {
        try {
            net.sf.json.JSONObject json = this.toJSONObject();
            if (json != null) {
                return json.toString();
            }
            return "{}";
        }
        catch (Exception e) {
            return "{}";
        }
    }

    public net.sf.json.JSONObject toJSONObject() {
        try {
            net.sf.json.JSONObject json = new net.sf.json.JSONObject();
            if (this.user != null) {
                json.put((Object)"user", (Object)this.user.getName());
            }
            json.put((Object)"reqQuitRoom", (Object)this.reqQuitRoom);
            json.put((Object)"playerStatus", (Object)this.playerStatus);
            if (this.gameMoneyInfo != null) {
                json.put((Object)"gameMoneyInfo", (Object)this.gameMoneyInfo.toJSONObject());
            }
            if (this.spInfo.handCards != null) {
                json.put((Object)"handCards", (Object)this.spInfo.handCards.toString());
            } else {
                json.put((Object)"handCards", (Object)"");
            }
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }

    public boolean dangChoSoChi() {
        return this.isPlaying() && !this.sochi;
    }

    public void prepareNewGame() {
        this.sochi = false;
        this.sapLang = false;
        this.spRes.resetResult();
        this.spInfo.clearInfo();
        this.tuDongChoi = 0;
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

    public void addCards(GroupCard groupCard, boolean cheat, int rule) {
        if (!cheat && this.user != null && !this.user.isBot()) {
            groupCard.kiemtraBo(rule);
        }
        this.spInfo.handCards = groupCard;
        this.spInfo.sorttedCard.ApplyNewGroupCards(this.spInfo.handCards, rule);
    }

    public int kiemTraMauBinh(int rule) {
        return this.spInfo.getKind(rule);
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

    public void autoSort(int rule) {
        this.spInfo.autoSort(rule);
    }

    public void thangThuaSapLang(GamePlayer gp2) {
        KetQuaSoBai kq11 = this.spRes.getResultWithPlayer(this.chair);
        KetQuaSoBai kq12 = gp2.spRes.getResultWithPlayer(this.chair);
        KetQuaSoBai kq22 = gp2.spRes.getResultWithPlayer(gp2.chair);
        KetQuaSoBai kq21 = this.spRes.getResultWithPlayer(gp2.chair);
        kq12.thangThuaSapLang(gp2.chair);
        kq21.thangThuaSapLang(this.chair);
        kq11.thangThuaSapLang(kq21);
        kq22.thangThuaSapLang(kq12);
    }
}

