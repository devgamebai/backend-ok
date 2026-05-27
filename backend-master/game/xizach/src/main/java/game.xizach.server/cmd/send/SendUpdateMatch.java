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
import java.nio.ByteBuffer;

public class SendUpdateMatch
extends BaseMsg {
    public byte chair;
    public boolean[] hasInfoAtChair = new boolean[6];
    public GamePlayer[] pInfos = new GamePlayer[6];

    public SendUpdateMatch() {
        super((short)3123);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 6; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            this.putStr(bf, gp.pInfo.nickName);
            this.putStr(bf, gp.pInfo.avatarUrl);
            bf.putLong(gp.gameMoneyInfo.currentMoney);
            bf.putInt(gp.getPlayerStatus());
        }
        return this.packBuffer(bf);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SendUpdateMatch to chair ").append(this.chair).append("\n");
        for (int i = 0; i < 6; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            sb.append(gp.pInfo.nickName).append("/").append(gp.pInfo.avatarUrl).append("/").append(gp.gameMoneyInfo.currentMoney).append("/").append(gp.getPlayerStatus()).append("\n");
        }
        return sb.toString();
    }
}

