/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendUpdateAutoStart
extends BaseMsgEx {
    public boolean isAutoStart;
    public byte autoStartTime;

    public SendUpdateAutoStart() {
        super(3107);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.isAutoStart));
        bf.put(this.autoStartTime);
        return this.packBuffer(bf);
    }
}

