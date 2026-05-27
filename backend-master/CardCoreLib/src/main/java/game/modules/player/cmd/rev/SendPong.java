/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.player.cmd.rev;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendPong
extends BaseMsg {
    public long v;

    public SendPong() {
        super((short)1050);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.v);
        return this.packBuffer(bf);
    }
}

