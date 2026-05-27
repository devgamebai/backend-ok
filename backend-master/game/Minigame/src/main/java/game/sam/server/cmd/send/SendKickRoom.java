/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendKickRoom
extends BaseMsgEx {
    public static final int ERROR_MONEY = 1;
    public byte reason;

    public SendKickRoom() {
        super(3120);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.reason);
        return this.packBuffer(bf);
    }
}

