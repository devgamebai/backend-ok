/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.sam.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendChonSam
extends BaseMsg {
    public byte chair = (byte)5;
    public boolean baosam = false;

    public SendChonSam() {
        super((short)3100);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putBoolean(bf, Boolean.valueOf(this.baosam));
        return this.packBuffer(bf);
    }
}

