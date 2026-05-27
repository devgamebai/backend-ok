/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class VQVipMsg
extends BaseMsgEx {
    public int prizeVin;
    public short prizeMulti;
    public short remainCount;
    public long currentMoneyVin;

    public VQVipMsg() {
        super(20044);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prizeVin);
        bf.putShort(this.prizeMulti);
        bf.putShort(this.remainCount);
        bf.putLong(this.currentMoneyVin);
        return this.packBuffer(bf);
    }
}

