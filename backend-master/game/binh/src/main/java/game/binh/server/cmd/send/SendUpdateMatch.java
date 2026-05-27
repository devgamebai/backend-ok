/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.binh.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.binh.server.GamePlayer;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import java.nio.ByteBuffer;

public class SendUpdateMatch
extends BaseMsg {
    public byte chair;
    public boolean[] hasInfoAtChair = new boolean[4];
    public GamePlayer[] pInfos = new GamePlayer[4];

    public SendUpdateMatch() {
        super((short)3123);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 4; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            this.putStr(bf, gp.pInfo.nickName);
            this.putStr(bf, gp.pInfo.avatarUrl);
            bf.putLong(gp.gameMoneyInfo.currentMoney);
            bf.putInt(gp.getPlayerStatus());
        }
        return this.packBuffer(bf);
    }
}

