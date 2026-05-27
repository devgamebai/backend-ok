/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tienlen.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendBaoChoBonDoiThong
extends BaseMsg {
    public int chair;

    public SendBaoChoBonDoiThong() {
        super((short)3124);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        return this.packBuffer(bf);
    }
}

