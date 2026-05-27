/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.spartan;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class MinimizeResultSpartanMsg
extends BaseMsg {
    public byte result;
    public long prize;
    public long curretMoney;

    public MinimizeResultSpartanMsg() {
        super((short)12014);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.result);
        this.putLong(bf, this.prize);
        this.putLong(bf, this.curretMoney);
        return this.packBuffer(bf);
    }
}

