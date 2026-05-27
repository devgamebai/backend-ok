/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tienlen.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendEndGame
extends BaseMsg {
    public byte[] winType = new byte[4];
    public long[] moneyArray = new long[4];
    public long[] ketQuaTinhTien;
    public byte[][] cards = new byte[4][];
    public int countdown;

    public SendEndGame() {
        super((short)3103);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putByteArray(bf, this.winType);
        this.putLongArray(bf, this.ketQuaTinhTien);
        this.putLongArray(bf, this.moneyArray);
        for (int i = 0; i < 4; ++i) {
            this.putByteArray(bf, this.cards[i]);
        }
        bf.put((byte)this.countdown);
        return this.packBuffer(bf);
    }
}

