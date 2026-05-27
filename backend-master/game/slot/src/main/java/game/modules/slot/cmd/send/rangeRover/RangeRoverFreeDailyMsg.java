/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.rangeRover;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class RangeRoverFreeDailyMsg
extends BaseMsg {
    public byte remain = 0;

    public RangeRoverFreeDailyMsg() {
        super((short)13012);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        return this.packBuffer(bf);
    }
}

