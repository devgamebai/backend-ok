/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.lieng.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendNewRound
extends BaseMsg {
    public int roundId;
    public byte[] cards;
    public byte cards_name;
    public long potMoney;

    public SendNewRound() {
        super((short)3105);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.roundId);
        this.putByteArray(bf, this.cards);
        bf.put(this.cards_name);
        bf.putLong(this.potMoney);
        return this.packBuffer(bf);
    }
}

