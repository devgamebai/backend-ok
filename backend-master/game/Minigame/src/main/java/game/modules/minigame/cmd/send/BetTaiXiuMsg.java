/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class BetTaiXiuMsg
extends BaseMsgEx {
    public long currentMoney;

    public BetTaiXiuMsg() {
        super(2110);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.currentMoney);
        return this.packBuffer(buffer);
    }
}

