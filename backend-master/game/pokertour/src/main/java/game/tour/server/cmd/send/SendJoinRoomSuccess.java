/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 */
package game.tour.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import game.tour.server.logic.PokerPlayerInfo;
import java.nio.ByteBuffer;

public class SendJoinRoomSuccess
extends BaseMsg {
    public int uChair;
    public int comission;
    public int comissionJackpot;
    public long moneyBet;
    public byte roomOwner;
    public int gameId;
    public int roomId;
    public int moneyType;
    public int rule;
    public byte[] playerStatus = new byte[6];
    public PlayerInfo[] playerList = new PlayerInfo[6];
    public PokerPlayerInfo[] moneyInfoList = new PokerPlayerInfo[6];
    public byte gameState;
    public byte gameAction;
    public byte[] handCardSize = new byte[9];
    public byte[] lastCard;
    public byte curentChair;
    public byte countDownTime;
    public int tourId = 0;

    public SendJoinRoomSuccess() {
        super((short)3118);
    }

    public SendJoinRoomSuccess(int i) {
        super((short)3118, i);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.uChair);
        bf.putLong(this.moneyBet);
        bf.put(this.roomOwner);
        bf.putInt(this.roomId);
        bf.putInt(this.gameId);
        bf.put((byte)this.moneyType);
        bf.put((byte)this.rule);
        this.putByteArray(bf, this.playerStatus);
        bf.putShort((short)this.playerList.length);
        for (int i = 0; i < this.playerList.length; ++i) {
            if (this.playerList[i] == null) {
                this.playerList[i] = new PlayerInfo();
            }
            this.putStr(bf, this.playerList[i].avatarUrl);
            this.putStr(bf, this.playerList[i].nickName);
            if (this.moneyInfoList[i] == null) {
                bf.putLong(0L);
                continue;
            }
            bf.putLong(this.moneyInfoList[i].currentMoney);
        }
        bf.put(this.gameAction);
        this.putByteArray(bf, this.handCardSize);
        bf.put(this.curentChair);
        bf.put(this.countDownTime);
        bf.putInt(40);
        bf.putInt(400);
        bf.put(this.gameState);
        bf.putInt(this.tourId);
        return this.packBuffer(bf);
    }
}

