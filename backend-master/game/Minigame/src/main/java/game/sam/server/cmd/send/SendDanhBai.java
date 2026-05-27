/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.sam.server.cmd.send;

import bitzero.util.common.business.Debug;
import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendDanhBai
extends BaseMsgEx {
    public byte chair;
    public byte[] cards;
    public byte numberOfRemainCards = 0;

    public SendDanhBai() {
        super(3101);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putByteArray(bf, this.cards);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.cards.length; ++i) {
            sb.append(this.cards[i]).append(" ");
        }
        Debug.trace(sb);
        bf.put(this.numberOfRemainCards);
        return this.packBuffer(bf);
    }
}

