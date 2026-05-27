/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.caothap;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdatePotCaoThapMsg
extends BaseMsgEx {
    public long value;

    public UpdatePotCaoThapMsg() {
        super(6003);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        return this.packBuffer(bf);
    }
}

