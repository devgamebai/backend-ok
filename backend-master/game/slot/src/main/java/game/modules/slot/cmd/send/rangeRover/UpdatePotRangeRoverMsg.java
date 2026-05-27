/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.rangeRover;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class UpdatePotRangeRoverMsg
extends BaseMsg {
    public long value100;
    public long value1000;
    public long value5000;
    public long value10000;
    public byte x2Room100 = 0;
    public byte x2Room1000 = 0;

    public UpdatePotRangeRoverMsg() {
        super((short)13002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value100);
        bf.putLong(this.value1000);
        bf.putLong(this.value5000);
        bf.putLong(this.value10000);
        bf.put(this.x2Room100);
        bf.put(this.x2Room1000);
        return this.packBuffer(bf);
    }
}

