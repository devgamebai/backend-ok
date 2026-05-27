/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.poker.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendEndGame
extends BaseMsg {
    public long moneyPot = 0L;
    public long[] moneyArray = new long[9];
    public long[] balanceMoney = new long[9];
    public long[] ketQuaTinhTien = new long[9];
    public boolean[] winLost = new boolean[9];
    public long[] rank = new long[9];
    public byte[] publicCard = new byte[9];
    public byte[][] cards = new byte[9][];
    public byte[] groupCardName = new byte[9];
    public byte[][] maxCards = new byte[9][];
    public byte[] hasInfoAtChair = new byte[9];
    public int countdown;

    public SendEndGame() {
        super((short)3103);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyPot);
        this.putLongArray(bf, this.rank);
        this.putLongArray(bf, this.ketQuaTinhTien);
        this.putBooleanArray(bf, this.winLost);
        this.putLongArray(bf, this.moneyArray);
        this.putLongArray(bf, this.balanceMoney);
        this.putByteArray(bf, this.publicCard);
        this.putByteArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 9; ++i) {
            if (this.hasInfoAtChair[i] == 0) continue;
            this.putByteArray(bf, this.cards[i]);
            bf.put(this.groupCardName[i]);
            if (this.maxCards != null) {
                this.putByteArray(bf, this.maxCards[i]);
                continue;
            }
            this.putByteArray(bf, new byte[0]);
        }
        bf.put((byte)this.countdown);
        return this.packBuffer(bf);
    }
}

