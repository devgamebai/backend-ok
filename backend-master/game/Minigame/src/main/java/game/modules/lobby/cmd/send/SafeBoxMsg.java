/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SafeBoxMsg
extends BaseMsgEx {
    public long amount;

    public SafeBoxMsg() {
        super(20311);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putLong(bf, this.amount);
        return this.packBuffer(bf);
    }
}

