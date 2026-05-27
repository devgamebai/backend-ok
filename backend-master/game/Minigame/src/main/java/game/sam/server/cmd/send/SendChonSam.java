/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendChonSam
extends BaseMsgEx {
    public byte chair = (byte)5;
    public boolean baosam = false;

    public SendChonSam() {
        super(3100);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putBoolean(bf, Boolean.valueOf(this.baosam));
        return this.packBuffer(bf);
    }
}

