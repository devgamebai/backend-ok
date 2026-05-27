/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  org.json.JSONObject
 */
package game.tienlen.server;

import bitzero.server.entities.User;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.tienlen.server.logic.DemLaThoi;
import game.tienlen.server.logic.GroupCard;
import game.tienlen.server.logic.Round;
import game.tienlen.server.logic.Turn;
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
    public boolean boLuot = false;
    public boolean choiTiepVanSau = true;
    public User user = null;
    public PlayerInfo pInfo = null;
    public GameMoneyInfo gameMoneyInfo = null;
    public sPlayerInfo spInfo = new sPlayerInfo();
    public long timeJoinRoom = 0L;
    private volatile int playerStatus = 0;
    public int tuDongChoiNhanh = 0;

    public void prepareNewGame() {
        this.boLuot = false;
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

    public void addCards(GroupCard groupCard) {
        this.spInfo.handCards = groupCard;
        this.spInfo.thrownCards.clear();
    }

    public int kiemTraToiTrang() {
        if (this.spInfo.handCards != null) {
            return this.spInfo.handCards.kiemTraToiTrang();
        }
        return 0;
    }

    public void takeChair(User user, PlayerInfo pInfo, GameMoneyInfo moneyInfo) {
        this.user = user;
        this.pInfo = pInfo;
        this.gameMoneyInfo = moneyInfo;
        this.reqQuitRoom = false;
        user.setProperty((Object)"user_chair", (Object)this.chair);
    }

    public int takeTurn(GroupCard playCard, Round round) {
        Turn prevTurn = round.getPrevTurn();
        Turn turn = new Turn();
        if (playCard.BO == 0) {
            return 1;
        }
        if (!this.spInfo.handCards.kiemTraBoBaiDanhRa(playCard)) {
            return 1;
        }
        if (prevTurn == null) {
            turn.turnType = 1;
        } else {
            GroupCard prevCards = prevTurn.throwCard;
            if (!playCard.chatDuoc(prevCards)) {
                return 1;
            }
        }
        if (playCard.coTheChatChong()) {
            round.soLaPhatChatChong = round.soLaChatChong;
            round.soLaChatChong += playCard.tinhChatChong();
            if (playCard.BO == 4 || playCard.BO == 6 || playCard.BO == 5) {
                ++round.phatChatChong;
            }
        }
        this.spInfo.handCards.minusCard(playCard);
        this.spInfo.thrownCards.add(playCard);
        turn.throwCard = playCard;
        turn.turnType = this.spInfo.isEndCards() ? 3 : 2;
        turn.owner = this;
        round.addTurn(turn);
        return turn.turnType;
    }

    public boolean isPlaying() {
        return this.playerStatus == 3;
    }

    public boolean canPlayNextGame() {
        return this.tuDongChoiNhanh == 0 && !this.reqQuitRoom && this.checkMoneyCanPlay();
    }

    public boolean hasUser() {
        return this.playerStatus != 0;
    }

    public long calculateMoneyLost(int kieuThang, long soTienNguoiThang, long moneyBet) {
        long moneyWin;
        long maxWin;
        int factor = 1;
        int soLaThua = 0;
        if (kieuThang == 4 || kieuThang == 8 || kieuThang == 7 || kieuThang == 9 || kieuThang == 6 || kieuThang == 5) {
            factor = 2;
            soLaThua += 13 * factor;
        }
        if (kieuThang == 2) {
            DemLaThoi kq = this.spInfo.handCards.demLaThoi();
            int soLaPhat = kq.soLaPhat;
            int soLaThoi = this.spInfo.handCards.cards.size() - kq.soLaThoi;
            factor = this.spInfo.handCards.cards.size() != 13 ? 1 : 2;
            soLaThua += factor * (soLaPhat + soLaThoi);
        }
        if ((moneyWin = (long)soLaThua * moneyBet) > (maxWin = Math.min(soTienNguoiThang, this.gameMoneyInfo.getCurrentMoneyFromCache()))) {
            moneyWin = maxWin;
        }
        return moneyWin;
    }

    public byte[] getMinCardS() {
        byte quanCuoi = (byte)this.spInfo.handCards.getCardAt((int)0).ID;
        byte[] cards = new byte[]{quanCuoi};
        return cards;
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

    public boolean tuDongBoLuot() {
        return this.tuDongChoiNhanh >= 2;
    }
}

