/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.benley;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class BenleyInfoMsg
extends BaseMsg {
    public String ngayX2;
    public byte remain;
    public long currentMoney;
    public byte freeSpin;
    public String lines = "";

    public BenleyInfoMsg() {
        super((short)4009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.ngayX2);
        bf.put(this.remain);
        bf.putLong(this.currentMoney);
        bf.put(this.freeSpin);
        this.putStr(bf, this.lines);
        return this.packBuffer(bf);
    }
}

