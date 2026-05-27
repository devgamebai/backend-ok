/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendDongYDanhBien
extends BaseMsg {
    public int chair;
    public int rate;

    public SendDongYDanhBien() {
        super((short)3108);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        bf.put((byte)this.rate);
        return this.packBuffer(bf);
    }
}

