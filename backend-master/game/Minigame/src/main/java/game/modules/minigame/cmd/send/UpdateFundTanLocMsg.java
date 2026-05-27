/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateFundTanLocMsg
extends BaseMsgEx {
    public long value;

    public UpdateFundTanLocMsg() {
        super(2120);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        return this.packBuffer(bf);
    }
}

