/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.rangeRover;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class RangeRoverInfoMsg
extends BaseMsg {
    public String ngayX2;
    public byte remain;
    public long currentMoney;

    public RangeRoverInfoMsg() {
        super((short)13011);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        bf.putLong(this.currentMoney);
        this.putStr(bf, this.ngayX2);
        return this.packBuffer(bf);
    }
}

