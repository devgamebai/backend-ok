/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.thanden;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class ThanDenTotalFreeSpin
extends BaseMsg {
    public int prize;
    public byte ratio;

    public ThanDenTotalFreeSpin() {
        super((short)19011);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prize);
        bf.put(this.ratio);
        return super.createData();
    }
}

