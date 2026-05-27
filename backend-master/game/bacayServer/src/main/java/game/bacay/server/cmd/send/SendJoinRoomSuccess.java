/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import java.nio.ByteBuffer;

public class SendJoinRoomSuccess
extends BaseMsg {
    public int chuongChair;
    public int uChair;
    public long moneyBet;
    public int gameId;
    public int roomId;
    public int moneyType;
    public int rule;
    public byte[] playerStatus = new byte[8];
    public PlayerInfo[] playerList = new PlayerInfo[8];
    public GameMoneyInfo[] moneyInfoList = new GameMoneyInfo[8];
    public byte gameAction;
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
        bf.put((byte)this.chuongChair);
        bf.putLong(this.moneyBet);
        bf.putInt(this.roomId);
        bf.putInt(this.gameId);
        bf.put((byte)this.moneyType);
        bf.put((byte)this.rule);
        this.putByteArray(bf, this.playerStatus);
        bf.putShort((short)8);
        for (int i = 0; i < 8; ++i) {
            PlayerInfo pInfo = this.playerList[i];
            GameMoneyInfo mInfo = this.moneyInfoList[i];
            if (pInfo != null && mInfo != null) {
                this.putStr(bf, pInfo.nickName);
                this.putStr(bf, pInfo.avatarUrl);
                bf.putLong(mInfo.currentMoney);
                continue;
            }
            this.putStr(bf, "");
            this.putStr(bf, "");
            bf.putLong(0L);
        }
        bf.put(this.gameAction);
        bf.put(this.countDownTime);
        return this.packBuffer(bf);
    }
}

