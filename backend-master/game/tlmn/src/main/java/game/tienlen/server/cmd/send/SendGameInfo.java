/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.tienlen.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.tienlen.server.GamePlayer;
import game.tienlen.server.logic.Card;
import game.tienlen.server.logic.GroupCard;
import game.tienlen.server.sPlayerInfo;
import java.nio.ByteBuffer;
import java.util.List;

public class SendGameInfo
extends BaseMsg {
    public int maxUserPerRoom;
    public byte chair;
    public byte[] cards;
    public boolean boluot = false;
    public int kieuToiTrang = 0;
    public boolean newRound = false;
    public int gameState;
    public int gameAction;
    public int currentChair;
    public int countdownTime;
    public byte[] lastTurnCards;
    public int moneyType;
    public long roomBet;
    public int gameId;
    public int roomId;
    public boolean[] hasInfoAtChair = new boolean[4];
    public GamePlayer[] pInfos = new GamePlayer[4];

    public SendGameInfo() {
        super((short)3110);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.maxUserPerRoom);
        bf.put(this.chair);
        this.putByteArray(bf, this.cards);
        this.putBoolean(bf, Boolean.valueOf(this.boluot));
        bf.putInt(this.kieuToiTrang);
        this.putBoolean(bf, Boolean.valueOf(this.newRound));
        bf.put((byte)this.gameState);
        bf.put((byte)this.gameAction);
        bf.put((byte)this.countdownTime);
        bf.put((byte)this.currentChair);
        this.putByteArray(bf, this.lastTurnCards);
        bf.put((byte)this.moneyType);
        this.putLong(bf, this.roomBet);
        bf.putInt(this.gameId);
        bf.putInt(this.roomId);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 4; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            if (gp.spInfo.handCards != null) {
                bf.put((byte)gp.spInfo.handCards.cards.size());
            } else {
                bf.put((byte)0);
            }
            bf.put((byte)gp.getPlayerStatus());
            PlayerInfo pInfo = gp.pInfo;
            this.putStr(bf, pInfo.avatarUrl);
            bf.putInt(pInfo.userId);
            this.putStr(bf, pInfo.nickName);
            bf.putLong(gp.gameMoneyInfo.currentMoney);
        }
        return this.packBuffer(bf);
    }

    public void initPrivateInfo(GamePlayer gp) {
        this.chair = (byte)gp.chair;
        this.cards = gp.spInfo.handCards != null ? gp.spInfo.handCards.toByteArray() : new byte[0];
    }
}

