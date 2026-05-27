/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendNotifyUsergetJackpot
extends BaseMsgEx {
    public long jackpot;
    public int chair;

    public SendNotifyUsergetJackpot() {
        super(3122);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putLong(bf, this.jackpot);
        bf.put((byte)this.chair);
        return this.packBuffer(bf);
    }
}

