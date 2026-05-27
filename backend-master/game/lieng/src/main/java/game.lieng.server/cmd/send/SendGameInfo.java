/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 */
package game.lieng.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import game.lieng.server.GamePlayer;
import game.lieng.server.logic.GroupCard;
import game.lieng.server.logic.LiengGameInfo;
import game.lieng.server.logic.LiengPlayerInfo;
import game.lieng.server.sPlayerInfo;
import java.nio.ByteBuffer;

public class SendGameInfo
extends BaseMsg {
    public int maxUserPerRoom;
    public byte chair;
    public byte[] cards;
    public int roundId = 0;
    public int gameState;
    public int gameAction;
    public int currentChair;
    public int countdownTime;
    public LiengGameInfo pokerGameInfo;
    public int moneyType;
    public long roomBet;
    public int gameId;
    public int roomId;
    public boolean[] hasInfoAtChair = new boolean[9];
    public GamePlayer[] pInfos = new GamePlayer[9];

    public SendGameInfo() {
        super((short)3110);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.maxUserPerRoom);
        bf.put(this.chair);
        this.putByteArray(bf, this.cards);
        if (this.pokerGameInfo != null) {
            bf.put((byte)this.pokerGameInfo.dealer);
            bf.put((byte)this.pokerGameInfo.smallBlind);
            bf.put((byte)this.pokerGameInfo.bigBlind);
            this.putLong(bf, this.pokerGameInfo.potMoney);
            this.putLong(bf, this.pokerGameInfo.maxBetMoney);
            this.putLong(bf, this.pokerGameInfo.lastRaise);
        } else {
            this.putByteArray(bf, new byte[0]);
            bf.put((byte)0);
            bf.put((byte)0);
            bf.put((byte)0);
            this.putLong(bf, 0L);
            this.putLong(bf, 0L);
            this.putLong(bf, 0L);
        }
        bf.put((byte)this.roundId);
        bf.put((byte)this.gameState);
        bf.put((byte)this.gameAction);
        bf.put((byte)this.countdownTime);
        bf.put((byte)this.currentChair);
        bf.put((byte)this.moneyType);
        this.putLong(bf, this.roomBet);
        bf.putInt(this.gameId);
        bf.putInt(this.roomId);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 9; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            LiengPlayerInfo playerInfo = gp.spInfo.pokerInfo;
            this.putBoolean(bf, Boolean.valueOf(playerInfo.fold));
            this.putBoolean(bf, Boolean.valueOf(playerInfo.allIn));
            this.putLong(bf, playerInfo.moneyBet);
            this.putLong(bf, playerInfo.currentMoney);
            bf.put((byte)gp.getPlayerStatus());
            PlayerInfo pInfo = gp.pInfo;
            this.putStr(bf, pInfo.avatarUrl);
            this.putStr(bf, pInfo.nickName);
        }
        bf.putInt(5);
        bf.putInt(200);
        return this.packBuffer(bf);
    }

    public void initPrivateInfo(GamePlayer gp) {
        this.chair = (byte)gp.chair;
        this.cards = gp.spInfo.handCards != null ? gp.spInfo.handCards.toByteArray() : new byte[0];
    }
}

