/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.sam.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendDanhBai
extends BaseMsg {
    public byte chair;
    public byte[] cards;
    public byte numberOfRemainCards = 0;

    public SendDanhBai() {
        super((short)3101);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putByteArray(bf, this.cards);
        bf.put(this.numberOfRemainCards);
        return this.packBuffer(bf);
    }
}

