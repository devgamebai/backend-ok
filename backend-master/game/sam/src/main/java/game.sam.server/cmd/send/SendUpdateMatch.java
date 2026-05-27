/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.sam.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.sam.server.GamePlayer;
import java.nio.ByteBuffer;

public class SendUpdateMatch
extends BaseMsg {
    public byte chair;
    public byte startChair;
    public boolean[] hasInfoAtChair = new boolean[5];
    public GamePlayer[] pInfos = new GamePlayer[5];

    public SendUpdateMatch() {
        super((short)3123);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 5; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            bf.putLong(gp.gameMoneyInfo.currentMoney);
            bf.putInt(gp.getPlayerStatus());
        }
        return this.packBuffer(bf);
    }
}

