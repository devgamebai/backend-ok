/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.lieng.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendShowCardSub
extends BaseMsg {
    public byte chair;
    public byte[] cards;

    public SendShowCardSub() {
        super((short)3112);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putByteArray(bf, this.cards);
        return this.packBuffer(bf);
    }
}

