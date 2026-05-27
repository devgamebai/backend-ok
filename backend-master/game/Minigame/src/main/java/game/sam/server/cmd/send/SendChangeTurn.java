/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendChangeTurn
extends BaseMsgEx {
    public boolean newRound;
    public byte curentChair;
    public byte countDownTime;

    public SendChangeTurn() {
        super(3112);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.newRound));
        bf.put(this.curentChair);
        bf.put(this.countDownTime);
        return this.packBuffer(bf);
    }
}

