/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class TaiXiuJackpotMsg
extends BaseMsgEx {
    public long id;
    public long amount;
    public short typeJP;

    public TaiXiuJackpotMsg() {
        super(2199);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.typeJP);
        buffer.putLong(this.id);
        buffer.putLong(this.amount);
        return this.packBuffer(buffer);
    }
}

