/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendNotifyUsergetJackpot
extends BaseMsg {
    public long jackpot;
    public int chair;

    public SendNotifyUsergetJackpot() {
        super((short)3122);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putLong(bf, this.jackpot);
        bf.put((byte)this.chair);
        return this.packBuffer(bf);
    }
}

