/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.xizach.server.GamePlayer;
import game.xizach.server.sResultInfo;
import java.nio.ByteBuffer;

public class SendGameInfo
extends BaseMsg {
    public byte chair;
    public byte chuongChair;
    public sResultInfo res;
    public boolean isAutoStart;
    public int gameState;
    public int gameAction;
    public int countdownTime;
    public int comissionRate;
    public int jackpotRate;
    public int moneyType;
    public long moneyBet;
    public int gameId;
    public int roomId;
    public byte[][] cardList = new byte[6][];
    public int[] playerStatusList = new int[6];
    public GamePlayer[] pInfos = new GamePlayer[6];

    public SendGameInfo() {
        super((short)3110);
    }

    public byte[] createData() {
        int i;
        GamePlayer gp;
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        bf.put(this.chuongChair);
        bf.put((byte)this.gameState);
        this.putBoolean(bf, Boolean.valueOf(this.isAutoStart));
        bf.put((byte)this.gameAction);
        bf.put((byte)this.countdownTime);
        bf.put((byte)this.moneyType);
        this.putLong(bf, this.moneyBet);
        bf.putInt(this.gameId);
        bf.putInt(this.roomId);
        this.putIntArray(bf, this.playerStatusList);
        for (i = 0; i < 6; ++i) {
            if (this.playerStatusList[i] <= 0) continue;
            gp = this.pInfos[i];
            bf.put((byte)gp.getPlayerStatus());
            this.putLong(bf, gp.gameMoneyInfo.currentMoney);
            PlayerInfo pInfo = gp.pInfo;
            this.putStr(bf, pInfo.avatarUrl);
            this.putStr(bf, pInfo.nickName);
        }
        for (i = 0; i < 6; ++i) {
            if (this.playerStatusList[i] != 3) continue;
            gp = this.pInfos[i];
            this.putByteArray(bf, this.cardList[i]);
        }
        for (i = 0; i < 6; ++i) {
            if (this.playerStatusList[i] != 3) continue;
            gp = this.pInfos[i];
            this.putBoolean(bf, Boolean.valueOf(gp.hasDanBai));
        }
        return this.packBuffer(bf);
    }

    public void initPrivateInfo(GamePlayer gp) {
    }
}

