/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.sam.server.GamePlayer;
import java.nio.ByteBuffer;

public class SendUpdateMatch
extends BaseMsgEx {
    public byte chair;
    public byte startChair;
    public boolean[] hasInfoAtChair = new boolean[5];
    public GamePlayer[] pInfos = new GamePlayer[5];

    public SendUpdateMatch() {
        super(3123);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        bf.put(this.startChair);
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

