/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class TanLocMsg
extends BaseMsgEx {
    public short result;
    public long currentMoney;

    public TanLocMsg() {
        super(2118);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putShort(this.result);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

