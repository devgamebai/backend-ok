/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class VQMMMsg
extends BaseMsgEx {
    public String prizeVin;
    public String prizeXu;
    public String prizeSlot;
    public short remainCount;
    public long currentMoneyVin;
    public long currentMoneyXu;

    public VQMMMsg() {
        super(20042);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.prizeVin);
        this.putStr(bf, this.prizeXu);
        this.putStr(bf, this.prizeSlot);
        bf.putShort(this.remainCount);
        bf.putLong(this.currentMoneyVin);
        bf.putLong(this.currentMoneyXu);
        return this.packBuffer(bf);
    }
}

