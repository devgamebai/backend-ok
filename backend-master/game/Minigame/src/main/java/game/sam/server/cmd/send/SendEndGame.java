/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendEndGame
extends BaseMsgEx {
    public byte[] winType = new byte[5];
    public long[] moneyArray = new long[5];
    public long[] ketQuaTinhTien;
    public byte[][] cards = new byte[5][];
    public int countdown;

    public SendEndGame() {
        super(3103);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putByteArray(bf, this.winType);
        this.putLongArray(bf, this.ketQuaTinhTien);
        this.putLongArray(bf, this.moneyArray);
        for (int i = 0; i < 5; ++i) {
            this.putByteArray(bf, this.cards[i]);
        }
        bf.put((byte)this.countdown);
        return this.packBuffer(bf);
    }
}

