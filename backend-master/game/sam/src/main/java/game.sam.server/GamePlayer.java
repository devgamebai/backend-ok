/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.util.common.business.Debug
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  org.json.JSONObject
 */
package game.sam.server;

import bitzero.server.entities.User;
import bitzero.util.common.business.Debug;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.sam.server.logic.Card;
import game.sam.server.logic.GroupCard;
import game.sam.server.logic.Round;
import game.sam.server.logic.Turn;
import game.sam.server.logic.ai.SamAI;
import game.sam.server.sPlayerInfo;
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
    public long timeJoinRoom = 0L;
    public boolean reqQuitRoom = false;
    public boolean baosam = false;
    public boolean huyBaoSam = false;
    public boolean boLuot = false;
    public boolean baoMot = false;
    public boolean denBaoMot = false;
    public boolean choiTiepVanSau = true;
    public int tuDongChoiNhanh = 0;
    public User user = null;
    public PlayerInfo pInfo = null;
    public GameMoneyInfo gameMoneyInfo = null;
    public sPlayerInfo spInfo = new sPlayerInfo();
    private volatile int playerStatus = 0;
    public SamAI ai = new SamAI();

    public void prepareNewGame() {
        this.baosam = false;
        this.boLuot = false;
        this.baoMot = false;
        this.denBaoMot = false;
        this.huyBaoSam = false;
    }

    public boolean dangChoBaoSam() {
        return this.isPlaying() && !this.baosam && !this.huyBaoSam;
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
        this.spInfo.kiemTraNoHu();
        this.ai.initStart(this.chair, groupCard);
    }

    public int kiemTraToiTrang() {
        return this.spInfo.handCards.kiemTraToiTrang();
    }

    public boolean kiemTraBaoMot() {
        this.baoMot = this.spInfo.handCards.cards.size() == 1;
        return this.baoMot;
    }

    public void takeChair(User user, PlayerInfo pInfo, GameMoneyInfo moneyInfo) {
        this.user = user;
        this.pInfo = pInfo;
        this.gameMoneyInfo = moneyInfo;
        this.reqQuitRoom = false;
        user.setProperty((Object)"user_chair", (Object)this.chair);
    }

    public int takeTurn(boolean baomot, byte[] cards, Round round) {
        Debug.trace((Object[])new Object[]{this.pInfo.nickName, "cam bo bai", this.spInfo.handCards});
        Turn prevTurn = round.getPrevTurn();
        Turn turn = new Turn();
        GroupCard playCard = new GroupCard(cards);
        if (playCard.BO == 0) {
            Debug.trace((Object)"Turn loi khong dong bo");
            return 1;
        }
        if (!this.spInfo.handCards.kiemTraBoBaiDanhRa(playCard)) {
            Debug.trace((Object)"Thoi hai, hoac khong co bo bai duoc danh ra");
            return 1;
        }
        if (prevTurn == null) {
            turn.turnType = 1;
        } else {
            GroupCard prevCards = prevTurn.throwCard;
            if (!playCard.chatDuoc(prevCards)) {
                Debug.trace((Object)"Turn loi khong chat duoc");
                Debug.trace((Object)playCard.toString());
                Debug.trace((Object)prevCards.toString());
                return 1;
            }
        }
        if (playCard.coTheChatChong()) {
            round.soLaPhatChatChong = round.soLaChatChong;
            round.soLaChatChong += playCard.tinhChatChong();
            ++round.chatchong;
        }
        if (baomot) {
            this.denBaoMot = !GroupCard.kiemTraLonNhat(this.spInfo.handCards, playCard);
        }
        this.spInfo.handCards.minusCard(playCard);
        this.spInfo.thrownCards.add(playCard);
        turn.throwCard = playCard;
        Debug.trace((Object[])new Object[]{this.pInfo.nickName, "danh ra", playCard});
        this.kiemTraBaoMot();
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
        long maxWin;
        int soLaPhat = this.spInfo.handCards.demLaPhat();
        int soLaThoi = this.spInfo.handCards.demLaThoi();
        Debug.trace((Object)this.spInfo.handCards.toString());
        Debug.trace((Object[])new Object[]{"So la phat:", soLaPhat, "So la thoi:", soLaThoi, "kieu thang", kieuThang, "bet=", moneyBet});
        int factor = 1;
        int soLaThua = 0;
        if (kieuThang == 3 || kieuThang == 4) {
            factor = 3;
            soLaThua += 10 * factor;
        }
        if (kieuThang == 6) {
            factor = 4;
            soLaThua += 10 * factor;
        }
        if (kieuThang == 9 || kieuThang == 8 || kieuThang == 7) {
            factor = 3;
            soLaThua += 10 * factor;
        }
        if (kieuThang == 2 || kieuThang == 10) {
            factor = soLaPhat != 10 ? 1 : 2;
            soLaThua += soLaPhat * factor + soLaThoi;
        }
        long moneyWin = (long)soLaThua * moneyBet;
        if (kieuThang != 4 && kieuThang != 10 && moneyWin > (maxWin = Math.min(soTienNguoiThang, this.gameMoneyInfo.getCurrentMoneyFromCache()))) {
            moneyWin = maxWin;
        }
        return moneyWin;
    }

    public byte[] getMinCardS() {
        byte quanThuHai;
        byte[] cards = new byte[1];
        byte quanCuoi = (byte)this.spInfo.handCards.getLastCard().SO;
        cards[0] = quanCuoi == 12 ? ((quanThuHai = (byte)this.spInfo.handCards.cards.get((int)1).SO) == 12 ? (byte)this.spInfo.handCards.getLastCard().ID : (byte)this.spInfo.handCards.cards.get((int)0).ID) : (byte)this.spInfo.handCards.cards.get((int)0).ID;
        return cards;
    }

    public void kiemtraDenBaoMot(Round round) {
        Turn prevTurn = round.getPrevTurn();
        if (prevTurn != null) {
            GroupCard gc = prevTurn.throwCard;
            if (gc.BO == 1) {
                this.denBaoMot = GroupCard.kiemTraChatDuoc(this.spInfo.handCards, gc);
            }
        }
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

    public void updateAIDanhBai(byte[] cards) {
        GroupCard gc = new GroupCard(cards);
        this.ai.throwMyCard(this.chair, this.spInfo.handCards.cards.size(), gc);
    }
}

