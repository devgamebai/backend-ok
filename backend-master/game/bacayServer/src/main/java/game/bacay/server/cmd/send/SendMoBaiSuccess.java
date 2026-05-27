/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendMoBaiSuccess
extends BaseMsg {
    public int chair;
    public byte[] cards;
    public int bo;

    public SendMoBaiSuccess() {
        super((short)3101);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        this.putByteArray(bf, this.cards);
        bf.putInt(this.bo);
        return this.packBuffer(bf);
    }
}

