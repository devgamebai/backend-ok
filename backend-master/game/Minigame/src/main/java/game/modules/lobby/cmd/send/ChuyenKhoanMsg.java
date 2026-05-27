/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ChuyenKhoanMsg
extends BaseMsgEx {
    public long moneyUse;

    public ChuyenKhoanMsg() {
        super(20014);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyUse);
        return this.packBuffer(bf);
    }
}

