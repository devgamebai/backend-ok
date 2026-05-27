/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.caro.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendTakeTurn
extends BaseMsg {
    public int chair = 2;
    public int x;
    public int y;
    public int type;

    public SendTakeTurn() {
        super((short)3101);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        bf.put((byte)this.x);
        bf.put((byte)this.y);
        bf.put((byte)this.type);
        return this.packBuffer(bf);
    }
}

