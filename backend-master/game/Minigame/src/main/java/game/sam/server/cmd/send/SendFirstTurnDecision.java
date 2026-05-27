/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendFirstTurnDecision
extends BaseMsgEx {
    public boolean isRandom = false;
    public byte chair = (byte)-1;
    public byte[] cards = new byte[5];

    public SendFirstTurnDecision() {
        super(3108);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.isRandom));
        bf.put(this.chair);
        this.putByteArray(bf, this.cards);
        return this.packBuffer(bf);
    }
}

