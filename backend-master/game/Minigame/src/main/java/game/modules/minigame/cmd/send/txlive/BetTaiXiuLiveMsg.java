/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.txlive;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class BetTaiXiuLiveMsg
extends BaseMsgEx {
    public long currentMoney;

    public BetTaiXiuLiveMsg() {
        super(29006);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.currentMoney);
        return this.packBuffer(buffer);
    }
}

