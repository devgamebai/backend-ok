/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tienlen.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendFirstTurnDecision
extends BaseMsg {
    public boolean isRandom = false;
    public byte chair = (byte)-1;
    public byte[] cards = new byte[5];

    public SendFirstTurnDecision() {
        super((short)3108);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.isRandom));
        bf.put(this.chair);
        this.putByteArray(bf, this.cards);
        return this.packBuffer(bf);
    }
}

