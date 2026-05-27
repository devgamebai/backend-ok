/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendDealCard
extends BaseMsgEx {
    public byte[] cards = new byte[10];
    public int toitrang = 0;
    public int gameId;

    public SendDealCard() {
        super(3105);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putByteArray(bf, this.cards);
        bf.put((byte)this.toitrang);
        bf.put((byte)40);
        bf.putInt(this.gameId);
        return this.packBuffer(bf);
    }
}

