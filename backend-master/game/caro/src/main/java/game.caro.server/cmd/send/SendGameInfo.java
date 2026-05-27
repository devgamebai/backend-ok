/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.caro.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.caro.server.GamePlayer;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import java.nio.ByteBuffer;

public class SendGameInfo
extends BaseMsg {
    public int maxUserPerRoom;
    public byte chair;
    public byte[][] map;
    public byte lastX;
    public byte lastY;
    public int gameState;
    public int gameAction;
    public int currentChair;
    public int countdownTime;
    public int moneyType;
    public long roomBet;
    public int gameId;
    public int roomId;
    public boolean[] hasInfoAtChair = new boolean[2];
    public GamePlayer[] pInfos = new GamePlayer[2];

    public SendGameInfo() {
        super((short)3110);
    }

    public byte[] createData() {
        int i;
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.maxUserPerRoom);
        bf.put(this.chair);
        bf.putShort((short)this.map.length);
        for (i = 0; i < this.map.length; ++i) {
            this.putByteArray(bf, this.map[i]);
        }
        bf.put((byte)this.gameState);
        bf.put((byte)this.gameAction);
        bf.put((byte)this.countdownTime);
        bf.put((byte)this.currentChair);
        bf.put((byte)this.moneyType);
        this.putLong(bf, this.roomBet);
        bf.putInt(this.gameId);
        bf.putInt(this.roomId);
        bf.put(this.lastX);
        bf.put(this.lastY);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (i = 0; i < 2; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            bf.put((byte)gp.getPlayerStatus());
            PlayerInfo pInfo = gp.pInfo;
            this.putStr(bf, pInfo.avatarUrl);
            this.putStr(bf, pInfo.nickName);
            if (gp.gameMoneyInfo != null) {
                this.putLong(bf, gp.gameMoneyInfo.currentMoney);
            } else {
                this.putLong(bf, 0L);
            }
            bf.put((byte)gp.tickType);
        }
        return this.packBuffer(bf);
    }

    public void initPrivateInfo(GamePlayer gp) {
        this.chair = (byte)gp.chair;
    }
}

