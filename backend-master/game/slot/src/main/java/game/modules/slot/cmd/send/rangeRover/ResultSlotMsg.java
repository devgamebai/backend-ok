/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.slot.cmd.send.rangeRover;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class ResultSlotMsg
extends BaseMsg {
    public long referenceId;
    public byte result;
    public String matrix = "";
    public String linesWin = "";
    public String haiSao = "";
    public long prize;
    public long currentMoney;

    public ResultSlotMsg() {
        super((short)13001);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.referenceId);
        bf.put(this.result);
        this.putStr(bf, this.matrix);
        this.putStr(bf, this.linesWin);
        this.putStr(bf, this.haiSao);
        bf.putLong(this.prize);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

