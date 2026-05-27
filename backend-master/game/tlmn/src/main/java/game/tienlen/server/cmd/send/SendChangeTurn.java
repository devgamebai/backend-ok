/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tienlen.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendChangeTurn
extends BaseMsg {
    public boolean newRound;
    public byte curentChair;
    public byte prevChairThrowCard;
    public byte countDownTime;

    public SendChangeTurn() {
        super((short)3112);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.newRound));
        bf.put(this.curentChair);
        bf.put(this.prevChairThrowCard);
        bf.put(this.countDownTime);
        return this.packBuffer(bf);
    }
}

