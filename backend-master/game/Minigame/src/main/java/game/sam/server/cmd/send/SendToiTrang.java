/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendToiTrang
extends BaseMsgEx {
    public byte chair = (byte)5;

    public SendToiTrang() {
        super(3104);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        return this.packBuffer(bf);
    }
}

