/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.lieng.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendTakeTurn
extends BaseMsg {
    public int chair = 9;
    public int action;
    public long raiseAmount;
    public long currentBet;
    public long maxBet;
    public long currentMoney;
    public long raiseStep;
    public boolean raiseBlock;
    public long potMoney;

    public SendTakeTurn() {
        super((short)3101);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        bf.put((byte)this.action);
        this.putLong(bf, this.raiseAmount);
        this.putLong(bf, this.currentBet);
        this.putLong(bf, this.maxBet);
        this.putLong(bf, this.currentMoney);
        this.putLong(bf, this.raiseStep);
        this.putBoolean(bf, Boolean.valueOf(this.raiseBlock));
        this.putLong(bf, this.potMoney);
        return this.packBuffer(bf);
    }
}

