/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.spartan;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class BigWinSpartanMsg
extends BaseMsg {
    public String username;
    public byte type;
    public long totalPrizes;
    public String timestamp;
    public short betValue;

    public BigWinSpartanMsg() {
        super((short)12010);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.username);
        bf.put(this.type);
        bf.putShort(this.betValue);
        bf.putLong(this.totalPrizes);
        this.putStr(bf, this.timestamp);
        return this.packBuffer(bf);
    }
}

