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
    public byte[] playerStatus = new byte[4];
    public PlayerInfo[] playerList = new PlayerInfo[4];
    public GameMoneyInfo[] moneyInfoList = new GameMoneyInfo[4];
    public byte gameAction;
    public byte[] handCardSize = new byte[5];
    public byte[] lastCard;
    public byte curentChair;
    public byte countDownTime;

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
        return this.packBuffer(bf);
    }
}

