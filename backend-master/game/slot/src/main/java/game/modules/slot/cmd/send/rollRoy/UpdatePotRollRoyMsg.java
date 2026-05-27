/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.rollRoy;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class UpdatePotRollRoyMsg
extends BaseMsg {
    public long value100;
    public long value1000;
    public long value10000;
    public byte x2Room100 = 0;
    public byte x2Room1000 = 0;

    public UpdatePotRollRoyMsg() {
        super((short)5002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value100);
        bf.putLong(this.value1000);
        bf.putLong(this.value10000);
        bf.put(this.x2Room100);
        bf.put(this.x2Room1000);
        return this.packBuffer(bf);
    }
}

