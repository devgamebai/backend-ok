/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.cotuong.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendChangeTurn
extends BaseMsg {
    public int curentChair;
    public int turnTime;
    public int gameTime;

    public SendChangeTurn() {
        super((short)3104);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.curentChair);
        bf.putInt(this.turnTime);
        bf.putInt(this.gameTime);
        return this.packBuffer(bf);
    }
}

